package mechanist;

import java.util.List;

/** Smoke for exact producing-machine identity in forecast and item provenance. */
final class Milestone03ProductionMachineProvenanceSmoke {
    public static void main(String[] args) {
        BaseObject machine = new BaseObject("Test Forge", 'f', 12, 9, 0, 0);
        ProductionMachineIdentityAuthority.MachineIdentity identity =
                ProductionMachineIdentityAuthority.evaluate(machine);
        requireContains(identity.lines(), "Test Forge / production station f at 12,9", "forecast machine identity");

        ProductionRecipe recipe = ProductionRecipe.create("Test Tool", Faction.HIVER, "Common",
                "Common Tool Patterns", "Test Forge");
        ItemProvenanceRecord made = ItemProvenanceRecord.produced(recipe, machine, null, 8, "Test Operator");
        requireContains(made.qualityContextLines(),
                "Producing machine: Test Forge / production station f at 12,9", "machine provenance");

        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(made.encode());
        require(decoded != null && made.producingMachine.equals(decoded.producingMachine),
                "producing machine should survive save encoding");
        ItemProvenanceRecord transferred = ItemProvenanceRecord.transferred(
                decoded, made.itemName, null, 9, "moved to storage");
        require(decoded.producingMachine.equals(transferred.producingMachine),
                "producing machine should survive transfer");

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

    private Milestone03ProductionMachineProvenanceSmoke() { }
}
