#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
import re
import sys
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SOURCE = ROOT / "src" / "mechanist" / "GamePanel.java"
DEFAULT_OUT = ROOT / "docs" / "gamepanel_catalog"
DEFAULT_SLICE_DIR = DEFAULT_OUT / "slices"

CONTROL_WORDS = {
    "if", "for", "while", "switch", "catch", "try", "do", "else", "synchronized", "new",
    "return", "throw", "case", "default"
}

TYPE_WORDS = {"class", "interface", "enum", "record", "@interface"}
MODIFIER_WORDS = {
    "public", "protected", "private", "static", "final", "abstract", "synchronized", "native",
    "strictfp", "transient", "volatile", "default", "sealed", "non-sealed"
}

METHOD_RE = re.compile(
    r"(?P<prefix>\b(?:public|protected|private|static|final|synchronized|abstract|native|strictfp|default|sealed|non-sealed)\b[\w\s<>,\[\].?@&:-]*|[A-Za-z_$][\w\s<>,\[\].?@&:-]*)"
    r"\s+(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)\s*\([^;{}]*\)\s*(?:throws\s+[\w\s,.<>]+)?\{"
)
CONSTRUCTOR_RE_TEMPLATE = r"(?P<prefix>(?:\b(?:public|protected|private)\b\s*)?{name}\s*\([^;{{}}]*\)\s*(?:throws\s+[\w\s,.<>]+)?\{{)"
TYPE_RE = re.compile(r"\b(class|interface|enum|record|@interface)\s+([A-Za-z_$][A-Za-z0-9_$]*)")
FIELD_NAME_RE = re.compile(r"([A-Za-z_$][A-Za-z0-9_$]*)\s*(?:=|,|$)")


@dataclass
class MethodRecord:
    owner: str
    name: str
    signature: str
    start_line: int
    end_line: int
    line_count: int
    char_start: int
    char_end: int
    category: str
    extraction_priority: str
    recommendation: str


@dataclass
class FieldRecord:
    owner: str
    name: str
    declaration: str
    start_line: int
    end_line: int
    category: str


@dataclass
class TypeRecord:
    kind: str
    name: str
    start_line: int
    end_line: int
    line_count: int
    depth: int


@dataclass
class Catalog:
    schema: str
    time: str
    source: str
    source_size_bytes: int
    source_line_count: int
    method_count: int
    field_count: int
    type_count: int
    methods: list[MethodRecord]
    fields: list[FieldRecord]
    types: list[TypeRecord]


def mask_java(text: str) -> str:
    """Replace comments and literals with spaces while preserving offsets and newlines."""
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


def line_starts(text: str) -> list[int]:
    starts = [0]
    for i, ch in enumerate(text):
        if ch == "\n":
            starts.append(i + 1)
    return starts


def line_no(starts: list[int], pos: int) -> int:
    lo = 0
    hi = len(starts) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if starts[mid] <= pos:
            lo = mid + 1
        else:
            hi = mid - 1
    return hi + 1


def line_range_text(lines: list[str], start_line: int, end_line: int) -> str:
    return "\n".join(lines[start_line - 1:end_line]) + "\n"


def brace_depths(masked: str) -> list[int]:
    out = [0] * len(masked)
    depth = 0
    for i, ch in enumerate(masked):
        out[i] = depth
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth = max(0, depth - 1)
    return out


def close_brace(masked: str, open_pos: int) -> int:
    depth = 0
    for i in range(open_pos, len(masked)):
        if masked[i] == "{":
            depth += 1
        elif masked[i] == "}":
            depth -= 1
            if depth == 0:
                return i
    raise RuntimeError(f"unclosed brace at char {open_pos}")


def safe_name(raw: str) -> str:
    return re.sub(r"[^A-Za-z0-9_.-]+", "_", raw).strip("_") or "unnamed"


