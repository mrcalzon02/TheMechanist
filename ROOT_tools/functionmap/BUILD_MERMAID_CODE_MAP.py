#!/usr/bin/env python3
"""Canonical ROOT_tools entry point for the Mermaid code map generator.

The implementation currently lives in the legacy `scripts/` tree for compatibility
with earlier automation. Current documentation and human usage should call this
wrapper instead:

    py -3 ROOT_tools/functionmap/BUILD_MERMAID_CODE_MAP.py --apply

This wrapper deliberately does not duplicate the generator implementation. It
executes the legacy implementation from repository root so output paths and
relative source discovery remain unchanged.
"""

from __future__ import annotations

import runpy
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
LEGACY_SCRIPT = ROOT / "scripts" / "BUILD_MERMAID_CODE_MAP.py"

if not LEGACY_SCRIPT.exists():
    raise SystemExit("Missing legacy Mermaid generator implementation: " + str(LEGACY_SCRIPT))

sys.path.insert(0, str(ROOT))
runpy.run_path(str(LEGACY_SCRIPT), run_name="__main__")
