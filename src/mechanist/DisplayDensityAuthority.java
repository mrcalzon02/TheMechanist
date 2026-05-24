package mechanist;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Owns desktop display-density invariants for the pure Swing/Java2D client.
 * This authority intentionally separates standard Swing text density from
 * custom game-surface/world density so tiny text is not produced by OS stretch
 * scaling or by ad-hoc component shrinking.
 */
final class DisplayDensityAuthority {
    static final String VERSION = "0.9.10it";
    private static final float MIN_SCALE = 0.50f;
    private static final float MAX_SCALE = 2.00f;
    private static boolean jvmPropertiesApplied = false;
    private static float lastAppliedTextScale = -1.0f;
    private static final Map<Object, Font> baseSwingFonts = new LinkedHashMap<>();

    private DisplayDensityAuthority() {}

    static void configureJvmDisplayPropertiesBeforeSwing() {
        System.setProperty("sun.java2d.uiScale", "1.0");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        jvmPropertiesApplied = true;
    }

    static DisplayDensitySettings settingsFrom(GameOptions options) {
        if (options == null) return new DisplayDensitySettings(1.0f, 1.0f);
        return new DisplayDensitySettings(options.uiScale / 100.0f, options.fontScale / 100.0f);
    }

    static void applyGlobalSwingTextScale(GameOptions options) {
        DisplayDensitySettings settings = settingsFrom(options);
        float textScale = settings.textScale();
        if (Math.abs(lastAppliedTextScale - textScale) < 0.001f) return;
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();
        if (baseSwingFonts.isEmpty()) {
            for (Enumeration<Object> keys = defaults.keys(); keys.hasMoreElements();) {
                Object key = keys.nextElement();
                Object value = defaults.get(key);
                if (value instanceof Font font) baseSwingFonts.put(key, font);
            }
        }
        for (Map.Entry<Object, Font> entry : baseSwingFonts.entrySet()) {
            Font font = entry.getValue();
            float size = Math.max(8.0f, font.getSize2D() * textScale);
            defaults.put(entry.getKey(), new FontUIResource(font.deriveFont(size)));
        }
        lastAppliedTextScale = textScale;
        DebugLog.audit("DISPLAY_DENSITY", auditSummary(options));
    }

    static void refreshSwingTree(Component root) {
        if (root == null) return;
        SwingUtilities.updateComponentTreeUI(root);
        root.invalidate();
        root.validate();
        root.repaint();
    }

    static void applyTinyTextRenderingHints(Graphics2D g) {
        applyTextRenderingHints(g, null);
    }

    static void applyTextRenderingHints(Graphics2D g, GameOptions options) {
        if (g == null) return;
        int quality = options == null ? 2 : Math.max(0, Math.min(GameOptions.RENDER_QUALITY_LABELS.length - 1, options.renderQualityIndex));
        if (quality == 0) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        } else if (quality == 1) {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
        } else {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, quality == 2 ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
    }

    static float clampScale(float scale) {
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
    }

    static List<String> infopediaLines(GameOptions options) {
        DisplayDensitySettings settings = settingsFrom(options);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Display density authority " + VERSION + ".");
        lines.add("JVM native pixel canvas: sun.java2d.uiScale=1.0; Swing/AWT text anti-aliasing requested before GUI startup.");
        lines.add("Text scale: " + pct(settings.textScale()) + " for standard Swing fonts and game text fonts; compact is the development default and larger sizes are opt-in.");
        lines.add("GUI/world scale: " + pct(settings.guiScale()) + " for custom panel density and world-view zoom-out math.");
        lines.add("Text crispness: render-quality option selects performance, balanced GASP, or LCD crisp text; crisp mode uses LCD subpixel text with fractional metrics on.");
        lines.add("Rule: text density and world/gui density remain separate so fitting more information does not rely on blurry OS stretch scaling.");
        return lines;
    }

    static String auditSummary(GameOptions options) {
        DisplayDensitySettings settings = settingsFrom(options);
        return "displayDensity version=" + VERSION
                + " jvmPropertiesApplied=" + jvmPropertiesApplied
                + " uiScale=" + pct(settings.guiScale())
                + " textScale=" + pct(settings.textScale())
                + " java2dUiScale=" + System.getProperty("sun.java2d.uiScale", "unset")
                + " awtAA=" + System.getProperty("awt.useSystemAAFontSettings", "unset")
                + " swingAA=" + System.getProperty("swing.aatext", "unset")
                + " tinyTextHints=" + (options == null ? "CRISP_TEXT" : options.renderQualityLabel()) + "+fractionalMetrics=" + ((options == null || options.renderQualityIndex == 2) ? "ON" : "OFF") + " textHints=" + (options == null ? "CRISP_TEXT" : options.renderQualityLabel());
    }

    private static String pct(float scale) {
        return String.format(Locale.US, "%.0f%%", clampScale(scale) * 100.0f);
    }
}

record DisplayDensitySettings(float guiScale, float textScale) {
    DisplayDensitySettings {
        guiScale = DisplayDensityAuthority.clampScale(guiScale);
        textScale = DisplayDensityAuthority.clampScale(textScale);
    }
}
