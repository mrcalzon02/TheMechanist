package mechanist;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Process-bound companion-server smoke executed from the packaged client JAR.
 *
 * <p>The server must already be running as a separate process. This client
 * proves that two packaged supervised clients can authenticate, join the
 * connected-only roster, exchange bounded relay data, submit the narrow
 * authoritative wait command, disconnect privately, and resume by protected
 * token custody. It intentionally does not claim movement, map, inventory, or
 * full remote-world authority.</p>
 */
public final class CompanionServerExternalClientJoinSmoke {
    private static final Duration TIMEOUT = Duration.ofSeconds(12);

    public static void main(String[] args) throws Exception {
        Map<String, String> options = options(args);
        String host = value(options, "host", "127.0.0.1");
        int port = Integer.parseInt(value(options, "port", "25565"));
        String serverKey = value(
                options,
                "server-key",
                "limited-alpha/external-companion-host");
        String profilePrefix = value(
                options,
                "profile-prefix",
                "external.companion");
        boolean expectInitialResume = Boolean.parseBoolean(
                value(options, "expect-initial-resume", "false"));
        Path clientStorage = Path.of(required(options, "client-storage"))
                .toAbsolutePath()
                .normalize();
        Files.createDirectories(clientStorage);

        String alphaProfile = profilePrefix + ".alpha.0001";
        String betaProfile = profilePrefix + ".beta.0002";
        IndependentHostResumeTokenStore tokenStore =
                new IndependentHostResumeTokenStore(clientStorage);

        String alphaPlayer;
        long alphaGeneration;
        long alphaWorldTurn;
        try (IndependentHostClientSupervisor alpha =
                     new IndependentHostClientSupervisor(
                             tokenStore,
                             serverKey,
                             alphaProfile);
             IndependentHostClientSupervisor beta =
                     new IndependentHostClientSupervisor(
                             tokenStore,
                             serverKey,
                             betaProfile)) {
            IndependentHostClientSupervisor.ConnectionIdentity alphaIdentity =
                    alpha.connect(host, port, TIMEOUT);
            IndependentHostClientSupervisor.ConnectionIdentity betaIdentity =
                    beta.connect(host, port, TIMEOUT);
            require(alphaIdentity.resumed() == expectInitialResume,
                    "alpha initial resume state differed from expectation");
            require(betaIdentity.resumed() == expectInitialResume,
                    "beta initial resume state differed from expectation");
            require("RELAY_ONLY+WAIT_CONTROL".equals(alphaIdentity.accessClass()),
                    "alpha received the wrong access class");
            require("RELAY_ONLY+WAIT_CONTROL".equals(betaIdentity.accessClass()),
                    "beta received the wrong access class");
            require(alpha.resumeTokenPersisted() && beta.resumeTokenPersisted(),
                    "packaged client did not persist resume-token custody");

            alphaPlayer = alphaIdentity.playerId();
            alphaGeneration = alphaIdentity.connectionGeneration();
            waitForVisiblePlayers(alpha, 2, TIMEOUT);
            waitForVisiblePlayers(beta, 2, TIMEOUT);
            require(alpha.latestRoster().entryFor(betaIdentity.playerId()) != null,
                    "alpha did not receive beta's authoritative join roster");
            require(beta.latestRoster().entryFor(alphaPlayer) != null,
                    "beta did not receive alpha's authoritative join roster");

            IndependentHostClientSupervisor.HostedCommandAck ready =
                    alpha.setReady(true, TIMEOUT);
            require(ready.ready(),
                    "external companion host did not acknowledge readiness");
            waitForRosterState(beta, alphaPlayer, true, "available", TIMEOUT);

            long relaySequence = alpha.sendRelayPayload(
                    "external-companion-client-join-smoke");
            IndependentHostClientSupervisor.RelayFrame relay =
                    beta.pollRelay(TIMEOUT);
            require(relay != null,
                    "beta did not receive alpha's packaged relay frame");
            require(relay.sequence() == relaySequence,
                    "relay sequence changed between packaged clients");
            require("external-companion-client-join-smoke".equals(relay.payload()),
                    "relay payload changed between packaged clients");

            IndependentHostClientSupervisor.WorldWaitAck wait =
                    alpha.waitAuthoritativeTurn(TIMEOUT);
            require("WAIT".equals(wait.command()),
                    "external companion host accepted the wrong world command");
            require(wait.playerTurn() >= 1L && wait.worldTurn() >= 1L,
                    "external companion host did not advance wait authority");
            require(wait.acceptedPlayerCommands() >= 1L
                            && wait.acceptedWorldCommands() >= 1L,
                    "external companion host did not account for wait authority");
            alphaWorldTurn = wait.worldTurn();
            require(alpha.statusLine().contains("movementAuthority=false")
                            && alpha.statusLine().contains("mapAuthority=false")
                            && alpha.statusLine().contains("fullWorldAuthority=false"),
                    "packaged client status overclaimed remote world authority");

            alpha.close();
            waitForVisiblePlayers(beta, 1, TIMEOUT);
            require(beta.latestRoster().entryFor(alphaPlayer) == null,
                    "disconnected alpha identity remained public in beta roster");

            try (IndependentHostClientSupervisor resumed =
                         new IndependentHostClientSupervisor(
                                 tokenStore,
                                 serverKey,
                                 alphaProfile)) {
                IndependentHostClientSupervisor.ConnectionIdentity resumedIdentity =
                        resumed.connect(host, port, TIMEOUT);
                require(resumedIdentity.resumed(),
                        "living external host did not resume alpha by token custody");
                require(alphaPlayer.equals(resumedIdentity.playerId()),
                        "external host resume changed alpha's stable player id");
                require(resumedIdentity.connectionGeneration() > alphaGeneration,
                        "external host resume did not advance connection generation");
                waitForVisiblePlayers(beta, 2, TIMEOUT);
                IndependentHostClientSupervisor.HostedCommandAck away =
                        resumed.setPresence("away", TIMEOUT);
                require("away".equals(away.presence()),
                        "resumed external client presence was not acknowledged");
                waitForRosterState(beta, alphaPlayer, false, "away", TIMEOUT);
            }
            waitForVisiblePlayers(beta, 1, TIMEOUT);
        }

        require(Files.isDirectory(clientStorage),
                "client token-custody root disappeared during external join smoke");
        require(IndependentHostClientSupervisor.VERSION
                        .startsWith("independent-host-client-supervisor-"),
                "packaged client supervisor version identity is missing");

        System.out.println("CompanionServerExternalClientJoinSmoke PASS"
                + " externalServerProcess=true"
                + " initialResume=" + expectInitialResume
                + " stablePlayerId=" + alphaPlayer
                + " initialGeneration=" + alphaGeneration
                + " worldTurn=" + alphaWorldTurn
                + " twoClientJoin=true"
                + " connectedOnlyRoster=true"
                + " readinessBroadcast=true"
                + " relay=true"
                + " authoritativeWait=true"
                + " disconnectPrivacy=true"
                + " livingHostResume=true"
                + " movementAuthority=false"
                + " mapAuthority=false"
                + " fullWorldAuthority=false");
    }

