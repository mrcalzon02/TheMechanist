package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Forecasts the quality floor of concrete named units consumed by manual crafting. */
final class ProductionMaterialQualityAuthority {
    record MaterialQuality(boolean active, boolean complete, int limitingTier, String limitingItem,
                           List<String> selectedUnits, List<String> lines) { }

    private ProductionMaterialQualityAuthority() { }

    static MaterialQuality evaluate(GamePanel game, CraftingRecipe recipe) {
        if (recipe == null || recipe.itemInputs.isEmpty()) {
            return new MaterialQuality(false, true, QualityAuthorityApi.UNLIMITED_TIER, "none", List.of(),
                    List.of("Material quality hook: open; this recipe consumes no named item units."));
        }
        ArrayList<String> selected = new ArrayList<>();
        int limitingTier = QualityAuthorityApi.UNLIMITED_TIER;
        String limitingItem = "none";
        boolean complete = true;
        for (Map.Entry<String, Integer> requirement : recipe.itemInputs.entrySet()) {
            ArrayList<String> matches = matchingUnits(game, requirement.getKey());
            int needed = Math.max(0, requirement.getValue());
            if (matches.size() < needed) complete = false;
            for (int i = 0; i < Math.min(needed, matches.size()); i++) {
                String unit = matches.get(i);
                selected.add(unit);
                int tier = ItemQuality.tierIndex(unit);
                if (tier < limitingTier) {
                    limitingTier = tier;
                    limitingItem = unit;
                }
            }
        }
        ArrayList<String> lines = new ArrayList<>();
        if (!complete) {
            lines.add("Material quality: unavailable until all named input units are present.");
        } else {
            lines.add("Material quality cap: " + QualityAuthorityApi.qualityName(limitingTier)
                    + " from " + limitingItem + ".");
            lines.add("Material selection: carried inventory first, then base storage; " + selected.size()
                    + " named unit(s) will be consumed.");
        }
        return new MaterialQuality(true, complete, limitingTier, limitingItem, List.copyOf(selected), List.copyOf(lines));
    }

    private static ArrayList<String> matchingUnits(GamePanel game, String wanted) {
        ArrayList<String> matches = new ArrayList<>();
        if (game == null) return matches;
        addMatches(matches, game.inventory, wanted);
        addMatches(matches, game.baseStorage, wanted);
        return matches;
    }

    private static void addMatches(List<String> matches, List<String> source, String wanted) {
        if (source == null) return;
        for (String item : source) if (ItemQuality.namesMatch(item, wanted)) matches.add(item);
    }
}
