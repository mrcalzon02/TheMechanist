package mechanist;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ensures authoritative snapshot-version exhaustion fails before world mutation,
 * snapshot publication failures poison the runtime after mutation, incoherent
 * snapshot identities fail closed, and published world snapshots cannot retain
 * caller mutation authority.
 */
final class AuthoritativeWorldRuntimeOverflowSmoke {
    public static void main(String[] args) throws Exception {
        verifyWorldSnapshotImmutability();
        verifyWorldSnapshotBounds();
        verifyPublicationFailureFailsClosed();
        verifySnapshotIdentityMismatchFailsClosed();
        verifyPlayerIdentityMismatchFailsClosed();
        verifyWorldVersionExhaustion();

        System.out.println(
                "AuthoritativeWorldRuntimeOverflowSmoke PASS"
                        + " snapshotDetached=true"
                        + " nullStateNormalized=true"
                        + " snapshotBoundsEnforced=true"
                        + " publicationFailureFailedClosed=true"
                        + " snapshotIdentityValidated=true"
                        + " playerIdentityValidated=true"
                        + " lastGoodSnapshotPreserved=true"
                        + " exhaustionRejected=true"
                        + " mutationPrevented=true"
                        + " wrapPrevented=true");
    }

    private static void verifyPublicationFailureFailsClosed() {
        AtomicInteger mutations = new AtomicInteger();
        AuthoritativeWorldRuntime.SnapshotSource healthySource = nullSnapshotSource();
        AuthoritativeWorldRuntime.SnapshotSource failingSource =
                new AuthoritativeWorldRuntime.SnapshotSource() {
                    @Override
                    public WorldSnapshot worldSnapshot(
                            long version,
                            SectorKey sector
                    ) {
                        throw new IllegalStateException(
                                "forced snapshot construction failure");
                    }

                    @Override
                    public AuthoritativeWorldSnapshot authoritativeSnapshot(
                            long version,
                            String playerId,
                            SectorKey sector,
                            String reason,
                            SectorSnapshot sectorSnapshot,
                            WorldSnapshot worldSnapshot,
                            String mutationThread
                    ) {
                        return null;
                    }
                };

        try (AuthoritativeWorldRuntime runtime =
                     new AuthoritativeWorldRuntime(
                             "mechanist-world-publication-failure-smoke")) {
            AuthoritativeWorldSnapshot first = runtime.submitAndJoin(
                    healthySource,
                    "publication-smoke-player",
                    null,
                    "initial publication",
                    () -> {
                        mutations.incrementAndGet();
                        return null;
                    });
            require(first.version() == 1L,
                    "initial publication did not establish version one");
            require(mutations.get() == 1,
                    "initial mutation did not execute exactly once");

            Throwable publicationFailure = null;
            try {
                runtime.submitAndJoin(
                        failingSource,
                        "publication-smoke-player",
                        null,
                        "forced publication failure",
                        () -> {
                            mutations.incrementAndGet();
                            return null;
                        });
            } catch (Throwable caught) {
                publicationFailure = caught;
            }
            require(publicationFailure != null,
                    "snapshot construction failure was not surfaced");
            require(allMessages(publicationFailure).contains(
                            "forced snapshot construction failure"),
                    "unexpected snapshot construction failure: "
                            + allMessages(publicationFailure));
            require(mutations.get() == 2,
                    "mutation did not execute before forced publication failure");
            require(runtime.worldVersion() == 1L,
                    "failed publication advanced authoritative world version");
            require(runtime.latestSnapshot() == first,
                    "failed publication replaced the last good snapshot");
            require(runtime.statusLine().contains(
                            "publicationState=failed-closed"),
                    "runtime did not report failed-closed publication state");

            Throwable closedFailure = null;
            try {
                runtime.submitAndJoin(
                        healthySource,
                        "publication-smoke-player",
                        null,
                        "submission after publication failure",
                        () -> {
                            mutations.incrementAndGet();
                            return null;
                        });
            } catch (Throwable caught) {
                closedFailure = caught;
            }
            require(closedFailure != null,
                    "runtime accepted mutation after publication failure");
            require(allMessages(closedFailure).contains(
                            "runtime is failed closed"),
                    "unexpected failed-closed rejection: "
                            + allMessages(closedFailure));
            require(mutations.get() == 2,
                    "mutation executed after runtime failed closed");
            require(runtime.worldVersion() == 1L,
                    "failed-closed rejection changed authoritative version");
            require(runtime.latestSnapshot() == first,
                    "failed-closed rejection replaced last good snapshot");
        }
    }

