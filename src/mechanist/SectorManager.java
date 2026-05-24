package mechanist;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Server-authoritative sector simulation boundary.
 *
 * This class is intentionally independent of Swing/client presentation.  It is the launcher/server
 * seam for future multiplayer work: sectors with players run full-detail authoritative ticks, while
 * empty sectors shed heavy entities into a pool and continue only lightweight background ticks.
 */
final class SectorManager implements AutoCloseable {
    static final String VERSION = "sector-manager-authoritative-0.9.10gj";

    private final Map<SectorKey, SectorRuntime> sectors = new ConcurrentHashMap<>();
    private final Map<String, SectorKey> playerSectorIndex = new ConcurrentHashMap<>();
    private final ThreadPoolExecutor sectorTickPool;
    private final ScheduledThreadPoolExecutor scheduler;
    private final SectorEntityPool entityPool;
    private final SectorNetworkGateway networkGateway;
    private final long fullTickMillis;
    private final long emptyTickMillis;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    SectorManager() {
        this(Runtime.getRuntime().availableProcessors(), Duration.ofMillis(100), Duration.ofSeconds(2),
                new SectorEntityPool(4096), new InMemorySectorNetworkGateway());
    }

    SectorManager(int workerThreads,
                  Duration fullTickPeriod,
                  Duration emptyTickPeriod,
                  SectorEntityPool entityPool,
                  SectorNetworkGateway networkGateway) {
        int workers = Math.max(1, Math.min(16, workerThreads <= 0 ? 1 : workerThreads));
        this.fullTickMillis = Math.max(16L, nullSafeDurationMillis(fullTickPeriod, 100L));
        this.emptyTickMillis = Math.max(this.fullTickMillis, nullSafeDurationMillis(emptyTickPeriod, 2_000L));
        this.entityPool = entityPool == null ? new SectorEntityPool(4096) : entityPool;
        this.networkGateway = networkGateway == null ? new InMemorySectorNetworkGateway() : networkGateway;
        this.sectorTickPool = new ThreadPoolExecutor(
                workers,
                workers,
                30L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                namedThreadFactory("mechanist-sector-tick-"));
        this.sectorTickPool.allowCoreThreadTimeOut(true);
        this.scheduler = new ScheduledThreadPoolExecutor(Math.max(1, Math.min(4, workers)), namedThreadFactory("mechanist-sector-schedule-"));
        this.scheduler.setRemoveOnCancelPolicy(true);
    }

    SectorSnapshot ensureSector(SectorKey key) {
        requireOpen();
        return runtimeFor(key).snapshot();
    }

    SectorSnapshot playerEnteredSector(String playerId, SectorKey newSector) {
        requireOpen();
        PlayerHandle player = PlayerHandle.of(playerId);
        SectorKey target = Objects.requireNonNull(newSector, "newSector");
        SectorKey prior = playerSectorIndex.put(player.playerId(), target);
        if (prior != null && !prior.equals(target)) {
            SectorRuntime oldRuntime = sectors.get(prior);
            if (oldRuntime != null) oldRuntime.removePlayer(player.playerId());
            networkGateway.unbindPlayerFromSector(player, prior);
        }
        SectorRuntime runtime = runtimeFor(target);
        runtime.addPlayer(player);
        networkGateway.bindPlayerToSector(player, target);
        DebugLog.audit("SECTOR_PLAYER_ENTER", "player=" + player.playerId() + " sector=" + target.compact() + " " + runtime.snapshot().compact());
        return runtime.snapshot();
    }

    SectorSnapshot playerLeftCurrentSector(String playerId) {
        requireOpen();
        PlayerHandle player = PlayerHandle.of(playerId);
        SectorKey prior = playerSectorIndex.remove(player.playerId());
        if (prior == null) return null;
        networkGateway.unbindPlayerFromSector(player, prior);
        SectorRuntime runtime = sectors.get(prior);
        if (runtime == null) return null;
        runtime.removePlayer(player.playerId());
        DebugLog.audit("SECTOR_PLAYER_LEAVE", "player=" + player.playerId() + " sector=" + prior.compact() + " " + runtime.snapshot().compact());
        return runtime.snapshot();
    }

