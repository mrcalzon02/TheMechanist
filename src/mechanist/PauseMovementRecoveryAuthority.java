package mechanist;

/** Player-facing pause-menu bridge for explicit movement recovery. */
final class PauseMovementRecoveryAuthority {
    static final String VERSION = "0.9.10ki";
    static final String BUTTON_LABEL = "Unstuck";
    static final String BUTTON_TIP = "Move to the nearest safe standable tile and record the result in the event log.";
    private static final int START_RADIUS = 1;
    private static final int MAX_RADIUS = 12;

    private PauseMovementRecoveryAuthority() { }

    static MovementPlanningAuthority.MovementRecoveryApplicationResult recoverFromPause(GamePanel game) {
        if (game == null) return MovementPlanningAuthority.applyNearestStandableRecovery(null, START_RADIUS, MAX_RADIUS);
        game.logEvent("Unstuck requested from the pause menu.");
        MovementPlanningAuthority.MovementRecoveryApplicationResult result =
                MovementPlanningAuthority.applyNearestStandableRecovery(game, START_RADIUS, MAX_RADIUS);
        MovementDebugOverlayAuthority.recordRecovery(game, result);
        game.screen = GamePanel.Screen.PAUSE;
        game.repaint();
        return result;
    }

    static String auditSummary() {
        return "pauseMovementRecoveryAuthority version=" + VERSION
                + " action=Unstuck bridge=applyNearestStandableRecovery"
                + " feedback=eventLog+targetingReport pauseState=preserved silentTeleport=false";
    }
}
