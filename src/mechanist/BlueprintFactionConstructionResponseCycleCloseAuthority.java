package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only close decision contract for future faction construction response cycles. */
final class BlueprintFactionConstructionResponseCycleCloseAuthority {
    record ResponseCycleClose(String jobId, String blueprintName, String factionName, String cycleState,
                              boolean followupReady, boolean cycleDecisionReadable,
                              boolean statusReturnDeclared, boolean notificationReturnDeclared,
                              boolean unresolvedBlockersReadable, boolean archiveBoundaryReady,
                              boolean closeReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleCloseAuthority() { }

    static List<ResponseCycleClose> sampleClosures() {
        List<BlueprintFactionConstructionResponseFollowupAuthority.ResponseFollowup> followups =
                BlueprintFactionConstructionResponseFollowupAuthority.sampleFollowups();
        ArrayList<ResponseCycleClose> rows = new ArrayList<>();
        rows.add(closeFor(followups.get(0), true, true, true, true, true));
        rows.add(closeFor(followups.get(1), true, true, true, true, true));
        rows.add(closeFor(followups.get(2), false, false, true, true, false));
        return List.copyOf(rows);
    }

    static ResponseCycleClose closeFor(BlueprintFactionConstructionResponseFollowupAuthority.ResponseFollowup followup,
                                       boolean cycleDecisionReadable, boolean statusReturnDeclared,
                                       boolean notificationReturnDeclared, boolean unresolvedBlockersReadable,
                                       boolean archiveBoundaryReady) {
        String id = followup == null ? "job-unassigned" : clean(followup.jobId(), "job-unassigned");
        String blueprint = followup == null ? "Unknown blueprint" : clean(followup.blueprintName(), "Unknown blueprint");
        String faction = followup == null ? "Unaffiliated" : clean(followup.factionName(), "Unaffiliated");
        String action = followup == null ? "Close response cycle" : clean(followup.actionLine(), "Close response cycle");
        boolean followupReady = followup != null && followup.followupReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!followupReady) blockers.add("response follow-up blocked");
        if (!cycleDecisionReadable) blockers.add("cycle decision not readable");
        if (!statusReturnDeclared) blockers.add("status return not declared");
        if (!notificationReturnDeclared) blockers.add("notification return not declared");
        if (!unresolvedBlockersReadable) blockers.add("unresolved blockers not readable");
        if (!archiveBoundaryReady) blockers.add("archive boundary not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_CLOSE_READY" : "RESPONSE_CYCLE_CLOSE_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle close for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; followupReady=" + followupReady
                + ", cycleDecisionReadable=" + cycleDecisionReadable
                + ", statusReturnDeclared=" + statusReturnDeclared
                + ", notificationReturnDeclared=" + notificationReturnDeclared
                + ", unresolvedBlockersReadable=" + unresolvedBlockersReadable
                + ", archiveBoundaryReady=" + archiveBoundaryReady
                + "; blockers=" + blockerLine
                + "; audit only, no cycle-close mutation.";
        return new ResponseCycleClose(id, blueprint, faction, state, followupReady, cycleDecisionReadable,
                statusReturnDeclared, notificationReturnDeclared, unresolvedBlockersReadable, archiveBoundaryReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleClose> samples = sampleClosures();
        int ready = 0;
        int blocked = 0;
        int followupBlocked = 0;
        int decisionBlocked = 0;
        int statusBlocked = 0;
        int archiveBlocked = 0;
        for (ResponseCycleClose close : samples) {
            if (close.closeReady()) ready++;
            else blocked++;
            if (!close.followupReady()) followupBlocked++;
            if (!close.cycleDecisionReadable()) decisionBlocked++;
            if (!close.statusReturnDeclared()) statusBlocked++;
            if (!close.archiveBoundaryReady()) archiveBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle close audit: owner=BlueprintFactionConstructionResponseCycleCloseAuthority, followupOwner=BlueprintFactionConstructionResponseFollowupAuthority, statusOwner=BlueprintFactionConstructionStatusReportAuthority, notificationOwner=BlueprintFactionConstructionNotificationReadinessAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle close audit: future response cycles may be closed only after follow-up readiness, readable cycle decision, status return declaration, notification return declaration, readable unresolved blockers, and archive boundary are all declared.",
                "Blueprint faction construction response cycle close sample audit: sampleClosures=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", followupBlocked=" + followupBlocked
                        + ", decisionBlocked=" + decisionBlocked
                        + ", statusBlocked=" + statusBlocked
                        + ", archiveBlocked=" + archiveBlocked + ".",
                "Blueprint faction construction response cycle close examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle close rule: a future cycle owner must show readable cycle decision, status return declaration, notification return declaration, readable unresolved blockers, and archive boundary before closing or returning a construction response cycle.",
                "Blueprint faction construction response cycle close boundary: this audit does not close response cycles, schedule status returns, enqueue notifications, archive decisions, alter job state, mutate heat, mutate suspicion, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleCloseAuditSmoke checks response cycle close coverage, follow-up and decision blockers, close readability, future closure boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
