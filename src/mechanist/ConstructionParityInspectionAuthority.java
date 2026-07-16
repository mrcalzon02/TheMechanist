package mechanist;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Phase 18 editor/audit surface for construction parity, acquisition, vendor,
 * legality, and capability data. It reports gaps; it does not invent hidden
 * exceptions to make the audit look green.
 */
final class ConstructionParityInspectionAuthority {
    enum Capability {
        SUPPORTED,
        CONDITIONAL,
        NOT_SUPPORTED
    }

    record RecipeInspection(String recipeName, String category,
                            Capability playerCapability,
                            Capability factionCapability,
                            String blueprintName, boolean blueprintMappingValid,
                            String issuingFaction, String vendorCategory,
                            String acquisitionPath, String accessGate,
                            String legalClass, String materialSummary,
                            String workforceSummary, String exceptionClass,
                            String exceptionReason) {
        boolean valid() {
            return recipeName != null && !recipeName.isBlank()
                    && category != null && !category.isBlank()
                    && blueprintName != null && !blueprintName.isBlank()
                    && blueprintMappingValid
                    && acquisitionPath != null && !acquisitionPath.isBlank()
                    && accessGate != null && !accessGate.isBlank()
                    && legalClass != null && !legalClass.isBlank()
                    && materialSummary != null && !materialSummary.isBlank()
                    && workforceSummary != null && !workforceSummary.isBlank()
                    && exceptionClass != null && !exceptionClass.isBlank()
                    && exceptionReason != null && !exceptionReason.isBlank();
        }

        String editorRow() {
            return recipeName + " | " + category
                    + " | player=" + capabilityLabel(playerCapability)
                    + " | factions=" + capabilityLabel(factionCapability)
                    + " | source=" + issuingFaction
                    + " | vendor=" + vendorCategory
                    + " | access=" + accessGate
                    + " | legal=" + legalClass
                    + " | parity=" + exceptionClass + ": " + exceptionReason;
        }
    }

    private ConstructionParityInspectionAuthority() { }

