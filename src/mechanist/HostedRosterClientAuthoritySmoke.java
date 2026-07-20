package mechanist;

import java.util.List;
import java.util.Optional;

/** Adversarial checks for canonical client-side hosted-roster assembly. */
final class HostedRosterClientAuthoritySmoke {
    private static final String ALPHA = "remote-11111111111111111111";
    private static final String BETA = "remote-22222222222222222222";

    public static void main(String[] args) {
        HostedRosterClientAuthority client = new HostedRosterClientAuthority();
        HostedRosterClientAuthority.Snapshot first = acceptRoster(
                client,
                1L,
                "client-roster-smoke",
                List.of(
                        entry(ALPHA, 1L, true, "available", "idle", 1L, 10L),
                        entry(BETA, 2L, false, "busy", "typing", 3L, 20L)));
        require(first.visiblePlayers() == 2,
                "valid roster did not expose two connected players");
        require(first.entryFor(ALPHA) != null && first.entryFor(BETA) != null,
                "valid roster omitted an expected player");
        require(!first.worldAuthority(),
                "valid roster overclaimed world authority");

        HostedRosterClientAuthority.Snapshot idempotent = acceptRoster(
                client,
                1L,
                "client-roster-smoke",
                List.of(
                        entry(ALPHA, 1L, true, "available", "idle", 1L, 10L),
                        entry(BETA, 2L, false, "busy", "typing", 3L, 20L)));
        require(idempotent.equals(first),
                "same-version identical roster was not idempotent");

        expectFailure(() -> acceptRoster(
                client,
                0L,
                "client-roster-smoke",
                List.of()),
                "moved backward");
        expectFailure(() -> acceptRoster(
                client,
                1L,
                "client-roster-smoke",
                List.of(entry(ALPHA, 1L, false, "away", "idle", 1L, 30L))),
                "same-version");
        expectFailure(() -> client.accept(
                "MECH|HOSTED_ROSTER_BEGIN|2|client-roster-smoke|1|1|true"),
                "world authority");
        expectFailure(() -> client.accept(
                "MECH|HOSTED_ROSTER_BEGIN|2|client-roster-smoke|65|65|false"),
                "between 0 and 64");
        expectFailure(() -> client.accept(
                "MECH|HOSTED_ROSTER_ENTRY|" + ALPHA
                        + "|true|1|false|available|idle|0|1"),
                "without a begin");

        client.accept("MECH|HOSTED_ROSTER_BEGIN|2|client-roster-smoke|1|1|false");
        expectFailure(() -> client.accept(
                "MECH|HOSTED_ROSTER_ENTRY|" + ALPHA
                        + "|false|1|false|offline|idle|0|1"),
                "offline persisted identity");
        require(!client.assembling(),
                "failed roster did not clear the partial assembly");

        client.accept("MECH|HOSTED_ROSTER_BEGIN|2|client-roster-smoke|2|2|false");
        client.accept(entry(BETA, 1L, false, "available", "idle", 0L, 1L));
        expectFailure(() -> client.accept(entry(
                ALPHA, 1L, false, "available", "idle", 0L, 2L)),
                "out of order");

        client.accept("MECH|HOSTED_ROSTER_BEGIN|2|client-roster-smoke|1|1|false");
        client.accept(entry(ALPHA, 1L, false, "available", "idle", 0L, 1L));
        expectFailure(() -> client.accept(
                "MECH|HOSTED_ROSTER_END|3"),
                "versions do not match");

        client.accept("MECH|HOSTED_ROSTER_BEGIN|2|client-roster-smoke|2|2|false");
        client.accept(entry(ALPHA, 1L, false, "available", "idle", 0L, 1L));
        expectFailure(() -> client.accept(
                "MECH|HOSTED_ROSTER_END|2"),
                "declared entry count");

        HostedRosterClientAuthority.Snapshot second = acceptRoster(
                client,
                2L,
                "client-roster-smoke",
                List.of(entry(ALPHA, 2L, false, "away", "idle", 4L, 40L)));
        require(second.version() == 2L
                        && second.visiblePlayers() == 1
                        && second.entryFor(BETA) == null,
                "new roster did not atomically replace the prior roster");

        expectFailure(() -> acceptRoster(
                client,
                3L,
                "different-world",
                List.of(entry(ALPHA, 1L, false, "available", "idle", 0L, 1L))),
                "world changed");
        client.resetForWorldChange();
        HostedRosterClientAuthority.Snapshot changedWorld = acceptRoster(
                client,
                1L,
                "different-world",
                List.of(entry(ALPHA, 1L, false, "available", "idle", 0L, 1L)));
        require("different-world".equals(changedWorld.worldId()),
                "explicit world reset did not permit a new roster world");

        require(HostedRosterClientAuthority.isRosterControlLine(
                        "MECH|HOSTED_ROSTER_BEGIN|1|w|0|0|false"),
                "roster begin was not recognized as a control frame");
        require(!HostedRosterClientAuthority.isRosterControlLine(
                        "SEQ|0|gameplay-payload"),
                "relay payload was misclassified as roster control");
        require(client.statusLine().contains("rosterVisibility=connected-only")
                        && client.statusLine().contains("worldAuthority=false"),
                "client roster authority status overclaimed its boundary");

        System.out.println("HostedRosterClientAuthoritySmoke PASS"
                + " completeFrameGroups=true"
                + " monotonicVersions=true"
                + " idempotentSameVersion=true"
                + " divergentSameVersionRejected=true"
                + " connectedOnly=true"
                + " maxVisiblePlayers=64"
                + " deterministicUniquePlayers=true"
                + " partialAssemblyClearedOnFailure=true"
                + " explicitWorldReset=true"
                + " relayPayloadSeparation=true"
                + " worldAuthority=false");
    }

    private static HostedRosterClientAuthority.Snapshot acceptRoster(
            HostedRosterClientAuthority client,
            long version,
            String worldId,
            List<String> entries
    ) {
        Optional<HostedRosterClientAuthority.Snapshot> begin = client.accept(
                "MECH|HOSTED_ROSTER_BEGIN|" + version + "|" + worldId
                        + "|" + entries.size() + "|" + entries.size() + "|false");
        require(begin.isEmpty(), "roster begin completed a snapshot unexpectedly");
        for (String entry : entries) {
            require(client.accept(entry).isEmpty(),
                    "roster entry completed a snapshot unexpectedly");
        }
        return client.accept("MECH|HOSTED_ROSTER_END|" + version)
                .orElseThrow(() -> new AssertionError(
                        "roster end did not publish an immutable snapshot"));
    }

    private static String entry(
            String playerId,
            long generation,
            boolean ready,
            String presence,
            String chatState,
            long commands,
            long lastSeen
    ) {
        return "MECH|HOSTED_ROSTER_ENTRY|" + playerId
                + "|true|" + generation
                + "|" + ready
                + "|" + presence
                + "|" + chatState
                + "|" + commands
                + "|" + lastSeen;
    }

    private static void expectFailure(Runnable action, String expectedText) {
        try {
            action.run();
            throw new AssertionError(
                    "expected failure containing: " + expectedText);
        } catch (RuntimeException expected) {
            String message = expected.getMessage() == null
                    ? ""
                    : expected.getMessage();
            require(message.toLowerCase().contains(expectedText.toLowerCase()),
                    "unexpected failure: " + message);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private HostedRosterClientAuthoritySmoke() { }
}
