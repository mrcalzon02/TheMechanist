#!/usr/bin/env python3
"""Audit The Mechanist repository inventory for alpha-release ownership.

The input is the governed seven-column repository manifest. The audit rejects
structural corruption, unsafe paths, hash failures, duplicate rows, and obvious
development/source artifacts inside player package trees. It also emits a
complete ownership ledger so asset redistribution and runtime necessity can be
reviewed without treating every repository file as releasable.
"""

from __future__ import annotations

import argparse
import csv
import json
import pathlib
import sys
from collections import Counter, defaultdict

COLUMNS = [
    "relative_path",
    "file_kind",
    "text_or_binary",
    "extension",
    "bytes",
    "modified_utc",
    "sha256",
]
PACKAGE_PREFIXES = ("PACKAGE_client/", "PACKAGE_launcher/", "PACKAGE_installer/")
RUNTIME_ASSET_PREFIXES = (
    "PACKAGE_client/assets/",
    "PACKAGE_launcher/java/src/main/resources/assets/",
)
SOURCE_ONLY_EXTENSIONS = {
    "psd", "psb", "xcf", "kra", "blend", "blend1", "ase", "aseprite",
    "svgz", "ai", "eps", "obj", "fbx", "glb", "gltf", "wav.bak",
}
TEMPORARY_EXTENSIONS = {"tmp", "temp", "bak", "old", "orig", "rej", "log", "class"}
ASSET_EXTENSIONS = {
    "png", "jpg", "jpeg", "gif", "bmp", "ico", "svg", "webp",
    "mp3", "wav", "ogg", "flac", "m4a", "ttf", "otf", "woff", "woff2",
}
THIRD_PARTY_BINARY_EXTENSIONS = {"jar", "dll", "so", "dylib"}


def safe_path(text: str) -> bool:
    relative = pathlib.PurePosixPath(text)
    return bool(text) and not relative.is_absolute() and ".." not in relative.parts and "\\" not in text


def valid_sha(value: str) -> bool:
    text = value.strip().lower()
    return len(text) == 64 and all(char in "0123456789abcdef" for char in text)


def classify(path: str, kind: str, extension: str) -> tuple[str, bool, str, str]:
    lower = path.lower()
    ext = extension.lower()

    if lower.startswith("root_src_assets/"):
        return (
            "protected-source",
            False,
            "source-only",
            "Protected upstream/source vault; never copied merely because it exists.",
        )
    if lower.startswith("package_client/assets/"):
        return (
            "client-runtime",
            True,
            "clearance-required" if ext in ASSET_EXTENSIONS else "runtime-review",
            "Client-owned physical runtime asset/data path.",
        )
    if lower.startswith("package_launcher/java/src/main/resources/assets/"):
        return (
            "launcher-runtime",
            True,
            "clearance-required" if ext in ASSET_EXTENSIONS else "runtime-review",
            "Launcher-owned runtime resource path.",
        )
    if lower.startswith("package_client/"):
        return (
            "client-package-input",
            True,
            "runtime-review",
            "Client package input outside the primary asset tree.",
        )
    if lower.startswith("package_launcher/"):
        return (
            "launcher-package-input",
            True,
            "runtime-review",
            "Thin-launcher source/resource/package input.",
        )
    if lower.startswith("package_installer/"):
        return (
            "installer-input",
            False,
            "build-only",
            "Native installer source, resource, or documentation input.",
        )
    if lower.startswith("src/"):
        return ("source-code", False, "build-only", "Compiled into client/server artifacts; not shipped loose.")
    if lower.startswith("root_docs/"):
        return ("development-documentation", False, "development-only", "Durable project documentation.")
    if lower.startswith("root_tools/") or lower.startswith("scripts/"):
        return ("developer-tooling", False, "development-only", "Developer/build/maintenance tooling.")
    if lower.startswith("root_build/"):
        return ("build-orchestration", False, "build-only", "Checked-in release/build orchestration.")
    if lower.startswith(".github/"):
        return ("continuous-integration", False, "development-only", "GitHub workflow/configuration.")
    if lower.startswith("config/"):
        return ("build-configuration", False, "build-only", "Compiler, obfuscation, or packaging configuration.")
    if lower in {"pom.xml", "readme.md", ".gitignore", ".gitattributes"}:
        return ("repository-control", False, "development-only", "Repository-level build or orientation file.")
    if ext in ASSET_EXTENSIONS:
        return ("unowned-asset-review", False, "clearance-required", "Asset is outside an approved consuming package root.")
    if ext in THIRD_PARTY_BINARY_EXTENSIONS:
        return ("unowned-binary-review", False, "clearance-required", "Binary is outside an approved support/package staging path.")
    return ("unclassified-review", False, "review-required", f"No release owner matched file kind {kind!r}.")


def load_manifest(path: pathlib.Path) -> list[dict[str, str]]:
    if not path.is_file() or path.stat().st_size == 0:
        raise RuntimeError(f"repository manifest is missing or empty: {path}")
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        if reader.fieldnames != COLUMNS:
            raise RuntimeError(
                f"repository manifest columns {reader.fieldnames!r}, expected {COLUMNS!r}"
            )
        return [dict(row) for row in reader]


