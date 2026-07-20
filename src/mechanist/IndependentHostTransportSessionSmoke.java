package mechanist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Packaged independent-host transport and remote-session continuity smoke.
 *
 * This proves exact bind, client-driven authentication, server-owned session
 * identity, resume-token continuity, immutable session snapshots, sequencing,
 * relay broadcast, denial paths, close, and restart. World gameplay authority
 * remains intentionally outside this smoke.
 */
final class IndependentHostTransportSessionSmoke {
    private static final String LOOPBACK = "127.0.0.1";
    private static final String NON_LOCAL_TEST_ADDRESS = "192.0.2.1";
    private static final String SENDER_PROFILE = "alpha.sender.0001";
    private static final String RECEIVER_PROFILE = "alpha.receiver.0002";

    public static void main(String[] args) throws Exception {
        int firstPort = NetworkPortAuthority.firstAvailableGamePort();
        HostBindingResult first = MultiplayerHostBindingService.bind(
                config(firstPort, LOOPBACK, "relay-session-one"));
        require(first.success(), "loopback relay bind failed: " + first.compactLine());
        require(LOOPBACK.equals(first.boundAddress()),
                "explicit loopback bind widened or changed: " + first.compactLine());
        require(first.session() instanceof NativeTcpRelayServer,
                "current packaged fallback should be the real native relay, found: "
                        + first.transportName());

        NativeTcpRelayServer relay = (NativeTcpRelayServer) first.session();
        require(relay.running(), "native relay is not running after a successful bind");
        require(LOOPBACK.equals(relay.boundAddress()),
                "native relay did not retain the requested loopback address");
        require(relay.port() == first.port(),
                "binding result and relay disagree about the port");

        try {
            verifyDataBeforeHandshakeDenied(relay.port());
            waitForAuthenticatedClients(relay, 0, 3_000L);

            verifyBadChallengeDenied(relay.port());
            waitForAuthenticatedClients(relay, 0, 3_000L);

            try (ClientSession sender = ClientSession.connect(
                         relay.port(), SENDER_PROFILE, "");
                 ClientSession receiver = ClientSession.connect(
                         relay.port(), RECEIVER_PROFILE, "")) {
                waitForAuthenticatedClients(relay, 2, 3_000L);
                require(sender.relayOnlyAccess && receiver.relayOnlyAccess,
                        "successful handshakes did not grant RELAY_ONLY access");
                require(!sender.resumed && !receiver.resumed,
                        "new profile sessions were incorrectly reported as resumed");
                require(sender.connectionGeneration == 1L
                                && receiver.connectionGeneration == 1L,
                        "new sessions did not begin at connection generation one");
                require(relay.remoteSessionCount() == 2
                                && relay.activeRemoteSessionCount() == 2,
                        "relay ledger did not register two active server-owned sessions");

                verifyActiveDuplicateDenied(
                        relay.port(), SENDER_PROFILE, sender.resumeToken);
                waitForAuthenticatedClients(relay, 2, 3_000L);

                sender.write("SEQ|0|alpha-transport-frame");
                require("SEQ|0|alpha-transport-frame".equals(receiver.read()),
                        "receiver did not obtain the first accepted relay frame");

                sender.write("SEQ|1|beta-transport-frame");
                require("SEQ|1|beta-transport-frame".equals(receiver.read()),
                        "receiver did not obtain the second accepted relay frame");

                SessionReadout beforeDisconnect = sender.sessionStatus();
                require(beforeDisconnect.connected,
                        "session snapshot did not report the sender connected");
                require(beforeDisconnect.connectionGeneration == 1L,
                        "session snapshot returned the wrong initial generation");
                require(beforeDisconnect.acceptedRelayFrames == 2L
                                && beforeDisconnect.lastConnectionSequence == 1L,
                        "session snapshot did not account for accepted relay frames");
                require(sender.playerId.equals(beforeDisconnect.playerId),
                        "session snapshot changed the stable player id");

                receiver.socket.setSoTimeout(700);
                sender.write("SEQ|0|replayed-transport-frame");
                boolean replayBroadcast = false;
                try {
                    replayBroadcast = receiver.read() != null;
                } catch (SocketTimeoutException expected) {
                    replayBroadcast = false;
                }
                require(!replayBroadcast,
                        "replayed sequence frame was broadcast to another client");
                waitForAuthenticatedClients(relay, 1, 3_000L);
                waitForSessionConnected(relay, SENDER_PROFILE, false, 3_000L);

                verifyInvalidResumeTokenDenied(relay.port(), SENDER_PROFILE);
                waitForAuthenticatedClients(relay, 1, 3_000L);

                receiver.socket.setSoTimeout(3_000);
                try (ClientSession resumed = ClientSession.connect(
                        relay.port(), SENDER_PROFILE, sender.resumeToken)) {
                    waitForAuthenticatedClients(relay, 2, 3_000L);
                    require(resumed.resumed,
                            "valid resume token did not restore the server-owned session");
                    require(resumed.playerId.equals(sender.playerId),
                            "resume changed the stable remote player id");
                    require(resumed.connectionGeneration == 2L,
                            "resumed session did not advance the connection generation");
                    require(resumed.accessSnapshotVersion
                                    > beforeDisconnect.snapshotVersion,
                            "resumed access did not advance the immutable ledger version");
                    require(relay.remoteSessionCount() == 2
                                    && relay.activeRemoteSessionCount() == 2,
                            "resume created a duplicate session instead of restoring continuity");

                    resumed.write("SEQ|0|resumed-transport-frame");
                    require("SEQ|0|resumed-transport-frame".equals(receiver.read()),
                            "receiver did not obtain the resumed session frame");
                    SessionReadout afterResume = resumed.sessionStatus();
                    require(afterResume.connected,
                            "resumed session snapshot did not report connected state");
                    require(afterResume.connectionGeneration == 2L,
                            "resumed snapshot lost connection-generation continuity");
                    require(afterResume.acceptedRelayFrames == 3L,
                            "resumed snapshot lost lifetime relay-frame accounting");
                    require(afterResume.lastConnectionSequence == 0L,
                            "resumed connection did not reset its per-connection sequence");
                    require(afterResume.snapshotVersion
                                    > beforeDisconnect.snapshotVersion,
                            "resumed snapshot version did not advance monotonically");
                }
                waitForAuthenticatedClients(relay, 1, 3_000L);
                waitForSessionConnected(relay, SENDER_PROFILE, false, 3_000L);
            }
            waitForAuthenticatedClients(relay, 0, 3_000L);
            require(relay.remoteSessionCount() == 2
                            && relay.activeRemoteSessionCount() == 0,
                    "closed clients were not retained as offline resumable sessions");
        } finally {
            first.close();
        }
        require(!relay.running(), "native relay remained running after host close");

        int restartPort = NetworkPortAuthority.firstAvailableGamePort();
        HostBindingResult restarted = MultiplayerHostBindingService.bind(
                config(restartPort, LOOPBACK, "relay-session-restart"));
        try {
            require(restarted.success(),
                    "relay could not restart after close: " + restarted.compactLine());
            require(LOOPBACK.equals(restarted.boundAddress()),
                    "restarted relay widened the requested loopback bind");
            require(restarted.session() instanceof NativeTcpRelayServer,
                    "restarted host did not expose the native relay session");
            NativeTcpRelayServer restartedRelay =
                    (NativeTcpRelayServer) restarted.session();
            try (ClientSession client = ClientSession.connect(
                    restarted.port(), "alpha.restart.0003", "")) {
                waitForAuthenticatedClients(restartedRelay, 1, 3_000L);
                require(client.relayOnlyAccess,
                        "client did not regain relay-only access after host restart");
                require(client.connectionGeneration == 1L && !client.resumed,
                        "fresh host restart did not create a fresh process-local session");
            }
        } finally {
            restarted.close();
        }

        int deniedPort = NetworkPortAuthority.firstAvailableGamePort();
        HostBindingResult denied = MultiplayerHostBindingService.bind(
                config(deniedPort, NON_LOCAL_TEST_ADDRESS,
                        "relay-session-denied-address"));
        try {
            require(!denied.success(),
                    "non-local TEST-NET bind unexpectedly succeeded: "
                            + denied.compactLine());
            require(NON_LOCAL_TEST_ADDRESS.equals(denied.boundAddress()),
                    "failed explicit bind widened to another interface: "
                            + denied.compactLine());
            require(!"0.0.0.0".equals(denied.boundAddress())
                            && !"::".equals(denied.boundAddress()),
                    "failed explicit bind widened to a wildcard interface");
        } finally {
            denied.close();
        }

        System.out.println("IndependentHostTransportSessionSmoke PASS"
                + " exactLoopbackBind=true"
                + " clientDrivenHandshake=true"
                + " manifestAcquisition=true"
                + " integrityChallenge=true"
                + " serverOwnedSessionLedger=true"
                + " stableRemotePlayerIdentity=true"
                + " resumeTokenContinuity=true"
                + " duplicateAttachmentDenied=true"
                + " invalidResumeTokenDenied=true"
                + " immutableSessionSnapshots=true"
                + " lifetimeRelayAccounting=true"
                + " preAuthenticationDataDenied=true"
                + " badChallengeDenied=true"
                + " twoClientRelay=true"
                + " sequencing=true"
                + " replayRejected=true"
                + " restart=true"
                + " wildcardWideningRejected=true"
                + " relayAccessOnly=true"
                + " worldAuthority=false"
                + " gameplaySessionCertified=false");
    }

