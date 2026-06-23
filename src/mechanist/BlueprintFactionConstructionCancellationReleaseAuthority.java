package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only cancellation and release contract for future faction construction jobs. */
final class BlueprintFactionConstructionCancellationReleaseAuthority {
    record ReleasePlan(String jobId, String blueprintName, String factionName, String lifecycleState,
                       String cancellationReason, boolean siteReleaseRequired, boolean crewReleaseRequired,
                       boolean materialReleaseRequired, boolean budgetReleaseRequired, boolean attentionReleaseRequired,
                       boolean releaseReady, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionCancellationReleaseAuthority() { }

    static List<ReleasePlan> sampleReleasePlans() {
        ArrayList<ReleasePlan> rows = new ArrayList<>();
        rows.add(releaseFor("job-storage-public", Faction.HIVER, BuildRecipe.storage(), "CANCELLED",
                "player cancelled before staging", true, true, true, true, true));
        rows.add(releaseFor("job-sensor-restricted", Faction.CIVIC_WARDENS, BuildRecipe.securitySensorMast(), "FAILED",
                "heat authorization expired", true, true, true, true, true));
        rows.add(releaseFor("job-shop-public", Faction.NONE, BuildRecipe.shopCounter(), "CANCELLED",
                "", true, false, false, false, true));
        return List.copyOf(rows);
    }

    static ReleasePlan releaseFor(String jobId, Faction faction, BuildRecipe recipe, String lifecycleState,
                                  String cancellationReason, boolean siteReleaseRequired, boolean crewReleaseRequired,
                                  boolean materialReleaseRequired, boolean budgetReleaseRequired,
                                  boolean attentionReleaseRequired) {
        String id = clean(jobId, "job-unassigned");
        Faction owner = faction == null ? Faction.NONE : faction;
        BuildRecipe safeRecipe = recipe == null ? BuildRecipe.storage() : recipe;
        String state = clean(lifecycleState, "CANCELLED").toUpperCase(java.util.Locale.ROOT);
        String reason = clean(cancellationReason, "");
        ArrayList<String> blockers = new ArrayList<>();
        if (reason.isBlank()) blockers.add("missing cancellation reason");
        if (!siteReleaseRequired) blockers.add("site release not declared");
        if (!crewReleaseRequired) blockers.add("crew release not declared");
        if (!materialReleaseRequired) blockers.add("material release not declared");
        if (!budgetReleaseRequired) blockers.add("budget release not declared");
        if (!attentionReleaseRequired) blockers.add("attention release not declared");
        boolean ready = blockers.isEmpty();
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " " + state + " release for " + safeRecipe.name
                + " by " + owner.label
                + " reason=" + (reason.isBlank() ? "missing" : reason)
                + "; releases=site:" + siteReleaseRequired
                + ", crew:" + crewReleaseRequired
                + ", materials:" + materialReleaseRequired
                + ", budget:" + budgetReleaseRequired
                + ", attention:" + attentionReleaseRequired
                + "; blockers=" + blockerLine
                + "; audit only, no reservation mutation.";
        return new ReleasePlan(id, safeRecipe.name, owner.label, state, reason,
                siteReleaseRequired, crewReleaseRequired, materialReleaseRequired,
                budgetReleaseRequired, attentionReleaseRequired, ready, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ReleasePlan> samples = sampleReleasePlans();
        int ready = 0;
        int blocked = 0;
        int cancelled = 0;
        int failed = 0;
        for (ReleasePlan sample : samples) {
            if (sample.releaseReady()) ready++;
            else blocked++;
            if ("CANCELLED".equals(sample.lifecycleState())) cancelled++;
            if ("FAILED".equals(sample.lifecycleState())) failed++;
        }
        return List.of(
                "Blueprint faction construction cancellation release audit: owner=BlueprintFactionConstructionCancellationReleaseAuthority, jobOwner=BlueprintFactionConstructionJobDefinitionAuthority, siteOwner=BlueprintFactionConstructionSiteReadinessAuthority, crewOwner=BlueprintFactionConstructionCrewAssignmentAuthority, materialOwner=BlueprintFactionConstructionMaterialReservationAuthority, budgetOwner=BlueprintFactionConstructionBudgetHeatAuthorizationAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction release audit: future cancelled or failed jobs must record a reason and release site, crew, materials, budget hold, and attention preview before any execution owner can retire the job.",
                "Blueprint faction construction release sample audit: sampleReleases=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", cancelled=" + cancelled
                        + ", failed=" + failed + ".",
                "Blueprint faction construction release examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction release rule: a future execution owner must unwind authorization in the reverse order of staging and leave a cancellation or failure reason visible to audit tools.",
                "Blueprint faction construction release boundary: this audit does not cancel live jobs, release live reservations, refund budget, mutate heat, mutate suspicion, move crew, return materials, remove staged sites, place objects, advance labor, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionCancellationReleaseAuditSmoke checks release requirements, cancellation reasons, sample readiness, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
