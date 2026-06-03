#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC_ROOT = ROOT / "src"

# Known Java identifier fractures caused by catalog/surgery text handling or older hand edits.
# These replacements are applied only outside comments and string/char literals.
EXACT_REPLACEMENTS = {
    "Faction.CIVIC WARDENS": "Faction.CIVIC_WARDENS",
    "Faction.CIVIC LEDGER OFFICE": "Faction.CIVIC_LEDGER_OFFICE",
    "Faction.MECHANIST COLLEGIA": "Faction.MECHANIST_COLLEGIA",
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
    "faction.MECHANIST COLLEGIA": "Faction.MECHANIST_COLLEGIA",
    "faction.IMPERIAL GUARD": "Faction.IMPERIAL_GUARD",
    "faction.ROGUE MACHINE": "Faction.ROGUE_MACHINE",
    "CONTAINER FACTION STOCK PREFIX": "CONTAINER_FACTION_STOCK_PREFIX",
    "CONTAINER FACTION STORAGE PREFIX": "CONTAINER_FACTION_STORAGE_PREFIX",
    "CONTAINER FACTION STOCK": "CONTAINER_FACTION_STOCK",
}

BARE_ENUM_PHRASES = (
    "CIVIC LEDGER OFFICE",
    "CIVIC WARDENS",
    "MECHANIST COLLEGIA",
    "IMPERIAL GUARD",
    "ROGUE MACHINE",
    "TECH PRIEST",
    "GENESTEALER CULT",
)

FACTION_FRACTURE_RE = re.compile(r"\bFaction\.([A-Z][A-Z0-9_]*(?:\s+[A-Z][A-Z0-9_]*)+)")
LOWER_FACTION_FRACTURE_RE = re.compile(r"\bfaction\.([A-Z][A-Z0-9_]*(?:\s+[A-Z][A-Z0-9_]*)+)")
BARE_ENUM_PHRASE_RE = re.compile(r"\b(" + "|".join(re.escape(p) for p in BARE_ENUM_PHRASES) + r")\b")
UPPER_CONSTANT_MEMBER_RE = re.compile(
    r"\b([A-Z][A-Z0-9_]*(?:\s+[A-Z][A-Z0-9_]*){1,5})(?=\s*(?:\+|\.|\(|;|,|\)|\]|==|!=|<=|>=|<|>|&&|\|\|))"
)

SAFE_UPPER_CONSTANT_PHRASE_PREFIXES = (
    "CONTAINER ",
    "ITEM ",
    "NPC ",
    "WORLD ",
    "ZONE ",
    "SECTOR ",
    "FACTION ",
    "TILE ",
    "ROAD ",
    "ROOM ",
    "BUILD ",
    "SAVE ",
    "LOAD ",
)


@dataclass(frozen=True)
class Replacement:
    start: int
    end: int
    value: str


def mask_java(text: str) -> str:
    """Replace comments and literals with spaces while preserving offsets/newlines."""
    c = list(text)
    i = 0
    n = len(c)
    while i < n:
        ch = c[i]
        nx = c[i + 1] if i + 1 < n else ""
        nx2 = c[i + 2] if i + 2 < n else ""

        if ch == "/" and nx == "/":
            while i < n and c[i] != "\n":
                c[i] = " "
                i += 1
            continue

        if ch == "/" and nx == "*":
            c[i] = c[i + 1] = " "
            i += 2
            while i + 1 < n and not (c[i] == "*" and c[i + 1] == "/"):
                if c[i] not in "\r\n":
                    c[i] = " "
                i += 1
            if i + 1 < n:
                c[i] = c[i + 1] = " "
                i += 2
            continue

        if ch == '"' and nx == '"' and nx2 == '"':
            c[i] = c[i + 1] = c[i + 2] = " "
            i += 3
            while i + 2 < n and not (c[i] == '"' and c[i + 1] == '"' and c[i + 2] == '"'):
                if c[i] not in "\r\n":
                    c[i] = " "
                i += 1
            if i + 2 < n:
                c[i] = c[i + 1] = c[i + 2] = " "
                i += 3
            continue

        if ch in ('"', "'"):
            quote = ch
            c[i] = " "
            i += 1
            escaped = False
            while i < n:
                cur = c[i]
                if cur not in "\r\n":
                    c[i] = " "
                if escaped:
                    escaped = False
                elif cur == "\\":
                    escaped = True
                elif cur == quote:
                    i += 1
                    break
                i += 1
            continue

        i += 1
    return "".join(c)


def schedule_exact(masked: str, replacements: list[Replacement]) -> None:
    for bad, good in EXACT_REPLACEMENTS.items():
        for match in re.finditer(re.escape(bad), masked):
            replacements.append(Replacement(match.start(), match.end(), good))


def schedule_regex(masked: str, replacements: list[Replacement]) -> None:
    for match in FACTION_FRACTURE_RE.finditer(masked):
        replacements.append(Replacement(match.start(), match.end(), "Faction." + "_".join(match.group(1).split())))

    for match in LOWER_FACTION_FRACTURE_RE.finditer(masked):
        replacements.append(Replacement(match.start(), match.end(), "Faction." + "_".join(match.group(1).split())))

    for match in BARE_ENUM_PHRASE_RE.finditer(masked):
        replacements.append(Replacement(match.start(), match.end(), "_".join(match.group(1).split())))

    for match in UPPER_CONSTANT_MEMBER_RE.finditer(masked):
        phrase = match.group(1)
        if phrase.startswith(SAFE_UPPER_CONSTANT_PHRASE_PREFIXES):
            replacements.append(Replacement(match.start(), match.end(), "_".join(phrase.split())))


def apply_replacements(text: str, replacements: list[Replacement]) -> tuple[str, int]:
    if not replacements:
        return text, 0

    replacements.sort(key=lambda r: (r.start, -(r.end - r.start)))
    selected: list[Replacement] = []
    last_end = -1
    for repl in replacements:
        if repl.start < last_end:
            continue
        selected.append(repl)
        last_end = repl.end

    out: list[str] = []
    cursor = 0
    changed = 0
    for repl in selected:
        if text[repl.start:repl.end] == repl.value:
            continue
        out.append(text[cursor:repl.start])
        out.append(repl.value)
        cursor = repl.end
        changed += 1
    out.append(text[cursor:])
    return "".join(out), changed


def repair_text(text: str) -> tuple[str, int]:
    masked = mask_java(text)
    replacements: list[Replacement] = []
    schedule_exact(masked, replacements)
    schedule_regex(masked, replacements)
    return apply_replacements(text, replacements)


def discover_default_targets() -> list[Path]:
    if not SRC_ROOT.exists():
        return []
    return sorted(p for p in SRC_ROOT.rglob("*.java") if p.is_file())


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
    return count


def main() -> int:
    parser = argparse.ArgumentParser(description="Repair known whitespace-fractured Java identifiers after GamePanel surgery.")
    parser.add_argument("paths", nargs="*", help="Optional files to repair. Defaults to every Java source file under src/.")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    targets = [Path(p) for p in args.paths] if args.paths else discover_default_targets()
    normalized = [p if p.is_absolute() else ROOT / p for p in targets]

    total = 0
    scanned = 0
    for path in normalized:
        scanned += 1
        total += repair_file(path, args.dry_run)
    print(f"Identifier fracture repair scanned files: {scanned}")
    print(f"Identifier fracture repair total: {total}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
