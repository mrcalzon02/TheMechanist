package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Smoke for the Phase 18 construction blueprint parity audit surface. */
final class Milestone03BlueprintParityAuditSmoke {
    public static void main(String[] args) {
        List<String> audit = BlueprintConstructionAuthority.parityAuditLines();
        ArrayList<BuildRecipe> recipes = BuildRecipe.allBuildRecipes();
        int expectedCategories = ConstructionCategoryAuthority.CATEGORIES.length - 1;

        require(recipes.size() >= 30, "expected live build recipe catalog");
        requireContains(audit, "owner=BlueprintConstructionAuthority", "blueprint parity owner");
        requireContains(audit, "buildRecipeOwner=BuildRecipe", "build recipe owner");
        requireContains(audit, "categoryOwner=ConstructionCategoryAuthority", "category owner");
        requireContains(audit, "roomBlueprintOwner=RoomBlueprint", "room blueprint owner");
        requireContains(audit, "acquisitionOwner=future vendor or contract owner", "future acquisition owner");
        requireContains(audit, "catalogRecipes=" + recipes.size(), "catalog count");
        requireContains(audit, "playerFacingNames=" + recipes.size(), "player-facing names");
        requireContains(audit, "describedRecipes=" + recipes.size(), "description count");
        requireContains(audit, "categorizedRecipes=" + recipes.size(), "category coverage");
        requireContains(audit, "categoryCount=" + expectedCategories, "category count");
        requireContains(audit, "sampleRoomBlueprint=Hollow Box Test Room", "sample room mapping");
        requireContains(audit, "role=room", "room role");
        requireContains(audit, "theme=generic-structure", "room theme");
        requireContains(audit, "validRoomMapping=true", "valid room mapping");
        requireContains(audit, "buildRecipeAssetMappings=" + recipes.size(), "asset mapping count");
        requireContains(audit, "factionRestrictedBlueprints=", "faction restriction count");
        requireContains(audit, "knowledgeGatedBlueprints=", "knowledge gate count");
        requireContains(audit, "workbenchGatedBlueprints=", "workbench gate count");
        requireContains(audit, "explanations come from faction, knowledge, workbench, quality, components, and description fields", "access explanation");
        requireContains(audit, "faction vendor stock categories, reputation gates, permits, acquisition paths, faction construction capability, heat, and suspicion impacts are future owners", "future vendor boundary");
        requireContains(audit, "non-acquirable, player-only, and faction-only exceptions require explicit future data owners", "parity exception boundary");
        requireContains(audit, "Milestone03BlueprintParityAuditSmoke", "guard reference");

        for (int i = 1; i < ConstructionCategoryAuthority.CATEGORIES.length; i++) {
            require(!ConstructionCategoryAuthority.filtered(recipes, i).isEmpty(),
                    "expected non-empty construction category " + ConstructionCategoryAuthority.categoryName(i));
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint parity audit leaked implementation text: " + line);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03BlueprintParityAuditSmoke() { }
}
