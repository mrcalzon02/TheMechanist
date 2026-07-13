package mechanist;

import java.util.List;

/** Smoke for the Phase 18 production quality definition audit surface. */
final class Milestone03ProductionQualityDefinitionAuditSmoke {
    public static void main(String[] args) {
        List<String> audit = ProductionQualityTraceAuthority.definitionAuditLines();
        requireContains(audit, "owner=ProductionQualityTraceAuthority", "quality trace owner");
        requireContains(audit, "capInputs=doctrine+recipe+machine+material+facility+tool+manual operator", "cap input list");
        requireContains(audit, "limiterOwner=ProductionQualityTraceAuthority", "limiter owner");
        requireContains(audit, "batchOwner=ProductionBatchAuthority", "batch owner");
        requireContains(audit, "issueTagOwner=ProductionBatchIssueAuthority", "issue tag owner");
        requireContains(audit, "provenanceOwner=ItemProvenanceRecord", "provenance owner");
        requireContains(audit, "material comes from named consumed input units", "material cap boundary");
        requireContains(audit, "worker quality does not cap immediate manual Craft", "worker boundary");
        requireContains(audit, "one manual Craft action creates one batch ID", "batch identity rule");
        requireContains(audit, "reduce ordinary resale value by 40%", "defect consequence");
        requireContains(audit, "item statistics, law enforcement, contamination effects, recalls, and counterfeit enforcement remain future owners", "effect boundary");
        requireContains(audit, "defective batch", "defective tag");
        requireContains(audit, "contaminated batch", "contaminated tag");
        requireContains(audit, "restricted batch", "restricted tag");
        requireContains(audit, "faction-certified batch", "certified tag");
        requireContains(audit, "logs final output quality, main quality limiter, fatigue band, batch state, and defect risk",
                "completion readback fields");
        requireContains(audit, "preserves that summary in shared operation history", "completion history preservation");
        requireContains(audit, "crafting panel Status and History actions", "completion panel status/history actions");
        requireContains(audit, "workbench Status and History actions", "completion workbench status/history actions");
        requireContains(audit, "machine interaction Status and History actions", "completion interaction status/history actions");
        requireContains(audit, "scope queue counts, live operations, latest completion, readiness, and completed records to the machine being operated",
                "machine-scoped production status fields");
        requireContains(audit, "workbench opened through Operate keeps its machine target while recipes change",
                "workbench recipe target persistence");
        requireContains(audit, "forecast and manual Craft use that machine for compatibility, condition, quality, fatigue, wear, provenance, and completion history",
                "workbench machine execution fields");
        requireContains(audit, "Operate and base-machine Craft open the same machine-bound workbench",
                "workbench entry route parity");
        requireContains(audit, "list includes only known recipes compatible with the operated machine",
                "workbench recipe compatibility filter");
        requireContains(audit, "assigned queued production advances from ordinary elapsed game turns",
                "workbench staffed background execution");
        requireContains(audit, "while the player may leave the workbench",
                "workbench staffed background independence");
        requireContains(audit, "blockers pause non-mutatingly",
                "workbench staffed blocker boundary");
        requireContains(audit, "Staff Jobs exposes compatible known generated jobs with category, readiness, and page controls",
                "workbench staffed job selector");
        requireContains(audit, "worker cycling reuses role/skill validation",
                "workbench staffed worker validation");
        requireContains(audit, "queue controls add or remove runs within 0 to 20",
                "workbench staffed queue controls");
        requireContains(audit, "changing jobs clears the prior queue and progress",
                "workbench staffed queue reset");
        requireContains(audit, "material shortage policy can wait, release the assigned worker, or cancel the queue",
                "workbench material policy choices");
        requireContains(audit, "capacity-checked claimed-room faction container",
                "workbench output container routing");
        requireContains(audit, "interactable floor pile",
                "workbench floor output routing");
        requireContains(audit, "no-room policy can wait, release, cancel, or dump to floor",
                "workbench output blocker policy choices");
        requireContains(audit, "Policies, progress, and last blocker persist with the machine",
                "workbench policy persistence");
        requireContains(audit, "base-wide board prioritizes blocked and running machines",
                "production board attention order");
        requireContains(audit, "reuses worker validation, bounded queue mutation, policy, clear, selected-machine Status and History, and staffed Workbench controls",
                "production board shared control boundaries");
        requireContains(audit, "production_status", "completion status command");
        requireContains(audit, "production_history", "completion history command");
        requireContains(audit, "producing room/facility/machine/operator", "provenance identity fields");
        requireContains(audit, "repair/modification history", "repair history field");
        requireContains(audit, "Milestone03ProductionQualityDefinitionAuditSmoke", "guard reference");
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Production quality definition audit leaked implementation text: " + line);
            }
        }
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProductionQualityDefinitionAuditSmoke() { }
}