    SectorSnapshot movePlayer(String playerId, SectorKey target) {
        return playerEnteredSector(playerId, target);
    }

    SectorSnapshot runLocalAuthoritativeTurn(String playerId, SectorKey sector, String reason, Runnable localTurnBody) {
        requireOpen();
        PlayerHandle player = PlayerHandle.of(playerId);
        SectorKey target = Objects.requireNonNull(sector, "sector");
        SectorKey current = playerSectorIndex.get(player.playerId());
        if (!target.equals(current)) {
            playerEnteredSector(player.playerId(), target);
        }
        SectorRuntime runtime = runtimeFor(target);
        runtime.runLocalAuthoritativeTurn(reason, localTurnBody);
        return runtime.snapshot();
    }

    SectorSnapshot snapshot(SectorKey key) {
        SectorRuntime runtime = sectors.get(key);
        return runtime == null ? null : runtime.snapshot();
    }

    List<SectorSnapshot> snapshots() {
        ArrayList<SectorSnapshot> out = new ArrayList<>();
        for (SectorRuntime runtime : sectors.values()) out.add(runtime.snapshot());
        return Collections.unmodifiableList(out);
    }

    SectorNetworkGateway networkGateway() { return networkGateway; }
    SectorEntityPool entityPool() { return entityPool; }

    String statusLine() {
        int active = 0;
        int empty = 0;
        long heavy = 0L;
        for (SectorRuntime runtime : sectors.values()) {
            SectorSnapshot s = runtime.snapshot();
            if (s.tier() == SectorSimulationTier.INHABITED_FULL) active++; else empty++;
            heavy += s.heavyEntityCount();
        }
        return "authority=" + VERSION
                + " sectors=" + sectors.size()
                + " inhabited=" + active
                + " empty=" + empty
                + " heavyEntities=" + heavy
                + " pool=" + entityPool.summary()
                + " network=" + networkGateway.summary();
    }

    static String auditSummary() {
        return "authority=" + VERSION + " tiers=inhabited-full/empty-lightweight pool=sector-population network=sector-isolated java=17";
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        for (SectorRuntime runtime : sectors.values()) runtime.shutdown();
        scheduler.shutdownNow();
        sectorTickPool.shutdownNow();
        networkGateway.clear();
        DebugLog.audit("SECTOR_MANAGER_CLOSE", statusLine());
    }

    private SectorRuntime runtimeFor(SectorKey key) {
        Objects.requireNonNull(key, "sector key");
        return sectors.computeIfAbsent(key, k -> new SectorRuntime(k));
    }

    private void requireOpen() {
        if (closed.get()) throw new IllegalStateException("SectorManager is closed");
    }

    private static long nullSafeDurationMillis(Duration duration, long fallback) {
        if (duration == null) return fallback;
        long v = duration.toMillis();
        return v <= 0L ? fallback : v;
    }

    private static ThreadFactory namedThreadFactory(String prefix) {
        AtomicInteger id = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + id.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }

    private final class SectorRuntime {
        private final SectorKey key;
        private final Map<String, PlayerHandle> inhabitants = new ConcurrentHashMap<>();
        private final ArrayList<SectorPopulationEntity> heavyEntities = new ArrayList<>();
        private final ReentrantLock stateLock = new ReentrantLock();
        private final AtomicReference<SectorSimulationTier> tier = new AtomicReference<>(SectorSimulationTier.UNINHABITED_LIGHTWEIGHT);
        private final AtomicBoolean tickInFlight = new AtomicBoolean(false);
        private final AtomicLong fullTicks = new AtomicLong(0L);
        private final AtomicLong backgroundTicks = new AtomicLong(0L);
        private final AtomicLong suppressedNetworkTicks = new AtomicLong(0L);
        private volatile ScheduledFuture<?> scheduledTask;

