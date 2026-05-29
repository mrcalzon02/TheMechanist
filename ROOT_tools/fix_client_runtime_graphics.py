#!/usr/bin/env python3
from __future__ import annotations

import json, shutil, subprocess, sys
from datetime import datetime, timezone
from pathlib import Path

ROOT_TOOLS = Path(__file__).resolve().parent
REPO_ROOT = ROOT_TOOLS.parent
SRC_ROOT = REPO_ROOT / "ROOT_SRC_assets"
PREFERRED = SRC_ROOT / "Mechanist_art_SRC_do_not_MODIFY" / "Mechanist art"
CLIENT_ART = REPO_ROOT / "PACKAGE_client/assets/a/r"
CLIENT_DEFAULT_32 = REPO_ROOT / "PACKAGE_client/assets/graphics/packages/default_32"
COMPILED_32 = REPO_ROOT / "ROOT_tools/atlas_asset_pipeline/compiled_assets/32px"
REPORT = REPO_ROOT / "docs/client_runtime_graphics_repair.json"
EXTS = {".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp", ".json", ".tsv", ".csv", ".txt"}

REQUIRED_FILES = [
    CLIENT_ART / "source/Title/TITEL.png",
    CLIENT_ART / "source/Title/Sub title.png",
    CLIENT_ART / "source/Background/Backdrop.png",
    CLIENT_ART / "source/Background/CLOUDS1slow.png",
    CLIENT_ART / "source/Background/Clouds2fast.png",
]
REQUIRED_CELL_ROOT = CLIENT_ART / "tiles/quality/low_32/cells"


def rel(path: Path) -> str:
    try:
        return path.resolve().relative_to(REPO_ROOT.resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def payload(path: Path) -> bool:
    return path.is_file() and path.suffix.lower() in EXTS and path.name not in {"README.md", "README.txt", ".gitkeep", ".keep"}


def shaped(path: Path) -> bool:
    if not path.is_dir():
        return False
    names = {p.name.lower() for p in path.iterdir() if p.is_dir()}
    return "background" in names and "title" in names and "tiles" in names


def find_art_root() -> Path:
    if PREFERRED.is_dir():
        return PREFERRED
    candidates = [p for p in SRC_ROOT.rglob("*") if p.is_dir() and shaped(p)] if SRC_ROOT.is_dir() else []
    if not candidates:
        raise SystemExit(f"Could not find source art root under {SRC_ROOT}")
    candidates.sort(key=lambda p: (len(p.parts), str(p)))
    return candidates[0]


def copy_tree(src: Path, dst: Path, note: str, rows: list[dict]) -> int:
    count = 0
    if not src.is_dir():
        return 0
    for file in sorted(src.rglob("*")):
        if not payload(file):
            continue
        target = dst / file.relative_to(src)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(file, target)
        rows.append({"source": rel(file), "destination": rel(target), "note": note, "bytes": file.stat().st_size})
        count += 1
    return count


def main() -> int:
    print("=== The Mechanist graphics repair ===")
    art_root = find_art_root()
    print("Source art root:", rel(art_root))
    rows: list[dict] = []
    copied = 0
    copied += copy_tree(art_root / "Background", CLIENT_ART / "source/Background", "background", rows)
    copied += copy_tree(art_root / "Title", CLIENT_ART / "source/Title", "title", rows)
    copied += copy_tree(art_root / "Faction", CLIENT_ART / "source/Faction", "faction", rows)
    copied += copy_tree(art_root / "TILES", REQUIRED_CELL_ROOT, "tiles_low_32_cells", rows)
    if COMPILED_32.is_dir():
        copied += copy_tree(COMPILED_32, CLIENT_DEFAULT_32, "compiled_default_32", rows)
        (CLIENT_DEFAULT_32 / "package_info.json").write_text(json.dumps({"id":"default_32","ready_to_use":True,"self_contained":True}, indent=2), encoding="utf-8")
    checks = []
    for path in REQUIRED_FILES:
        checks.append({"path": rel(path), "ok": path.is_file() and path.stat().st_size > 0})
    tile_pngs = len(list(REQUIRED_CELL_ROOT.rglob("*.png"))) if REQUIRED_CELL_ROOT.is_dir() else 0
    checks.append({"path": rel(REQUIRED_CELL_ROOT), "ok": tile_pngs > 0, "png_count": tile_pngs})
    package_pngs = len(list(CLIENT_DEFAULT_32.rglob("*.png"))) if CLIENT_DEFAULT_32.is_dir() else 0
    checks.append({"path": rel(CLIENT_DEFAULT_32), "ok": package_pngs > 0, "png_count": package_pngs})
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps({"schema":"client_runtime_graphics_repair.v1","time":datetime.now(timezone.utc).isoformat(),"source_art_root":rel(art_root),"copied":copied,"checks":checks,"rows":rows}, indent=2), encoding="utf-8")
    print("Copied files:", copied)
    failed = [c for c in checks if not c.get("ok")]
    for c in checks:
        print(("OK   " if c.get("ok") else "FAIL ") + c["path"])
    if failed:
        print("Graphics repair failed.", file=sys.stderr)
        return 2
    scanner = ROOT_TOOLS / "repository_scan_indexer.py"
    if scanner.exists():
        code = subprocess.run([sys.executable, str(scanner)], cwd=REPO_ROOT).returncode
        if code != 0:
            return code
    print("Graphics repair completed successfully.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
