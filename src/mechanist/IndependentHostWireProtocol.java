package mechanist;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Minimal authenticated wire exchange for the independent-host relay.
 *
 * This protocol proves that a client actually participates in identity,
 * manifest, acquisition, restart, and integrity phases before relay access is
 * granted. Access is explicitly RELAY_ONLY; no world snapshot or gameplay
 * authority is exposed by this class.
 */
final class IndependentHostWireProtocol {
    static final String VERSION = "independent-host-wire-1";
    private static final String PREFIX = "MECH";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecureHandshakeStateMachine handshake;
    private final SecureHandshakeStateMachine.ModManifestRecord manifest;
    private final String manifestFingerprint;
    private String profileIdentity = "";
    private boolean accessGranted;
    private boolean disconnected;

    IndependentHostWireProtocol(String sessionId) {
        this.handshake = new SecureHandshakeStateMachine(sessionId);
        this.manifest = new SecureHandshakeStateMachine.ModManifestRecord(
                "mechanist-base-" + BuildIdentityAuthority.version(),
                0L,
                List.of(),
                Instant.EPOCH);
        this.manifestFingerprint = manifest.stableFingerprint();
    }

    String helloLine() {
        return line("HELLO", VERSION, handshake.sessionId(), BuildIdentityAuthority.version(), "RELAY_ONLY");
    }

    Result accept(String rawLine) {
        if (disconnected) return Result.disconnect("session already disconnected");
        if (rawLine == null || rawLine.isBlank()) return disconnect("blank protocol frame");
        if (rawLine.length() > 4096) return disconnect("protocol frame exceeded 4096 characters");
        String[] fields = rawLine.split("\\|", -1);
        if (fields.length < 2 || !PREFIX.equals(fields[0])) {
            return disconnect("non-protocol frame arrived before relay access");
        }
        String command = fields[1].trim().toUpperCase(Locale.ROOT);
        try {
            return switch (command) {
                case "IDENTITY" -> acceptIdentity(fields);
                case "ACQUIRED" -> acceptAcquired(fields);
                case "RESTARTED" -> acceptRestarted(fields);
                case "CHALLENGE_RESPONSE" -> acceptChallengeResponse(fields);
                case "PING" -> Result.responses(line("PONG", handshake.sessionId(), handshake.phase().name()));
                default -> disconnect("unknown protocol command " + command);
            };
        } catch (RuntimeException failure) {
            return disconnect(failure.getMessage() == null
                    ? failure.getClass().getSimpleName()
                    : failure.getMessage());
        }
    }

    boolean relayAccessGranted() {
        return accessGranted
                && !disconnected
                && handshake.phase() == SecureHandshakeStateMachine.Phase.ACCESS_GRANTED;
    }

    String sessionId() {
        return handshake.sessionId();
    }

    String profileIdentity() {
        return profileIdentity;
    }

    SecureHandshakeStateMachine.Phase phase() {
        return handshake.phase();
    }

    String manifestFingerprint() {
        return manifestFingerprint;
    }

    void disconnect() {
        if (disconnected) return;
        disconnected = true;
        handshake.disconnect();
    }

    String auditSummary() {
        return "authority=" + VERSION
                + " session=" + handshake.sessionId()
                + " phase=" + handshake.phase()
                + " identity=" + (profileIdentity.isBlank() ? "unverified" : "verified")
                + " relayAccess=" + relayAccessGranted()
                + " worldAuthority=false";
    }

    private Result acceptIdentity(String[] fields) {
        requirePhase(SecureHandshakeStateMachine.Phase.IDENTITY_VERIFICATION);
        requireCount(fields, 3, "IDENTITY");
        profileIdentity = requireOpaqueIdentity(fields[2]);
        handshake.markIdentityVerified();
        handshake.beginManifestDelivery();
        handshake.deliverManifest(manifest);
        handshake.beginAcquisitionAndSync();
        return Result.responses(line(
                "MANIFEST",
                manifest.manifestId(),
                manifestFingerprint,
                Long.toString(manifest.sequenceId()),
                Integer.toString(manifest.entries().size())));
    }

    private Result acceptAcquired(String[] fields) {
        requirePhase(SecureHandshakeStateMachine.Phase.ACQUISITION_AND_SYNC);
        requireCount(fields, 3, "ACQUIRED");
        String received = requireToken(fields[2], "manifest fingerprint").toLowerCase(Locale.ROOT);
        if (!manifestFingerprint.equals(received)) {
            throw new IllegalArgumentException("client acquired fingerprint does not match the server manifest");
        }
        handshake.markModFilesAcquired();
        handshake.markFolderLayoutVerified();
        handshake.beginClientHotRestart();
        return Result.responses(line("RESTART_REQUIRED", manifestFingerprint));
    }

