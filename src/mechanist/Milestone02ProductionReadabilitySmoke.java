package mechanist;

import java.util.LinkedHashMap;
import java.util.List;

final class Milestone02ProductionReadabilitySmoke {
    public static void main(String[] args) {
        CraftingRecipe recipe = new CraftingRecipe("Smoke Cutter", "Emergency Cutter", Faction.MECHANICUS,
                "Scrap-Forging Doctrine", 'f', 2, 1, 2, 1, 9, 4, 1, "Mechanics", "A test production run.")
                .input("Mechanical detritus", 1);
        BaseObject machine = new BaseObject("Test Micro Forge", 'f', 0, 0, 0, 0);
        machine.qualityName = "Serviceable";
        machine.integrity = 5;
        machine.assignedWorker = "Hest Var";
        machine.assignedRecipe = "Queued Cutter Pattern";
        machine.productionQueueTarget = 3;
        machine.productionQueueRemaining = 2;
        LinkedHashMap<String, Integer> inputs = new LinkedHashMap<>();
        inputs.put("Mechanical detritus", 1);

        List<String> lines = ProductionReadabilityAuthority.preview(recipe, machine, 4, 3, inputs,
                "Serviceable", 7, 2, null);
        requireContains(lines, "READY", "readiness");
        requireContains(lines, "2x Serviceable", "output count and quality");
        requireContains(lines, "carried inventory", "destination");
        requireContains(lines, "does not create a queued", "queue honesty");
        requireContains(lines, "integrity 5 -> 4", "machine wear");
        requireContains(lines, "7 turn", "adjusted turns");
        requireContains(lines, "Mechanical detritus 1/1", "item availability");
        requireContains(lines, "Outcome estimate", "value, charge, and defect forecast");
        requireContains(lines, "defect risk about", "defect estimate");
        requireContains(lines, "one inspection disposition", "batch inspection rule");
        requireContains(lines, "40% ordinary-trader resale penalty", "defect appraisal consequence");
        List<String> machineLines = ProductionReadabilityAuthority.machineContext(machine, 2, 1, 4);
        requireContains(machineLines, "Hest Var", "assigned worker");
        requireContains(machineLines, "manual Craft remains player-operated", "manual worker boundary");
        requireContains(machineLines, "remaining 2/3", "machine queue state");
        requireContains(machineLines, "pending 2, active 1, recorded 4", "shared queue state");
        requireContains(machineLines, "no separate power or fuel gate", "utility boundary");
        requireContains(machineLines, "queued-machine output routing is not controlled", "routing boundary");
        lines = new java.util.ArrayList<>(lines);
        lines.addAll(machineLines);
        for (String line : lines) if (PlayerFacingText.containsLikelyLeak(line)) {
            throw new AssertionError("Production preview leaked implementation text: " + line);
        }
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone02ProductionReadabilitySmoke() {}
}
