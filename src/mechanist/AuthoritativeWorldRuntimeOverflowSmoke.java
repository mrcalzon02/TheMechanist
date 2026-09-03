package mechanist;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ensures authoritative snapshot-version exhaustion fails before world mutation
 * and published world snapshots cannot retain caller mutation authority.
 */
final class AuthoritativeWorldRuntimeOverflowSmoke {
    public static void main(String[] args) throws Exception {
        verifyWorldSnapshotImmutability();

        AtomicInteger commits = new AtomicInteger();
        AuthoritativeWorldRuntime.SnapshotSource source =
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
                        return null;
                    }
                };

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

            System.out.println(
                    "AuthoritativeWorldRuntimeOverflowSmoke PASS"
                            + " snapshotDetached=true"
                            + " nullStateNormalized=true"
                            + " initialPublication=true"
                            + " exhaustionRejected=true"
                            + " mutationPrevented=true"
                            + " wrapPrevented=true");
        }
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
