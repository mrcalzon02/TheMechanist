package mechanist;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Central load-aware traffic controller for direct acquisition fallback traffic.
 * Gameplay clients are accounted separately from clients downloading mod files so
 * asset transfer can decay without starving live game tick packets.
 */
final class NetworkThrottlingManager {
    static final long DEFAULT_OUTBOUND_POOL_BYTES_PER_SECOND = 8L * 1024L * 1024L;
    static final long MIN_DOWNLOAD_BYTES_PER_SECOND = 64L * 1024L;
    static final long LIVE_PLAYER_RESERVED_BYTES_PER_SECOND = 48L * 1024L;
    private static final double CONNECTION_DECAY = 0.18d;

    private final AtomicInteger activePlayers = new AtomicInteger();
    private final AtomicInteger downloadingClients = new AtomicInteger();
    private final AtomicLong outboundPoolBytesPerSecond = new AtomicLong(DEFAULT_OUTBOUND_POOL_BYTES_PER_SECOND);
    private final Map<String, ChannelBudget> budgets = new ConcurrentHashMap<>();

    int activePlayers() { return activePlayers.get(); }
    int downloadingClients() { return downloadingClients.get(); }

    void setOutboundPoolBytesPerSecond(long bytesPerSecond) {
        outboundPoolBytesPerSecond.set(Math.max(MIN_DOWNLOAD_BYTES_PER_SECOND, bytesPerSecond));
        recalibrateAll();
    }

    ChannelBudget registerChannel(String sessionId) {
        String id = sanitizeSessionId(sessionId);
        ChannelBudget budget = new ChannelBudget(id, perDownloadingClientLimitBytesPerSecond());
        budgets.put(id, budget);
        return budget;
    }

    void unregisterChannel(String sessionId) {
        ChannelBudget removed = budgets.remove(sanitizeSessionId(sessionId));
        if (removed != null) removed.close();
    }

    void playerJoined() {
        activePlayers.incrementAndGet();
        recalibrateAll();
    }

    void playerLeft() {
        decrementFloorZero(activePlayers);
        recalibrateAll();
    }

    void downloadStarted(String sessionId) {
        downloadingClients.incrementAndGet();
        registerChannel(sessionId).setLimitBytesPerSecond(perDownloadingClientLimitBytesPerSecond());
        recalibrateAll();
    }

    void downloadFinished(String sessionId) {
        decrementFloorZero(downloadingClients);
        ChannelBudget budget = budgets.get(sanitizeSessionId(sessionId));
        if (budget != null) budget.resetWindow();
        recalibrateAll();
    }

    ThrottleSnapshot snapshot() {
        return new ThrottleSnapshot(activePlayers.get(), downloadingClients.get(), outboundPoolBytesPerSecond.get(), perDownloadingClientLimitBytesPerSecond());
    }

    long perDownloadingClientLimitBytesPerSecond() {
        int players = Math.max(0, activePlayers.get());
        int downloads = Math.max(1, downloadingClients.get());
        int totalConnections = Math.max(1, players + downloads);
        long reservedForGameplay = Math.min(outboundPoolBytesPerSecond.get() / 2L, players * LIVE_PLAYER_RESERVED_BYTES_PER_SECOND);
        long remainingPool = Math.max(MIN_DOWNLOAD_BYTES_PER_SECOND, outboundPoolBytesPerSecond.get() - reservedForGameplay);
        double decayMultiplier = 1.0d / (1.0d + CONNECTION_DECAY * Math.max(0, totalConnections - 1));
        long scaled = (long)Math.floor((remainingPool / (double)downloads) * decayMultiplier);
        return Math.max(MIN_DOWNLOAD_BYTES_PER_SECOND, Math.min(remainingPool, scaled));
    }

    private void recalibrateAll() {
        long limit = perDownloadingClientLimitBytesPerSecond();
        for (ChannelBudget budget : budgets.values()) budget.setLimitBytesPerSecond(limit);
    }

    private static void decrementFloorZero(AtomicInteger counter) {
        int current;
        do {
            current = counter.get();
            if (current <= 0) return;
        } while (!counter.compareAndSet(current, current - 1));
    }

    private static String sanitizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) return "anonymous-session";
        return sessionId.replaceAll("[^A-Za-z0-9._:-]", "_");
    }

    static final class ChannelBudget implements AutoCloseable {
        private final String sessionId;
        private final AtomicLong limitBytesPerSecond;
        private final AtomicBoolean open = new AtomicBoolean(true);
        private long windowStartNanos = System.nanoTime();
        private long windowBytes;

        ChannelBudget(String sessionId, long initialLimitBytesPerSecond) {
            this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
            this.limitBytesPerSecond = new AtomicLong(Math.max(MIN_DOWNLOAD_BYTES_PER_SECOND, initialLimitBytesPerSecond));
        }

        String sessionId() { return sessionId; }
        long limitBytesPerSecond() { return limitBytesPerSecond.get(); }

        void setLimitBytesPerSecond(long value) {
            limitBytesPerSecond.set(Math.max(MIN_DOWNLOAD_BYTES_PER_SECOND, value));
        }

        synchronized void resetWindow() {
            windowStartNanos = System.nanoTime();
            windowBytes = 0L;
        }

        void writeThrottled(OutputStream out, byte[] bytes) throws IOException, InterruptedException {
            Objects.requireNonNull(out, "out");
            Objects.requireNonNull(bytes, "bytes");
            awaitPermit(bytes.length);
            out.write(bytes);
        }

        synchronized void awaitPermit(long byteCount) throws InterruptedException {
            if (!open.get()) throw new InterruptedException("channel budget closed for " + sessionId);
            long now = System.nanoTime();
            long elapsedNanos = Math.max(1L, now - windowStartNanos);
            if (elapsedNanos >= TimeUnit.SECONDS.toNanos(1)) {
                windowStartNanos = now;
                windowBytes = 0L;
                elapsedNanos = 1L;
            }
            long limit = limitBytesPerSecond.get();
            long prospectiveBytes = windowBytes + Math.max(0L, byteCount);
            long allowedSoFar = Math.max(1L, (limit * elapsedNanos) / TimeUnit.SECONDS.toNanos(1));
            if (prospectiveBytes > allowedSoFar) {
                long excess = prospectiveBytes - allowedSoFar;
                long sleepNanos = Math.min(TimeUnit.SECONDS.toNanos(1), (excess * TimeUnit.SECONDS.toNanos(1)) / Math.max(1L, limit));
                long sleepMillis = Math.max(1L, TimeUnit.NANOSECONDS.toMillis(sleepNanos));
                Thread.sleep(sleepMillis);
                now = System.nanoTime();
                if (now - windowStartNanos >= TimeUnit.SECONDS.toNanos(1)) {
                    windowStartNanos = now;
                    windowBytes = 0L;
                }
            }
            windowBytes += Math.max(0L, byteCount);
        }

        @Override public void close() { open.set(false); }
    }

    record ThrottleSnapshot(int activePlayers, int downloadingClients, long outboundPoolBytesPerSecond, long perDownloadClientBytesPerSecond) { }
}
