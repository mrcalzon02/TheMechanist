package mechanist;

import java.util.List;
import java.util.Locale;

/** Resolves a deliberately equipped fabrication or repair tool for manual Craft. */
final class ProductionToolQualityAuthority {
    record ToolQuality(boolean active, String itemName, int tier, String quality, List<String> lines) { }

    private ProductionToolQualityAuthority() { }

    static ToolQuality evaluate(GamePanel game) {
        String left = game == null ? null : game.equippedLeftHandItem;
        String right = game == null ? null : game.equippedRightHandItem;
        String selected = betterTool(left, right);
        if (selected == null) {
            return new ToolQuality(false, "integrated machine tooling", QualityAuthorityApi.UNLIMITED_TIER, "open",
                    List.of("Tool quality: open; no fabrication or repair tool is equipped, so the machine's integrated tooling governs the operation."));
        }
        int tier = ItemQuality.tierIndex(selected);
        String quality = QualityAuthorityApi.qualityName(tier);
        return new ToolQuality(true, selected, tier, quality, List.of(
                "Tool quality cap: " + quality + " from equipped " + selected + ".",
                "Tool rule: only deliberately equipped fabrication or repair tools participate; unrelated carried items do not silently cap production."));
    }

    static boolean qualifies(String item) {
        ItemDef def = ItemCatalog.get(item);
        if (def == null) return false;
        String category = def.category == null ? "" : def.category.toLowerCase(Locale.ROOT);
        String use = def.use == null ? "" : def.use.toLowerCase(Locale.ROOT);
        if (!category.contains("tool")) return false;
        return containsAny(use, "fabrication", "repair", "workbench", "machine", "mechanic", "electrical");
    }

    private static String betterTool(String left, String right) {
        boolean leftTool = qualifies(left);
        boolean rightTool = qualifies(right);
        if (!leftTool) return rightTool ? right : null;
        if (!rightTool) return left;
        return ItemQuality.tierIndex(right) > ItemQuality.tierIndex(left) ? right : left;
    }

    private static boolean containsAny(String text, String... values) {
        for (String value : values) if (text.contains(value)) return true;
        return false;
    }
}
