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
 * This is the single client authority for handshake progression, local resume-token
 * custody, hosted-command sequencing, the wait-only authoritative world command,
 * canonical roster assembly, control/relay dispatch, failure state, and orderly
 * shutdown. It exposes no movement, map, combat, inventory, interaction,
 * world-snapshot request, or generic gameplay-command API.
 */
final class IndependentHostClientSupervisor implements AutoCloseable {
    static final String VERSION = "independent-host-client-supervisor-3";
    private static final int MAX_RELAY_PAYLOAD_CHARS = 60 * 1024;
    private static final int MAX_RELAY_INBOX = 1024;

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
            new LinkedBlockingQueue<>(MAX_RELAY_INBOX);
    private final AtomicLong relaySequence = new AtomicLong();
    private final AtomicLong hostedCommandSequence = new AtomicLong();
    private final AtomicLong worldCommandSequence = new AtomicLong();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<PendingCommand> pendingCommand =
            new AtomicReference<>();
    private final AtomicReference<PendingWorldCommand> pendingWorldCommand =
            new AtomicReference<>();
    private final Object lifecycleLock = new Object();
    private final Object writeLock = new Object();
    private final Object commandLock = new Object();
    private final Object worldCommandLock = new Object();
    private final Object rosterSignal = new Object();

    private volatile State state = State.NEW;
    private volatile String lastEvent = "not connected";
    private volatile Throwable failure;
    private volatile ConnectionIdentity identity;
    private volatile SessionSnapshot sessionSnapshot;
    private volatile WorldWaitAck latestWorldWait;
    private volatile long rosterEvents;
    private volatile boolean resumeTokenPersisted;
    private volatile String tokenPersistenceFailure = "none";
    private volatile Socket socket;
    private volatile BufferedReader reader;
    private volatile BufferedWriter writer;
    private volatile Thread readerThread;

    IndependentHostClientSupervisor(
            IndependentHostResumeTokenStore tokenStore,
            String serverKey,
            String profileIdentity
    ) {
        this.tokenStore = Objects.requireNonNull(tokenStore, "tokenStore");
        this.serverKey = safeKey(serverKey, "server key");
        this.profileIdentity = safeProfile(profileIdentity);
    }

