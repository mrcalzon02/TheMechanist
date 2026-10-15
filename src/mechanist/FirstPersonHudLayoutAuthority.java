package mechanist;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

/** Compact first-person telemetry strips; all text uses measured pixel widths. */
final class FirstPersonHudLayoutAuthority {
    record Strip(Rectangle bounds, String label, int baseline) { }

    private FirstPersonHudLayoutAuthority() { }

    static Strip measure(FontMetrics fm, String label, int viewportWidth, int top, boolean alignRight) {
        int viewport = Math.max(1, viewportWidth);
        int maxWidth = Math.max(1, viewport - 32);
        String shown = GuiLayoutApi.fitLabel(label, fm, Math.max(1, maxWidth - 16));
        int desiredWidth = fm == null ? maxWidth : fm.stringWidth(shown) + 16;
        int boxWidth = Math.min(maxWidth, Math.max(1, desiredWidth));
        int x = alignRight ? Math.max(0, viewport - 16 - boxWidth) : Math.min(16, Math.max(0, viewport - boxWidth));
        Rectangle box = new Rectangle(x, Math.max(0, top), boxWidth, 28);
        int ascent = fm == null ? 12 : fm.getAscent();
        int descent = fm == null ? 3 : fm.getDescent();
        int baseline = box.y + (box.height + ascent - descent) / 2;
        return new Strip(box, shown, baseline);
    }

    static void drawStrip(Graphics2D g, String label, int viewportWidth, int top,
                          boolean alignRight, Color textColor) {
        if (g == null) return;
        Strip strip = measure(g.getFontMetrics(), label, viewportWidth, top, alignRight);
        Color previousColor = g.getColor();
        Shape previousClip = g.getClip();
        try {
            Rectangle box = strip.bounds();
            g.clip(box);
            g.setColor(new Color(0, 0, 0, 172));
            g.fillRoundRect(box.x, box.y, box.width, box.height, 8, 8);
            g.setColor(textColor == null ? previousColor : textColor);
            g.drawString(strip.label(), box.x + 8, strip.baseline());
        } finally {
            g.setClip(previousClip);
            g.setColor(previousColor);
        }
    }
}
