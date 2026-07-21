#!/usr/bin/env python3
"""Verify cross-workflow evidence and prepare one limited-alpha prerelease."""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import re
import sys
import traceback
from typing import Any

SCHEMA = 1
PLATFORMS = ("windows-x64", "linux-x64")

INSTALLER_TRUE = (
    "releaseHardened",
    "installerInstalled",
    "installedImageVerified",
    "nativeLauncherRunning",
    "gameProcessRunning",
    "launcherBundledPackageVerification",
    "singlePlayerLifecycle",
    "installTreeImmutable",
    "installerUninstalled",
    "installedExecutablesRemoved",
    "mutableDataSurvivedUninstall",
)

COMPANION_TRUE = (
    "releaseHardened",
    "exactLoopbackBind",
    "externalServerProcess",
    "externalServerRestart",
    "packagedTwoClientJoin",
    "packagedClientResumeAfterServerRestart",
    "connectedOnlyRoster",
    "readinessBroadcast",
    "boundedRelay",
    "authenticatedWaitAuthority",
    "candidateTreeImmutable",
    "mutableStorageOutsideCandidate",
)

COMPANION_FALSE = (
    "movementAuthority",
    "mapAuthority",
    "fullRemoteWorldAuthority",
)


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def exact_file(root: pathlib.Path, name: str) -> pathlib.Path:
    matches = sorted(path for path in root.rglob(name) if path.is_file())
    if len(matches) != 1:
        raise RuntimeError(f"expected exactly one {name}, found {matches}")
    return matches[0]


def exact_pattern(root: pathlib.Path, pattern: str, label: str) -> pathlib.Path:
    matches = sorted(path for path in root.rglob(pattern) if path.is_file())
    if len(matches) != 1:
        raise RuntimeError(f"expected exactly one {label}, found {matches}")
    return matches[0]


def load(path: pathlib.Path, label: str) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"invalid JSON in {label}: {path}: {exc}") from exc
    if not isinstance(value, dict):
        raise RuntimeError(f"{label} root is not an object: {path}")
    return value


def require(data: dict[str, Any], key: str, expected: Any, label: str) -> None:
    actual = data.get(key)
    if actual != expected:
        raise RuntimeError(
            f"{label} requires {key}={expected!r}, found {actual!r}"
        )


def require_identity(
    data: dict[str, Any],
    *,
    platform: str,
    commit: str,
    label: str,
) -> str:
    require(data, "status", "passed", label)
    require(data, "platform", platform, label)
    require(data, "commit", commit, label)
    version = data.get("version")
    if not isinstance(version, str) or not version.strip():
        raise RuntimeError(f"{label} has no candidate version")
    return version.strip()


