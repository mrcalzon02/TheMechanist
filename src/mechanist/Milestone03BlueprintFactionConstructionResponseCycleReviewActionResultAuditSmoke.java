package mechanist;

import java.util.List;

/** Smoke for audit-only faction construction response cycle review action result contracts. */
final class Milestone03BlueprintFactionConstructionResponseCycleReviewActionResultAuditSmoke {
    public static void main(String[] args) {
        List<BlueprintFactionConstructionResponseCycleReviewActionResultAuthority.ResponseCycleReviewActionResult> samples =
                BlueprintFactionConstructionResponseCycleReviewActionResultAuthority.sampleResults();
        List<String> audit = BlueprintFactionConstructionResponseCycleReviewActionResultAuthority.definitionAuditLines();

        require(samples.size() == 3, "expected three sample response cycle review action results");
        requireContains(audit, "owner=BlueprintFactionConstructionResponseCycleReviewActionResultAuthority", "result owner");
        requireContains(audit, "handoffOwner=BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority", "handoff owner");
        requireContains(audit, "actionOwner=BlueprintFactionConstructionResponseCycleReviewActionAuthority", "action owner");
        requireContains(audit, "readiness=audit-only", "audit boundary");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-id boundary");
        requireContains(audit, "handoff readiness, readable command outcome, audit ledger readiness", "result gates");
        requireContains(audit, "sampleResults=3", "sample count");
        requireContains(audit, "ready=1", "ready count");
        requireContains(audit, "blocked=2", "blocked count");
        requireContains(audit, "handoffBlocked=2", "handoff blocked count");
        requireContains(audit, "outcomeBlocked=1", "outcome blocked count");
        requireContains(audit, "rollbackBlocked=1", "rollback blocked count");
        requireContains(audit, "followupBlocked=1", "follow-up blocked count");
        requireContains(audit, "job-storage-public response cycle review action result for Storage Crate", "storage result");
        requireContains(audit, "state=RESPONSE_CYCLE_REVIEW_ACTION_RESULT_READY", "ready state");
        requireContains(audit, "Inspect completed site", "ready action label");
        requireContains(audit, "job-sensor-restricted response cycle review action result for Security Sensor Mast", "sensor result");
        requireContains(audit, "response cycle review action handoff blocked", "handoff blocker");
        requireContains(audit, "job-shop-public response cycle review action result for Licensed Shop Counter", "shop result");
        requireContains(audit, "review action command outcome not readable", "outcome blocker");
        requireContains(audit, "readable command outcome, audit ledger readiness, rollback outcome, follow-up boundary, and notification boundary", "readability rule");
        requireContains(audit, "does not record result rows", "record boundary");
        requireContains(audit, "complete construction", "completion boundary");
        requireContains(audit, "Milestone03BlueprintFactionConstructionResponseCycleReviewActionResultAuditSmoke", "guard reference");

        BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority.ResponseCycleReviewActionHandoff handoff =
                BlueprintFactionConstructionResponseCycleReviewActionHandoffAuthority.sampleHandoffs().get(0);
        BlueprintFactionConstructionResponseCycleReviewActionResultAuthority.ResponseCycleReviewActionResult direct =
                BlueprintFactionConstructionResponseCycleReviewActionResultAuthority.resultFor(handoff, true, true, true, true, true);
        require(direct.resultReady(), "direct response cycle review action result should be ready");
        requireContains(direct.boundaryLine(), "audit only, no review-action result mutation", "direct boundary");

        for (BlueprintFactionConstructionResponseCycleReviewActionResultAuthority.ResponseCycleReviewActionResult sample : samples) {
            requireNotBlank(sample.jobId(), "job id");
            requireNotBlank(sample.blueprintName(), "blueprint name");
            requireNotBlank(sample.factionName(), "faction name");
            requireNotBlank(sample.resultState(), "result state");
            requireNotBlank(sample.actionLine(), "action line");
            requireNotBlank(sample.blockerLine(), "blocker line");
            requireNotBlank(sample.boundaryLine(), "boundary line");
        }
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Blueprint faction construction response cycle review action result audit leaked implementation text: " + line);
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

    private Milestone03BlueprintFactionConstructionResponseCycleReviewActionResultAuditSmoke() { }
}