        SectorRuntime(SectorKey key) {
            this.key = key;
            transitionToEmptyLightweight("created");
        }

        void addPlayer(PlayerHandle player) {
            Objects.requireNonNull(player, "player");
            inhabitants.put(player.playerId(), player);
            if (inhabitants.size() == 1) transitionToFull("player entered " + player.playerId());
        }

        void removePlayer(String playerId) {
            if (playerId == null) return;
            inhabitants.remove(playerId);
            if (inhabitants.isEmpty()) transitionToEmptyLightweight("last player left");
        }

        SectorSnapshot snapshot() {
            stateLock.lock();
            try {
                return new SectorSnapshot(key, tier.get(), inhabitants.size(), heavyEntities.size(), fullTicks.get(), backgroundTicks.get(), suppressedNetworkTicks.get());
            } finally {
                stateLock.unlock();
            }
        }

        void shutdown() {
            cancelScheduledTask();
            stateLock.lock();
            try {
                entityPool.releaseAll(heavyEntities);
                heavyEntities.clear();
                tier.set(SectorSimulationTier.PAUSED);
            } finally {
                stateLock.unlock();
            }
        }

        private void transitionToFull(String reason) {
            stateLock.lock();
            try {
                if (tier.get() == SectorSimulationTier.INHABITED_FULL) return;
                cancelScheduledTask();
                if (heavyEntities.isEmpty()) {
                    heavyEntities.addAll(entityPool.acquirePopulation(key, desiredPopulationFor(key)));
                }
                tier.set(SectorSimulationTier.INHABITED_FULL);
                scheduledTask = scheduler.scheduleAtFixedRate(this::submitFullTick, 0L, fullTickMillis, TimeUnit.MILLISECONDS);
                DebugLog.audit("SECTOR_TIER_FULL", "sector=" + key.compact() + " reason=" + reason + " entities=" + heavyEntities.size());
            } finally {
                stateLock.unlock();
            }
        }

        private void transitionToEmptyLightweight(String reason) {
            stateLock.lock();
            try {
                if (tier.get() == SectorSimulationTier.UNINHABITED_LIGHTWEIGHT && heavyEntities.isEmpty() && scheduledTask != null && !scheduledTask.isCancelled()) return;
                cancelScheduledTask();
                entityPool.releaseAll(heavyEntities);
                heavyEntities.clear();
                tier.set(SectorSimulationTier.UNINHABITED_LIGHTWEIGHT);
                scheduledTask = scheduler.scheduleAtFixedRate(this::submitLightweightTick, emptyTickMillis, emptyTickMillis, TimeUnit.MILLISECONDS);
                DebugLog.audit("SECTOR_TIER_LIGHTWEIGHT", "sector=" + key.compact() + " reason=" + reason + " entitiesReturned pool=" + entityPool.summary());
            } finally {
                stateLock.unlock();
            }
        }

        private void cancelScheduledTask() {
            ScheduledFuture<?> task = scheduledTask;
            if (task != null) task.cancel(false);
            scheduledTask = null;
        }

        private void submitFullTick() {
            if (tier.get() != SectorSimulationTier.INHABITED_FULL || inhabitants.isEmpty()) return;
            if (!tickInFlight.compareAndSet(false, true)) return;
            sectorTickPool.execute(() -> {
                try { fullAuthoritativeTick(); }
                finally { tickInFlight.set(false); }
            });
        }

        private void submitLightweightTick() {
            if (tier.get() != SectorSimulationTier.UNINHABITED_LIGHTWEIGHT || !inhabitants.isEmpty()) return;
            if (!tickInFlight.compareAndSet(false, true)) return;
            sectorTickPool.execute(() -> {
                try { lightweightBackgroundTick(); }
                finally { tickInFlight.set(false); }
            });
        }


