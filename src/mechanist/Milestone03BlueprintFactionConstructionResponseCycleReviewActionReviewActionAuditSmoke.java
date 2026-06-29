package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction response cycle review action review action contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuthority.ResponseCycleReviewActionReviewAction> samples =
                BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuthority.sampleReviewActions();
        List<String> audit = BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle review action review actions");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuthority", "review action owner");
        requireContains(audit, "reviewOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewAuthority", "review owner");
        requireContains(audit, "readbackOwner=BlueprintFactionConstructionResponseCycleReviewActionReadbackAuthority", "readback owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "review readiness, permission readiness, confirmation readiness", "review action gates");
        requireContains(audit, "sampleReviewActions=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "reviewBlocked=2", "review blocked count");
        requireContains(audit, "permissionBlocked=1", "permission blocked count");
        requireContains(audit, "confirmationBlocked=1", "confirmation blocked count");
        requireContains(audit, "auditTextBlocked=1", "audit text blocked count");
        requireContains(audit, "job-storage-public response cycle review action review action for Storage Crate", "storage review action");
        requireContains(audit, "state=RESPONSE_CYCLE_REVIEW_ACTION_REVIEW_ACTION_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle review action review action for Security Sensor Mast", "sensor review action");
        requireContains(audit, "response cycle review action review blocked", "review blocker");
        requireContains(audit, "job-shop-public response cycle review action review action for Licensed Shop Counter", "shop review action");
        requireContains(audit, "review-action review action permission not ready", "permission blocker");
        requireContains(audit, "permission readiness, confirmation readiness, evidence selection, audit text, and non-reopen boundary", "readability rule");
        requireContains(audit, "does not execute review-action review actions", "execution boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuditSmoke", "guard reference");

        BlueprintFactionConstructionResponseCycleReviewActionReviewAuthority.ResponseCycleReviewActionReview review =
                BlueprintFactionConstructionResponseCycleReviewActionReviewAuthority.sampleReviews().get(0);
        BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuthority.ResponseCycleReviewActionReviewAction direct =
                BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuthority.actionFor(review, true, true, true, true, true);
        require(direct.actionReady(), "direct response cycle review action review action should be ready");
        requireContains(direct.boundaryLine(), "audit only, no review-action review action mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuthority.ResponseCycleReviewActionReviewAction sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.actionState(), "action state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response cycle review action review action audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewActionAuditSmoke() { }
}
