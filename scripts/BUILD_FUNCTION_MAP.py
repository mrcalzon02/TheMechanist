#!/usr/bin/env python3
"""
BUILD_FUNCTION_MAP.py

Builds a systemic, non-destructive function and document map for The Mechanist.

This tool does not delete, move, or rewrite source files. It inventories what the
repository currently contains so the next refactor phase can attach behavior to
proper subsystem owners and retire duplicate/extraneous documents deliberately.

Run from repository root:

    py -3 scripts\BUILD_FUNCTION_MAP.py --apply

Outputs:

    ROOT_DOCS/functionmap/generated/FUNCTION_MAP.tsv
    ROOT_DOCS/functionmap/generated/CLASS_MAP.tsv
    ROOT_DOCS/functionmap/generated/DUPLICATE_FUNCTION_NAMES.tsv
    ROOT_DOCS/functionmap/generated/FILE_INVENTORY.tsv
    ROOT_DOCS/functionmap/generated/DOCUMENT_INVENTORY.tsv
    ROOT_DOCS/functionmap/generated/DOC_RETIREMENT_CANDIDATES.tsv
    ROOT_DOCS/functionmap/generated/OVERSIZED_OWNERS.tsv
    ROOT_DOCS/functionmap/generated/SUMMARY.md
"""

from __future__ import annotations

import argparse
import dataclasses
import datetime as dt
import hashlib
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Tuple

ROOT = Path.cwd()
OUT_DIR = ROOT / "ROOT_DOCS" / "functionmap" / "generated"

JAVA_ROOTS = [ROOT / "src", ROOT / "test", ROOT / "tests"]
DOC_ROOTS = [ROOT / "ROOT_DOCS", ROOT / "docs"]
SCRIPT_ROOTS = [ROOT / "scripts"]

CONTROL_WORDS = {"if", "for", "while", "switch", "catch", "try", "else", "do", "return", "new", "case", "default"}

CLASS_RE = re.compile(r"\b(?:public\s+|private\s+|protected\s+|abstract\s+|final\s+|static\s+)*(class|interface|enum|record)\s+([A-Za-z_$][\w$]*)")
METHOD_RE = re.compile(
    r"^\s*(?:(?:public|private|protected|static|final|synchronized|abstract|native|strictfp)\s+)*"
    r"(?:(?:[A-Za-z_$][\w$<>\[\], ?.&]*|void|int|long|double|float|boolean|char|byte|short)\s+)?"
    r"(?P<name>[A-Za-z_$][\w$]*)\s*\([^;{}]*\)\s*(?:throws\s+[^{}]+)?\{"
)

ZONE_RULES: List[Tuple[str, Sequence[str]]] = [
    ("ui.render.surface", [r"paint|draw|render|surface|painter|graphics|hud|overlay|splash|menu|panel|frame|color|font"]),
    ("ui.input.navigation", [r"key|mouse|wheel|scroll|input|controller|gamepad|keyboard|route|navigation|button"]),
    ("runtime.options", [r"option|display|graphics|sound|volume|jvm|runtime|accessibility|scale|density|doom"]),
    ("world.generation.transition", [r"world|zone|sector|atlas|generation|transition|audit|room|road|plaza|frontage"]),
    ("inventory.items.persistence", [r"inventory|item|container|loot|equip|save|load|autosave|profile|persistence|storage"]),
    ("interaction.fixtures.machines", [r"fixture|machine|vending|interact|use|hack|smelter|assembler|relay|turret|defense"]),
    ("combat.entities.simulation", [r"combat|attack|damage|npc|entity|turn|movement|path|faction|heat|suspicion|simulation"]),
    ("assets.registry.art", [r"asset|registry|tile|art|image|glyph|semantic|infopedia|texture|atlas"]),
    ("server.authority.launcher", [r"server|authoritative|launcher|multiplayer|session|client|bridge|host"]),
    ("diagnostics.smoke.audit", [r"debug|diagnostic|smoke|audit|test|validation|report|ledger"]),
]

DOC_ACTIVE_HINTS = [
    "master development plan",
    "standards",
    "practices",
    "development history",
    "readme",
    "governance",
    "architecture_map",
    "shard_mining_method",
    "compatibility_ledger",
    "function_map",
]

DOC_RETIRE_HINTS = [
    "old", "archive", "archived", "backup", "copy", "duplicate", "draft", "tmp", "temp", "deprecated", "obsolete",
    "shard", "smoke", "generated_subsystems", "retired_shards", "diagnostics"
]

