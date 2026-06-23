package mechanist;

import java.util.List;

/** Smoke for preserving generated-production legal status in item provenance. */
final class Milestone03ProductionLegalStatusProvenanceSmoke {
    public static void main(String[] args) {
        FactionRecipeVariant variant = firstStatusVariant();
        require(variant != null, "expected generated faction variant with law status");
        String label = ProductionLegalStatusAuthority.provenanceLabel(variant);
        require(label.contains(variant.lawStatus), "legal label should include variant law status");

        ProductionRecipe recipe = ProductionRecipe.create(variant.base.outputBaseItem, variant.faction,
                variant.qualityName, variant.requiredKnowledge, variant.machineHint);
        BaseObject machine = new BaseObject("Status Forge", 'w', 2, 3, 0, 0);
        ItemProvenanceRecord made = ItemProvenanceRecord.produced(recipe, machine, null, 7, "Hest Var");
        made.productionLegalStatus = label;
        requireContains(made.qualityContextLines(), "Production legal status: " + label, "legal status line");

        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(made.encode());
        require(decoded != null && made.productionLegalStatus.equals(decoded.productionLegalStatus),
                "legal status should survive save encoding");
        ItemProvenanceRecord transferred = ItemProvenanceRecord.transferred(decoded, made.itemName, null, 8, "moved to storage");
        require(decoded.productionLegalStatus.equals(transferred.productionLegalStatus),
                "legal status should survive transfer");

        String legacy = String.join("~", ItemProvenanceRecord.enc("Legacy Tool"), ItemProvenanceRecord.enc("NONE"),
                ItemProvenanceRecord.enc("legacy maker"), ItemProvenanceRecord.enc("legacy place"),
                ItemProvenanceRecord.enc("legacy inputs"), ItemProvenanceRecord.enc("legacy route"), "3");
        require(ItemProvenanceRecord.decode(legacy) != null, "legacy provenance should remain readable");
    }

    private static FactionRecipeVariant firstStatusVariant() {
        for (FactionRecipeVariant variant : FactionRecipeVariantApi.generatedFactionVariants()) {
            if (variant != null && variant.lawStatus != null && !variant.lawStatus.isBlank()) return variant;
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProductionLegalStatusProvenanceSmoke() { }
}
