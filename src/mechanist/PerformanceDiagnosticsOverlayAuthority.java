package mechanist;

import java.awt.*;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Draws the F3-style engine monitor over the final Java2D surface.
 * This is diagnostic UI only; it does not own simulation timing or gameplay truth.
 */
final class PerformanceDiagnosticsOverlayAuthority {
    static final String VERSION = "0.9.10hb";
    private boolean visible;
    private long frameCounter;
    private long lastFpsMillis = System.currentTimeMillis();
    private int framesThisSecond;
    private int fps;
    private double lastPaintMs;
    private double worstPaintMs;
    private long lastGcCount = -1L;

    PerformanceDiagnosticsOverlayAuthority(boolean visible) {
        this.visible = visible;
    }

    boolean visible() { return visible; }

    void setVisible(boolean visible) { this.visible = visible; }

    String toggle() {
        visible = !visible;
        return "Performance diagnostics overlay " + (visible ? "enabled" : "disabled") + ".";
    }

    void recordFrame(long paintNanos) {
        frameCounter++;
        framesThisSecond++;
        lastPaintMs = paintNanos / 1_000_000.0;
        if (lastPaintMs > worstPaintMs) worstPaintMs = lastPaintMs;
        long now = System.currentTimeMillis();
        if (now - lastFpsMillis >= 1000L) {
            fps = framesThisSecond;
            framesThisSecond = 0;
            lastFpsMillis = now;
            lastGcCount = totalGcCollections();
        }
    }

    void draw(Graphics2D g, GamePanel game, int screenW, int screenH) {
        if (!visible || g == null) return;
        Graphics2D gg = (Graphics2D) g.create();
        try {
            DisplayDensityAuthority.applyTinyTextRenderingHints(gg);
            Font f = new Font("Monospaced", Font.BOLD, 12);
            gg.setFont(f);
            FontMetrics fm = gg.getFontMetrics();
            List<String> lines = overlayLines(game);
            int row = Math.max(14, fm.getHeight());
            int w = 0;
            for (String s : lines) w = Math.max(w, fm.stringWidth(s));
            int pad = 8;
            int x = Math.max(8, Math.min(18, screenW / 80));
            int y = Math.max(8, Math.min(18, screenH / 80));
            int boxW = Math.min(screenW - x - 8, w + pad * 2);
            int boxH = Math.min(screenH - y - 8, lines.size() * row + pad * 2);
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.78f));
            gg.setColor(Color.BLACK);
            gg.fillRoundRect(x, y, boxW, boxH, 10, 10);
            gg.setComposite(AlphaComposite.SrcOver);
            gg.setColor(new Color(70, 230, 225));
            gg.drawRoundRect(x, y, boxW, boxH, 10, 10);
            for (int i = 0; i < lines.size(); i++) {
                if (i == 0) gg.setColor(new Color(135, 240, 255));
                else if (lastPaintMs > 24.0 && lines.get(i).startsWith("FPS")) gg.setColor(new Color(255, 95, 75));
                else if (lines.get(i).contains("Gameplay light")) gg.setColor(new Color(190, 235, 155));
                else gg.setColor(new Color(135, 235, 135));
                gg.drawString(lines.get(i), x + pad, y + pad + fm.getAscent() + i * row);
            }
        } finally {
            gg.dispose();
        }
    }

    List<String> overlayLines(GamePanel game) {
        Runtime rt = Runtime.getRuntime();
        long usedMb = (rt.totalMemory() - rt.freeMemory()) / 1024L / 1024L;
        long totalMb = rt.totalMemory() / 1024L / 1024L;
        ArrayList<String> lines = new ArrayList<>();
        lines.add("--- ENGINE MONITOR (F3) ---");
        lines.add("FPS: " + fps + " | Paint: " + fmt(lastPaintMs) + "ms | Worst: " + fmt(worstPaintMs) + "ms");
        lines.add("JVM RAM: " + usedMb + "MB / " + totalMb + "MB | GC Events: " + (lastGcCount < 0 ? totalGcCollections() : lastGcCount));
        if (game != null) {
            lines.add("Render: " + game.renderScaling.profileLabel() + " / " + game.renderScaling.internalWidth() + "x" + game.renderScaling.internalHeight()
                    + " | Target: " + game.options.targetFpsLabel());
            lines.add("Lighting: " + game.options.lightingFxLabel() + " | " + game.visualLighting.auditSummary());
            lines.add("Server lane: " + (game.singlePlayerSectorBridge == null ? "unmounted" : game.singlePlayerSectorBridge.statusLine()));
            lines.add("Screen: " + game.screen + " | Turn: " + game.turn + " | Snapshot/render frame: " + frameCounter);
        }
        lines.add("Pipeline: Java2D Swing CPU surface | OS: " + System.getProperty("os.name", "unknown"));
        lines.add("Gameplay light: turn-stable; flicker/bloom are render-only.");
        return lines;
    }

    String auditSummary() {
        return "performanceDiagnostics version=" + VERSION + " visible=" + visible + " fps=" + fps
                + " lastPaintMs=" + fmt(lastPaintMs) + " worstPaintMs=" + fmt(worstPaintMs)
                + " gcEvents=" + (lastGcCount < 0 ? totalGcCollections() : lastGcCount);
    }

    private static long totalGcCollections() {
        long n = 0L;
        try {
            for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
                long c = bean.getCollectionCount();
                if (c > 0) n += c;
            }
        } catch (Throwable ignored) {}
        return n;
    }

    private static String fmt(double d) { return String.format(Locale.US, "%.2f", d); }
}
