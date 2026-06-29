package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction response cycle review action readback contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleReviewActionReadbackAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority.ResponseCycleReviewActionReadback> samples =
                BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority.sampleReadbacks();
        List<String> audit = BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle review action readbacks");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority", "readback owner");
        requireContains(audit, "archiveOwner=BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority", "archive owner");
        requireContains(audit, "closeOwner=BlueprintFactionConstructionResponseCycleReviewActionCloseAuthority", "close owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "archive readiness, archive lookup readiness, readable summary", "readback gates");
        requireContains(audit, "sampleReadbacks=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "archiveBlocked=2", "archive blocked count");
        requireContains(audit, "lookupBlocked=1", "lookup blocked count");
        requireContains(audit, "privacyBlocked=1", "privacy blocked count");
        requireContains(audit, "replayBlocked=1", "replay blocked count");
        requireContains(audit, "job-storage-public response cycle review action readback for Storage Crate", "storage readback");
        requireContains(audit, "state=RESPONSE_CYCLE_REVIEW_ACTION_READBACK_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle review action readback for Security Sensor Mast", "sensor readback");
        requireContains(audit, "response cycle review action archive blocked", "archive blocker");
        requireContains(audit, "job-shop-public response cycle review action readback for Licensed Shop Counter", "shop readback");
        requireContains(audit, "review action archive lookup not ready", "lookup blocker");
        requireContains(audit, "archive lookup readiness, readable summary, privacy filter readiness, readable replay reference, and stale record marker", "readability rule");
        requireContains(audit, "does not read archives from storage", "storage boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleReviewActionReadbackAuditSmoke", "guard reference");

        BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority.ResponseCycleReviewActionArchive archive =
                BlueprintFactionConstructionResponseCycleReviewActionArchiveAuthority.sampleArchives().get(0);
        BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority.ResponseCycleReviewActionReadback direct =
                BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority.readbackFor(archive, true, true, true, true, true);
        require(direct.readbackReady(), "direct response cycle review action readback should be ready");
        requireContains(direct.boundaryLine(), "audit only, no review-action readback mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority.ResponseCycleReviewActionReadback sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.readbackState(), "readback state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response cycle review action readback audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleReviewActionReadbackAuditSmoke() { }
}