        private void runLocalAuthoritativeTurn(String reason, Runnable localTurnBody) {
            if (tier.get() != SectorSimulationTier.INHABITED_FULL || inhabitants.isEmpty()) {
                transitionToFull("local authoritative turn: " + (reason == null ? "unspecified" : reason));
            }
            long waitStart = System.nanoTime();
            boolean acquired = false;
            while (!(acquired = tickInFlight.compareAndSet(false, true))) {
                Thread.onSpinWait();
                if (System.nanoTime() - waitStart > 5_000_000L) {
                    try { Thread.sleep(1L); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
                }
            }
            if (!acquired) return;
            long tickNumber = -1L;
            int entityCount = 0;
            try {
                if (localTurnBody != null) localTurnBody.run();
                stateLock.lock();
                try {
                    tickNumber = fullTicks.incrementAndGet();
                    entityCount = heavyEntities.size();
                } finally {
                    stateLock.unlock();
                }
            } finally {
                tickInFlight.set(false);
            }
            if (tickNumber >= 0L) {
                SectorOutboundPacket packet = new SectorOutboundPacket(key, "single-player-sector-turn", tickNumber, entityCount,
                        "local authoritative player turn: " + (reason == null ? "unspecified" : reason));
                int delivered = networkGateway.broadcastToSector(key, packet);
                if (delivered == 0) suppressedNetworkTicks.incrementAndGet();
            }
        }

        private void fullAuthoritativeTick() {
            long tickNumber;
            int entityCount;
            stateLock.lock();
            try {
                if (tier.get() != SectorSimulationTier.INHABITED_FULL || inhabitants.isEmpty()) return;
                tickNumber = fullTicks.incrementAndGet();
                for (SectorPopulationEntity entity : heavyEntities) entity.fullDetailTick(tickNumber);
                entityCount = heavyEntities.size();
            } finally {
                stateLock.unlock();
            }
            SectorOutboundPacket packet = new SectorOutboundPacket(key, "sector-delta", tickNumber, entityCount,
                    "authoritative sector delta: tick=" + tickNumber + " entities=" + entityCount);
            int delivered = networkGateway.broadcastToSector(key, packet);
            if (delivered == 0) suppressedNetworkTicks.incrementAndGet();
        }

        private void lightweightBackgroundTick() {
            long tickNumber = backgroundTicks.incrementAndGet();
            // No player is present; advance only an abstract ledger and intentionally emit no sector-state packets.
            if (tickNumber % 32L == 0L) {
                DebugLog.audit("SECTOR_LIGHTWEIGHT_TICK", "sector=" + key.compact() + " backgroundTicks=" + tickNumber);
            }
        }

        private int desiredPopulationFor(SectorKey key) {
            int spread = Math.abs(Objects.hash(key.hiveId(), key.sectorX(), key.sectorY(), key.floor(), key.zoneId())) % 24;
            return 16 + spread;
        }
    }
}

enum SectorSimulationTier {
    INHABITED_FULL,
    UNINHABITED_LIGHTWEIGHT,
    PAUSED
}

record SectorKey(int hiveId, int sectorX, int sectorY, int floor, String zoneId) {
    SectorKey {
        zoneId = zoneId == null || zoneId.isBlank() ? "zone" : zoneId.trim();
    }
    static SectorKey of(int hiveId, int sectorX, int sectorY, int floor, String zoneId) {
        return new SectorKey(hiveId, sectorX, sectorY, floor, zoneId);
    }
    String compact() { return "h" + hiveId + ":s" + sectorX + "," + sectorY + ":f" + floor + ":" + zoneId; }
}

final class PlayerHandle {
    private final String playerId;

    private PlayerHandle(String playerId) {
        this.playerId = playerId;
    }

