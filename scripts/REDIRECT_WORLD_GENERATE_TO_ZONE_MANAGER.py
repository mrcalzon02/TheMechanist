#!/usr/bin/env python3
"""
REDIRECT_WORLD_GENERATE_TO_ZONE_MANAGER.py

After SPLIT_WORLD_GENERATE_PIPELINE.py has been applied, replace the local
World.generate() phase list with a delegation call to ZoneGenerationManager.

Run from repository root:

    py -3 scripts\REDIRECT_WORLD_GENERATE_TO_ZONE_MANAGER.py --apply
"""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Optional, Sequence, Tuple

ROOT = Path.cwd()
TARGET = ROOT / "src" / "mechanist" / "WorldRuntimeGenerationFramework.java"

OLD = '''    void generate(){
        WorldGenerationPipelineRunState state = new WorldGenerationPipelineRunState();
        worldgenPhaseResetAndSeed(state);
        worldgenPhasePlazaAndRoads(state);
        worldgenPhaseRoomsAndFactionClaims(state);
        worldgenPhaseRoadAdjacencyFixturesAndValidation(state);
        worldgenPhaseBoundaryInterwallAndMetadata(state);
        worldgenPhasePopulationEconomyAndFinalCompile(state);
    }
'''

NEW = '''    void generate(){
        ZoneGenerationManager.generate(this);
    }
'''


def main(argv: Optional[Sequence[str]] = None) -> int:
    ap = argparse.ArgumentParser(description="Redirect World.generate to ZoneGenerationManager.")
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args(argv)
    text = TARGET.read_text(encoding="utf-8", errors="replace")
    if NEW in text:
        print("World.generate already delegates to ZoneGenerationManager.")
        return 0
    if OLD not in text:
        raise SystemExit("Expected split World.generate body not found. Run SPLIT_WORLD_GENERATE_PIPELINE.py --apply first or inspect manually.")
    updated = text.replace(OLD, NEW, 1)
    print("Redirect preview: World.generate -> ZoneGenerationManager.generate(this)")
    if not args.apply:
        print("Dry run only. Re-run with --apply to rewrite file.")
        return 0
    TARGET.write_text(updated, encoding="utf-8", newline="\n")
    print("Updated", TARGET)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
