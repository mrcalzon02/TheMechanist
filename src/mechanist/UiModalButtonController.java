package mechanist;

final class UiModalButtonController {
    private UiModalButtonController() {}

    static boolean buttonIsModalInteractive(GamePanel panel, ButtonBox button) {
        if (button == null) return false;
        if (panel.screen == GamePanel.Screen.OPTIONS && panel.graphicsDropdown >= 0) {
            return GraphicsDropdownOptionsRuntime.isGraphicsDropdownButton(panel, button);
        }
        if (panel.screen == GamePanel.Screen.SECTOR_AUDIT && panel.auditZoneDropdownOpen) {
            return panel.isZoneAuditDropdownButton(button) || (button.label != null && button.label.startsWith("ZONE:"));
        }
        if (panel.newGameSetupActive && panel.screen == GamePanel.Screen.CHARACTER
                && button.label != null && button.label.trim().equalsIgnoreCase("Edit Name")
                && panel.candidates.isEmpty()) {
            return false;
        }
        return true;
    }

    static int activeHoverButtonIndex(GamePanel panel) {
        if (panel.mouseX < 0 || panel.mouseY < 0) return -1;
        for (int i = panel.buttons.size() - 1; i >= 0; i--) {
            ButtonBox button = panel.buttons.get(i);
            if (!buttonIsModalInteractive(panel, button)) continue;
            if (button.contains(panel.mouseX, panel.mouseY)) return i;
        }
        return -1;
    }

    static void activateSelectedButton(GamePanel panel) {
        if (panel.buttons.isEmpty()) return;
        if (panel.selectedButton < 0) {
            panel.selectedButton = recoverBoundarySelectionIndex(panel, false);
        } else if (panel.selectedButton >= panel.buttons.size()) {
            panel.selectedButton = recoverBoundarySelectionIndex(panel, true);
        }
        ButtonBox button = panel.buttons.get(panel.selectedButton);
        if (!buttonIsModalInteractive(panel, button)) {
            panel.sounds.play("panelClose", panel.options);
            return;
        }
        panel.sounds.play("button", panel.options);
        if (MenuNavigationRuntime.interceptMainMenuButton(panel, button, "keyboard/button selection")) return;
        panel.runGuarded("BUTTON", "activate selected button " + button.label, button.action);
    }

    private static int recoverBoundarySelectionIndex(GamePanel panel, boolean fromEnd) {
        int boundaryIndex = fromEnd ? panel.buttons.size() - 1 : 0;
        int step = fromEnd ? -1 : 1;
        for (int i = boundaryIndex; i >= 0 && i < panel.buttons.size(); i += step) {
            if (buttonIsModalInteractive(panel, panel.buttons.get(i))) return i;
        }
        return boundaryIndex;
    }
}
