package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only follow-up contract for future archived faction construction response cycle review action results. */
final class BlueprintFactionConstructionResponseCycleReviewActionFollowupAuthority {
    record ResponseCycleReviewActionFollowup(String jobId, String blueprintName, String factionName, String followupState,
                                             boolean reviewActionResultReady, boolean reviewerSummaryReady,
                                             boolean evidenceDispositionReady, boolean statusRefreshReady,
                                             boolean notificationBoundaryReady, boolean closureBoundaryReady,
                                             boolean followupReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleReviewActionFollowupAuthority() { }

    static List<ResponseCycleReviewActionFollowup> sampleFollowups() {
        List<BlueprintFactionConstructionResponseCycleReviewActionResultAuthority.ResponseCycleReviewActionResult> results =
                BlueprintFactionConstructionResponseCycleReviewActionResultAuthority.sampleResults();
        ArrayList<ResponseCycleReviewActionFollowup> rows = new ArrayList<>();
        rows.add(followupFor(results.get(0), true, true, true, true, true));
        rows.add(followupFor(results.get(1), true, true, true, true, true));
        rows.add(followupFor(results.get(2), false, true, false, false, true));
        return List.copyOf(rows);
    }

    static ResponseCycleReviewActionFollowup followupFor(BlueprintFactionConstructionResponseCycleReviewActionResultAuthority.ResponseCycleReviewActionResult result,
                                                         boolean reviewerSummaryReady, boolean evidenceDispositionReady,
                                                         boolean statusRefreshReady, boolean notificationBoundaryReady,
                                                         boolean closureBoundaryReady) {
        String id = result == null ? "job-unassigned" : clean(result.jobId(), "job-unassigned");
        String blueprint = result == null ? "Unknown blueprint" : clean(result.blueprintName(), "Unknown blueprint");
        String faction = result == null ? "Unaffiliated" : clean(result.factionName(), "Unaffiliated");
        String action = result == null ? "Resolve review action follow-up" : clean(result.actionLine(), "Resolve review action follow-up");
        boolean resultReady = result != null && result.resultReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!resultReady) blockers.add("response cycle review action result blocked");
        if (!reviewerSummaryReady) blockers.add("reviewer summary not ready");
        if (!evidenceDispositionReady) blockers.add("evidence disposition not ready");
        if (!statusRefreshReady) blockers.add("status refresh not ready");
        if (!notificationBoundaryReady) blockers.add("notification boundary not ready");
        if (!closureBoundaryReady) blockers.add("closure boundary not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_REVIEW_ACTION_FOLLOWUP_READY" : "RESPONSE_CYCLE_REVIEW_ACTION_FOLLOWUP_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle review action follow-up for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; reviewActionResultReady=" + resultReady
                + ", reviewerSummaryReady=" + reviewerSummaryReady
                + ", evidenceDispositionReady=" + evidenceDispositionReady
                + ", statusRefreshReady=" + statusRefreshReady
                + ", notificationBoundaryReady=" + notificationBoundaryReady
                + ", closureBoundaryReady=" + closureBoundaryReady
                + "; blockers=" + blockerLine
                + "; audit only, no review-action follow-up mutation.";
        return new ResponseCycleReviewActionFollowup(id, blueprint, faction, state, resultReady, reviewerSummaryReady,
                evidenceDispositionReady, statusRefreshReady, notificationBoundaryReady, closureBoundaryReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleReviewActionFollowup> samples = sampleFollowups();
        int ready = 0;
        int blocked = 0;
        int resultBlocked = 0;
        int summaryBlocked = 0;
        int statusBlocked = 0;
        int notificationBlocked = 0;
        for (ResponseCycleReviewActionFollowup followup : samples) {
            if (followup.followupReady()) ready++;
            else blocked++;
            if (!followup.reviewActionResultReady()) resultBlocked++;
            if (!followup.reviewerSummaryReady()) summaryBlocked++;
            if (!followup.statusRefreshReady()) statusBlocked++;
            if (!followup.notificationBoundaryReady()) notificationBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle review action follow-up audit: owner=BlueprintFactionConstructionResponseCycleReviewActionFollowupAuthority, resultOwner=BlueprintFactionConstructionResponseCycleReviewActionResultAuthority, handoffOwner=BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle review action follow-up audit: future archived review action follow-up may be scheduled only after result readiness, reviewer summary, evidence disposition, status refresh, notification boundary, and closure boundary are all declared.",
                "Blueprint faction construction response cycle review action follow-up sample audit: sampleFollowups=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", resultBlocked=" + resultBlocked
                        + ", summaryBlocked=" + summaryBlocked
                        + ", statusBlocked=" + statusBlocked
                        + ", notificationBlocked=" + notificationBlocked + ".",
                "Blueprint faction construction response cycle review action follow-up examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle review action follow-up rule: a future follow-up owner must show reviewer summary, evidence disposition, status refresh, notification boundary, and closure boundary before scheduling archived review action follow-up.",
                "Blueprint faction construction response cycle review action follow-up boundary: this audit does not schedule follow-up, write summaries, move evidence, update status, enqueue notifications, close cycles, reveal hidden faction data, alter job state, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleReviewActionFollowupAuditSmoke checks response cycle review action follow-up coverage, result and summary blockers, follow-up readability, future follow-up boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
