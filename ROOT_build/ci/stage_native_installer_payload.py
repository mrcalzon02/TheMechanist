#!/usr/bin/env python3
"""Stage a verified canonical distribution for native jpackage installers.

The canonical portable distribution remains the source of truth. Native
installers consume its verified launcher, client, server, support libraries,
documentation, and launch scripts instead of rebuilding a competing package
tree. The canonical jlink runtime is supplied to jpackage separately and is
therefore not duplicated inside the app payload.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import shutil
import sys

from verify_limited_alpha_operations import (
    verify_distribution as verify_playtest_distribution,
    verify_source as verify_playtest_source,
)
from verify_runnable_distribution import (
    REMOTE_CLIENT_MAIN,
    verify_distribution,
)

ROOT = pathlib.Path(__file__).resolve().parents[2]
LAUNCHER_MANIFEST = pathlib.PurePosixPath(
    "manifests/launcher-runtime-manifest.json"
)
COPY_ROOTS = ("launcher", "packages", "docs")
COPY_FILES = (
    "Run-The-Mechanist.cmd",
    "Run-Client-Direct.cmd",
    "Run-Remote-Client.cmd",
    "Run-Server.cmd",
    "run-the-mechanist.sh",
    "run-client-direct.sh",
    "run-remote-client.sh",
    "run-server.sh",
)


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def copy_optional(source: pathlib.Path, destination: pathlib.Path) -> None:
    if source.is_dir():
        shutil.copytree(source, destination, dirs_exist_ok=True)
    elif source.is_file():
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)


def platform_remote_launcher(platform_name: str) -> str:
    return (
        "Run-Remote-Client.cmd"
        if platform_name.startswith("windows-")
        else "run-remote-client.sh"
    )


def verify_staged_playtest_documents(
    output: pathlib.Path,
    playtest: dict[str, object],
) -> list[dict[str, object]]:
    records = playtest.get("documents")
    if not isinstance(records, list) or len(records) != 8:
        raise RuntimeError(
            "native staging requires exactly eight verified playtest documents"
        )
    staged: list[dict[str, object]] = []
    for record in records:
        if not isinstance(record, dict):
            raise RuntimeError(
                "native staging received a malformed playtest document record"
            )
        relative = record.get("path")
        expected_hash = record.get("sha256")
        if not isinstance(relative, str) or not relative.startswith("docs/"):
            raise RuntimeError(
                f"native staging received an invalid playtest path: {relative!r}"
            )
        if not isinstance(expected_hash, str) or len(expected_hash) != 64:
            raise RuntimeError(
                f"native staging received an invalid playtest hash: {relative}"
            )
        path = output.joinpath(*pathlib.PurePosixPath(relative).parts)
        if not path.is_file():
            raise RuntimeError(
                f"native installer payload is missing playtest document: {relative}"
            )
        actual_hash = sha256(path)
        if actual_hash != expected_hash:
            raise RuntimeError(
                f"native staged playtest document hash mismatch: {relative}"
            )
        staged.append({
            "path": relative,
            "size": path.stat().st_size,
            "sha256": actual_hash,
        })
    return staged


def stage_payload(
    distribution: pathlib.Path,
    output: pathlib.Path,
    expected_platform: str | None,
) -> dict[str, object]:
    distribution = distribution.resolve()
    output = output.resolve()
    summary = verify_distribution(
        distribution,
        archive=None,
        require_release_hardened=True,
    )
    playtest_source = verify_playtest_source(ROOT)
    playtest = verify_playtest_distribution(
        distribution,
        playtest_source,
        True,
    )
    platform_name = str(summary["platform"])
    if expected_platform and platform_name != expected_platform:
        raise RuntimeError(
            f"canonical distribution platform {platform_name!r}, "
            f"expected {expected_platform!r}"
        )
    if playtest.get("platform") != platform_name:
        raise RuntimeError(
            "playtest operations platform disagrees with canonical distribution"
        )
    if playtest.get("commit") != summary.get("commit"):
        raise RuntimeError(
            "playtest operations commit disagrees with canonical distribution"
        )
    if summary.get("remoteClientEntryPoint") != REMOTE_CLIENT_MAIN:
        raise RuntimeError(
            "canonical distribution does not expose the governed remote-client entry"
        )

    source_manifest = distribution / "manifests" / "runtime-manifest.json"
    launcher_manifest = distribution.joinpath(*LAUNCHER_MANIFEST.parts)

    shutil.rmtree(output, ignore_errors=True)
    output.mkdir(parents=True, exist_ok=True)

    for name in COPY_ROOTS:
        copy_optional(distribution / name, output / name)
    for name in COPY_FILES:
        copy_optional(distribution / name, output / name)

    staged_playtest_documents = verify_staged_playtest_documents(output, playtest)

    active_manifest = output.joinpath(*LAUNCHER_MANIFEST.parts)
    active_manifest.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(launcher_manifest, active_manifest)

    certification = output / "certification"
    certification.mkdir(parents=True, exist_ok=True)
    shutil.copy2(
        source_manifest,
        certification / "canonical-runtime-manifest.json",
    )

    remote_launcher = platform_remote_launcher(platform_name)
    source_record = {
        "schema": 4,
        "sourceDistribution": distribution.name,
        "version": summary["version"],
        "platform": platform_name,
        "commit": summary["commit"],
        "javaRelease": summary["javaRelease"],
        "releaseHardened": summary["releaseHardened"],
        "canonicalManifestSha256": sha256(source_manifest),
        "canonicalArtifactCount": summary["artifactCount"],
        "launcherCompatibilityManifest": LAUNCHER_MANIFEST.as_posix(),
        "remoteClientEntryPoint": REMOTE_CLIENT_MAIN,
        "portableRemoteClientLauncher": remote_launcher,
        "nativeRemoteClientLauncher": "The Mechanist Remote Lobby",
        "runtimeImageSource": (
            "canonical distribution runtime/ supplied to jpackage"
        ),
        "mutableStorageIncluded": False,
        "playtestOperationsVerified": True,
        "playtestDocumentCount": len(staged_playtest_documents),
        "playtestDocuments": staged_playtest_documents,
        "authenticatedWaitAuthority": True,
        "waitCommand": "WAIT",
        "movementAuthority": False,
        "mapAuthority": False,
        "fullRemoteWorldAuthority": False,
        "remoteGameplayCertified": False,
    }
    source_record_path = (
        certification / "installer-source-verification.json"
    )
    source_record_path.write_text(
        json.dumps(source_record, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )

    required = (
        output / "launcher" / "MechanistLauncher.jar",
        output / "packages" / "client" / "TheMechanist.jar",
        output / "packages" / "server" / "TheMechanistServer.jar",
        active_manifest,
        output / remote_launcher,
    )
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise RuntimeError(
            f"native installer payload is incomplete: {missing}"
        )

    return {
        "payload": str(output),
        "runtimeImage": str(distribution / "runtime"),
        "version": summary["version"],
        "platform": platform_name,
        "commit": summary["commit"],
        "releaseHardened": True,
        "sourceVerification": str(source_record_path),
        "remoteClientEntryPoint": REMOTE_CLIENT_MAIN,
        "portableRemoteClientLauncher": remote_launcher,
        "nativeRemoteClientLauncher": "The Mechanist Remote Lobby",
        "mutableStorageIncluded": False,
        "playtestOperationsVerified": True,
        "playtestDocumentCount": len(staged_playtest_documents),
        "authenticatedWaitAuthority": True,
        "waitCommand": "WAIT",
        "movementAuthority": False,
        "mapAuthority": False,
        "fullRemoteWorldAuthority": False,
        "remoteGameplayCertified": False,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("distribution", type=pathlib.Path)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    parser.add_argument("--expected-platform")
    parser.add_argument("--report", type=pathlib.Path)
    args = parser.parse_args()
    try:
        result = stage_payload(
            args.distribution,
            args.output,
            args.expected_platform,
        )
    except Exception as exc:  # noqa: BLE001 - one packaging failure surface
        print(f"NATIVE INSTALLER STAGING FAILED: {exc}", file=sys.stderr)
        return 1
    rendered = json.dumps(result, indent=2, sort_keys=True)
    print(rendered)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(rendered + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
