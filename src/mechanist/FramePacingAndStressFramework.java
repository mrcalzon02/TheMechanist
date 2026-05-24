package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Frame pacing, rolling telemetry, and deterministic render stress helpers.
 * The live Swing surface still executes on the EDT; this authority gives that
 * path a nanoTime-based pacing gate, an uncapped mode, and a bounded stress
 * harness without moving mutable game state off-thread.
 */
final class FramePacingAndStressFramework {
    private FramePacingAndStressFramework() {}

    static final int DEFAULT_TARGET_FPS = 30;
    static final long NANOS_PER_SECOND = 1_000_000_000L;

    static long frameIntervalNanos(GameOptions options) {
        int fps = options == null ? DEFAULT_TARGET_FPS : options.targetFpsValue();
        if (fps <= 0 || (options != null && !options.isFrameLimited)) return 0L;
        return Math.max(1_000_000L, NANOS_PER_SECOND / Math.max(1, fps));
    }

    static int preferredTimerDelayMillis(GameOptions options) {
        if (options == null) return 1;
        if (!options.isFrameLimited || options.targetFpsValue() <= 0) return 1;
        return Math.max(1, (int)Math.max(1L, frameIntervalNanos(options) / 1_000_000L));
    }
}

record FrameTelemetrySnapshot(double fps, double msPerFrame, double peakFps, double varianceMs,
                              long frameCount, int activeElements, boolean stressActive, boolean limited) {
    String compactLine() {
        return "FPS " + fixed1(fps) + " | " + fixed2(msPerFrame) + " ms | peak " + fixed1(peakFps)
                + " | var " + fixed2(varianceMs) + " | elem " + activeElements + " | "
                + (limited ? "limited" : "uncapped") + (stressActive ? " | STRESS" : "");
    }

    private static String fixed1(double value) {
        long scaled = Math.round(Math.max(-99999.0, Math.min(99999.0, value)) * 10.0);
        return (scaled / 10) + "." + Math.abs(scaled % 10);
    }

    private static String fixed2(double value) {
        long scaled = Math.round(Math.max(-99999.0, Math.min(99999.0, value)) * 100.0);
        long frac = Math.abs(scaled % 100);
        return (scaled / 100) + "." + (frac < 10 ? "0" : "") + frac;
    }
}

final class FrameLimiterEngine {
    private static final int WINDOW = 60;
    private final long[] frameNanos = new long[WINDOW];
    private int cursor;
    private int samples;
    private long frameCount;
    private long nextFrameNanos;
    private long peakFrameNanos = Long.MAX_VALUE;
    private long worstFrameNanos;
    private boolean limited = true;
    private long intervalNanos = FramePacingAndStressFramework.NANOS_PER_SECOND / FramePacingAndStressFramework.DEFAULT_TARGET_FPS;
    private int activeElements;

    void configure(GameOptions options) {
        limited = options == null || (options.isFrameLimited && options.targetFpsValue() > 0);
        intervalNanos = FramePacingAndStressFramework.frameIntervalNanos(options);
        if (!limited || intervalNanos <= 0L) nextFrameNanos = 0L;
    }

    boolean shouldRunPulse(long nowNanos) {
        if (!limited || intervalNanos <= 0L) return true;
        if (nextFrameNanos == 0L) {
            nextFrameNanos = nowNanos + intervalNanos;
            return true;
        }
        if (nowNanos + 500_000L < nextFrameNanos) return false;
        long behind = nowNanos - nextFrameNanos;
        if (behind > intervalNanos * 4L) nextFrameNanos = nowNanos + intervalNanos;
        else nextFrameNanos += intervalNanos;
        return true;
    }

    void recordFrame(long elapsedNanos, int activeElements) {
        long safe = Math.max(1L, elapsedNanos);
        this.activeElements = Math.max(0, activeElements);
        frameNanos[cursor] = safe;
        cursor = (cursor + 1) % WINDOW;
        samples = Math.min(WINDOW, samples + 1);
        frameCount++;
        if (safe < peakFrameNanos) peakFrameNanos = safe;
        if (safe > worstFrameNanos) worstFrameNanos = safe;
    }

    FrameTelemetrySnapshot snapshot(boolean stressActive) {
        if (samples <= 0) return new FrameTelemetrySnapshot(0.0, 0.0, 0.0, 0.0, frameCount, activeElements, stressActive, limited);
        double sum = 0.0;
        for (int i = 0; i < samples; i++) sum += frameNanos[i] / 1_000_000.0;
        double mean = sum / samples;
        double variance = 0.0;
        for (int i = 0; i < samples; i++) {
            double d = frameNanos[i] / 1_000_000.0 - mean;
            variance += d * d;
        }
        variance /= samples;
        double fps = mean <= 0.0001 ? 0.0 : 1000.0 / mean;
        double peak = peakFrameNanos == Long.MAX_VALUE ? 0.0 : 1_000_000_000.0 / Math.max(1L, peakFrameNanos);
        return new FrameTelemetrySnapshot(fps, mean, peak, Math.sqrt(variance), frameCount, activeElements, stressActive, limited);
    }

