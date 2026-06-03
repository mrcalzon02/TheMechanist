#!/usr/bin/env python3
"""
BUILD_LEGACY_PANEL_REFERENCE_LEDGER.py

Builds a retirement ledger for lingering references to the temporary GamePanel
compatibility bridge.  This keeps the bridge honest: every old panel dependency
should eventually be replaced by a narrower context/manager.

Run from repository root:

    py -3 scripts\BUILD_LEGACY_PANEL_REFERENCE_LEDGER.py --apply

Outputs:

    ROOT_docs/functionmap/generated/LEGACY_PANEL_REFERENCE_LEDGER.tsv
    ROOT_docs/functionmap/generated/LEGACY_PANEL_REFERENCE_SUMMARY.md
"""

from __future__ import annotations

import argparse
import dataclasses
import datetime as dt
import re
from collections import Counter
from pathlib import Path
from typing import Iterable, List, Optional, Sequence

ROOT = Path.cwd()
SRC_ROOT = ROOT / "src"
OUT_DIR = ROOT / "ROOT_docs" / "functionmap" / "generated"

REF_RE = re.compile(r"\bGamePanel\b|\bpanel\.[A-Za-z_][A-Za-z0-9_]*|\bgame\.[A-Za-z_][A-Za-z0-9_]*")
MEMBER_RE = re.compile(r"\b(?:panel|game)\.([A-Za-z_][A-Za-z0-9_]*)")

@dataclasses.dataclass(frozen=True)
class LegacyPanelRef:
    path: str
    line: int
    kind: str
    member: str
    text: str


def rel(path: Path) -> str:
    return path.resolve().relative_to(ROOT.resolve()).as_posix()


def owner_for(path: str, member: str) -> str:
    p = path.lower()
    m = member.lower()
    if "key" in p or m in {"screen", "panelmode", "buttons", "selectedbutton", "keyboardinputbridge", "lastinputmillis"}:
        return "UI_INPUT / UI_RENDER context"
    if "paint" in p or "surface" in p or "screen" in p or m.endswith("font") or m in {"lastaccessiblenarration"}:
        return "UI_RENDER context"
    if "save" in p or "persistence" in p or m in {"inventory", "basestorage", "carriedscript", "basestashedscript"}:
        return "INVENTORY_PERSIST / save context"
    if "world" in p or m in {"world", "atlas", "turn", "worldturn", "playerx", "playery"}:
        return "WORLD_RUNTIME context"
    if "admin" in p or "command" in p:
        return "SERVER_AUTH command context"
    if "faction" in p or "logistics" in p:
        return "COMBAT_SIM / INVENTORY_PERSIST faction context"
    return "UNASSIGNED legacy panel context"


def gather() -> List[LegacyPanelRef]:
    refs: List[LegacyPanelRef] = []
    if not SRC_ROOT.exists():
        return refs
    for path in sorted(SRC_ROOT.rglob("*.java"), key=lambda p: rel(p)):
        r = rel(path)
        if r.endswith("LegacyPanelContext.java"):
            continue
        lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
        for i, line in enumerate(lines, start=1):
            if "GamePanel" not in line and "panel." not in line and "game." not in line:
                continue
            stripped = line.strip()
            if not stripped or stripped.startswith("//"):
                continue
            if "GamePanel" in line:
                refs.append(LegacyPanelRef(r, i, "type-reference", "GamePanel", stripped))
            for match in MEMBER_RE.finditer(line):
                refs.append(LegacyPanelRef(r, i, "member-reference", match.group(1), stripped))
    return refs


def write_tsv(path: Path, header: Sequence[str], rows: Iterable[Sequence[object]]) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as f:
        f.write("\t".join(header) + "\n")
        for row in rows:
            f.write("\t".join(str(x).replace("\t", " ").replace("\n", " ") for x in row) + "\n")


def write_reports(refs: List[LegacyPanelRef]) -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    write_tsv(
        OUT_DIR / "LEGACY_PANEL_REFERENCE_LEDGER.tsv",
        ["path", "line", "kind", "member", "proposed_owner", "source"],
        [(r.path, r.line, r.kind, r.member, owner_for(r.path, r.member), r.text) for r in refs],
    )
    by_file = Counter(r.path for r in refs)
    by_member = Counter(r.member for r in refs if r.kind == "member-reference")
    with (OUT_DIR / "LEGACY_PANEL_REFERENCE_SUMMARY.md").open("w", encoding="utf-8", newline="\n") as f:
        f.write("# Legacy Panel Reference Summary\n\n")
        f.write(f"Generated: `{stamp}`\n\n")
        f.write("Status: active retirement ledger for the temporary `LegacyPanelContext.java` bridge.\n\n")
        f.write(f"- Total references: `{len(refs)}`\n")
        f.write(f"- Files with references: `{len(by_file)}`\n")
        f.write(f"- Unique member references: `{len(by_member)}`\n\n")
        f.write("## Top Files\n\n")
        for path, count in by_file.most_common(30):
            f.write(f"- `{path}`: `{count}`\n")
        f.write("\n## Top Members\n\n")
        for member, count in by_member.most_common(40):
            f.write(f"- `{member}`: `{count}`\n")
        f.write("\n## Retirement Rule\n\n")
        f.write("Do not add new gameplay behavior to `LegacyPanelContext.java`. Use it only to preserve compile continuity while each listed reference is retargeted to a smaller context or manager.\n")


def main(argv: Optional[Sequence[str]] = None) -> int:
    ap = argparse.ArgumentParser(description="Build legacy GamePanel reference ledger.")
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args(argv)
    refs = gather()
    print(f"Legacy GamePanel references: {len(refs)}")
    if not args.apply:
        print("Dry run only. Re-run with --apply to write reports.")
        return 0
    write_reports(refs)
    print(f"Wrote legacy panel reference reports under {OUT_DIR}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
