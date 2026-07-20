package mechanist;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/** Server-owned remote player identity, reconnect, persistence, and immutable session-state authority. */
final class RemoteSessionLedgerAuthority implements AutoCloseable {
    static final String VERSION = "remote-session-ledger-2";
    private static final String PERSISTENCE_SCHEMA = "1";
    private static final int MAX_PERSISTED_SESSIONS = 10_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String worldId;
    private final Path persistenceFile;
    private final Map<String, MutableSession> sessionsByProfile = new LinkedHashMap<>();
    private long snapshotVersion;
    private boolean closed;

    /** In-memory authority used by focused tests and nonpersistent embedding. */
    RemoteSessionLedgerAuthority(String worldId) {
        this.worldId = safeToken(worldId, "remote-world");
        this.persistenceFile = null;
    }

    /** Persistent authority used by the independent headless host. */
    RemoteSessionLedgerAuthority(String worldId, Path persistenceFile) throws IOException {
        this.worldId = safeToken(worldId, "remote-world");
        this.persistenceFile = Objects.requireNonNull(persistenceFile, "persistenceFile")
                .toAbsolutePath().normalize();
        loadIfPresent();
    }

    synchronized Attachment attach(
            String profileIdentity,
            String presentedResumeToken,
            String connectionId
    ) {
        requireOpen();
        String profile = safeToken(profileIdentity, "unknown-profile");
        String connection = safeToken(connectionId, "unknown-connection");
        String resume = Objects.requireNonNullElse(presentedResumeToken, "")
                .trim()
                .toLowerCase();
        MutableSession session = sessionsByProfile.get(profile);
        MutableSession previous = session == null ? null : session.copy();
        long previousVersion = snapshotVersion;
        boolean resumed = false;

        if (session == null) {
            if (!resume.isBlank()) {
                throw new SecurityException(
                        "resume token does not match any server-owned session");
            }
            String issuedToken = randomHex(32);
            session = new MutableSession(
                    profile,
                    playerIdFor(profile),
                    sha256Hex(issuedToken));
            session.activeResumeToken = issuedToken;
            sessionsByProfile.put(profile, session);
        } else {
            if (resume.isBlank()
                    || !constantTimeEquals(
                    session.resumeTokenHash,
                    sha256Hex(resume))) {
                throw new SecurityException(
                        "a valid resume token is required for an existing profile session");
            }
            if (session.connected
                    && !connection.equals(session.activeConnectionId)) {
                throw new IllegalStateException(
                        "profile session is already connected");
            }
            if (connection.equals(session.activeConnectionId)) {
                return attachment(session, true);
            }
            resumed = true;
            session.connectionGeneration++;
            session.activeResumeToken = resume;
        }

        long now = System.currentTimeMillis();
        session.connected = true;
        session.activeConnectionId = connection;
        session.connectedAtMillis = now;
        session.lastSeenMillis = now;
        session.lastConnectionSequence = -1L;
        session.lastEvent = resumed ? "session resumed" : "session created";
        snapshotVersion++;
        persistOrRollback(profile, previous, previousVersion);
        return attachment(session, resumed);
    }

    synchronized SessionSnapshot noteRelayFrameAccepted(
            Attachment attachment,
            long sequence
    ) {
        requireOpen();
        MutableSession session = requireActive(attachment);
        MutableSession previous = session.copy();
        long previousVersion = snapshotVersion;
        long expected = session.lastConnectionSequence + 1L;
        if (sequence != expected) {
            throw new IllegalStateException(
                    "session ledger sequence mismatch: incoming=" + sequence
                            + " expected=" + expected);
        }
        session.lastConnectionSequence = sequence;
        session.acceptedRelayFrames++;
        session.lastSeenMillis = System.currentTimeMillis();
        session.lastEvent = "relay frame accepted";
        snapshotVersion++;
        persistOrRollback(session.profileIdentity, previous, previousVersion);
        return snapshot(session);
    }

    synchronized SessionSnapshot snapshot(Attachment attachment) {
        requireOpen();
        MutableSession session = requireOwned(attachment);
        return snapshot(session);
    }

    synchronized SessionSnapshot snapshotForProfile(String profileIdentity) {
        requireOpen();
        MutableSession session = sessionsByProfile.get(
                safeToken(profileIdentity, "unknown-profile"));
        return session == null ? null : snapshot(session);
    }

