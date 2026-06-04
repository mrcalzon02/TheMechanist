package mechanist;

final class PanelTargetingController {
    private PanelTargetingController() {}

    static void targetPanelCursorFromMouse(GamePanel panel, int tx, int ty) {
        if (panel.world == null || !panel.world.inBounds(tx, ty)) return;
        if (panel.panelMode == GamePanel.PanelMode.LOOK) {
            panel.lookCursorActive = true;
            panel.lookX = tx;
            panel.lookY = ty;
            panel.lookStackIndex = 0;
            panel.lookStackScroll = 0;
            panel.activeScrollTag = "look-stack";
            panel.setFacingToward(panel.lookX, panel.lookY, "mouse look cursor");
            ProgressiveLookAuthority.reset(panel, "mouse look cursor moved");
        } else if (panel.panelMode == GamePanel.PanelMode.COMBAT) {
            panel.combatCursorActive = true;
            panel.combatX = tx;
            panel.combatY = ty;
            panel.setFacingToward(panel.combatX, panel.combatY, "mouse combat cursor");
            panel.lastTargetingReport = panel.targetingSolutionAt(tx, ty).summary;
        } else if (panel.panelMode == GamePanel.PanelMode.INTERACT) {
            panel.interactCursorActive = true;
            panel.lookX = tx;
            panel.lookY = ty;
            panel.clampInteractCursorToAdjacent();
            panel.lookStackIndex = 0;
            panel.setFacingToward(panel.lookX, panel.lookY, "mouse interact cursor");
            ProgressiveLookAuthority.reset(panel, "mouse interact cursor moved");
            panel.updatePendingInteractionSummary();
        }
        DebugLog.audit("MOUSE_TARGETING", "panel=" + panel.panelMode + " tile=" + tx + "," + ty + " source=virtual-map");
    }
}
