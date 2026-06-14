package mechanist;

import java.util.List;

final class Milestone02ContainerReadabilitySmoke {
    public static void main(String[] args) {
        ItemInstance evidence = new ItemInstance("smoke", "Stolen faction leader journal", "locker", "unit", null);
        List<String> full = ContainerReadabilityAuthority.transferPreview(
                "Evidence Locker", 3, evidence, "Frag grenade", 20, 20);
        requireContains(full, "Storage: Evidence Locker", "storage identity");
        requireContains(full, "no separate lock", "access honesty");
        requireContains(full, "no enforced capacity", "capacity honesty");
        requireContains(full, "carrying load is full", "take denial");
        requireContains(full, "evidence", "mission item warning");
        requireContains(full, "volatile or hazardous", "deposit warning");

        List<String> open = ContainerReadabilityAuthority.transferPreview(
                "Base Storage", 0, null, null, 2, 20);
        requireContains(open, "no container item selected", "empty take preview");
        requireContains(open, "no carried item selected", "empty put preview");

        for (String line : full) rejectLeaks(line);
        for (String line : open) rejectLeaks(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Container preview leaked implementation text: " + line);
    }

    private Milestone02ContainerReadabilitySmoke() {}
}
