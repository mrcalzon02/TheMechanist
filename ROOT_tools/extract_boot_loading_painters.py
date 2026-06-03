#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import shutil
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GAMEPANEL = ROOT / "src" / "mechanist" / "GamePanel.java"
SRC_DIR = ROOT / "src" / "mechanist"

CONTROL_WORDS = {
    "if", "for", "while", "switch", "catch", "try", "do", "else", "synchronized", "new",
    "return", "throw", "case", "default"
}

METHOD_RE = re.compile(
    r"(?P<prefix>\b(?:public|protected|private|static|final|synchronized|abstract|native|strictfp|default|sealed|non-sealed)\b[\w\s<>,\[\].?@&:-]*|[A-Za-z_$][\w\s<>,\[\].?@&:-]*)"
    r"\s+(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)\s*\([^;{}]*\)\s*(?:throws\s+[\w\s,.<>]+)?\{"
)

FIELD_NAME_RE = re.compile(r"([A-Za-z_$][A-Za-z0-9_$]*)\s*(?:=|,|;)")
TOKEN_RE = re.compile(r"\b[A-Za-z_$][A-Za-z0-9_$]*\b")

INHERITED_PANEL_METHODS = {
    "getWidth", "getHeight", "getSize", "getInsets", "getFontMetrics",
    "getBackground", "getForeground", "repaint", "requestFocusInWindow"
}

PAINTER_TEMPLATE = """package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Stateless extracted rendering surface for {surface_label}.
 *
 * Source: GamePanel.{method_name}(Graphics2D g)
 */
final class {class_name} implements ScreenPainter {{
    @Override
    public void paint(Graphics2D g, GamePanel panel) {{
{body}
    }}
}}
"""


@dataclass
class MethodSlice:
    name: str
    signature_start: int
    open_brace: int
    close_brace: int
    start_line: int
    end_line: int
    signature: str
    full_text: str
    body_text: str


def mask_java(text: str) -> str:
    """Replace comments and literals with spaces while preserving offsets/newlines."""
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
    lo, hi = 0, len(starts) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if starts[mid] <= pos:
            lo = mid + 1
        else:
            hi = mid - 1
    return hi + 1


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


def find_method(text: str, masked: str, starts: list[int], depths: list[int], method_name: str) -> MethodSlice:
    matches = []
    for hit in METHOD_RE.finditer(masked):
        if hit.group("name") != method_name:
            continue
        if hit.group("name") in CONTROL_WORDS:
            continue
        # Direct GamePanel methods sit at brace depth 1 before their opening brace.
        if depths[hit.start()] != 1:
            continue
        open_brace = masked.find("{", hit.start())
        if open_brace < 0:
            continue
        close = close_brace(masked, open_brace)
        matches.append((hit, open_brace, close))

    if not matches:
        raise SystemExit(f"Could not find direct GamePanel method: {method_name}")
    if len(matches) > 1:
        raise SystemExit(f"Ambiguous GamePanel method {method_name}: found {len(matches)} direct matches")

    hit, open_brace, close = matches[0]
    sig = re.sub(r"\s+", " ", text[hit.start():open_brace].strip())
    return MethodSlice(
        name=method_name,
        signature_start=hit.start(),
        open_brace=open_brace,
        close_brace=close,
        start_line=line_no(starts, hit.start()),
        end_line=line_no(starts, close),
        signature=sig,
        full_text=text[hit.start():close + 1],
        body_text=text[open_brace + 1:close],
    )


def extract_gamepanel_member_names(text: str, masked: str, starts: list[int], depths: list[int]) -> tuple[set[str], set[str]]:
    method_names: set[str] = set(INHERITED_PANEL_METHODS)
    field_names: set[str] = set()

    for hit in METHOD_RE.finditer(masked):
        if depths[hit.start()] == 1:
            name = hit.group("name")
            if name not in CONTROL_WORDS:
                method_names.add(name)

    # Field/member statements at direct GamePanel class depth. This intentionally captures broad
    # member names for rendering extraction, not a perfect Java AST.
    statement_start = None
    for i, ch in enumerate(masked):
        if depths[i] != 1:
            continue
        if statement_start is None and not ch.isspace():
            statement_start = i
        if ch == ";" and statement_start is not None:
            raw_masked = masked[statement_start:i + 1]
            raw_text = text[statement_start:i + 1]
            statement_start = None
            if "(" in raw_masked:
                continue
            if raw_text.strip().startswith(("package ", "import ")):
                continue
            # Split declarations with comma members, preserve likely variable tokens.
            before_init = re.sub(r"=[^,;]*", "", raw_text)
            tokens = re.findall(r"\b[A-Za-z_$][A-Za-z0-9_$]*\b", before_init)
            skip = {
                "public", "protected", "private", "static", "final", "volatile", "transient",
                "int", "long", "boolean", "double", "float", "short", "byte", "char", "void",
                "new", "return", "class", "interface", "enum", "extends", "implements"
            }
            candidates = [t for t in tokens if t not in skip and t[:1].islower()]
            if candidates:
                # Last lowercase identifier in each comma segment is usually the field name.
                segments = before_init.split(",")
                for seg in segments:
                    seg_tokens = re.findall(r"\b[A-Za-z_$][A-Za-z0-9_$]*\b", seg)
                    seg_candidates = [t for t in seg_tokens if t not in skip and t[:1].islower()]
                    if seg_candidates:
                        field_names.add(seg_candidates[-1])
    return field_names, method_names


