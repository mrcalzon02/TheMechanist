package mechanist;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** Headless geometry regression for player-facing hover copy. */
final class UiHoverHelpPainterSmoke {
    private UiHoverHelpPainterSmoke() {}

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Graphics2D g = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB).createGraphics();
        try {
            g.setFont(new Font("Dialog", Font.PLAIN, 14));
            FontMetrics fm = g.getFontMetrics();
            String longText = "The main menu contains options for saving, loading, diagnostics, and managing "
                    + "world state; long labels must never silently overflow the visible popup. ".repeat(50);
            for (int[] viewport : new int[][] {{800, 600}, {320, 200}, {120, 85}, {76, 54}}) {
                int width = viewport[0], height = viewport[1];
                for (int[] cursor : new int[][] {{0, 0}, {width - 1, height - 1}, {width / 2, height / 2}}) {
                    UiHoverHelpPainter.HoverLayout layout = UiHoverHelpPainter.layout(
                            longText, fm, width, height, cursor[0], cursor[1]);
                    check(layout != null, "No layout at " + width + "x" + height);
                    Rectangle box = layout.bounds;
                    check(box.x >= 8 && box.y >= 8 && box.x + box.width <= width - 8
                                    && box.y + box.height <= height - 8,
                            "Tooltip escaped viewport: " + box + " in " + width + "x" + height);
                    check(layout.lines.size() * layout.lineHeight + 14 == box.height,
                            "Tooltip rows escaped its box");
                    for (String line : layout.lines) {
                        check(fm.stringWidth(line) + 22 <= box.width,
                                "Tooltip text escaped its box: " + line);
                    }
                    check(layout.lines.get(layout.lines.size() - 1).contains("…"),
                            "Truncated tooltip lacks continuation marker");
                }
            }
            UiHoverHelpPainter.HoverLayout shortHelp = UiHoverHelpPainter.layout(
                    "Open", fm, 800, 600, 790, 590);
            check(shortHelp != null && shortHelp.bounds.width < 190,
                    "Short tooltip should remain compact");
            check(shortHelp.lines.equals(java.util.List.of("Open")), "Short text changed");
            check(UiHoverHelpPainter.layout("", fm, 320, 200, 0, 0) == null,
                    "Empty help should be suppressed");
            check(UiHoverHelpPainter.layout("help", fm, 20, 20, 0, 0) == null,
                    "Too-small view should suppress tooltip instead of drawing outside");
            System.out.println("PASS: tooltip pixel bounds, long copy, compact short copy, and edge anchors");
        } finally {
            g.dispose();
        }
    }
}
