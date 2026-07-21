#!/usr/bin/env python3
"""Generate and audit the governed repository inventory outside GitHub Actions.

This command is the local equivalent of the Repository Inventory Alpha Gate. It
never commits or pushes. Updating the committed manifest requires the explicit
--update-committed-manifest option. Release clearance remains fail-closed when
--require-release-clearance is supplied.
"""

from __future__ import annotations

import argparse
import datetime as dt
import difflib
import json
import pathlib
import shutil
import subprocess
import sys
import traceback
from typing import Sequence

ROOT = pathlib.Path(__file__).resolve().parents[2]
MANIFEST = ROOT / "ROOT_docs" / "REPOSITORY_FILE_MANIFEST.tsv"
CLEARANCE = ROOT / "ROOT_docs" / "RELEASE_CLEARANCE.tsv"
SCHEMA = 1


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat().replace("+00:00", "Z")


def run(command: Sequence[str], log_path: pathlib.Path) -> None:
    log_path.parent.mkdir(parents=True, exist_ok=True)
    with log_path.open("w", encoding="utf-8", newline="\n") as log:
        process = subprocess.Popen(
            list(command),
            cwd=ROOT,
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
        code = process.wait()
    if code != 0:
        raise RuntimeError(f"command failed with exit code {code}: {' '.join(command)}")


def git_head() -> str:
    result = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=ROOT,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    value = result.stdout.strip()
    if len(value) != 40:
        raise RuntimeError(f"invalid Git HEAD identity: {value!r}")
    return value


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--update-committed-manifest",
        action="store_true",
        help="Replace ROOT_docs/REPOSITORY_FILE_MANIFEST.tsv with the generated manifest.",
    )
    parser.add_argument(
        "--require-release-clearance",
        action="store_true",
        help="Fail unless every release-relevant entry has exact clearance.",
    )
    parser.add_argument(
        "--output",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-inventory-gate",
    )
    parser.add_argument(
        "--report",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-inventory-gate-report.json",
    )
    args = parser.parse_args()

    output = args.output.resolve()
    report_path = args.report.resolve()
    shutil.rmtree(output, ignore_errors=True)
    output.mkdir(parents=True, exist_ok=True)
    report_path.parent.mkdir(parents=True, exist_ok=True)

    report: dict[str, object] = {
        "schema": SCHEMA,
        "status": "running",
        "startedAtUtc": utc_now(),
        "updateCommittedManifest": args.update_committed_manifest,
        "requireReleaseClearance": args.require_release_clearance,
    }

    try:
        if not MANIFEST.is_file():
            raise RuntimeError(f"missing governed manifest: {MANIFEST}")
        if not CLEARANCE.is_file():
            raise RuntimeError(f"missing clearance registry: {CLEARANCE}")

        commit = git_head()
        report["commit"] = commit
        committed = output / "committed.tsv"
        generated = output / "generated.tsv"
        shutil.copy2(MANIFEST, committed)
        shutil.copy2(MANIFEST, generated)

        run(
            [
                sys.executable,
                "ROOT_tools/update_repository_file_manifest_incremental.py",
                "--target",
                str(generated),
                "--force-hash",
            ],
            output / "generation.log",
        )

        committed_text = committed.read_text(encoding="utf-8")
        generated_text = generated.read_text(encoding="utf-8")
        changed = committed_text != generated_text
        report["manifestChanged"] = changed
        report["generatedManifest"] = str(generated.relative_to(ROOT))

        diff_path = output / "manifest-diff.txt"
        diff = "".join(
            difflib.unified_diff(
                committed_text.splitlines(keepends=True),
                generated_text.splitlines(keepends=True),
                fromfile="committed.tsv",
                tofile="generated.tsv",
            )
        )
        diff_path.write_text(diff, encoding="utf-8")

        audit_command = [
            sys.executable,
            "ROOT_build/ci/audit_repository_release_inventory.py",
            str(generated),
            "--clearance-registry",
            str(CLEARANCE),
            "--ledger",
            str(output / "release-ownership-ledger.tsv"),
            "--pending",
            str(output / "RELEASE_CLEARANCE_PENDING.tsv"),
            "--report",
            str(output / "release-inventory-report.json"),
        ]
        if args.require_release_clearance:
            audit_command.append("--require-clearance")
        run(audit_command, output / "audit.log")

        audit = json.loads((output / "release-inventory-report.json").read_text(encoding="utf-8"))
        report["auditStatus"] = audit.get("status")
        for key in (
            "entryCount",
            "releaseRelevantCount",
            "clearancePendingCount",
            "blockerCount",
            "staleRegistryCount",
            "protectedRuntimeDuplicateCount",
        ):
            if key in audit:
                report[key] = audit[key]

        if args.update_committed_manifest:
            shutil.copy2(generated, MANIFEST)
            report["committedManifestUpdated"] = True
        else:
            report["committedManifestUpdated"] = False
            if changed:
                raise RuntimeError(
                    "committed repository inventory is stale; review manifest-diff.txt and rerun with --update-committed-manifest"
                )

        report["status"] = "passed"
        report["completedAtUtc"] = utc_now()
        report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        print(f"LOCAL INVENTORY GATE PASSED: {report_path}")
        return 0
    except Exception as exc:  # noqa: BLE001 - single fail-closed diagnostic surface
        report["status"] = "failed"
        report["completedAtUtc"] = utc_now()
        report["errorType"] = type(exc).__name__
        report["error"] = str(exc)
        report["traceback"] = traceback.format_exc()
        report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        print(f"LOCAL INVENTORY GATE FAILED: {exc}", file=sys.stderr)
        print(f"Report: {report_path}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
