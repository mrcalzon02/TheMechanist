package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only status report contract for future faction construction jobs. */
final class BlueprintFactionConstructionStatusReportAuthority {
    record StatusReport(String jobId, String blueprintName, String factionName, String reportState,
                        boolean closeoutReady, boolean summaryReadable, boolean blockersReadable,
                        boolean nextActionReadable, boolean timelineReadable, boolean rawIdsHidden,
                        boolean reportReady, String nextActionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionStatusReportAuthority() { }

    static List<StatusReport> sampleReports() {
        List<BlueprintFactionConstructionJobCloseoutAuthority.JobCloseout> closeouts =
                BlueprintFactionConstructionJobCloseoutAuthority.sampleCloseouts();
        ArrayList<StatusReport> rows = new ArrayList<>();
        rows.add(reportFor(closeouts.get(0), true, true, true, true, true, "Review completed construction"));
        rows.add(reportFor(closeouts.get(1), true, true, true, true, true, "Resolve completion blockers"));
        rows.add(reportFor(closeouts.get(2), true, true, false, false, true, "Repair closeout packet"));
        return List.copyOf(rows);
    }

    static StatusReport reportFor(BlueprintFactionConstructionJobCloseoutAuthority.JobCloseout closeout,
                                  boolean summaryReadable, boolean blockersReadable,
                                  boolean nextActionReadable, boolean timelineReadable,
                                  boolean rawIdsHidden, String nextActionLine) {
        String id = closeout == null ? "job-unassigned" : clean(closeout.jobId(), "job-unassigned");
        String blueprint = closeout == null ? "Unknown blueprint" : clean(closeout.blueprintName(), "Unknown blueprint");
        String faction = closeout == null ? "Unaffiliated" : clean(closeout.factionName(), "Unaffiliated");
        boolean closeoutReady = closeout != null && closeout.closeoutReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!closeoutReady) blockers.add("closeout blocked");
        if (!summaryReadable) blockers.add("summary not readable");
        if (!blockersReadable) blockers.add("blockers not readable");
        if (!nextActionReadable) blockers.add("next action not readable");
        if (!timelineReadable) blockers.add("timeline not readable");
        if (!rawIdsHidden) blockers.add("raw identifiers visible");
        boolean ready = blockers.isEmpty();
        String state = ready ? "STATUS_REPORT_READY" : "STATUS_REPORT_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String nextAction = clean(nextActionLine, ready ? "Review completed construction" : "Resolve blockers");
        String boundary = id + " status report for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; closeoutReady=" + closeoutReady
                + ", summaryReadable=" + summaryReadable
                + ", blockersReadable=" + blockersReadable
                + ", nextActionReadable=" + nextActionReadable
                + ", timelineReadable=" + timelineReadable
                + ", rawIdsHidden=" + rawIdsHidden
                + "; nextAction=" + nextAction
                + "; blockers=" + blockerLine
                + "; audit only, no status mutation.";
        return new StatusReport(id, blueprint, faction, state, closeoutReady, summaryReadable, blockersReadable,
                nextActionReadable, timelineReadable, rawIdsHidden, ready, nextAction, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<StatusReport> samples = sampleReports();
        int ready = 0;
        int blocked = 0;
        int closeoutBlocked = 0;
        int nextActionBlocked = 0;
        int rawIdBlocked = 0;
        for (StatusReport report : samples) {
            if (report.reportReady()) ready++;
            else blocked++;
            if (!report.closeoutReady()) closeoutBlocked++;
            if (!report.nextActionReadable()) nextActionBlocked++;
            if (!report.rawIdsHidden()) rawIdBlocked++;
        }
        return List.of(
                "Blueprint faction construction status report audit: owner=BlueprintFactionConstructionStatusReportAuthority, closeoutOwner=BlueprintFactionConstructionJobCloseoutAuthority, infopediaOwner=SemanticAssetInfopediaAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction status report audit: future UI may show a faction construction status packet only after closeout readiness, readable summary, readable blockers, readable next action, readable timeline, and hidden raw identifiers are all declared.",
                "Blueprint faction construction status report sample audit: sampleReports=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", closeoutBlocked=" + closeoutBlocked
                        + ", nextActionBlocked=" + nextActionBlocked
                        + ", rawIdBlocked=" + rawIdBlocked + ".",
                "Blueprint faction construction status report examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction status report rule: a future reporting owner must show a readable status packet with summary, blockers, next action, and timeline before exposing faction construction job state to ordinary UI.",
                "Blueprint faction construction status report boundary: this audit does not write UI state, write job records, release reservations, mutate room ownership, mutate heat, mutate suspicion, enable facility operation, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionStatusReportAuditSmoke checks status packet coverage, closeout and next-action blockers, raw-id hiding, future reporting boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
