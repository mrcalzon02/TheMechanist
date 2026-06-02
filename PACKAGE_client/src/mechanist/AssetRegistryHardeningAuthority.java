package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import javax.swing.ImageIcon;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stage 9 guardrail for the Semantic Asset Registry migration.
 *
 * The earlier stages intentionally kept legacy aliases alive so the renderer did
 * not collapse while the registry was being populated.  Stage 9 does not delete
 * every historical loader yet; it hardens the categories that already entered
 * the registry by verifying that they have registry coverage and typed missing
 * art, and by giving runtime preview systems a clear fail-closed policy instead
 * of silently using semantically wrong legacy art.
 */
public final class AssetRegistryHardeningAuthority {
    public static final String VERSION = "0.9.10kb-stage9-registry-hardening";

    private static final AssetType[] MIGRATED_TYPES = {
            AssetType.PORTRAIT,
            AssetType.WALL_TILE,
            AssetType.FLOOR_TILE,
            AssetType.ROAD_TILE,
            AssetType.SIDEWALK_TILE,
            AssetType.CORRIDOR_TILE,
            AssetType.OBJECT,
            AssetType.MACHINE,
            AssetType.FIXTURE,
            AssetType.ITEM_ICON,
            AssetType.WEAPON_ICON,
            AssetType.ARMOR_ICON,
            AssetType.UI_ICON,
            AssetType.CORPSE_DECAY
    };

    private AssetRegistryHardeningAuthority() {}

    public record HardeningAudit(
            int registryEntries,
            int migratedTypesChecked,
            int typedFallbacksChecked,
            int missingCoverageTypes,
            int unmappedTileAliases,
            int missingTileAliasMetadata,
            int missingObjectMappings,
            int missingItemMappings,
            int missingPortraitPartitions,
            List<String> errors
    ) {
        public boolean passed() { return errors.isEmpty(); }

        public String summaryLine() {
            return "entries=" + registryEntries
                    + " migratedTypes=" + migratedTypesChecked
                    + " typedFallbacks=" + typedFallbacksChecked
                    + " missingCoverageTypes=" + missingCoverageTypes
                    + " unmappedTileAliases=" + unmappedTileAliases
                    + " missingTileAliasMetadata=" + missingTileAliasMetadata
                    + " missingObjectMappings=" + missingObjectMappings
                    + " missingItemMappings=" + missingItemMappings
                    + " missingPortraitPartitions=" + missingPortraitPartitions
                    + " errors=" + errors.size();
        }
    }

    public static AssetType[] migratedTypes() {
        return MIGRATED_TYPES.clone();
    }

    /**
     * Stage 9 typed missing icon policy.  A bad/missing migrated asset must show
     * its family as missing; it must not fall back to a visually unrelated glyph.
     */
    public static ImageIcon typedMissingIcon(AssetType type) {
        return AssetManager.missingAssetIcon(type == null ? AssetType.UNKNOWN : type);
    }

    public static boolean isMigratedType(AssetType type) {
        if (type == null) return false;
        for (AssetType migrated : MIGRATED_TYPES) if (migrated == type) return true;
        return false;
    }

    public static HardeningAudit audit(Path projectRoot, AssetRegistry registry) throws Exception {
        AssetRegistry safe = Objects.requireNonNullElseGet(registry, () -> AssetManager.registry());
        ArrayList<String> errors = new ArrayList<>();
        EnumMap<AssetType, Integer> coverage = new EnumMap<>(AssetType.class);
        int typedFallbacks = 0;
        int missingCoverage = 0;
        int unmappedTileAliases = 0;
        int missingTileAliasMetadata = 0;
        int missingObjectMappings = 0;
        int missingItemMappings = 0;
        int missingPortraitPartitions = 0;

        for (AssetType type : MIGRATED_TYPES) {
            int count = safe.byType(type).size();
            coverage.put(type, count);
            if (count <= 0) {
                missingCoverage++;
                errors.add("Migrated asset type has no registry rows: " + type);
            }
            ImageIcon typed = AssetManager.missingAssetIcon(type);
            if (typed == null || !AssetManager.isMissingAssetIcon(typed) || typed == AssetManager.missingAssetIcon()) {
                errors.add("Typed missing icon is not distinct/recognized for " + type);
            }
            typedFallbacks++;
        }

        Map<String, String> tileAliases = TileSemanticAssetAuthority.auditAliasMap();
        if (tileAliases.isEmpty()) {
            errors.add("Tile semantic alias map is empty after Stage 5");
        }
        for (Map.Entry<String, String> entry : tileAliases.entrySet()) {
            String alias = entry.getKey();
            String id = entry.getValue();
            Optional<AssetMetadata> metadata = safe.find(id);
            if (metadata.isEmpty()) {
                missingTileAliasMetadata++;
                errors.add("Tile alias " + alias + " maps to missing asset ID " + id);
            } else if (!isMigratedType(metadata.get().type())) {
                errors.add("Tile alias " + alias + " maps to non-migrated type " + metadata.get().type() + " id=" + id);
            }
        }
        for (String required : List.of("road_north_south", "road_sidewalk", "floor_exterior_maintenance_corridor_north_south", "wall_bulkhead", "door_double")) {
            if (TileSemanticAssetAuthority.assetIdForAlias(required).isEmpty()) {
                unmappedTileAliases++;
                errors.add("Required migrated tile alias is unmapped: " + required);
            }
        }

        for (Map.Entry<String, String> entry : ObjectSemanticAssetAuthority.auditExactMappings().entrySet()) {
            if (safe.find(entry.getValue()).isEmpty()) {
                missingObjectMappings++;
                errors.add("Object semantic mapping points at missing asset: " + entry.getKey() + " -> " + entry.getValue());
            }
        }
        for (String item : List.of("Scrap knife", "Bolter", "Water Barrel", "Sleeping Cot", "Scavenger rags", "Arbites armor", "PDF Armor", "Newspaper")) {
            String id = ItemSemanticAssetAuthority.semanticAssetIdForItemName(item);
            if (safe.find(id).isEmpty()) {
                missingItemMappings++;
                errors.add("Item semantic mapping points at missing asset: " + item + " -> " + id);
            }
        }

        PortraitSemanticAssetAuthority.PartitionAudit portraitAudit = PortraitSemanticAssetAuthority.audit(projectRoot, safe);
        if (!portraitAudit.passed()) {
            missingPortraitPartitions = portraitAudit.errors().size();
            errors.addAll(portraitAudit.errors());
        }

        return new HardeningAudit(
                safe.size(),
                MIGRATED_TYPES.length,
                typedFallbacks,
                missingCoverage,
                unmappedTileAliases,
                missingTileAliasMetadata,
                missingObjectMappings,
                missingItemMappings,
                missingPortraitPartitions,
                List.copyOf(errors));
    }
}
