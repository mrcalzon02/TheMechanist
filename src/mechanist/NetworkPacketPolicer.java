package mechanist;

import java.time.Instant;
import java.util.Objects;

/** Enforces hard dual-direction packet ceilings for one network session. */
final class NetworkPacketPolicer {
    static final int DEFAULT_INBOUND_PACKETS_PER_SECOND = 60;
    static final int DEFAULT_OUTBOUND_PACKETS_PER_SECOND = 120;

    record Decision(boolean allowed, String reason, Instant decidedAt) {
        static Decision allow() { return new Decision(true, "allowed", Instant.now()); }
        static Decision deny(String reason) { return new Decision(false, Objects.requireNonNullElse(reason, "denied"), Instant.now()); }
    }

    private final String sessionId;
    private final PacketRateLimiter inbound;
    private final PacketRateLimiter outbound;

    NetworkPacketPolicer(String sessionId) {
        this(sessionId, DEFAULT_INBOUND_PACKETS_PER_SECOND, DEFAULT_OUTBOUND_PACKETS_PER_SECOND);
    }

    NetworkPacketPolicer(String sessionId, int inboundLimit, int outboundLimit) {
        this.sessionId = sessionId == null || sessionId.isBlank() ? "unknown" : sessionId;
        this.inbound = new PacketRateLimiter(inboundLimit);
        this.outbound = new PacketRateLimiter(outboundLimit);
    }

    Decision inboundPacket() {
        return inbound.tryAcquire() ? Decision.allow() : Decision.deny("inbound packet rate exceeded for " + sessionId);
    }

    Decision outboundPacket() {
        return outbound.tryAcquire() ? Decision.allow() : Decision.deny("outbound response rate exceeded for " + sessionId);
    }
}
