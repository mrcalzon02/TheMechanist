package mechanist;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

final class MouseGamePanelController {
    private MouseGamePanelController() {}

    static boolean handleGameAndPanelTargeting(GamePanel panel, MouseEvent event, int mx, int my) {
        if (panel.screen == GamePanel.Screen.GAME && panel.world != null && panel.firstPersonRenderViewport.handleMouseClicked(panel, event, mx, my)) {
            panel.requestFocusInWindow();
            return true;
        }
        if (handleGameWorldClick(panel, event, mx, my)) return true;
        if (panel.screen == GamePanel.Screen.PANEL && panel.world != null && panel.buildPlacementActive
                && (panel.panelMode == GamePanel.PanelMode.BUILD || panel.panelMode == GamePanel.PanelMode.WORKBENCH)) {
            Point tile = panel.screenPointToWorldTile(mx, my);
            if (tile != null && SwingUtilities.isLeftMouseButton(event)) {
                panel.targetBuildPlacementAt(tile.x, tile.y, "mouse placement");
                panel.requestFocusInWindow();
                return true;
            }
            if (SwingUtilities.isRightMouseButton(event)) {
                panel.cancelBuildPlacement("mouse cancel");
                panel.requestFocusInWindow();
                return true;
            }
        }
        if (panel.screen == GamePanel.Screen.PANEL && panel.world != null && (panel.panelMode == GamePanel.PanelMode.LOOK || panel.panelMode == GamePanel.PanelMode.COMBAT || panel.panelMode == GamePanel.PanelMode.INTERACT)) {
            Point tile = panel.screenPointToWorldTile(mx, my);
            if (tile != null && (SwingUtilities.isLeftMouseButton(event) || SwingUtilities.isRightMouseButton(event))) {
                PanelTargetingController.targetPanelCursorFromMouse(panel, tile.x, tile.y);
                panel.repaint();
                panel.requestFocusInWindow();
                return true;
            }
        }
        return false;
    }

    static boolean handleGameWorldClick(GamePanel panel, MouseEvent event, int mx, int my) {
        if (panel.screen != GamePanel.Screen.GAME || panel.world == null) return false;
        Point tile = panel.screenPointToWorldTile(mx, my);
        if (SwingUtilities.isRightMouseButton(event) && tile != null) {
            if (tile.x == panel.playerX && tile.y == panel.playerY) {
                panel.clearMouseMovementPreview("right-click self opens look targeting");
                panel.interactCursorActive = false;
                panel.lookX = panel.playerX;
                panel.lookY = panel.playerY;
                panel.lookCursorActive = true;
                panel.openPanel(GamePanel.PanelMode.LOOK);
                panel.repaint();
                panel.requestFocusInWindow();
                return true;
            }
            if (panel.mouseMovePreviewActive) panel.clearMouseMovementPreview("right-click toggle");
            else panel.updateMouseMovementPreviewTo(tile.x, tile.y);
            panel.repaint();
            panel.requestFocusInWindow();
            return true;
        }
        if (SwingUtilities.isLeftMouseButton(event) && panel.mouseMovePreviewActive) {
            panel.executeMouseMovementPreview();
            panel.requestFocusInWindow();
            return true;
        }
        return false;
    }
}
