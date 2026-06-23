package mechanist;

import java.util.List;

/** Smoke for visible and provenance-backed faction production mutations. */
final class Milestone03ProductionFactionMutationSmoke {
    public static void main(String[] args) {
        ProductionRecipe mechanicus = ProductionRecipe.create("Machine part", Faction.MECHANICUS, "Fine",
                "Scrap-Forging Doctrine", "Micro Forge");
        ProductionFactionMutationAuthority.Mutation riteForged = ProductionFactionMutationAuthority.evaluate(mechanicus);
        require("Mechanicus / Rite-Forged".equals(riteForged.label()), "Mechanicus production should expose its faction mutation");
        requireContains(riteForged.lines(), "value x1.89", "Mechanicus effective value consequence");
        requireContains(riteForged.lines(), "charges x1.62", "Mechanicus effective charge consequence");
        requireContains(riteForged.lines(), "defect pressure x0.46", "Mechanicus effective defect consequence");

        ProductionRecipe scavenger = ProductionRecipe.create("Machine part", Faction.SCAVENGER, "Fine",
                "Junk Fabrication Patterns", "Scrap Workbench");
        ProductionFactionMutationAuthority.Mutation improvised = ProductionFactionMutationAuthority.evaluate(scavenger);
        requireContains(improvised.lines(), "Scavver / Improvised", "Scavver mutation label");
        requireContains(improvised.lines(), "defect pressure x2.67", "Scavver defect tradeoff");

        ItemProvenanceRecord made = ItemProvenanceRecord.produced(mechanicus, null, null, 9, "Test Operator");
        requireContains(made.qualityContextLines(), "Faction production mutation: Mechanicus / Rite-Forged", "inventory mutation context");
        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(made.encode());
        require(decoded != null && made.factionMutation.equals(decoded.factionMutation),
                "faction mutation should survive save encoding");
        ItemProvenanceRecord transferred = ItemProvenanceRecord.transferred(decoded, made.itemName, null, 10, "moved to storage");
        require(decoded.factionMutation.equals(transferred.factionMutation), "faction mutation should survive transfer");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProductionFactionMutationSmoke() { }
}
