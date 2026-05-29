#!/usr/bin/env python3
"""Sync real source art payloads into the client runtime art root.

The Java runtime resolves bundled art through an art root equivalent to:
  PACKAGE_client/assets/a/r

The current source-art tree is expected to contain a root like:
  ROOT_SRC_assets/Mechanist_art_SRC_do_not_MODIFY/Mechanist art/
    Background/
    Faction/
    TILES/
    Title/

This tool copies actual files from that source tree into the client runtime tree
without modifying the source-art tree and without creating pointer files.

Runtime mapping:
  Background/ -> PACKAGE_client/assets/a/r/source/Background/
  Title/      -> PACKAGE_client/assets/a/r/source/Title/
  Faction/    -> PACKAGE_client/assets/a/r/source/Faction/
  TILES/      -> PACKAGE_client/assets/a/r/tiles/

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
PREFERRED_SOURCE_ART_ROOT = SOURCE_ROOT / "Mechanist_art_SRC_do_not_MODIFY" / "Mechanist art"
CLIENT_ART_ROOT = REPO_ROOT / "PACKAGE_client/assets/a/r"
DEFAULT_JSON = REPO_ROOT / "docs/source_art_client_sync.json"
DEFAULT_TSV = REPO_ROOT / "docs/source_art_client_sync.tsv"

PAYLOAD_EXTENSIONS = {
    ".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp",
    ".json", ".jsonl", ".tsv", ".csv", ".txt", ".properties", ".cfg"
}
POINTER_ONLY_NAMES = {"README.md", "README.txt", ".gitkeep", ".keep"}
SKIP_DIRS = {".git", "target", "build", "dist", "ROOT_RELEASE", "PACKAGE_client"}

# Runtime files the Java loader already requests by exact path/key.
EXPECTED_RUNTIME_MARKERS = [
    "source/Title/TITEL.png",
    "source/Title/Sub title.png",
    "source/Background/Backdrop.png",
    "source/Background/CLOUDS1slow.png",
    "source/Background/Clouds2fast.png",
]

RUNTIME_FOLDER_MAP = {
    "Background": Path("source/Background"),
    "Title": Path("source/Title"),
    "Faction": Path("source/Faction"),
    "TILES": Path("tiles"),
    "Tiles": Path("tiles"),
    "tiles": Path("tiles"),
}


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


def has_mechanist_art_shape(path: Path) -> bool:
    if not path.is_dir():
        return False
    names = {child.name.lower() for child in path.iterdir() if child.is_dir()}
    return {"background", "title"}.issubset(names) and ("tiles" in names or "tiles".lower() in names)


def find_source_art_root(explicit: str | None) -> Path:
    if explicit:
        path = Path(explicit).expanduser().resolve()
        if not path.is_dir():
            raise FileNotFoundError(f"Explicit source art root is not a directory: {path}")
        return path
    if PREFERRED_SOURCE_ART_ROOT.is_dir():
        return PREFERRED_SOURCE_ART_ROOT
    if not SOURCE_ROOT.is_dir():
        raise FileNotFoundError(f"Source root does not exist: {SOURCE_ROOT}")
    candidates: list[Path] = []
    for path in SOURCE_ROOT.rglob("*"):
        if skip_path(path):
            continue
        if path.is_dir() and path.name.lower() == "mechanist art" and has_mechanist_art_shape(path):
            candidates.append(path)
    if not candidates:
        for path in SOURCE_ROOT.rglob("*"):
            if skip_path(path):
                continue
            if path.is_dir() and has_mechanist_art_shape(path):
                candidates.append(path)
    if not candidates:
        raise RuntimeError(
            "Could not find a Mechanist source art root with Background, Title, and TILES folders under "
            f"{SOURCE_ROOT}. Expected: {PREFERRED_SOURCE_ART_ROOT}"
        )
    candidates.sort(key=lambda p: (0 if "do_not" in str(p).lower() or "donot" in str(p).lower() else 1, len(p.parts), str(p)))
    return candidates[0]


def collect_files(source_art_root: Path) -> list[Path]:
    files: list[Path] = []
    for folder_name in RUNTIME_FOLDER_MAP:
        folder = source_art_root / folder_name
        if not folder.is_dir():
            continue
        for path in sorted(folder.rglob("*")):
            if skip_path(path):
                continue
            if is_payload(path):
                files.append(path)
    if not files:
        raise RuntimeError(f"No art payload files found in mapped folders below {source_art_root}")
    return files


def destination_for(source: Path, source_art_root: Path) -> Path:
    relative = source.relative_to(source_art_root)
    top = relative.parts[0]
    mapped = RUNTIME_FOLDER_MAP.get(top, Path("source_imported") / top)
    return CLIENT_ART_ROOT / mapped / Path(*relative.parts[1:])


def sync(source_art_root: Path, apply: bool, overwrite: bool) -> list[CopyRecord]:
    records: list[CopyRecord] = []
    for source in collect_files(source_art_root):
        destination = destination_for(source, source_art_root)
        size = source.stat().st_size
        if destination.exists() and not overwrite:
            records.append(CopyRecord(rel(source), rel(destination), "skipped", size, "destination_exists"))
            continue
        action = "copied" if apply else "would_copy"
        records.append(CopyRecord(rel(source), rel(destination), action, size, "mechanist_source_art_runtime_sync"))
        if apply:
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)
    return records


def marker_records() -> list[MarkerRecord]:
    records: list[MarkerRecord] = []
    for marker in EXPECTED_RUNTIME_MARKERS:
        path = CLIENT_ART_ROOT / marker
        records.append(MarkerRecord(marker=marker, exists=path.exists(), path=rel(path)))
    return records


def write_reports(records: list[CopyRecord], source_art_root: Path, markers: list[MarkerRecord], output_json: Path, output_tsv: Path, apply: bool) -> None:
    output_json.parent.mkdir(parents=True, exist_ok=True)
    payload = {
        "schema": "mechanist.source_art_client_sync.v2",
        "generated_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "apply": apply,
        "source_art_root": rel(source_art_root),
        "client_art_root": rel(CLIENT_ART_ROOT),
        "record_count": len(records),
        "copied_count": sum(1 for r in records if r.action == "copied"),
        "would_copy_count": sum(1 for r in records if r.action == "would_copy"),
        "skipped_count": sum(1 for r in records if r.action == "skipped"),
        "missing_marker_count": sum(1 for m in markers if not m.exists),
        "runtime_markers": [asdict(marker) for marker in markers],
        "records": [asdict(record) for record in records],
    }
    output_json.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    with output_tsv.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=["source", "destination", "action", "size_bytes", "note"], delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for record in records:
            writer.writerow(asdict(record))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Copy real Mechanist source art payloads into PACKAGE_client/assets/a/r.")
    parser.add_argument("--source-art-root", default="", help="Optional explicit source art root. Example: ROOT_SRC_assets/Mechanist_art_SRC_do_not_MODIFY/Mechanist art")
    parser.add_argument("--apply", action="store_true", help="Actually copy files. Omit for dry-run.")
    parser.add_argument("--overwrite", action="store_true", help="Overwrite existing client art files.")
    parser.add_argument("--output-json", default=str(DEFAULT_JSON))
    parser.add_argument("--output-tsv", default=str(DEFAULT_TSV))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    source_art_root = find_source_art_root(args.source_art_root or None)
    records = sync(source_art_root, args.apply, args.overwrite)
    markers = marker_records()
    write_reports(records, source_art_root, markers, Path(args.output_json).resolve(), Path(args.output_tsv).resolve(), args.apply)
    print(f"Source art root: {rel(source_art_root)}")
    print(f"Client art root: {rel(CLIENT_ART_ROOT)}")
    print(f"Source art rows: {len(records)}")
    print(f"Copied:       {sum(1 for r in records if r.action == 'copied')}")
    print(f"Would copy:   {sum(1 for r in records if r.action == 'would_copy')}")
    print(f"Skipped:      {sum(1 for r in records if r.action == 'skipped')}")
    print("Runtime markers:")
    for marker in markers:
        print(f"  {'OK' if marker.exists else 'MISSING'} {marker.marker} -> {marker.path}")
    if not args.apply:
        print("Dry run only. Re-run with --apply to copy files into the client runtime art root.")
    if any(not marker.exists for marker in markers) and args.apply:
        raise SystemExit("One or more required runtime art markers are still missing after sync.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
