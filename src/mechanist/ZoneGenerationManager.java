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
        // Keep normal generation unchanged and profiling disabled unless
        // requested by a developer or the bounded diagnostic jobs.
        if (!Boolean.getBoolean("mechanist.profileWorldgen")) {
            TileGenerationManager.resetSubstrate(world, state);
            world.worldgenPhasePlazaAndRoads(state);
            RoomGenerationManager.placeRoadFirstRoomsAndClaims(world, state);
            ZoneFixturePlacementManager.placeRoadAndRoomFixtures(world, state);
            world.worldgenPhaseBoundaryInterwallAndMetadata(state);
            EntitySpawnManager.populateAndInitializeEconomy(world, state);
            finishGeneration(world, state);
            return;
        }
        long begin = System.nanoTime();
        try {
            profilePhase(world, "substrate", () ->
                    TileGenerationManager.resetSubstrate(world, state));
            profilePhase(world, "plaza-roads", () ->
                    world.worldgenPhasePlazaAndRoads(state));
            profilePhase(world, "rooms-faction", () ->
                    RoomGenerationManager.placeRoadFirstRoomsAndClaims(world, state));
            profilePhase(world, "road-room-fixtures", () ->
                    ZoneFixturePlacementManager.placeRoadAndRoomFixtures(world, state));
            profilePhase(world, "boundary-interwall", () ->
                    world.worldgenPhaseBoundaryInterwallAndMetadata(state));
            profilePhase(world, "population-economy-compile", () ->
                    EntitySpawnManager.populateAndInitializeEconomy(world, state));
            finishGeneration(world, state);
        } finally {
            DebugLog.log("WORLDGEN_PROFILE phase=all status=finished totalMs="
                    + elapsedMs(begin) + " tiles=" + world.w + "x" + world.h
                    + " rooms=" + world.rooms.size() + " seed=" + world.seed);
        }
    }

    /** Final acceptance is shared by ordinary and profiled generation. */
    private static void finishGeneration(World world, WorldGenerationPipelineRunState state) {
        TileStructureAuthority.authorRoomRoofs(world);
        TileStructureAuthority.authorOpenRoads(world);
        ArchitecturalClutterAuthority.enable(world, true);
        World.GenerationValidationResult result =
                world.validateWorldgenSelfReport("FINAL_POST_PLACEMENT");
        world.finalGenerationValidation = result;
        String acceptance = result.summary() + " repairs=" + state.repairs
                + " widenedCorridors=" + state.widenedCorridors
                + " zone=" + world.zoneType.label + " layer=" + world.layerText()
                + " seed=" + world.seed;
        if ("ACCEPTED".equals(result.status()))
            DebugLog.audit("WORLDGEN_ACCEPTANCE", acceptance);
        else DebugLog.warn("WORLDGEN_ACCEPTANCE", acceptance);
        SectorGenerationTraceAuthority.record(world, "VALIDATION", acceptance);
    }

    private static void profilePhase(World world, String phase, Runnable work) {
        long start = System.nanoTime();
        DebugLog.log("WORLDGEN_PROFILE phase=" + phase + " status=started"
                + " seed=" + world.seed + " tiles=" + world.w + "x" + world.h);
        try {
            work.run();
            DebugLog.log("WORLDGEN_PROFILE phase=" + phase + " status=completed"
                    + " elapsedMs=" + elapsedMs(start)
                    + " rooms=" + world.rooms.size());
        } catch (RuntimeException | Error failure) {
            DebugLog.log("WORLDGEN_PROFILE phase=" + phase + " status=failed"
                    + " elapsedMs=" + elapsedMs(start)
                    + " error=" + failure.getClass().getSimpleName());
            throw failure;
        }
    }


    /**
     * Subphase checkpoints are opt-in and never change procedural RNG,
     * world-generation data, or phase ordering.
     */
    static long beginDetail(World world, String stage) {
        if (!Boolean.getBoolean("mechanist.profileWorldgen")) return 0L;
        long started = System.nanoTime();
        DebugLog.log("WORLDGEN_DETAIL stage=" + stage + " status=started"
                + " seed=" + world.seed + " rooms=" + world.rooms.size()
                + " npcs=" + world.npcs.size());
        return started;
    }

    static void endDetail(World world, String stage, long started) {
        if (started == 0L) return;
        DebugLog.log("WORLDGEN_DETAIL stage=" + stage + " status=completed"
                + " elapsedMs=" + elapsedMs(started)
                + " rooms=" + world.rooms.size()
                + " npcs=" + world.npcs.size());
    }

    private static long elapsedMs(long start) {
        return Math.max(0L, (System.nanoTime() - start) / 1_000_000L);
    }
}