    synchronized void disconnect(Attachment attachment, String reason) {
        requireOpen();
        if (attachment == null) return;
        MutableSession session = sessionsByProfile.get(
                attachment.profileIdentity());
        if (session == null) return;
        if (!attachment.connectionId().equals(session.activeConnectionId)
                || attachment.connectionGeneration()
                != session.connectionGeneration) {
            return;
        }
        MutableSession previous = session.copy();
        long previousVersion = snapshotVersion;
        session.connected = false;
        session.activeConnectionId = "";
        session.activeResumeToken = "";
        session.lastConnectionSequence = -1L;
        session.lastSeenMillis = System.currentTimeMillis();
        session.lastEvent = "disconnected: " + safeEvent(reason);
        snapshotVersion++;
        persistOrRollback(session.profileIdentity, previous, previousVersion);
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
                + " persistence="
                + (persistenceFile == null ? "disabled" : persistenceFile)
                + " tokenStorage=sha256-only"
                + " atomicMove=required"
                + " worldAuthority=false";
    }

    String worldId() {
        return worldId;
    }

    Path persistenceFile() {
        return persistenceFile;
    }

    boolean persistenceEnabled() {
        return persistenceFile != null;
    }

    private MutableSession requireOwned(Attachment attachment) {
        if (attachment == null) {
            throw new IllegalStateException(
                    "remote session attachment is missing");
        }
        MutableSession session = sessionsByProfile.get(
                attachment.profileIdentity());
        if (session == null
                || !session.playerId.equals(attachment.playerId())) {
            throw new IllegalStateException(
                    "remote session attachment is stale or foreign");
        }
        return session;
    }

    private MutableSession requireActive(Attachment attachment) {
        MutableSession session = requireOwned(attachment);
        if (!session.connected
                || !attachment.connectionId().equals(session.activeConnectionId)
                || attachment.connectionGeneration()
                != session.connectionGeneration) {
            throw new IllegalStateException(
                    "remote session attachment is not the active connection");
        }
        return session;
    }