@dataclasses.dataclass(frozen=True)
class JavaClass:
    path: str
    name: str
    kind: str
    line: int
    owner_zone: str
    file_lines: int
    sha256: str

@dataclasses.dataclass(frozen=True)
class JavaFunction:
    path: str
    class_name: str
    name: str
    signature: str
    start_line: int
    end_line: int
    owner_zone: str
    visibility: str
    is_static: bool
    line_count: int
    sha256: str

@dataclasses.dataclass(frozen=True)
class FileRecord:
    path: str
    kind: str
    bytes: int
    lines: int
    sha256: str

@dataclasses.dataclass(frozen=True)
class DocRecord:
    path: str
    title: str
    status_guess: str
    reason: str
    bytes: int
    lines: int
    sha256: str


def rel(path: Path) -> str:
    return path.resolve().relative_to(ROOT.resolve()).as_posix()


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def sha(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8", errors="replace")).hexdigest()


def safe_lines(text: str) -> List[str]:
    return text.replace("\r\n", "\n").replace("\r", "\n").splitlines(keepends=True)


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


def classify_zone(path: str, name: str, signature: str = "") -> str:
    hay = f"{path}\n{name}\n{signature}".lower()
    for zone, patterns in ZONE_RULES:
        if any(re.search(p, hay, re.I) for p in patterns):
            return zone
    return "unclassified"


def visibility_of(signature: str) -> str:
    s = signature.strip()
    for v in ("public", "private", "protected"):
        if re.search(r"(^|\s)" + v + r"\s", s):
            return v
    return "package"


def signature_name(signature: str) -> Optional[str]:
    compact = " ".join(signature.strip().split())
    if not compact or ";" in compact.split("{")[0]:
        return None
    m = METHOD_RE.search(compact)
    if not m:
        return None
    name = m.group("name")
    if name in CONTROL_WORDS:
        return None
    return name


def parse_signature_at(lines: Sequence[str], idx: int, max_lines: int = 12) -> Tuple[Optional[str], Optional[int], str]:
    sig_lines: List[str] = []
    parens = 0
    for off in range(max_lines):
        j = idx + off
        if j >= len(lines):
            return None, None, ""
        raw = lines[j]
        stripped = raw.strip()
        if off == 0:
            if not stripped or stripped.startswith(("@", "//", "*", "/*")):
                return None, None, ""
            first = re.split(r"\s+|\(", stripped, maxsplit=1)[0]
            if first in CONTROL_WORDS or ("=" in stripped and "(" not in stripped):
                return None, None, ""
        sig_lines.append(raw)
        safe, _ = neutralize(raw)
        parens += safe.count("(") - safe.count(")")
        if "{" in safe and parens <= 0:
            signature = " ".join(line.strip() for line in sig_lines)
            name = signature_name(signature)
            return (name, j, signature) if name else (None, None, "")
        if ";" in safe and parens <= 0:
            return None, None, ""
    return None, None, ""


def parse_java(path: Path) -> Tuple[List[JavaClass], List[JavaFunction]]:
    text = read_text(path)
    lines = safe_lines(text)
    relative = rel(path)
    classes: List[JavaClass] = []
    functions: List[JavaFunction] = []

    # Class map by nearest preceding declaration. This is intentionally simple and deterministic.
    class_at_line: List[Tuple[int, str]] = []
    for i, line in enumerate(lines, start=1):
        m = CLASS_RE.search(line)
        if m:
            kind, name = m.group(1), m.group(2)
            zone = classify_zone(relative, name)
            classes.append(JavaClass(relative, name, kind, i, zone, len(lines), sha(text)))
            class_at_line.append((i, name))
    fallback_class = path.stem

    def nearest_class(line_no: int) -> str:
        prior = [name for line, name in class_at_line if line <= line_no]
        return prior[-1] if prior else fallback_class

    i = 0
    while i < len(lines):
        name, sig_end, signature = parse_signature_at(lines, i)
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
        class_name = nearest_class(i + 1)
        zone = classify_zone(relative, name, signature)
        functions.append(JavaFunction(
            path=relative,
            class_name=class_name,
            name=name,
            signature=" ".join(signature.split()),
            start_line=i + 1,
            end_line=end + 1,
            owner_zone=zone,
            visibility=visibility_of(signature),
            is_static=bool(re.search(r"(^|\s)static\s", signature)),
            line_count=end - i + 1,
            sha256=sha(block),
        ))
        i = end + 1
    return classes, functions


def gather_files() -> List[Path]:
    paths: List[Path] = []
    for root in [ROOT / "src", ROOT / "ROOT_DOCS", ROOT / "docs", ROOT / "scripts"]:
        if root.exists():
            for p in root.rglob("*"):
                if p.is_file() and ".git" not in p.parts:
                    paths.append(p)
    for p in ROOT.glob("README*"):
        if p.is_file():
            paths.append(p)
    return sorted(set(paths), key=lambda p: rel(p))


def file_kind(path: Path) -> str:
    suffix = path.suffix.lower()
    if suffix == ".java":
        return "java"
    if suffix in {".md", ".txt", ".rst"}:
        return "document"
    if suffix in {".py", ".ps1", ".bat", ".sh"}:
        return "script"
    if suffix in {".tsv", ".csv", ".json", ".xml", ".properties"}:
        return "data"
    return "other"


def inventory_file(path: Path) -> FileRecord:
    data = path.read_bytes()
    try:
        text = data.decode("utf-8", errors="replace")
        lines = len(text.splitlines())
        digest = hashlib.sha256(text.encode("utf-8", errors="replace")).hexdigest()
    except Exception:
        lines = 0
        digest = hashlib.sha256(data).hexdigest()
    return FileRecord(rel(path), file_kind(path), len(data), lines, digest)


def first_heading(text: str, fallback: str) -> str:
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("#"):
            return stripped.lstrip("#").strip() or fallback
    return fallback


def document_status(path: Path) -> DocRecord:
    text = read_text(path)
    r = rel(path)
    lowered = f"{r}\n{text[:2000]}".lower()
    title = first_heading(text, path.stem)
    title_key = re.sub(r"[^a-z0-9]+", " ", title.lower()).strip()
    path_key = r.lower()
    if any(h in lowered for h in DOC_ACTIVE_HINTS):
        status = "active_or_governance"
        reason = "matches active governance/function/shard-map hint"
    elif any(h in path_key for h in DOC_RETIRE_HINTS):
        status = "review_for_retirement"
        reason = "path/name matches generated, retired, shard, smoke, diagnostic, archive, or duplicate hint"
    elif len(text.encode("utf-8", errors="replace")) < 512:
        status = "review_for_retirement"
        reason = "very small document; may be stub or obsolete"
    else:
        status = "unknown_review"
        reason = "no active or retirement hint matched"
    return DocRecord(r, title_key or title, status, reason, len(text.encode("utf-8", errors="replace")), len(text.splitlines()), sha(text))


def write_tsv(path: Path, header: Sequence[str], rows: Iterable[Sequence[object]]) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as f:
        f.write("\t".join(header) + "\n")
        for row in rows:
            f.write("\t".join(str(x).replace("\t", " ").replace("\n", " ") for x in row) + "\n")


def build_reports(out_dir: Path) -> None:
    stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    files = [inventory_file(p) for p in gather_files()]
    java_paths = [ROOT / f.path for f in files if f.kind == "java"]
    classes: List[JavaClass] = []
    functions: List[JavaFunction] = []
    for p in java_paths:
        try:
            cs, fs = parse_java(p)
            classes.extend(cs)
            functions.extend(fs)
        except Exception as ex:
            functions.append(JavaFunction(rel(p), p.stem, "__PARSE_ERROR__", str(ex), 0, 0, "diagnostics.smoke.audit", "package", False, 0, ""))

    doc_paths = [ROOT / f.path for f in files if f.kind == "document"]
    docs = [document_status(p) for p in doc_paths]

    out_dir.mkdir(parents=True, exist_ok=True)
    write_tsv(out_dir / "FILE_INVENTORY.tsv", ["path", "kind", "bytes", "lines", "sha256"],
              [(f.path, f.kind, f.bytes, f.lines, f.sha256) for f in files])
    write_tsv(out_dir / "CLASS_MAP.tsv", ["path", "class", "kind", "line", "owner_zone", "file_lines", "sha256"],
              [(c.path, c.name, c.kind, c.line, c.owner_zone, c.file_lines, c.sha256) for c in classes])
    write_tsv(out_dir / "FUNCTION_MAP.tsv", ["path", "class", "function", "start", "end", "lines", "visibility", "static", "owner_zone", "sha256", "signature"],
              [(fn.path, fn.class_name, fn.name, fn.start_line, fn.end_line, fn.line_count, fn.visibility, fn.is_static, fn.owner_zone, fn.sha256, fn.signature) for fn in functions])

    by_name: Dict[str, List[JavaFunction]] = defaultdict(list)
    for fn in functions:
        by_name[fn.name].append(fn)
    duplicate_rows: List[Tuple[object, ...]] = []
    for name, items in sorted(by_name.items()):
        paths = sorted({i.path for i in items})
        classes_seen = sorted({i.class_name for i in items})
        if len(items) > 1 and name != "__PARSE_ERROR__":
            duplicate_rows.append((name, len(items), len(paths), ", ".join(classes_seen[:20]), ", ".join(paths[:20])))
    write_tsv(out_dir / "DUPLICATE_FUNCTION_NAMES.tsv", ["function", "count", "file_count", "classes", "paths"], duplicate_rows)

    oversized_rows = []
    for f in files:
        if f.kind == "java" and (f.lines >= 800 or f.bytes >= 120_000):
            oversized_rows.append((f.path, f.bytes, f.lines, "oversized_java_owner"))
        elif f.kind == "document" and (f.bytes >= 250_000 or "generated_subsystems" in f.path):
            oversized_rows.append((f.path, f.bytes, f.lines, "large_or_generated_document"))
    write_tsv(out_dir / "OVERSIZED_OWNERS.tsv", ["path", "bytes", "lines", "reason"], oversized_rows)

    write_tsv(out_dir / "DOCUMENT_INVENTORY.tsv", ["path", "title_key", "status_guess", "reason", "bytes", "lines", "sha256"],
              [(d.path, d.title, d.status_guess, d.reason, d.bytes, d.lines, d.sha256) for d in docs])

    title_groups: Dict[str, List[DocRecord]] = defaultdict(list)
    for d in docs:
        title_groups[d.title].append(d)
    retire_rows = []
    for d in docs:
        if d.status_guess == "review_for_retirement":
            retire_rows.append((d.path, d.title, d.reason, d.bytes, d.lines))
    for title, group in sorted(title_groups.items()):
        if len(group) > 1 and title:
            for d in group:
                retire_rows.append((d.path, d.title, f"duplicate title group size {len(group)}", d.bytes, d.lines))
    write_tsv(out_dir / "DOC_RETIREMENT_CANDIDATES.tsv", ["path", "title_key", "reason", "bytes", "lines"], retire_rows)

    zone_counts = Counter(fn.owner_zone for fn in functions)
    class_zone_counts = Counter(c.owner_zone for c in classes)
    doc_status_counts = Counter(d.status_guess for d in docs)
    with (out_dir / "SUMMARY.md").open("w", encoding="utf-8", newline="\n") as f:
        f.write("# Function Map Summary\n\n")
        f.write(f"Generated: `{stamp}`\n\n")
        f.write("## Counts\n\n")
        f.write(f"- Files inventoried: `{len(files)}`\n")
        f.write(f"- Java classes/interfaces/enums/records: `{len(classes)}`\n")
        f.write(f"- Java functions/methods parsed: `{len(functions)}`\n")
        f.write(f"- Duplicate function-name groups: `{len(duplicate_rows)}`\n")
        f.write(f"- Documents inventoried: `{len(docs)}`\n")
        f.write(f"- Document retirement candidate rows: `{len(retire_rows)}`\n")
        f.write(f"- Oversized owner rows: `{len(oversized_rows)}`\n\n")
        f.write("## Function Owner Zones\n\n")
        for zone, count in sorted(zone_counts.items()):
            f.write(f"- `{zone}`: `{count}` functions\n")
        f.write("\n## Class Owner Zones\n\n")
        for zone, count in sorted(class_zone_counts.items()):
            f.write(f"- `{zone}`: `{count}` classes\n")
        f.write("\n## Document Status Guesses\n\n")
        for status, count in sorted(doc_status_counts.items()):
            f.write(f"- `{status}`: `{count}` documents\n")
        f.write("\n## Next Use\n\n")
        f.write("Use FUNCTION_MAP.tsv as the canonical raw inventory, then promote verified ownership decisions into ROOT_DOCS/functionmap/Function_Ownership_Ledger.md. Do not delete documents from DOC_RETIREMENT_CANDIDATES.tsv until a human confirms that they are obsolete or duplicated by active governance docs.\n")


def main(argv: Optional[Sequence[str]] = None) -> int:
    ap = argparse.ArgumentParser(description="Build non-destructive Java function and documentation map reports.")
    ap.add_argument("--out-dir", default=str(OUT_DIR), help="Output directory for generated reports.")
    ap.add_argument("--apply", action="store_true", help="Write reports. Without this, only print target path and exit.")
    args = ap.parse_args(argv)
    out_dir = Path(args.out_dir).resolve()
    if not args.apply:
        print("Dry run. Re-run with --apply to write reports.")
        print(f"Target output: {out_dir}")
        return 0
    build_reports(out_dir)
    print(f"Function map reports written to {out_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
