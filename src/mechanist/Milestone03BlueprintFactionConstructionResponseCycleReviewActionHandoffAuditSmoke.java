package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction response cycle review action handoff contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleReviewActionHandoffAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority.ResponseCycleReviewActionHandoff> samples =
                BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority.sampleHandoffs();
        List<String> audit = BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle review action handoffs");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority", "handoff owner");
        requireContains(audit, "actionOwner=BlueprintFactionConstructionResponseCycleReviewActionAuthority", "action owner");
        requireContains(audit, "reviewOwner=BlueprintFactionConstructionResponseCycleReviewAuthority", "review owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "review action readiness, target resolution, command owner", "handoff gates");
        requireContains(audit, "sampleHandoffs=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "actionBlocked=2", "action blocked count");
        requireContains(audit, "targetBlocked=1", "target blocked count");
        requireContains(audit, "ownerBlocked=1", "owner blocked count");
        requireContains(audit, "turnCostBlocked=1", "turn cost blocked count");
        requireContains(audit, "job-storage-public response cycle review action handoff for Storage Crate", "storage handoff");
        requireContains(audit, "state=RESPONSE_CYCLE_REVIEW_ACTION_HANDOFF_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle review action handoff for Security Sensor Mast", "sensor handoff");
        requireContains(audit, "response cycle review action blocked", "action blocker");
        requireContains(audit, "job-shop-public response cycle review action handoff for Licensed Shop Counter", "shop handoff");
        requireContains(audit, "review action target not resolved", "target blocker");
        requireContains(audit, "target resolution, command owner, rollback preview, turn cost preview, and result text", "readability rule");
        requireContains(audit, "does not hand off commands", "handoff boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleReviewActionHandoffAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionResponseActionAuthority.ResponseAction responseAction =
                BlueprintFactionConstructionResponseActionAuthority.actionFor(notification, true, true, true, true, true,
                        "Inspect completed site");
        BlueprintFactionConstructionResponseExecutionHandoffAuthority.ResponseExecutionHandoff handoff =
                BlueprintFactionConstructionResponseExecutionHandoffAuthority.handoffFor(responseAction, true, true, true, true, true);
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
        BlueprintFactionConstructionResponseCycleReviewActionAuthority.ResponseCycleReviewAction action =
                BlueprintFactionConstructionResponseCycleReviewActionAuthority.actionFor(review, true, true, true, true, true);
        BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority.ResponseCycleReviewActionHandoff direct =
                BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority.handoffFor(action, true, true, true, true, true);
        require(direct.handoffReady(), "direct response cycle review action handoff should be ready");
        requireContains(direct.boundaryLine(), "audit only, no review-action handoff mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority.ResponseCycleReviewActionHandoff sample : samples) {
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
                throw new AssertionError("Blueprint faction construction response cycle review action handoff audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleReviewActionHandoffAuditSmoke() { }
}
