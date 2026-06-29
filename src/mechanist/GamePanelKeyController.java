package mechanist;

import java.awt.event.KeyEvent;
import java.util.ArrayList;

final class GamePanelKeyController {
    private GamePanelKeyController() {}

    static void keyPressed(GamePanel panel, KeyEvent event) {
        panel.keyboardInputBridge.keyPressed(event);
        panel.runGuarded("INPUT", "keyPressed code=" + event.getKeyCode(), () -> handleKeyPressed(panel, event));
    }

    static void handleKeyPressed(GamePanel panel, KeyEvent event) {
        panel.lastInputMillis = System.currentTimeMillis();
        int code = event.getKeyCode();
        if (KeyEarlyScreenController.handleEarlyKey(panel, code)) return;
        if (CharacterNameKeyController.handleCharacterNameEditKey(panel, code)) return;
        if (handleEscapeRoute(panel, code)) return;
        if (handleKnowledgeScreenKey(panel, code)) return;
        if (handleEditorKey(panel, event, code)) return;
        if (handleMultiplayerKey(panel, code)) return;
        if (handleZoneAuditKey(panel, code)) return;
        if (handleInfopediaPanelKey(panel, code)) return;
        if (InventoryPanelKeyController.handleInventoryPanelKey(panel, code)) return;
        if (handleConstructionPanelKey(panel, event, code)) return;
        if (handleUniversalMenuKey(panel, code)) return;
        if (handleTargetingPanelKey(panel, code)) return;
        if (handleTabKey(panel, code)) return;
        if (handleBuildPlacementPanelKey(panel, code)) return;
        if (handleScrollKeys(panel, code)) return;
        if (handleCharacterScreenKey(panel, code)) return;
        if (handleGameWorldKey(panel, code)) return;
    }

    static boolean handleEscapeRoute(GamePanel panel, int code) {
        if (code != KeyEvent.VK_ESCAPE) return false;
        if (panel.screen == GamePanel.Screen.GAME && panel.manualMovementPlanActive) {
            panel.cancelManualMovementPlan("keyboard cancel");
            return true;
        }
        if (panel.screen == GamePanel.Screen.PANEL && panel.buildPlacementActive
                && (panel.panelMode == GamePanel.PanelMode.BUILD || panel.panelMode == GamePanel.PanelMode.WORKBENCH)) {
            panel.cancelBuildPlacement("keyboard cancel");
            return true;
        }
        if (panel.screen == GamePanel.Screen.GAME) {
            panel.setScreen(GamePanel.Screen.PAUSE);
            return true;
        }
        if (panel.screen == GamePanel.Screen.OPTIONS) {
            panel.closeOptionsScreen();
            return true;
        }
        if (panel.screen == GamePanel.Screen.CHARACTER && panel.newGameSetupActive) {
            panel.newGameSetupActive = false;
            panel.characterNameEditActive = false;
            panel.setScreen(GamePanel.Screen.MENU);
            return true;
        }
        if (panel.screen == GamePanel.Screen.PANEL || panel.screen == GamePanel.Screen.CHARACTER
                || panel.screen == GamePanel.Screen.INVENTORY || panel.screen == GamePanel.Screen.MAP
                || panel.screen == GamePanel.Screen.INFO) {
            panel.closePanel();
            return true;
        }
        if (panel.screen == GamePanel.Screen.PAUSE) {
            panel.setScreen(GamePanel.Screen.GAME);
            return true;
        }
        if (panel.screen == GamePanel.Screen.SECTOR_AUDIT) {
            panel.setScreen(GamePanel.Screen.MODS);
            return true;
        }
        if (panel.screen == GamePanel.Screen.KNOWLEDGE) {
            panel.closeKnowledgeScreen();
            return true;
        }
        if (panel.screen == GamePanel.Screen.MULTIPLAYER) {
            panel.multiplayerMenu.endDirectEdit();
            panel.setScreen(GamePanel.Screen.MENU);
            return true;
        }
        if (panel.screen == GamePanel.Screen.MODS) {
            panel.setScreen(GamePanel.Screen.MENU);
            return true;
        }
        panel.setScreen(GamePanel.Screen.MENU);
        return true;
    }