def write_output(path: pathlib.Path | None, values: dict[str, str]) -> None:
    if path is None:
        return
    with path.open("a", encoding="utf-8", newline="\n") as stream:
        for key, value in values.items():
            stream.write(f"{key}={value}\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--artifact-root", type=pathlib.Path, required=True)
    parser.add_argument("--expected-commit", required=True)
    parser.add_argument("--report", type=pathlib.Path, required=True)
    parser.add_argument("--notes", type=pathlib.Path, required=True)
    parser.add_argument("--checksums", type=pathlib.Path, required=True)
    parser.add_argument("--release-files", type=pathlib.Path, required=True)
    parser.add_argument("--github-output", type=pathlib.Path)
    args = parser.parse_args()

    root = args.artifact_root.resolve()
    report_path = args.report.resolve()
    notes_path = args.notes.resolve()
    checksums_path = args.checksums.resolve()
    release_files_path = args.release_files.resolve()
    for path in (report_path, notes_path, checksums_path, release_files_path):
        path.parent.mkdir(parents=True, exist_ok=True)
    result: dict[str, object] = {
        "schema": SCHEMA,
        "status": "failed",
        "artifactRoot": str(root),
        "expectedCommit": args.expected_commit,
    }

    try:
        commit = args.expected_commit.strip()
        if not re.fullmatch(r"[0-9a-f]{40}", commit):
            raise RuntimeError(f"expected commit is invalid: {commit!r}")
        versions: set[str] = set()
        installer_reports: dict[str, pathlib.Path] = {}
        companion_reports: dict[str, pathlib.Path] = {}
        evidence: dict[str, object] = {}

        for platform in PLATFORMS:
            installer_path = exact_file(
                root,
                f"installer-lifecycle-{platform}.json",
            )
            installer = load(installer_path, f"{platform} installer lifecycle")
            versions.add(require_identity(
                installer,
                platform=platform,
                commit=commit,
                label=f"{platform} installer lifecycle",
            ))
            for key in INSTALLER_TRUE:
                require(installer, key, True, f"{platform} installer lifecycle")
            installer_reports[platform] = installer_path

            companion_path = exact_file(
                root,
                f"companion-deployment-{platform}.json",
            )
            companion = load(companion_path, f"{platform} companion deployment")
            versions.add(require_identity(
                companion,
                platform=platform,
                commit=commit,
                label=f"{platform} companion deployment",
            ))
            for key in COMPANION_TRUE:
                require(companion, key, True, f"{platform} companion deployment")
            for key in COMPANION_FALSE:
                require(companion, key, False, f"{platform} companion deployment")
            companion_reports[platform] = companion_path
            evidence[platform] = {
                "installer": str(installer_path),
                "companion": str(companion_path),
            }

        if len(versions) != 1:
            raise RuntimeError(
                f"workflow evidence disagrees on candidate version: {sorted(versions)}"
            )
        version = next(iter(versions))
        safe_version = re.sub(r"[^0-9A-Za-z._-]+", "-", version).strip("-._")
        if not safe_version:
            raise RuntimeError(f"candidate version cannot form a release tag: {version!r}")

        windows_msi = exact_pattern(root, "*.msi", "Windows MSI")
        linux_deb = exact_pattern(root, "*.deb", "Linux DEB")
        windows_image = exact_pattern(
            root,
            "*windows-x64-native-app-image.zip",
            "Windows native app-image archive",
        )
        linux_image = exact_pattern(
            root,
            "*linux-x64-native-app-image.tar.gz",
            "Linux native app-image archive",
        )
        release_files: list[pathlib.Path] = [
            windows_msi,
            linux_deb,
            windows_image,
            linux_image,
            installer_reports["windows-x64"],
            installer_reports["linux-x64"],
            companion_reports["windows-x64"],
            companion_reports["linux-x64"],
        ]
        for platform in PLATFORMS:
            release_files.append(exact_file(
                root,
                f"native-image-verification-{platform}.json",
            ))
            release_files.append(exact_file(
                root,
                f"operations-{platform}.json",
            ))

        tag = f"alpha-{safe_version}-{commit[:12]}"
        title = f"The Mechanist {version} Limited Alpha ({commit[:12]})"
        result.update({
            "status": "verified",
            "version": version,
            "commit": commit,
            "tag": tag,
            "title": title,
            "prerelease": True,
            "platforms": list(PLATFORMS),
            "installerDeploymentVerified": True,
            "companionServerDeploymentVerified": True,
            "windowsMsi": str(windows_msi),
            "linuxDeb": str(linux_deb),
            "windowsNativeImage": str(windows_image),
            "linuxNativeImage": str(linux_image),
            "movementAuthority": False,
            "mapAuthority": False,
            "fullRemoteWorldAuthority": False,
            "evidence": evidence,
        })
        report_path.write_text(
            json.dumps(result, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        release_files.append(report_path)

        checksum_lines = [
            f"{sha256(path)}  {path.name}"
            for path in sorted(release_files, key=lambda item: item.name.lower())
        ]
        checksums_path.write_text("\n".join(checksum_lines) + "\n", encoding="utf-8")
        release_files.append(checksums_path)

        notes_path.write_text(
            f"""# The Mechanist {version} Limited Alpha\n\n"
            f"Exact source commit: `{commit}`\n\n"
            "This prerelease was created automatically only after the Windows and "
            "Linux installer-deployment gates and the Windows and Linux companion-"
            "server gates passed for the same exact commit.\n\n"
            "## Verified deployment boundary\n\n"
            "- Windows MSI and Linux DEB installed through their operating-system "
            "package managers.\n"
            "- Installed native launcher and installed game process remained running.\n"
            "- Bundled package verification and single-player save/resume passed from "
            "the installed payload.\n"
            "- Uninstall removed installed executables while preserving mutable user "
            "data outside the application directory.\n"
            "- A separately launched packaged companion server accepted two packaged "
            "clients on Windows and Linux.\n"
            "- Connected-only roster, readiness broadcast, bounded relay traffic, "
            "wait-only authoritative turn control, disconnect privacy, token-gated "
            "resume, and server-process restart continuity passed.\n\n"
            "## Intentional authority limit\n\n"
            "This alpha does not certify remote movement, map authority, inventory "
            "authority, combat authority, or a fully authoritative remote gameplay "
            "world. Companion-server certification remains limited to the authenticated "
            "lobby, relay, roster, reconnect, and WAIT-control boundary described in "
            "the packaged operating documents.\n\n"
            "Review the attached lifecycle, companion, native-image, operations, and "
            "checksum evidence before installing.\n""",
            encoding="utf-8",
        )
        release_files.append(notes_path)
        release_files_path.write_text(
            "\n".join(str(path.resolve()) for path in release_files) + "\n",
            encoding="utf-8",
        )
        write_output(args.github_output, {
            "tag": tag,
            "title": title,
            "version": version,
            "report": str(report_path),
            "notes": str(notes_path),
            "checksums": str(checksums_path),
            "release_files": str(release_files_path),
        })
        print(f"ALPHA RELEASE CANDIDATE VERIFIED: {report_path}")
        return 0
    except Exception as exc:  # noqa: BLE001 - one prerelease gate surface
        result["errorType"] = type(exc).__name__
        result["error"] = str(exc)
        result["traceback"] = traceback.format_exc()
        report_path.write_text(
            json.dumps(result, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"ALPHA RELEASE CANDIDATE FAILED: {exc}", file=sys.stderr)
        print(f"Report: {report_path}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
