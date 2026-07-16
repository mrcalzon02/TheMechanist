package mechanist;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

/** Builds player-facing Infopedia text from the live construction metadata owners. */
final class ConstructionBlueprintInfopediaBridgeAuthority {
    private ConstructionBlueprintInfopediaBridgeAuthority() { }

    static String sourceLine(BuildRecipe recipe) {
        if (recipe == null) return "construction planning offices and documented recovery channels";
        BlueprintAcquisitionPathAuthority.AcquisitionPath path =
                BlueprintAcquisitionPathAuthority.pathFor(recipe);
        return path.sourceFaction() + " / " + path.representativeType()
                + "; contract reward, recovered-plan, permit, theft, and counterfeit routes may also exist when a quest explicitly names them";
    }

    static String description(BuildRecipe recipe) {
        if (recipe == null) return "An unidentified construction plan with no registered build definition.";
        BlueprintAcquisitionPathAuthority.AcquisitionPath path =
                BlueprintAcquisitionPathAuthority.pathFor(recipe);
        return "Infopedia construction dossier for " + recipe.name + ". Category: "
                + path.constructionCategory() + ". Player construction: supported after this blueprint is owned and all permission, placement, material, knowledge, workbench, utility, and labor checks pass. "
                + "Faction construction: supported through known faction plans when a compatible controlled room, workforce, materials, and facility authority exist. "
                + "Issuing source: " + path.sourceFaction() + ". Ordinary representative: "
                + path.representativeType() + ". Legal class: " + path.legalLabel() + ".";
    }

    static String useLine(BuildRecipe recipe) {
        if (recipe == null) return "No registered construction use is available.";
        BlueprintAcquisitionPathAuthority.AcquisitionPath path =
                BlueprintAcquisitionPathAuthority.pathFor(recipe);
        return "Records the persistent " + recipe.name + " construction unlock. Acquisition: "
                + path.acquisitionPath() + ". Access: " + path.accessLabel()
                + ". Build requirements: " + requirementSummary(recipe)
                + ". Materials: " + materialSummary(recipe)
                + ". Contracts can grant, permit, steal, recover, counterfeit, or reveal this plan; a revealed lead does not grant ownership. "
                + "Stolen and counterfeit rewards create visible suspicion or legality risk. Search the Infopedia for '"
                + ConstructionBlueprintOwnershipAuthority.blueprintItemName(recipe)
                + "' to reopen this exact dossier.";
    }

    static ArrayList<String> dossierLines(BuildRecipe recipe) {
        ArrayList<String> lines = new ArrayList<>();
        if (recipe == null) {
            lines.add("No construction blueprint definition is registered.");
            return lines;
        }
        BlueprintAcquisitionPathAuthority.AcquisitionPath path =
                BlueprintAcquisitionPathAuthority.pathFor(recipe);
        lines.add(recipe.name + " construction blueprint");
        lines.add("Player construction: supported after ownership and all readiness checks pass.");
        lines.add("Faction construction: supported through compatible known-plan, room, workforce, material, and facility rules.");
        lines.add("Blueprint unlock: "
                + ConstructionBlueprintOwnershipAuthority.blueprintItemName(recipe) + ".");
        lines.add("Issuing faction or market: " + path.sourceFaction() + ".");
        lines.add("Ordinary seller or grantor: " + path.representativeType() + ".");
        lines.add("Access and reputation: " + path.accessLabel() + ".");
        lines.add("Acquisition pathways: " + path.acquisitionPath() + ".");
        lines.add("Requirements: " + requirementSummary(recipe) + ".");
        lines.add("Materials: " + materialSummary(recipe) + ".");
        lines.add("Legality and attention: " + path.legalLabel() + ". "
                + liveConsequenceLine(path));
        lines.add("Contract outcomes: grant, permit, stolen, recovered, and counterfeit rewards can unlock the plan; reveal rewards record a lead without ownership.");
        return lines;
    }

    private static String requirementSummary(BuildRecipe recipe) {
        ArrayList<String> parts = new ArrayList<>();
        parts.add("Mechanics " + Math.max(0, recipe.reqMechanics));
        parts.add("Intellect " + Math.max(0, recipe.reqIntellect));
        parts.add("quality " + safe(recipe.qualityName, "Common"));
        if (recipe.requiredKnowledge != null && !recipe.requiredKnowledge.isBlank()) {
            parts.add("knowledge " + recipe.requiredKnowledge);
        } else {
            parts.add("no doctrine gate");
        }
        parts.add(recipe.requiresWorkbench ? "workbench required" : "no workbench required");
        if (recipe.requiredFaction != null && recipe.requiredFaction != Faction.NONE) {
            parts.add("issued by "
                    + FactionInventoryStockAuthority.normalizeFaction(recipe.requiredFaction).label);
        }
        return String.join(", ", parts);
    }

    private static String materialSummary(BuildRecipe recipe) {
        ArrayList<String> parts = new ArrayList<>();
        if (recipe.supplyCost > 0) parts.add(recipe.supplyCost + " construction supplies");
        if (recipe.partCost > 0) parts.add(recipe.partCost + " machine parts");
        if (recipe.componentCosts != null) {
            for (Map.Entry<String, Integer> entry : recipe.componentCosts.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()
                        || entry.getValue() == null || entry.getValue() <= 0) continue;
                parts.add(entry.getValue() + " " + entry.getKey());
            }
        }
        return parts.isEmpty() ? "no material cost registered" : String.join(", ", parts);
    }

    private static String liveConsequenceLine(
            BlueprintAcquisitionPathAuthority.AcquisitionPath path) {
        String legal = path == null ? "" : safe(path.legalLabel(), "").toLowerCase(Locale.ROOT);
        if (legal.contains("black-market") || legal.contains("stolen")) {
            return "Illicit acquisition and use can increase suspicion and preserve disputed provenance.";
        }
        if (legal.contains("restricted") || legal.contains("military")
                || legal.contains("license") || legal.contains("permit")
                || legal.contains("noble")) {
            return "Standing, membership, permits, conflict, scarcity, staffing, and active facility stock can restrict transfer.";
        }
        return "Ordinary construction still applies visible placement, resource, and expansion-attention rules.";
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().replace('|', '/');
    }
}
