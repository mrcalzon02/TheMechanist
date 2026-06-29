package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only close contract for future archived faction construction response cycle review action follow-ups. */
final class BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority {
    record ResponseCycleReviewActionClose(String jobId, String blueprintName, String factionName, String closeState,
                                          boolean followupReady, boolean closeDecisionReadable,
                                          boolean statusReturnDeclared, boolean notificationReturnDeclared,
                                          boolean evidenceRetentionBoundaryReady, boolean archiveRefreshBoundaryReady,
                                          boolean closeReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority() { }

    static List<ResponseCycleReviewActionClose> sampleClosures() {
        List<BlueprintFactionConstructionResponseCycleReviewActionFollowupAuthority.ResponseCycleReviewActionFollowup> followups =
                BlueprintFactionConstructionResponseCycleReviewActionFollowupAuthority.sampleFollowups();
        ArrayList<ResponseCycleReviewActionClose> rows = new ArrayList<>();
        rows.add(closeFor(followups.get(0), true, true, true, true, true));
        rows.add(closeFor(followups.get(1), true, true, true, true, true));
        rows.add(closeFor(followups.get(2), false, false, true, false, true));
        return List.copyOf(rows);
    }

    static ResponseCycleReviewActionClose closeFor(BlueprintFactionConstructionResponseCycleReviewActionFollowupAuthority.ResponseCycleReviewActionFollowup followup,
                                                   boolean closeDecisionReadable, boolean statusReturnDeclared,
                                                   boolean notificationReturnDeclared, boolean evidenceRetentionBoundaryReady,
                                                   boolean archiveRefreshBoundaryReady) {
        String id = followup == null ? "job-unassigned" : clean(followup.jobId(), "job-unassigned");
        String blueprint = followup == null ? "Unknown blueprint" : clean(followup.blueprintName(), "Unknown blueprint");
        String faction = followup == null ? "Unaffiliated" : clean(followup.factionName(), "Unaffiliated");
        String action = followup == null ? "Close review action follow-up" : clean(followup.actionLine(), "Close review action follow-up");
        boolean followupReady = followup != null && followup.followupReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!followupReady) blockers.add("response cycle review action follow-up blocked");
        if (!closeDecisionReadable) blockers.add("review action close decision not readable");
        if (!statusReturnDeclared) blockers.add("review action status return not declared");
        if (!notificationReturnDeclared) blockers.add("review action notification return not declared");
        if (!evidenceRetentionBoundaryReady) blockers.add("review action evidence retention boundary not ready");
        if (!archiveRefreshBoundaryReady) blockers.add("review action archive refresh boundary not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_REVIEW_ACTION_CLOSE_READY" : "RESPONSE_CYCLE_REVIEW_ACTION_CLOSE_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle review action close for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; followupReady=" + followupReady
                + ", closeDecisionReadable=" + closeDecisionReadable
                + ", statusReturnDeclared=" + statusReturnDeclared
                + ", notificationReturnDeclared=" + notificationReturnDeclared
                + ", evidenceRetentionBoundaryReady=" + evidenceRetentionBoundaryReady
                + ", archiveRefreshBoundaryReady=" + archiveRefreshBoundaryReady
                + "; blockers=" + blockerLine
                + "; audit only, no review-action close mutation.";
        return new ResponseCycleReviewActionClose(id, blueprint, faction, state, followupReady, closeDecisionReadable,
                statusReturnDeclared, notificationReturnDeclared, evidenceRetentionBoundaryReady, archiveRefreshBoundaryReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleReviewActionClose> samples = sampleClosures();
        int ready = 0;
        int blocked = 0;
        int followupBlocked = 0;
        int decisionBlocked = 0;
        int statusBlocked = 0;
        int evidenceBlocked = 0;
        for (ResponseCycleReviewActionClose close : samples) {
            if (close.closeReady()) ready++;
            else blocked++;
            if (!close.followupReady()) followupBlocked++;
            if (!close.closeDecisionReadable()) decisionBlocked++;
            if (!close.statusReturnDeclared()) statusBlocked++;
            if (!close.evidenceRetentionBoundaryReady()) evidenceBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle review action close audit: owner=BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority, followupOwner=BlueprintFactionConstructionResponseCycleReviewActionFollowupAuthority, resultOwner=BlueprintFactionConstructionResponseCycleReviewActionResultAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle review action close audit: future archived review action follow-up may close only after follow-up readiness, readable close decision, status return declaration, notification return declaration, evidence retention boundary, and archive refresh boundary are all declared.",
                "Blueprint faction construction response cycle review action close sample audit: sampleClosures=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", followupBlocked=" + followupBlocked
                        + ", decisionBlocked=" + decisionBlocked
                        + ", statusBlocked=" + statusBlocked
                        + ", evidenceBlocked=" + evidenceBlocked + ".",
                "Blueprint faction construction response cycle review action close examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle review action close rule: a future close owner must show readable close decision, status return declaration, notification return declaration, evidence retention boundary, and archive refresh boundary before closing archived review action follow-up.",
                "Blueprint faction construction response cycle review action close boundary: this audit does not close follow-up, update status, enqueue notifications, move evidence, refresh archives, reveal hidden faction data, alter job state, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleReviewActionCloseAuditSmoke checks response cycle review action close coverage, follow-up and decision blockers, close readability, future close boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
