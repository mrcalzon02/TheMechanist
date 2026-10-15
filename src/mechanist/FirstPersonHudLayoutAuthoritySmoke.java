package mechanist;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/** Headless first-person HUD pixel containment at common playable viewport sizes. */
final class FirstPersonHudLayoutAuthoritySmoke {
    private static void check(boolean valid, String why) {
        if (!valid) throw new AssertionError(why);
    }

    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            FontMetrics fm = g.getFontMetrics();
            String longText = "SECTOR " + "Megastructure north primary engineering service access / ".repeat(20);
            for (int width : new int[] {160, 320, 640, 800, 1280, 1920}) {
                for (boolean right : new boolean[] {false, true}) {
                    var strip = FirstPersonHudLayoutAuthority.measure(fm, longText, width, right ? 50 : 16, right);
                    Rectangle bounds = strip.bounds();
                    check(bounds.x >= 0 && bounds.x + bounds.width <= width, "HUD bounds overflow at " + width);
                    check(fm.stringWidth(strip.label()) + 16 <= bounds.width,
                            "HUD text overflows at " + width + ": " + strip.label());
                    check(strip.baseline() >= bounds.y + fm.getAscent()
                                    && strip.baseline() + fm.getDescent() <= bounds.y + bounds.height,
                            "HUD baseline escapes its capsule");
                    check(strip.label().endsWith("…"), "Long HUD status must visibly abbreviate");
                    g.setColor(Color.WHITE);
                    var before = g.getColor();
                    var clip = g.getClip();
                    FirstPersonHudLayoutAuthority.drawStrip(g, longText, width, bounds.y, right, Color.ORANGE);
                    check(g.getColor().equals(before) && g.getClip() == clip, "Painter altered graphics state");
                }
            }
            System.out.println("PASS: HUD labels fit, both alignments, graphics state preserved");
        } finally {
            g.dispose();
        }
    }

    private FirstPersonHudLayoutAuthoritySmoke() { }
}
