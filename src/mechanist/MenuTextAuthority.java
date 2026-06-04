package mechanist;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Small UI text bridge for player-facing menu copy.
 *
 * This intentionally stays lightweight: existing Java 17 Swing surfaces can ask
 * for text by stable key while still providing a safe English fallback.  The
 * active local language file lives at packages/client/assets/lang/en_US.properties.
 */
final class MenuTextAuthority {
    private static final String DEFAULT_LANGUAGE = "en_US";
    private static final Properties TEXT = loadText();

    private MenuTextAuthority() {}

    static String text(String key, String fallback) {
        if (key == null || key.isBlank()) return fallback == null ? "" : fallback;
        String value = TEXT.getProperty(key);
        return value == null || value.isBlank() ? (fallback == null ? "" : fallback) : value;
    }

    static void drawMenuReference(Graphics2D g, GamePanel panel, Rectangle bounds, String menuId, String textKey, String fallbackTitle) {
        if (g == null || bounds == null || menuId == null || menuId.isBlank()) return;
        String label = "#" + menuId;
        String title = text(textKey, fallbackTitle == null ? "" : fallbackTitle);
        if (!title.isBlank()) label += " " + title;
        java.awt.Font oldFont = g.getFont();
        java.awt.Color oldColor = g.getColor();
        try {
            float size = panel == null || panel.smallFont == null ? 8f : Math.max(7f, panel.smallFont.getSize2D() - 5f);
            g.setFont((panel == null || panel.smallFont == null ? new Font("Monospaced", Font.PLAIN, 8) : panel.smallFont).deriveFont(Font.PLAIN, size));
            FontMetrics fm = g.getFontMetrics();
            int pad = 5;
            int tw = fm.stringWidth(label);
            int x = bounds.x + bounds.width - tw - 8;
            int y = bounds.y + fm.getAscent() + 5;
            g.setColor(new Color(0, 0, 0, 138));
            g.fillRoundRect(x - pad, y - fm.getAscent() - 2, tw + pad * 2, fm.getHeight() + 3, 7, 7);
            g.setColor(new Color(190, 164, 92, 176));
            g.drawRoundRect(x - pad, y - fm.getAscent() - 2, tw + pad * 2, fm.getHeight() + 3, 7, 7);
            g.setColor(new Color(224, 206, 146, 218));
            g.drawString(label, x, y);
        } finally {
            g.setFont(oldFont);
            g.setColor(oldColor);
        }
    }

    static int drawWrappedBlock(Graphics2D g, GamePanel panel, String key, String fallback, Rectangle box, Color color) {
        if (g == null || box == null) return 0;
        Font oldFont = g.getFont();
        Color oldColor = g.getColor();
        try {
            g.setFont(panel == null || panel.smallFont == null ? new Font("Monospaced", Font.PLAIN, 14) : panel.smallFont);
            FontMetrics fm = g.getFontMetrics();
            List<String> lines = GuiLayoutApi.wrapText(text(key, fallback), fm, Math.max(8, box.width - 14));
            return drawWrappedLines(g, lines, box, color == null ? oldColor : color);
        } finally {
            g.setFont(oldFont);
            g.setColor(oldColor);
        }
    }

    static int drawWrappedLines(Graphics2D g, List<String> lines, Rectangle box, Color color) {
        if (g == null || box == null || lines == null || lines.isEmpty()) return 0;
        FontMetrics fm = g.getFontMetrics();
        int lineH = Math.max(14, fm.getHeight() + 2);
        ArrayList<String> visible = new ArrayList<>();
        int maxLines = Math.max(1, (box.height - 8) / lineH);
        for (String line : lines) {
            if (visible.size() >= maxLines) break;
            visible.add(line == null ? "" : line);
        }
        int usedH = visible.size() * lineH + 8;
        int maxW = 0;
        for (String line : visible) maxW = Math.max(maxW, fm.stringWidth(line));
        int bw = Math.min(box.width, Math.max(12, maxW + 14));
        Color old = g.getColor();
        g.setColor(new Color(0, 0, 0, 108));
        g.fillRoundRect(box.x, box.y, bw, usedH, 8, 8);
        g.setColor(new Color(128, 105, 58, 78));
        g.drawRoundRect(box.x, box.y, bw, usedH, 8, 8);
        g.setColor(color == null ? old : color);
        int y = box.y + fm.getAscent() + 4;
        for (String line : visible) {
            g.drawString(line, box.x + 7, y);
            y += lineH;
        }
        g.setColor(old);
        return usedH;
    }

    private static Properties loadText() {
        Properties out = new Properties();
        String lang = configuredLanguage();
        for (Path path : List.of(
                Path.of("packages/client/assets/lang/" + lang + ".properties"),
                Path.of("assets/lang/" + lang + ".properties"),
                Path.of("packages/client/assets/lang/" + DEFAULT_LANGUAGE + ".properties"),
                Path.of("assets/lang/" + DEFAULT_LANGUAGE + ".properties")
        )) {
            if (!Files.isRegularFile(path)) continue;
            try (FileInputStream in = new FileInputStream(path.toFile())) {
                out.load(in);
                DebugLog.audit("MENU_TEXT", "loaded language file " + path);
                return out;
            } catch (Throwable t) {
                DebugLog.warn("MENU_TEXT", "Could not load language file " + path + ": " + t.getMessage());
            }
        }
        DebugLog.warn("MENU_TEXT", "No menu language file found; using Java fallbacks.");
        return out;
    }

    private static String configuredLanguage() {
        String prop = System.getProperty("mechanist.language");
        if (prop != null && !prop.isBlank()) return prop.trim();
        Path options = Path.of("settings/options.properties");
        if (Files.isRegularFile(options)) {
            Properties p = new Properties();
            try (FileInputStream in = new FileInputStream(options.toFile())) {
                p.load(in);
                String lang = p.getProperty("language", p.getProperty("locale", DEFAULT_LANGUAGE));
                if (lang != null && !lang.isBlank()) return lang.trim();
            } catch (Throwable ignored) {}
        }
        return DEFAULT_LANGUAGE;
    }
}
