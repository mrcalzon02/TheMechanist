#!/usr/bin/env python3
"""
BUILD_SEMANTIC_TEXT_CANDIDATES.py

Builds a staged migration ledger for player/user-facing text literals.

This tool does not rewrite Java.  It scans source files for string literals that
look like UI/player text, assigns proposed semantic keys, and writes candidate
locale entries.  Migration should happen in small passes: replace a literal with
SemanticTextManager.text(key), run smoke, then promote the key to the reviewed
locale file.

Run from repository root:

    py -3 scripts\BUILD_SEMANTIC_TEXT_CANDIDATES.py --apply

Outputs:

    ROOT_docs/functionmap/generated/SEMANTIC_TEXT_CANDIDATES.tsv
    ROOT_docs/functionmap/generated/SEMANTIC_TEXT_CANDIDATES.properties
    ROOT_docs/functionmap/generated/SEMANTIC_TEXT_SUMMARY.md
"""

from __future__ import annotations

import argparse
import dataclasses
import datetime as dt
import hashlib
import re
from collections import Counter
from pathlib import Path
from typing import Iterable, List, Optional, Sequence

ROOT = Path.cwd()
SRC_ROOT = ROOT / "src"
OUT_DIR = ROOT / "ROOT_docs" / "functionmap" / "generated"

STRING_RE = re.compile(r'"(?:\\.|[^"\\])*"')
SKIP_PATH_PARTS = {"target", "build", ".git"}
SKIP_LITERAL_PREFIXES = ("/", "\\", "http", "https", "jdbc", "file:", "classpath:")
SKIP_LITERAL_EXACT = {"", " ", "\n", "\t", "true", "false", "null", "UTF-8", "utf-8"}

@dataclasses.dataclass(frozen=True)
class TextCandidate:
    path: str
    line: int
    semantic_key: str
    literal: str
    reason: str
    sha256: str


def rel(path: Path) -> str:
    return path.resolve().relative_to(ROOT.resolve()).as_posix()


def decode_java_literal(raw: str) -> str:
    inner = raw[1:-1]
    try:
        return bytes(inner, "utf-8").decode("unicode_escape")
    except Exception:
        return inner.replace('\\"', '"').replace('\\n', '\n').replace('\\t', '\t')


def looks_player_facing(text: str) -> bool:
    if text in SKIP_LITERAL_EXACT:
        return False
    stripped = text.strip()
    if len(stripped) < 3:
        return False
    low = stripped.lower()
    if low.startswith(SKIP_LITERAL_PREFIXES):
        return False
    if re.fullmatch(r"[A-Z0-9_.$:/\\-]+", stripped):
        return False
    if re.fullmatch(r"[a-z0-9_.:/\\-]+", stripped) and " " not in stripped:
        return False
    if any(ch.isalpha() for ch in stripped) and (" " in stripped or any(ch in stripped for ch in ":.!?,-—/")):
        return True
    return False


def reason_for(path: str, text: str) -> str:
    low_path = path.lower()
    low = text.lower()
    if "button" in low_path or "menu" in low_path or "screen" in low_path or "panel" in low_path:
        return "ui_surface_text"
    if "trade" in low_path or "vending" in low_path or "inventory" in low_path:
        return "commerce_or_inventory_text"
    if "error" in low or "missing" in low or "failed" in low or "invalid" in low:
        return "error_or_status_text"
    if "debug" in low_path or "diagnostic" in low_path or "smoke" in low_path:
        return "diagnostic_text_review"
    return "candidate_player_text"


def key_for(path: str, line: int, text: str) -> str:
    stem = Path(path).stem
    stem_key = re.sub(r"[^a-z0-9]+", ".", stem.lower()).strip(".")
    words = re.findall(r"[a-zA-Z0-9]+", text.lower())[:6]
    text_key = ".".join(words) if words else "text"
    digest = hashlib.sha1((path + str(line) + text).encode("utf-8", errors="replace")).hexdigest()[:8]
    return f"auto.{stem_key}.{line}.{text_key}.{digest}"


