package mechanist;

import java.awt.event.KeyEvent;

final class InventoryPanelKeyController {
    private InventoryPanelKeyController() {}

    static boolean handleInventoryPanelKey(GamePanel panel, int code) {
        if (panel.screen != GamePanel.Screen.PANEL || panel.panelMode != GamePanel.PanelMode.INVENTORY) return false;
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
            panel.inventoryTargetColumnActive = false;
            panel.inventoryItemDescriptionScroll = 0;
            panel.repaint();
            return true;
        }
        if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
            panel.inventoryTargetColumnActive = true;
            panel.inventoryItemDescriptionScroll = 0;
            panel.repaint();
            return true;
        }
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
            if (panel.inventoryTargetColumnActive) panel.moveTargetInventorySelection(-1);
            else panel.moveInventorySelection(-1);
            panel.repaint();
            return true;
        }
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
            if (panel.inventoryTargetColumnActive) panel.moveTargetInventorySelection(1);
            else panel.moveInventorySelection(1);
            panel.repaint();
            return true;
        }
        if (code == KeyEvent.VK_PAGE_UP || code == KeyEvent.VK_PAGE_DOWN) {
            panel.inventoryItemDescriptionScroll = Math.max(0, panel.inventoryItemDescriptionScroll + (code == KeyEvent.VK_PAGE_DOWN ? 3 : -3));
            panel.repaint();
            return true;
        }
        if (code == KeyEvent.VK_T) {
            panel.throwSelectedPortableLight();
            return true;
        }
        return false;
    }
}