    private static void verifySnapshotIdentityMismatchFailsClosed() {
        AtomicInteger mutations = new AtomicInteger();
        AuthoritativeWorldRuntime.SnapshotSource healthySource = nullSnapshotSource();
        AuthoritativeWorldRuntime.SnapshotSource mismatchedSource =
                new AuthoritativeWorldRuntime.SnapshotSource() {
                    @Override
                    public WorldSnapshot worldSnapshot(
                            long version,
                            SectorKey sector
                    ) {
                        return new WorldSnapshot(
                                version + 1L,
                                sector,
                                PlayerSnapshot.empty(),
                                List.of(),
                                List.of(),
                                List.of(),
                                List.of(),
                                UiStateSnapshot.empty(),
                                System.currentTimeMillis());
                    }

                    @Override
                    public AuthoritativeWorldSnapshot authoritativeSnapshot(
                            long version,
                            String playerId,
                            SectorKey sector,
                            String reason,
                            SectorSnapshot sectorSnapshot,
                            WorldSnapshot worldSnapshot,
                            String mutationThread
                    ) {
                        return null;
                    }
                };

        try (AuthoritativeWorldRuntime runtime =
                     new AuthoritativeWorldRuntime(
                             "mechanist-world-identity-mismatch-smoke")) {
            AuthoritativeWorldSnapshot first = runtime.submitAndJoin(
                    healthySource,
                    "identity-smoke-player",
                    null,
                    "initial identity publication",
                    () -> {
                        mutations.incrementAndGet();
                        return null;
                    });
            require(first.version() == 1L,
                    "identity smoke did not establish version one");

            Throwable mismatchFailure = null;
            try {
                runtime.submitAndJoin(
                        mismatchedSource,
                        "identity-smoke-player",
                        null,
                        "mismatched snapshot identity",
                        () -> {
                            mutations.incrementAndGet();
                            return null;
                        });
            } catch (Throwable caught) {
                mismatchFailure = caught;
            }
            require(mismatchFailure != null,
                    "mismatched snapshot identity was accepted");
            require(allMessages(mismatchFailure).contains(
                            "Snapshot source returned world version 3 "
                                    + "for authoritative version 2"),
                    "unexpected snapshot identity failure: "
                            + allMessages(mismatchFailure));
            require(mutations.get() == 2,
                    "identity mismatch did not occur after committed mutation");
            require(runtime.worldVersion() == 1L,
                    "identity mismatch advanced authoritative version");
            require(runtime.latestSnapshot() == first,
                    "identity mismatch replaced last good snapshot");
            require(runtime.statusLine().contains(
                            "publicationState=failed-closed"),
                    "identity mismatch did not fail runtime closed");
        }
    }

