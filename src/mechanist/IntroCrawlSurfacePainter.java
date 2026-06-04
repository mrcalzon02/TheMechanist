package mechanist;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracted renderer for the intro crawl surface.
 *
 * The visible intro is now owned here as a stateless immediate-mode surface rather
 * than by the GamePanel shard.  GamePanel supplies only runtime dimensions,
 * fonts, and boot clock state.
 */
final class IntroCrawlSurfacePainter implements ScreenPainter {
    private static final Color BACKDROP_TOP = new Color(6, 7, 9);
    private static final Color BACKDROP_BOTTOM = new Color(22, 20, 14);
    private static final Color TITLE_GOLD = new Color(218, 194, 116);
    private static final Color BODY_GOLD = new Color(185, 170, 116);
    private static final Color MUTED_GREEN = new Color(116, 132, 96);
    private static final Color RULE_COLOR = new Color(92, 86, 58, 150);

    private static final String TITLE = "NEW WORLD INSERTION";
    private static final List<String> FALLBACK_CRAWL = List.of(
            "The arcology opens beneath you.",
            "Its machines are waiting.",
            "Its debts are not asleep."
    );

    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        if (g == null || panel == null) return;
        int w = Math.max(1, panel.getWidth());
        int h = Math.max(1, panel.getHeight());
        long elapsed = Math.max(0L, System.currentTimeMillis() - panel.bootStartMillis);

