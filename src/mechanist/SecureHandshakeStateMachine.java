package mechanist;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Authoritative connection handshake lifecycle.
 * Live world bytes are forbidden until the client has acquired assets, hot-restarted
 * its local runtime, and passed the post-restart integrity challenge.
 */
final class SecureHandshakeStateMachine {
    enum Phase {
        IDENTITY_VERIFICATION(0),
        MANIFEST_DELIVERY(1),
        ACQUISITION_AND_SYNC(2),
        CLIENT_HOT_RESTART(3),
        INTEGRITY_CHALLENGE(4),
        LIVE_WORLD_INITIALIZATION(5),
        ACCESS_GRANTED(6),
        DISCONNECTED(99);

        private final int wireId;
        Phase(int wireId) { this.wireId = wireId; }
        int wireId() { return wireId; }

        boolean allowsWorldSerialization() {
            return this == LIVE_WORLD_INITIALIZATION || this == ACCESS_GRANTED;
        }

        boolean preLiveAcquisition() {
            return switch (this) {
                case MANIFEST_DELIVERY, ACQUISITION_AND_SYNC, CLIENT_HOT_RESTART, INTEGRITY_CHALLENGE -> true;
                case IDENTITY_VERIFICATION, LIVE_WORLD_INITIALIZATION, ACCESS_GRANTED, DISCONNECTED -> false;
            };
        }
    }

    sealed interface HandshakeState permits HandshakeState.IdentityVerification,
            HandshakeState.ManifestDelivery,
            HandshakeState.AcquisitionAndSync,
            HandshakeState.ClientHotRestart,
            HandshakeState.IntegrityChallenge,
            HandshakeState.LiveWorldInitialization,
            HandshakeState.AccessGranted,
            HandshakeState.Disconnected {
        Phase phase();
        int wireId();
        String label();
        default boolean worldBytesAllowed() { return phase().allowsWorldSerialization(); }

        record IdentityVerification(String sessionId) implements HandshakeState {
            public Phase phase() { return Phase.IDENTITY_VERIFICATION; }
            public int wireId() { return phase().wireId(); }
            public String label() { return "STATE 0: IDENTITY_VERIFICATION"; }
        }
        record ManifestDelivery(String sessionId) implements HandshakeState {
            public Phase phase() { return Phase.MANIFEST_DELIVERY; }
            public int wireId() { return phase().wireId(); }
            public String label() { return "STATE 1: MANIFEST_DELIVERY"; }
        }
        record AcquisitionAndSync(String sessionId) implements HandshakeState {
            public Phase phase() { return Phase.ACQUISITION_AND_SYNC; }
            public int wireId() { return phase().wireId(); }
            public String label() { return "STATE 2: ACQUISITION_AND_SYNC"; }
        }
        record ClientHotRestart(String sessionId) implements HandshakeState {
            public Phase phase() { return Phase.CLIENT_HOT_RESTART; }
            public int wireId() { return phase().wireId(); }
            public String label() { return "STATE 3: CLIENT_HOT_RESTART"; }
        }
        record IntegrityChallenge(String sessionId) implements HandshakeState {
            public Phase phase() { return Phase.INTEGRITY_CHALLENGE; }
            public int wireId() { return phase().wireId(); }
            public String label() { return "STATE 4: INTEGRITY_CHALLENGE"; }
        }
        record LiveWorldInitialization(String sessionId) implements HandshakeState {
            public Phase phase() { return Phase.LIVE_WORLD_INITIALIZATION; }
            public int wireId() { return phase().wireId(); }
            public String label() { return "STATE 5: LIVE_WORLD_INITIALIZATION"; }
        }
        record AccessGranted(String sessionId) implements HandshakeState {
            public Phase phase() { return Phase.ACCESS_GRANTED; }
            public int wireId() { return phase().wireId(); }
            public String label() { return "STATE 6: ACCESS_GRANTED"; }
        }
        record Disconnected(String sessionId) implements HandshakeState {
            public Phase phase() { return Phase.DISCONNECTED; }
            public int wireId() { return phase().wireId(); }
            public String label() { return "DISCONNECTED"; }
        }
    }

    record ModManifestEntry(String modId, String version, String sha256, long byteSize) {
        ModManifestEntry {
            modId = requireToken(modId, "modId");
            version = requireToken(version, "version");
            sha256 = sha256 == null ? "" : sha256.trim().toLowerCase();
            if (byteSize < 0) throw new IllegalArgumentException("byteSize must be non-negative");
        }
    }