    private static void verifyDataBeforeHandshakeDenied(int port) throws Exception {
        try (Socket socket = connect(port);
             BufferedReader reader = reader(socket);
             BufferedWriter writer = writer(socket)) {
            requireCommand(reader.readLine(), "HELLO");
            writeLine(writer, "SEQ|0|unauthenticated-frame");
            String denial = reader.readLine();
            requireCommand(denial, "DENIED");
            require(denial.toLowerCase().contains("before relay access"),
                    "pre-authentication denial did not explain the access boundary: "
                            + denial);
        }
    }

    private static void verifyBadChallengeDenied(int port) throws Exception {
        try (Socket socket = connect(port);
             BufferedReader reader = reader(socket);
             BufferedWriter writer = writer(socket)) {
            HandshakeChallenge challenge = beginHandshake(
                    reader, writer, "alpha.invalid.0000", "");
            writeLine(writer, "MECH|CHALLENGE_RESPONSE|" + "0".repeat(64));
            String denial = reader.readLine();
            requireCommand(denial, "DENIED");
            require(denial.toLowerCase().contains("integrity challenge"),
                    "bad challenge denial did not identify integrity rejection: "
                            + denial);
            require(challenge.fingerprint.matches("[a-f0-9]{64}"),
                    "bad-challenge setup did not receive a valid fingerprint");
        }
    }

