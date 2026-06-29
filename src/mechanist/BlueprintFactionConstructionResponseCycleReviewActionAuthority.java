package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only action contract for future archived faction construction response cycle reviews. */
final class BlueprintFactionConstructionResponseCycleReviewActionAuthority {
    record ResponseCycleReviewAction(String jobId, String blueprintName, String factionName, String actionState,
                                     boolean reviewReady, boolean permissionReady,
                                     boolean confirmationReady, boolean evidenceSelectionReady,
                                     boolean auditTextReady, boolean nonReopenBoundaryReady,
                                     boolean actionReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleReviewActionAuthority() { }

    static List<ResponseCycleReviewAction> sampleReviewActions() {
        List<BlueprintFactionConstructionResponseCycleReviewAuthority.ResponseCycleReview> reviews =
                BlueprintFactionConstructionResponseCycleReviewAuthority.sampleReviews();
        ArrayList<ResponseCycleReviewAction> rows = new ArrayList<>();
        rows.add(actionFor(reviews.get(0), true, true, true, true, true));
        rows.add(actionFor(reviews.get(1), true, true, true, true, true));
        rows.add(actionFor(reviews.get(2), false, false, true, false, true));
        return List.copyOf(rows);
    }

    static ResponseCycleReviewAction actionFor(BlueprintFactionConstructionResponseCycleReviewAuthority.ResponseCycleReview review,
                                               boolean permissionReady, boolean confirmationReady,
                                               boolean evidenceSelectionReady, boolean auditTextReady,
                                               boolean nonReopenBoundaryReady) {
        String id = review == null ? "job-unassigned" : clean(review.jobId(), "job-unassigned");
        String blueprint = review == null ? "Unknown blueprint" : clean(review.blueprintName(), "Unknown blueprint");
        String faction = review == null ? "Unaffiliated" : clean(review.factionName(), "Unaffiliated");
        String action = review == null ? "Prepare review action" : clean(review.actionLine(), "Prepare review action");
        boolean reviewReady = review != null && review.reviewReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!reviewReady) blockers.add("response cycle review blocked");
        if (!permissionReady) blockers.add("review action permission not ready");
        if (!confirmationReady) blockers.add("review action confirmation not ready");
        if (!evidenceSelectionReady) blockers.add("evidence selection not ready");
        if (!auditTextReady) blockers.add("review action audit text not ready");
        if (!nonReopenBoundaryReady) blockers.add("non-reopen boundary not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_REVIEW_ACTION_READY" : "RESPONSE_CYCLE_REVIEW_ACTION_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle review action for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; reviewReady=" + reviewReady
                + ", permissionReady=" + permissionReady
                + ", confirmationReady=" + confirmationReady
                + ", evidenceSelectionReady=" + evidenceSelectionReady
                + ", auditTextReady=" + auditTextReady
                + ", nonReopenBoundaryReady=" + nonReopenBoundaryReady
                + "; blockers=" + blockerLine
                + "; audit only, no review-action mutation.";
        return new ResponseCycleReviewAction(id, blueprint, faction, state, reviewReady, permissionReady,
                confirmationReady, evidenceSelectionReady, auditTextReady, nonReopenBoundaryReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleReviewAction> samples = sampleReviewActions();
        int ready = 0;
        int blocked = 0;
        int reviewBlocked = 0;
        int permissionBlocked = 0;
        int confirmationBlocked = 0;
        int auditTextBlocked = 0;
        for (ResponseCycleReviewAction action : samples) {
            if (action.actionReady()) ready++;
            else blocked++;
            if (!action.reviewReady()) reviewBlocked++;
            if (!action.permissionReady()) permissionBlocked++;
            if (!action.confirmationReady()) confirmationBlocked++;
            if (!action.auditTextReady()) auditTextBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle review action audit: owner=BlueprintFactionConstructionResponseCycleReviewActionAuthority, reviewOwner=BlueprintFactionConstructionResponseCycleReviewAuthority, readbackOwner=BlueprintFactionConstructionResponseCycleReadbackAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle review action audit: future archived review actions may be offered only after review readiness, permission readiness, confirmation readiness, evidence selection, audit text, and non-reopen boundary are all declared.",
                "Blueprint faction construction response cycle review action sample audit: sampleReviewActions=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", reviewBlocked=" + reviewBlocked
                        + ", permissionBlocked=" + permissionBlocked
                        + ", confirmationBlocked=" + confirmationBlocked
                        + ", auditTextBlocked=" + auditTextBlocked + ".",
                "Blueprint faction construction response cycle review action examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle review action rule: a future review-action owner must show permission readiness, confirmation readiness, evidence selection, audit text, and non-reopen boundary before offering archived review actions.",
                "Blueprint faction construction response cycle review action boundary: this audit does not execute review actions, reopen cycles, write archives, export evidence, reveal hidden faction data, update status, enqueue notifications, alter job state, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleReviewActionAuditSmoke checks response cycle review action coverage, review and permission blockers, action readability, future action boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