def body_local_names(body: str) -> set[str]:
    # Conservative local-name protection. Rendering methods usually use simple local declarations;
    # this prevents accidental panel-prefixing of w/h/elapsed/bg/etc. when names happen to collide.
    locals_found: set[str] = {"g", "panel"}
    declaration_re = re.compile(
        r"\b(?:final\s+)?(?:int|long|boolean|double|float|short|byte|char|String|Color|Font|Rectangle|BufferedImage|Image|Object|var|[A-Z][A-Za-z0-9_$<>\[\].?]*)\s+([a-z_$][A-Za-z0-9_$]*)\b"
    )
    for match in declaration_re.finditer(mask_java(body)):
        locals_found.add(match.group(1))
    for match in re.finditer(r"catch\s*\([^)]*\s+([a-z_$][A-Za-z0-9_$]*)\s*\)", mask_java(body)):
        locals_found.add(match.group(1))
    for match in re.finditer(r"for\s*\([^:;]+\s+([a-z_$][A-Za-z0-9_$]*)\s*:", mask_java(body)):
        locals_found.add(match.group(1))
    return locals_found


def prefix_gamepanel_members(body: str, field_names: set[str], method_names: set[str]) -> str:
    masked = mask_java(body)
    locals_found = body_local_names(body)
    member_names = (field_names | method_names) - locals_found
    if not member_names:
        return body

    replacements: list[tuple[int, int, str]] = []
    for match in TOKEN_RE.finditer(masked):
        token = match.group(0)
        if token not in member_names:
            continue
        start, end = match.span()

        # Already qualified or part of a qualified/member/static access.
        prev = start - 1
        while prev >= 0 and masked[prev].isspace() and masked[prev] not in "\r\n":
            prev -= 1
        if prev >= 0 and masked[prev] in ".":
            continue
        before = body[max(0, start - 6):start]
        if before.endswith("panel."):
            continue

        # Do not prefix labels or named arguments in rare syntactic positions.
        nxt = end
        while nxt < len(masked) and masked[nxt].isspace() and masked[nxt] not in "\r\n":
            nxt += 1
        if nxt < len(masked) and masked[nxt] == ":":
            continue

        replacements.append((start, end, "panel." + token))

    if not replacements:
        return body

    out = []
    last = 0
    for start, end, replacement in replacements:
        out.append(body[last:start])
        out.append(replacement)
        last = end
    out.append(body[last:])
    return "".join(out)


def normalize_body_indent(body: str) -> str:
    lines = body.splitlines()
    while lines and not lines[0].strip():
        lines.pop(0)
    while lines and not lines[-1].strip():
        lines.pop()
    if not lines:
        return ""

    indents = [len(line) - len(line.lstrip(" ")) for line in lines if line.strip()]
    trim = min(indents) if indents else 0
    normalized = []
    for line in lines:
        stripped = line[trim:] if len(line) >= trim else line.lstrip()
        normalized.append("        " + stripped)
    return "\n".join(normalized) + "\n"


def painter_source(class_name: str, surface_label: str, source_method_name: str, transformed_body: str) -> str:
    return PAINTER_TEMPLATE.format(
        class_name=class_name,
        surface_label=surface_label,
        method_name=source_method_name,
        body=normalize_body_indent(transformed_body).rstrip("\n"),
    )


def replace_method_with_delegate(text: str, method: MethodSlice, delegate_field: str) -> str:
    replacement = f"    void {method.name}(Graphics2D g) {{\n        {delegate_field}.paint(g, this);\n    }}"
    return text[:method.signature_start] + replacement + text[method.close_brace + 1:]


