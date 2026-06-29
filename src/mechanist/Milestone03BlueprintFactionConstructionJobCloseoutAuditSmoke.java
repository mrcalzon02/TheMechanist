package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction job closeout contracts. */
final class Milestone03BlueprintFactionConstructionJobCloseoutAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionJobCloseoutAuthority.JobCloseout> samples =
                BlueprintFactionConstructionJobCloseoutAuthority.sampleCloseouts();
        List<String> audit = BlueprintFactionConstructionJobCloseoutAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample closeout rows");
        requireContains(audit, "owner=BlueprintFactionConstructionJobCloseoutAuthority", "closeout owner");
        requireContains(audit, "completionOwner=BlueprintFactionConstructionCompletionReadinessAuthority", "completion owner");
        requireContains(audit, "releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority", "release owner");
        requireContains(audit, "budgetOwner=BlueprintFactionConstructionBudgetHeatAuthorizationAuthority", "budget owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "completion readiness, readable site status, crew release readiness", "closeout gates");
        requireContains(audit, "sampleCloseouts=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "completionBlocked=2", "completion blocked count");
        requireContains(audit, "crewBlocked=1", "crew blocked count");
        requireContains(audit, "attentionBlocked=1", "attention blocked count");
        requireContains(audit, "job-storage-public job closeout for Storage Crate", "storage closeout");
        requireContains(audit, "state=JOB_CLOSEOUT_READY", "ready state");
        requireContains(audit, "job-sensor-restricted job closeout for Security Sensor Mast", "sensor closeout");
        requireContains(audit, "completion readiness blocked", "completion blocker");
        requireContains(audit, "job-shop-public job closeout for Licensed Shop Counter", "shop closeout");
        requireContains(audit, "crew release not ready", "release blocker");
        requireContains(audit, "readable completed, failed, or blocked job record", "readability rule");
        requireContains(audit, "does not release crew", "non-mutation boundary");
        requireContains(audit, "write job records", "record mutation boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionJobCloseoutAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionJobCloseoutAuthority.JobCloseout direct =
                BlueprintFactionConstructionJobCloseoutAuthority.closeoutFor(completion, true, true, true, true, true, true);
        require(direct.closeoutReady(), "direct closeout should be ready");
        requireContains(direct.boundaryLine(), "audit only, no closeout mutation", "direct boundary");

        for (BlueprintFactionConstructionJobCloseoutAuthority.JobCloseout sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.closeoutState(), "closeout state");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction job closeout audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionJobCloseoutAuditSmoke() { }
}
