package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction response cycle review action review-review action handoff contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionHandoffAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionHandoffAuthority.ResponseCycleReviewActionReviewReviewActionHandoff> samples =
                BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionHandoffAuthority.sampleHandoffs();
        List<String> audit = BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionHandoffAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle review action review review action handoffs");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionHandoffAuthority", "handoff owner");
        requireContains(audit, "actionOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionAuthority", "action owner");
        requireContains(audit, "reviewOwner=BlueprintFactionConstructionResponseCycleReviewActionReviewReviewAuthority", "review owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "action readiness, target resolution, command owner", "handoff gates");
        requireContains(audit, "sampleHandoffs=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "actionBlocked=2", "action blocked count");
        requireContains(audit, "targetBlocked=1", "target blocked count");
        requireContains(audit, "ownerBlocked=1", "owner blocked count");
        requireContains(audit, "turnCostBlocked=1", "turn cost blocked count");
        requireContains(audit, "job-storage-public response cycle review action review review action handoff for Storage Crate", "storage handoff");
        requireContains(audit, "state=RESPONSE_CYCLE_REVIEW_ACTION_REVIEW_REVIEW_ACTION_HANDOFF_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle review action review review action handoff for Security Sensor Mast", "sensor handoff");
        requireContains(audit, "response cycle review action review review action blocked", "action blocker");
        requireContains(audit, "job-shop-public response cycle review action review review action handoff for Licensed Shop Counter", "shop handoff");
        requireContains(audit, "review-action review review action target not resolved", "target blocker");
        requireContains(audit, "target resolution, command owner, rollback preview, turn cost preview, and result text", "readability rule");
        requireContains(audit, "does not hand off commands", "handoff boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionHandoffAuditSmoke", "guard reference");

        BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionAuthority.ResponseCycleReviewActionReviewReviewAction action =
                BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionAuthority.sampleReviewActions().get(0);
        BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionHandoffAuthority.ResponseCycleReviewActionReviewReviewActionHandoff direct =
                BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionHandoffAuthority.handoffFor(
                        action, true, true, true, true, true);
        require(direct.handoffReady(), "direct response cycle review action review review action handoff should be ready");
        requireContains(direct.boundaryLine(), "audit only, no review-action review review action handoff mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionHandoffAuthority.ResponseCycleReviewActionReviewReviewActionHandoff sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.handoffState(), "handoff state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response cycle review action review review action handoff audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleReviewActionReviewReviewActionHandoffAuditSmoke() { }
}
