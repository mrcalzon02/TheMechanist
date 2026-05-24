package mechanist;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Native Java sequence validator used by the fallback relay and by future Netty
 * adapters.  When Netty is present, the adapter should call validate(frame)
 * from ChannelInboundHandlerAdapter.channelRead before forwarding the packet.
 */
final class PacketSequenceValidator {
    private final String sessionId;
    private final AtomicLong expectedSequenceId = new AtomicLong(0L);
    private final long maxQueuedGap;
    private final Duration gapTimeout;
    private final Consumer<String> disconnectAction;
    private final TreeMap<Long, SequencedGamePacket> pending = new TreeMap<>();
    private Instant firstGapSeenAt;

    PacketSequenceValidator(String sessionId, long maxQueuedGap, Duration gapTimeout, Consumer<String> disconnectAction) {
        this.sessionId = sessionId == null || sessionId.isBlank() ? "unknown" : sessionId;
        this.maxQueuedGap = Math.max(0L, maxQueuedGap);
        this.gapTimeout = gapTimeout == null ? Duration.ofMillis(250) : gapTimeout;
        this.disconnectAction = Objects.requireNonNull(disconnectAction, "disconnectAction");
    }

    synchronized SequenceDecision validate(SequencedGamePacket packet) {
        Objects.requireNonNull(packet, "packet");
        long expected = expectedSequenceId.get();
        long incoming = packet.sequenceId();
        if (incoming == expected) {
            expectedSequenceId.incrementAndGet();
            firstGapSeenAt = null;
            flushReady();
            return new SequenceDecision(SequenceState.ACCEPTED, incoming, expectedSequenceId.get(), "accepted");
        }
        if (incoming < expected) {
            String reason = ObfuscatedStringTable.text(ObfuscatedStringTable.Key.REPLAY_ATTACK_OR_DUPLICATE_PACKET) + " for " + sessionId + ": incoming=" + incoming + " expected=" + expected;
            disconnectAction.accept(reason);
            return new SequenceDecision(SequenceState.REPLAY_ATTACK, incoming, expected, reason);
        }
        long gap = incoming - expected;
        if (gap > maxQueuedGap) {
            String reason = ObfuscatedStringTable.text(ObfuscatedStringTable.Key.SEQUENCE_GAP_EXCEEDS_LIMIT) + " for " + sessionId + ": incoming=" + incoming + " expected=" + expected + " gap=" + gap;
            disconnectAction.accept(reason);
            return new SequenceDecision(SequenceState.FUTURE_GAP_DISCONNECT, incoming, expected, reason);
        }
        if (firstGapSeenAt == null) firstGapSeenAt = Instant.now();
        pending.putIfAbsent(incoming, packet);
        if (Duration.between(firstGapSeenAt, Instant.now()).compareTo(gapTimeout) > 0) {
            String reason = ObfuscatedStringTable.text(ObfuscatedStringTable.Key.SEQUENCE_GAP_TIMEOUT) + " for " + sessionId + ": incoming=" + incoming + " expected=" + expected;
            disconnectAction.accept(reason);
            return new SequenceDecision(SequenceState.FUTURE_GAP_DISCONNECT, incoming, expected, reason);
        }
        return new SequenceDecision(SequenceState.QUEUED_WAITING_FOR_GAP, incoming, expected, "queued pending missing sequence " + expected);
    }

    private void flushReady() {
        while (true) {
            long expected = expectedSequenceId.get();
            SequencedGamePacket ready = pending.remove(expected);
            if (ready == null) return;
            expectedSequenceId.incrementAndGet();
        }
    }

    long expectedSequenceId() { return expectedSequenceId.get(); }

    enum SequenceState { ACCEPTED, QUEUED_WAITING_FOR_GAP, REPLAY_ATTACK, FUTURE_GAP_DISCONNECT }

    record SequenceDecision(SequenceState state, long incomingSequenceId, long expectedSequenceId, String reason) {
        boolean passToGameLoop() { return state == SequenceState.ACCEPTED; }
        boolean disconnect() { return state == SequenceState.REPLAY_ATTACK || state == SequenceState.FUTURE_GAP_DISCONNECT; }
    }

    record SequencedGamePacket(long sequenceId, String authenticatedSessionToken, long timestampNanos, byte[] payload, Map<String, String> metadata) {
        public SequencedGamePacket {
            if (sequenceId < 0) throw new IllegalArgumentException("sequenceId must be non-negative");
            authenticatedSessionToken = authenticatedSessionToken == null ? "" : authenticatedSessionToken;
            payload = payload == null ? new byte[0] : payload.clone();
            metadata = Map.copyOf(Objects.requireNonNullElse(metadata, Map.of()));
        }
        @Override public byte[] payload() { return payload.clone(); }
    }
}