def classify_method(name: str, signature: str, line_count: int) -> tuple[str, str, str]:
    lname = name.lower()
    sig = signature.lower()
    if lname.startswith("draw") or "graphics" in sig or "graphics2d" in sig or "paintcomponent" == lname:
        category = "render"
    elif "mouse" in lname or "key" in lname or "input" in lname or "wheel" in lname:
        category = "input"
    elif "load" in lname or "save" in lname or "storage" in lname:
        category = "persistence"
    elif "inventory" in lname or "item" in lname or "loot" in lname or "container" in lname:
        category = "inventory"
    elif "combat" in lname or "attack" in lname or "damage" in lname or "death" in lname or "defeat" in lname:
        category = "combat"
    elif "world" in lname or "zone" in lname or "sector" in lname or "map" in lname:
        category = "world"
    elif "button" in lname or "panel" in lname or "menu" in lname or "options" in lname or "screen" in lname:
        category = "ui"
    elif "npc" in lname or "actor" in lname or "creature" in lname:
        category = "entities"
    elif "craft" in lname or "build" in lname or "production" in lname or "recipe" in lname:
        category = "production"
    elif "audio" in lname or "sound" in lname or "music" in lname:
        category = "audio"
    else:
        category = "misc"

    if line_count >= 160:
        priority = "high"
        recommendation = "oversized method; split after first stable seam pass"
    elif line_count >= 80:
        priority = "medium"
        recommendation = "large method; inspect for delegation seam"
    elif category == "render":
        priority = "render"
        recommendation = "screen/overlay painter candidate"
    else:
        priority = "normal"
        recommendation = "leave until owning subsystem is clearer"
    return category, priority, recommendation


def classify_field(declaration: str, name: str) -> str:
    d = declaration.lower()
    n = name.lower()
    if "font" in d or "color" in d or "image" in d or "render" in d or "bufferedimage" in d:
        return "render_assets"
    if "mouse" in n or "key" in n or "input" in n or "selectedbutton" in n:
        return "input_ui_state"
    if "loading" in n or "boot" in n or "screen" in n or "panelmode" in n:
        return "screen_lifecycle"
    if "world" in n or "zone" in n or "sector" in n or "atlas" in n or "map" in n:
        return "world_zone_state"
    if "player" in n or "npc" in n or "actor" in n or "candidate" in n or "faction" in n:
        return "entity_state"
    if "inventory" in n or "item" in n or "loot" in n or "container" in n:
        return "inventory_state"
    if "combat" in n or "attack" in n or "target" in n or "health" in n:
        return "combat_state"
    if "audio" in n or "sound" in n or "music" in n:
        return "audio_state"
    if "option" in n or "menu" in n or "button" in n or "scroll" in n or "tab" in n:
        return "ui_state"
    return "misc_state"


def extract_signature(text: str, start: int, open_brace: int) -> str:
    prefix = text[start:open_brace].strip()
    return re.sub(r"\s+", " ", prefix)


def enclosing_type(types: list[TypeRecord], line: int) -> str:
    matches = [t for t in types if t.start_line <= line <= t.end_line]
    if not matches:
        return "<file>"
    matches.sort(key=lambda t: (t.line_count, t.start_line))
    return matches[0].name


def scan_types(masked: str, starts: list[int], depths: list[int]) -> list[TypeRecord]:
    records: list[TypeRecord] = []
    for hit in TYPE_RE.finditer(masked):
        kind = hit.group(1)
        name = hit.group(2)
        open_brace = masked.find("{", hit.end())
        if open_brace < 0:
            continue
        try:
            close = close_brace(masked, open_brace)
        except RuntimeError:
            continue
        start_line = line_no(starts, hit.start())
        end_line = line_no(starts, close)
        records.append(TypeRecord(kind, name, start_line, end_line, end_line - start_line + 1, depths[hit.start()]))
    records.sort(key=lambda r: (r.start_line, r.depth, r.name))
    return records


