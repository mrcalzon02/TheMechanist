package mechanist;

import java.util.List;
import java.util.Set;

/** Smoke for preserving production-quality context in item provenance. */
final class Milestone03QualityProvenanceSmoke {
    public static void main(String[] args) {
        ProductionQualityTraceAuthority.QualityTrace trace = ProductionQualityTraceAuthority.evaluate(
                Set.of("Fine Ballistics Patterns"), "Serviceable Ballistics Patterns", "Masterwork", 3, 4, 5);
        ProductionRecipe recipe = ProductionRecipe.create("Autopistol", Faction.HIVER, trace.outputQuality(),
                "Serviceable Ballistics Patterns", "Test Forge");
        BaseObject machine = new BaseObject("Test Forge", 'f', 0, 0, 0, 0);
        machine.qualityName = "Masterwork";
        ProductionOperatorSkillAuthority.OperatorSkill operator = new ProductionOperatorSkillAuthority.OperatorSkill(
                "Firearms", "Firearms", 9, "skilled", 0, 4, "Fine");
        ProductionKnowledgeSourceAuthority.KnowledgeSource knowledge = new ProductionKnowledgeSourceAuthority.KnowledgeSource(
                true, false, true, false, "", recipe.knowledgeName, "installed machine doctrine", Set.of(recipe.knowledgeName), List.of());
        ProductionBatchAuthority.BatchDisposition batch = ProductionBatchAuthority.assess(
                recipe, machine, operator, 42, 1, 77L);
        ItemProvenanceRecord made = ItemProvenanceRecord.produced(
                recipe, machine, null, 42, "Test Operator", trace, operator, knowledge, batch);
        made.batchIssueTags = ProductionBatchIssueAuthority.tagsFor(recipe, batch);

        requireContains(made.qualityContextLines(), "Production quality: Serviceable", "output quality");
        requireContains(made.qualityContextLines(), "Knowledge source: Serviceable Ballistics Patterns", "knowledge source");
        requireContains(made.qualityContextLines(), "Knowledge provider: installed machine doctrine", "knowledge provider");
        requireContains(made.qualityContextLines(), "Production batch: BATCH-42-", "batch identity");
        requireContains(made.qualityContextLines(), "Batch inspection: defect flagged", "batch disposition");
        requireContains(made.qualityContextLines(), "Batch issue tags: defective batch", "batch issue tags");
        requireContains(made.qualityContextLines(), "Producing machine quality: Masterwork", "machine quality");
        requireContains(made.qualityContextLines(), "Producing machine: Test Forge / production station f at 0,0", "machine identity");
        requireContains(made.qualityContextLines(), "Producing operator: Test Operator", "operator identity");
        requireContains(made.qualityContextLines(), "Production workforce: immediate manual Craft / operator Test Operator", "production mode");
        requireContains(made.qualityContextLines(), "Producing machine condition: serviceable", "machine condition");
        requireContains(made.qualityContextLines(), "Recorded quality limiter: recipe pattern", "limiter");
        requireContains(made.qualityContextLines(), "Producing operator skill: Firearms via Firearms 9", "operator skill");
        requireContains(made.qualityContextLines(), "Producing operator band: skilled", "operator band");
        requireContains(made.qualityContextLines(), "Consumed material quality cap: Serviceable", "material quality");
        requireContains(made.qualityContextLines(), "Producing facility quality cap: Fine", "facility quality");
        requireContains(made.qualityContextLines(), "Equipped production tool quality cap: Masterwork", "tool quality");
        requireContains(made.qualityContextLines(), "Faction production mutation: Civilian / Civic", "faction mutation");

        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(made.encode());
        require(decoded != null, "encoded provenance should decode");
        require(made.outputQuality.equals(decoded.outputQuality), "output quality should survive save encoding");
        require(made.qualityLimiter.equals(decoded.qualityLimiter), "limiter should survive save encoding");
        require(made.operatorSkill.equals(decoded.operatorSkill), "operator skill should survive save encoding");
        require(made.operatorSkillBand.equals(decoded.operatorSkillBand), "operator band should survive save encoding");
        require(made.materialQuality.equals(decoded.materialQuality), "material quality should survive save encoding");
        require(made.facilityQuality.equals(decoded.facilityQuality), "facility quality should survive save encoding");
        require(made.toolQuality.equals(decoded.toolQuality), "tool quality should survive save encoding");
        require(made.factionMutation.equals(decoded.factionMutation), "faction mutation should survive save encoding");
        require(made.producingMachine.equals(decoded.producingMachine), "producing machine should survive save encoding");
        require(made.producingOperator.equals(decoded.producingOperator), "producing operator should survive save encoding");
        require(made.productionMode.equals(decoded.productionMode), "production mode should survive save encoding");
        require(made.knowledgeProvider.equals(decoded.knowledgeProvider), "knowledge provider should survive save encoding");
        require(made.batchId.equals(decoded.batchId), "batch identity should survive save encoding");
        require(made.defectState.equals(decoded.defectState), "defect disposition should survive save encoding");
        require(made.batchIssueTags.equals(decoded.batchIssueTags), "batch issue tags should survive save encoding");

        ItemProvenanceRecord transferred = ItemProvenanceRecord.transferred(decoded, made.itemName, null, 50, "moved to base storage");
        require(decoded.knowledgeSource.equals(transferred.knowledgeSource), "knowledge source should survive transfer");
        require(decoded.knowledgeProvider.equals(transferred.knowledgeProvider), "knowledge provider should survive transfer");
        require(decoded.batchId.equals(transferred.batchId), "batch identity should survive transfer");
        require(decoded.defectState.equals(transferred.defectState), "defect disposition should survive transfer");
        require(decoded.batchIssueTags.equals(transferred.batchIssueTags), "batch issue tags should survive transfer");
        require(decoded.machineQuality.equals(transferred.machineQuality), "machine quality should survive transfer");
        require(decoded.machineCondition.equals(transferred.machineCondition), "machine condition should survive transfer");
        require(decoded.operatorSkill.equals(transferred.operatorSkill), "operator skill should survive transfer");
        require(decoded.operatorSkillBand.equals(transferred.operatorSkillBand), "operator band should survive transfer");
        require(decoded.materialQuality.equals(transferred.materialQuality), "material quality should survive transfer");
        require(decoded.facilityQuality.equals(transferred.facilityQuality), "facility quality should survive transfer");
        require(decoded.toolQuality.equals(transferred.toolQuality), "tool quality should survive transfer");
        require(decoded.factionMutation.equals(transferred.factionMutation), "faction mutation should survive transfer");
        require(decoded.producingMachine.equals(transferred.producingMachine), "producing machine should survive transfer");
        require(decoded.producingOperator.equals(transferred.producingOperator), "producing operator should survive transfer");
        require(decoded.productionMode.equals(transferred.productionMode), "production mode should survive transfer");

        String legacy = String.join("~", ItemProvenanceRecord.enc("Common Legacy Tool"), ItemProvenanceRecord.enc("NONE"),
                ItemProvenanceRecord.enc("legacy maker"), ItemProvenanceRecord.enc("legacy place"),
                ItemProvenanceRecord.enc("legacy inputs"), ItemProvenanceRecord.enc("legacy route"), "3");
        require(ItemProvenanceRecord.decode(legacy) != null, "legacy provenance should remain readable");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03QualityProvenanceSmoke() { }
}
