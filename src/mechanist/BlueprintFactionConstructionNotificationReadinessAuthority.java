package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only notification readiness contract for future faction construction job updates. */
final class BlueprintFactionConstructionNotificationReadinessAuthority {
    record NotificationReadiness(String jobId, String blueprintName, String factionName, String notificationState,
                                 boolean statusReportReady, boolean severityReadable, boolean audienceDeclared,
                                 boolean deliveryTextReady, boolean dedupeReady, boolean privacyReady,
                                 boolean notificationReady, String severityLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionNotificationReadinessAuthority() { }

    static List<NotificationReadiness> sampleNotifications() {
        List<BlueprintFactionConstructionStatusReportAuthority.StatusReport> reports =
                BlueprintFactionConstructionStatusReportAuthority.sampleReports();
        ArrayList<NotificationReadiness> rows = new ArrayList<>();
        rows.add(notificationFor(reports.get(0), true, true, true, true, true, "notice"));
        rows.add(notificationFor(reports.get(1), true, true, true, true, true, "warning"));
        rows.add(notificationFor(reports.get(2), true, false, false, false, true, "alert"));
        return List.copyOf(rows);
    }

    static NotificationReadiness notificationFor(BlueprintFactionConstructionStatusReportAuthority.StatusReport report,
                                                 boolean severityReadable, boolean audienceDeclared,
                                                 boolean deliveryTextReady, boolean dedupeReady,
                                                 boolean privacyReady, String severityLine) {
        String id = report == null ? "job-unassigned" : clean(report.jobId(), "job-unassigned");
        String blueprint = report == null ? "Unknown blueprint" : clean(report.blueprintName(), "Unknown blueprint");
        String faction = report == null ? "Unaffiliated" : clean(report.factionName(), "Unaffiliated");
        boolean statusReady = report != null && report.reportReady();
        String severity = severityFor(severityLine, statusReady);
        ArrayList<String> blockers = new ArrayList<>();
        if (!statusReady) blockers.add("status report blocked");
        if (!severityReadable) blockers.add("severity not readable");
        if (!audienceDeclared) blockers.add("audience not declared");
        if (!deliveryTextReady) blockers.add("delivery text not ready");
        if (!dedupeReady) blockers.add("dedupe key not ready");
        if (!privacyReady) blockers.add("privacy redaction not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "NOTIFICATION_READY" : "NOTIFICATION_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " notification readiness for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; statusReportReady=" + statusReady
                + ", severityReadable=" + severityReadable
                + ", audienceDeclared=" + audienceDeclared
                + ", deliveryTextReady=" + deliveryTextReady
                + ", dedupeReady=" + dedupeReady
                + ", privacyReady=" + privacyReady
                + "; severity=" + severity
                + "; blockers=" + blockerLine
                + "; audit only, no notification mutation.";
        return new NotificationReadiness(id, blueprint, faction, state, statusReady, severityReadable,
                audienceDeclared, deliveryTextReady, dedupeReady, privacyReady, ready, severity, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<NotificationReadiness> samples = sampleNotifications();
        int ready = 0;
        int blocked = 0;
        int statusBlocked = 0;
        int audienceBlocked = 0;
        int privacyBlocked = 0;
        for (NotificationReadiness notification : samples) {
            if (notification.notificationReady()) ready++;
            else blocked++;
            if (!notification.statusReportReady()) statusBlocked++;
            if (!notification.audienceDeclared()) audienceBlocked++;
            if (!notification.privacyReady()) privacyBlocked++;
        }
        return List.of(
                "Blueprint faction construction notification readiness audit: owner=BlueprintFactionConstructionNotificationReadinessAuthority, statusOwner=BlueprintFactionConstructionStatusReportAuthority, uiOwner=GamePanel, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction notification readiness audit: future UI may alert a construction update only after status report readiness, readable severity, declared audience, delivery text, dedupe key, and privacy redaction are all declared.",
                "Blueprint faction construction notification readiness sample audit: sampleNotifications=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", statusBlocked=" + statusBlocked
                        + ", audienceBlocked=" + audienceBlocked
                        + ", privacyBlocked=" + privacyBlocked + ".",
                "Blueprint faction construction notification readiness examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction notification readiness rule: a future notification owner must show player-safe severity, audience, delivery text, dedupe, and privacy redaction before surfacing faction construction updates.",
                "Blueprint faction construction notification readiness boundary: this audit does not write UI state, enqueue notifications, write job records, reveal hidden faction data, mutate heat, mutate suspicion, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionNotificationReadinessAuditSmoke checks notification readiness coverage, status and audience blockers, privacy redaction, future reporting boundaries, and raw-ID hiding."
        );
    }

    private static String severityFor(String severityLine, boolean statusReady) {
        String value = clean(severityLine, statusReady ? "notice" : "warning").toLowerCase(java.util.Locale.ROOT);
        if (value.contains("alert")) return "alert";
        if (value.contains("warning")) return "warning";
        return "notice";
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
