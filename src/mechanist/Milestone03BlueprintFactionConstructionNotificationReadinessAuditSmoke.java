package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction notification readiness contracts. */
final class Milestone03BlueprintFactionConstructionNotificationReadinessAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionNotificationReadinessAuthority.NotificationReadiness> samples =
                BlueprintFactionConstructionNotificationReadinessAuthority.sampleNotifications();
        List<String> audit = BlueprintFactionConstructionNotificationReadinessAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample notifications");
        requireContains(audit, "owner=BlueprintFactionConstructionNotificationReadinessAuthority", "notification owner");
        requireContains(audit, "statusOwner=BlueprintFactionConstructionStatusReportAuthority", "status owner");
        requireContains(audit, "uiOwner=GamePanel", "ui owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "status report readiness, readable severity, declared audience", "notification gates");
        requireContains(audit, "sampleNotifications=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "statusBlocked=2", "status blocked count");
        requireContains(audit, "audienceBlocked=1", "audience blocked count");
        requireContains(audit, "privacyBlocked=0", "privacy blocked count");
        requireContains(audit, "job-storage-public notification readiness for Storage Crate", "storage notification");
        requireContains(audit, "state=NOTIFICATION_READY", "ready state");
        requireContains(audit, "severity=notice", "notice severity");
        requireContains(audit, "job-sensor-restricted notification readiness for Security Sensor Mast", "sensor notification");
        requireContains(audit, "status report blocked", "status blocker");
        requireContains(audit, "job-shop-public notification readiness for Licensed Shop Counter", "shop notification");
        requireContains(audit, "audience not declared", "audience blocker");
        requireContains(audit, "player-safe severity, audience, delivery text, dedupe, and privacy redaction", "readability rule");
        requireContains(audit, "does not write UI state", "ui mutation boundary");
        requireContains(audit, "enqueue notifications", "notification mutation boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionNotificationReadinessAuditSmoke", "guard reference");

        BlueprintFactionConstructionExecutionHandoffAuthority.ExecutionHandoff readyHandoff =
                BlueprintFactionConstructionExecutionHandoffAuthority.handoffFor(
                        BlueprintFactionConstructionJobDefinitionAuthority.definitionFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), "AUTHORIZED", "storage alcove"),
                        BlueprintFactionConstructionMaterialReservationAuthority.reservationFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), Map.of("Construction supplies", 4, "Rivet set", 2)),
                        BlueprintFactionConstructionCrewAssignmentAuthority.assignmentFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), 1),
                        BlueprintFactionConstructionSiteReadinessAuthority.readinessFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), "storage alcove", true, true, true, true, true),
                        BlueprintFactionConstructionBudgetHeatAuthorizationAuthority.authorizationFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), 100, 20, 20),
                        BlueprintFactionConstructionCancellationReleaseAuthority.releaseFor("job-direct", Faction.HIVER,
                                BuildRecipe.storage(), "FAILED", "rollback predeclared", true, true, true, true, true));
        BlueprintFactionConstructionQueueAdmissionAuthority.QueueAdmission admission =
                BlueprintFactionConstructionQueueAdmissionAuthority.admissionFor(readyHandoff, 8, true);
        BlueprintFactionConstructionReservationLedgerAuthority.ReservationLedger ledger =
                BlueprintFactionConstructionReservationLedgerAuthority.ledgerFor(admission, true, true, true, true, true);
        BlueprintFactionConstructionStagedHandoffAuthority.StagedHandoff handoff =
                BlueprintFactionConstructionStagedHandoffAuthority.handoffFor(ledger, true, true, true, true);
        BlueprintFactionConstructionPlacementOutcomeAuthority.PlacementOutcome outcome =
                BlueprintFactionConstructionPlacementOutcomeAuthority.outcomeFor(handoff, true, true, true, true);
        BlueprintFactionConstructionProgressTickAuthority.ProgressTick tick =
                BlueprintFactionConstructionProgressTickAuthority.tickFor(outcome, true, true, true, true, true, 3, 1, 4);
        BlueprintFactionConstructionCompletionReadinessAuthority.CompletionReadiness completion =
                BlueprintFactionConstructionCompletionReadinessAuthority.readinessFor(tick, true, true, true, true, true);
        BlueprintFactionConstructionJobCloseoutAuthority.JobCloseout closeout =
                BlueprintFactionConstructionJobCloseoutAuthority.closeoutFor(completion, true, true, true, true, true, true);
        BlueprintFactionConstructionStatusReportAuthority.StatusReport report =
                BlueprintFactionConstructionStatusReportAuthority.reportFor(closeout, true, true, true, true, true,
                        "Review completed construction");
        BlueprintFactionConstructionNotificationReadinessAuthority.NotificationReadiness direct =
                BlueprintFactionConstructionNotificationReadinessAuthority.notificationFor(report, true, true, true, true, true, "notice");
        require(direct.notificationReady(), "direct notification should be ready");
        requireContains(direct.boundaryLine(), "audit only, no notification mutation", "direct boundary");

        for (BlueprintFactionConstructionNotificationReadinessAuthority.NotificationReadiness sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.notificationState(), "notification state");
            requireNotBlank(sample.severityLine(), "severity line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction notification readiness audit leaked implementation text: " + line);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireNotBlank(String value, String label) {
        require(value != null && !value.isBlank(), "expected nonblank " + label);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03BlueprintFactionConstructionNotificationReadinessAuditSmoke() { }
}
