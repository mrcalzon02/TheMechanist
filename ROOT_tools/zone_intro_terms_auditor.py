#!/usr/bin/env python3
"""Audit and optionally rewrite borrowed terms in player-facing zone intro text.

This is targeted at zone/introduction prose only. It avoids docs, tools, build
outputs, and source-asset folders. Default mode is audit-only; pass --apply to
rewrite matching text.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
DEFAULT_JSON = REPO_ROOT / "docs/zone_intro_terms_audit.json"
DEFAULT_TSV = REPO_ROOT / "docs/zone_intro_terms_audit.tsv"
SCAN_ROOTS = [REPO_ROOT / "src", REPO_ROOT / "PACKAGE_client/assets", REPO_ROOT / "config"]
SKIP_DIRS = {".git", "target", "build", "dist", "ROOT_RELEASE", "ROOT_tools", "docs", "ROOT_SRC_assets"}
TEXT_EXTENSIONS = {".java", ".json", ".txt", ".md", ".tsv", ".csv", ".properties", ".cfg"}

REPLACEMENTS = {
    "Imperium": "Concord",
    "Imperial": "Concord",
    "Mechanicus": "Mechanist Collegia",
    "Administratum": "Civic Ledger Office",
    "Arbites": "Civic Wardens",
    "Inquisition": "Concord Inquiry Office",
    "Inquisitor": "Concord Inquiry Officer",
    "Cogitator": "Logic Engine",
    "Servitor": "Bound Labor Automaton",
    "Hive City": "Arcology Stack",
    "Hive": "Arcology",
    "Lasgun": "Light Rifle",
    "Bolter": "Mass-Reactive Carbine",
    "Promethium": "Industrial Fuelgel",
}

PATTERNS = [(term, repl, re.compile(r"\b" + re.escape(term) + r"\b", re.IGNORECASE)) for term, repl in sorted(REPLACEMENTS.items(), key=lambda x: len(x[0]), reverse=True)]
ZONE_HINTS = ("zone", "intro", "introduction", "arrival", "district", "sector", "plaza")


@dataclass
class Hit:
    path: str
    line: int
    term: str
    replacement: str
    context: str
    action: str


def rel(path: Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def skip(path: Path) -> bool:
    return any(part in SKIP_DIRS for part in path.parts)


def candidates() -> Iterable[Path]:
    seen: set[Path] = set()
    for root in SCAN_ROOTS:
        if not root.exists():
            continue
        for path in sorted(root.rglob("*")):
            if path in seen or skip(path):
                continue
            if path.is_file() and path.suffix.lower() in TEXT_EXTENSIONS:
                seen.add(path)
                yield path


def likely_zone_intro(path: Path, text: str) -> bool:
    path_text = rel(path).lower()
    if any(hint in path_text for hint in ZONE_HINTS):
        return True
    sample = text[:12000].lower()
    return sum(1 for hint in ZONE_HINTS if hint in sample) >= 2


def keep_case(original: str, replacement: str) -> str:
    if original.isupper():
        return replacement.upper()
    if original[:1].islower():
        return replacement[:1].lower() + replacement[1:]
    return replacement


def process_file(path: Path, apply: bool) -> list[Hit]:
    try:
        text = path.read_text(encoding="utf-8-sig", errors="replace")
    except Exception:
        return []
    if "\x00" in text or not likely_zone_intro(path, text):
        return []
    hits: list[Hit] = []
    output: list[str] = []
    changed = False
    for line_no, line in enumerate(text.splitlines(keepends=True), start=1):
        new_line = line
        for term, replacement, pattern in PATTERNS:
            def repl(match: re.Match[str]) -> str:
                nonlocal changed
                changed = True
                value = keep_case(match.group(0), replacement)
                hits.append(Hit(rel(path), line_no, match.group(0), value, line.strip()[:260], "replaced" if apply else "would_replace"))
                return value
            new_line = pattern.sub(repl, new_line)
        output.append(new_line)
    if apply and changed:
        path.write_text("".join(output), encoding="utf-8")
    return hits


def write_reports(hits: list[Hit], output_json: Path, output_tsv: Path, apply: bool) -> None:
    output_json.parent.mkdir(parents=True, exist_ok=True)
    output_json.write_text(json.dumps({
        "schema": "mechanist.zone_intro_terms_audit.v1",
        "generated_utc": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "apply": apply,
        "hit_count": len(hits),
        "replacements": REPLACEMENTS,
        "hits": [asdict(hit) for hit in hits],
    }, indent=2), encoding="utf-8")
    with output_tsv.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=["path", "line", "term", "replacement", "context", "action"], delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for hit in hits:
            writer.writerow(asdict(hit))


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Audit/rewrite borrowed terms in zone intro text.")
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--output-json", default=str(DEFAULT_JSON))
    parser.add_argument("--output-tsv", default=str(DEFAULT_TSV))
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    hits: list[Hit] = []
    for path in candidates():
        hits.extend(process_file(path, args.apply))
    write_reports(hits, Path(args.output_json).resolve(), Path(args.output_tsv).resolve(), args.apply)
    print(f"Zone intro term hits: {len(hits)}")
    print(f"Action: {'replaced' if args.apply else 'audit only'}")
    print(f"Wrote JSON: {Path(args.output_json).resolve()}")
    print(f"Wrote TSV:  {Path(args.output_tsv).resolve()}")
    if not args.apply:
        print("Dry run only. Review TSV before running with --apply.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
