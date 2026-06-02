package mechanist;

/** Fixture-placement ownership shell for staged world-generation extraction. */
final class ZoneFixturePlacementManager {
    private ZoneFixturePlacementManager() {}

    static void placeRoadAndRoomFixtures(World world, WorldGenerationPipelineRunState state) {
        if (world == null || state == null) return;
        world.worldgenPhaseRoadAdjacencyFixturesAndValidation(state);
    }
}
