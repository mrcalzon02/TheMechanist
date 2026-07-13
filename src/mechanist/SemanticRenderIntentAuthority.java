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
    static final String VERSION = "semantic-render-intent-authority-0.2-infrastructure";

    private SemanticRenderIntentAuthority() { }

    static Optional<SemanticRenderAssetResolver.RenderIntent> itemIntent(String rawName) {
        String text = normalizeItem(rawName);
        if (text.isBlank()) return Optional.empty();

        if (contains(text, "knife", "shiv", "dagger", "sword", "blade", "axe", "hatchet",
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
        if (contains(text, "wrench", "spanner", "hammer", "shovel", "spade", "cutter", "drill",
                "saw", "tool", "repair kit", "maintenance kit", "fabrication kit")) {
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
        if (contains(text, "food", "ration", "meal", "water", "canteen", "bottle", "flask",
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

        if (contains(text, "door", "hatch", "bulkhead")) {
            if (contains(text, "open", "opened", "unsealed")) {
                return Optional.of(SemanticRenderAssetResolver.RenderIntent.DOOR_OPEN);
            }
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED);
        }

        if (contains(text, "streetlight", "street light", "lamp post", "street lamp", "lamppost")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.STREETLIGHT_FIXTURE);
        }
        if (contains(text, "traffic light", "signal light", "crossing signal")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.TRAFFIC_LIGHT_FIXTURE);
        }
        if (contains(text, "security camera", "surveillance camera", "cctv")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.SECURITY_CAMERA_FIXTURE);
        }
        if (contains(text, "junction box", "electrical box", "power box")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.JUNCTION_BOX_FIXTURE);
        }
        if (contains(text, "ventilation unit", "vent unit", "air handler", "exhaust fan")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.VENTILATION_UNIT_FIXTURE);
        }
        if (contains(text, "sewer pipe", "waste pipe", "drain pipe", "sludge pipe")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.SEWER_PIPE_FIXTURE);
        }
        if (contains(text, "water pipe", "fresh water pipe", "water main")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.WATER_PIPE_FIXTURE);
        }
        if (contains(text, "transformer", "power transformer", "electrical transformer")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.TRANSFORMER_MACHINE);
        }
        if (contains(text, "generator", "power generator", "genset")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.GENERATOR_MACHINE);
        }

        if (contains(text, "toolbox", "tool box")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.TOOLBOX_CONTAINER);
        }
        if (contains(text, "medical cabinet", "medicine cabinet", "clinic cabinet")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.MEDICAL_CABINET_CONTAINER);
        }
        if (contains(text, "weapons locker", "weapon locker", "armory locker", "armoury locker")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.WEAPONS_LOCKER_CONTAINER);
        }
        if (contains(text, "wardrobe", "clothes cabinet", "clothing cabinet")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.WARDROBE_CONTAINER);
        }
        if (contains(text, "filing cabinet", "file cabinet", "records cabinet")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.FILING_CABINET_CONTAINER);
        }
        if (contains(text, "refrigerated storage", "cold storage", "freezer", "refrigerator", "chiller locker")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.REFRIGERATED_STORAGE_CONTAINER);
        }
        if (contains(text, "cargo container", "shipping container", "cargo crate", "freight crate")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.CARGO_CONTAINER);
        }

        if (contains(text, "medical table", "operating table", "surgery table")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.MEDICAL_TABLE);
        }
        if (contains(text, "workshop table", "workbench", "fabrication table")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.WORKSHOP_TABLE);
        }
        if (contains(text, "dining table", "mess table", "kitchen table")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.DINING_TABLE);
        }
        if (contains(text, "altar", "shrine")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.SHRINE_ALTAR);
        }
        if (contains(text, "market counter", "shop counter", "stall counter", "trader counter")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.MARKET_COUNTER);
        }
        if (contains(text, "interrogation desk", "security interview desk")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.INTERROGATION_DESK);
        }
        if (contains(text, "administrative desk", "office desk", "records desk")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.ADMINISTRATIVE_DESK);
        }
        return Optional.empty();
    }

    static Optional<String> resolveItemFamily(String rawName) {
        return resolveItemFamily(AssetManager.registry(), rawName);
    }

    static Optional<String> resolveItemFamily(AssetRegistry registry, String rawName) {
        return itemIntent(rawName).flatMap(intent -> resolve(registry, intent));
    }

    static Optional<String> resolveObjectFamily(String rawText) {
        return resolveObjectFamily(AssetManager.registry(), rawText);
    }

    static Optional<String> resolveObjectFamily(AssetRegistry registry, String rawText) {
        return objectIntent(rawText).flatMap(intent -> resolve(registry, intent));
    }

    static Optional<String> resolve(AssetRegistry registry, SemanticRenderAssetResolver.RenderIntent intent) {
        if (intent == null) return Optional.empty();
        SemanticRenderAssetResolver.Resolution resolution = SemanticRenderAssetResolver.resolve(registry, intent);
        return resolution.found() ? Optional.of(resolution.asset.id()) : Optional.empty();
    }

    static String auditSummary() {
        return "authority=" + VERSION + " lanes=item+object authoredHintsRemainFirst=true strictFamilyFallback=true";
    }

    private static String normalizeItem(String raw) {
        if (raw == null) return "";
        return normalize(ItemQuality.stripManufacturingIdentity(ItemQuality.stripQuality(raw)));
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        return raw.toLowerCase(Locale.ROOT)
                .replace('‑', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("[^a-z0-9+./ ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean contains(String text, String... needles) {
        for (String needle : needles) {
            String normalized = normalize(needle);
            if (!normalized.isBlank() && text.contains(normalized)) return true;
        }
        return false;
    }
}