    ConnectionIdentity connect(
            String host,
            int port,
            Duration timeout
    ) throws Exception {
        String useHost = safeHost(host);
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
        Duration useTimeout = boundedTimeout(timeout);
        synchronized (lifecycleLock) {
            if (state != State.NEW) {
                throw new IllegalStateException(
                        "client supervisor can connect only from NEW state; current="
                                + state);
            }
            state = State.CONNECTING;
            lastEvent = "connecting to " + useHost + ":" + port;
        }

        try {
            Optional<IndependentHostResumeTokenStore.Record> stored =
                    tokenStore.load(serverKey, profileIdentity);
            Socket mountedSocket = new Socket();
            socket = mountedSocket;
            int timeoutMillis = Math.toIntExact(useTimeout.toMillis());
            mountedSocket.connect(
                    new InetSocketAddress(useHost, port),
                    timeoutMillis);
            mountedSocket.setSoTimeout(timeoutMillis);
            reader = new BufferedReader(new InputStreamReader(
                    mountedSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(
                    mountedSocket.getOutputStream(), StandardCharsets.UTF_8));

            ConnectionIdentity connected = completeHandshake(stored);
            identity = connected;
            mountTokenCustody(connected);

            mountedSocket.setSoTimeout(0);
            running.set(true);
            Thread mountedReader = new Thread(
                    this::readLoop,
                    "mechanist-independent-host-client-reader");
            mountedReader.setDaemon(true);
            readerThread = mountedReader;
            mountedReader.start();

            requestRosterRefresh(useTimeout);
            lastEvent = "authenticated relay session with wait-only world control mounted";
            return connected;
        } catch (Exception connectFailure) {
            fail(connectFailure, "connection or handshake failed");
            closeTransportQuietly();
            throw connectFailure;
        } catch (Error fatalFailure) {
            fail(fatalFailure, "connection or handshake failed");
            closeTransportQuietly();
            throw fatalFailure;
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
        return sendHostedCommand(
                "PRESENCE",
                enumToken(
                        presence,
                        "presence",
                        "available",
                        "away",
                        "busy"),
                timeout);
    }

    HostedCommandAck setChatState(String chatState, Duration timeout)
            throws Exception {
        return sendHostedCommand(
                "CHAT_STATE",
                enumToken(
                        chatState,
                        "chat state",
                        "idle",
                        "typing"),
                timeout);
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
            PendingCommand pending = new PendingCommand(
                    commandId,
                    useCommand,
                    new CompletableFuture<>());
            if (!pendingCommand.compareAndSet(null, pending)) {
                throw new IllegalStateException(
                        "another hosted command is already awaiting acknowledgement");
            }
            try {
                writeLine("MECH|SESSION_COMMAND|" + commandId
                        + "|" + useCommand + "|" + useValue);
                HostedCommandAck acknowledgement;
                try {
                    acknowledgement = pending.future().get(
                            useTimeout.toMillis(),
                            TimeUnit.MILLISECONDS);
                } catch (java.util.concurrent.TimeoutException timedOut) {
                    TimeoutException timeoutFailure = new TimeoutException(
                            "timed out waiting for hosted-command acknowledgement");
                    fail(timeoutFailure, "hosted-command acknowledgement timed out");
                    closeTransportQuietly();
                    throw timeoutFailure;
                }
                waitForRosterVersion(
                        acknowledgement.snapshotVersion(),
                        useTimeout);
                return acknowledgement;
            } finally {
                pendingCommand.compareAndSet(pending, null);
            }
        }
    }

    WorldWaitAck waitAuthoritativeTurn(Duration timeout) throws Exception {
        ensureAuthenticated();
        Duration useTimeout = boundedTimeout(timeout);
        synchronized (worldCommandLock) {
            long commandId = worldCommandSequence.getAndIncrement();
            PendingWorldCommand pending = new PendingWorldCommand(
                    commandId,
                    new CompletableFuture<>());
            if (!pendingWorldCommand.compareAndSet(null, pending)) {
                throw new IllegalStateException(
                        "another authoritative wait command is awaiting acknowledgement");
            }
            try {
                writeLine("MECH|WORLD_COMMAND|" + commandId + "|WAIT");
                try {
                    WorldWaitAck acknowledgement = pending.future().get(
                            useTimeout.toMillis(),
                            TimeUnit.MILLISECONDS);
                    latestWorldWait = acknowledgement;
                    lastEvent = "authoritative wait accepted at world turn "
                            + acknowledgement.worldTurn();
                    return acknowledgement;
                } catch (java.util.concurrent.TimeoutException timedOut) {
                    TimeoutException timeoutFailure = new TimeoutException(
                            "timed out waiting for authoritative wait acknowledgement");
                    fail(timeoutFailure, "authoritative wait acknowledgement timed out");
                    closeTransportQuietly();
                    throw timeoutFailure;
                }
            } finally {
                pendingWorldCommand.compareAndSet(pending, null);
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
        try {
            writeLine("SEQ|" + sequence + "|" + use);
            return sequence;
        } catch (IOException failure) {
            fail(failure, "relay write failed");
            closeTransportQuietly();
            throw failure;
        }
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
        ensureAuthenticated();
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
                throwIfReaderFailed("independent-host reader failed");
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

    WorldWaitAck latestWorldWait() {
        return latestWorldWait;
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
        WorldWaitAck wait = latestWorldWait;
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
                + " worldCommandSequence=" + worldCommandSequence.get()
                + " relaySequence=" + relaySequence.get()
                + " playerTurn=" + (wait == null ? 0 : wait.playerTurn())
                + " worldTurn=" + (wait == null ? 0 : wait.worldTurn())
                + " waitAuthority=true"
                + " movementAuthority=false"
                + " mapAuthority=false"
                + " lastEvent=" + lastEvent
                + " tokenInDiagnostics=false"
                + " fullWorldAuthority=false";
    }

    private void mountTokenCustody(ConnectionIdentity connected) {
        try {
            tokenStore.save(
                    serverKey,
                    profileIdentity,
                    connected.playerId(),
                    connected.resumeToken(),
                    connected.connectionGeneration());
            resumeTokenPersisted = true;
            tokenPersistenceFailure = "none";
            state = State.AUTHENTICATED;
        } catch (IOException custodyFailure) {
            resumeTokenPersisted = false;
            tokenPersistenceFailure = safeReason(custodyFailure.getMessage());
            state = State.AUTHENTICATED_VOLATILE_TOKEN;
        }
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
                    "server attempted to grant an unsupported transport access class");
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
                    "server granted an unsupported transport access class");
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
                "RELAY_ONLY+WAIT_CONTROL");
    }

    private void readLoop() {
        try {
            String line;
            BufferedReader mountedReader = reader;
            if (mountedReader == null) {
                throw new IOException("independent-host reader is not mounted");
            }
            while (running.get() && (line = mountedReader.readLine()) != null) {
                if (line.startsWith("MECH|HOSTED_ROSTER_BEGIN|")) {
                    HostedRosterClientAuthority.Snapshot roster =
                            HostedRosterStreamReader.read(
                                    mountedReader,
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
                    completePendingCommand(parseCommandAck(line));
                    continue;
                }
                if (line.startsWith("MECH|WORLD_COMMAND_ACCEPTED|")) {
                    completePendingWorldCommand(parseWorldWaitAck(line));
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
            failPendingOperations(
                    failure == null
                            ? new IOException("client session closed")
                            : failure);
            synchronized (rosterSignal) {
                rosterSignal.notifyAll();
            }
            closeTransportQuietly();
        }
    }

    private void completePendingCommand(HostedCommandAck acknowledgement)
            throws IOException {
        PendingCommand pending = pendingCommand.get();
        if (pending == null) {
            throw new IOException(
                    "unsolicited hosted-command acknowledgement");
        }
        if (acknowledgement.commandId() != pending.commandId()
                || !pending.command().equals(acknowledgement.command())) {
            throw new IOException(
                    "hosted-command acknowledgement does not match its request");
        }
        pending.future().complete(acknowledgement);
    }

    private void completePendingWorldCommand(WorldWaitAck acknowledgement)
            throws IOException {
        PendingWorldCommand pending = pendingWorldCommand.get();
        if (pending == null) {
            throw new IOException(
                    "unsolicited authoritative wait acknowledgement");
        }
        if (acknowledgement.commandId() != pending.commandId()
                || !"WAIT".equals(acknowledgement.command())) {
            throw new IOException(
                    "authoritative wait acknowledgement does not match its request");
        }
        ConnectionIdentity current = identity;
        if (current == null
                || acknowledgement.connectionGeneration()
                != current.connectionGeneration()) {
            throw new IOException(
                    "authoritative wait acknowledgement used the wrong connection generation");
        }
        pending.future().complete(acknowledgement);
    }

    private HostedCommandAck parseCommandAck(String line)
            throws IOException {
        String[] fields = requireCommand(
                line,
                "SESSION_COMMAND_ACCEPTED",
                11);
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

    private WorldWaitAck parseWorldWaitAck(String line)
            throws IOException {
        String[] fields = requireCommand(
                line,
                "WORLD_COMMAND_ACCEPTED",
                12);
        if (!"WAIT".equals(fields[3])) {
            throw new IOException(
                    "server acknowledged an unsupported world command");
        }
        long commandId = nonNegativeLong(fields[2], "world command id");
        long lastCommandId = nonNegativeLong(
                fields[6],
                "last world command id");
        if (commandId != lastCommandId) {
            throw new IOException(
                    "world command acknowledgement sequence is inconsistent");
        }
        return new WorldWaitAck(
                commandId,
                fields[3],
                nonNegativeLong(fields[4], "world snapshot version"),
                positiveLong(fields[5], "connection generation"),
                lastCommandId,
                nonNegativeLong(fields[7], "player turn"),
                nonNegativeLong(fields[8], "world turn"),
                nonNegativeLong(fields[9], "accepted player world commands"),
                nonNegativeLong(fields[10], "accepted global world commands"),
                fields[11]);
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
            throw new IOException(
                    "relay frame is missing its payload separator");
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
                throwIfReaderFailed(
                        "client reader failed before roster confirmation");
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0L) {
                    throw new TimeoutException(
                            "timed out waiting for authoritative roster confirmation");
                }
                TimeUnit.NANOSECONDS.timedWait(rosterSignal, remaining);
            }
        }
    }

    private void throwIfReaderFailed(String message) throws IOException {
        Throwable currentFailure = failure;
        if (currentFailure != null) {
            throw new IOException(message, currentFailure);
        }
        if (!running.get()
                && state != State.CONNECTING
                && state != State.AUTHENTICATED
                && state != State.AUTHENTICATED_VOLATILE_TOKEN) {
            throw new IOException(
                    "independent-host reader is not running");
        }
    }

    private void writeLine(String line) throws IOException {
        BufferedWriter mountedWriter = writer;
        if (mountedWriter == null) {
            throw new IOException(
                    "independent-host writer is not mounted");
        }
        synchronized (writeLock) {
            mountedWriter.write(line);
            mountedWriter.newLine();
            mountedWriter.flush();
        }
    }

    private String readRequired(String expected) throws IOException {
        BufferedReader mountedReader = reader;
        if (mountedReader == null) {
            throw new IOException(
                    "independent-host reader is not mounted");
        }
        String line = mountedReader.readLine();
        if (line == null) {
            throw new IOException("server closed before " + expected);
        }
        return line;
    }

    private void ensureAuthenticated() {
        State current = state;
        if (current != State.AUTHENTICATED
                && current != State.AUTHENTICATED_VOLATILE_TOKEN) {
            throw new IllegalStateException(
                    "independent-host client is not authenticated; state="
                            + current);
        }
        if (!running.get()) {
            throw new IllegalStateException(
                    "independent-host reader is not running");
        }
    }

    private void fail(Throwable cause, String event) {
        synchronized (lifecycleLock) {
            if (state == State.CLOSED) return;
            if (failure == null) failure = cause;
            state = State.FAILED;
            lastEvent = event + ": " + safeReason(
                    cause == null ? "unknown" : cause.getMessage());
            running.set(false);
        }
        synchronized (rosterSignal) {
            rosterSignal.notifyAll();
        }
    }

    private void failPendingOperations(Throwable cause) {
        PendingCommand hosted = pendingCommand.getAndSet(null);
        if (hosted != null && !hosted.future().isDone()) {
            hosted.future().completeExceptionally(cause);
        }
        PendingWorldCommand world = pendingWorldCommand.getAndSet(null);
        if (world != null && !world.future().isDone()) {
            world.future().completeExceptionally(cause);
        }
    }

    @Override
    public void close() {
        Thread mountedReader;
        synchronized (lifecycleLock) {
            if (state == State.CLOSED) return;
            state = State.CLOSED;
            running.set(false);
            lastEvent = "client supervisor closed";
            mountedReader = readerThread;
        }
        closeTransportQuietly();
        if (mountedReader != null && mountedReader != Thread.currentThread()) {
            mountedReader.interrupt();
            try {
                mountedReader.join(2_000L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        failPendingOperations(new IOException("client supervisor closed"));
        synchronized (rosterSignal) {
            rosterSignal.notifyAll();
        }
    }

    private void closeTransportQuietly() {
        Socket mountedSocket = socket;
        if (mountedSocket == null) return;
        try {
            mountedSocket.close();
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

    private record PendingCommand(
            long commandId,
            String command,
            CompletableFuture<HostedCommandAck> future
    ) { }

    private record PendingWorldCommand(
            long commandId,
            CompletableFuture<WorldWaitAck> future
    ) { }

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

    record WorldWaitAck(
            long commandId,
            String command,
            long snapshotVersion,
            long connectionGeneration,
            long lastConnectionCommandId,
            long playerTurn,
            long worldTurn,
            long acceptedPlayerCommands,
            long acceptedWorldCommands,
            String lastEvent
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
