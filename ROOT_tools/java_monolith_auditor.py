#!/usr/bin/env python3
"""Audit oversized Java source files and produce a safe extraction plan.

This tool is intentionally read-only with respect to Java source. It does not
move classes, rewrite imports, rename symbols, or change runtime behavior.

Default target:
  src/mechanist/TheMechanist.java

Default outputs:
  docs/java_monolith_audit.tsv
  docs/java_monolith_extraction_plan.md
  docs/java_monolith_audit.json

The first refactor phase should use this report to extract package-private
TOP-LEVEL declarations into same-package files without behavior changes. Do not
start by splitting GamePanel internals.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
DEFAULT_SOURCE = REPO_ROOT / "src/mechanist/TheMechanist.java"
DEFAULT_TSV = REPO_ROOT / "docs/java_monolith_audit.tsv"
DEFAULT_JSON = REPO_ROOT / "docs/java_monolith_audit.json"
DEFAULT_PLAN = REPO_ROOT / "docs/java_monolith_extraction_plan.md"

DECLARATION_PATTERN = re.compile(
    r"\b(?P<kind>class|record|interface|enum)\s+(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)\b"
)
METHOD_PATTERN = re.compile(
    r"(?P<prefix>\b(?:public|protected|private|static|final|synchronized|abstract|native|strictfp|default)\b[\w\s<>,\[\].?@]*|[A-Za-z_$][\w\s<>,\[\].?@]*)\s+"
    r"(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)\s*\([^;{}]*\)\s*(?:throws\s+[\w\s,.<>]+)?\{"
)
CONTROL_WORDS = {"if", "for", "while", "switch", "catch", "try", "do", "else", "synchronized", "new"}


@dataclass(frozen=True)
class JavaDeclaration:
    kind: str
    name: str
    start_line: int
    end_line: int
    line_count: int
    public: bool
    package_private: bool
    top_level_index: int
    extract_recommendation: str
    extraction_phase: str
    notes: str


@dataclass(frozen=True)
class JavaMethodCandidate:
    owner: str
    name: str
    start_line: int
    approximate_end_line: int
    approximate_line_count: int
    notes: str


def read_text(path: Path) -> str:
    if not path.exists():
        raise FileNotFoundError(f"Java source not found: {path}")
    return path.read_text(encoding="utf-8", errors="replace")


def line_starts(text: str) -> list[int]:
    starts = [0]
    for index, ch in enumerate(text):
        if ch == "\n":
            starts.append(index + 1)
    return starts


def line_number(starts: list[int], pos: int) -> int:
    lo = 0
    hi = len(starts) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if starts[mid] <= pos:
            lo = mid + 1
        else:
            hi = mid - 1
    return hi + 1


def mask_java_non_code(text: str) -> str:
    """Replace comments and string/char/text-block content with spaces, preserving newlines."""
    chars = list(text)
    i = 0
    n = len(chars)
    while i < n:
        ch = chars[i]
        nxt = chars[i + 1] if i + 1 < n else ""

        if ch == "/" and nxt == "/":
            j = i
            while j < n and chars[j] != "\n":
                if chars[j] != "\r":
                    chars[j] = " "
                j += 1
            i = j
            continue

        if ch == "/" and nxt == "*":
            chars[i] = chars[i + 1] = " "
            i += 2
            while i + 1 < n and not (chars[i] == "*" and chars[i + 1] == "/"):
                if chars[i] not in "\r\n":
                    chars[i] = " "
                i += 1
            if i + 1 < n:
                chars[i] = chars[i + 1] = " "
                i += 2
            continue

        if text.startswith('"""', i):
            chars[i] = chars[i + 1] = chars[i + 2] = " "
            i += 3
            while i + 2 < n and not text.startswith('"""', i):
                if chars[i] not in "\r\n":
                    chars[i] = " "
                i += 1
            if i + 2 < n:
                chars[i] = chars[i + 1] = chars[i + 2] = " "
                i += 3
            continue

        if ch in {'"', "'"}:
            quote = ch
            chars[i] = " "
            i += 1
            escaped = False
            while i < n:
                current = chars[i]
                if current not in "\r\n":
                    chars[i] = " "
                if escaped:
                    escaped = False
                elif current == "\\":
                    escaped = True
                elif current == quote:
                    i += 1
                    break
                i += 1
            continue

        i += 1
    return "".join(chars)


