package mechanist;

final class MenuNavigationRuntime {
    private MenuNavigationRuntime() {}

    static void returnToMainMenu(GamePanel panel, String reason) {
        if (panel == null) return;
        panel.screen = GamePanel.Screen.MENU;
        panel.panelMode = GamePanel.PanelMode.NONE;
        panel.selectedButton = 0;
        panel.lookCursorActive = false;
        panel.interactCursorActive = false;
        panel.combatCursorActive = false;
        panel.manualMovementPlanActive = false;
        panel.mouseMovePreviewActive = false;
        panel.mouseMovePreviewValid = false;
        panel.buildPlacementActive = false;
        panel.characterNameEditActive = false;
        panel.activeScrollTag = "";
        panel.logEvent("Returned to main menu" + (reason == null || reason.isBlank() ? "." : ": " + reason + "."));
        panel.repaint();
        panel.requestFocusInWindow();
    }

    static boolean interceptMainMenuButton(GamePanel panel, ButtonBox button, String reason) {
        if (panel == null || button == null || button.label == null) return false;
        String label = button.label.trim().toLowerCase(java.util.Locale.ROOT);
        if (!label.contains("main menu")) return false;
        if (panel.screen != GamePanel.Screen.PAUSE && panel.screen != GamePanel.Screen.PANEL && panel.screen != GamePanel.Screen.INFO) return false;
        returnToMainMenu(panel, reason);
        return true;
    }
}