    private static void waitForVisiblePlayers(
            IndependentHostClientSupervisor client,
            int expected,
            Duration timeout
    ) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            HostedRosterClientAuthority.Snapshot roster = client.latestRoster();
            if (roster != null && roster.visiblePlayers() == expected) return;
            Thread.sleep(25L);
        }
        HostedRosterClientAuthority.Snapshot roster = client.latestRoster();
        throw new IllegalStateException(
                "timed out waiting for visiblePlayers=" + expected
                        + " latest=" + rosterSummary(roster));
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
            HostedRosterClientAuthority.Snapshot roster = client.latestRoster();
            HostedRosterClientAuthority.Entry entry =
                    roster == null ? null : roster.entryFor(playerId);
            if (entry != null
                    && entry.ready() == ready
                    && presence.equals(entry.presence())) return;
            Thread.sleep(25L);
        }
        HostedRosterClientAuthority.Snapshot roster = client.latestRoster();
        throw new IllegalStateException(
                "timed out waiting for roster state player=" + playerId
                        + " ready=" + ready
                        + " presence=" + presence
                        + " latest=" + rosterSummary(roster));
    }

    private static String rosterSummary(
            HostedRosterClientAuthority.Snapshot roster
    ) {
        return roster == null
                ? "none"
                : "version=" + roster.version()
                + ",world=" + roster.worldId()
                + ",visiblePlayers=" + roster.visiblePlayers()
                + ",worldAuthority=" + roster.worldAuthority();
    }

    private static Map<String, String> options(String[] args) {
        Map<String, String> values = new HashMap<>();
        if (args == null) return values;
        for (String arg : args) {
            if (arg == null || !arg.startsWith("--")) continue;
            int separator = arg.indexOf('=');
            if (separator <= 2) continue;
            values.put(
                    arg.substring(2, separator).trim().toLowerCase(),
                    arg.substring(separator + 1).trim());
        }
        return values;
    }

    private static String value(
            Map<String, String> options,
            String key,
            String fallback
    ) {
        String value = options.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String required(Map<String, String> options, String key) {
        String value = options.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing required --" + key + "=VALUE");
        }
        return value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private CompanionServerExternalClientJoinSmoke() { }
}
