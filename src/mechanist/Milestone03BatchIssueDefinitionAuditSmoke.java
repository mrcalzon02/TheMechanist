package mechanist;

import java.util.List;

/** Smoke for the Phase 18 batch issue definition audit surface. */
final class Milestone03BatchIssueDefinitionAuditSmoke {
    public static void main(String[] args) {
        List<String> audit = ProductionBatchIssueAuthority.definitionAuditLines();
        requireContains(audit, "owner=ProductionBatchIssueAuthority", "batch issue owner");
        requireContains(audit, "batchOwner=ProductionBatchAuthority", "batch owner");
        requireContains(audit, "provenanceOwner=ItemProvenanceRecord", "provenance owner");
        requireContains(audit, "good batch", "good disposition tag");
        requireContains(audit, "defective batch", "defective disposition tag");
        requireContains(audit, "contaminated batch", "contaminated tag");
        requireContains(audit, "unstable batch", "unstable tag");
        requireContains(audit, "counterfeit batch", "counterfeit tag");
        requireContains(audit, "stolen-risk batch", "stolen-risk tag");
        requireContains(audit, "restricted batch", "restricted tag");
        requireContains(audit, "faction-certified batch", "certified tag");
        requireContains(audit, "recallFlag=reserved future owner", "recall reservation");
        requireContains(audit, "do not enforce recall, seizure, law, reputation, contamination damage, or counterfeit penalties", "future effect boundary");
        requireContains(audit, "only existing defect appraisal changes ordinary resale value", "ordinary value boundary");
        requireContains(audit, "item statistics and ordinary use effects remain unchanged", "stat boundary");
        requireContains(audit, "tags require inspection, recipe, law, faction, or source metadata evidence", "evidence boundary");
        requireContains(audit, "Milestone03BatchIssueDefinitionAuditSmoke", "guard reference");

        ProductionRecipe recipe = ProductionRecipe.create("Radiant mineral dust", Faction.SCAVENGER, "Common",
                "Chemical Synthesis", "Chem Bench");
        ProductionBatchAuthority.BatchDisposition defective = ProductionBatchAuthority.assess(
                recipe, new BaseObject("Chem Bench", 'f', 0, 0, 0, 0), null, 33, 1, 404L);
        String manualTags = ProductionBatchIssueAuthority.tagsFor(recipe, defective);
        requireContains(manualTags, "defective batch", "manual defective tag");
        requireContains(manualTags, "contaminated batch", "manual contaminated tag");
        requireContains(manualTags, "unstable batch", "manual unstable tag");

        FactionRecipeVariant restricted = firstRestrictedCertifiedVariant();
        require(restricted != null, "expected restricted faction-certified variant");
        String generatedTags = ProductionBatchIssueAuthority.tagsFor(restricted, null);
        requireContains(generatedTags, "restricted batch", "generated restricted tag");
        requireContains(generatedTags, "faction-certified batch", "generated certified tag");

        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Batch issue definition audit leaked implementation text: " + line);
            }
        }
    }

    private static FactionRecipeVariant firstRestrictedCertifiedVariant() {
        for (FactionRecipeVariant variant : FactionRecipeVariantApi.generatedFactionVariants()) {
            if (variant == null) continue;
            boolean certified = variant.faction == Faction.ARBITES || variant.faction == Faction.IMPERIAL_GUARD
                    || variant.faction == Faction.MECHANICUS || variant.faction == Faction.NOBLE;
            String law = variant.lawStatus == null ? "" : variant.lawStatus.toLowerCase(java.util.Locale.ROOT);
            if (certified && (law.contains("restricted") || law.contains("contraband") || law.contains("illegal"))) {
                return variant;
            }
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03BatchIssueDefinitionAuditSmoke() { }
}
