#!/usr/bin/env python3
"""
SPLIT_WORLD_GENERATE_PIPELINE.py

Narrow local rewrite for WorldRuntimeGenerationFramework.java.

It replaces only the current World.generate() method with a phase orchestrator and
inserts named phase methods inside class World.  This is deliberately scripted
because the file is very large and should not be manually replaced through the
connector contents API.

Run from repository root:

    py -3 scripts\SPLIT_WORLD_GENERATE_PIPELINE.py --apply

Then run smoke:

    powershell -ExecutionPolicy Bypass -File scripts\SMOKE_FUNCTION_MAP_OPERATIONS_WINDOWS.ps1
"""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Optional, Sequence, Tuple

ROOT = Path.cwd()
TARGET = ROOT / "src" / "mechanist" / "WorldRuntimeGenerationFramework.java"

NEW_GENERATE = '''    void generate(){
        WorldGenerationPipelineRunState state = new WorldGenerationPipelineRunState();
        worldgenPhaseResetAndSeed(state);
        worldgenPhasePlazaAndRoads(state);
        worldgenPhaseRoomsAndFactionClaims(state);
        worldgenPhaseRoadAdjacencyFixturesAndValidation(state);
        worldgenPhaseBoundaryInterwallAndMetadata(state);
        worldgenPhasePopulationEconomyAndFinalCompile(state);
    }

'''

