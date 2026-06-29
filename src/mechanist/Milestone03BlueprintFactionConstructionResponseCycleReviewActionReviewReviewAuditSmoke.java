package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction response cycle review action review review contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuthority.ResponseCycleReviewActionReviewReview> samples =
                BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuthority.sampleReviews();
        List<String> audit = BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle review action review reviews");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuthority", "review owner");
        requireContains(audit, "readbackOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewReadbackAuthority", "readback owner");
        requireContains(audit, "archiveOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewArchiveAuthority", "archive owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "readback readiness, readable reviewer context, evidence links", "review gates");
        requireContains(audit, "sampleReviews=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "readbackBlocked=2", "readback blocked count");
        requireContains(audit, "reviewerBlocked=1", "reviewer blocked count");
        requireContains(audit, "actionsBlocked=1", "actions blocked count");
        requireContains(audit, "privacyBlocked=1", "privacy blocked count");
        requireContains(audit, "job-storage-public response cycle review action review review for Storage Crate", "storage review");
        requireContains(audit, "state=RESPONSE_CYCLE_REVIEW_ACTION_REVIEW_REVIEW_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle review action review review for Security Sensor Mast", "sensor review");
        requireContains(audit, "response cycle review action review readback blocked", "readback blocker");
        requireContains(audit, "job-shop-public response cycle review action review review for Licensed Shop Counter", "shop review");
        requireContains(audit, "review-action review reviewer context not readable", "reviewer blocker");
        requireContains(audit, "readable reviewer context, evidence links, readable allowed actions, privacy reminder, and follow-up boundary", "readability rule");
        requireContains(audit, "does not reopen review-action reviews", "reopen boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuditSmoke", "guard reference");

        BlueprintFactionConstructionResponseCycleReviewActionReviewReadbackAuthority.ResponseCycleReviewActionReviewReadback readback =
                BlueprintFactionConstructionResponseCycleReviewActionReviewReadbackAuthority.sampleReadbacks().get(0);
        BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuthority.ResponseCycleReviewActionReviewReview direct =
                BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuthority.reviewFor(readback, true, true, true, true, true);
        require(direct.reviewReady(), "direct response cycle review action review review should be ready");
        requireContains(direct.boundaryLine(), "audit only, no review-action review review mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuthority.ResponseCycleReviewActionReviewReview sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.reviewState(), "review state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response cycle review action review review audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuditSmoke() { }
}
