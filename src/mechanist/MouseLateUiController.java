package mechanist;

final class MouseLateUiController {
    private MouseLateUiController() {}

    static boolean handleLateUiClick(GamePanel panel, int mx, int my) {
        if (panel.screen == GamePanel.Screen.PANEL && panel.panelMode == GamePanel.PanelMode.INVENTORY && panel.handleInventoryStackPanelClick(mx, my)) {
            panel.requestFocusInWindow();
            return true;
        }
        if (panel.findScrollRegion(mx, my) != null) {
            panel.handleScrollbarClick(mx, my);
            panel.repaint();
            panel.requestFocusInWindow();
            return true;
        }
        if (panel.screen == GamePanel.Screen.KNOWLEDGE && panel.handleKnowledgeTreeClick(mx, my)) {
            panel.requestFocusInWindow();
            return true;
        }
        if (OptionsDropdownMouseController.handleOptionsDropdownClick(panel, mx, my)) return true;
        if (handleCharacterNameClick(panel, mx, my)) return true;
        for (int i = panel.buttons.size() - 1; i >= 0; i--) {
            ButtonBox button = panel.buttons.get(i);
            if (UiModalButtonController.buttonIsModalInteractive(panel, button) && button.contains(mx, my)) {
                panel.selectedButton = i;
                panel.sounds.play("button", panel.options);
                panel.runGuarded("MOUSE", "click button " + button.label, button.action);
                panel.repaint();
                return true;
            }
        }
        panel.requestFocusInWindow();
        return true;
    }

    static boolean handleCharacterNameClick(GamePanel panel, int mx, int my) {
        if (panel.screen != GamePanel.Screen.CHARACTER) return false;
        if (panel.characterNameEditRect.contains(mx, my)) {
            panel.characterNameEditActive = true;
            panel.requestFocusInWindow();
            panel.repaint();
            return true;
        }
        if (panel.characterNameEditActive) {
            Candidate candidate = panel.candidates.isEmpty() ? null : panel.candidates.get(panel.candidateIndex);
            if (candidate != null) {
                candidate.name = CharacterCreationAuthority.sanitizePlayerName(candidate.name, panel.rng);
                panel.refreshNameLockedCandidateState(candidate);
            }
            panel.characterNameEditActive = false;
        }
        return false;
    }
}