def brace_depths(masked: str) -> list[int]:
    depths = [0] * len(masked)
    depth = 0
    for i, ch in enumerate(masked):
        depths[i] = depth
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth = max(0, depth - 1)
    return depths


def find_matching_brace(masked: str, open_pos: int) -> int:
    if open_pos < 0 or open_pos >= len(masked) or masked[open_pos] != "{":
        return -1
    depth = 0
    for i in range(open_pos, len(masked)):
        if masked[i] == "{":
            depth += 1
        elif masked[i] == "}":
            depth -= 1
            if depth == 0:
                return i
    return -1


def declaration_start_line(text: str, starts: list[int], keyword_pos: int) -> int:
    line = line_number(starts, keyword_pos)
    # Pull declaration start upward over annotations/modifiers on immediately preceding lines.
    current = line
    lines = text.splitlines()
    while current > 1:
        prev = lines[current - 2].strip()
        if not prev:
            break
        if prev.startswith("@") or re.search(r"\b(public|protected|private|static|final|abstract|sealed|non-sealed|strictfp)\b", prev):
            current -= 1
            continue
        break
    return current


def find_top_level_declarations(text: str) -> list[JavaDeclaration]:
    masked = mask_java_non_code(text)
    starts = line_starts(text)
    depths = brace_depths(masked)
    declarations: list[JavaDeclaration] = []

    for match in DECLARATION_PATTERN.finditer(masked):
        if depths[match.start()] != 0:
            continue
        kind = match.group("kind")
        name = match.group("name")
        open_brace = masked.find("{", match.end())
        if open_brace < 0:
            continue
        close_brace = find_matching_brace(masked, open_brace)
        if close_brace < 0:
            close_brace = len(masked) - 1

        start_line = declaration_start_line(text, starts, match.start())
        end_line = line_number(starts, close_brace)
        line_count = max(1, end_line - start_line + 1)
        prefix_start = starts[start_line - 1]
        prefix = masked[prefix_start:match.start()]
        is_public = bool(re.search(r"\bpublic\b", prefix))
        is_package_private = not bool(re.search(r"\b(public|private|protected)\b", prefix))
        index = len(declarations) + 1
        recommendation, phase, notes = recommendation_for(name, is_public, is_package_private, line_count, index)
        declarations.append(JavaDeclaration(kind, name, start_line, end_line, line_count, is_public, is_package_private, index, recommendation, phase, notes))

    return declarations


def recommendation_for(name: str, is_public: bool, is_package_private: bool, line_count: int, index: int) -> tuple[str, str, str]:
    if name == "TheMechanist":
        return ("keep_in_place", "phase_0_anchor", "Public application entrypoint. Keep file anchor stable while extracting siblings.")
    if name == "GamePanel":
        return ("defer", "phase_2_gamepanel", "Large stateful runtime panel. Do not split until top-level sibling classes are extracted and compile-clean.")
    if is_package_private:
        size_note = "large" if line_count >= 500 else "small"
        return ("extract_same_package", "phase_1_top_level_siblings", f"Package-private top-level {size_note} declaration. Move to src/mechanist/{name}.java with same package and preserved imports.")
    if is_public:
        return ("review", "phase_1_review", "Unexpected additional public declaration. Java may already require its own file; inspect before moving.")
    return ("review", "phase_1_review", "Non-public top-level declaration but visibility was not plain package-private; inspect before moving.")


def find_methods_for_declaration(text: str, declaration: JavaDeclaration) -> list[JavaMethodCandidate]:
    lines = text.splitlines()
    block_text = "\n".join(lines[declaration.start_line - 1:declaration.end_line])
    masked = mask_java_non_code(block_text)
    starts = line_starts(block_text)
    depths = brace_depths(masked)
    methods: list[JavaMethodCandidate] = []

    for match in METHOD_PATTERN.finditer(masked):
        if depths[match.start()] != 1:
            continue
        name = match.group("name")
        if name in CONTROL_WORDS:
            continue
        open_brace = masked.find("{", match.start())
        if open_brace < 0:
            continue
        close_brace = find_matching_brace(masked, open_brace)
        if close_brace < 0:
            continue
        start = declaration.start_line + line_number(starts, match.start()) - 1
        end = declaration.start_line + line_number(starts, close_brace) - 1
        line_count = max(1, end - start + 1)
        if line_count >= 80:
            note = "oversized_method_candidate" if line_count >= 160 else "large_method_candidate"
            methods.append(JavaMethodCandidate(declaration.name, name, start, end, line_count, note))

    return methods


