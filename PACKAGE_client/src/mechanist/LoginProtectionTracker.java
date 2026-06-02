package mechanist;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** Applies and revokes authoritative 60-second login invulnerability windows. */
final class LoginProtectionTracker implements AutoCloseable {
    enum PlayerActionType { MOVE, FIRE_WEAPON, INVENTORY_USE, COMBAT_SKILL, LOOK, CHAT, UI_NAVIGATION, IDLE }
    record ProtectionState(String identityKey, Instant startedAt, Instant expiresAt, boolean active) { }

    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> expirations = new ConcurrentHashMap<>();
    private final Map<String, ProtectionState> states = new ConcurrentHashMap<>();
    private final Duration window;

    LoginProtectionTracker(ScheduledExecutorService scheduler) { this(scheduler, Duration.ofSeconds(60)); }

    LoginProtectionTracker(ScheduledExecutorService scheduler, Duration window) {
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.window = window == null || window.isNegative() || window.isZero() ? Duration.ofSeconds(60) : window;
    }

    ProtectionState apply(String identityKey) {
        String key = clean(identityKey);
        Instant now = Instant.now();
        ProtectionState state = new ProtectionState(key, now, now.plus(window), true);
        states.put(key, state);
        ScheduledFuture<?> old = expirations.remove(key);
        if (old != null) old.cancel(false);
        expirations.put(key, scheduler.schedule(() -> expire(key), window.toMillis(), TimeUnit.MILLISECONDS));
        return state;
    }

    boolean isProtected(String identityKey) {
        ProtectionState state = states.get(clean(identityKey));
        return state != null && state.active() && Instant.now().isBefore(state.expiresAt());
    }

    void observeInput(String identityKey, PlayerActionType actionType) {
        if (actionType == null) return;
        boolean breaks = switch (actionType) {
            case MOVE, FIRE_WEAPON, INVENTORY_USE, COMBAT_SKILL -> true;
            case LOOK, CHAT, UI_NAVIGATION, IDLE -> false;
        };
        if (breaks) revoke(identityKey, "player took active gameplay action: " + actionType);
    }

    void revoke(String identityKey, String reason) {
        String key = clean(identityKey);
        ScheduledFuture<?> future = expirations.remove(key);
        if (future != null) future.cancel(false);
        ProtectionState state = states.get(key);
        if (state != null && state.active()) {
            states.put(key, new ProtectionState(key, state.startedAt(), Instant.now(), false));
            DebugLog.audit("LOGIN_PROTECTION", "revoked identity=" + key + " reason=" + reason);
        }
    }

    private void expire(String key) {
        ProtectionState state = states.get(key);
        if (state != null && state.active()) states.put(key, new ProtectionState(key, state.startedAt(), Instant.now(), false));
        expirations.remove(key);
    }

    private static String clean(String identityKey) { return identityKey == null || identityKey.isBlank() ? "unknown" : identityKey; }

    @Override public void close() {
        for (ScheduledFuture<?> future : expirations.values()) future.cancel(false);
        expirations.clear();
        states.clear();
    }
}
