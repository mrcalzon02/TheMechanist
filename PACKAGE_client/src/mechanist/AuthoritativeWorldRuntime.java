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

/** Single-writer authoritative lane for legacy World mutation under the internal server runtime. */
final class AuthoritativeWorldRuntime implements AutoCloseable {
    static final String VERSION = "authoritative-world-runtime-0.9.10gm";

    interface MutationCommit {
        SectorSnapshot run();
    }

    private final ExecutorService mutationExecutor;
    private final AtomicReference<AuthoritativeWorldSnapshot> latestSnapshot = new AtomicReference<>();
    private final AtomicReference<WorldSnapshot> latestWorldSnapshot = new AtomicReference<>();
    private final AtomicReference<Thread> ownerThread = new AtomicReference<>();
    private final AtomicLong worldVersion = new AtomicLong(0L);
    private final AtomicLong submissions = new AtomicLong(0L);
    private final AtomicLong submissionsFromEdt = new AtomicLong(0L);
    private final AtomicLong rejected = new AtomicLong(0L);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    AuthoritativeWorldRuntime(String name) {
        String threadName = name == null || name.isBlank() ? "mechanist-authoritative-world" : name.trim();
        ThreadFactory factory = r -> {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            ownerThread.set(t);
            return t;
        };
        this.mutationExecutor = Executors.newSingleThreadExecutor(factory);
    }

    AuthoritativeWorldSnapshot submitAndJoin(GamePanel game, String playerId, SectorKey sector, String reason, MutationCommit commit) {
        Objects.requireNonNull(commit, "commit");
        if (closed.get()) {
            rejected.incrementAndGet();
            throw new IllegalStateException("AuthoritativeWorldRuntime is closed");
        }
        submissions.incrementAndGet();
        if (SwingUtilities.isEventDispatchThread()) submissionsFromEdt.incrementAndGet();
        Thread owner = ownerThread.get();
        if (owner != null && Thread.currentThread() == owner) {
            SectorSnapshot ss = commit.run();
            return publishSnapshot(game, playerId, sector, reason, ss);
        }
        CompletableFuture<AuthoritativeWorldSnapshot> future = CompletableFuture.supplyAsync(() -> {
            assertNotSwingThreadForWorldMutation(reason);
            SectorSnapshot ss = commit.run();
            return publishSnapshot(game, playerId, sector, reason, ss);
        }, mutationExecutor);
        try {
            return future.get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            rejected.incrementAndGet();
            throw new RuntimeException("Interrupted while waiting for authoritative world mutation", ie);
        } catch (Exception e) {
            rejected.incrementAndGet();
            Throwable cause = e.getCause() == null ? e : e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException("Authoritative world mutation failed", cause);
        }
    }

    AuthoritativeWorldSnapshot latestSnapshot() {
        return latestSnapshot.get();
    }

    WorldSnapshot latestWorldSnapshot() {
        return latestWorldSnapshot.get();
    }

    long worldVersion() { return worldVersion.get(); }
    long submissionsFromEdt() { return submissionsFromEdt.get(); }

    String statusLine() {
        AuthoritativeWorldSnapshot s = latestSnapshot.get();
        return "authority=" + VERSION
                + " worldVersion=" + worldVersion.get()
                + " submissions=" + submissions.get()
                + " edtSubmissions=" + submissionsFromEdt.get()
                + " rejected=" + rejected.get()
                + " latest=" + (s == null ? "none" : s.compact())
                + " worldSnapshot=" + (latestWorldSnapshot.get() == null ? "none" : latestWorldSnapshot.get().compact());
    }

    static String auditSummary() {
        return "authority=" + VERSION + " lane=single-writer-internal-server mutationThread=not-swing snapshot=atomic-reference immutableWorldSnapshot=published-after-commit";
    }

    private AuthoritativeWorldSnapshot publishSnapshot(GamePanel game, String playerId, SectorKey sector, String reason, SectorSnapshot sectorSnapshot) {
        long version = worldVersion.incrementAndGet();
        WorldSnapshot worldSnapshot = WorldSnapshot.fromGame(version, game, sector);
        latestWorldSnapshot.set(worldSnapshot);
        AuthoritativeWorldSnapshot snapshot = AuthoritativeWorldSnapshot.fromGame(version, game, playerId, sector, reason, sectorSnapshot, worldSnapshot, Thread.currentThread().getName());
        latestSnapshot.set(snapshot);
        if (version <= 3 || version % 50L == 0L) DebugLog.audit("AUTHORITATIVE_WORLD_SNAPSHOT", snapshot.compact());
        return snapshot;
    }

    static void assertNotSwingThreadForWorldMutation(String reason) {
        if (SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("World mutation attempted on Swing EDT; reason=" + (reason == null ? "unspecified" : reason));
        }
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        mutationExecutor.shutdownNow();
        try { mutationExecutor.awaitTermination(500L, TimeUnit.MILLISECONDS); }
        catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        DebugLog.audit("AUTHORITATIVE_WORLD_CLOSE", statusLine());
    }
}

record AuthoritativeWorldSnapshot(long version,
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
                                  long committedAtMillis) {
    static AuthoritativeWorldSnapshot fromGame(long version, GamePanel game, String playerId, SectorKey sector, String reason, SectorSnapshot sectorSnapshot, WorldSnapshot worldSnapshot, String mutationThread) {
        if (game == null) {
            return new AuthoritativeWorldSnapshot(version, safe(playerId), sector, safe(reason), 0L, 0L, 0, 0, "none", "none", 0, 0, "none", "none", worldSnapshot, safe(mutationThread), sectorSnapshot, System.currentTimeMillis());
        }
        String zone = game.world == null ? "none" : game.world.zoneType.label + " " + game.world.zoneCoordText();
        String activeName = game.active == null ? "none" : game.active.name;
        String activeAction = game.singlePlayerSectorBridge == null ? "none" : game.singlePlayerSectorBridge.activeActionDisplayLine();
        if (activeAction == null || activeAction.isBlank()) activeAction = "none";
        return new AuthoritativeWorldSnapshot(version, safe(playerId), sector, safe(reason), game.turn, game.worldTurn, game.playerX, game.playerY,
                String.valueOf(game.screen), zone, game.countMoney(), game.eventLog.size(), activeName, safe(activeAction), worldSnapshot, safe(mutationThread), sectorSnapshot, System.currentTimeMillis());
    }

    String compact() {
        return "v=" + version
                + " player=" + playerId
                + " sector=" + (sector == null ? "none" : sector.compact())
                + " turn=" + turn
                + " worldTurn=" + worldTurn
                + " pos=" + playerX + "," + playerY
                + " screen=" + screen
                + " script=" + carriedScript
                + " action=" + activeAction
                + " worldSnapshot=" + (worldSnapshot == null ? "none" : ("v" + worldSnapshot.version()))
                + " thread=" + mutationThread
                + " reason=" + reason;
    }

    private static String safe(String s) { return s == null || s.isBlank() ? "unspecified" : s.replace('\n', ' '); }
}
