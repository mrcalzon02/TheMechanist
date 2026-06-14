package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Semantic bridge from renderer intent to indexed graphical asset metadata.
 *
 * This is the repair-path layer that sits between live renderers and the
 * existing semantic asset indexes. Callers ask for meaning such as sewer floor,
 * generic wall, streetlight fixture, closed door, room furniture, zone theme,
 * container class, or item icon. This resolver searches the semantic registry
 * and refuses known-bad cross-theme fallbacks.
 */
final class SemanticRenderAssetResolver {
    static final String VERSION = "semantic-render-asset-resolver-0.4-door-infrastructure";

    enum RenderIntent {
        SEWER_FLOOR,
        SEWER_WALL,
        GENERIC_FLOOR,
        GENERIC_WALL,
        INDUSTRIAL_FLOOR,
        INDUSTRIAL_WALL,
        HABITATION_FLOOR,
        HABITATION_WALL,
        MARKET_FLOOR,
        MARKET_WALL,
        MEDICAL_FLOOR,
        SECURITY_FLOOR,
        ADMINISTRATIVE_FLOOR,
        RELIGIOUS_FLOOR,
        TRANSIT_FLOOR,
        WAREHOUSE_FLOOR,
        NOBLE_FLOOR,
        SLUM_FLOOR,
        STREETLIGHT_FIXTURE,
        DOOR_CLOSED,
        DOOR_OPEN,
        WORKSHOP_TABLE,
        DINING_TABLE,
        MEDICAL_TABLE,
        SHRINE_ALTAR,
        MARKET_COUNTER,
        ADMINISTRATIVE_DESK,
        INTERROGATION_DESK,
        TOOLBOX_CONTAINER,
        MEDICAL_CABINET_CONTAINER,
        WEAPONS_LOCKER_CONTAINER,
        WARDROBE_CONTAINER,
        CARGO_CONTAINER,
        FILING_CABINET_CONTAINER,
        WEAPON_ITEM_ICON,
        ARMOR_ITEM_ICON,
        TOOL_ITEM_ICON,
        MEDICAL_ITEM_ICON,
        DRUG_ITEM_ICON,
        FOOD_ITEM_ICON,
        INDUSTRIAL_COMPONENT_ITEM_ICON,
        TRADE_GOOD_ITEM_ICON,
        RELIGIOUS_OBJECT_ITEM_ICON,
        DATA_DEVICE_ITEM_ICON
    }

    static final class Resolution {
        final RenderIntent intent;
        final AssetMetadata asset;
        final String reason;

        private Resolution(RenderIntent intent, AssetMetadata asset, String reason) {
            this.intent = intent;
            this.asset = asset;
            this.reason = reason == null ? "" : reason;
        }

        static Resolution found(RenderIntent intent, AssetMetadata asset, String reason) {
            return new Resolution(intent, asset, reason);
        }

        static Resolution missing(RenderIntent intent, String reason) {
            return new Resolution(intent, null, reason);
        }

        boolean found() {
            return asset != null;
        }

        String assetIdOrMissing() {
            return asset == null ? "<missing>" : asset.id();
        }
    }

    private SemanticRenderAssetResolver() {}

    static Resolution resolve(AssetRegistry registry, RenderIntent intent) {
        AssetRegistry safe = registry == null ? AssetRegistry.empty() : registry;
        List<AssetMetadata> candidates = candidatesFor(safe, intent);
        if (candidates.isEmpty()) {
            return Resolution.missing(intent, "No semantic asset matched " + intent + ". Renderer must use explicit missing-art fallback, not unrelated art.");
        }
        return Resolution.found(intent, candidates.get(0), "Matched indexed semantic asset for " + intent + ".");
    }

    static Optional<AssetMetadata> optional(AssetRegistry registry, RenderIntent intent) {
        Resolution resolution = resolve(registry, intent);
        return resolution.found() ? Optional.of(resolution.asset) : Optional.empty();
    }

