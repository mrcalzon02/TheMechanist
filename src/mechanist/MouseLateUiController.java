package mechanist;

final class MouseLateUiController {
    private MouseLateUiController() {}

    static boolean handleLateUiClick(GamePanel panel, int mx, int my) {
        if (panel.screen == GamePanel.Screen.PANEL && panel.panelMode == GamePanel.PanelMode.INVENTORY && panel.handleInventoryStackPanelClick(mx, my)) {
            panel.requestFocusInWindow();
            return true;
        }
        if (handleCharacterEquipmentPaperDollClick(panel, mx, my)) return true;
        if (handleCharacterMedicalPaperDollClick(panel, mx, my)) return true;
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
                if (MenuNavigationRuntime.interceptMainMenuButton(panel, button, "mouse click")) return true;
                panel.runGuarded("MOUSE", "click button " + button.label, button.action);
                panel.repaint();
                return true;
            }
        }
        panel.requestFocusInWindow();
        return false;
    }

    static boolean handleCharacterEquipmentPaperDollClick(GamePanel panel, int mx, int my) {
        if (panel == null || panel.active == null || panel.newGameSetupActive) return false;
        boolean characterSurface = panel.screen == GamePanel.Screen.CHARACTER
                || panel.panelMode == GamePanel.PanelMode.CHARACTER;
        if (!characterSurface
                || CharacterEquipmentAndMedicalAuthority.CharacterTab.at(panel.characterTab)
                != CharacterEquipmentAndMedicalAuthority.CharacterTab.EQUIPMENT) return false;

        java.awt.Rectangle doll = characterEquipmentPaperDollBounds(panel.getWidth(), panel.getHeight());
        String bodyPart = CharacterEquipmentAndMedicalAuthority.bodyPartAt(panel.active, doll, mx, my);
        CharacterEquipmentAndMedicalAuthority.EquipmentSlot slot = equipmentSlotForBodyPart(bodyPart);
        if (slot == null) return false;

        panel.selectedCharacterEquipmentSlot = slot.ordinal();
        panel.sounds.play("button", panel.options);
        panel.requestFocusInWindow();
        panel.repaint();
        return true;
    }

    static CharacterEquipmentAndMedicalAuthority.EquipmentSlot equipmentSlotForBodyPart(String bodyPart) {
        return CharacterEquipmentAndMedicalAuthority.equipmentSlotForBodyPart(bodyPart);
    }

    static boolean handleCharacterMedicalPaperDollClick(GamePanel panel, int mx, int my) {
        if (panel == null || panel.active == null || panel.newGameSetupActive) return false;
        boolean characterSurface = panel.screen == GamePanel.Screen.CHARACTER
                || panel.panelMode == GamePanel.PanelMode.CHARACTER;
        if (!characterSurface
                || CharacterEquipmentAndMedicalAuthority.CharacterTab.at(panel.characterTab)
                != CharacterEquipmentAndMedicalAuthority.CharacterTab.MEDICAL) return false;

        java.awt.Rectangle doll = characterMedicalPaperDollBounds(panel.getWidth(), panel.getHeight());
        String bodyPart = CharacterEquipmentAndMedicalAuthority.bodyPartAt(panel.active, doll, mx, my);
        if (bodyPart == null || bodyPart.isBlank()) return false;

        panel.selectedCharacterMedicalBodyPart = bodyPart;
        panel.sounds.play("button", panel.options);
        panel.requestFocusInWindow();
        panel.repaint();
        return true;
    }

    static java.awt.Rectangle characterEquipmentPaperDollBounds(int width, int height) {
        java.awt.Rectangle content = characterTabContentBounds(width, height);
        int dollWidth = Math.max(230, Math.min(310, content.width * 27 / 100));
        return new java.awt.Rectangle(content.x, content.y, dollWidth, content.height);
    }

    static java.awt.Rectangle characterMedicalPaperDollBounds(int width, int height) {
        java.awt.Rectangle content = characterTabContentBounds(width, height);
        int dollWidth = Math.max(250, Math.min(330, content.width * 34 / 100));
        return new java.awt.Rectangle(content.x, content.y, dollWidth, content.height);
    }

    private static java.awt.Rectangle characterTabContentBounds(int width, int height) {
        int panelWidth = Math.max(820, Math.min(width - 48, (int)Math.round(width * 0.90)));
        int panelHeight = Math.max(560, Math.min(height - 92, (int)Math.round(height * 0.84)));
        int panelX = Math.max(18, (width - panelWidth) / 2);
        int panelY = Math.max(46, (height - panelHeight) / 2);

        int bodyX = panelX + 18;
        int bodyY = panelY + 54;
        int bodyWidth = panelWidth - 36;
        int bodyHeight = panelHeight - 112;

        int contentY = bodyY + 40;
        int contentHeight = Math.max(120, bodyHeight - 40);
        return new java.awt.Rectangle(bodyX, contentY, bodyWidth, contentHeight);
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
