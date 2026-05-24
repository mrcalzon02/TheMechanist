package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Owns the lightweight Java2D render target, aspect-correct presentation rectangle,
 * physical-to-virtual input mapping, and cached CRT scanline layer.
 */
final class RenderScalingCrtAuthority {
    static final String VERSION = "0.9.10fc";
    static final int VIRTUAL_WIDTH = 960;
    static final int VIRTUAL_HEIGHT = 540;

    enum RenderProfile {
        DESKTOP_1080("1080p native", 1920, 1080, 1.0f, true),
        DESKTOP_720("720p native", 1280, 720, 1.0f, true),
        PERF_1080_HALF("1080p window / 50% performance", 1920, 1080, 0.50f, true),
        XFCE_TEST_HALF("Linux XFCE i5 / 50% window", 1280, 720, 0.50f, true);

        final String label;
        final int windowWidth;
        final int windowHeight;
        final float performanceScale;
        final boolean crtEnabled;

        RenderProfile(String label, int windowWidth, int windowHeight, float performanceScale, boolean crtEnabled) {
            this.label = label;
            this.windowWidth = windowWidth;
            this.windowHeight = windowHeight;
            this.performanceScale = performanceScale;
            this.crtEnabled = crtEnabled;
        }
    }

    static final class Layout {
        final int x;
        final int y;
        final int width;
        final int height;

        Layout(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        boolean contains(int px, int py) {
            return px >= x && py >= y && px < x + width && py < y + height;
        }
    }

    private RenderProfile profile = RenderProfile.DESKTOP_720;
    private float activeDownscale = 1.0f;
    private String activeDownscaleLabel = "NATIVE 100%";
    private BufferedImage internalBuffer;
    private BufferedImage crtOverlay;
    private int overlayW = -1;
    private int overlayH = -1;

    RenderProfile profile() { return profile; }
    String profileLabel() { return profile.label; }
    float performanceScale() { return activeDownscale; }
    String downscaleLabel() { return activeDownscaleLabel; }
    int virtualWidth() { return VIRTUAL_WIDTH; }
    int virtualHeight() { return VIRTUAL_HEIGHT; }

    RenderProfile cycleProfile() {
        RenderProfile[] values = RenderProfile.values();
        profile = values[(profile.ordinal() + 1) % values.length];
        setDownscale(profile.performanceScale, profile.label);
        return profile;
    }

    void applyOptions(GameOptions options) {
        if (options == null) return;
        setDownscale(options.downscaleFactor(), options.downscaleLabel());
    }

    void setDownscale(float scale, String label) {
        float clamped = Math.max(0.25f, Math.min(1.0f, scale));
        if (Math.abs(activeDownscale - clamped) > 0.001f) internalBuffer = null;
        activeDownscale = clamped;
        activeDownscaleLabel = (label == null || label.isBlank()) ? Math.round(clamped * 100f) + "%" : label;
        crtOverlay = null;
        overlayW = overlayH = -1;
    }

    int internalWidth() {
        return internalWidth(false);
    }

    int internalHeight() {
        return internalHeight(false);
    }

    int internalWidth(boolean forceReadableUi) {
        if (forceReadableUi) return VIRTUAL_WIDTH;
        return Math.max(320, Math.round(VIRTUAL_WIDTH * Math.max(0.25f, Math.min(1.0f, activeDownscale))));
    }

    int internalHeight(boolean forceReadableUi) {
        if (forceReadableUi) return VIRTUAL_HEIGHT;
        return Math.max(180, Math.round(VIRTUAL_HEIGHT * Math.max(0.25f, Math.min(1.0f, activeDownscale))));
    }

    double bufferScaleX() { return bufferScaleX(false); }
    double bufferScaleY() { return bufferScaleY(false); }
    double bufferScaleX(boolean forceReadableUi) { return internalWidth(forceReadableUi) / (double)VIRTUAL_WIDTH; }
    double bufferScaleY(boolean forceReadableUi) { return internalHeight(forceReadableUi) / (double)VIRTUAL_HEIGHT; }

