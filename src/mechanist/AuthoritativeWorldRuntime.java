package mechanist;

import javax.swing.SwingUtilities;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Single-writer authoritative lane for committed world mutation.
 *
 * Desktop single-player and headless server runtimes share this lane. The
 * caller supplies one immutable snapshot source; the mutation executor owns the
 * commit and publishes snapshots only after the commit succeeds.
 */
final class AuthoritativeWorldRuntime implements AutoCloseable {
    static final String VERSION = "authoritative-world-runtime-0.9.10gp";

    interface MutationCommit {
        SectorSnapshot run();
    }

    interface SnapshotSource {
        WorldSnapshot worldSnapshot(long version, SectorKey sector);

        AuthoritativeWorldSnapshot authoritativeSnapshot(
                long version,
                String playerId,
                SectorKey sector,
                String reason,
                SectorSnapshot sectorSnapshot,
                WorldSnapshot worldSnapshot,
                String mutationThread);

        static SnapshotSource fromGamePanel(GamePanel game) {
            return new SnapshotSource() {
                @Override
                public WorldSnapshot worldSnapshot(
                        long version,
                        SectorKey sector
                ) {
                    return WorldSnapshot.fromGame(version, game, sector);
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
                    return AuthoritativeWorldSnapshot.fromGame(
                            version,
                            game,
                            playerId,
                            sector,
                            reason,
                            sectorSnapshot,
                            worldSnapshot,
                            mutationThread);
                }
            };
        }
    }

    private final ExecutorService mutationExecutor;
    private final AtomicReference<AuthoritativeWorldSnapshot> latestSnapshot =
            new AtomicReference<>();
    private final AtomicReference<WorldSnapshot> latestWorldSnapshot =
            new AtomicReference<>();
    private final AtomicReference<Thread> ownerThread = new AtomicReference<>();
    private final AtomicReference<String> publicationFailure =
            new AtomicReference<>();
    private final AtomicLong worldVersion = new AtomicLong(0L);
    private final AtomicLong submissions = new AtomicLong(0L);
    private final AtomicLong submissionsFromEdt = new AtomicLong(0L);
    private final AtomicLong rejected = new AtomicLong(0L);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object publicationLock = new Object();

    AuthoritativeWorldRuntime(String name) {
        String threadName = name == null || name.isBlank()
                ? "mechanist-authoritative-world"
                : name.trim();
        ThreadFactory factory = runnable -> {
            Thread thread = new Thread(runnable, threadName);
            thread.setDaemon(true);
            ownerThread.set(thread);
            return thread;
        };
        this.mutationExecutor = Executors.newSingleThreadExecutor(factory);
    }

    AuthoritativeWorldSnapshot submitAndJoin(
            GamePanel game,
            String playerId,
            SectorKey sector,
            String reason,
            MutationCommit commit
    ) {
        return submitAndJoin(
                SnapshotSource.fromGamePanel(game),
                playerId,
                sector,
                reason,
                commit);
    }

    AuthoritativeWorldSnapshot submitAndJoin(
            SnapshotSource source,
            String playerId,
            SectorKey sector,
            String reason,
            MutationCommit commit
    ) {
        Objects.requireNonNull(source, "snapshot source");
        Objects.requireNonNull(commit, "commit");
        if (closed.get()) {
            rejected.incrementAndGet();
            throw new IllegalStateException(
                    "AuthoritativeWorldRuntime is closed");
        }
        submissions.incrementAndGet();
        if (SwingUtilities.isEventDispatchThread()) {
            submissionsFromEdt.incrementAndGet();
        }
        Thread owner = ownerThread.get();
        if (owner != null && Thread.currentThread() == owner) {
            requirePublicationCapacity();
            SectorSnapshot sectorSnapshot = commit.run();
            return publishSnapshot(
                    source,
                    playerId,
                    sector,
                    reason,
                    sectorSnapshot);
        }
        CompletableFuture<AuthoritativeWorldSnapshot> future =
                CompletableFuture.supplyAsync(() -> {
                    assertNotSwingThreadForWorldMutation(reason);
                    requirePublicationCapacity();
                    SectorSnapshot sectorSnapshot = commit.run();
                    return publishSnapshot(
                            source,
                            playerId,
                            sector,
                            reason,
                            sectorSnapshot);
                }, mutationExecutor);
        try {
            return future.get();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            rejected.incrementAndGet();
            throw new RuntimeException(
                    "Interrupted while waiting for authoritative world mutation",
                    interrupted);
        } catch (Exception failure) {
            rejected.incrementAndGet();
            Throwable cause = failure.getCause() == null
                    ? failure
                    : failure.getCause();
            if (cause instanceof RuntimeException runtimeFailure) {
                throw runtimeFailure;
            }
            throw new RuntimeException(
                    "Authoritative world mutation failed",
                    cause);
        }
    }

    AuthoritativeWorldSnapshot latestSnapshot() {
        synchronized (publicationLock) {
            return latestSnapshot.get();
        }
    }

