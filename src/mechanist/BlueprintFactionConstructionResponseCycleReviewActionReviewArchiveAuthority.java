package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only archive contract for future closed faction construction response cycle review-action reviews. */
final class BlueprintFactionConstructionResponseCycleReviewActionReviewArchiveAuthority {
    record ResponseCycleReviewActionReviewArchive(String jobId, String blueprintName, String factionName, String archiveState,
                                                  boolean reviewCloseReady, boolean archiveReasonReadable,
                                                  boolean retentionLabelReady, boolean privacyLabelReady,
                                                  boolean resultSnapshotReady, boolean replayReferenceReady,
                                                  boolean archiveReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleReviewActionReviewArchiveAuthority() { }

    static List<ResponseCycleReviewActionReviewArchive> sampleArchives() {
        List<BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority.ResponseCycleReviewActionReviewClose> closes =
                BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority.sampleClosures();
        ArrayList<ResponseCycleReviewActionReviewArchive> rows = new ArrayList<>();
        rows.add(archiveFor(closes.get(0), true, true, true, true, true));
        rows.add(archiveFor(closes.get(1), true, true, true, true, true));
        rows.add(archiveFor(closes.get(2), false, true, false, false, true));
        return List.copyOf(rows);
    }

    static ResponseCycleReviewActionReviewArchive archiveFor(BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority.ResponseCycleReviewActionReviewClose close,
                                                             boolean archiveReasonReadable, boolean retentionLabelReady,
                                                             boolean privacyLabelReady, boolean resultSnapshotReady,
                                                             boolean replayReferenceReady) {
        String id = close == null ? "job-unassigned" : clean(close.jobId(), "job-unassigned");
        String blueprint = close == null ? "Unknown blueprint" : clean(close.blueprintName(), "Unknown blueprint");
        String faction = close == null ? "Unaffiliated" : clean(close.factionName(), "Unaffiliated");
        String action = close == null ? "Archive review-action review close" : clean(close.actionLine(), "Archive review-action review close");
        boolean closeReady = close != null && close.closeReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!closeReady) blockers.add("response cycle review action review close blocked");
        if (!archiveReasonReadable) blockers.add("review-action review archive reason not readable");
        if (!retentionLabelReady) blockers.add("review-action review retention label not ready");
        if (!privacyLabelReady) blockers.add("review-action review privacy label not ready");
        if (!resultSnapshotReady) blockers.add("review-action review result snapshot not ready");
        if (!replayReferenceReady) blockers.add("review-action review replay reference not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_REVIEW_ACTION_REVIEW_ARCHIVE_READY" : "RESPONSE_CYCLE_REVIEW_ACTION_REVIEW_ARCHIVE_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle review action review archive for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; reviewCloseReady=" + closeReady
                + ", archiveReasonReadable=" + archiveReasonReadable
                + ", retentionLabelReady=" + retentionLabelReady
                + ", privacyLabelReady=" + privacyLabelReady
                + ", resultSnapshotReady=" + resultSnapshotReady
                + ", replayReferenceReady=" + replayReferenceReady
                + "; blockers=" + blockerLine
                + "; audit only, no review-action review archive mutation.";
        return new ResponseCycleReviewActionReviewArchive(id, blueprint, faction, state, closeReady, archiveReasonReadable,
                retentionLabelReady, privacyLabelReady, resultSnapshotReady, replayReferenceReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleReviewActionReviewArchive> samples = sampleArchives();
        int ready = 0;
        int blocked = 0;
        int closeBlocked = 0;
        int reasonBlocked = 0;
        int privacyBlocked = 0;
        int snapshotBlocked = 0;
        for (ResponseCycleReviewActionReviewArchive archive : samples) {
            if (archive.archiveReady()) ready++;
            else blocked++;
            if (!archive.reviewCloseReady()) closeBlocked++;
            if (!archive.archiveReasonReadable()) reasonBlocked++;
            if (!archive.privacyLabelReady()) privacyBlocked++;
            if (!archive.resultSnapshotReady()) snapshotBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle review action review archive audit: owner=BlueprintFactionConstructionResponseCycleReviewActionReviewArchiveAuthority, closeOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority, followupOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle review action review archive audit: future archived review-action review close records may be archived only after close readiness, readable archive reason, retention label, privacy label, result snapshot, and replay reference are all declared.",
                "Blueprint faction construction response cycle review action review archive sample audit: sampleArchives=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", closeBlocked=" + closeBlocked
                        + ", reasonBlocked=" + reasonBlocked
                        + ", privacyBlocked=" + privacyBlocked
                        + ", snapshotBlocked=" + snapshotBlocked + ".",
                "Blueprint faction construction response cycle review action review archive examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle review action review archive rule: a future archive owner must show readable archive reason, retention label, privacy label, result snapshot, and replay reference before preserving a closed archived review-action review.",
                "Blueprint faction construction response cycle review action review archive boundary: this audit does not write archives, redact records, delete records, update status, enqueue notifications, move evidence, refresh archives, reveal hidden faction data, alter job state, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewArchiveAuditSmoke checks response cycle review action review archive coverage, close and privacy blockers, archive readability, future archive boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
