package mechanist;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Authoritative login/logout lifecycle manager that removes disconnected entities from live simulation. */
final class PlayerSessionManager {
    record LivePlayerEntity(String identityKey, String worldId, double x, double y, double z, double orientationDegrees, List<String> inventory, int health) { }
    record LoginResult(String identityKey, SpawnPlacementResolver.SpawnResolution spawn, LoginProtectionTracker.ProtectionState protectionState) { }

    private final Map<String, LivePlayerEntity> liveEntities = new ConcurrentHashMap<>();
    private final Map<String, PlayerWorldStateRecord> worldDatabase = new ConcurrentHashMap<>();
    private final SpawnPlacementResolver spawnResolver;
    private final LoginProtectionTracker loginProtectionTracker;

    PlayerSessionManager(SpawnPlacementResolver spawnResolver, LoginProtectionTracker loginProtectionTracker) {
        this.spawnResolver = Objects.requireNonNull(spawnResolver, "spawnResolver");
        this.loginProtectionTracker = Objects.requireNonNull(loginProtectionTracker, "loginProtectionTracker");
    }

    LoginResult login(PlayerIdentity identity, String worldId) {
        Objects.requireNonNull(identity, "identity");
        String key = identity.storageKey();
        PlayerWorldStateRecord saved = worldDatabase.getOrDefault(key, new PlayerWorldStateRecord(key, worldId, 0, 0, 0, 0, List.of(), 100, Instant.now()));
        SpawnPlacementResolver.SpawnResolution spawn = spawnResolver.resolve(saved.x(), saved.y());
        LivePlayerEntity entity = new LivePlayerEntity(key, saved.worldId(), spawn.x(), spawn.y(), saved.z(), saved.orientationDegrees(), saved.inventory(), saved.health());
        liveEntities.put(key, entity);
        LoginProtectionTracker.ProtectionState protection = loginProtectionTracker.apply(key);
        return new LoginResult(key, spawn, protection);
    }

    PlayerWorldStateRecord disconnect(PlayerIdentity identity, String reason) {
        Objects.requireNonNull(identity, "identity");
        String key = identity.storageKey();
        LivePlayerEntity entity = liveEntities.remove(key);
        if (entity == null) return worldDatabase.get(key);
        PlayerWorldStateRecord record = new PlayerWorldStateRecord(entity.identityKey(), entity.worldId(), entity.x(), entity.y(), entity.z(), entity.orientationDegrees(), entity.inventory(), entity.health(), Instant.now());
        worldDatabase.put(key, record);
        loginProtectionTracker.revoke(key, "disconnect: " + reason);
        DebugLog.audit("PLAYER_SESSION_DISCONNECT", "identity=" + key + " reason=" + reason + " x=" + record.x() + " y=" + record.y());
        return record;
    }

    void observeAction(PlayerIdentity identity, LoginProtectionTracker.PlayerActionType actionType) {
        loginProtectionTracker.observeInput(identity.storageKey(), actionType);
    }

    boolean isLive(PlayerIdentity identity) { return liveEntities.containsKey(identity.storageKey()); }
    boolean isProtected(PlayerIdentity identity) { return loginProtectionTracker.isProtected(identity.storageKey()); }
    int liveCount() { return liveEntities.size(); }
}
