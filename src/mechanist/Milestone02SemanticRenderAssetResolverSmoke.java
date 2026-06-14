package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Milestone02SemanticRenderAssetResolverSmoke {
    public static void main(String[] args) throws Exception {
        AssetRegistry registry = testRegistry();
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.SEWЕR_FLOOR.name().replace('Е', 'E'), "SEM-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.SEWЕR_WALL.name().replace('Е', 'E'), "SEM-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.GENERIC_FLOOR.name(), "GEN-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.GENERIC_WALL.name(), "GEN-0002");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.STREETLIGHT_FIXTURE.name(), "FIX-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED.name(), "DOR-0001");
        assertFound(registry, SemanticRenderAssetResolver.RenderIntent.DOOR_OPEN.name(), "DOR-0002");

        if (SemanticRenderAssetResolver.canUse(asset("BAD-0001", AssetType.ITEM_ICON, "System Inventory Light", "assets/ui/system_light.png", "system inventory icon light"), SemanticRenderAssetResolver.RenderIntent.STREETLIGHT_FIXTURE)) {
            throw new AssertionError("streetlight resolver accepted system inventory icon");
        }
        if (SemanticRenderAssetResolver.canUse(asset("BAD-0002", AssetType.FLOOR_TILE, "Sewer Floor", "assets/sewer/floor.png", "sewer floor"), SemanticRenderAssetResolver.RenderIntent.GENERIC_FLOOR)) {
            throw new AssertionError("generic floor resolver accepted sewer floor tile");
        }
        if (SemanticRenderAssetResolver.canUse(asset("BAD-0003", AssetType.FLOOR_TILE, "Generic Floor", "assets/generic/floor.png", "generic main floor"), SemanticRenderAssetResolver.RenderIntent.SEWЕR_FLOOR)) {
            throw new AssertionError("sewer floor resolver accepted generic floor tile");
        }
        if (SemanticRenderAssetResolver.canUse(asset("BAD-0004", AssetType.WALL_TILE, "Closed Doorish Wall", "assets/wall.png", "generic wall"), SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED)) {
            throw new AssertionError("closed-door resolver accepted generic wall");
        }

        System.out.println("Milestone02SemanticRenderAssetResolverSmoke PASS " + SemanticRenderAssetResolver.VERSION);
    }

    private static void assertFound(AssetRegistry registry, String intentName, String expectedId) {
        SemanticRenderAssetResolver.RenderIntent intent = SemanticRenderAssetResolver.RenderIntent.valueOf(intentName);
        SemanticRenderAssetResolver.Resolution resolution = SemanticRenderAssetResolver.resolve(registry, intent);
        if (!resolution.found()) {
            throw new AssertionError(intent + " did not resolve: " + resolution.reason);
        }
        if (!expectedId.equals(resolution.asset.id())) {
            throw new AssertionError(intent + " resolved " + resolution.asset.id() + " instead of " + expectedId);
        }
    }

    private static AssetRegistry testRegistry() throws Exception {
        Map<String, AssetMetadata> entries = new LinkedHashMap<>();
        put(entries, asset("SEM-0001", AssetType.FLOOR_TILE, "Sewer Floor Wet Utility Tunnel", "assets/tiles/sewer/floor_wet.png", "sewer floor wet utility tunnel tile"));
        put(entries, asset("SEM-0002", AssetType.WALL_TILE, "Sewer Wall Brick Drain", "assets/tiles/sewer/wall_brick.png", "sewer wall brick drain utility tunnel tile"));
        put(entries, asset("GEN-0001", AssetType.FLOOR_TILE, "Generic Floor Plain", "assets/tiles/generic/floor_plain.png", "generic main floor tile"));
        put(entries, asset("GEN-0002", AssetType.WALL_TILE, "Generic Wall Plain", "assets/tiles/generic/wall_plain.png", "generic main wall tile"));
        put(entries, asset("FIX-0001", AssetType.FIXTURE, "Streetlight Pole Lamp", "assets/fixtures/infrastructure/streetlight_pole.png", "streetlight fixture street light pole lamp"));
        put(entries, asset("DOR-0001", AssetType.FIXTURE, "Closed Door Variant", "assets/tiles/doors/door_closed_a.png", "door closed shut tile variant"));
        put(entries, asset("DOR-0002", AssetType.FIXTURE, "Open Door Variant", "assets/tiles/doors/door_open_a.png", "door open opened tile variant"));
        put(entries, asset("BAD-0001", AssetType.ITEM_ICON, "System Inventory Light", "assets/ui/system_light.png", "system inventory icon light"));
        Constructor<AssetRegistry> ctor = AssetRegistry.class.getDeclaredConstructor(java.nio.file.Path.class, java.nio.file.Path.class, Map.class);
        ctor.setAccessible(true);
        return ctor.newInstance(java.nio.file.Path.of("."), null, entries);
    }

    private static void put(Map<String, AssetMetadata> entries, AssetMetadata asset) {
        entries.put(asset.id(), asset);
    }

    private static AssetMetadata asset(String id, AssetType type, String name, String path, String description) {
        return new AssetMetadata(id, path, name, type, description);
    }

    private Milestone02SemanticRenderAssetResolverSmoke() {}
}
