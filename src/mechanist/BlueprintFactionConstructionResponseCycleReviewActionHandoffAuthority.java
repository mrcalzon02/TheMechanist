package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only handoff contract for future archived faction construction response cycle review actions. */
final class BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority {
    record ResponseCycleReviewActionHandoff(String jobId, String blueprintName, String factionName, String handoffState,
                                            boolean reviewActionReady, boolean targetResolutionReady,
                                            boolean commandOwnerReady, boolean rollbackPreviewReady,
                                            boolean turnCostPreviewReady, boolean resultTextReady,
                                            boolean handoffReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority() { }

    static List<ResponseCycleReviewActionHandoff> sampleHandoffs() {
        List<BlueprintFactionConstructionResponseCycleReviewActionAuthority.ResponseCycleReviewAction> actions =
                BlueprintFactionConstructionResponseCycleReviewActionAuthority.sampleReviewActions();
        ArrayList<ResponseCycleReviewActionHandoff> rows = new ArrayList<>();
        rows.add(handoffFor(actions.get(0), true, true, true, true, true));
        rows.add(handoffFor(actions.get(1), true, true, true, true, true));
        rows.add(handoffFor(actions.get(2), false, false, true, false, true));
        return List.copyOf(rows);
    }

    static ResponseCycleReviewActionHandoff handoffFor(BlueprintFactionConstructionResponseCycleReviewActionAuthority.ResponseCycleReviewAction action,
                                                       boolean targetResolutionReady, boolean commandOwnerReady,
                                                       boolean rollbackPreviewReady, boolean turnCostPreviewReady,
                                                       boolean resultTextReady) {
        String id = action == null ? "job-unassigned" : clean(action.jobId(), "job-unassigned");
        String blueprint = action == null ? "Unknown blueprint" : clean(action.blueprintName(), "Unknown blueprint");
        String faction = action == null ? "Unaffiliated" : clean(action.factionName(), "Unaffiliated");
        String actionLine = action == null ? "Hand off review action" : clean(action.actionLine(), "Hand off review action");
        boolean actionReady = action != null && action.actionReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!actionReady) blockers.add("response cycle review action blocked");
        if (!targetResolutionReady) blockers.add("review action target not resolved");
        if (!commandOwnerReady) blockers.add("review action command owner not ready");
        if (!rollbackPreviewReady) blockers.add("review action rollback preview not ready");
        if (!turnCostPreviewReady) blockers.add("review action turn cost not ready");
        if (!resultTextReady) blockers.add("review action result text not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_REVIEW_ACTION_HANDOFF_READY" : "RESPONSE_CYCLE_REVIEW_ACTION_HANDOFF_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle review action handoff for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + actionLine
                + "; reviewActionReady=" + actionReady
                + ", targetResolutionReady=" + targetResolutionReady
                + ", commandOwnerReady=" + commandOwnerReady
                + ", rollbackPreviewReady=" + rollbackPreviewReady
                + ", turnCostPreviewReady=" + turnCostPreviewReady
                + ", resultTextReady=" + resultTextReady
                + "; blockers=" + blockerLine
                + "; audit only, no review-action handoff mutation.";
        return new ResponseCycleReviewActionHandoff(id, blueprint, faction, state, actionReady, targetResolutionReady,
                commandOwnerReady, rollbackPreviewReady, turnCostPreviewReady, resultTextReady,
                ready, actionLine, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleReviewActionHandoff> samples = sampleHandoffs();
        int ready = 0;
        int blocked = 0;
        int actionBlocked = 0;
        int targetBlocked = 0;
        int ownerBlocked = 0;
        int turnCostBlocked = 0;
        for (ResponseCycleReviewActionHandoff handoff : samples) {
            if (handoff.handoffReady()) ready++;
            else blocked++;
            if (!handoff.reviewActionReady()) actionBlocked++;
            if (!handoff.targetResolutionReady()) targetBlocked++;
            if (!handoff.commandOwnerReady()) ownerBlocked++;
            if (!handoff.turnCostPreviewReady()) turnCostBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle review action handoff audit: owner=BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority, actionOwner=BlueprintFactionConstructionResponseCycleReviewActionAuthority, reviewOwner=BlueprintFactionConstructionResponseCycleReviewAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle review action handoff audit: future archived review actions may be handed off only after review action readiness, target resolution, command owner, rollback preview, turn cost preview, and result text are all declared.",
                "Blueprint faction construction response cycle review action handoff sample audit: sampleHandoffs=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", actionBlocked=" + actionBlocked
                        + ", targetBlocked=" + targetBlocked
                        + ", ownerBlocked=" + ownerBlocked
                        + ", turnCostBlocked=" + turnCostBlocked + ".",
                "Blueprint faction construction response cycle review action handoff examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle review action handoff rule: a future handoff owner must show target resolution, command owner, rollback preview, turn cost preview, and result text before handing off archived review actions.",
                "Blueprint faction construction response cycle review action handoff boundary: this audit does not hand off commands, execute review actions, reopen cycles, write archives, export evidence, reveal hidden faction data, update status, alter job state, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleReviewActionHandoffAuditSmoke checks response cycle review action handoff coverage, action and target blockers, handoff readability, future handoff boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
