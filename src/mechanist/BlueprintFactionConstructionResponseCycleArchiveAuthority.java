package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only archive contract for future faction construction response cycles. */
final class BlueprintFactionConstructionResponseCycleArchiveAuthority {
    record ResponseCycleArchive(String jobId, String blueprintName, String factionName, String archiveState,
                                boolean cycleCloseReady, boolean archiveReasonReadable,
                                boolean retentionLabelReady, boolean privacyLabelReady,
                                boolean statusSnapshotReady, boolean replayReferenceReady,
                                boolean archiveReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleArchiveAuthority() { }

    static List<ResponseCycleArchive> sampleArchives() {
        List<BlueprintFactionConstructionResponseCycleCloseAuthority.ResponseCycleClose> closes =
                BlueprintFactionConstructionResponseCycleCloseAuthority.sampleClosures();
        ArrayList<ResponseCycleArchive> rows = new ArrayList<>();
        rows.add(archiveFor(closes.get(0), true, true, true, true, true));
        rows.add(archiveFor(closes.get(1), true, true, true, true, true));
        rows.add(archiveFor(closes.get(2), false, true, false, false, true));
        return List.copyOf(rows);
    }

    static ResponseCycleArchive archiveFor(BlueprintFactionConstructionResponseCycleCloseAuthority.ResponseCycleClose close,
                                           boolean archiveReasonReadable, boolean retentionLabelReady,
                                           boolean privacyLabelReady, boolean statusSnapshotReady,
                                           boolean replayReferenceReady) {
        String id = close == null ? "job-unassigned" : clean(close.jobId(), "job-unassigned");
        String blueprint = close == null ? "Unknown blueprint" : clean(close.blueprintName(), "Unknown blueprint");
        String faction = close == null ? "Unaffiliated" : clean(close.factionName(), "Unaffiliated");
        String action = close == null ? "Archive response cycle" : clean(close.actionLine(), "Archive response cycle");
        boolean closeReady = close != null && close.closeReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!closeReady) blockers.add("response cycle close blocked");
        if (!archiveReasonReadable) blockers.add("archive reason not readable");
        if (!retentionLabelReady) blockers.add("retention label not ready");
        if (!privacyLabelReady) blockers.add("privacy label not ready");
        if (!statusSnapshotReady) blockers.add("status snapshot not ready");
        if (!replayReferenceReady) blockers.add("replay reference not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_ARCHIVE_READY" : "RESPONSE_CYCLE_ARCHIVE_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle archive for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; cycleCloseReady=" + closeReady
                + ", archiveReasonReadable=" + archiveReasonReadable
                + ", retentionLabelReady=" + retentionLabelReady
                + ", privacyLabelReady=" + privacyLabelReady
                + ", statusSnapshotReady=" + statusSnapshotReady
                + ", replayReferenceReady=" + replayReferenceReady
                + "; blockers=" + blockerLine
                + "; audit only, no archive mutation.";
        return new ResponseCycleArchive(id, blueprint, faction, state, closeReady, archiveReasonReadable,
                retentionLabelReady, privacyLabelReady, statusSnapshotReady, replayReferenceReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleArchive> samples = sampleArchives();
        int ready = 0;
        int blocked = 0;
        int closeBlocked = 0;
        int reasonBlocked = 0;
        int privacyBlocked = 0;
        int snapshotBlocked = 0;
        for (ResponseCycleArchive archive : samples) {
            if (archive.archiveReady()) ready++;
            else blocked++;
            if (!archive.cycleCloseReady()) closeBlocked++;
            if (!archive.archiveReasonReadable()) reasonBlocked++;
            if (!archive.privacyLabelReady()) privacyBlocked++;
            if (!archive.statusSnapshotReady()) snapshotBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle archive audit: owner=BlueprintFactionConstructionResponseCycleArchiveAuthority, closeOwner=BlueprintFactionConstructionResponseCycleCloseAuthority, statusOwner=BlueprintFactionConstructionStatusReportAuthority, notificationOwner=BlueprintFactionConstructionNotificationReadinessAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle archive audit: future response cycles may be archived only after cycle close readiness, readable archive reason, retention label, privacy label, status snapshot, and replay reference are all declared.",
                "Blueprint faction construction response cycle archive sample audit: sampleArchives=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", closeBlocked=" + closeBlocked
                        + ", reasonBlocked=" + reasonBlocked
                        + ", privacyBlocked=" + privacyBlocked
                        + ", snapshotBlocked=" + snapshotBlocked + ".",
                "Blueprint faction construction response cycle archive examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle archive rule: a future archive owner must show readable archive reason, retention label, privacy label, status snapshot, and replay reference before preserving a construction response cycle.",
                "Blueprint faction construction response cycle archive boundary: this audit does not write archives, redact records, delete records, update status, enqueue notifications, alter job state, mutate heat, mutate suspicion, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleArchiveAuditSmoke checks response cycle archive coverage, close and privacy blockers, archive readability, future archive boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
