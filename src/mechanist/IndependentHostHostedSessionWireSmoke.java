package mechanist;

import java.util.Arrays;
import java.util.List;

/** Verifies authenticated hosted-session commands, connected-only roster responses, and peer-broadcast classification. */
final class IndependentHostHostedSessionWireSmoke {
    public static void main(String[] args) throws Exception {
        RemoteSessionLedgerAuthority ledger =
                new RemoteSessionLedgerAuthority("hosted-session-wire-smoke");
        IndependentHostTurnAuthority turns =
                new IndependentHostTurnAuthority("hosted-session-wire-smoke");
        IndependentHostWireProtocol alpha = null;
        IndependentHostWireProtocol beta = null;
        IndependentHostWireProtocol duplicate = null;
        IndependentHostWireProtocol resumed = null;
        try {
            alpha = new IndependentHostWireProtocol(
                    "wire-alpha-one",
                    ledger,
                    turns);
            Access alphaAccess = authenticate(alpha, "profile.alpha.1001", "");
            require(!alphaAccess.resumed, "new wire session incorrectly reported resume");

            IndependentHostWireProtocol.Result ready = alpha.accept(
                    "MECH|SESSION_COMMAND|0|READY|true");
            require(!ready.disconnect(), "READY command disconnected the session: " + ready.reason());
            String[] readyAccepted = requireCommand(
                    ready.responses().get(0), "SESSION_COMMAND_ACCEPTED");
            require(readyAccepted.length == 11,
                    "invalid SESSION_COMMAND_ACCEPTED field count: "
                            + Arrays.toString(readyAccepted));
            require("READY".equals(readyAccepted[3])
                            && "true".equals(readyAccepted[4])
                            && "true".equals(readyAccepted[6]),
                    "READY command response did not report authoritative state");
            List<String> readyRoster = ready.responses().subList(1, ready.responses().size());
            verifyRoster(readyRoster, 1, 1, false);
            require(ready.broadcasts().equals(readyRoster),
                    "READY command did not classify its immutable roster as a peer broadcast");

            IndependentHostWireProtocol.Result presence = alpha.accept(
                    "MECH|SESSION_COMMAND|1|PRESENCE|busy");
            require(!presence.disconnect(), "PRESENCE command was rejected");
            String[] presenceAccepted = requireCommand(
                    presence.responses().get(0), "SESSION_COMMAND_ACCEPTED");
            require("busy".equals(presenceAccepted[7]),
                    "PRESENCE command response did not report busy state");
            require(presence.broadcasts().equals(
                            presence.responses().subList(1, presence.responses().size())),
                    "PRESENCE command peer broadcast diverged from the direct roster");

            IndependentHostWireProtocol.Result chat = alpha.accept(
                    "MECH|SESSION_COMMAND|2|CHAT_STATE|typing");
            require(!chat.disconnect(), "CHAT_STATE command was rejected");
            String[] chatAccepted = requireCommand(
                    chat.responses().get(0), "SESSION_COMMAND_ACCEPTED");
            require("typing".equals(chatAccepted[8])
                            && "3".equals(chatAccepted[9])
                            && "2".equals(chatAccepted[10]),
                    "CHAT_STATE command response lost hosted-command accounting");
            require(chat.broadcasts().equals(
                            chat.responses().subList(1, chat.responses().size())),
                    "CHAT_STATE peer broadcast diverged from the direct roster");

            IndependentHostWireProtocol.Result status = alpha.accept("MECH|SESSION_STATUS");
            String[] statusLine = requireCommand(
                    status.responses().get(0), "SESSION_SNAPSHOT");
            require(statusLine.length == 10,
                    "legacy SESSION_STATUS compatibility changed unexpectedly");
            require(status.broadcasts().isEmpty(),
                    "private SESSION_STATUS response was incorrectly classified as a peer broadcast");

            beta = new IndependentHostWireProtocol(
                    "wire-beta-one",
                    ledger,
                    turns);
            authenticate(beta, "profile.beta.1002", "");
            IndependentHostWireProtocol.Result roster = beta.accept("MECH|SESSION_ROSTER");
            verifyRoster(roster.responses(), 2, 2, false);
            require(roster.broadcasts().isEmpty(),
                    "explicit roster request was incorrectly rebroadcast to peers");

            duplicate = new IndependentHostWireProtocol(
                    "wire-alpha-duplicate",
                    ledger,
                    turns);
            Handshake duplicateHandshake = beginHandshake(
                    duplicate, "profile.alpha.1001", alphaAccess.resumeToken);
            String duplicateDigest = SecureHandshakeStateMachine.computeIntegrityDigest(
                    duplicateHandshake.salt,
                    duplicateHandshake.manifestFingerprint,
                    duplicateHandshake.mountedFingerprint);
            IndependentHostWireProtocol.Result duplicateDenied = duplicate.accept(
                    "MECH|CHALLENGE_RESPONSE|" + duplicateDigest);
            require(duplicateDenied.disconnect()
                            && duplicateDenied.reason().contains("already connected"),
                    "active duplicate session did not reach the ledger attachment denial: "
                            + duplicateDenied.reason());
            require(duplicateDenied.broadcasts().isEmpty(),
                    "unattached duplicate denial incorrectly emitted a departure roster");

            alpha.disconnect("wire reconnect smoke");
            resumed = new IndependentHostWireProtocol(
                    "wire-alpha-two",
                    ledger,
                    turns);
            Access resumedAccess = authenticate(
                    resumed, "profile.alpha.1001", alphaAccess.resumeToken);
            require(resumedAccess.resumed,
                    "valid resume token did not restore the hosted session");
            require(resumedAccess.playerId.equals(alphaAccess.playerId),
                    "resume changed the stable remote player id");
            require(resumedAccess.connectionGeneration == 2L,
                    "resume did not advance the connection generation");

            IndependentHostWireProtocol.Result resumedCommand = resumed.accept(
                    "MECH|SESSION_COMMAND|0|PRESENCE|away");
            String[] resumedAccepted = requireCommand(
                    resumedCommand.responses().get(0), "SESSION_COMMAND_ACCEPTED");
            require("4".equals(resumedAccepted[9])
                            && "0".equals(resumedAccepted[10]),
                    "hosted command lifetime accounting did not survive reconnect");

            IndependentHostWireProtocol.Result wrongLaneDenied = resumed.accept(
                    "MECH|SESSION_COMMAND|1|MOVE|north");
            require(wrongLaneDenied.disconnect()
                            && wrongLaneDenied.reason().contains("world authority is closed"),
                    "world verb was not rejected from the hosted-lobby command lane");
            verifyRoster(wrongLaneDenied.broadcasts(), 1, 1, false);
            require(!containsPlayer(wrongLaneDenied.broadcasts(), resumedAccess.playerId),
                    "departure roster exposed an offline persisted resume identity");
            require(!ledger.hostedSessionSnapshot().worldAuthority(),
                    "hosted-session roster overclaimed full world authority");
            require(turns.acceptedCommands() == 0L && turns.worldTurn() == 0L,
                    "wrong-lane MOVE mutated the separate wait authority");

            beta.disconnect("wire smoke complete");
            System.out.println("IndependentHostHostedSessionWireSmoke PASS"
                    + " hostedCommands=true"
                    + " immutableRoster=true"
                    + " connectedOnlyRosterVisibility=true"
                    + " offlineResumeIdentityPrivate=true"
                    + " peerBroadcastClassification=true"
                    + " privateResponsesNotBroadcast=true"
                    + " departureRosterClassified=true"
                    + " duplicateAttachmentReachedLedger=true"
                    + " reconnectContinuity=true"
                    + " hostedAndWorldCommandLanesSeparated=true"
                    + " relayTransportOnly=true"
                    + " fullWorldAuthority=false");
        } finally {
            disconnectQuietly(resumed, "wire smoke cleanup");
            disconnectQuietly(duplicate, "wire smoke cleanup");
            disconnectQuietly(beta, "wire smoke cleanup");
            disconnectQuietly(alpha, "wire smoke cleanup");
            turns.close();
            ledger.close();
        }
    }

