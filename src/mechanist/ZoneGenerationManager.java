package mechanist;

/**
 * High-level owner for zone generation phase orchestration.
 *
 * The low-level algorithms still live on World during the current extraction
 * stage. This manager owns the sequence and routes major phases through named
 * subsystem managers so later passes can move phase bodies and algorithms out
 * one manager at a time.
 */
final class ZoneGenerationManager {
    private ZoneGenerationManager() {}

    static void generate(World world) {
        if (world == null) return;
        WorldGenerationPipelineRunState state = new WorldGenerationPipelineRunState();
        TileGenerationManager.resetSubstrate(world, state);
        world.worldgenPhasePlazaAndRoads(state);
        RoomGenerationManager.placeRoadFirstRoomsAndClaims(world, state);
        ZoneFixturePlacementManager.placeRoadAndRoomFixtures(world, state);
        world.worldgenPhaseBoundaryInterwallAndMetadata(state);
        EntitySpawnManager.populateAndInitializeEconomy(world, state);
        TileStructureAuthority.authorRoomRoofs(world);
        TileStructureAuthority.authorOpenRoads(world);
        ArchitecturalClutterAuthority.enable(world,true);
        // This is the only final acceptance label: all population, compiled tiles,
        // roofs, roads and architectural clutter are in place before validation.
        World.GenerationValidationResult result =
                world.validateWorldgenSelfReport("FINAL_POST_PLACEMENT");
        world.finalGenerationValidation = result;
        String acceptance = result.summary() + " repairs=" + state.repairs
                + " widenedCorridors=" + state.widenedCorridors
                + " zone=" + world.zoneType.label + " layer=" + world.layerText()
                + " seed=" + world.seed;
        if ("ACCEPTED".equals(result.status())) DebugLog.audit("WORLDGEN_ACCEPTANCE", acceptance);
        else DebugLog.warn("WORLDGEN_ACCEPTANCE", acceptance);
        SectorGenerationTraceAuthority.record(world, "VALIDATION", acceptance);
    }
}
