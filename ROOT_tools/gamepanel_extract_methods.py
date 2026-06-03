#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src" / "mechanist" / "GamePanel.java"
OUT_DIR = ROOT / "docs" / "gamepanel_method_extracts"
OUT_JSON = ROOT / "docs" / "gamepanel_method_extracts.json"
METHOD_RE = re.compile(r"(?P<prefix>\b(?:public|protected|private|static|final|synchronized|abstract|native|strictfp|default)\b[\w\s<>,\[\].?@]*|[A-Za-z_$][\w\s<>,\[\].?@]*)\s+(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)\s*\([^;{}]*\)\s*(?:throws\s+[\w\s,.<>]+)?\{")
DEFAULT_METHODS = ["rebuildButtons", "drawIntroCrawl", "drawGame", "panelLines", "drawOptions", "mouseClicked", "handleKeyPressed"]
CONTROL = {"if", "for", "while", "switch", "catch", "try", "do", "else", "synchronized", "new"}

@dataclass
class ExtractedMethod:
    name: str
    start_line: int
    end_line: int
    line_count: int
    output: str


def mask(text: str) -> str:
    c = list(text); i = 0; n = len(c)
    while i < n:
        ch = c[i]; nx = c[i+1] if i+1 < n else ""
        if ch == "/" and nx == "/":
            while i < n and c[i] != "\n":
                c[i] = " "; i += 1
            continue
        if ch == "/" and nx == "*":
            c[i] = c[i+1] = " "; i += 2
            while i+1 < n and not (c[i] == "*" and c[i+1] == "/"):
                if c[i] not in "\r\n": c[i] = " "
                i += 1
            if i+1 < n:
                c[i] = c[i+1] = " "; i += 2
            continue
        if ch in ('"', "'"):
            q = ch; c[i] = " "; i += 1; esc = False
            while i < n:
                cur = c[i]
                if cur not in "\r\n": c[i] = " "
                if esc: esc = False
                elif cur == "\\": esc = True
                elif cur == q:
                    i += 1; break
                i += 1
            continue
        i += 1
    return "".join(c)


def line_starts(text: str) -> list[int]:
    starts = [0]
    for i, ch in enumerate(text):
        if ch == "\n": starts.append(i+1)
    return starts


def line_no(starts: list[int], pos: int) -> int:
    lo, hi = 0, len(starts)-1
    while lo <= hi:
        mid = (lo+hi)//2
        if starts[mid] <= pos: lo = mid+1
        else: hi = mid-1
    return hi+1


def depths(masked: str) -> list[int]:
    out = [0]*len(masked); d = 0
    for i, ch in enumerate(masked):
        out[i] = d
        if ch == "{": d += 1
        elif ch == "}": d = max(0, d-1)
    return out


def close_brace(masked: str, open_pos: int) -> int:
    d = 0
    for i in range(open_pos, len(masked)):
        if masked[i] == "{": d += 1
        elif masked[i] == "}":
            d -= 1
            if d == 0: return i
    raise RuntimeError("unclosed method brace")


def extract_methods(names: list[str]) -> list[ExtractedMethod]:
    if not SRC.exists():
        raise FileNotFoundError(f"Missing {SRC}")
    text = SRC.read_text(encoding="utf-8", errors="replace")
    lines = text.splitlines()
    masked = mask(text); starts = line_starts(text); dep = depths(masked)
    wanted = set(names)
    found: list[ExtractedMethod] = []
    for hit in METHOD_RE.finditer(masked):
        if dep[hit.start()] != 1: continue
        name = hit.group("name")
        if name in CONTROL or name not in wanted: continue
        ob = masked.find("{", hit.start())
        cb = close_brace(masked, ob)
        start = line_no(starts, hit.start())
        end = line_no(starts, cb)
        method_text = "\n".join(lines[start-1:end]) + "\n"
        out = OUT_DIR / f"{name}.javafrag"
        out.parent.mkdir(parents=True, exist_ok=True)
        header = f"// Extracted from src/mechanist/GamePanel.java lines {start}-{end}.\n// This is an examination slice, not compiled source.\n\n"
        out.write_text(header + method_text, encoding="utf-8")
        found.append(ExtractedMethod(name, start, end, end-start+1, str(out.relative_to(ROOT)).replace("\\", "/")))
    missing = wanted - {m.name for m in found}
    if missing:
        raise RuntimeError("Missing requested methods: " + ", ".join(sorted(missing)))
    found.sort(key=lambda m: names.index(m.name))
    return found


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Extract manageable GamePanel method slices into docs for review.")
    parser.add_argument("methods", nargs="*", help="Method names to extract. Defaults to the immediate seam targets.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    names = args.methods or DEFAULT_METHODS
    extracted = extract_methods(names)
    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    OUT_JSON.write_text(json.dumps({
        "schema": "mechanist.gamepanel_method_extracts.v1",
        "time": datetime.now(timezone.utc).isoformat(),
        "source": str(SRC),
        "methods": [asdict(m) for m in extracted],
    }, indent=2), encoding="utf-8")
    print(f"Extracted GamePanel method slices: {len(extracted)}")
    for m in extracted:
        print(f"  {m.name}: {m.start_line}-{m.end_line} -> {m.output}")
    print(f"Index: {OUT_JSON}")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
