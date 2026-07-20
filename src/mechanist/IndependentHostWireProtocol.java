package mechanist;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Authenticated wire exchange for the independent-host relay, hosted-session
 * ledger, and the narrowly scoped authoritative wait command.
 *
 * Client identity, manifest acquisition, restart, and integrity must complete
 * before a server-owned remote session is attached. The transport access class
 * remains RELAY_ONLY. A separate authenticated WORLD_COMMAND control frame may
 * invoke only WaitCommand; movement, maps, interaction, combat, inventory, and
 * general remote gameplay authority remain closed.
 */
final class IndependentHostWireProtocol {
    static final String VERSION = "independent-host-wire-6";
    private static final String PREFIX = "MECH";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecureHandshakeStateMachine handshake;
    private final SecureHandshakeStateMachine.ModManifestRecord manifest;
    private final String manifestFingerprint;
    private final RemoteSessionLedgerAuthority sessionLedger;
    private final IndependentHostTurnAuthority turnAuthority;
    private String profileIdentity = "";
    private String requestedResumeToken = "";
    private RemoteSessionLedgerAuthority.Attachment sessionAttachment;
    private boolean accessGranted;
    private boolean disconnected;

    IndependentHostWireProtocol(String sessionId) {
        this(
                sessionId,
                new RemoteSessionLedgerAuthority("standalone-" + sessionId),
                new IndependentHostTurnAuthority("standalone-" + sessionId));
    }

    IndependentHostWireProtocol(
            String sessionId,
            RemoteSessionLedgerAuthority sessionLedger
    ) {
        this(
                sessionId,
                sessionLedger,
                new IndependentHostTurnAuthority(sessionLedger.worldId()));
    }

    IndependentHostWireProtocol(
            String sessionId,
            RemoteSessionLedgerAuthority sessionLedger,
            IndependentHostTurnAuthority turnAuthority
    ) {
        this.handshake = new SecureHandshakeStateMachine(sessionId);
        this.sessionLedger = Objects.requireNonNull(sessionLedger, "sessionLedger");
        this.turnAuthority = Objects.requireNonNull(turnAuthority, "turnAuthority");
        this.manifest = new SecureHandshakeStateMachine.ModManifestRecord(
                "mechanist-base-" + BuildIdentityAuthority.version(),
                0L,
                List.of(),
                Instant.EPOCH);
        this.manifestFingerprint = manifest.stableFingerprint();
    }

    String helloLine() {
        return line("HELLO", VERSION, handshake.sessionId(),
                BuildIdentityAuthority.version(), "RELAY_ONLY");
    }

    Result accept(String rawLine) {
        if (disconnected) return Result.disconnect("session already disconnected");
        if (rawLine == null || rawLine.isBlank()) return deny("blank protocol frame");
        if (rawLine.length() > 4096) return deny("protocol frame exceeded 4096 characters");
        String[] fields = rawLine.split("\\|", -1);
        if (fields.length < 2 || !PREFIX.equals(fields[0])) {
            return deny("non-protocol frame arrived before relay access");
        }
        String command = fields[1].trim().toUpperCase(Locale.ROOT);
        try {
            return switch (command) {
                case "IDENTITY" -> acceptIdentity(fields);
                case "ACQUIRED" -> acceptAcquired(fields);
                case "RESTARTED" -> acceptRestarted(fields);
                case "CHALLENGE_RESPONSE" -> acceptChallengeResponse(fields);
                case "SESSION_STATUS" -> acceptSessionStatus(fields);
                case "SESSION_COMMAND" -> acceptSessionCommand(fields);
                case "SESSION_ROSTER" -> acceptSessionRoster(fields);
                case "WORLD_COMMAND" -> acceptWorldCommand(fields);
                case "PING" -> acceptPing(fields);
                default -> deny("unknown protocol command " + command);
            };
        } catch (RuntimeException failure) {
            return deny(failure.getMessage() == null
                    ? failure.getClass().getSimpleName()
                    : failure.getMessage());
        }
    }

