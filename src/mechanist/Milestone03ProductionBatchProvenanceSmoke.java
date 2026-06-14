package mechanist;

/** Smoke for shared production-batch identity and recorded defect disposition. */
final class Milestone03ProductionBatchProvenanceSmoke {
    public static void main(String[] args) {
        ProductionRecipe recipe = ProductionRecipe.create("Test Tool", Faction.HIVER, "Common",
                "Common Tool Patterns", "Test Forge");
        BaseObject machine = new BaseObject("Test Forge", 'f', 0, 0, 0, 0);
        machine.integrity = 3;
        ProductionOperatorSkillAuthority.OperatorSkill operator = new ProductionOperatorSkillAuthority.OperatorSkill(
                "Mechanics", "Mechanics", 8, "skilled", 0);

        ProductionBatchAuthority.BatchDisposition failed = ProductionBatchAuthority.assess(
                recipe, machine, operator, 12, 1, 100L);
        ProductionBatchAuthority.BatchDisposition passed = ProductionBatchAuthority.assess(
                recipe, machine, operator, 12, 100, 101L);
        require(failed.batchId().startsWith("BATCH-12-"), "batch id should include production turn");
        require("defect flagged".equals(failed.defectState()), "low inspection roll should flag the batch");
        require("passed inspection".equals(passed.defectState()), "high inspection roll should pass the batch");
        require(failed.lines().get(2).contains("not yet reduced"), "stat-effect boundary should remain explicit");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03ProductionBatchProvenanceSmoke() { }
}
