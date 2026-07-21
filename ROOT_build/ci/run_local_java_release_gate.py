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
from collections.abc import Callable
from typing import Sequence

ROOT = pathlib.Path(__file__).resolve().parents[2]
STANDARDS = ROOT / "ROOT_docs" / "STANDARDS_AND_PRACTICES.md"
REPORT_SCHEMA = 3


class GateFailure(RuntimeError):
    def __init__(self, step_result: dict[str, object]):
        command = [str(value) for value in step_result.get("command", [])]
        returncode = int(step_result.get("returnCode", -1))
        step = str(step_result.get("name", "unknown-step"))
        super().__init__(
            f"{step} failed with exit code {returncode}: {' '.join(command)}"
        )
        self.step_result = step_result
        self.step = step
        self.command = command
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
    steps: list[dict[str, object]],
    env: dict[str, str] | None = None,
) -> dict[str, object]:
    log_path = log_dir / f"{name}.log"
    started = utc_now()
    returncode = -1
    launch_error: str | None = None
    try:
        with log_path.open("w", encoding="utf-8", newline="\n") as log:
            try:
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
            except OSError as exc:
                launch_error = str(exc)
                log.write(f"process launch failed: {exc}\n")
                raise
            assert process.stdout is not None
            for line in process.stdout:
                print(line, end="")
                log.write(line)
            returncode = process.wait()
    except OSError:
        returncode = -1

    result: dict[str, object] = {
        "name": name,
        "command": list(command),
        "startedAtUtc": started,
        "completedAtUtc": utc_now(),
        "returnCode": returncode,
        "log": str(log_path.relative_to(ROOT)),
        "passed": returncode == 0,
    }
    if launch_error is not None:
        result["launchError"] = launch_error
    steps.append(result)
    if returncode != 0:
        raise GateFailure(result)
    return result


def run_internal_check(
    name: str,
    check: Callable[[], dict[str, object]],
    steps: list[dict[str, object]],
) -> dict[str, object]:
    started = utc_now()
    try:
        summary = check()
    except Exception as exc:
        result: dict[str, object] = {
            "name": name,
            "command": ["internal-evidence-check"],
            "startedAtUtc": started,
            "completedAtUtc": utc_now(),
            "returnCode": 1,
            "passed": False,
            "error": str(exc),
        }
        steps.append(result)
        raise GateFailure(result) from exc
    result = {
        "name": name,
        "command": ["internal-evidence-check"],
        "startedAtUtc": started,
        "completedAtUtc": utc_now(),
        "returnCode": 0,
        "passed": True,
        "summary": summary,
    }
    steps.append(result)
    return summary


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


def require_clean_worktree() -> dict[str, object]:
    completed = subprocess.run(
        ["git", "status", "--porcelain=v1", "--untracked-files=all"],
        cwd=ROOT,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    entries = [line for line in completed.stdout.splitlines() if line]
    generated = [
        line
        for line in entries
        if line == "?? dist/" or line.startswith("?? dist/")
    ]
    source_entries = [line for line in entries if line not in generated]
    if source_entries:
        preview = "; ".join(source_entries[:12])
        raise RuntimeError(
            "release gate requires a clean source worktree; "
            f"found {len(source_entries)} source entries: {preview}"
        )
    return {
        "status": "clean",
        "sourceEntryCount": 0,
        "ignoredGeneratedEntryCount": len(generated),
        "ignoredGeneratedRoot": "dist/",
    }


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


def load_json_object(path: pathlib.Path, label: str) -> dict[str, object]:
    if not path.is_file():
        raise RuntimeError(f"{label} report is missing: {path}")
    try:
        loaded = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, ValueError, TypeError) as exc:
        raise RuntimeError(f"{label} report is unreadable: {path}: {exc}") from exc
    if not isinstance(loaded, dict):
        raise RuntimeError(f"{label} report must contain one JSON object: {path}")
    return loaded


def require_value(
    source: dict[str, object],
    key: str,
    expected: object,
    label: str,
) -> None:
    actual = source.get(key)
    if actual != expected:
        raise RuntimeError(
            f"{label} {key} mismatch: expected {expected!r}, found {actual!r}"
        )


