package mechanist;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

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
        panel.center(g, "SAVE / LOAD", box.x + box.width / 2, box.y + 44);

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
        drawButton(g, panel, panel.saveLoadNewGameRect(), "New Game", true);

        g.setColor(panel.optionColor(GameOptions.TEXT_DIM));
        panel.center(g, "Enter loads selected slot. N starts a new game. Esc returns to the main menu.",
                box.x + box.width / 2, box.y + box.height - 78);
    }

    private static void drawButton(Graphics2D g, GamePanel panel, Rectangle r, String label, boolean primary) {
        g.setColor(primary ? new Color(76, 62, 32, 232) : new Color(16, 18, 16, 224));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(primary ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : new Color(145, 118, 64, 140));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(primary ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : panel.optionColor(GameOptions.TEXT_MAIN));
        panel.center(g, label, r.x + r.width / 2, r.y + 22);
    }

    private static String safe(String text) {
        return text == null || text.isBlank() ? "Select a save slot." : text;
    }
}
