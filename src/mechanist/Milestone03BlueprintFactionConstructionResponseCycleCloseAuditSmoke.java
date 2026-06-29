package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction response cycle close contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleCloseAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleCloseAuthority.ResponseCycleClose> samples =
                BlueprintFactionConstructionResponseCycleCloseAuthority.sampleClosures();
        List<String> audit = BlueprintFactionConstructionResponseCycleCloseAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle closures");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleCloseAuthority", "cycle close owner");
        requireContains(audit, "followupOwner=BlueprintFactionConstructionResponseFollowupAuthority", "follow-up owner");
        requireContains(audit, "statusOwner=BlueprintFactionConstructionStatusReportAuthority", "status owner");
        requireContains(audit, "notificationOwner=BlueprintFactionConstructionNotificationReadinessAuthority", "notification owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "follow-up readiness, readable cycle decision, status return declaration", "cycle close gates");
        requireContains(audit, "sampleClosures=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "followupBlocked=2", "follow-up blocked count");
        requireContains(audit, "decisionBlocked=1", "decision blocked count");
        requireContains(audit, "statusBlocked=1", "status blocked count");
        requireContains(audit, "archiveBlocked=1", "archive blocked count");
        requireContains(audit, "job-storage-public response cycle close for Storage Crate", "storage cycle close");
        requireContains(audit, "state=RESPONSE_CYCLE_CLOSE_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle close for Security Sensor Mast", "sensor cycle close");
        requireContains(audit, "response follow-up blocked", "follow-up blocker");
        requireContains(audit, "job-shop-public response cycle close for Licensed Shop Counter", "shop cycle close");
        requireContains(audit, "cycle decision not readable", "decision blocker");
        requireContains(audit, "readable cycle decision, status return declaration, notification return declaration, readable unresolved blockers, and archive boundary", "readability rule");
        requireContains(audit, "does not close response cycles", "close boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleCloseAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionStagedHandoffAuthority.StagedHandoff staged =
                BlueprintFactionConstructionStagedHandoffAuthority.handoffFor(ledger, true, true, true, true);
        BlueprintFactionConstructionPlacementOutcomeAuthority.PlacementOutcome outcome =
                BlueprintFactionConstructionPlacementOutcomeAuthority.outcomeFor(staged, true, true, true, true);
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
        BlueprintFactionConstructionResponseActionAuthority.ResponseAction action =
                BlueprintFactionConstructionResponseActionAuthority.actionFor(notification, true, true, true, true, true,
                        "Inspect completed site");
        BlueprintFactionConstructionResponseExecutionHandoffAuthority.ResponseExecutionHandoff handoff =
                BlueprintFactionConstructionResponseExecutionHandoffAuthority.handoffFor(action, true, true, true, true, true);
        BlueprintFactionConstructionResponseResultAuthority.ResponseResult result =
                BlueprintFactionConstructionResponseResultAuthority.resultFor(handoff, true, true, true, true, true);
        BlueprintFactionConstructionResponseFollowupAuthority.ResponseFollowup followup =
                BlueprintFactionConstructionResponseFollowupAuthority.followupFor(result, true, true, true, true, true);
        BlueprintFactionConstructionResponseCycleCloseAuthority.ResponseCycleClose direct =
                BlueprintFactionConstructionResponseCycleCloseAuthority.closeFor(followup, true, true, true, true, true);
        require(direct.closeReady(), "direct response cycle close should be ready");
        requireContains(direct.boundaryLine(), "audit only, no cycle-close mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleCloseAuthority.ResponseCycleClose sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.cycleState(), "cycle state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response cycle close audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleCloseAuditSmoke() { }
}
