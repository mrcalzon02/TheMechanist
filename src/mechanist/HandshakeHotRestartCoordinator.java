package mechanist;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** Coordinates post-acquisition client hot restart and server-side world-stream gating. */
final class HandshakeHotRestartCoordinator {
    private final EngineHotLauncher hotLauncher;

    HandshakeHotRestartCoordinator(EngineHotLauncher hotLauncher) {
        this.hotLauncher = Objects.requireNonNull(hotLauncher, "hotLauncher");
    }

    CompletableFuture<EngineHotLauncher.HotRestartResult> startClientHotRestart(EngineHotLauncher.HotRestartRequest request) {
        return hotLauncher.launchAsync(request);
    }

    static final class ServerGate {
        private final SecureHandshakeStateMachine stateMachine;
        private final SecureRandom secureRandom = new SecureRandom();
        private final AtomicBoolean worldStreamingStarted = new AtomicBoolean(false);

        ServerGate(SecureHandshakeStateMachine stateMachine) {
            this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        }

        HandshakeNetworkFrame onFrame(HandshakeNetworkFrame frame) {
            Objects.requireNonNull(frame, "frame");
            try {
                if (frame instanceof HandshakeNetworkFrame.IdentityProof proof) return handleIdentity(proof);
                if (frame instanceof HandshakeNetworkFrame.AcquisitionComplete complete) return handleAcquisitionComplete(complete);
                if (frame instanceof HandshakeNetworkFrame.HotRestartComplete complete) return handleHotRestartComplete(complete);
                if (frame instanceof HandshakeNetworkFrame.IntegrityChallengeResponse response) return handleChallengeResponse(response);
                if (frame instanceof HandshakeNetworkFrame.HotRestartStarted started) return new HandshakeNetworkFrame.HotRestartStarted(started.sessionId(), Instant.now().toEpochMilli());
                if (frame instanceof HandshakeNetworkFrame.ManifestPayload) return reject("MANIFEST_DELIVERY", "client may not author server manifest payloads");
                if (frame instanceof HandshakeNetworkFrame.IntegrityChallengeIssued) return reject("INTEGRITY_CHALLENGE", "client may not issue integrity challenges");
                if (frame instanceof HandshakeNetworkFrame.WorldSnapshotReady) return reject("LIVE_WORLD_INITIALIZATION", "client may not author world snapshot notifications");
                if (frame instanceof HandshakeNetworkFrame.AccessGranted) return reject("ACCESS_GRANTED", "client may not grant access");
                if (frame instanceof HandshakeNetworkFrame.Rejected rejected) return rejected;
                return reject(stateMachine.phase().name(), "unknown handshake frame");
            } catch (RuntimeException ex) {
                return reject(stateMachine.phase().name(), ex.getMessage());
            }
        }

        HandshakeNetworkFrame.ManifestPayload serverManifestFrame(SecureHandshakeStateMachine.ModManifestRecord manifest) {
            if (stateMachine.phase() == SecureHandshakeStateMachine.Phase.IDENTITY_VERIFICATION) stateMachine.markIdentityVerified();
            if (stateMachine.phase() == SecureHandshakeStateMachine.Phase.IDENTITY_VERIFICATION) stateMachine.beginManifestDelivery();
            if (stateMachine.phase() != SecureHandshakeStateMachine.Phase.MANIFEST_DELIVERY) {
                throw new IllegalStateException("server manifest can only be emitted during MANIFEST_DELIVERY, not " + stateMachine.phase());
            }
            stateMachine.deliverManifest(manifest);
            return new HandshakeNetworkFrame.ManifestPayload(stateMachine.sessionId(), manifest.stableFingerprint(), Instant.now().toEpochMilli());
        }

        CompletableFuture<SecureHandshakeStateMachine.WorldSnapshotResult> serializeWorldAfterChallenge(Executor executor, SecureHandshakeStateMachine.WorldSnapshotSerializer serializer) {
            if (stateMachine.phase() != SecureHandshakeStateMachine.Phase.LIVE_WORLD_INITIALIZATION || !stateMachine.clientHotRestartCompleted()) {
                return CompletableFuture.completedFuture(new SecureHandshakeStateMachine.WorldSnapshotResult.Rejected(stateMachine.sessionId(), stateMachine.phase(), "server gate blocked world bytes until client hot restart and integrity challenge complete"));
            }
            if (!worldStreamingStarted.compareAndSet(false, true)) {
                return CompletableFuture.completedFuture(new SecureHandshakeStateMachine.WorldSnapshotResult.Rejected(stateMachine.sessionId(), stateMachine.phase(), "world streaming already started for this session"));
            }
            return stateMachine.serializeWorldWhenAllowed(executor, serializer);
        }

        private HandshakeNetworkFrame handleIdentity(HandshakeNetworkFrame.IdentityProof proof) {
            if (stateMachine.phase() != SecureHandshakeStateMachine.Phase.IDENTITY_VERIFICATION) return reject("IDENTITY_VERIFICATION", "identity proof arrived after identity phase");
            if (proof.identityToken().length() < 8) return reject("IDENTITY_VERIFICATION", "identity token too short");
            stateMachine.markIdentityVerified();
            stateMachine.beginManifestDelivery();
            return new HandshakeNetworkFrame.ManifestPayload(stateMachine.sessionId(), "manifest-pending", Instant.now().toEpochMilli());
        }

        private HandshakeNetworkFrame handleAcquisitionComplete(HandshakeNetworkFrame.AcquisitionComplete complete) {
            if (stateMachine.phase() == SecureHandshakeStateMachine.Phase.MANIFEST_DELIVERY) stateMachine.beginAcquisitionAndSync();
            if (stateMachine.phase() != SecureHandshakeStateMachine.Phase.ACQUISITION_AND_SYNC) return reject("ACQUISITION_AND_SYNC", "acquisition completion arrived during " + stateMachine.phase());
            if (!complete.filesAcquired() || !complete.layoutVerified()) return reject("ACQUISITION_AND_SYNC", "client acquisition or layout verification failed");
            stateMachine.markModFilesAcquired();
            stateMachine.markFolderLayoutVerified();
            stateMachine.beginClientHotRestart();
            return new HandshakeNetworkFrame.HotRestartStarted(stateMachine.sessionId(), Instant.now().toEpochMilli());
        }

        private HandshakeNetworkFrame handleHotRestartComplete(HandshakeNetworkFrame.HotRestartComplete complete) {
            stateMachine.markClientHotRestartCompleted(complete.mountedFingerprint());
            byte[] salt = new byte[16];
            secureRandom.nextBytes(salt);
            var challenge = stateMachine.issueIntegrityChallenge(HexFormat.of().formatHex(salt));
            return new HandshakeNetworkFrame.IntegrityChallengeIssued(stateMachine.sessionId(), challenge.saltHex(), challenge.expectedDigestHex(), Instant.now().toEpochMilli());
        }

        private HandshakeNetworkFrame handleChallengeResponse(HandshakeNetworkFrame.IntegrityChallengeResponse response) {
            if (!stateMachine.verifyIntegrityChallengeResponse(response.digestHex())) return reject("INTEGRITY_CHALLENGE", "client digest did not match post-restart mounted mod state");
            stateMachine.beginLiveWorldInitialization();
            return new HandshakeNetworkFrame.WorldSnapshotReady(stateMachine.sessionId(), 0, Instant.now().toEpochMilli());
        }

        private HandshakeNetworkFrame.Rejected reject(String phase, String reason) {
            return new HandshakeNetworkFrame.Rejected(stateMachine.sessionId(), phase, reason, Instant.now().toEpochMilli());
        }
    }
}
