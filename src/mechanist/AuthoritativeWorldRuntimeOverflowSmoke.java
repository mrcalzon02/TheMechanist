package mechanist;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Ensures authoritative snapshot-version exhaustion fails before world mutation.
 */
final class AuthoritativeWorldRuntimeOverflowSmoke {
    public static void main(String[] args) throws Exception {
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
                            + " initialPublication=true"
                            + " exhaustionRejected=true"
                            + " mutationPrevented=true"
                            + " wrapPrevented=true");
        }
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
