package mechanist;

/** Entity/NPC spawn ownership shell for staged world-generation extraction. */
final class EntitySpawnManager {
    private EntitySpawnManager() {}

    static void populateAndInitializeEconomy(World world, WorldGenerationPipelineRunState state) {
        if (world == null || state == null) return;
        world.worldgenPhasePopulationEconomyAndFinalCompile(state);
    }
}
