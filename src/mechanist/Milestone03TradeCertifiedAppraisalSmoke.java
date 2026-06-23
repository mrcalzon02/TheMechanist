package mechanist;

import java.util.List;
import java.util.Set;

/** Smoke for Certified Market Appraisal recognizing formal trade proof. */
final class Milestone03TradeCertifiedAppraisalSmoke {
    public static void main(String[] args) {
        ItemProvenanceRecord certified = new ItemProvenanceRecord();
        certified.batchIssueTags = "good batch, faction-certified batch";
        certified.productionLegalStatus = "generated variant law status: lawful issue";

        List<String> untrained = TradeReadabilityAuthority.salePreview("Stamped machine part", 18, certified);
        requireContains(untrained, "trained Certified Market Appraisal would separate formal proof",
                "untrained certificate hint");
        rejectContains(untrained, "trained certificate review is active", "untrained active claim");

        List<String> trained = TradeReadabilityAuthority.salePreview("Stamped machine part", 18, certified,
                Set.of("trade-batch-appraisal", "trade-guilder-certification"));
        requireContains(trained, "for 18 script", "ordinary price remains unchanged");
        requireContains(trained, "Certified Market Appraisal: trained certificate review is active",
                "trained certificate active");
        requireContains(trained, "faction-certified batch proof is recognized as formal trade evidence",
                "formal proof recognition");
        requireContains(trained, "legal status record available", "legal status review");
        requireContains(trained, "does not bypass faction access, protected hand-ins, or buyer policy",
                "certified appraisal boundary");
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
            throw new AssertionError("Certified appraisal leaked implementation text: " + line);
        }
    }

    private Milestone03TradeCertifiedAppraisalSmoke() { }
}
