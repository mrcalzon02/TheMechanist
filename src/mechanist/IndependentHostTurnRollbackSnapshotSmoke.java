package mechanist;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Regression proof that a persistence failure cannot expose the failed
 * independent-host command through the last published world snapshot.
 */
final class IndependentHostTurnRollbackSnapshotSmoke {
    private static final String WORLD_ID =
            "independent-host-rollback-snapshot-smoke-world";
    private static final String PLAYER_ID =
            "remote-0123456789abcdefabcd";

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(
                "Mechanist Remote Rollback Snapshot With Spaces ");
        Path parent = root.resolve("Server State");
        Path persistence = parent.resolve("turns.properties");
        try {
            try (IndependentHostTurnAuthority authority =
                         new IndependentHostTurnAuthority(
                                 WORLD_ID,
                                 persistence)) {
                IndependentHostTurnAuthority.TurnCommandResult first =
                        authority.applyCommand(
                                PLAYER_ID,
                                1L,
                                0L,
                                new WaitCommand(PLAYER_ID));
                WorldSnapshot published = first.snapshot().worldSnapshot();
                require(published != null
                                && published.player().turn() == 1L
                                && published.player().worldTurn() == 1L,
                        "setup command did not publish the committed first snapshot");
                long publishedVersion = published.version();
                List<String> publishedActions =
                        List.copyOf(published.recentActions());

                Files.delete(persistence);
                Files.delete(parent);
                Files.writeString(parent, "block persistence parent");

                expectFailure(
                        () -> authority.applyCommand(
                                PLAYER_ID,
                                1L,
                                1L,
                                new WaitCommand(PLAYER_ID)),
                        "could not commit atomically");

                IndependentHostTurnAuthority.TurnSnapshot after =
                        authority.snapshotForPlayer(PLAYER_ID);
                require(after != null
                                && after.playerTurn() == 1L
                                && after.worldTurn() == 1L
                                && after.acceptedPlayerCommands() == 1L
                                && after.acceptedWorldCommands() == 1L,
                        "failed command leaked into restored turn accounting");
                require(after.worldSnapshot() != null,
                        "failed command discarded the last committed world snapshot");
                require(after.worldSnapshot().version() == publishedVersion
                                && after.worldSnapshot().player().turn() == 1L
                                && after.worldSnapshot().player().worldTurn() == 1L,
                        "failed command leaked into the published world snapshot");
                require(after.worldSnapshot().recentActions()
                                .equals(publishedActions),
                        "failed command leaked into published action history");
                require(after.version() == publishedVersion,
                        "failed command advanced the externally visible world version");
            }

            System.out.println(
                    "IndependentHostTurnRollbackSnapshotSmoke PASS"
                            + " persistenceFailureRollback=true"
                            + " failedSnapshotNotPublished=true"
                            + " lastCommittedSnapshotPreserved=true"
                            + " failedVersionNotPublished=true");
        } finally {
            deleteRecursively(root);
        }
    }

    private static void expectFailure(
            ThrowingAction action,
            String expectedText
    ) throws Exception {
        Throwable failure = null;
        try {
            action.run();
        } catch (Throwable caught) {
            failure = caught;
        }
        require(failure != null,
                "expected failure containing " + expectedText);
        String combined = allMessages(failure).toLowerCase(Locale.ROOT);
        require(combined.contains(expectedText.toLowerCase(Locale.ROOT)),
                "unexpected failure: " + combined);
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

    private IndependentHostTurnRollbackSnapshotSmoke() {
    }
}
