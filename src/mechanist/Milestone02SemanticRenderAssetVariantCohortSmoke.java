package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Focused regression proof for stable semantic texture variety.
 * Lower-priority but otherwise valid assets must never enter the variant cohort.
 */
public final class Milestone02SemanticRenderAssetVariantCohortSmoke {
    public static void main(String[] args) throws Exception {
        AssetRegistry registry = registry();
        boolean sawPlain = false;
        boolean sawRibbed = false;

        for (long key = 0; key < 64; key++) {
            SemanticRenderAssetResolver.Resolution first = SemanticRenderAssetResolver.resolve(
                    registry, SemanticRenderAssetResolver.RenderIntent.GENERIC_FLOOR, key);
            SemanticRenderAssetResolver.Resolution repeated = SemanticRenderAssetResolver.resolve(
                    registry, SemanticRenderAssetResolver.RenderIntent.GENERIC_FLOOR, key);

            require(first.found(), "generic floor stable variant was missing for key " + key);
            require(repeated.found(), "repeated generic floor stable variant was missing for key " + key);
            require(first.asset.id().equals(repeated.asset.id()),
                    "stable key " + key + " changed variant between identical resolutions");
            require(!"LOW-0001".equals(first.asset.id()),
                    "stable variety admitted a lower-priority generic floor into the top cohort");

            sawPlain |= "TOP-0001".equals(first.asset.id());
            sawRibbed |= "TOP-0002".equals(first.asset.id());
        }

        require(sawPlain && sawRibbed,
                "stable variety did not exercise both equally ranked top-priority floor variants");
        System.out.println("Milestone02SemanticRenderAssetVariantCohortSmoke PASS "
                + SemanticRenderAssetResolver.VERSION);
    }

    private static AssetRegistry registry() throws Exception {
        Map<String, AssetMetadata> entries = new LinkedHashMap<>();
        entries.put("TOP-0001", asset("TOP-0001", "Generic Floor Plain",
                "assets/tiles/generic/floor_plain.png", "generic floor tile"));
        entries.put("TOP-0002", asset("TOP-0002", "Generic Floor Ribbed",
                "assets/tiles/generic/floor_ribbed.png", "generic floor tile"));
        entries.put("LOW-0001", asset("LOW-0001", "Plain Deck",
                "assets/tiles/deck_plain.png", "default plain deck tile"));

        Constructor<AssetRegistry> ctor = AssetRegistry.class.getDeclaredConstructor(
                java.nio.file.Path.class, java.nio.file.Path.class, Map.class);
        ctor.setAccessible(true);
        return ctor.newInstance(java.nio.file.Path.of("."), null, entries);
    }

    private static AssetMetadata asset(String id, String name, String path, String description) {
        return new AssetMetadata(id, path, name, AssetType.FLOOR_TILE, description);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone02SemanticRenderAssetVariantCohortSmoke() {}
}
