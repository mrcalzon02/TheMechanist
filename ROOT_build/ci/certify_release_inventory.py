#!/usr/bin/env python3
"""Certify repository inventory and clearance for one exact release commit."""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import re
import sys

COMMIT_RE = re.compile(r"^[0-9a-f]{40}$")


def fail(message: str) -> None:
    raise RuntimeError(message)


def sha256(path: pathlib.Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def certify(
    generated_manifest: pathlib.Path,
    committed_manifest: pathlib.Path,
    clearance_registry: pathlib.Path,
    audit_report: pathlib.Path,
    source_commit: str,
) -> dict[str, object]:
    files = (
        generated_manifest,
        committed_manifest,
        clearance_registry,
        audit_report,
    )
    for path in files:
        if not path.is_file():
            fail(f"required inventory evidence is missing: {path}")

    commit = source_commit.strip().lower()
    if not COMMIT_RE.fullmatch(commit):
        fail(f"source commit must be an exact 40-character SHA: {source_commit!r}")

    generated_bytes = generated_manifest.read_bytes()
    committed_bytes = committed_manifest.read_bytes()
    if generated_bytes != committed_bytes:
        fail(
            "the committed repository manifest differs from the deterministic "
            "full-checkout manifest"
        )

    report = json.loads(audit_report.read_text(encoding="utf-8"))
    if report.get("status") != "verified":
        fail(f"inventory audit status is not verified: {report.get('status')!r}")
    if report.get("releaseReady") is not True:
        fail("inventory audit did not declare releaseReady=true")
    for key in (
        "blockerCount",
        "clearancePendingCount",
        "registryErrorCount",
        "staleRegistryCount",
        "protectedRuntimeDuplicateCount",
    ):
        if int(report.get(key, -1)) != 0:
            fail(f"inventory audit requires {key}=0, found {report.get(key)!r}")

    manifest_hash = hashlib.sha256(generated_bytes).hexdigest()
    registry_hash = sha256(clearance_registry)
    if report.get("manifestSha256") != manifest_hash:
        fail("inventory audit manifest hash differs from exact generated manifest")
    if report.get("clearanceRegistrySha256") != registry_hash:
        fail("inventory audit registry hash differs from exact clearance registry")

    row_count = int(report.get("rowCount", 0))
    required_count = int(report.get("clearanceRequiredCount", 0))
    approved_count = int(report.get("clearanceApprovedCount", 0))
    if row_count < 1:
        fail("repository manifest contains no governed file rows")
    if required_count != approved_count:
        fail(
            "clearance approval count differs from required count: "
            f"required={required_count} approved={approved_count}"
        )

    return {
        "status": "verified",
        "sourceCommit": commit,
        "manifest": str(committed_manifest),
        "manifestSha256": manifest_hash,
        "clearanceRegistry": str(clearance_registry),
        "clearanceRegistrySha256": registry_hash,
        "auditReport": str(audit_report),
        "rowCount": row_count,
        "clearanceRequiredCount": required_count,
        "clearanceApprovedCount": approved_count,
        "clearancePendingCount": 0,
        "blockerCount": 0,
        "staleRegistryCount": 0,
        "protectedRuntimeDuplicateCount": 0,
        "releaseReady": True,
        "legalConclusion": (
            "not-provided; certificate proves exact project-owner registry "
            "coverage and release-gate consistency only"
        ),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--generated-manifest", type=pathlib.Path, required=True)
    parser.add_argument("--committed-manifest", type=pathlib.Path, required=True)
    parser.add_argument("--clearance-registry", type=pathlib.Path, required=True)
    parser.add_argument("--audit-report", type=pathlib.Path, required=True)
    parser.add_argument("--source-commit", required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    args = parser.parse_args()
    try:
        result = certify(
            args.generated_manifest.resolve(),
            args.committed_manifest.resolve(),
            args.clearance_registry.resolve(),
            args.audit_report.resolve(),
            args.source_commit,
        )
    except Exception as failure:  # noqa: BLE001 - one certification failure surface
        print(f"RELEASE INVENTORY CERTIFICATION FAILED: {failure}", file=sys.stderr)
        return 1
    rendered = json.dumps(result, indent=2, sort_keys=True)
    print(rendered)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(rendered + "\n", encoding="utf-8")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
