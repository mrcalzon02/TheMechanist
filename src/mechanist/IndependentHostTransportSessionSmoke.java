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
 * Packaged independent-host transport smoke.
 *
 * This proves the current relay boundary only: exact bind, client-driven
 * identity/manifest/acquisition/restart/integrity exchange, relay-only access,
 * two-client sequencing and broadcast, replay disconnect, close, and restart.
 * It deliberately does not claim authoritative remote world/gameplay support.
 */
final class IndependentHostTransportSessionSmoke {
    private static final String LOOPBACK = "127.0.0.1";
    private static final String NON_LOCAL_TEST_ADDRESS = "192.0.2.1";

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
                         relay.port(), "alpha.sender.0001");
                 ClientSession receiver = ClientSession.connect(
                         relay.port(), "alpha.receiver.0002")) {
                waitForAuthenticatedClients(relay, 2, 3_000L);
                require(sender.relayOnlyAccess && receiver.relayOnlyAccess,
                        "successful handshakes did not grant RELAY_ONLY access");

                sender.write("SEQ|0|alpha-transport-frame");
                require("SEQ|0|alpha-transport-frame".equals(receiver.read()),
                        "receiver did not obtain the first accepted relay frame");

                sender.write("SEQ|1|beta-transport-frame");
                require("SEQ|1|beta-transport-frame".equals(receiver.read()),
                        "receiver did not obtain the second accepted relay frame");

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
            }
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
                    restarted.port(), "alpha.restart.0003")) {
                waitForAuthenticatedClients(restartedRelay, 1, 3_000L);
                require(client.relayOnlyAccess,
                        "client did not regain relay-only access after host restart");
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
            requireCommand(reader.readLine(), "HELLO");
            writeLine(writer, "MECH|IDENTITY|alpha.invalid.0000");
            String[] manifest = requireCommand(reader.readLine(), "MANIFEST");
            require(manifest.length == 6,
                    "invalid MANIFEST field count: " + Arrays.toString(manifest));
            String fingerprint = manifest[3];
            writeLine(writer, "MECH|ACQUIRED|" + fingerprint);
            String[] restart = requireCommand(reader.readLine(), "RESTART_REQUIRED");
            require(restart.length == 3 && fingerprint.equals(restart[2]),
                    "restart fingerprint did not match the delivered manifest");
            writeLine(writer, "MECH|RESTARTED|" + fingerprint);
            requireCommand(reader.readLine(), "CHALLENGE");
            writeLine(writer, "MECH|CHALLENGE_RESPONSE|"
                    + "0".repeat(64));
            String denial = reader.readLine();
            requireCommand(denial, "DENIED");
            require(denial.toLowerCase().contains("integrity challenge"),
                    "bad challenge denial did not identify integrity rejection: "
                            + denial);
        }
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class ClientSession implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final boolean relayOnlyAccess;

        private ClientSession(
                Socket socket,
                BufferedReader reader,
                BufferedWriter writer,
                boolean relayOnlyAccess
        ) {
            this.socket = socket;
            this.reader = reader;
            this.writer = writer;
            this.relayOnlyAccess = relayOnlyAccess;
        }

        static ClientSession connect(int port, String profileIdentity)
                throws Exception {
            Socket socket = IndependentHostTransportSessionSmoke.connect(port);
            BufferedReader reader = reader(socket);
            BufferedWriter writer = writer(socket);
            try {
                String[] hello = requireCommand(reader.readLine(), "HELLO");
                require(hello.length == 6,
                        "invalid HELLO field count: " + Arrays.toString(hello));
                require(IndependentHostWireProtocol.VERSION.equals(hello[2]),
                        "server advertised unexpected wire protocol: " + hello[2]);
                require("RELAY_ONLY".equals(hello[5]),
                        "server HELLO did not declare relay-only access");

                writeLine(writer, "MECH|IDENTITY|" + profileIdentity);
                String[] manifest = requireCommand(reader.readLine(), "MANIFEST");
                require(manifest.length == 6,
                        "invalid MANIFEST field count: "
                                + Arrays.toString(manifest));
                String fingerprint = manifest[3];
                require(fingerprint.matches("[a-f0-9]{64}"),
                        "manifest fingerprint is not SHA-256: " + fingerprint);

                writeLine(writer, "MECH|ACQUIRED|" + fingerprint);
                String[] restart = requireCommand(
                        reader.readLine(), "RESTART_REQUIRED");
                require(restart.length == 3
                                && fingerprint.equals(restart[2]),
                        "restart requirement did not preserve the manifest fingerprint");

                writeLine(writer, "MECH|RESTARTED|" + fingerprint);
                String[] challenge = requireCommand(reader.readLine(), "CHALLENGE");
                require(challenge.length == 5,
                        "invalid CHALLENGE field count: "
                                + Arrays.toString(challenge));
                require(fingerprint.equals(challenge[3])
                                && fingerprint.equals(challenge[4]),
                        "challenge fingerprints do not match the acquired package");
                String digest = SecureHandshakeStateMachine.computeIntegrityDigest(
                        challenge[2], challenge[3], challenge[4]);
                writeLine(writer, "MECH|CHALLENGE_RESPONSE|" + digest);

                String[] access = requireCommand(reader.readLine(), "ACCESS");
                require(access.length == 6,
                        "invalid ACCESS field count: " + Arrays.toString(access));
                require("RELAY_ONLY".equals(access[2]),
                        "server granted an unexpected access class: " + access[2]);
                require(profileIdentity.equals(access[5]),
                        "server access response returned the wrong profile identity");
                return new ClientSession(socket, reader, writer, true);
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

        @Override
        public void close() throws Exception {
            socket.close();
        }
    }

    private IndependentHostTransportSessionSmoke() { }
}
