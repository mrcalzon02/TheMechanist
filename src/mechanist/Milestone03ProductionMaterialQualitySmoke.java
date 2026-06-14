package mechanist;

import java.util.Set;

/** Smoke for named-input material selection and output-quality capping. */
final class Milestone03ProductionMaterialQualitySmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.inventory.clear();
        game.baseStorage.clear();
        game.inventory.add("Fine Mechanical detritus");
        game.baseStorage.add("Junk Mechanical detritus");
        CraftingRecipe one = new CraftingRecipe("One Input", "Tool", Faction.HIVER,
                "Fine Tools Patterns", 'w', 0, 0, 1, 0, 1, 0, 0, "Mechanics", "test").input("Mechanical detritus", 1);
        ProductionMaterialQualityAuthority.MaterialQuality carried = ProductionMaterialQualityAuthority.evaluate(game, one);
        require(carried.limitingTier() == 4, "carried Fine unit should be selected before stored Junk unit");

        CraftingRecipe two = new CraftingRecipe("Two Inputs", "Tool", Faction.HIVER,
                "Fine Tools Patterns", 'w', 0, 0, 1, 0, 1, 0, 0, "Mechanics", "test").input("Mechanical detritus", 2);
        ProductionMaterialQualityAuthority.MaterialQuality mixed = ProductionMaterialQualityAuthority.evaluate(game, two);
        require(mixed.limitingTier() == 0, "lowest consumed quality should cap mixed inputs");
        ProductionQualityTraceAuthority.QualityTrace trace = ProductionQualityTraceAuthority.evaluate(
                Set.of("Fine Tools Patterns"), "Fine Tools Patterns", "Fine", mixed.limitingTier());
        require("Junk".equals(trace.outputQuality()), "Junk named input should cap output quality");
        require(trace.limiterLabel().contains("named input material"), "trace should name material limiter");
        game.unlockedKnowledges.add("Fine Tools Patterns");
        BaseObject fineMachine = new BaseObject("Fine Bench", 'w', 0, 0, 0, 0);
        fineMachine.qualityName = "Fine";
        require("Junk".equals(game.cappedProductionQuality(fineMachine, two)),
                "compact recipe status should use the same material cap as execution");

        CraftingRecipe abstractOnly = new CraftingRecipe("Abstract", "Tool", Faction.HIVER,
                "Fine Tools Patterns", 'w', 1, 1, 1, 0, 1, 0, 0, "Mechanics", "test");
        require(!ProductionMaterialQualityAuthority.evaluate(game, abstractOnly).active(),
                "abstract supplies and parts should leave material quality open");
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03ProductionMaterialQualitySmoke() { }
}