    void drawOverlay(Graphics2D g, int w, int h, boolean stressActive) {
        if (g == null) return;
        FrameTelemetrySnapshot s = snapshot(stressActive);
        String line = s.compactLine();
        Font oldFont = g.getFont();
        Color oldColor = g.getColor();
        g.setFont(FramePacingStyles.FONT);
        FontMetrics fm = g.getFontMetrics();
        int boxW = Math.min(Math.max(300, fm.stringWidth(line) + 20), Math.max(320, w - 24));
        int x = 12;
        int y = Math.max(12, h - 152);
        g.setColor(FramePacingStyles.BACKGROUND);
        g.fillRoundRect(x, y, boxW, 30, 8, 8);
        g.setColor(stressActive ? FramePacingStyles.STRESS_TEXT : FramePacingStyles.TEXT);
        g.drawString(line, x + 10, y + 20);
        g.setFont(oldFont);
        g.setColor(oldColor);
    }

    String finalReport(boolean stressActive) {
        FrameTelemetrySnapshot s = snapshot(stressActive);
        long worstScaled = Math.round((worstFrameNanos / 1_000_000.0) * 100.0);
        return "Frame telemetry report | " + s.compactLine() + " | frames=" + s.frameCount()
                + " | worstMs=" + (worstScaled / 100) + "." + (Math.abs(worstScaled % 100) < 10 ? "0" : "") + Math.abs(worstScaled % 100);
    }
}

final class FramePacingStyles {
    static final Font FONT = new Font("Monospaced", Font.BOLD, 12);
    static final Color BACKGROUND = new Color(0, 0, 0, 178);
    static final Color TEXT = new Color(180, 238, 190, 236);
    static final Color STRESS_TEXT = new Color(255, 208, 104, 245);
    private FramePacingStyles() {}
}

final class HighPrecisionFramePulseLoop implements AutoCloseable {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Runnable pulse;
    private final String threadName;
    private Thread thread;
    private volatile boolean limited = true;
    private volatile long intervalNanos = FramePacingAndStressFramework.NANOS_PER_SECOND / FramePacingAndStressFramework.DEFAULT_TARGET_FPS;

    HighPrecisionFramePulseLoop(String threadName, Runnable pulse) {
        this.threadName = (threadName == null || threadName.isBlank()) ? "mechanist-frame-pulse" : threadName;
        this.pulse = java.util.Objects.requireNonNull(pulse, "pulse");
    }

    void configure(GameOptions options) {
        this.limited = options == null || (options.isFrameLimited && options.targetFpsValue() > 0);
        this.intervalNanos = FramePacingAndStressFramework.frameIntervalNanos(options);
    }

