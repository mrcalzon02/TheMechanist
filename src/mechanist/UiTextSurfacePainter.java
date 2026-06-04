package mechanist;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.List;

final class UiTextSurfacePainter {
    private UiTextSurfacePainter() {}

    static void drawUiTextLine(Graphics2D g, String text, int x, int y) {
        if (g == null || text == null) return;
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(text);
        int th = fm.getHeight();
        Color old = g.getColor();
        g.setColor(new Color(0, 0, 0, 196));
        g.fillRoundRect(x - 5, y - fm.getAscent() - 3, tw + 10, th + 5, 7, 7);
        g.setColor(new Color(128, 105, 58, 135));
        g.drawRoundRect(x - 5, y - fm.getAscent() - 3, tw + 10, th + 5, 7, 7);
        g.setColor(contrastRatio(old, Color.BLACK) < 3.0 ? new Color(224, 226, 208) : old);
        g.drawString(text, x, y);
    }

    private static double contrastRatio(Color a, Color b) {
        double la = luminance(a) + 0.05;
        double lb = luminance(b) + 0.05;
        return Math.max(la, lb) / Math.min(la, lb);
    }

    private static double luminance(Color color) {
        if (color == null) return 0.0;
        return channel(color.getRed()) * 0.2126 + channel(color.getGreen()) * 0.7152 + channel(color.getBlue()) * 0.0722;
    }

    private static double channel(int value) {
        double c = Math.max(0, Math.min(255, value)) / 255.0;
        return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    static void center(Graphics2D g, String text, int x, int y) {
        if (text == null) return;
        FontMetrics fm = g.getFontMetrics();
        drawUiTextLine(g, text, x - fm.stringWidth(text) / 2, y);
    }

    static void drawClippedWrappedLines(GamePanel panel, Graphics2D g, List<String> lines, int x, int y, int w, int h, Font font, boolean centered) {
        if (lines == null || w <= 4 || h <= 4) return;
        Font oldFont = g.getFont();
        Shape oldClip = g.getClip();
        g.setFont(font == null ? panel.smallFont : font);
        FontMetrics fm = g.getFontMetrics();
        int lineH = Math.max(12, fm.getHeight() + 1);
        g.setClip(x, y, Math.max(1, w), Math.max(1, h));
        int yy = y + fm.getAscent();
        for (String line : TextLayoutAuthority.wrapAllPixels(g, g.getFont(), lines, Math.max(8, w - 4))) {
            if (yy > y + h) {
                g.setClip(oldClip);
                g.setFont(oldFont);
                return;
            }
            g.setColor(LayerF.optionColor(panel, GameOptions.TEXT_MAIN));
            if (centered) g.drawString(line, x + (w - fm.stringWidth(line)) / 2, yy);
            else g.drawString(line, x, yy);
            yy += lineH;
        }
        g.setClip(oldClip);
        g.setFont(oldFont);
    }

    static void drawFrameWrappedCenteredText(GamePanel panel, Graphics2D g, String text, int x, int y, int w, int h, Font font) {
        if (text == null) text = "";
        Font oldFont = g.getFont();
        g.setFont(font == null ? panel.uiFont : font);
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = TextLayoutAuthority.wrapPixels(g, g.getFont(), text, Math.max(8, w - 8));
        int lineH = Math.max(fm.getHeight() + 6, 22);
        int totalH = lines.size() * lineH;
        int yy = y + Math.max(fm.getAscent(), (h - totalH) / 2 + fm.getAscent());
        for (String line : lines) {
            if (yy > y + h) break;
            g.setColor(LayerF.optionColor(panel, GameOptions.TEXT_MAIN));
            center(g, line, x + w / 2, yy);
            yy += lineH;
        }
        g.setFont(oldFont);
    }

    static void drawTextPanel(GamePanel panel, Graphics2D g, int x, int y, int w, int h, List<String> lines, boolean centered) {
        if (w <= 4 || h <= 4) return;
        g.setColor(new Color(0, 0, 0, 224));
        g.fillRoundRect(x, y, w, h, 12, 12);
        panel.drawSlicedFrame(g, x, y, w, h, "inner");
        panel.stampUiFrameId(g, "T", lines == null || lines.isEmpty() ? "text-panel" : lines.get(0), x, y, w, h);
        Shape oldClip = g.getClip();
        g.setClip(x + 8, y + 8, Math.max(1, w - 16), Math.max(1, h - 16));
        g.setFont(panel.smallFont);
        FontMetrics fm = g.getFontMetrics();
        int yy = y + 22;
        for (String line : TextLayoutAuthority.wrapAllPixels(g, g.getFont(), lines, Math.max(8, w - 32))) {
            if (yy > y + h - 8) {
                g.setClip(oldClip);
                return;
            }
            g.setColor(LayerF.optionColor(panel, GameOptions.TEXT_MAIN));
            if (centered) g.drawString(line, x + (w - fm.stringWidth(line)) / 2, yy);
            else g.drawString(line, x + 14, yy);
            yy += fm.getHeight();
        }
        g.setClip(oldClip);
    }
}
