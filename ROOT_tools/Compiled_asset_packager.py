#!/usr/bin/env python3
"""
Stable root-level command for promoting compiled atlas assets into package assets.

This wrapper intentionally gives the asset promotion bridge a durable, memorable
entry point:

    python ROOT_tools/Compiled_asset_packager.py

It delegates to:

    ROOT_tools/atlas_asset_pipeline/promote_compiled_assets_to_packages.py

Use this tool after atlas slicing/indexing work whenever generated graphical
assets must be copied into PACKAGE_client and the runtime manifest refreshed.
"""

from __future__ import annotations

import runpy
from pathlib import Path
import sys


ROOT_TOOLS = Path(__file__).resolve().parent
PROMOTION_SCRIPT = ROOT_TOOLS / "atlas_asset_pipeline" / "promote_compiled_assets_to_packages.py"


def main() -> int:
    if not PROMOTION_SCRIPT.exists():
        print(f"Missing promotion script: {PROMOTION_SCRIPT}", file=sys.stderr)
        return 2

    # Preserve CLI arguments exactly. run_path executes the target as __main__,
    # so argparse inside the promotion tool sees the caller's original options.
    sys.argv[0] = str(PROMOTION_SCRIPT)
    runpy.run_path(str(PROMOTION_SCRIPT), run_name="__main__")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
