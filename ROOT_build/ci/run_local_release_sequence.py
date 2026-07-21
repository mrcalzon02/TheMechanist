#!/usr/bin/env python3
"""Run the local release gates in one authoritative sequence.

This command coordinates the existing playtest-operations, Java, inventory,
native, and evidence verifiers without duplicating their implementation. It
stops at the first failed stage, preserves every stage report, and writes one
top-level summary. It never commits, pushes, publishes, or updates release
history.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import pathlib
import subprocess
import sys
import traceback
from typing import Sequence

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCHEMA = 3


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat().replace("+00:00", "Z")


def run_stage(name: str, command: Sequence[str], log_dir: pathlib.Path) -> dict[str, object]:
    log_path = log_dir / f"{name}.log"
    result: dict[str, object] = {
        "name": name,
        "command": list(command),
        "startedAtUtc": utc_now(),
        "log": str(log_path.relative_to(ROOT)),
        "passed": False,
    }
    return_code = -1
    launch_error: str | None = None
    with log_path.open("w", encoding="utf-8", newline="\n") as log:
        try:
            process = subprocess.Popen(
                list(command),
                cwd=ROOT,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                encoding="utf-8",
                errors="replace",
            )
        except OSError as exc:
            launch_error = str(exc)
            log.write(f"process launch failed: {exc}\n")
        else:
            assert process.stdout is not None
            for line in process.stdout:
                print(line, end="")
                log.write(line)
            return_code = process.wait()
    result["completedAtUtc"] = utc_now()
    result["returnCode"] = return_code
    result["passed"] = return_code == 0
    if launch_error is not None:
        result["launchError"] = launch_error
    return result


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


def write_report(path: pathlib.Path, report: dict[str, object]) -> None:
    path.write_text(
        json.dumps(report, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--require-clearance",
        action="store_true",
        help="Require exact release clearance in the inventory and final evidence stages.",
    )
    parser.add_argument(
        "--update-committed-manifest",
        action="store_true",
        help=(
            "Allow the inventory stage to replace the governed manifest, then stop "
            "for mandatory review and commit before native or final evidence stages."
        ),
    )
    parser.add_argument(
        "--report",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-release-sequence-report.json",
    )
    args = parser.parse_args()

    report_path = args.report.resolve()
    sequence_dir = ROOT / "dist" / "local-release-sequence"
    log_dir = sequence_dir / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)
    report_path.parent.mkdir(parents=True, exist_ok=True)

    initial_commit = git_head()
    report: dict[str, object] = {
        "schema": SCHEMA,
        "status": "running",
        "startedAtUtc": utc_now(),
        "commit": initial_commit,
        "requireClearance": args.require_clearance,
        "updateCommittedManifest": args.update_committed_manifest,
        "stages": [],
    }
    stages: list[dict[str, object]] = report["stages"]  # type: ignore[assignment]

    try:
        python = sys.executable
        commands: list[tuple[str, list[str]]] = [
            (
                "playtest-operations",
                [
                    python,
                    "ROOT_build/ci/verify_limited_alpha_operations.py",
                    "--repo",
                    ".",
                    "--report",
                    str(sequence_dir / "playtest-operations.json"),
                ],
            ),
            (
                "java",
                [python, "ROOT_build/ci/run_local_java_release_gate.py", "--release-hardened"],
            ),
            (
                "inventory",
                [python, "ROOT_build/ci/run_local_inventory_gate.py"],
            ),
            (
                "native",
                [python, "ROOT_build/ci/run_local_native_gate.py"],
            ),
        ]
        if args.update_committed_manifest:
            commands[2][1].append("--update-committed-manifest")
        if args.require_clearance:
            commands[2][1].append("--require-release-clearance")

        evidence_command = [python, "ROOT_build/ci/verify_local_release_evidence.py"]
        if args.require_clearance:
            evidence_command.append("--require-clearance")
        commands.append(("evidence", evidence_command))

        for name, command in commands:
            result = run_stage(name, command, log_dir)
            stages.append(result)
            if not result["passed"]:
                report["failedStage"] = name
                report["failedStageResult"] = result
                raise RuntimeError(f"local release sequence stopped at {name}")

            current_commit = git_head()
            if current_commit != initial_commit:
                raise RuntimeError(
                    f"repository HEAD changed during {name}: "
                    f"{initial_commit} -> {current_commit}"
                )

            if name == "inventory" and args.update_committed_manifest:
                report["status"] = "review-required"
                report["completedAtUtc"] = utc_now()
                report["stoppedAfterStage"] = "inventory"
                report["manifestCommitRequired"] = True
                report["nextAction"] = (
                    "Review dist/local-inventory-gate/manifest-diff.txt, commit "
                    "ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv directly to main, then "
                    "rerun the sequence without --update-committed-manifest."
                )
                write_report(report_path, report)
                print(
                    "LOCAL RELEASE SEQUENCE STOPPED FOR MANIFEST REVIEW: "
                    f"{report_path}",
                    file=sys.stderr,
                )
                return 2
        report["status"] = "passed"
        report["completedAtUtc"] = utc_now()
        write_report(report_path, report)
        print(f"LOCAL RELEASE SEQUENCE PASSED: {report_path}")
        return 0
    except Exception as exc:  # noqa: BLE001
        report["status"] = "failed"
        report["completedAtUtc"] = utc_now()
        report["errorType"] = type(exc).__name__
        report["error"] = str(exc)
        report["traceback"] = traceback.format_exc()
        write_report(report_path, report)
        print(f"LOCAL RELEASE SEQUENCE FAILED: {exc}", file=sys.stderr)
        print(f"Report: {report_path}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
