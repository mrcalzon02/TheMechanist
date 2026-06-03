#!/usr/bin/env python3
"""
Split src/mechanist/gamepanel-shard*.txt into explicit subsystem documents.

The tool writes every parsed method and every residual source fragment into
ROOT_DOCS/shardmining/generated_subsystems.  In apply mode it first backs up the
original shards, then optionally retires the source shard files by moving them out
of src/mechanist into the generated_subsystems/_retired_shards folder.

PowerShell one-command run from repo root:

py -3 -m py_compile scripts\EXTRACT_GAMEPANEL_SHARDS_TO_SUBSYSTEMS.py; if ($?) { py -3 scripts\EXTRACT_GAMEPANEL_SHARDS_TO_SUBSYSTEMS.py --apply --retire-stripped-shards }

Dry run:

py -3 scripts\EXTRACT_GAMEPANEL_SHARDS_TO_SUBSYSTEMS.py
"""

from __future__ import annotations

import argparse
import dataclasses
import datetime as dt
import hashlib
import re
import shutil
import sys
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

ROOT = Path.cwd()
SHARD_DIR = ROOT / "src" / "mechanist"
OUT_DIR = ROOT / "ROOT_DOCS" / "shardmining" / "generated_subsystems"
SHARD_GLOB = "gamepanel-shard*.txt"

CONTROL_WORDS = {"if", "for", "while", "switch", "catch", "try", "else", "do", "return", "new", "case", "default"}

METHOD_SIG_RE = re.compile(
    r"^\s*(?:(?:public|private|protected|static|final|synchronized|abstract)\s+)*"
    r"(?:(?:[A-Za-z_$][\w$<>\[\], ?.&]*|void|int|long|double|float|boolean|char|byte|short)\s+)?"
    r"(?P<name>[A-Za-z_$][\w$]*)\s*\([^;{}]*\)\s*(?:throws\s+[^{}]+)?\{"
)

# Explicit subsystem remap. First matching bucket wins.
SUBSYSTEMS: List[Tuple[str, Sequence[str], str]] = [
    ("00_core_shell_state", [r"^(GamePanel|stateSummary|scaled|uiLayout|.*Rect)$", r"\b(screen|panelMode|font|layout|rect|button|scrollRegion)\b"], "Core shell state, constructors, shared layout, and geometry."),
    ("01_startup_lifecycle_audit", [r"^(init|start|stop|load|apply|resolve|configure|audit|generateCandidates)", r"\b(runtime|launcher|profile|server|diagnostic|audit|accessibility)\b"], "Startup lifecycle, runtime binding, profile detection, and audit reporting."),
    ("02_render_surfaces", [r"^(draw|paint|render|center|fill|outline|frame|icon|color|optionColor)", r"\b(Graphics2D|surface|painter|hud|overlay|zoneSplash|loading|menu|infopedia)\b"], "Immediate-mode drawing, screen surfaces, HUD, and visual panels."),
    ("03_input_scroll_navigation", [r"^(key|mouse|wheel|scroll|handleScrollbar|findScrollRegion|currentScrollableTag|setScreen|navigate|activate)", r"\b(input|keyboard|gamepad|scroll|mouse|selectedButton|navigation)\b"], "Keyboard, mouse, controller, scrollbars, and route changes."),
    ("04_panel_buttons_commands", [r"^(rebuildButtons|add.*Button|beginBuild|beginDefenseBuild|open.*Infopedia)", r"\b(ButtonBox|CLAIM ROOM|SAVE DIAGNOSTIC|VALIDATE|command panel)\b"], "Button construction and command-surface wiring."),
    ("05_inventory_items_containers", [r"\b(inventory|item|container|loot|equip|unequip|transfer|stash|scavenge|ItemActionResult|ContainerRecord)\b"], "Inventory, item, equipment, container, loot, and scavenge operations."),
    ("06_interaction_fixtures_machines", [r"^(interact|use|vend|hack|emergency|machine|fixture)", r"\b(vending|emergency|machine|fixture|Data spike|cooldown|smelter|assembler|steam engine)\b"], "Fixture, emergency machine, vending, powered-machine, and hack interaction rules."),
    ("07_world_profile_generation", [r"\b(world|zone|atlas|generation|transition|audit|sector|profile|candidate|character|CampaignWorld|WorldSetup|WorldSave)\b", r"^(openWorld|useSelectedWorld|deleteSelectedWorld|generateConfiguredWorld|loadWorld)"] , "Profile flow, world setup, generation, atlas, zone transition, and audit UI."),
    ("08_combat_entities_simulation", [r"\b(combat|attack|damage|target|npc|entity|turn|advanceTurn|move|movement|motion|path|faction|suspicion|heat)\b"], "Entity, turn, movement, combat, faction, heat, and simulation hooks."),
    ("09_save_profile_persistence", [r"\b(save|load|autosave|slot|persistence|profile|EULA|consent|fallback)\b", r"^(saveGameSlot|loadGameSlot|logPersistence|openUserProfile)"] , "Save/load, autosave, profile, EULA, and persistence behavior."),
    ("10_options_runtime_controls", [r"\b(option|display|graphics|sound|volume|jvm|runtime|accessibility|density|scale|doom|map viewport)\b", r"^(cycle|toggle|set.*Option|applyOptions|increase|decrease)"] , "Options screen actions and display/audio/JVM/accessibility toggles."),
    ("99_unassigned_fragments", [r".*"], "Fallback for partial methods, class shell material, and unmatched fragments."),
]

