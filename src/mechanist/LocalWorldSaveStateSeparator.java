package mechanist;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

/** Transactionally splits a single-player combined save into world-owned data plus a host profile file. */
final class LocalWorldSaveStateSeparator {
    private final CharacterSaveManager characterSaveManager;
    private final SecurityPathGuard worldGuard;

    LocalWorldSaveStateSeparator(CharacterSaveManager characterSaveManager, Path worldRoot) throws IOException {
        this.characterSaveManager = Objects.requireNonNull(characterSaveManager, "characterSaveManager");
        this.worldGuard = new SecurityPathGuard(worldRoot);
    }

    LocalWorldConversionManifest splitSinglePlayerProfile(Path singlePlayerSaveFile, PlayerIdentity hostIdentity) throws SaveConversionException {
        Objects.requireNonNull(singlePlayerSaveFile, "singlePlayerSaveFile");
        Objects.requireNonNull(hostIdentity, "hostIdentity");
        Path source = singlePlayerSaveFile.toAbsolutePath().normalize();
        if (!Files.exists(source)) throw new SaveConversionException("single-player save does not exist: " + source);
        Path backup = source.resolveSibling(source.getFileName() + ".pre-multiplayer.bak");
        Path worldOut;
        try {
            Properties combined = new Properties();
            try (InputStream in = Files.newInputStream(source)) { combined.load(in); }
            Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
            String worldId = safe(combined.getProperty("world.id", combined.getProperty("worlddef.worldId", "local-world-" + UUID.randomUUID())));
            worldOut = worldGuard.resolveInside(worldId + ".mechworld");
            CharacterStateRecord hostCharacter = extractCharacter(combined, hostIdentity);
            Properties worldOnly = new Properties();
            for (String name : combined.stringPropertyNames()) {
                if (!isPlayerScoped(name)) worldOnly.setProperty(name, combined.getProperty(name));
            }
            worldOnly.setProperty("save.architecture", "local-multiplayer-split");
            worldOnly.setProperty("save.multiplayer.locked", "true");
            worldOnly.setProperty("save.host.identity", hostIdentity.storageKey());
            worldOnly.setProperty("save.host.profileRef", "data/profiles/" + hostIdentity.storageKey().replaceAll("[^A-Za-z0-9._-]", "_") + ".dat");
            atomicStore(worldOut, worldOnly, "The Mechanist split local multiplayer world state");
            characterSaveManager.atomicSaveSync(hostCharacter);
            return new LocalWorldConversionManifest(true, hostIdentity.storageKey(), source.toString(), backup.toString(), worldOut.toString(), Instant.now().toString(), "single-player profile split into host profile and world state");
        } catch (IOException | RuntimeException ex) {
            try { if (Files.exists(backup)) Files.copy(backup, source, StandardCopyOption.REPLACE_EXISTING); } catch (IOException rollback) { DebugLog.error("SAVE_CONVERSION_ROLLBACK", "Could not restore source save after conversion failure.", rollback); }
            throw new SaveConversionException("world remains in safe single-player mode; conversion failed: " + ex.getMessage(), ex);
        }
    }

    private static CharacterStateRecord extractCharacter(Properties p, PlayerIdentity id) {
        String name = p.getProperty("player.name", p.getProperty("character.name", "Host"));
        double x = parseDouble(p.getProperty("player.x"), 0);
        double y = parseDouble(p.getProperty("player.y"), 0);
        double z = parseDouble(p.getProperty("player.z"), 0);
        int hp = parseInt(p.getProperty("player.health"), 100);
        String zone = p.getProperty("player.zone", p.getProperty("zone.id", "origin-zone"));
        return new CharacterStateRecord(id.storageKey(), name, x, y, z, zone, hp, List.of(), List.of("ration-pack", "work-clothes"), Map.of("civic-authority", 0), Instant.now());
    }

    private static void atomicStore(Path finalPath, Properties p, String comment) throws IOException {
        Files.createDirectories(finalPath.getParent());
        Path tmp = finalPath.resolveSibling(finalPath.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(tmp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            p.store(out, comment);
        }
        try (FileChannel channel = FileChannel.open(tmp, StandardOpenOption.WRITE)) { channel.force(true); }
        try { Files.move(tmp, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (java.nio.file.AtomicMoveNotSupportedException ex) { Files.move(tmp, finalPath, StandardCopyOption.REPLACE_EXISTING); }
    }

    private static boolean isPlayerScoped(String key) {
        if (key == null) return false;
        return key.startsWith("player.") || key.startsWith("character.") || key.startsWith("inventory.player.") || key.startsWith("skill.player.");
    }
    private static String safe(String value) { return (value == null || value.isBlank() ? "local-world" : value).replaceAll("[^A-Za-z0-9._-]", "_"); }
    private static double parseDouble(String raw, double fallback) { try { return Double.parseDouble(raw); } catch (Exception ex) { return fallback; } }
    private static int parseInt(String raw, int fallback) { try { return Integer.parseInt(raw); } catch (Exception ex) { return fallback; } }
}

final class SaveConversionException extends Exception {
    SaveConversionException(String message) { super(message); }
    SaveConversionException(String message, Throwable cause) { super(message, cause); }
}

record LocalWorldConversionManifest(boolean multiplayerLocked, String hostIdentityKey, String originalSavePath, String backupSavePath, String splitWorldPath, String convertedAtIso, String message) { }
