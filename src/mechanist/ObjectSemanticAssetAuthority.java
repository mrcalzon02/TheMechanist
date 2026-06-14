package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

import javax.swing.ImageIcon;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Stage 6 bridge for placeable objects, machines, fixtures, construction buttons,
 * traps, lights, and editor palette items.
 *
 * This class is deliberately a migration authority: it routes player-facing object
 * previews through the Semantic Asset Registry first while preserving legacy tile
 * aliases as fallback.  Later schema work can replace the name/type classifiers
 * with durable assetId fields on every recipe, fixture, trap, light, and editor
 * record.
 */
final class ObjectSemanticAssetAuthority {
    static final String VERSION = "0.9.10kc-active-registry";

    private static final Map<String, String> EXACT = new LinkedHashMap<>();

    static {
        // Core construction recipes and base objects.
        map("storage crate", "BLD-0001");
        map("scrap workbench", "BLD-0002");
        map("workbench", "BLD-0002");
        map("barricade", "BLD-0003");
        map("sleeping cot", "OBJ-CT01");
        map("cot", "OBJ-CT01");
        map("water barrel", "OBJ-WB01");
        map("alarm trap", "BLD-0004");
        map("shop counter", "BLD-0005");
        map("licensed shop counter", "BLD-0005");
        map("clinic stall", "FTR-0007");
        map("watch post", "FTR-0001");
        map("guard barracks", "DOM-0102");
        map("reinforced door", "DOR-0005");
        map("power turret", "BLD-0009");
        map("shield relay", "MAC-0103");
        map("decor", "DOM-0503");
        map("business addon", "BLD-0005");
        map("carrying station", "SHF-0401");
        map("supply post", "SHF-0205");
        map("logistics center", "SHF-0102");
        map("sandbag line", "BLD-0006");
        map("razor wire coil", "BLD-0007");
        map("reinforced wall panel", "WAL-0303");
        map("arbites reinforced door", "DOR-0005");
        map("security sensor mast", "FTR-0005");
        map("light stub turret", "BLD-0008");
        map("heavy stub turret", "BLD-0010");
        map("arbites suppression turret", "BLD-0011");
        map("gilded sentry turret", "BLD-0012");
        map("precinct defensive fixture set", "FTR-0006");

        // Emergency machines and laboratory/manufacturing fixtures.
        map("atmospheric condenser", "MACH-C01");
        map("atmos condenser", "MACH-C01");
        map("micro forge", "MACH-F01");
        map("emergency forge", "MACH-F01");
        map("micro lab", "BLD-0013");
        map("crude chem bench", "BLD-0014");
        map("reagent bench", "MAC-0302");
        map("distillation column", "MAC-0402");
        map("sterile medicae clean bench", "FTR-0007");
        map("fume hood", "MAC-0404");
        map("injector filling station", "MAC-0305");
        map("fungal grow tray bank", "MAC-0205");
        map("ritual censer kiln", "MAC-0103");

        // Editor palette items and common room fixtures.
        map("sealed ration", "ITEM-G01");
        map("stubcarbine", "WP3-0105");
        map("cogitator core", "FTR-0008");
        map("repair kit", "ITEM-G01");
        map("water canister", "DOM-0205");
        map("scrap bundle", "ITEM-G01");
        map("civilian spawn", "PORT-A01");
        map("guard patrol node", "FTR-0005");
        map("scavenger camp node", "SHF-0503");
        map("servitor route", "MAC-0101");
        map("trader counter", "FTR-0011");
        map("hostile ambush", "FTR-0006");
        map("sink", "DOM-0401");
        map("dresser", "DOM-0303");
        map("cabinet", "DOM-0302");
        map("generator", "MAC-0105");
        map("pillar", "WAL-0402");
        map("altar", "MAC-0103");
        map("terminal", "FTR-0008");
        map("crate", "BLD-0001");
        map("candle rack", "MAC-0103");
        map("light fixture", "FTR-0003");
        map("light switch", "FTR-0004");
        map("motion sensor", "FTR-0005");
        map("planted explosive", "FTR-0006");
    }

    private ObjectSemanticAssetAuthority() {}

    static String assetIdForBuildRecipe(BuildRecipe recipe) {
        return assetIdForName(recipe == null ? null : recipe.name);
    }

    static Optional<String> runtimeAssetIdForBuildRecipe(BuildRecipe recipe) {
        String name = recipe == null ? "" : recipe.name;
        return runtimeAssetIdForName(name);
    }

    static Optional<AssetMetadata> metadataForBuildRecipe(BuildRecipe recipe) {
        return runtimeAssetIdForBuildRecipe(recipe).flatMap(AssetManager::metadata);
    }

    static String assetIdForBaseObject(BaseObject object) {
        return assetIdForName(object == null ? null : object.name);
    }

