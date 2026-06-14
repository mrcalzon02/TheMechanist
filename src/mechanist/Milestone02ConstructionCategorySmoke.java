package mechanist;

import java.util.ArrayList;
import java.util.HashSet;

final class Milestone02ConstructionCategorySmoke {
    public static void main(String[] args) {
        ArrayList<BuildRecipe> all = BuildRecipe.allBuildRecipes();
        if (all.size() < 30) throw new AssertionError("Expected the live blueprint catalog, got " + all.size());

        HashSet<BuildRecipe> categorized = new HashSet<>();
        for (int i = 1; i < ConstructionCategoryAuthority.CATEGORIES.length; i++) {
            ArrayList<BuildRecipe> group = ConstructionCategoryAuthority.filtered(all, i);
            if (group.isEmpty()) throw new AssertionError("Empty construction category: " + ConstructionCategoryAuthority.categoryName(i));
            categorized.addAll(group);
        }
        if (categorized.size() != all.size()) {
            throw new AssertionError("Not every blueprint was categorized: " + categorized.size() + "/" + all.size());
        }
        requireCategory(BuildRecipe.storage(), "Shelter and Storage");
        requireCategory(BuildRecipe.lightStubTurret(), "Defense");
        requireCategory(BuildRecipe.microForge(), "Machines and Utilities");
        requireCategory(BuildRecipe.clinicStall(), "Commerce and Medical");
        requireCategory(BuildRecipe.logisticsCenter(), "Logistics");
        requireCategory(BuildRecipe.fumeHood(), "Laboratory");
        if (ConstructionCategoryAuthority.nextCategory(0, -1) != ConstructionCategoryAuthority.CATEGORIES.length - 1) {
            throw new AssertionError("Construction category reverse cycling did not wrap.");
        }
    }

    private static void requireCategory(BuildRecipe recipe, String expected) {
        String actual = ConstructionCategoryAuthority.categoryFor(recipe);
        if (!expected.equals(actual)) throw new AssertionError(recipe.name + " categorized as " + actual + ", expected " + expected);
    }

    private Milestone02ConstructionCategorySmoke() {}
}
