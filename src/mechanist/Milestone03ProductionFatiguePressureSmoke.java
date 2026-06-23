package mechanist;

import java.util.List;

/** Smoke for fatigue-pressure forecasting, blocking, batch risk, and provenance. */
final class Milestone03ProductionFatiguePressureSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.fatigue = 19;
        require(ProductionFatiguePressureAuthority.evaluate(game, 2).defectRiskAdd() == 0,
                "ready fatigue should not add defect risk");
        game.fatigue = 20;
        require(ProductionFatiguePressureAuthority.evaluate(game, 2).defectRiskAdd() == 2,
                "slightly tired fatigue should add two defect points");
        game.fatigue = 45;
        ProductionFatiguePressureAuthority.FatiguePressure tired =
                ProductionFatiguePressureAuthority.evaluate(game, 3);
        require(tired.defectRiskAdd() == 5 && !tired.blocked(), "tired fatigue should add five points without blocking");
        game.fatigue = 75;
        require(ProductionFatiguePressureAuthority.evaluate(game, 1).blocked(),
                "established exhausted band should block manual Craft");

        ProductionRecipe recipe = ProductionRecipe.create("Test Tool", Faction.HIVER, "Common",
                "Common Tool Patterns", "Test Forge");
        BaseObject machine = new BaseObject("Test Forge", 'f', 0, 0, 0, 0);
        machine.integrity = 3;
        ProductionOperatorSkillAuthority.OperatorSkill operator = new ProductionOperatorSkillAuthority.OperatorSkill(
                "Mechanics", "Mechanics", 8, "skilled", 0, 4, "Fine");
        int baseRisk = recipe.estimatedDefectPercent(machine, operator.defectRiskAdjust());
        ProductionBatchAuthority.BatchDisposition batch = ProductionBatchAuthority.assess(
                recipe, machine, operator, tired, 12, 100, 99L);
        require(batch.defectRiskPercent() == baseRisk + 5, "batch risk should include fatigue pressure");

        ItemProvenanceRecord made = ItemProvenanceRecord.produced(
                recipe, machine, null, 12, "Test Operator", null, operator, null, batch, tired);
        requireContains(made.qualityContextLines(), "Production fatigue pressure: tired", "provenance fatigue pressure");
        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(made.encode());
        require(decoded != null && made.productionPressure.equals(decoded.productionPressure),
                "fatigue pressure should survive save encoding");
        require(made.productionPressure.equals(ItemProvenanceRecord.transferred(
                decoded, made.itemName, null, 13, "moved to storage").productionPressure),
                "fatigue pressure should survive transfer");
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProductionFatiguePressureSmoke() { }
}
