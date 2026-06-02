package mechanist;

import java.awt.*;

/**
 * Stateless extracted rendering surface for loading transitions.
 *
 * This intentionally stays conservative until the richer loading animation is
 * reconstructed from the GamePanel shards. It must never delegate back into itself.
 */
final class LoadingSurfacePainter implements ScreenPainter {
    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        int w = Math.max(1, panel.getWidth());
        int h = Math.max(1, panel.getHeight());

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);

        g.setFont(panel.titleFont == null ? new Font("Monospaced", Font.BOLD, 32) : panel.titleFont.deriveFont(Font.BOLD, Math.max(24f, Math.min(40f, h / 16f))));
        g.setColor(new Color(185, 225, 150));
        drawCentered(g, "LOADING", w / 2, Math.max(90, h / 2 - 24));

        g.setFont(panel.smallFont == null ? new Font("Monospaced", Font.BOLD, 14) : panel.smallFont);
        g.setColor(new Color(130, 160, 130));
        drawCentered(g, "please stand by", w / 2, Math.max(120, h / 2 + 18));
    }

    private void drawCentered(Graphics2D g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text == null ? "" : text, x - fm.stringWidth(text == null ? "" : text) / 2, y);
    }
}
