package mechanist;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Common pixel-fitted text and isolated Java2D contexts for early-game screens. */
final class EarlyScreenTextAuthority {
    private EarlyScreenTextAuthority() { }

    static Font fitFont(Graphics2D g, Font font, String text, int maxWidth, float minimumSize) {
        Font selected = font == null ? new Font("SansSerif", Font.PLAIN, 14) : font;
        if (g == null) return selected;
        float min = Math.max(10f, Math.min(selected.getSize2D(), minimumSize));
        while (selected.getSize2D() > min
                && g.getFontMetrics(selected).stringWidth(text == null ? "" : text) > Math.max(1, maxWidth)) {
            selected = selected.deriveFont(Math.max(min, selected.getSize2D() - 1f));
        }
        return selected;
    }

    static String fit(Graphics2D g, String text, int maxWidth) {
        if (g == null) return "";
        return GuiLayoutApi.fitLabel(text, g.getFontMetrics(), Math.max(1, maxWidth));
    }

    static void drawCentered(Graphics2D g, String text, int x, int baseline, int maxWidth) {
        if (g == null) return;
        String fitted = fit(g, text, maxWidth);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(fitted, x - fm.stringWidth(fitted) / 2, baseline);
    }

    static Graphics2D isolatedScene(Graphics2D source, int width, int height) {
        Graphics2D scene = (Graphics2D) source.create();
        scene.clipRect(0, 0, Math.max(1, width), Math.max(1, height));
        scene.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        scene.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return scene;
    }
}
