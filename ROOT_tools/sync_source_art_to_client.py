#!/usr/bin/env python3
"""Sync real source art payloads into the client runtime art root.

The current Java runtime resolves bundled art through an art root equivalent to:
  PACKAGE_client/assets/a/r

This tool copies actual files from ROOT_SRC_assets into that client art root. It
never creates pointer-only packages, README placeholders, or manifest stubs in
place of assets.

Default mode is dry-run. Pass --apply to copy files.
"""

from __future__ import annotations

import argparse
import csv
import json
import shutil
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
SOURCE_ROOT = REPO_ROOT / "ROOT_SRC_assets"
CLIENT_ART_ROOT = REPO_ROOT / "PACKAGE_client/assets/a/r"
DEFAULT_JSON = REPO_ROOT / "docs/source_art_client_sync.json"
DEFAULT_TSV = REPO_ROOT / "docs/source_art_client_sync.tsv"

PAYLOAD_EXTENSIONS = {
    ".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp",
    ".json", ".jsonl", ".tsv", ".csv", ".txt", ".properties", ".cfg"
}
SKIP_DIRS = {".git", "target", "build", "dist", "ROOT_RELEASE", "PACKAGE_client"}
POINTER_ONLY_NAMES = {"README.md", "README.txt", ".gitkeep", ".keep"}
EXPECTED_RUNTIME_MARKERS = [
    "source/Title/TITEL.png",
    "source/Background/Backdrop.png",
    "tiles/quality/low_32/cells",
]


@dataclass
class CopyRecord:
    source: str
    destination: str
    action: str
    size_bytes: int
    note: str = ""


@dataclass
class MarkerRecord:
    marker: str
    exists: bool
    path: str


def rel(path: Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def skip_path(path: Path) -> bool:
    return any(part in SKIP_DIRS for part in path.parts)


def is_payload(path: Path) -> bool:
    if not path.is_file():
        return False
    if path.name in POINTER_ONLY_NAMES:
        return False
    return path.suffix.lower() in PAYLOAD_EXTENSIONS


def find_art_pack_roots() -> list[Path]:
    """Find source subtrees that already look like runtime art roots."""
    roots: set[Path] = set()
    if not SOURCE_ROOT.exists():
        return []
    for title in SOURCE_ROOT.rglob("TITEL.png"):
        if skip_path(title):
            continue
        parts = title.parts
        # .../<root>/source/Title/TITEL.png
        if len(parts) >= 3 and parts[-3].lower() == "source" and parts[-2].lower() == "title":
            roots.add(title.parents[2])
    for quality in SOURCE_ROOT.rglob("quality"):
        if skip_path(quality):
            continue
        if quality.is_dir() and (quality / "low_32" / "cells").is_dir():
            # .../<root>/tiles/quality
            if quality.parent.name.lower() == "tiles":
                roots.add(quality.parents[1])
    return sorted(roots, key=lambda p: (len(p.parts), str(p)))


def collect_files_from_roots(roots: list[Path]) -> list[Path]:
    files: list[Path] = []
    seen: set[Path] = set()
    for root in roots:
        for path in sorted(root.rglob("*")):
            if path in seen or skip_path(path):
                continue
            if is_payload(path):
                seen.add(path)
                files.append(path)
    return files


def collect_fallback_files() -> list[Path]:
    if not SOURCE_ROOT.exists():
        return []
    files: list[Path] = []
    for path in sorted(SOURCE_ROOT.rglob("*")):
        if skip_path(path):
            continue
        if is_payload(path):
            files.append(path)
    return files


def destination_for(source: Path, roots: list[Path]) -> Path:
    for root in roots:
        try:
            relative = source.relative_to(root)
            return CLIENT_ART_ROOT / relative
        except ValueError:
            continue
    return CLIENT_ART_ROOT / "source_imported" / source.relative_to(SOURCE_ROOT)


def sync(apply: bool, overwrite: bool) -> tuple[list[CopyRecord], list[Path]]:
    roots = find_art_pack_roots()
    files = collect_files_from_roots(roots) if roots else collect_fallback_files()
    if not files:
        raise RuntimeError(f"No source art payload files found under {SOURCE_ROOT}")
    records: list[CopyRecord] = []
    for source in files:
        destination = destination_for(source, roots)
        size = source.stat().st_size
        if destination.exists() and not overwrite:
            records.append(CopyRecord(rel(source), rel(destination), "skipped", size, "destination_exists"))
            continue
        action = "copied" if apply else "would_copy"
        note = "runtime_art_root_sync" if roots else "fallback_source_imported_sync"
        records.append(CopyRecord(rel(source), rel(destination), action, size, note))
        if apply:
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
    return records, roots


def marker_records() -> list[MarkerRecord]:
    records: list[MarkerRecord] = []
    for marker in EXPECTED_RUNTIME_MARKERS:
        path = CLIENT_ART_ROOT / marker
        records.append(MarkerRecord(marker=marker, exists=path.exists(), path=rel(path)))
    return records


def write_reports(records: list[CopyRecord], roots: list[Path], markers: list[MarkerRecord], output_json: Path, output_tsv: Path, apply: bool) -> None:
    payload = {
        "schema": "mechanist.source_art_client_sync.v1",
        "generated_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "apply": apply,
        "source_root": str(SOURCE_ROOT),
        "client_art_root": str(CLIENT_ART_ROOT),
        "detected_art_roots": [rel(root) for root in roots],
        "record_count": len(records),
        "copied_count": sum(1 for r in records if r.action == "copied"),
        "would_copy_count": sum(1 for r in records if r.action == "would_copy"),
        "skipped_count": sum(1 for r in records if r.action == "skipped"),
        "runtime_markers": [asdict(marker) for marker in markers],
        "records": [asdict(record) for record in records],
    }
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    with output_tsv.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=["source", "destination", "action", "size_bytes", "note"], delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for record in records:
            writer.writerow(asdict(record))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Copy real source art payloads into PACKAGE_client/assets/a/r.")
    parser.add_argument("--apply", action="store_true", help="Actually copy files. Omit for dry-run.")
    parser.add_argument("--overwrite", action="store_true", help="Overwrite existing client art files.")
    parser.add_argument("--output-json", default=str(DEFAULT_JSON))
    parser.add_argument("--output-tsv", default=str(DEFAULT_TSV))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    records, roots = sync(args.apply, args.overwrite)
    markers = marker_records()
    write_reports(records, roots, markers, Path(args.output_json).resolve(), Path(args.output_tsv).resolve(), args.apply)
    print(f"Detected art roots: {len(roots)}")
    for root in roots[:10]:
        print(f"  {rel(root)}")
    print(f"Source art rows: {len(records)}")
    print(f"Copied:       {sum(1 for r in records if r.action == 'copied')}")
    print(f"Would copy:   {sum(1 for r in records if r.action == 'would_copy')}")
    print(f"Skipped:      {sum(1 for r in records if r.action == 'skipped')}")
    print("Runtime markers:")
    for marker in markers:
        print(f"  {'OK' if marker.exists else 'MISSING'} {marker.marker} -> {marker.path}")
    if not args.apply:
        print("Dry run only. Re-run with --apply to copy files into the client runtime art root.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