    private static Access authenticate(
            IndependentHostWireProtocol protocol,
            String profile,
            String resumeToken
    ) {
        Handshake handshake = beginHandshake(protocol, profile, resumeToken);
        String digest = SecureHandshakeStateMachine.computeIntegrityDigest(
                handshake.salt,
                handshake.manifestFingerprint,
                handshake.mountedFingerprint);
        IndependentHostWireProtocol.Result accessResult = protocol.accept(
                "MECH|CHALLENGE_RESPONSE|" + digest);
        require(!accessResult.disconnect(),
                "valid handshake was denied: " + accessResult.reason());
        String[] access = requireCommand(
                accessResult.responses().get(0), "ACCESS");
        require(access.length == 11,
                "invalid ACCESS field count: " + Arrays.toString(access));
        require("RELAY_ONLY".equals(access[2]),
                "hosted-session wire granted an unexpected transport access class");
        require(!accessResult.broadcasts().isEmpty(),
                "successful session attachment did not classify a peer roster broadcast");
        String[] broadcastBegin = requireCommand(
                accessResult.broadcasts().get(0), "HOSTED_ROSTER_BEGIN");
        require(!Boolean.parseBoolean(broadcastBegin[6]),
                "hosted-session roster overclaimed full world authority");
        return new Access(
                access[6],
                access[7],
                Long.parseLong(access[8]),
                Long.parseLong(access[9]),
                Boolean.parseBoolean(access[10]));
    }

