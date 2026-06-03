#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import subprocess
import sys
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
GAMEPANEL = REPO_ROOT / "src" / "mechanist" / "GamePanel.java"
REPORT = REPO_ROOT / "docs" / "ui_runtime_fix_pass.json"

WORLDGEN_REPLACEMENTS = [
    ("GENERATE HIVE WORLD", "GENERATE SPIRE SECTOR"),
    ("Generate Hive World", "Generate Spire Sector"),
    ("generate hive world", "generate spire sector"),
    ("HIVE WORLD", "SPIRE SECTOR"),
    ("Hive World", "Spire Sector"),
    ("hive world", "spire sector"),
    ("Zone size: Standard (worldgen weight 600-760; dimensions derived, not raw 500+ layers)", "Zone size: Standard"),
    ("Hoarder mode: OFF — Strength/Endurance carry limit", "Hoarder: OFF — STR/END carry limit"),
    ("Hoarder mode: ON — personal inventory decoupled from Strength/Endurance", "Hoarder: ON — inventory decoupled"),
    ("Simulation age runs additional history batches before world save/start. Higher values improve provenance ledgers but cost generation time. Hoarder mode decouples personal inventory from Strength and Endurance.", "Simulation age adds history batches before start. Higher values improve provenance but costs time."),
    ("Configure a new hive world, then generate it before selecting a character.", "Configure a spire sector, then generate it before selecting a character."),
    ("Configure a new Hive World, then generate it before selecting a character.", "Configure a new Spire Sector, then generate it before selecting a character."),
    ("world save/start. Higher values improve provenance ledgers but cost generation time. Hoarder mode decouples", "start. Higher values improve provenance but cost time. Hoarder decouples"),
    ("personal inventory from Strength and Endurance.", "inventory from STR/END."),
]

INTRO_STRING_REPLACEMENTS = [
    ("loracroll", "intro crawl"),
    ("Loracroll", "Intro Crawl"),
    ("LORACROLL", "INTRO CRAWL"),
    ("lorecrawl", "intro crawl"),
    ("Lorecrawl", "Intro Crawl"),
    ("LORECRAWL", "INTRO CRAWL"),
]

GRAPHIC_HINT_REPLACEMENTS = [
    ("new_world_backdrop_rebase", "source/Background/Backdrop.png"),
    ("clouds_slow_rebase", "source/Background/CLOUDS1slow.png"),
    ("clouds_fast_rebase", "source/Background/Clouds2fast.png"),
]

@dataclass
class Step:
    name: str
    ok: bool
    detail: str

@dataclass
class Replacement:
    category: str
    old: str
    new: str
    count: int


def run(cmd: list[str], steps: list[Step], name: str, required: bool = False) -> int:
    if not Path(cmd[1]).exists() if len(cmd) > 1 and cmd[0] == sys.executable else False:
        steps.append(Step(name, False, "tool missing: " + cmd[1]))
        return 127
    completed = subprocess.run(cmd, cwd=REPO_ROOT, text=True)
    ok = completed.returncode == 0
    steps.append(Step(name, ok, " ".join(cmd) + f" exit={completed.returncode}"))
    if required and not ok:
        raise SystemExit(completed.returncode)
    return completed.returncode


def read_gamepanel() -> str:
    if not GAMEPANEL.exists() or GAMEPANEL.stat().st_size < 10000:
        raise SystemExit(f"Missing or suspiciously small GamePanel.java: {GAMEPANEL}")
    return GAMEPANEL.read_text(encoding="utf-8", errors="replace")


def replace_literals(text: str, replacements: list[tuple[str, str]], category: str, records: list[Replacement]) -> str:
    out = text
    for old, new in replacements:
        count = out.count(old)
        if count:
            out = out.replace(old, new)
        records.append(Replacement(category, old, new, count))
    return out


