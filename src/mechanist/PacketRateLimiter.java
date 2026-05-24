package mechanist;

import java.time.Duration;

/** Small lock-based token bucket suitable for per-session packet policing. */
final class PacketRateLimiter {
    private final int maxTokens;
    private final double refillPerNanos;
    private double tokens;
    private long lastRefillNanos;

    PacketRateLimiter(int maxPacketsPerSecond) {
        this(maxPacketsPerSecond, Duration.ofSeconds(1));
    }

    PacketRateLimiter(int maxPackets, Duration refillPeriod) {
        if (maxPackets < 1) throw new IllegalArgumentException("maxPackets must be positive");
        if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) throw new IllegalArgumentException("refillPeriod must be positive");
        this.maxTokens = maxPackets;
        this.tokens = maxPackets;
        this.refillPerNanos = maxPackets / (double)refillPeriod.toNanos();
        this.lastRefillNanos = System.nanoTime();
    }

    synchronized boolean tryAcquire() { return tryAcquire(1); }

    synchronized boolean tryAcquire(int cost) {
        if (cost < 1) return true;
        refill();
        if (tokens >= cost) {
            tokens -= cost;
            return true;
        }
        return false;
    }

    synchronized int availableWholeTokens() {
        refill();
        return (int)Math.floor(tokens);
    }

    private void refill() {
        long now = System.nanoTime();
        long elapsed = Math.max(0L, now - lastRefillNanos);
        if (elapsed > 0L) {
            tokens = Math.min(maxTokens, tokens + elapsed * refillPerNanos);
            lastRefillNanos = now;
        }
    }
}