    private static void verifyPlayerIdentityMismatchFailsClosed() {
        AtomicInteger mutations = new AtomicInteger();
        AuthoritativeWorldRuntime.SnapshotSource healthySource = nullSnapshotSource();
        AuthoritativeWorldRuntime.SnapshotSource mismatchedPlayerSource =
                new AuthoritativeWorldRuntime.SnapshotSource() {
                    @Override
                    public WorldSnapshot worldSnapshot(
                            long version,
                            SectorKey sector
                    ) {
                        return null;
                    }

                    @Override
                    public AuthoritativeWorldSnapshot authoritativeSnapshot(
                            long version,
                            String playerId,
                            SectorKey sector,
                            String reason,
                            SectorSnapshot sectorSnapshot,
                            WorldSnapshot worldSnapshot,
                            String mutationThread
                    ) {
                        return new AuthoritativeWorldSnapshot(
                                version,
                                "different-authenticated-player",
                                sector,
                                reason,
                                0L,
                                0L,
                                0,
                                0,
                                "none",
                                "none",
                                0,
                                0,
                                "none",
                                "none",
                                worldSnapshot,
                                mutationThread,
                                sectorSnapshot,
                                System.currentTimeMillis());
                    }
                };

        try (AuthoritativeWorldRuntime runtime =
                     new AuthoritativeWorldRuntime(
                             "mechanist-player-identity-mismatch-smoke")) {
            AuthoritativeWorldSnapshot first = runtime.submitAndJoin(
                    healthySource,
                    "authenticated-player",
                    null,
                    "initial player identity publication",
                    () -> {
                        mutations.incrementAndGet();
                        return null;
                    });
            require(first.version() == 1L,
                    "player identity smoke did not establish version one");

            Throwable mismatchFailure = null;
            try {
                runtime.submitAndJoin(
                        mismatchedPlayerSource,
                        "authenticated-player",
                        null,
                        "mismatched player identity",
                        () -> {
                            mutations.incrementAndGet();
                            return null;
                        });
            } catch (Throwable caught) {
                mismatchFailure = caught;
            }
            require(mismatchFailure != null,
                    "mismatched authoritative player identity was accepted");
            require(allMessages(mismatchFailure).contains(
                            "Snapshot source returned authoritative player "
                                    + "different-authenticated-player for submitted player "
                                    + "authenticated-player"),
                    "unexpected player identity failure: "
                            + allMessages(mismatchFailure));
            require(mutations.get() == 2,
                    "player identity mismatch did not follow committed mutation");
            require(runtime.worldVersion() == 1L,
                    "player identity mismatch advanced authoritative version");
            require(runtime.latestSnapshot() == first,
                    "player identity mismatch replaced last good snapshot");
            require(runtime.statusLine().contains(
                            "publicationState=failed-closed"),
                    "player identity mismatch did not fail runtime closed");
        }
    }

    private static void verifyWorldVersionExhaustion() throws Exception {
        AtomicInteger commits = new AtomicInteger();
        AuthoritativeWorldRuntime.SnapshotSource source = nullSnapshotSource();

        try (AuthoritativeWorldRuntime runtime =
                     new AuthoritativeWorldRuntime(
                             "mechanist-world-version-overflow-smoke")) {
            AuthoritativeWorldSnapshot first = runtime.submitAndJoin(
                    source,
                    "overflow-smoke-player",
                    null,
                    "initial publication",
                    () -> {
                        commits.incrementAndGet();
                        return null;
                    });
            require(first.version() == 1L,
                    "initial authoritative version was not one");
            require(runtime.worldVersion() == 1L,
                    "initial authoritative version did not publish");
            require(commits.get() == 1,
                    "initial mutation did not execute exactly once");

            worldVersionCounter(runtime).set(Long.MAX_VALUE);
            Throwable failure = null;
            try {
                runtime.submitAndJoin(
                        source,
                        "overflow-smoke-player",
                        null,
                        "exhausted publication",
                        () -> {
                            commits.incrementAndGet();
                            return null;
                        });
            } catch (Throwable caught) {
                failure = caught;
            }

            require(failure != null,
                    "exhausted world version did not reject submission");
            require(allMessages(failure).contains(
                            "world version exhausted supported range"),
                    "unexpected exhausted-version failure: "
                            + allMessages(failure));
            require(commits.get() == 1,
                    "world mutation executed after version exhaustion");
            require(runtime.worldVersion() == Long.MAX_VALUE,
                    "exhausted version wrapped or changed");
        }
    }

    private static AuthoritativeWorldRuntime.SnapshotSource nullSnapshotSource() {
        return new AuthoritativeWorldRuntime.SnapshotSource() {
            @Override
            public WorldSnapshot worldSnapshot(
                    long version,
                    SectorKey sector
            ) {
                return null;
            }

            @Override
            public AuthoritativeWorldSnapshot authoritativeSnapshot(
                    long version,
                    String playerId,
                    SectorKey sector,
                    String reason,
                    SectorSnapshot sectorSnapshot,
                    WorldSnapshot worldSnapshot,
                    String mutationThread
            ) {
                return null;
            }
        };
    }

