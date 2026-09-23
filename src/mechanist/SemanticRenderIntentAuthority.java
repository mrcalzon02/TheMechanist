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
    static final String VERSION = "semantic-render-intent-authority-0.13-equipment-kit-boundary";

    private SemanticRenderIntentAuthority() { }

    static Optional<SemanticRenderAssetResolver.RenderIntent> itemIntent(String rawName) {
        String text = normalizeItem(rawName);
        if (text.isBlank()) return Optional.empty();

        boolean toolBlade = contains(text, "saw blade", "cutter blade", "tool blade");
        boolean equipmentKit = contains(text, "repair kit", "maintenance kit", "fabrication kit", "maintenance tools");
        if (toolBlade || equipmentKit) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.TOOL_ITEM_ICON);
        }
        if (contains(text, "knife", "knives", "shiv", "dagger", "sword", "blade", "axe", "hatchet",
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
                "tourniquet", "splint", "first aid", "trauma kit", "scalpel")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.MEDICAL_ITEM_ICON);
        }
        if (contains(text, "wrench", "spanner", "hammer", "shovel", "spade", "cutter", "drill",
                "saw", "tool")) {
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

        if (contains(text, "light fixture", "light switch", "motion sensor")
                && contains(text, "door", "hatch", "bulkhead")) {
            return Optional.empty();
        }

        boolean structuralBulkhead = contains(text, "bulkhead")
                && contains(text, "wall", "panel", "partition")
                && !contains(text, "door", "hatch");
        if (!structuralBulkhead && contains(text, "door", "hatch", "bulkhead")) {
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
        if (contains(text, "terminal", "console", "cogitator")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.TERMINAL_FIXTURE);
        }
        if (contains(text, "machine", "generator", "fabricator", "assembler", "smelter", "forge", "boiler", "condenser")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.MACHINE_FIXTURE);
        }
        if (contains(text, "bed", "cot", "bunk")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.BED_FIXTURE);
        }
        if (contains(text, "shelf", "rack", "storage shelf")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.SHELF_FIXTURE);
        }
        if (contains(text, "water barrel", "water storage fixture", "water dispenser")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.WATER_FIXTURE);
        }
        if (contains(text, "table", "desk", "workbench")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.TABLE_FIXTURE);
        }
        if (contains(text, "container", "crate", "chest", "locker")) {
            return Optional.of(SemanticRenderAssetResolver.RenderIntent.CONTAINER_FIXTURE);
        }
        return Optional.empty();
    }

    static Optional<String> resolve(AssetRegistry registry, SemanticRenderAssetResolver.RenderIntent intent) {
        return SemanticRenderAssetResolver.resolve(registry, intent);
    }

    static String auditSummary() {
        return "authority=" + VERSION
                + " registryFirst=true strictTypedFallback=true"
                + " waterFixtureBoundary=true toolBladeBoundary=true medicalScalpelBoundary=true equipmentKitBoundary=true";
    }

    private static String normalizeItem(String raw) {
        if (raw == null) return "";
        return normalize(ItemQuality.stripManufacturingIdentity(ItemQuality.stripQuality(raw)));
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        String text = raw.toLowerCase(Locale.ROOT)
                .replace('‑', '-')
                .replace('–', '-')
                .replace('—', '-')
                .replaceAll("[^a-z0-9+./ -]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return text;
    }

    private static boolean contains(String text, String... terms) {
        for (String term : terms) {
            if (term == null || term.isBlank()) continue;
            String normalizedTerm = normalize(term);
            int from = 0;
            while (from <= text.length() - normalizedTerm.length()) {
                int at = text.indexOf(normalizedTerm, from);
                if (at < 0) break;
                int end = at + normalizedTerm.length();
                boolean leftBoundary = at == 0 || !Character.isLetterOrDigit(text.charAt(at - 1));
                boolean rightBoundary = end == text.length() || !Character.isLetterOrDigit(text.charAt(end));
                if (leftBoundary && rightBoundary) return true;
                from = at + 1;
            }
        }
        return false;
    }
}