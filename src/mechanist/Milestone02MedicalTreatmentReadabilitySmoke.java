package mechanist;

import java.util.List;

final class Milestone02MedicalTreatmentReadabilitySmoke {
    public static void main(String[] args) {
        List<String> supplied = MedicalTreatmentReadabilityAuthority.summary(
                List.of("Stub pistol", "Bandage roll", "Medkit", "Street Stimm"), 3, 2, 4, 3);
        requireContains(supplied, "Bandage roll", "carried bandage");
        requireContains(supplied, "Medkit", "carried medkit");
        requireContains(supplied, "reduces wound burden, bleeding, infection risk, and pain", "runtime treatment effect");
        requireContains(supplied, "specialized effects are not active", "drug-system boundary");

        List<String> missing = MedicalTreatmentReadabilityAuthority.summary(List.of("Street Stimm"), 1, 1, 0, 0);
        requireContains(missing, "no usable wound-care item", "missing treatment guidance");
        if (MedicalTreatmentReadabilityAuthority.isRuntimeTreatment("Street Stimm")) {
            throw new AssertionError("Catalog-only combat drugs must not be advertised as live wound treatment.");
        }
        for (String line : supplied) rejectLeaks(line);
        for (String line : missing) rejectLeaks(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Medical guidance leaked implementation text: " + line);
    }

    private Milestone02MedicalTreatmentReadabilitySmoke() {}
}
