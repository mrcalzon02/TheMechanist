package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only close contract for future archived faction construction response cycle review-action review follow-ups. */
final class BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority {
    record ResponseCycleReviewActionReviewClose(String jobId, String blueprintName, String factionName, String closeState,
                                                boolean followupReady, boolean closeDecisionReadable,
                                                boolean statusReturnDeclared, boolean notificationReturnDeclared,
                                                boolean evidenceRetentionBoundaryReady, boolean archiveRefreshBoundaryReady,
                                                boolean closeReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority() { }

    static List<ResponseCycleReviewActionReviewClose> sampleClosures() {
        List<BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority.ResponseCycleReviewActionReviewFollowup> followups =
                BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority.sampleFollowups();
        ArrayList<ResponseCycleReviewActionReviewClose> rows = new ArrayList<>();
        rows.add(closeFor(followups.get(0), true, true, true, true, true));
        rows.add(closeFor(followups.get(1), true, true, true, true, true));
        rows.add(closeFor(followups.get(2), false, false, true, false, true));
        return List.copyOf(rows);
    }

    static ResponseCycleReviewActionReviewClose closeFor(BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority.ResponseCycleReviewActionReviewFollowup followup,
                                                         boolean closeDecisionReadable, boolean statusReturnDeclared,
                                                         boolean notificationReturnDeclared, boolean evidenceRetentionBoundaryReady,
                                                         boolean archiveRefreshBoundaryReady) {
        String id = followup == null ? "job-unassigned" : clean(followup.jobId(), "job-unassigned");
        String blueprint = followup == null ? "Unknown blueprint" : clean(followup.blueprintName(), "Unknown blueprint");
        String faction = followup == null ? "Unaffiliated" : clean(followup.factionName(), "Unaffiliated");
        String action = followup == null ? "Close review-action review follow-up" : clean(followup.actionLine(), "Close review-action review follow-up");
        boolean followupReady = followup != null && followup.followupReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!followupReady) blockers.add("response cycle review action review follow-up blocked");
        if (!closeDecisionReadable) blockers.add("review-action review close decision not readable");
        if (!statusReturnDeclared) blockers.add("review-action review status return not declared");
        if (!notificationReturnDeclared) blockers.add("review-action review notification return not declared");
        if (!evidenceRetentionBoundaryReady) blockers.add("review-action review evidence retention boundary not ready");
        if (!archiveRefreshBoundaryReady) blockers.add("review-action review archive refresh boundary not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_REVIEW_ACTION_REVIEW_CLOSE_READY" : "RESPONSE_CYCLE_REVIEW_ACTION_REVIEW_CLOSE_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle review action review close for " + blueprint
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
                + "; audit only, no review-action review close mutation.";
        return new ResponseCycleReviewActionReviewClose(id, blueprint, faction, state, followupReady, closeDecisionReadable,
                statusReturnDeclared, notificationReturnDeclared, evidenceRetentionBoundaryReady, archiveRefreshBoundaryReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleReviewActionReviewClose> samples = sampleClosures();
        int ready = 0;
        int blocked = 0;
        int followupBlocked = 0;
        int decisionBlocked = 0;
        int statusBlocked = 0;
        int evidenceBlocked = 0;
        for (ResponseCycleReviewActionReviewClose close : samples) {
            if (close.closeReady()) ready++;
            else blocked++;
            if (!close.followupReady()) followupBlocked++;
            if (!close.closeDecisionReadable()) decisionBlocked++;
            if (!close.statusReturnDeclared()) statusBlocked++;
            if (!close.evidenceRetentionBoundaryReady()) evidenceBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle review action review close audit: owner=BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority, followupOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority, resultOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewResultAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle review action review close audit: future archived review-action review follow-up may close only after follow-up readiness, readable close decision, status return declaration, notification return declaration, evidence retention boundary, and archive refresh boundary are all declared.",
                "Blueprint faction construction response cycle review action review close sample audit: sampleClosures=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", followupBlocked=" + followupBlocked
                        + ", decisionBlocked=" + decisionBlocked
                        + ", statusBlocked=" + statusBlocked
                        + ", evidenceBlocked=" + evidenceBlocked + ".",
                "Blueprint faction construction response cycle review action review close examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle review action review close rule: a future close owner must show readable close decision, status return declaration, notification return declaration, evidence retention boundary, and archive refresh boundary before closing archived review-action review follow-up.",
                "Blueprint faction construction response cycle review action review close boundary: this audit does not close follow-up, update status, enqueue notifications, move evidence, refresh archives, reveal hidden faction data, alter job state, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuditSmoke checks response cycle review action review close coverage, follow-up and decision blockers, close readability, future close boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
