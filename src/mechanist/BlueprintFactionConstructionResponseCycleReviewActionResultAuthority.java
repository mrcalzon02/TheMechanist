package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only result contract for future archived faction construction response cycle review actions. */
final class BlueprintFactionConstructionResponseCycleReviewActionResultAuthority {
    record ResponseCycleReviewActionResult(String jobId, String blueprintName, String factionName, String resultState,
                                           boolean handoffReady, boolean commandOutcomeReadable,
                                           boolean auditLedgerReady, boolean rollbackOutcomeReady,
                                           boolean followupBoundaryReady, boolean notificationBoundaryReady,
                                           boolean resultReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleReviewActionResultAuthority() { }

    static List<ResponseCycleReviewActionResult> sampleResults() {
        List<BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority.ResponseCycleReviewActionHandoff> handoffs =
                BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority.sampleHandoffs();
        ArrayList<ResponseCycleReviewActionResult> rows = new ArrayList<>();
        rows.add(resultFor(handoffs.get(0), true, true, true, true, true));
        rows.add(resultFor(handoffs.get(1), true, true, true, true, true));
        rows.add(resultFor(handoffs.get(2), false, true, false, false, true));
        return List.copyOf(rows);
    }

    static ResponseCycleReviewActionResult resultFor(BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority.ResponseCycleReviewActionHandoff handoff,
                                                     boolean commandOutcomeReadable, boolean auditLedgerReady,
                                                     boolean rollbackOutcomeReady, boolean followupBoundaryReady,
                                                     boolean notificationBoundaryReady) {
        String id = handoff == null ? "job-unassigned" : clean(handoff.jobId(), "job-unassigned");
        String blueprint = handoff == null ? "Unknown blueprint" : clean(handoff.blueprintName(), "Unknown blueprint");
        String faction = handoff == null ? "Unaffiliated" : clean(handoff.factionName(), "Unaffiliated");
        String action = handoff == null ? "Record review action result" : clean(handoff.actionLine(), "Record review action result");
        boolean readyHandoff = handoff != null && handoff.handoffReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!readyHandoff) blockers.add("response cycle review action handoff blocked");
        if (!commandOutcomeReadable) blockers.add("review action command outcome not readable");
        if (!auditLedgerReady) blockers.add("review action audit ledger not ready");
        if (!rollbackOutcomeReady) blockers.add("review action rollback outcome not ready");
        if (!followupBoundaryReady) blockers.add("review action follow-up boundary not ready");
        if (!notificationBoundaryReady) blockers.add("review action notification boundary not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_REVIEW_ACTION_RESULT_READY" : "RESPONSE_CYCLE_REVIEW_ACTION_RESULT_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle review action result for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; handoffReady=" + readyHandoff
                + ", commandOutcomeReadable=" + commandOutcomeReadable
                + ", auditLedgerReady=" + auditLedgerReady
                + ", rollbackOutcomeReady=" + rollbackOutcomeReady
                + ", followupBoundaryReady=" + followupBoundaryReady
                + ", notificationBoundaryReady=" + notificationBoundaryReady
                + "; blockers=" + blockerLine
                + "; audit only, no review-action result mutation.";
        return new ResponseCycleReviewActionResult(id, blueprint, faction, state, readyHandoff, commandOutcomeReadable,
                auditLedgerReady, rollbackOutcomeReady, followupBoundaryReady, notificationBoundaryReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleReviewActionResult> samples = sampleResults();
        int ready = 0;
        int blocked = 0;
        int handoffBlocked = 0;
        int outcomeBlocked = 0;
        int rollbackBlocked = 0;
        int followupBlocked = 0;
        for (ResponseCycleReviewActionResult result : samples) {
            if (result.resultReady()) ready++;
            else blocked++;
            if (!result.handoffReady()) handoffBlocked++;
            if (!result.commandOutcomeReadable()) outcomeBlocked++;
            if (!result.rollbackOutcomeReady()) rollbackBlocked++;
            if (!result.followupBoundaryReady()) followupBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle review action result audit: owner=BlueprintFactionConstructionResponseCycleReviewActionResultAuthority, handoffOwner=BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority, actionOwner=BlueprintFactionConstructionResponseCycleReviewActionAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle review action result audit: future archived review action results may be recorded only after handoff readiness, readable command outcome, audit ledger readiness, rollback outcome, follow-up boundary, and notification boundary are all declared.",
                "Blueprint faction construction response cycle review action result sample audit: sampleResults=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", handoffBlocked=" + handoffBlocked
                        + ", outcomeBlocked=" + outcomeBlocked
                        + ", rollbackBlocked=" + rollbackBlocked
                        + ", followupBlocked=" + followupBlocked + ".",
                "Blueprint faction construction response cycle review action result examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle review action result rule: a future result owner must show readable command outcome, audit ledger readiness, rollback outcome, follow-up boundary, and notification boundary before recording archived review action results.",
                "Blueprint faction construction response cycle review action result boundary: this audit does not record result rows, execute review actions, reopen cycles, write archives, export evidence, reveal hidden faction data, update status, alter job state, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleReviewActionResultAuditSmoke checks response cycle review action result coverage, handoff and outcome blockers, result readability, future result boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
