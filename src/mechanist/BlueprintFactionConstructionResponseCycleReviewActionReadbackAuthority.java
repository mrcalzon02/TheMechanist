package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Audit-only readback contract for future archived faction construction response cycle review actions. */
final class BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority {
    record ResponseCycleReviewActionReadback(String jobId, String blueprintName, String factionName, String readbackState,
                                             boolean archiveReady, boolean archiveLookupReady,
                                             boolean summaryReadable, boolean privacyFilterReady,
                                             boolean replayReferenceReadable, boolean staleRecordMarkerReady,
                                             boolean readbackReady, String actionLine, String blockerLine, String boundaryLine) { }

    private BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority() { }

    static List<ResponseCycleReviewActionReadback> sampleReadbacks() {
        List<BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority.ResponseCycleReviewActionArchive> archives =
                BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority.sampleArchives();
        ArrayList<ResponseCycleReviewActionReadback> rows = new ArrayList<>();
        rows.add(readbackFor(archives.get(0), true, true, true, true, true));
        rows.add(readbackFor(archives.get(1), true, true, true, true, true));
        rows.add(readbackFor(archives.get(2), false, true, false, false, true));
        return List.copyOf(rows);
    }

    static ResponseCycleReviewActionReadback readbackFor(BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority.ResponseCycleReviewActionArchive archive,
                                                         boolean archiveLookupReady, boolean summaryReadable,
                                                         boolean privacyFilterReady, boolean replayReferenceReadable,
                                                         boolean staleRecordMarkerReady) {
        String id = archive == null ? "job-unassigned" : clean(archive.jobId(), "job-unassigned");
        String blueprint = archive == null ? "Unknown blueprint" : clean(archive.blueprintName(), "Unknown blueprint");
        String faction = archive == null ? "Unaffiliated" : clean(archive.factionName(), "Unaffiliated");
        String action = archive == null ? "Review archived review action" : clean(archive.actionLine(), "Review archived review action");
        boolean archiveReady = archive != null && archive.archiveReady();
        ArrayList<String> blockers = new ArrayList<>();
        if (!archiveReady) blockers.add("response cycle review action archive blocked");
        if (!archiveLookupReady) blockers.add("review action archive lookup not ready");
        if (!summaryReadable) blockers.add("review action summary not readable");
        if (!privacyFilterReady) blockers.add("review action privacy filter not ready");
        if (!replayReferenceReadable) blockers.add("review action replay reference not readable");
        if (!staleRecordMarkerReady) blockers.add("review action stale record marker not ready");
        boolean ready = blockers.isEmpty();
        String state = ready ? "RESPONSE_CYCLE_REVIEW_ACTION_READBACK_READY" : "RESPONSE_CYCLE_REVIEW_ACTION_READBACK_BLOCKED";
        String blockerLine = ready ? "none" : String.join("; ", blockers);
        String boundary = id + " response cycle review action readback for " + blueprint
                + " by " + faction
                + " state=" + state
                + "; action=" + action
                + "; archiveReady=" + archiveReady
                + ", archiveLookupReady=" + archiveLookupReady
                + ", summaryReadable=" + summaryReadable
                + ", privacyFilterReady=" + privacyFilterReady
                + ", replayReferenceReadable=" + replayReferenceReadable
                + ", staleRecordMarkerReady=" + staleRecordMarkerReady
                + "; blockers=" + blockerLine
                + "; audit only, no review-action readback mutation.";
        return new ResponseCycleReviewActionReadback(id, blueprint, faction, state, archiveReady, archiveLookupReady,
                summaryReadable, privacyFilterReady, replayReferenceReadable, staleRecordMarkerReady,
                ready, action, blockerLine, boundary);
    }

    static List<String> definitionAuditLines() {
        List<ResponseCycleReviewActionReadback> samples = sampleReadbacks();
        int ready = 0;
        int blocked = 0;
        int archiveBlocked = 0;
        int lookupBlocked = 0;
        int privacyBlocked = 0;
        int replayBlocked = 0;
        for (ResponseCycleReviewActionReadback readback : samples) {
            if (readback.readbackReady()) ready++;
            else blocked++;
            if (!readback.archiveReady()) archiveBlocked++;
            if (!readback.archiveLookupReady()) lookupBlocked++;
            if (!readback.privacyFilterReady()) privacyBlocked++;
            if (!readback.replayReferenceReadable()) replayBlocked++;
        }
        return List.of(
                "Blueprint faction construction response cycle review action readback audit: owner=BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority, archiveOwner=BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority, closeOwner=BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority, readiness=audit-only, ordinaryUiRawIds=false.",
                "Blueprint faction construction response cycle review action readback audit: future archived review actions may be read back only after archive readiness, archive lookup readiness, readable summary, privacy filter readiness, readable replay reference, and stale record marker are all declared.",
                "Blueprint faction construction response cycle review action readback sample audit: sampleReadbacks=" + samples.size()
                        + ", ready=" + ready
                        + ", blocked=" + blocked
                        + ", archiveBlocked=" + archiveBlocked
                        + ", lookupBlocked=" + lookupBlocked
                        + ", privacyBlocked=" + privacyBlocked
                        + ", replayBlocked=" + replayBlocked + ".",
                "Blueprint faction construction response cycle review action readback examples: " + samples.get(0).boundaryLine()
                        + " | " + samples.get(1).boundaryLine()
                        + " | " + samples.get(2).boundaryLine(),
                "Blueprint faction construction response cycle review action readback rule: a future readback owner must show archive lookup readiness, readable summary, privacy filter readiness, readable replay reference, and stale record marker before presenting an archived review action.",
                "Blueprint faction construction response cycle review action readback boundary: this audit does not read archives from storage, write archives, replay commands, reveal hidden faction data, update status, enqueue notifications, alter job state, move evidence, refresh archives, release reservations, or complete construction.",
                "Guard: Milestone03BlueprintFactionConstructionResponseCycleReviewActionReadbackAuditSmoke checks response cycle review action readback coverage, archive and lookup blockers, readback readability, future readback boundaries, and raw-ID hiding."
        );
    }

    private static String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
