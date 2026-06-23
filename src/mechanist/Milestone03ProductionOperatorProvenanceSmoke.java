package mechanist;

import java.util.List;

/** Smoke for explicit immediate-manual operator identity in item provenance. */
final class Milestone03ProductionOperatorProvenanceSmoke {
    public static void main(String[] args) {
        require("Test Operator".equals(ProductionOperatorIdentityAuthority.provenanceLabel(" Test Operator ")),
                "operator identity should be normalized");
        require("unknown manual operator".equals(ProductionOperatorIdentityAuthority.provenanceLabel("")),
                "blank operator identity should remain honest");

        ProductionRecipe recipe = ProductionRecipe.create("Test Tool", Faction.HIVER, "Common",
                "Common Tool Patterns", "Test Forge");
        BaseObject machine = new BaseObject("Test Forge", 'f', 12, 9, 0, 0);
        ItemProvenanceRecord made = ItemProvenanceRecord.produced(
                recipe, machine, null, 8, "Test Operator");
        requireContains(made.qualityContextLines(), "Producing operator: Test Operator", "operator provenance");

        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(made.encode());
        require(decoded != null && made.producingOperator.equals(decoded.producingOperator),
                "producing operator should survive save encoding");
        ItemProvenanceRecord transferred = ItemProvenanceRecord.transferred(
                decoded, made.itemName, null, 9, "moved to storage");
        require(decoded.producingOperator.equals(transferred.producingOperator),
                "producing operator should survive transfer");

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

    private Milestone03ProductionOperatorProvenanceSmoke() { }
}
