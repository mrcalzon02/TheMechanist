package mechanist;

import java.util.ArrayList;

/** Names the recipe/source material behind generated staffed production output. */
final class ProductionSourceProvenanceAuthority {
    private ProductionSourceProvenanceAuthority() { }

    static String generatedSource(FactionRecipeVariant variant) {
        if (variant == null || variant.base == null) return "";
        ArrayList<String> parts = new ArrayList<>();
        add(parts, "source", variant.base.source);
        add(parts, "note", variant.base.note);
        add(parts, "variant", variant.productionNote);
        if (parts.isEmpty()) return "";
        return "generated recipe " + String.join("; ", parts);
    }

    private static void add(ArrayList<String> parts, String label, String value) {
        if (value == null || value.isBlank()) return;
        parts.add(label + " " + value.trim());
    }
}
