#!/usr/bin/env python3
"""Patch the oversized world-generation UI text in TheMechanist.java.

This is a targeted bridge patch while GamePanel is still too large to safely edit
through ordinary connector line ranges. It performs conservative literal string
replacements only. It does not modify source art, assets, saves, or generated
packages.

Default mode is dry-run. Pass --apply to rewrite src/mechanist/TheMechanist.java.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass, asdict
from datetime import datetime, timezone
from pathlib import Path

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
JAVA_FILE = REPO_ROOT / "src/mechanist/TheMechanist.java"
REPORT_JSON = REPO_ROOT / "docs/worldgen_ui_compact_patch.json"


@dataclass
class ReplacementRecord:
    old: str
    new: str
    count: int


# Keep these to visible UI/help prose. These replacements are intentionally short
# so the fixed-size hand-drawn panels stop overrunning neighboring boxes.
REPLACEMENTS: list[tuple[str, str]] = [
    ("GENERATE HIVE WORLD", "GENERATE SPIRE SECTOR"),
    ("Generate Hive World", "Generate Spire Sector"),
    ("generate hive world", "generate spire sector"),
    ("HIVE WORLD", "SPIRE SECTOR"),
    ("Hive World", "Spire Sector"),
    ("hive world", "spire sector"),
    (
        "Simulation information stays left; controls stay right so low-resolution layouts do not trample themselves.",
        "Info left. Controls right. Compact layout enforced."
    ),
    (
        "Zone size: Standard (worldgen weight 600-760; dimensions derived, not raw 500+ layers)",
        "Zone size: Standard"
    ),
    (
        "Hoarder mode: OFF — Strength/Endurance carry limit",
        "Hoarder: OFF — STR/END carry limit"
    ),
    (
        "Hoarder mode: ON — personal inventory decoupled from Strength/Endurance",
        "Hoarder: ON — inventory decoupled"
    ),
    (
        "Simulation age runs additional history batches before world save/start. Higher values improve provenance ledgers but cost generation time. Hoarder mode decouples personal inventory from Strength and Endurance.",
        "Simulation age adds history batches before start. Higher values improve provenance but cost generation time."
    ),
    (
        "Configure a new hive world, then generate it before selecting a character.",
        "Configure a spire sector, then generate it before selecting a character."
    ),
    (
        "Configure a new Hive World, then generate it before selecting a character.",
        "Configure a new Spire Sector, then generate it before selecting a character."
    ),
    (
        "world save/start. Higher values improve provenance ledgers but cost generation time. Hoarder mode decouples",
        "start. Higher values improve provenance but cost generation time. Hoarder decouples"
    ),
    (
        "personal inventory from Strength and Endurance.",
        "inventory from STR/END."
    ),
]


def load_text() -> str:
    if not JAVA_FILE.exists():
        raise FileNotFoundError(f"Missing Java file: {JAVA_FILE}")
    return JAVA_FILE.read_text(encoding="utf-8", errors="replace")


def apply_replacements(text: str) -> tuple[str, list[ReplacementRecord]]:
    records: list[ReplacementRecord] = []
    new_text = text
    for old, new in REPLACEMENTS:
        count = new_text.count(old)
        if count:
            new_text = new_text.replace(old, new)
        records.append(ReplacementRecord(old, new, count))
    return new_text, records


def write_report(records: list[ReplacementRecord], apply: bool) -> None:
    REPORT_JSON.parent.mkdir(parents=True, exist_ok=True)
    REPORT_JSON.write_text(json.dumps({
        "schema": "mechanist.worldgen_ui_compact_patch.v1",
        "generated_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "apply": apply,
        "java_file": str(JAVA_FILE),
        "total_replacements": sum(record.count for record in records),
        "records": [asdict(record) for record in records],
    }, indent=2), encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Compact overlong worldgen UI strings and rename Hive World to Spire Sector.")
    parser.add_argument("--apply", action="store_true", help="Rewrite TheMechanist.java. Omit for dry-run.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    original = load_text()
    patched, records = apply_replacements(original)
    changed = patched != original
    if args.apply and changed:
        JAVA_FILE.write_text(patched, encoding="utf-8")
    write_report(records, args.apply)
    print(f"Java file: {JAVA_FILE}")
    print(f"Total replacements: {sum(record.count for record in records)}")
    for record in records:
        if record.count:
            print(f"  {record.count}x {record.old!r} -> {record.new!r}")
    print(f"Report: {REPORT_JSON}")
    if not args.apply:
        print("Dry run only. Re-run with --apply to rewrite the Java source.")
    elif not changed:
        print("No changes were required.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