        Object oldAntialias = g.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        Object oldRendering = g.getRenderingHint(RenderingHints.KEY_RENDERING);
        Stroke oldStroke = g.getStroke();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            paintBackdrop(g, w, h, elapsed);
            paintTitle(g, panel, w, h);
            paintCrawl(g, panel, w, h, elapsed);
            paintPrompt(g, panel, w, h, elapsed);
        } finally {
            g.setStroke(oldStroke);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAntialias);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, oldRendering);
        }
    }

    private void paintBackdrop(Graphics2D g, int w, int h, long elapsed) {
        g.setPaint(new LinearGradientPaint(0, 0, 0, h,
                new float[]{0.0f, 1.0f},
                new Color[]{BACKDROP_TOP, BACKDROP_BOTTOM}));
        g.fillRect(0, 0, w, h);

        int centerX = w / 2;
        int horizonY = Math.max(68, h / 5);
        g.setColor(new Color(48, 43, 28, 85));
        g.fillOval(centerX - w / 3, horizonY - h / 7, Math.max(1, (w * 2) / 3), Math.max(1, h / 4));

        g.setColor(new Color(152, 136, 84, 72));
        int seed = (int)((elapsed / 80L) % 997L);
        for (int i = 0; i < 42; i++) {
            int x = Math.floorMod(i * 97 + seed * 7, Math.max(1, w));
            int y = Math.floorMod(i * 53 + seed * 3, Math.max(1, Math.max(1, h - 130))) + 34;
            int size = 1 + (i % 3 == 0 ? 1 : 0);
            g.fillRect(x, y, size, size);
        }

        g.setColor(new Color(0, 0, 0, 54));
        for (int y = 0; y < h; y += 4) {
            g.drawLine(0, y, w, y);
        }
    }

    private void paintTitle(Graphics2D g, GamePanel panel, int w, int h) {
        Font title = panel.titleFont == null ? new Font("Monospaced", Font.BOLD, 34) : panel.titleFont;
        Font subtitle = panel.smallFont == null ? new Font("Monospaced", Font.PLAIN, 15) : panel.smallFont;
        int titleY = Math.max(54, h / 6);

        g.setFont(title);
        g.setColor(new Color(0, 0, 0, 190));
        drawCentered(g, TITLE, w / 2 + 2, titleY + 2);
        g.setColor(TITLE_GOLD);
        drawCentered(g, TITLE, w / 2, titleY);

        g.setFont(subtitle.deriveFont(Font.BOLD, Math.max(11f, subtitle.getSize2D())));
        g.setColor(MUTED_GREEN);
        String operator = panel.active == null ? "Unnamed operator" : panel.active.name + " // " + panel.active.job;
        drawCentered(g, operator, w / 2, titleY + Math.max(26, title.getSize() / 2 + 16));

        int ruleY = titleY + Math.max(44, title.getSize() + 18);
        int ruleWidth = Math.max(180, Math.min(620, w - 96));
        g.setColor(RULE_COLOR);
        g.setStroke(new BasicStroke(1.4f));
        g.drawLine(w / 2 - ruleWidth / 2, ruleY, w / 2 + ruleWidth / 2, ruleY);
    }

    private void paintCrawl(Graphics2D g, GamePanel panel, int w, int h, long elapsed) {
        Font body = panel.smallFont == null ? new Font("Monospaced", Font.PLAIN, 15) : panel.smallFont;
        g.setFont(body);
        FontMetrics fm = g.getFontMetrics();
        int maxWidth = Math.max(220, Math.min(760, w - 120));
        int lineHeight = Math.max(18, fm.getHeight() + 4);
        int paragraphGap = Math.max(10, lineHeight / 2);
        int driftRange = Math.max(1, h + 260);
        int drift = (int)((elapsed / 18L) % driftRange);
        int y = Math.max(h / 3, h - drift + 84);
        int bottomLimit = Math.max(80, h - 82);

        g.setColor(BODY_GOLD);
        for (String paragraph : introCrawlParagraphs(panel)) {
            List<String> lines = GuiLayoutApi.wrapText(paragraph, fm, maxWidth);
            for (String line : lines) {
                if (y > Math.max(0, h / 4) && y < bottomLimit) {
                    int alpha = fadeAlpha(y, h);
                    g.setColor(new Color(BODY_GOLD.getRed(), BODY_GOLD.getGreen(), BODY_GOLD.getBlue(), alpha));
                    drawCentered(g, line, w / 2, y);
                }
                y += lineHeight;
            }
            y += paragraphGap;
        }
    }

    private List<String> introCrawlParagraphs(GamePanel panel) {
        ArrayList<String> loaded = new ArrayList<>();
        try {
            if (panel != null && panel.images != null) loaded.addAll(panel.images.loadIntroCrawlLines());
        } catch (Throwable t) {
            DebugLog.warn("INTRO_CRAWL", "Could not load authored intro crawl text; using fallback: " + t.getMessage());
        }
        loaded.removeIf(line -> line == null || line.isBlank());
        return loaded.isEmpty() ? FALLBACK_CRAWL : loaded;
    }

    private void paintPrompt(Graphics2D g, GamePanel panel, int w, int h, long elapsed) {
        Font body = panel.smallFont == null ? new Font("Monospaced", Font.PLAIN, 15) : panel.smallFont;
        boolean bright = ((elapsed / 650L) % 2L) == 0L;
        Color prompt = bright ? new Color(150, 164, 116) : new Color(86, 96, 72);
        g.setColor(prompt);
        g.setFont(body.deriveFont(Font.BOLD, Math.max(11f, body.getSize2D())));
        drawCentered(g, "Enter / Space / Esc to continue", w / 2, h - 58);
    }

    private int fadeAlpha(int y, int h) {
        int top = Math.max(1, h / 4);
        int bottom = Math.max(top + 1, h - 88);
        int alpha = 225;
        if (y < top + 72) alpha = Math.max(40, (y - top) * 225 / 72);
        if (y > bottom - 72) alpha = Math.min(alpha, Math.max(40, (bottom - y) * 225 / 72));
        return Math.max(40, Math.min(225, alpha));
    }

    private void drawCentered(Graphics2D g, String text, int x, int y) {
        String safe = text == null ? "" : text;
        FontMetrics fm = g.getFontMetrics();
        g.drawString(safe, x - fm.stringWidth(safe) / 2, y);
    }
}
