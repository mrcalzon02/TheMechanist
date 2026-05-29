#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
import re
import shutil
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GAMEPANEL = ROOT / "src" / "mechanist" / "GamePanel.java"
OUT_DIR = ROOT / "docs" / "gamepanel_hard_chop"
CATALOG = ROOT / "docs" / "gamepanel_catalog" / "gamepanel_methods.tsv"

CATEGORY_ORDER = [
    "render", "ui", "input", "world", "inventory", "combat", "production",
    "entities", "audio", "persistence", "misc"
]

SCREEN_HINTS = [
    ("boot_loading", re.compile(r"\b(drawBoot|drawBootGear|drawLoading)\b")),
    ("menu_intro_options", re.compile(r"\b(drawMenu|drawIntroCrawl|drawOptions|drawWorldSetup|drawMods|drawMultiplayer|drawEditor|drawPause|drawLost|drawZoneSplash)\b")),
    ("game_hud_map", re.compile(r"\b(drawGame|drawMap|drawHud|drawPanel|drawCharacter|drawInventory|drawKnowledge|drawInfopedia|drawSectorAudit)\b")),
    ("input_mouse_keyboard", re.compile(r"\b(mouse|key|input|wheel|controller|gamepad|handleKey|click)", re.IGNORECASE)),
    ("world_zone_sector", re.compile(r"\b(world|zone|sector|atlas|audit|transition|splash)", re.IGNORECASE)),
    ("inventory_trade_items", re.compile(r"\b(inventory|item|loot|trade|container|vendor|vending)", re.IGNORECASE)),
    ("combat_actor_entity", re.compile(r"\b(combat|attack|damage|death|defeat|npc|actor|creature|faction)", re.IGNORECASE)),
    ("construction_production", re.compile(r"\b(build|craft|recipe|production|machine|workbench|construction)", re.IGNORECASE)),
]


@dataclass
class MethodRecord:
    owner: str
    name: str
    signature: str
    start_line: int
    end_line: int
    line_count: int
    category: str
    extraction_priority: str
    recommendation: str
    char_start: int
    char_end: int


@dataclass
class ChopFile:
    path: str
    title: str
    category: str
    start_line: int
    end_line: int
    method_count: int
    line_count: int
    largest_method: str
    largest_method_lines: int


def read_catalog() -> list[MethodRecord]:
    if not CATALOG.exists():
        raise SystemExit(f"Missing method catalog: {CATALOG}. Run ROOT_tools/gamepanel_catalog_slicer.py first.")
    rows: list[MethodRecord] = []
    with CATALOG.open("r", encoding="utf-8", newline="") as f:
        reader = csv.DictReader(f, delimiter="\t")
        for row in reader:
            try:
                rows.append(MethodRecord(
                    owner=row.get("owner", ""),
                    name=row.get("name", ""),
                    signature=row.get("signature", ""),
                    start_line=int(row.get("start_line", "0")),
                    end_line=int(row.get("end_line", "0")),
                    line_count=int(row.get("line_count", "0")),
                    category=row.get("category", "misc"),
                    extraction_priority=row.get("extraction_priority", ""),
                    recommendation=row.get("recommendation", ""),
                    char_start=int(row.get("char_start", "0")),
                    char_end=int(row.get("char_end", "0")),
                ))
            except ValueError:
                continue
    return sorted(rows, key=lambda r: (r.start_line, r.end_line, r.name))


def classify_section(method: MethodRecord) -> str:
    haystack = f"{method.name} {method.signature} {method.category}"
    for name, regex in SCREEN_HINTS:
        if regex.search(haystack):
            return name
    return method.category if method.category in CATEGORY_ORDER else "misc"


def safe_name(text: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9_.-]+", "_", text).strip("_")
    return cleaned or "unnamed"


def slice_lines(lines: list[str], start: int, end: int) -> str:
    return "\n".join(lines[start - 1:end]) + "\n"