def verify_runtime_versions(
    verification: dict[str, object],
    expected_version: str,
) -> dict[str, str]:
    require_value(
        verification,
        "versionAuthorityVerified",
        True,
        "distribution verification",
    )
    jars = verification.get("jars")
    if not isinstance(jars, dict):
        raise RuntimeError("distribution verification has no runtime JAR evidence")
    expected_roles = {"launcher", "client", "server"}
    if set(jars) != expected_roles:
        raise RuntimeError(
            "distribution verification runtime roles mismatch: "
            f"expected {sorted(expected_roles)}, found {sorted(jars)}"
        )
    versions: dict[str, str] = {}
    for role in sorted(expected_roles):
        block = jars.get(role)
        if not isinstance(block, dict):
            raise RuntimeError(
                f"distribution verification {role} JAR evidence is malformed"
            )
        actual = block.get("implementationVersion")
        if actual != expected_version:
            raise RuntimeError(
                f"distribution verification {role} implementationVersion mismatch: "
                f"expected {expected_version!r}, found {actual!r}"
            )
        versions[role] = str(actual)
    return versions


def verify_local_evidence_coherence(
    *,
    output: pathlib.Path,
    distribution: pathlib.Path,
    commit: str,
    platform_name: str,
    release_hardened: bool,
) -> dict[str, object]:
    manifest_path = distribution / "manifests" / "runtime-manifest.json"
    manifest = load_json_object(manifest_path, "canonical runtime manifest")
    verification_path = output / "verification.json"
    synthetic_path = output / "synthetic.json"
    contract_path = output / "synthetic-contract.json"
    proguard_path = output / "proguard-policy.json"

    verification = load_json_object(verification_path, "distribution verification")
    synthetic = load_json_object(synthetic_path, "synthetic distribution")
    contract = load_json_object(contract_path, "synthetic release contract")
    proguard = load_json_object(proguard_path, "ProGuard policy")

    for source, label in (
        (manifest, "canonical runtime manifest"),
        (verification, "distribution verification"),
    ):
        require_value(source, "commit", commit, label)
        require_value(source, "platform", platform_name, label)
        require_value(source, "javaRelease", 17, label)
        require_value(source, "releaseHardened", release_hardened, label)

    version = manifest.get("version")
    if not isinstance(version, str) or not version.strip():
        raise RuntimeError(
            f"canonical runtime manifest version is invalid: {version!r}"
        )
    require_value(
        verification,
        "version",
        version,
        "distribution verification",
    )
    runtime_versions = verify_runtime_versions(verification, version)

    require_value(verification, "status", "verified", "distribution verification")
    require_value(synthetic, "status", "passed", "synthetic distribution")
    require_value(
        synthetic,
        "distribution",
        distribution.name,
        "synthetic distribution",
    )
    require_value(
        synthetic,
        "releaseHardened",
        release_hardened,
        "synthetic distribution",
    )
    require_value(
        synthetic,
        "nativeInstallerPayloadStageRequired",
        release_hardened,
        "synthetic distribution",
    )
    require_value(
        synthetic,
        "nativeInstallerPayloadStage",
        release_hardened,
        "synthetic distribution",
    )

    require_value(contract, "status", "verified", "synthetic release contract")
    require_value(
        contract,
        "platforms",
        [platform_name],
        "synthetic release contract",
    )
    require_value(
        contract,
        "releaseHardenedRequired",
        release_hardened,
        "synthetic release contract",
    )
    require_value(
        contract,
        "nativeInstallerStageRequired",
        release_hardened,
        "synthetic release contract",
    )
    require_value(proguard, "status", "verified", "ProGuard policy")
    require_value(
        proguard,
        "mappingArtifactsOutsideDistribution",
        True,
        "ProGuard policy",
    )

    return {
        "status": "verified",
        "commit": commit,
        "platform": platform_name,
        "version": version,
        "javaRelease": 17,
        "releaseHardened": release_hardened,
        "versionAuthorityVerified": True,
        "runtimeArtifactVersions": runtime_versions,
        "distribution": distribution.name,
        "reports": {
            "runtimeManifest": str(manifest_path.relative_to(ROOT)),
            "distributionVerification": str(verification_path.relative_to(ROOT)),
            "syntheticDistribution": str(synthetic_path.relative_to(ROOT)),
            "syntheticReleaseContract": str(contract_path.relative_to(ROOT)),
            "proguardPolicy": str(proguard_path.relative_to(ROOT)),
        },
    }


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
    steps: list[dict[str, object]] = report["steps"]  # type: ignore[assignment]

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
        worktree = require_clean_worktree()
        report["platform"] = platform_name
        report["commit"] = commit
        report["worktree"] = worktree
        report["standardsRead"] = str(STANDARDS.relative_to(ROOT))

        python = sys.executable
        mvn = require_tool("mvn")
        java = require_tool("java")
        require_tool("javac")
        require_tool("git")

        run_step(
            "python-syntax",
            [python, "-m", "compileall", "-q", "ROOT_build/ci"],
            log_dir,
            steps,
        )
        run_step(
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
            steps,
        )
        if platform_name == "linux-x64":
            bash = require_tool("bash")
            run_step(
                "linux-package-script-syntax",
                [bash, "-n", "scripts/package/build-linux-installers.sh"],
                log_dir,
                steps,
            )
        else:
            pwsh = require_tool("pwsh")
            run_step(
                "windows-package-script-syntax",
                [
                    pwsh,
                    "-NoProfile",
                    "-Command",
                    "$t=$null;$e=$null;"
                    "[System.Management.Automation.Language.Parser]::ParseFile("
                    "(Resolve-Path 'scripts/package/build-windows-installers.ps1'),"
                    "[ref]$t,[ref]$e)|Out-Null;"
                    "if($e.Count -gt 0){$e|%{Write-Error $_.Message};exit 1}",
                ],
                log_dir,
                steps,
            )

        run_step(
            "maven-java17-package",
            [mvn, "-B", "-DskipTests", "clean", "package"],
            log_dir,
            steps,
        )
        run_step(
            "boot-smoke",
            [
                java,
                "-Djava.awt.headless=true",
                "-cp",
                "target/TheMechanist-all.jar",
                "mechanist.BootStartupAudioSilenceSmoke",
            ],
            log_dir,
            steps,
        )

        server_home = output / "server-home"
        server_home.mkdir(parents=True, exist_ok=True)
        server_env = os.environ.copy()
        server_env["MECHANIST_SERVER_HOME"] = str(server_home)
        run_step(
            "server-operation-smoke",
            [
                java,
                f"-Duser.home={server_home}",
                "-jar",
                "target/TheMechanistServer-all.jar",
                "--help",
            ],
            log_dir,
            steps,
            env=server_env,
        )

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
        run_step(
            "build-portable-distribution",
            build_command,
            log_dir,
            steps,
        )

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
        run_step(
            "verify-portable-distribution",
            verify_command,
            log_dir,
            steps,
        )

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
        run_step(
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
            steps,
        )

        run_step(
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
            steps,
        )

        contract_command = [
            python,
            "ROOT_build/ci/verify_synthetic_release_contract.py",
            str(synthetic_report),
            "--expected-platforms",
            platform_name,
            "--report",
            str(contract_report),
        ]
        if args.release_hardened:
            contract_command.extend(
                ["--require-release-hardened", "--require-native-stage"]
            )
        run_step(
            "synthetic-release-contract",
            contract_command,
            log_dir,
            steps,
        )

        evidence_summary = run_internal_check(
            "evidence-coherence",
            lambda: verify_local_evidence_coherence(
                output=output,
                distribution=distribution,
                commit=commit,
                platform_name=platform_name,
                release_hardened=args.release_hardened,
            ),
            steps,
        )
        report["evidenceCoherence"] = evidence_summary

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
        report["stepCount"] = len(steps)
        report["errorType"] = type(exc).__name__
        report["error"] = str(exc)
        if isinstance(exc, GateFailure):
            report["failedStep"] = exc.step
            report["failedCommand"] = exc.command
            report["returnCode"] = exc.returncode
            report["failedStepResult"] = exc.step_result
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
