package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction response cycle review action contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleReviewActionAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleReviewActionAuthority.ResponseCycleReviewAction> samples =
                BlueprintFactionConstructionResponseCycleReviewActionAuthority.sampleReviewActions();
        List<String> audit = BlueprintFactionConstructionResponseCycleReviewActionAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle review actions");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleReviewActionAuthority", "review action owner");
        requireContains(audit, "reviewOwner=BlueprintFactionConstructionResponseCycleReviewAuthority", "review owner");
        requireContains(audit, "readbackOwner=BlueprintFactionConstructionResponseCycleReadbackAuthority", "readback owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "review readiness, permission readiness, confirmation readiness", "review action gates");
        requireContains(audit, "sampleReviewActions=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "reviewBlocked=2", "review blocked count");
        requireContains(audit, "permissionBlocked=1", "permission blocked count");
        requireContains(audit, "confirmationBlocked=1", "confirmation blocked count");
        requireContains(audit, "auditTextBlocked=1", "audit text blocked count");
        requireContains(audit, "job-storage-public response cycle review action for Storage Crate", "storage review action");
        requireContains(audit, "state=RESPONSE_CYCLE_REVIEW_ACTION_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle review action for Security Sensor Mast", "sensor review action");
        requireContains(audit, "response cycle review blocked", "review blocker");
        requireContains(audit, "job-shop-public response cycle review action for Licensed Shop Counter", "shop review action");
        requireContains(audit, "review action permission not ready", "permission blocker");
        requireContains(audit, "permission readiness, confirmation readiness, evidence selection, audit text, and non-reopen boundary", "readability rule");
        requireContains(audit, "does not execute review actions", "execution boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleReviewActionAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionResponseCycleReadbackAuthority.ResponseCycleReadback readback =
                BlueprintFactionConstructionResponseCycleReadbackAuthority.readbackFor(archive, true, true, true, true, true);
        BlueprintFactionConstructionResponseCycleReviewAuthority.ResponseCycleReview review =
                BlueprintFactionConstructionResponseCycleReviewAuthority.reviewFor(readback, true, true, true, true, true);
        BlueprintFactionConstructionResponseCycleReviewActionAuthority.ResponseCycleReviewAction direct =
                BlueprintFactionConstructionResponseCycleReviewActionAuthority.actionFor(review, true, true, true, true, true);
        require(direct.actionReady(), "direct response cycle review action should be ready");
        requireContains(direct.boundaryLine(), "audit only, no review-action mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleReviewActionAuthority.ResponseCycleReviewAction sample : samples) {
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
                throw new AssertionError("Blueprint faction construction response cycle review action audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleReviewActionAuditSmoke() { }
}
