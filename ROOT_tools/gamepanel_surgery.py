#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC_DIR = ROOT / "src" / "mechanist"
GAMEPANEL = SRC_DIR / "GamePanel.java"
BOOT_PAINTER = SRC_DIR / "BootSurfacePainter.java"
LOADING_PAINTER = SRC_DIR / "LoadingSurfacePainter.java"
CATALOG = ROOT / "ROOT_tools" / "gamepanel_catalog_slicer.py"
BOOT_LOADING_EXTRACTOR = ROOT / "ROOT_tools" / "extract_boot_loading_painters.py"

METHOD_RE = re.compile(
    r"(?P<prefix>\b(?:public|protected|private|static|final|synchronized|abstract|native|strictfp|default|sealed|non-sealed)\b[\w\s<>,\[\].?@&:-]*|[A-Za-z_$][\w\s<>,\[\].?@&:-]*)"
    r"\s+(?P<name>[A-Za-z_$][A-Za-z0-9_$]*)\s*\([^;{}]*\)\s*(?:throws\s+[\w\s,.<>]+)?\{"
)
CONTROL_WORDS = {"if", "for", "while", "switch", "catch", "try", "do", "else", "synchronized", "new", "return", "throw", "case", "default"}


@dataclass
class MethodSlice:
    name: str
    signature_start: int
    open_brace: int
    close_brace: int
    signature: str
    body: str


def run_cmd(cmd: list[str], *, check: bool = True) -> subprocess.CompletedProcess[str]:
    print("$ " + " ".join(cmd))
    return subprocess.run(cmd, cwd=ROOT, text=True, check=check)


def mask_java(text: str) -> str:
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


def find_direct_method(text: str, method_name: str) -> MethodSlice | None:
    masked = mask_java(text)
    depths = brace_depths(masked)
    hits: list[MethodSlice] = []
    for hit in METHOD_RE.finditer(masked):
        if hit.group("name") != method_name:
            continue
        if method_name in CONTROL_WORDS:
            continue
        if depths[hit.start()] != 1:
            continue
        open_brace = masked.find("{", hit.start())
        if open_brace < 0:
            continue
        close = close_brace(masked, open_brace)
        sig = re.sub(r"\s+", " ", text[hit.start():open_brace].strip())
        hits.append(MethodSlice(method_name, hit.start(), open_brace, close, sig, text[open_brace + 1:close]))
    if not hits:
        return None
    if len(hits) > 1:
        raise SystemExit(f"Ambiguous direct method {method_name}: {len(hits)} matches")
    return hits[0]


def remove_direct_method(text: str, method: MethodSlice) -> str:
    start = method.signature_start
    end = method.close_brace + 1
    # Eat neighboring blank lines conservatively.
    while start > 0 and text[start - 1] in " \t":
        start -= 1
    while start > 0 and text[start - 1] in "\r\n":
        start -= 1
        if start > 0 and text[start - 1] in "\r\n":
            break
    while end < len(text) and text[end] in " \t\r\n":
        if text[end] in "\r\n" and end + 1 < len(text) and text[end + 1] in "\r\n":
            end += 1
            break
        end += 1
    return text[:start] + "\n" + text[end:]


def normalize_helper_body(body: str) -> str:
    lines = body.splitlines()
    while lines and not lines[0].strip():
        lines.pop(0)
    while lines and not lines[-1].strip():
        lines.pop()
    if not lines:
        return ""
    indents = [len(line) - len(line.lstrip(" ")) for line in lines if line.strip()]
    trim = min(indents) if indents else 0
    return "\n".join("        " + (line[trim:] if len(line) >= trim else line.lstrip()) for line in lines) + "\n"


def boot_loading_done() -> bool:
    if not GAMEPANEL.exists():
        raise SystemExit(f"Missing {GAMEPANEL}")
    text = GAMEPANEL.read_text(encoding="utf-8", errors="replace")
    return (
        BOOT_PAINTER.exists()
        and LOADING_PAINTER.exists()
        and find_direct_method(text, "drawBoot") is None
        and find_direct_method(text, "drawLoading") is None
        and "bootSurfacePainter.paint(g, this)" in text
        and "loadingSurfacePainter.paint(g, this)" in text
    )


def stage_boot_loading(dry_run: bool = False) -> bool:
    if boot_loading_done():
        print("[done] boot/loading painter extraction already complete")
        return False
    if not BOOT_LOADING_EXTRACTOR.exists():
        raise SystemExit(f"Missing {BOOT_LOADING_EXTRACTOR}")
    cmd = [sys.executable, str(BOOT_LOADING_EXTRACTOR)]
    if dry_run:
        cmd.append("--dry-run")
    else:
        cmd.extend(["--force", "--no-backup"])
    run_cmd(cmd)
    return not dry_run