    BufferedImage ensureInternalBuffer() {
        return ensureInternalBuffer(false);
    }

    BufferedImage ensureInternalBuffer(boolean forceReadableUi) {
        int iw = internalWidth(forceReadableUi);
        int ih = internalHeight(forceReadableUi);
        if (internalBuffer == null || internalBuffer.getWidth() != iw || internalBuffer.getHeight() != ih) {
            internalBuffer = new BufferedImage(iw, ih, BufferedImage.TYPE_INT_ARGB);
        }
        return internalBuffer;
    }

    Layout layoutFor(int physicalW, int physicalH) {
        int w = Math.max(1, physicalW);
        int h = Math.max(1, physicalH);
        double target = VIRTUAL_WIDTH / (double)VIRTUAL_HEIGHT;
        int rw = w;
        int rh = (int)Math.round(w / target);
        if (rh > h) {
            rh = h;
            rw = (int)Math.round(h * target);
        }
        int x = Math.max(0, (w - rw) / 2);
        int y = Math.max(0, (h - rh) / 2);
        return new Layout(x, y, Math.max(1, rw), Math.max(1, rh));
    }

    Point mapPhysicalToVirtual(int px, int py, int physicalW, int physicalH) {
        Layout l = layoutFor(physicalW, physicalH);
        if (!l.contains(px, py)) return null;
        int vx = (int)Math.floor((px - l.x) * (VIRTUAL_WIDTH / (double)l.width));
        int vy = (int)Math.floor((py - l.y) * (VIRTUAL_HEIGHT / (double)l.height));
        vx = Math.max(0, Math.min(VIRTUAL_WIDTH - 1, vx));
        vy = Math.max(0, Math.min(VIRTUAL_HEIGHT - 1, vy));
        return new Point(vx, vy);
    }

    BufferedImage ensureCrtOverlay(int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        if (crtOverlay == null || overlayW != w || overlayH != h) {
            overlayW = w;
            overlayH = h;
            crtOverlay = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = crtOverlay.createGraphics();
            try {
                g.setComposite(AlphaComposite.Src);
                g.setColor(new Color(0, 0, 0, 0));
                g.fillRect(0, 0, w, h);
                g.setComposite(AlphaComposite.SrcOver);
                g.setColor(new Color(0, 0, 0, 7));
                for (int y = 0; y < h; y += 4) g.drawLine(0, y, w, y);
            } finally {
                g.dispose();
            }
        }
        return crtOverlay;
    }

    List<String> infopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Render scaling / CRT authority " + VERSION + ".");
        lines.add("Virtual canvas: " + VIRTUAL_WIDTH + "x" + VIRTUAL_HEIGHT + " 16:9.");
        lines.add("Active profile: " + profile.label + ", downscale " + activeDownscaleLabel + ", internal buffer " + internalWidth() + "x" + internalHeight() + ".");
        lines.add("Presentation: aspect-ratio letterbox/pillarbox; input mapper converts physical mouse coordinates back into virtual coordinates.");
        lines.add("CRT: cached transparent scanline image with reduced alpha/spacing; no per-frame pixel loop; bilinear presentation provides cheap softness on Java2D/Swing.");
        lines.add("Profile hotkey: F10 cycles desktop 1080p, desktop 720p, 1080p half-scale, and Linux XFCE i5 half-scale test profiles.");
        return lines;
    }

    String auditSummary() {
        return "renderScalingCrt version=" + VERSION
                + " virtual=" + VIRTUAL_WIDTH + "x" + VIRTUAL_HEIGHT
                + " profile=\"" + profile.label + "\""
                + " internal=" + internalWidth() + "x" + internalHeight()
                + " scale=" + String.format(Locale.US, "%.2f", activeDownscale)
                + " downscale=\"" + activeDownscaleLabel + "\""
                + " crtCached=true inputMapped=true letterbox=true java2d=true";
    }
}
