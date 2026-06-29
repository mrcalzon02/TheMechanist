package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction response cycle review action archive contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleReviewActionArchiveAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority.ResponseCycleReviewActionArchive> samples =
                BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority.sampleArchives();
        List<String> audit = BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle review action archives");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority", "archive owner");
        requireContains(audit, "closeOwner=BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority", "close owner");
        requireContains(audit, "followupOwner=BlueprintFactionConstructionResponseCycleReviewActionFollowupAuthority", "follow-up owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "close readiness, readable archive reason, retention label", "archive gates");
        requireContains(audit, "sampleArchives=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "closeBlocked=2", "close blocked count");
        requireContains(audit, "reasonBlocked=1", "reason blocked count");
        requireContains(audit, "privacyBlocked=1", "privacy blocked count");
        requireContains(audit, "snapshotBlocked=1", "snapshot blocked count");
        requireContains(audit, "job-storage-public response cycle review action archive for Storage Crate", "storage archive");
        requireContains(audit, "state=RESPONSE_CYCLE_REVIEW_ACTION_ARCHIVE_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle review action archive for Security Sensor Mast", "sensor archive");
        requireContains(audit, "response cycle review action close blocked", "close blocker");
        requireContains(audit, "job-shop-public response cycle review action archive for Licensed Shop Counter", "shop archive");
        requireContains(audit, "review action archive reason not readable", "reason blocker");
        requireContains(audit, "readable archive reason, retention label, privacy label, result snapshot, and replay reference", "readability rule");
        requireContains(audit, "does not write archives", "archive boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleReviewActionArchiveAuditSmoke", "guard reference");

        BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority.ResponseCycleReviewActionClose close =
                BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority.sampleClosures().get(0);
        BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority.ResponseCycleReviewActionArchive direct =
                BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority.archiveFor(close, true, true, true, true, true);
        require(direct.archiveReady(), "direct response cycle review action archive should be ready");
        requireContains(direct.boundaryLine(), "audit only, no review-action archive mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority.ResponseCycleReviewActionArchive sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.archiveState(), "archive state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response cycle review action archive audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleReviewActionArchiveAuditSmoke() { }
}