def scan_methods(text: str, masked: str, starts: list[int], depths: list[int], lines: list[str], types: list[TypeRecord]) -> list[MethodRecord]:
    records: list[MethodRecord] = []
    occupied: set[tuple[int, int]] = set()

    for hit in METHOD_RE.finditer(masked):
        if depths[hit.start()] < 1:
            continue
        name = hit.group("name")
        if name in CONTROL_WORDS or name in TYPE_WORDS:
            continue
        open_brace = masked.find("{", hit.start())
        if open_brace < 0:
            continue
        start_line = line_no(starts, hit.start())
        owner = enclosing_type(types, start_line)
        try:
            close = close_brace(masked, open_brace)
        except RuntimeError:
            continue
        end_line = line_no(starts, close)
        key = (start_line, end_line)
        if key in occupied:
            continue
        occupied.add(key)
        sig = extract_signature(text, hit.start(), open_brace)
        category, priority, recommendation = classify_method(name, sig, end_line - start_line + 1)
        records.append(MethodRecord(owner, name, sig, start_line, end_line, end_line - start_line + 1, hit.start(), close, category, priority, recommendation))

    # Constructors do not always match METHOD_RE because they lack a return type.
    top_types = [t for t in types if t.kind == "class"]
    for t in top_types:
        ctor_re = re.compile(CONSTRUCTOR_RE_TEMPLATE.format(name=re.escape(t.name)))
        for hit in ctor_re.finditer(masked):
            if depths[hit.start()] < 1:
                continue
            open_brace = masked.find("{", hit.start())
            if open_brace < 0:
                continue
            start_line = line_no(starts, hit.start())
            owner = enclosing_type(types, start_line)
            if owner != t.name:
                continue
            try:
                close = close_brace(masked, open_brace)
            except RuntimeError:
                continue
            end_line = line_no(starts, close)
            key = (start_line, end_line)
            if key in occupied:
                continue
            occupied.add(key)
            sig = extract_signature(text, hit.start(), open_brace)
            category, priority, recommendation = classify_method(t.name, sig, end_line - start_line + 1)
            records.append(MethodRecord(owner, t.name, sig, start_line, end_line, end_line - start_line + 1, hit.start(), close, category, priority, "constructor; inspect before extraction"))

    records.sort(key=lambda r: (r.start_line, r.name))
    return records


def find_top_level_member_statements(masked: str, class_record: TypeRecord | None, starts: list[int], depths: list[int]) -> list[tuple[int, int]]:
    if class_record is None:
        return []
    statements: list[tuple[int, int]] = []
    class_start_line = class_record.start_line
    class_end_line = class_record.end_line
    cursor = 0
    stmt_start = None
    for i, ch in enumerate(masked):
        line = line_no(starts, i)
        if line <= class_start_line or line >= class_end_line:
            continue
        # Direct members of GamePanel appear at brace depth 1 before their own opening brace.
        if depths[i] != 1:
            continue
        if stmt_start is None and not ch.isspace():
            stmt_start = i
        if ch == ";" and stmt_start is not None:
            statements.append((stmt_start, i))
            stmt_start = None
        elif ch == "{" and stmt_start is not None:
            # Method/type declarations are not field statements. Skip body.
            try:
                cursor = close_brace(masked, i)
            except RuntimeError:
                cursor = i
            stmt_start = None
    return statements


def scan_fields(text: str, masked: str, starts: list[int], depths: list[int], lines: list[str], types: list[TypeRecord]) -> list[FieldRecord]:
    gamepanel = next((t for t in types if t.name == "GamePanel" and t.kind == "class"), None)
    records: list[FieldRecord] = []
    for start, end in find_top_level_member_statements(masked, gamepanel, starts, depths):
        raw = text[start:end + 1].strip()
        if not raw or "(" in raw:
            continue
        compact = re.sub(r"\s+", " ", raw)
        if compact.startswith("package ") or compact.startswith("import "):
            continue
        # Remove annotations and modifiers/type prefix loosely, then pick plausible declared names.
        tail = compact[:-1]
        if "=" in tail:
            scan_part = tail.split("=", 1)[0]
        else:
            scan_part = tail
        tokens = scan_part.replace("[]", " [] ").split()
        candidates = [tok.strip(",") for tok in tokens if re.match(r"^[A-Za-z_$][A-Za-z0-9_$]*,?$", tok)]
        candidates = [c for c in candidates if c not in MODIFIER_WORDS and c not in {"final", "static"}]
        name = candidates[-1] if candidates else f"field_at_{line_no(starts, start)}"
        name = name.strip(",")
        start_line = line_no(starts, start)
        end_line = line_no(starts, end)
        records.append(FieldRecord("GamePanel", name, compact, start_line, end_line, classify_field(compact, name)))
    records.sort(key=lambda r: (r.start_line, r.name))
    return records


