#!/usr/bin/env python3
"""Move MP3 source/export files out of the client runtime package.

Runtime client audio should be self-contained and ready to play, but source/export
formats that the Java runtime does not directly use should not live inside the
client package. This tool moves .mp3 files from PACKAGE_client/assets/sound into
ROOT_SRC_assets/ROOT_Sounds while preserving the relative sound folder layout.

No pointer files are created. No README placeholders are created.

Default mode is dry-run. Pass --apply to move files.
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
CLIENT_SOUND_ROOT = REPO_ROOT / "PACKAGE_client/assets/sound"
SOURCE_SOUND_ROOT = REPO_ROOT / "ROOT_SRC_assets/ROOT_Sounds"
DEFAULT_JSON = REPO_ROOT / "docs/client_mp3_move_audit.json"
DEFAULT_TSV = REPO_ROOT / "docs/client_mp3_move_audit.tsv"


@dataclass
class MoveRecord:
    source: str
    destination: str
    action: str
    size_bytes: int
    note: str = ""


def rel(path: Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def discover_mp3s() -> list[Path]:
    if not CLIENT_SOUND_ROOT.exists():
        return []
    return sorted(path for path in CLIENT_SOUND_ROOT.rglob("*.mp3") if path.is_file())


def destination_for(source: Path) -> Path:
    relative = source.relative_to(CLIENT_SOUND_ROOT)
    return SOURCE_SOUND_ROOT / relative


def move_mp3s(apply: bool, overwrite: bool) -> list[MoveRecord]:
    records: list[MoveRecord] = []
    for source in discover_mp3s():
        destination = destination_for(source)
        size = source.stat().st_size
        if destination.exists() and not overwrite:
            records.append(MoveRecord(rel(source), rel(destination), "skipped", size, "destination_exists"))
            continue
        action = "moved" if apply else "would_move"
        records.append(MoveRecord(rel(source), rel(destination), action, size))
        if apply:
            destination.parent.mkdir(parents=True, exist_ok=True)
            if destination.exists() and overwrite:
                destination.unlink()
            shutil.move(str(source), str(destination))
    if apply:
        prune_empty_dirs(CLIENT_SOUND_ROOT)
    return records


def prune_empty_dirs(root: Path) -> None:
    if not root.exists():
        return
    for path in sorted((p for p in root.rglob("*") if p.is_dir()), key=lambda p: len(p.parts), reverse=True):
        try:
            path.rmdir()
        except OSError:
            pass


def write_reports(records: list[MoveRecord], output_json: Path, output_tsv: Path, apply: bool) -> None:
    payload = {
        "schema": "mechanist.client_mp3_move_audit.v1",
        "generated_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "apply": apply,
        "client_sound_root": str(CLIENT_SOUND_ROOT),
        "source_sound_root": str(SOURCE_SOUND_ROOT),
        "record_count": len(records),
        "moved_count": sum(1 for record in records if record.action == "moved"),
        "would_move_count": sum(1 for record in records if record.action == "would_move"),
        "skipped_count": sum(1 for record in records if record.action == "skipped"),
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
    parser = argparse.ArgumentParser(description="Move .mp3 source/export files from PACKAGE_client/assets/sound to ROOT_SRC_assets/ROOT_Sounds.")
    parser.add_argument("--apply", action="store_true", help="Actually move files. Omit for dry-run.")
    parser.add_argument("--overwrite", action="store_true", help="Overwrite existing destination MP3 files when applying.")
    parser.add_argument("--output-json", default=str(DEFAULT_JSON))
    parser.add_argument("--output-tsv", default=str(DEFAULT_TSV))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    records = move_mp3s(args.apply, args.overwrite)
    output_json = Path(args.output_json).resolve()
    output_tsv = Path(args.output_tsv).resolve()
    write_reports(records, output_json, output_tsv, args.apply)
    print(f"MP3 files found in client sound root: {len(records)}")
    print(f"Moved:      {sum(1 for record in records if record.action == 'moved')}")
    print(f"Would move: {sum(1 for record in records if record.action == 'would_move')}")
    print(f"Skipped:    {sum(1 for record in records if record.action == 'skipped')}")
    print(f"Wrote JSON: {output_json}")
    print(f"Wrote TSV:  {output_tsv}")
    if not args.apply:
        print("Dry run only. Re-run with --apply to move files.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
