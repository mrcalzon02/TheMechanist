package mechanist;

import mechanist.assets.AssetRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Focused Phase 17.4 smoke for the live construction Infopedia path. */
final class Milestone05BlueprintInfopediaBridgeSmoke {
    private static final String CONSTRUCTION_PREFIX = "CONSTRUCTION - ";

    public static void main(String[] args) {
        AssetRegistry registry = AssetRegistry.empty();
        List<String> allRows = SemanticAssetInfopediaAuthority.entries(registry, null, "");
        int licensed = 0;
        int publicRecipes = 0;

        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            require(recipe != null, "construction catalog must not contain null recipes");
            String row = CONSTRUCTION_PREFIX + recipe.name + " ["
                    + ConstructionCategoryAuthority.categoryFor(recipe) + "]";
            require(allRows.contains(row),
                    recipe.name + " must be a first-class row in the All Infopedia list: " + allRows);
            require(SemanticAssetInfopediaAuthority.entries(registry, null, recipe.name).contains(row),
                    recipe.name + " must be searchable by exact recipe name");

            List<String> dossier =
                    ConstructionBlueprintInfopediaBridgeAuthority.dossierLines(recipe);
            List<String> details = SemanticAssetInfopediaAuthority.detailLines(
                    registry, row, null, recipe.name);
            require(details.equals(dossier),
                    recipe.name + " Infopedia row must parse to its canonical construction dossier\n"
                            + "expected=" + dossier + "\nactual=" + details);
            requireContains(details, recipe.name, recipe.name + " dossier title");
            requireCapabilityLine(details, "Player construction:",
                    recipe.name + " player capability");
            requireCapabilityLine(details, "Faction construction:",
                    recipe.name + " faction capability");

            BlueprintAcquisitionPathAuthority.AcquisitionPath path =
                    BlueprintAcquisitionPathAuthority.pathFor(recipe);
            require(details.contains("Plan source or catalog: " + path.sourceFaction() + "."),
                    recipe.name + " must show its exact issuing source or public catalog");
            require(details.contains("Ordinary acquisition channel: " + path.representativeType() + "."),
                    recipe.name + " must show its exact acquisition channel");
            require(details.contains("Access and reputation: " + path.accessLabel() + "."),
                    recipe.name + " must show its exact access and reputation gate");
            require(details.contains("Acquisition pathways: " + path.acquisitionPath() + "."),
                    recipe.name + " must show its exact acquisition pathways");
            require(details.contains("Materials: " + materialSummary(recipe) + "."),
                    recipe.name + " must show its exact construction materials");
            require(details.contains(ConstructionReadabilityAuthority.effortPreview(recipe)),
                    recipe.name + " must show its exact labor effort preview");
            requireContains(details, ConstructionReadabilityAuthority.attentionPreview(recipe),
                    recipe.name + " exact expansion-attention preview");
            assertPlayerFacingConstructionDetails(details, recipe.name);

            List<String> related = SemanticAssetInfopediaAuthority.relatedRowsForEntry(
                    registry, row, null);
            requireContains(related, "MECHANIC - Construction Blueprints [Construction]",
                    recipe.name + " related construction rules");

            if (!ConstructionBlueprintOwnershipAuthority.requiresLicensedBlueprint(recipe)) {
                publicRecipes++;
                require(details.contains(
                                "Blueprint access: PUBLIC - no licensed faction folio is required."),
                        recipe.name + " public recipe must name its no-folio access");
                requireNotContains(details,
                        ConstructionBlueprintOwnershipAuthority.blueprintItemName(recipe),
                        recipe.name + " public dossier must not invent a licensed folio");
                continue;
            }

            licensed++;
            String itemName = ConstructionBlueprintOwnershipAuthority.blueprintItemName(recipe);
            require(details.contains("Blueprint unlock: " + itemName + "."),
                    recipe.name + " licensed dossier must name its exact persistent unlock");
            requireContains(related, "ITEM - " + itemName + " [knowledge blueprint construction]",
                    recipe.name + " related licensed folio");

            ItemDef definition = ItemCatalog.get(itemName);
            require(definition != null,
                    "licensed blueprint must remain registered as an Infopedia-visible item: "
                            + itemName);
            requireContains(definition.description, "Infopedia construction dossier",
                    itemName + " catalog dossier");
            ConstructionParityInspectionAuthority.RecipeInspection parity =
                    ConstructionParityInspectionAuthority.inspect(recipe);
            requireContains(definition.description,
                    "Player construction: " + capabilityLabel(parity.playerCapability()),
                    itemName + " exact player construction guidance");
            requireContains(definition.description,
                    "Faction construction: " + capabilityLabel(parity.factionCapability()),
                    itemName + " exact faction construction guidance");
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

        assertStorageCrateDetails(registry);
        assertMicroForgeDetails(registry);
        require(licensed > 0,
                "fixture requires licensed construction blueprints in the catalog");
        require(publicRecipes > 0,
                "fixture requires public recipes so the bridge covers both access classes");
        System.out.println("Milestone 05 blueprint Infopedia bridge smoke passed.");
    }

