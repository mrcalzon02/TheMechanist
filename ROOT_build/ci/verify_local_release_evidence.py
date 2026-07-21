#!/usr/bin/env python3
"""Verify that local alpha-release gate evidence belongs together.

This verifier is read-only. It does not build, package, update manifests, publish,
or mutate release history. It prevents separate playtest-operations, Java,
inventory, and native gate reports from being mistaken for one coherent release
candidate when their policy, commit, platform, hardening, or status identities
disagree.
"""

from __future__ import annotations

import argparse
import json
import pathlib
import sys
from typing import Any

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCHEMA = 2


def load_report(path: pathlib.Path, label: str) -> dict[str, Any]:
    if not path.is_file():
        raise RuntimeError(f"missing {label} report: {path}")
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"invalid JSON in {label} report {path}: {exc}") from exc
    if not isinstance(data, dict):
        raise RuntimeError(f"{label} report is not a JSON object: {path}")
    return data


def require(data: dict[str, Any], key: str, expected: Any, label: str) -> None:
    actual = data.get(key)
    if actual != expected:
        raise RuntimeError(
            f"{label} report requires {key}={expected!r}, found {actual!r}"
        )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--operations-report",
        type=pathlib.Path,
        default=(
            ROOT / "dist" / "local-release-sequence" / "playtest-operations.json"
        ),
    )
    parser.add_argument(
        "--java-report",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-java-gate-report.json",
    )
    parser.add_argument(
        "--inventory-report",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-inventory-gate-report.json",
    )
    parser.add_argument(
        "--native-report",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-native-gate-report.json",
    )
    parser.add_argument(
        "--output",
        type=pathlib.Path,
        default=ROOT / "dist" / "local-release-evidence-report.json",
    )
    parser.add_argument(
        "--require-clearance",
        action="store_true",
        help="Require the inventory gate to have passed release-clearance mode.",
    )
    args = parser.parse_args()

    output = args.output.resolve()
    output.parent.mkdir(parents=True, exist_ok=True)
    result: dict[str, Any] = {"schema": SCHEMA, "status": "failed"}

    try:
        operations = load_report(
            args.operations_report.resolve(), "playtest-operations gate"
        )
        java = load_report(args.java_report.resolve(), "Java gate")
        inventory = load_report(args.inventory_report.resolve(), "inventory gate")
        native = load_report(args.native_report.resolve(), "native gate")

        require(operations, "status", "verified", "playtest-operations gate")
        source_operations = operations.get("source")
        if not isinstance(source_operations, dict):
            raise RuntimeError(
                "playtest-operations report has no verified source-policy object"
            )
        require(
            source_operations,
            "status",
            "verified",
            "playtest-operations source policy",
        )
        documents = source_operations.get("documents")
        if not isinstance(documents, list) or len(documents) != 8:
            raise RuntimeError(
                "playtest-operations report must verify exactly eight packaged documents"
            )
        destinations: set[str] = set()
        for record in documents:
            if not isinstance(record, dict):
                raise RuntimeError(
                    "playtest-operations report contains a malformed document record"
                )
            destination = record.get("destination")
            digest = record.get("sha256")
            if not isinstance(destination, str) or not destination.startswith("docs/"):
                raise RuntimeError(
                    "playtest-operations report contains an invalid package destination"
                )
            if destination in destinations:
                raise RuntimeError(
                    f"playtest-operations report repeats destination {destination}"
                )
            if not isinstance(digest, str) or len(digest) != 64:
                raise RuntimeError(
                    f"playtest-operations report has invalid SHA-256 for {destination}"
                )
            destinations.add(destination)
        issue_form = source_operations.get("issueForm")
        if not isinstance(issue_form, dict):
            raise RuntimeError(
                "playtest-operations report has no structured limited-alpha issue form"
            )
        if issue_form.get("path") != ".github/ISSUE_TEMPLATE/limited-alpha-defect.yml":
            raise RuntimeError(
                "playtest-operations report references the wrong limited-alpha issue form"
            )
        issue_digest = issue_form.get("sha256")
        if not isinstance(issue_digest, str) or len(issue_digest) != 64:
            raise RuntimeError(
                "playtest-operations report has an invalid issue-form SHA-256"
            )

        require(java, "status", "passed", "Java gate")
        require(java, "releaseHardened", True, "Java gate")
        require(inventory, "status", "passed", "inventory gate")
        require(native, "status", "passed", "native gate")
        require(native, "installerCertificationClaimed", False, "native gate")

        commit = java.get("commit")
        platform = java.get("platform")
        if not isinstance(commit, str) or len(commit) != 40:
            raise RuntimeError(f"Java gate commit identity is invalid: {commit!r}")
        if platform not in {"linux-x64", "windows-x64"}:
            raise RuntimeError(f"Java gate platform identity is invalid: {platform!r}")
        if inventory.get("commit") != commit:
            raise RuntimeError(
                f"inventory commit {inventory.get('commit')!r} does not match Java commit {commit!r}"
            )
        if native.get("commit") != commit:
            raise RuntimeError(
                f"native commit {native.get('commit')!r} does not match Java commit {commit!r}"
            )
        if native.get("platform") != platform:
            raise RuntimeError(
                f"native platform {native.get('platform')!r} does not match Java platform {platform!r}"
            )

        audit_status = inventory.get("auditStatus")
        if audit_status not in {"verified", "review-required"}:
            raise RuntimeError(f"inventory audit status is invalid: {audit_status!r}")
        if int(inventory.get("rowCount", 0)) < 100:
            raise RuntimeError("inventory report does not contain a credible full-checkout row count")
        if int(inventory.get("blockerCount", -1)) != 0:
            raise RuntimeError("inventory report contains structural or rejection blockers")
        if int(inventory.get("registryErrorCount", -1)) != 0:
            raise RuntimeError("inventory report contains clearance-registry errors")

        if args.require_clearance:
            require(inventory, "requireReleaseClearance", True, "inventory gate")
            require(inventory, "auditStatus", "verified", "inventory gate")
            require(inventory, "releaseReady", True, "inventory gate")

        result.update({
            "status": "verified",
            "commit": commit,
            "platform": platform,
            "releaseHardened": True,
            "playtestOperationsVerified": True,
            "playtestDocumentCount": len(documents),
            "structuredIssueFormVerified": True,
            "releaseClearanceRequired": args.require_clearance,
            "releaseClearanceVerified": inventory.get("releaseReady") is True,
            "installerCertificationClaimed": False,
            "operationsReport": str(args.operations_report.resolve()),
            "javaReport": str(args.java_report.resolve()),
            "inventoryReport": str(args.inventory_report.resolve()),
            "nativeReport": str(args.native_report.resolve()),
        })
        output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        print(f"LOCAL RELEASE EVIDENCE VERIFIED: {output}")
        return 0
    except Exception as exc:  # noqa: BLE001 - one fail-closed evidence surface
        result["errorType"] = type(exc).__name__
        result["error"] = str(exc)
        output.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        print(f"LOCAL RELEASE EVIDENCE FAILED: {exc}", file=sys.stderr)
        print(f"Report: {output}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
