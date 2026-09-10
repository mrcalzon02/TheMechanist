package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.nio.file.Path;
import java.util.List;

/** End-to-end checks for the visually audited compiled graphical asset corpus. */
public final class DeepSemanticAssetCoverageSmoke {
    public static void main(String[] args) throws Exception {
        Path root = args.length == 0
                ? Path.of("").toAbsolutePath().normalize()
                : Path.of(args[0]).toAbsolutePath().normalize();
        Path index = root.resolve("PACKAGE_client/assets/indexes/asset_content_index_32px.tsv");
        AssetRegistry registry = AssetRegistry.loadFromCompiledContentIndex(index, root);
        AssetRegistry.RegistryAudit audit = registry.audit(true);

        require(registry.size() == 2_898, "expected 2,898 compiled cells, got " + registry.size());
        require(audit.duplicateIdCount() == 0, "duplicate semantic IDs: " + audit.duplicateIdCount());
        require(audit.unknownTypeCount() == 0, "unknown runtime types: " + audit.unknownTypeCount());
        require(audit.blankDescriptionCount() == 0, "blank semantic descriptions: " + audit.blankDescriptionCount());
        require(audit.missingPathCount() == 0, "missing compiled images: " + audit.missingPathCount());
        require(registry.byType(AssetType.INTERNAL).size() == 14,
                "Faction_2 must expose exactly fourteen reserved blank slots as INTERNAL");

        List<AssetMetadata> roadDoors = assetsFrom(registry, "/Doors/Road_doors_");
        require(countType(roadDoors, AssetType.ROAD_TILE) == 14,
                "Road_doors must contain fourteen navigable road cells");
        require(countType(roadDoors, AssetType.FIXTURE) == 11,
                "Road_doors must contain eleven gate/platform fixtures");

        List<AssetMetadata> bulkheads = assetsFrom(registry, "/BULKHEAD/");
        require(countType(bulkheads, AssetType.FIXTURE) == 14,
                "BULKHEAD atlases must contain fourteen explicit door-state fixtures");
        require(countType(bulkheads, AssetType.WALL_TILE) == 36,
                "BULKHEAD atlases must retain thirty-six structural wall cells");

        requireResolved(registry, SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED);
        requireResolved(registry, SemanticRenderAssetResolver.RenderIntent.DOOR_OPEN);
        requireResolved(registry, SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON);
        requireResolved(registry, SemanticRenderAssetResolver.RenderIntent.TRADE_GOOD_ITEM_ICON);
        requireResolved(registry, SemanticRenderAssetResolver.RenderIntent.DATA_DEVICE_ITEM_ICON);
        requireResolved(registry, SemanticRenderAssetResolver.RenderIntent.ARMOR_ITEM_ICON);
        requireResolved(registry, SemanticRenderAssetResolver.RenderIntent.WEAPON_ITEM_ICON);
        requireResolved(registry, SemanticRenderAssetResolver.RenderIntent.SHRINE_ALTAR);
        requireResolved(registry, SemanticRenderAssetResolver.RenderIntent.MARKET_COUNTER);

        System.out.println("DeepSemanticAssetCoverageSmoke PASS entries=" + registry.size()
                + " runtimeTypes=" + audit.categoryCount()
                + " resolver=" + SemanticRenderAssetResolver.VERSION);
    }

    private static List<AssetMetadata> assetsFrom(AssetRegistry registry, String pathFragment) {
        return registry.all().stream()
                .filter(asset -> asset.pathOrUri().replace('\\', '/').contains(pathFragment))
                .toList();
    }

    private static long countType(List<AssetMetadata> assets, AssetType type) {
        return assets.stream().filter(asset -> asset.type() == type).count();
    }

    private static void requireResolved(
            AssetRegistry registry,
            SemanticRenderAssetResolver.RenderIntent intent
    ) {
        SemanticRenderAssetResolver.Resolution resolution = SemanticRenderAssetResolver.resolve(registry, intent);
        require(resolution.found(), "runtime resolver has no indexed candidate for " + intent);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private DeepSemanticAssetCoverageSmoke() {}
}