    private Result acceptRestarted(String[] fields) {
        requirePhase(SecureHandshakeStateMachine.Phase.CLIENT_HOT_RESTART);
        requireCount(fields, 3, "RESTARTED");
        String mounted = requireToken(fields[2], "mounted fingerprint").toLowerCase(Locale.ROOT);
        if (!manifestFingerprint.equals(mounted)) {
            throw new IllegalArgumentException("mounted fingerprint does not match the delivered manifest");
        }
        handshake.markClientHotRestartCompleted(mounted);
        String salt = randomHex(16);
        SecureHandshakeStateMachine.IntegrityChallenge challenge =
                handshake.issueIntegrityChallenge(salt);
        return Result.responses(line(
                "CHALLENGE",
                challenge.saltHex(),
                manifestFingerprint,
                mounted));
    }

    private Result acceptChallengeResponse(String[] fields) {
        requirePhase(SecureHandshakeStateMachine.Phase.INTEGRITY_CHALLENGE);
        requireCount(fields, 3, "CHALLENGE_RESPONSE");
        String digest = requireToken(fields[2], "challenge response").toLowerCase(Locale.ROOT);
        if (!handshake.verifyIntegrityChallengeResponse(digest)) {
            throw new IllegalArgumentException("integrity challenge response was rejected");
        }
        handshake.beginLiveWorldInitialization();
        handshake.grantAccess();
        accessGranted = true;
        return Result.responses(line(
                "ACCESS",
                "RELAY_ONLY",
                handshake.sessionId(),
                BuildIdentityAuthority.version(),
                profileIdentity));
    }

    private void requirePhase(SecureHandshakeStateMachine.Phase expected) {
        if (handshake.phase() != expected) {
            throw new IllegalStateException(
                    "command is invalid during " + handshake.phase() + "; expected " + expected);
        }
    }

    private Result disconnect(String reason) {
        disconnect();
        return Result.disconnect(reason == null || reason.isBlank() ? "protocol rejected" : reason);
    }

    private static void requireCount(String[] fields, int expected, String command) {
        if (fields.length != expected) {
            throw new IllegalArgumentException(command + " requires " + (expected - 2) + " value field(s)");
        }
    }

    private static String requireOpaqueIdentity(String value) {
        String token = requireToken(value, "profile identity");
        if (token.length() < 8 || token.length() > 256) {
            throw new IllegalArgumentException("profile identity must be 8-256 characters");
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '.' || c == '_' || c == ':' || c == '-')) {
                throw new IllegalArgumentException("profile identity contains an unsafe character");
            }
        }
        return token;
    }

    private static String requireToken(String value, String label) {
        String token = Objects.requireNonNullElse(value, "").trim();
        if (token.isBlank()) throw new IllegalArgumentException(label + " must not be blank");
        if (token.length() > 4096) throw new IllegalArgumentException(label + " is too long");
        if (token.indexOf('|') >= 0 || token.indexOf('\n') >= 0 || token.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(label + " contains a protocol separator");
        }
        return token;
    }

    private static String randomHex(int bytes) {
        byte[] data = new byte[Math.max(8, Math.min(64, bytes))];
        RANDOM.nextBytes(data);
        return HexFormat.of().formatHex(data);
    }

    private static String line(String... fields) {
        ArrayList<String> clean = new ArrayList<>(fields.length + 1);
        clean.add(PREFIX);
        for (String field : fields) clean.add(requireToken(field, "protocol field"));
        return String.join("|", clean);
    }

    record Result(List<String> responses, boolean disconnect, String reason) {
        Result {
            responses = List.copyOf(Objects.requireNonNullElse(responses, List.of()));
            reason = Objects.requireNonNullElse(reason, "");
        }

        static Result responses(String... lines) {
            return new Result(List.of(lines), false, "");
        }

        static Result disconnect(String reason) {
            return new Result(List.of(line("DENIED", sanitizeReason(reason))), true, reason);
        }

        private static String sanitizeReason(String reason) {
            String text = Objects.requireNonNullElse(reason, "protocol rejected")
                    .replace('|', '/')
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .trim();
            return text.isBlank() ? "protocol rejected" : text.substring(0, Math.min(256, text.length()));
        }
    }
}
