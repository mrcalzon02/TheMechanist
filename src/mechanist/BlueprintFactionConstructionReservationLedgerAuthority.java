package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only reservation ledger contract for future faction construction jobs. */
final class BlueprintFactionConstructionReservationLedgerAuthority {
    record ReservationLedger(String jobId, String blueprintName, String factionName, String ledgerState,
                             boolean queueReserved, boolean materialHold, boolean crewHold,
                             boolean siteHold, boolean budgetHold, boolean attentionHold,
                             boolean ledgerReady, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionReservationLedgerAuthority() { }

    static List<ReservationLedger> sampleLedgers() {
        List<BlueprintFactionConstructionQueueAdmissionAuthority.QueueAdmission> admissions =
                BlueprintFactionConstructionQueueAdmissionAuthority.sampleAdmissions();
        ArrayList<ReservationLedger> rows = new ArrayList<>();
        rows.add(ledgerFor(admissions.get(0), true, true, true, true, true));
        rows.add(ledgerFor(admissions.get(1), true, true, false, true, true));
        rows.add(ledgerFor(admissions.get(2), false, false, false, false, false));
        return List.copyOf(rows);
    }

    static ReservationLedger ledgerFor(BlueprintFactionConstructionQueueAdmissionAuthority.QueueAdmission admission,
                                       boolean materialHold, boolean crewHold, boolean siteHold,
                                       boolean budgetHold, boolean attentionHold) {
        String id = admission == null ? "job-unassigned" : clean(admission.jobId(), "job-unassigned");
        String blueprint = admission == null ? "Unknown blueprint" : clean(admission.blueprintName(), "Unknown blueprint");
        String faction = admission == null ? "Unaffiliated" : clean(admission.factionName(), "Unaffiliated");
        boolean queueReserved = admission != null && admission.admissionReady()
                && "RESERVED".equals(admission.admittedState());
        ArrayList<String> blockers = new ArrayList<>();
        if (!queueReserved) blockers.add("queue admission not reserved");
        if (!materialHold) blockers.add("material hold missing");
        if (!crewHold) blockers.add("crew hold missing");
        if (!siteHold) blockers.add("site hold missing");
        if (!budgetHold) blockers.add("budget hold missing");
        if (!attentionHold) blockers.add("attention hold missing");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESERVATION_LEDGER_READY" : "RESERVATION_LEDGER_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " reservation ledger for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; holds=queue:" + queueReserved
                + ", materials:" + materialHold
                + ", crew:" + crewHold
                + ", site:" + siteHold
                + ", budget:" + budgetHold
                + ", attention:" + attentionHold
                + "; blockers=" + blockerLine
                + "; audit only, no reservation writes.";
        return new ReservationLedger(id, blueprint, faction, state, queueReserved, materialHold, crewHold,
                siteHold, budgetHold, attentionHold, ready, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ReservationLedger> samples = sampleLedgers();
        int ready = 0;
        int blocked = 0;
        int queueReserved = 0;
        int siteBlocked = 0;
        for (ReservationLedger ledger : samples) {
            if (ledger.ledgerReady()) ready++;
            else blocked++;
            if (ledger.queueReserved()) queueReserved++;
            if (!ledger.siteHold()) siteBlocked++;
        }
        return List.of(
                "Blueprint faction construction reservation ledger audit: owner=BlueprintFactionConstructionReservationLedgerAuthority, queueOwner=BlueprintFactionConstructionQueueAdmissionAuthority, handoffOwner=BlueprintFactionConstructionExecutionHandoffAuthority, materialOwner=BlueprintFactionConstructionMaterialReservationAuthority, crewOwner=BlueprintFactionConstructionCrewAssignmentAuthority, siteOwner=BlueprintFactionConstructionSiteReadinessAuthority, budgetOwner=BlueprintFactionConstructionBudgetHeatAuthorizationAuthority, releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction reservation ledger audit: future execution may treat a faction construction job as reserved only when queue admission is reserved and material, crew, site, budget, and attention holds are all declared together.",
                "Blueprint faction construction reservation ledger sample audit: sampleLedgers=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", queueReserved=" + queueReserved
                        + ", siteBlocked=" + siteBlocked + ".",
                "Blueprint faction construction reservation ledger examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction reservation ledger rule: a future execution owner must create and release reservation holds as one ledger, with cancellation release remaining the rollback owner before any staged construction placement.",
                "Blueprint faction construction reservation ledger boundary: this audit does not write reservation rows, create a live job queue, reserve workers, reserve sites, remove materials, spend budget, mutate heat, mutate suspicion, place staged construction, advance labor, cancel jobs, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionReservationLedgerAuditSmoke checks reservation hold coverage, blocked ledgers, rollback ownership, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
