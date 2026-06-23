package mechanist;

import java.util.List;
import java.util.Locale;

/** Explains the existing faction manufacturing modifiers applied to produced items. */
final class ProductionFactionMutationAuthority {
    record Mutation(String label, String provenanceLabel, List<String> lines) { }

    private ProductionFactionMutationAuthority() { }

    static Mutation evaluate(ProductionRecipe recipe) {
        if (recipe == null) {
            return new Mutation("unavailable", "", List.of("Faction production mutation: unavailable until an output recipe is selected."));
        }
        FactionManufacturingProfile profile = recipe.profile;
        double value = profile.valueBias * Math.max(0.50, profile.prestigeBias);
        double charges = profile.chargeBias * profile.efficiencyBias;
        double defectPressure = profile.defectBias / Math.max(0.25, profile.reliabilityBias);
        String label = profile.label + " / " + profile.recipePrefix;
        String consequences = "value x" + fmt(value) + ", charges x" + fmt(charges)
                + ", defect pressure x" + fmt(defectPressure);
        return new Mutation(label, label + " / " + consequences, List.of(
                "Faction production mutation: " + label + ".",
                "Visible faction consequences: output prefix " + profile.recipePrefix + "; " + consequences + ".",
                "Faction doctrine character: strengths " + profile.strengths + "; tradeoffs " + profile.weaknesses + "."));
    }

    static List<String> definitionAuditLines() {
        return List.of(
                "Faction production mutation audit: owner=ProductionFactionMutationAuthority, profileOwner=FactionManufacturingProfile, provenanceOwner=ItemProvenanceRecord, ordinaryUiRawIds=false.",
                "Faction mutation formula audit: visible value uses value bias and prestige bias; visible charges use charge bias and efficiency bias; visible defect pressure uses defect bias and reliability bias.",
                "Faction mutation source audit: recipe faction selects the manufacturing profile, output prefix, strengths, and tradeoffs; the audit does not create a separate faction-stat model.",
                "Faction mutation provenance audit: produced items preserve the faction/profile label and visible consequence summary through save/load and transfers.",
                "Faction mutation boundary audit: mutation affects existing output prefix, value, charges, and defect pressure only; law enforcement, reputation changes, seizure, corruption, and faction hostility remain future owners.",
                "Guard: Milestone03ProductionFactionMutationAuditSmoke checks formula ownership, profile source, provenance preservation, effect boundaries, and raw-ID hiding."
        );
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
