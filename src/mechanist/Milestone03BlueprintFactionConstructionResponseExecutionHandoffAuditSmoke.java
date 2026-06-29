package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction response execution handoff contracts. */
final class Milestone03BlueprintFactionConstructionResponseExecutionHandoffAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseExecutionHandoffAuthority.ResponseExecutionHandoff> samples =
                BlueprintFactionConstructionResponseExecutionHandoffAuthority.sampleHandoffs();
        List<String> audit = BlueprintFactionConstructionResponseExecutionHandoffAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response execution handoffs");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseExecutionHandoffAuthority", "handoff owner");
        requireContains(audit, "responseOwner=BlueprintFactionConstructionResponseActionAuthority", "response owner");
        requireContains(audit, "commandOwner=GamePanel", "command owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "response action readiness, target resolution, command owner declaration", "handoff gates");
        requireContains(audit, "sampleHandoffs=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "responseBlocked=2", "response blocked count");
        requireContains(audit, "targetBlocked=1", "target blocked count");
        requireContains(audit, "rollbackBlocked=1", "rollback blocked count");
        requireContains(audit, "resultTextBlocked=1", "result text blocked count");
        requireContains(audit, "job-storage-public response execution handoff for Storage Crate", "storage handoff");
        requireContains(audit, "state=RESPONSE_EXECUTION_HANDOFF_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response execution handoff for Security Sensor Mast", "sensor handoff");
        requireContains(audit, "response action blocked", "response blocker");
        requireContains(audit, "job-shop-public response execution handoff for Licensed Shop Counter", "shop handoff");
        requireContains(audit, "target not resolved", "target blocker");
        requireContains(audit, "rollback preview not ready", "rollback blocker");
        requireContains(audit, "target, command owner, rollback preview, turn cost, and result text", "readability rule");
        requireContains(audit, "does not execute commands", "execution boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseExecutionHandoffAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionResponseExecutionHandoffAuthority.ResponseExecutionHandoff direct =
                BlueprintFactionConstructionResponseExecutionHandoffAuthority.handoffFor(action, true, true, true, true, true);
        require(direct.handoffReady(), "direct response execution handoff should be ready");
        requireContains(direct.boundaryLine(), "audit only, no command execution", "direct boundary");

        for (BlueprintFactionConstructionResponseExecutionHandoffAuthority.ResponseExecutionHandoff sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.handoffState(), "handoff state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response execution handoff audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseExecutionHandoffAuditSmoke() { }
}
