package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction budget and heat authorization contracts. */
final class Milestone03BlueprintFactionConstructionBudgetHeatAuthorizationAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.BudgetHeatAuthorization> samples =
                BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.sampleAuthorizations();
        List<String> audit = BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample budget authorizations");
        requireContains(audit, "owner=BlueprintFactionConstructionBudgetHeatAuthorizationAuthority", "budget owner");
        requireContains(audit, "jobOwner=BlueprintFactionConstructionJobDefinitionAuthority", "job owner");
        requireContains(audit, "siteOwner=BlueprintFactionConstructionSiteReadinessAuthority", "site owner");
        requireContains(audit, "heatOwner=BlueprintExpansionHeatAuthority", "heat owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "Construction supplies, Machine parts, named component count, workbench need, faction restriction, and base labor turns", "budget inputs");
        requireContains(audit, "reuse BlueprintExpansionHeatAuthority drivers", "heat reuse");
        requireContains(audit, "sampleAuthorizations=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "heatBearing=3", "heat-bearing count");
        requireContains(audit, "budgetBlocked=2", "budget blocked count");
        requireContains(audit, "job-storage-public budget and heat authorization for Storage Crate", "storage sample");
        requireContains(audit, "budget 7/20", "storage budget");
        requireContains(audit, "job-sensor-restricted budget and heat authorization for Security Sensor Mast", "sensor sample");
        requireContains(audit, "budget shortfall", "sensor budget blocker");
        requireContains(audit, "heat limit exceeded", "sensor heat blocker");
        requireContains(audit, "job-shop-public budget and heat authorization for Licensed Shop Counter", "shop sample");
        requireContains(audit, "suspicion limit exceeded", "shop suspicion blocker");
        requireContains(audit, "approve budget and heat together", "approval rule");
        requireContains(audit, "re-check both before staging", "recheck rule");
        requireContains(audit, "does not spend faction budget, mutate heat, mutate suspicion", "non-mutation boundary");
        requireContains(audit, "reserve sites, assign crew, remove materials", "execution boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionBudgetHeatAuthorizationAuditSmoke", "guard reference");

        BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.BudgetHeatAuthorization direct =
                BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.authorizationFor("job-direct", Faction.MECHANICUS,
                        BuildRecipe.microForge(), 100, 100, 100);
        require(direct.authorizationReady(), "direct micro forge budget should be ready");
        require(direct.estimatedBudget() > 0, "direct budget should be positive");
        require(direct.projectedHeat() > 0, "direct heat should be projected");
        require(direct.projectedSuspicion() > 0, "direct suspicion should be projected");
        requireContains(direct.boundaryLine(), "audit only, no spend or heat mutation", "direct boundary");

        for (BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.BudgetHeatAuthorization sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            require(sample.estimatedBudget() > 0, "estimated budget should be positive");
            require(sample.availableBudget() >= 0, "available budget should not be negative");
            require(sample.projectedHeat() >= 0, "projected heat should not be negative");
            require(sample.projectedSuspicion() >= 0, "projected suspicion should not be negative");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction budget and heat audit leaked implementation text: " + line);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireNotBlank(String value, String label) {
        require(value != null && !value.isBlank(), "expected nonblank " + label);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03BlueprintFactionConstructionBudgetHeatAuthorizationAuditSmoke() { }
}
