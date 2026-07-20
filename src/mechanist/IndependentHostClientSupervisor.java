package mechanist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Supervised independent-host client connection for the limited-alpha control plane.
 *
 * This authority owns handshake progression, local resume-token custody,
 * hosted-command sequencing, canonical roster assembly, asynchronous control
 * versus relay dispatch, and orderly shutdown. It intentionally exposes no
 * movement, combat, inventory, world-snapshot, or gameplay command API.
 */
final class IndependentHostClientSupervisor implements AutoCloseable {
    static final String VERSION = "independent-host-client-supervisor-1";
    private static final int MAX_RELAY_PAYLOAD_CHARS = 60 * 1024;

    enum State {
        NEW,
        CONNECTING,
        AUTHENTICATED,
        AUTHENTICATED_VOLATILE_TOKEN,
        FAILED,
        CLOSED
    }

    private final IndependentHostResumeTokenStore tokenStore;
    private final String serverKey;
    private final String profileIdentity;
    private final HostedRosterClientAuthority rosterAuthority =
            new HostedRosterClientAuthority();
    private final BlockingQueue<RelayFrame> relayInbox =
            new LinkedBlockingQueue<>(1024);
    private final AtomicLong relaySequence = new AtomicLong();
    private final AtomicLong hostedCommandSequence = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<CompletableFuture<HostedCommandAck>> pendingCommand =
            new AtomicReference<>();
    private final Object writeLock = new Object();
    private final Object commandLock = new Object();
    private final Object rosterSignal = new Object();

    private volatile State state = State.NEW;
    private volatile String lastEvent = "not connected";
    private volatile Throwable failure;
    private volatile ConnectionIdentity identity;
    private volatile SessionSnapshot sessionSnapshot;
    private volatile long rosterEvents;
    private volatile boolean resumeTokenPersisted;
    private volatile String tokenPersistenceFailure = "none";
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread readerThread;

    IndependentHostClientSupervisor(
            IndependentHostResumeTokenStore tokenStore,
            String serverKey,
            String profileIdentity
    ) {
        this.tokenStore = Objects.requireNonNull(tokenStore, "tokenStore");
        this.serverKey = safeKey(serverKey, "server key");
        this.profileIdentity = safeProfile(profileIdentity);
    }

    synchronized ConnectionIdentity connect(
            String host,
            int port,
            Duration timeout
    ) throws Exception {
        if (state != State.NEW) {
            throw new IllegalStateException(
                    "client supervisor can connect only from NEW state; current="
                            + state);
        }
        String useHost = safeHost(host);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        Duration useTimeout = boundedTimeout(timeout);
        state = State.CONNECTING;
        lastEvent = "connecting to " + useHost + ":" + port;
        Optional<IndependentHostResumeTokenStore.Record> stored =
                tokenStore.load(serverKey, profileIdentity);
        try {
            socket = new Socket();
            int timeoutMillis = Math.toIntExact(useTimeout.toMillis());
            socket.connect(new InetSocketAddress(useHost, port), timeoutMillis);
            socket.setSoTimeout(timeoutMillis);
            reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));

            ConnectionIdentity connected = completeHandshake(stored);
            identity = connected;
            try {
                tokenStore.save(
                        serverKey,
                        profileIdentity,
                        connected.playerId(),
                        connected.resumeToken(),
                        connected.connectionGeneration());
                resumeTokenPersisted = true;
                state = State.AUTHENTICATED;
            } catch (IOException custodyFailure) {
                resumeTokenPersisted = false;
                tokenPersistenceFailure = safeReason(custodyFailure.getMessage());
                state = State.AUTHENTICATED_VOLATILE_TOKEN;
            }

