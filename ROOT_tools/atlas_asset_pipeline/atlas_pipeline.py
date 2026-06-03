#!/usr/bin/env python3
"""
Single entry point for atlas slicing and image-edit actions.

This wrapper keeps the atlas pipeline callable through one named tool while
leaving each subtool small and independently testable.
"""

from __future__ import annotations

import runpy
import sys
from pathlib import Path


PIPELINE_ROOT = Path(__file__).resolve().parent

TOOLS = {
    "slice": PIPELINE_ROOT / "dynamic_atlas_slicer.py",
    "compile": PIPELINE_ROOT / "adaptive_asset_compiler.py",
    "censor-boxes": PIPELINE_ROOT / "image_tools/censor_texture_atlases.py",
    "cell-targets": PIPELINE_ROOT / "cell_tool_targets.py",
    "context-stamp": PIPELINE_ROOT / "image_tools/context_stamp_infill_boxes.py",
    "texture-infill": PIPELINE_ROOT / "image_tools/infill_scrubbed_texture_boxes.py",
    "procedural-infill": PIPELINE_ROOT / "image_tools/procedural_texture_infill_boxes.py",
    "template-scrub": PIPELINE_ROOT / "image_tools/scrub_all_asset_template_matches.py",
    "index-content": PIPELINE_ROOT / "semantic_asset_indexer.py",
}


def main() -> int:
    if len(sys.argv) < 2 or sys.argv[1] in {"-h", "--help"}:
        actions = ", ".join(sorted(TOOLS))
        print("usage: atlas_pipeline.py ACTION [ACTION_ARGS...]")
        print()
        print("Run atlas asset pipeline actions.")
        print()
        print(f"actions: {actions}")
        return 0

    action = sys.argv[1]
    if action not in TOOLS:
        actions = ", ".join(sorted(TOOLS))
        print(f"Unknown action: {action}", file=sys.stderr)
        print(f"Available actions: {actions}", file=sys.stderr)
        return 2

    tool_path = TOOLS[action]
    sys.argv = [str(tool_path), *sys.argv[2:]]
    runpy.run_path(str(tool_path), run_name="__main__")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
