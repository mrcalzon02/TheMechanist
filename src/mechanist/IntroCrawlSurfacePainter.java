package mechanist;

import java.awt.*;
import java.util.List;

/**
 * Conservative extracted renderer for the intro crawl surface.
 *
 * This restores the missing ScreenPainter bridge without touching GamePanel.java.
 * The richer original crawl behavior can be re-mined later; this implementation
 * keeps boot/navigation compile-safe and readable.
 */
final class IntroCrawlSurfacePainter implements ScreenPainter {
    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        if (g == null || panel == null) return;
        int w = Math.max(1, panel.getWidth());
        int h = Math.max(1, panel.getHeight());
        long elapsed = Math.max(0L, System.currentTimeMillis() - panel.bootStartMillis);
        int drift = (int)((elapsed / 60L) % Math.max(1, h + 220));

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);

        Font title = panel.titleFont == null ? new Font("Monospaced", Font.BOLD, 34) : panel.titleFont;
        Font body = panel.smallFont == null ? new Font("Monospaced", Font.PLAIN, 15) : panel.smallFont;

        g.setFont(title);
        g.setColor(new Color(210, 188, 110));
        drawCentered(g, "THE MECHANIST", w / 2, Math.max(54, h / 5));

        g.setFont(body);
        g.setColor(new Color(180, 165, 112));
        int maxWidth = Math.max(220, Math.min(760, w - 120));
        List<String> lines = GuiLayoutApi.wrapText(
                "A sanctioned machine-city wakes beneath a failing Concord. Every corridor records debt, every room remembers ownership, and every useful device waits to be reclaimed by someone with tools, patience, or authority.",
                g.getFontMetrics(), maxWidth);

        int y = Math.max(h / 3, h - drift + 80);
        for (String line : lines) {
            drawCentered(g, line, w / 2, y);
            y += Math.max(18, g.getFontMetrics().getHeight() + 4);
        }

        g.setColor(new Color(116, 132, 96));
        g.setFont(body.deriveFont(Font.BOLD, Math.max(11f, body.getSize2D())));
        drawCentered(g, "Press any key to continue", w / 2, h - 58);
    }

    private void drawCentered(Graphics2D g, String text, int x, int y) {
        String safe = text == null ? "" : text;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(safe, x - fm.stringWidth(safe) / 2, y);
    }
}
