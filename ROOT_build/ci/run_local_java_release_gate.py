#!/usr/bin/env python3
"""Run the portable Java 17 release gate outside GitHub Actions.

This is a deterministic fallback for repositories where Actions execution or
status visibility is unavailable. It follows the same source-of-truth build,
package, verification, Gate 3, and synthetic-contract sequence as the primary
Java workflow. It does not publish a release or mutate development history.
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
STANDARDS = ROOT / "ROOT_docs" / "STANDARDS_AND_PRACTICES.md"
REPORT_SCHEMA = 1


class GateFailure(RuntimeError):
    def __init__(self, step: str, command: Sequence[str], returncode: int):
        super().__init__(
            f"{step} failed with exit code {returncode}: {' '.join(command)}"
        )
        self.step = step
        self.command = list(command)
        self.returncode = returncode


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


def run_step(
    name: str,
    command: Sequence[str],
    log_dir: pathlib.Path,
    env: dict[str, str] | None = None,
) -> dict[str, object]:
    log_path = log_dir / f"{name}.log"
    started = utc_now()
    with log_path.open("w", encoding="utf-8", newline="\n") as log:
        process = subprocess.Popen(
            list(command),
            cwd=ROOT,
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
        )
        assert process.stdout is not None
        for line in process.stdout:
            print(line, end="")
            log.write(line)
        returncode = process.wait()
    result = {
        "name": name,
        "command": list(command),
        "startedAtUtc": started,
        "completedAtUtc": utc_now(),
        "returnCode": returncode,
        "log": str(log_path.relative_to(ROOT)),
        "passed": returncode == 0,
    }
    if returncode != 0:
        raise GateFailure(name, command, returncode)
    return result


def require_tool(name: str) -> str:
    path = shutil.which(name)
    if not path:
        raise RuntimeError(f"required tool is not available on PATH: {name}")
    return path


def git_head() -> str:
    completed = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=ROOT,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    value = completed.stdout.strip()
    if len(value) != 40:
        raise RuntimeError(f"invalid Git HEAD identity: {value!r}")
    return value


def find_one(root: pathlib.Path, pattern: str, kind: str) -> pathlib.Path:
    matches = sorted(root.glob(pattern))
    matches = [
        item
        for item in matches
        if (item.is_dir() if kind == "directory" else item.is_file())
    ]
    if len(matches) != 1:
        raise RuntimeError(
            f"expected exactly one {kind} matching {pattern!r}; found {matches}"
        )
    return matches[0]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--release-hardened",
        action="store_true",
        help="Build and require the obfuscated release-hardened distribution.",
    )
    parser.add_argument(
        "--output",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-java-gate",
    )
    parser.add_argument(
        "--report",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-java-gate-report.json",
    )
    args = parser.parse_args()

    output = args.output.resolve()
    report_path = args.report.resolve()
    log_dir = output / "logs"
    release_dir = output / "releases"
    synthetic_report = output / "synthetic.json"
    contract_report = output / "synthetic-contract.json"
    shutil.rmtree(output, ignore_errors=True)
    log_dir.mkdir(parents=True, exist_ok=True)
    release_dir.mkdir(parents=True, exist_ok=True)
    report_path.parent.mkdir(parents=True, exist_ok=True)

    report: dict[str, object] = {
        "schema": REPORT_SCHEMA,
        "status": "running",
        "startedAtUtc": utc_now(),
        "repository": str(ROOT),
        "releaseHardened": args.release_hardened,
        "steps": [],
    }

    try:
        if not STANDARDS.is_file():
            raise RuntimeError(f"required standards document is missing: {STANDARDS}")
        standards_text = STANDARDS.read_text(encoding="utf-8")
        if "Java 17" not in standards_text and "--release 17" not in standards_text:
            raise RuntimeError(
                "standards document does not contain the governed Java 17 requirement"
            )

        platform_name = detect_platform()
        commit = git_head()
        report["platform"] = platform_name
        report["commit"] = commit
        report["standardsRead"] = str(STANDARDS.relative_to(ROOT))

        python = sys.executable
        mvn = require_tool("mvn")
        java = require_tool("java")
        require_tool("javac")
        require_tool("git")

        steps: list[dict[str, object]] = report["steps"]  # type: ignore[assignment]
        steps.append(run_step(
            "python-syntax",
            [python, "-m", "compileall", "-q", "ROOT_build/ci"],
            log_dir,
        ))
        steps.append(run_step(
            "proguard-policy",
            [
                python,
                "ROOT_build/ci/verify_proguard_configuration.py",
                "--repo",
                ".",
                "--report",
                str(output / "proguard-policy.json"),
            ],
            log_dir,
        ))
        if platform_name == "linux-x64":
            bash = require_tool("bash")
            steps.append(run_step(
                "linux-package-script-syntax",
                [bash, "-n", "scripts/package/build-linux-installers.sh"],
                log_dir,
            ))
        elif shutil.which("pwsh"):
            steps.append(run_step(
                "windows-package-script-syntax",
                [
                    "pwsh",
                    "-NoProfile",
                    "-Command",
                    "$t=$null;$e=$null;"
                    "[System.Management.Automation.Language.Parser]::ParseFile("
                    "(Resolve-Path 'scripts/package/build-windows-installers.ps1'),"
                    "[ref]$t,[ref]$e)|Out-Null;"
                    "if($e.Count -gt 0){$e|%{Write-Error $_.Message};exit 1}",
                ],
                log_dir,
            ))

        steps.append(run_step(
            "maven-java17-package",
            [mvn, "-B", "-DskipTests", "clean", "package"],
            log_dir,
        ))
        steps.append(run_step(
            "boot-smoke",
            [
                java,
                "-Djava.awt.headless=true",
                "-cp",
                "target/TheMechanist-all.jar",
                "mechanist.BootStartupAudioSilenceSmoke",
            ],
            log_dir,
        ))

        server_home = output / "server-home"
        server_home.mkdir(parents=True, exist_ok=True)
        server_env = os.environ.copy()
        server_env["MECHANIST_SERVER_HOME"] = str(server_home)
        steps.append(run_step(
            "server-operation-smoke",
            [
                java,
                f"-Duser.home={server_home}",
                "-jar",
                "target/TheMechanistServer-all.jar",
                "--help",
            ],
            log_dir,
            env=server_env,
        ))

        build_command = [
            python,
            "ROOT_build/ci/build_runnable_distribution.py",
            "--repo",
            ".",
            "--commit",
            commit,
            "--output",
            str(release_dir),
        ]
        if args.release_hardened:
            build_command.append("--release-hardened")
        steps.append(run_step(
            "build-portable-distribution",
            build_command,
            log_dir,
        ))

        distribution = find_one(
            release_dir,
            f"TheMechanist-*-{platform_name}",
            "directory",
        )
        archive = find_one(
            release_dir,
            f"TheMechanist-*-{platform_name}.zip",
            "file",
        )
        report["distribution"] = str(distribution)
        report["archive"] = str(archive)

        verify_command = [
            python,
            "ROOT_build/ci/verify_runnable_distribution.py",
            str(distribution),
            "--archive",
            str(archive),
            "--report",
            str(output / "verification.json"),
        ]
        if args.release_hardened:
            verify_command.append("--require-release-hardened")
        steps.append(run_step(
            "verify-portable-distribution",
            verify_command,
            log_dir,
        ))

        runtime_java = distribution / "runtime" / "bin" / (
            "java.exe" if platform_name == "windows-x64" else "java"
        )
        classpath_separator = ";" if platform_name == "windows-x64" else ":"
        packaged_classpath = classpath_separator.join([
            str(distribution / "packages" / "client" / "TheMechanist.jar"),
            str(distribution / "packages" / "support" / "lib" / "*"),
        ])
        gate_profile = output / "gate3-profile"
        gate_profile.mkdir(parents=True, exist_ok=True)
        steps.append(run_step(
            "packaged-gate3",
            [
                str(runtime_java),
                f"-Duser.home={gate_profile}",
                "-Djava.awt.headless=true",
                "-cp",
                packaged_classpath,
                "mechanist.Gate3PlayerFacingTextSmokeSuite",
            ],
            log_dir,
        ))

        steps.append(run_step(
            "synthetic-distribution",
            [
                python,
                "ROOT_build/ci/run_synthetic_distribution_tests.py",
                str(distribution),
                "--verifier",
                "ROOT_build/ci/verify_runnable_distribution.py",
                "--report",
                str(synthetic_report),
            ],
            log_dir,
        ))

        contract_command = [
            python,
            "ROOT_build/ci/verify_synthetic_release_contract.py",
            str(synthetic_report),
            "--expected-platforms",
            platform_name,
            "--require-native-stage",
            "--report",
            str(contract_report),
        ]
        if args.release_hardened:
            contract_command.append("--require-release-hardened")
        steps.append(run_step(
            "synthetic-release-contract",
            contract_command,
            log_dir,
        ))

        report["status"] = "passed"
        report["completedAtUtc"] = utc_now()
        report["stepCount"] = len(steps)
        report_path.write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"LOCAL JAVA RELEASE GATE PASSED: {report_path}")
        return 0
    except Exception as exc:  # noqa: BLE001 - one fail-closed gate surface
        report["status"] = "failed"
        report["completedAtUtc"] = utc_now()
        report["errorType"] = type(exc).__name__
        report["error"] = str(exc)
        if isinstance(exc, GateFailure):
            report["failedStep"] = exc.step
            report["failedCommand"] = exc.command
            report["returnCode"] = exc.returncode
        report["traceback"] = traceback.format_exc()
        report_path.write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"LOCAL JAVA RELEASE GATE FAILED: {exc}", file=sys.stderr)
        print(f"Report: {report_path}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
