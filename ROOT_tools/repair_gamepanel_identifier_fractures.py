#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_TARGETS = [
    ROOT / "src" / "mechanist" / "GamePanel.java",
    ROOT / "src" / "mechanist" / "BootSurfacePainter.java",
    ROOT / "src" / "mechanist" / "LoadingSurfacePainter.java",
]

# Known Java identifier fractures caused by catalog/surgery text handling or older hand edits.
# This is deliberately conservative: it fixes exact broken tokens that cannot compile.
EXACT_REPLACEMENTS = {
    "Faction.CIVIC WARDENS": "Faction.CIVIC_WARDENS",
    "Faction.CIVIC LEDGER OFFICE": "Faction.CIVIC_LEDGER_OFFICE",
    "Faction.IMPERIAL GUARD": "Faction.IMPERIAL_GUARD",
    "Faction.ROGUE MACHINE": "Faction.ROGUE_MACHINE",
    "Faction.TECH PRIEST": "Faction.TECH_PRIEST",
    "Faction.GENESTEALER CULT": "Faction.GENESTEALER_CULT",
    "Faction.NOBLE HOUSE VARN": "Faction.NOBLE_HOUSE_VARN",
    "Faction.NOBLE HOUSE KASTOR": "Faction.NOBLE_HOUSE_KASTOR",
    "Faction.NOBLE HOUSE MORVAIN": "Faction.NOBLE_HOUSE_MORVAIN",
    "Faction.NOBLE HOUSE CYRA": "Faction.NOBLE_HOUSE_CYRA",
    "Faction.NOBLE HOUSE DRAKE": "Faction.NOBLE_HOUSE_DRAKE",
    "Faction.NOBLE HOUSE TOLL": "Faction.NOBLE_HOUSE_TOLL",
    "Faction.NOBLE HOUSE OSSUARY": "Faction.NOBLE_HOUSE_OSSUARY",
    "faction.CIVIC WARDENS": "Faction.CIVIC_WARDENS",
    "faction.CIVIC LEDGER OFFICE": "Faction.CIVIC_LEDGER_OFFICE",
    "faction.IMPERIAL GUARD": "Faction.IMPERIAL_GUARD",
    "faction.ROGUE MACHINE": "Faction.ROGUE_MACHINE",
}

# Repair enum-style member references after Faction. when a phrase got split by spaces.
# Example: Faction.CIVIC WARDENS -> Faction.CIVIC_WARDENS
FACTION_FRACTURE_RE = re.compile(r"\bFaction\.([A-Z][A-Z0-9_]*)(?:\s+([A-Z][A-Z0-9_]*))+")

# Repair the specific lower-case typo seen in local compile output.
LOWER_FACTION_FRACTURE_RE = re.compile(r"\bfaction\.([A-Z][A-Z0-9_]*)(?:\s+([A-Z][A-Z0-9_]*))+")


def repair_text(text: str) -> tuple[str, int]:
    count = 0
    for bad, good in EXACT_REPLACEMENTS.items():
        n = text.count(bad)
        if n:
            text = text.replace(bad, good)
            count += n

    def faction_repl(match: re.Match[str]) -> str:
        nonlocal count
        count += 1
        return "Faction." + "_".join(match.group(0).split("Faction.", 1)[1].split())

    def lower_faction_repl(match: re.Match[str]) -> str:
        nonlocal count
        count += 1
        return "Faction." + "_".join(match.group(0).split("faction.", 1)[1].split())

    text = FACTION_FRACTURE_RE.sub(faction_repl, text)
    text = LOWER_FACTION_FRACTURE_RE.sub(lower_faction_repl, text)
    return text, count


def repair_file(path: Path, dry_run: bool) -> int:
    if not path.exists():
        return 0
    original = path.read_text(encoding="utf-8", errors="replace")
    repaired, count = repair_text(original)
    if count and not dry_run:
        path.write_text(repaired, encoding="utf-8", newline="\n")
    if count:
        action = "would repair" if dry_run else "repaired"
        print(f"[{action}] {count} identifier fracture(s) in {path.relative_to(ROOT)}")
    else:
        print(f"[ok] no identifier fractures found in {path.relative_to(ROOT)}")
    return count


def main() -> int:
    parser = argparse.ArgumentParser(description="Repair known whitespace-fractured Java identifiers after GamePanel surgery.")
    parser.add_argument("paths", nargs="*", help="Optional files to repair. Defaults to GamePanel and generated painters.")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    targets = [Path(p) for p in args.paths] if args.paths else DEFAULT_TARGETS
    normalized = []
    for p in targets:
        normalized.append(p if p.is_absolute() else ROOT / p)

    total = 0
    for path in normalized:
        total += repair_file(path, args.dry_run)
    print(f"Identifier fracture repair total: {total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
