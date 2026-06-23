package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Defines inspectable acquisition-path metadata for construction blueprints. */
final class BlueprintAcquisitionPathAuthority {
    record AcquisitionPath(String blueprintName, String constructionCategory, String sourceFaction,
                           String representativeType, String accessLabel, String acquisitionPath,
                           String legalLabel, String explanation) { }

    private BlueprintAcquisitionPathAuthority() { }

    static List<AcquisitionPath> paths() {
        ArrayList<AcquisitionPath> paths = new ArrayList<>();
        for (BuildRecipe recipe : BuildRecipe.allBuildRecipes()) {
            if (recipe == null) continue;
            paths.add(pathFor(recipe));
        }
        return List.copyOf(paths);
    }

    static AcquisitionPath pathFor(BuildRecipe recipe) {
        String category = ConstructionCategoryAuthority.categoryFor(recipe);
        Faction faction = FactionInventoryStockAuthority.normalizeFaction(
                recipe == null ? Faction.NONE : recipe.requiredFaction);
        boolean factionBound = faction != Faction.NONE;
        String sourceFaction = factionBound ? faction.label : "public construction market";
        String representative = representativeFor(recipe, faction, category);
        String legal = legalLabelFor(recipe, faction, category);
        String access = accessLabelFor(recipe, faction, legal);
        String acquisition = acquisitionFor(recipe, faction, representative, legal);
        String explanation = explanationFor(recipe, faction, access, legal);
        return new AcquisitionPath(recipe == null ? "Unknown Blueprint" : safe(recipe.name, "Unknown Blueprint"),
                safe(category, "Machines and Utilities"), sourceFaction, representative, access, acquisition, legal, explanation);
    }

    static List<String> definitionAuditLines() {
        List<AcquisitionPath> paths = paths();
        int factionBound = 0;
        int knowledgeGated = 0;
        int permitOrLicense = 0;
        int restricted = 0;
        int blackMarket = 0;
        int heatOrSuspicion = 0;
        for (AcquisitionPath path : paths) {
            String text = (path.accessLabel + " " + path.acquisitionPath + " " + path.legalLabel + " " + path.explanation).toLowerCase(Locale.ROOT);
            if (!"public construction market".equals(path.sourceFaction)) factionBound++;
            if (text.contains("knowledge")) knowledgeGated++;
            if (text.contains("permit") || text.contains("license")) permitOrLicense++;
            if (text.contains("restricted") || text.contains("military")) restricted++;
            if (text.contains("black-market") || text.contains("stolen") || text.contains("counterfeit")) blackMarket++;
            if (text.contains("heat") || text.contains("suspicion")) heatOrSuspicion++;
        }
        return List.of(
                "Blueprint acquisition definition audit: owner=BlueprintAcquisitionPathAuthority, catalogOwner=BuildRecipe, categoryOwner=ConstructionCategoryAuthority, traderStockOwner=TraderTradeActionAuthority, factionStockOwner=FactionInventoryStockAuthority, ordinaryUiRawIds=false.",
                "Blueprint acquisition catalog audit: blueprintPaths=" + paths.size()
                        + ", factionBoundPaths=" + factionBound
                        + ", knowledgeGatedPaths=" + knowledgeGated
                        + ", permitOrLicensePaths=" + permitOrLicense
                        + ", restrictedPaths=" + restricted
                        + ", blackMarketOrStolenPaths=" + blackMarket
                        + ", heatOrSuspicionPaths=" + heatOrSuspicion + ".",
                "Blueprint acquisition channel audit: supported channels include public construction market, faction representative, civic permit office, Mechanist Collegia vendor, Guard quartermaster, Civic Wardens armory desk, noble estate factor, contract reward, salvage research, theft or black-market broker.",
                "Blueprint acquisition gate audit: owning a blueprint is distinct from having permission, reputation, license, permit, materials, workbench, knowledge, placement access, utilities, and construction labor.",
                "Blueprint acquisition sample audit: " + sampleLine("Licensed Shop Counter")
                        + " | " + sampleLine("Security Sensor Mast")
                        + " | " + sampleLine("EMM Micro Forge") + ".",
                "Blueprint acquisition boundary: this audit does not add live vendor offers, reputation spending, permit purchase, theft resolution, heat mutation, suspicion mutation, or faction construction execution.",
                "Guard: Milestone03BlueprintAcquisitionPathAuditSmoke checks path coverage, representative labels, access gates, sample acquisition paths, future-owner boundaries, and raw-ID hiding."
        );
    }

