package mechanist;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.Properties;

/** End-to-end checks for the supervised limited-alpha independent-host client. */
final class IndependentHostClientSupervisorSmoke {
    private static final String LOOPBACK = "127.0.0.1";
    private static final String WORLD_ID = "client-supervisor-smoke-world";
    private static final String SERVER_KEY = "alpha-host/client-supervisor-smoke";
    private static final String ALPHA_PROFILE = "alpha.client.3001";
    private static final String BETA_PROFILE = "beta.client.3002";
    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(
                "Mechanist Client Supervisor With Spaces ");
        Path serverStorage = root.resolve("Server Storage");
        Path clientStorage = root.resolve("Client Token Storage");
        String previous = System.getProperty("mechanist.storage.root");
        System.setProperty("mechanist.storage.root", serverStorage.toString());
        try {
            IndependentHostResumeTokenStore tokenStore =
                    new IndependentHostResumeTokenStore(clientStorage);
            String alphaPlayer;
            String alphaToken;
            long alphaHostedCommands;
            long alphaWorldCommands;

            NativeTcpRelayServer firstHost = bindHost();
            IndependentHostClientSupervisor alpha =
                    new IndependentHostClientSupervisor(
                            tokenStore,
                            SERVER_KEY,
                            ALPHA_PROFILE);
            IndependentHostClientSupervisor beta =
                    new IndependentHostClientSupervisor(
                            tokenStore,
                            SERVER_KEY,
                            BETA_PROFILE);
            try {
                IndependentHostClientSupervisor.ConnectionIdentity alphaIdentity =
                        alpha.connect(LOOPBACK, firstHost.port(), TIMEOUT);
                alphaPlayer = alphaIdentity.playerId();
                alphaToken = alphaIdentity.resumeToken();
                require(!alphaIdentity.resumed()
                                && alphaIdentity.connectionGeneration() == 1L,
                        "first alpha connection incorrectly reported resume");
                require("RELAY_ONLY+WAIT_CONTROL".equals(alphaIdentity.accessClass()),
                        "client did not expose the narrow wait-control capability");
                require(alpha.resumeTokenPersisted(),
                        "alpha resume token was not placed in client custody");
                require(alpha.latestRoster() != null
                                && alpha.latestRoster().visiblePlayers() == 1
                                && alpha.latestRoster().entryFor(alphaPlayer) != null,
                        "alpha did not mount its initial authoritative roster");
                require(!alpha.statusLine().contains(alphaToken),
                        "client status leaked the reusable resume token");

                IndependentHostClientSupervisor.ConnectionIdentity betaIdentity =
                        beta.connect(LOOPBACK, firstHost.port(), TIMEOUT);
                require(!betaIdentity.resumed()
                                && betaIdentity.connectionGeneration() == 1L,
                        "first beta connection incorrectly reported resume");
                waitForVisiblePlayers(alpha, 2, TIMEOUT);
                require(alpha.latestRoster().entryFor(betaIdentity.playerId()) != null,
                        "alpha did not receive beta's join roster broadcast");

                IndependentHostClientSupervisor.HostedCommandAck ready =
                        alpha.setReady(true, TIMEOUT);
                require(ready.ready()
                                && ready.commandId() == 0L
                                && ready.acceptedHostedCommands() == 1L,
                        "supervisor READY command was not acknowledged authoritatively");
                waitForRosterState(
                        beta,
                        alphaPlayer,
                        true,
                        "available",
                        TIMEOUT);

                IndependentHostClientSupervisor.WorldWaitAck firstWait =
                        alpha.waitAuthoritativeTurn(TIMEOUT);
                require(firstWait.commandId() == 0L
                                && "WAIT".equals(firstWait.command())
                                && firstWait.connectionGeneration() == 1L
                                && firstWait.lastConnectionCommandId() == 0L
                                && firstWait.playerTurn() == 1L
                                && firstWait.worldTurn() == 1L
                                && firstWait.acceptedPlayerCommands() == 1L
                                && firstWait.acceptedWorldCommands() == 1L,
                        "first supervised wait did not commit authoritative turn state");
                require(alpha.latestWorldWait() == firstWait,
                        "client did not retain its latest authoritative wait acknowledgement");
                require(alpha.statusLine().contains("waitAuthority=true")
                                && alpha.statusLine().contains("playerTurn=1")
                                && alpha.statusLine().contains("worldTurn=1")
                                && alpha.statusLine().contains("movementAuthority=false")
                                && alpha.statusLine().contains("mapAuthority=false")
                                && alpha.statusLine().contains("fullWorldAuthority=false"),
                        "client status overclaimed or omitted the wait-only authority boundary");
                NativeTcpRelayServer.TurnPathReadout firstTurns =
                        firstHost.remoteTurnPersistence();
                require(firstTurns.persistenceEnabled()
                                && firstTurns.acceptedCommands() == 1L
                                && firstTurns.worldTurn() == 1L
                                && Files.isRegularFile(firstTurns.persistenceFile()),
                        "live host did not persist the first authoritative wait");

                long sentSequence = alpha.sendRelayPayload(
                        "client-supervisor-relay-frame");
                require(sentSequence == 0L,
                        "first client relay sequence did not begin at zero");
                IndependentHostClientSupervisor.RelayFrame received =
                        beta.pollRelay(TIMEOUT);
                require(received != null
                                && received.sequence() == 0L
                                && "client-supervisor-relay-frame".equals(
                                received.payload()),
                        "beta did not receive the supervised relay frame");

                alphaHostedCommands = ready.acceptedHostedCommands();
                alphaWorldCommands = firstWait.acceptedWorldCommands();
                alpha.close();
                waitForVisiblePlayers(beta, 1, TIMEOUT);
                require(beta.latestRoster().entryFor(alphaPlayer) == null,
                        "alpha's offline resume identity remained visible to beta");
                require(firstHost.remoteSessionCount() == 2
                                && firstHost.activeRemoteSessionCount() == 1,
                        "server did not retain alpha privately after disconnect");

                try (IndependentHostClientSupervisor resumed =
                             new IndependentHostClientSupervisor(
                                     tokenStore,
                                     SERVER_KEY,
                                     ALPHA_PROFILE)) {
                    IndependentHostClientSupervisor.ConnectionIdentity resumedIdentity =
                            resumed.connect(LOOPBACK, firstHost.port(), TIMEOUT);
                    require(resumedIdentity.resumed(),
                            "stored client token did not resume alpha in the living host");
                    require(alphaPlayer.equals(resumedIdentity.playerId()),
                            "living-host resume changed alpha's stable player id");
                    require(resumedIdentity.connectionGeneration() == 2L,
                            "living-host resume did not advance alpha's generation");
                    waitForVisiblePlayers(beta, 2, TIMEOUT);
                    IndependentHostClientSupervisor.HostedCommandAck away =
                            resumed.setPresence("away", TIMEOUT);
                    require(away.commandId() == 0L,
                            "hosted command sequence did not reset for resumed connection");
                    require(away.acceptedHostedCommands()
                                    == alphaHostedCommands + 1L,
                            "lifetime hosted-command accounting did not survive resume");
                    alphaHostedCommands = away.acceptedHostedCommands();
                    waitForRosterState(
                            beta,
                            alphaPlayer,
                            false,
                            "away",
                            TIMEOUT);

                    IndependentHostClientSupervisor.WorldWaitAck resumedWait =
                            resumed.waitAuthoritativeTurn(TIMEOUT);
                    require(resumedWait.commandId() == 0L
                                    && resumedWait.connectionGeneration() == 2L
                                    && resumedWait.lastConnectionCommandId() == 0L
                                    && resumedWait.playerTurn() == 2L
                                    && resumedWait.worldTurn() == 2L
                                    && resumedWait.acceptedPlayerCommands() == 2L
                                    && resumedWait.acceptedWorldCommands()
                                    == alphaWorldCommands + 1L,
                            "living-host resume lost authoritative wait continuity");
                    alphaWorldCommands = resumedWait.acceptedWorldCommands();
                }
                waitForVisiblePlayers(beta, 1, TIMEOUT);
            } finally {
                alpha.close();
                beta.close();
                firstHost.close();
            }

            NativeTcpRelayServer restartedHost = bindHost();
            try (IndependentHostClientSupervisor restarted =
                         new IndependentHostClientSupervisor(
                                 tokenStore,
                                 SERVER_KEY,
                                 ALPHA_PROFILE)) {
                IndependentHostClientSupervisor.ConnectionIdentity identity =
                        restarted.connect(
                                LOOPBACK,
                                restartedHost.port(),
                                TIMEOUT);
                require(identity.resumed(),
                        "client token did not resume alpha after host restart");
                require(alphaPlayer.equals(identity.playerId()),
                        "host restart changed alpha's stable player id");
                require(identity.connectionGeneration() == 3L,
                        "host restart resume did not advance alpha generation");
                HostedRosterClientAuthority.Entry entry =
                        restarted.latestRoster().entryFor(alphaPlayer);
                require(entry != null
                                && !entry.ready()
                                && "available".equals(entry.presence())
                                && "idle".equals(entry.chatState())
                                && entry.acceptedHostedCommands()
                                == alphaHostedCommands,
                        "host restart did not preserve accounting and reset liveness");
                require(restarted.resumeTokenPersisted(),
                        "restarted client did not update token custody metadata");
                NativeTcpRelayServer.TurnPathReadout restoredTurns =
                        restartedHost.remoteTurnPersistence();
                require(restoredTurns.acceptedCommands() == alphaWorldCommands
                                && restoredTurns.worldTurn() == 2L,
                        "host restart did not restore authoritative turn accounting");

                IndependentHostClientSupervisor.WorldWaitAck restartedWait =
                        restarted.waitAuthoritativeTurn(TIMEOUT);
                require(restartedWait.commandId() == 0L
                                && restartedWait.connectionGeneration() == 3L
                                && restartedWait.lastConnectionCommandId() == 0L
                                && restartedWait.playerTurn() == 3L
                                && restartedWait.worldTurn() == 3L
                                && restartedWait.acceptedPlayerCommands() == 3L
                                && restartedWait.acceptedWorldCommands()
                                == alphaWorldCommands + 1L,
                        "host restart resume did not continue authoritative wait state");
                alphaWorldCommands = restartedWait.acceptedWorldCommands();
            } finally {
                restartedHost.close();
            }

            Path alphaRecord = findRecord(clientStorage, ALPHA_PROFILE);
            String storedText = Files.readString(
                    alphaRecord,
                    StandardCharsets.UTF_8);
            require(storedText.contains(alphaToken),
                    "client custody file does not contain the reusable token");
            require(!tokenStore.statusLine().contains(alphaToken),
                    "token-store status leaked the reusable token");
            require(noTemporaryFiles(clientStorage),
                    "client token custody left a temporary file behind");

            Properties corrupted = new Properties();
            try (var input = Files.newInputStream(alphaRecord)) {
                corrupted.load(input);
            }
            corrupted.setProperty("schema", "99");
            try (var output = Files.newOutputStream(alphaRecord)) {
                corrupted.store(output, "corrupted client-token smoke");
            }
            IndependentHostClientSupervisor denied =
                    new IndependentHostClientSupervisor(
                            tokenStore,
                            SERVER_KEY,
                            ALPHA_PROFILE);
            try {
                expectFailure(
                        () -> denied.connect(
                                LOOPBACK,
                                NetworkPortAuthority.firstAvailableGamePort(),
                                Duration.ofSeconds(1)),
                        "schema");
                require(denied.state()
                                == IndependentHostClientSupervisor.State.FAILED,
                        "corrupted local token did not fail the supervisor closed");
            } finally {
                denied.close();
            }

            verifyNoGenericWorldCommandApi();
            require(alphaWorldCommands == 3L,
                    "supervised authoritative wait accounting ended at the wrong value");
            require(IndependentHostClientSupervisor.VERSION
                            .startsWith("independent-host-client-supervisor-"),
                    "client supervisor version identity is missing");

            System.out.println("IndependentHostClientSupervisorSmoke PASS"
                    + " handshakeOwnership=true"
                    + " atomicClientTokenCustody=true"
                    + " tokenDiagnosticsRedacted=true"
                    + " initialRoster=true"
                    + " peerRosterBroadcasts=true"
                    + " hostedCommandSequencing=true"
                    + " relayDispatch=true"
                    + " authoritativeWait=true"
                    + " authoritativeWaitOrdering=true"
                    + " authoritativeWaitLivingResume=true"
                    + " authoritativeWaitHostRestartResume=true"
                    + " disconnectPrivacy=true"
                    + " livingHostResume=true"
                    + " hostRestartResume=true"
                    + " staleLivenessReset=true"
                    + " canonicalRosterClientAuthority=true"
                    + " corruptedLocalTokenRejected=true"
                    + " genericWorldCommandApi=false"
                    + " movementAuthority=false"
                    + " mapAuthority=false"
                    + " fullWorldAuthority=false");
        } finally {
            if (previous == null) {
                System.clearProperty("mechanist.storage.root");
            } else {
                System.setProperty("mechanist.storage.root", previous);
            }
            deleteRecursively(root);
        }
    }

    private static NativeTcpRelayServer bindHost() throws Exception {
        HostBindingResult result = MultiplayerHostBindingService.bind(
                ServerConfig.fromWorldSettings(
                        0xC11E17A2026L,
                        "Client Supervisor Alpha Host",
                        WORLD_ID,
                        WorldSetupSettings.standard(),
                        4,
                        NetworkPortAuthority.firstAvailableGamePort(),
                        LOOPBACK,
                        MultiplayerProtocolState.CLOSED,
                        false));
        require(result.success(),
                "client-supervisor host bind failed: " + result.compactLine());
        require(result.session() instanceof NativeTcpRelayServer,
                "client-supervisor smoke did not receive the native relay");
        return (NativeTcpRelayServer) result.session();
    }

    private static void waitForVisiblePlayers(
            IndependentHostClientSupervisor client,
            int expected,
            Duration timeout
    ) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            HostedRosterClientAuthority.Snapshot roster =
                    client.latestRoster();
            if (roster != null && roster.visiblePlayers() == expected) return;
            if (client.state() == IndependentHostClientSupervisor.State.FAILED) {
                throw new AssertionError(
                        "client failed while waiting for roster: "
                                + client.statusLine());
            }
            Thread.sleep(20L);
        }
        throw new AssertionError(
                "visible roster count did not become " + expected
                        + "; status=" + client.statusLine());
    }

    private static void waitForRosterState(
            IndependentHostClientSupervisor client,
            String playerId,
            boolean ready,
            String presence,
            Duration timeout
    ) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            HostedRosterClientAuthority.Snapshot roster =
                    client.latestRoster();
            HostedRosterClientAuthority.Entry entry =
                    roster == null ? null : roster.entryFor(playerId);
            if (entry != null
                    && entry.ready() == ready
                    && presence.equals(entry.presence())) {
                return;
            }
            Thread.sleep(20L);
        }
        throw new AssertionError(
                "roster state did not converge for player " + playerId
                        + "; status=" + client.statusLine());
    }

    private static Path findRecord(
            Path root,
            String profileIdentity
    ) throws Exception {
        try (var files = Files.list(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> {
                        Properties properties = new Properties();
                        try (var input = Files.newInputStream(path)) {
                            properties.load(input);
                            return profileIdentity.equals(
                                    properties.getProperty("profileIdentity"));
                        } catch (Exception ignored) {
                            return false;
                        }
                    })
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "missing client token record for " + profileIdentity));
        }
    }

    private static boolean noTemporaryFiles(Path root) throws Exception {
        try (var files = Files.list(root)) {
            return files.noneMatch(path -> path.getFileName()
                    .toString()
                    .contains(".tmp-"));
        }
    }

    private static void verifyNoGenericWorldCommandApi() {
        for (Method method : IndependentHostClientSupervisor.class
                .getDeclaredMethods()) {
            String name = method.getName().toLowerCase(Locale.ROOT);
            require(!name.contains("move")
                            && !name.contains("combat")
                            && !name.contains("inventory")
                            && !name.contains("worldcommand")
                            && !name.contains("worldsnapshot"),
                    "client supervisor exposed unfinished generic world API: "
                            + method.getName());
        }
    }

    private static void expectFailure(
            ThrowingAction action,
            String expectedText
    ) throws Exception {
        Throwable failure = null;
        try {
            action.run();
        } catch (Throwable expected) {
            failure = expected;
        }
        require(failure != null,
                "expected failure containing: " + expectedText);
        String message = failure.getMessage() == null
                ? ""
                : failure.getMessage();
        require(message.toLowerCase(Locale.ROOT).contains(
                        expectedText.toLowerCase(Locale.ROOT)),
                "unexpected failure: " + message);
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private IndependentHostClientSupervisorSmoke() { }
}