def stage_boot_gear(dry_run: bool = False) -> bool:
    if not BOOT_PAINTER.exists():
        print("[skip] BootSurfacePainter.java does not exist yet")
        return False
    if not GAMEPANEL.exists():
        raise SystemExit(f"Missing {GAMEPANEL}")

    gp = GAMEPANEL.read_text(encoding="utf-8", errors="replace")
    boot = BOOT_PAINTER.read_text(encoding="utf-8", errors="replace")
    helper = find_direct_method(gp, "drawBootGear")

    if helper is None:
        if "drawBootGear(" in boot:
            print("[done] drawBootGear already lives outside GamePanel")
        else:
            print("[done] no drawBootGear helper remains to extract")
        return False

    changed = False
    if "private void drawBootGear(" not in boot:
        private_sig = re.sub(r"^\s*void\s+drawBootGear\s*\(", "private void drawBootGear(", helper.signature)
        helper_text = "\n\n" + "    " + private_sig + " {\n" + normalize_helper_body(helper.body) + "    }\n"
        insert_at = boot.rfind("\n}")
        if insert_at < 0:
            raise SystemExit("Could not locate end of BootSurfacePainter class")
        boot = boot[:insert_at] + helper_text + boot[insert_at:]
        changed = True
        print("[stage] will add drawBootGear helper to BootSurfacePainter")

    if "panel.drawBootGear(" in boot:
        boot = boot.replace("panel.drawBootGear(", "drawBootGear(")
        changed = True
        print("[stage] will retarget BootSurfacePainter calls away from panel.drawBootGear")

    # Remove the GamePanel helper if the only real remaining ownership is the extracted painter.
    gp_without_helper = remove_direct_method(gp, helper)
    changed = True
    print("[stage] will remove drawBootGear from GamePanel")

    if dry_run:
        print("Dry run only. No files changed for boot gear stage.")
        return False

    BOOT_PAINTER.write_text(boot, encoding="utf-8", newline="\n")
    GAMEPANEL.write_text(gp_without_helper, encoding="utf-8", newline="\n")
    print("[done] extracted drawBootGear into BootSurfacePainter and removed GamePanel helper")
    return changed


def run_catalog(slice_methods: list[str] | None = None) -> None:
    if not CATALOG.exists():
        print(f"[skip] catalog slicer missing: {CATALOG}")
        return
    cmd = [sys.executable, str(CATALOG)]
    if slice_methods:
        for name in slice_methods:
            cmd.extend(["--method", name])
    run_cmd(cmd)


def run_compile() -> bool:
    java_files = sorted(str(p) for p in (ROOT / "src").rglob("*.java"))
    if not java_files:
        raise SystemExit("No Java files found under src")
    out = ROOT / "out"
    out.mkdir(exist_ok=True)
    cmd = ["javac", "-d", str(out)] + java_files
    result = subprocess.run(cmd, cwd=ROOT, text=True)
    if result.returncode == 0:
        print("[ok] javac compile passed")
        return True
    print(f"[fail] javac compile failed with exit code {result.returncode}")
    return False


def run_next(dry_run: bool = False) -> bool:
    if not boot_loading_done():
        return stage_boot_loading(dry_run=dry_run)
    if find_direct_method(GAMEPANEL.read_text(encoding="utf-8", errors="replace"), "drawBootGear") is not None:
        return stage_boot_gear(dry_run=dry_run)
    print("[done] no currently automated GamePanel surgery stages remain")
    return False


def run_until_stable(dry_run: bool = False) -> bool:
    changed = False
    while True:
        did = run_next(dry_run=dry_run)
        changed = changed or did
        if dry_run or not did:
            break
    return changed


def main() -> int:
    parser = argparse.ArgumentParser(description="Auto-detect and run safe GamePanel decomposition stages.")
    parser.add_argument("--stage", choices=["next", "all", "catalog", "boot-loading", "boot-gear", "compile"], default="next")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--no-catalog", action="store_true")
    parser.add_argument("--no-compile", action="store_true")
    args = parser.parse_args()

    if args.stage == "catalog":
        run_catalog()
        return 0
    if args.stage == "compile":
        return 0 if run_compile() else 1

    changed = False
    if args.stage == "boot-loading":
        changed = stage_boot_loading(dry_run=args.dry_run)
    elif args.stage == "boot-gear":
        changed = stage_boot_gear(dry_run=args.dry_run)
    elif args.stage == "all":
        changed = run_until_stable(dry_run=args.dry_run)
    else:
        changed = run_next(dry_run=args.dry_run)

    if not args.dry_run and not args.no_catalog:
        run_catalog(["drawBootGear", "drawOptions", "drawIntroCrawl", "drawGame"])
    if not args.dry_run and not args.no_compile:
        if not run_compile():
            return 1
    if changed:
        print("[result] surgery stage changed files")
    else:
        print("[result] no source changes required")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