    private static String sampleLine(String recipeName) {
        for (AcquisitionPath path : paths()) {
            if (path.blueprintName.equalsIgnoreCase(recipeName)) {
                return path.blueprintName + " via " + path.representativeType + " as " + path.accessLabel;
            }
        }
        return recipeName + " path missing";
    }

    private static String representativeFor(BuildRecipe recipe, Faction faction, String category) {
        String text = text(recipe);
        if (faction == Faction.CIVIC_WARDENS) return "Civic Wardens armory desk";
        if (faction == Faction.IMPERIAL_GUARD) return "Guard quartermaster";
        if (faction == Faction.MECHANIST_COLLEGIA) return "Mechanist Collegia vendor";
        if (faction == Faction.NOBLE) return "noble estate factor";
        if (text.contains("shop") || text.contains("business") || text.contains("permit")) return "civic permit office";
        if ("Laboratory".equals(category)) return "research or clinic supplier";
        if ("Logistics".equals(category)) return "cargo office representative";
        if ("Defense".equals(category)) return "security goods broker";
        return "public construction market";
    }

    private static String accessLabelFor(BuildRecipe recipe, Faction faction, String legalLabel) {
        String quality = recipe == null ? "Common" : safe(recipe.qualityName, "Common");
        String knowledge = recipe == null || recipe.requiredKnowledge == null || recipe.requiredKnowledge.isBlank()
                ? "no doctrine gate"
                : "knowledge gate: " + recipe.requiredKnowledge;
        String workbench = recipe != null && recipe.requiresWorkbench ? ", workbench required" : "";
        String factionGate = faction == Faction.NONE ? "public blueprint" : "faction-approved blueprint for " + faction.label;
        return factionGate + ", " + legalLabel + ", quality " + quality + ", " + knowledge + workbench;
    }

    private static String acquisitionFor(BuildRecipe recipe, Faction faction, String representative, String legalLabel) {
        String text = text(recipe);
        if (faction != Faction.NONE && legalLabel.contains("license")) {
            return "earn standing, buy from " + representative + " after license check, receive as contract reward, or salvage/research a comparable plan";
        }
        if (legalLabel.contains("permit") || legalLabel.contains("license")) {
            return "buy from " + representative + " after permit or license check";
        }
        if (text.contains("stolen") || text.contains("illegal") || text.contains("raises heat")) {
            return "buy through " + representative + " when legal, or pursue theft or black-market broker route with visible heat and suspicion risk";
        }
        if (faction != Faction.NONE) {
            return "earn standing, buy from " + representative + ", receive as contract reward, or salvage/research a comparable plan";
        }
        return "buy from " + representative + ", receive as contract reward, or learn through salvage research";
    }

    private static String legalLabelFor(BuildRecipe recipe, Faction faction, String category) {
        String text = text(recipe);
        if (text.contains("permit") || text.contains("licensed")) return "civic permit-bound";
        if (text.contains("illegal") || text.contains("black-market")) return "black-market restricted";
        if (text.contains("stolen")) return "stolen-risk";
        if (faction == Faction.IMPERIAL_GUARD || text.contains("military")) return "military restricted";
        if (faction == Faction.CIVIC_WARDENS || text.contains("serialized") || text.contains("security")) return "restricted civic";
        if (faction == Faction.NOBLE) return "noble-house controlled";
        if (faction == Faction.MECHANIST_COLLEGIA) return "forge license-bound";
        if ("Laboratory".equals(category)) return "controlled technical";
        return "public civilian";
    }

    private static String explanationFor(BuildRecipe recipe, Faction faction, String access, String legal) {
        String heat = legal.contains("restricted") || legal.contains("black-market") || legal.contains("stolen")
                || legal.contains("military") || faction != Faction.NONE
                ? " Visible use may raise heat or suspicion until a future owner applies consequences."
                : " Ordinary use has no special heat claim in this audit.";
        return "Access explanation: " + access + ". " + heat;
    }

    private static String text(BuildRecipe recipe) {
        if (recipe == null) return "";
        return (safe(recipe.name, "") + " " + safe(recipe.description, "") + " " + safe(recipe.requiredKnowledge, ""))
                .toLowerCase(Locale.ROOT);
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
