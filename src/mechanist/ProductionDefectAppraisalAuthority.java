package mechanist;

import java.util.List;

/** Applies the first gameplay consequence for production batch inspection results. */
final class ProductionDefectAppraisalAuthority {
    static final int FLAGGED_RESALE_PERCENT = 60;

    record Appraisal(boolean defectFlagged, int ordinaryPrice, int adjustedPrice, List<String> lines) { }

    private ProductionDefectAppraisalAuthority() { }

    static Appraisal appraise(int ordinaryPrice, ItemProvenanceRecord provenance) {
        int base = Math.max(0, ordinaryPrice);
        boolean flagged = provenance != null && "defect flagged".equalsIgnoreCase(provenance.defectState);
        int adjusted = flagged ? Math.max(1, (int)Math.floor(base * FLAGGED_RESALE_PERCENT / 100.0)) : base;
        if (!flagged) {
            return new Appraisal(false, base, adjusted,
                    List.of("Batch appraisal: no recorded defect penalty applies."));
        }
        return new Appraisal(true, base, adjusted, List.of(
                "Batch appraisal: defect flagged; resale is " + adjusted + " script instead of " + base + ".",
                "Defect consequence: traders apply a 40% value penalty. Item statistics remain unchanged until a per-item condition owner exists."));
    }

    static String inventoryLine(ItemProvenanceRecord provenance) {
        return provenance != null && "defect flagged".equalsIgnoreCase(provenance.defectState)
                ? "Defect consequence: ordinary traders reduce resale value by 40%; no hidden combat or use penalty is applied."
                : "";
    }
}
