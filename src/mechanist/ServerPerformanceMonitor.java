package mechanist;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Tracks simulation tick health and gates background I/O during graceful degradation. */
final class ServerPerformanceMonitor implements AutoCloseable {
    static final long TARGET_TICK_NANOS = 16_666_667L;
    static final long DEGRADED_TICK_NANOS = 28_000_000L;

    private final BackgroundIoGate backgroundIoGate;
    private final ScheduledExecutorService monitorExecutor;
    private final AtomicLong lastTickDurationNanos = new AtomicLong(TARGET_TICK_NANOS);
    private final AtomicLong packetQueueDepth = new AtomicLong();
    private final AtomicBoolean degraded = new AtomicBoolean(false);

    ServerPerformanceMonitor(BackgroundIoGate backgroundIoGate) {
        this.backgroundIoGate = Objects.requireNonNull(backgroundIoGate, "backgroundIoGate");
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "mechanist-performance-monitor");
            t.setDaemon(true);
            return t;
        };
        this.monitorExecutor = Executors.newSingleThreadScheduledExecutor(factory);
    }

    void start() {
        monitorExecutor.scheduleAtFixedRate(this::sample, 250L, 250L, TimeUnit.MILLISECONDS);
    }

    void recordTickDuration(long nanos) { lastTickDurationNanos.set(Math.max(0L, nanos)); }
    void recordPacketQueueDepth(long depth) { packetQueueDepth.set(Math.max(0L, depth)); }
    boolean degraded() { return degraded.get(); }

    private void sample() {
        try {
            boolean shouldDegrade = lastTickDurationNanos.get() > DEGRADED_TICK_NANOS || packetQueueDepth.get() > 240L;
            boolean changed = degraded.getAndSet(shouldDegrade) != shouldDegrade;
            if (shouldDegrade) backgroundIoGate.pause("simulation tick preservation"); else backgroundIoGate.resume();
            if (changed) DebugLog.audit("SERVER_DEGRADATION", "degraded=" + shouldDegrade + " tickNanos=" + lastTickDurationNanos.get() + " queueDepth=" + packetQueueDepth.get());
        } catch (RuntimeException ex) {
            DebugLog.error("SERVER_PERFORMANCE_MONITOR", "Monitor loop failed without stopping server.", ex);
        }
    }

    @Override public void close() { monitorExecutor.shutdownNow(); }

    static final class BackgroundIoGate {
        private final AtomicBoolean paused = new AtomicBoolean(false);
        private volatile String reason = "not paused";

        boolean paused() { return paused.get(); }
        String reason() { return reason; }

        void pause(String reason) {
            this.reason = reason == null || reason.isBlank() ? "server degraded" : reason;
            paused.set(true);
        }

        void resume() {
            reason = "not paused";
            paused.set(false);
        }

        void awaitIfPaused() throws InterruptedException {
            while (paused.get()) Thread.sleep(Duration.ofMillis(25).toMillis());
        }
    }
}