def insert_painter_fields(text: str) -> str:
    wanted = [
        "    private final ScreenPainter bootSurfacePainter = new BootSurfacePainter();",
        "    private final ScreenPainter loadingSurfacePainter = new LoadingSurfacePainter();",
    ]
    missing = [line for line in wanted if line not in text]
    if not missing:
        return text

    # Prefer to attach near existing ScreenPainter fields.
    marker_re = re.compile(r"(^\s*private\s+final\s+ScreenPainter\s+[^;]+;\s*$)", re.MULTILINE)
    matches = list(marker_re.finditer(text))
    if matches:
        insert_at = matches[-1].end()
        return text[:insert_at] + "\n" + "\n".join(missing) + text[insert_at:]

    # Fallback: insert after enum declarations near top of GamePanel.
    enum_marker = "enum PanelMode"
    pos = text.find(enum_marker)
    if pos >= 0:
        line_end = text.find("\n", pos)
        if line_end >= 0:
            return text[:line_end + 1] + "\n".join(missing) + "\n" + text[line_end + 1:]
    raise SystemExit("Could not locate a safe location for ScreenPainter fields")


def write_new_file(path: Path, content: str, force: bool) -> None:
    if path.exists() and not force:
        raise SystemExit(f"Refusing to overwrite existing file without --force: {path}")
    path.write_text(content, encoding="utf-8", newline="\n")
    print(f"Wrote {path.relative_to(ROOT)}")


def run(force: bool, backup: bool, dry_run: bool) -> None:
    if not GAMEPANEL.exists():
        raise SystemExit(f"Missing {GAMEPANEL}")

    text = GAMEPANEL.read_text(encoding="utf-8", errors="replace")
    masked = mask_java(text)
    starts = line_starts(text)
    depths = brace_depths(masked)

    boot = find_method(text, masked, starts, depths, "drawBoot")
    loading = find_method(text, masked, starts, depths, "drawLoading")
    field_names, method_names = extract_gamepanel_member_names(text, masked, starts, depths)

    print(f"Found drawBoot lines {boot.start_line}-{boot.end_line}: {boot.signature}")
    print(f"Found drawLoading lines {loading.start_line}-{loading.end_line}: {loading.signature}")
    print(f"Detected {len(field_names)} GamePanel field/member names and {len(method_names)} method names for panel-prefixing")

    boot_body = prefix_gamepanel_members(boot.body_text, field_names, method_names)
    loading_body = prefix_gamepanel_members(loading.body_text, field_names, method_names)

    boot_source = painter_source("BootSurfacePainter", "boot screen", "drawBoot", boot_body)
    loading_source = painter_source("LoadingSurfacePainter", "loading screen", "drawLoading", loading_body)

    updated = text
    # Replace later method first so earlier offsets remain valid for the first replacement.
    for method, delegate in sorted([(boot, "bootSurfacePainter"), (loading, "loadingSurfacePainter")], key=lambda x: x[0].signature_start, reverse=True):
        updated = replace_method_with_delegate(updated, method, delegate)
    updated = insert_painter_fields(updated)

    if dry_run:
        print("Dry run only. No files changed.")
        print("Would write src/mechanist/BootSurfacePainter.java")
        print("Would write src/mechanist/LoadingSurfacePainter.java")
        print("Would update src/mechanist/GamePanel.java")
        return

    if backup:
        backup_path = GAMEPANEL.with_suffix(".java.bak")
        shutil.copy2(GAMEPANEL, backup_path)
        print(f"Backed up {GAMEPANEL.relative_to(ROOT)} -> {backup_path.relative_to(ROOT)}")

    write_new_file(SRC_DIR / "BootSurfacePainter.java", boot_source, force)
    write_new_file(SRC_DIR / "LoadingSurfacePainter.java", loading_source, force)
    GAMEPANEL.write_text(updated, encoding="utf-8", newline="\n")
    print(f"Updated {GAMEPANEL.relative_to(ROOT)} with ScreenPainter fields and delegates")
    print("Next: compile the project and inspect the two generated painter files before committing.")


def main() -> int:
    parser = argparse.ArgumentParser(description="Extract GamePanel boot/loading render methods into ScreenPainter classes.")
    parser.add_argument("--force", action="store_true", help="Overwrite existing painter files if present")
    parser.add_argument("--no-backup", action="store_true", help="Do not write src/mechanist/GamePanel.java.bak")
    parser.add_argument("--dry-run", action="store_true", help="Parse and report without writing files")
    args = parser.parse_args()
    run(force=args.force, backup=not args.no_backup, dry_run=args.dry_run)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
