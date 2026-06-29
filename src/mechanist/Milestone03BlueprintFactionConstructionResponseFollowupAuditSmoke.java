package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction response follow-up contracts. */
final class Milestone03BlueprintFactionConstructionResponseFollowupAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseFollowupAuthority.ResponseFollowup> samples =
                BlueprintFactionConstructionResponseFollowupAuthority.sampleFollowups();
        List<String> audit = BlueprintFactionConstructionResponseFollowupAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response follow-ups");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseFollowupAuthority", "follow-up owner");
        requireContains(audit, "resultOwner=BlueprintFactionConstructionResponseResultAuthority", "result owner");
        requireContains(audit, "statusOwner=BlueprintFactionConstructionStatusReportAuthority", "status owner");
        requireContains(audit, "notificationOwner=BlueprintFactionConstructionNotificationReadinessAuthority", "notification owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "response result readiness, readable continuation intent, status cycle readiness", "follow-up gates");
        requireContains(audit, "sampleFollowups=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "resultBlocked=2", "result blocked count");
        requireContains(audit, "continuationBlocked=1", "continuation blocked count");
        requireContains(audit, "statusBlocked=1", "status blocked count");
        requireContains(audit, "notificationBlocked=1", "notification blocked count");
        requireContains(audit, "closeoutBlocked=1", "closeout blocked count");
        requireContains(audit, "job-storage-public response follow-up for Storage Crate", "storage follow-up");
        requireContains(audit, "state=RESPONSE_FOLLOWUP_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response follow-up for Security Sensor Mast", "sensor follow-up");
        requireContains(audit, "response result blocked", "result blocker");
        requireContains(audit, "job-shop-public response follow-up for Licensed Shop Counter", "shop follow-up");
        requireContains(audit, "continuation intent not readable", "continuation blocker");
        requireContains(audit, "readable continuation intent, status cycle readiness, notification refresh readiness, closeout consequence, and rollback consequence", "readability rule");
        requireContains(audit, "does not schedule follow-up actions", "schedule boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseFollowupAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionResponseFollowupAuthority.ResponseFollowup direct =
                BlueprintFactionConstructionResponseFollowupAuthority.followupFor(result, true, true, true, true, true);
        require(direct.followupReady(), "direct response follow-up should be ready");
        requireContains(direct.boundaryLine(), "audit only, no follow-up mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseFollowupAuthority.ResponseFollowup sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.followupState(), "follow-up state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response follow-up audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseFollowupAuditSmoke() { }
}
