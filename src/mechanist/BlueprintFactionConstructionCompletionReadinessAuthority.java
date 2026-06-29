package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only completion readiness contract for future faction staged construction jobs. */
final class BlueprintFactionConstructionCompletionReadinessAuthority {
    record CompletionReadiness(String jobId, String blueprintName, String factionName, String completionState,
                               boolean progressTickReady, boolean laborComplete, boolean finalSymbolReady,
                               boolean inspectionTextReady, boolean saveUpdateReady, boolean operationBoundaryReady,
                               boolean releasePlanReady, boolean completionReady,
                               String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionCompletionReadinessAuthority() { }

    static List<CompletionReadiness> sampleCompletions() {
        List<BlueprintFactionConstructionPlacementOutcomeAuthority.PlacementOutcome> outcomes =
                BlueprintFactionConstructionPlacementOutcomeAuthority.sampleOutcomes();
        ArrayList<CompletionReadiness> rows = new ArrayList<>();
        rows.add(readinessFor(BlueprintFactionConstructionProgressTickAuthority.tickFor(
                outcomes.get(0), true, true, true, true, true, 3, 1, 4), true, true, true, true, true));
        rows.add(readinessFor(BlueprintFactionConstructionProgressTickAuthority.tickFor(
                outcomes.get(1), true, true, true, true, true, 0, 1, 6), true, true, true, true, true));
        rows.add(readinessFor(BlueprintFactionConstructionProgressTickAuthority.tickFor(
                outcomes.get(2), false, true, false, false, false, 0, 0, 5), false, true, false, false, false));
        return List.copyOf(rows);
    }

    static CompletionReadiness readinessFor(BlueprintFactionConstructionProgressTickAuthority.ProgressTick tick,
                                            boolean finalSymbolReady, boolean inspectionTextReady,
                                            boolean saveUpdateReady, boolean operationBoundaryReady,
                                            boolean releasePlanReady) {
        String id = tick == null ? "job-unassigned" : clean(tick.jobId(), "job-unassigned");
        String blueprint = tick == null ? "Unknown blueprint" : clean(tick.blueprintName(), "Unknown blueprint");
        String faction = tick == null ? "Unaffiliated" : clean(tick.factionName(), "Unaffiliated");
        boolean tickReady = tick != null && tick.tickReady();
        boolean laborComplete = tick != null && tick.completionEligible();
        ArrayList<String> blockers = new ArrayList<>();
        if (!tickReady) blockers.add("progress tick blocked");
        if (!laborComplete) blockers.add("labor not complete");
        if (!finalSymbolReady) blockers.add("final symbol restoration not ready");
        if (!inspectionTextReady) blockers.add("completion inspection text not ready");
        if (!saveUpdateReady) blockers.add("save update not ready");
        if (!operationBoundaryReady) blockers.add("operation boundary not ready");
        if (!releasePlanReady) blockers.add("reservation release plan not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "COMPLETION_READY" : "COMPLETION_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " completion readiness for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; progressTickReady=" + tickReady
                + ", laborComplete=" + laborComplete
                + ", finalSymbolReady=" + finalSymbolReady
                + ", inspectionTextReady=" + inspectionTextReady
                + ", saveUpdateReady=" + saveUpdateReady
                + ", operationBoundaryReady=" + operationBoundaryReady
                + ", releasePlanReady=" + releasePlanReady
                + "; blockers=" + blockerLine
                + "; audit only, no completion mutation.";
        return new CompletionReadiness(id, blueprint, faction, state, tickReady, laborComplete, finalSymbolReady,
                inspectionTextReady, saveUpdateReady, operationBoundaryReady, releasePlanReady, ready,
                blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<CompletionReadiness> samples = sampleCompletions();
        int ready = 0;
        int blocked = 0;
        int laborBlocked = 0;
        int symbolBlocked = 0;
        int releaseBlocked = 0;
        for (CompletionReadiness completion : samples) {
            if (completion.completionReady()) ready++;
            else blocked++;
            if (!completion.laborComplete()) laborBlocked++;
            if (!completion.finalSymbolReady()) symbolBlocked++;
            if (!completion.releasePlanReady()) releaseBlocked++;
        }
        return List.of(
                "Blueprint faction construction completion readiness audit: owner=BlueprintFactionConstructionCompletionReadinessAuthority, progressOwner=BlueprintFactionConstructionProgressTickAuthority, stagedConstructionOwner=ProgressiveConstructionAuthority, releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction completion readiness audit: future execution may mark a staged site complete only after progress tick readiness, labor completion, final symbol restoration, completion inspection text, save update readiness, operation boundary readiness, and reservation release plan are all declared.",
                "Blueprint faction construction completion readiness sample audit: sampleCompletions=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", laborBlocked=" + laborBlocked
                        + ", symbolBlocked=" + symbolBlocked
                        + ", releaseBlocked=" + releaseBlocked + ".",
                "Blueprint faction construction completion readiness examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction completion readiness rule: a future execution owner must keep completion separate from ordinary facility operation and release reserved job holds only after the completed site is readable and saved.",
                "Blueprint faction construction completion readiness boundary: this audit does not restore tiles, configure facilities, enable machine operation, mutate room ownership, spend budget, mutate heat, mutate suspicion, move crew, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionCompletionReadinessAuditSmoke checks completion readiness coverage, labor and symbol blockers, operation separation, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