    static boolean canUse(AssetMetadata asset, RenderIntent intent) {
        if (asset == null || intent == null) return false;
        String haystack = haystack(asset);
        return switch (intent) {
            case SEWER_FLOOR -> isFloor(asset) && themed(haystack, "sewer", "sump", "drain", "utility tunnel") && !generic(haystack);
            case SEWER_WALL -> isWall(asset) && themed(haystack, "sewer", "sump", "drain", "utility tunnel") && !generic(haystack);
            case GENERIC_FLOOR -> isFloor(asset) && generic(haystack) && !themed(haystack, "sewer", "sump");
            case GENERIC_WALL -> isWall(asset) && generic(haystack) && !themed(haystack, "sewer", "sump");
            case INDUSTRIAL_FLOOR -> isFloor(asset) && themed(haystack, "industrial", "factory", "machine shop", "workshop") && !themed(haystack, "sewer");
            case INDUSTRIAL_WALL -> isWall(asset) && themed(haystack, "industrial", "factory", "machine shop", "workshop") && !themed(haystack, "sewer");
            case HABITATION_FLOOR -> isFloor(asset) && themed(haystack, "habitation", "hab", "apartment", "residential") && !themed(haystack, "sewer");
            case HABITATION_WALL -> isWall(asset) && themed(haystack, "habitation", "hab", "apartment", "residential") && !themed(haystack, "sewer");
            case MARKET_FLOOR -> isFloor(asset) && themed(haystack, "market", "bazaar", "commercial", "retail") && !themed(haystack, "sewer");
            case MARKET_WALL -> isWall(asset) && themed(haystack, "market", "bazaar", "commercial", "retail") && !themed(haystack, "sewer");
            case MEDICAL_FLOOR -> isFloor(asset) && themed(haystack, "medical", "clinic", "hospital", "surgery") && !themed(haystack, "sewer");
            case SECURITY_FLOOR -> isFloor(asset) && themed(haystack, "security", "checkpoint", "prison", "brig") && !themed(haystack, "sewer");
            case ADMINISTRATIVE_FLOOR -> isFloor(asset) && themed(haystack, "administrative", "office", "records", "bureau") && !themed(haystack, "sewer");
            case RELIGIOUS_FLOOR -> isFloor(asset) && themed(haystack, "religious", "shrine", "chapel", "altar") && !themed(haystack, "sewer");
            case TRANSIT_FLOOR -> isFloor(asset) && themed(haystack, "transit", "station", "platform", "rail") && !themed(haystack, "sewer");
            case WAREHOUSE_FLOOR -> isFloor(asset) && themed(haystack, "warehouse", "storage", "cargo", "loading") && !themed(haystack, "sewer");
            case NOBLE_FLOOR -> isFloor(asset) && themed(haystack, "noble", "luxury", "estate", "manor") && !themed(haystack, "sewer");
            case SLUM_FLOOR -> isFloor(asset) && themed(haystack, "slum", "shanty", "tenement", "scrap") && !themed(haystack, "sewer");
            case STREETLIGHT_FIXTURE -> (asset.type() == AssetType.FIXTURE || asset.type() == AssetType.OBJECT) && themed(haystack, "streetlight", "street light", "lamp post", "street lamp") && !themed(haystack, "system inventory", "item icon", "ui icon");
            case DOOR_CLOSED -> doorType(asset) && themed(haystack, "door") && themed(haystack, "closed", "shut") && !themed(haystack, "open") && !generic(haystack);
            case DOOR_OPEN -> doorType(asset) && themed(haystack, "door") && themed(haystack, "open", "opened") && !themed(haystack, "closed", "shut") && !generic(haystack);
            case WORKSHOP_TABLE -> fixtureOrObject(asset) && themed(haystack, "workshop table", "workbench", "fabrication table");
            case DINING_TABLE -> fixtureOrObject(asset) && themed(haystack, "dining table", "mess table", "kitchen table");
            case MEDICAL_TABLE -> fixtureOrObject(asset) && themed(haystack, "medical table", "operating table", "surgery table");
            case SHRINE_ALTAR -> fixtureOrObject(asset) && themed(haystack, "altar", "shrine");
            case MARKET_COUNTER -> fixtureOrObject(asset) && themed(haystack, "market counter", "shop counter", "stall counter");
            case ADMINISTRATIVE_DESK -> fixtureOrObject(asset) && themed(haystack, "administrative desk", "office desk", "records desk");
            case INTERROGATION_DESK -> fixtureOrObject(asset) && themed(haystack, "interrogation desk", "security desk");
            case TOOLBOX_CONTAINER -> containerType(asset) && themed(haystack, "toolbox", "tool box");
            case MEDICAL_CABINET_CONTAINER -> containerType(asset) && themed(haystack, "medical cabinet", "medicine cabinet", "clinic cabinet");
            case WEAPONS_LOCKER_CONTAINER -> containerType(asset) && themed(haystack, "weapons locker", "weapon locker", "armory locker");
            case WARDROBE_CONTAINER -> containerType(asset) && themed(haystack, "wardrobe", "clothes cabinet");
            case CARGO_CONTAINER -> containerType(asset) && themed(haystack, "cargo container", "crate", "shipping container");
            case FILING_CABINET_CONTAINER -> containerType(asset) && themed(haystack, "filing cabinet", "records cabinet", "file cabinet");
            case WEAPON_ITEM_ICON -> weaponIcon(asset) && themed(haystack, "weapon", "gun", "blade", "ammo");
            case ARMOR_ITEM_ICON -> armorIcon(asset) && themed(haystack, "armor", "armour", "helmet", "clothing");
            case TOOL_ITEM_ICON -> itemIcon(asset) && themed(haystack, "tool", "wrench", "repair", "fabrication");
            case MEDICAL_ITEM_ICON -> itemIcon(asset) && themed(haystack, "medical", "medkit", "bandage", "suture", "medicine");
            case DRUG_ITEM_ICON -> itemIcon(asset) && themed(haystack, "drug", "narcotic", "stimulant", "dose");
            case FOOD_ITEM_ICON -> itemIcon(asset) && themed(haystack, "food", "ration", "meal", "water");
            case INDUSTRIAL_COMPONENT_ITEM_ICON -> itemIcon(asset) && themed(haystack, "component", "part", "industrial", "machine part");
            case TRADE_GOOD_ITEM_ICON -> itemIcon(asset) && themed(haystack, "trade good", "goods", "commodity", "barter");
            case RELIGIOUS_OBJECT_ITEM_ICON -> itemIcon(asset) && themed(haystack, "religious", "relic", "prayer", "holy object", "devotional");
            case DATA_DEVICE_ITEM_ICON -> itemIcon(asset) && themed(haystack, "data", "device", "datapad", "terminal", "chip");
        };
    }

