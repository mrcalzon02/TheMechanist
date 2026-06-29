package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only labor progress tick contract for future faction staged construction jobs. */
final class BlueprintFactionConstructionProgressTickAuthority {
    record ProgressTick(String jobId, String blueprintName, String factionName, String tickState,
                        boolean placementOutcomeReady, boolean crewPresent, boolean materialsStaged,
                        boolean workWindowOpen, boolean progressRecordReady, boolean rollbackVisible,
                        int laborBefore, int laborAfter, int requiredLaborTurns,
                        boolean tickReady, boolean completionEligible, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionProgressTickAuthority() { }

    static List<ProgressTick> sampleTicks() {
        List<BlueprintFactionConstructionPlacementOutcomeAuthority.PlacementOutcome> outcomes =
                BlueprintFactionConstructionPlacementOutcomeAuthority.sampleOutcomes();
        ArrayList<ProgressTick> rows = new ArrayList<>();
        rows.add(tickFor(outcomes.get(0), true, true, true, true, true, 1, 1, 4));
        rows.add(tickFor(outcomes.get(1), true, true, true, true, true, 0, 1, 6));
        rows.add(tickFor(outcomes.get(2), false, true, false, false, false, 0, 0, 5));
        return List.copyOf(rows);
    }

    static ProgressTick tickFor(BlueprintFactionConstructionPlacementOutcomeAuthority.PlacementOutcome outcome,
                                boolean crewPresent, boolean materialsStaged, boolean workWindowOpen,
                                boolean progressRecordReady, boolean rollbackVisible,
                                int laborBefore, int laborDelta, int requiredLaborTurns) {
        String id = outcome == null ? "job-unassigned" : clean(outcome.jobId(), "job-unassigned");
        String blueprint = outcome == null ? "Unknown blueprint" : clean(outcome.blueprintName(), "Unknown blueprint");
        String faction = outcome == null ? "Unaffiliated" : clean(outcome.factionName(), "Unaffiliated");
        boolean placementReady = outcome != null && outcome.outcomeReady();
        int required = Math.max(1, requiredLaborTurns);
        int before = Math.max(0, laborBefore);
        int after = Math.min(required, Math.max(before, before + Math.max(0, laborDelta)));
        ArrayList<String> blockers = new ArrayList<>();
        if (!placementReady) blockers.add("placement outcome blocked");
        if (!crewPresent) blockers.add("crew not present");
        if (!materialsStaged) blockers.add("staged materials missing");
        if (!workWindowOpen) blockers.add("work window closed");
        if (!progressRecordReady) blockers.add("progress record not ready");
        if (!rollbackVisible) blockers.add("rollback outcome not visible");
        boolean ready = blockers.isEmpty();
        boolean complete = ready && after >= required;
        String state = ready ? "PROGRESS_TICK_READY" : "PROGRESS_TICK_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " progress tick for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; placementOutcomeReady=" + placementReady
                + ", crewPresent=" + crewPresent
                + ", materialsStaged=" + materialsStaged
                + ", workWindowOpen=" + workWindowOpen
                + ", progressRecordReady=" + progressRecordReady
                + ", rollbackVisible=" + rollbackVisible
                + "; labor=" + before + "->" + after + "/" + required
                + "; completionEligible=" + complete
                + "; blockers=" + blockerLine
                + "; audit only, no labor mutation.";
        return new ProgressTick(id, blueprint, faction, state, placementReady, crewPresent, materialsStaged,
                workWindowOpen, progressRecordReady, rollbackVisible, before, after, required,
                ready, complete, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ProgressTick> samples = sampleTicks();
        int ready = 0;
        int blocked = 0;
        int placementBlocked = 0;
        int crewBlocked = 0;
        int completionEligible = 0;
        for (ProgressTick tick : samples) {
            if (tick.tickReady()) ready++;
            else blocked++;
            if (!tick.placementOutcomeReady()) placementBlocked++;
            if (!tick.crewPresent()) crewBlocked++;
            if (tick.completionEligible()) completionEligible++;
        }
        return List.of(
                "Blueprint faction construction progress tick audit: owner=BlueprintFactionConstructionProgressTickAuthority, placementOwner=BlueprintFactionConstructionPlacementOutcomeAuthority, crewOwner=BlueprintFactionConstructionCrewAssignmentAuthority, materialOwner=BlueprintFactionConstructionMaterialReservationAuthority, stagedConstructionOwner=ProgressiveConstructionAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction progress tick audit: future execution may record labor progress only after placement outcome readiness, crew presence, staged materials, open work window, progress record readiness, and visible rollback outcome are all declared.",
                "Blueprint faction construction progress tick sample audit: sampleTicks=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", placementBlocked=" + placementBlocked
                        + ", crewBlocked=" + crewBlocked
                        + ", completionEligible=" + completionEligible + ".",
                "Blueprint faction construction progress tick examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction progress tick rule: a future execution owner must separate labor progress from completion, keep failed ticks readable, and leave final facility configuration to a later completion owner.",
                "Blueprint faction construction progress tick boundary: this audit does not advance labor, consume staged materials, move crew, mutate room ownership, configure facilities, spend budget, mutate heat, mutate suspicion, cancel jobs, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionProgressTickAuditSmoke checks labor tick coverage, placement and crew blockers, completion separation, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
