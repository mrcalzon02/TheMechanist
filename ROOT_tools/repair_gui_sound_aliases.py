#!/usr/bin/env python3
"""Repair missing GUI sound literals by copying existing WAV payloads.

The Java runtime currently expects seven GUI/bootstrap sound files under:
  PACKAGE_client/assets/sound/wav/

This tool does not create placeholders and does not write pointer files. It only
copies real existing .wav files from PACKAGE_client/assets/sound into the exact
runtime paths. Default mode is dry-run. Pass --apply to copy files.
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
DEST_ROOT = CLIENT_SOUND_ROOT / "wav"
DEFAULT_JSON = REPO_ROOT / "docs/gui_sound_alias_repair.json"
DEFAULT_TSV = REPO_ROOT / "docs/gui_sound_alias_repair.tsv"

# Ordered candidate basenames. First existing match wins.
REPAIRS: dict[str, list[str]] = {
    "tp_gui_button_press_01.wav": ["Command-key.wav", "Clank.wav", "Clankingkeys.wav", "Cathonk.wav"],
    "tp_gui_panel_open_01.wav": ["Book flip.wav", "Book snap.wav", "Door open.wav", "Clank.wav"],
    "tp_gui_panel_close_01.wav": ["Book close.wav", "Door close.wav", "Book snap.wav", "Cathonk.wav"],
    "tp_gui_portrait_select_01.wav": ["Cathonk.wav", "Clank.wav", "Book snap.wav"],
    "tp_gui_tab_change_01.wav": ["Book snap.wav", "Book flip.wav", "Command-key.wav", "Clank.wav"],
    "typing_sounds.wav": ["Clankingkeys.wav", "Command-key.wav", "Clank.wav"],
    "machine_start.wav": ["machine_start.wav", "Machine start.wav", "A_car_engine_#4-1779775163346.wav", "Clank.wav"],
}


@dataclass
class RepairRecord:
    target: str
    source: str
    action: str
    note: str


def rel(path: Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def build_wav_index() -> dict[str, list[Path]]:
    index: dict[str, list[Path]] = {}
    if not CLIENT_SOUND_ROOT.exists():
        return index
    for path in sorted(CLIENT_SOUND_ROOT.rglob("*.wav")):
        if path.is_file():
            index.setdefault(path.name.lower(), []).append(path)
    return index


def choose_source(candidates: list[str], index: dict[str, list[Path]]) -> Path | None:
    for candidate in candidates:
        hits = index.get(candidate.lower())
        if hits:
            return hits[0]
    return None


def repair(apply: bool, overwrite: bool) -> list[RepairRecord]:
    index = build_wav_index()
    records: list[RepairRecord] = []
    for target_name, candidates in REPAIRS.items():
        target = DEST_ROOT / target_name
        if target.exists() and not overwrite:
            records.append(RepairRecord(rel(target), rel(target), "already_exists", "target already present"))
            continue
        source = choose_source(candidates, index)
        if source is None:
            records.append(RepairRecord(rel(target), "", "missing_source", "no configured source candidate exists in client sound tree"))
            continue
        action = "copied" if apply else "would_copy"
        records.append(RepairRecord(rel(target), rel(source), action, "copied real existing wav payload"))
        if apply:
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)
    return records


def write_reports(records: list[RepairRecord], output_json: Path, output_tsv: Path, apply: bool) -> None:
    payload = {
        "schema": "mechanist.gui_sound_alias_repair.v1",
        "generated_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "apply": apply,
        "record_count": len(records),
        "copied_count": sum(1 for record in records if record.action == "copied"),
        "would_copy_count": sum(1 for record in records if record.action == "would_copy"),
        "missing_source_count": sum(1 for record in records if record.action == "missing_source"),
        "records": [asdict(record) for record in records],
    }
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(json.dumps(payload, indent=2), encoding="utf-8")
    with output_tsv.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=["target", "source", "action", "note"], delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for record in records:
            writer.writerow(asdict(record))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Copy existing WAV payloads into missing GUI sound literal paths.")
    parser.add_argument("--apply", action="store_true", help="Actually copy files. Omit for dry-run.")
    parser.add_argument("--overwrite", action="store_true", help="Overwrite existing target files.")
    parser.add_argument("--output-json", default=str(DEFAULT_JSON))
    parser.add_argument("--output-tsv", default=str(DEFAULT_TSV))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    records = repair(args.apply, args.overwrite)
    write_reports(records, Path(args.output_json).resolve(), Path(args.output_tsv).resolve(), args.apply)
    print(f"GUI sound repair rows: {len(records)}")
    print(f"Copied:       {sum(1 for record in records if record.action == 'copied')}")
    print(f"Would copy:   {sum(1 for record in records if record.action == 'would_copy')}")
    print(f"Missing src:  {sum(1 for record in records if record.action == 'missing_source')}")
    if not args.apply:
        print("Dry run only. Re-run with --apply to copy real WAV files into expected runtime paths.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