    static Optional<String> runtimeAssetIdForBaseObject(BaseObject object) {
        String name = object == null ? "" : object.name;
        return SemanticAssetHintResolver.resolve(assetIdForBaseObject(object), name, java.util.Set.of(
                AssetType.OBJECT, AssetType.FIXTURE, AssetType.MACHINE,
                AssetType.ITEM_ICON, AssetType.WALL_TILE));
    }

    static BufferedImage imageForBaseObject(BaseObject object) {
        String id = runtimeAssetIdForBaseObject(object).orElse(assetIdForBaseObject(object));
        return imageForAssetId(id);
    }

    static String assetIdForMapObject(MapObjectState object) {
        if (object == null) return "ITEM-G01";
        String label = object.label == null ? "" : object.label;
        String type = object.type == null ? "" : object.type;
        String stock = object.stockState == null ? "" : object.stockState;
        String id = assetIdForName(label);
        if (!"ITEM-G01".equals(id)) return id;
        id = assetIdForName(type);
        if (!"ITEM-G01".equals(id)) return id;
        return assetIdForName(stock);
    }

    static Optional<String> runtimeAssetIdForMapObject(MapObjectState object) {
        if (object == null) return Optional.empty();
        String semantic = (object.label == null ? "" : object.label) + " "
                + (object.type == null ? "" : object.type) + " "
                + (object.stockState == null ? "" : object.stockState);
        return SemanticAssetHintResolver.resolve(assetIdForMapObject(object), semantic, java.util.Set.of(
                AssetType.OBJECT, AssetType.FIXTURE, AssetType.MACHINE,
                AssetType.ITEM_ICON, AssetType.WEAPON_ICON, AssetType.ARMOR_ICON));
    }

    static BufferedImage imageForMapObject(MapObjectState object) {
        String id = runtimeAssetIdForMapObject(object).orElse(assetIdForMapObject(object));
        return imageForAssetId(id);
    }

    static String assetIdForEditorPalette(String category, String item) {
        return runtimeAssetIdForEditorPalette(category, item)
                .orElseGet(() -> assetHintForEditorPalette(category, item));
    }

    static Optional<String> runtimeAssetIdForEditorPalette(String category, String item) {
        String cat = normalize(category);
        if (cat.contains("floor")) return TileSemanticAssetAuthority.assetIdForAlias("floor_bare_underhive");
        if (cat.contains("wall")) return TileSemanticAssetAuthority.assetIdForAlias("wall_bulkhead");
        String semantic = cat + " " + normalize(item);
        return SemanticAssetHintResolver.resolve(assetHintForEditorPalette(category, item), semantic,
                java.util.Set.of(AssetType.OBJECT, AssetType.FIXTURE, AssetType.MACHINE,
                        AssetType.ITEM_ICON, AssetType.WEAPON_ICON, AssetType.ARMOR_ICON));
    }

    private static String assetHintForEditorPalette(String category, String item) {
        String direct = assetIdForName(item);
        if (!"ITEM-G01".equals(direct)) return direct;
        String cat = normalize(category);
        if (cat.contains("floor")) return "FLR-0101";
        if (cat.contains("wall")) return "WALL-A01";
        return direct;
    }

    static String assetIdForLight(ZoneLightSourceRecord light) {
        return runtimeAssetIdForLight(light).orElseGet(() -> assetHintForLight(light));
    }

    static Optional<String> runtimeAssetIdForLight(ZoneLightSourceRecord light) {
        String semantic = light == null ? "light fixture" : normalize(
                light.profile + " " + light.colorName + " " + light.groupId + " light fixture");
        return SemanticAssetHintResolver.resolve(assetHintForLight(light), semantic,
                java.util.Set.of(AssetType.FIXTURE, AssetType.OBJECT, AssetType.MACHINE));
    }

    private static String assetHintForLight(ZoneLightSourceRecord light) {
        if (light == null) return "FTR-0003";
        String profile = normalize(light.profile + " " + light.colorName + " " + light.groupId);
        if (profile.contains("switch")) return "FTR-0004";
        if (profile.contains("sensor")) return "FTR-0005";
        return "FTR-0003";
    }

    static String assetIdForTrap(TrapRecord trap) {
        return runtimeAssetIdForTrap(trap).orElseGet(() -> assetHintForTrap(trap));
    }

    static Optional<String> runtimeAssetIdForTrap(TrapRecord trap) {
        String semantic = trap == null ? "alarm trap" : normalize(
                trap.type + " " + trap.label + " " + trap.effect + " trap");
        return SemanticAssetHintResolver.resolve(assetHintForTrap(trap), semantic,
                java.util.Set.of(AssetType.FIXTURE, AssetType.OBJECT, AssetType.MACHINE, AssetType.ITEM_ICON));
    }

