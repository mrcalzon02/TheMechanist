package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction response cycle readback contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleReadbackAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleReadbackAuthority.ResponseCycleReadback> samples =
                BlueprintFactionConstructionResponseCycleReadbackAuthority.sampleReadbacks();
        List<String> audit = BlueprintFactionConstructionResponseCycleReadbackAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle readbacks");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleReadbackAuthority", "readback owner");
        requireContains(audit, "archiveOwner=BlueprintFactionConstructionResponseCycleArchiveAuthority", "archive owner");
        requireContains(audit, "closeOwner=BlueprintFactionConstructionResponseCycleCloseAuthority", "close owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "archive readiness, archive lookup readiness, readable summary", "readback gates");
        requireContains(audit, "sampleReadbacks=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "archiveBlocked=2", "archive blocked count");
        requireContains(audit, "lookupBlocked=1", "lookup blocked count");
        requireContains(audit, "privacyBlocked=1", "privacy blocked count");
        requireContains(audit, "replayBlocked=1", "replay blocked count");
        requireContains(audit, "job-storage-public response cycle readback for Storage Crate", "storage readback");
        requireContains(audit, "state=RESPONSE_CYCLE_READBACK_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle readback for Security Sensor Mast", "sensor readback");
        requireContains(audit, "response cycle archive blocked", "archive blocker");
        requireContains(audit, "job-shop-public response cycle readback for Licensed Shop Counter", "shop readback");
        requireContains(audit, "archive lookup not ready", "lookup blocker");
        requireContains(audit, "archive lookup readiness, readable summary, privacy filter readiness, readable replay reference, and stale record marker", "readability rule");
        requireContains(audit, "does not read archives from storage", "storage boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleReadbackAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionResponseCycleCloseAuthority.ResponseCycleClose close =
                BlueprintFactionConstructionResponseCycleCloseAuthority.closeFor(followup, true, true, true, true, true);
        BlueprintFactionConstructionResponseCycleArchiveAuthority.ResponseCycleArchive archive =
                BlueprintFactionConstructionResponseCycleArchiveAuthority.archiveFor(close, true, true, true, true, true);
        BlueprintFactionConstructionResponseCycleReadbackAuthority.ResponseCycleReadback direct =
                BlueprintFactionConstructionResponseCycleReadbackAuthority.readbackFor(archive, true, true, true, true, true);
        require(direct.readbackReady(), "direct response cycle readback should be ready");
        requireContains(direct.boundaryLine(), "audit only, no readback mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleReadbackAuthority.ResponseCycleReadback sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.readbackState(), "readback state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response cycle readback audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleReadbackAuditSmoke() { }
}