    private static void assertStorageCrateDetails(AssetRegistry registry) {
        BuildRecipe recipe = recipe("Storage Crate");
        List<String> details = details(registry, recipe);
        require(details.contains(
                        "Blueprint access: PUBLIC - no licensed faction folio is required."),
                "Storage Crate must be explicitly PUBLIC without a licensed folio");
        requireNotContains(details, "Storage Crate licensed blueprint",
                "Storage Crate must not advertise a nonexistent licensed folio");
        requireContains(details, "available by default in the public construction catalog",
                "Storage Crate must name automatic public-plan availability");
        requireNotContains(details, "receive as contract reward",
                "Storage Crate must not invent an ownership reward for an auto-owned public plan");
    }

    private static void assertMicroForgeDetails(AssetRegistry registry) {
        BuildRecipe recipe = recipe("EMM Micro Forge");
        List<String> details = details(registry, recipe);
        require(details.contains(
                        "Blueprint unlock: EMM Micro Forge licensed blueprint."),
                "Micro Forge must name its licensed construction unlock");
        requireLineContainsAll(details, "Micro Forge conditional player capability",
                "Player construction:", "conditional");
        requireLineContainsAll(details, "Micro Forge supported faction capability",
                "Faction construction:", "supported");
        require(details.contains("Plan source or catalog: Mechanist Collegia."),
                "Micro Forge must name the Mechanist Collegia issuer");
        require(details.contains(
                        "Ordinary acquisition channel: Mechanist Collegia vendor."),
                "Micro Forge must name the exact ordinary seller");
        require(details.contains(
                        "Access and reputation: faction-approved blueprint for Mechanist Collegia, forge license-bound, quality Common, knowledge gate: Scrap-Forging Doctrine, workbench required."),
                "Micro Forge must show its exact standing, license, knowledge, and workbench access");
        require(details.contains(
                        "Acquisition pathways: earn standing, buy from Mechanist Collegia vendor after license check, receive as contract reward, or salvage/research a comparable plan."),
                "Micro Forge must show its exact supported acquisition pathways");
        require(details.contains(
                        "Materials: 4 construction supplies, 3 machine parts, 1 Gear train, 1 Bearing set, 1 Motor coil pack, 1 Ceramic insulator blank, 1 Heat sink."),
                "Micro Forge must show its exact itemized construction materials");
        require(details.contains(ConstructionReadabilityAuthority.effortPreview(recipe)),
                "Micro Forge must show its exact ten-turn labor effort");
        requireContains(details, ConstructionReadabilityAuthority.attentionPreview(recipe),
                "Micro Forge exact expansion-attention preview");
    }

