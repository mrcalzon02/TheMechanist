package mechanist;

import java.util.Arrays;
import java.util.Locale;

/** Direct authenticated-wire checks for the wait-only remote authority. */
final class IndependentHostWorldWaitWireSmoke {
    private static final String WORLD_ID = "world-wait-wire-smoke";
    private static final String PROFILE = "alpha.world.wait.1001";

    public static void main(String[] args) throws Exception {
        RemoteSessionLedgerAuthority sessions =
                new RemoteSessionLedgerAuthority(WORLD_ID);
        IndependentHostTurnAuthority turns =
                new IndependentHostTurnAuthority(WORLD_ID);
        try {
            IndependentHostWireProtocol preAccess =
                    new IndependentHostWireProtocol(
                            "world-wait-pre-access",
                            sessions,
                            turns);
            IndependentHostWireProtocol.Result preAccessDenied =
                    preAccess.accept("MECH|WORLD_COMMAND|0|WAIT");
            require(preAccessDenied.disconnect()
                            && allMessages(preAccessDenied.reason())
                            .contains("before relay access"),
                    "pre-authentication world wait was not denied");

            IndependentHostWireProtocol first =
                    new IndependentHostWireProtocol(
                            "world-wait-first",
                            sessions,
                            turns);
            Access firstAccess = authenticate(first, PROFILE, "");
            require(!firstAccess.resumed
                            && firstAccess.connectionGeneration == 1L,
                    "first world-wait connection incorrectly reported resume");

            String[] firstAccepted = requireCommand(
                    first.accept("MECH|WORLD_COMMAND|0|WAIT")
                            .responses().get(0),
                    "WORLD_COMMAND_ACCEPTED");
            verifyWaitAccepted(firstAccepted, 0L, 1L, 1L, 1L, 1L, 1L);
            IndependentHostTurnAuthority.TurnSnapshot firstSnapshot =
                    first.turnSnapshot();
            require(firstSnapshot != null
                            && firstSnapshot.playerId().equals(firstAccess.playerId)
                            && firstSnapshot.playerTurn() == 1L,
                    "wire protocol did not expose its authoritative turn snapshot");

            IndependentHostWireProtocol.Result replay =
                    first.accept("MECH|WORLD_COMMAND|0|WAIT");
            require(replay.disconnect()
                            && allMessages(replay.reason()).contains("sequence mismatch"),
                    "replayed world command was not rejected and disconnected");
            require(turns.snapshotForPlayer(firstAccess.playerId).playerTurn() == 1L,
                    "replayed world command changed authoritative state");

            IndependentHostWireProtocol resumed =
                    new IndependentHostWireProtocol(
                            "world-wait-resumed",
                            sessions,
                            turns);
            Access resumedAccess = authenticate(
                    resumed,
                    PROFILE,
                    firstAccess.resumeToken);
            require(resumedAccess.resumed
                            && resumedAccess.playerId.equals(firstAccess.playerId)
                            && resumedAccess.connectionGeneration == 2L,
                    "valid resume did not recover the authoritative player identity");
            String[] resumedAccepted = requireCommand(
                    resumed.accept("MECH|WORLD_COMMAND|0|WAIT")
                            .responses().get(0),
                    "WORLD_COMMAND_ACCEPTED");
            verifyWaitAccepted(resumedAccepted, 0L, 2L, 0L, 2L, 2L, 2L);

            IndependentHostWireProtocol unsupported =
                    new IndependentHostWireProtocol(
                            "world-wait-unsupported",
                            new RemoteSessionLedgerAuthority(
                                    "unsupported-world-wait"),
                            new IndependentHostTurnAuthority(
                                    "unsupported-world-wait"));
            try {
                authenticate(unsupported, "alpha.world.wait.2002", "");
                IndependentHostWireProtocol.Result denied =
                        unsupported.accept("MECH|WORLD_COMMAND|0|MOVE");
                require(denied.disconnect()
                                && allMessages(denied.reason())
                                .contains("only authoritative wait is open"),
                        "unsupported movement verb was not rejected at the wire boundary");
            } finally {
                unsupported.disconnect("world wait smoke complete");
            }

            require(turns.worldTurn() == 2L
                            && turns.acceptedCommands() == 2L,
                    "wire command accounting diverged from authoritative state");
            require(first.helloLine().endsWith("|RELAY_ONLY"),
                    "transport access-class compatibility changed unexpectedly");
            require(first.auditSummary().contains("networkWaitAuthority=true")
                            && first.auditSummary().contains("movementAuthority=false")
                            && first.auditSummary().contains("mapAuthority=false")
                            && first.auditSummary().contains("gameplaySessionCertified=false"),
                    "wire audit overclaimed its narrow wait authority");

            resumed.disconnect("world wait smoke complete");
            System.out.println("IndependentHostWorldWaitWireSmoke PASS"
                    + " authenticatedWait=true"
                    + " separateWorldCommandOrdering=true"
                    + " reconnectGenerationReset=true"
                    + " replayRejected=true"
                    + " unsupportedMovementRejected=true"
                    + " relayTransportCompatibility=true"
                    + " movementAuthority=false"
                    + " mapAuthority=false"
                    + " gameplaySessionCertified=false");
        } finally {
            sessions.close();
            turns.close();
        }
    }

