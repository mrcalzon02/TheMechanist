package mechanist;

/** Smoke for the player-facing pause-menu Unstuck action contract. */
final class Milestone02PauseMovementRecoverySmoke {
    public static void main(String[] args) {
        require("Unstuck".equals(PauseMovementRecoveryAuthority.BUTTON_LABEL), "pause recovery button label should be Unstuck");
        requireContains(PauseMovementRecoveryAuthority.BUTTON_TIP, "nearest safe standable tile", "pause recovery tooltip");
        requireContains(PauseMovementRecoveryAuthority.auditSummary(), "bridge=applyNearestStandableRecovery", "pause recovery bridge audit");
        requireContains(PauseMovementRecoveryAuthority.auditSummary(), "feedback=eventLog+targetingReport", "pause recovery feedback audit");
        requireContains(PauseMovementRecoveryAuthority.auditSummary(), "silentTeleport=false", "pause recovery visibility audit");

        MovementPlanningAuthority.MovementRecoveryApplicationResult missing =
                PauseMovementRecoveryAuthority.recoverFromPause(null);
        require(!missing.applied(), "pause recovery should fail safely without a game");
        requireContains(missing.summary(), "No world is loaded", "missing-world recovery summary");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02PauseMovementRecoverySmoke() { }
}
