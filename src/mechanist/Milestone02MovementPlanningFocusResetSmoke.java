package mechanist;

final class Milestone02MovementPlanningFocusResetSmoke {
    public static void main(String[] args) {
        if (MovementPlanningFocusResetAuthority.reset(null, "smoke")) {
            throw new AssertionError("Null focus reset should fail safely without reporting a change.");
        }
        String audit = MovementPlanningFocusResetAuthority.auditSummary();
        requireContains(audit, "manual ghost", "manual planning reset");
        requireContains(audit, "mouse preview", "mouse planning reset");
        requireContains(audit, "hazard flags", "hazard reset");
        requireContains(audit, "save/load and main-menu transitions", "transition coverage");
        if (PlayerFacingText.containsLikelyLeak(audit)) throw new AssertionError("Movement focus reset audit leaked implementation text: " + audit);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone02MovementPlanningFocusResetSmoke() { }
}
