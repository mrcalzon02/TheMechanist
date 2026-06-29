package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction response cycle review action review follow-up contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority.ResponseCycleReviewActionReviewFollowup> samples =
                BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority.sampleFollowups();
        List<String> audit = BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle review action review follow-ups");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority", "follow-up owner");
        requireContains(audit, "resultOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewResultAuthority", "result owner");
        requireContains(audit, "handoffOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewHandoffAuthority", "handoff owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "result readiness, reviewer summary, evidence disposition", "follow-up gates");
        requireContains(audit, "sampleFollowups=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "resultBlocked=2", "result blocked count");
        requireContains(audit, "summaryBlocked=1", "summary blocked count");
        requireContains(audit, "statusBlocked=1", "status blocked count");
        requireContains(audit, "notificationBlocked=1", "notification blocked count");
        requireContains(audit, "job-storage-public response cycle review action review follow-up for Storage Crate", "storage follow-up");
        requireContains(audit, "state=RESPONSE_CYCLE_REVIEW_ACTION_REVIEW_FOLLOWUP_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle review action review follow-up for Security Sensor Mast", "sensor follow-up");
        requireContains(audit, "response cycle review action review result blocked", "result blocker");
        requireContains(audit, "job-shop-public response cycle review action review follow-up for Licensed Shop Counter", "shop follow-up");
        requireContains(audit, "review-action review summary not ready", "summary blocker");
        requireContains(audit, "reviewer summary, evidence disposition, status refresh, notification boundary, and closure boundary", "readability rule");
        requireContains(audit, "does not schedule follow-up", "schedule boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuditSmoke", "guard reference");

        BlueprintFactionConstructionResponseCycleReviewActionReviewResultAuthority.ResponseCycleReviewActionReviewResult result =
                BlueprintFactionConstructionResponseCycleReviewActionReviewResultAuthority.sampleResults().get(0);
        BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority.ResponseCycleReviewActionReviewFollowup direct =
                BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority.followupFor(result, true, true, true, true, true);
        require(direct.followupReady(), "direct response cycle review action review follow-up should be ready");
        requireContains(direct.boundaryLine(), "audit only, no review-action review follow-up mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuthority.ResponseCycleReviewActionReviewFollowup sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.followupState(), "follow-up state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response cycle review action review follow-up audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewFollowupAuditSmoke() { }
}
