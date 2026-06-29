package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction response cycle review action review close contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority.ResponseCycleReviewActionReviewClose> samples =
                BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority.sampleClosures();
        List<String> audit = BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle review action review closes");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority", "close owner");
        requireContains(audit, "followupOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority", "follow-up owner");
        requireContains(audit, "resultOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewResultAuthority", "result owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "follow-up readiness, readable close decision, status return declaration", "close gates");
        requireContains(audit, "sampleClosures=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "followupBlocked=2", "follow-up blocked count");
        requireContains(audit, "decisionBlocked=1", "decision blocked count");
        requireContains(audit, "statusBlocked=1", "status blocked count");
        requireContains(audit, "evidenceBlocked=1", "evidence blocked count");
        requireContains(audit, "job-storage-public response cycle review action review close for Storage Crate", "storage close");
        requireContains(audit, "state=RESPONSE_CYCLE_REVIEW_ACTION_REVIEW_CLOSE_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle review action review close for Security Sensor Mast", "sensor close");
        requireContains(audit, "response cycle review action review follow-up blocked", "follow-up blocker");
        requireContains(audit, "job-shop-public response cycle review action review close for Licensed Shop Counter", "shop close");
        requireContains(audit, "review-action review close decision not readable", "decision blocker");
        requireContains(audit, "readable close decision, status return declaration, notification return declaration, evidence retention boundary, and archive refresh boundary", "readability rule");
        requireContains(audit, "does not close follow-up", "close boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuditSmoke", "guard reference");

        BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority.ResponseCycleReviewActionReviewFollowup followup =
                BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority.sampleFollowups().get(0);
        BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority.ResponseCycleReviewActionReviewClose direct =
                BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority.closeFor(followup, true, true, true, true, true);
        require(direct.closeReady(), "direct response cycle review action review close should be ready");
        requireContains(direct.boundaryLine(), "audit only, no review-action review close mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuthority.ResponseCycleReviewActionReviewClose sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.closeState(), "close state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response cycle review action review close audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewCloseAuditSmoke() { }
}
