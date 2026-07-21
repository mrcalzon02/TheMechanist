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
REPORT_SCHEMA = 1


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
        "log": str(log_path.relative_to(ROOT)),
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

    report: dict[str, object] = {
        "schema": REPORT_SCHEMA,
        "status": "running",
        "startedAtUtc": utc_now(),
        "platform": platform_name,
        "commit": git_head(),
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
        if manifest.get("platform") != platform_name:
            raise RuntimeError(
                f"distribution platform mismatch: {manifest.get('platform')} != {platform_name}"
            )
        if manifest.get("releaseHardened") is not True:
            raise RuntimeError("native gate requires releaseHardened=true")
        if manifest.get("commit") != report["commit"]:
            raise RuntimeError(
                f"distribution commit {manifest.get('commit')} does not match HEAD {report['commit']}"
            )
        report["distribution"] = str(distribution)
        report["version"] = manifest.get("version")

        steps: list[dict[str, object]] = report["steps"]  # type: ignore[assignment]
        if platform_name == "linux-x64":
            command = [
                bash,
                "scripts/package/build-linux-installers.sh",
                "--distribution", str(distribution),
                "--package-types", "app-image",
                "--output", str(output / "installers"),
            ]
        else:
            command = [
                pwsh,
                "-NoProfile",
                "-File", "scripts/package/build-windows-installers.ps1",
                "-DistributionRoot", str(distribution),
                "-PackageTypes", "app-image",
                "-OutputDir", str(output / "installers"),
            ]
        steps.append(run_step("native-app-image", command, log_dir))

        expected_reports = [
            output / "installers" / f"source-verification-{platform_name}.json",
            output / "installers" / f"staging-{platform_name}.json",
            output / "installers" / f"native-image-verification-{platform_name}.json",
            output / "installers" / "SHA256SUMS.txt",
        ]
        missing = [str(path) for path in expected_reports if not path.is_file()]
        if missing:
            raise RuntimeError(f"native gate did not produce required evidence: {missing}")

        verification = json.loads(expected_reports[2].read_text(encoding="utf-8"))
        if verification.get("status") != "verified":
            raise RuntimeError(
                f"native image verification status is not verified: {verification.get('status')}"
            )
        if verification.get("releaseHardened") is not True:
            raise RuntimeError("native image verification did not retain release hardening")

        report["evidence"] = [str(path.relative_to(ROOT)) for path in expected_reports]
        report["status"] = "passed"
        report["completedAtUtc"] = utc_now()
        report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
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
        report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        print(f"LOCAL NATIVE APP-IMAGE GATE FAILED: {exc}", file=sys.stderr)
        print(f"Report: {report_path}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