    private Attachment attachment(
            MutableSession session,
            boolean resumed
    ) {
        if (session.activeResumeToken.isBlank()) {
            throw new IllegalStateException(
                    "active remote session does not have a returnable resume token");
        }
        return new Attachment(
                session.profileIdentity,
                session.playerId,
                session.activeResumeToken,
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

    private void persistOrRollback(
            String profile,
            MutableSession previous,
            long previousVersion
    ) {
        try {
            persist();
        } catch (IOException | RuntimeException failure) {
            snapshotVersion = previousVersion;
            if (previous == null) sessionsByProfile.remove(profile);
            else sessionsByProfile.put(profile, previous);
            throw new IllegalStateException(
                    "remote session ledger could not persist atomically",
                    failure);
        }
    }

    private void loadIfPresent() throws IOException {
        if (persistenceFile == null || !Files.exists(persistenceFile)) return;
        if (!Files.isRegularFile(persistenceFile)) {
            throw new IOException(
                    "remote session ledger path is not a regular file: "
                            + persistenceFile);
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(persistenceFile)) {
            properties.load(input);
        }
        String schema = properties.getProperty("schema", "").trim();
        if (!PERSISTENCE_SCHEMA.equals(schema)) {
            throw new IOException(
                    "unsupported remote session ledger schema: " + schema);
        }
        String storedWorld = properties.getProperty("worldId", "").trim();
        if (!worldId.equals(storedWorld)) {
            throw new IOException(
                    "remote session ledger world mismatch: expected " + worldId
                            + " but found " + storedWorld);
        }
        snapshotVersion = parseLong(
                properties.getProperty("snapshotVersion"),
                0L,
                "snapshotVersion");
        int count = parseCount(properties.getProperty("session.count"));
        for (int index = 0; index < count; index++) {
            String prefix = "session." + index + ".";
            String profile = requiredProperty(
                    properties,
                    prefix + "profile");
            String playerId = requiredProperty(
                    properties,
                    prefix + "playerId");
            String expectedPlayerId = playerIdFor(profile);
            if (!expectedPlayerId.equals(playerId)) {
                throw new IOException(
                        "remote session player identity mismatch for profile "
                                + profile);
            }
            String tokenHash = requiredProperty(
                    properties,
                    prefix + "resumeTokenSha256")
                    .toLowerCase();
            if (!tokenHash.matches("[a-f0-9]{64}")) {
                throw new IOException(
                        "remote session resume-token hash is invalid for profile "
                                + profile);
            }
            if (sessionsByProfile.containsKey(profile)) {
                throw new IOException(
                        "duplicate remote session profile in ledger: "
                                + profile);
            }
            MutableSession session = new MutableSession(
                    profile,
                    playerId,
                    tokenHash);
            session.connectionGeneration = Math.max(
                    1L,
                    parseLong(
                            properties.getProperty(
                                    prefix + "connectionGeneration"),
                            1L,
                            prefix + "connectionGeneration"));
            session.acceptedRelayFrames = parseLong(
                    properties.getProperty(
                            prefix + "acceptedRelayFrames"),
                    0L,
                    prefix + "acceptedRelayFrames");
            session.lastSeenMillis = parseLong(
                    properties.getProperty(prefix + "lastSeenMillis"),
                    0L,
                    prefix + "lastSeenMillis");
            session.connected = false;
            session.activeConnectionId = "";
            session.activeResumeToken = "";
            session.lastConnectionSequence = -1L;
            session.connectedAtMillis = 0L;
            session.lastEvent =
                    "restored offline after server restart";
            sessionsByProfile.put(profile, session);
        }
        snapshotVersion++;
        setOwnerOnlyPermissions(persistenceFile);
    }

    private void persist() throws IOException {
        if (persistenceFile == null) return;
        Path parent = persistenceFile.getParent();
        if (parent == null) {
            throw new IOException(
                    "remote session ledger persistence path has no parent: "
                            + persistenceFile);
        }
        Files.createDirectories(parent);
        Properties properties = new Properties();
        properties.setProperty("schema", PERSISTENCE_SCHEMA);
        properties.setProperty("worldId", worldId);
        properties.setProperty(
                "snapshotVersion",
                Long.toString(snapshotVersion));
        List<MutableSession> ordered = new ArrayList<>(
                sessionsByProfile.values());
        ordered.sort(Comparator.comparing(
                session -> session.profileIdentity));
        properties.setProperty(
                "session.count",
                Integer.toString(ordered.size()));
        for (int index = 0; index < ordered.size(); index++) {
            MutableSession session = ordered.get(index);
            String prefix = "session." + index + ".";
            properties.setProperty(
                    prefix + "profile",
                    session.profileIdentity);
            properties.setProperty(
                    prefix + "playerId",
                    session.playerId);
            properties.setProperty(
                    prefix + "resumeTokenSha256",
                    session.resumeTokenHash);
            properties.setProperty(
                    prefix + "connectionGeneration",
                    Long.toString(session.connectionGeneration));
            properties.setProperty(
                    prefix + "acceptedRelayFrames",
                    Long.toString(session.acceptedRelayFrames));
            properties.setProperty(
                    prefix + "lastSeenMillis",
                    Long.toString(session.lastSeenMillis));
            properties.setProperty(
                    prefix + "lastEvent",
                    safeEvent(session.lastEvent));
        }

        Path temporary = parent.resolve(
                persistenceFile.getFileName()
                        + ".tmp-"
                        + randomHex(8));
        try {
            try (FileChannel channel = FileChannel.open(
                         temporary,
                         StandardOpenOption.CREATE_NEW,
                         StandardOpenOption.WRITE);
                 OutputStream output = new BufferedOutputStream(
                         Channels.newOutputStream(channel))) {
                setOwnerOnlyPermissions(temporary);
                properties.store(
                        output,
                        "The Mechanist remote session ledger; resume tokens stored as SHA-256 only");
                output.flush();
                channel.force(true);
            }
            Files.move(
                    temporary,
                    persistenceFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            setOwnerOnlyPermissions(persistenceFile);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String requiredProperty(
            Properties properties,
            String key
    ) throws IOException {
        String value = properties.getProperty(key, "").trim();
        if (value.isBlank()) {
            throw new IOException(
                    "remote session ledger is missing " + key);
        }
        if (value.length() > 4096
                || value.indexOf('|') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0) {
            throw new IOException(
                    "remote session ledger contains unsafe " + key);
        }
        return value;
    }

    private static int parseCount(String value) throws IOException {
        long parsed = parseLong(value, 0L, "session.count");
        if (parsed > MAX_PERSISTED_SESSIONS) {
            throw new IOException(
                    "remote session ledger exceeds maximum session count "
                            + MAX_PERSISTED_SESSIONS);
        }
        return (int) parsed;
    }

    private static long parseLong(
            String value,
            long fallback,
            String label
    ) throws IOException {
        if (value == null || value.isBlank()) return fallback;
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed < 0L) throw new NumberFormatException("negative");
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IOException(
                    "remote session ledger contains invalid " + label,
                    failure);
        }
    }

    private static void setOwnerOnlyPermissions(Path path) {
        try {
            Files.setPosixFilePermissions(
                    path,
                    EnumSet.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE));
        } catch (IOException | UnsupportedOperationException ignored) {
            // Windows and some mounted filesystems do not expose POSIX permissions.
        }
    }

    private static boolean constantTimeEquals(
            String expected,
            String actual
    ) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                Objects.requireNonNullElse(actual, "")
                        .getBytes(StandardCharsets.UTF_8));
    }

