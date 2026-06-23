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
