package mechanist;

import javax.swing.Timer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns the desktop single-player internal-host lifecycle.
 *
 * The simulation remains in-process for this alpha line, but world mutation is
 * delegated to SinglePlayerSectorRuntimeBridge's authoritative single-writer
 * lane. This supervisor mounts that bridge once, binds the local session when a
 * world becomes available, reports health, and guarantees orderly shutdown.
 */
final class SinglePlayerInternalHostSupervisor implements AutoCloseable {
    static final String VERSION = "single-player-internal-host-" + BuildIdentityAuthority.version();

    enum State {
        STARTING,
        WAITING_FOR_WORLD,
        ACTIVE,
        FAILED,
        CLOSED
    }

    private final GamePanel game;
    private final RuntimeProfile runtimeProfile;
    private final SinglePlayerSectorRuntimeBridge bridge;
    private final Timer lifecycleTimer;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile State state = State.STARTING;
    private volatile String lastReason = "mount requested";
    private volatile String lastFailure = "none";
    private volatile long refreshCount;

    private SinglePlayerInternalHostSupervisor(
            GamePanel game,
            RuntimeProfile runtimeProfile,
            SinglePlayerSectorRuntimeBridge bridge
    ) {
        this.game = Objects.requireNonNull(game, "game");
        this.runtimeProfile = runtimeProfile == null ? RuntimeProfile.defaultProfile() : runtimeProfile;
        this.bridge = Objects.requireNonNull(bridge, "bridge");
        this.lifecycleTimer = new Timer(250, event -> refreshNow("lifecycle poll"));
        this.lifecycleTimer.setRepeats(true);
        this.lifecycleTimer.setCoalesce(true);
    }

    static SinglePlayerInternalHostSupervisor mount(GamePanel game, RuntimeProfile runtimeProfile) {
        Objects.requireNonNull(game, "game");
        SinglePlayerSectorRuntimeBridge bridge = game.singlePlayerSectorBridge;
        if (bridge == null) {
            bridge = new SinglePlayerSectorRuntimeBridge(new SectorManager());
            game.singlePlayerSectorBridge = bridge;
        }
        SinglePlayerInternalHostSupervisor supervisor =
                new SinglePlayerInternalHostSupervisor(game, runtimeProfile, bridge);
        supervisor.bridge.bindSession(game.userProfile);
        supervisor.refreshNow("application mount");
        supervisor.lifecycleTimer.start();
        DebugLog.audit("SINGLE_PLAYER_INTERNAL_HOST", supervisor.statusLine());
        return supervisor;
    }

    void refreshNow(String reason) {
        if (closed.get()) return;
        refreshCount++;
        lastReason = safe(reason);
        try {
            if (game.singlePlayerSectorBridge == null) {
                game.singlePlayerSectorBridge = bridge;
            } else if (game.singlePlayerSectorBridge != bridge) {
                throw new IllegalStateException("GamePanel mounted a competing single-player authority bridge");
            }
            bridge.bindSession(game.userProfile);
            if (game.world == null || game.active == null) {
                state = State.WAITING_FOR_WORLD;
                return;
            }
            bridge.bindCurrentWorld(game, lastReason);
            state = State.ACTIVE;
            if (refreshCount <= 3 || refreshCount % 40 == 0) {
                DebugLog.audit("SINGLE_PLAYER_INTERNAL_HOST_HEALTH", statusLine());
            }
        } catch (Throwable failure) {
            state = State.FAILED;
            lastFailure = safe(failure.getMessage());
            DebugLog.error("SINGLE_PLAYER_INTERNAL_HOST", "Internal host supervision failed: " + lastFailure, failure);
        }
    }

    State state() {
        return state;
    }

    boolean active() {
        return state == State.ACTIVE && !closed.get();
    }

    SinglePlayerSectorRuntimeBridge bridge() {
        return bridge;
    }

    String statusLine() {
        return "authority=" + VERSION
                + " state=" + state
                + " build=" + BuildIdentityAuthority.version()
                + " requestedMode=" + runtimeProfile.requestedMode
                + " effectiveMode=" + runtimeProfile.effectiveMode
                + " internalServerRequested=" + runtimeProfile.internalServerRequested
                + " world=" + (game.world == null ? "none" : game.world.zoneCoordText())
                + " character=" + (game.active == null ? "none" : safe(game.active.name))
                + " refreshes=" + refreshCount
                + " lastReason=" + lastReason
                + " failure=" + lastFailure
                + " bridge={" + bridge.statusLine() + "}";
    }

    static String auditSummary() {
        return "authority=" + VERSION
                + " model=in-process-authoritative-host"
                + " worldMutation=single-writer"
                + " localSession=bound"
                + " shutdown=supervised"
                + " networkClientSession=not-required-for-local-alpha";
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        lifecycleTimer.stop();
        state = State.CLOSED;
        try {
            bridge.close();
        } catch (Throwable failure) {
            lastFailure = safe(failure.getMessage());
            DebugLog.error("SINGLE_PLAYER_INTERNAL_HOST_CLOSE", "Internal host shutdown failed: " + lastFailure, failure);
        } finally {
            if (game.singlePlayerSectorBridge == bridge) game.singlePlayerSectorBridge = null;
            DebugLog.audit("SINGLE_PLAYER_INTERNAL_HOST_CLOSE", statusLine());
        }
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "unspecified" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
