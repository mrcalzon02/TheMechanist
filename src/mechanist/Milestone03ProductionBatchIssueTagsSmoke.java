package mechanist;

import java.util.List;

/** Smoke for preserving Phase 9.3 batch issue tags in produced-item provenance. */
final class Milestone03ProductionBatchIssueTagsSmoke {
    public static void main(String[] args) {
        ProductionRecipe recipe = ProductionRecipe.create("Radiant mineral dust", Faction.SCAVENGER, "Common",
                "Chemical Synthesis", "Chem Bench");
        BaseObject machine = new BaseObject("Chem Bench", 'f', 0, 0, 0, 0);
        ProductionOperatorSkillAuthority.OperatorSkill operator = new ProductionOperatorSkillAuthority.OperatorSkill(
                "Chemical Synthesis", "Intellect", 5, "practiced", 0, 2, "Serviceable");
        ProductionBatchAuthority.BatchDisposition batch = ProductionBatchAuthority.assess(
                recipe, machine, operator, 18, 1, 202L);
        String manualTags = ProductionBatchIssueAuthority.tagsFor(recipe, batch);
        requireContains(manualTags, "defective batch", "defective tag");
        requireContains(manualTags, "contaminated batch", "contaminated tag");
        requireContains(manualTags, "unstable batch", "unstable tag");

        ItemProvenanceRecord made = ItemProvenanceRecord.produced(recipe, machine, null, 18,
                "Test Operator", null, operator, null, batch);
        made.batchIssueTags = manualTags;
        requireContains(made.qualityContextLines(), "Batch issue tags: " + manualTags, "batch issue provenance line");
        requireContains(ProductionBatchIssueAuthority.lines(manualTags),
                "only the existing defect appraisal changes ordinary resale value", "issue boundary");

        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(made.encode());
        require(decoded != null && manualTags.equals(decoded.batchIssueTags),
                "batch issue tags should survive save encoding");
        ItemProvenanceRecord transferred = ItemProvenanceRecord.transferred(decoded, made.itemName, null, 19, "moved to storage");
        require(manualTags.equals(transferred.batchIssueTags), "batch issue tags should survive transfer");

        FactionRecipeVariant variant = firstRestrictedVariant();
        require(variant != null, "expected restricted generated variant");
        String generatedTags = ProductionBatchIssueAuthority.tagsFor(variant, null);
        requireContains(generatedTags, "restricted batch", "generated restricted tag");

        String legacy = String.join("~", ItemProvenanceRecord.enc("Legacy Tool"), ItemProvenanceRecord.enc("NONE"),
                ItemProvenanceRecord.enc("legacy maker"), ItemProvenanceRecord.enc("legacy place"),
                ItemProvenanceRecord.enc("legacy inputs"), ItemProvenanceRecord.enc("legacy route"), "3");
        require(ItemProvenanceRecord.decode(legacy) != null, "legacy provenance should remain readable");
    }

    private static FactionRecipeVariant firstRestrictedVariant() {
        for (FactionRecipeVariant variant : FactionRecipeVariantApi.generatedFactionVariants()) {
            String law = variant == null || variant.lawStatus == null ? "" : variant.lawStatus.toLowerCase(java.util.Locale.ROOT);
            if (law.contains("restricted") || law.contains("contraband") || law.contains("illegal")) return variant;
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

    private Milestone03ProductionBatchIssueTagsSmoke() { }
}