    private static void verifyActiveDuplicateDenied(
            int port,
            String profileIdentity,
            String resumeToken
    ) throws Exception {
        try (Socket socket = connect(port);
             BufferedReader reader = reader(socket);
             BufferedWriter writer = writer(socket)) {
            HandshakeChallenge challenge = beginHandshake(
                    reader, writer, profileIdentity, resumeToken);
            writeLine(writer, "MECH|CHALLENGE_RESPONSE|" + challenge.digest());
            String denial = reader.readLine();
            requireCommand(denial, "DENIED");
            require(denial.toLowerCase().contains("already connected"),
                    "duplicate attachment denial did not identify split-brain risk: "
                            + denial);
        }
    }

    private static void verifyInvalidResumeTokenDenied(
            int port,
            String profileIdentity
    ) throws Exception {
        try (Socket socket = connect(port);
             BufferedReader reader = reader(socket);
             BufferedWriter writer = writer(socket)) {
            HandshakeChallenge challenge = beginHandshake(
                    reader, writer, profileIdentity, "f".repeat(64));
            writeLine(writer, "MECH|CHALLENGE_RESPONSE|" + challenge.digest());
            String denial = reader.readLine();
            requireCommand(denial, "DENIED");
            require(denial.toLowerCase().contains("resume token"),
                    "invalid resume-token denial was not explicit: " + denial);
        }
    }

