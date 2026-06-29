package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction staged-construction handoff contracts. */
final class Milestone03BlueprintFactionConstructionStagedHandoffAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionStagedHandoffAuthority.StagedHandoff> samples =
                BlueprintFactionConstructionStagedHandoffAuthority.sampleHandoffs();
        List<String> audit = BlueprintFactionConstructionStagedHandoffAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample staged handoffs");
        requireContains(audit, "owner=BlueprintFactionConstructionStagedHandoffAuthority", "staged handoff owner");
        requireContains(audit, "reservationOwner=BlueprintFactionConstructionReservationLedgerAuthority", "reservation owner");
        requireContains(audit, "stagedConstructionOwner=ProgressiveConstructionAuthority", "staged owner");
        requireContains(audit, "siteOwner=BlueprintFactionConstructionSiteReadinessAuthority", "site owner");
        requireContains(audit, "releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority", "release owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "original tile capture, material transfer plan, and rollback plan", "handoff gates");
        requireContains(audit, "sampleHandoffs=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "reservationBlocked=2", "reservation blocked count");
        requireContains(audit, "tileCaptureBlocked=1", "tile capture blocked count");
        requireContains(audit, "job-storage-public staged construction handoff for Storage Crate", "storage handoff");
        requireContains(audit, "state=STAGED_HANDOFF_READY", "ready state");
        requireContains(audit, "job-sensor-restricted staged construction handoff for Security Sensor Mast", "sensor handoff");
        requireContains(audit, "reservation ledger blocked", "reservation blocker");
        requireContains(audit, "job-shop-public staged construction handoff for Licensed Shop Counter", "shop handoff");
        requireContains(audit, "original tile capture missing", "tile blocker");
        requireContains(audit, "re-check the site immediately", "recheck rule");
        requireContains(audit, "does not create staged sites", "non-mutation boundary");
        requireContains(audit, "mutate heat, mutate suspicion", "attention boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionStagedHandoffAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionStagedHandoffAuthority.StagedHandoff direct =
                BlueprintFactionConstructionStagedHandoffAuthority.handoffFor(ledger, true, true, true, true);
        require(direct.handoffReady(), "direct staged handoff should be ready");
        requireContains(direct.boundaryLine(), "audit only, no staged site placement", "direct boundary");

        for (BlueprintFactionConstructionStagedHandoffAuthority.StagedHandoff sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.handoffState(), "handoff state");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction staged handoff audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionStagedHandoffAuditSmoke() { }
}
