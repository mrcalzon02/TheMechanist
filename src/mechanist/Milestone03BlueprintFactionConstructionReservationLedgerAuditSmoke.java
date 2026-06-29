package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction reservation ledger contracts. */
final class Milestone03BlueprintFactionConstructionReservationLedgerAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionReservationLedgerAuthority.ReservationLedger> samples =
                BlueprintFactionConstructionReservationLedgerAuthority.sampleLedgers();
        List<String> audit = BlueprintFactionConstructionReservationLedgerAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample ledgers");
        requireContains(audit, "owner=BlueprintFactionConstructionReservationLedgerAuthority", "reservation ledger owner");
        requireContains(audit, "queueOwner=BlueprintFactionConstructionQueueAdmissionAuthority", "queue owner");
        requireContains(audit, "handoffOwner=BlueprintFactionConstructionExecutionHandoffAuthority", "handoff owner");
        requireContains(audit, "materialOwner=BlueprintFactionConstructionMaterialReservationAuthority", "material owner");
        requireContains(audit, "crewOwner=BlueprintFactionConstructionCrewAssignmentAuthority", "crew owner");
        requireContains(audit, "siteOwner=BlueprintFactionConstructionSiteReadinessAuthority", "site owner");
        requireContains(audit, "budgetOwner=BlueprintFactionConstructionBudgetHeatAuthorizationAuthority", "budget owner");
        requireContains(audit, "releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority", "release owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "material, crew, site, budget, and attention holds", "hold list");
        requireContains(audit, "sampleLedgers=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "queueReserved=1", "reserved count");
        requireContains(audit, "siteBlocked=2", "site blocked count");
        requireContains(audit, "job-storage-public reservation ledger for Storage Crate", "storage ledger");
        requireContains(audit, "state=RESERVATION_LEDGER_READY", "ready state");
        requireContains(audit, "job-sensor-restricted reservation ledger for Security Sensor Mast", "sensor ledger");
        requireContains(audit, "site hold missing", "site hold blocker");
        requireContains(audit, "job-shop-public reservation ledger for Licensed Shop Counter", "shop ledger");
        requireContains(audit, "queue admission not reserved", "queue blocker");
        requireContains(audit, "create and release reservation holds as one ledger", "ledger rule");
        requireContains(audit, "does not write reservation rows", "non-mutation boundary");
        requireContains(audit, "mutate heat, mutate suspicion", "attention boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionReservationLedgerAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionReservationLedgerAuthority.ReservationLedger direct =
                BlueprintFactionConstructionReservationLedgerAuthority.ledgerFor(admission, true, true, true, true, true);
        require(direct.ledgerReady(), "direct ledger should be ready");
        requireContains(direct.boundaryLine(), "audit only, no reservation writes", "direct boundary");

        for (BlueprintFactionConstructionReservationLedgerAuthority.ReservationLedger sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.ledgerState(), "ledger state");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction reservation ledger audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionReservationLedgerAuditSmoke() { }
}
