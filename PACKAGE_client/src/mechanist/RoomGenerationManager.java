package mechanist;

/** Room-generation ownership shell for the staged World.generate extraction. */
final class RoomGenerationManager {
    private RoomGenerationManager() {}

    static void placeRoadFirstRoomsAndClaims(World world, WorldGenerationPipelineRunState state) {
        if (world == null || state == null) return;
        world.worldgenPhaseRoomsAndFactionClaims(state);
    }
}