def patch_intro_timing(text: str, records: list[Replacement]) -> str:
    # Target named constants/assignments that mention intro/lore + crawl + duration/time.
    patterns = [
        re.compile(r"(?i)((?:intro|lore)[_A-Z0-9a-z]*crawl[_A-Z0-9a-z]*(?:duration|time|millis|ms)[_A-Z0-9a-z]*\s*=\s*)(\d[\d_]*)(L?)"),
        re.compile(r"(?i)((?:duration|time|millis|ms)[_A-Z0-9a-z]*(?:intro|lore)[_A-Z0-9a-z]*crawl[_A-Z0-9a-z]*\s*=\s*)(\d[\d_]*)(L?)"),
        re.compile(r"(?i)((?:INTRO|LORE)[_A-Z0-9]*CRAWL[_A-Z0-9]*(?:DURATION|TIME|MILLIS|MS)[_A-Z0-9]*\s*=\s*)(\d[\d_]*)(L?)"),
    ]
    out = text
    total = 0
    for pat in patterns:
        def repl(m: re.Match[str]) -> str:
            nonlocal total
            total += 1
            suffix = m.group(3) or ""
            return m.group(1) + "202_000" + suffix
        out = pat.sub(repl, out)
    records.append(Replacement("intro_timing", "intro/lore crawl duration assignments", "202_000 ms", total))

    # If code contains the common raw 200000 ms value near crawl terms, fix that too, but only in a tight window.
    def window_fix(match: re.Match[str]) -> str:
        nonlocal out
        chunk = match.group(0)
        changed = re.sub(r"\b200_?000\b", "202_000", chunk)
        return changed
    crawl_window = re.compile(r"(?is)(.{0,120}(?:intro|lore)[_\sA-Za-z0-9]*crawl.{0,220})")
    before = out
    out = crawl_window.sub(window_fix, out)
    raw_count = before.count("200000") + before.count("200_000") - (out.count("200000") + out.count("200_000"))
    records.append(Replacement("intro_timing", "200000/200_000 near intro/lore crawl", "202_000", max(0, raw_count)))
    return out


def patch_wrapping_standards(text: str, records: list[Replacement]) -> str:
    out = text
    # Replace obvious overlong GUI prose fragments that are being drawn as single-line strings.
    compact_pairs = [
        ("Simulation information stays left; controls stay right so low-resolution layouts do not trample themselves.", "Info left. Controls right. Compact layout enforced."),
        ("Higher values improve provenance ledgers but cost generation time.", "Higher values improve provenance but cost time."),
        ("personal inventory decoupled from Strength/Endurance", "inventory decoupled"),
        ("Strength/Endurance", "STR/END"),
    ]
    return replace_literals(out, compact_pairs, "compact_text", records)


def write_report(steps: list[Step], replacements: list[Replacement]) -> None:
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps({
        "schema": "mechanist.ui_runtime_fix_pass.v1",
        "time": datetime.now(timezone.utc).isoformat(),
        "steps": [asdict(s) for s in steps],
        "replacements": [asdict(r) for r in replacements],
        "total_replacements": sum(r.count for r in replacements),
    }, indent=2), encoding="utf-8")


def main() -> int:
    steps: list[Step] = []
    replacements: list[Replacement] = []
    print("=== The Mechanist UI/runtime fix pass ===")

    graphics_tool = ROOT_TOOLS / "fix_client_runtime_graphics.py"
    if graphics_tool.exists():
        run([sys.executable, str(graphics_tool)], steps, "repair_runtime_graphics")
    else:
        steps.append(Step("repair_runtime_graphics", False, "fix_client_runtime_graphics.py not present"))

    original = read_gamepanel()
    patched = original
    patched = replace_literals(patched, WORLDGEN_REPLACEMENTS, "worldgen_text", replacements)
    patched = replace_literals(patched, INTRO_STRING_REPLACEMENTS, "intro_title_text", replacements)
    patched = replace_literals(patched, GRAPHIC_HINT_REPLACEMENTS, "runtime_graphic_paths", replacements)
    patched = patch_wrapping_standards(patched, replacements)
    patched = patch_intro_timing(patched, replacements)

    if patched != original:
        GAMEPANEL.write_text(patched, encoding="utf-8", newline="")
        steps.append(Step("patch_gamepanel", True, f"updated {GAMEPANEL}"))
    else:
        steps.append(Step("patch_gamepanel", True, "no textual changes required"))

    for tool_name, label in [
        ("gamepanel_method_indexer.py", "refresh_gamepanel_method_index"),
        ("gamepanel_extract_methods.py", "refresh_gamepanel_method_extracts"),
        ("repository_scan_indexer.py", "refresh_repository_manifest"),
    ]:
        tool = ROOT_TOOLS / tool_name
        if tool.exists():
            run([sys.executable, str(tool)], steps, label)

    write_report(steps, replacements)
    print(f"Report: {REPORT}")
    print(f"Total replacements: {sum(r.count for r in replacements)}")
    for r in replacements:
        if r.count:
            print(f"  {r.category}: {r.count}x {r.old!r} -> {r.new!r}")
    failed_required = [s for s in steps if not s.ok and s.name in {"patch_gamepanel"}]
    if failed_required:
        return 2
    print("UI/runtime fix pass completed.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
