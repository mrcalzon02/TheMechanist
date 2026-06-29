package mechanist;

import java.util.List;
import java.util.Map;

/** Smoke for audit-only faction construction status report contracts. */
final class Milestone03BlueprintFactionConstructionStatusReportAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionStatusReportAuthority.StatusReport> samples =
                BlueprintFactionConstructionStatusReportAuthority.sampleReports();
        List<String> audit = BlueprintFactionConstructionStatusReportAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample status reports");
        requireContains(audit, "owner=BlueprintFactionConstructionStatusReportAuthority", "status report owner");
        requireContains(audit, "closeoutOwner=BlueprintFactionConstructionJobCloseoutAuthority", "closeout owner");
        requireContains(audit, "infopediaOwner=SemanticAssetInfopediaAuthority", "infopedia owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "closeout readiness, readable summary, readable blockers", "status gates");
        requireContains(audit, "sampleReports=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "closeoutBlocked=2", "closeout blocked count");
        requireContains(audit, "nextActionBlocked=1", "next action blocked count");
        requireContains(audit, "rawIdBlocked=0", "raw id blocked count");
        requireContains(audit, "job-storage-public status report for Storage Crate", "storage report");
        requireContains(audit, "state=STATUS_REPORT_READY", "ready state");
        requireContains(audit, "Review completed construction", "ready next action");
        requireContains(audit, "job-sensor-restricted status report for Security Sensor Mast", "sensor report");
        requireContains(audit, "closeout blocked", "closeout blocker");
        requireContains(audit, "job-shop-public status report for Licensed Shop Counter", "shop report");
        requireContains(audit, "next action not readable", "next action blocker");
        requireContains(audit, "readable status packet with summary, blockers, next action, and timeline", "readability rule");
        requireContains(audit, "does not write UI state", "ui mutation boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionStatusReportAuditSmoke", "guard reference");

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
        BlueprintFactionConstructionJobCloseoutAuthority.JobCloseout closeout =
                BlueprintFactionConstructionJobCloseoutAuthority.closeoutFor(completion, true, true, true, true, true, true);
        BlueprintFactionConstructionStatusReportAuthority.StatusReport direct =
                BlueprintFactionConstructionStatusReportAuthority.reportFor(closeout, true, true, true, true, true,
                        "Review completed construction");
        require(direct.reportReady(), "direct status report should be ready");
        requireContains(direct.boundaryLine(), "audit only, no status mutation", "direct boundary");

        for (BlueprintFactionConstructionStatusReportAuthority.StatusReport sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.reportState(), "report state");
            requireNotBlank(sample.nextActionLine(), "next action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction status report audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionStatusReportAuditSmoke() { }
}
