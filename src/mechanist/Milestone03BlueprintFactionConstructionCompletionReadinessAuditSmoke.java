package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction completion readiness contracts. */
final class Milestone03BlueprintFactionConstructionCompletionReadinessAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionCompletionReadinessAuthority.CompletionReadiness> samples =
                BlueprintFactionConstructionCompletionReadinessAuthority.sampleCompletions();
        List<String> audit = BlueprintFactionConstructionCompletionReadinessAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample completion rows");
        requireContains(audit, "owner=BlueprintFactionConstructionCompletionReadinessAuthority", "completion owner");
        requireContains(audit, "progressOwner=BlueprintFactionConstructionProgressTickAuthority", "progress owner");
        requireContains(audit, "stagedConstructionOwner=ProgressiveConstructionAuthority", "staged construction owner");
        requireContains(audit, "releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority", "release owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "progress tick readiness, labor completion, final symbol restoration", "completion gates");
        requireContains(audit, "sampleCompletions=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "laborBlocked=2", "labor blocked count");
        requireContains(audit, "symbolBlocked=1", "symbol blocked count");
        requireContains(audit, "releaseBlocked=1", "release blocked count");
        requireContains(audit, "job-storage-public completion readiness for Storage Crate", "storage completion");
        requireContains(audit, "state=COMPLETION_READY", "ready state");
        requireContains(audit, "job-sensor-restricted completion readiness for Security Sensor Mast", "sensor completion");
        requireContains(audit, "labor not complete", "labor blocker");
        requireContains(audit, "job-shop-public completion readiness for Licensed Shop Counter", "shop completion");
        requireContains(audit, "final symbol restoration not ready", "symbol blocker");
        requireContains(audit, "keep completion separate from ordinary facility operation", "operation separation rule");
        requireContains(audit, "does not restore tiles", "non-mutation boundary");
        requireContains(audit, "release reservations, or complete construction", "release boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionCompletionReadinessAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionCompletionReadinessAuthority.CompletionReadiness direct =
                BlueprintFactionConstructionCompletionReadinessAuthority.readinessFor(tick, true, true, true, true, true);
        require(direct.completionReady(), "direct completion readiness should be ready");
        requireContains(direct.boundaryLine(), "audit only, no completion mutation", "direct boundary");

        for (BlueprintFactionConstructionCompletionReadinessAuthority.CompletionReadiness sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.completionState(), "completion state");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction completion readiness audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionCompletionReadinessAuditSmoke() { }
}