def write_tsv(declarations: list[JavaDeclaration], methods: list[JavaMethodCandidate], output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", encoding="utf-8", newline="") as handle:
        fieldnames = [
            "record_type", "kind", "name", "owner", "start_line", "end_line", "line_count",
            "public", "package_private", "top_level_index", "extract_recommendation", "extraction_phase", "notes"
        ]
        writer = csv.DictWriter(handle, fieldnames=fieldnames, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        for item in declarations:
            writer.writerow({
                "record_type": "top_level_declaration",
                "kind": item.kind,
                "name": item.name,
                "owner": "",
                "start_line": item.start_line,
                "end_line": item.end_line,
                "line_count": item.line_count,
                "public": str(item.public).lower(),
                "package_private": str(item.package_private).lower(),
                "top_level_index": item.top_level_index,
                "extract_recommendation": item.extract_recommendation,
                "extraction_phase": item.extraction_phase,
                "notes": item.notes,
            })
        for item in methods:
            writer.writerow({
                "record_type": "method_candidate",
                "kind": "method",
                "name": item.name,
                "owner": item.owner,
                "start_line": item.start_line,
                "end_line": item.approximate_end_line,
                "line_count": item.approximate_line_count,
                "public": "",
                "package_private": "",
                "top_level_index": "",
                "extract_recommendation": "inspect_later",
                "extraction_phase": "phase_3_internal_method_extraction",
                "notes": item.notes,
            })


def write_json(source: Path, declarations: list[JavaDeclaration], methods: list[JavaMethodCandidate], output: Path) -> None:
    payload = {
        "schema": "mechanist.java_monolith_audit.v1",
        "source": str(source),
        "top_level_declaration_count": len(declarations),
        "large_method_candidate_count": len(methods),
        "declarations": [asdict(item) for item in declarations],
        "large_methods": [asdict(item) for item in methods],
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(payload, indent=2), encoding="utf-8")


def markdown_table(rows: Iterable[list[str]]) -> str:
    rows = list(rows)
    if not rows:
        return ""
    header = rows[0]
    lines = ["| " + " | ".join(header) + " |", "|" + "|".join("---" for _ in header) + "|"]
    for row in rows[1:]:
        safe = [str(cell).replace("|", "\\|") for cell in row]
        lines.append("| " + " | ".join(safe) + " |")
    return "\n".join(lines)


def write_plan(source: Path, declarations: list[JavaDeclaration], methods: list[JavaMethodCandidate], output: Path) -> None:
    phase_1 = [item for item in declarations if item.extraction_phase == "phase_1_top_level_siblings"]
    phase_1_sorted = sorted(phase_1, key=lambda item: (item.line_count, item.name.lower()))
    game_panel = next((item for item in declarations if item.name == "GamePanel"), None)
    entry = next((item for item in declarations if item.name == "TheMechanist"), None)

    lines: list[str] = []
    lines.append("# Java Monolith Extraction Plan")
    lines.append("")
    lines.append("Generated by `ROOT_tools/java_monolith_auditor.py`.")
    lines.append("")
    lines.append("## Non-negotiable extraction rule")
    lines.append("")
    lines.append("This plan is for behavior-preserving extraction only. Do not combine file moves with gameplay, rendering, input, save, package, or UI behavior changes.")
    lines.append("")
    lines.append("## Source")
    lines.append("")
    lines.append(f"`{source.relative_to(REPO_ROOT).as_posix() if source.is_relative_to(REPO_ROOT) else source}`")
    lines.append("")
    lines.append("## Summary")
    lines.append("")
    total_lines = max((item.end_line for item in declarations), default=0)
    lines.append(f"- Top-level declarations found: `{len(declarations)}`")
    lines.append(f"- First-phase same-package extraction candidates: `{len(phase_1)}`")
    lines.append(f"- Large method candidates for later inspection: `{len(methods)}`")
    if entry:
        lines.append(f"- Entry anchor: `{entry.name}` lines `{entry.start_line}-{entry.end_line}`")
    if game_panel:
        lines.append(f"- Deferred runtime panel: `{game_panel.name}` lines `{game_panel.start_line}-{game_panel.end_line}` ({game_panel.line_count} lines)")
    lines.append(f"- Approximate scanned line extent: `{total_lines}`")
    lines.append("")

    lines.append("## Phase 0 — Preserve anchors")
    lines.append("")
    lines.append("Keep `TheMechanist.java` as the public application entrypoint. Do not split `GamePanel` in the first extraction batch.")
    lines.append("")

    lines.append("## Phase 1 — Extract package-private top-level siblings")
    lines.append("")
    if phase_1_sorted:
        lines.append("Move these declarations into same-package files under `src/mechanist/`, preserving package name and broad imports first. Compile after each small batch.")
        lines.append("")
        rows = [["Order", "Declaration", "Kind", "Lines", "Target File", "Notes"]]
        for order, item in enumerate(phase_1_sorted, start=1):
            rows.append([str(order), item.name, item.kind, str(item.line_count), f"src/mechanist/{item.name}.java", item.notes])
        lines.append(markdown_table(rows))
    else:
        lines.append("No obvious package-private top-level sibling declarations were found. The next pass must inspect `GamePanel` seams instead of assuming there are easy top-level extractions.")
    lines.append("")

    lines.append("## Phase 1 compile loop")
    lines.append("")
    lines.append("```powershell")
    lines.append("mvn -q -DskipTests package")
    lines.append("java -jar .\\target\\the-mechanist-0.9.10iz.jar")
    lines.append("```")
    lines.append("")

    lines.append("## Phase 2 — Defer GamePanel internal splitting")
    lines.append("")
    lines.append("After top-level sibling extraction is compile-clean, split `GamePanel` by delegation seams rather than stealing state all at once. Candidate seams include options controls, graphics package selection, render surfaces, input routing, lifecycle, debug surfaces, and screen routing.")
    lines.append("")

    if methods:
        lines.append("## Later large-method inspection candidates")
        lines.append("")
        rows = [["Owner", "Method", "Lines", "Start", "End", "Notes"]]
        for item in sorted(methods, key=lambda method: method.approximate_line_count, reverse=True)[:80]:
            rows.append([item.owner, item.name, str(item.approximate_line_count), str(item.start_line), str(item.approximate_end_line), item.notes])
        lines.append(markdown_table(rows))
        lines.append("")

    lines.append("## Commit discipline")
    lines.append("")
    lines.append("Each extraction batch should be small enough to review. Suggested commit message form:")
    lines.append("")
    lines.append("```text")
    lines.append("Refactor: extract package-private runtime helper classes")
    lines.append("```")
    lines.append("")
    lines.append("Do not add new behavior in extraction commits.")
    lines.append("")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text("\n".join(lines), encoding="utf-8")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Audit a large Java source file and generate a behavior-preserving extraction plan.")
    parser.add_argument("--source", default=str(DEFAULT_SOURCE), help="Java source file to audit.")
    parser.add_argument("--output-tsv", default=str(DEFAULT_TSV), help="TSV audit output path.")
    parser.add_argument("--output-json", default=str(DEFAULT_JSON), help="JSON audit output path.")
    parser.add_argument("--output-plan", default=str(DEFAULT_PLAN), help="Markdown extraction plan output path.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    source = Path(args.source).resolve()
    text = read_text(source)
    declarations = find_top_level_declarations(text)
    methods: list[JavaMethodCandidate] = []
    for declaration in declarations:
        methods.extend(find_methods_for_declaration(text, declaration))

    output_tsv = Path(args.output_tsv).resolve()
    output_json = Path(args.output_json).resolve()
    output_plan = Path(args.output_plan).resolve()
    write_tsv(declarations, methods, output_tsv)
    write_json(source, declarations, methods, output_json)
    write_plan(source, declarations, methods, output_plan)

    phase_1_count = sum(1 for item in declarations if item.extraction_phase == "phase_1_top_level_siblings")
    print(f"Audited Java source: {source}")
    print(f"Top-level declarations: {len(declarations)}")
    print(f"Phase 1 extraction candidates: {phase_1_count}")
    print(f"Large method candidates: {len(methods)}")
    print(f"Wrote TSV:  {output_tsv}")
    print(f"Wrote JSON: {output_json}")
    print(f"Wrote plan: {output_plan}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