def properties_escape(text: str) -> str:
    return text.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t")


def gather_candidates() -> List[TextCandidate]:
    out: List[TextCandidate] = []
    if not SRC_ROOT.exists():
        return out
    for path in sorted(SRC_ROOT.rglob("*.java"), key=lambda p: rel(p)):
        if any(part in SKIP_PATH_PARTS for part in path.parts):
            continue
        relative = rel(path)
        try:
            lines = path.read_text(encoding="utf-8", errors="replace").splitlines()
        except Exception:
            continue
        for i, line in enumerate(lines, start=1):
            if "SemanticTextManager" in line or "ItemCatalog" in line:
                continue
            for match in STRING_RE.finditer(line):
                literal = decode_java_literal(match.group(0))
                if not looks_player_facing(literal):
                    continue
                key = key_for(relative, i, literal)
                out.append(TextCandidate(relative, i, key, literal, reason_for(relative, literal), hashlib.sha256(literal.encode("utf-8", errors="replace")).hexdigest()))
    return out


def write_tsv(path: Path, header: Sequence[str], rows: Iterable[Sequence[object]]) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as f:
        f.write("\t".join(header) + "\n")
        for row in rows:
            f.write("\t".join(str(x).replace("\t", " ").replace("\n", "\\n") for x in row) + "\n")


def write_reports(candidates: List[TextCandidate]) -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    stamp = dt.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    write_tsv(
        OUT_DIR / "SEMANTIC_TEXT_CANDIDATES.tsv",
        ["path", "line", "semantic_key", "reason", "sha256", "literal"],
        [(c.path, c.line, c.semantic_key, c.reason, c.sha256, c.literal) for c in candidates],
    )
    with (OUT_DIR / "SEMANTIC_TEXT_CANDIDATES.properties").open("w", encoding="utf-8", newline="\n") as f:
        f.write("# Candidate locale entries generated for staged review.\n")
        f.write("# Do not bulk-apply without smoke-tested Java replacements.\n")
        f.write(f"# Generated: {stamp}\n\n")
        for c in candidates:
            f.write(f"# {c.path}:{c.line} {c.reason}\n")
            f.write(f"{c.semantic_key}={properties_escape(c.literal)}\n")
    by_reason = Counter(c.reason for c in candidates)
    by_file = Counter(c.path for c in candidates)
    with (OUT_DIR / "SEMANTIC_TEXT_SUMMARY.md").open("w", encoding="utf-8", newline="\n") as f:
        f.write("# Semantic Text Candidate Summary\n\n")
        f.write(f"Generated: `{stamp}`\n\n")
        f.write(f"- Candidate literals: `{len(candidates)}`\n")
        f.write(f"- Files with candidates: `{len(by_file)}`\n\n")
        f.write("## Candidate Reasons\n\n")
        for reason, count in sorted(by_reason.items()):
            f.write(f"- `{reason}`: `{count}`\n")
        f.write("\n## Top Files\n\n")
        for path, count in by_file.most_common(25):
            f.write(f"- `{path}`: `{count}` candidates\n")
        f.write("\n## Migration Rule\n\n")
        f.write("Replace literals in small batches with `SemanticTextManager.text(key)`, add reviewed keys to `PACKAGE_client/assets/locales/en.properties`, then run the function-map operations smoke.\n")


def main(argv: Optional[Sequence[str]] = None) -> int:
    ap = argparse.ArgumentParser(description="Build semantic text candidate reports.")
    ap.add_argument("--apply", action="store_true", help="Write reports.")
    args = ap.parse_args(argv)
    candidates = gather_candidates()
    print(f"Semantic text candidates: {len(candidates)}")
    if not args.apply:
        print("Dry run only. Re-run with --apply to write candidate reports.")
        return 0
    write_reports(candidates)
    print(f"Wrote semantic text candidate reports under {OUT_DIR}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
