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
    static final String VERSION = "authoritative-world-runtime-0.9.10gn";

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
    private final AtomicLong worldVersion = new AtomicLong(0L);
    private final AtomicLong submissions = new AtomicLong(0L);
    private final AtomicLong submissionsFromEdt = new AtomicLong(0L);
    private final AtomicLong rejected = new AtomicLong(0L);
    private final AtomicBoolean closed = new AtomicBoolean(false);

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
        return latestSnapshot.get();
    }

    WorldSnapshot latestWorldSnapshot() {
        return latestWorldSnapshot.get();
    }

    long worldVersion() {
        return worldVersion.get();
    }

    long submissionsFromEdt() {
        return submissionsFromEdt.get();
    }

    String statusLine() {
        AuthoritativeWorldSnapshot snapshot = latestSnapshot.get();
        WorldSnapshot worldSnapshot = latestWorldSnapshot.get();
        return "authority=" + VERSION
                + " worldVersion=" + worldVersion.get()
                + " submissions=" + submissions.get()
                + " edtSubmissions=" + submissionsFromEdt.get()
                + " rejected=" + rejected.get()
                + " latest="
                + (snapshot == null ? "none" : snapshot.compact())
                + " worldSnapshot="
                + (worldSnapshot == null
                ? "none"
                : worldSnapshot.compact());
    }

    static String auditSummary() {
        return "authority=" + VERSION
                + " lane=single-writer-authoritative"
                + " mutationThread=not-swing"
                + " snapshotSource=desktop-or-headless"
                + " snapshot=atomic-reference"
                + " immutableWorldSnapshot=published-after-commit";
    }

    private AuthoritativeWorldSnapshot publishSnapshot(
            SnapshotSource source,
            String playerId,
            SectorKey sector,
            String reason,
            SectorSnapshot sectorSnapshot
    ) {
        long version = worldVersion.incrementAndGet();
        WorldSnapshot worldSnapshot =
                source.worldSnapshot(version, sector);
        if (worldSnapshot == null) {
            worldSnapshot = new WorldSnapshot(
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
        latestWorldSnapshot.set(worldSnapshot);
        AuthoritativeWorldSnapshot snapshot =
                source.authoritativeSnapshot(
                        version,
                        playerId,
                        sector,
                        reason,
                        sectorSnapshot,
                        worldSnapshot,
                        Thread.currentThread().getName());
        if (snapshot == null) {
            snapshot = new AuthoritativeWorldSnapshot(
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
        latestSnapshot.set(snapshot);
        if (version <= 3L || version % 50L == 0L) {
            DebugLog.audit(
                    "AUTHORITATIVE_WORLD_SNAPSHOT",
                    snapshot.compact());
        }
        return snapshot;
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
