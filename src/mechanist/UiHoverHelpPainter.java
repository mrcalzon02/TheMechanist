package mechanist;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;

/** Draws compact hover guidance within the visible game viewport. */
final class UiHoverHelpPainter {
    private static final int EDGE = 8;
    private static final int ANCHOR_OFFSET = 18;
    private static final int PAD_X = 11;
    private static final int PAD_Y = 7;
    private static final int MAX_WIDTH = 340;

    private UiHoverHelpPainter() {}

    static final class HoverLayout {
        final Rectangle bounds;
        final List<String> lines;
        final int lineHeight;

        HoverLayout(Rectangle bounds, List<String> lines, int lineHeight) {
            this.bounds = bounds;
            this.lines = lines;
            this.lineHeight = lineHeight;
        }
    }

    static HoverLayout layout(String help, FontMetrics fm, int viewportWidth, int viewportHeight,
                              int anchorX, int anchorY) {
        if (help == null || help.isBlank() || fm == null) return null;
        int maxBoxWidth = Math.min(MAX_WIDTH, viewportWidth - EDGE * 2);
        int textWidth = maxBoxWidth - PAD_X * 2;
        int lineHeight = Math.max(1, fm.getHeight());
        int maxLines = (viewportHeight - EDGE * 2 - PAD_Y * 2) / lineHeight;
        if (textWidth < fm.charWidth('W') || maxLines < 1) return null;

        List<String> wrapped = GuiLayoutApi.wrapText(help, fm, textWidth);
        int visibleCount = Math.min(maxLines, wrapped.size());
        ArrayList<String> visible = new ArrayList<>(wrapped.subList(0, visibleCount));
        if (wrapped.size() > visibleCount) {
            // A hover popup is not scrollable: show when content is longer than the available viewport.
            if (visibleCount == 1) {
                visible.set(0, GuiLayoutApi.fitLabel(visible.get(0) + "…", fm, textWidth));
            } else {
                int undisplayed = wrapped.size() - (visibleCount - 1);
                visible.set(visibleCount - 1,
                        GuiLayoutApi.fitLabel("… " + undisplayed + " more lines", fm, textWidth));
            }
        }

        int naturalTextWidth = 0;
        for (String line : visible) naturalTextWidth = Math.max(naturalTextWidth, fm.stringWidth(line));
        int boxWidth = Math.min(maxBoxWidth, Math.max(64, naturalTextWidth + PAD_X * 2));
        int boxHeight = PAD_Y * 2 + visible.size() * lineHeight;
        int maxX = viewportWidth - EDGE - boxWidth;
        int maxY = viewportHeight - EDGE - boxHeight;
        int x = anchorX + ANCHOR_OFFSET;
        int y = anchorY + ANCHOR_OFFSET;
        if (x > maxX) x = anchorX - boxWidth - ANCHOR_OFFSET;
        if (y > maxY) y = anchorY - boxHeight - ANCHOR_OFFSET;
        x = Math.max(EDGE, Math.min(maxX, x));
        y = Math.max(EDGE, Math.min(maxY, y));
        return new HoverLayout(new Rectangle(x, y, boxWidth, boxHeight), visible, lineHeight);
    }

    static void drawHoverHelp(GamePanel panel, Graphics2D g, int hoverButtonIndex) {
        if (panel == null || g == null || !panel.options.hoverHelp) return;
        boolean mouseVisible = panel.mouseX >= 0 && panel.mouseX < panel.getWidth()
                && panel.mouseY >= 0 && panel.mouseY < panel.getHeight();
        ButtonBox target = null;
        if (mouseVisible && hoverButtonIndex >= 0 && hoverButtonIndex < panel.buttons.size()) {
            target = panel.buttons.get(hoverButtonIndex);
        } else if (!mouseVisible && panel.selectedButton >= 0 && panel.selectedButton < panel.buttons.size()) {
            ButtonBox selected = panel.buttons.get(panel.selectedButton);
            if (panel.buttonIsModalInteractive(selected)) target = selected;
        }
        if (target == null || target.tip == null || target.tip.isBlank()) return;
        int anchorX = mouseVisible ? panel.mouseX : target.r.x + target.r.width / 2;
        int anchorY = mouseVisible ? panel.mouseY : target.r.y + target.r.height / 2;
        Font previousFont = g.getFont();
        Color previousColor = g.getColor();
        Shape previousClip = g.getClip();
        try {
            g.setFont(panel.smallFont);
            FontMetrics fm = g.getFontMetrics();
            HoverLayout placement = layout(target.tip, fm, panel.getWidth(), panel.getHeight(), anchorX, anchorY);
            if (placement == null) return;
            Rectangle r = placement.bounds;
            g.clipRect(r.x, r.y, r.width, r.height);
            g.setColor(new Color(8, 8, 9, 238));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(new Color(165, 138, 82));
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            g.setColor(new Color(220, 205, 160));
            int ty = r.y + PAD_Y + fm.getAscent();
            for (String line : placement.lines) {
                UiTextSurfacePainter.drawUiTextLine(g, line, r.x + PAD_X, ty);
                ty += placement.lineHeight;
            }
        } finally {
            g.setClip(previousClip);
            g.setColor(previousColor);
            g.setFont(previousFont);
        }
    }
}
