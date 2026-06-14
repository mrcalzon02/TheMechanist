package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Coverage/audit surface for the Phase 4.16 semantic rendering migration. */
final class SemanticAssetMigrationCoverageAuthority {
    static final String VERSION = "semantic-asset-migration-coverage-0.1";

    enum Family { ZONE_TILE, ROOM_TILE, INFRASTRUCTURE, DOOR, FURNITURE, CONTAINER, ITEM }

    record Entry(Family family, SemanticRenderAssetResolver.RenderIntent intent, String assetId,
                 String assetName, boolean available, String reason) {}

    record Report(int total, int available, int missing,
                  Map<Family, Integer> availableByFamily,
                  Map<Family, Integer> missingByFamily,
                  List<Entry> entries) {
        boolean complete() { return missing == 0; }
        String summary() { return "semanticAssetCoverage version=" + VERSION + " total=" + total + " available=" + available + " missing=" + missing; }
    }

    private SemanticAssetMigrationCoverageAuthority() {}

    static Report audit(AssetRegistry registry) {
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;
        ArrayList<Entry> entries = new ArrayList<>();
        EnumMap<Family, Integer> availableByFamily = new EnumMap<>(Family.class);
        EnumMap<Family, Integer> missingByFamily = new EnumMap<>(Family.class);
        int available = 0;
        int missing = 0;
        for (SemanticRenderAssetResolver.RenderIntent intent : SemanticRenderAssetResolver.RenderIntent.values()) {
            Family family = familyOf(intent);
            SemanticRenderAssetResolver.Resolution resolution = SemanticRenderAssetResolver.resolve(safe, intent);
            if (resolution.found()) {
                AssetMetadata asset = resolution.asset;
                entries.add(new Entry(family, intent, asset.id(), asset.name(), true, resolution.reason));
                available++;
                availableByFamily.merge(family, 1, Integer::sum);
            } else {
                entries.add(new Entry(family, intent, "<missing>", "Missing semantic art", false, resolution.reason));
                missing++;
                missingByFamily.merge(family, 1, Integer::sum);
            }
        }
        for (Family family : Family.values()) {
            availableByFamily.putIfAbsent(family, 0);
            missingByFamily.putIfAbsent(family, 0);
        }
        return new Report(entries.size(), available, missing, Map.copyOf(availableByFamily), Map.copyOf(missingByFamily), List.copyOf(entries));
    }

    static List<String> auditLines(AssetRegistry registry) {
        Report report = audit(registry);
        ArrayList<String> lines = new ArrayList<>();
        lines.add(report.summary());
        for (Family family : Family.values()) {
            lines.add(family + ": available=" + report.availableByFamily().getOrDefault(family, 0)
                    + " missing=" + report.missingByFamily().getOrDefault(family, 0));
        }
        for (Entry entry : report.entries()) {
            lines.add(entry.family() + " / " + entry.intent() + " -> " + entry.assetId()
                    + " / " + entry.assetName() + " / " + entry.reason());
        }
        return List.copyOf(lines);
    }

    static Family familyOf(SemanticRenderAssetResolver.RenderIntent intent) {
        if (intent == null) return Family.ZONE_TILE;
        return switch (intent) {
            case STREETLIGHT_FIXTURE -> Family.INFRASTRUCTURE;
            case DOOR_CLOSED, DOOR_OPEN -> Family.DOOR;
            case WORKSHOP_TABLE, DINING_TABLE, MEDICAL_TABLE, SHRINE_ALTAR,
                    MARKET_COUNTER, ADMINISTRATIVE_DESK, INTERROGATION_DESK -> Family.FURNITURE;
            case TOOLBOX_CONTAINER, MEDICAL_CABINET_CONTAINER, WEAPONS_LOCKER_CONTAINER,
                    WARDROBE_CONTAINER, CARGO_CONTAINER, FILING_CABINET_CONTAINER -> Family.CONTAINER;
            case WEAPON_ITEM_ICON, ARMOR_ITEM_ICON, TOOL_ITEM_ICON, MEDICAL_ITEM_ICON,
                    DRUG_ITEM_ICON, FOOD_ITEM_ICON, INDUSTRIAL_COMPONENT_ITEM_ICON,
                    TRADE_GOOD_ITEM_ICON, RELIGIOUS_OBJECT_ITEM_ICON, DATA_DEVICE_ITEM_ICON -> Family.ITEM;
            case MEDICAL_FLOOR, SECURITY_FLOOR, ADMINISTRATIVE_FLOOR,
                    RELIGIOUS_FLOOR, TRANSIT_FLOOR, WAREHOUSE_FLOOR -> Family.ROOM_TILE;
            default -> Family.ZONE_TILE;
        };
    }
}
