package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction labor progress tick contracts. */
final class Milestone03BlueprintFactionConstructionProgressTickAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionProgressTickAuthority.ProgressTick> samples =
                BlueprintFactionConstructionProgressTickAuthority.sampleTicks();
        List<String> audit = BlueprintFactionConstructionProgressTickAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample progress ticks");
        requireContains(audit, "owner=BlueprintFactionConstructionProgressTickAuthority", "progress tick owner");
        requireContains(audit, "placementOwner=BlueprintFactionConstructionPlacementOutcomeAuthority", "placement owner");
        requireContains(audit, "crewOwner=BlueprintFactionConstructionCrewAssignmentAuthority", "crew owner");
        requireContains(audit, "materialOwner=BlueprintFactionConstructionMaterialReservationAuthority", "material owner");
        requireContains(audit, "stagedConstructionOwner=ProgressiveConstructionAuthority", "staged construction owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "placement outcome readiness, crew presence, staged materials", "progress gates");
        requireContains(audit, "sampleTicks=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "placementBlocked=2", "placement blocked count");
        requireContains(audit, "crewBlocked=1", "crew blocked count");
        requireContains(audit, "completionEligible=0", "completion separation count");
        requireContains(audit, "job-storage-public progress tick for Storage Crate", "storage tick");
        requireContains(audit, "state=PROGRESS_TICK_READY", "ready state");
        requireContains(audit, "labor=1->2/4", "labor forecast");
        requireContains(audit, "job-sensor-restricted progress tick for Security Sensor Mast", "sensor tick");
        requireContains(audit, "placement outcome blocked", "placement blocker");
        requireContains(audit, "job-shop-public progress tick for Licensed Shop Counter", "shop tick");
        requireContains(audit, "crew not present", "crew blocker");
        requireContains(audit, "separate labor progress from completion", "completion separation rule");
        requireContains(audit, "does not advance labor", "non-mutation boundary");
        requireContains(audit, "mutate heat, mutate suspicion", "attention boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionProgressTickAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionProgressTickAuthority.ProgressTick direct =
                BlueprintFactionConstructionProgressTickAuthority.tickFor(outcome, true, true, true, true, true, 2, 1, 4);
        require(direct.tickReady(), "direct progress tick should be ready");
        require(!direct.completionEligible(), "direct progress tick should not complete construction");
        requireContains(direct.boundaryLine(), "audit only, no labor mutation", "direct boundary");

        for (BlueprintFactionConstructionProgressTickAuthority.ProgressTick sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.tickState(), "tick state");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
            require(sample.laborAfter() >= sample.laborBefore(), "labor should not forecast backwards");
            require(sample.requiredLaborTurns() >= 1, "required labor should be positive");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction progress tick audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionProgressTickAuditSmoke() { }
}
