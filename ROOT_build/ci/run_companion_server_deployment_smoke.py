#!/usr/bin/env python3
"""Run a packaged companion server and prove packaged clients can join it.

The harness accepts either a canonical portable distribution or a native
jpackage app image. It starts the packaged headless server as a separate process,
runs the packaged external-client join smoke, stops and restarts the server from
the same storage ledger, and requires the packaged clients to resume through
protected client token custody. It does not claim movement, map, inventory, or
full remote-world authority.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import pathlib
import signal
import socket
import subprocess
import sys
import time
import traceback
from typing import Iterable

from verify_native_installer_image import verify_image
from verify_runnable_distribution import verify_distribution

SCHEMA = 1
LOOPBACK = "127.0.0.1"


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def snapshot_tree(root: pathlib.Path) -> dict[str, tuple[int, str]]:
    result: dict[str, tuple[int, str]] = {}
    for path in sorted(root.rglob("*")):
        if path.is_file():
            result[path.relative_to(root).as_posix()] = (
                path.stat().st_size,
                sha256(path),
            )
    if not result:
        raise RuntimeError(f"candidate contains no files: {root}")
    return result


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


def resolve_candidate(
    source: pathlib.Path,
    expected_platform: str,
    expected_commit: str,
) -> dict[str, object]:
    source = source.resolve()
    if (source / "manifests" / "runtime-manifest.json").is_file():
        summary = verify_distribution(
            source,
            archive=None,
            require_release_hardened=True,
        )
        payload = source
        runtime_java = source / "runtime" / "bin" / (
            "java.exe" if expected_platform.startswith("windows-") else "java"
        )
        candidate_kind = "canonical-distribution"
    else:
        summary = verify_image(source, expected_platform)
        payload = pathlib.Path(str(summary["payload"])).resolve()
        runtime_java = pathlib.Path(str(summary["runtimeJava"])).resolve()
        candidate_kind = "native-app-image"
    if summary.get("platform") != expected_platform:
        raise RuntimeError(
            f"candidate platform {summary.get('platform')!r}, expected {expected_platform!r}"
        )
    if summary.get("commit") != expected_commit:
        raise RuntimeError(
            f"candidate commit {summary.get('commit')!r}, expected {expected_commit!r}"
        )
    if summary.get("releaseHardened") is not True:
        raise RuntimeError("companion deployment candidate is not release hardened")
    if not runtime_java.is_file():
        raise RuntimeError(f"candidate runtime Java is missing: {runtime_java}")
    return {
        "kind": candidate_kind,
        "root": source,
        "payload": payload,
        "runtimeJava": runtime_java,
        "summary": summary,
    }


def choose_port() -> int:
    for port in range(25500, 25600):
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as probe:
            probe.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            try:
                probe.bind((LOOPBACK, port))
            except OSError:
                continue
            return port
    raise RuntimeError("no free governed companion-server port is available")


def wait_for_port(process: subprocess.Popen[str], port: int, timeout: float) -> None:
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline:
        code = process.poll()
        if code is not None:
            raise RuntimeError(f"companion server exited before bind with code {code}")
        try:
            with socket.create_connection((LOOPBACK, port), timeout=0.5):
                return
        except OSError:
            time.sleep(0.2)
    raise RuntimeError(f"companion server did not bind {LOOPBACK}:{port}")


def start_server(
    java: pathlib.Path,
    payload: pathlib.Path,
    server_storage: pathlib.Path,
    profile_root: pathlib.Path,
    env: dict[str, str],
    port: int,
    world_id: str,
    stdout_path: pathlib.Path,
    stderr_path: pathlib.Path,
) -> tuple[subprocess.Popen[str], object, object, list[str]]:
    command = [
        str(java),
        f"-Duser.home={profile_root}",
        f"-Dmechanist.storage.root={server_storage}",
        "-Djava.awt.headless=true",
        "-cp",
        classpath(payload, "server"),
        "mechanist.MechanistServerMain",
        "--host",
        "--no-steam",
        f"--bind={LOOPBACK}",
        f"--port={port}",
        f"--world-id={world_id}",
        "--world-name=Companion Deployment Alpha",
        "--max-players=4",
    ]
    stdout_path.parent.mkdir(parents=True, exist_ok=True)
    stdout_stream = stdout_path.open("w", encoding="utf-8", newline="\n")
    stderr_stream = stderr_path.open("w", encoding="utf-8", newline="\n")
    creationflags = 0
    if os.name == "nt":
        creationflags = subprocess.CREATE_NEW_PROCESS_GROUP  # type: ignore[attr-defined]
    process = subprocess.Popen(
        command,
        cwd=profile_root,
        env=env,
        text=True,
        encoding="utf-8",
        errors="replace",
        stdout=stdout_stream,
        stderr=stderr_stream,
        creationflags=creationflags,
    )
    try:
        wait_for_port(process, port, 30.0)
    except Exception:
        stop_server(process)
        stdout_stream.close()
        stderr_stream.close()
        raise
    return process, stdout_stream, stderr_stream, command


def stop_server(process: subprocess.Popen[str]) -> None:
    if process.poll() is not None:
        return
    try:
        if os.name == "nt":
            process.send_signal(signal.CTRL_BREAK_EVENT)  # type: ignore[attr-defined]
        else:
            process.send_signal(signal.SIGTERM)
        process.wait(timeout=20)
    except (OSError, subprocess.TimeoutExpired):
        if process.poll() is None:
            process.kill()
            process.wait(timeout=10)


def run_client_join(
    java: pathlib.Path,
    payload: pathlib.Path,
    profile_root: pathlib.Path,
    client_storage: pathlib.Path,
    env: dict[str, str],
    port: int,
    server_key: str,
    profile_prefix: str,
    expect_resume: bool,
) -> dict[str, object]:
    command = [
        str(java),
        f"-Duser.home={profile_root}",
        "-Djava.awt.headless=true",
        "-cp",
        classpath(payload, "client"),
        "mechanist.CompanionServerExternalClientJoinSmoke",
        f"--host={LOOPBACK}",
        f"--port={port}",
        f"--server-key={server_key}",
        f"--profile-prefix={profile_prefix}",
        f"--client-storage={client_storage}",
        f"--expect-initial-resume={str(expect_resume).lower()}",
    ]
    completed = subprocess.run(
        command,
        cwd=profile_root,
        env=env,
        text=True,
        encoding="utf-8",
        errors="replace",
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=180,
        check=False,
    )
    if completed.returncode != 0:
        raise RuntimeError(
            f"packaged client join failed with exit {completed.returncode}: "
            f"stdout={completed.stdout[-5000:]!r} stderr={completed.stderr[-5000:]!r}"
        )
    if "CompanionServerExternalClientJoinSmoke PASS" not in completed.stdout:
        raise RuntimeError("packaged client join did not print its PASS authority line")
    return {
        "command": command,
        "returnCode": completed.returncode,
        "expectedInitialResume": expect_resume,
        "stdoutTail": completed.stdout[-5000:],
        "stderrTail": completed.stderr[-5000:],
    }


def count_files(root: pathlib.Path) -> int:
    return sum(1 for path in root.rglob("*") if path.is_file())


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=pathlib.Path)
    parser.add_argument("--expected-platform", required=True)
    parser.add_argument("--expected-commit", required=True)
    parser.add_argument("--work-root", type=pathlib.Path, required=True)
    parser.add_argument("--report", type=pathlib.Path, required=True)
    args = parser.parse_args()

    report_path = args.report.resolve()
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report: dict[str, object] = {
        "schema": SCHEMA,
        "status": "running",
        "source": str(args.source.resolve()),
        "expectedPlatform": args.expected_platform,
        "expectedCommit": args.expected_commit,
    }
    first_process: subprocess.Popen[str] | None = None
    second_process: subprocess.Popen[str] | None = None
    streams: list[object] = []
    try:
        candidate = resolve_candidate(
            args.source,
            args.expected_platform,
            args.expected_commit,
        )
        source_root = pathlib.Path(str(candidate["root"])).resolve()
        payload = pathlib.Path(str(candidate["payload"])).resolve()
        java = pathlib.Path(str(candidate["runtimeJava"])).resolve()
        summary = candidate["summary"]
        if not isinstance(summary, dict):
            raise RuntimeError("candidate verifier returned malformed summary")

        work_root = args.work_root.resolve()
        profile_root = work_root / "Companion Profile With Spaces"
        server_storage = work_root / "Companion Server Storage With Spaces"
        client_storage = work_root / "Companion Client Tokens With Spaces"
        for path in (profile_root, server_storage, client_storage):
            path.mkdir(parents=True, exist_ok=True)
            if source_root == path or source_root in path.parents:
                raise RuntimeError(f"mutable companion path is inside candidate: {path}")
        env = profile_environment(profile_root)
        before = snapshot_tree(source_root)
        port = choose_port()
        world_id = "companion-deployment-alpha-world"
        server_key = "limited-alpha/companion-deployment-host"
        profile_prefix = "companion.deployment"

        first_stdout = work_root / "logs" / "server-first.stdout.log"
        first_stderr = work_root / "logs" / "server-first.stderr.log"
        first_process, first_out, first_err, first_command = start_server(
            java,
            payload,
            server_storage,
            profile_root,
            env,
            port,
            world_id,
            first_stdout,
            first_stderr,
        )
        streams.extend((first_out, first_err))
        first_join = run_client_join(
            java,
            payload,
            profile_root,
            client_storage,
            env,
            port,
            server_key,
            profile_prefix,
            False,
        )
        stop_server(first_process)
        first_out.close()
        first_err.close()
        streams.remove(first_out)
        streams.remove(first_err)
        first_process = None

        second_stdout = work_root / "logs" / "server-restart.stdout.log"
        second_stderr = work_root / "logs" / "server-restart.stderr.log"
        second_process, second_out, second_err, second_command = start_server(
            java,
            payload,
            server_storage,
            profile_root,
            env,
            port,
            world_id,
            second_stdout,
            second_stderr,
        )
        streams.extend((second_out, second_err))
        restart_join = run_client_join(
            java,
            payload,
            profile_root,
            client_storage,
            env,
            port,
            server_key,
            profile_prefix,
            True,
        )
        stop_server(second_process)
        second_out.close()
        second_err.close()
        streams.remove(second_out)
        streams.remove(second_err)
        second_process = None

        first_log = first_stdout.read_text(encoding="utf-8", errors="replace")
        restart_log = second_stdout.read_text(encoding="utf-8", errors="replace")
        for label, text in (("first", first_log), ("restart", restart_log)):
            if LOOPBACK not in text:
                raise RuntimeError(f"{label} server log did not report exact loopback bind")
            if "The Mechanist server is hosting" not in text:
                raise RuntimeError(f"{label} server process did not enter hosting state")
        if count_files(server_storage) < 1:
            raise RuntimeError("external companion server produced no persisted storage")
        if count_files(client_storage) < 2:
            raise RuntimeError("packaged clients did not produce separate token custody")

        after = snapshot_tree(source_root)
        if after != before:
            raise RuntimeError("companion server/client execution mutated the packaged candidate")

        report.update({
            "status": "passed",
            "candidateKind": candidate["kind"],
            "version": summary.get("version"),
            "platform": summary.get("platform"),
            "commit": summary.get("commit"),
            "releaseHardened": True,
            "exactLoopbackBind": True,
            "externalServerProcess": True,
            "externalServerRestart": True,
            "packagedTwoClientJoin": True,
            "packagedClientResumeAfterServerRestart": True,
            "connectedOnlyRoster": True,
            "readinessBroadcast": True,
            "boundedRelay": True,
            "authenticatedWaitAuthority": True,
            "movementAuthority": False,
            "mapAuthority": False,
            "fullRemoteWorldAuthority": False,
            "candidateTreeImmutable": True,
            "mutableStorageOutsideCandidate": True,
            "serverStorageFileCount": count_files(server_storage),
            "clientCustodyFileCount": count_files(client_storage),
            "port": port,
            "firstServerCommand": first_command,
            "restartServerCommand": second_command,
            "firstJoin": first_join,
            "restartJoin": restart_join,
            "firstServerStdout": str(first_stdout),
            "firstServerStderr": str(first_stderr),
            "restartServerStdout": str(second_stdout),
            "restartServerStderr": str(second_stderr),
        })
        report_path.write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"COMPANION SERVER DEPLOYMENT SMOKE PASSED: {report_path}")
        return 0
    except Exception as exc:  # noqa: BLE001 - one process-gate failure surface
        report["status"] = "failed"
        report["errorType"] = type(exc).__name__
        report["error"] = str(exc)
        report["traceback"] = traceback.format_exc()
        report_path.write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"COMPANION SERVER DEPLOYMENT SMOKE FAILED: {exc}", file=sys.stderr)
        print(f"Report: {report_path}", file=sys.stderr)
        return 1
    finally:
        if first_process is not None:
            stop_server(first_process)
        if second_process is not None:
            stop_server(second_process)
        for stream in streams:
            try:
                stream.close()
            except Exception:
                pass


if __name__ == "__main__":
    raise SystemExit(main())
