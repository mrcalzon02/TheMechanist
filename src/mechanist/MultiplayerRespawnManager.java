package mechanist;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Multiplayer death handling: spectator queue, safe placement lookup, and state restoration without rewinding world state. */
final class MultiplayerRespawnManager {
    enum RespawnState { ALIVE, SPECTATOR_QUEUE, RESPAWNING, RESPAWNED }
    record DeathEvent(String identityKey, double deathX, double deathY, String cause, Instant diedAt) { }
    record RespawnResult(String identityKey, RespawnState state, double x, double y, int health, String reason) { }

    private final SpawnPlacementResolver spawnPlacementResolver;
    private final ScheduledExecutorService scheduler;
    private final Duration respawnDelay;
    private final Map<String, RespawnState> states = new ConcurrentHashMap<>();

    MultiplayerRespawnManager(SpawnPlacementResolver spawnPlacementResolver, ScheduledExecutorService scheduler, Duration respawnDelay) {
        this.spawnPlacementResolver = Objects.requireNonNull(spawnPlacementResolver, "spawnPlacementResolver");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.respawnDelay = respawnDelay == null || respawnDelay.isNegative() ? Duration.ofSeconds(5) : respawnDelay;
    }

    void markAlive(String identityKey) { states.put(clean(identityKey), RespawnState.ALIVE); }

    void onPlayerDeath(DeathEvent event, java.util.function.Consumer<RespawnResult> callback) {
        Objects.requireNonNull(event, "event");
        String key = clean(event.identityKey());
        states.put(key, RespawnState.SPECTATOR_QUEUE);
        scheduler.schedule(() -> {
            try {
                states.put(key, RespawnState.RESPAWNING);
                SpawnPlacementResolver.SpawnResolution spawn = spawnPlacementResolver.resolve(event.deathX(), event.deathY());
                states.put(key, RespawnState.RESPAWNED);
                RespawnResult result = new RespawnResult(key, RespawnState.RESPAWNED, spawn.x(), spawn.y(), 100, "multiplayer respawn near death/logout anchor; " + spawn.reason());
                if (callback != null) callback.accept(result);
            } catch (RuntimeException ex) {
                DebugLog.error("MULTIPLAYER_RESPAWN", "Could not respawn " + key, ex);
            }
        }, respawnDelay.toMillis(), TimeUnit.MILLISECONDS);
    }

    RespawnState stateOf(String identityKey) { return states.getOrDefault(clean(identityKey), RespawnState.ALIVE); }
    private static String clean(String key) { return key == null || key.isBlank() ? "unknown" : key; }
}
