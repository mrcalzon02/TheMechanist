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
SCHEMA = 4
PLAYTEST_DOCUMENT_COUNT = 8


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


def document_hashes(
    records: object,
    *,
    label: str,
    path_key: str,
    require_role: bool,
) -> dict[str, str]:
    if not isinstance(records, list) or len(records) != PLAYTEST_DOCUMENT_COUNT:
        raise RuntimeError(
            f"{label} must contain exactly {PLAYTEST_DOCUMENT_COUNT} documents"
        )
    hashes: dict[str, str] = {}
    for record in records:
        if not isinstance(record, dict):
            raise RuntimeError(f"{label} contains a malformed document record")
        path = record.get(path_key)
        digest = record.get("sha256")
        if not isinstance(path, str) or not path.startswith("docs/"):
            raise RuntimeError(f"{label} contains an invalid document path: {path!r}")
        if path in hashes:
            raise RuntimeError(f"{label} repeats document path {path}")
        if not isinstance(digest, str) or len(digest) != 64:
            raise RuntimeError(f"{label} has invalid SHA-256 for {path}")
        if require_role and record.get("role") != "documentation":
            raise RuntimeError(f"{label} has wrong role for {path}")
        hashes[path] = digest
    return hashes


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
        source_hashes = document_hashes(
            source_operations.get("documents"),
            label="playtest-operations source policy",
            path_key="destination",
            require_role=False,
        )

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
        packaged_hashes = document_hashes(
            packaged_operations.get("documents"),
            label="packaged playtest operations",
            path_key="path",
            require_role=True,
        )
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

        staging = native.get("stagingVerification")
        if not isinstance(staging, dict):
            raise RuntimeError("native gate report has no staging verification object")
        if staging.get("commit") != commit or staging.get("platform") != platform:
            raise RuntimeError(
                "native staging verification identity does not match the Java candidate"
            )
        require(staging, "releaseHardened", True, "native staging verification")
        require(
            staging,
            "playtestOperationsVerified",
            True,
            "native staging verification",
        )
        require(
            staging,
            "playtestDocumentCount",
            PLAYTEST_DOCUMENT_COUNT,
            "native staging verification",
        )

        image = native.get("imageVerification")
        if not isinstance(image, dict):
            raise RuntimeError("native gate report has no image verification object")
        require(image, "status", "verified", "native image verification")
        if image.get("commit") != commit or image.get("platform") != platform:
            raise RuntimeError(
                "native image verification identity does not match the Java candidate"
            )
        require(image, "releaseHardened", True, "native image verification")
        require(
            image,
            "installerSourceVerificationSchema",
            4,
            "native image verification",
        )
        require(
            image,
            "playtestOperationsVerified",
            True,
            "native image verification",
        )
        require(
            image,
            "playtestDocumentCount",
            PLAYTEST_DOCUMENT_COUNT,
            "native image verification",
        )
        native_hashes = document_hashes(
            image.get("playtestDocuments"),
            label="native image verification",
            path_key="path",
            require_role=True,
        )
        if native_hashes != packaged_hashes:
            raise RuntimeError(
                "native image playtest documents differ from the canonical package"
            )
        require(image, "mutableStorageIncluded", False, "native image verification")
        require(image, "remoteGameplayCertified", False, "native image verification")

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
            "playtestSourceDocumentCount": len(source_hashes),
            "playtestPackagedDocumentCount": len(packaged_hashes),
            "playtestNativeDocumentCount": len(native_hashes),
            "playtestPackagedIdentityVerified": True,
            "playtestNativeIdentityVerified": True,
            "structuredIssueFormVerified": True,
            "nativeInstallerSourceVerificationSchema": 4,
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