PHASE_METHODS = '''    void worldgenPhaseResetAndSeed(WorldGenerationPipelineRunState state){
        // 0.9.10ji PLAZA-FIRST / ROAD-SECOND SECTOR SEED:
        // The plaza is the first physical claim placed in every zone. Roads then grow
        // from that central civic tile mass, while later rooms are bumped down the
        // generation queue and must respect both the plaza and road network.
        state.target = 0;
        state.attempts = 0;
        state.repairs = 0;
        state.widenedCorridors = 0;
        state.doorwayObjectRepairs = 0;
        resetGenerationState(seed ^ 0x91010ADEL);
        for(int x=0;x<w;x++) for(int y=0;y<h;y++) tiles[x][y]='#';
        SectorGenerationTraceAuthority.record(this, "RESET", "Plaza-first zone substrate filled before central anchor placement.");
        visitedZones[Math.max(0,Math.min(2,zoneX-1))][Math.max(0,Math.min(2,zoneY-1))] = true;
    }

    void worldgenPhasePlazaAndRoads(WorldGenerationPipelineRunState state){
        state.centralPlaza = seedCentralPlazaAnchor();
        SectorGenerationTraceAuthority.record(this, "PLAZA", "Central plaza placed before roads, rooms, fixtures, and transitions.", state.centralPlaza, false);
        int plazaApron = seedCentralPlazaRoadApron(state.centralPlaza);
        SectorGenerationTraceAuthority.record(this, "PLAZA", "Central plaza one-tile street apron cut before road placement apronTiles=" + plazaApron + ".", state.centralPlaza, false);

        RoadGridIntegrationAuthority.Result roadGridResult = RoadGridIntegrationAuthority.apply(this, r);
        int plazaStreetLinks = connectCentralPlazaToStreetGrid(state.centralPlaza);
        SectorGenerationTraceAuthority.record(this, "ROADS", "Core road grid placed after the central plaza anchor: " + roadGridResult.summary() + " plazaStreetLinks=" + plazaStreetLinks);
        DebugLog.audit("PLAZA_FIRST_ROAD_GRID", roadGridResult.summary() + " plazaStreetLinks=" + plazaStreetLinks);

        decorateCentralPlaza(state.centralPlaza);
        SectorGenerationTraceAuthority.record(this, "ASSET-SWEEP", "Central plaza decoration sweep completed after roads so plaza remains the sole first generation claim.", state.centralPlaza, false);
    }

    void worldgenPhaseRoomsAndFactionClaims(WorldGenerationPipelineRunState state){
        state.target = roadFirstRoomTarget();
        state.attempts = buildRoadFirstRoomLayout(state.target);
        SectorGenerationTraceAuthority.record(this, "ROOM", "Road-first room seed completed rooms=" + rooms.size() + " target=" + state.target + " attempts=" + state.attempts);

        ensureRoomQuotaFromStreetBlocks(state.target);
        applyFactionRoomManifest();
        SectorGenerationTraceAuthority.record(this, "MANIFEST", "Faction room manifest applied to road-first rooms.");
        markSpecialRooms();
        SectorGenerationTraceAuthority.record(this, "SPECIAL", "Special room flags marked.");
        placeMartianEmergencyMachineRooms();
        SectorGenerationTraceAuthority.record(this, "SPECIAL", "Emergency machine rooms placed where validation allowed.");
        int shellRepairs = normalizeRoomShellsAndCorridorHalos();
        SectorGenerationTraceAuthority.record(this, "ROOM", "Room shell normalization enforced one wall layer and access halos repairs=" + shellRepairs + ".");
        assignRoomFactions();
        SectorGenerationTraceAuthority.record(this, "FACTIONS", "Room faction ownership assigned.");
        seedNeutralContestRooms();
        SectorGenerationTraceAuthority.record(this, "CONTEST", "Neutral contest rooms seeded.");
        EconomicGenerationBiasAuthority.Result economicBiasResult = EconomicGenerationBiasAuthority.apply(this, r);
        DebugLog.audit("ECONOMIC_GENERATION_BIAS", economicBiasResult.summary());
        placeSpecialInReachableRoom(sewerLayer ? 'v' : 'S', 0.18, 0.20);
        placeSpecialInReachableRoom('E', 0.82, 0.20);
        stampFactionRepresentativeBarNearTransit();
    }

    void worldgenPhaseRoadAdjacencyFixturesAndValidation(WorldGenerationPipelineRunState state){
        RoadAdjacencyIntegrationAuthority.Result roadAdjacencyResult = RoadAdjacencyIntegrationAuthority.apply(this, r);
        SectorGenerationTraceAuthority.record(this, "ROADS", "Road-adjacent civic structures applied after room placement: " + roadAdjacencyResult.summary());
        DebugLog.audit("ROAD_ADJACENCY_FOUNDATION", roadAdjacencyResult.summary());
        RoadFrontageFixtureAuthority.Result roadFrontageResult = RoadFrontageFixtureAuthority.apply(this, r);
        SectorGenerationTraceAuthority.record(this, "FRONTAGE", "Roadside fixtures/lights/shrines/vending sweep applied: " + roadFrontageResult.summary());
        DebugLog.audit("ROAD_FRONTAGE_FIXTURES", roadFrontageResult.summary());
        RoomFixtureInteractionAuthority.Result roomFixtureResult = RoomFixtureInteractionAuthority.apply(this, r);
        SectorGenerationTraceAuthority.record(this, "ROOM-ASSETS", "Room fixture interaction sweep applied: " + roomFixtureResult.summary());
        DebugLog.audit("ROOM_FIXTURE_INTERACTIONS", roomFixtureResult.summary());
        validateSpecialTransitions();
        validateWorldgenSelfReport("POST_ROAD_FIRST_TRANSITION_STAMP");
        state.repairs = repairWorldgenValidationIssues("POST_ROAD_FIRST_TRANSITION_STAMP");
        state.widenedCorridors = widenOneTileCorridors("POST_REPAIR_TWO_WIDE_PREFERENCE");
        if(state.widenedCorridors > 0) {
            validateSpecialTransitions();
            state.repairs += repairWorldgenValidationIssues("POST_CORRIDOR_WIDEN");
        }
        if(state.repairs > 0 || state.widenedCorridors > 0) validateWorldgenSelfReport("POST_REPAIR");
        if(!allRoomsReachableStrict()) {
            DebugLog.warn("LEVELGEN_POST_VALIDATE", "Road-first validation still found detached content; preserving rooms for Zone Audit trace. seed="+seed+" zone="+zoneType.label);
        }
        validateWorldgenSelfReport("FINAL_PRE_POPULATE");
    }

    void worldgenPhaseBoundaryInterwallAndMetadata(WorldGenerationPipelineRunState state){
        applyBoundedOuterHiveWall();
        SectorGenerationTraceAuthority.record(this, "BOUNDARY", "Bounded outer arcology wall/interwall envelope applied.");
        int sectorDoorRepairs = stampRoadMaintenanceTransitionDoors();
        if(sectorDoorRepairs > 0) SectorGenerationTraceAuthority.record(this, "TRANSITIONS", "Road-end double doors stamped against the inner maintenance bulkhead, not map tile edges.");
        int boundaryRepairs = repairUnreachableRooms("POST_BOUNDARY_LAYER");
        if(boundaryRepairs > 0){
            state.repairs += boundaryRepairs;
            validateWorldgenSelfReport("POST_BOUNDARY_REPAIR");
        }
        applyInterstitialHiveMass();
        SectorGenerationTraceAuthority.record(this, "INTERWALL", "Interstitial arcology mass and hidden features seeded after road-first room validation.");
        PersonnelPopulationApi.ensureLedgers(this, r);
        EnvironmentalHazardVisibilityApi.Result hazardResult = EnvironmentalHazardVisibilityApi.seed(this, r);
        DebugLog.audit("HAZARD_WARNING_OVERLAY", hazardResult.summary());
        TrapInteractionApi.Result trapResult = TrapInteractionApi.seed(this, r);
        DebugLog.audit("TRAP_BOOBYTRAP_FOUNDATION", trapResult.summary());
        EconomicLocalTopologyMetadataSurfaceAuthority.Result localTopologyResult = EconomicLocalTopologyMetadataSurfaceAuthority.apply(this);
        DebugLog.audit("LOCAL_TOPOLOGY_METADATA", localTopologyResult.summary());
        EconomicTopologyReportingOverlayAuthority.Result topologyOverlayResult = EconomicTopologyReportingOverlayAuthority.apply(this);
        DebugLog.audit("ECONOMIC_TOPOLOGY_REPORTING_OVERLAY", topologyOverlayResult.summary());
        EconomicTopologyMapIntelBridgeAuthority.Result mapIntelResult = EconomicTopologyMapIntelBridgeAuthority.apply(this);
        DebugLog.audit("ECONOMIC_TOPOLOGY_MAP_INTEL_BRIDGE", mapIntelResult.summary());
        RetargetReadinessAuditAuthority.Result retargetResult = RetargetReadinessAuditAuthority.apply(this);
        DebugLog.audit("RETARGET_READINESS_AUDIT", retargetResult.summary());
        DebugLog.audit("WORLDGEN_ACCEPTANCE", "status=" + (state.repairs>0 ? "REPAIRED" : "ACCEPTED") + " repairs="+state.repairs+" rooms="+rooms.size()+" zone="+zoneType.label+" layer="+layerText()+" seed="+seed);
    }

    void worldgenPhasePopulationEconomyAndFinalCompile(WorldGenerationPipelineRunState state){
        populate();
        SectorGenerationTraceAuthority.record(this, "POPULATION", "Population, resident amenities, and ordinary entities seeded after road-first terrain/facility layout.");
        WorldEconomyInitializationAuthority.Result economyInit = WorldEconomyInitializationAuthority.apply(this, r);
        DebugLog.audit("WORLD_ECONOMY_INITIALIZATION", economyInit.summary());
        state.doorwayObjectRepairs = 0;
        for(int pass=0; pass<4; pass++){
            int fixed = sanitizeGeneratedObjectDoorAccess();
            state.doorwayObjectRepairs += fixed;
            if(fixed == 0) break;
        }
        if(state.doorwayObjectRepairs > 0) DebugLog.audit("OBJECT_DOORWAY_CLEARANCE_REPAIR", "relocatedOrRemoved="+state.doorwayObjectRepairs+" zone="+zoneType.label+" layer="+layerText()+" seed="+seed);
        TileDataCompilationAuthority.Result tileCompileResult = TileDataCompilationAuthority.compile(this);
        SectorGenerationTraceAuthority.record(this, "TILE-COMPILE", "Tile descriptors compiled for renderer and audit view: " + tileCompileResult.summary());
        DebugLog.audit("TILE_DATA_COMPILE", tileCompileResult.summary());
        DebugLog.log("Generated road-first world seed="+seed+" zone="+zoneType.label+" layer="+layerText()+" rooms="+rooms.size()+" attempts="+state.attempts+" roads=core-first");
    }

'''


