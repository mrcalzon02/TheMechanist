package mechanist;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

/** Exercises canonical character persistence failure and round-trip boundaries. */
final class CharacterSaveManagerRegressionSmoke {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("mechanist-character-regression-");
        try (CharacterSaveManager manager = new CharacterSaveManager(root)) {
            PlayerIdentity identity =
                    PlayerIdentity.fallbackFromCredential("character-smoke-profile");
            CharacterStateRecord fresh =
                    manager.loadOrCreate(identity, "Test Citizen").join();
            CharacterStateRecord progressed = new CharacterStateRecord(
                    fresh.identityKey(), fresh.characterName(),
                    11.25, -2.0, 3.75, "industrial-sector", 67,
                    List.of("mechanics", "field-repair"),
                    List.of("ration-pack", "field-kit"),
                    Map.of("civic-authority", 8, "outlands", -3),
                    fresh.updatedAt());
            manager.saveAsync(progressed).join();
            CharacterStateRecord reloaded =
                    manager.loadOrCreateStrict(identity, "Ignored Name");
            check(reloaded.identityKey().equals(progressed.identityKey())
                            && reloaded.selectedSkills().equals(progressed.selectedSkills())
                            && reloaded.startingItems().equals(progressed.startingItems())
                            && reloaded.factionReputation().equals(progressed.factionReputation())
                            && reloaded.zoneId().equals(progressed.zoneId())
                            && reloaded.x() == progressed.x()
                            && reloaded.health() == progressed.health(),
                    "canonical character progress was lost on reload");

            PlayerIdentity corrupt =
                    PlayerIdentity.fallbackFromCredential("character-corrupt-profile");
            Path corruptPath = manager.profilePath(corrupt);
            String valid = CharacterStateRecord.fresh(corrupt, "Test").toJson();
            String malformed = valid.replace(
                    "\"selectedSkills\": []", "\"selectedSkills\": [invalid]");
            check(!valid.equals(malformed), "malformed fixture was not constructed");
            Files.writeString(corruptPath, malformed, StandardCharsets.UTF_8);
            try {
                manager.loadOrCreateStrict(corrupt, "Test");
                throw new AssertionError("strict corrupt load reported success");
            } catch (IOException expected) {
                check(expected.getCause() != null,
                        "malformed saved data lost its parse failure cause");
            }
            expectAsyncIoFailure(manager.loadOrCreate(corrupt, "Test"));
            check(malformed.equals(Files.readString(corruptPath)),
                    "corrupt character was silently replaced by defaults");

            PlayerIdentity blocked =
                    PlayerIdentity.fallbackFromCredential("character-blocked-profile");
            Path blockedFile = manager.profilePath(blocked);
            Path blockedTemp = blockedFile.resolveSibling(
                    blockedFile.getFileName().toString().replace(".dat", ".tmp"));
            Files.createDirectory(blockedTemp);
            Files.writeString(blockedTemp.resolve("sentinel"), "do-not-delete");
            expectAsyncIoFailure(manager.saveAsync(
                    CharacterStateRecord.fresh(blocked, "Blocked")));
            expectAsyncIoFailure(manager.loadOrCreate(blocked, "Blocked"));
            check(!Files.exists(blockedFile),
                    "failed persistence produced a misleading canonical character file");
            System.out.println("CharacterSaveManagerRegressionSmoke PASS"
                    + " progressRoundTrip=true"
                    + " corruptRecordFailClosed=true"
                    + " asyncFailuresVisible=true");
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    private static void expectAsyncIoFailure(java.util.concurrent.CompletableFuture<?> future) {
        try {
            future.join();
            throw new AssertionError("persistence failure was silently reported as success");
        } catch (CompletionException expected) {
            check(expected.getCause() instanceof UncheckedIOException,
                    "async failure did not propagate its I/O cause");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private CharacterSaveManagerRegressionSmoke() { }
}
