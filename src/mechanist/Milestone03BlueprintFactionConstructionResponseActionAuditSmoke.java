package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction response action readiness contracts. */
final class Milestone03BlueprintFactionConstructionResponseActionAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseActionAuthority.ResponseAction> samples =
                BlueprintFactionConstructionResponseActionAuthority.sampleActions();
        List<String> audit = BlueprintFactionConstructionResponseActionAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response actions");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseActionAuthority", "response action owner");
        requireContains(audit, "notificationOwner=BlueprintFactionConstructionNotificationReadinessAuthority", "notification owner");
        requireContains(audit, "uiOwner=GamePanel", "ui owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "inspect, resolve blockers, pause, or cancel response commands", "response command set");
        requireContains(audit, "sampleActions=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "notificationBlocked=2", "notification blocked count");
        requireContains(audit, "labelBlocked=1", "label blocked count");
        requireContains(audit, "permissionBlocked=1", "permission blocked count");
        requireContains(audit, "cooldownBlocked=1", "cooldown blocked count");
        requireContains(audit, "job-storage-public response action readiness for Storage Crate", "storage action");
        requireContains(audit, "state=RESPONSE_ACTION_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response action readiness for Security Sensor Mast", "sensor action");
        requireContains(audit, "notification readiness blocked", "notification blocker");
        requireContains(audit, "job-shop-public response action readiness for Licensed Shop Counter", "shop action");
        requireContains(audit, "permission check not ready", "permission blocker");
        requireContains(audit, "readable action labels, permission reasons, safety prompts, cooldown state, and audit text", "readability rule");
        requireContains(audit, "does not write UI state", "ui mutation boundary");
        requireContains(audit, "execute commands", "command mutation boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseActionAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionNotificationReadinessAuthority.NotificationReadiness notification =
                BlueprintFactionConstructionNotificationReadinessAuthority.notificationFor(report, true, true, true, true, true, "notice");
        BlueprintFactionConstructionResponseActionAuthority.ResponseAction direct =
                BlueprintFactionConstructionResponseActionAuthority.actionFor(notification, true, true, true, true, true,
                        "Inspect completed site");
        require(direct.responseReady(), "direct response action should be ready");
        requireContains(direct.boundaryLine(), "audit only, no response action mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseActionAuthority.ResponseAction sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.actionState(), "action state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response action audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseActionAuditSmoke() { }
}