    record ModManifestRecord(String manifestId, long sequenceId, List<ModManifestEntry> entries, Instant createdAt) {
        ModManifestRecord {
            manifestId = requireToken(manifestId, "manifestId");
            if (sequenceId < 0) throw new IllegalArgumentException("sequenceId must be non-negative");
            entries = List.copyOf(Objects.requireNonNullElse(entries, List.of()));
            createdAt = createdAt == null ? Instant.now() : createdAt;
        }

        String stableFingerprint() {
            StringBuilder sb = new StringBuilder(manifestId).append('|').append(sequenceId);
            for (ModManifestEntry entry : entries) {
                sb.append('|').append(entry.modId()).append('@').append(entry.version()).append('=').append(entry.sha256()).append(':').append(entry.byteSize());
            }
            return sha256Hex(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
    }

    record IntegrityChallenge(String saltHex, String expectedDigestHex, Instant issuedAt) {
        IntegrityChallenge {
            saltHex = requireToken(saltHex, "saltHex").toLowerCase();
            expectedDigestHex = requireToken(expectedDigestHex, "expectedDigestHex").toLowerCase();
            issuedAt = issuedAt == null ? Instant.now() : issuedAt;
        }
    }

    sealed interface WorldSnapshotResult permits WorldSnapshotResult.Rejected, WorldSnapshotResult.Accepted {
        record Rejected(String sessionId, Phase phase, String reason) implements WorldSnapshotResult { }
        record Accepted(String sessionId, Phase phase, byte[] payload) implements WorldSnapshotResult { }
    }

    interface WorldSnapshotSerializer { byte[] serializeSnapshot(String sessionId) throws Exception; }

    private final String sessionId;
    private final AtomicReference<Phase> phase = new AtomicReference<>(Phase.IDENTITY_VERIFICATION);
    private volatile boolean identityVerified;
    private volatile boolean modFilesAcquired;
    private volatile boolean folderLayoutVerified;
    private volatile boolean clientHotRestartCompleted;
    private volatile boolean integrityChallengePassed;
    private volatile Instant liveInitializationStartedAt;
    private volatile ModManifestRecord currentManifest;
    private volatile IntegrityChallenge currentChallenge;
    private volatile String mountedModFingerprint = "";

    SecureHandshakeStateMachine(String sessionId) {
        this.sessionId = sessionId == null || sessionId.isBlank() ? UUID.randomUUID().toString() : sessionId;
    }

    String sessionId() { return sessionId; }
    Phase phase() { return phase.get(); }
    boolean identityVerified() { return identityVerified; }
    boolean clientHotRestartCompleted() { return clientHotRestartCompleted; }
    String mountedModFingerprint() { return mountedModFingerprint; }
    ModManifestRecord currentManifest() { return currentManifest; }
    IntegrityChallenge currentChallenge() { return currentChallenge; }

    HandshakeState state() {
        return switch (phase.get()) {
            case IDENTITY_VERIFICATION -> new HandshakeState.IdentityVerification(sessionId);
            case MANIFEST_DELIVERY -> new HandshakeState.ManifestDelivery(sessionId);
            case ACQUISITION_AND_SYNC -> new HandshakeState.AcquisitionAndSync(sessionId);
            case CLIENT_HOT_RESTART -> new HandshakeState.ClientHotRestart(sessionId);
            case INTEGRITY_CHALLENGE -> new HandshakeState.IntegrityChallenge(sessionId);
            case LIVE_WORLD_INITIALIZATION -> new HandshakeState.LiveWorldInitialization(sessionId);
            case ACCESS_GRANTED -> new HandshakeState.AccessGranted(sessionId);
            case DISCONNECTED -> new HandshakeState.Disconnected(sessionId);
        };
    }

    void markIdentityVerified() { identityVerified = true; }

    void beginManifestDelivery() {
        if (!identityVerified) throw new IllegalStateException("Identity must be verified before manifest delivery for " + sessionId);
        transition(Phase.IDENTITY_VERIFICATION, Phase.MANIFEST_DELIVERY);
    }

    void deliverManifest(ModManifestRecord manifest) {
        if (phase.get() != Phase.MANIFEST_DELIVERY) throw new IllegalStateException("Cannot deliver manifest while in " + phase.get());
        this.currentManifest = Objects.requireNonNull(manifest, "manifest");
    }

    void beginAcquisitionAndSync() { transition(Phase.MANIFEST_DELIVERY, Phase.ACQUISITION_AND_SYNC); }

    void markModFilesAcquired() { modFilesAcquired = true; }
    void markFolderLayoutVerified() { folderLayoutVerified = true; }

    void beginClientHotRestart() {
        if (!modFilesAcquired || !folderLayoutVerified) {
            throw new IllegalStateException("Cannot enter client hot restart before mod acquisition and folder verification complete for " + sessionId);
        }
        transition(Phase.ACQUISITION_AND_SYNC, Phase.CLIENT_HOT_RESTART);
    }

    void markClientHotRestartCompleted(String mountedFingerprint) {
        if (phase.get() != Phase.CLIENT_HOT_RESTART) throw new IllegalStateException("Client hot restart completion arrived while in " + phase.get());
        mountedModFingerprint = requireToken(mountedFingerprint, "mountedFingerprint").toLowerCase();
        clientHotRestartCompleted = true;
        transition(Phase.CLIENT_HOT_RESTART, Phase.INTEGRITY_CHALLENGE);
    }

    IntegrityChallenge issueIntegrityChallenge(String saltHex) {
        if (phase.get() != Phase.INTEGRITY_CHALLENGE || !clientHotRestartCompleted) {
            throw new IllegalStateException("Cannot issue integrity challenge before completed hot restart for " + sessionId);
        }
        String salt = requireToken(saltHex, "saltHex").toLowerCase();
        String manifestFingerprint = currentManifest == null ? "no-manifest" : currentManifest.stableFingerprint();
        String expected = computeIntegrityDigest(salt, manifestFingerprint, mountedModFingerprint);
        currentChallenge = new IntegrityChallenge(salt, expected, Instant.now());
        return currentChallenge;
    }

    boolean verifyIntegrityChallengeResponse(String clientDigestHex) {
        if (currentChallenge == null) throw new IllegalStateException("No integrity challenge has been issued for " + sessionId);
        boolean passed = currentChallenge.expectedDigestHex().equalsIgnoreCase(requireToken(clientDigestHex, "clientDigestHex"));
        if (passed) integrityChallengePassed = true;
        return passed;
    }

    void markIntegrityChallengePassed() { integrityChallengePassed = true; }

    void beginLiveWorldInitialization() {
        if (!modFilesAcquired || !folderLayoutVerified || !clientHotRestartCompleted || !integrityChallengePassed) {
            throw new IllegalStateException("World initialization denied until acquisition, layout verification, hot restart, and integrity challenge pass for " + sessionId);
        }
        transition(Phase.INTEGRITY_CHALLENGE, Phase.LIVE_WORLD_INITIALIZATION);
        liveInitializationStartedAt = Instant.now();
    }

    void grantAccess() { transition(Phase.LIVE_WORLD_INITIALIZATION, Phase.ACCESS_GRANTED); }

    CompletableFuture<WorldSnapshotResult> serializeWorldWhenAllowed(Executor executor, WorldSnapshotSerializer serializer) {
        Objects.requireNonNull(executor, "executor");
        Objects.requireNonNull(serializer, "serializer");
        Phase current = phase.get();
        if (!current.allowsWorldSerialization()) {
            return CompletableFuture.completedFuture(new WorldSnapshotResult.Rejected(sessionId, current, "world-state serialization is forbidden before LIVE_WORLD_INITIALIZATION"));
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] payload = serializer.serializeSnapshot(sessionId);
                if (payload == null) payload = ("empty-world-snapshot:" + sessionId).getBytes(StandardCharsets.UTF_8);
                phase.compareAndSet(Phase.LIVE_WORLD_INITIALIZATION, Phase.ACCESS_GRANTED);
                return new WorldSnapshotResult.Accepted(sessionId, phase.get(), payload);
            } catch (Exception ex) {
                return new WorldSnapshotResult.Rejected(sessionId, phase.get(), "snapshot serialization failed: " + ex.getMessage());
            }
        }, executor);
    }

    void disconnect() { phase.set(Phase.DISCONNECTED); }

    Instant liveInitializationStartedAt() { return liveInitializationStartedAt; }

    static String computeIntegrityDigest(String saltHex, String manifestFingerprint, String mountedModFingerprint) {
        String material = requireToken(saltHex, "saltHex").toLowerCase()
                + "|" + requireToken(manifestFingerprint, "manifestFingerprint").toLowerCase()
                + "|" + requireToken(mountedModFingerprint, "mountedModFingerprint").toLowerCase();
        return sha256Hex(material.getBytes(StandardCharsets.UTF_8));
    }

    static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(Objects.requireNonNull(data, "data")));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private void transition(Phase expected, Phase next) {
        if (!phase.compareAndSet(expected, next)) {
            throw new IllegalStateException("Illegal handshake transition for " + sessionId + ": expected " + expected + " but was " + phase.get() + " while moving to " + next);
        }
    }

    private static String requireToken(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        String clean = value.trim();
        if (clean.length() > 4096) throw new IllegalArgumentException(name + " is too long");
        return clean;
    }
}
