package mechanist;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

/**
 * Focused 06.R continuity smoke: opening an existing saved world must not
 * synthesize extra history or rewrite its definition before a zone is loaded.
 * Does not build a 500-1000 tile physical slice.
 */
final class Milestone06SavedWorldHistoryContinuitySmoke {
    public static void main(String[] args) throws Exception {
        Path storage = Path.of("build", "world-history-restore-"
                + System.nanoTime()).toAbsolutePath();
        String previous = System.getProperty(GameStorageManager.OVERRIDE_PROPERTY);
        System.setProperty(GameStorageManager.OVERRIDE_PROPERTY, storage.toString());
        try {
            long seed = 61026L;
            WorldSetupSettings setup = WorldSetupSettings.standard();
            setup.simulationAge = 3;
            HiveWorldDefinition saved = CampaignWorldApi.createDefinition(seed, setup);
            String key = saved.zoneKey(1, 1, 2, 2, 4, false);
            saved.zoneEpochs.put(key, "saved faction epoch sentinel");
            saved.zoneFacilities.put(key, "saved facility sentinel");
            saved.zoneProduction.put(key, "saved production sentinel");
            saved.zoneStockMovements.put(key, "saved stock movement sentinel");
            saved.zoneConflictLosses.put(key, "saved loss sentinel");
            saved.zoneMaterializedItems.put(key, "saved materialized item sentinel");
            saved.zoneLaborAssignments.put(key, "saved labor sentinel");
            CampaignWorldApi.saveWorldDefinition(saved);
            Path definitionFile = CampaignWorldApi.worldFile(saved);
            require(Files.isRegularFile(definitionFile),
                    "a saved world definition must exist before restoration");
            byte[] beforeBytes = Files.readAllBytes(definitionFile);

            long start = System.nanoTime();
            WorldAtlas restored = WorldAtlas.loadSavedRun(seed, setup);
            long savedRunMs = (System.nanoTime() - start) / 1_000_000L;
            require(restored.hiveWorld.loadedFromWorldFile,
                    "saved run must recognize its on-disk definition");
            require(restored.hiveWorld.settings().encode().equals(setup.encode()),
                    "saved run must retain exact selected setup");
            requireHistoricalLedgers(saved, restored.hiveWorld, key);
            require(Arrays.equals(beforeBytes, Files.readAllBytes(definitionFile)),
                    "opening a saved world must not rewrite its definition file");

            start = System.nanoTime();
            WorldAtlas existing = WorldAtlas.loadExisting(seed, setup);
            long existingMs = (System.nanoTime() - start) / 1_000_000L;
            requireHistoricalLedgers(saved, existing.hiveWorld, key);
            require(Arrays.equals(beforeBytes, Files.readAllBytes(definitionFile)),
                    "loading an existing world must not advance or rewrite history");
            System.out.println("Milestone 06 saved-world history continuity smoke passed. "
                    + "savedRunLoadMs=" + savedRunMs
                    + " existingWorldLoadMs=" + existingMs);
        } finally {
            if (previous == null) System.clearProperty(GameStorageManager.OVERRIDE_PROPERTY);
            else System.setProperty(GameStorageManager.OVERRIDE_PROPERTY, previous);
            if (Files.exists(storage)) try (var paths = Files.walk(storage)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (Exception ignored) { }
                });
            }
        }
    }

    private static void requireHistoricalLedgers(HiveWorldDefinition expected,
                                                  HiveWorldDefinition actual,
                                                  String key) {
        require(expected.zoneEpochs.equals(actual.zoneEpochs),
                "opening a world must not expand faction-history batches");
        require(expected.zoneFacilities.equals(actual.zoneFacilities),
                "opening a world must not expand facility-history batches");
        require(expected.zoneProduction.equals(actual.zoneProduction),
                "opening a world must not expand production-history batches");
        require(expected.zoneStockMovements.equals(actual.zoneStockMovements),
                "opening a world must not expand distribution batches");
        require(expected.zoneConflictLosses.equals(actual.zoneConflictLosses),
                "opening a world must not expand conflict-loss batches");
        require(expected.zoneMaterializedItems.equals(actual.zoneMaterializedItems),
                "opening a world must not expand item-materialization batches");
        require(expected.zoneLaborAssignments.equals(actual.zoneLaborAssignments),
                "opening a world must not expand labor-assignment batches");
        require(actual.zoneEpochs.containsKey(key) && actual.zoneEpochs.size() == 1,
                "persisted sparse world history must be preserved exactly");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06SavedWorldHistoryContinuitySmoke() { }
}
