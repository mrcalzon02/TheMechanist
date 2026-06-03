package mechanist;

/** Tile-generation ownership shell for substrate, sanitation, and descriptor compile flow. */
final class TileGenerationManager {
    private TileGenerationManager() {}

    static void resetSubstrate(World world, WorldGenerationPipelineRunState state) {
        if (world == null || state == null) return;
        world.worldgenPhaseResetAndSeed(state);
    }

    static void compileTiles(World world, WorldGenerationPipelineRunState state) {
        if (world == null || state == null) return;
        TileDataCompilationAuthority.Result tileCompileResult = TileDataCompilationAuthority.compile(world);
        SectorGenerationTraceAuthority.record(world, "TILE-COMPILE", "Tile descriptors compiled for renderer and audit view: " + tileCompileResult.summary());
        DebugLog.audit("TILE_DATA_COMPILE", tileCompileResult.summary());
    }
}