def write_chop_file(base: Path, title: str, category: str, methods: list[MethodRecord], lines: list[str], index: int) -> ChopFile:
    methods = sorted(methods, key=lambda r: (r.start_line, r.end_line, r.name))
    start_line = min(m.start_line for m in methods)
    end_line = max(m.end_line for m in methods)
    largest = max(methods, key=lambda r: r.line_count)
    rel = Path(category) / f"{index:03d}_{safe_name(title)}.javafrag"
    target = base / rel
    target.parent.mkdir(parents=True, exist_ok=True)

    header = [
        "// GamePanel hard-chop workbench fragment.",
        "// This is NOT compiled source. It is an extraction planning and connector-readable slice.",
        "// Source: src/mechanist/GamePanel.java",
        f"// Section: {title}",
        f"// Category: {category}",
        f"// Source lines: {start_line}-{end_line}",
        f"// Method count: {len(methods)}",
        "//",
        "// Methods in this fragment:",
    ]
    for m in methods:
        header.append(f"// - {m.name} :: lines {m.start_line}-{m.end_line} ({m.line_count}) :: {m.signature}")
    header.append("\n")

    body_parts: list[str] = []
    cursor = start_line
    for m in methods:
        if m.start_line > cursor:
            gap = m.start_line - cursor
            if gap <= 12:
                body_parts.append(slice_lines(lines, cursor, m.start_line - 1))
            else:
                body_parts.append(f"\n// ... omitted {gap} non-method/source lines between cataloged methods ...\n\n")
        body_parts.append(slice_lines(lines, m.start_line, m.end_line))
        cursor = m.end_line + 1

    target.write_text("\n".join(header) + "".join(body_parts), encoding="utf-8", newline="\n")
    return ChopFile(
        path=str(target.relative_to(ROOT)).replace("\\", "/"),
        title=title,
        category=category,
        start_line=start_line,
        end_line=end_line,
        method_count=len(methods),
        line_count=end_line - start_line + 1,
        largest_method=largest.name,
        largest_method_lines=largest.line_count,
    )


def group_contiguous(methods: list[MethodRecord], max_lines: int, max_methods: int) -> list[list[MethodRecord]]:
    groups: list[list[MethodRecord]] = []
    current: list[MethodRecord] = []
    group_start = 0
    group_end = 0

    for method in methods:
        if not current:
            current = [method]
            group_start = method.start_line
            group_end = method.end_line
            continue

        proposed_start = min(group_start, method.start_line)
        proposed_end = max(group_end, method.end_line)
        proposed_lines = proposed_end - proposed_start + 1
        if proposed_lines > max_lines or len(current) >= max_methods:
            groups.append(current)
            current = [method]
            group_start = method.start_line
            group_end = method.end_line
        else:
            current.append(method)
            group_start = proposed_start
            group_end = proposed_end
    if current:
        groups.append(current)
    return groups


def write_raw_line_chunks(base: Path, lines: list[str], chunk_lines: int) -> list[ChopFile]:
    raw_dir = base / "raw_line_chunks"
    raw_dir.mkdir(parents=True, exist_ok=True)
    out: list[ChopFile] = []
    total = len(lines)
    idx = 1
    for start in range(1, total + 1, chunk_lines):
        end = min(total, start + chunk_lines - 1)
        target = raw_dir / f"{idx:03d}_lines_{start:05d}_{end:05d}.javafrag"
        header = (
            "// GamePanel raw line chunk. NOT compiled source.\n"
            "// Source: src/mechanist/GamePanel.java\n"
            f"// Source lines: {start}-{end}\n\n"
        )
        target.write_text(header + slice_lines(lines, start, end), encoding="utf-8", newline="\n")
        out.append(ChopFile(
            path=str(target.relative_to(ROOT)).replace("\\", "/"),
            title=f"raw lines {start}-{end}",
            category="raw_line_chunks",
            start_line=start,
            end_line=end,
            method_count=0,
            line_count=end - start + 1,
            largest_method="",
            largest_method_lines=0,
        ))
        idx += 1
    return out