def write_ledger(path: pathlib.Path, rows: list[dict[str, object]]) -> None:
    columns = [
        "relative_path",
        "file_kind",
        "extension",
        "bytes",
        "sha256",
        "release_owner",
        "release_candidate",
        "clearance_state",
        "reason",
        "source_runtime_duplicate",
    ]
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=columns, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def audit(manifest_path: pathlib.Path) -> tuple[dict[str, object], list[dict[str, object]]]:
    source_rows = load_manifest(manifest_path)
    blockers: list[str] = []
    warnings: list[str] = []
    seen_paths: set[str] = set()
    hashes: dict[str, list[str]] = defaultdict(list)
    normalized: list[dict[str, object]] = []
    self_rows = 0
    total_bytes = 0

    for index, row in enumerate(source_rows, start=2):
        path = (row.get("relative_path") or "").strip()
        if path in seen_paths:
            blockers.append(f"duplicate manifest path at line {index}: {path}")
        seen_paths.add(path)
        if not safe_path(path):
            blockers.append(f"unsafe manifest path at line {index}: {path!r}")

        if row.get("file_kind") == "generated_repository_manifest":
            self_rows += 1
            if row.get("sha256") != "SELF_GENERATED":
                blockers.append("generated repository manifest self-row has invalid SHA marker")
            continue

        sha = (row.get("sha256") or "").strip().lower()
        if sha.startswith("hash_error:"):
            blockers.append(f"hash failure for {path}: {sha}")
        elif not valid_sha(sha):
            blockers.append(f"invalid SHA-256 for {path}: {sha!r}")
        else:
            hashes[sha].append(path)

        try:
            size = int(row.get("bytes") or "")
            if size < 0:
                raise ValueError
            total_bytes += size
        except ValueError:
            blockers.append(f"invalid byte length for {path}: {row.get('bytes')!r}")
            size = 0

        owner, candidate, clearance, reason = classify(
            path,
            row.get("file_kind", ""),
            row.get("extension", ""),
        )
        lower = path.lower()
        ext = (row.get("extension") or "").lower()
        if lower.startswith(tuple(prefix.lower() for prefix in PACKAGE_PREFIXES)):
            if ext in SOURCE_ONLY_EXTENSIONS or ext in TEMPORARY_EXTENSIONS:
                blockers.append(
                    f"development/source artifact is inside a package tree: {path}"
                )
            if any(part in {"diagnostics", "target", "build", "dist", "scratch", "temp"}
                   for part in pathlib.PurePosixPath(lower).parts):
                blockers.append(f"generated/development directory is inside a package tree: {path}")
        normalized.append({
            "relative_path": path,
            "file_kind": row.get("file_kind", ""),
            "extension": row.get("extension", ""),
            "bytes": size,
            "sha256": sha,
            "release_owner": owner,
            "release_candidate": "true" if candidate else "false",
            "clearance_state": clearance,
            "reason": reason,
            "source_runtime_duplicate": "false",
        })

    if len(normalized) < 100:
        blockers.append(
            f"repository inventory contains only {len(normalized)} file rows; full checkout inventory is not credible"
        )
    if self_rows != 1:
        blockers.append(f"repository manifest must contain exactly one self-row, found {self_rows}")

    duplicate_hashes = {sha: paths for sha, paths in hashes.items() if len(paths) > 1}
    protected_runtime_duplicates: set[str] = set()
    for sha, paths in duplicate_hashes.items():
        has_source = any(path.lower().startswith("root_src_assets/") for path in paths)
        runtime_paths = [
            path for path in paths
            if path.lower().startswith(tuple(prefix.lower() for prefix in RUNTIME_ASSET_PREFIXES))
        ]
        if has_source and runtime_paths:
            protected_runtime_duplicates.update(paths)
            warnings.append(
                "exact source-vault/runtime duplicate requires transformation and redistribution review: "
                + ", ".join(paths[:8])
            )

    for row in normalized:
        if row["relative_path"] in protected_runtime_duplicates:
            row["source_runtime_duplicate"] = "true"

    owner_counts = Counter(str(row["release_owner"]) for row in normalized)
    clearance_counts = Counter(str(row["clearance_state"]) for row in normalized)
    candidate_rows = [row for row in normalized if row["release_candidate"] == "true"]
    candidate_bytes = sum(int(row["bytes"]) for row in candidate_rows)
    clearance_review = [
        row["relative_path"]
        for row in candidate_rows
        if row["clearance_state"] in {"clearance-required", "runtime-review", "review-required"}
    ]

    report = {
        "status": "blocked" if blockers else "audited",
        "manifest": str(manifest_path),
        "inventoryRows": len(normalized),
        "totalBytes": total_bytes,
        "releaseCandidateRows": len(candidate_rows),
        "releaseCandidateBytes": candidate_bytes,
        "ownerCounts": dict(sorted(owner_counts.items())),
        "clearanceCounts": dict(sorted(clearance_counts.items())),
        "duplicateContentHashes": len(duplicate_hashes),
        "protectedSourceRuntimeDuplicateRows": len(protected_runtime_duplicates),
        "clearanceReviewRequired": len(clearance_review),
        "clearanceReviewSample": clearance_review[:50],
        "structuralBlockers": blockers,
        "reviewWarnings": warnings[:100],
        "releaseReady": not blockers and not clearance_review,
    }
    return report, normalized


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=pathlib.Path)
    parser.add_argument("--ledger", type=pathlib.Path, required=True)
    parser.add_argument("--report", type=pathlib.Path, required=True)
    parser.add_argument(
        "--require-clearance",
        action="store_true",
        help="Fail while any release-candidate asset/binary remains in review.",
    )
    args = parser.parse_args()
    try:
        report, ledger = audit(args.manifest.resolve())
        write_ledger(args.ledger.resolve(), ledger)
        args.report.resolve().parent.mkdir(parents=True, exist_ok=True)
        args.report.resolve().write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
    except Exception as exc:  # noqa: BLE001 - inventory audit needs one clear failure
        print(f"REPOSITORY RELEASE INVENTORY AUDIT FAILED: {exc}", file=sys.stderr)
        return 1

    print(json.dumps(report, indent=2, sort_keys=True))
    if report["structuralBlockers"]:
        return 1
    if args.require_clearance and report["clearanceReviewRequired"]:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
