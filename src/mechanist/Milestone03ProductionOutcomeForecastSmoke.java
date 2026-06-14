package mechanist;

/** Smoke for quality-sensitive production value, charge, and defect forecasts. */
final class Milestone03ProductionOutcomeForecastSmoke {
    public static void main(String[] args) {
        ProductionRecipe common = ProductionRecipe.create("Autopistol", Faction.HIVER, "Common",
                "Common Ballistics Patterns", "Common Forge");
        ProductionRecipe fine = ProductionRecipe.create("Autopistol", Faction.HIVER, "Fine",
                "Fine Ballistics Patterns", "Fine Forge");

        require(fine.estimatedValue() > common.estimatedValue(), "fine output should forecast greater value");
        require(fine.outputCharges() > common.outputCharges(), "fine output should forecast more usable charges");
        require(fine.estimatedDefectPercent() < common.estimatedDefectPercent(),
                "fine output should forecast lower defect risk");

        ProductionRecipe scavenger = ProductionRecipe.create("Autopistol", Faction.SCAVENGER, "Common",
                "Common Ballistics Patterns", "Common Forge");
        require(scavenger.estimatedDefectPercent() != common.estimatedDefectPercent(),
                "faction manufacturing profile should remain visible in defect risk");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03ProductionOutcomeForecastSmoke() { }
}
