package mechanist;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
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
                return loadOrCreateStrict(identity, requestedName);
            } catch (IOException ex) {
                DebugLog.error("CHARACTER_LOAD",
                        "Could not load/create character for " + identity.storageKey(), ex);
                throw new UncheckedIOException(ex);
            }
        }, ioExecutor);
    }

    CompletableFuture<Void> saveAsync(CharacterStateRecord record) {
        Objects.requireNonNull(record, "record");
        return CompletableFuture.runAsync(() -> {
            try { atomicSaveSync(record); }
            catch (IOException ex) {
                DebugLog.error("CHARACTER_SAVE",
                        "Could not persist character " + record.identityKey(), ex);
                throw new UncheckedIOException(ex);
            }
        }, ioExecutor);
    }

    synchronized CharacterStateRecord loadOrCreateStrict(
            PlayerIdentity identity,
            String requestedName
    ) throws IOException {
        Objects.requireNonNull(identity, "identity");
        Path file = profilePath(identity);
        CharacterStateRecord record;
        if (Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(file)) {
                throw new IOException("symbolic-link character record denied for "
                        + identity.storageKey());
            }
            try (FileChannel channel = FileChannel.open(
                    file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                record = CharacterStateRecord.fromJson(
                        new String(Channels.newInputStream(channel).readAllBytes(),
                                StandardCharsets.UTF_8));
            } catch (RuntimeException invalid) {
                throw new IOException(
                        "invalid persisted character record for " + identity.storageKey(),
                        invalid);
            }
            if (!identity.storageKey().equals(record.identityKey())) {
                throw new IOException(
                        "character identity mismatch for " + identity.storageKey());
            }
        } else {
            record = CharacterStateRecord.fresh(identity, requestedName);
            atomicSaveSync(record);
        }
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException(
                    "canonical character record was not persisted for "
                            + identity.storageKey());
        }
        return record;
    }

    synchronized void atomicSaveSync(CharacterStateRecord record) throws IOException {
        CharacterStateRecord stamped = new CharacterStateRecord(record.identityKey(), record.characterName(), record.x(), record.y(), record.z(), record.zoneId(), record.health(), record.selectedSkills(), record.startingItems(), record.factionReputation(), Instant.now());
        Path finalPath = profileGuard.resolveInside(safeFileName(stamped.identityKey()) + ".dat");
        // A fresh, exclusive sibling survives orphaned legacy .tmp paths and
        // never opens a pre-existing hard link for truncation.
        Path tmpPath = profileGuard.resolveInside(
                safeFileName(stamped.identityKey()) + "." + UUID.randomUUID() + ".tmp");
        if (Files.isSymbolicLink(finalPath)
                || (Files.exists(finalPath, LinkOption.NOFOLLOW_LINKS)
                    && !Files.isRegularFile(finalPath, LinkOption.NOFOLLOW_LINKS))) {
            throw new IOException("unsafe canonical character save destination");
        }
        byte[] data = stamped.toJson().getBytes(StandardCharsets.UTF_8);
        try {
            try (FileChannel channel = FileChannel.open(tmpPath,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer bytes = ByteBuffer.wrap(data);
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            try {
                Files.move(tmpPath, finalPath,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (java.nio.file.AtomicMoveNotSupportedException ex) {
                Files.move(tmpPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmpPath);
        }
    }

    /**
     * Durable movement prerequisite for a future authoritative hosted-world
     * command. The caller must already own the canonical World and its single
     * writer; this method does NOT mount a map or enable remote MOVE.
     *
     * Refuse missing maps, cross-zone/cross-profile requests, blocked tiles,
     * non-cardinal steps, and stale snapshots before changing the only
     * canonical character record. Never create a character as a side effect
     * of a movement request.
     */
    synchronized CharacterStateRecord commitHostedCardinalStep(
            PlayerIdentity identity,
            CharacterStateRecord expected,
            World hostedWorld,
            String hostedZoneId,
            int dx,
            int dy
    ) throws IOException {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(expected, "expected character state");
        if (hostedWorld == null) {
            throw new IllegalStateException("canonical hosted world is not mounted");
        }
        if (!identity.storageKey().equals(expected.identityKey())) {
            throw new SecurityException("hosted movement character identity mismatch");
        }
        if (hostedZoneId == null || !hostedZoneId.equals(expected.zoneId())) {
            throw new IllegalStateException("hosted movement zone does not match character");
        }
        if (Math.abs((long) dx) + Math.abs((long) dy) != 1L) {
            throw new IllegalArgumentException("hosted movement requires one cardinal tile");
        }
        if (expected.x() != Math.rint(expected.x())
                || expected.y() != Math.rint(expected.y())
                || expected.x() < Integer.MIN_VALUE
                || expected.x() > Integer.MAX_VALUE
                || expected.y() < Integer.MIN_VALUE
                || expected.y() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("hosted movement origin is not a grid tile");
        }
        int fromX = (int) expected.x();
        int fromY = (int) expected.y();
        int toX = Math.addExact(fromX, dx);
        int toY = Math.addExact(fromY, dy);
        if (!hostedWorld.walkable(fromX, fromY)
                || !hostedWorld.walkable(toX, toY)) {
            throw new IllegalArgumentException("hosted movement is blocked by the canonical map");
        }

        Path file = profilePath(identity);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(file)) {
            throw new IOException("hosted movement requires an existing canonical character");
        }
        CharacterStateRecord persisted = loadOrCreateStrict(identity, expected.characterName());
        if (!persisted.equals(expected)) {
            throw new IllegalStateException("stale canonical character state");
        }
        CharacterStateRecord moved = new CharacterStateRecord(
                persisted.identityKey(), persisted.characterName(),
                toX, toY, persisted.z(), persisted.zoneId(), persisted.health(),
                persisted.selectedSkills(), persisted.startingItems(),
                persisted.factionReputation(), persisted.updatedAt());
        atomicSaveSync(moved);
        return loadOrCreateStrict(identity, persisted.characterName());
    }

    Path profilePath(PlayerIdentity identity) throws IOException { return profileGuard.resolveInside(safeFileName(identity.storageKey()) + ".dat"); }

    private static String safeFileName(String value) { return value.replaceAll("[^A-Za-z0-9._-]", "_"); }

    @Override public void close() { ioExecutor.shutdown(); }
}
