#!/usr/bin/env python3
"""Audit repository inventory and exact hash-bound release clearance.

The governed repository manifest is the source of truth for file identity. This
script classifies release ownership, rejects structural corruption, applies the
human-reviewed clearance registry to exact path/SHA pairs, emits a complete
ledger plus a reviewer-ready pending list, and reports whether a release can be
certified without inventing ownership or licensing conclusions.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
import pathlib
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass

MANIFEST_COLUMNS = [
    "relative_path",
    "file_kind",
    "text_or_binary",
    "extension",
    "bytes",
    "modified_utc",
    "sha256",
]
REGISTRY_COLUMNS = [
    "path",
    "sha256",
    "decision",
    "owner_or_license",
    "evidence",
    "approved_by",
    "approved_at_utc",
    "notes",
]
LEDGER_COLUMNS = [
    "relative_path",
    "file_kind",
    "extension",
    "bytes",
    "sha256",
    "release_owner",
    "release_candidate",
    "clearance_required",
    "clearance_state",
    "registry_decision",
    "owner_or_license",
    "evidence",
    "approved_by",
    "approved_at_utc",
    "reason",
    "source_runtime_duplicate",
]
PENDING_COLUMNS = [
    "path",
    "sha256",
    "decision",
    "owner_or_license",
    "evidence",
    "approved_by",
    "approved_at_utc",
    "notes",
]

PACKAGE_PREFIXES = (
    "package_client/",
    "package_launcher/",
    "package_installer/",
)
RUNTIME_ASSET_PREFIXES = (
    "package_client/assets/",
    "package_launcher/java/src/main/resources/assets/",
    "package_installer/",
)
SOURCE_ONLY_EXTENSIONS = {
    "psd", "psb", "xcf", "kra", "blend", "blend1", "ase", "aseprite",
    "svgz", "ai", "eps", "obj", "fbx", "glb", "gltf", "wav.bak",
}
TEMPORARY_EXTENSIONS = {
    "tmp", "temp", "bak", "old", "orig", "rej", "log", "class",
}
MEDIA_EXTENSIONS = {
    "png", "jpg", "jpeg", "gif", "bmp", "ico", "svg", "webp",
    "mp3", "wav", "ogg", "flac", "m4a",
    "ttf", "otf", "woff", "woff2",
}
ARCHIVE_BINARY_EXTENSIONS = {
    "jar", "zip", "7z", "rar", "tar", "gz", "tgz",
    "exe", "msi", "dll", "so", "dylib", "bin",
}
CLEARANCE_EXTENSIONS = MEDIA_EXTENSIONS | ARCHIVE_BINARY_EXTENSIONS
ALLOWED_DECISIONS = {"approve", "reject"}


@dataclass(frozen=True)
class RegistryDecision:
    path: str
    sha256: str
    decision: str
    owner_or_license: str
    evidence: str
    approved_by: str
    approved_at_utc: str
    notes: str


def safe_path(text: str) -> bool:
    relative = pathlib.PurePosixPath(text)
    return (
        bool(text)
        and not relative.is_absolute()
        and ".." not in relative.parts
        and "\\" not in text
    )


def valid_sha(value: str) -> bool:
    text = value.strip().lower()
    return len(text) == 64 and all(char in "0123456789abcdef" for char in text)


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for block in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def load_tsv(path: pathlib.Path, columns: list[str], label: str) -> list[dict[str, str]]:
    if not path.is_file() or path.stat().st_size == 0:
        raise RuntimeError(f"{label} is missing or empty: {path}")
    with path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        if reader.fieldnames != columns:
            raise RuntimeError(
                f"{label} columns {reader.fieldnames!r}, expected {columns!r}"
            )
        return [dict(row) for row in reader]


def load_manifest(path: pathlib.Path) -> list[dict[str, str]]:
    return load_tsv(path, MANIFEST_COLUMNS, "repository manifest")


def load_registry(
    path: pathlib.Path,
) -> tuple[dict[str, RegistryDecision], list[str]]:
    rows = load_tsv(path, REGISTRY_COLUMNS, "release clearance registry")
    decisions: dict[str, RegistryDecision] = {}
    errors: list[str] = []
    for index, row in enumerate(rows, start=2):
        candidate = {key: (row.get(key) or "").strip() for key in REGISTRY_COLUMNS}
        item_path = candidate["path"]
        digest = candidate["sha256"].lower()
        decision = candidate["decision"].lower()
        if not item_path and not any(candidate.values()):
            continue
        if not safe_path(item_path):
            errors.append(f"registry line {index} has unsafe path {item_path!r}")
            continue
        if item_path in decisions:
            errors.append(f"registry line {index} duplicates path {item_path}")
            continue
        if not valid_sha(digest):
            errors.append(f"registry line {index} has invalid SHA-256 for {item_path}")
            continue
        if decision not in ALLOWED_DECISIONS:
            errors.append(
                f"registry line {index} decision for {item_path} must be approve or reject"
            )
            continue
        required_fields = (
            "owner_or_license",
            "evidence",
            "approved_by",
            "approved_at_utc",
        )
        missing = [name for name in required_fields if not candidate[name]]
        if missing:
            errors.append(
                f"registry line {index} for {item_path} is missing {', '.join(missing)}"
            )
            continue
        decisions[item_path] = RegistryDecision(
            path=item_path,
            sha256=digest,
            decision=decision,
            owner_or_license=candidate["owner_or_license"],
            evidence=candidate["evidence"],
            approved_by=candidate["approved_by"],
            approved_at_utc=candidate["approved_at_utc"],
            notes=candidate["notes"],
        )
    return decisions, errors


def classify(
    path: str,
    kind: str,
    extension: str,
) -> tuple[str, bool, bool, str, str]:
    lower = path.lower()
    ext = extension.lower()

    if lower.startswith("root_src_assets/"):
        return (
            "protected-source",
            False,
            False,
            "source-only",
            "Protected source vault; never shipped merely because it exists.",
        )

    if lower.startswith("package_client/assets/"):
        needs_clearance = ext in CLEARANCE_EXTENSIONS
        return (
            "client-runtime",
            True,
            needs_clearance,
            "clearance-required" if needs_clearance else "project-runtime",
            "Client-owned runtime asset or data path.",
        )

    if lower.startswith("package_launcher/java/src/main/resources/assets/"):
        needs_clearance = ext in CLEARANCE_EXTENSIONS
        return (
            "launcher-runtime",
            True,
            needs_clearance,
            "clearance-required" if needs_clearance else "project-runtime",
            "Launcher-owned runtime resource path.",
        )

    if lower.startswith("package_installer/"):
        needs_clearance = ext in CLEARANCE_EXTENSIONS
        return (
            "installer-input",
            needs_clearance,
            needs_clearance,
            "clearance-required" if needs_clearance else "build-only",
            "Native installer input; binary/media payloads require exact clearance.",
        )

    if lower.startswith("package_client/"):
        needs_clearance = ext in ARCHIVE_BINARY_EXTENSIONS
        return (
            "client-package-input",
            True,
            needs_clearance,
            "clearance-required" if needs_clearance else "project-runtime",
            "Client package input outside the primary asset tree.",
        )

    if lower.startswith("package_launcher/"):
        needs_clearance = ext in ARCHIVE_BINARY_EXTENSIONS
        return (
            "launcher-package-input",
            True,
            needs_clearance,
            "clearance-required" if needs_clearance else "project-runtime",
            "Thin-launcher source, resource, or package input.",
        )

    if lower.startswith("src/"):
        return (
            "source-code", False, False, "build-only",
            "Compiled into client/server artifacts; not shipped loose.",
        )
    if lower.startswith("root_docs/"):
        return (
            "development-documentation", False, False, "development-only",
            "Durable project documentation.",
        )
    if lower.startswith("root_tools/") or lower.startswith("scripts/"):
        return (
            "developer-tooling", False, False, "development-only",
            "Developer, build, or maintenance tooling.",
        )
    if lower.startswith("root_build/"):
        return (
            "build-orchestration", False, False, "build-only",
            "Checked-in release and build orchestration.",
        )
    if lower.startswith(".github/"):
        return (
            "continuous-integration", False, False, "development-only",
            "GitHub workflow or configuration.",
        )
    if lower.startswith("config/"):
        return (
            "build-configuration", False, False, "build-only",
            "Compiler, obfuscation, or packaging configuration.",
        )
    if lower in {"pom.xml", "readme.md", ".gitignore", ".gitattributes"}:
        return (
            "repository-control", False, False, "development-only",
            "Repository-level build or orientation file.",
        )
    if ext in MEDIA_EXTENSIONS:
        return (
            "unowned-asset-review", False, False, "not-shipped",
            "Asset is outside an approved consuming package root.",
        )
    if ext in ARCHIVE_BINARY_EXTENSIONS:
        return (
            "unowned-binary-review", False, False, "not-shipped",
            "Binary is outside an approved package staging path.",
        )
    return (
        "unclassified-review", False, False, "not-shipped",
        f"No release owner matched file kind {kind!r}.",
    )


def write_tsv(path: pathlib.Path, columns: list[str], rows: list[dict[str, object]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=columns,
            delimiter="\t",
            lineterminator="\n",
            extrasaction="ignore",
        )
        writer.writeheader()
        writer.writerows(rows)


def audit(
    manifest_path: pathlib.Path,
    registry_path: pathlib.Path,
) -> tuple[dict[str, object], list[dict[str, object]], list[dict[str, object]]]:
    source_rows = load_manifest(manifest_path)
    registry, registry_errors = load_registry(registry_path)

    blockers: list[str] = []
    warnings: list[str] = []
    seen_paths: set[str] = set()
    hashes: dict[str, list[str]] = defaultdict(list)
    normalized: list[dict[str, object]] = []
    self_rows = 0
    total_bytes = 0

    for index, row in enumerate(source_rows, start=2):
        item_path = (row.get("relative_path") or "").strip()
        if item_path in seen_paths:
            blockers.append(f"duplicate manifest path at line {index}: {item_path}")
        seen_paths.add(item_path)
        if not safe_path(item_path):
            blockers.append(f"unsafe manifest path at line {index}: {item_path!r}")

        if row.get("file_kind") == "generated_repository_manifest":
            self_rows += 1
            if row.get("sha256") != "SELF_GENERATED":
                blockers.append("generated repository manifest self-row has invalid SHA marker")
            continue

        digest = (row.get("sha256") or "").strip().lower()
        if digest.startswith("hash_error:"):
            blockers.append(f"hash failure for {item_path}: {digest}")
        elif not valid_sha(digest):
            blockers.append(f"invalid SHA-256 for {item_path}: {digest!r}")
        else:
            hashes[digest].append(item_path)

        try:
            size = int(row.get("bytes") or "")
            if size < 0:
                raise ValueError
            total_bytes += size
        except ValueError:
            blockers.append(
                f"invalid byte length for {item_path}: {row.get('bytes')!r}"
            )
            size = 0

        owner, candidate, clearance_required, clearance_state, reason = classify(
            item_path,
            row.get("file_kind", ""),
            row.get("extension", ""),
        )
        lower = item_path.lower()
        ext = (row.get("extension") or "").lower()
        if lower.startswith(PACKAGE_PREFIXES):
            if ext in SOURCE_ONLY_EXTENSIONS or ext in TEMPORARY_EXTENSIONS:
                blockers.append(
                    f"development/source artifact is inside a package tree: {item_path}"
                )
            if any(
                part in {"diagnostics", "target", "build", "dist", "scratch", "temp"}
                for part in pathlib.PurePosixPath(lower).parts
            ):
                blockers.append(
                    f"generated/development directory is inside a package tree: {item_path}"
                )

        normalized.append({
            "relative_path": item_path,
            "file_kind": row.get("file_kind", ""),
            "extension": row.get("extension", ""),
            "bytes": size,
            "sha256": digest,
            "release_owner": owner,
            "release_candidate": candidate,
            "clearance_required": clearance_required,
            "clearance_state": clearance_state,
            "registry_decision": "",
            "owner_or_license": "",
            "evidence": "",
            "approved_by": "",
            "approved_at_utc": "",
            "reason": reason,
            "source_runtime_duplicate": False,
        })

    if len(normalized) < 100:
        blockers.append(
            f"repository inventory contains only {len(normalized)} file rows; "
            "full checkout inventory is not credible"
        )
    if self_rows != 1:
        blockers.append(
            f"repository manifest must contain exactly one self-row, found {self_rows}"
        )

    duplicate_hashes = {
        digest: paths for digest, paths in hashes.items() if len(paths) > 1
    }
    protected_runtime_paths: set[str] = set()
    for paths in duplicate_hashes.values():
        has_source = any(path.lower().startswith("root_src_assets/") for path in paths)
        runtime_paths = [
            path
            for path in paths
            if path.lower().startswith(RUNTIME_ASSET_PREFIXES)
        ]
        if has_source and runtime_paths:
            protected_runtime_paths.update(runtime_paths)
            warnings.append(
                "exact source-vault/runtime duplicate requires explicit release review: "
                + ", ".join(paths[:8])
            )

    rows_by_path = {str(row["relative_path"]): row for row in normalized}
    for item_path in protected_runtime_paths:
        row = rows_by_path[item_path]
        row["source_runtime_duplicate"] = True
        row["clearance_required"] = True
        row["clearance_state"] = "clearance-required"
        row["reason"] = (
            str(row["reason"])
            + " Exact bytes also exist under ROOT_src_assets and require explicit review."
        )

    stale_registry: list[str] = []
    rejected: list[str] = []
    pending_rows: list[dict[str, object]] = []
    approved_count = 0
    required_count = 0
    unresolved_protected_duplicates = 0

    for row in normalized:
        item_path = str(row["relative_path"])
        decision = registry.get(item_path)
        required = bool(row["clearance_required"])
        if required:
            required_count += 1
            if decision is None:
                row["clearance_state"] = "pending"
                pending_rows.append({
                    "path": item_path,
                    "sha256": row["sha256"],
                    "decision": "",
                    "owner_or_license": "",
                    "evidence": "",
                    "approved_by": "",
                    "approved_at_utc": "",
                    "notes": row["reason"],
                })
                if row["source_runtime_duplicate"]:
                    unresolved_protected_duplicates += 1
                continue
            if decision.sha256 != row["sha256"]:
                row["clearance_state"] = "stale-registry"
                stale_registry.append(
                    f"{item_path}: registry SHA {decision.sha256} != manifest SHA {row['sha256']}"
                )
                if row["source_runtime_duplicate"]:
                    unresolved_protected_duplicates += 1
                continue
            row["registry_decision"] = decision.decision
            row["owner_or_license"] = decision.owner_or_license
            row["evidence"] = decision.evidence
            row["approved_by"] = decision.approved_by
            row["approved_at_utc"] = decision.approved_at_utc
            if decision.decision == "approve":
                row["clearance_state"] = "approved"
                approved_count += 1
            else:
                row["clearance_state"] = "rejected"
                rejected.append(item_path)
                if row["source_runtime_duplicate"]:
                    unresolved_protected_duplicates += 1
        elif decision is not None:
            stale_registry.append(
                f"{item_path}: registry entry exists but current classification does not require clearance"
            )
            row["clearance_state"] = "stale-registry"
            row["registry_decision"] = decision.decision

    for item_path in registry:
        if item_path not in rows_by_path:
            stale_registry.append(
                f"{item_path}: registry entry is absent from the current repository manifest"
            )

    owner_counts = Counter(str(row["release_owner"]) for row in normalized)
    clearance_counts = Counter(str(row["clearance_state"]) for row in normalized)
    candidate_rows = [row for row in normalized if bool(row["release_candidate"])]
    candidate_bytes = sum(int(row["bytes"]) for row in candidate_rows)

    release_ready = not (
        blockers
        or registry_errors
        or stale_registry
        or pending_rows
        or rejected
    )
    if blockers or registry_errors or rejected:
        status = "blocked"
    elif stale_registry or pending_rows:
        status = "review-required"
    else:
        status = "verified"

    report = {
        "status": status,
        "manifest": str(manifest_path),
        "manifestSha256": sha256(manifest_path),
        "clearanceRegistry": str(registry_path),
        "clearanceRegistrySha256": sha256(registry_path),
        "rowCount": len(normalized),
        "inventoryRows": len(normalized),
        "totalBytes": total_bytes,
        "releaseCandidateRows": len(candidate_rows),
        "releaseCandidateBytes": candidate_bytes,
        "ownerCounts": dict(sorted(owner_counts.items())),
        "clearanceCounts": dict(sorted(clearance_counts.items())),
        "duplicateContentHashes": len(duplicate_hashes),
        "protectedSourceRuntimeDuplicateRows": len(protected_runtime_paths),
        "protectedRuntimeDuplicateCount": unresolved_protected_duplicates,
        "clearanceRequiredCount": required_count,
        "clearanceApprovedCount": approved_count,
        "clearancePendingCount": len(pending_rows),
        "clearanceRejectedCount": len(rejected),
        "registryEntryCount": len(registry),
        "registryErrorCount": len(registry_errors),
        "staleRegistryCount": len(stale_registry),
        "blockerCount": len(blockers) + len(rejected),
        "structuralBlockers": blockers,
        "registryErrors": registry_errors,
        "staleRegistryEntries": stale_registry[:100],
        "rejectedPaths": rejected[:100],
        "reviewWarnings": warnings[:100],
        "releaseReady": release_ready,
        "legalConclusion": (
            "not-provided; this audit verifies exact project registry coverage "
            "and does not substitute for legal review"
        ),
    }
    return report, normalized, pending_rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("manifest", type=pathlib.Path)
    parser.add_argument(
        "--clearance-registry",
        type=pathlib.Path,
        default=pathlib.Path("ROOT_docs/RELEASE_CLEARANCE.tsv"),
    )
    parser.add_argument("--ledger", type=pathlib.Path, required=True)
    parser.add_argument("--pending", type=pathlib.Path)
    parser.add_argument("--report", type=pathlib.Path, required=True)
    parser.add_argument(
        "--require-clearance",
        action="store_true",
        help="Fail unless every exact clearance-required hash is approved.",
    )
    args = parser.parse_args()
    try:
        report, ledger, pending = audit(
            args.manifest.resolve(),
            args.clearance_registry.resolve(),
        )
        write_tsv(args.ledger.resolve(), LEDGER_COLUMNS, ledger)
        if args.pending:
            write_tsv(args.pending.resolve(), PENDING_COLUMNS, pending)
        args.report.resolve().parent.mkdir(parents=True, exist_ok=True)
        args.report.resolve().write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
    except Exception as exc:  # noqa: BLE001 - one audit failure surface
        print(f"REPOSITORY RELEASE INVENTORY AUDIT FAILED: {exc}", file=sys.stderr)
        return 1

    print(json.dumps(report, indent=2, sort_keys=True))
    if report["blockerCount"] or report["registryErrorCount"]:
        return 1
    if args.require_clearance and not report["releaseReady"]:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
