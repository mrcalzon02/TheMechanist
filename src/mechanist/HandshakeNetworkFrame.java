package mechanist;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Small typed frame model used by both native fallback and future Netty handshake adapters. */
sealed interface HandshakeNetworkFrame permits HandshakeNetworkFrame.IdentityProof,
        HandshakeNetworkFrame.ManifestPayload,
        HandshakeNetworkFrame.AcquisitionComplete,
        HandshakeNetworkFrame.HotRestartStarted,
        HandshakeNetworkFrame.HotRestartComplete,
        HandshakeNetworkFrame.IntegrityChallengeIssued,
        HandshakeNetworkFrame.IntegrityChallengeResponse,
        HandshakeNetworkFrame.WorldSnapshotReady,
        HandshakeNetworkFrame.AccessGranted,
        HandshakeNetworkFrame.Rejected {

    String sessionId();
    long createdAtMillis();
    String type();

    record IdentityProof(String sessionId, String identityToken, long createdAtMillis) implements HandshakeNetworkFrame {
        public IdentityProof { sessionId = clean(sessionId); identityToken = clean(identityToken); }
        public String type() { return "IDENTITY_PROOF"; }
    }
    record ManifestPayload(String sessionId, String manifestFingerprint, long createdAtMillis) implements HandshakeNetworkFrame {
        public ManifestPayload { sessionId = clean(sessionId); manifestFingerprint = clean(manifestFingerprint); }
        public String type() { return "MANIFEST_PAYLOAD"; }
    }
    record AcquisitionComplete(String sessionId, boolean filesAcquired, boolean layoutVerified, long createdAtMillis) implements HandshakeNetworkFrame {
        public AcquisitionComplete { sessionId = clean(sessionId); }
        public String type() { return "ACQUISITION_COMPLETE"; }
    }
    record HotRestartStarted(String sessionId, long createdAtMillis) implements HandshakeNetworkFrame {
        public HotRestartStarted { sessionId = clean(sessionId); }
        public String type() { return "HOT_RESTART_STARTED"; }
    }
    record HotRestartComplete(String sessionId, String mountedFingerprint, List<String> mountedJars, long createdAtMillis) implements HandshakeNetworkFrame {
        public HotRestartComplete { sessionId = clean(sessionId); mountedFingerprint = clean(mountedFingerprint); mountedJars = List.copyOf(Objects.requireNonNullElse(mountedJars, List.of())); }
        public String type() { return "HOT_RESTART_COMPLETE"; }
    }
    record IntegrityChallengeIssued(String sessionId, String saltHex, String expectedDigestHex, long createdAtMillis) implements HandshakeNetworkFrame {
        public IntegrityChallengeIssued { sessionId = clean(sessionId); saltHex = clean(saltHex); expectedDigestHex = clean(expectedDigestHex); }
        public String type() { return "INTEGRITY_CHALLENGE_ISSUED"; }
    }
    record IntegrityChallengeResponse(String sessionId, String digestHex, long createdAtMillis) implements HandshakeNetworkFrame {
        public IntegrityChallengeResponse { sessionId = clean(sessionId); digestHex = clean(digestHex); }
        public String type() { return "INTEGRITY_CHALLENGE_RESPONSE"; }
    }
    record WorldSnapshotReady(String sessionId, int payloadBytes, long createdAtMillis) implements HandshakeNetworkFrame {
        public WorldSnapshotReady { sessionId = clean(sessionId); if (payloadBytes < 0) throw new IllegalArgumentException("payloadBytes must be non-negative"); }
        public String type() { return "WORLD_SNAPSHOT_READY"; }
    }
    record AccessGranted(String sessionId, long createdAtMillis) implements HandshakeNetworkFrame {
        public AccessGranted { sessionId = clean(sessionId); }
        public String type() { return "ACCESS_GRANTED"; }
    }
    record Rejected(String sessionId, String phase, String reason, long createdAtMillis) implements HandshakeNetworkFrame {
        public Rejected { sessionId = clean(sessionId); phase = clean(phase); reason = reason == null || reason.isBlank() ? "rejected" : reason.replace('\n', ' ').trim(); }
        public String type() { return "REJECTED"; }
    }

    default String toWireLine() {
        if (this instanceof IdentityProof f) return join(f.type(), f.sessionId(), f.identityToken(), Long.toString(f.createdAtMillis()));
        if (this instanceof ManifestPayload f) return join(f.type(), f.sessionId(), f.manifestFingerprint(), Long.toString(f.createdAtMillis()));
        if (this instanceof AcquisitionComplete f) return join(f.type(), f.sessionId(), Boolean.toString(f.filesAcquired()), Boolean.toString(f.layoutVerified()), Long.toString(f.createdAtMillis()));
        if (this instanceof HotRestartStarted f) return join(f.type(), f.sessionId(), Long.toString(f.createdAtMillis()));
        if (this instanceof HotRestartComplete f) return join(f.type(), f.sessionId(), f.mountedFingerprint(), String.join(",", f.mountedJars()), Long.toString(f.createdAtMillis()));
        if (this instanceof IntegrityChallengeIssued f) return join(f.type(), f.sessionId(), f.saltHex(), f.expectedDigestHex(), Long.toString(f.createdAtMillis()));
        if (this instanceof IntegrityChallengeResponse f) return join(f.type(), f.sessionId(), f.digestHex(), Long.toString(f.createdAtMillis()));
        if (this instanceof WorldSnapshotReady f) return join(f.type(), f.sessionId(), Integer.toString(f.payloadBytes()), Long.toString(f.createdAtMillis()));
        if (this instanceof AccessGranted f) return join(f.type(), f.sessionId(), Long.toString(f.createdAtMillis()));
        if (this instanceof Rejected f) return join(f.type(), f.sessionId(), f.phase(), f.reason(), Long.toString(f.createdAtMillis()));
        throw new IllegalStateException("Unknown handshake frame type: " + getClass().getName());
    }

    static HandshakeNetworkFrame parse(String line) {
        if (line == null || line.isBlank()) throw new IllegalArgumentException("blank handshake frame");
        String[] parts = line.split("\\|", -1);
        return switch (parts[0]) {
            case "IDENTITY_PROOF" -> new IdentityProof(part(parts, 1), part(parts, 2), longPart(parts, 3));
            case "MANIFEST_PAYLOAD" -> new ManifestPayload(part(parts, 1), part(parts, 2), longPart(parts, 3));
            case "ACQUISITION_COMPLETE" -> new AcquisitionComplete(part(parts, 1), Boolean.parseBoolean(part(parts, 2)), Boolean.parseBoolean(part(parts, 3)), longPart(parts, 4));
            case "HOT_RESTART_STARTED" -> new HotRestartStarted(part(parts, 1), longPart(parts, 2));
            case "HOT_RESTART_COMPLETE" -> new HotRestartComplete(part(parts, 1), part(parts, 2), splitList(part(parts, 3)), longPart(parts, 4));
            case "INTEGRITY_CHALLENGE_ISSUED" -> new IntegrityChallengeIssued(part(parts, 1), part(parts, 2), part(parts, 3), longPart(parts, 4));
            case "INTEGRITY_CHALLENGE_RESPONSE" -> new IntegrityChallengeResponse(part(parts, 1), part(parts, 2), longPart(parts, 3));
            case "WORLD_SNAPSHOT_READY" -> new WorldSnapshotReady(part(parts, 1), intPart(parts, 2), longPart(parts, 3));
            case "ACCESS_GRANTED" -> new AccessGranted(part(parts, 1), longPart(parts, 2));
            case "REJECTED" -> new Rejected(part(parts, 1), part(parts, 2), part(parts, 3), longPart(parts, 4));
            default -> throw new IllegalArgumentException("unknown handshake frame type: " + parts[0]);
        };
    }

    private static String join(String... raw) {
        if (raw.length == 0) return "";
        StringBuilder sb = new StringBuilder(raw[0] == null ? "" : raw[0]);
        for (int i = 1; i < raw.length; i++) sb.append('|').append(escape(raw[i]));
        return sb.toString();
    }

    private static String escape(String value) {
        String s = value == null ? "" : value;
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String unescape(String value) {
        try { return new String(java.util.Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8); }
        catch (IllegalArgumentException ex) { return value; }
    }

    private static String part(String[] parts, int index) {
        if (index >= parts.length) throw new IllegalArgumentException("handshake frame is missing part " + index);
        return unescape(parts[index]);
    }

    private static long longPart(String[] parts, int index) { return Long.parseLong(part(parts, index)); }
    private static int intPart(String[] parts, int index) { return Integer.parseInt(part(parts, index)); }
    private static List<String> splitList(String value) { return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split(",")).filter(s -> !s.isBlank()).toList(); }
    private static String clean(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("handshake frame value must not be blank");
        return value.trim();
    }
}
