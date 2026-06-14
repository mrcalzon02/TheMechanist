package mechanist;

/** Smoke for manual production operator-skill bands and defect adjustments. */
final class Milestone03ProductionOperatorSkillSmoke {
    public static void main(String[] args) {
        require("Firearms".equals(ProductionOperatorSkillAuthority.coreStatFor("Firearms")), "firearms mapping");
        require("Intellect".equals(ProductionOperatorSkillAuthority.coreStatFor("Medical")), "medical mapping");
        require("Charm".equals(ProductionOperatorSkillAuthority.coreStatFor("Commerce")), "commerce mapping");
        require("Mechanics".equals(ProductionOperatorSkillAuthority.coreStatFor("Mechanics")), "mechanics mapping");

        ProductionOperatorSkillAuthority.OperatorSkill fallback = ProductionOperatorSkillAuthority.evaluate(null, "Mechanics");
        require("practiced".equals(fallback.band()), "fallback operator should be practiced");
        require(fallback.defectRiskAdjust() == 3, "practiced operator should add three defect points");

        ProductionRecipe product = ProductionRecipe.create("Tool", Faction.HIVER, "Common", "Common Tools Patterns", "Bench");
        BaseObject machine = new BaseObject("Bench", 'w', 0, 0, 0, 0);
        machine.integrity = 3;
        int baseline = product.estimatedDefectPercent(machine);
        require(product.estimatedDefectPercent(machine, 6) == baseline + 6, "novice adjustment should increase risk");
        require(product.estimatedDefectPercent(machine, -3) == Math.max(1, baseline - 3), "expert adjustment should reduce risk");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03ProductionOperatorSkillSmoke() { }
}