    private static String randomHex(int bytes) {
        byte[] data = new byte[Math.max(16, Math.min(64, bytes))];
        RANDOM.nextBytes(data);
        return HexFormat.of().formatHex(data);
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(
                            value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    failure);
        }
    }

    private static String safeToken(String value, String fallback) {
        String token = Objects.requireNonNullElse(value, "").trim();
        if (token.isBlank()) token = fallback;
        token = token.replace('|', '_')
                .replace('\n', ' ')
                .replace('\r', ' ');
        return token.substring(0, Math.min(256, token.length()));
    }

    private static String safeEvent(String value) {
        String event = Objects.requireNonNullElse(value, "unspecified")
                .replace('|', '/')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        return event.isBlank()
                ? "unspecified"
                : event.substring(0, Math.min(160, event.length()));
    }

    private void requireOpen() {
        if (closed) {
            throw new IllegalStateException(
                    "remote session ledger is closed");
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        List<MutableSession> previous = new ArrayList<>();
        for (MutableSession session : sessionsByProfile.values()) {
            previous.add(session.copy());
            session.connected = false;
            session.activeConnectionId = "";
            session.activeResumeToken = "";
            session.lastConnectionSequence = -1L;
            session.lastSeenMillis = System.currentTimeMillis();
            session.lastEvent = "server ledger closed";
        }
        long previousVersion = snapshotVersion;
        if (!sessionsByProfile.isEmpty()) snapshotVersion++;
        try {
            persist();
            closed = true;
        } catch (IOException | RuntimeException failure) {
            sessionsByProfile.clear();
            for (MutableSession session : previous) {
                sessionsByProfile.put(
                        session.profileIdentity,
                        session);
            }
            snapshotVersion = previousVersion;
            if (failure instanceof IOException ioFailure) {
                throw ioFailure;
            }
            throw new IOException(
                    "remote session ledger could not close atomically",
                    failure);
        }
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
        final String resumeTokenHash;
        String activeResumeToken = "";
        String activeConnectionId = "";
        boolean connected;
        long connectionGeneration = 1L;
        long acceptedRelayFrames;
        long lastConnectionSequence = -1L;
        long connectedAtMillis;
        long lastSeenMillis;
        String lastEvent = "session allocated at " + Instant.now();

        MutableSession(
                String profileIdentity,
                String playerId,
                String resumeTokenHash
        ) {
            this.profileIdentity = profileIdentity;
            this.playerId = playerId;
            this.resumeTokenHash = resumeTokenHash;
        }

        MutableSession copy() {
            MutableSession copy = new MutableSession(
                    profileIdentity,
                    playerId,
                    resumeTokenHash);
            copy.activeResumeToken = activeResumeToken;
            copy.activeConnectionId = activeConnectionId;
            copy.connected = connected;
            copy.connectionGeneration = connectionGeneration;
            copy.acceptedRelayFrames = acceptedRelayFrames;
            copy.lastConnectionSequence = lastConnectionSequence;
            copy.connectedAtMillis = connectedAtMillis;
            copy.lastSeenMillis = lastSeenMillis;
            copy.lastEvent = lastEvent;
            return copy;
        }
    }
}
