package mechanist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Packaged independent-host transport smoke.
 *
 * This proves the current native relay boundary only: exact bind, two-client
 * connection, monotonic sequencing, broadcast, replay disconnect, close, and
 * restart. It deliberately does not claim authoritative world/gameplay session
 * support.
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
                "current packaged fallback should be the real native relay, found: " + first.transportName());

        NativeTcpRelayServer relay = (NativeTcpRelayServer) first.session();
        require(relay.running(), "native relay is not running after a successful bind");
        require(LOOPBACK.equals(relay.boundAddress()),
                "native relay did not retain the requested loopback address");
        require(relay.port() == first.port(), "binding result and relay disagree about the port");

        try (Socket sender = connect(first.port()); Socket receiver = connect(first.port())) {
            waitForClients(relay, 2, 3_000L);
            BufferedWriter senderOut = new BufferedWriter(new OutputStreamWriter(
                    sender.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader receiverIn = new BufferedReader(new InputStreamReader(
                    receiver.getInputStream(), StandardCharsets.UTF_8));

            writeLine(senderOut, "SEQ|0|alpha-transport-frame");
            require("SEQ|0|alpha-transport-frame".equals(receiverIn.readLine()),
                    "receiver did not obtain the first accepted relay frame");

            writeLine(senderOut, "SEQ|1|beta-transport-frame");
            require("SEQ|1|beta-transport-frame".equals(receiverIn.readLine()),
                    "receiver did not obtain the second accepted relay frame");

            receiver.setSoTimeout(700);
            writeLine(senderOut, "SEQ|0|replayed-transport-frame");
            boolean replayBroadcast = false;
            try {
                replayBroadcast = receiverIn.readLine() != null;
            } catch (SocketTimeoutException expected) {
                replayBroadcast = false;
            }
            require(!replayBroadcast, "replayed sequence frame was broadcast to another client");
            waitForClients(relay, 1, 3_000L);
        } finally {
            first.close();
        }
        require(!relay.running(), "native relay remained running after host close");

        int restartPort = NetworkPortAuthority.firstAvailableGamePort();
        HostBindingResult restarted = MultiplayerHostBindingService.bind(
                config(restartPort, LOOPBACK, "relay-session-restart"));
        try {
            require(restarted.success(), "relay could not restart after close: " + restarted.compactLine());
            require(LOOPBACK.equals(restarted.boundAddress()),
                    "restarted relay widened the requested loopback bind");
            try (Socket client = connect(restarted.port())) {
                require(client.isConnected(), "client did not connect after independent host restart");
            }
        } finally {
            restarted.close();
        }

        int deniedPort = NetworkPortAuthority.firstAvailableGamePort();
        HostBindingResult denied = MultiplayerHostBindingService.bind(
                config(deniedPort, NON_LOCAL_TEST_ADDRESS, "relay-session-denied-address"));
        try {
            require(!denied.success(),
                    "non-local TEST-NET bind unexpectedly succeeded: " + denied.compactLine());
            require(NON_LOCAL_TEST_ADDRESS.equals(denied.boundAddress()),
                    "failed explicit bind widened to another interface: " + denied.compactLine());
            require(!"0.0.0.0".equals(denied.boundAddress()) && !"::".equals(denied.boundAddress()),
                    "failed explicit bind widened to a wildcard interface");
        } finally {
            denied.close();
        }

        System.out.println("IndependentHostTransportSessionSmoke PASS"
                + " exactLoopbackBind=true"
                + " twoClientRelay=true"
                + " sequencing=true"
                + " replayRejected=true"
                + " restart=true"
                + " wildcardWideningRejected=true"
                + " transportRelayOnly=true"
                + " gameplaySessionCertified=false");
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

    private static void writeLine(BufferedWriter writer, String line) throws Exception {
        writer.write(line);
        writer.newLine();
        writer.flush();
    }

    private static void waitForClients(NativeTcpRelayServer relay, int expected, long timeoutMillis)
            throws Exception {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            if (relay.clientCount() == expected) return;
            Thread.sleep(20L);
        }
        throw new AssertionError("relay client count did not reach " + expected
                + "; actual=" + relay.clientCount());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private IndependentHostTransportSessionSmoke() { }
}
