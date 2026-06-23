package mechanist;

import java.util.List;
import java.util.Set;

/** Smoke for Streetwise Appraisal improving visible trade-risk detection. */
final class Milestone03TradeSkillAppraisalSmoke {
    public static void main(String[] args) {
        ItemProvenanceRecord risky = new ItemProvenanceRecord();
        risky.defectState = "defect flagged";
        risky.batchIssueTags = "counterfeit batch, stolen-risk batch";
        risky.productionLegalStatus = "generated variant law status: black-market";

        List<String> untrained = TradeReadabilityAuthority.salePreview("Questionable tool", 10, risky);
        requireContains(untrained, "trained Streetwise Appraisal would call out the risk", "untrained hint");
        rejectContains(untrained, "trained street-market judgment is active", "untrained active claim");

        List<String> trained = TradeReadabilityAuthority.salePreview("Questionable tool", 10, risky,
                Set.of("trade-batch-appraisal", "trade-streetwise-appraisal"));
        requireContains(trained, "for 6 script", "defect appraisal price remains active");
        requireContains(trained, "Streetwise Appraisal: trained street-market judgment is active", "trained appraisal active");
        requireContains(trained, "buyer-risk tags noticed", "batch issue risk");
        requireContains(trained, "counterfeit batch, stolen risk batch", "readable risk text");
        requireContains(trained, "legal status may narrow safe buyers", "legal status risk");
        requireContains(trained, "does not override protected hand-ins, law enforcement, or defect resale math",
                "appraisal boundary");
        for (String line : trained) rejectLeaks(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectContains(List<String> lines, String rejected, String label) {
        for (String line : lines) {
            if (line != null && line.contains(rejected)) {
                throw new AssertionError("Unexpected " + label + " text '" + rejected + "': " + lines);
            }
        }
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) {
            throw new AssertionError("Trade skill appraisal leaked implementation text: " + line);
        }
    }

    private Milestone03TradeSkillAppraisalSmoke() { }
}
