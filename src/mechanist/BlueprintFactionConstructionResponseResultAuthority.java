package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only result record contract for future faction construction response commands. */
final class BlueprintFactionConstructionResponseResultAuthority {
    record ResponseResult(String jobId, String blueprintName, String factionName, String resultState,
                          boolean executionHandoffReady, boolean commandOutcomeReadable, boolean auditLedgerReady,
                          boolean rollbackOutcomeReady, boolean followupStatusReady, boolean notificationRefreshReady,
                          boolean resultReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseResultAuthority() { }

    static List<ResponseResult> sampleResults() {
        List<BlueprintFactionConstructionResponseExecutionHandoffAuthority.ResponseExecutionHandoff> handoffs =
                BlueprintFactionConstructionResponseExecutionHandoffAuthority.sampleHandoffs();
        ArrayList<ResponseResult> rows = new ArrayList<>();
        rows.add(resultFor(handoffs.get(0), true, true, true, true, true));
        rows.add(resultFor(handoffs.get(1), true, true, true, true, true));
        rows.add(resultFor(handoffs.get(2), false, true, false, false, false));
        return List.copyOf(rows);
    }

    static ResponseResult resultFor(BlueprintFactionConstructionResponseExecutionHandoffAuthority.ResponseExecutionHandoff handoff,
                                    boolean commandOutcomeReadable, boolean auditLedgerReady,
                                    boolean rollbackOutcomeReady, boolean followupStatusReady,
                                    boolean notificationRefreshReady) {
        String id = handoff == null ? "job-unassigned" : clean(handoff.jobId(), "job-unassigned");
        String blueprint = handoff == null ? "Unknown blueprint" : clean(handoff.blueprintName(), "Unknown blueprint");
        String faction = handoff == null ? "Unaffiliated" : clean(handoff.factionName(), "Unaffiliated");
        String action = handoff == null ? "Resolve response blockers" : clean(handoff.actionLine(), "Resolve response blockers");
        boolean handoffReady = handoff != null && handoff.handoffReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!handoffReady) blockers.add("execution handoff blocked");
        if (!commandOutcomeReadable) blockers.add("command outcome not readable");
        if (!auditLedgerReady) blockers.add("audit ledger not ready");
        if (!rollbackOutcomeReady) blockers.add("rollback outcome not ready");
        if (!followupStatusReady) blockers.add("follow-up status not ready");
        if (!notificationRefreshReady) blockers.add("notification refresh not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_RESULT_READY" : "RESPONSE_RESULT_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response result for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; executionHandoffReady=" + handoffReady
                + ", commandOutcomeReadable=" + commandOutcomeReadable
                + ", auditLedgerReady=" + auditLedgerReady
                + ", rollbackOutcomeReady=" + rollbackOutcomeReady
                + ", followupStatusReady=" + followupStatusReady
                + ", notificationRefreshReady=" + notificationRefreshReady
                + "; blockers=" + blockerLine
                + "; audit only, no result mutation.";
        return new ResponseResult(id, blueprint, faction, state, handoffReady, commandOutcomeReadable,
                auditLedgerReady, rollbackOutcomeReady, followupStatusReady, notificationRefreshReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseResult> samples = sampleResults();
        int ready = 0;
        int blocked = 0;
        int handoffBlocked = 0;
        int outcomeBlocked = 0;
        int rollbackBlocked = 0;
        int notificationBlocked = 0;
        for (ResponseResult result : samples) {
            if (result.resultReady()) ready++;
            else blocked++;
            if (!result.executionHandoffReady()) handoffBlocked++;
            if (!result.commandOutcomeReadable()) outcomeBlocked++;
            if (!result.rollbackOutcomeReady()) rollbackBlocked++;
            if (!result.notificationRefreshReady()) notificationBlocked++;
        }
        return List.of(
                "Blueprint faction construction response result audit: owner=BlueprintFactionConstructionResponseResultAuthority, handoffOwner=BlueprintFactionConstructionResponseExecutionHandoffAuthority, statusOwner=BlueprintFactionConstructionStatusReportAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response result audit: future command results may be recorded only after execution handoff readiness, readable command outcome, audit ledger readiness, rollback outcome, follow-up status, and notification refresh are all declared.",
                "Blueprint faction construction response result sample audit: sampleResults=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", handoffBlocked=" + handoffBlocked
                        + ", outcomeBlocked=" + outcomeBlocked
                        + ", rollbackBlocked=" + rollbackBlocked
                        + ", notificationBlocked=" + notificationBlocked + ".",
                "Blueprint faction construction response result examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response result rule: a future result owner must show readable command outcome, audit ledger, rollback outcome, follow-up status, and notification refresh before recording construction response results.",
                "Blueprint faction construction response result boundary: this audit does not write result records, execute commands, alter job status, refresh notifications, mutate heat, mutate suspicion, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseResultAuditSmoke checks response result coverage, handoff and rollback blockers, result readability, future record boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