@dataclasses.dataclass(frozen=True)
class Segment:
    shard: str
    kind: str
    name: str
    subsystem: str
    start: int
    end: int
    text: str
    digest: str


def digest(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8", errors="replace")).hexdigest()


def normalize(text: str) -> str:
    return text.replace("\r\n", "\n").replace("\r", "\n")


def neutralize(line: str, in_block: bool = False) -> Tuple[str, bool]:
    out: List[str] = []
    quote: Optional[str] = None
    esc = False
    i = 0
    while i < len(line):
        ch = line[i]
        nxt = line[i + 1] if i + 1 < len(line) else ""
        if in_block:
            if ch == "*" and nxt == "/":
                in_block = False
                out.append("  ")
                i += 2
            else:
                out.append(" ")
                i += 1
            continue
        if quote:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == quote:
                quote = None
            out.append(" ")
            i += 1
            continue
        if ch == "/" and nxt == "/":
            out.append(" " * (len(line) - i))
            break
        if ch == "/" and nxt == "*":
            in_block = True
            out.append("  ")
            i += 2
            continue
        if ch in ("'", '"'):
            quote = ch
            out.append(" ")
            i += 1
            continue
        out.append(ch)
        i += 1
    return "".join(out), in_block


def match_signature(text: str) -> Optional[str]:
    compact = " ".join(text.strip().split())
    if not compact or ";" in compact.split("{")[0]:
        return None
    m = METHOD_SIG_RE.search(compact)
    if not m:
        return None
    name = m.group("name")
    return None if name in CONTROL_WORDS else name


def signature_at(lines: Sequence[str], i: int, max_lines: int = 10) -> Tuple[Optional[str], Optional[int]]:
    sig: List[str] = []
    parens = 0
    for off in range(max_lines):
        j = i + off
        if j >= len(lines):
            return None, None
        raw = lines[j]
        stripped = raw.strip()
        if off == 0:
            if not stripped or stripped.startswith(("@", "//", "*", "/*")):
                return None, None
            first = re.split(r"\s+|\(", stripped, maxsplit=1)[0]
            if first in CONTROL_WORDS or ("=" in stripped and "(" not in stripped):
                return None, None
        sig.append(raw)
        safe, _ = neutralize(raw)
        parens += safe.count("(") - safe.count(")")
        if "{" in safe and parens <= 0:
            name = match_signature(" ".join(sig))
            return (name, j) if name else (None, None)
        if ";" in safe and parens <= 0:
            return None, None
    return None, None


def classify(name: str, text: str, shard: str, kind: str) -> str:
    hay = f"{name}\n{kind}\n{shard}\n{text}"
    for subsystem, patterns, _desc in SUBSYSTEMS:
        if any(re.search(p, hay, re.I | re.M) for p in patterns):
            return subsystem
    return "99_unassigned_fragments"


def extract(path: Path) -> Tuple[str, List[Segment]]:
    text = normalize(path.read_text(encoding="utf-8", errors="replace"))
    lines = text.splitlines(keepends=True)
    used = [False] * len(lines)
    segments: List[Segment] = []
    i = 0
    while i < len(lines):
        name, sig_end = signature_at(lines, i)
        if not name or sig_end is None:
            i += 1
            continue
        depth = 0
        in_block = False
        end = sig_end
        started = False
        for j in range(i, len(lines)):
            safe, in_block = neutralize(lines[j], in_block)
            if "{" in safe:
                started = True
            depth += safe.count("{") - safe.count("}")
            if started and depth <= 0:
                end = j
                break
        block = "".join(lines[i:end + 1])
        subsystem = classify(name, block, path.name, "method")
        segments.append(Segment(path.name, "method", name, subsystem, i + 1, end + 1, block, digest(block)))
        for k in range(i, end + 1):
            used[k] = True
        i = end + 1

    run: Optional[int] = None
    for idx, flag in enumerate(used + [True]):
        if idx < len(used) and not flag:
            if run is None:
                run = idx
            continue
        if run is not None:
            end = idx - 1
            block = "".join(lines[run:end + 1])
            if block.strip():
                subsystem = classify("residual", block, path.name, "residual")
                segments.append(Segment(path.name, "residual", f"residual_{run + 1}_{end + 1}", subsystem, run + 1, end + 1, block, digest(block)))
            run = None
    return text, sorted(segments, key=lambda s: (s.start, s.end, s.name))


def verify(path: Path, text: str, segments: Sequence[Segment]) -> List[str]:
    lines = text.splitlines(keepends=True)
    counts = [0] * len(lines)
    for seg in segments:
        for n in range(seg.start, seg.end + 1):
            if 1 <= n <= len(counts):
                counts[n - 1] += 1
    bad = [i + 1 for i, line in enumerate(lines) if line.strip() and counts[i] != 1]
    if bad:
        return [f"{path.name}: line accounting failed for {bad[:30]}{'...' if len(bad) > 30 else ''}"]
    return []


def write_outputs(out_dir: Path, results: Sequence[Tuple[Path, str, List[Segment]]], stamp: str, retire: bool) -> None:
    out_dir.mkdir(parents=True, exist_ok=True)
    by_sub: Dict[str, List[Segment]] = {}
    desc = {name: d for name, _p, d in SUBSYSTEMS}
    for _path, _text, segs in results:
        for seg in segs:
            by_sub.setdefault(seg.subsystem, []).append(seg)

    for subsystem, segs in sorted(by_sub.items()):
        target = out_dir / f"{subsystem}.txt"
        with target.open("w", encoding="utf-8", newline="\n") as f:
            f.write(f"# {subsystem}\n\nGenerated: {stamp}\n\n{desc.get(subsystem, '')}\n\n")
            f.write("These are raw GamePanel shard extraction blocks, not compile-ready Java classes.\n")
            for seg in sorted(segs, key=lambda s: (s.shard, s.start, s.end)):
                f.write("\n---\n")
                f.write(f"Source: {seg.shard}:{seg.start}-{seg.end}\nKind: {seg.kind}\nName: {seg.name}\nSHA256: {seg.digest}\n\n")
                f.write("```java\n")
                f.write(seg.text)
                if not seg.text.endswith("\n"):
                    f.write("\n")
                f.write("```\n")

    with (out_dir / "MANIFEST.md").open("w", encoding="utf-8", newline="\n") as f:
        f.write("# GamePanel Shard Subsystem Extraction Manifest\n\n")
        f.write(f"Generated: `{stamp}`\n\n")
        f.write(f"Retire stripped shards: `{retire}`\n\n")
        f.write("| Shard | Bytes | SHA256 | Segments |\n|---|---:|---|---:|\n")
        for path, text, segs in results:
            f.write(f"| `{path.as_posix()}` | {len(text.encode('utf-8', errors='replace'))} | `{digest(text)}` | {len(segs)} |\n")

    with (out_dir / "REMAP_TABLE.tsv").open("w", encoding="utf-8", newline="\n") as f:
        f.write("subsystem\tshard\tkind\tname\tstart_line\tend_line\tsha256\n")
        for _path, _text, segs in results:
            for seg in segs:
                f.write(f"{seg.subsystem}\t{seg.shard}\t{seg.kind}\t{seg.name}\t{seg.start}\t{seg.end}\t{seg.digest}\n")


def main(argv: Optional[Sequence[str]] = None) -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--shard-dir", default=str(SHARD_DIR))
    ap.add_argument("--out-dir", default=str(OUT_DIR))
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--retire-stripped-shards", action="store_true", help="Move source shards to generated_subsystems/_retired_shards after successful extraction.")
    args = ap.parse_args(argv)

    shard_dir = Path(args.shard_dir).resolve()
    out_dir = Path(args.out_dir).resolve()
    shards = sorted(shard_dir.glob(SHARD_GLOB), key=lambda p: p.name)
    if not shards:
        print(f"No shards found in {shard_dir}", file=sys.stderr)
        return 2
    if args.retire_stripped_shards and not args.apply:
        print("--retire-stripped-shards requires --apply", file=sys.stderr)
        return 2

    stamp = dt.datetime.now().strftime("%Y%m%d_%H%M%S")
    results: List[Tuple[Path, str, List[Segment]]] = []
    errors: List[str] = []
    for path in shards:
        text, segs = extract(path)
        results.append((path, text, segs))
        errors.extend(verify(path, text, segs))

    print(f"GamePanel subsystem remap: {'APPLY' if args.apply else 'DRY RUN'}")
    print(f"Output: {out_dir}")
    for path, text, segs in results:
        methods = sum(1 for s in segs if s.kind == "method")
        residual = sum(1 for s in segs if s.kind == "residual")
        print(f"{path.name}: {methods} methods, {residual} residual blocks, {len(text.encode('utf-8', errors='replace'))} bytes")

    if errors:
        print("\nRefusing to continue; coverage errors:", file=sys.stderr)
        for err in errors:
            print("  " + err, file=sys.stderr)
        return 3

    if not args.apply:
        print("\nDry run complete. Re-run with --apply --retire-stripped-shards to generate docs and retire shard files.")
        return 0

    backup = out_dir / "_backups" / stamp
    backup.mkdir(parents=True, exist_ok=True)
    for path in shards:
        shutil.copy2(path, backup / path.name)
    print(f"Backed up shards: {backup}")

    write_outputs(out_dir, results, stamp, args.retire_stripped_shards)
    print(f"Generated subsystem documents: {out_dir}")

    if args.retire_stripped_shards:
        retired = out_dir / "_retired_shards" / stamp
        retired.mkdir(parents=True, exist_ok=True)
        for path in shards:
            shutil.move(str(path), str(retired / path.name))
            print(f"Retired shard: {path} -> {retired / path.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
