#!/usr/bin/env python3
"""Verify that local alpha-release gate evidence belongs together.

This verifier is read-only. It does not build, package, update manifests, publish,
or mutate release history. It prevents separate packaged playtest-operations,
Java, inventory, and native gate reports from being mistaken for one coherent
release candidate when their policy, commit, platform, hardening, or status
identities disagree.
"""

from __future__ import annotations

import argparse
import json
import pathlib
import sys
from typing import Any

ROOT = pathlib.Path(__file__).resolve().parents[2]
SCHEMA = 3


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
            ROOT / "dist" / "local-release-sequence"
            / "playtest-operations-package.json"
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
            args.operations_report.resolve(), "packaged playtest-operations gate"
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
                "playtest-operations report must verify exactly eight source documents"
            )
        source_hashes: dict[str, str] = {}
        for record in documents:
            if not isinstance(record, dict):
                raise RuntimeError(
                    "playtest-operations report contains a malformed source document record"
                )
            destination = record.get("destination")
            digest = record.get("sha256")
            if not isinstance(destination, str) or not destination.startswith("docs/"):
                raise RuntimeError(
                    "playtest-operations report contains an invalid package destination"
                )
            if destination in source_hashes:
                raise RuntimeError(
                    f"playtest-operations report repeats destination {destination}"
                )
            if not isinstance(digest, str) or len(digest) != 64:
                raise RuntimeError(
                    f"playtest-operations report has invalid SHA-256 for {destination}"
                )
            source_hashes[destination] = digest

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

        packaged_operations = operations.get("distribution")
        if not isinstance(packaged_operations, dict):
            raise RuntimeError(
                "playtest-operations report has no reopened packaged distribution object"
            )
        require(
            packaged_operations,
            "status",
            "verified",
            "packaged playtest operations",
        )
        packaged_documents = packaged_operations.get("documents")
        if not isinstance(packaged_documents, list) or len(packaged_documents) != 8:
            raise RuntimeError(
                "packaged playtest operations must verify exactly eight documents"
            )
        packaged_hashes: dict[str, str] = {}
        for record in packaged_documents:
            if not isinstance(record, dict):
                raise RuntimeError(
                    "packaged playtest operations contain a malformed document record"
                )
            path = record.get("path")
            digest = record.get("sha256")
            if not isinstance(path, str) or path not in source_hashes:
                raise RuntimeError(
                    f"packaged playtest operations contain an unexpected document: {path!r}"
                )
            if path in packaged_hashes:
                raise RuntimeError(
                    f"packaged playtest operations repeat document {path}"
                )
            if digest != source_hashes[path]:
                raise RuntimeError(
                    f"packaged playtest document hash differs from source: {path}"
                )
            if record.get("role") != "documentation":
                raise RuntimeError(
                    f"packaged playtest document has wrong role: {path}"
                )
            packaged_hashes[path] = digest
        if packaged_hashes != source_hashes:
            raise RuntimeError(
                "packaged playtest document set does not exactly match governed source"
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
        if packaged_operations.get("commit") != commit:
            raise RuntimeError(
                "packaged playtest operations commit does not match the Java candidate: "
                f"{packaged_operations.get('commit')!r} != {commit!r}"
            )
        if packaged_operations.get("platform") != platform:
            raise RuntimeError(
                "packaged playtest operations platform does not match the Java candidate: "
                f"{packaged_operations.get('platform')!r} != {platform!r}"
            )
        require(
            packaged_operations,
            "javaRelease",
            17,
            "packaged playtest operations",
        )
        require(
            packaged_operations,
            "releaseHardened",
            True,
            "packaged playtest operations",
        )
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
            "playtestSourceDocumentCount": len(documents),
            "playtestPackagedDocumentCount": len(packaged_documents),
            "playtestPackagedIdentityVerified": True,
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