    static boolean handleKnowledgeScreenKey(GamePanel panel, int code) {
        if (panel.screen != GamePanel.Screen.KNOWLEDGE) return false;
        panel.handleKnowledgeKeyPressed(code);
        return true;
    }

    static boolean handleZoneAuditKey(GamePanel panel, int code) {
        if (panel.screen != GamePanel.Screen.SECTOR_AUDIT) return false;
        if (code == KeyEvent.VK_R) { panel.rerollSectorAudit(); return true; }
        if (code == KeyEvent.VK_T) { panel.cycleAuditZoneType(1); return true; }
        if (code == KeyEvent.VK_G) { panel.cycleAuditZoneDensity(); return true; }
        if (code == KeyEvent.VK_O) { panel.cycleAuditOverlay(); return true; }
        if (code == KeyEvent.VK_Q) { panel.buildAssetAuditDevRoom(); return true; }
        if (code == KeyEvent.VK_N) { panel.jumpAuditFinding(1); return true; }
        if (code == KeyEvent.VK_B) { panel.jumpAuditFinding(-1); return true; }
        if (code == KeyEvent.VK_P || code == KeyEvent.VK_SPACE) { panel.toggleAuditReplay(); return true; }
        boolean assetRoom = AssetAuditDevRoomAuthority.isDevRoom(panel.auditWorld);
        if (code == KeyEvent.VK_OPEN_BRACKET) { if (assetRoom) panel.cycleAuditTileAsset(-1); else panel.stepAuditReplay(-1); return true; }
        if (code == KeyEvent.VK_CLOSE_BRACKET || code == KeyEvent.VK_ENTER) { if (assetRoom) panel.cycleAuditTileAsset(1); else panel.stepAuditReplay(1); return true; }
        if (code == KeyEvent.VK_PAGE_UP) { panel.stepAuditReplay(-1); return true; }
        if (code == KeyEvent.VK_PAGE_DOWN) { panel.stepAuditReplay(1); return true; }
        if (code == KeyEvent.VK_HOME) { panel.rewindAuditReplay(); return true; }
        if (code == KeyEvent.VK_END) { panel.finishAuditReplay(); return true; }
        int dx = 0;
        int dy = 0;
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) dx = -1;
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) dx = 1;
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) dy = -1;
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) dy = 1;
        if (dx != 0 || dy != 0) { panel.moveAuditCursor(dx, dy); return true; }
        return false;
    }

    static boolean handleInfopediaPanelKey(GamePanel panel, int code) {
        if (panel.screen != GamePanel.Screen.PANEL || panel.panelMode != GamePanel.PanelMode.INFOPEDIA) return false;
        if (panel.isAssetInfopediaTab(panel.infopediaTab)) {
            if (code == KeyEvent.VK_SLASH) { panel.activeScrollTag = "infopedia-asset-filter"; panel.repaint(); return true; }
            if ("infopedia-asset-filter".equals(panel.activeScrollTag)) {
                if (code == KeyEvent.VK_BACK_SPACE) { panel.backspaceInfopediaAssetFilter(); return true; }
                if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_ESCAPE) { panel.activeScrollTag = "infopedia-list"; panel.repaint(); return true; }
                return true;
            }
        }
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) { panel.cycleInfopediaTab(-1); return true; }
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) { panel.cycleInfopediaTab(1); return true; }
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) { panel.moveInfopediaSelection(-1); return true; }
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) { panel.moveInfopediaSelection(1); return true; }
        if (code == KeyEvent.VK_PAGE_UP) { panel.activeScrollTag = "infopedia-detail"; panel.scrollActivePanel(-1, true); return true; }
        if (code == KeyEvent.VK_PAGE_DOWN) { panel.activeScrollTag = "infopedia-detail"; panel.scrollActivePanel(1, true); return true; }
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_E) { panel.activeScrollTag = "infopedia-list"; panel.repaint(); return true; }
        return false;
    }

    static boolean handleTargetingPanelKey(GamePanel panel, int code) {
        if (panel.screen != GamePanel.Screen.PANEL) return false;
        if (panel.panelMode == GamePanel.PanelMode.COMBAT && panel.world != null) return handleCombatPanelKey(panel, code);
        if ((panel.panelMode == GamePanel.PanelMode.LOOK || panel.panelMode == GamePanel.PanelMode.INTERACT) && panel.world != null) return handleLookInteractPanelKey(panel, code);
        return false;
    }

    static boolean handleCombatPanelKey(GamePanel panel, int code) {
        int dx = 0;
        int dy = 0;
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) dx = -1;
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) dx = 1;
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) dy = -1;
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) dy = 1;
        if (code == KeyEvent.VK_F) { panel.cycleFireMode(); panel.repaint(); return true; }
        if (code == KeyEvent.VK_Q || code == KeyEvent.VK_TAB) { panel.cycleEquippedWeaponHand(); panel.repaint(); return true; }
        if (code == KeyEvent.VK_G) { panel.throwSelectedExplosiveAtCursor(); panel.repaint(); return true; }
        if (code == KeyEvent.VK_X) { panel.reloadCurrentRangedWeapon(); panel.repaint(); return true; }
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) { panel.confirmCombatTarget(); panel.repaint(); return true; }
        if (dx != 0 || dy != 0) { panel.moveCombatCursor(dx, dy); panel.repaint(); return true; }
        return false;
    }

    static boolean handleLookInteractPanelKey(GamePanel panel, int code) {
        if ((code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_E) && panel.panelMode == GamePanel.PanelMode.INTERACT) { panel.confirmInteraction(); panel.repaint(); return true; }
        if (code == KeyEvent.VK_E && panel.panelMode == GamePanel.PanelMode.LOOK) { panel.examineSelectedLookTarget(); panel.repaint(); return true; }
        int dx = 0;
        int dy = 0;
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) dx = -1;
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) dx = 1;
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) dy = -1;
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) dy = 1;
        if (dx != 0 || dy != 0) {
            if (panel.panelMode == GamePanel.PanelMode.INTERACT) panel.moveInteractCursor(dx, dy);
            else {
                panel.lookCursorActive = true;
                panel.lookX = Math.max(0, Math.min(panel.world.w - 1, panel.lookX + dx));
                panel.lookY = Math.max(0, Math.min(panel.world.h - 1, panel.lookY + dy));
                panel.setFacingToward(panel.lookX, panel.lookY, "look cursor");
                panel.lookStackIndex = 0;
                panel.lookStackScroll = 0;
                ProgressiveLookAuthority.reset(panel, "look cursor moved");
            }
            panel.repaint();
            return true;
        }
        if (panel.panelMode == GamePanel.PanelMode.LOOK && (code == KeyEvent.VK_OPEN_BRACKET || code == KeyEvent.VK_CLOSE_BRACKET)) {
            ArrayList<String> stack = panel.tileStackAt(panel.lookX, panel.lookY);
            if (!stack.isEmpty()) {
                panel.lookStackIndex += (code == KeyEvent.VK_CLOSE_BRACKET ? 1 : -1);
                panel.lookStackIndex = Math.max(0, Math.min(stack.size() - 1, panel.lookStackIndex));
                panel.lookStackScroll = 0;
            }
            panel.repaint();
            return true;
        }
        return false;
    }

    static boolean handleBuildPlacementPanelKey(GamePanel panel, int code) {
        if (panel.screen != GamePanel.Screen.PANEL || !panel.buildPlacementActive) return false;
        if (panel.panelMode != GamePanel.PanelMode.BUILD && panel.panelMode != GamePanel.PanelMode.WORKBENCH) return false;
        int dx = 0;
        int dy = 0;
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) dx = -1;
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) dx = 1;
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) dy = -1;
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) dy = 1;
        if (dx != 0 || dy != 0) { panel.moveBuildCursor(dx, dy); panel.repaint(); return true; }
        if (code == KeyEvent.VK_E || code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) { panel.confirmBuildPlacement(); panel.repaint(); return true; }
        return false;
    }

    static boolean handleConstructionPanelKey(GamePanel panel, KeyEvent event, int code) {
        if (panel.screen != GamePanel.Screen.PANEL) return false;
        if (panel.panelMode != GamePanel.PanelMode.BUILD && panel.panelMode != GamePanel.PanelMode.WORKBENCH) return false;
        int recipeSlot = constructionRecipeSlotForKey(code);
        if (recipeSlot >= 0) {
            return panel.selectVisibleBuildRecipeSlot(recipeSlot, "keyboard");
        }
        if (code == KeyEvent.VK_C || code == KeyEvent.VK_TAB) {
            boolean backwards = code == KeyEvent.VK_TAB && event != null && event.isShiftDown();
            panel.cycleBuildRecipeCategory(backwards ? -1 : 1);
            return true;
        }
        if (code == KeyEvent.VK_PAGE_UP) {
            panel.changeBuildRecipePage(-1);
            return true;
        }
        if (code == KeyEvent.VK_PAGE_DOWN) {
            panel.changeBuildRecipePage(1);
            return true;
        }
        if (code == KeyEvent.VK_HOME) {
            panel.jumpBuildRecipePage(false);
            return true;
        }
        if (code == KeyEvent.VK_END) {
            panel.jumpBuildRecipePage(true);
            return true;
        }
        return false;
    }

    private static int constructionRecipeSlotForKey(int code) {
        if (code >= KeyEvent.VK_1 && code <= KeyEvent.VK_9) return code - KeyEvent.VK_1;
        if (code == KeyEvent.VK_0) return 9;
        if (code >= KeyEvent.VK_NUMPAD1 && code <= KeyEvent.VK_NUMPAD9) return code - KeyEvent.VK_NUMPAD1;
        if (code == KeyEvent.VK_NUMPAD0) return 9;
        return -1;
    }

    static boolean handleUniversalMenuKey(GamePanel panel, int code) {
        boolean cursorSpecial = panel.screen == GamePanel.Screen.PANEL && (panel.panelMode == GamePanel.PanelMode.LOOK || panel.panelMode == GamePanel.PanelMode.COMBAT || panel.panelMode == GamePanel.PanelMode.INTERACT || panel.panelMode == GamePanel.PanelMode.INFOPEDIA || ((panel.panelMode == GamePanel.PanelMode.BUILD || panel.panelMode == GamePanel.PanelMode.WORKBENCH) && panel.buildPlacementActive));
        if (panel.screen == GamePanel.Screen.GAME || cursorSpecial) return false;
        if (panel.screen == GamePanel.Screen.CHARACTER && panel.newGameSetupActive
                && (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_A || code == KeyEvent.VK_D)) return false;
        if (code == KeyEvent.VK_W || code == KeyEvent.VK_A || code == KeyEvent.VK_UP || code == KeyEvent.VK_LEFT) { panel.moveSelectedButton(-1); return true; }
        if (code == KeyEvent.VK_S || code == KeyEvent.VK_D || code == KeyEvent.VK_DOWN || code == KeyEvent.VK_RIGHT) { panel.moveSelectedButton(1); return true; }
        if (code == KeyEvent.VK_E || code == KeyEvent.VK_ENTER) { panel.activateSelectedButtonUniversal(); return true; }
        return false;
    }

    static boolean handleCharacterScreenKey(GamePanel panel, int code) {
        if (panel.screen != GamePanel.Screen.CHARACTER) return false;
        if (code == KeyEvent.VK_PAGE_UP) { panel.jobDossierScroll = Math.max(0, panel.jobDossierScroll - 3); panel.repaint(); return true; }
        if (code == KeyEvent.VK_PAGE_DOWN) { panel.jobDossierScroll += 3; panel.repaint(); return true; }
        if (panel.candidates.isEmpty()) return false;
        if (panel.newGameSetupActive && code == KeyEvent.VK_R) { panel.rerollSelectedCandidate(); return true; }
        if (panel.newGameSetupActive && code == KeyEvent.VK_N) { panel.rerollNewGameRoster(); return true; }
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
            panel.characterNameEditActive = false;
            if (panel.newGameSetupActive) panel.cycleSelectedCandidate(-1);
            else {
                panel.candidateIndex = (panel.candidateIndex + panel.candidates.size() - 1) % panel.candidates.size();
                resetCharacterDossier(panel);
            }
            return true;
        }
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
            panel.characterNameEditActive = false;
            if (panel.newGameSetupActive) panel.cycleSelectedCandidate(1);
            else {
                panel.candidateIndex = (panel.candidateIndex + 1) % panel.candidates.size();
                resetCharacterDossier(panel);
            }
            return true;
        }
        return false;
    }

    static void resetCharacterDossier(GamePanel panel) {
        panel.jobIndex = 0;
        panel.jobDossierScroll = 0;
        panel.jobDossierTab = 0;
        panel.logEvent("Selected candidate " + panel.candidates.get(panel.candidateIndex).name);
        panel.repaint();
    }

    static boolean handleGameWorldKey(GamePanel panel, int code) {
        if (panel.screen != GamePanel.Screen.GAME || panel.world == null) return false;
        if (handleManualMovementPlanKey(panel, code)) return true;
        if (code == KeyEvent.VK_R) { panel.cycleMovementMode(); panel.repaint(); return true; }
        if (code == KeyEvent.VK_P) { panel.beginManualMovementPlan(); return true; }
        if (code == KeyEvent.VK_PERIOD || code == KeyEvent.VK_NUMPAD5) { panel.waitOneTurn(); panel.repaint(); return true; }
        if (code == KeyEvent.VK_L) { panel.beginLookMode(); return true; }
        if (code == KeyEvent.VK_E) { panel.beginInteractMode(); panel.repaint(); return true; }
        if (code == KeyEvent.VK_I) { panel.openPanel(GamePanel.PanelMode.INVENTORY); return true; }
        if (code == KeyEvent.VK_F) { panel.beginCombatTargeting(); panel.repaint(); return true; }
        if (code == KeyEvent.VK_G) { panel.beginExplosiveTargeting(); panel.repaint(); return true; }
        if (code == KeyEvent.VK_X) { panel.reloadCurrentRangedWeapon(); panel.repaint(); return true; }
        if (code == KeyEvent.VK_C) { panel.openPanel(GamePanel.PanelMode.CHARACTER); return true; }
        if (code == KeyEvent.VK_B) { panel.openPanel(GamePanel.PanelMode.BUILD); return true; }
        if (code == KeyEvent.VK_M) { panel.openPanel(GamePanel.PanelMode.MAP); return true; }
        int dx = 0;
        int dy = 0;
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) dx = -1;
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) dx = 1;
        if (code == KeyEvent.VK_W) dy = -1;
        if (code == KeyEvent.VK_S) dy = 1;
        if (dx != 0 || dy != 0) MovementExecutionAuthority.executeStep(panel, dx, dy, "keyboard movement", true);
        panel.repaint();
        panel.sanityCheck("KEY_END");
        return dx != 0 || dy != 0;
    }

    static boolean handleManualMovementPlanKey(GamePanel panel, int code) {
        if (!panel.manualMovementPlanActive) return false;
        int dx = 0;
        int dy = 0;
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) dx = -1;
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) dx = 1;
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) dy = -1;
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) dy = 1;
        if (dx != 0 || dy != 0) { panel.nudgeManualMovementPlan(dx, dy); return true; }
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_E) { panel.confirmManualMovementPlan(); return true; }
        return false;
    }

    static boolean handleEditorKey(GamePanel panel, KeyEvent event, int code) {
        if (panel.screen != GamePanel.Screen.EDITOR) return false;
        if (panel.handleInGameEditorTextKey(code)) return true;
        if (code == KeyEvent.VK_N) {
            panel.createNewInGameEditorEntry();
            return true;
        }
        if (code == KeyEvent.VK_Z && event.isControlDown()) {
            panel.inGameEditorUndo();
            return true;
        }
        if (code == KeyEvent.VK_Y && event.isControlDown()) {
            panel.inGameEditorRedo();
            return true;
        }
        return false;
    }

    static boolean handleMultiplayerKey(GamePanel panel, int code) {
        if (panel.screen != GamePanel.Screen.MULTIPLAYER) return false;
        if (panel.multiplayerMenu.handleKeyPressed(code)) {
            panel.repaint();
            return true;
        }
        try {
            if (code == KeyEvent.VK_I || code == KeyEvent.VK_E) {
                MultiplayerPrivacyAuthority.promptDirectAddress(panel);
                panel.repaint();
                return true;
            }
            if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
                panel.multiplayerMenu.cycleHistory(-1);
                panel.repaint();
                return true;
            }
            if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
                panel.multiplayerMenu.cycleHistory(1);
                panel.repaint();
                return true;
            }
            if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
                panel.multiplayerMenu.cycleFavorite(-1);
                panel.repaint();
                return true;
            }
            if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
                panel.multiplayerMenu.cycleFavorite(1);
                panel.repaint();
                return true;
            }
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_J) {
                NetworkPortAuthority.Endpoint endpoint = panel.multiplayerMenu.joinDirect();
                panel.logEvent("Multiplayer direct endpoint prepared: " + MultiplayerPrivacyAuthority.redactEndpoint(endpoint.display()) + ".");
                panel.repaint();
                return true;
            }
            if (code == KeyEvent.VK_F) {
                panel.multiplayerMenu.addFavoriteFromDirect("Direct server");
                panel.repaint();
                return true;
            }
            if (code == KeyEvent.VK_G) {
                NetworkPortAuthority.Endpoint endpoint = panel.multiplayerMenu.joinFavorite();
                panel.logEvent("Multiplayer favorite endpoint prepared: " + MultiplayerPrivacyAuthority.redactEndpoint(endpoint.display()) + ".");
                panel.repaint();
                return true;
            }
            if (code == KeyEvent.VK_H) {
                if (panel.world == null) {
                    panel.multiplayerMenu.setStatus("Load or start a world before hosting a local multiplayer session.");
                } else {
                    String worldName = panel.world.hiveName == null || panel.world.hiveName.isBlank() ? "The Mechanist World" : panel.world.hiveName;
                    HostBindingResult result = panel.multiplayerMenu.hostFromWorld(panel.seed, worldName, "singleplayer-" + Math.abs(panel.seed), panel.worldSetup, 8);
                    panel.logEvent(result.success() ? "Multiplayer host route active; binding details hidden."
                            : "Multiplayer host route failed; see private diagnostics.");
                }
                panel.repaint();
                return true;
            }
            if (code == KeyEvent.VK_T) {
                panel.multiplayerMenu.joinViaSteamFriend();
                panel.repaint();
                return true;
            }
        } catch (RuntimeException ex) {
            panel.multiplayerMenu.setStatus("Multiplayer command failed: " + ex.getMessage());
            panel.repaint();
            return true;
        }
        return false;
    }

    static boolean handleTabKey(GamePanel panel, int code) {
        if (code != KeyEvent.VK_TAB) return false;
        if (!panel.buttons.isEmpty()) panel.selectedButton = (panel.selectedButton + 1) % panel.buttons.size();
        panel.sounds.play("tab", panel.options);
        panel.repaint();
        return true;
    }

    static boolean handleScrollKeys(GamePanel panel, int code) {
        if (code == KeyEvent.VK_PAGE_UP) return panel.scrollActivePanel(-1, true);
        if (code == KeyEvent.VK_PAGE_DOWN) return panel.scrollActivePanel(1, true);
        if ((panel.screen == GamePanel.Screen.PANEL || panel.screen == GamePanel.Screen.CHARACTER) && (code == KeyEvent.VK_UP || code == KeyEvent.VK_W)) {
            return panel.scrollActivePanel(-1, false);
        }
        if ((panel.screen == GamePanel.Screen.PANEL || panel.screen == GamePanel.Screen.CHARACTER) && (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S)) {
            return panel.scrollActivePanel(1, false);
        }
        return false;
    }
}
