package mechanist;

import java.awt.event.KeyEvent;

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
        if (InventoryPanelKeyController.handleInventoryPanelKey(panel, code)) return;
        if (handleEditorKey(panel, event, code)) return;
        if (handleMultiplayerKey(panel, code)) return;
        if (handleTabKey(panel, code)) return;
        if (handleScrollKeys(panel, code)) return;
    }

    static boolean handleEscapeRoute(GamePanel panel, int code) {
        if (code != KeyEvent.VK_ESCAPE) return false;
        if (panel.screen == GamePanel.Screen.GAME && panel.manualMovementPlanActive) {
            panel.cancelManualMovementPlan("keyboard cancel");
            return true;
        }
        if (panel.screen == GamePanel.Screen.GAME) {
            panel.setScreen(GamePanel.Screen.PAUSE);
            return true;
        }
        if (panel.screen == GamePanel.Screen.PANEL) {
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

    static boolean handleEditorKey(GamePanel panel, KeyEvent event, int code) {
        if (panel.screen != GamePanel.Screen.EDITOR) return false;
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
        if (panel.screen == GamePanel.Screen.MULTIPLAYER && panel.multiplayerMenu.handleKeyPressed(code)) {
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
