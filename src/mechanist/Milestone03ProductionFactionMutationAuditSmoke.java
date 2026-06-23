package mechanist;

import java.util.List;

/** Smoke for the Phase 18 faction production mutation audit surface. */
final class Milestone03ProductionFactionMutationAuditSmoke {
    public static void main(String[] args) {
        List<String> audit = ProductionFactionMutationAuthority.definitionAuditLines();
        requireContains(audit, "owner=ProductionFactionMutationAuthority", "mutation owner");
        requireContains(audit, "profileOwner=FactionManufacturingProfile", "profile owner");
        requireContains(audit, "provenanceOwner=ItemProvenanceRecord", "provenance owner");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "visible value uses value bias and prestige bias", "value formula");
        requireContains(audit, "visible charges use charge bias and efficiency bias", "charge formula");
        requireContains(audit, "visible defect pressure uses defect bias and reliability bias", "defect formula");
        requireContains(audit, "recipe faction selects the manufacturing profile", "profile source");
        requireContains(audit, "does not create a separate faction-stat model", "duplicate model boundary");
        requireContains(audit, "preserve the faction/profile label and visible consequence summary", "provenance preservation");
        requireContains(audit, "law enforcement, reputation changes, seizure, corruption, and faction hostility remain future owners", "future owner boundary");
        requireContains(audit, "Milestone03ProductionFactionMutationAuditSmoke", "guard reference");

        ProductionRecipe mechanicus = ProductionRecipe.create("Machine part", Faction.MECHANICUS, "Fine",
                "Scrap-Forging Doctrine", "Micro Forge");
        ProductionFactionMutationAuthority.Mutation mutation = ProductionFactionMutationAuthority.evaluate(mechanicus);
        requireContains(mutation.lines(), "Mechanicus / Rite-Forged", "profile label");
        requireContains(mutation.lines(), "value x1.89", "visible value");
        requireContains(mutation.lines(), "charges x1.62", "visible charges");
        requireContains(mutation.lines(), "defect pressure x0.46", "visible defect pressure");
        for (String line : audit) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Faction mutation audit leaked implementation text: " + line);
            }
        }
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProductionFactionMutationAuditSmoke() { }
}
