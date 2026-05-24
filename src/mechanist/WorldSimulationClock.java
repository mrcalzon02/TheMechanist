package mechanist;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Converts a local input-driven simulation loop into an integrated continuous multiplayer tick without reloading world state. */
final class WorldSimulationClock implements AutoCloseable {
    enum Mode { INPUT_DRIVEN_SINGLE_PLAYER, CONTINUOUS_MULTIPLAYER_TICK, PAUSED, STOPPED }
    interface TickTarget { void tick(long tickId, Duration fixedDelta); }

    private final TickTarget tickTarget;
    private final ScheduledExecutorService scheduler;
    private final ReentrantReadWriteLock transitionLock = new ReentrantReadWriteLock();
    private final AtomicLong tickCounter = new AtomicLong();
    private final AtomicBoolean multiplayerLocked = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> continuousTask;
    private volatile Mode mode = Mode.INPUT_DRIVEN_SINGLE_PLAYER;
    private volatile Duration fixedDelta = Duration.ofMillis(50);

    WorldSimulationClock(TickTarget tickTarget) {
        this.tickTarget = Objects.requireNonNull(tickTarget, "tickTarget");
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "mechanist-world-simulation-clock");
            t.setDaemon(true);
            return t;
        };
        this.scheduler = Executors.newSingleThreadScheduledExecutor(factory);
    }

    ClockTransitionResult processSinglePlayerInputTick() {
        transitionLock.readLock().lock();
        try {
            if (mode != Mode.INPUT_DRIVEN_SINGLE_PLAYER) return new ClockTransitionResult(mode, tickCounter.get(), false, "input tick ignored outside input-driven mode");
            long id = tickCounter.incrementAndGet();
            tickTarget.tick(id, Duration.ZERO);
            return new ClockTransitionResult(mode, id, true, "processed one input-driven turn");
        } finally { transitionLock.readLock().unlock(); }
    }

    ClockTransitionResult openToMultiplayer(int targetHz) {
        transitionLock.writeLock().lock();
        try {
            if (multiplayerLocked.get()) return new ClockTransitionResult(mode, tickCounter.get(), false, "multiplayer session already locked");
            int hz = Math.max(1, Math.min(120, targetHz));
            fixedDelta = Duration.ofNanos(1_000_000_000L / hz);
            cancelContinuousTask();
            mode = Mode.CONTINUOUS_MULTIPLAYER_TICK;
            multiplayerLocked.set(true);
            continuousTask = scheduler.scheduleAtFixedRate(this::safeContinuousTick, 0L, fixedDelta.toNanos(), TimeUnit.NANOSECONDS);
            return new ClockTransitionResult(mode, tickCounter.get(), true, "converted to continuous " + hz + "Hz multiplayer tick without world reload");
        } finally { transitionLock.writeLock().unlock(); }
    }

    ClockTransitionResult pause(String reason) {
        transitionLock.writeLock().lock();
        try {
            cancelContinuousTask();
            mode = Mode.PAUSED;
            return new ClockTransitionResult(mode, tickCounter.get(), true, "paused: " + reason);
        } finally { transitionLock.writeLock().unlock(); }
    }

    ClockTransitionResult resumeContinuous() {
        transitionLock.writeLock().lock();
        try {
            if (!multiplayerLocked.get()) return new ClockTransitionResult(mode, tickCounter.get(), false, "cannot resume continuous mode before multiplayer conversion");
            if (mode == Mode.CONTINUOUS_MULTIPLAYER_TICK) return new ClockTransitionResult(mode, tickCounter.get(), false, "already continuous");
            mode = Mode.CONTINUOUS_MULTIPLAYER_TICK;
            continuousTask = scheduler.scheduleAtFixedRate(this::safeContinuousTick, 0L, fixedDelta.toNanos(), TimeUnit.NANOSECONDS);
            return new ClockTransitionResult(mode, tickCounter.get(), true, "continuous multiplayer tick resumed");
        } finally { transitionLock.writeLock().unlock(); }
    }

    boolean isMultiplayerSessionLocked() { return multiplayerLocked.get(); }
    Mode mode() { return mode; }
    long tickId() { return tickCounter.get(); }

    private void safeContinuousTick() {
        transitionLock.readLock().lock();
        try {
            if (mode != Mode.CONTINUOUS_MULTIPLAYER_TICK) return;
            long id = tickCounter.incrementAndGet();
            tickTarget.tick(id, fixedDelta);
        } catch (Throwable t) {
            DebugLog.error("WORLD_SIMULATION_CLOCK", "Continuous multiplayer tick failed; pausing clock.", t);
            mode = Mode.PAUSED;
        } finally { transitionLock.readLock().unlock(); }
    }

    private void cancelContinuousTask() {
        ScheduledFuture<?> task = continuousTask;
        if (task != null) task.cancel(false);
        continuousTask = null;
    }

    @Override public void close() {
        transitionLock.writeLock().lock();
        try {
            cancelContinuousTask();
            mode = Mode.STOPPED;
            scheduler.shutdownNow();
        } finally { transitionLock.writeLock().unlock(); }
    }
}

record ClockTransitionResult(WorldSimulationClock.Mode mode, long tickId, boolean changed, String message) { }