    WorldSnapshot latestWorldSnapshot() {
        synchronized (publicationLock) {
            return latestWorldSnapshot.get();
        }
    }

    long worldVersion() {
        synchronized (publicationLock) {
            return worldVersion.get();
        }
    }

    long submissionsFromEdt() {
        return submissionsFromEdt.get();
    }

    String statusLine() {
        synchronized (publicationLock) {
            AuthoritativeWorldSnapshot snapshot = latestSnapshot.get();
            WorldSnapshot worldSnapshot = latestWorldSnapshot.get();
            String failure = publicationFailure.get();
            return "authority=" + VERSION
                    + " worldVersion=" + worldVersion.get()
                    + " submissions=" + submissions.get()
                    + " edtSubmissions=" + submissionsFromEdt.get()
                    + " rejected=" + rejected.get()
                    + " publicationState="
                    + (failure == null ? "healthy" : "failed-closed")
                    + " latest="
                    + (snapshot == null ? "none" : snapshot.compact())
                    + " worldSnapshot="
                    + (worldSnapshot == null
                    ? "none"
                    : worldSnapshot.compact());
        }
    }

    static String auditSummary() {
        return "authority=" + VERSION
                + " lane=single-writer-authoritative"
                + " mutationThread=not-swing"
                + " snapshotSource=desktop-or-headless"
                + " snapshot=atomic-reference"
                + " immutableWorldSnapshot=published-after-commit"
                + " snapshotIdentity=validated"
                + " publicationFailure=fail-closed";
    }

    private void requirePublicationCapacity() {
        synchronized (publicationLock) {
            String failure = publicationFailure.get();
            if (failure != null) {
                throw new IllegalStateException(
                        "AuthoritativeWorldRuntime publication failed after mutation; "
                                + "runtime is failed closed: " + failure);
            }
            if (worldVersion.get() == Long.MAX_VALUE) {
                throw new IllegalStateException(
                        "AuthoritativeWorldRuntime world version exhausted supported range");
            }
        }
    }

