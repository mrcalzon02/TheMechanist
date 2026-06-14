package mechanist;

/** Clears transient movement previews when world or menu focus changes. */
final class MovementPlanningFocusResetAuthority {
    static boolean reset(GamePanel panel, String reason) {
        if (panel == null) return false;
        boolean changed = panel.manualMovementPlanActive || panel.mouseMovePreviewActive
                || panel.manualMovementPlanHazardous || panel.mouseMovePreviewHazardous
                || !panel.manualMovementPlanPath.isEmpty() || !panel.mouseMovePreviewPath.isEmpty();
        panel.manualMovementPlanActive = false;
        panel.manualMovementPlanHazardous = false;
        panel.mouseMovePreviewActive = false;
        panel.mouseMovePreviewValid = false;
        panel.mouseMovePreviewHazardous = false;
        panel.manualMovementPlanPath.clear();
        panel.mouseMovePreviewPath.clear();
        panel.lookCursorActive = false;
        panel.mouseMovePreviewTargetX = panel.playerX;
        panel.mouseMovePreviewTargetY = panel.playerY;
        if (changed) {
            panel.lastTargetingReport = "Movement planning cleared for "
                    + (reason == null || reason.isBlank() ? "focus change" : PlayerFacingText.sanitize(reason)) + ".";
        }
        return changed;
    }

    static String auditSummary() {
        return "Movement focus reset covers manual ghost, mouse preview, hazard flags, route lists, cursor focus, and preview target; used by save/load and main-menu transitions.";
    }

    private MovementPlanningFocusResetAuthority() { }
}
