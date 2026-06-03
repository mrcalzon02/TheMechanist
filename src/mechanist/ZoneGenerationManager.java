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
    }
}
