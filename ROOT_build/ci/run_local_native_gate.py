#!/usr/bin/env python3
"""Run the target-platform native app-image gate outside GitHub Actions.

This gate consumes an already verified release-hardened canonical distribution,
invokes the authoritative platform packaging script for app-image only, and
records the first concrete native failure with its log and report metadata.
It does not publish, install, update release history, or claim installer
certification for DEB, RPM, EXE, or MSI packages.
"""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
import pathlib
import platform as host_platform
import shutil
import subprocess
import sys
import traceback
from typing import Sequence

ROOT = pathlib.Path(__file__).resolve().parents[2]
REPORT_SCHEMA = 2


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat().replace("+00:00", "Z")


def detect_platform() -> str:
    machine = host_platform.machine().lower()
    if machine not in {"x86_64", "amd64"}:
        raise RuntimeError(f"unsupported local architecture: {machine}")
    if os.name == "nt":
        return "windows-x64"
    if sys.platform.startswith("linux"):
        return "linux-x64"
    raise RuntimeError(f"unsupported local operating system: {sys.platform}")


def git_head() -> str:
    completed = subprocess.run(
        ["git", "rev-parse", "HEAD"], cwd=ROOT, check=True, text=True,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )
    value = completed.stdout.strip()
    if len(value) != 40:
        raise RuntimeError(f"invalid Git HEAD identity: {value!r}")
    return value


def require_tool(name: str) -> str:
    path = shutil.which(name)
    if not path:
        raise RuntimeError(f"required tool is not available on PATH: {name}")
    return path


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def find_distribution(search_root: pathlib.Path, platform_name: str) -> pathlib.Path:
    matches = sorted(
        item for item in search_root.glob(f"TheMechanist-*-{platform_name}")
        if item.is_dir()
    )
    if len(matches) != 1:
        raise RuntimeError(
            f"expected exactly one {platform_name} canonical distribution under "
            f"{search_root}; found {matches}"
        )
    return matches[0].resolve()


def run_step(name: str, command: Sequence[str], log_dir: pathlib.Path) -> dict[str, object]:
    log_path = log_dir / f"{name}.log"
    started = utc_now()
    result: dict[str, object] = {
        "name": name,
        "command": list(command),
        "startedAtUtc": started,
        "log": str(log_path),
        "passed": False,
    }
    try:
        with log_path.open("w", encoding="utf-8", newline="\n") as log:
            process = subprocess.Popen(
                list(command), cwd=ROOT, stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT, text=True, encoding="utf-8",
                errors="replace",
            )
            assert process.stdout is not None
            for line in process.stdout:
                print(line, end="")
                log.write(line)
            returncode = process.wait()
        result["returnCode"] = returncode
        result["completedAtUtc"] = utc_now()
        result["passed"] = returncode == 0
        if returncode != 0:
            raise RuntimeError(f"{name} failed with exit code {returncode}")
        return result
    except Exception:
        result["completedAtUtc"] = utc_now()
        result.setdefault("returnCode", None)
        raise NativeStepFailure(result) from None


class NativeStepFailure(RuntimeError):
    def __init__(self, step: dict[str, object]):
        super().__init__(f"native gate step failed: {step['name']}")
        self.step = step


def require_identity(
    record: dict[str, object],
    label: str,
    *,
    platform_name: str,
    commit: str,
    version: object,
    require_status: bool,
) -> None:
    if require_status and record.get("status") != "verified":
        raise RuntimeError(
            f"{label} status is not verified: {record.get('status')!r}"
        )
    if record.get("platform") != platform_name:
        raise RuntimeError(
            f"{label} platform mismatch: {record.get('platform')!r} != {platform_name!r}"
        )
    if record.get("commit") != commit:
        raise RuntimeError(
            f"{label} commit mismatch: {record.get('commit')!r} != {commit!r}"
        )
    if record.get("version") != version:
        raise RuntimeError(
            f"{label} version mismatch: {record.get('version')!r} != {version!r}"
        )
    if record.get("releaseHardened") is not True:
        raise RuntimeError(f"{label} did not retain releaseHardened=true")


