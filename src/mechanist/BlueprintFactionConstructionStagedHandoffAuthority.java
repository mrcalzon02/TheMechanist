package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only staged-construction handoff contract for future faction construction jobs. */
final class BlueprintFactionConstructionStagedHandoffAuthority {
    record StagedHandoff(String jobId, String blueprintName, String factionName, String handoffState,
                         boolean reservationReady, boolean placementRechecked, boolean originalTileCaptured,
                         boolean materialsTransferPlanned, boolean rollbackPlanned, boolean handoffReady,
                         String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionStagedHandoffAuthority() { }

    static List<StagedHandoff> sampleHandoffs() {
        List<BlueprintFactionConstructionReservationLedgerAuthority.ReservationLedger> ledgers =
                BlueprintFactionConstructionReservationLedgerAuthority.sampleLedgers();
        ArrayList<StagedHandoff> rows = new ArrayList<>();
        rows.add(handoffFor(ledgers.get(0), true, true, true, true));
        rows.add(handoffFor(ledgers.get(1), true, true, true, true));
        rows.add(handoffFor(ledgers.get(2), true, false, false, false));
        return List.copyOf(rows);
    }

    static StagedHandoff handoffFor(BlueprintFactionConstructionReservationLedgerAuthority.ReservationLedger ledger,
                                    boolean placementRechecked, boolean originalTileCaptured,
                                    boolean materialsTransferPlanned, boolean rollbackPlanned) {
        String id = ledger == null ? "job-unassigned" : clean(ledger.jobId(), "job-unassigned");
        String blueprint = ledger == null ? "Unknown blueprint" : clean(ledger.blueprintName(), "Unknown blueprint");
        String faction = ledger == null ? "Unaffiliated" : clean(ledger.factionName(), "Unaffiliated");
        boolean reservationReady = ledger != null && ledger.ledgerReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!reservationReady) blockers.add("reservation ledger blocked");
        if (!placementRechecked) blockers.add("placement recheck missing");
        if (!originalTileCaptured) blockers.add("original tile capture missing");
        if (!materialsTransferPlanned) blockers.add("material transfer plan missing");
        if (!rollbackPlanned) blockers.add("rollback plan missing");
        boolean ready = blockers.isEmpty();
        String state = ready ? "STAGED_HANDOFF_READY" : "STAGED_HANDOFF_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " staged construction handoff for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; reservationReady=" + reservationReady
                + ", placementRechecked=" + placementRechecked
                + ", originalTileCaptured=" + originalTileCaptured
                + ", materialsTransferPlanned=" + materialsTransferPlanned
                + ", rollbackPlanned=" + rollbackPlanned
                + "; blockers=" + blockerLine
                + "; audit only, no staged site placement.";
        return new StagedHandoff(id, blueprint, faction, state, reservationReady, placementRechecked,
                originalTileCaptured, materialsTransferPlanned, rollbackPlanned, ready, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<StagedHandoff> samples = sampleHandoffs();
        int ready = 0;
        int blocked = 0;
        int reservationBlocked = 0;
        int tileCaptureBlocked = 0;
        for (StagedHandoff handoff : samples) {
            if (handoff.handoffReady()) ready++;
            else blocked++;
            if (!handoff.reservationReady()) reservationBlocked++;
            if (!handoff.originalTileCaptured()) tileCaptureBlocked++;
        }
        return List.of(
                "Blueprint faction construction staged handoff audit: owner=BlueprintFactionConstructionStagedHandoffAuthority, reservationOwner=BlueprintFactionConstructionReservationLedgerAuthority, stagedConstructionOwner=ProgressiveConstructionAuthority, siteOwner=BlueprintFactionConstructionSiteReadinessAuthority, releaseOwner=BlueprintFactionConstructionCancellationReleaseAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction staged handoff audit: future execution may place a staged construction site only after reservation ledger readiness, immediate placement recheck, original tile capture, material transfer plan, and rollback plan are all declared.",
                "Blueprint faction construction staged handoff sample audit: sampleHandoffs=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", reservationBlocked=" + reservationBlocked
                        + ", tileCaptureBlocked=" + tileCaptureBlocked + ".",
                "Blueprint faction construction staged handoff examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction staged handoff rule: a future execution owner must re-check the site immediately before handing a reserved job to ProgressiveConstructionAuthority and keep rollback release available if placement fails.",
                "Blueprint faction construction staged handoff boundary: this audit does not create staged sites, reserve tiles, mutate original tiles, transfer materials, assign workers, spend budget, mutate heat, mutate suspicion, advance labor, cancel jobs, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionStagedHandoffAuditSmoke checks staged handoff gates, tile-capture requirements, blocked handoffs, future execution boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