    private static HandshakeChallenge beginHandshake(
            BufferedReader reader,
            BufferedWriter writer,
            String profileIdentity,
            String resumeToken
    ) throws Exception {
        String[] hello = requireCommand(reader.readLine(), "HELLO");
        require(hello.length == 6,
                "invalid HELLO field count: " + Arrays.toString(hello));
        require(IndependentHostWireProtocol.VERSION.equals(hello[2]),
                "server advertised unexpected wire protocol: " + hello[2]);
        require("RELAY_ONLY".equals(hello[5]),
                "server HELLO did not declare relay-only access");

        writeLine(writer, "MECH|IDENTITY|" + profileIdentity
                + (resumeToken == null || resumeToken.isBlank()
                        ? ""
                        : "|" + resumeToken));
        String[] manifest = requireCommand(reader.readLine(), "MANIFEST");
        require(manifest.length == 6,
                "invalid MANIFEST field count: " + Arrays.toString(manifest));
        String fingerprint = manifest[3];
        require(fingerprint.matches("[a-f0-9]{64}"),
                "manifest fingerprint is not SHA-256: " + fingerprint);

        writeLine(writer, "MECH|ACQUIRED|" + fingerprint);
        String[] restart = requireCommand(reader.readLine(), "RESTART_REQUIRED");
        require(restart.length == 3 && fingerprint.equals(restart[2]),
                "restart requirement did not preserve the manifest fingerprint");

        writeLine(writer, "MECH|RESTARTED|" + fingerprint);
        String[] challenge = requireCommand(reader.readLine(), "CHALLENGE");
        require(challenge.length == 5,
                "invalid CHALLENGE field count: " + Arrays.toString(challenge));
        require(fingerprint.equals(challenge[3])
                        && fingerprint.equals(challenge[4]),
                "challenge fingerprints do not match the acquired package");
        return new HandshakeChallenge(
                challenge[2], challenge[3], challenge[4], fingerprint);
    }

    private static ServerConfig config(int port, String address, String worldId) {
        return ServerConfig.fromWorldSettings(
                0x1A17A2026L,
                "Limited Alpha Relay",
                worldId,
                WorldSetupSettings.standard(),
                4,
                port,
                address,
                MultiplayerProtocolState.CLOSED,
                false);
    }

    private static Socket connect(int port) throws Exception {
        Socket socket = new Socket();
        socket.connect(new java.net.InetSocketAddress(LOOPBACK, port), 3_000);
        socket.setSoTimeout(3_000);
        return socket;
    }

    private static BufferedReader reader(Socket socket) throws Exception {
        return new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
    }

