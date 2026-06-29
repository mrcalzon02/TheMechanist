package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only review contract for future archived faction construction response cycle review action readbacks. */
final class BlueprintFactionConstructionResponseCycleReviewActionReviewAuthority {
    record ResponseCycleReviewActionReview(String jobId, String blueprintName, String factionName, String reviewState,
                                           boolean readbackReady, boolean reviewerContextReadable,
                                           boolean evidenceLinksReady, boolean allowedActionsReadable,
                                           boolean privacyReminderReady, boolean followupBoundaryReady,
                                           boolean reviewReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleReviewActionReviewAuthority() { }

    static List<ResponseCycleReviewActionReview> sampleReviews() {
        List<BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority.ResponseCycleReviewActionReadback> readbacks =
                BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority.sampleReadbacks();
        ArrayList<ResponseCycleReviewActionReview> rows = new ArrayList<>();
        rows.add(reviewFor(readbacks.get(0), true, true, true, true, true));
        rows.add(reviewFor(readbacks.get(1), true, true, true, true, true));
        rows.add(reviewFor(readbacks.get(2), false, true, false, false, true));
        return List.copyOf(rows);
    }

    static ResponseCycleReviewActionReview reviewFor(BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority.ResponseCycleReviewActionReadback readback,
                                                     boolean reviewerContextReadable, boolean evidenceLinksReady,
                                                     boolean allowedActionsReadable, boolean privacyReminderReady,
                                                     boolean followupBoundaryReady) {
        String id = readback == null ? "job-unassigned" : clean(readback.jobId(), "job-unassigned");
        String blueprint = readback == null ? "Unknown blueprint" : clean(readback.blueprintName(), "Unknown blueprint");
        String faction = readback == null ? "Unaffiliated" : clean(readback.factionName(), "Unaffiliated");
        String action = readback == null ? "Review archived review action" : clean(readback.actionLine(), "Review archived review action");
        boolean readbackReady = readback != null && readback.readbackReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!readbackReady) blockers.add("response cycle review action readback blocked");
        if (!reviewerContextReadable) blockers.add("review action reviewer context not readable");
        if (!evidenceLinksReady) blockers.add("review action evidence links not ready");
        if (!allowedActionsReadable) blockers.add("review action allowed actions not readable");
        if (!privacyReminderReady) blockers.add("review action privacy reminder not ready");
        if (!followupBoundaryReady) blockers.add("review action follow-up boundary not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_REVIEW_ACTION_REVIEW_READY" : "RESPONSE_CYCLE_REVIEW_ACTION_REVIEW_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle review action review for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; readbackReady=" + readbackReady
                + ", reviewerContextReadable=" + reviewerContextReadable
                + ", evidenceLinksReady=" + evidenceLinksReady
                + ", allowedActionsReadable=" + allowedActionsReadable
                + ", privacyReminderReady=" + privacyReminderReady
                + ", followupBoundaryReady=" + followupBoundaryReady
                + "; blockers=" + blockerLine
                + "; audit only, no review-action review mutation.";
        return new ResponseCycleReviewActionReview(id, blueprint, faction, state, readbackReady, reviewerContextReadable,
                evidenceLinksReady, allowedActionsReadable, privacyReminderReady, followupBoundaryReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleReviewActionReview> samples = sampleReviews();
        int ready = 0;
        int blocked = 0;
        int readbackBlocked = 0;
        int reviewerBlocked = 0;
        int actionsBlocked = 0;
        int privacyBlocked = 0;
        for (ResponseCycleReviewActionReview review : samples) {
            if (review.reviewReady()) ready++;
            else blocked++;
            if (!review.readbackReady()) readbackBlocked++;
            if (!review.reviewerContextReadable()) reviewerBlocked++;
            if (!review.allowedActionsReadable()) actionsBlocked++;
            if (!review.privacyReminderReady()) privacyBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle review action review audit: owner=BlueprintFactionConstructionResponseCycleReviewActionReviewAuthority, readbackOwner=BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority, archiveOwner=BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle review action review audit: future archived review actions may be reviewed only after readback readiness, readable reviewer context, evidence links, readable allowed actions, privacy reminder, and follow-up boundary are all declared.",
                "Blueprint faction construction response cycle review action review sample audit: sampleReviews=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", readbackBlocked=" + readbackBlocked
                        + ", reviewerBlocked=" + reviewerBlocked
                        + ", actionsBlocked=" + actionsBlocked
                        + ", privacyBlocked=" + privacyBlocked + ".",
                "Blueprint faction construction response cycle review action review examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle review action review rule: a future review owner must show readable reviewer context, evidence links, readable allowed actions, privacy reminder, and follow-up boundary before offering archived review action review.",
                "Blueprint faction construction response cycle review action review boundary: this audit does not reopen review actions, execute review actions, write archives, reveal hidden faction data, update status, enqueue notifications, alter job state, move evidence, refresh archives, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewAuditSmoke checks response cycle review action review coverage, readback and reviewer blockers, review readability, future review boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
