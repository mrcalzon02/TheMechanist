package mechanist;

import java.awt.Dimension;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Properties;

/** Verifies world-size setup ownership across preview, creation, existing-world, and save-load paths. */
final class WorldGenerationSetupPersistenceSmoke {
    public static void main(String[] args) throws Exception {
        Path storage = Path.of("build", "worldgen-setup-smoke-" + System.nanoTime()).toAbsolutePath();
        System.setProperty(GameStorageManager.OVERRIDE_PROPERTY, storage.toString());
        try {
            verifyAllFixedSquareDimensions();
            long seed = 867530912345L;
            WorldSetupSettings selected = WorldSetupSettings.standard();
            selected.zoneSize = 0;
            selected.zoneDensity = 0;

            WorldSetupSettings activeBeforePreview = WorldSetupSettings.standard();
            activeBeforePreview.priceDifficulty = 2;
            WorldGenerationApi.setActiveSettings(activeBeforePreview);
            WorldAtlas preview = WorldAtlas.preview(seed, selected);
            preview.generateScaffold();
            require(preview.hiveWorld.settings().zoneSize == 0, "preview must use selected zone size");
            require(!Files.exists(CampaignWorldApi.worldFile(preview.hiveWorld)), "preview must not write a .mechworld file");
            require(WorldGenerationApi.settings().encode().equals(activeBeforePreview.encode()), "preview must not mutate process-wide active settings");
            requireWorldMatches(preview.currentWorld(), selected, "preview");

            HiveWorldDefinition stale = CampaignWorldApi.createDefinition(seed, WorldSetupSettings.standard());
            CampaignWorldApi.saveWorldDefinition(stale);
            require(Files.exists(CampaignWorldApi.worldFile(stale)), "stale world fixture should exist");

            WorldAtlas created = WorldAtlas.createNew(seed, selected);
            created.generateScaffold();
            require(created.hiveWorld.settings().zoneSize == 0, "new-world creation must replace stale setup for the selected seed");
            requireWorldMatches(created.currentWorld(), selected, "new world");
            Properties stored = load(CampaignWorldApi.worldFile(created.hiveWorld));
            require(selected.encode().equals(stored.getProperty("worlddef.setup")), "new-world file must store selected setup");

            WorldAtlas existing = WorldAtlas.loadExisting(seed, WorldSetupSettings.standard());
            require(existing.hiveWorld.settings().zoneSize == 0, "existing-world load must trust its stored setup");

            WorldSetupSettings compactSavedRun = WorldSetupSettings.standard();
            compactSavedRun.zoneSize = 0;
            WorldAtlas restored = WorldAtlas.loadSavedRun(seed, compactSavedRun);
            restored.generateScaffold();
            require(restored.hiveWorld.settings().zoneSize == 0, "saved run setup must override a mismatched world definition");
            requireWorldMatches(restored.currentWorld(), compactSavedRun, "saved run");
        } finally {
            if (Files.exists(storage)) {
                try (var paths = Files.walk(storage)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (Exception ignored) { }
                    });
                }
            }
        }
    }

    private static void verifyAllFixedSquareDimensions() {
        long[] seeds = {1L, 867530912345L, Long.MAX_VALUE, -992211L};
        for (int index = 0; index < WorldSetupSettings.ZONE_SIZE_TILES.length; index++) {
            WorldSetupSettings settings = WorldSetupSettings.standard();
            settings.zoneSize = index;
            int edge = WorldSetupSettings.ZONE_SIZE_TILES[index];
            require(WorldTopologySettingsBridge.fixedSectorSize(settings).tiles == edge, "topology bridge mismatch for " + edge);
            for (long seed : seeds) {
                Dimension actual = WorldGenerationApi.zoneSliceSize(seed, settings);
                require(actual.width == edge && actual.height == edge,
                        "size " + edge + " changed for seed " + seed + ": " + actual.width + "x" + actual.height);
            }
        }
        boolean rejected = false;
        try { WorldTopologyContract.SectorSize.fromTiles(750); }
        catch (IllegalArgumentException expected) { rejected = true; }
        require(rejected, "non-option size 750 must be rejected, not rounded");
    }

    private static Properties load(Path file) throws Exception {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) { p.load(in); }
        return p;
    }

    private static void requireWorldMatches(World world, WorldSetupSettings settings, String label) {
        require(world.generationSettings().encode().equals(settings.encode()), label + " world must retain selected setup");
        Dimension expected = WorldGenerationApi.zoneSliceSize(world.seed, settings);
        require(world.w == expected.width && world.h == expected.height,
                label + " dimensions expected " + expected.width + "x" + expected.height + " but were " + world.w + "x" + world.h);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private WorldGenerationSetupPersistenceSmoke() { }
}
