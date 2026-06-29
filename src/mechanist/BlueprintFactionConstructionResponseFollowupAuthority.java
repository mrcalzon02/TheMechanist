package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only follow-up contract after future faction construction response results. */
final class BlueprintFactionConstructionResponseFollowupAuthority {
    record ResponseFollowup(String jobId, String blueprintName, String factionName, String followupState,
                            boolean responseResultReady, boolean continuationIntentReadable,
                            boolean statusCycleReady, boolean notificationCycleReady,
                            boolean closeoutConsequenceReady, boolean rollbackConsequenceReady,
                            boolean followupReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseFollowupAuthority() { }

    static List<ResponseFollowup> sampleFollowups() {
        List<BlueprintFactionConstructionResponseResultAuthority.ResponseResult> results =
                BlueprintFactionConstructionResponseResultAuthority.sampleResults();
        ArrayList<ResponseFollowup> rows = new ArrayList<>();
        rows.add(followupFor(results.get(0), true, true, true, true, true));
        rows.add(followupFor(results.get(1), true, true, true, true, true));
        rows.add(followupFor(results.get(2), false, false, false, false, true));
        return List.copyOf(rows);
    }

    static ResponseFollowup followupFor(BlueprintFactionConstructionResponseResultAuthority.ResponseResult result,
                                        boolean continuationIntentReadable, boolean statusCycleReady,
                                        boolean notificationCycleReady, boolean closeoutConsequenceReady,
                                        boolean rollbackConsequenceReady) {
        String id = result == null ? "job-unassigned" : clean(result.jobId(), "job-unassigned");
        String blueprint = result == null ? "Unknown blueprint" : clean(result.blueprintName(), "Unknown blueprint");
        String faction = result == null ? "Unaffiliated" : clean(result.factionName(), "Unaffiliated");
        String action = result == null ? "Review response result" : clean(result.actionLine(), "Review response result");
        boolean resultReady = result != null && result.resultReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!resultReady) blockers.add("response result blocked");
        if (!continuationIntentReadable) blockers.add("continuation intent not readable");
        if (!statusCycleReady) blockers.add("status cycle not ready");
        if (!notificationCycleReady) blockers.add("notification cycle not ready");
        if (!closeoutConsequenceReady) blockers.add("closeout consequence not ready");
        if (!rollbackConsequenceReady) blockers.add("rollback consequence not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_FOLLOWUP_READY" : "RESPONSE_FOLLOWUP_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response follow-up for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; responseResultReady=" + resultReady
                + ", continuationIntentReadable=" + continuationIntentReadable
                + ", statusCycleReady=" + statusCycleReady
                + ", notificationCycleReady=" + notificationCycleReady
                + ", closeoutConsequenceReady=" + closeoutConsequenceReady
                + ", rollbackConsequenceReady=" + rollbackConsequenceReady
                + "; blockers=" + blockerLine
                + "; audit only, no follow-up mutation.";
        return new ResponseFollowup(id, blueprint, faction, state, resultReady, continuationIntentReadable,
                statusCycleReady, notificationCycleReady, closeoutConsequenceReady, rollbackConsequenceReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseFollowup> samples = sampleFollowups();
        int ready = 0;
        int blocked = 0;
        int resultBlocked = 0;
        int continuationBlocked = 0;
        int statusBlocked = 0;
        int notificationBlocked = 0;
        int closeoutBlocked = 0;
        for (ResponseFollowup followup : samples) {
            if (followup.followupReady()) ready++;
            else blocked++;
            if (!followup.responseResultReady()) resultBlocked++;
            if (!followup.continuationIntentReadable()) continuationBlocked++;
            if (!followup.statusCycleReady()) statusBlocked++;
            if (!followup.notificationCycleReady()) notificationBlocked++;
            if (!followup.closeoutConsequenceReady()) closeoutBlocked++;
        }
        return List.of(
                "Blueprint faction construction response follow-up audit: owner=BlueprintFactionConstructionResponseFollowupAuthority, resultOwner=BlueprintFactionConstructionResponseResultAuthority, statusOwner=BlueprintFactionConstructionStatusReportAuthority, notificationOwner=BlueprintFactionConstructionNotificationReadinessAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response follow-up audit: future follow-up decisions may be scheduled only after response result readiness, readable continuation intent, status cycle readiness, notification refresh readiness, closeout consequence, and rollback consequence are all declared.",
                "Blueprint faction construction response follow-up sample audit: sampleFollowups=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", resultBlocked=" + resultBlocked
                        + ", continuationBlocked=" + continuationBlocked
                        + ", statusBlocked=" + statusBlocked
                        + ", notificationBlocked=" + notificationBlocked
                        + ", closeoutBlocked=" + closeoutBlocked + ".",
                "Blueprint faction construction response follow-up examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response follow-up rule: a future follow-up owner must show readable continuation intent, status cycle readiness, notification refresh readiness, closeout consequence, and rollback consequence before scheduling another construction response cycle.",
                "Blueprint faction construction response follow-up boundary: this audit does not schedule follow-up actions, write status reports, enqueue notifications, close jobs, roll back results, mutate heat, mutate suspicion, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseFollowupAuditSmoke checks response follow-up coverage, result and continuation blockers, follow-up readability, future scheduling boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
