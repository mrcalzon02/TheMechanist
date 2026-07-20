package mechanist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.util.stream.Stream;

/** Packaged socket smoke for atomic remote-session continuity across a clean host restart. */
final class IndependentHostPersistentSessionRestartSmoke {
    private static final String LOOPBACK = "127.0.0.1";
    private static final String WORLD_ID = "persistent-relay-session-smoke";
    private static final String PROFILE = "alpha.persistent.0004";

    public static void main(String[] args) throws Exception {
        Path ledgerFile = ServerRuntimePaths.remoteSessionLedgerPath(WORLD_ID);
        clearSmokeLedger(ledgerFile);
        String token;
        String playerId;
        long firstSnapshotVersion;

        int firstPort = NetworkPortAuthority.firstAvailableGamePort();
        HostBindingResult first = MultiplayerHostBindingService.bind(
                config(firstPort));
        require(first.success(),
                "persistent relay first bind failed: " + first.compactLine());
        require(first.session() instanceof NativeTcpRelayServer,
                "persistent relay first bind did not use native host session");
        NativeTcpRelayServer firstRelay = (NativeTcpRelayServer) first.session();
        NativeTcpRelayServer.PathReadout firstPath = firstRelay.remoteSessionPersistence();
        require(firstPath.persistenceEnabled(),
                "live relay did not enable remote-session persistence");
        require(ledgerFile.toAbsolutePath().normalize().equals(firstPath.persistenceFile()),
                "live relay selected the wrong remote-session ledger file");

        try (Client client = Client.connect(first.port(), PROFILE, "")) {
            require(!client.resumed,
                    "fresh persistent session incorrectly reported resume");
            require(client.connectionGeneration == 1L,
                    "fresh persistent session did not begin at generation one");
            token = client.resumeToken;
            playerId = client.playerId;
            client.write("SEQ|0|persistent-before-restart");
            waitForFrameCount(firstRelay, PROFILE, 1L, 3_000L);
            SessionReadout status = client.sessionStatus();
            require(status.connected && status.acceptedRelayFrames == 1L,
                    "first persistent session snapshot did not account for its frame");
            firstSnapshotVersion = status.snapshotVersion;
        } finally {
            first.close();
        }

        require(Files.isRegularFile(ledgerFile),
                "clean host shutdown did not leave a remote-session ledger");
        String persisted = Files.readString(
                ledgerFile, StandardCharsets.ISO_8859_1);
        require(!persisted.contains(token),
                "live relay persisted a reusable plaintext resume token");
        require(persisted.contains(sha256Hex(token)),
                "live relay did not persist the resume-token hash");
        require(!hasTemporarySibling(ledgerFile),
                "live relay left an atomic-write temporary sibling");

        int secondPort = NetworkPortAuthority.firstAvailableGamePort();
        HostBindingResult second = MultiplayerHostBindingService.bind(
                config(secondPort));
        require(second.success(),
                "persistent relay restart bind failed: " + second.compactLine());
        require(second.session() instanceof NativeTcpRelayServer,
                "persistent relay restart did not use native host session");
        NativeTcpRelayServer secondRelay = (NativeTcpRelayServer) second.session();
        try {
            require(secondRelay.remoteSessionCount() == 1
                            && secondRelay.activeRemoteSessionCount() == 0,
                    "restarted relay did not restore exactly one offline session");
            RemoteSessionLedgerAuthority.SessionSnapshot restoredOffline =
                    secondRelay.remoteSessionSnapshot(PROFILE);
            require(restoredOffline != null && !restoredOffline.connected(),
                    "restarted relay did not force restored session offline");
            require(restoredOffline.playerId().equals(playerId),
                    "restarted relay changed the stable player identity");
            require(restoredOffline.acceptedRelayFrames() == 1L,
                    "restarted relay lost lifetime relay accounting");
            require(restoredOffline.version() > firstSnapshotVersion,
                    "restarted relay did not advance the immutable ledger version");

            try (Client resumed = Client.connect(
                    second.port(), PROFILE, token)) {
                require(resumed.resumed,
                        "valid token did not resume after full host restart");
                require(resumed.playerId.equals(playerId),
                        "host restart resume changed the stable player id");
                require(resumed.connectionGeneration == 2L,
                        "host restart resume did not advance connection generation");
                SessionReadout beforeFrame = resumed.sessionStatus();
                require(beforeFrame.acceptedRelayFrames == 1L,
                        "host restart resume lost prior relay accounting");
                resumed.write("SEQ|0|persistent-after-restart");
                waitForFrameCount(secondRelay, PROFILE, 2L, 3_000L);
                SessionReadout afterFrame = resumed.sessionStatus();
                require(afterFrame.connectionGeneration == 2L,
                        "post-restart snapshot lost connection generation");
                require(afterFrame.acceptedRelayFrames == 2L,
                        "post-restart snapshot did not preserve lifetime accounting");
                require(afterFrame.lastConnectionSequence == 0L,
                        "post-restart connection did not restart sequence at zero");
                require(afterFrame.snapshotVersion > beforeFrame.snapshotVersion,
                        "post-restart relay frame did not advance snapshot version");
            }
        } finally {
            second.close();
        }

        String valid = Files.readString(ledgerFile, StandardCharsets.ISO_8859_1);
        Files.writeString(
                ledgerFile,
                valid.replaceFirst("schema=1", "schema=999"),
                StandardCharsets.ISO_8859_1);
        int corruptPort = NetworkPortAuthority.firstAvailableGamePort();
        HostBindingResult corrupt = MultiplayerHostBindingService.bind(
                config(corruptPort));
        try {
            require(!corrupt.success(),
                    "corrupted remote-session ledger unexpectedly allowed host bind");
            require(corrupt.message().toLowerCase().contains("schema")
                            || corrupt.message().toLowerCase().contains("failed"),
                    "corrupted-ledger bind failure was not diagnosable: "
                            + corrupt.compactLine());
        } finally {
            corrupt.close();
            clearSmokeLedger(ledgerFile);
        }

        System.out.println("IndependentHostPersistentSessionRestartSmoke PASS"
                + " atomicLedger=true"
                + " tokenStorage=sha256-only"
                + " cleanHostRestartResume=true"
                + " stablePlayerIdentity=true"
                + " generationContinuity=true"
                + " lifetimeRelayAccounting=true"
                + " corruptedLedgerRejected=true"
                + " worldAuthority=false"
                + " gameplaySessionCertified=false");
    }