    private static Handshake beginHandshake(
            IndependentHostWireProtocol protocol,
            String profile,
            String resumeToken
    ) {
        String[] hello = requireCommand(protocol.helloLine(), "HELLO");
        require(hello.length == 6
                        && IndependentHostWireProtocol.VERSION.equals(hello[2]),
                "HELLO did not advertise the current hosted-session wire");
        IndependentHostWireProtocol.Result manifestResult = protocol.accept(
                "MECH|IDENTITY|" + profile
                        + (resumeToken == null || resumeToken.isBlank()
                        ? ""
                        : "|" + resumeToken));
        String[] manifest = requireCommand(
                manifestResult.responses().get(0), "MANIFEST");
        String fingerprint = manifest[3];
        IndependentHostWireProtocol.Result acquired = protocol.accept(
                "MECH|ACQUIRED|" + fingerprint);
        requireCommand(acquired.responses().get(0), "RESTART_REQUIRED");
        IndependentHostWireProtocol.Result restarted = protocol.accept(
                "MECH|RESTARTED|" + fingerprint);
        String[] challenge = requireCommand(
                restarted.responses().get(0), "CHALLENGE");
        return new Handshake(challenge[2], challenge[3], challenge[4]);
    }

    private static void disconnectQuietly(
            IndependentHostWireProtocol protocol,
            String reason
    ) {
        if (protocol == null) return;
        try {
            protocol.disconnect(reason);
        } catch (RuntimeException ignored) {
        }
    }

    private static boolean containsPlayer(List<String> lines, String playerId) {
        for (String line : lines) {
            String[] fields = line.split("\\|", -1);
            if (fields.length >= 3
                    && "HOSTED_ROSTER_ENTRY".equals(fields[1])
                    && playerId.equals(fields[2])) {
                return true;
            }
        }
        return false;
    }

    private static void verifyRoster(
            List<String> lines,
            int expectedTotal,
            int expectedActive,
            boolean expectedWorldAuthority
    ) {
        require(lines.size() == expectedTotal + 2,
                "hosted roster returned the wrong number of frames: " + lines);
        String[] begin = requireCommand(lines.get(0), "HOSTED_ROSTER_BEGIN");
        require(Integer.parseInt(begin[4]) == expectedTotal,
                "hosted roster total count is incorrect");
        require(Integer.parseInt(begin[5]) == expectedActive,
                "hosted roster active count is incorrect");
        require(Boolean.parseBoolean(begin[6]) == expectedWorldAuthority,
                "hosted roster full-world-authority flag is incorrect");
        String previousPlayer = "";
        for (int i = 1; i <= expectedTotal; i++) {
            String[] entry = requireCommand(lines.get(i), "HOSTED_ROSTER_ENTRY");
            require(entry.length == 10,
                    "invalid HOSTED_ROSTER_ENTRY field count: "
                            + Arrays.toString(entry));
            require(Boolean.parseBoolean(entry[3]),
                    "wire roster serialized an offline persisted session");
            require(previousPlayer.compareTo(entry[2]) < 0,
                    "hosted roster entries are not deterministically ordered");
            previousPlayer = entry[2];
        }
        String[] end = requireCommand(
                lines.get(lines.size() - 1), "HOSTED_ROSTER_END");
        require(begin[2].equals(end[2]),
                "hosted roster begin/end versions do not match");
    }

    private static String[] requireCommand(String line, String command) {
        require(line != null, "missing MECH|" + command + " response");
        String[] fields = line.split("\\|", -1);
        require(fields.length >= 2
                        && "MECH".equals(fields[0])
                        && command.equals(fields[1]),
                "expected MECH|" + command + " but received " + line);
        return fields;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record Handshake(
            String salt,
            String manifestFingerprint,
            String mountedFingerprint
    ) { }

    private record Access(
            String playerId,
            String resumeToken,
            long connectionGeneration,
            long snapshotVersion,
            boolean resumed
    ) { }

    private IndependentHostHostedSessionWireSmoke() { }
}
