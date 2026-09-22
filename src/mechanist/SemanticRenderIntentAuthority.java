package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetRegistry;

import java.util.Locale;
import java.util.Optional;

/**
 * Classifies player-facing item and world-object text into the strict semantic
 * render intents owned by {@link SemanticRenderAssetResolver}.
 *
 * This authority does not replace authored asset IDs. It supplies a typed,
 * theme-safe fallback when an authored or structured hint cannot be resolved
 * in the active registry.
 */
final class SemanticRenderIntentAuthority {
    static final String VERSION = "semantic-render-intent-authority-0.11-tool-blade-boundary";

    private SemanticRenderIntentAuthority() { }

    static Optional<SemanticRenderAssetResolver.RenderIntent> itemIntent(String rawName) {
        String text = normalizeItem(rawName);
        if (text.isBlank()) return Optional.empty();

        boolean toolBlade = contains(text, "saw blade", "cutter blade", "tool blade");
        if (!toolBlade && contains(text, "knife", "knives", "shiv", "dagger", "sword", "blade", "axe", "hatchet",
                "spear", "polearm", "gun", "pistol", "rifle", "carbine", "shotgun", "bolter",
                "flamer", "melta", "stubber", "autocannon", "lasgun", "lascannon", "ammo", "ammunition")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.WEAPON_ITEM_ICON);
        }
        if (contains(text, "armor", "armour", "helmet", "helm", "vest", "carapace", "flak",
                "clothing", "coat", "robe", "uniform", "rags", "coverall", "workwear", "overalls")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.ARMOR_ITEM_ICON);
        }
        if (contains(text, "stimulant", "narcotic", "drug", "dose", "injector", "opiate",
                "sedative", "combat stim", "painkiller")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.DRUG_ITEM_ICON);
        }
        if (contains(text, "medkit", "medical kit", "bandage", "suture", "medicine", "antiseptic",
                "tourniquet", "splint", "first aid", "trauma kit")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.MEDICAL_ITEM_ICON);
        }
        if (toolBlade || contains(text, "wrench", "spanner", "hammer", "shovel", "spade", "cutter", "drill",
                "saw", "tool", "repair kit", "maintenance kit", "fabrication kit", "maintenance tools")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.TOOL_ITEM_ICON);
        }
        if (contains(text, "machine part", "component", "bearing", "fastener", "rivet", "circuit",
                "scrap plate", "construction supplies", "reagent", "industrial part")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON);
        }
        if (contains(text, "relic", "prayer", "devotional", "holy object", "icon of faith",
                "religious object", "rosary", "censer")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.RELIGIOUS_OBJECT_ITEM_ICON);
        }
        if (contains(text, "datapad", "data pad", "data device", "data slate", "dataslate", "terminal",
                "cogitator", "chip", "knowledge device", "skill device", "memory core")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.DATA_DEVICE_ITEM_ICON);
        }
        boolean fixedWaterFixture = contains(text, "water barrel", "water storage fixture", "water dispenser");
        if (!fixedWaterFixture && contains(text, "food", "ration", "meal", "water", "canteen", "flask",
                "drink", "provisions", "nutrient")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.FOOD_ITEM_ICON);
        }
        if (contains(text, "trade good", "commodity", "barter", "merchandise", "wares", "luxury good",
                "cargo lot", "export good", "import good")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.TRADE_GOOD_ITEM_ICON);
        }
        return Optional.empty();
    }

    static Optional<SemanticRenderAssetResolver.RenderIntent> objectIntent(String rawText) {
        String text = normalize(rawText);
        if (text.isBlank()) return Optional.empty();

        // A light mounted on or associated with a bulkhead/hatch remains a light fixture.
        // Leave these generic light semantics unclassified so the owning light authority
        // can use its dedicated fixture/switch/sensor hint instead of collapsing them
        // into the broader door/hatch/bulkhead intent.
        if (contains(text, "light fixture", "light switch", "motion sensor")
                && contains(text, "door", "hatch", "bulkhead")) {
            return Optional.empty();
        }

        // "Bulkhead" is also structural-wall vocabulary. Do not turn an explicitly
        // structural bulkhead panel/partition/wall into door art merely because the
        // generic bulkhead token is present. Explicit door/hatch wording still wins.
        boolean structuralBulkhead = contains(text, "bulkhead")
                && contains(text, "wall", "panel", "partition")
                && !contains(text, "door", "hatch");
        if (!structuralBulkhead && contains(text, "door", "hatch", "bulkhead")) {
            if (contains(text, "open", "opened", "unsealed")) {
                return Optional.of(SemanticRenderAssetResolver.RenderIntent.DOOR_OPEN);
            }
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED);
        }
        if (contains(text, "corridor", "hallway", "passage")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.CORRIDOR_TILE);
        }
        if (contains(text, "sidewalk", "pavement", "walkway")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.SIDEWALK_TILE);
        }
        if (contains(text, "road", "street", "lane")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.ROAD_TILE);
        }
        if (contains(text, "wall", "partition", "barricade")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.WALL_TILE);
        }
        if (contains(text, "floor", "deck", "ground")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.FLOOR_TILE);
        }
        if (contains(text, "corpse", "body", "cadaver", "remains", "decay", "rotting")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.CORPSE_DECAY);
        }
        if (contains(text, "machine", "assembler", "forge", "smelter", "boiler", "condenser")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.MACHINE_OBJECT);
        }
        if (contains(text, "fixture", "shelf", "cot", "bed", "barrel", "dispenser")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.FIXTURE_OBJECT);
        }
        return Optional.empty();
    }

    static Optional<String> resolve(AssetRegistry registry, SemanticRenderAssetResolver.RenderIntent intent) {
        return SemanticRenderAssetResolver.resolve(registry, intent).map(meta -> meta.id());
    }

    static Optional<String> resolveActive(SemanticRenderAssetResolver.RenderIntent intent) {
        return resolve(AssetManager.registry(), intent);
    }

    static String auditSummary() {
        return "authority=" + VERSION + " typedIntent=true toolBladeBoundary=true registry="
                + SemanticRenderAssetResolver.auditSummary(AssetManager.registry());
    }

    private static String normalizeItem(String raw) {
        return normalize(ItemQuality.stripManufacturingIdentity(ItemQuality.stripQuality(raw)));
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        return raw.toLowerCase(Locale.ROOT)
                .replace('‑', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replaceAll("[^a-z0-9+./ -]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean contains(String text, String... needles) {
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && text.contains(needle.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}