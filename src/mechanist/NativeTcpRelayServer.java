package mechanist;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/** Exact-bind, handshake-gated TCP relay used when verified Netty/Steam adapters are unavailable. */
final class NativeTcpRelayServer implements AutoCloseable {
    private static final int MAX_FRAME_BYTES = 64 * 1024;

    private final ServerConfig config;
    private final String bindAddress;
    private final int boundPort;
    private final ServerSocketChannel serverChannel;
    private final ExecutorService workers;
    private final List<ClientConnection> clients = Collections.synchronizedList(new ArrayList<>());
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread acceptThread;
    private final NetworkThrottlingManager throttlingManager = new NetworkThrottlingManager();
    private final RemoteSessionLedgerAuthority sessionLedger;

    private NativeTcpRelayServer(
            ServerConfig config,
            String bindAddress,
            int boundPort,
            ServerSocketChannel serverChannel
    ) {
        this.config = config;
        this.bindAddress = bindAddress;
        this.boundPort = boundPort;
        this.serverChannel = serverChannel;
        this.sessionLedger = new RemoteSessionLedgerAuthority(config.worldId());
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, "mechanist-native-relay-client");
            thread.setDaemon(true);
            return thread;
        };
        this.workers = Executors.newCachedThreadPool(factory);
        this.acceptThread = new Thread(this::acceptLoop, "mechanist-native-relay-accept");
        this.acceptThread.setDaemon(true);
    }

    static NativeTcpRelayServer bind(ServerConfig config, boolean ipv6) throws IOException {
        if (config == null) throw new IOException("Server configuration is required.");
        String requested = config.boundAddress() == null || config.boundAddress().isBlank()
                ? (ipv6 ? "::" : "0.0.0.0")
                : config.boundAddress().trim();
        InetAddress address = InetAddress.getByName(requested);
        if (ipv6 && !(address instanceof Inet6Address)) {
            throw new IOException("Requested IPv6 bind resolved to a non-IPv6 address: " + requested);
        }
        if (!ipv6 && !(address instanceof Inet4Address)) {
            throw new IOException("Requested IPv4 bind resolved to a non-IPv4 address: " + requested);
        }

        ServerSocketChannel channel = ServerSocketChannel.open(
                ipv6
                        ? java.net.StandardProtocolFamily.INET6
                        : java.net.StandardProtocolFamily.INET);
        try {
            channel.configureBlocking(true);
            channel.setOption(java.net.StandardSocketOptions.SO_REUSEADDR, Boolean.FALSE);
            channel.bind(new InetSocketAddress(address, config.port()));
            InetSocketAddress local = (InetSocketAddress) channel.getLocalAddress();
            String actualAddress = canonicalAddress(local.getAddress());
            int actualPort = local.getPort();
            MultiplayerProtocolState protocol = ipv6
                    ? MultiplayerProtocolState.NATIVE_IPV6
                    : MultiplayerProtocolState.NATIVE_IPV4;
            ServerConfig boundConfig = config.withBinding(actualAddress, actualPort, protocol);
            NativeTcpRelayServer server = new NativeTcpRelayServer(
                    boundConfig, actualAddress, actualPort, channel);
            server.start();
            return server;
        } catch (IOException | RuntimeException failure) {
            try {
                channel.close();
            } catch (IOException ignored) {
            }
            throw failure;
        }
    }

    String boundAddress() {
        return bindAddress;
    }

    int port() {
        return boundPort;
    }

    int clientCount() {
        return clients.size();
    }

    int authenticatedClientCount() {
        synchronized (clients) {
            int count = 0;
            for (ClientConnection client : clients) {
                if (client.protocol.relayAccessGranted()) count++;
            }
            return count;
        }
    }

    int remoteSessionCount() {
        return sessionLedger.totalSessionCount();
    }

    int activeRemoteSessionCount() {
        return sessionLedger.activeSessionCount();
    }

    RemoteSessionLedgerAuthority.SessionSnapshot remoteSessionSnapshot(String profileIdentity) {
        return sessionLedger.snapshotForProfile(profileIdentity);
    }

    boolean running() {
        return running.get();
    }

    private void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                close();
            } catch (Exception exception) {
                DebugLog.error(
                        "NATIVE_RELAY_SHUTDOWN",
                        "Shutdown hook could not close native relay.",
                        exception);
            }
        }, "mechanist-native-relay-shutdown"));
        acceptThread.start();
        DebugLog.audit(
                "NATIVE_RELAY_BOUND",
                "world=" + config.worldName()
                        + " address=" + bindAddress
                        + " port=" + boundPort
                        + " maxPlayers=" + config.maxPlayers()
                        + " handshake=" + IndependentHostWireProtocol.VERSION
                        + " access=RELAY_ONLY"
                        + " ledger={" + sessionLedger.auditSummary() + "}");
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                SocketChannel client = serverChannel.accept();
                if (client == null) continue;
                client.configureBlocking(true);
                if (clients.size() >= config.maxPlayers()) {
                    try {
                        client.close();
                    } catch (IOException ignored) {
                    }
                    continue;
                }
                client.socket().setSoTimeout(45_000);
                ClientConnection connection = new ClientConnection(client);
                clients.add(connection);
                throttlingManager.playerJoined();
                workers.submit(connection::readLoop);
            } catch (SocketException exception) {
                if (running.get()) {
                    DebugLog.error(
                            "NATIVE_RELAY_ACCEPT",
                            "Socket failure in relay accept loop.",
                            exception);
                }
            } catch (IOException exception) {
                if (running.get()) {
                    DebugLog.error(
                            "NATIVE_RELAY_ACCEPT",
                            "I/O failure in relay accept loop.",
                            exception);
                }
            } catch (RuntimeException exception) {
                if (running.get()) {
                    DebugLog.error(
                            "NATIVE_RELAY_ACCEPT",
                            "Runtime failure in relay accept loop.",
                            exception);
                }
            }
        }
    }

    private void broadcast(String line, ClientConnection from) {
        if (line == null || line.length() > MAX_FRAME_BYTES) return;
        synchronized (clients) {
            for (ClientConnection client : clients) {
                if (client != from && client.protocol.relayAccessGranted()) {
                    client.write(line);
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        if (!running.getAndSet(false)) return;
        IOException failure = null;
        try {
            serverChannel.close();
        } catch (IOException exception) {
            failure = exception;
        }
        synchronized (clients) {
            for (ClientConnection client : clients) client.closeQuietly();
            clients.clear();
        }
        workers.shutdownNow();
        DebugLog.audit(
                "NATIVE_RELAY_CLOSED",
                "address=" + bindAddress
                        + " port=" + boundPort
                        + " ledger={" + sessionLedger.auditSummary() + "}");
        if (failure != null) throw failure;
    }

    private static String canonicalAddress(InetAddress address) {
        if (address == null) return "unknown";
        if (address.isLoopbackAddress()) {
            return address instanceof Inet6Address ? "::1" : "127.0.0.1";
        }
        if (address.isAnyLocalAddress()) {
            return address instanceof Inet6Address ? "::" : "0.0.0.0";
        }
        return address.getHostAddress();
    }

    private final class ClientConnection implements Closeable {
        private final SocketChannel channel;
        private final BufferedWriter out;
        private final NetworkPacketPolicer packetPolicer;
        private final NetworkThrottlingManager.ChannelBudget outboundBudget;
        private final IndependentHostWireProtocol protocol;
        private final PacketSequenceValidator sequenceValidator;
        private final AtomicBoolean open = new AtomicBoolean(true);

        ClientConnection(SocketChannel channel) throws IOException {
            this.channel = channel;
            String session = "native-" + System.identityHashCode(this)
                    + "-" + Long.toUnsignedString(System.nanoTime(), 36);
            this.packetPolicer = new NetworkPacketPolicer(session);
            this.sequenceValidator = new PacketSequenceValidator(
                    session,
                    4,
                    Duration.ofMillis(250),
                    reason -> {
                        DebugLog.warn("NATIVE_RELAY_SEQUENCE", reason);
                        closeQuietly();
                    });
            this.outboundBudget = throttlingManager.registerChannel(session);
            this.protocol = new IndependentHostWireProtocol(session, sessionLedger);
            this.out = new BufferedWriter(new OutputStreamWriter(
                    Channels.newOutputStream(channel), StandardCharsets.UTF_8));
            write(protocol.helloLine());
            DebugLog.audit("NATIVE_RELAY_CLIENT_HELLO", protocol.auditSummary());
        }

        void readLoop() {
            try (InputStream input = Channels.newInputStream(channel)) {
                String line;
                while (running.get()
                        && open.get()
                        && (line = readBoundedLine(input)) != null) {
                    NetworkPacketPolicer.Decision decision = packetPolicer.inboundPacket();
                    if (!decision.allowed()) {
                        DebugLog.warn("NATIVE_RELAY_RATE", decision.reason());
                        protocol.disconnect("inbound rate limit exceeded");
                        closeQuietly();
                        break;
                    }

                    if (!protocol.relayAccessGranted() || line.startsWith("MECH|")) {
                        IndependentHostWireProtocol.Result result = protocol.accept(line);
                        for (String response : result.responses()) write(response);
                        DebugLog.audit("NATIVE_RELAY_HANDSHAKE", protocol.auditSummary());
                        if (result.disconnect()) {
                            DebugLog.warn("NATIVE_RELAY_HANDSHAKE_DENIED", result.reason());
                            closeQuietly();
                            break;
                        }
                        continue;
                    }

                    if (!line.startsWith("SEQ|")) {
                        write("MECH|DENIED|relay frames require SEQ sequence headers");
                        protocol.disconnect("relay frame omitted sequence header");
                        closeQuietly();
                        break;
                    }
                    PacketSequenceValidator.SequenceDecision sequenceDecision = validateSequence(line);
                    if (sequenceDecision.disconnect()) {
                        protocol.disconnect(sequenceDecision.reason());
                        closeQuietly();
                        break;
                    }
                    if (sequenceDecision.state()
                            == PacketSequenceValidator.SequenceState.QUEUED_WAITING_FOR_GAP) {
                        continue;
                    }
                    try {
                        RemoteSessionLedgerAuthority.SessionSnapshot snapshot =
                                protocol.noteRelayFrameAccepted(
                                        sequenceDecision.incomingSequenceId());
                        DebugLog.audit("REMOTE_SESSION_RELAY_ACCEPTED", snapshot.compactLine());
                    } catch (RuntimeException failure) {
                        write("MECH|DENIED|" + safeProtocolReason(failure.getMessage()));
                        protocol.disconnect(failure.getMessage());
                        closeQuietly();
                        break;
                    }
                    broadcast(line, this);
                }
            } catch (java.net.SocketTimeoutException exception) {
                if (running.get() && open.get()) {
                    DebugLog.warn(
                            "NATIVE_RELAY_SLOWLORIS",
                            "Client timed out during bounded frame read: " + exception.getMessage());
                }
            } catch (IOException exception) {
                if (running.get() && open.get()) {
                    DebugLog.warn(
                            "NATIVE_RELAY_CLIENT",
                            "Client disconnected: " + exception.getMessage());
                }
            } finally {
                closeQuietly();
                clients.remove(this);
                throttlingManager.playerLeft();
            }
        }

        void write(String line) {
            if (!open.get()) return;
            try {
                NetworkPacketPolicer.Decision decision = packetPolicer.outboundPacket();
                if (!decision.allowed()) {
                    DebugLog.warn("NATIVE_RELAY_OUTBOUND_RATE", decision.reason());
                    return;
                }
                byte[] bytes = (line + System.lineSeparator())
                        .getBytes(StandardCharsets.UTF_8);
                outboundBudget.awaitPermit(bytes.length);
                synchronized (out) {
                    out.write(line);
                    out.newLine();
                    out.flush();
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                closeQuietly();
            } catch (IOException exception) {
                closeQuietly();
            }
        }

        private String readBoundedLine(InputStream input) throws IOException {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream(256);
            while (true) {
                int value = input.read();
                if (value < 0) {
                    return buffer.size() == 0
                            ? null
                            : buffer.toString(StandardCharsets.UTF_8);
                }
                if (value == '\n') {
                    return buffer.toString(StandardCharsets.UTF_8)
                            .replaceFirst("\\r$", "");
                }
                buffer.write(value);
                if (buffer.size() > MAX_FRAME_BYTES) {
                    throw new IOException(
                            "incoming frame exceeded " + MAX_FRAME_BYTES + " byte hard cap");
                }
            }
        }

        private PacketSequenceValidator.SequenceDecision validateSequence(String line) {
            int first = line.indexOf('|');
            int second = line.indexOf('|', first + 1);
            if (second <= first) {
                return sequenceValidator.validate(
                        new PacketSequenceValidator.SequencedGamePacket(
                                Long.MAX_VALUE,
                                protocol.sessionId(),
                                System.nanoTime(),
                                line.getBytes(StandardCharsets.UTF_8),
                                Map.of("parse", "missing-separator")));
            }
            try {
                long sequence = Long.parseLong(line.substring(first + 1, second));
                return sequenceValidator.validate(
                        new PacketSequenceValidator.SequencedGamePacket(
                                sequence,
                                protocol.sessionId(),
                                System.nanoTime(),
                                line.substring(second + 1).getBytes(StandardCharsets.UTF_8),
                                Map.of(
                                        "transport", "native",
                                        "profile", protocol.profileIdentity(),
                                        "access", "relay-only")));
            } catch (NumberFormatException exception) {
                return sequenceValidator.validate(
                        new PacketSequenceValidator.SequencedGamePacket(
                                Long.MAX_VALUE,
                                protocol.sessionId(),
                                System.nanoTime(),
                                line.getBytes(StandardCharsets.UTF_8),
                                Map.of("parse", "bad-sequence")));
            }
        }

        void closeQuietly() {
            try {
                close();
            } catch (IOException ignored) {
            }
        }

        @Override
        public void close() throws IOException {
            if (!open.getAndSet(false)) return;
            protocol.disconnect("transport closed");
            outboundBudget.close();
            throttlingManager.unregisterChannel(outboundBudget.sessionId());
            channel.close();
        }
    }

    private static String safeProtocolReason(String reason) {
        String value = reason == null || reason.isBlank()
                ? "session ledger rejected relay frame"
                : reason;
        return value.replace('|', '/').replace('\n', ' ').replace('\r', ' ')
                .substring(0, Math.min(220, value.length()));
    }
}
