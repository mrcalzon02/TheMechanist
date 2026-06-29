package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Audit-only go/no-go handoff contract before future faction construction execution. */
final class BlueprintFactionConstructionExecutionHandoffAuthority {
    record ExecutionHandoff(String jobId, String blueprintName, String factionName, String lifecycleState,
                            boolean capabilityReady, boolean materialReady, boolean crewReady,
                            boolean siteReady, boolean budgetReady, boolean rollbackReady,
                            boolean handoffReady, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionExecutionHandoffAuthority() { }

    static List<ExecutionHandoff> sampleHandoffs() {
        ArrayList<ExecutionHandoff> rows = new ArrayList<>();
        rows.add(handoffFor(
                BlueprintFactionConstructionJobDefinitionAuthority.definitionFor("job-storage-public", Faction.HIVER,
                        BuildRecipe.storage(), "AUTHORIZED", "claimed storage room"),
                BlueprintFactionConstructionMaterialReservationAuthority.reservationFor("job-storage-public", Faction.HIVER,
                        BuildRecipe.storage(), Map.of("Construction supplies", 4, "Machine parts", 2, "Rivet set", 2)),
                BlueprintFactionConstructionCrewAssignmentAuthority.assignmentFor("job-storage-public", Faction.HIVER,
                        BuildRecipe.storage(), 1),
                BlueprintFactionConstructionSiteReadinessAuthority.readinessFor("job-storage-public", Faction.HIVER,
                        BuildRecipe.storage(), "claimed storage room", true, true, true, true, true),
                BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.authorizationFor("job-storage-public", Faction.HIVER,
                        BuildRecipe.storage(), 20, 6, 5),
                BlueprintFactionConstructionCancellationReleaseAuthority.releaseFor("job-storage-public", Faction.HIVER,
                        BuildRecipe.storage(), "CANCELLED", "rollback before staging", true, true, true, true, true)));
        rows.add(handoffFor(
                BlueprintFactionConstructionJobDefinitionAuthority.definitionFor("job-sensor-restricted", Faction.CIVIC_WARDENS,
                        BuildRecipe.securitySensorMast(), "AUTHORIZED", "security room"),
                BlueprintFactionConstructionMaterialReservationAuthority.reservationFor("job-sensor-restricted", Faction.CIVIC_WARDENS,
                        BuildRecipe.securitySensorMast(), Map.of("Construction supplies", 5, "Machine parts", 1, "Sensor lens", 1, "Wire bundle", 1)),
                BlueprintFactionConstructionCrewAssignmentAuthority.assignmentFor("job-sensor-restricted", Faction.CIVIC_WARDENS,
                        BuildRecipe.securitySensorMast(), 2),
                BlueprintFactionConstructionSiteReadinessAuthority.readinessFor("job-sensor-restricted", Faction.CIVIC_WARDENS,
                        BuildRecipe.securitySensorMast(), "security room", true, true, true, false, true),
                BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.authorizationFor("job-sensor-restricted", Faction.CIVIC_WARDENS,
                        BuildRecipe.securitySensorMast(), 18, 7, 7),
                BlueprintFactionConstructionCancellationReleaseAuthority.releaseFor("job-sensor-restricted", Faction.CIVIC_WARDENS,
                        BuildRecipe.securitySensorMast(), "FAILED", "site validation failed before staging", true, true, true, true, true)));
        rows.add(handoffFor(
                BlueprintFactionConstructionJobDefinitionAuthority.definitionFor("job-shop-public", Faction.NONE,
                        BuildRecipe.shopCounter(), "REQUESTED", "market frontage"),
                BlueprintFactionConstructionMaterialReservationAuthority.reservationFor("job-shop-public", Faction.NONE,
                        BuildRecipe.shopCounter(), Map.of("Construction supplies", 1, "Warehouse inventory tag bundle", 1)),
                BlueprintFactionConstructionCrewAssignmentAuthority.assignmentFor("job-shop-public", Faction.NONE,
                        BuildRecipe.shopCounter(), 0),
                BlueprintFactionConstructionSiteReadinessAuthority.readinessFor("job-shop-public", Faction.NONE,
                        BuildRecipe.shopCounter(), "market frontage", false, true, true, true, true),
                BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.authorizationFor("job-shop-public", Faction.NONE,
                        BuildRecipe.shopCounter(), 12, 10, 4),
                BlueprintFactionConstructionCancellationReleaseAuthority.releaseFor("job-shop-public", Faction.NONE,
                        BuildRecipe.shopCounter(), "CANCELLED", "", true, false, false, false, true)));
        return List.copyOf(rows);
    }

    static ExecutionHandoff handoffFor(BlueprintFactionConstructionJobDefinitionAuthority.JobDefinition job,
                                       BlueprintFactionConstructionMaterialReservationAuthority.MaterialReservation materials,
                                       BlueprintFactionConstructionCrewAssignmentAuthority.CrewAssignment crew,
                                       BlueprintFactionConstructionSiteReadinessAuthority.SiteReadiness site,
                                       BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.BudgetHeatAuthorization budget,
                                       BlueprintFactionConstructionCancellationReleaseAuthority.ReleasePlan rollback) {
        String id = job == null ? "job-unassigned" : clean(job.jobId(), "job-unassigned");
        String blueprint = firstNonBlank(job == null ? null : job.blueprintName(),
                materials == null ? null : materials.blueprintName(),
                crew == null ? null : crew.blueprintName(),
                site == null ? null : site.blueprintName(),
                budget == null ? null : budget.blueprintName(),
                rollback == null ? null : rollback.blueprintName(),
                "Unknown blueprint");
        String faction = firstNonBlank(job == null ? null : job.factionName(),
                materials == null ? null : materials.factionName(),
                crew == null ? null : crew.factionName(),
                site == null ? null : site.factionName(),
                budget == null ? null : budget.factionName(),
                rollback == null ? null : rollback.factionName(),
                "Unaffiliated");
        String state = job == null ? "REQUESTED" : clean(job.lifecycleState(), "REQUESTED").toUpperCase(java.util.Locale.ROOT);
        boolean capabilityReady = job != null && "AUTHORIZED".equals(state);
        boolean materialReady = materials != null && materials.reservationReady();
        boolean crewReady = crew != null && crew.crewReady();
        boolean siteReady = site != null && site.siteReady();
        boolean budgetReady = budget != null && budget.authorizationReady();
        boolean rollbackReady = rollback != null && rollback.releaseReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!capabilityReady) blockers.add("job not authorized");
        if (!materialReady) blockers.add("material reservation blocked");
        if (!crewReady) blockers.add("crew assignment blocked");
        if (!siteReady) blockers.add("site readiness blocked");
        if (!budgetReady) blockers.add("budget or heat authorization blocked");
        if (!rollbackReady) blockers.add("rollback release plan blocked");
        boolean ready = blockers.isEmpty();
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " execution handoff for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; readiness=capability:" + capabilityReady
                + ", materials:" + materialReady
                + ", crew:" + crewReady
                + ", site:" + siteReady
                + ", budgetHeat:" + budgetReady
                + ", rollback:" + rollbackReady
                + "; blockers=" + blockerLine
                + "; audit only, no faction job execution.";
        return new ExecutionHandoff(id, blueprint, faction, state, capabilityReady, materialReady, crewReady,
                siteReady, budgetReady, rollbackReady, ready, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ExecutionHandoff> samples = sampleHandoffs();
        int ready = 0;
        int blocked = 0;
        int rollbackReady = 0;
        int siteBlocked = 0;
        for (ExecutionHandoff handoff : samples) {
            if (handoff.handoffReady()) ready++;
            else blocked++;
            if (handoff.rollbackReady()) rollbackReady++;
            if (!handoff.siteReady()) siteBlocked++;
        }
        return List.of(
                "Blueprint faction construction execution handoff audit: owner=BlueprintFactionConstructionExecutionHandoffAuthority, jobOwner=BlueprintFactionConstructionJobDefinitionAuthority, capabilityOwner=BlueprintFactionConstructionCapabilityAuthority, materialOwner=BlueprintFactionConstructionMaterialReservationAuthority, crewOwner=BlueprintFactionConstructionCrewAssignmentAuthority, siteOwner=BlueprintFactionConstructionSiteReadinessAuthority, budgetOwner=BlueprintFactionConstructionBudgetHeatAuthorizationAuthority, releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction handoff audit: future execution may start only after job authorization, material reservation readiness, crew assignment readiness, site readiness, budget and heat authorization, and rollback release readiness all pass together.",
                "Blueprint faction construction handoff sample audit: sampleHandoffs=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", rollbackReady=" + rollbackReady
                        + ", siteBlocked=" + siteBlocked + ".",
                "Blueprint faction construction handoff examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction handoff rule: a future execution owner must atomically re-check the handoff immediately before reserving a live job and must retain the rollback release plan before any staged construction handoff.",
                "Blueprint faction construction handoff boundary: this audit does not create a live job queue, reserve a site, reserve workers, remove materials, spend budget, mutate heat, mutate suspicion, place staged construction, advance labor, cancel jobs, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionExecutionHandoffAuditSmoke checks all readiness gates, blocker aggregation, rollback requirements, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