def verify_checksum_ledger(
    ledger_path: pathlib.Path,
    installer_root: pathlib.Path,
    required_archive: pathlib.Path,
) -> dict[str, object]:
    installer_root = installer_root.resolve()
    required_archive = required_archive.resolve()
    entries: list[dict[str, object]] = []
    covered: set[pathlib.Path] = set()

    for line_number, raw in enumerate(
        ledger_path.read_text(encoding="utf-8").splitlines(), start=1
    ):
        line = raw.strip()
        if not line:
            continue
        parts = line.split(None, 1)
        if len(parts) != 2 or len(parts[0]) != 64:
            raise RuntimeError(
                f"invalid SHA256SUMS entry at line {line_number}: {raw!r}"
            )
        expected = parts[0].lower()
        declared = parts[1].strip()
        candidate = pathlib.Path(declared)
        path = candidate.resolve() if candidate.is_absolute() else (installer_root / candidate).resolve()
        try:
            relative = path.relative_to(installer_root)
        except ValueError as exc:
            raise RuntimeError(
                f"SHA256SUMS path escapes installer output at line {line_number}: {declared!r}"
            ) from exc
        if path == ledger_path.resolve():
            raise RuntimeError("SHA256SUMS.txt must not hash itself")
        if not path.is_file():
            raise RuntimeError(
                f"SHA256SUMS entry is missing at line {line_number}: {path}"
            )
        actual = sha256(path)
        if actual != expected:
            raise RuntimeError(
                f"SHA256 mismatch for {relative.as_posix()}: {actual} != {expected}"
            )
        covered.add(path)
        entries.append({
            "path": relative.as_posix(),
            "sha256": actual,
            "size": path.stat().st_size,
        })

    if not entries:
        raise RuntimeError("SHA256SUMS.txt contains no verified entries")
    if required_archive not in covered:
        raise RuntimeError(
            f"portable native app-image archive is not covered by SHA256SUMS.txt: {required_archive}"
        )
    return {
        "entryCount": len(entries),
        "portableArchive": str(required_archive),
        "portableArchiveSha256": sha256(required_archive),
        "entries": entries,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--distribution",
        type=pathlib.Path,
        help="Existing verified release-hardened canonical distribution.",
    )
    parser.add_argument(
        "--java-gate-output",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-java-gate" / "releases",
        help="Search root used when --distribution is omitted.",
    )
    parser.add_argument(
        "--output",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-native-gate",
    )
    parser.add_argument(
        "--report",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-native-gate-report.json",
    )
    args = parser.parse_args()

    platform_name = detect_platform()
    output = args.output.resolve()
    report_path = args.report.resolve()
    log_dir = output / "logs"
    shutil.rmtree(output, ignore_errors=True)
    log_dir.mkdir(parents=True, exist_ok=True)
    report_path.parent.mkdir(parents=True, exist_ok=True)

    commit = git_head()
    report: dict[str, object] = {
        "schema": REPORT_SCHEMA,
        "status": "running",
        "startedAtUtc": utc_now(),
        "platform": platform_name,
        "commit": commit,
        "packageTypes": ["app-image"],
        "installerCertificationClaimed": False,
        "steps": [],
    }

    try:
        require_tool("git")
        require_tool("jpackage")
        if platform_name == "linux-x64":
            bash = require_tool("bash")
            require_tool("python3")
        else:
            pwsh = require_tool("pwsh")
            require_tool("python")

        distribution = (
            args.distribution.resolve()
            if args.distribution
            else find_distribution(args.java_gate_output.resolve(), platform_name)
        )
        manifest_path = distribution / "manifests" / "runtime-manifest.json"
        if not manifest_path.is_file():
            raise RuntimeError(f"runtime manifest is missing: {manifest_path}")
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        version = manifest.get("version")
        if manifest.get("platform") != platform_name:
            raise RuntimeError(
                f"distribution platform mismatch: {manifest.get('platform')} != {platform_name}"
            )
        if manifest.get("releaseHardened") is not True:
            raise RuntimeError("native gate requires releaseHardened=true")
        if manifest.get("commit") != commit:
            raise RuntimeError(
                f"distribution commit {manifest.get('commit')} does not match HEAD {commit}"
            )
        if not version:
            raise RuntimeError("canonical distribution manifest has no version")
        report["distribution"] = str(distribution)
        report["version"] = version

        steps: list[dict[str, object]] = report["steps"]  # type: ignore[assignment]
        installer_root = output / "installers"
        if platform_name == "linux-x64":
            command = [
                bash,
                "scripts/package/build-linux-installers.sh",
                "--distribution", str(distribution),
                "--package-types", "app-image",
                "--output", str(installer_root),
            ]
        else:
            command = [
                pwsh,
                "-NoProfile",
                "-File", "scripts/package/build-windows-installers.ps1",
                "-DistributionRoot", str(distribution),
                "-PackageTypes", "app-image",
                "-OutputDir", str(installer_root),
            ]
        steps.append(run_step("native-app-image", command, log_dir))

        source_path = installer_root / f"source-verification-{platform_name}.json"
        staging_path = installer_root / f"staging-{platform_name}.json"
        image_path = installer_root / f"native-image-verification-{platform_name}.json"
        ledger_path = installer_root / "SHA256SUMS.txt"
        archive_suffix = "tar.gz" if platform_name == "linux-x64" else "zip"
        archive_matches = sorted(
            installer_root.glob(
                f"TheMechanist-*-{platform_name}-native-app-image.{archive_suffix}"
            )
        )
        if len(archive_matches) != 1:
            raise RuntimeError(
                "expected exactly one portable native app-image archive; "
                f"found {archive_matches}"
            )
        archive_path = archive_matches[0]

        required = [source_path, staging_path, image_path, ledger_path, archive_path]
        missing = [str(path) for path in required if not path.is_file()]
        if missing:
            raise RuntimeError(f"native gate did not produce required evidence: {missing}")

        source = json.loads(source_path.read_text(encoding="utf-8"))
        staging = json.loads(staging_path.read_text(encoding="utf-8"))
        image = json.loads(image_path.read_text(encoding="utf-8"))
        require_identity(
            source,
            "source distribution verification",
            platform_name=platform_name,
            commit=commit,
            version=version,
            require_status=True,
        )
        require_identity(
            staging,
            "native staging report",
            platform_name=platform_name,
            commit=commit,
            version=version,
            require_status=False,
        )
        require_identity(
            image,
            "native image verification",
            platform_name=platform_name,
            commit=commit,
            version=version,
            require_status=True,
        )
        if image.get("remoteLobbyEntryPoint") != "mechanist.RemoteClientMain":
            raise RuntimeError("native image verification remote-lobby entry point is invalid")
        if image.get("remoteLobbyNativeExecutable") is not True:
            raise RuntimeError("native image verification did not prove the remote-lobby executable")
        if image.get("mutableStorageIncluded") is not False:
            raise RuntimeError("native image verification detected mutable storage in the payload")

        checksum_summary = verify_checksum_ledger(
            ledger_path,
            installer_root,
            archive_path,
        )
        report["sourceVerification"] = source
        report["stagingVerification"] = staging
        report["imageVerification"] = image
        report["checksumVerification"] = checksum_summary
        report["evidence"] = [str(path) for path in required]
        report["status"] = "passed"
        report["completedAtUtc"] = utc_now()
        report_path.write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"LOCAL NATIVE APP-IMAGE GATE PASSED: {report_path}")
        return 0
    except Exception as exc:  # noqa: BLE001
        report["status"] = "failed"
        report["completedAtUtc"] = utc_now()
        report["errorType"] = type(exc).__name__
        report["error"] = str(exc)
        if isinstance(exc, NativeStepFailure):
            report["failedStep"] = exc.step
            steps = report["steps"]  # type: ignore[assignment]
            steps.append(exc.step)
        report["traceback"] = traceback.format_exc()
        report_path.write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"LOCAL NATIVE APP-IMAGE GATE FAILED: {exc}", file=sys.stderr)
        print(f"Report: {report_path}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