def write_tsv(path: Path, rows: Iterable[dict], fieldnames: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for row in rows:
            writer.writerow(row)


def write_method_slices(methods: list[MethodRecord], lines: list[str], out_dir: Path, method_names: list[str] | None, category: str | None, slice_all: bool) -> list[dict]:
    selected = methods
    if method_names:
        wanted = set(method_names)
        selected = [m for m in selected if m.name in wanted]
        missing = sorted(wanted - {m.name for m in selected})
        if missing:
            raise SystemExit("Requested methods not found: " + ", ".join(missing))
    if category:
        selected = [m for m in selected if m.category == category]
    if not slice_all and not method_names and not category:
        return []

    out: list[dict] = []
    for m in selected:
        category_dir = out_dir / safe_name(m.category)
        category_dir.mkdir(parents=True, exist_ok=True)
        file_name = f"{m.start_line:05d}_{safe_name(m.owner)}_{safe_name(m.name)}.javafrag"
        target = category_dir / file_name
        header = (
            f"// Extracted from src/mechanist/GamePanel.java\n"
            f"// Owner: {m.owner}\n"
            f"// Method: {m.name}\n"
            f"// Lines: {m.start_line}-{m.end_line}\n"
            f"// Category: {m.category}\n"
            f"// This is an examination slice, not compiled source.\n\n"
        )
        target.write_text(header + line_range_text(lines, m.start_line, m.end_line), encoding="utf-8")
        out.append({
            "name": m.name,
            "owner": m.owner,
            "category": m.category,
            "start_line": m.start_line,
            "end_line": m.end_line,
            "line_count": m.line_count,
            "output": str(target.relative_to(ROOT)).replace("\\", "/"),
        })
    return out


def write_manifest_markdown(path: Path, catalog: Catalog, slices: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    methods_by_size = sorted(catalog.methods, key=lambda m: (-m.line_count, m.start_line))[:40]
    render_methods = [m for m in catalog.methods if m.category == "render"][:80]
    categories: dict[str, int] = {}
    for m in catalog.methods:
        categories[m.category] = categories.get(m.category, 0) + 1
    field_categories: dict[str, int] = {}
    for f in catalog.fields:
        field_categories[f.category] = field_categories.get(f.category, 0) + 1

    lines = [
        "# GamePanel Catalog",
        "",
        "Generated by `ROOT_tools/gamepanel_catalog_slicer.py`.",
        "",
        f"Source: `{catalog.source}`",
        f"Lines: `{catalog.source_line_count}`",
        f"Methods: `{catalog.method_count}`",
        f"Fields/direct member statements: `{catalog.field_count}`",
        f"Types: `{catalog.type_count}`",
        "",
        "## Method categories",
        "",
        "| Category | Count |",
        "|---|---:|",
    ]
    for key, count in sorted(categories.items()):
        lines.append(f"| `{key}` | {count} |")
    lines += ["", "## Field categories", "", "| Category | Count |", "|---|---:|"]
    for key, count in sorted(field_categories.items()):
        lines.append(f"| `{key}` | {count} |")
    lines += ["", "## Largest methods", "", "| Method | Owner | Lines | Location | Category | Priority |", "|---|---|---:|---|---|---|"]
    for m in methods_by_size:
        lines.append(f"| `{m.name}` | `{m.owner}` | {m.line_count} | {m.start_line}-{m.end_line} | `{m.category}` | `{m.extraction_priority}` |")
    lines += ["", "## Render candidates", "", "| Method | Lines | Location | Recommendation |", "|---|---:|---|---|"]
    for m in render_methods:
        lines.append(f"| `{m.name}` | {m.line_count} | {m.start_line}-{m.end_line} | {m.recommendation} |")
    if slices:
        lines += ["", "## Slices emitted", "", "| Method | Category | Lines | Output |", "|---|---|---:|---|"]
        for s in slices:
            lines.append(f"| `{s['name']}` | `{s['category']}` | {s['line_count']} | `{s['output']}` |")
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def build_catalog(source: Path) -> tuple[Catalog, list[str]]:
    text = source.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    masked = mask_java(text)
    starts = line_starts(text)
    depths = brace_depths(masked)
    types = scan_types(masked, starts, depths)
    methods = scan_methods(text, masked, starts, depths, lines, types)
    fields = scan_fields(text, masked, starts, depths, lines, types)
    catalog = Catalog(
        schema="mechanist.gamepanel_catalog.v1",
        time=datetime.now(timezone.utc).isoformat(),
        source=str(source),
        source_size_bytes=source.stat().st_size,
        source_line_count=len(lines),
        method_count=len(methods),
        field_count=len(fields),
        type_count=len(types),
        methods=methods,
        fields=fields,
        types=types,
    )
    return catalog, lines


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Catalog and slice src/mechanist/GamePanel.java for defensive decomposition."
    )
    parser.add_argument("--source", default=str(DEFAULT_SOURCE), help="Path to GamePanel.java")
    parser.add_argument("--out", default=str(DEFAULT_OUT), help="Output directory for indexes and manifest")
    parser.add_argument("--slice-dir", default=str(DEFAULT_SLICE_DIR), help="Output directory for method slices")
    parser.add_argument("--method", action="append", default=[], help="Method name to slice. Repeatable.")
    parser.add_argument("--category", default="", help="Slice only methods in this category, e.g. render/input/ui/world")
    parser.add_argument("--slice-all", action="store_true", help="Emit javafrag slices for every indexed method")
    parser.add_argument("--no-json", action="store_true", help="Skip writing full JSON manifest")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    source = Path(args.source)
    if not source.is_absolute():
        source = ROOT / source
    if not source.exists():
        raise SystemExit(f"Missing source file: {source}")

    out_dir = Path(args.out)
    if not out_dir.is_absolute():
        out_dir = ROOT / out_dir
    slice_dir = Path(args.slice_dir)
    if not slice_dir.is_absolute():
        slice_dir = ROOT / slice_dir
    out_dir.mkdir(parents=True, exist_ok=True)

    catalog, lines = build_catalog(source)
    methods_as_dict = [asdict(m) for m in catalog.methods]
    fields_as_dict = [asdict(f) for f in catalog.fields]
    types_as_dict = [asdict(t) for t in catalog.types]

    if not args.no_json:
        manifest = asdict(catalog)
        manifest["methods"] = methods_as_dict
        manifest["fields"] = fields_as_dict
        manifest["types"] = types_as_dict
        (out_dir / "gamepanel_catalog.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")

    write_tsv(
        out_dir / "gamepanel_methods.tsv",
        methods_as_dict,
        ["owner", "name", "signature", "start_line", "end_line", "line_count", "category", "extraction_priority", "recommendation", "char_start", "char_end"],
    )
    write_tsv(
        out_dir / "gamepanel_fields.tsv",
        fields_as_dict,
        ["owner", "name", "declaration", "start_line", "end_line", "category"],
    )
    write_tsv(
        out_dir / "gamepanel_types.tsv",
        types_as_dict,
        ["kind", "name", "start_line", "end_line", "line_count", "depth"],
    )

    slices = write_method_slices(
        catalog.methods,
        lines,
        slice_dir,
        args.method,
        args.category or None,
        args.slice_all,
    )
    (out_dir / "gamepanel_slices.json").write_text(json.dumps({
        "schema": "mechanist.gamepanel_slices.v1",
        "time": datetime.now(timezone.utc).isoformat(),
        "source": str(source),
        "slice_count": len(slices),
        "slices": slices,
    }, indent=2), encoding="utf-8")
    write_manifest_markdown(out_dir / "gamepanel_catalog.md", catalog, slices)

    print(f"Cataloged {catalog.method_count} methods, {catalog.field_count} fields/member statements, {catalog.type_count} types.")
    print(f"Wrote {out_dir / 'gamepanel_catalog.md'}")
    print(f"Wrote {out_dir / 'gamepanel_methods.tsv'}")
    print(f"Wrote {out_dir / 'gamepanel_fields.tsv'}")
    print(f"Wrote {out_dir / 'gamepanel_types.tsv'}")
    if not args.no_json:
        print(f"Wrote {out_dir / 'gamepanel_catalog.json'}")
    if slices:
        print(f"Wrote {len(slices)} method slice(s) under {slice_dir}")
    else:
        print("No method slices emitted. Use --method NAME, --category CATEGORY, or --slice-all.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
