package mechanist;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

final class SaveLoadSurfacePainter implements ScreenPainter {
    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        Rectangle box = panel.saveLoadPanelRect();
        g.setColor(new Color(0, 0, 0, 198));
        g.fillRoundRect(box.x, box.y, box.width, box.height, 14, 14);
        panel.drawSlicedFrame(g, box.x, box.y, box.width, box.height, "inner");
        panel.stampUiFrameId(g, "F", "save-load-menu", box.x, box.y, box.width, box.height);

        g.setFont(panel.titleFont.deriveFont(Font.BOLD, Math.max(24f, Math.min(38f, box.height / 12f))));
        g.setColor(panel.optionColor(GameOptions.TEXT_HIGHLIGHT));
        panel.center(g, "WORLD MANAGEMENT", box.x + box.width / 2, box.y + 44);

        g.setFont(panel.smallFont);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(panel.optionColor(GameOptions.TEXT_MAIN));
        panel.center(g, safe(panel.saveLoadStatus), box.x + box.width / 2, box.y + 72);

        int[] slots = panel.saveLoadSlots();
        for (int i = 0; i < slots.length; i++) {
            Rectangle row = panel.saveLoadSlotRect(i);
            boolean selected = i == panel.saveLoadSelectedIndex;
            g.setColor(selected ? new Color(76, 62, 32, 232) : new Color(16, 18, 16, 224));
            g.fillRoundRect(row.x, row.y, row.width, row.height, 8, 8);
            g.setColor(selected ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : new Color(145, 118, 64, 125));
            g.drawRoundRect(row.x, row.y, row.width, row.height, 8, 8);

            String label = (selected ? "> " : "  ") + SaveSlotSurfaceApi.saveSlotSummary(slots[i]);
            g.setColor(selected ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : panel.optionColor(GameOptions.TEXT_MAIN));
            g.drawString(GuiLayoutApi.fitLabel(label, fm, Math.max(120, row.width - 18)), row.x + 10, row.y + 29);
        }

        drawButton(g, panel, panel.saveLoadBackRect(), "Back", false);
        drawDangerButton(g, panel, deleteSelectedSlotRect(panel), "Delete Slot");
        drawButton(g, panel, panel.saveLoadNewGameRect(), "New Game", true);

        g.setColor(panel.optionColor(GameOptions.TEXT_DIM));
        panel.center(g, "Enter loads selected slot. Delete/D deletes selected slot. N starts a new game. Esc returns.",
                box.x + box.width / 2, box.y + box.height - 78);
    }

    static Rectangle deleteSelectedSlotRect(GamePanel panel) {
        Rectangle p = panel.saveLoadPanelRect();
        return new Rectangle(p.x + p.width / 2 - 78, p.y + p.height - 58, 156, 34);
    }

    static boolean handleDeleteClick(GamePanel panel, int mx, int my) {
        if (panel == null || panel.screen != GamePanel.Screen.SAVE_LOAD) return false;
        if (!deleteSelectedSlotRect(panel).contains(mx, my)) return false;
        requestDeleteSelectedSlot(panel, "mouse");
        panel.repaint();
        panel.requestFocusInWindow();
        return true;
    }

    static boolean handleDeleteKey(GamePanel panel, int code) {
        if (panel == null || panel.screen != GamePanel.Screen.SAVE_LOAD) return false;
        if (code != KeyEvent.VK_DELETE && code != KeyEvent.VK_BACK_SPACE && code != KeyEvent.VK_D) return false;
        requestDeleteSelectedSlot(panel, "key");
        panel.repaint();
        panel.requestFocusInWindow();
        return true;
    }

    private static void requestDeleteSelectedSlot(GamePanel panel, String reason) {
        int slot = panel.selectedSaveLoadSlot();
        java.nio.file.Path path = SaveSlotSurfaceApi.savePathForSlot(slot);
        String label = SaveSlotSurfaceApi.slotLabel(slot);
        if (!java.nio.file.Files.isRegularFile(path)) {
            panel.saveLoadStatus = label + " is already empty.";
            panel.logEvent(panel.saveLoadStatus);
            return;
        }
        String confirm = "Confirm delete " + label + ": press Delete/D again.";
        if (!confirm.equals(panel.saveLoadStatus)) {
            panel.saveLoadStatus = confirm;
            panel.logEvent(panel.saveLoadStatus);
            return;
        }
        try {
            java.nio.file.Files.deleteIfExists(path);
            panel.saveLoadStatus = "Deleted " + label + ".";
            panel.logEvent(panel.saveLoadStatus);
            DebugLog.audit("SAVE_LOAD_MENU", "deleted slot=" + slot + " reason=" + (reason == null ? "unspecified" : reason) + " path=" + path);
        } catch (Throwable t) {
            panel.saveLoadStatus = "Could not delete " + label + ": " + t.getMessage();
            panel.logEvent(panel.saveLoadStatus);
            DebugLog.error("SAVE_LOAD_MENU", panel.saveLoadStatus, t);
        }
    }

    private static void drawButton(Graphics2D g, GamePanel panel, Rectangle r, String label, boolean primary) {
        g.setColor(primary ? new Color(76, 62, 32, 232) : new Color(16, 18, 16, 224));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(primary ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : new Color(145, 118, 64, 140));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(primary ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : panel.optionColor(GameOptions.TEXT_MAIN));
        drawButtonLabel(g, panel, r, label);
    }

    private static void drawDangerButton(Graphics2D g, GamePanel panel, Rectangle r, String label) {
        g.setColor(new Color(58, 18, 14, 232));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(new Color(192, 74, 58, 180));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(new Color(236, 168, 142));
        drawButtonLabel(g, panel, r, label);
    }

    private static void drawButtonLabel(Graphics2D g, GamePanel panel, Rectangle r, String label) {
        BufferedImage icon = panel.systemButtonIconForLabel(label);
        FontMetrics fm = g.getFontMetrics();
        int iconSize = icon == null ? 0 : Math.max(18, Math.min(r.height - 8, 28));
        int labelW = fm.stringWidth(label);
        int groupW = labelW + (iconSize > 0 ? iconSize + 8 : 0);
        int x = r.x + Math.max(8, (r.width - groupW) / 2);
        if (icon != null) {
            g.drawImage(icon, x, r.y + (r.height - iconSize) / 2, iconSize, iconSize, null);
            x += iconSize + 8;
        }
        g.drawString(label, x, r.y + 22);
    }

    private static String safe(String text) {
        return text == null || text.isBlank() ? "Select a save slot." : text;
    }
}
