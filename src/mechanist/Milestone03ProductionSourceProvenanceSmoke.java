package mechanist;

import java.util.List;

/** Smoke for preserving generated recipe source in staffed-production provenance. */
final class Milestone03ProductionSourceProvenanceSmoke {
    public static void main(String[] args) {
        FactionRecipeVariant variant = firstSourceVariant();
        require(variant != null, "expected generated faction variant with source metadata");
        String label = ProductionSourceProvenanceAuthority.generatedSource(variant);
        require(label.startsWith("generated recipe "), "source label should identify generated recipe origin");
        require(label.contains(variant.base.source), "source label should include base recipe source");

        ProductionRecipe recipe = ProductionRecipe.create(variant.base.outputBaseItem, variant.faction,
                variant.qualityName, variant.requiredKnowledge, variant.machineHint);
        BaseObject machine = new BaseObject("Source Forge", 'w', 2, 3, 0, 0);
        ItemProvenanceRecord made = ItemProvenanceRecord.produced(recipe, machine, null, 7, "Hest Var");
        made.productionSource = label;
        requireContains(made.qualityContextLines(), "Production source: " + label, "production source line");

        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(made.encode());
        require(decoded != null && made.productionSource.equals(decoded.productionSource),
                "production source should survive save encoding");
        ItemProvenanceRecord transferred = ItemProvenanceRecord.transferred(decoded, made.itemName, null, 8, "moved to storage");
        require(decoded.productionSource.equals(transferred.productionSource),
                "production source should survive transfer");

        String legacy = String.join("~", ItemProvenanceRecord.enc("Legacy Tool"), ItemProvenanceRecord.enc("NONE"),
                ItemProvenanceRecord.enc("legacy maker"), ItemProvenanceRecord.enc("legacy place"),
                ItemProvenanceRecord.enc("legacy inputs"), ItemProvenanceRecord.enc("legacy route"), "3");
        require(ItemProvenanceRecord.decode(legacy) != null, "legacy provenance should remain readable");
    }

    private static FactionRecipeVariant firstSourceVariant() {
        for (FactionRecipeVariant variant : FactionRecipeVariantApi.generatedFactionVariants()) {
            if (variant != null && variant.base != null
                    && variant.base.source != null && !variant.base.source.isBlank()) return variant;
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

    private Milestone03ProductionSourceProvenanceSmoke() { }
}
