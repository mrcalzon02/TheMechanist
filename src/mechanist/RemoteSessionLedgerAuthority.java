package mechanist;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Process-local remote player identity, reconnect, and immutable session-state authority. */
final class RemoteSessionLedgerAuthority {
    static final String VERSION = "remote-session-ledger-1";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String worldId;
    private final Map<String, MutableSession> sessionsByProfile = new LinkedHashMap<>();
    private long snapshotVersion;

    RemoteSessionLedgerAuthority(String worldId) {
        this.worldId = safeToken(worldId, "remote-world");
    }

    synchronized Attachment attach(
            String profileIdentity,
            String presentedResumeToken,
            String connectionId
    ) {
        String profile = safeToken(profileIdentity, "unknown-profile");
        String connection = safeToken(connectionId, "unknown-connection");
        String resume = Objects.requireNonNullElse(presentedResumeToken, "").trim();
        MutableSession session = sessionsByProfile.get(profile);
        boolean resumed = false;

        if (session == null) {
            if (!resume.isBlank()) {
                throw new SecurityException("resume token does not match any server-owned session");
            }
            session = new MutableSession(
                    profile,
                    playerIdFor(profile),
                    randomHex(32));
            sessionsByProfile.put(profile, session);
        } else {
            if (resume.isBlank() || !constantTimeEquals(session.resumeToken, resume)) {
                throw new SecurityException("a valid resume token is required for an existing profile session");
            }
            if (session.connected && !connection.equals(session.activeConnectionId)) {
                throw new IllegalStateException("profile session is already connected");
            }
            if (connection.equals(session.activeConnectionId)) {
                return attachment(session, true);
            }
            resumed = true;
            session.connectionGeneration++;
        }

        long now = System.currentTimeMillis();
        session.connected = true;
        session.activeConnectionId = connection;
        session.connectedAtMillis = now;
        session.lastSeenMillis = now;
        session.lastConnectionSequence = -1L;
        session.lastEvent = resumed ? "session resumed" : "session created";
        snapshotVersion++;
        return attachment(session, resumed);
    }

    synchronized SessionSnapshot noteRelayFrameAccepted(Attachment attachment, long sequence) {
        MutableSession session = requireActive(attachment);
        long expected = session.lastConnectionSequence + 1L;
        if (sequence != expected) {
            throw new IllegalStateException(
                    "session ledger sequence mismatch: incoming=" + sequence + " expected=" + expected);
        }
        session.lastConnectionSequence = sequence;
        session.acceptedRelayFrames++;
        session.lastSeenMillis = System.currentTimeMillis();
        session.lastEvent = "relay frame accepted";
        snapshotVersion++;
        return snapshot(session);
    }

    synchronized SessionSnapshot snapshot(Attachment attachment) {
        MutableSession session = requireOwned(attachment);
        return snapshot(session);
    }

    synchronized SessionSnapshot snapshotForProfile(String profileIdentity) {
        MutableSession session = sessionsByProfile.get(safeToken(profileIdentity, "unknown-profile"));
        return session == null ? null : snapshot(session);
    }

    synchronized void disconnect(Attachment attachment, String reason) {
        if (attachment == null) return;
        MutableSession session = sessionsByProfile.get(attachment.profileIdentity());
        if (session == null) return;
        if (!attachment.connectionId().equals(session.activeConnectionId)
                || attachment.connectionGeneration() != session.connectionGeneration) {
            return;
        }
        session.connected = false;
        session.activeConnectionId = "";
        session.lastSeenMillis = System.currentTimeMillis();
        session.lastEvent = "disconnected: " + safeEvent(reason);
        snapshotVersion++;
    }

    synchronized int activeSessionCount() {
        int active = 0;
        for (MutableSession session : sessionsByProfile.values()) {
            if (session.connected) active++;
        }
        return active;
    }

    synchronized int totalSessionCount() {
        return sessionsByProfile.size();
    }

    synchronized String auditSummary() {
        return "authority=" + VERSION
                + " world=" + worldId
                + " sessions=" + sessionsByProfile.size()
                + " active=" + activeSessionCount()
                + " snapshotVersion=" + snapshotVersion
                + " worldAuthority=false";
    }

