package mechanist;

import java.util.List;

/** Smoke for the owned-machine field repair preview and resource contract. */
final class Milestone03MachineRepairWorkflowSmoke {
    public static void main(String[] args) {
        BaseObject machine = new BaseObject("Test Forge", 'f', 0, 0, 0, 0);
        machine.integrity = 0;
        MachineRepairAuthority.RepairPreview ready = MachineRepairAuthority.preview(machine, 1);
        require(ready.available(), "broken machine with a part should be repairable");
        require(ready.partCost() == 1, "repair should cost one machine part");
        require(ready.projectedIntegrity() == 2, "repair should restore two integrity");
        requireContains(MachineRepairAuthority.detailLines(machine, 1), "1 turn", "turn cost");

        MachineRepairAuthority.RepairPreview missing = MachineRepairAuthority.preview(machine, 0);
        require(!missing.available(), "repair should refuse missing parts");
        require(missing.summary().contains("available 0"), "missing-part refusal should state availability");

        machine.integrity = 2;
        require(MachineRepairAuthority.preview(machine, 1).projectedIntegrity() == 3,
                "repair should stop at serviceable integrity");
        machine.integrity = 5;
        require(!MachineRepairAuthority.preview(machine, 2).available(), "serviceable machine should not consume parts");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03MachineRepairWorkflowSmoke() { }
}
