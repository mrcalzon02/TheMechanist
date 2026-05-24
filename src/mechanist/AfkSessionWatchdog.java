package mechanist;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Per-session AFK watchdog with cryptographically randomized kick windows. */
final class AfkSessionWatchdog implements AutoCloseable {
    interface DisconnectAction { void disconnect(String sessionId, String reason); }

    record TimeoutPlan(int baseMinutes, int jitterMinutes, int jitterSign, Duration threshold) { }

    private final String sessionId;
    private final ScheduledExecutorService scheduler;
    private final DisconnectAction disconnectAction;
    private final TimeoutPlan timeoutPlan;
    private final AtomicLong lastGenuineInputMillis = new AtomicLong(System.currentTimeMillis());
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final ScheduledFuture<?> future;

    AfkSessionWatchdog(String sessionId, ScheduledExecutorService scheduler, DisconnectAction disconnectAction) {
        this(sessionId, scheduler, disconnectAction, new SecureRandom());
    }

    AfkSessionWatchdog(String sessionId, ScheduledExecutorService scheduler, DisconnectAction disconnectAction, SecureRandom secureRandom) {
        this.sessionId = sessionId == null || sessionId.isBlank() ? "unknown" : sessionId;
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.disconnectAction = Objects.requireNonNull(disconnectAction, "disconnectAction");
        this.timeoutPlan = createPlan(Objects.requireNonNull(secureRandom, "secureRandom"));
        this.future = scheduler.scheduleAtFixedRate(this::check, 5L, 5L, TimeUnit.SECONDS);
    }

    TimeoutPlan timeoutPlan() { return timeoutPlan; }
    Instant lastGenuineInputAt() { return Instant.ofEpochMilli(lastGenuineInputMillis.get()); }

    void recordGenuineInteraction() { lastGenuineInputMillis.set(System.currentTimeMillis()); }

    private void check() {
        if (closed.get()) return;
        long idleMillis = System.currentTimeMillis() - lastGenuineInputMillis.get();
        if (idleMillis >= timeoutPlan.threshold().toMillis()) {
            close();
            disconnectAction.disconnect(sessionId, "AFK timeout after randomized threshold " + timeoutPlan.threshold().toMinutes() + " minutes");
        }
    }

    static TimeoutPlan createPlan(SecureRandom secureRandom) {
        int base = 15 + secureRandom.nextInt(16);
        int jitter = secureRandom.nextInt(6);
        int sign = secureRandom.nextBoolean() ? 1 : -1;
        int finalMinutes = Math.max(15, Math.min(35, base + sign * jitter));
        return new TimeoutPlan(base, jitter, sign, Duration.ofMinutes(finalMinutes));
    }

    @Override public void close() {
        if (!closed.getAndSet(true)) future.cancel(false);
    }
}
