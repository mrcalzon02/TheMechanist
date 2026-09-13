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
        if (handleCharacterNameClick(panel, mx, my)) return true;
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
        for (int i = panel.buttons.size() - 1; i >= 0; i--) {
            ButtonBox button = panel.buttons.get(i);
            if (UiModalButtonController.buttonIsModalInteractive(panel, button) && button.contains(mx, my)) {
                panel.selectedButton = i;
                UiModalButtonController.reconcileCharacterSetupSelection(panel, button);
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
                || (panel.screen == GamePanel.Screen.PANEL && panel.panelMode == GamePanel.PanelMode.CHARACTER);
        if (!characterSurface
                || CharacterEquipmentAndMedicalAuthority.CharacterTab.at(panel.characterTab)
                != CharacterEquipmentAndMedicalAuthority.CharacterTab.EQUIPMENT) return false;

        java.awt.Rectangle doll = characterEquipmentPaperDollBounds(panel.getWidth(), panel.getHeight());
        String bodyPart = CharacterEquipmentAndMedicalAuthority.bodyPartAt(panel.active, doll, mx, my);
        CharacterEquipmentAndMedicalAuthority.EquipmentSlot slot = equipmentSlotForBodyPart(
                bodyPart, panel.selectedCharacterEquipmentSlot);
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

    static CharacterEquipmentAndMedicalAuthority.EquipmentSlot equipmentSlotForBodyPart(String bodyPart,
                                                                                          int preferredSelection) {
        if (bodyPart != null && !bodyPart.isBlank()) {
            CharacterEquipmentAndMedicalAuthority.EquipmentSlot preferredSlot =
                    CharacterEquipmentAndMedicalAuthority.EquipmentSlot.at(preferredSelection);
            for (String region : CharacterEquipmentAndMedicalAuthority.bodyRegionsForEquipmentSlot(preferredSlot)) {
                if (sameEquipmentRegionAssociation(bodyPart, region)) return preferredSlot;
            }
        }
        return equipmentSlotForBodyPart(bodyPart);
    }

    private static boolean sameEquipmentRegionAssociation(String bodyPart, String region) {
        if (bodyPart == null || region == null) return false;
        if (region.equalsIgnoreCase(bodyPart)) return true;
        CharacterEquipmentAndMedicalAuthority.EquipmentSlot bodySlot = equipmentSlotForBodyPart(bodyPart);
        CharacterEquipmentAndMedicalAuthority.EquipmentSlot regionSlot = equipmentSlotForBodyPart(region);
        return bodySlot != null && bodySlot == regionSlot;
    }

    static boolean handleCharacterMedicalPaperDollClick(GamePanel panel, int mx, int my) {
        if (panel == null || panel.active == null || panel.newGameSetupActive) return false;
        boolean characterSurface = panel.screen == GamePanel.Screen.CHARACTER
                || (panel.screen == GamePanel.Screen.PANEL && panel.panelMode == GamePanel.PanelMode.CHARACTER);
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
        int preferredWidth = Math.max(230, Math.min(310, content.width * 27 / 100));
        int dollWidth = Math.max(1, Math.min(content.width, preferredWidth));
        return new java.awt.Rectangle(content.x, content.y, dollWidth, content.height);
    }

    static java.awt.Rectangle characterMedicalPaperDollBounds(int width, int height) {
        java.awt.Rectangle content = characterTabContentBounds(width, height);
        int preferredWidth = Math.max(250, Math.min(330, content.width * 34 / 100));
        int dollWidth = Math.max(1, Math.min(content.width, preferredWidth));
        return new java.awt.Rectangle(content.x, content.y, dollWidth, content.height);
    }

    private static java.awt.Rectangle characterTabContentBounds(int width, int height) {
        int safeWidth = Math.max(1, width);
        int safeHeight = Math.max(1, height);
        int availablePanelWidth = Math.max(1, safeWidth - 36);
        int availablePanelHeight = Math.max(1, safeHeight - 92);
        int preferredPanelWidth = Math.max(820, Math.min(Math.max(1, safeWidth - 48), (int)Math.round(safeWidth * 0.90)));
        int preferredPanelHeight = Math.max(560, Math.min(Math.max(1, safeHeight - 92), (int)Math.round(safeHeight * 0.84)));
        int panelWidth = Math.min(availablePanelWidth, preferredPanelWidth);
        int panelHeight = Math.min(availablePanelHeight, preferredPanelHeight);
        int panelX = Math.max(0, (safeWidth - panelWidth) / 2);
        int panelY = Math.max(0, (safeHeight - panelHeight) / 2);

        int horizontalInset = Math.min(18, Math.max(0, (panelWidth - 1) / 2));
        int topInset = Math.min(54, Math.max(0, panelHeight - 1));
        int bottomInset = Math.min(58, Math.max(0, panelHeight - topInset - 1));
        int bodyX = panelX + horizontalInset;
        int bodyY = panelY + topInset;
        int bodyWidth = Math.max(1, panelWidth - horizontalInset * 2);
        int bodyHeight = Math.max(1, panelHeight - topInset - bottomInset);

        int headerHeight = Math.min(40, Math.max(0, bodyHeight - 1));
        int contentY = bodyY + headerHeight;
        int contentHeight = Math.max(1, bodyHeight - headerHeight);
        return new java.awt.Rectangle(bodyX, contentY, bodyWidth, contentHeight);
    }

    static boolean handleCharacterNameClick(GamePanel panel, int mx, int my) {
        if (panel == null || !panel.newGameSetupActive || panel.screen != GamePanel.Screen.CHARACTER || panel.characterNameEditRect == null) return false;
        if (panel.characterNameEditRect.contains(mx, my)) {
            if (panel.candidates.isEmpty()) {
                panel.characterNameEditActive = false;
                return false;
            }
            panel.candidateIndex = Math.max(0, Math.min(panel.candidateIndex, panel.candidates.size() - 1));
            panel.characterNameEditActive = true;
            panel.requestFocusInWindow();
            panel.repaint();
            return true;
        }
        if (panel.characterNameEditActive) {
            Candidate candidate = null;
            if (!panel.candidates.isEmpty()) {
                panel.candidateIndex = Math.max(0, Math.min(panel.candidateIndex, panel.candidates.size() - 1));
                candidate = panel.candidates.get(panel.candidateIndex);
            }
            if (candidate != null) {
                candidate.name = CharacterCreationAuthority.sanitizePlayerName(candidate.name, panel.rng);
                panel.refreshNameLockedCandidateState(candidate);
            }
            panel.characterNameEditActive = false;
        }
        return false;
    }
}
