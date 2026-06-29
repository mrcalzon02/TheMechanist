package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only placement outcome contract for future faction staged construction jobs. */
final class BlueprintFactionConstructionPlacementOutcomeAuthority {
    record PlacementOutcome(String jobId, String blueprintName, String factionName, String outcomeState,
                            boolean stagedHandoffReady, boolean placeholderWritten, boolean finalSymbolRecorded,
                            boolean inspectionTextReady, boolean rollbackVisible, boolean outcomeReady,
                            String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionPlacementOutcomeAuthority() { }

    static List<PlacementOutcome> sampleOutcomes() {
        List<BlueprintFactionConstructionStagedHandoffAuthority.StagedHandoff> handoffs =
                BlueprintFactionConstructionStagedHandoffAuthority.sampleHandoffs();
        ArrayList<PlacementOutcome> rows = new ArrayList<>();
        rows.add(outcomeFor(handoffs.get(0), true, true, true, true));
        rows.add(outcomeFor(handoffs.get(1), true, true, true, true));
        rows.add(outcomeFor(handoffs.get(2), false, true, false, false));
        return List.copyOf(rows);
    }

    static PlacementOutcome outcomeFor(BlueprintFactionConstructionStagedHandoffAuthority.StagedHandoff handoff,
                                       boolean placeholderWritten, boolean finalSymbolRecorded,
                                       boolean inspectionTextReady, boolean rollbackVisible) {
        String id = handoff == null ? "job-unassigned" : clean(handoff.jobId(), "job-unassigned");
        String blueprint = handoff == null ? "Unknown blueprint" : clean(handoff.blueprintName(), "Unknown blueprint");
        String faction = handoff == null ? "Unaffiliated" : clean(handoff.factionName(), "Unaffiliated");
        boolean handoffReady = handoff != null && handoff.handoffReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!handoffReady) blockers.add("staged handoff blocked");
        if (!placeholderWritten) blockers.add("construction placeholder not reserved");
        if (!finalSymbolRecorded) blockers.add("final symbol not recorded");
        if (!inspectionTextReady) blockers.add("inspection text not ready");
        if (!rollbackVisible) blockers.add("rollback outcome not visible");
        boolean ready = blockers.isEmpty();
        String state = ready ? "PLACEMENT_OUTCOME_READY" : "PLACEMENT_OUTCOME_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " placement outcome for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; stagedHandoffReady=" + handoffReady
                + ", placeholderWritten=" + placeholderWritten
                + ", finalSymbolRecorded=" + finalSymbolRecorded
                + ", inspectionTextReady=" + inspectionTextReady
                + ", rollbackVisible=" + rollbackVisible
                + "; blockers=" + blockerLine
                + "; audit only, no placement mutation.";
        return new PlacementOutcome(id, blueprint, faction, state, handoffReady, placeholderWritten,
                finalSymbolRecorded, inspectionTextReady, rollbackVisible, ready, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<PlacementOutcome> samples = sampleOutcomes();
        int ready = 0;
        int blocked = 0;
        int handoffBlocked = 0;
        int placeholderBlocked = 0;
        for (PlacementOutcome outcome : samples) {
            if (outcome.outcomeReady()) ready++;
            else blocked++;
            if (!outcome.stagedHandoffReady()) handoffBlocked++;
            if (!outcome.placeholderWritten()) placeholderBlocked++;
        }
        return List.of(
                "Blueprint faction construction placement outcome audit: owner=BlueprintFactionConstructionPlacementOutcomeAuthority, stagedHandoffOwner=BlueprintFactionConstructionStagedHandoffAuthority, stagedConstructionOwner=ProgressiveConstructionAuthority, tileOwner=ProgressiveConstructionAuthority, inspectionOwner=ConstructionReadabilityAuthority, releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction placement outcome audit: future execution may report a placed staged site only after staged handoff readiness, construction placeholder reservation, final symbol recording, staged inspection text, and visible rollback outcome are all declared.",
                "Blueprint faction construction placement outcome sample audit: sampleOutcomes=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", handoffBlocked=" + handoffBlocked
                        + ", placeholderBlocked=" + placeholderBlocked + ".",
                "Blueprint faction construction placement outcome examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction placement outcome rule: a future execution owner must make successful and failed placement outcomes readable before it can transfer a reserved job into staged construction.",
                "Blueprint faction construction placement outcome boundary: this audit does not write tiles, create staged sites, mutate original tiles, transfer materials, assign workers, spend budget, mutate heat, mutate suspicion, advance labor, cancel jobs, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionPlacementOutcomeAuditSmoke checks placement outcome coverage, placeholder and final-symbol requirements, blocked outcomes, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
