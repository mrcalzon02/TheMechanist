package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only response action readiness contract for future faction construction job updates. */
final class BlueprintFactionConstructionResponseActionAuthority {
    record ResponseAction(String jobId, String blueprintName, String factionName, String actionState,
                          boolean notificationReady, boolean actionLabelsReadable, boolean permissionReady,
                          boolean safetyConfirmationReady, boolean cooldownReady, boolean auditTextReady,
                          boolean responseReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseActionAuthority() { }

    static List<ResponseAction> sampleActions() {
        List<BlueprintFactionConstructionNotificationReadinessAuthority.NotificationReadiness> notifications =
                BlueprintFactionConstructionNotificationReadinessAuthority.sampleNotifications();
        ArrayList<ResponseAction> rows = new ArrayList<>();
        rows.add(actionFor(notifications.get(0), true, true, true, true, true, "Inspect completed site"));
        rows.add(actionFor(notifications.get(1), true, true, true, true, true, "Resolve completion blockers"));
        rows.add(actionFor(notifications.get(2), false, false, true, false, true, "Repair notification packet"));
        return List.copyOf(rows);
    }

    static ResponseAction actionFor(BlueprintFactionConstructionNotificationReadinessAuthority.NotificationReadiness notification,
                                    boolean actionLabelsReadable, boolean permissionReady,
                                    boolean safetyConfirmationReady, boolean cooldownReady,
                                    boolean auditTextReady, String actionLine) {
        String id = notification == null ? "job-unassigned" : clean(notification.jobId(), "job-unassigned");
        String blueprint = notification == null ? "Unknown blueprint" : clean(notification.blueprintName(), "Unknown blueprint");
        String faction = notification == null ? "Unaffiliated" : clean(notification.factionName(), "Unaffiliated");
        boolean noticeReady = notification != null && notification.notificationReady();
        String action = clean(actionLine, noticeReady ? "Inspect construction update" : "Resolve response blockers");
        ArrayList<String> blockers = new ArrayList<>();
        if (!noticeReady) blockers.add("notification readiness blocked");
        if (!actionLabelsReadable) blockers.add("action labels not readable");
        if (!permissionReady) blockers.add("permission check not ready");
        if (!safetyConfirmationReady) blockers.add("safety confirmation not ready");
        if (!cooldownReady) blockers.add("cooldown state not ready");
        if (!auditTextReady) blockers.add("audit text not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_ACTION_READY" : "RESPONSE_ACTION_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response action readiness for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; notificationReady=" + noticeReady
                + ", actionLabelsReadable=" + actionLabelsReadable
                + ", permissionReady=" + permissionReady
                + ", safetyConfirmationReady=" + safetyConfirmationReady
                + ", cooldownReady=" + cooldownReady
                + ", auditTextReady=" + auditTextReady
                + "; action=" + action
                + "; blockers=" + blockerLine
                + "; audit only, no response action mutation.";
        return new ResponseAction(id, blueprint, faction, state, noticeReady, actionLabelsReadable,
                permissionReady, safetyConfirmationReady, cooldownReady, auditTextReady, ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseAction> samples = sampleActions();
        int ready = 0;
        int blocked = 0;
        int notificationBlocked = 0;
        int labelBlocked = 0;
        int permissionBlocked = 0;
        int cooldownBlocked = 0;
        for (ResponseAction action : samples) {
            if (action.responseReady()) ready++;
            else blocked++;
            if (!action.notificationReady()) notificationBlocked++;
            if (!action.actionLabelsReadable()) labelBlocked++;
            if (!action.permissionReady()) permissionBlocked++;
            if (!action.cooldownReady()) cooldownBlocked++;
        }
        return List.of(
                "Blueprint faction construction response action audit: owner=BlueprintFactionConstructionResponseActionAuthority, notificationOwner=BlueprintFactionConstructionNotificationReadinessAuthority, uiOwner=GamePanel, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response action audit: future UI may offer inspect, resolve blockers, pause, or cancel response commands only after notification readiness, readable action labels, permission checks, safety confirmation, cooldown state, and audit text are all declared.",
                "Blueprint faction construction response action sample audit: sampleActions=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", notificationBlocked=" + notificationBlocked
                        + ", labelBlocked=" + labelBlocked
                        + ", permissionBlocked=" + permissionBlocked
                        + ", cooldownBlocked=" + cooldownBlocked + ".",
                "Blueprint faction construction response action examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response action rule: a future command owner must show readable action labels, permission reasons, safety prompts, cooldown state, and audit text before making construction response buttons actionable.",
                "Blueprint faction construction response action boundary: this audit does not write UI state, execute commands, pause jobs, cancel jobs, assign crew, release reservations, mutate heat, mutate suspicion, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseActionAuditSmoke checks response action coverage, notification and permission blockers, readable action text, future command boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
