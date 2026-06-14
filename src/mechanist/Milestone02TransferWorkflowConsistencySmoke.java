package mechanist;

import java.util.List;

final class Milestone02TransferWorkflowConsistencySmoke {
    public static void main(String[] args) {
        List<String> reversible = TransferWorkflowReadabilityAuthority.describe("carried inventory", "base storage",
                "Faction leader journal", 1, "owned base storage access", true, true, true, null);
        requireContains(reversible, "carried inventory -> base storage", "source and destination");
        requireContains(reversible, "quantity 1", "quantity");
        requireContains(reversible, "Protection warning", "protected item warning");
        requireContains(reversible, "can be moved back", "reversibility");
        requireContains(reversible, "Cancel or Back", "confirmation grammar");

        List<String> blocked = TransferWorkflowReadabilityAuthority.describe("vendor stock", "carried inventory",
                "Autopistol", 1, "vendor purchase access", false, false, false, "carrying load is full");
        requireContains(blocked, "transfer blocked", "capacity failure");
        requireContains(blocked, "not automatically reversible", "trade finality");
        for (String line : reversible) rejectLeaks(line);
        for (String line : blocked) rejectLeaks(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Transfer guidance leaked implementation text: " + line);
    }

    private Milestone02TransferWorkflowConsistencySmoke() {}
}