            socket.setSoTimeout(0);
            running.set(true);
            readerThread = new Thread(
                    this::readLoop,
                    "mechanist-independent-host-client-reader");
            readerThread.setDaemon(true);
            readerThread.start();
            requestRosterRefresh(useTimeout);
            lastEvent = "authenticated relay-only session mounted";
            return connected;
        } catch (Throwable connectFailure) {
            fail(connectFailure, "connection or handshake failed");
            closeTransportQuietly();
            throw connectFailure;
        }
    }

    HostedCommandAck setReady(boolean ready, Duration timeout)
            throws Exception {
        return sendHostedCommand(
                "READY",
                Boolean.toString(ready),
                timeout);
    }

    HostedCommandAck setPresence(String presence, Duration timeout)
            throws Exception {
        String value = enumToken(
                presence,
                "presence",
                "available",
                "away",
                "busy");
        return sendHostedCommand("PRESENCE", value, timeout);
    }

    HostedCommandAck setChatState(String chatState, Duration timeout)
            throws Exception {
        String value = enumToken(
                chatState,
                "chat state",
                "idle",
                "typing");
        return sendHostedCommand("CHAT_STATE", value, timeout);
    }

    HostedCommandAck sendHostedCommand(
            String command,
            String value,
            Duration timeout
    ) throws Exception {
        ensureAuthenticated();
        String useCommand = enumToken(
                command,
                "hosted command",
                "ready",
                "presence",
                "chat_state").toUpperCase(Locale.ROOT);
        String useValue = safeControlValue(value);
        Duration useTimeout = boundedTimeout(timeout);
        synchronized (commandLock) {
            long commandId = hostedCommandSequence.getAndIncrement();
            CompletableFuture<HostedCommandAck> future = new CompletableFuture<>();
            if (!pendingCommand.compareAndSet(null, future)) {
                throw new IllegalStateException(
                        "another hosted command is already awaiting acknowledgement");
            }
            try {
                writeLine("MECH|SESSION_COMMAND|" + commandId
                        + "|" + useCommand + "|" + useValue);
                HostedCommandAck acknowledgement;
                try {
                    acknowledgement = future.get(
                            useTimeout.toMillis(),
                            TimeUnit.MILLISECONDS);
                } catch (java.util.concurrent.TimeoutException timedOut) {
                    throw new TimeoutException(
                            "timed out waiting for hosted-command acknowledgement");
                }
                if (acknowledgement.commandId() != commandId
                        || !useCommand.equals(acknowledgement.command())) {
                    throw new IllegalStateException(
                            "hosted-command acknowledgement does not match its request");
                }
                waitForRosterVersion(
                        acknowledgement.snapshotVersion(),
                        useTimeout);
                return acknowledgement;
            } finally {
                pendingCommand.compareAndSet(future, null);
            }
        }
    }

    long sendRelayPayload(String payload) throws IOException {
        ensureAuthenticated();
        String use = Objects.requireNonNullElse(payload, "");
        if (use.isBlank()
                || use.length() > MAX_RELAY_PAYLOAD_CHARS
                || use.indexOf('\n') >= 0
                || use.indexOf('\r') >= 0
                || use.startsWith("MECH|")) {
            throw new IllegalArgumentException(
                    "relay payload is blank, oversized, multiline, or a control frame");
        }
        long sequence = relaySequence.getAndIncrement();
        writeLine("SEQ|" + sequence + "|" + use);
        return sequence;
    }

    RelayFrame pollRelay(Duration timeout) throws InterruptedException {
        Duration useTimeout = boundedTimeout(timeout);
        return relayInbox.poll(
                useTimeout.toMillis(),
                TimeUnit.MILLISECONDS);
    }

    HostedRosterClientAuthority.Snapshot requestRosterRefresh(
            Duration timeout
    ) throws Exception {
        ensureAuthenticatedOrConnectingReader();
        Duration useTimeout = boundedTimeout(timeout);
        long before;
        synchronized (rosterSignal) {
            before = rosterEvents;
        }
        writeLine("MECH|SESSION_ROSTER");
        long deadline = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(useTimeout.toMillis());
        synchronized (rosterSignal) {
            while (rosterEvents <= before) {
                Throwable currentFailure = failure;
                if (currentFailure != null) {
                    throw new IOException(
                            "independent-host reader failed",
                            currentFailure);
                }
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0L) {
                    throw new TimeoutException(
                            "timed out waiting for hosted roster");
                }
                TimeUnit.NANOSECONDS.timedWait(rosterSignal, remaining);
            }
        }
        HostedRosterClientAuthority.Snapshot snapshot = rosterAuthority.latest();
        if (snapshot == null) {
            throw new IllegalStateException(
                    "roster event completed without an immutable snapshot");
        }
        return snapshot;
    }

    HostedRosterClientAuthority.Snapshot latestRoster() {
        return rosterAuthority.latest();
    }

    SessionSnapshot latestSessionSnapshot() {
        return sessionSnapshot;
    }

    ConnectionIdentity identity() {
        return identity;
    }

    State state() {
        return state;
    }

    boolean resumeTokenPersisted() {
        return resumeTokenPersisted;
    }

    String statusLine() {
        ConnectionIdentity current = identity;
        HostedRosterClientAuthority.Snapshot roster = rosterAuthority.latest();
        return "authority=" + VERSION
                + " state=" + state
                + " serverKey=" + serverKey
                + " profile=" + profileIdentity
                + " player=" + (current == null ? "none" : current.playerId())
                + " generation=" + (current == null
                        ? 0
                        : current.connectionGeneration())
                + " resumed=" + (current != null && current.resumed())
                + " resumeTokenPersisted=" + resumeTokenPersisted
                + " tokenPersistenceFailure=" + tokenPersistenceFailure
                + " rosterVersion=" + (roster == null ? 0 : roster.version())
                + " visiblePlayers=" + (roster == null
                        ? 0
                        : roster.visiblePlayers())
                + " relayInbox=" + relayInbox.size()
                + " hostedCommandSequence=" + hostedCommandSequence.get()
                + " relaySequence=" + relaySequence.get()
                + " lastEvent=" + lastEvent
                + " tokenInDiagnostics=false"
                + " worldAuthority=false";
    }

    private ConnectionIdentity completeHandshake(
            Optional<IndependentHostResumeTokenStore.Record> stored
    ) throws Exception {
        String[] hello = requireCommand(readRequired("HELLO"), "HELLO", 6);
        if (!IndependentHostWireProtocol.VERSION.equals(hello[2])) {
            throw new IOException(
                    "server advertised unsupported wire protocol " + hello[2]);
        }
        if (!"RELAY_ONLY".equals(hello[5])) {
            throw new SecurityException(
                    "server attempted to grant an unsupported access class");
        }
        String resumeToken = stored
                .map(IndependentHostResumeTokenStore.Record::resumeToken)
                .orElse("");
        writeLine("MECH|IDENTITY|" + profileIdentity
                + (resumeToken.isBlank() ? "" : "|" + resumeToken));
        String[] manifest = requireCommand(
                readRequired("MANIFEST"),
                "MANIFEST",
                6);
        String fingerprint = manifest[3].toLowerCase(Locale.ROOT);
        if (!fingerprint.matches("[a-f0-9]{64}")) {
            throw new IOException(
                    "server manifest fingerprint is not SHA-256");
        }
        writeLine("MECH|ACQUIRED|" + fingerprint);
        String[] restart = requireCommand(
                readRequired("RESTART_REQUIRED"),
                "RESTART_REQUIRED",
                3);
        if (!fingerprint.equals(restart[2])) {
            throw new IOException(
                    "server restart requirement changed the manifest fingerprint");
        }
        writeLine("MECH|RESTARTED|" + fingerprint);
        String[] challenge = requireCommand(
                readRequired("CHALLENGE"),
                "CHALLENGE",
                5);
        if (!fingerprint.equals(challenge[3])
                || !fingerprint.equals(challenge[4])) {
            throw new IOException(
                    "server integrity challenge changed the mounted manifest");
        }
        String digest = SecureHandshakeStateMachine.computeIntegrityDigest(
                challenge[2],
                challenge[3],
                challenge[4]);
        writeLine("MECH|CHALLENGE_RESPONSE|" + digest);
        String accessLine = readRequired("ACCESS");
        if (accessLine.startsWith("MECH|DENIED|")) {
            throw new SecurityException(
                    "server denied independent-host access: "
                            + safeReason(accessLine.substring(
                            "MECH|DENIED|".length())));
        }
        String[] access = requireCommand(accessLine, "ACCESS", 11);
        if (!"RELAY_ONLY".equals(access[2])) {
            throw new SecurityException(
                    "server granted an unsupported access class");
        }
        if (!profileIdentity.equals(access[5])) {
            throw new SecurityException(
                    "server returned a different profile identity");
        }
        String playerId = access[6];
        String issuedToken = access[7].toLowerCase(Locale.ROOT);
        if (!playerId.matches("remote-[a-f0-9]{20}")) {
            throw new IOException("server returned an invalid player id");
        }
        if (!issuedToken.matches("[a-f0-9]{64}")) {
            throw new IOException("server returned an invalid resume token");
        }
        long generation = positiveLong(
                access[8],
                "connection generation");
        long snapshotVersion = nonNegativeLong(
                access[9],
                "access snapshot version");
        boolean resumed = strictBoolean(access[10], "resumed flag");
        if (stored.isPresent()) {
            IndependentHostResumeTokenStore.Record previous = stored.get();
            if (!previous.playerId().equals(playerId)) {
                throw new SecurityException(
                        "resume changed the stable server-owned player id");
            }
            if (!resumed || generation <= previous.connectionGeneration()) {
                throw new SecurityException(
                        "resume did not advance the connection generation");
            }
        } else if (resumed) {
            throw new SecurityException(
                    "server reported a resumed session without a local resume token");
        }
        return new ConnectionIdentity(
                serverKey,
                profileIdentity,
                playerId,
                issuedToken,
                generation,
                snapshotVersion,
                resumed,
                "RELAY_ONLY");
    }

    private void readLoop() {
        try {
            String line;
            while (running.get() && (line = reader.readLine()) != null) {
                if (line.startsWith("MECH|HOSTED_ROSTER_BEGIN|")) {
                    HostedRosterClientAuthority.Snapshot roster =
                            HostedRosterStreamReader.read(
                                    reader,
                                    line,
                                    rosterAuthority);
                    synchronized (rosterSignal) {
                        rosterEvents++;
                        rosterSignal.notifyAll();
                    }
                    lastEvent = "accepted hosted roster version "
                            + roster.version();
                    continue;
                }
                if (line.startsWith("MECH|SESSION_COMMAND_ACCEPTED|")) {
                    HostedCommandAck acknowledgement = parseCommandAck(line);
                    CompletableFuture<HostedCommandAck> pending =
                            pendingCommand.get();
                    if (pending == null) {
                        throw new IOException(
                                "unsolicited hosted-command acknowledgement");
                    }
                    pending.complete(acknowledgement);
                    continue;
                }
                if (line.startsWith("MECH|SESSION_SNAPSHOT|")) {
                    sessionSnapshot = parseSessionSnapshot(line);
                    continue;
                }
                if (line.startsWith("MECH|PONG|")) {
                    lastEvent = "received host pong";
                    continue;
                }
                if (line.startsWith("MECH|DENIED|")) {
                    throw new SecurityException(
                            "server denied the client session: "
                                    + safeReason(line.substring(
                                    "MECH|DENIED|".length())));
                }
                if (line.startsWith("MECH|")) {
                    throw new IOException(
                            "unknown independent-host control frame: "
                                    + safeReason(line));
                }
                RelayFrame relay = parseRelayFrame(line);
                if (!relayInbox.offer(relay)) {
                    throw new IOException(
                            "relay inbox exceeded its bounded capacity");
                }
            }
            if (running.get()) {
                throw new IOException(
                        "independent host closed the connection");
            }
        } catch (Throwable readerFailure) {
            if (running.get()) {
                fail(readerFailure, "reader loop failed");
            }
        } finally {
            running.set(false);
            CompletableFuture<HostedCommandAck> pending =
                    pendingCommand.getAndSet(null);
            if (pending != null && !pending.isDone()) {
                pending.completeExceptionally(
                        failure == null
                                ? new IOException("client session closed")
                                : failure);
            }
            synchronized (rosterSignal) {
                rosterSignal.notifyAll();
            }
            closeTransportQuietly();
        }
    }

    private HostedCommandAck parseCommandAck(String line)
            throws IOException {
        String[] fields = requireCommand(line, "SESSION_COMMAND_ACCEPTED", 11);
        return new HostedCommandAck(
                nonNegativeLong(fields[2], "hosted command id"),
                fields[3],
                fields[4],
                nonNegativeLong(fields[5], "command snapshot version"),
                strictBoolean(fields[6], "ready flag"),
                fields[7],
                fields[8],
                nonNegativeLong(fields[9], "accepted hosted commands"),
                nonNegativeLong(fields[10], "last connection command id"));
    }

    private SessionSnapshot parseSessionSnapshot(String line)
            throws IOException {
        String[] fields = requireCommand(line, "SESSION_SNAPSHOT", 10);
        return new SessionSnapshot(
                fields[2],
                fields[3],
                nonNegativeLong(fields[4], "session snapshot version"),
                strictBoolean(fields[5], "connected flag"),
                positiveLong(fields[6], "connection generation"),
                nonNegativeLong(fields[7], "accepted relay frames"),
                signedSequence(fields[8], "last connection sequence"),
                fields[9]);
    }

    private RelayFrame parseRelayFrame(String line) throws IOException {
        if (!line.startsWith("SEQ|")) {
            throw new IOException(
                    "non-control line is not a sequenced relay frame");
        }
        int second = line.indexOf('|', 4);
        if (second < 0) {
            throw new IOException("relay frame is missing its payload separator");
        }
        long sequence = nonNegativeLong(
                line.substring(4, second),
                "relay sequence");
        String payload = line.substring(second + 1);
        if (payload.isBlank()
                || payload.length() > MAX_RELAY_PAYLOAD_CHARS
                || payload.indexOf('\n') >= 0
                || payload.indexOf('\r') >= 0) {
            throw new IOException("relay payload is invalid");
        }
        return new RelayFrame(sequence, payload);
    }

    private void waitForRosterVersion(
            long minimumVersion,
            Duration timeout
    ) throws Exception {
        long deadline = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(timeout.toMillis());
        synchronized (rosterSignal) {
            while (true) {
                HostedRosterClientAuthority.Snapshot roster =
                        rosterAuthority.latest();
                if (roster != null && roster.version() >= minimumVersion) return;
                if (failure != null) {
                    throw new IOException(
                            "client reader failed before roster confirmation",
                            failure);
                }
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0L) {
                    throw new TimeoutException(
                            "timed out waiting for authoritative roster confirmation");
                }
                TimeUnit.NANOSECONDS.timedWait(rosterSignal, remaining);
            }
        }
    }

    private void writeLine(String line) throws IOException {
        BufferedWriter useWriter = writer;
        if (useWriter == null) {
            throw new IOException("independent-host writer is not mounted");
        }
        synchronized (writeLock) {
            useWriter.write(line);
            useWriter.newLine();
            useWriter.flush();
        }
    }

    private String readRequired(String expected) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("server closed before " + expected);
        }
        return line;
    }

    private void ensureAuthenticated() {
        if (state != State.AUTHENTICATED
                && state != State.AUTHENTICATED_VOLATILE_TOKEN) {
            throw new IllegalStateException(
                    "independent-host client is not authenticated; state="
                            + state);
        }
        if (!running.get()) {
            throw new IllegalStateException(
                    "independent-host reader is not running");
        }
    }

    private void ensureAuthenticatedOrConnectingReader() {
        if (state != State.CONNECTING
                && state != State.AUTHENTICATED
                && state != State.AUTHENTICATED_VOLATILE_TOKEN) {
            throw new IllegalStateException(
                    "independent-host client cannot request a roster in state "
                            + state);
        }
        if (state != State.CONNECTING && !running.get()) {
            throw new IllegalStateException(
                    "independent-host reader is not running");
        }
    }

    private synchronized void fail(Throwable cause, String event) {
        failure = cause;
        state = State.FAILED;
        lastEvent = event + ": " + safeReason(
                cause == null ? "unknown" : cause.getMessage());
        running.set(false);
        synchronized (rosterSignal) {
            rosterSignal.notifyAll();
        }
    }

    @Override
    public synchronized void close() {
        if (state == State.CLOSED) return;
        running.set(false);
        closeTransportQuietly();
        Thread thread = readerThread;
        if (thread != null && thread != Thread.currentThread()) {
            thread.interrupt();
            try {
                thread.join(2_000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        state = State.CLOSED;
        lastEvent = "client supervisor closed";
        synchronized (rosterSignal) {
            rosterSignal.notifyAll();
        }
    }

    private void closeTransportQuietly() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }

    private static String[] requireCommand(
            String line,
            String command,
            int expectedFields
    ) throws IOException {
        String[] fields = Objects.requireNonNullElse(line, "")
                .split("\\|", -1);
        if (fields.length != expectedFields
                || !"MECH".equals(fields[0])
                || !command.equals(fields[1])) {
            throw new IOException(
                    "expected MECH|" + command + " with "
                            + expectedFields + " fields but received "
                            + Arrays.toString(fields));
        }
        return fields;
    }

    private static Duration boundedTimeout(Duration timeout) {
        Duration use = timeout == null ? Duration.ofSeconds(5) : timeout;
        if (use.isNegative()
                || use.isZero()
                || use.compareTo(Duration.ofMinutes(2)) > 0) {
            throw new IllegalArgumentException(
                    "timeout must be greater than zero and no more than two minutes");
        }
        return use;
    }

    private static String safeHost(String value) {
        String host = Objects.requireNonNullElse(value, "").trim();
        if (host.isBlank()
                || host.length() > 255
                || host.indexOf('|') >= 0
                || host.indexOf('\n') >= 0
                || host.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("host is invalid");
        }
        return host;
    }

    private static String safeKey(String value, String label) {
        String token = Objects.requireNonNullElse(value, "").trim();
        if (token.isBlank()
                || token.length() > 256
                || token.indexOf('|') >= 0
                || token.indexOf('\n') >= 0
                || token.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(label + " is invalid");
        }
        return token;
    }

    private static String safeProfile(String value) {
        String profile = safeKey(value, "profile identity");
        if (profile.length() < 8) {
            throw new IllegalArgumentException(
                    "profile identity must be at least eight characters");
        }
        for (int index = 0; index < profile.length(); index++) {
            char c = profile.charAt(index);
            if (!(Character.isLetterOrDigit(c)
                    || c == '.'
                    || c == '_'
                    || c == ':'
                    || c == '-')) {
                throw new IllegalArgumentException(
                        "profile identity contains an unsafe character");
            }
        }
        return profile;
    }

    private static String safeControlValue(String value) {
        String token = Objects.requireNonNullElse(value, "").trim();
        if (token.isBlank()
                || token.length() > 64
                || token.indexOf('|') >= 0
                || token.indexOf('\n') >= 0
                || token.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(
                    "hosted-command value is invalid");
        }
        return token;
    }

    private static String enumToken(
            String value,
            String label,
            String... allowed
    ) {
        String token = safeControlValue(value).toLowerCase(Locale.ROOT);
        for (String candidate : allowed) {
            if (candidate.equals(token)) return token;
        }
        throw new IllegalArgumentException(
                label + " must be one of " + Arrays.toString(allowed));
    }

    private static long nonNegativeLong(String value, String label)
            throws IOException {
        try {
            long parsed = Long.parseLong(
                    Objects.requireNonNullElse(value, "").trim());
            if (parsed < 0L) throw new NumberFormatException("negative");
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IOException(label + " is invalid", failure);
        }
    }

    private static long positiveLong(String value, String label)
            throws IOException {
        long parsed = nonNegativeLong(value, label);
        if (parsed < 1L) throw new IOException(label + " must be positive");
        return parsed;
    }

    private static long signedSequence(String value, String label)
            throws IOException {
        try {
            long parsed = Long.parseLong(
                    Objects.requireNonNullElse(value, "").trim());
            if (parsed < -1L) throw new NumberFormatException("below -1");
            return parsed;
        } catch (NumberFormatException failure) {
            throw new IOException(label + " is invalid", failure);
        }
    }

    private static boolean strictBoolean(String value, String label)
            throws IOException {
        String token = Objects.requireNonNullElse(value, "")
                .trim()
                .toLowerCase(Locale.ROOT);
        if (!"true".equals(token) && !"false".equals(token)) {
            throw new IOException(label + " must be true or false");
        }
        return Boolean.parseBoolean(token);
    }

    private static String safeReason(String value) {
        String reason = Objects.requireNonNullElse(value, "unspecified")
                .replace('|', '/')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        return reason.isBlank()
                ? "unspecified"
                : reason.substring(0, Math.min(180, reason.length()));
    }

    record ConnectionIdentity(
            String serverKey,
            String profileIdentity,
            String playerId,
            String resumeToken,
            long connectionGeneration,
            long accessSnapshotVersion,
            boolean resumed,
            String accessClass
    ) { }

    record HostedCommandAck(
            long commandId,
            String command,
            String value,
            long snapshotVersion,
            boolean ready,
            String presence,
            String chatState,
            long acceptedHostedCommands,
            long lastConnectionCommandId
    ) { }

    record SessionSnapshot(
            String playerId,
            String worldId,
            long snapshotVersion,
            boolean connected,
            long connectionGeneration,
            long acceptedRelayFrames,
            long lastConnectionSequence,
            String lastEvent
    ) { }

    record RelayFrame(long sequence, String payload) { }
}