    private static String assetHintForTrap(TrapRecord trap) {
        if (trap == null) return "BLD-0004";
        String text = normalize(trap.type + " " + trap.label + " " + trap.effect);
        if (text.contains("wire") || text.contains("razor")) return "BLD-0007";
        if (text.contains("explosive")) return "FTR-0006";
        if (text.contains("sensor")) return "FTR-0005";
        return "BLD-0004";
    }

    static BufferedImage imageForAssetId(String assetId) {
        if (assetId == null || assetId.isBlank()) return AssetManager.missingAssetImage(AssetType.OBJECT);
        AssetType type = AssetManager.metadata(assetId).map(AssetMetadata::type).orElse(AssetType.OBJECT);
        ImageIcon icon = AssetManager.getAsset(assetId, type);
        if (icon == null) return AssetManager.missingAssetImage(type);
        Image image = icon.getImage();
        if (image instanceof BufferedImage buffered) return buffered;
        int w = Math.max(1, icon.getIconWidth());
        int h = Math.max(1, icon.getIconHeight());
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(image, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    static Optional<String> runtimeAssetIdForName(String name) {
        return SemanticAssetHintResolver.resolve(assetIdForName(name), normalize(name), java.util.Set.of(
                AssetType.OBJECT, AssetType.FIXTURE, AssetType.MACHINE,
                AssetType.ITEM_ICON, AssetType.WEAPON_ICON, AssetType.ARMOR_ICON, AssetType.WALL_TILE));
    }

    static String semanticSummaryForName(String name) {
        String hint = assetIdForName(name);
        Optional<AssetMetadata> meta = runtimeAssetIdForName(name).flatMap(AssetManager::metadata);
        if (meta.isEmpty()) return "Semantic object asset hint: " + hint + " (no compatible active-registry asset).";
        AssetMetadata m = meta.get();
        return "Semantic object asset: " + m.id() + " / " + m.type().displayName() + " / " + m.name()
                + " / authoredHint=" + hint + ".";
    }

    static String auditSummary() {
        return "objectSemanticAssetAuthority version=" + VERSION + " exactMappings=" + EXACT.size() +
                " domains=construction+base-objects+map-fixtures+traps+lights+editor-palettes activeRegistryValidated=true legacyHintsRetained=true typedMissing=true";
    }

    static Map<String, String> auditExactMappings() {
        return Map.copyOf(EXACT);
    }

    static String assetIdForName(String rawName) {
        String name = normalize(rawName);
        if (name.isBlank()) return "ITEM-G01";
        String exact = EXACT.get(name);
        if (exact != null) return exact;
        if (containsAny(name, "water barrel", "water storage")) return "OBJ-WB01";
        if (containsAny(name, "water dispenser")) return "OBJ-WD01";
        if (containsAny(name, "cot", "bunk", "bed")) return name.contains("guard") ? "DOM-0102" : "OBJ-CT01";
        if (containsAny(name, "shelf", "supply", "counter", "shop", "trade", "vendor")) return "SHF-0101";
        if (containsAny(name, "crate", "storage", "locker", "cabinet", "box")) return "BLD-0001";
        if (containsAny(name, "turret", "gun emplacement")) return "BLD-0009";
        if (containsAny(name, "alarm", "trap")) return "BLD-0004";
        if (containsAny(name, "wire", "razor")) return "BLD-0007";
        if (containsAny(name, "sandbag", "barricade")) return "BLD-0006";
        if (containsAny(name, "door", "hatch")) return "DOR-0005";
        if (containsAny(name, "light", "lamp", "torch")) return "FTR-0003";
        if (containsAny(name, "switch")) return "FTR-0004";
        if (containsAny(name, "sensor", "mast")) return "FTR-0005";
        if (containsAny(name, "explosive", "charge", "bomb")) return "FTR-0006";
        if (containsAny(name, "clinic", "medicae", "sterile")) return "FTR-0007";
        if (containsAny(name, "terminal", "cogitator", "data")) return "FTR-0008";
        if (containsAny(name, "workbench", "bench")) return "BLD-0002";
        if (containsAny(name, "condenser")) return "MACH-C01";
        if (containsAny(name, "assembler")) return "MACH-A01";
        if (containsAny(name, "boiler")) return "MACH-B01";
        if (containsAny(name, "forge", "smelter", "kiln")) return "MACH-F01";
        if (containsAny(name, "lab", "chem", "reagent", "injector", "hood", "distillation")) return "BLD-0014";
        if (containsAny(name, "generator", "relay", "power")) return "MAC-0105";
        if (containsAny(name, "sink")) return "DOM-0401";
        if (containsAny(name, "stove")) return "DOM-0402";
        if (containsAny(name, "table")) return "DOM-0501";
        if (containsAny(name, "pillar", "column")) return "WAL-0402";
        if (containsAny(name, "altar", "shrine", "candle")) return "MAC-0103";
        return "ITEM-G01";
    }

    private static void map(String token, String assetId) {
        EXACT.put(normalize(token), assetId);
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

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (needle != null && !needle.isBlank() && text.contains(needle.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
