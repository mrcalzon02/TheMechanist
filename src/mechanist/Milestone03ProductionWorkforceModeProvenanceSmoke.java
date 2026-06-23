package mechanist;

import java.util.List;

/** Smoke for preserving manual-vs-staffed production mode in item provenance. */
final class Milestone03ProductionWorkforceModeProvenanceSmoke {
    public static void main(String[] args) {
        ProductionRecipe recipe = ProductionRecipe.create("Test Tool", Faction.HIVER, "Common",
                "Common Tool Patterns", "Test Forge");
        BaseObject machine = new BaseObject("Test Forge", 'f', 1, 2, 0, 0);

        ItemProvenanceRecord manual = ItemProvenanceRecord.produced(recipe, machine, null, 5, "Test Operator");
        requireContains(manual.qualityContextLines(),
                "Production workforce: immediate manual Craft / operator Test Operator", "manual workforce mode");

        manual.productionMode = ProductionWorkforceModeAuthority.staffedLabel("Hest Var");
        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(manual.encode());
        require(decoded != null && manual.productionMode.equals(decoded.productionMode),
                "production mode should survive save encoding");
        ItemProvenanceRecord transferred = ItemProvenanceRecord.transferred(decoded, manual.itemName, null, 6, "moved to storage");
        require(decoded.productionMode.equals(transferred.productionMode), "production mode should survive transfer");

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

    private Milestone03ProductionWorkforceModeProvenanceSmoke() { }
}
