package mechanist;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side world turn-mode authority for strict turns and slow continuous ticking. */
final class WorldTurnManager {
    static final String VERSION = "world-turn-manager-0.9.10gm";
    static final long DEFAULT_SLOW_TICK_RATE_MS = 2_000L;
    static final long MIN_TICK_RATE_MS = 50L;

    enum PlayMode {
        STRICT_TURN_BASED,
        SLOW_CONTINUOUS
    }

    private final Map<String, PlayMode> worldModes = new ConcurrentHashMap<>();
    private final Map<String, Long> worldTickRates = new ConcurrentHashMap<>();
    private final Map<String, Long> lastTickTime = new ConcurrentHashMap<>();

    void setWorldMode(String worldId, PlayMode mode) {
        String id = cleanWorldId(worldId);
        PlayMode use = mode == null ? PlayMode.STRICT_TURN_BASED : mode;
        worldModes.put(id, use);
        if (use == PlayMode.SLOW_CONTINUOUS) {
            worldTickRates.putIfAbsent(id, DEFAULT_SLOW_TICK_RATE_MS);
            lastTickTime.put(id, System.currentTimeMillis());
        }
        DebugLog.audit("WORLD_TURN_MODE", "world=" + id + " mode=" + use + " rateMs=" + tickRateMs(id));
    }

    PlayMode getWorldMode(String worldId) {
        return worldModes.getOrDefault(cleanWorldId(worldId), PlayMode.STRICT_TURN_BASED);
    }

    void setTickRate(String worldId, long rateMs) {
        String id = cleanWorldId(worldId);
        long safe = Math.max(MIN_TICK_RATE_MS, rateMs);
        worldTickRates.put(id, safe);
        DebugLog.audit("WORLD_TICK_RATE", "world=" + id + " rateMs=" + safe);
    }

    long tickRateMs(String worldId) {
        return worldTickRates.getOrDefault(cleanWorldId(worldId), DEFAULT_SLOW_TICK_RATE_MS);
    }

    boolean shouldTickContinuous(String worldId) {
        String id = cleanWorldId(worldId);
        if (getWorldMode(id) != PlayMode.SLOW_CONTINUOUS) return false;
        long now = System.currentTimeMillis();
        long last = lastTickTime.getOrDefault(id, 0L);
        long rate = tickRateMs(id);
        if (now - last >= rate) {
            lastTickTime.put(id, now);
            return true;
        }
        return false;
    }

    String statusLine(String worldId) {
        String id = cleanWorldId(worldId);
        return "authority=" + VERSION + " world=" + id + " mode=" + getWorldMode(id) + " rateMs=" + tickRateMs(id);
    }

    static String auditSummary() {
        return "authority=" + VERSION + " modes=strict-turn-based/slow-continuous defaultRateMs=" + DEFAULT_SLOW_TICK_RATE_MS + " java=17";
    }

    private static String cleanWorldId(String worldId) {
        String id = worldId == null ? "local-world" : worldId.trim();
        return id.isEmpty() ? "local-world" : id;
    }
}
