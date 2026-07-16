package mechanist;

import java.util.List;

/** Focused Phase 17.4 smoke for exact construction acquisition guidance. */
final class Milestone05BlueprintInfopediaBridgeSmoke {
    public static void main(String[] args) {
        int licensed = 0;
        int publicRecipes = 0;
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            require(recipe != null, "construction catalog must not contain null recipes");
            List<String> dossier =
                    ConstructionBlueprintInfopediaBridgeAuthority.dossierLines(recipe);
            requireContains(dossier, recipe.name, recipe.name + " dossier title");
            requireContains(dossier, "Player construction:", recipe.name + " player capability");
            requireContains(dossier, "Faction construction:", recipe.name + " faction capability");
            requireContains(dossier, "Blueprint unlock:", recipe.name + " unlock name");
            requireContains(dossier, "Issuing faction or market:", recipe.name + " issuing source");
            requireContains(dossier, "Ordinary seller or grantor:", recipe.name + " seller");
            requireContains(dossier, "Access and reputation:", recipe.name + " access gate");
            requireContains(dossier, "Acquisition pathways:", recipe.name + " acquisition paths");
            requireContains(dossier, "Requirements:", recipe.name + " build requirements");
            requireContains(dossier, "Materials:", recipe.name + " material requirements");
            requireContains(dossier, "Legality and attention:", recipe.name + " legal risk");
            requireContains(dossier, "Contract outcomes:", recipe.name + " quest bridge");
            requireContains(dossier, "reveal rewards record a lead without ownership",
                    recipe.name + " reveal ownership boundary");
            for (String line : dossier) {
                require(line != null && !line.isBlank(),
                        recipe.name + " dossier should not contain blank rows");
                require(!PlayerFacingText.containsLikelyLeak(line),
                        recipe.name + " dossier leaked implementation text: " + line);
            }

            if (!ConstructionBlueprintOwnershipAuthority.requiresLicensedBlueprint(recipe)) {
                publicRecipes++;
                continue;
            }
            licensed++;
            String itemName =
                    ConstructionBlueprintOwnershipAuthority.blueprintItemName(recipe);
            ItemDef definition = ItemCatalog.get(itemName);
            require(definition != null,
                    "licensed blueprint must be registered as an Infopedia-visible item: "
                            + itemName);
            requireContains(definition.description, "Infopedia construction dossier",
                    itemName + " catalog dossier");
            requireContains(definition.description, "Player construction: supported",
                    itemName + " player construction guidance");
            requireContains(definition.description, "Faction construction: supported",
                    itemName + " faction construction guidance");
            requireContains(definition.description, "Ordinary representative:",
                    itemName + " seller guidance");
            requireContains(definition.description, "Legal class:",
                    itemName + " legality guidance");
            requireContains(definition.source, "contract reward",
                    itemName + " contract source");
            requireContains(definition.source, "recovered-plan",
                    itemName + " recovery source");
            requireContains(definition.source, "theft",
                    itemName + " theft source");
            requireContains(definition.source, "counterfeit",
                    itemName + " counterfeit source");
            requireContains(definition.use, "Access:",
                    itemName + " access requirement");
            requireContains(definition.use, "Build requirements:",
                    itemName + " build requirement summary");
            requireContains(definition.use, "Materials:",
                    itemName + " material summary");
            requireContains(definition.use,
                    "a revealed lead does not grant ownership",
                    itemName + " reveal boundary");
            requireContains(definition.use, "suspicion or legality risk",
                    itemName + " risk guidance");
            requireContains(definition.use, itemName,
                    itemName + " exact Infopedia search guidance");
        }

        require(licensed > 0,
                "fixture requires licensed construction blueprints in the catalog");
        require(publicRecipes > 0,
                "fixture requires public recipes so the bridge covers both access classes");
        System.out.println("Milestone 05 blueprint Infopedia bridge smoke passed.");
    }

    private static void requireContains(List<String> lines, String expected,
                                        String label) {
        for (String line : lines) {
            if (line != null && line.contains(expected)) return;
        }
        throw new AssertionError("Expected " + label + " to contain '"
                + expected + "': " + lines);
    }

    private static void requireContains(String text, String expected,
                                        String label) {
        require(text != null && text.contains(expected),
                "Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05BlueprintInfopediaBridgeSmoke() { }
}