    String worldId() {
        return worldId;
    }

    private MutableSession requireOwned(Attachment attachment) {
        if (attachment == null) throw new IllegalStateException("remote session attachment is missing");
        MutableSession session = sessionsByProfile.get(attachment.profileIdentity());
        if (session == null || !session.playerId.equals(attachment.playerId())) {
            throw new IllegalStateException("remote session attachment is stale or foreign");
        }
        return session;
    }

    private MutableSession requireActive(Attachment attachment) {
        MutableSession session = requireOwned(attachment);
        if (!session.connected
                || !attachment.connectionId().equals(session.activeConnectionId)
                || attachment.connectionGeneration() != session.connectionGeneration) {
            throw new IllegalStateException("remote session attachment is not the active connection");
        }
        return session;
    }

    private Attachment attachment(MutableSession session, boolean resumed) {
        return new Attachment(
                session.profileIdentity,
                session.playerId,
                session.resumeToken,
                session.activeConnectionId,
                session.connectionGeneration,
                snapshotVersion,
                resumed);
    }

    private SessionSnapshot snapshot(MutableSession session) {
        return new SessionSnapshot(
                snapshotVersion,
                worldId,
                session.playerId,
                session.connected,
                session.connectionGeneration,
                session.acceptedRelayFrames,
                session.lastConnectionSequence,
                session.connectedAtMillis,
                session.lastSeenMillis,
                session.lastEvent);
    }

    private String playerIdFor(String profileIdentity) {
        String digest = sha256Hex(worldId + "|" + profileIdentity);
        return "remote-" + digest.substring(0, 20);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                Objects.requireNonNullElse(actual, "").getBytes(StandardCharsets.UTF_8));
    }

    private static String randomHex(int bytes) {
        byte[] data = new byte[Math.max(16, Math.min(64, bytes))];
        RANDOM.nextBytes(data);
        return HexFormat.of().formatHex(data);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw new IllegalStateException("SHA-256 is unavailable", failure);
        }
    }

    private static String safeToken(String value, String fallback) {
        String token = Objects.requireNonNullElse(value, "").trim();
        if (token.isBlank()) token = fallback;
        token = token.replace('|', '_').replace('\n', ' ').replace('\r', ' ');
        return token.substring(0, Math.min(256, token.length()));
    }

    private static String safeEvent(String value) {
        String event = Objects.requireNonNullElse(value, "unspecified")
                .replace('|', '/')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        return event.isBlank() ? "unspecified" : event.substring(0, Math.min(160, event.length()));
    }

    record Attachment(
            String profileIdentity,
            String playerId,
            String resumeToken,
            String connectionId,
            long connectionGeneration,
            long snapshotVersion,
            boolean resumed
    ) { }

    record SessionSnapshot(
            long version,
            String worldId,
            String playerId,
            boolean connected,
            long connectionGeneration,
            long acceptedRelayFrames,
            long lastConnectionSequence,
            long connectedAtMillis,
            long lastSeenMillis,
            String lastEvent
    ) {
        String compactLine() {
            return "sessionSnapshot=v" + version
                    + " world=" + worldId
                    + " player=" + playerId
                    + " connected=" + connected
                    + " generation=" + connectionGeneration
                    + " relayFrames=" + acceptedRelayFrames
                    + " lastSequence=" + lastConnectionSequence
                    + " lastEvent=" + lastEvent;
        }
    }

    private static final class MutableSession {
        final String profileIdentity;
        final String playerId;
        final String resumeToken;
        String activeConnectionId = "";
        boolean connected;
        long connectionGeneration = 1L;
        long acceptedRelayFrames;
        long lastConnectionSequence = -1L;
        long connectedAtMillis;
        long lastSeenMillis;
        String lastEvent = "session allocated at " + Instant.now();

        MutableSession(String profileIdentity, String playerId, String resumeToken) {
            this.profileIdentity = profileIdentity;
            this.playerId = playerId;
            this.resumeToken = resumeToken;
        }
    }

    private RemoteSessionLedgerAuthority() {
        throw new AssertionError("use world-bound constructor");
    }
}