    private static BufferedWriter writer(Socket socket) throws Exception {
        return new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream(), StandardCharsets.UTF_8));
    }

    private static void writeLine(BufferedWriter writer, String line)
            throws Exception {
        writer.write(line);
        writer.newLine();
        writer.flush();
    }

    private static String[] requireCommand(String line, String command) {
        require(line != null, "server closed before " + command + " response");
        String[] fields = line.split("\\|", -1);
        require(fields.length >= 2
                        && "MECH".equals(fields[0])
                        && command.equals(fields[1]),
                "expected MECH|" + command + " but received: " + line);
        return fields;
    }

    private static void waitForAuthenticatedClients(
            NativeTcpRelayServer relay,
            int expected,
            long timeoutMillis
    ) throws Exception {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            if (relay.authenticatedClientCount() == expected) return;
            Thread.sleep(20L);
        }
        throw new AssertionError(
                "relay authenticated client count did not reach " + expected
                        + "; actual=" + relay.authenticatedClientCount()
                        + " total=" + relay.clientCount());
    }

    private static void waitForSessionConnected(
            NativeTcpRelayServer relay,
            String profileIdentity,
            boolean expected,
            long timeoutMillis
    ) throws Exception {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            RemoteSessionLedgerAuthority.SessionSnapshot snapshot =
                    relay.remoteSessionSnapshot(profileIdentity);
            if (snapshot != null && snapshot.connected() == expected) return;
            Thread.sleep(20L);
        }
        RemoteSessionLedgerAuthority.SessionSnapshot snapshot =
                relay.remoteSessionSnapshot(profileIdentity);
        throw new AssertionError(
                "remote session connected state did not become " + expected
                        + "; snapshot=" + (snapshot == null
                        ? "missing"
                        : snapshot.compactLine()));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record HandshakeChallenge(
            String salt,
            String manifestFingerprint,
            String mountedFingerprint,
            String fingerprint
    ) {
        String digest() {
            return SecureHandshakeStateMachine.computeIntegrityDigest(
                    salt, manifestFingerprint, mountedFingerprint);
        }
    }

    private record SessionReadout(
            String playerId,
            String worldId,
            long snapshotVersion,
            boolean connected,
            long connectionGeneration,
            long acceptedRelayFrames,
            long lastConnectionSequence,
            String lastEvent
    ) { }

    private static final class ClientSession implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final boolean relayOnlyAccess;
        private final String profileIdentity;
        private final String playerId;
        private final String resumeToken;
        private final long connectionGeneration;
        private final long accessSnapshotVersion;
        private final boolean resumed;

        private ClientSession(
                Socket socket,
                BufferedReader reader,
                BufferedWriter writer,
                boolean relayOnlyAccess,
                String profileIdentity,
                String playerId,
                String resumeToken,
                long connectionGeneration,
                long accessSnapshotVersion,
                boolean resumed
        ) {
            this.socket = socket;
            this.reader = reader;
            this.writer = writer;
            this.relayOnlyAccess = relayOnlyAccess;
            this.profileIdentity = profileIdentity;
            this.playerId = playerId;
            this.resumeToken = resumeToken;
            this.connectionGeneration = connectionGeneration;
            this.accessSnapshotVersion = accessSnapshotVersion;
            this.resumed = resumed;
        }

        static ClientSession connect(
                int port,
                String profileIdentity,
                String resumeToken
        ) throws Exception {
            Socket socket = IndependentHostTransportSessionSmoke.connect(port);
            BufferedReader reader = reader(socket);
            BufferedWriter writer = writer(socket);
            try {
                HandshakeChallenge challenge = beginHandshake(
                        reader, writer, profileIdentity, resumeToken);
                writeLine(writer, "MECH|CHALLENGE_RESPONSE|" + challenge.digest());

                String[] access = requireCommand(reader.readLine(), "ACCESS");
                require(access.length == 11,
                        "invalid ACCESS field count: " + Arrays.toString(access));
                require("RELAY_ONLY".equals(access[2]),
                        "server granted an unexpected access class: " + access[2]);
                require(profileIdentity.equals(access[5]),
                        "server access response returned the wrong profile identity");
                require(access[6].matches("remote-[a-f0-9]{20}"),
                        "server returned an invalid stable player id: " + access[6]);
                require(access[7].matches("[a-f0-9]{64}"),
                        "server returned an invalid resume token");
                return new ClientSession(
                        socket,
                        reader,
                        writer,
                        true,
                        profileIdentity,
                        access[6],
                        access[7],
                        Long.parseLong(access[8]),
                        Long.parseLong(access[9]),
                        Boolean.parseBoolean(access[10]));
            } catch (Throwable failure) {
                try {
                    socket.close();
                } catch (Exception ignored) {
                }
                throw failure;
            }
        }

        void write(String line) throws Exception {
            writeLine(writer, line);
        }

        String read() throws Exception {
            return reader.readLine();
        }

        SessionReadout sessionStatus() throws Exception {
            write("MECH|SESSION_STATUS");
            String[] snapshot = requireCommand(reader.readLine(), "SESSION_SNAPSHOT");
            require(snapshot.length == 10,
                    "invalid SESSION_SNAPSHOT field count: "
                            + Arrays.toString(snapshot));
            return new SessionReadout(
                    snapshot[2],
                    snapshot[3],
                    Long.parseLong(snapshot[4]),
                    Boolean.parseBoolean(snapshot[5]),
                    Long.parseLong(snapshot[6]),
                    Long.parseLong(snapshot[7]),
                    Long.parseLong(snapshot[8]),
                    snapshot[9]);
        }

        @Override
        public void close() throws Exception {
            socket.close();
        }
    }

    private IndependentHostTransportSessionSmoke() { }
}
