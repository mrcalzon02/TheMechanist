package mechanist;

/** Smoke for useful denial reasons without implementation leakage. */
final class Milestone02ActionDenialGuidanceSmoke {
    public static void main(String[] args) {
        String occupied = ActionDenialGuidanceAuthority.explain(
                ActionDenialGuidanceAuthority.DenialKind.CONSTRUCTION,
                "BuildRecipe blocked [PASSABILITY]: tile is occupied by an NPC context=build cursor 8,9");
        requireContains(occupied, "Construction unavailable", "construction denial prefix");
        requireContains(occupied, "Clear or relocate", "occupied resolution path");
        rejectContains(occupied, "BuildRecipe", "construction class leak");
        rejectContains(occupied, "context=", "construction context leak");

        String knowledge = ActionDenialGuidanceAuthority.explain(
                ActionDenialGuidanceAuthority.DenialKind.BLUEPRINT, "missing knowledge: Applied Machinery");
        requireContains(knowledge, "Learn the required knowledge", "knowledge resolution path");

        String locked = ActionDenialGuidanceAuthority.explain(
                ActionDenialGuidanceAuthority.DenialKind.ACCESS, "vault remains locked");
        requireContains(locked, "key, tool, or authorized access", "locked resolution path");
        requireContains(ActionDenialGuidanceAuthority.auditSummary(), "sanitizedReason+resolutionPath", "denial audit");
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void rejectContains(String text, String rejected, String label) {
        if (text != null && text.contains(rejected)) throw new AssertionError("Expected " + label + " to hide '" + rejected + "': " + text);
    }

    private Milestone02ActionDenialGuidanceSmoke() { }
}
