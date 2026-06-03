#!/usr/bin/env python3
"""
Clear generated atlas preview/audit artifacts.

This removes stale browser preview outputs and manual slice audit outputs so a new
slicer/export pass cannot be confused with old projected geometry.

Default targets, relative to ROOT_tools/atlas_asset_pipeline:
  - atlas_slice_preview
  - diagnostics/manual_slice_audits

The script intentionally refuses to delete arbitrary paths outside the pipeline
root unless --allow-outside-pipeline is supplied.
"""

from __future__ import annotations

import argparse
import shutil
from pathlib import Path


PIPELINE_ROOT = Path(__file__).resolve().parent
DEFAULT_TARGETS = [
    "atlas_slice_preview",
    "diagnostics/manual_slice_audits",
]


def is_relative_to(child: Path, parent: Path) -> bool:
    try:
        child.resolve().relative_to(parent.resolve())
        return True
    except ValueError:
        return False


def clear_target(target: Path, dry_run: bool) -> tuple[int, int]:
    files = 0
    dirs = 0
    if not target.exists():
        print(f"missing: {target}")
        return files, dirs

    if target.is_file():
        print(f"delete file: {target}")
        if not dry_run:
            target.unlink()
        return 1, 0

    for child in sorted(target.rglob("*"), key=lambda item: len(item.parts), reverse=True):
        if child.is_file() or child.is_symlink():
            print(f"delete file: {child}")
            files += 1
            if not dry_run:
                child.unlink()
        elif child.is_dir():
            print(f"delete dir:  {child}")
            dirs += 1
            if not dry_run:
                shutil.rmtree(child, ignore_errors=True)

    if not dry_run:
        target.mkdir(parents=True, exist_ok=True)

    return files, dirs


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Clear stale atlas preview and manual slice audit artifacts.")
    parser.add_argument("--target", action="append", default=[], help="Additional path to clear. Relative paths are resolved under the pipeline root.")
    parser.add_argument("--dry-run", action="store_true", help="List what would be deleted without deleting it.")
    parser.add_argument("--allow-outside-pipeline", action="store_true", help="Allow explicit targets outside ROOT_tools/atlas_asset_pipeline.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    targets = [*DEFAULT_TARGETS, *args.target]
    total_files = 0
    total_dirs = 0

    print(f"Atlas pipeline root: {PIPELINE_ROOT}")
    print(f"Dry run: {args.dry_run}")

    for target_text in targets:
        target = Path(target_text)
        if not target.is_absolute():
            target = PIPELINE_ROOT / target
        target = target.resolve()

        if not args.allow_outside_pipeline and not is_relative_to(target, PIPELINE_ROOT):
            raise ValueError(f"Refusing to clear path outside pipeline root: {target}")

        files, dirs = clear_target(target, args.dry_run)
        total_files += files
        total_dirs += dirs

    print(f"Cleared preview artifacts: files={total_files}, dirs={total_dirs}, dry_run={args.dry_run}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
