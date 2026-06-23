package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only budget and heat authorization contract for future faction construction jobs. */
final class BlueprintFactionConstructionBudgetHeatAuthorizationAuthority {
    record BudgetHeatAuthorization(String jobId, String blueprintName, String factionName,
                                   int estimatedBudget, int availableBudget, int projectedHeat, int projectedSuspicion,
                                   boolean authorizationReady, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionBudgetHeatAuthorizationAuthority() { }

    static List<BudgetHeatAuthorization> sampleAuthorizations() {
        ArrayList<BudgetHeatAuthorization> rows = new ArrayList<>();
        rows.add(authorizationFor("job-storage-public", Faction.HIVER, BuildRecipe.storage(), 20, 6, 5));
        rows.add(authorizationFor("job-sensor-restricted", Faction.CIVIC_WARDENS, BuildRecipe.securitySensorMast(), 18, 7, 7));
        rows.add(authorizationFor("job-shop-public", Faction.NONE, BuildRecipe.shopCounter(), 12, 10, 4));
        return List.copyOf(rows);
    }

    static BudgetHeatAuthorization authorizationFor(String jobId, Faction faction, BuildRecipe recipe,
                                                    int availableBudget, int heatLimit, int suspicionLimit) {
        String id = clean(jobId, "job-unassigned");
        Faction owner = faction == null ? Faction.NONE : faction;
        BuildRecipe safeRecipe = recipe == null ? BuildRecipe.storage() : recipe;
        BlueprintExpansionHeatAuthority.HeatProfile heat = BlueprintExpansionHeatAuthority.profileFor(safeRecipe);
        int budget = estimatedBudgetFor(safeRecipe);
        int available = Math.max(0, availableBudget);
        int heatCap = Math.max(0, heatLimit);
        int suspicionCap = Math.max(0, suspicionLimit);
        ArrayList<String> blockers = new ArrayList<>();
        if (available < budget) blockers.add("budget shortfall " + (budget - available));
        if (heat.heatImpact() > heatCap) blockers.add("heat limit exceeded by " + (heat.heatImpact() - heatCap));
        if (heat.suspicionImpact() > suspicionCap) blockers.add("suspicion limit exceeded by " + (heat.suspicionImpact() - suspicionCap));
        boolean ready = blockers.isEmpty();
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " budget and heat authorization for " + safeRecipe.name
                + " by " + owner.label
                + " estimates budget " + budget + "/" + available
                + ", heat+" + heat.heatImpact()
                + ", suspicion+" + heat.suspicionImpact()
                + ", drivers=" + heat.driverSummary()
                + "; blockers=" + blockerLine
                + "; audit only, no spend or heat mutation.";
        return new BudgetHeatAuthorization(id, safeRecipe.name, owner.label, budget, available,
                heat.heatImpact(), heat.suspicionImpact(), ready, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<BudgetHeatAuthorization> samples = sampleAuthorizations();
        int ready = 0;
        int blocked = 0;
        int heatBearing = 0;
        int budgetBlocked = 0;
        for (BudgetHeatAuthorization sample : samples) {
            if (sample.authorizationReady()) ready++;
            else blocked++;
            if (sample.projectedHeat() > 0 || sample.projectedSuspicion() > 0) heatBearing++;
            if (sample.blockerLine().contains("budget shortfall")) budgetBlocked++;
        }
        return List.of(
                "Blueprint faction construction budget and heat authorization audit: owner=BlueprintFactionConstructionBudgetHeatAuthorizationAuthority, jobOwner=BlueprintFactionConstructionJobDefinitionAuthority, siteOwner=BlueprintFactionConstructionSiteReadinessAuthority, heatOwner=BlueprintExpansionHeatAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction budget audit: estimated budget derives from Construction supplies, Machine parts, named component count, workbench need, faction restriction, and base labor turns before any future job can spend funds.",
                "Blueprint faction construction heat audit: projected heat and suspicion reuse BlueprintExpansionHeatAuthority drivers before any future job can apply live heat, suspicion, law response, or faction schemes.",
                "Blueprint faction construction budget sample audit: sampleAuthorizations=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", heatBearing=" + heatBearing
                        + ", budgetBlocked=" + budgetBlocked + ".",
                "Blueprint faction construction budget examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction budget rule: a future execution owner must approve budget and heat together, re-check both before staging, and record a cancellation reason before releasing reserved site, crew, and materials.",
                "Blueprint faction construction budget boundary: this audit does not spend faction budget, mutate heat, mutate suspicion, trigger law response, schedule faction schemes, reserve sites, assign crew, remove materials, place objects, advance labor, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionBudgetHeatAuthorizationAuditSmoke checks budget estimates, heat projections, sample readiness, future execution boundaries, and raw-ID hiding."
        );
    }

    private static int estimatedBudgetFor(BuildRecipe recipe) {
        if (recipe == null) return 0;
        int budget = Math.max(0, recipe.supplyCost) * 2
                + Math.max(0, recipe.partCost) * 3
                + Math.max(0, recipe.baseTurns);
        if (recipe.componentCosts != null) {
            for (Integer count : recipe.componentCosts.values()) {
                if (count != null && count > 0) budget += count * 2;
            }
        }
        if (recipe.requiresWorkbench) budget += 4;
        if (recipe.requiredFaction != null && recipe.requiredFaction != Faction.NONE) budget += 3;
        return Math.max(1, budget);
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