    static PlayerHandle of(String playerId) {
        String id = playerId == null ? "" : playerId.trim();
        if (id.isEmpty()) throw new IllegalArgumentException("playerId is required");
        return new PlayerHandle(id);
    }

    String playerId() { return playerId; }

    @Override public boolean equals(Object other) {
        return other instanceof PlayerHandle that && playerId.equals(that.playerId);
    }

    @Override public int hashCode() { return playerId.hashCode(); }
    @Override public String toString() { return playerId; }
}

record SectorSnapshot(SectorKey key,
                      SectorSimulationTier tier,
                      int players,
                      int heavyEntityCount,
                      long fullTicks,
                      long backgroundTicks,
                      long suppressedNetworkTicks) {
    String compact() {
        return "sector=" + key.compact()
                + " tier=" + tier
                + " players=" + players
                + " heavy=" + heavyEntityCount
                + " fullTicks=" + fullTicks
                + " backgroundTicks=" + backgroundTicks
                + " suppressedNet=" + suppressedNetworkTicks;
    }
}

record SectorOutboundPacket(SectorKey sector, String packetType, long tick, int entityCount, String payload) { }

interface SectorNetworkGateway {
    void bindPlayerToSector(PlayerHandle player, SectorKey sector);
    void unbindPlayerFromSector(PlayerHandle player, SectorKey sector);
    int broadcastToSector(SectorKey sector, SectorOutboundPacket packet);
    String summary();
    void clear();
}

final class InMemorySectorNetworkGateway implements SectorNetworkGateway {
    private final Map<SectorKey, Map<String, PlayerHandle>> inhabitantsBySector = new ConcurrentHashMap<>();
    private final Map<String, List<SectorOutboundPacket>> deliveredByPlayer = new ConcurrentHashMap<>();
    private final AtomicLong attemptedBroadcasts = new AtomicLong(0L);
    private final AtomicLong deliveredPackets = new AtomicLong(0L);

    @Override
    public void bindPlayerToSector(PlayerHandle player, SectorKey sector) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(sector, "sector");
        inhabitantsBySector.computeIfAbsent(sector, k -> new ConcurrentHashMap<>()).put(player.playerId(), player);
    }

    @Override
    public void unbindPlayerFromSector(PlayerHandle player, SectorKey sector) {
        if (player == null || sector == null) return;
        Map<String, PlayerHandle> players = inhabitantsBySector.get(sector);
        if (players != null) {
            players.remove(player.playerId());
            if (players.isEmpty()) inhabitantsBySector.remove(sector, players);
        }
    }

    @Override
    public int broadcastToSector(SectorKey sector, SectorOutboundPacket packet) {
        Objects.requireNonNull(sector, "sector");
        Objects.requireNonNull(packet, "packet");
        if (!sector.equals(packet.sector())) {
            throw new IllegalArgumentException("packet sector " + packet.sector().compact() + " cannot be broadcast through " + sector.compact());
        }
        attemptedBroadcasts.incrementAndGet();
        Map<String, PlayerHandle> recipients = inhabitantsBySector.get(sector);
        if (recipients == null || recipients.isEmpty()) return 0;
        int delivered = 0;
        for (PlayerHandle player : recipients.values()) {
            deliveredByPlayer.computeIfAbsent(player.playerId(), k -> Collections.synchronizedList(new ArrayList<>())).add(packet);
            delivered++;
        }
        deliveredPackets.addAndGet(delivered);
        return delivered;
    }

    List<SectorOutboundPacket> deliveredTo(String playerId) {
        List<SectorOutboundPacket> packets = deliveredByPlayer.get(playerId);
        if (packets == null) return List.of();
        synchronized (packets) { return List.copyOf(packets); }
    }

    boolean playerReceivedPacketForSector(String playerId, SectorKey sector) {
        for (SectorOutboundPacket packet : deliveredTo(playerId)) if (packet.sector().equals(sector)) return true;
        return false;
    }