    static List<RecipeInspection> inspectAll() {
        ArrayList<RecipeInspection> out = new ArrayList<>();
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            if (recipe != null) out.add(inspect(recipe));
        }
        return List.copyOf(out);
    }

    static RecipeInspection inspect(BuildRecipe recipe) {
        if (recipe == null) {
            return new RecipeInspection("Unknown construction recipe", "Uncategorized",
                    Capability.NOT_SUPPORTED, Capability.NOT_SUPPORTED,
                    "No blueprint mapping", false, "No issuing source",
                    "No vendor category", "No acquisition path",
                    "No access gate", "undefined", "No materials registered",
                    "No workforce path", "INVALID", "Null recipe definition");
        }

        boolean licensed =
                ConstructionBlueprintOwnershipAuthority.requiresLicensedBlueprint(recipe);
        BlueprintAcquisitionPathAuthority.AcquisitionPath path =
                BlueprintAcquisitionPathAuthority.pathFor(recipe);
        String blueprintName = licensed
                ? ConstructionBlueprintOwnershipAuthority.blueprintItemName(recipe)
                : recipe.name + " public construction plan";
        boolean mappingValid = !ConstructionBlueprintOwnershipAuthority
                .blueprintId(recipe).isBlank();
        Faction issuer = FactionInventoryStockAuthority.normalizeFaction(
                recipe.requiredFaction == null ? Faction.NONE : recipe.requiredFaction);
        Capability player = playerCapability(recipe, licensed, path);
        Capability faction = factionCapability(recipe, issuer);
        String exceptionClass = exceptionClass(player, faction);
        String exceptionReason = exceptionReason(recipe, player, faction, licensed);
        String vendor = licensed
                ? ConstructionBlueprintOwnershipAuthority.vendorCategoryFor(recipe).id
                : "public construction market";
        String issuing = issuer == Faction.NONE
                ? "public construction market" : issuer.label;
        return new RecipeInspection(recipe.name,
                ConstructionCategoryAuthority.categoryFor(recipe),
                player, faction, blueprintName, mappingValid, issuing, vendor,
                path.acquisitionPath(), path.accessLabel(), path.legalLabel(),
                materialSummary(recipe), workforceSummary(recipe, faction),
                exceptionClass, exceptionReason);
    }

    static List<String> editorRows(String filter) {
        String q = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        ArrayList<String> rows = new ArrayList<>();
        for (RecipeInspection inspection : inspectAll()) {
            String row = inspection.editorRow();
            if (q.isBlank() || row.toLowerCase(Locale.ROOT).contains(q)) rows.add(row);
        }
        if (rows.isEmpty()) rows.add("No construction parity entries match the active filter.");
        return List.copyOf(rows);
    }

    static List<String> auditLines() {
        List<RecipeInspection> inspections = inspectAll();
        int valid = 0;
        int symmetric = 0;
        int playerOnly = 0;
        int factionOnly = 0;
        int conditional = 0;
        int invalidMappings = 0;
        int restricted = 0;
        int contractPaths = 0;
        int salvagePaths = 0;
        EnumMap<FactionCriticalVendorPlacementAuthority.Category, Integer> vendorCounts =
                new EnumMap<>(FactionCriticalVendorPlacementAuthority.Category.class);
        LinkedHashMap<String, Integer> legalCounts = new LinkedHashMap<>();
        for (RecipeInspection inspection : inspections) {
            if (inspection.valid()) valid++;
            if ("SYMMETRIC".equals(inspection.exceptionClass())) symmetric++;
            else if ("PLAYER_ONLY".equals(inspection.exceptionClass())) playerOnly++;
            else if ("FACTION_ONLY".equals(inspection.exceptionClass())) factionOnly++;
            else conditional++;
            if (!inspection.blueprintMappingValid()) invalidMappings++;
            if (!"public civilian".equalsIgnoreCase(inspection.legalClass())) restricted++;
            String acquisition = inspection.acquisitionPath().toLowerCase(Locale.ROOT);
            if (acquisition.contains("contract reward")) contractPaths++;
            if (acquisition.contains("salvage") || acquisition.contains("recover")) salvagePaths++;
            legalCounts.merge(inspection.legalClass(), 1, Integer::sum);
            for (FactionCriticalVendorPlacementAuthority.Category category
                    : FactionCriticalVendorPlacementAuthority.Category.values()) {
                if (category.id.equals(inspection.vendorCategory())) {
                    vendorCounts.merge(category, 1, Integer::sum);
                }
            }
        }
        return List.of(
                "Construction parity inspection: recipes=" + inspections.size()
                        + ", valid=" + valid + ", invalidMappings=" + invalidMappings + ".",
                "Capability parity: symmetric=" + symmetric
                        + ", playerOnly=" + playerOnly
                        + ", factionOnly=" + factionOnly
                        + ", conditionalOrUnsupported=" + conditional + ".",
                "Acquisition coverage: contractRewardPaths=" + contractPaths
                        + ", salvageOrRecoveryPaths=" + salvagePaths
                        + ", restrictedOrControlled=" + restricted + ".",
                "Vendor category coverage: " + vendorCounts + ".",
                "Legal/access coverage: " + legalCounts + ".",
                "Live owners: recipe catalog=BuildRecipe; categories=ConstructionCategoryAuthority; ownership and issuing factions=ConstructionBlueprintOwnershipAuthority; acquisition paths=BlueprintAcquisitionPathAuthority; contract rewards=ConstructionBlueprintContractRewardAuthority; faction construction and maintenance=FactionPhysicalConstructionAuthority; seizure, salvage, and specialists=FactionStrategicAssetAuthority; vendor staffing, scarcity, conflict, and reputation=FactionMarketAccessAuthority; Infopedia dossiers=ConstructionBlueprintInfopediaBridgeAuthority.",
                "Parity rule: unsupported, player-only, faction-only, and conditional cases remain explicit in exceptionClass and exceptionReason; the audit never upgrades them to symmetric by assumption."
        );
    }

    private static Capability playerCapability(BuildRecipe recipe, boolean licensed,
                                               BlueprintAcquisitionPathAuthority.AcquisitionPath path) {
        if (recipe == null || recipe.name == null || recipe.name.isBlank()) {
            return Capability.NOT_SUPPORTED;
        }
        if (!licensed) return Capability.SUPPORTED;
        return path == null || path.acquisitionPath() == null
                || path.acquisitionPath().isBlank()
                ? Capability.NOT_SUPPORTED : Capability.CONDITIONAL;
    }

    private static Capability factionCapability(BuildRecipe recipe, Faction issuer) {
        if (recipe == null) return Capability.NOT_SUPPORTED;
        if (issuer == Faction.NONE) return Capability.CONDITIONAL;
        if (issuer == Faction.MECHANIST_COLLEGIA
                && BuildRecipe.microForge().name.equals(recipe.name)) {
            return Capability.SUPPORTED;
        }
        return Capability.CONDITIONAL;
    }

    private static String exceptionClass(Capability player, Capability faction) {
        if (player == Capability.NOT_SUPPORTED && faction == Capability.NOT_SUPPORTED) {
            return "UNSUPPORTED";
        }
        if (player == Capability.NOT_SUPPORTED) return "FACTION_ONLY";
        if (faction == Capability.NOT_SUPPORTED) return "PLAYER_ONLY";
        if (player == Capability.SUPPORTED && faction == Capability.SUPPORTED) {
            return "SYMMETRIC";
        }
        return "CONDITIONAL";
    }

    private static String exceptionReason(BuildRecipe recipe, Capability player,
                                          Capability faction, boolean licensed) {
        StringBuilder reason = new StringBuilder();
        reason.append(licensed
                ? "player requires the licensed blueprint and live access gates"
                : "player uses the public construction catalog");
        reason.append("; faction capability is ")
                .append(capabilityLabel(faction));
        if (faction == Capability.SUPPORTED) {
            reason.append(" through the live physical construction authority");
        } else if (faction == Capability.CONDITIONAL) {
            reason.append(" pending a compatible known-plan operation, controlled room, workers, materials, and facility handler");
        }
        if (recipe.requiredKnowledge != null && !recipe.requiredKnowledge.isBlank()) {
            reason.append("; knowledge gate ").append(recipe.requiredKnowledge);
        }
        if (recipe.requiresWorkbench) reason.append("; workbench required");
        return reason.toString();
    }

    private static String materialSummary(BuildRecipe recipe) {
        ArrayList<String> materials = new ArrayList<>();
        if (recipe.supplyCost > 0) materials.add(recipe.supplyCost + " construction supplies");
        if (recipe.partCost > 0) materials.add(recipe.partCost + " machine parts");
        if (recipe.componentCosts != null) {
            for (Map.Entry<String, Integer> entry : recipe.componentCosts.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank()
                        && entry.getValue() != null && entry.getValue() > 0) {
                    materials.add(entry.getValue() + " " + entry.getKey());
                }
            }
        }
        return materials.isEmpty() ? "no material cost registered"
                : String.join(", ", materials);
    }

    private static String workforceSummary(BuildRecipe recipe,
                                           Capability factionCapability) {
        String player = "player labor uses staged construction work";
        String faction = factionCapability == Capability.SUPPORTED
                ? "faction labor uses assigned room workers"
                : factionCapability == Capability.CONDITIONAL
                ? "faction labor requires a compatible controlled-room workforce handler"
                : "no faction workforce path registered";
        return player + "; " + faction
                + (recipe.requiresWorkbench ? "; workbench operation applies" : "");
    }

    private static String capabilityLabel(Capability capability) {
        if (capability == null) return "not supported";
        return switch (capability) {
            case SUPPORTED -> "supported";
            case CONDITIONAL -> "conditional";
            case NOT_SUPPORTED -> "not supported";
        };
    }
}