    private static List<String> details(AssetRegistry registry, BuildRecipe recipe) {
        String row = CONSTRUCTION_PREFIX + recipe.name + " ["
                + ConstructionCategoryAuthority.categoryFor(recipe) + "]";
        return SemanticAssetInfopediaAuthority.detailLines(registry, row, null, recipe.name);
    }

    private static BuildRecipe recipe(String name) {
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            if (recipe != null && recipe.name.equalsIgnoreCase(name)) return recipe;
        }
        throw new AssertionError("Missing build recipe: " + name);
    }

    private static String materialSummary(BuildRecipe recipe) {
        ArrayList<String> parts = new ArrayList<>();
        if (recipe.supplyCost > 0) parts.add(recipe.supplyCost + " construction supplies");
        if (recipe.partCost > 0) parts.add(recipe.partCost + " machine parts");
        for (Map.Entry<String, Integer> entry : recipe.componentCosts.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()
                    || entry.getValue() == null || entry.getValue() <= 0) continue;
            parts.add(entry.getValue() + " " + entry.getKey());
        }
        return parts.isEmpty() ? "no material cost registered" : String.join(", ", parts);
    }

    private static void assertPlayerFacingConstructionDetails(List<String> lines, String label) {
        for (String line : lines) {
            require(line != null && !line.isBlank(),
                    label + " dossier should not contain blank rows");
            require(!PlayerFacingText.containsLikelyLeak(line),
                    label + " dossier leaked implementation text: " + line);
        }
        requireNotContains(lines, "Common value:",
                label + " must not fall through to generic item pricing prose");
        requireNotContains(lines, "Market and provenance:",
                label + " must not fall through to generic item provenance prose");
        requireNotContains(lines, "Typical access:",
                label + " must not receive a contradictory inferred access class");
        requireNotContains(lines, "Common sellers:",
                label + " must not receive contradictory generic sellers");
        requireNotContains(lines, "Availability:",
                label + " must not receive generic item availability prose");
        requireNotContains(lines, "A specific unit can also record local production",
                label + " is a plan, not a generic physical item unit");
        requireNotContains(lines, "general traders or faction specialists",
                label + " must use its exact blueprint seller");
        requireNotContains(lines, "ordinary legal stock",
                label + " must use its exact blueprint access class");
    }

    private static void requireLineContainsAll(List<String> lines, String label,
                                               String... expected) {
        for (String line : lines) {
            if (line == null) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            boolean matches = true;
            for (String value : expected) {
                if (value == null || !lower.contains(value.toLowerCase(Locale.ROOT))) {
                    matches = false;
                    break;
                }
            }
            if (matches) return;
        }
        throw new AssertionError("Expected " + label + " to contain all "
                + List.of(expected) + ": " + lines);
    }

    private static void requireCapabilityLine(List<String> lines, String prefix,
                                              String label) {
        for (String line : lines) {
            if (line == null || !line.startsWith(prefix)) continue;
            String lower = line.toLowerCase(Locale.ROOT);
            require(lower.contains("supported") || lower.contains("conditional"),
                    label + " must be explicitly supported, conditional, or not supported: " + line);
            return;
        }
        throw new AssertionError("Expected " + label + " in " + lines);
    }

    private static String capabilityLabel(
            ConstructionParityInspectionAuthority.Capability capability) {
        if (capability == null) return "not supported";
        return switch (capability) {
            case SUPPORTED -> "supported";
            case CONDITIONAL -> "conditional";
            case NOT_SUPPORTED -> "not supported";
        };
    }

    private static void requireContains(List<String> lines, String expected,
                                        String label) {
        for (String line : lines) {
            if (line != null && line.contains(expected)) return;
        }
        throw new AssertionError("Expected " + label + " to contain '"
                + expected + "': " + lines);
    }

    private static void requireNotContains(List<String> lines, String forbidden,
                                           String label) {
        for (String line : lines) {
            if (line != null && line.contains(forbidden)) {
                throw new AssertionError("Expected " + label + " to omit '"
                        + forbidden + "': " + lines);
            }
        }
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