    boolean relayAccessGranted() {
        return accessGranted
                && sessionAttachment != null
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

    RemoteSessionLedgerAuthority.Attachment sessionAttachment() {
        return sessionAttachment;
    }

    RemoteSessionLedgerAuthority.SessionSnapshot sessionSnapshot() {
        return sessionAttachment == null ? null : sessionLedger.snapshot(sessionAttachment);
    }

    IndependentHostTurnAuthority.TurnSnapshot turnSnapshot() {
        return sessionAttachment == null
                ? null
                : turnAuthority.snapshotForPlayer(sessionAttachment.playerId());
    }

    RemoteSessionLedgerAuthority.SessionSnapshot noteRelayFrameAccepted(long sequence) {
        if (!relayAccessGranted()) {
            throw new IllegalStateException(
                    "relay frame arrived before a server-owned session was attached");
        }
        return sessionLedger.noteRelayFrameAccepted(sessionAttachment, sequence);
    }

    void disconnect() {
        disconnect("transport closed");
    }

    void disconnect(String reason) {
        if (disconnected) return;
        disconnected = true;
        if (sessionAttachment != null) {
            turnAuthority.disconnectPlayer(sessionAttachment.playerId());
        }
        sessionLedger.disconnect(sessionAttachment, reason);
        handshake.disconnect();
    }

    String auditSummary() {
        RemoteSessionLedgerAuthority.SessionSnapshot snapshot = sessionSnapshot();
        IndependentHostTurnAuthority.TurnSnapshot turn = turnSnapshot();
        return "authority=" + VERSION
                + " session=" + handshake.sessionId()
                + " phase=" + handshake.phase()
                + " identity=" + (profileIdentity.isBlank() ? "unverified" : "verified")
                + " relayAccess=" + relayAccessGranted()
                + " player=" + (snapshot == null ? "none" : snapshot.playerId())
                + " generation=" + (snapshot == null ? 0 : snapshot.connectionGeneration())
                + " relayFrames=" + (snapshot == null ? 0 : snapshot.acceptedRelayFrames())
                + " hostedCommands=" + (snapshot == null ? 0 : snapshot.acceptedHostedCommands())
                + " hostedRoster=true"
                + " rosterBroadcasts=true"
                + " rosterVisibility=connected-only"
                + " networkWaitAuthority=true"
                + " playerTurn=" + (turn == null ? 0 : turn.playerTurn())
                + " worldTurn=" + (turn == null ? 0 : turn.worldTurn())
                + " movementAuthority=false"
                + " mapAuthority=false"
                + " gameplaySessionCertified=false";
    }

    private Result acceptIdentity(String[] fields) {
        requirePhase(SecureHandshakeStateMachine.Phase.IDENTITY_VERIFICATION);
        requireCountRange(fields, 3, 4, "IDENTITY");
        profileIdentity = requireOpaqueIdentity(fields[2]);
        requestedResumeToken = fields.length == 4
                ? requireResumeToken(fields[3])
                : "";
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
        String received = requireToken(fields[2], "manifest fingerprint")
                .toLowerCase(Locale.ROOT);
        if (!manifestFingerprint.equals(received)) {
            throw new IllegalArgumentException(
                    "client acquired fingerprint does not match the server manifest");
        }
        handshake.markModFilesAcquired();
        handshake.markFolderLayoutVerified();
        handshake.beginClientHotRestart();
        return Result.responses(line("RESTART_REQUIRED", manifestFingerprint));
    }

    private Result acceptRestarted(String[] fields) {
        requirePhase(SecureHandshakeStateMachine.Phase.CLIENT_HOT_RESTART);
        requireCount(fields, 3, "RESTARTED");
        String mounted = requireToken(fields[2], "mounted fingerprint")
                .toLowerCase(Locale.ROOT);
        if (!manifestFingerprint.equals(mounted)) {
            throw new IllegalArgumentException(
                    "mounted fingerprint does not match the delivered manifest");
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
        String digest = requireToken(fields[2], "challenge response")
                .toLowerCase(Locale.ROOT);
        if (!handshake.verifyIntegrityChallengeResponse(digest)) {
            throw new IllegalArgumentException(
                    "integrity challenge response was rejected");
        }
        sessionAttachment = sessionLedger.attach(
                profileIdentity,
                requestedResumeToken,
                handshake.sessionId());
        handshake.beginLiveWorldInitialization();
        handshake.grantAccess();
        accessGranted = true;
        String access = line(
                "ACCESS",
                "RELAY_ONLY",
                handshake.sessionId(),
                BuildIdentityAuthority.version(),
                profileIdentity,
                sessionAttachment.playerId(),
                sessionAttachment.resumeToken(),
                Long.toString(sessionAttachment.connectionGeneration()),
                Long.toString(sessionAttachment.snapshotVersion()),
                Boolean.toString(sessionAttachment.resumed()));
        return Result.responsesAndBroadcasts(
                List.of(access),
                hostedRosterLines(sessionLedger.hostedSessionSnapshot()));
    }

    private Result acceptSessionStatus(String[] fields) {
        requireCount(fields, 2, "SESSION_STATUS");
        requireRelayAccess("session status");
        return Result.responses(sessionSnapshotLine(sessionLedger.snapshot(sessionAttachment)));
    }

    private Result acceptSessionCommand(String[] fields) {
        requireCount(fields, 5, "SESSION_COMMAND");
        requireRelayAccess("hosted-session commands");
        long commandId = requireNonNegativeLong(fields[2], "hosted-session command id");
        String command = requireToken(fields[3], "hosted-session command");
        String value = requireToken(fields[4], "hosted-session command value");
        RemoteSessionLedgerAuthority.HostedSessionCommandResult result =
                sessionLedger.applyHostedSessionCommand(
                        sessionAttachment,
                        commandId,
                        command,
                        value);
        ArrayList<String> direct = new ArrayList<>();
        RemoteSessionLedgerAuthority.SessionSnapshot player = result.playerSnapshot();
        direct.add(line(
                "SESSION_COMMAND_ACCEPTED",
                Long.toString(result.commandId()),
                result.command(),
                result.value(),
                Long.toString(player.version()),
                Boolean.toString(player.ready()),
                player.presence(),
                player.chatState(),
                Long.toString(player.acceptedHostedCommands()),
                Long.toString(player.lastConnectionCommandId())));
        List<String> roster = hostedRosterLines(result.hostedSnapshot());
        direct.addAll(roster);
        return Result.responsesAndBroadcasts(direct, roster);
    }

    private Result acceptSessionRoster(String[] fields) {
        requireCount(fields, 2, "SESSION_ROSTER");
        requireRelayAccess("hosted-session roster");
        return Result.responses(hostedRosterLines(sessionLedger.hostedSessionSnapshot()));
    }

    private Result acceptWorldCommand(String[] fields) {
        requireCount(fields, 4, "WORLD_COMMAND");
        requireRelayAccess("authoritative wait command");
        long commandId = requireNonNegativeLong(fields[2], "world command id");
        String command = requireToken(fields[3], "world command")
                .toUpperCase(Locale.ROOT);
        if (!"WAIT".equals(command)) {
            throw new IllegalArgumentException(
                    "world command " + command
                            + " is unavailable; only authoritative wait is open");
        }
        IndependentHostTurnAuthority.TurnCommandResult result =
                turnAuthority.applyCommand(
                        sessionAttachment.playerId(),
                        sessionAttachment.connectionGeneration(),
                        commandId,
                        new WaitCommand(sessionAttachment.playerId()));
        IndependentHostTurnAuthority.TurnSnapshot snapshot = result.snapshot();
        return Result.responses(line(
                "WORLD_COMMAND_ACCEPTED",
                Long.toString(result.commandId()),
                result.command(),
                Long.toString(snapshot.version()),
                Long.toString(snapshot.connectionGeneration()),
                Long.toString(snapshot.lastConnectionCommandId()),
                Long.toString(snapshot.playerTurn()),
                Long.toString(snapshot.worldTurn()),
                Long.toString(snapshot.acceptedPlayerCommands()),
                Long.toString(snapshot.acceptedWorldCommands()),
                snapshot.lastEvent()));
    }

    private Result acceptPing(String[] fields) {
        requireCount(fields, 2, "PING");
        return Result.responses(line(
                "PONG",
                handshake.sessionId(),
                handshake.phase().name(),
                relayAccessGranted() ? "SESSION_ATTACHED" : "PRE_ACCESS"));
    }

    private String sessionSnapshotLine(
            RemoteSessionLedgerAuthority.SessionSnapshot snapshot
    ) {
        return line(
                "SESSION_SNAPSHOT",
                snapshot.playerId(),
                snapshot.worldId(),
                Long.toString(snapshot.version()),
                Boolean.toString(snapshot.connected()),
                Long.toString(snapshot.connectionGeneration()),
                Long.toString(snapshot.acceptedRelayFrames()),
                Long.toString(snapshot.lastConnectionSequence()),
                snapshot.lastEvent());
    }

    static List<String> hostedRosterLines(
            RemoteSessionLedgerAuthority.HostedSessionSnapshot snapshot
    ) {
        List<RemoteSessionLedgerAuthority.RosterEntry> visibleRoster =
                snapshot.roster().stream()
                        .filter(RemoteSessionLedgerAuthority.RosterEntry::connected)
                        .toList();
        ArrayList<String> lines = new ArrayList<>();
        lines.add(line(
                "HOSTED_ROSTER_BEGIN",
                Long.toString(snapshot.version()),
                snapshot.worldId(),
                Integer.toString(visibleRoster.size()),
                Integer.toString(visibleRoster.size()),
                Boolean.toString(snapshot.worldAuthority())));
        for (RemoteSessionLedgerAuthority.RosterEntry entry : visibleRoster) {
            lines.add(line(
                    "HOSTED_ROSTER_ENTRY",
                    entry.playerId(),
                    Boolean.toString(entry.connected()),
                    Long.toString(entry.connectionGeneration()),
                    Boolean.toString(entry.ready()),
                    entry.presence(),
                    entry.chatState(),
                    Long.toString(entry.acceptedHostedCommands()),
                    Long.toString(entry.lastSeenMillis())));
        }
        lines.add(line("HOSTED_ROSTER_END", Long.toString(snapshot.version())));
        return List.copyOf(lines);
    }

    private void requireRelayAccess(String capability) {
        if (!relayAccessGranted()) {
            throw new IllegalStateException(
                    capability + " is unavailable before relay access");
        }
    }

    private void requirePhase(SecureHandshakeStateMachine.Phase expected) {
        if (handshake.phase() != expected) {
            throw new IllegalStateException(
                    "command is invalid during " + handshake.phase()
                            + "; expected " + expected);
        }
    }

    private Result deny(String reason) {
        String use = reason == null || reason.isBlank()
                ? "protocol rejected"
                : reason;
        boolean attached = relayAccessGranted();
        disconnect(use);
        List<String> broadcasts = attached
                ? hostedRosterLines(sessionLedger.hostedSessionSnapshot())
                : List.of();
        return Result.disconnect(use, broadcasts);
    }

    private static void requireCount(
            String[] fields,
            int expected,
            String command
    ) {
        if (fields.length != expected) {
            throw new IllegalArgumentException(
                    command + " requires " + (expected - 2) + " value field(s)");
        }
    }

    private static void requireCountRange(
            String[] fields,
            int minimum,
            int maximum,
            String command
    ) {
        if (fields.length < minimum || fields.length > maximum) {
            throw new IllegalArgumentException(
                    command + " has an invalid field count");
        }
    }

    private static long requireNonNegativeLong(String value, String label) {
        String token = requireToken(value, label);
        try {
            long parsed = Long.parseLong(token);
            if (parsed < 0L) throw new NumberFormatException("negative");
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    label + " must be a non-negative integer");
        }
    }

    private static String requireOpaqueIdentity(String value) {
        String token = requireToken(value, "profile identity");
        if (token.length() < 8 || token.length() > 256) {
            throw new IllegalArgumentException(
                    "profile identity must be 8-256 characters");
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '.' || c == '_'
                    || c == ':' || c == '-')) {
                throw new IllegalArgumentException(
                        "profile identity contains an unsafe character");
            }
        }
        return token;
    }

    private static String requireResumeToken(String value) {
        String token = requireToken(value, "resume token")
                .toLowerCase(Locale.ROOT);
        if (!token.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException(
                    "resume token must be a 64-character hexadecimal token");
        }
        return token;
    }

    private static String requireToken(String value, String label) {
        String token = Objects.requireNonNullElse(value, "").trim();
        if (token.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }
        if (token.length() > 4096) {
            throw new IllegalArgumentException(label + " is too long");
        }
        if (token.indexOf('|') >= 0
                || token.indexOf('\n') >= 0
                || token.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(
                    label + " contains a protocol separator");
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
        for (String field : fields) {
            clean.add(requireToken(field, "protocol field"));
        }
        return String.join("|", clean);
    }

    record Result(
            List<String> responses,
            List<String> broadcasts,
            boolean disconnect,
            String reason
    ) {
        Result {
            responses = List.copyOf(
                    Objects.requireNonNullElse(responses, List.of()));
            broadcasts = List.copyOf(
                    Objects.requireNonNullElse(broadcasts, List.of()));
            reason = Objects.requireNonNullElse(reason, "");
        }

        static Result responses(String... lines) {
            return new Result(List.of(lines), List.of(), false, "");
        }

        static Result responses(List<String> lines) {
            return new Result(lines, List.of(), false, "");
        }

        static Result responsesAndBroadcasts(
                List<String> responses,
                List<String> broadcasts
        ) {
            return new Result(responses, broadcasts, false, "");
        }

        static Result disconnect(String reason) {
            return disconnect(reason, List.of());
        }

        static Result disconnect(String reason, List<String> broadcasts) {
            return new Result(
                    List.of(line("DENIED", sanitizeReason(reason))),
                    broadcasts,
                    true,
                    reason);
        }

        private static String sanitizeReason(String reason) {
            String text = Objects.requireNonNullElse(
                            reason,
                            "protocol rejected")
                    .replace('|', '/')
                    .replace('\n', ' ')
                    .replace('\r', ' ')
                    .trim();
            return text.isBlank()
                    ? "protocol rejected"
                    : text.substring(0, Math.min(256, text.length()));
        }
    }
}
