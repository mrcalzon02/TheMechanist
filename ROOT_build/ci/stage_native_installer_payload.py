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

from verify_runnable_distribution import (
    REMOTE_CLIENT_MAIN,
    verify_distribution,
)

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
    platform_name = str(summary["platform"])
    if expected_platform and platform_name != expected_platform:
        raise RuntimeError(
            f"canonical distribution platform {platform_name!r}, "
            f"expected {expected_platform!r}"
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
        "schema": 2,
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
        "remoteWorldAuthority": False,
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
        "remoteWorldAuthority": False,
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
