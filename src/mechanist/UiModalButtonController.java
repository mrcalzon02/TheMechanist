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
                && button.label != null && panel.candidates.isEmpty()) {
            String normalizedLabel = button.label.trim();
            if (normalizedLabel.equalsIgnoreCase("Edit Name") || normalizedLabel.equalsIgnoreCase("Reroll")) {
                return false;
            }
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
        } else if (!buttonIsModalInteractive(panel, panel.buttons.get(panel.selectedButton))) {
            panel.selectedButton = recoverNearestInteractiveSelectionIndex(panel, panel.selectedButton);
        }
        ButtonBox button = panel.buttons.get(panel.selectedButton);
        if (!buttonIsModalInteractive(panel, button)) {
            panel.sounds.play("panelClose", panel.options);
            return;
        }
        reconcileCharacterSetupSelection(panel, button);
        panel.sounds.play("button", panel.options);
        if (MenuNavigationRuntime.interceptMainMenuButton(panel, button, "keyboard/button selection")) return;
        panel.runGuarded("BUTTON", "activate selected button " + button.label, button.action);
    }

    static void reconcileCharacterSetupSelection(GamePanel panel, ButtonBox button) {
        if (panel == null || button == null || button.label == null) return;
        if (!panel.newGameSetupActive || panel.screen != GamePanel.Screen.CHARACTER) return;
        if (!button.label.trim().equalsIgnoreCase("Reroll") || panel.candidates.isEmpty()) return;
        panel.candidateIndex = Math.max(0, Math.min(panel.candidateIndex, panel.candidates.size() - 1));
    }

    private static int recoverNearestInteractiveSelectionIndex(GamePanel panel, int selectedIndex) {
        for (int distance = 1; distance < panel.buttons.size(); distance++) {
            int next = selectedIndex + distance;
            if (next < panel.buttons.size() && buttonIsModalInteractive(panel, panel.buttons.get(next))) return next;
            int previous = selectedIndex - distance;
            if (previous >= 0 && buttonIsModalInteractive(panel, panel.buttons.get(previous))) return previous;
        }
        return selectedIndex;
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