    private AuthoritativeWorldSnapshot publishSnapshot(
            SnapshotSource source,
            String playerId,
            SectorKey sector,
            String reason,
            SectorSnapshot sectorSnapshot
    ) {
        final long version;
        final WorldSnapshot worldSnapshot;
        final AuthoritativeWorldSnapshot snapshot;
        try {
            synchronized (publicationLock) {
                version = Math.incrementExact(worldVersion.get());
            }
            WorldSnapshot candidateWorldSnapshot =
                    source.worldSnapshot(version, sector);
            if (candidateWorldSnapshot == null) {
                candidateWorldSnapshot = new WorldSnapshot(
                        version,
                        sector,
                        PlayerSnapshot.empty(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        java.util.List.of(),
                        UiStateSnapshot.empty(),
                        System.currentTimeMillis());
            }
            validateWorldSnapshotIdentity(
                    version,
                    sector,
                    candidateWorldSnapshot);
            worldSnapshot = candidateWorldSnapshot;
            AuthoritativeWorldSnapshot candidateSnapshot =
                    source.authoritativeSnapshot(
                            version,
                            playerId,
                            sector,
                            reason,
                            sectorSnapshot,
                            worldSnapshot,
                            Thread.currentThread().getName());
            if (candidateSnapshot == null) {
                candidateSnapshot = new AuthoritativeWorldSnapshot(
                        version,
                        safe(playerId),
                        sector,
                        safe(reason),
                        0L,
                        0L,
                        0,
                        0,
                        "none",
                        "none",
                        0,
                        0,
                        "none",
                        "none",
                        worldSnapshot,
                        Thread.currentThread().getName(),
                        sectorSnapshot,
                        System.currentTimeMillis());
            }
            validateAuthoritativeSnapshotIdentity(
                    version,
                    playerId,
                    sector,
                    worldSnapshot,
                    candidateSnapshot);
            snapshot = candidateSnapshot;
        } catch (RuntimeException | Error failure) {
            publicationFailure.compareAndSet(
                    null,
                    failure.getClass().getSimpleName()
                            + ": " + safe(failure.getMessage()));
            throw failure;
        }
        synchronized (publicationLock) {
            worldVersion.set(version);
            latestWorldSnapshot.set(worldSnapshot);
            latestSnapshot.set(snapshot);
        }
        if (version <= 3L || version % 50L == 0L) {
            DebugLog.audit(
                    "AUTHORITATIVE_WORLD_SNAPSHOT",
                    snapshot.compact());
        }
        return snapshot;
    }

    private static void validateWorldSnapshotIdentity(
            long version,
            SectorKey sector,
            WorldSnapshot snapshot
    ) {
        if (snapshot.version() != version) {
            throw new IllegalStateException(
                    "Snapshot source returned world version "
                            + snapshot.version()
                            + " for authoritative version " + version);
        }
        if (!Objects.equals(snapshot.currentSector(), sector)) {
            throw new IllegalStateException(
                    "Snapshot source returned world sector "
                            + (snapshot.currentSector() == null
                            ? "none"
                            : snapshot.currentSector().compact())
                            + " for authoritative sector "
                            + (sector == null ? "none" : sector.compact()));
        }
    }

    private static void validateAuthoritativeSnapshotIdentity(
            long version,
            String playerId,
            SectorKey sector,
            WorldSnapshot worldSnapshot,
            AuthoritativeWorldSnapshot snapshot
    ) {
        if (snapshot.version() != version) {
            throw new IllegalStateException(
                    "Snapshot source returned authoritative version "
                            + snapshot.version()
                            + " for authoritative version " + version);
        }
        String expectedPlayerId = safe(playerId);
        if (!Objects.equals(snapshot.playerId(), expectedPlayerId)) {
            throw new IllegalStateException(
                    "Snapshot source returned authoritative player "
                            + safe(snapshot.playerId())
                            + " for submitted player " + expectedPlayerId);
        }
        if (!Objects.equals(snapshot.sector(), sector)) {
            throw new IllegalStateException(
                    "Snapshot source returned authoritative sector "
                            + (snapshot.sector() == null
                            ? "none"
                            : snapshot.sector().compact())
                            + " for authoritative sector "
                            + (sector == null ? "none" : sector.compact()));
        }
        if (snapshot.worldSnapshot() != worldSnapshot) {
            throw new IllegalStateException(
                    "Snapshot source detached authoritative snapshot from "
                            + "the validated world snapshot instance");
        }
    }

    static void assertNotSwingThreadForWorldMutation(String reason) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "World mutation attempted on Swing EDT; reason="
                            + safe(reason));
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        mutationExecutor.shutdownNow();
        try {
            mutationExecutor.awaitTermination(
                    500L,
                    TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
        DebugLog.audit(
                "AUTHORITATIVE_WORLD_CLOSE",
                statusLine());
    }

    private static String safe(String value) {
        return value == null || value.isBlank()
                ? "unspecified"
                : value.replace('\n', ' ').replace('\r', ' ');
    }
}

record AuthoritativeWorldSnapshot(
        long version,
        String playerId,
        SectorKey sector,
        String reason,
        long turn,
        long worldTurn,
        int playerX,
        int playerY,
        String screen,
        String zone,
        int carriedScript,
        int recentEvents,
        String activeName,
        String activeAction,
        WorldSnapshot worldSnapshot,
        String mutationThread,
        SectorSnapshot sectorSnapshot,
        long committedAtMillis
) {
    static AuthoritativeWorldSnapshot fromGame(
            long version,
            GamePanel game,
            String playerId,
            SectorKey sector,
            String reason,
            SectorSnapshot sectorSnapshot,
            WorldSnapshot worldSnapshot,
            String mutationThread
    ) {
        if (game == null) {
            return new AuthoritativeWorldSnapshot(
                    version,
                    safe(playerId),
                    sector,
                    safe(reason),
                    0L,
                    0L,
                    0,
                    0,
                    "none",
                    "none",
                    0,
                    0,
                    "none",
                    "none",
                    worldSnapshot,
                    safe(mutationThread),
                    sectorSnapshot,
                    System.currentTimeMillis());
        }
        String zone = game.world == null
                ? "none"
                : game.world.zoneType.label
                + " "
                + game.world.zoneCoordText();
        String activeName = game.active == null
                ? "none"
                : game.active.name;
        String activeAction = game.singlePlayerSectorBridge == null
                ? "none"
                : game.singlePlayerSectorBridge.activeActionDisplayLine();
        if (activeAction == null || activeAction.isBlank()) {
            activeAction = "none";
        }
        return new AuthoritativeWorldSnapshot(
                version,
                safe(playerId),
                sector,
                safe(reason),
                game.turn,
                game.worldTurn,
                game.playerX,
                game.playerY,
                String.valueOf(game.screen),
                zone,
                game.countMoney(),
                game.eventLog.size(),
                activeName,
                safe(activeAction),
                worldSnapshot,
                safe(mutationThread),
                sectorSnapshot,
                System.currentTimeMillis());
    }

    String compact() {
        return "v=" + version
                + " player=" + playerId
                + " sector="
                + (sector == null ? "none" : sector.compact())
                + " turn=" + turn
                + " worldTurn=" + worldTurn
                + " pos=" + playerX + "," + playerY
                + " screen=" + screen
                + " script=" + carriedScript
                + " action=" + activeAction
                + " worldSnapshot="
                + (worldSnapshot == null
                ? "none"
                : ("v" + worldSnapshot.version()))
                + " thread=" + mutationThread
                + " reason=" + reason;
    }

    private static String safe(String value) {
        return value == null || value.isBlank()
                ? "unspecified"
                : value.replace('\n', ' ').replace('\r', ' ');
    }
}
