package mechanist;

/** Preserves generated-production legal status without inventing law enforcement effects. */
final class ProductionLegalStatusAuthority {
    private ProductionLegalStatusAuthority() { }

    static String provenanceLabel(FactionRecipeVariant variant) {
        if (variant == null || variant.lawStatus == null || variant.lawStatus.isBlank()) return "";
        return "generated variant law status: " + variant.lawStatus.trim();
    }
}
