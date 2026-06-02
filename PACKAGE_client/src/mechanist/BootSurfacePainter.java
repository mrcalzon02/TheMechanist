package mechanist;

import java.awt.*;

/**
 * Stateless extracted rendering surface for the boot screen.
 *
 * This intentionally stays conservative until the original richer boot animation is
 * reconstructed from the GamePanel shards. It must never delegate back into itself.
 */
final class BootSurfacePainter implements ScreenPainter {
    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        int w = Math.max(1, panel.getWidth());
        int h = Math.max(1, panel.getHeight());
        long elapsed = Math.max(0L, System.currentTimeMillis() - panel.bootStartMillis);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);

        g.setFont(panel.titleFont == null ? new Font("Monospaced", Font.BOLD, 36) : panel.titleFont);
        g.setColor(new Color(185, 225, 150));
        drawCentered(g, "THE MECHANIST", w / 2, Math.max(80, h / 3));

        g.setFont(panel.smallFont == null ? new Font("Monospaced", Font.BOLD, 14) : panel.smallFont);
        g.setColor(new Color(130, 160, 130));
        drawCentered(g, "boot sequence // " + (elapsed / 250L % 4 == 0 ? "." : elapsed / 250L % 4 == 1 ? ".." : elapsed / 250L % 4 == 2 ? "..." : "...."), w / 2, Math.max(120, h / 3 + 42));
        drawCentered(g, "systems initializing", w / 2, Math.max(150, h / 3 + 70));
    }

    private void drawCentered(Graphics2D g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text == null ? "" : text, x - fm.stringWidth(text == null ? "" : text) / 2, y);
    }
}
