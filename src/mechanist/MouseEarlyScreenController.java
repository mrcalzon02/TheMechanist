package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

final class MouseEarlyScreenController {
    private MouseEarlyScreenController() {}

    static boolean handleIntroZoneEditorAndAudit(GamePanel panel, MouseEvent event, int mx, int my) {
        if (panel.screen == GamePanel.Screen.ZONE_SPLASH) {
            panel.continueFromZoneSplash();
            panel.repaint();
            panel.requestFocusInWindow();
            return true;
        }
        if (panel.screen == GamePanel.Screen.EDITOR && (SwingUtilities.isLeftMouseButton(event) || SwingUtilities.isRightMouseButton(event)) && panel.handleInGameEditorGridPaint(mx, my, SwingUtilities.isRightMouseButton(event))) {
            panel.requestFocusInWindow();
            return true;
        }
        if (handleSectorAuditDropdown(panel, mx, my)) return true;
        if (panel.screen == GamePanel.Screen.SECTOR_AUDIT && panel.auditWorld != null) {
            Point tile = panel.screenPointToAuditTile(mx, my);
            if (tile != null) {
                panel.auditCursorX = tile.x;
                panel.auditCursorY = tile.y;
                panel.repaint();
                panel.requestFocusInWindow();
                return true;
            }
        }
        return false;
    }

    static boolean handleSectorAuditDropdown(GamePanel panel, int mx, int my) {
        if (panel.screen != GamePanel.Screen.SECTOR_AUDIT || !panel.auditZoneDropdownOpen) return false;
        for (int i = panel.buttons.size() - 1; i >= 0; i--) {
            ButtonBox button = panel.buttons.get(i);
            boolean zoneSelectorControl = panel.isZoneAuditDropdownButton(button) || (button != null && button.label != null && button.label.startsWith("ZONE:"));
            if (zoneSelectorControl && button.contains(mx, my)) {
                panel.selectedButton = i;
                panel.sounds.play("button", panel.options);
                panel.runGuarded("MOUSE", "click zone audit selector " + button.label, button.action);
                panel.repaint();
                panel.requestFocusInWindow();
                return true;
            }
        }
        Rectangle dropdown = panel.zoneAuditDropdownOuterRect(panel.uiLayout());
        if (dropdown.contains(mx, my)) {
            panel.repaint();
            panel.requestFocusInWindow();
            return true;
        }
        panel.auditZoneDropdownOpen = false;
        panel.repaint();
        panel.requestFocusInWindow();
        return true;
    }
}