    @Override
    public String summary() {
        int inhabitants = 0;
        for (Map<String, PlayerHandle> players : inhabitantsBySector.values()) inhabitants += players.size();
        return "inhabitedSectorBindings=" + inhabitantsBySector.size()
                + " players=" + inhabitants
                + " broadcasts=" + attemptedBroadcasts.get()
                + " delivered=" + deliveredPackets.get();
    }

    @Override
    public void clear() {
        inhabitantsBySector.clear();
        deliveredByPlayer.clear();
    }
}

final class SectorEntityPool {
    private final int maxRetained;
    private final ConcurrentLinkedDeque<SectorPopulationEntity> freeEntities = new ConcurrentLinkedDeque<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final AtomicLong created = new AtomicLong(0L);
    private final AtomicLong borrowed = new AtomicLong(0L);
    private final AtomicLong returned = new AtomicLong(0L);
    private final AtomicLong discarded = new AtomicLong(0L);

    SectorEntityPool(int maxRetained) {
        this.maxRetained = Math.max(0, maxRetained);
    }

    List<SectorPopulationEntity> acquirePopulation(SectorKey sector, int count) {
        int safeCount = Math.max(0, count);
        ArrayList<SectorPopulationEntity> out = new ArrayList<>(safeCount);
        for (int i = 0; i < safeCount; i++) out.add(acquire(sector, i));
        return out;
    }

    private SectorPopulationEntity acquire(SectorKey sector, int ordinal) {
        SectorPopulationEntity entity = freeEntities.pollFirst();
        if (entity == null) {
            entity = new SectorPopulationEntity(nextId.getAndIncrement());
            created.incrementAndGet();
        }
        entity.resetForSector(sector, ordinal);
        borrowed.incrementAndGet();
        return entity;
    }

    void releaseAll(List<SectorPopulationEntity> entities) {
        if (entities == null || entities.isEmpty()) return;
        for (SectorPopulationEntity entity : entities) release(entity);
    }

    private void release(SectorPopulationEntity entity) {
        if (entity == null) return;
        entity.scrubForPool();
        if (freeEntities.size() >= maxRetained) {
            discarded.incrementAndGet();
            return;
        }
        freeEntities.offerFirst(entity);
        returned.incrementAndGet();
    }

    int available() { return freeEntities.size(); }

    String summary() {
        return "created=" + created.get()
                + " borrowed=" + borrowed.get()
                + " returned=" + returned.get()
                + " pooled=" + freeEntities.size()
                + " discarded=" + discarded.get();
    }
}

final class SectorPopulationEntity {
    private final int poolId;
    private SectorKey sector;
    private String runtimeId;
    private int x;
    private int y;
    private int hp;
    private long ticks;
    private String aiState;

    SectorPopulationEntity(int poolId) {
        this.poolId = poolId;
    }

    void resetForSector(SectorKey sector, int ordinal) {
        this.sector = sector;
        this.runtimeId = sector.compact() + ":pop:" + poolId;
        this.x = 4 + (ordinal % 16);
        this.y = 4 + (ordinal / 16);
        this.hp = 10;
        this.ticks = 0L;
        this.aiState = "sector-authoritative-idle";
    }

    void fullDetailTick(long sectorTick) {
        ticks++;
        int stride = (poolId % 3) - 1;
        x = Math.max(1, x + stride);
        y = Math.max(1, y + ((sectorTick + poolId) % 5L == 0L ? 1 : 0));
        aiState = "authoritative-tick-" + sectorTick;
    }

    void scrubForPool() {
        sector = null;
        runtimeId = null;
        x = 0;
        y = 0;
        hp = 0;
        ticks = 0L;
        aiState = "pooled";
    }

    @Override
    public String toString() {
        return "SectorPopulationEntity{" + poolId + ", sector=" + (sector == null ? "pooled" : sector.compact()) + ", id=" + runtimeId + ", state=" + aiState + "}";
    }
}
