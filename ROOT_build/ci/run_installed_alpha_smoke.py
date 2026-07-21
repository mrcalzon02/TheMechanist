#!/usr/bin/env python3
"""Prove that an installed native alpha image can launch and run the game.

The package manager installs the candidate before this harness runs. The
harness reopens the installed jpackage image, verifies exact candidate identity,
launches the installed native launcher and game process, runs deterministic
launcher and single-player lifecycle smokes from the installed payload, proves
that mutable state remains outside the install tree, and writes a survival
marker that the workflow checks after uninstall.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import pathlib
import subprocess
import sys
import time
import traceback
from typing import Iterable

from verify_native_installer_image import verify_image

SCHEMA = 1


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def snapshot_tree(root: pathlib.Path) -> dict[str, dict[str, object]]:
    snapshot: dict[str, dict[str, object]] = {}
    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(root).as_posix()
        snapshot[relative] = {
            "size": path.stat().st_size,
            "sha256": sha256(path),
        }
    if not snapshot:
        raise RuntimeError(f"installed image contains no files: {root}")
    return snapshot


def profile_environment(profile: pathlib.Path) -> dict[str, str]:
    roaming = profile / "AppData" / "Roaming"
    local = profile / "AppData" / "Local"
    data = profile / ".local" / "share"
    config = profile / ".config"
    cache = profile / ".cache"
    for path in (profile, roaming, local, data, config, cache):
        path.mkdir(parents=True, exist_ok=True)
    env = os.environ.copy()
    env.update({
        "HOME": str(profile),
        "USERPROFILE": str(profile),
        "APPDATA": str(roaming),
        "LOCALAPPDATA": str(local),
        "XDG_DATA_HOME": str(data),
        "XDG_CONFIG_HOME": str(config),
        "XDG_CACHE_HOME": str(cache),
    })
    return env


def classpath(payload: pathlib.Path, role: str) -> str:
    jar_name = "TheMechanist.jar" if role == "client" else "TheMechanistServer.jar"
    separator = ";" if os.name == "nt" else ":"
    return (
        str(payload / "packages" / role / jar_name)
        + separator
        + str(payload / "packages" / "support" / "lib" / "*")
    )


def run_checked(
    command: Iterable[object],
    *,
    cwd: pathlib.Path,
    env: dict[str, str],
    label: str,
    timeout: int,
) -> dict[str, object]:
    rendered = [str(item) for item in command]
    completed = subprocess.run(
        rendered,
        cwd=cwd,
        env=env,
        text=True,
        encoding="utf-8",
        errors="replace",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=timeout,
        check=False,
    )
    if completed.returncode != 0:
        raise RuntimeError(
            f"{label} failed with exit {completed.returncode}: "
            f"stdout={completed.stdout[-4000:]!r} stderr={completed.stderr[-4000:]!r}"
        )
    return {
        "label": label,
        "command": rendered,
        "returnCode": completed.returncode,
        "stdoutTail": completed.stdout[-4000:],
        "stderrTail": completed.stderr[-4000:],
    }


def probe_running_process(
    command: Iterable[object],
    *,
    cwd: pathlib.Path,
    env: dict[str, str],
    label: str,
    hold_seconds: int,
) -> dict[str, object]:
    rendered = [str(item) for item in command]
    process = subprocess.Popen(
        rendered,
        cwd=cwd,
        env=env,
        text=True,
        encoding="utf-8",
        errors="replace",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    try:
        deadline = time.monotonic() + hold_seconds
        while time.monotonic() < deadline:
            code = process.poll()
            if code is not None:
                stdout, stderr = process.communicate(timeout=5)
                raise RuntimeError(
                    f"{label} exited before the running probe completed with {code}: "
                    f"stdout={stdout[-4000:]!r} stderr={stderr[-4000:]!r}"
                )
            time.sleep(0.25)
        return {
            "label": label,
            "command": rendered,
            "remainedRunningSeconds": hold_seconds,
            "running": True,
        }
    finally:
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=10)
        if process.stdout is not None:
            process.stdout.close()
        if process.stderr is not None:
            process.stderr.close()


def ensure_outside(path: pathlib.Path, forbidden_root: pathlib.Path, label: str) -> None:
    resolved = path.resolve()
    root = forbidden_root.resolve()
    if resolved == root or root in resolved.parents:
        raise RuntimeError(f"{label} leaked into the installed application tree: {resolved}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("image", type=pathlib.Path)
    parser.add_argument("--expected-platform", required=True)
    parser.add_argument("--expected-commit", required=True)
    parser.add_argument("--profile-root", type=pathlib.Path, required=True)
    parser.add_argument("--report", type=pathlib.Path, required=True)
    parser.add_argument("--hold-seconds", type=int, default=12)
    args = parser.parse_args()

    report_path = args.report.resolve()
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report: dict[str, object] = {
        "schema": SCHEMA,
        "status": "running",
        "image": str(args.image.resolve()),
        "expectedPlatform": args.expected_platform,
        "expectedCommit": args.expected_commit,
    }
    try:
        image_root = args.image.resolve()
        profile_root = args.profile_root.resolve()
        if not image_root.is_dir():
            raise RuntimeError(f"installed native image is missing: {image_root}")
        env = profile_environment(profile_root)
        summary = verify_image(image_root, args.expected_platform)
        if summary.get("commit") != args.expected_commit:
            raise RuntimeError(
                "installed image commit differs from workflow candidate: "
                f"{summary.get('commit')!r} != {args.expected_commit!r}"
            )
        if summary.get("releaseHardened") is not True:
            raise RuntimeError("installed image is not release hardened")
        if summary.get("playtestOperationsVerified") is not True:
            raise RuntimeError("installed image did not preserve tester operations")

        payload = pathlib.Path(str(summary["payload"])).resolve()
        runtime_java = pathlib.Path(str(summary["runtimeJava"])).resolve()
        launcher = pathlib.Path(str(summary["mainNativeLauncher"])).resolve()
        launcher_jar = payload / "launcher" / "MechanistLauncher.jar"
        client_jar = payload / "packages" / "client" / "TheMechanist.jar"
        for required in (runtime_java, launcher, launcher_jar, client_jar):
            if not required.is_file():
                raise RuntimeError(f"installed candidate file is missing: {required}")

        game_storage = profile_root / "TheMechanist" / "installed-game-storage"
        ensure_outside(profile_root, image_root, "profile root")
        ensure_outside(game_storage, image_root, "game storage")
        before = snapshot_tree(image_root)

        launcher_probe = probe_running_process(
            [launcher],
            cwd=image_root,
            env=env,
            label="installed native launcher",
            hold_seconds=args.hold_seconds,
        )
        launcher_verification = run_checked(
            [
                runtime_java,
                f"-Duser.home={profile_root}",
                f"-Dmechanist.launcher.installRoot={payload}",
                "-Djava.awt.headless=true",
                "-cp",
                launcher_jar,
                "mechanist.launcher.LauncherBundledPackageVerificationSmoke",
            ],
            cwd=profile_root,
            env=env,
            label="installed launcher bundled-package verification",
            timeout=300,
        )
        game_probe = probe_running_process(
            [
                runtime_java,
                f"-Duser.home={profile_root}",
                f"-Dmechanist.storage.root={game_storage}",
                "-Djava.awt.headless=false",
                "-cp",
                classpath(payload, "client"),
                "mechanist.TheMechanist",
            ],
            cwd=profile_root,
            env=env,
            label="installed game process",
            hold_seconds=args.hold_seconds,
        )
        single_player = run_checked(
            [
                runtime_java,
                f"-Duser.home={profile_root}",
                f"-Dmechanist.storage.root={game_storage}",
                "-Djava.awt.headless=true",
                "-cp",
                classpath(payload, "client"),
                "mechanist.SinglePlayerInternalHostLifecycleSmoke",
            ],
            cwd=profile_root,
            env=env,
            label="installed single-player lifecycle",
            timeout=900,
        )

        after = snapshot_tree(image_root)
        if after != before:
            added = sorted(set(after) - set(before))[:20]
            removed = sorted(set(before) - set(after))[:20]
            changed = sorted(
                path for path in set(before) & set(after)
                if before[path] != after[path]
            )[:20]
            raise RuntimeError(
                "installed application tree changed during launch: "
                f"added={added} removed={removed} changed={changed}"
            )

        profile_files = [path for path in profile_root.rglob("*") if path.is_file()]
        if not profile_files:
            raise RuntimeError("installed execution produced no mutable profile evidence")
        marker = profile_root / "installer-uninstall-survival.marker"
        marker.write_text(
            "The Mechanist installed-alpha mutable data survival marker\n",
            encoding="utf-8",
        )

        report.update({
            "status": "passed",
            "version": summary.get("version"),
            "platform": summary.get("platform"),
            "commit": summary.get("commit"),
            "releaseHardened": True,
            "playtestOperationsVerified": True,
            "playtestDocumentCount": summary.get("playtestDocumentCount"),
            "installedImageVerified": True,
            "nativeLauncherRunning": True,
            "gameProcessRunning": True,
            "launcherBundledPackageVerification": True,
            "singlePlayerLifecycle": True,
            "installTreeImmutable": True,
            "mutableStorageOutsideInstall": True,
            "profileFileCount": len(profile_files) + 1,
            "survivalMarker": str(marker),
            "launcherProbe": launcher_probe,
            "gameProbe": game_probe,
            "launcherVerification": launcher_verification,
            "singlePlayer": single_player,
        })
        report_path.write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"INSTALLED ALPHA DEPLOYMENT SMOKE PASSED: {report_path}")
        return 0
    except Exception as exc:  # noqa: BLE001 - one deployment failure surface
        report["status"] = "failed"
        report["errorType"] = type(exc).__name__
        report["error"] = str(exc)
        report["traceback"] = traceback.format_exc()
        report_path.write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"INSTALLED ALPHA DEPLOYMENT SMOKE FAILED: {exc}", file=sys.stderr)
        print(f"Report: {report_path}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
