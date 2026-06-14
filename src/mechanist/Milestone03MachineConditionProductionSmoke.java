package mechanist;

import java.util.List;

/** Smoke for machine-integrity blocking and defect-risk forecasting. */
final class Milestone03MachineConditionProductionSmoke {
    public static void main(String[] args) {
        BaseObject machine = new BaseObject("Test Forge", 'f', 0, 0, 0, 0);
        machine.integrity = 0;
        require(!MachineConditionProductionAuthority.evaluate(machine).operational(), "zero-integrity machine should be broken");
        requireContains(MachineConditionProductionAuthority.forecastLines(machine), "Repair the machine", "broken guidance");

        ProductionRecipe product = ProductionRecipe.create("Autopistol", Faction.HIVER, "Common",
                "Common Ballistics Patterns", "Test Forge");
        int brokenBase = product.estimatedDefectPercent();
        machine.integrity = 1;
        require(product.estimatedDefectPercent(machine) == brokenBase + 12, "critical machine should add twelve defect points");
        requireContains(MachineConditionProductionAuthority.forecastLines(machine), "+12 percentage points", "critical surcharge");
        machine.integrity = 2;
        require(product.estimatedDefectPercent(machine) == brokenBase + 6, "worn machine should add six defect points");
        machine.integrity = 3;
        require(product.estimatedDefectPercent(machine) == brokenBase, "serviceable machine should not add defect risk");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03MachineConditionProductionSmoke() { }
}
