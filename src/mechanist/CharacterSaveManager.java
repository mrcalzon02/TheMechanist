package mechanist;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

/** Server-authoritative async character lifecycle and atomic profile persistence. */
final class CharacterSaveManager implements AutoCloseable {
    private final SecurityPathGuard profileGuard;
    private final ScheduledExecutorService ioExecutor;

    CharacterSaveManager(Path profileRoot) throws IOException {
        this.profileGuard = new SecurityPathGuard(profileRoot);
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "mechanist-character-save-io");
            t.setDaemon(true);
            return t;
        };
        this.ioExecutor = Executors.newSingleThreadScheduledExecutor(factory);
    }

    CompletableFuture<CharacterStateRecord> loadOrCreate(PlayerIdentity identity, String requestedName) {
        Objects.requireNonNull(identity, "identity");
        return CompletableFuture.supplyAsync(() -> {
            try {
                Path file = profilePath(identity);
                if (Files.exists(file)) return CharacterStateRecord.fromJson(Files.readString(file, StandardCharsets.UTF_8));
                CharacterStateRecord fresh = CharacterStateRecord.fresh(identity, requestedName);
                atomicSaveSync(fresh);
                return fresh;
            } catch (IOException ex) {
                DebugLog.error("CHARACTER_LOAD", "Could not load/create character for " + identity.storageKey(), ex);
                return CharacterStateRecord.fresh(identity, requestedName);
            }
        }, ioExecutor);
    }

    CompletableFuture<Void> saveAsync(CharacterStateRecord record) {
        Objects.requireNonNull(record, "record");
        return CompletableFuture.runAsync(() -> {
            try { atomicSaveSync(record); }
            catch (IOException ex) { DebugLog.error("CHARACTER_SAVE", "Could not persist character " + record.identityKey(), ex); }
        }, ioExecutor);
    }

    void atomicSaveSync(CharacterStateRecord record) throws IOException {
        CharacterStateRecord stamped = new CharacterStateRecord(record.identityKey(), record.characterName(), record.x(), record.y(), record.z(), record.zoneId(), record.health(), record.selectedSkills(), record.startingItems(), record.factionReputation(), Instant.now());
        Path finalPath = profileGuard.resolveInside(safeFileName(stamped.identityKey()) + ".dat");
        Path tmpPath = profileGuard.resolveInside(safeFileName(stamped.identityKey()) + ".tmp");
        byte[] data = stamped.toJson().getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(tmpPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            channel.write(ByteBuffer.wrap(data));
            channel.force(true);
        }
        try {
            Files.move(tmpPath, finalPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
            Files.move(tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    Path profilePath(PlayerIdentity identity) throws IOException { return profileGuard.resolveInside(safeFileName(identity.storageKey()) + ".dat"); }

    private static String safeFileName(String value) { return value.replaceAll("[^A-Za-z0-9._-]", "_"); }

    @Override public void close() { ioExecutor.shutdown(); }
}