    private static Access authenticate(
            IndependentHostWireProtocol protocol,
            String profile,
            String resumeToken
    ) {
        String[] hello = requireCommand(protocol.helloLine(), "HELLO");
        require(hello.length == 6
                        && IndependentHostWireProtocol.VERSION.equals(hello[2])
                        && "RELAY_ONLY".equals(hello[5]),
                "HELLO did not advertise the current compatible transport class");
        IndependentHostWireProtocol.Result manifestResult = protocol.accept(
                "MECH|IDENTITY|" + profile
                        + (resumeToken == null || resumeToken.isBlank()
                        ? ""
                        : "|" + resumeToken));
        String[] manifest = requireCommand(
                manifestResult.responses().get(0),
                "MANIFEST");
        String fingerprint = manifest[3];
        IndependentHostWireProtocol.Result acquired = protocol.accept(
                "MECH|ACQUIRED|" + fingerprint);
        requireCommand(acquired.responses().get(0), "RESTART_REQUIRED");
        IndependentHostWireProtocol.Result restarted = protocol.accept(
                "MECH|RESTARTED|" + fingerprint);
        String[] challenge = requireCommand(
                restarted.responses().get(0),
                "CHALLENGE");
        String digest = SecureHandshakeStateMachine.computeIntegrityDigest(
                challenge[2],
                challenge[3],
                challenge[4]);
        IndependentHostWireProtocol.Result accessResult = protocol.accept(
                "MECH|CHALLENGE_RESPONSE|" + digest);
        require(!accessResult.disconnect(),
                "valid world-wait handshake was denied: " + accessResult.reason());
        String[] access = requireCommand(
                accessResult.responses().get(0),
                "ACCESS");
        require(access.length == 11
                        && "RELAY_ONLY".equals(access[2]),
                "world-wait wire changed the transport access frame: "
                        + Arrays.toString(access));
        return new Access(
                access[6],
                access[7],
                Long.parseLong(access[8]),
                Boolean.parseBoolean(access[10]));
    }

    private static void verifyWaitAccepted(
            String[] fields,
            long commandId,
            long generation,
            long lastCommandId,
            long playerTurn,
            long worldTurn,
            long acceptedCommands
    ) {
        require(fields.length == 12,
                "invalid WORLD_COMMAND_ACCEPTED field count: "
                        + Arrays.toString(fields));
        require(Long.parseLong(fields[2]) == commandId
                        && "WAIT".equals(fields[3])
                        && Long.parseLong(fields[5]) == generation
                        && Long.parseLong(fields[6]) == lastCommandId
                        && Long.parseLong(fields[7]) == playerTurn
                        && Long.parseLong(fields[8]) == worldTurn
                        && Long.parseLong(fields[9]) == acceptedCommands
                        && Long.parseLong(fields[10]) == acceptedCommands,
                "WORLD_COMMAND_ACCEPTED lost authoritative wait state: "
                        + Arrays.toString(fields));
    }

    private static String[] requireCommand(
            String line,
            String command
    ) {
        require(line != null, "missing MECH|" + command + " response");
        String[] fields = line.split("\\|", -1);
        require(fields.length >= 2
                        && "MECH".equals(fields[0])
                        && command.equals(fields[1]),
                "expected MECH|" + command + " but received " + line);
        return fields;
    }

    private static String allMessages(String message) {
        return message == null
                ? ""
                : message.toLowerCase(Locale.ROOT);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record Access(
            String playerId,
            String resumeToken,
            long connectionGeneration,
            boolean resumed
    ) {
    }

    private IndependentHostWorldWaitWireSmoke() {
    }
}
