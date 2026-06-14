package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ConstructionCategoryAuthority {
    static final String[] CATEGORIES = {
            "All", "Shelter and Storage", "Defense", "Machines and Utilities",
            "Commerce and Medical", "Logistics", "Laboratory"
    };

    private ConstructionCategoryAuthority() {}

    static String categoryName(int index) {
        return CATEGORIES[Math.max(0, Math.min(CATEGORIES.length - 1, index))];
    }

    static int nextCategory(int index, int delta) {
        return Math.floorMod(index + delta, CATEGORIES.length);
    }

    static ArrayList<BuildRecipe> filtered(List<BuildRecipe> recipes, int categoryIndex) {
        ArrayList<BuildRecipe> out = new ArrayList<>();
        if (recipes == null) return out;
        String selected = categoryName(categoryIndex);
        for (BuildRecipe recipe : recipes) {
            if (recipe != null && ("All".equals(selected) || selected.equals(categoryFor(recipe)))) out.add(recipe);
        }
        return out;
    }

    static String categoryFor(BuildRecipe recipe) {
        if (recipe == null) return "Machines and Utilities";
        String text = (recipe.name + " " + recipe.description).toLowerCase(Locale.ROOT);
        if (containsAny(text, "laboratory", "chemical", "medicae clean bench", "fume hood", "distillation",
                "reagent", "injector", "fungal", "censer kiln")) return "Laboratory";
        if (containsAny(text, "turret", "barricade", "defense", "defensive", "guard", "watch post",
                "razor wire", "reinforced door", "reinforced wall", "alarm trap", "shield relay")) return "Defense";
        if (containsAny(text, "logistics", "supply post", "carrying station")) return "Logistics";
        if (containsAny(text, "shop", "market", "clinic", "business", "vendor", "counter")) return "Commerce and Medical";
        if (containsAny(text, "storage", "crate", "cot", "decor", "furniture")) return "Shelter and Storage";
        return "Machines and Utilities";
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) if (text.contains(needle)) return true;
        return false;
    }
}
