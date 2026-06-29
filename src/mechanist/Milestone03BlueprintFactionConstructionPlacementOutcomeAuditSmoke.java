package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction placement outcome contracts. */
final class Milestone03BlueprintFactionConstructionPlacementOutcomeAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionPlacementOutcomeAuthority.PlacementOutcome> samples =
                BlueprintFactionConstructionPlacementOutcomeAuthority.sampleOutcomes();
        List<String> audit = BlueprintFactionConstructionPlacementOutcomeAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample placement outcomes");
        requireContains(audit, "owner=BlueprintFactionConstructionPlacementOutcomeAuthority", "placement outcome owner");
        requireContains(audit, "stagedHandoffOwner=BlueprintFactionConstructionStagedHandoffAuthority", "staged handoff owner");
        requireContains(audit, "stagedConstructionOwner=ProgressiveConstructionAuthority", "staged construction owner");
        requireContains(audit, "inspectionOwner=ConstructionReadabilityAuthority", "inspection owner");
        requireContains(audit, "releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority", "release owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "construction placeholder reservation, final symbol recording", "placement gates");
        requireContains(audit, "sampleOutcomes=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "handoffBlocked=2", "handoff blocked count");
        requireContains(audit, "placeholderBlocked=1", "placeholder blocked count");
        requireContains(audit, "job-storage-public placement outcome for Storage Crate", "storage outcome");
        requireContains(audit, "state=PLACEMENT_OUTCOME_READY", "ready state");
        requireContains(audit, "job-sensor-restricted placement outcome for Security Sensor Mast", "sensor outcome");
        requireContains(audit, "staged handoff blocked", "handoff blocker");
        requireContains(audit, "job-shop-public placement outcome for Licensed Shop Counter", "shop outcome");
        requireContains(audit, "construction placeholder not reserved", "placeholder blocker");
        requireContains(audit, "successful and failed placement outcomes readable", "readability rule");
        requireContains(audit, "does not write tiles", "non-mutation boundary");
        requireContains(audit, "mutate heat, mutate suspicion", "attention boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionPlacementOutcomeAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionPlacementOutcomeAuthority.PlacementOutcome direct =
                BlueprintFactionConstructionPlacementOutcomeAuthority.outcomeFor(handoff, true, true, true, true);
        require(direct.outcomeReady(), "direct placement outcome should be ready");
        requireContains(direct.boundaryLine(), "audit only, no placement mutation", "direct boundary");

        for (BlueprintFactionConstructionPlacementOutcomeAuthority.PlacementOutcome sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.outcomeState(), "outcome state");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction placement outcome audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionPlacementOutcomeAuditSmoke() { }
}