    void start() {
        if (!running.compareAndSet(false, true)) return;
        thread = new Thread(this::runLoop, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private void runLoop() {
        long next = System.nanoTime();
        while (running.get()) {
            long now = System.nanoTime();
            if (limited && intervalNanos > 0L && now < next) {
                sleepUntil(next, now);
                continue;
            }
            try { pulse.run(); }
            catch (Throwable t) { DebugLog.error("FRAME_PULSE_LOOP", "High precision pulse failed.", t); }
            if (limited && intervalNanos > 0L) {
                next += intervalNanos;
                if (System.nanoTime() - next > intervalNanos * 4L) next = System.nanoTime() + intervalNanos;
            } else {
                next = System.nanoTime();
                Thread.yield();
            }
        }
    }

    private void sleepUntil(long targetNanos, long nowNanos) {
        long remaining = targetNanos - nowNanos;
        if (remaining <= 0L) return;
        long millis = remaining / 1_000_000L;
        int nanos = (int)(remaining % 1_000_000L);
        try {
            if (millis > 0L || nanos > 50_000) Thread.sleep(millis, nanos);
            else Thread.onSpinWait();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }

    @Override public void close() {
        running.set(false);
        Thread t = thread;
        if (t != null) t.interrupt();
    }
}

final class RenderStressTestCoordinator {
    private static final int MAX_SYNTHETIC_SOLIDS = 64;
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final PrimitiveParticleEmitter2D particles = new PrimitiveParticleEmitter2D();
    private final Projectile2DPool projectiles = new Projectile2DPool();
    private final Collidable[] solids = new Collidable[MAX_SYNTHETIC_SOLIDS];
    private final LightSource[] lights = new LightSource[8];
    private final LightmapRenderer2D lightmap = new LightmapRenderer2D();
    private long startedNanos;
    private long lastUpdateNanos;
    private int syntheticRaycasts;
    private int impacts;
    private int peakElements;
    private String finalReport = "Render stress not run.";

    RenderStressTestCoordinator() {
        for (int i = 0; i < solids.length; i++) {
            int x = 40 + (i % 16) * 46;
            int y = 60 + (i / 16) * 58;
            solids[i] = new Collidable(x, y, 28, 28);
        }
        lights[0] = new LightSource(160, 120, 95, 0.7f, Color.WHITE);
        lights[1] = new LightSource(480, 260, 125, 0.8f, Color.WHITE);
    }

    String toggle(FrameLimiterEngine metrics) {
        if (active.compareAndSet(false, true)) {
            startedNanos = System.nanoTime();
            lastUpdateNanos = startedNanos;
            syntheticRaycasts = 0;
            impacts = 0;
            peakElements = 0;
            seedInitialBurst();
            return "Render stress test started: particles, impacts, and synthetic ray intersections active.";
        }
        if (active.compareAndSet(true, false)) {
            finalReport = (metrics == null ? "Render stress stopped." : metrics.finalReport(false))
                    + " | syntheticRaycasts=" + syntheticRaycasts + " | impacts=" + impacts + " | peakElements=" + peakElements;
            System.out.println(finalReport);
            DebugLog.metric("render-stress", finalReport);
            return "Render stress test stopped. " + finalReport;
        }
        return "Render stress state unchanged.";
    }

    boolean active() { return active.get(); }
    String finalReport() { return finalReport; }

    int activeElements() {
        return particles.activeCount() + 128 + syntheticRaycasts / 120;
    }

    void updateAndRender(Graphics2D g, GamePanel panel, int w, int h) {
        if (!active.get() || g == null) return;
        long now = System.nanoTime();
        float dt = Math.max(0.001f, Math.min(0.050f, (now - lastUpdateNanos) / 1_000_000_000.0f));
        lastUpdateNanos = now;
        floodParticles(w, h);
        particles.update(dt);
        impacts += AabbCollisionSystem.updateProjectiles(projectiles, solids, solids.length, particles, dt);
        syntheticRaycasts += syntheticRaycastLoad(panel, w, h);
        particles.render(g);
        renderSyntheticSolids(g);
        lightmap.render(g, w, h, lights, lights.length);
        int activeNow = activeElements();
        if (activeNow > peakElements) peakElements = activeNow;
        drawStressBanner(g, w, h, activeNow);
    }

    private void seedInitialBurst() {
        for (int i = 0; i < 96; i++) {
            projectiles.spawn(80 + (i % 24) * 22, 110 + (i / 24) * 26, 180f + (i % 9) * 16f, -65f + (i % 7) * 21f);
        }
    }

    private void floodParticles(int w, int h) {
        float cx = Math.max(40, w * 0.5f);
        float cy = Math.max(40, h * 0.42f);
        particles.spawnWeaponFire(new WeaponFireProfile.Plasma(30, 260f, 0xFF5AFFF0), cx, cy, 1f, 0.05f);
        particles.spawnWeaponFire(new WeaponFireProfile.ShotgunBlast(36, 1.05f, 320f, 0xFFFFA648), cx, cy + 20, 0.9f, -0.18f);
        if (((System.nanoTime() / 33_000_000L) & 3L) == 0L) {
            particles.spawnWeaponFire(new WeaponFireProfile.ImpactBurst(48, 210f, 0xFFFFE078), cx + 90, cy + 10, 1f, 0f);
        }
    }

    private int syntheticRaycastLoad(GamePanel panel, int w, int h) {
        if (panel == null || panel.world == null) return 0;
        int hits = 0;
        double ox = Math.max(0.5, Math.min(panel.world.w - 1.5, panel.playerX + 0.5));
        double oy = Math.max(0.5, Math.min(panel.world.h - 1.5, panel.playerY + 0.5));
        for (int i = 0; i < 360; i += 3) {
            double a = i * Math.PI / 180.0;
            double dx = Math.cos(a), dy = Math.sin(a);
            double px = ox, py = oy;
            for (int step = 0; step < 28; step++) {
                px += dx * 0.22;
                py += dy * 0.22;
                int tx = (int)px, ty = (int)py;
                if (!panel.world.inBounds(tx, ty) || !panel.world.walkable(tx, ty)) { hits++; break; }
            }
        }
        return hits;
    }

    private void renderSyntheticSolids(Graphics2D g) {
        Color old = g.getColor();
        g.setColor(new Color(170, 80, 52, 170));
        for (Collidable c : solids) g.drawRect(c.x(), c.y(), c.width(), c.height());
        g.setColor(old);
    }

    private void drawStressBanner(Graphics2D g, int w, int h, int activeNow) {
        Font oldFont = g.getFont();
        Color oldColor = g.getColor();
        g.setFont(FramePacingStyles.FONT);
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(12, 48, Math.min(w - 24, 540), 28);
        g.setColor(FramePacingStyles.STRESS_TEXT);
        g.drawString("RENDER STRESS: particles + collision + synthetic raycasts | active=" + activeNow + " rayHits=" + syntheticRaycasts, 22, 67);
        g.setFont(oldFont);
        g.setColor(oldColor);
    }
}
