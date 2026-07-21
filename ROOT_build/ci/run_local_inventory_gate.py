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
import hashlib
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
SCHEMA = 2
EXPECTED_AUDIT_STATUSES = {"verified", "review-required"}


def utc_now() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat().replace("+00:00", "Z")


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def run(command: Sequence[str], log_path: pathlib.Path) -> dict[str, object]:
    log_path.parent.mkdir(parents=True, exist_ok=True)
    started = utc_now()
    result: dict[str, object] = {
        "command": list(command),
        "startedAtUtc": started,
        "log": str(log_path.relative_to(ROOT)),
        "passed": False,
    }
    try:
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
        result["returnCode"] = code
        result["completedAtUtc"] = utc_now()
        result["passed"] = code == 0
        if code != 0:
            raise RuntimeError(
                f"command failed with exit code {code}: {' '.join(command)}"
            )
        return result
    except Exception:
        result["completedAtUtc"] = utc_now()
        result.setdefault("returnCode", None)
        raise InventoryStepFailure(result) from None


class InventoryStepFailure(RuntimeError):
    def __init__(self, step: dict[str, object]):
        super().__init__("inventory gate command failed")
        self.step = step


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


def require_nonempty(path: pathlib.Path, label: str) -> None:
    if not path.is_file() or path.stat().st_size == 0:
        raise RuntimeError(f"{label} is missing or empty: {path}")


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
        "steps": [],
    }

    try:
        require_nonempty(CLEARANCE, "clearance registry")
        if not MANIFEST.is_file():
            raise RuntimeError(f"missing governed manifest: {MANIFEST}")

        commit = git_head()
        report["commit"] = commit
        committed = output / "committed.tsv"
        generated = output / "generated.tsv"
        shutil.copy2(MANIFEST, committed)
        shutil.copy2(MANIFEST, generated)

        steps: list[dict[str, object]] = report["steps"]  # type: ignore[assignment]
        steps.append(run(
            [
                sys.executable,
                "ROOT_tools/update_repository_file_manifest_incremental.py",
                "--target",
                str(generated),
                "--force-hash",
            ],
            output / "generation.log",
        ))

        require_nonempty(generated, "generated repository manifest")
        generated_lines = generated.read_text(encoding="utf-8-sig").splitlines()
        if len(generated_lines) < 2:
            raise RuntimeError(
                "generated repository manifest contains no inventory rows"
            )

        committed_text = committed.read_text(encoding="utf-8")
        generated_text = generated.read_text(encoding="utf-8")
        changed = committed_text != generated_text
        report["manifestChanged"] = changed
        report["generatedManifest"] = str(generated.relative_to(ROOT))
        report["generatedManifestSha256"] = sha256(generated)
        report["generatedLineCount"] = len(generated_lines)

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

        ledger_path = output / "release-ownership-ledger.tsv"
        pending_path = output / "RELEASE_CLEARANCE_PENDING.tsv"
        audit_report_path = output / "release-inventory-report.json"
        audit_command = [
            sys.executable,
            "ROOT_build/ci/audit_repository_release_inventory.py",
            str(generated),
            "--clearance-registry",
            str(CLEARANCE),
            "--ledger",
            str(ledger_path),
            "--pending",
            str(pending_path),
            "--report",
            str(audit_report_path),
        ]
        if args.require_release_clearance:
            audit_command.append("--require-clearance")
        steps.append(run(audit_command, output / "audit.log"))

        for path, label in (
            (ledger_path, "release ownership ledger"),
            (pending_path, "pending clearance registry"),
            (audit_report_path, "release inventory audit report"),
        ):
            require_nonempty(path, label)

        audit = json.loads(audit_report_path.read_text(encoding="utf-8"))
        audit_status = audit.get("status")
        if audit_status not in EXPECTED_AUDIT_STATUSES:
            raise RuntimeError(f"inventory audit status is not acceptable: {audit_status!r}")

        row_count = audit.get("rowCount")
        if not isinstance(row_count, int) or row_count < 100:
            raise RuntimeError(
                f"inventory audit rowCount is not credible: {row_count!r}"
            )
        if audit.get("blockerCount") != 0:
            raise RuntimeError(
                f"inventory audit retained blockers: {audit.get('blockerCount')!r}"
            )
        if audit.get("registryErrorCount") != 0:
            raise RuntimeError(
                "inventory audit retained clearance-registry errors: "
                f"{audit.get('registryErrorCount')!r}"
            )
        if args.require_release_clearance:
            if audit_status != "verified" or audit.get("releaseReady") is not True:
                raise RuntimeError(
                    "release clearance was required but the audit is not verified and release-ready"
                )

        report["auditStatus"] = audit_status
        for key in (
            "rowCount",
            "inventoryRows",
            "totalBytes",
            "releaseCandidateRows",
            "releaseCandidateBytes",
            "clearanceRequiredCount",
            "clearanceApprovedCount",
            "clearancePendingCount",
            "clearanceRejectedCount",
            "registryEntryCount",
            "registryErrorCount",
            "staleRegistryCount",
            "blockerCount",
            "protectedRuntimeDuplicateCount",
            "releaseReady",
        ):
            report[key] = audit.get(key)

        report["evidence"] = {
            "manifestDiff": str(diff_path.relative_to(ROOT)),
            "ownershipLedger": str(ledger_path.relative_to(ROOT)),
            "pendingClearance": str(pending_path.relative_to(ROOT)),
            "auditReport": str(audit_report_path.relative_to(ROOT)),
        }

        if args.update_committed_manifest:
            shutil.copy2(generated, MANIFEST)
            if sha256(MANIFEST) != report["generatedManifestSha256"]:
                raise RuntimeError("committed manifest hash differs after replacement")
            report["committedManifestUpdated"] = True
            report["committedManifestSha256"] = sha256(MANIFEST)
        else:
            report["committedManifestUpdated"] = False
            if changed:
                raise RuntimeError(
                    "committed repository inventory is stale; review manifest-diff.txt and rerun with --update-committed-manifest"
                )

        report["status"] = "passed"
        report["completedAtUtc"] = utc_now()
        report_path.write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"LOCAL INVENTORY GATE PASSED: {report_path}")
        return 0
    except Exception as exc:  # noqa: BLE001 - single fail-closed diagnostic surface
        report["status"] = "failed"
        report["completedAtUtc"] = utc_now()
        report["errorType"] = type(exc).__name__
        report["error"] = str(exc)
        if isinstance(exc, InventoryStepFailure):
            report["failedStep"] = exc.step
            steps = report["steps"]  # type: ignore[assignment]
            steps.append(exc.step)
        report["traceback"] = traceback.format_exc()
        report_path.write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        print(f"LOCAL INVENTORY GATE FAILED: {exc}", file=sys.stderr)
        print(f"Report: {report_path}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
