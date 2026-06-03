#!/usr/bin/env python3
from __future__ import annotations

import json
import subprocess
import sys
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
SRC = REPO_ROOT / "src" / "mechanist"
THE_MECH = SRC / "TheMechanist.java"
GAME_PANEL = SRC / "GamePanel.java"
REPORT = REPO_ROOT / "docs" / "gamepanel_hard_chop_report.json"

@dataclass
class Step:
    name: str
    ok: bool
    detail: str


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8", errors="replace")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="")


def git_show(ref: str, path: str) -> str | None:
    try:
        out = subprocess.check_output(["git", "show", f"{ref}:{path}"], cwd=REPO_ROOT, stderr=subprocess.DEVNULL)
        return out.decode("utf-8", errors="replace")
    except Exception:
        return None


def restore_if_empty(steps: list[Step]) -> None:
    if THE_MECH.exists() and THE_MECH.stat().st_size > 1000:
        steps.append(Step("source_present", True, f"{THE_MECH} size={THE_MECH.stat().st_size}"))
        return
    steps.append(Step("source_present", False, "TheMechanist.java is missing or suspiciously small; searching git history."))
    try:
        hashes = subprocess.check_output(["git", "log", "--format=%H", "--", "src/mechanist/TheMechanist.java"], cwd=REPO_ROOT).decode().splitlines()
    except Exception as ex:
        raise RuntimeError(f"Could not read git history for TheMechanist.java: {ex}")
    for h in hashes:
        text = git_show(h, "src/mechanist/TheMechanist.java")
        if text and "public class TheMechanist" in text and "class GamePanel" in text and len(text) > 10000:
            write(THE_MECH, text)
            steps.append(Step("restore_from_history", True, f"restored TheMechanist.java from {h[:12]}"))
            return
    raise RuntimeError("Could not find a usable TheMechanist.java with GamePanel in recent git history.")


def mask_non_code(text: str) -> str:
    chars = list(text)
    i = 0
    n = len(chars)
    while i < n:
        ch = chars[i]
        nx = chars[i + 1] if i + 1 < n else ""
        if ch == "/" and nx == "/":
            while i < n and chars[i] != "\n":
                chars[i] = " "
                i += 1
            continue
        if ch == "/" and nx == "*":
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
        if ch in ('"', "'"):
            q = ch
            chars[i] = " "
            i += 1
            esc = False
            while i < n:
                c = chars[i]
                if c not in "\r\n":
                    chars[i] = " "
                if esc:
                    esc = False
                elif c == "\\":
                    esc = True
                elif c == q:
                    i += 1
                    break
                i += 1
            continue
        i += 1
    return "".join(chars)


def brace_depths(masked: str) -> list[int]:
    depths = [0] * len(masked)
    d = 0
    for i, ch in enumerate(masked):
        depths[i] = d
        if ch == "{":
            d += 1
        elif ch == "}":
            d = max(0, d - 1)
    return depths


def find_matching(masked: str, open_pos: int) -> int:
    d = 0
    for i in range(open_pos, len(masked)):
        if masked[i] == "{":
            d += 1
        elif masked[i] == "}":
            d -= 1
            if d == 0:
                return i
    raise RuntimeError("No matching brace for GamePanel class.")


def find_gamepanel_span(text: str) -> tuple[int, int]:
    masked = mask_non_code(text)
    depths = brace_depths(masked)
    needle = "class GamePanel"
    pos = 0
    while True:
        pos = masked.find(needle, pos)
        if pos < 0:
            raise RuntimeError("Could not find top-level 'class GamePanel' in TheMechanist.java.")
        if depths[pos] == 0:
            break
        pos += len(needle)
    line_start = text.rfind("\n", 0, pos) + 1
    # include immediately preceding annotations/comments only if they are part of the declaration block is intentionally avoided.
    open_brace = masked.find("{", pos)
    close_brace = find_matching(masked, open_brace)
    end = close_brace + 1
    while end < len(text) and text[end] in " \t\r\n":
        end += 1
    return line_start, end


def package_and_import_prefix(text: str) -> str:
    idx = text.find("public class TheMechanist")
    if idx < 0:
        idx = text.find("class TheMechanist")
    if idx < 0:
        raise RuntimeError("Could not find TheMechanist class declaration.")
    return text[:idx].rstrip() + "\n\n"


def run(cmd: list[str], label: str, steps: list[Step], fail: bool = False) -> int:
    try:
        done = subprocess.run(cmd, cwd=REPO_ROOT, text=True)
        ok = done.returncode == 0
        steps.append(Step(label, ok, " ".join(cmd) + f" exit={done.returncode}"))
        if fail and not ok:
            raise RuntimeError(f"{label} failed with exit {done.returncode}")
        return done.returncode
    except FileNotFoundError:
        steps.append(Step(label, False, f"command not found: {cmd[0]}"))
        if fail:
            raise
        return 127


def main() -> int:
    steps: list[Step] = []
    print("=== The Mechanist GamePanel hard chop ===")
    restore_if_empty(steps)
    text = read(THE_MECH)
    if GAME_PANEL.exists() and GAME_PANEL.stat().st_size > 1000:
        steps.append(Step("already_extracted", True, f"{rel(GAME_PANEL)} already exists"))
    else:
        start, end = find_gamepanel_span(text)
        prefix = package_and_import_prefix(text)
        gamepanel_text = text[start:end].strip() + "\n"
        remaining = text[:start].rstrip() + "\n\n" + text[end:].lstrip()
        write(GAME_PANEL, prefix + gamepanel_text)
        write(THE_MECH, remaining)
        steps.append(Step("extract_gamepanel", True, f"moved bytes {start}-{end} into {rel(GAME_PANEL)}"))
    after_mech = read(THE_MECH)
    after_panel = read(GAME_PANEL)
    steps.append(Step("verify_themechanist_anchor", "public class TheMechanist" in after_mech, "TheMechanist.java keeps public entry class"))
    steps.append(Step("verify_gamepanel_file", "class GamePanel" in after_panel, "GamePanel.java contains GamePanel"))
    steps.append(Step("verify_removed_duplicate", "class GamePanel" not in after_mech, "TheMechanist.java no longer contains GamePanel"))
    auditor = ROOT_TOOLS / "java_monolith_auditor.py"
    if auditor.exists():
        run([sys.executable, str(auditor)], "refresh_java_monolith_audit", steps)
    scanner = ROOT_TOOLS / "repository_scan_indexer.py"
    if scanner.exists():
        run([sys.executable, str(scanner)], "refresh_repository_manifest", steps)
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps({
        "schema": "mechanist.gamepanel_hard_chop.v1",
        "time": datetime.now(timezone.utc).isoformat(),
        "steps": [asdict(s) for s in steps],
    }, indent=2), encoding="utf-8")
    print(f"Report: {rel(REPORT)}")
    failed = [s for s in steps if not s.ok and s.name.startswith("verify")]
    for s in steps:
        print(("OK   " if s.ok else "FAIL ") + s.name + " — " + s.detail)
    if failed:
        print("Hard chop failed verification.", file=sys.stderr)
        return 2
    print("GamePanel hard chop completed successfully.")
    return 0


def rel(path: Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


if __name__ == "__main__":
    raise SystemExit(main())
