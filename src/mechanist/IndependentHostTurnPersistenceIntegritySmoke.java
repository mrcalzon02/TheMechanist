package mechanist;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.Properties;

/**
 * Corruption checks for persisted independent-host WAIT accounting.
 */
final class IndependentHostTurnPersistenceIntegritySmoke {
    private static final String WORLD_ID =
            "independent-host-turn-integrity-smoke-world";
    private static final String PLAYER_ID =
            "remote-0123456789abcdefabcd";

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(
                "Mechanist Remote Turn Integrity ");
        Path persistence = root.resolve("turns.properties");
        try {
            writeLedger(persistence, 2L, 1L, 1L, 1L);
            expectRestoreFailure(
                    persistence,
                    "world accounting mismatch");

            writeLedger(persistence, 1L, 1L, 2L, 1L);
            expectRestoreFailure(
                    persistence,
                    "player accounting mismatch");

            writeLedger(persistence, 1L, 1L, 1L, 1L);
            try (IndependentHostTurnAuthority authority =
                         new IndependentHostTurnAuthority(
                                 WORLD_ID,
                                 persistence)) {
                IndependentHostTurnAuthority.TurnSnapshot snapshot =
                        authority.snapshotForPlayer(PLAYER_ID);
                require(snapshot != null
                                && snapshot.worldTurn() == 1L
                                && snapshot.acceptedWorldCommands() == 1L
                                && snapshot.playerTurn() == 1L
                                && snapshot.acceptedPlayerCommands() == 1L,
                        "consistent persisted WAIT accounting did not restore");
            }

            System.out.println(
                    "IndependentHostTurnPersistenceIntegritySmoke PASS"
                            + " worldAccountingMismatchRejected=true"
                            + " playerAccountingMismatchRejected=true"
                            + " consistentAccountingRestored=true");
        } finally {
            deleteRecursively(root);
        }
    }

    private static void writeLedger(
            Path persistence,
            long worldTurn,
            long acceptedCommands,
            long playerTurn,
            long playerAcceptedCommands
    ) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("schema", "1");
        properties.setProperty("worldId", WORLD_ID);
        properties.setProperty("worldTurn", Long.toString(worldTurn));
        properties.setProperty(
                "acceptedCommands",
                Long.toString(acceptedCommands));
        properties.setProperty("player.count", "1");
        properties.setProperty("player.0.playerId", PLAYER_ID);
        properties.setProperty("player.0.connectionGeneration", "1");
        properties.setProperty("player.0.lastConnectionCommandId", "0");
        properties.setProperty("player.0.turn", Long.toString(playerTurn));
        properties.setProperty(
                "player.0.acceptedCommands",
                Long.toString(playerAcceptedCommands));
        properties.setProperty(
                "player.0.lastEvent",
                "authoritative wait accepted");
        properties.setProperty("player.0.event.count", "1");
        properties.setProperty("player.0.event.0", "wait");
        try (var output = Files.newOutputStream(persistence)) {
            properties.store(output, "remote turn integrity smoke");
        }
    }

    private static void expectRestoreFailure(
            Path persistence,
            String expectedText
    ) throws Exception {
        Throwable failure = null;
        try (IndependentHostTurnAuthority ignored =
                     new IndependentHostTurnAuthority(
                             WORLD_ID,
                             persistence)) {
        } catch (Throwable caught) {
            failure = caught;
        }
        require(failure != null,
                "expected persistence restore failure containing "
                        + expectedText);
        String combined = allMessages(failure).toLowerCase(Locale.ROOT);
        require(combined.contains(expectedText.toLowerCase(Locale.ROOT)),
                "unexpected restore failure: " + combined);
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

    private IndependentHostTurnPersistenceIntegritySmoke() {
    }
}
