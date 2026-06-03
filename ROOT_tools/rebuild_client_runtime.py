#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
from dataclasses import asdict, dataclass
from datetime import datetime, timezone
from pathlib import Path

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
SRC_ROOT = REPO_ROOT / "src"
CLIENT_ROOT = REPO_ROOT / "PACKAGE_client"
CLIENT_JAR = CLIENT_ROOT / "TheMechanist.jar"
BUILD_ROOT = REPO_ROOT / "ROOT_BUILD" / "client_runtime"
CLASSES_DIR = BUILD_ROOT / "classes"
REPORT = REPO_ROOT / "docs" / "client_runtime_rebuild_report.json"
MAIN_CLASS = "mechanist.TheMechanist"

@dataclass
class Step:
    name: str
    ok: bool
    detail: str


def run(cmd: list[str], steps: list[Step], name: str, required: bool = True) -> int:
    print("\n=== " + name + " ===")
    print(" ".join(str(c) for c in cmd))
    try:
        completed = subprocess.run(cmd, cwd=REPO_ROOT, text=True)
    except FileNotFoundError:
        steps.append(Step(name, False, f"command not found: {cmd[0]}"))
        if required:
            raise SystemExit(f"Required command not found: {cmd[0]}")
        return 127
    ok = completed.returncode == 0
    steps.append(Step(name, ok, " ".join(str(c) for c in cmd) + f" exit={completed.returncode}"))
    if required and not ok:
        raise SystemExit(completed.returncode)
    return completed.returncode


def tool(path_name: str) -> Path:
    return ROOT_TOOLS / path_name


def collect_java_sources() -> list[Path]:
    files = sorted(SRC_ROOT.rglob("*.java"))
    if not files:
        raise SystemExit(f"No Java sources found under {SRC_ROOT}")
    return files


def verify_assets(steps: list[Step]) -> None:
    required_files = [
        CLIENT_ROOT / "assets/a/r/source/Title/TITEL.png",
        CLIENT_ROOT / "assets/a/r/source/Title/Sub title.png",
        CLIENT_ROOT / "assets/a/r/source/Background/Backdrop.png",
        CLIENT_ROOT / "assets/a/r/source/Background/CLOUDS1slow.png",
        CLIENT_ROOT / "assets/a/r/source/Background/Clouds2fast.png",
    ]
    required_dirs = [
        CLIENT_ROOT / "assets/a/r/tiles/quality/low_32/cells",
        CLIENT_ROOT / "assets/graphics/packages/default_32",
    ]
    for path in required_files:
        ok = path.is_file() and path.stat().st_size > 0
        steps.append(Step("verify_runtime_asset", ok, str(path)))
        if not ok:
            raise SystemExit(f"Missing required runtime display asset: {path}")
    for path in required_dirs:
        count = len(list(path.rglob("*.png"))) if path.is_dir() else 0
        ok = count > 0
        steps.append(Step("verify_runtime_asset_dir", ok, f"{path} png_count={count}"))
        if not ok:
            raise SystemExit(f"Missing required runtime display asset directory payloads: {path}")


def compile_sources(steps: list[Step]) -> None:
    if CLASSES_DIR.exists():
        shutil.rmtree(CLASSES_DIR)
    CLASSES_DIR.mkdir(parents=True, exist_ok=True)
    sources = collect_java_sources()
    argfile = BUILD_ROOT / "sources.txt"
    argfile.parent.mkdir(parents=True, exist_ok=True)
    argfile.write_text("\n".join(str(p) for p in sources) + "\n", encoding="utf-8")
    run(["javac", "-encoding", "UTF-8", "-d", str(CLASSES_DIR), "@" + str(argfile)], steps, "compile_java_sources")


def build_jar(steps: list[Step]) -> None:
    CLIENT_ROOT.mkdir(parents=True, exist_ok=True)
    if CLIENT_JAR.exists():
        backup = CLIENT_JAR.with_suffix(".jar.bak")
        shutil.copy2(CLIENT_JAR, backup)
        steps.append(Step("backup_existing_client_jar", True, str(backup)))
    run(["jar", "cfe", str(CLIENT_JAR), MAIN_CLASS, "-C", str(CLASSES_DIR), "."], steps, "write_client_jar")
    ok = CLIENT_JAR.is_file() and CLIENT_JAR.stat().st_size > 0
    steps.append(Step("verify_client_jar", ok, f"{CLIENT_JAR} size={CLIENT_JAR.stat().st_size if CLIENT_JAR.exists() else 0}"))
    if not ok:
        raise SystemExit("Client jar was not written correctly.")


def write_report(steps: list[Step]) -> None:
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps({
        "schema": "mechanist.client_runtime_rebuild.v1",
        "time": datetime.now(timezone.utc).isoformat(),
        "client_jar": str(CLIENT_JAR),
        "main_class": MAIN_CLASS,
        "steps": [asdict(s) for s in steps],
    }, indent=2), encoding="utf-8")


def main() -> int:
    steps: list[Step] = []
    print("=== The Mechanist one-command local repair + client rebuild ===")
    print("This updates source, copies real display assets into PACKAGE_client, compiles Java, and rewrites PACKAGE_client/TheMechanist.jar.")

    for name, label, required in [
        ("fix_client_runtime_graphics.py", "copy_and_verify_runtime_graphics", True),
        ("apply_ui_runtime_fixes.py", "apply_ui_text_and_intro_fixes", True),
    ]:
        p = tool(name)
        if p.exists():
            run([sys.executable, str(p)], steps, label, required=required)
        elif required:
            raise SystemExit(f"Missing required tool: {p}")
        else:
            steps.append(Step(label, False, f"tool missing: {p}"))

    verify_assets(steps)
    compile_sources(steps)
    build_jar(steps)

    for name, label in [
        ("gamepanel_method_indexer.py", "refresh_gamepanel_method_index"),
        ("gamepanel_extract_methods.py", "refresh_gamepanel_extracts"),
        ("repository_scan_indexer.py", "refresh_repository_manifest"),
    ]:
        p = tool(name)
        if p.exists():
            run([sys.executable, str(p)], steps, label, required=False)

    write_report(steps)
    failed = [s for s in steps if not s.ok and s.name not in {"refresh_repository_manifest", "refresh_gamepanel_extracts", "refresh_gamepanel_method_index"}]
    print("\nReport:", REPORT)
    if failed:
        print("Client runtime rebuild failed.", file=sys.stderr)
        return 2
    print("Client runtime rebuild completed. Run PACKAGE_client/TheMechanist.jar to test the rebuilt jar.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
