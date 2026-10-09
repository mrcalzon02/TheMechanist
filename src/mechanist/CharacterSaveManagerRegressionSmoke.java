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

            // Corrupt scalar state previously loaded as defaults, silently
            // resetting position/health and potentially stranding a reconnect.
            String[][] invalidScalarRecords = {
                    {"missing-x", valid.replace("  \"x\": 0.0,\n", "")},
                    {"invalid-y", valid.replace("\"y\": 0.0", "\"y\": broken")},
                    {"non-finite-z", valid.replace("\"z\": 0.0", "\"z\": NaN")},
                    {"out-of-range-health", valid.replace("\"health\": 100", "\"health\": 101")},
                    {"missing-zone", valid.replace("  \"zoneId\": \"origin-zone\",\n", "")},
                    {"bad-timestamp", valid.replace("\"updatedAt\": \"", "\"updatedAt\": \"broken-")},
                    {"blank-identity", valid.replace(
                            "\"identityKey\": \"" + corrupt.storageKey() + "\"",
                            "\"identityKey\": \"\"")},
                    {"truncated-envelope", valid.substring(0, valid.length() - 1)}
            };
            for (String[] fixture : invalidScalarRecords) {
                check(!fixture[1].equals(valid),
                        "scalar corruption fixture was not constructed: " + fixture[0]);
                Files.writeString(corruptPath, fixture[1], StandardCharsets.UTF_8);
                try {
                    manager.loadOrCreateStrict(corrupt, "Test");
                    throw new AssertionError(
                            "corrupt saved scalar was accepted: " + fixture[0]);
                } catch (IOException expected) {
                    check(expected.getCause() instanceof IllegalArgumentException,
                            "corrupt scalar did not preserve the parse cause: " + fixture[0]);
                }
                check(fixture[1].equals(Files.readString(corruptPath)),
                        "corrupt scalar was overwritten: " + fixture[0]);
            }

            // The former flat JSON reader could accept duplicate keys,
            // wrong scalar types, and partial envelopes as valid profile state.
            String[][] invalidStructuralRecords = {
                    {"duplicate-identity", valid.replace(
                            "  \"characterName\":",
                            "  \"identityKey\": \"" + corrupt.storageKey()
                                    + "\",\n  \"characterName\":")},
                    {"duplicate-health", valid.replace(
                            "  \"updatedAt\":", "  \"health\": 100,\n  \"updatedAt\":")},
                    {"quoted-coordinate", valid.replace(
                            "\"x\": 0.0", "\"x\": \"0.0\"")},
                    {"numeric-zone", valid.replace(
                            "\"zoneId\": \"origin-zone\"", "\"zoneId\": 4")},
                    {"unquoted-name", valid.replace(
                            "\"characterName\": \"Test\"", "\"characterName\": Test")},
                    {"unknown-field", valid.replace(
                            "  \"updatedAt\":", "  \"unknown\": true,\n  \"updatedAt\":")},
                    {"invalid-number-token", valid.replace(
                            "\"y\": 0.0", "\"y\": 01")},
                    {"trailing-object-data", valid + " {}"}
            };
            for (String[] fixture : invalidStructuralRecords) {
                check(!fixture[1].equals(valid),
                        "structural corruption fixture was not constructed: " + fixture[0]);
                Files.writeString(corruptPath, fixture[1], StandardCharsets.UTF_8);
                try {
                    manager.loadOrCreateStrict(corrupt, "Test");
                    throw new AssertionError(
                            "corrupt saved structure was accepted: " + fixture[0]);
                } catch (IOException expected) {
                    check(expected.getCause() instanceof IllegalArgumentException,
                            "corrupt structure did not preserve parse cause: " + fixture[0]);
                }
                check(fixture[1].equals(Files.readString(corruptPath)),
                        "corrupt structure was overwritten: " + fixture[0]);
            }

            try {
                new CharacterStateRecord(
                        fresh.identityKey(), fresh.characterName(),
                        Double.POSITIVE_INFINITY, 0, 0, fresh.zoneId(), 100,
                        List.of(), List.of(), Map.of(), fresh.updatedAt());
                throw new AssertionError("non-finite character position was accepted");
            } catch (IllegalArgumentException expected) {
                // Invalid coordinates must not be serialized.
            }

            PlayerIdentity blocked =
                    PlayerIdentity.fallbackFromCredential("character-blocked-profile");
            Path blockedFile = manager.profilePath(blocked);
            Files.createDirectory(blockedFile);
            Files.writeString(blockedFile.resolve("sentinel"), "do-not-delete");
            expectAsyncIoFailure(manager.saveAsync(
                    CharacterStateRecord.fresh(blocked, "Blocked")));
            expectAsyncIoFailure(manager.loadOrCreate(blocked, "Blocked"));
            check(Files.isDirectory(blockedFile)
                            && "do-not-delete".equals(
                                    Files.readString(blockedFile.resolve("sentinel"))),
                    "failed persistence mutated a blocked destination");
            PlayerIdentity orphan =
                    PlayerIdentity.fallbackFromCredential("orphaned-temp-profile");
            Path orphanFile = manager.profilePath(orphan);
            Path orphanTemp = orphanFile.resolveSibling(
                    orphanFile.getFileName().toString().replace(".dat", ".tmp"));
            Files.createDirectory(orphanTemp);
            manager.saveAsync(CharacterStateRecord.fresh(orphan, "Recovered")).join();
            check("Recovered".equals(
                            manager.loadOrCreateStrict(orphan, "Ignored").characterName()),
                    "stale legacy temporary path prevented recovery");
            boolean hardLinkChecked = verifyHardLinkProtection(manager, root);
            boolean symlinkChecked = verifySymlinkProtection(manager, root);
            System.out.println("CharacterSaveManagerRegressionSmoke PASS"
                    + " progressRoundTrip=true"
                    + " corruptRecordFailClosed=true"
                    + " corruptScalarFailClosed=true"
                    + " strictStructureFailClosed=true"
                    + " nonFinitePositionRejected=true"
                    + " asyncFailuresVisible=true"
                    + " hardLinkProtection=" + (hardLinkChecked ? "verified" : "unsupported")
                    + " symlinkProtection=" + (symlinkChecked ? "verified" : "unsupported"));
        } finally {
            try (var files = Files.walk(root)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    /** A hard link is not a symlink; existing .tmp paths must never be truncated. */
    private static boolean verifyHardLinkProtection(
            CharacterSaveManager manager, Path root) throws Exception {
        Path outside = Files.createTempFile(
                root.getParent(), "mechanist-hardlink-outside-", ".dat");
        Path temp = null;
        try {
            Files.writeString(outside, "sentinel", StandardCharsets.UTF_8);
            PlayerIdentity linked = PlayerIdentity.fallbackFromCredential("hardlinked-temp");
            Path destination = manager.profilePath(linked);
            temp = destination.resolveSibling(
                    destination.getFileName().toString().replace(".dat", ".tmp"));
            try {
                Files.createLink(temp, outside);
            } catch (UnsupportedOperationException
                    | java.nio.file.FileSystemException
                    | SecurityException unavailable) {
                return false;
            }
            manager.saveAsync(CharacterStateRecord.fresh(linked, "Hardlinked Temp")).join();
            check(Files.isRegularFile(destination),
                    "isolated temporary save did not publish the character");
            check(Files.isSameFile(temp, outside),
                    "legacy hard link was replaced");
            check("sentinel".equals(Files.readString(outside)),
                    "hard-linked temporary path truncated another file");
            return true;
        } finally {
            if (temp != null) Files.deleteIfExists(temp);
            Files.deleteIfExists(outside);
        }
    }

    private static boolean verifySymlinkProtection(
            CharacterSaveManager manager, Path root) throws Exception {
        Path outside = Files.createTempFile(root.getParent(), "mechanist-outside-", ".dat");
        try {
            Files.writeString(outside, "sentinel", StandardCharsets.UTF_8);
            PlayerIdentity linked = PlayerIdentity.fallbackFromCredential("linked-character");
            Path file = manager.profilePath(linked);
            try {
                Files.createSymbolicLink(file, outside);
            } catch (UnsupportedOperationException
                    | java.nio.file.FileSystemException
                    | SecurityException unavailable) {
                return false;
            }
            expectAsyncIoFailure(manager.loadOrCreate(linked, "Linked"));
            expectAsyncIoFailure(manager.saveAsync(CharacterStateRecord.fresh(linked, "Linked")));
            check(Files.isSymbolicLink(file), "canonical link was replaced");
            check("sentinel".equals(Files.readString(outside)),
                    "canonical link modified an external file");

            PlayerIdentity tempLinked = PlayerIdentity.fallbackFromCredential("linked-temp");
            Path destination = manager.profilePath(tempLinked);
            Path temp = destination.resolveSibling(
                    destination.getFileName().toString().replace(".dat", ".tmp"));
            Files.createSymbolicLink(temp, outside);
            expectAsyncIoFailure(manager.saveAsync(
                    CharacterStateRecord.fresh(tempLinked, "Temp Linked")));
            check(!Files.exists(destination), "temporary link produced a character save");
            check("sentinel".equals(Files.readString(outside)),
                    "temporary link modified an external file");
            return true;
        } finally {
            Files.deleteIfExists(outside);
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