    private static ServerConfig config(int port) {
        return ServerConfig.fromWorldSettings(
                0x1A17A2026L,
                "Persistent Limited Alpha Relay",
                WORLD_ID,
                WorldSetupSettings.standard(),
                4,
                port,
                LOOPBACK,
                MultiplayerProtocolState.CLOSED,
                false);
    }

    private static void waitForFrameCount(
            NativeTcpRelayServer relay,
            String profile,
            long expected,
            long timeoutMillis
    ) throws Exception {
        long deadline = System.nanoTime() + timeoutMillis * 1_000_000L;
        while (System.nanoTime() < deadline) {
            RemoteSessionLedgerAuthority.SessionSnapshot snapshot =
                    relay.remoteSessionSnapshot(profile);
            if (snapshot != null && snapshot.acceptedRelayFrames() == expected) return;
            Thread.sleep(20L);
        }
        RemoteSessionLedgerAuthority.SessionSnapshot snapshot =
                relay.remoteSessionSnapshot(profile);
        throw new AssertionError(
                "relay frame count did not reach " + expected + "; snapshot="
                        + (snapshot == null ? "missing" : snapshot.compactLine()));
    }

    private static void clearSmokeLedger(Path ledgerFile) throws Exception {
        Files.deleteIfExists(ledgerFile);
        Path parent = ledgerFile.getParent();
        if (parent == null || !Files.isDirectory(parent)) return;
        String prefix = ledgerFile.getFileName() + ".tmp-";
        try (Stream<Path> paths = Files.list(parent)) {
            for (Path path : paths.filter(
                    item -> item.getFileName().toString().startsWith(prefix)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static boolean hasTemporarySibling(Path ledgerFile) throws Exception {
        Path parent = ledgerFile.getParent();
        if (parent == null || !Files.isDirectory(parent)) return false;
        String prefix = ledgerFile.getFileName() + ".tmp-";
        try (Stream<Path> paths = Files.list(parent)) {
            return paths.anyMatch(
                    path -> path.getFileName().toString().startsWith(prefix));
        }
    }

    private static String sha256Hex(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(
                digest.digest(value.getBytes(StandardCharsets.UTF_8)));
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
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

    private record Challenge(
            String salt,
            String manifestFingerprint,
            String mountedFingerprint
    ) {
        String digest() {
            return SecureHandshakeStateMachine.computeIntegrityDigest(
                    salt, manifestFingerprint, mountedFingerprint);
        }
    }

    private static final class Client implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final String playerId;
        private final String resumeToken;
        private final long connectionGeneration;
        private final boolean resumed;

        private Client(
                Socket socket,
                BufferedReader reader,
                BufferedWriter writer,
                String playerId,
                String resumeToken,
                long connectionGeneration,
                boolean resumed
        ) {
            this.socket = socket;
            this.reader = reader;
            this.writer = writer;
            this.playerId = playerId;
            this.resumeToken = resumeToken;
            this.connectionGeneration = connectionGeneration;
            this.resumed = resumed;
        }

        static Client connect(
                int port,
                String profile,
                String resumeToken
        ) throws Exception {
            Socket socket = IndependentHostPersistentSessionRestartSmoke.connect(port);
            BufferedReader reader = reader(socket);
            BufferedWriter writer = writer(socket);
            try {
                String[] hello = requireCommand(reader.readLine(), "HELLO");
                require(hello.length == 6
                                && IndependentHostWireProtocol.VERSION.equals(hello[2])
                                && "RELAY_ONLY".equals(hello[5]),
                        "persistent client received invalid HELLO: "
                                + Arrays.toString(hello));
                writeLine(writer, "MECH|IDENTITY|" + profile
                        + (resumeToken == null || resumeToken.isBlank()
                                ? ""
                                : "|" + resumeToken));
                String[] manifest = requireCommand(reader.readLine(), "MANIFEST");
                require(manifest.length == 6,
                        "persistent client received invalid MANIFEST");
                String fingerprint = manifest[3];
                writeLine(writer, "MECH|ACQUIRED|" + fingerprint);
                requireCommand(reader.readLine(), "RESTART_REQUIRED");
                writeLine(writer, "MECH|RESTARTED|" + fingerprint);
                String[] challenge = requireCommand(reader.readLine(), "CHALLENGE");
                Challenge use = new Challenge(
                        challenge[2], challenge[3], challenge[4]);
                writeLine(writer, "MECH|CHALLENGE_RESPONSE|" + use.digest());
                String[] access = requireCommand(reader.readLine(), "ACCESS");
                require(access.length == 11
                                && "RELAY_ONLY".equals(access[2])
                                && profile.equals(access[5]),
                        "persistent client received invalid ACCESS: "
                                + Arrays.toString(access));
                return new Client(
                        socket,
                        reader,
                        writer,
                        access[6],
                        access[7],
                        Long.parseLong(access[8]),
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

        SessionReadout sessionStatus() throws Exception {
            write("MECH|SESSION_STATUS");
            String[] snapshot = requireCommand(reader.readLine(), "SESSION_SNAPSHOT");
            require(snapshot.length == 10,
                    "persistent client received invalid SESSION_SNAPSHOT");
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

    private IndependentHostPersistentSessionRestartSmoke() { }
}
