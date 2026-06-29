package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only execution handoff contract for future faction construction response commands. */
final class BlueprintFactionConstructionResponseExecutionHandoffAuthority {
    record ResponseExecutionHandoff(String jobId, String blueprintName, String factionName, String handoffState,
                                    boolean responseActionReady, boolean targetResolved, boolean commandOwnerDeclared,
                                    boolean rollbackPreviewReady, boolean turnCostPreviewReady, boolean resultTextReady,
                                    boolean handoffReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseExecutionHandoffAuthority() { }

    static List<ResponseExecutionHandoff> sampleHandoffs() {
        List<BlueprintFactionConstructionResponseActionAuthority.ResponseAction> actions =
                BlueprintFactionConstructionResponseActionAuthority.sampleActions();
        ArrayList<ResponseExecutionHandoff> rows = new ArrayList<>();
        rows.add(handoffFor(actions.get(0), true, true, true, true, true));
        rows.add(handoffFor(actions.get(1), true, true, true, true, true));
        rows.add(handoffFor(actions.get(2), false, true, false, true, false));
        return List.copyOf(rows);
    }

    static ResponseExecutionHandoff handoffFor(BlueprintFactionConstructionResponseActionAuthority.ResponseAction action,
                                               boolean targetResolved, boolean commandOwnerDeclared,
                                               boolean rollbackPreviewReady, boolean turnCostPreviewReady,
                                               boolean resultTextReady) {
        String id = action == null ? "job-unassigned" : clean(action.jobId(), "job-unassigned");
        String blueprint = action == null ? "Unknown blueprint" : clean(action.blueprintName(), "Unknown blueprint");
        String faction = action == null ? "Unaffiliated" : clean(action.factionName(), "Unaffiliated");
        String actionLine = action == null ? "Resolve response blockers" : clean(action.actionLine(), "Resolve response blockers");
        boolean responseReady = action != null && action.responseReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!responseReady) blockers.add("response action blocked");
        if (!targetResolved) blockers.add("target not resolved");
        if (!commandOwnerDeclared) blockers.add("command owner not declared");
        if (!rollbackPreviewReady) blockers.add("rollback preview not ready");
        if (!turnCostPreviewReady) blockers.add("turn cost preview not ready");
        if (!resultTextReady) blockers.add("result text not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_EXECUTION_HANDOFF_READY" : "RESPONSE_EXECUTION_HANDOFF_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response execution handoff for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + actionLine
                + "; responseActionReady=" + responseReady
                + ", targetResolved=" + targetResolved
                + ", commandOwnerDeclared=" + commandOwnerDeclared
                + ", rollbackPreviewReady=" + rollbackPreviewReady
                + ", turnCostPreviewReady=" + turnCostPreviewReady
                + ", resultTextReady=" + resultTextReady
                + "; blockers=" + blockerLine
                + "; audit only, no command execution.";
        return new ResponseExecutionHandoff(id, blueprint, faction, state, responseReady, targetResolved,
                commandOwnerDeclared, rollbackPreviewReady, turnCostPreviewReady, resultTextReady,
                ready, actionLine, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseExecutionHandoff> samples = sampleHandoffs();
        int ready = 0;
        int blocked = 0;
        int responseBlocked = 0;
        int targetBlocked = 0;
        int rollbackBlocked = 0;
        int resultTextBlocked = 0;
        for (ResponseExecutionHandoff handoff : samples) {
            if (handoff.handoffReady()) ready++;
            else blocked++;
            if (!handoff.responseActionReady()) responseBlocked++;
            if (!handoff.targetResolved()) targetBlocked++;
            if (!handoff.rollbackPreviewReady()) rollbackBlocked++;
            if (!handoff.resultTextReady()) resultTextBlocked++;
        }
        return List.of(
                "Blueprint faction construction response execution handoff audit: owner=BlueprintFactionConstructionResponseExecutionHandoffAuthority, responseOwner=BlueprintFactionConstructionResponseActionAuthority, commandOwner=GamePanel, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response execution handoff audit: future command execution may run only after response action readiness, target resolution, command owner declaration, rollback preview, turn cost preview, and result text are all declared.",
                "Blueprint faction construction response execution handoff sample audit: sampleHandoffs=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", responseBlocked=" + responseBlocked
                        + ", targetBlocked=" + targetBlocked
                        + ", rollbackBlocked=" + rollbackBlocked
                        + ", resultTextBlocked=" + resultTextBlocked + ".",
                "Blueprint faction construction response execution handoff examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response execution handoff rule: a future command owner must show target, command owner, rollback preview, turn cost, and result text before executing construction response commands.",
                "Blueprint faction construction response execution handoff boundary: this audit does not execute commands, inspect sites, resolve blockers, pause jobs, cancel jobs, write UI state, mutate heat, mutate suspicion, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseExecutionHandoffAuditSmoke checks response execution handoff coverage, target and rollback blockers, command result text, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