def find_method(text: str, signature: str) -> Tuple[int, int]:
    start = text.find(signature)
    if start < 0:
        raise RuntimeError(f"Could not find signature: {signature}")
    brace = text.find("{", start)
    if brace < 0:
        raise RuntimeError("Could not find opening brace")
    depth = 0
    in_string = None
    escaped = False
    in_line_comment = False
    in_block_comment = False
    i = brace
    while i < len(text):
        ch = text[i]
        nxt = text[i + 1] if i + 1 < len(text) else ""
        if in_line_comment:
            if ch == "\n": in_line_comment = False
            i += 1
            continue
        if in_block_comment:
            if ch == "*" and nxt == "/":
                in_block_comment = False
                i += 2
            else:
                i += 1
            continue
        if in_string:
            if escaped: escaped = False
            elif ch == "\\": escaped = True
            elif ch == in_string: in_string = None
            i += 1
            continue
        if ch == "/" and nxt == "/":
            in_line_comment = True
            i += 2
            continue
        if ch == "/" and nxt == "*":
            in_block_comment = True
            i += 2
            continue
        if ch in ("'", '"'):
            in_string = ch
            i += 1
            continue
        if ch == "{": depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return start, i + 1
        i += 1
    raise RuntimeError("Could not find method end")


def apply_split(text: str) -> str:
    if "worldgenPhaseResetAndSeed" in text:
        raise RuntimeError("World.generate pipeline already appears split")
    start, end = find_method(text, "    void generate(){")
    insertion = text.find("    void applyBoundedOuterHiveWall(){", end)
    if insertion < 0:
        raise RuntimeError("Could not find insertion anchor applyBoundedOuterHiveWall")
    return text[:start] + NEW_GENERATE + PHASE_METHODS + text[end:]


def main(argv: Optional[Sequence[str]] = None) -> int:
    ap = argparse.ArgumentParser(description="Split World.generate into named pipeline phases.")
    ap.add_argument("--apply", action="store_true")
    args = ap.parse_args(argv)
    text = TARGET.read_text(encoding="utf-8", errors="replace")
    updated = apply_split(text)
    print("World.generate split preview:")
    print("  before bytes:", len(text.encode("utf-8", errors="replace")))
    print("  after bytes:", len(updated.encode("utf-8", errors="replace")))
    if not args.apply:
        print("Dry run only. Re-run with --apply to rewrite file.")
        return 0
    TARGET.write_text(updated, encoding="utf-8", newline="\n")
    print("Updated", TARGET)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
