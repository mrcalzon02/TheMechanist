package mechanist;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Properties;

/**
 * Adversarial persistence and ordering checks for the first remote
 * authoritative world command.
 */
final class IndependentHostTurnAuthoritySmoke {
    private static final String WORLD_ID =
            "independent-host-turn-smoke-world";
    private static final String PLAYER_ID =
            "remote-0123456789abcdefabcd";

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(
                "Mechanist Remote Turn Authority With Spaces ");
        Path persistence = root.resolve(
                "Server State").resolve("turns.properties");
        try {
            long committedVersion;
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
                require(first.commandId() == 0L
                                && "WAIT".equals(first.command()),
                        "first authoritative command identity changed");
                require(first.snapshot().playerTurn() == 1L
                                && first.snapshot().worldTurn() == 1L,
                        "first authoritative wait did not advance turn state");
                require(first.snapshot().acceptedPlayerCommands() == 1L
                                && first.snapshot().acceptedWorldCommands() == 1L,
                        "first authoritative wait did not update command accounting");
                require(first.authoritativeSnapshot() != null
                                && first.authoritativeSnapshot().worldSnapshot() != null,
                        "first authoritative wait did not publish immutable snapshots");
                require(!Thread.currentThread().getName().equals(
                                first.snapshot().mutationThread()),
                        "remote world mutation executed on the caller thread");
                committedVersion = first.snapshot().version();

                expectFailure(
                        () -> authority.applyCommand(
                                PLAYER_ID,
                                1L,
                                0L,
                                new WaitCommand(PLAYER_ID)),
                        "sequence mismatch");
                require(authority.snapshotForPlayer(PLAYER_ID).playerTurn() == 1L,
                        "replayed command changed authoritative turn state");

                expectFailure(
                        () -> authority.applyCommand(
                                PLAYER_ID,
                                1L,
                                1L,
                                new MovePlayerCommand(
                                        PLAYER_ID,
                                        1,
                                        0,
                                        "smoke")),
                        "only authoritative wait is open");
                require(authority.snapshotForPlayer(PLAYER_ID).playerTurn() == 1L,
                        "rejected movement changed authoritative turn state");

                IndependentHostTurnAuthority.TurnCommandResult resumed =
                        authority.applyCommand(
                                PLAYER_ID,
                                2L,
                                0L,
                                new WaitCommand(PLAYER_ID));
                require(resumed.snapshot().connectionGeneration() == 2L
                                && resumed.snapshot().lastConnectionCommandId() == 0L,
                        "new connection generation did not reset command ordering");
                require(resumed.snapshot().playerTurn() == 2L
                                && resumed.snapshot().worldTurn() == 2L,
                        "resumed authoritative wait did not advance persisted turns");
                require(resumed.snapshot().version() > committedVersion,
                        "authoritative snapshot version did not advance");

                expectFailure(
                        () -> authority.applyCommand(
                                PLAYER_ID,
                                1L,
                                1L,
                                new WaitCommand(PLAYER_ID)),
                        "stale connection generation");
                require(authority.acceptedCommands() == 2L,
                        "rejected commands changed global accounting");
                authority.disconnectPlayer(PLAYER_ID);
                require(authority.snapshotForPlayer(PLAYER_ID).playerTurn() == 2L,
                        "disconnect removed persisted authoritative state");
            }

            require(Files.isRegularFile(persistence),
                    "authoritative turn persistence file was not created");
            try (var files = Files.list(persistence.getParent())) {
                require(files.noneMatch(path -> path.getFileName()
                                .toString()
                                .contains(".tmp-")),
                        "authoritative turn persistence left a temporary file");
            }
            Properties stored = new Properties();
            try (var input = Files.newInputStream(persistence)) {
                stored.load(input);
            }
            require("1".equals(stored.getProperty("schema"))
                            && WORLD_ID.equals(stored.getProperty("worldId")),
                    "authoritative turn persistence identity is invalid");
            require("2".equals(stored.getProperty("worldTurn"))
                            && "2".equals(stored.getProperty("acceptedCommands")),
                    "authoritative turn persistence lost committed accounting");

            try (IndependentHostTurnAuthority restored =
                         new IndependentHostTurnAuthority(
                                 WORLD_ID,
                                 persistence)) {
                IndependentHostTurnAuthority.TurnSnapshot before =
                        restored.snapshotForPlayer(PLAYER_ID);
                require(before != null
                                && before.playerTurn() == 2L
                                && before.worldTurn() == 2L
                                && before.acceptedPlayerCommands() == 2L,
                        "authoritative turn state did not survive clean restart");

                IndependentHostTurnAuthority.TurnCommandResult after =
                        restored.applyCommand(
                                PLAYER_ID,
                                3L,
                                0L,
                                new WaitCommand(PLAYER_ID));
                require(after.snapshot().playerTurn() == 3L
                                && after.snapshot().worldTurn() == 3L
                                && after.snapshot().acceptedWorldCommands() == 3L,
                        "post-restart authoritative wait did not continue state");
            }

            String text = Files.readString(
                    persistence,
                    StandardCharsets.ISO_8859_1);
            require(text.contains("worldTurn=3")
                            && text.contains("acceptedCommands=3"),
                    "post-restart turn ledger did not persist the third command");

            Properties corrupted = new Properties();
            try (var input = Files.newInputStream(persistence)) {
                corrupted.load(input);
            }
            corrupted.setProperty("schema", "99");
            try (var output = Files.newOutputStream(persistence)) {
                corrupted.store(output, "corrupt turn authority smoke");
            }
            expectFailure(
                    () -> {
                        try (IndependentHostTurnAuthority ignored =
                                     new IndependentHostTurnAuthority(
                                             WORLD_ID,
                                             persistence)) {
                        }
                    },
                    "could not restore persistence");

            System.out.println(
                    "IndependentHostTurnAuthoritySmoke PASS"
                            + " sharedWorldCommandRequest=true"
                            + " singleWriterAuthority=true"
                            + " waitCommandAuthority=true"
                            + " exactCommandOrdering=true"
                            + " connectionGenerationReset=true"
                            + " immutableSnapshots=true"
                            + " atomicPersistence=true"
                            + " cleanRestartContinuity=true"
                            + " unsupportedMovementRejected=true"
                            + " worldMapAuthority=false"
                            + " remoteGameplayCertified=false");
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

    private static void require(
            boolean condition,
            String message
    ) {
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface
    private interface ThrowingAction {
        void run() throws Exception;
    }

    private IndependentHostTurnAuthoritySmoke() {
    }
}
