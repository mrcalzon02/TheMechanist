package mechanist;

import java.util.List;

final class Milestone02InventoryReadabilitySmoke {
    public static void main(String[] args) {
        List<String> carried = InventoryReadabilityAuthority.detailLines(
                "Fine Autopistol", false, false, true, 6, 20, null);
        requireContains(carried, "Quality: Fine", "quality");
        requireContains(carried, "regulated equipment", "legality");
        requireContains(carried, "right hand", "equipped hand");
        requireContains(carried, "base storage", "store consequence");
        requireContains(carried, "no trace record", "missing provenance");

        List<String> stored = InventoryReadabilityAuthority.detailLines(
                "Witchsalt", true, false, false, 4, 20, null);
        requireContains(stored, "forbidden", "forbidden status");
        requireContains(stored, "carried inventory", "take consequence");
        requireContains(stored, "no separate damage", "condition honesty");

        List<String> stack = InventoryReadabilityAuthority.stackLines("Fine Autopistol",
                List.of("Fine Autopistol", "Common Autopistol", "Fine Autopistol", "Medkit"));
        requireContains(stack, "2 exact selection", "exact selected quality count");
        requireContains(stack, "3 in the same item family", "mixed-quality family count");
        requireContains(stack, "2 quality grades", "quality grade count");
        requireContains(stack, "one selected unit at a time", "single-unit action scope");

        for (String line : carried) rejectLeaks(line);
        for (String line : stored) rejectLeaks(line);
        for (String line : stack) rejectLeaks(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Inventory detail leaked implementation text: " + line);
    }

    private Milestone02InventoryReadabilitySmoke() {}
}