    private static void verifyWorldSnapshotImmutability() {
        ArrayList<String> actions = new ArrayList<>();
        actions.add("committed action");
        ArrayList<TileSnapshot> tiles = new ArrayList<>();
        tiles.add(new TileSnapshot(
                1,
                2,
                '.',
                true,
                true,
                "floor",
                "smoke",
                "",
                0,
                "",
                "",
                "",
                "",
                "tile.smoke"));

        WorldSnapshot snapshot = new WorldSnapshot(
                7L,
                null,
                null,
                tiles,
                null,
                null,
                actions,
                null,
                1L);

        actions.add("late mutation");
        tiles.clear();

        require(snapshot.player() != null,
                "null player snapshot was not normalized");
        require(snapshot.uiState() != null,
                "null UI snapshot was not normalized");
        require(snapshot.visibleNpcs().isEmpty(),
                "null NPC collection was not normalized");
        require(snapshot.visibleObjects().isEmpty(),
                "null object collection was not normalized");
        require(snapshot.recentActions().equals(List.of("committed action")),
                "published action history retained caller mutation authority");
        require(snapshot.visibleTiles().size() == 1,
                "published tile view retained caller mutation authority");
        require(snapshot.compact().contains("player=none@0,0"),
                "normalized snapshot could not render a compact audit line");

        boolean immutable = false;
        try {
            snapshot.recentActions().add("illegal mutation");
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        require(immutable,
                "published action history remained externally mutable");
    }

    private static void verifyWorldSnapshotBounds() {
        ArrayList<String> excessiveActions = new ArrayList<>();
        for (int index = 0; index <= WorldSnapshot.MAX_ACTIONS; index++) {
            excessiveActions.add("action-" + index);
        }
        Throwable actionFailure = null;
        try {
            new WorldSnapshot(
                    8L,
                    null,
                    PlayerSnapshot.empty(),
                    List.of(),
                    List.of(),
                    List.of(),
                    excessiveActions,
                    UiStateSnapshot.empty(),
                    1L);
        } catch (Throwable caught) {
            actionFailure = caught;
        }
        require(actionFailure != null
                        && allMessages(actionFailure).contains(
                        "exceeds maximum recent actions"),
                "oversized recent action snapshot was accepted");

        ArrayList<TileSnapshot> excessiveTiles = new ArrayList<>();
        for (int index = 0; index <= WorldSnapshot.MAX_TILES; index++) {
            excessiveTiles.add(new TileSnapshot(
                    index,
                    0,
                    '.',
                    true,
                    false,
                    "floor",
                    "smoke",
                    "",
                    0,
                    "",
                    "",
                    "",
                    "",
                    "tile.smoke"));
        }
        Throwable tileFailure = null;
        try {
            new WorldSnapshot(
                    9L,
                    null,
                    PlayerSnapshot.empty(),
                    excessiveTiles,
                    List.of(),
                    List.of(),
                    List.of(),
                    UiStateSnapshot.empty(),
                    1L);
        } catch (Throwable caught) {
            tileFailure = caught;
        }
        require(tileFailure != null
                        && allMessages(tileFailure).contains(
                        "exceeds maximum visible tiles"),
                "oversized visible tile snapshot was accepted");
    }

    private static AtomicLong worldVersionCounter(
            AuthoritativeWorldRuntime runtime
    ) throws Exception {
        Field field = AuthoritativeWorldRuntime.class.getDeclaredField(
                "worldVersion");
        field.setAccessible(true);
        return (AtomicLong) field.get(runtime);
    }

    private static String allMessages(Throwable failure) {
        StringBuilder out = new StringBuilder();
        Throwable current = failure;
        while (current != null) {
            if (current.getMessage() != null) {
                if (!out.isEmpty()) out.append(" | ");
                out.append(current.getMessage());
            }
            current = current.getCause();
        }
        return out.toString();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
