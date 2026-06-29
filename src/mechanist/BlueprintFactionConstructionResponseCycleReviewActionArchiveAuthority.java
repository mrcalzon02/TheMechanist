package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only archive contract for future closed faction construction response cycle review actions. */
final class BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority {
    record ResponseCycleReviewActionArchive(String jobId, String blueprintName, String factionName, String archiveState,
                                            boolean reviewActionCloseReady, boolean archiveReasonReadable,
                                            boolean retentionLabelReady, boolean privacyLabelReady,
                                            boolean resultSnapshotReady, boolean replayReferenceReady,
                                            boolean archiveReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority() { }

    static List<ResponseCycleReviewActionArchive> sampleArchives() {
        List<BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority.ResponseCycleReviewActionClose> closes =
                BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority.sampleClosures();
        ArrayList<ResponseCycleReviewActionArchive> rows = new ArrayList<>();
        rows.add(archiveFor(closes.get(0), true, true, true, true, true));
        rows.add(archiveFor(closes.get(1), true, true, true, true, true));
        rows.add(archiveFor(closes.get(2), false, true, false, false, true));
        return List.copyOf(rows);
    }

    static ResponseCycleReviewActionArchive archiveFor(BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority.ResponseCycleReviewActionClose close,
                                                       boolean archiveReasonReadable, boolean retentionLabelReady,
                                                       boolean privacyLabelReady, boolean resultSnapshotReady,
                                                       boolean replayReferenceReady) {
        String id = close == null ? "job-unassigned" : clean(close.jobId(), "job-unassigned");
        String blueprint = close == null ? "Unknown blueprint" : clean(close.blueprintName(), "Unknown blueprint");
        String faction = close == null ? "Unaffiliated" : clean(close.factionName(), "Unaffiliated");
        String action = close == null ? "Archive review action close" : clean(close.actionLine(), "Archive review action close");
        boolean closeReady = close != null && close.closeReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!closeReady) blockers.add("response cycle review action close blocked");
        if (!archiveReasonReadable) blockers.add("review action archive reason not readable");
        if (!retentionLabelReady) blockers.add("review action retention label not ready");
        if (!privacyLabelReady) blockers.add("review action privacy label not ready");
        if (!resultSnapshotReady) blockers.add("review action result snapshot not ready");
        if (!replayReferenceReady) blockers.add("review action replay reference not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_REVIEW_ACTION_ARCHIVE_READY" : "RESPONSE_CYCLE_REVIEW_ACTION_ARCHIVE_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle review action archive for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; reviewActionCloseReady=" + closeReady
                + ", archiveReasonReadable=" + archiveReasonReadable
                + ", retentionLabelReady=" + retentionLabelReady
                + ", privacyLabelReady=" + privacyLabelReady
                + ", resultSnapshotReady=" + resultSnapshotReady
                + ", replayReferenceReady=" + replayReferenceReady
                + "; blockers=" + blockerLine
                + "; audit only, no review-action archive mutation.";
        return new ResponseCycleReviewActionArchive(id, blueprint, faction, state, closeReady, archiveReasonReadable,
                retentionLabelReady, privacyLabelReady, resultSnapshotReady, replayReferenceReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleReviewActionArchive> samples = sampleArchives();
        int ready = 0;
        int blocked = 0;
        int closeBlocked = 0;
        int reasonBlocked = 0;
        int privacyBlocked = 0;
        int snapshotBlocked = 0;
        for (ResponseCycleReviewActionArchive archive : samples) {
            if (archive.archiveReady()) ready++;
            else blocked++;
            if (!archive.reviewActionCloseReady()) closeBlocked++;
            if (!archive.archiveReasonReadable()) reasonBlocked++;
            if (!archive.privacyLabelReady()) privacyBlocked++;
            if (!archive.resultSnapshotReady()) snapshotBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle review action archive audit: owner=BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority, closeOwner=BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority, followupOwner=BlueprintFactionConstructionResponseCycleReviewActionFollowupAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle review action archive audit: future archived review action close records may be archived only after close readiness, readable archive reason, retention label, privacy label, result snapshot, and replay reference are all declared.",
                "Blueprint faction construction response cycle review action archive sample audit: sampleArchives=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", closeBlocked=" + closeBlocked
                        + ", reasonBlocked=" + reasonBlocked
                        + ", privacyBlocked=" + privacyBlocked
                        + ", snapshotBlocked=" + snapshotBlocked + ".",
                "Blueprint faction construction response cycle review action archive examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle review action archive rule: a future archive owner must show readable archive reason, retention label, privacy label, result snapshot, and replay reference before preserving a closed archived review action.",
                "Blueprint faction construction response cycle review action archive boundary: this audit does not write archives, redact records, delete records, update status, enqueue notifications, move evidence, refresh archives, reveal hidden faction data, alter job state, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleReviewActionArchiveAuditSmoke checks response cycle review action archive coverage, close and privacy blockers, archive readability, future archive boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
