package mechanist;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import javax.accessibility.*;
import javax.swing.*;

/**
 * Owns current Java2D accessibility compatibility transforms for custom canvas rendering.
 * These effects are render-side compatibility aids, not gameplay/simulation authority.
 */
final class AccessibilityCompatibilityAuthority {
    static final String VERSION = "0.9.10hb";

    enum CvdMode {
        NORMAL("Normal"),
        PROTANOPIA("Protanopia correction"),
        DEUTERANOPIA("Deuteranopia correction"),
        TRITANOPIA("Tritanopia correction");
        final String label;
        CvdMode(String label) { this.label = label; }
    }

    private static final float[] PROTAN_MATRIX = {
            0.10889f, 0.89111f, 0.00000f,
            0.10889f, 0.89111f, 0.00000f,
            0.00447f, -0.00447f, 1.00000f
    };
    private static final float[] DEUTAN_MATRIX = {
            0.27411f, 0.72589f, 0.00000f,
            0.27411f, 0.72589f, 0.00000f,
            0.01977f, -0.01977f, 1.00000f
    };
    private static final float[] TRITAN_MATRIX = {
            1.01354f, 0.14268f, -0.15622f,
            -0.01181f, 0.87561f, 0.13619f,
            0.07707f, 0.81208f, 0.11085f
    };

    static String cvdLabel(int idx) { return mode(idx).label; }

    static CvdMode mode(int idx) {
        CvdMode[] values = CvdMode.values();
        return values[Math.max(0, Math.min(values.length - 1, idx))];
    }

    static void processCompositedBackbuffer(BufferedImage image, GameOptions options) {
        if (image == null || options == null || options.cvdModeIndex <= 0) return;
        try {
            DataBuffer buffer = image.getRaster().getDataBuffer();
            if (!(buffer instanceof DataBufferInt)) return;
            processPixels(((DataBufferInt)buffer).getData(), mode(options.cvdModeIndex));
        } catch (Throwable t) {
            DebugLog.warn("A11Y_DALTONIZE", "Color compatibility pass skipped: " + t.getMessage());
        }
    }

    static void processPixels(int[] pixels, CvdMode mode) {
        if (pixels == null || mode == null || mode == CvdMode.NORMAL) return;
        float[] m;
        switch (mode) {
            case PROTANOPIA: m = PROTAN_MATRIX; break;
            case DEUTERANOPIA: m = DEUTAN_MATRIX; break;
            case TRITANOPIA: m = TRITAN_MATRIX; break;
            default: return;
        }
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int a = (p >>> 24) & 0xFF;
            int r = (p >>> 16) & 0xFF;
            int g = (p >>> 8) & 0xFF;
            int b = p & 0xFF;
            float rL = r / 255f, gL = g / 255f, bL = b / 255f;
            float simR = (rL * m[0]) + (gL * m[1]) + (bL * m[2]);
            float simG = (rL * m[3]) + (gL * m[4]) + (bL * m[5]);
            float simB = (rL * m[6]) + (gL * m[7]) + (bL * m[8]);
            float errR = rL - simR;
            float errG = gL - simG;
            float shiftR = rL + (errG * 0.7f);
            float shiftG = gL + (errR * 0.7f);
            float shiftB = bL + (errR * 0.5f) + (errG * 0.5f);
            int fr = clamp255(Math.round(shiftR * 255f));
            int fg = clamp255(Math.round(shiftG * 255f));
            int fb = clamp255(Math.round(shiftB * 255f));
            pixels[i] = (a << 24) | (fr << 16) | (fg << 8) | fb;
        }
    }

    static void configurePanelAccessibleContext(JPanel panel, String description) {
        if (panel == null) return;
        try {
            AccessibleContext ctx = panel.getAccessibleContext();
            if (ctx != null) {
                ctx.setAccessibleName(description == null ? "The Mechanist game surface" : description);
                ctx.setAccessibleDescription(description == null ? "Custom tile-game canvas and menu surface." : description);
            }
        } catch (Throwable ignored) {}
    }

    static void pushNarration(JComponent component, String oldText, String newText) {
        if (component == null || newText == null || newText.isBlank()) return;
        try {
            AccessibleContext ctx = component.getAccessibleContext();
            if (ctx != null) {
                ctx.setAccessibleName(newText);
                ctx.setAccessibleDescription(newText);
                ctx.firePropertyChange(AccessibleContext.ACCESSIBLE_NAME_PROPERTY, oldText, newText);
            }
        } catch (Throwable ignored) {}
    }

    static java.util.List<String> optionLines(GameOptions options) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Accessibility compatibility authority " + VERSION + ".");
        lines.add("Color vision mode: " + cvdLabel(options == null ? 0 : options.cvdModeIndex) + ".");
        lines.add("High contrast text containers: " + onOff(options != null && options.highContrastText) + ".");
        lines.add("Instant conversation text: " + onOff(options != null && options.instantDialogueText) + ".");
        lines.add("Screen shake intensity: " + (options == null ? 100 : options.screenShakePercent) + "%.");
        lines.add("Critical states should use dual-channel signaling: color plus icon, border, text, or texture.");
        lines.add("The Daltonization pass transforms the composited Java2D backbuffer and does not change gameplay tile state.");
        lines.add("Custom-canvas accessibility updates are exposed through the Swing AccessibleContext name/description channel.");
        return lines;
    }

    static String auditSummary(GameOptions options) {
        return "a11yCompatibility version=" + VERSION
                + " cvd=" + cvdLabel(options == null ? 0 : options.cvdModeIndex)
                + " highContrastText=" + (options != null && options.highContrastText)
                + " instantDialogueText=" + (options != null && options.instantDialogueText)
                + " screenShake=" + (options == null ? 100 : options.screenShakePercent) + "%"
                + " renderSideDaltonization=true accessibleContext=true";
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }
    private static String onOff(boolean v) { return v ? "ON" : "OFF"; }
}