def write_manifest(out_dir: Path, chops: list[ChopFile], method_count: int, line_count: int) -> None:
    manifest = {
        "schema": "mechanist.gamepanel_hard_chop.v1",
        "time": datetime.now(timezone.utc).isoformat(),
        "source": "src/mechanist/GamePanel.java",
        "source_line_count": line_count,
        "method_count": method_count,
        "fragment_count": len(chops),
        "fragments": [asdict(c) for c in chops],
    }
    (out_dir / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")

    md: list[str] = [
        "# GamePanel Hard Chop Workbench",
        "",
        "These files are connector-readable extraction workbench fragments. They are not compiled Java source.",
        "",
        f"Source lines: `{line_count}`",
        f"Cataloged methods: `{method_count}`",
        f"Fragments: `{len(chops)}`",
        "",
        "## Fragments",
        "",
        "| Fragment | Category | Lines | Methods | Largest Method |",
        "|---|---|---:|---:|---|",
    ]
    for c in chops:
        largest = f"`{c.largest_method}` ({c.largest_method_lines})" if c.largest_method else ""
        md.append(f"| `{c.path}` | `{c.category}` | {c.line_count} | {c.method_count} | {largest} |")
    (out_dir / "README.md").write_text("\n".join(md) + "\n", encoding="utf-8")


def run(max_lines: int, max_methods: int, raw_chunk_lines: int, include_raw: bool) -> None:
    if not GAMEPANEL.exists():
        raise SystemExit(f"Missing source file: {GAMEPANEL}")
    if OUT_DIR.exists():
        shutil.rmtree(OUT_DIR)
    OUT_DIR.mkdir(parents=True, exist_ok=True)

    text = GAMEPANEL.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    methods = read_catalog()

    buckets: dict[str, list[MethodRecord]] = {}
    for method in methods:
        buckets.setdefault(classify_section(method), []).append(method)

    chops: list[ChopFile] = []
    sequence = 1
    ordered_keys = []
    for preferred in [name for name, _ in SCREEN_HINTS] + CATEGORY_ORDER:
        if preferred in buckets and preferred not in ordered_keys:
            ordered_keys.append(preferred)
    ordered_keys.extend(sorted(k for k in buckets if k not in ordered_keys))

    for key in ordered_keys:
        groups = group_contiguous(buckets[key], max_lines=max_lines, max_methods=max_methods)
        for group_index, group in enumerate(groups, start=1):
            title = f"{key}_{group_index:02d}_lines_{group[0].start_line:05d}_{group[-1].end_line:05d}"
            chops.append(write_chop_file(OUT_DIR, title, key, group, lines, sequence))
            sequence += 1

    if include_raw:
        chops.extend(write_raw_line_chunks(OUT_DIR, lines, raw_chunk_lines))

    write_manifest(OUT_DIR, chops, method_count=len(methods), line_count=len(lines))
    print(f"Hard-chopped GamePanel into {len(chops)} connector-readable fragments under {OUT_DIR.relative_to(ROOT)}")
    print(f"Wrote {OUT_DIR.relative_to(ROOT) / 'README.md'}")
    print(f"Wrote {OUT_DIR.relative_to(ROOT) / 'manifest.json'}")


def main() -> int:
    parser = argparse.ArgumentParser(description="Hard-chop GamePanel.java into connector-readable workbench fragments.")
    parser.add_argument("--max-lines", type=int, default=650, help="Maximum approximate line span per method-group fragment.")
    parser.add_argument("--max-methods", type=int, default=45, help="Maximum cataloged methods per method-group fragment.")
    parser.add_argument("--raw-chunk-lines", type=int, default=700, help="Line count for raw line chunks.")
    parser.add_argument("--no-raw", action="store_true", help="Do not emit raw line chunks.")
    args = parser.parse_args()
    run(args.max_lines, args.max_methods, args.raw_chunk_lines, include_raw=not args.no_raw)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
