package mechanist;

import java.util.List;

/** Smoke for preserving repaired-machine history in produced-item provenance. */
final class Milestone03ProductionRepairHistoryProvenanceSmoke {
    public static void main(String[] args) {
        BaseObject machine = new BaseObject("Patched Forge", 'f', 2, 4, 0, 0);
        machine.integrity = 1;
        MachineRepairAuthority.RepairPreview preview = MachineRepairAuthority.preview(machine, 1);
        require(preview.available(), "worn machine should have a repair preview");
        machine.machineRepairHistory = MachineRepairHistoryAuthority.recordLine(machine, preview, 21, "Hest Var");
        require(machine.machineRepairHistory.contains("field repair turn 21"), "repair history should include turn");
        require(machine.saveLine().split("\\|", 25).length >= 25, "base object save line should include repair history");

        ProductionRecipe recipe = ProductionRecipe.create("Machine part", Faction.HIVER, "Common",
                "Common Tool Patterns", "Patched Forge");
        ItemProvenanceRecord made = ItemProvenanceRecord.produced(recipe, machine, null, 22, "Hest Var");
        requireContains(made.qualityContextLines(), "Repair/modification history: field repair turn 21",
                "repair history provenance");

        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(made.encode());
        require(decoded != null && made.repairModificationHistory.equals(decoded.repairModificationHistory),
                "repair history should survive save encoding");
        ItemProvenanceRecord transferred = ItemProvenanceRecord.transferred(decoded, made.itemName, null, 23, "moved to storage");
        require(decoded.repairModificationHistory.equals(transferred.repairModificationHistory),
                "repair history should survive transfer");

        String legacy = String.join("~", ItemProvenanceRecord.enc("Legacy Tool"), ItemProvenanceRecord.enc("NONE"),
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

    private Milestone03ProductionRepairHistoryProvenanceSmoke() { }
}
