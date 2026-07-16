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
        ConstructionParityInspectionAuthority.RecipeInspection parity =
                ConstructionParityInspectionAuthority.inspect(recipe);
        return "Infopedia construction dossier for " + recipe.name + ". Category: "
                + path.constructionCategory() + ". Player construction: "
                + capabilityLabel(parity.playerCapability())
                + " after all ownership, permission, placement, material, knowledge, workbench, utility, and labor checks pass. "
                + "Faction construction: " + capabilityLabel(parity.factionCapability())
                + "; " + parity.workforceSummary() + ". "
                + "Issuing source: " + path.sourceFaction() + ". Ordinary representative: "
                + path.representativeType() + ". Legal class: " + path.legalLabel() + ".";
    }

    static String useLine(BuildRecipe recipe) {
        if (recipe == null) return "No registered construction use is available.";
        BlueprintAcquisitionPathAuthority.AcquisitionPath path =
                BlueprintAcquisitionPathAuthority.pathFor(recipe);
        boolean licensed = ConstructionBlueprintOwnershipAuthority.requiresLicensedBlueprint(recipe);
        return "Records the persistent " + recipe.name + " construction unlock. Acquisition: "
                + path.acquisitionPath() + ". Access: " + path.accessLabel()
                + ". Build requirements: " + requirementSummary(recipe)
                + ". Materials: " + materialSummary(recipe)
                + (licensed
                ? ". Contracts can grant, permit, steal, recover, counterfeit, or reveal this plan; a revealed lead does not grant ownership. "
                : ". This public plan needs no licensed folio or contract reward for ownership. ")
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
        ConstructionParityInspectionAuthority.RecipeInspection parity =
                ConstructionParityInspectionAuthority.inspect(recipe);
        boolean licensed = ConstructionBlueprintOwnershipAuthority.requiresLicensedBlueprint(recipe);
        lines.add(recipe.name + " construction blueprint");
        lines.add("Player construction: " + capabilityLabel(parity.playerCapability()) + "; "
                + (licensed ? "the licensed plan and all readiness checks are required."
                : "the public construction catalog and all readiness checks are available; no licensed folio is required."));
        lines.add("Faction construction: " + capabilityLabel(parity.factionCapability())
                + "; " + parity.workforceSummary() + ".");
        lines.add(licensed
                ? "Blueprint unlock: " + ConstructionBlueprintOwnershipAuthority.blueprintItemName(recipe) + "."
                : "Blueprint access: PUBLIC - no licensed faction folio is required.");
        lines.add("Plan source or catalog: " + path.sourceFaction() + ".");
        lines.add("Ordinary acquisition channel: " + path.representativeType() + ".");
        lines.add("Access and reputation: " + path.accessLabel() + ".");
        lines.add("Acquisition pathways: " + path.acquisitionPath() + ".");
        lines.add("Requirements: " + requirementSummary(recipe) + ".");
        lines.add("Materials: " + materialSummary(recipe) + ".");
        lines.add(ConstructionReadabilityAuthority.effortPreview(recipe));
        lines.add("Legality and attention: " + path.legalLabel() + ". "
                + ConstructionReadabilityAuthority.attentionPreview(recipe) + " "
                + liveConsequenceLine(path));
        lines.add(licensed
                ? "Contract outcomes: grant, permit, stolen, recovered, and counterfeit rewards can unlock the plan; reveal rewards record a lead without ownership."
                : "Contract outcomes: this public plan needs no ownership reward; contracts may still name construction work or reveal supporting context.");
        lines.add("Parity status: " + parity.exceptionClass() + " - "
                + parity.exceptionReason() + ".");
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

    private static String capabilityLabel(
            ConstructionParityInspectionAuthority.Capability capability) {
        if (capability == null) return "not supported";
        return switch (capability) {
            case SUPPORTED -> "supported";
            case CONDITIONAL -> "conditional";
            case NOT_SUPPORTED -> "not supported";
        };
    }
}
