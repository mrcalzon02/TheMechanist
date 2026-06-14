package mechanist;

import java.util.List;

/** Smoke for visible and execution-shared defective-batch resale appraisal. */
final class Milestone03ProductionDefectAppraisalSmoke {
    public static void main(String[] args) {
        ItemProvenanceRecord flagged = new ItemProvenanceRecord();
        flagged.defectState = "defect flagged";
        flagged.batchId = "BATCH-SMOKE";
        ProductionDefectAppraisalAuthority.Appraisal appraisal = ProductionDefectAppraisalAuthority.appraise(10, flagged);
        require(appraisal.defectFlagged(), "flagged provenance should activate appraisal penalty");
        require(appraisal.adjustedPrice() == 6, "ten-script item should appraise at six script when flagged");
        requireContains(appraisal.lines(), "40% value penalty", "appraisal explanation");

        ItemProvenanceRecord passed = new ItemProvenanceRecord();
        passed.defectState = "passed inspection";
        require(ProductionDefectAppraisalAuthority.appraise(10, passed).adjustedPrice() == 10,
                "passed batch should retain ordinary resale price");
        require(ProductionDefectAppraisalAuthority.appraise(1, flagged).adjustedPrice() == 1,
                "positive sale values should not be reduced below one script");

        List<String> preview = TradeReadabilityAuthority.salePreview("Fine tool", 10, flagged);
        requireContains(preview, "for 6 script", "sale preview adjusted price");
        requireContains(preview, "defect flagged", "sale preview defect reason");
        requireContains(flagged.qualityContextLines(), "reduce resale value by 40%", "inventory consequence");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProductionDefectAppraisalSmoke() { }
}
