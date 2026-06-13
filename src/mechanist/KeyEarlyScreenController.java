package mechanist;

import java.awt.event.KeyEvent;

final class KeyEarlyScreenController {
    private KeyEarlyScreenController() {}

    static boolean handleEarlyKey(GamePanel panel, int code) {
        if (handleEulaGate(panel, code)) return true;
        if (SaveLoadSurfacePainter.handleDeleteKey(panel, code)) return true;
        if (code == KeyEvent.VK_F3) {
            panel.togglePerformanceDiagnostics();
            return true;
        }
        if (code == KeyEvent.VK_F1 && (panel.screen == GamePanel.Screen.GAME || panel.screen == GamePanel.Screen.PANEL)) {
            panel.toggleTacticalSlate();
            return true;
        }
        if (code == KeyEvent.VK_F2 && panel.screen != GamePanel.Screen.EDITOR) {
            panel.openChatWindow();
            return true;
        }
        if (code == KeyEvent.VK_F4 && (panel.screen == GamePanel.Screen.GAME || panel.screen == GamePanel.Screen.PANEL)) {
            panel.openAuspexPanel();
            return true;
        }
        if (code == KeyEvent.VK_F5 && (panel.screen == GamePanel.Screen.GAME || panel.screen == GamePanel.Screen.PANEL)) {
            panel.openCraftingPanel();
            return true;
        }
        if (code == KeyEvent.VK_F6 && (panel.screen == GamePanel.Screen.GAME || panel.screen == GamePanel.Screen.PANEL)) {
            panel.openScavengePanel();
            return true;
        }
        if (code == KeyEvent.VK_Y && panel.screen != GamePanel.Screen.EDITOR) {
            panel.openChatWindow();
            return true;
        }
        if (panel.screen == GamePanel.Screen.GAME && panel.firstPersonRenderViewport.handleKeyPressed(panel, code)) {
            panel.repaint();
            return true;
        }
        if (panel.worldZoomControlActive() && (code == KeyEvent.VK_EQUALS || code == KeyEvent.VK_PLUS || code == KeyEvent.VK_ADD || code == KeyEvent.VK_MINUS || code == KeyEvent.VK_SUBTRACT)) {
            boolean zoomIn = code == KeyEvent.VK_EQUALS || code == KeyEvent.VK_PLUS || code == KeyEvent.VK_ADD;
            panel.changeWorldZoom(zoomIn ? 1 : -1, KeyEvent.getKeyText(code));
            return true;
        }
        if (panel.screen == GamePanel.Screen.INTRO_CRAWL) {
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ESCAPE) {
                panel.continueFromIntroCrawl();
                panel.repaint();
            }
            return true;
        }
        if (panel.screen == GamePanel.Screen.ZONE_SPLASH) {
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ESCAPE) {
                panel.continueFromZoneSplash();
                panel.repaint();
            }
            return true;
        }
        if (panel.screen == GamePanel.Screen.CAPTURE) {
            if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_E) {
                panel.setScreen(GamePanel.Screen.GAME);
                panel.repaint();
            }
            return true;
        }
        if (panel.screen == GamePanel.Screen.BOOT) {
            if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE) {
                panel.sounds.play("button", panel.options);
                BootMenuFlowAuthority.finishBootSequence(panel, "key");
            }
            return true;
        }
        return false;
    }

    static boolean handleEulaGate(GamePanel panel, int code) {
        if (!panel.eulaGateActive) return false;
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_A) {
            panel.acceptEulaGate();
            panel.repaint();
            return true;
        }
        if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_Q || code == KeyEvent.VK_X) {
            panel.requestApplicationExit("EULA declined by key");
            return true;
        }
        if (code == KeyEvent.VK_PAGE_DOWN) {
            panel.scrollEulaGate(1, true);
            return true;
        }
        if (code == KeyEvent.VK_PAGE_UP) {
            panel.scrollEulaGate(-1, true);
            return true;
        }
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
            panel.scrollEulaGate(1, false);
            return true;
        }
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
            panel.scrollEulaGate(-1, false);
            return true;
        }
        if (code == KeyEvent.VK_HOME) {
            panel.eulaScroll = 0;
            panel.repaint();
            return true;
        }
        if (code == KeyEvent.VK_END) {
            panel.eulaScroll = Math.max(0, panel.eulaMaxScroll);
            panel.repaint();
            return true;
        }
        return true;
    }
}
