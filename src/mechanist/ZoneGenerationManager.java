package mechanist;

/**
 * High-level owner for zone generation phase orchestration.
 *
 * The low-level algorithms still live on World during the current extraction
 * stage. This manager is the first subsystem shell: it owns the sequence and
 * creates the run state, while later passes move individual phase bodies and
 * lower-level algorithms into RoomGenerationManager, TileGenerationManager,
 * EntitySpawnManager, ZoneFixturePlacementManager, and related authorities.
 */
final class ZoneGenerationManager {
    private ZoneGenerationManager() {}

    static void generate(World world) {
        if (world == null) return;
        WorldGenerationPipelineRunState state = new WorldGenerationPipelineRunState();
        world.worldgenPhaseResetAndSeed(state);
        world.worldgenPhasePlazaAndRoads(state);
        world.worldgenPhaseRoomsAndFactionClaims(state);
        world.worldgenPhaseRoadAdjacencyFixturesAndValidation(state);
        world.worldgenPhaseBoundaryInterwallAndMetadata(state);
        world.worldgenPhasePopulationEconomyAndFinalCompile(state);
    }
}