    static List<String> auditLines(AssetRegistry registry) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Semantic render asset resolver " + VERSION + ".");
        for (RenderIntent intent : RenderIntent.values()) {
            Resolution r = resolve(registry, intent);
            lines.add(intent + ": " + r.assetIdOrMissing() + " - " + r.reason);
        }
        return lines;
    }

    private static List<AssetMetadata> candidatesFor(AssetRegistry registry, RenderIntent intent) {
        return registry.all().stream()
                .filter(asset -> canUse(asset, intent))
                .sorted(Comparator.comparing((AssetMetadata asset) -> priority(asset, intent)).reversed()
                        .thenComparing(AssetMetadata::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AssetMetadata::id))
                .toList();
    }

    private static int priority(AssetMetadata asset, RenderIntent intent) {
        String h = haystack(asset);
        int score = 0;
        if (contains(h, normalize(intent.name()))) score += 8;
        if (asset.name() != null && contains(asset.name(), normalize(intent.name()))) score += 5;
        if (asset.semanticDescription() != null && contains(asset.semanticDescription(), normalize(intent.name()))) score += 4;
        if (asset.pathOrUri() != null && contains(asset.pathOrUri(), normalize(intent.name()))) score += 2;
        if (intent == RenderIntent.STREETLIGHT_FIXTURE && contains(h, "streetlight")) score += 10;
        if (intent == RenderIntent.DOOR_OPEN || intent == RenderIntent.DOOR_CLOSED) {
            if (contains(h, "variant")) score += 3;
            if (asset.type() == AssetType.FIXTURE) score += 10;
            if (contains(h, "semantic state")) score += 12;
        }
        return score;
    }

    private static boolean isFloor(AssetMetadata asset) { return asset.type() == AssetType.FLOOR_TILE || asset.type() == AssetType.ROAD_TILE || asset.type() == AssetType.SIDEWALK_TILE || asset.type() == AssetType.CORRIDOR_TILE; }
    private static boolean isWall(AssetMetadata asset) { return asset.type() == AssetType.WALL_TILE; }
    private static boolean fixtureOrObject(AssetMetadata asset) { return asset.type() == AssetType.FIXTURE || asset.type() == AssetType.OBJECT || asset.type() == AssetType.MACHINE; }
    private static boolean containerType(AssetMetadata asset) { return asset.type() == AssetType.OBJECT || asset.type() == AssetType.FIXTURE || asset.type() == AssetType.ITEM_ICON; }
    private static boolean itemIcon(AssetMetadata asset) { return asset.type() == AssetType.ITEM_ICON || asset.type() == AssetType.WEAPON_ICON || asset.type() == AssetType.ARMOR_ICON; }
    private static boolean weaponIcon(AssetMetadata asset) { return asset.type() == AssetType.WEAPON_ICON || (asset.type() == AssetType.ITEM_ICON && themed(haystack(asset), "weapon")); }
    private static boolean armorIcon(AssetMetadata asset) { return asset.type() == AssetType.ARMOR_ICON || (asset.type() == AssetType.ITEM_ICON && themed(haystack(asset), "armor", "armour", "clothing")); }
    private static boolean doorType(AssetMetadata asset) { return asset.type() == AssetType.FIXTURE || asset.type() == AssetType.WALL_TILE || asset.type() == AssetType.FLOOR_TILE || asset.type() == AssetType.CORRIDOR_TILE; }
    private static boolean generic(String text) { return themed(text, "generic", "plain", "main floor", "main wall", "default"); }
    private static boolean themed(String text, String... needles) { return contains(text, needles); }

    private static String haystack(AssetMetadata asset) {
        return normalize(asset.id() + " " + asset.name() + " " + asset.pathOrUri() + " " + asset.type().displayName() + " " + asset.semanticDescription());
    }

    private static boolean contains(String text, String... needles) {
        String h = normalize(text);
        for (String needle : needles) {
            String n = normalize(needle);
            if (!n.isBlank() && h.contains(n)) return true;
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ').trim();
    }
}
