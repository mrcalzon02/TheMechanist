package mechanist;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Routes the desktop single-player loop through the local internal-server boundary.
 * Legacy World mutation is committed on a single authoritative world lane; Swing submits
 * requests and renders the latest committed state instead of owning world mutation.
 */
final class SinglePlayerSectorRuntimeBridge implements AutoCloseable, AdminCommandDispatcher.MutationSink {
    static final String VERSION = "single-player-sector-runtime-0.9.10gm";
    static final String LOCAL_PLAYER_ID = "single-player-local";

    private final SectorManager sectorManager;
    private final AuthoritativeWorldRuntime authoritativeWorldRuntime;
    private final WorldTurnManager worldTurnManager;
    private final PlayerActionRegistry actionRegistry;
    private InternalServerSessionAuthority sessionAuthority;
    private AdminCommandDispatcher adminDispatcher;
    private SectorKey currentSector;
    private final AtomicBoolean insideAuthoritativeTurn = new AtomicBoolean(false);
    private boolean closed = false;
    private long routedTurns = 0L;
    private String lastReason = "No single-player sector turn routed yet.";
    private String lastRejectedCommand = "none";
    private final AtomicLong lastRenderedSnapshotVersion = new AtomicLong(0L);

    SinglePlayerSectorRuntimeBridge(SectorManager sectorManager) {
        this.sectorManager = Objects.requireNonNull(sectorManager, "sectorManager");
        this.authoritativeWorldRuntime = new AuthoritativeWorldRuntime("mechanist-authoritative-world-single-player");
        this.worldTurnManager = new WorldTurnManager();
        this.actionRegistry = new PlayerActionRegistry();
    }

    void bindSession(UserProfileAuthority.Profile profile) {
        if (sessionAuthority != null) return;
        sessionAuthority = new InternalServerSessionAuthority(profile, LOCAL_PLAYER_ID);
        adminDispatcher = new AdminCommandDispatcher(worldTurnManager, actionRegistry, this);
        DebugLog.audit("INTERNAL_SERVER_SESSION", sessionAuthority.statusLine());
        DebugLog.audit("ADMIN_COMMAND_DISPATCHER", AdminCommandDispatcher.auditSummary());
        DebugLog.audit("WORLD_TURN_MANAGER", WorldTurnManager.auditSummary());
        DebugLog.audit("PLAYER_ACTION_REGISTRY", PlayerActionRegistry.auditSummary());
        DebugLog.audit("AUTHORITATIVE_WORLD_RUNTIME", AuthoritativeWorldRuntime.auditSummary());
    }

    boolean shouldOwnAuthoritativeTurns(GamePanel game) {
        return game != null && game.world != null && game.active != null && !closed;
    }

    boolean insideAuthoritativeTurn() {
        return insideAuthoritativeTurn.get();
    }

    void bindCurrentWorld(GamePanel game, String reason) {
        if (!shouldOwnAuthoritativeTurns(game)) return;
        bindSession(game.userProfile);
        SectorKey key = keyFor(game);
        if (key == null || key.equals(currentSector)) return;
        if (currentSector != null) {
            sectorManager.playerLeftCurrentSector(LOCAL_PLAYER_ID);
        }
        currentSector = key;
        sectorManager.playerEnteredSector(LOCAL_PLAYER_ID, key);
        worldTurnManager.setWorldMode(key.compact(), WorldTurnManager.PlayMode.STRICT_TURN_BASED);
        lastReason = safe(reason);
        DebugLog.audit("SINGLE_PLAYER_SECTOR_BIND", "player=" + LOCAL_PLAYER_ID + " sector=" + key.compact() + " reason=" + lastReason + " " + sectorManager.statusLine());
    }

    void runAuthoritativeTurn(GamePanel game, String reason, Runnable legacyTurnBody) {
        if (legacyTurnBody == null) return;
        if (!shouldOwnAuthoritativeTurns(game) || insideAuthoritativeTurn()) {
            legacyTurnBody.run();
            return;
        }
        bindCurrentWorld(game, reason);
        SectorKey key = currentSector;
        if (key == null) {
            legacyTurnBody.run();
            return;
        }
        if (actionRegistry.isGated(LOCAL_PLAYER_ID) && reason != null && !reason.startsWith("continuous server tick") && !reason.startsWith("long-action")) {
            DebugLog.warn("AUTHORITATIVE_COMMAND_REJECTED", "reason=player-gated action=" + actionRegistry.activeAction(LOCAL_PLAYER_ID));
            return;
        }
        insideAuthoritativeTurn.set(true);
        try {
            AuthoritativeWorldSnapshot snapshot = authoritativeWorldRuntime.submitAndJoin(game, LOCAL_PLAYER_ID, key, reason, () -> {
                SectorSnapshot sectorSnapshot = sectorManager.runLocalAuthoritativeTurn(LOCAL_PLAYER_ID, key, reason, () -> {
                    legacyTurnBody.run();
                    routedTurns++;
                    lastReason = safe(reason);
                });
                return sectorSnapshot;
            });
            if (snapshot != null && (routedTurns <= 3 || routedTurns % 50 == 0)) {
                DebugLog.audit("SINGLE_PLAYER_AUTHORITATIVE_TURN", "turns=" + routedTurns + " " + snapshot.compact());
            }
        } finally {
            insideAuthoritativeTurn.set(false);
        }
    }

    void maybeRunContinuousTick(GamePanel game) {
        if (!shouldOwnAuthoritativeTurns(game) || insideAuthoritativeTurn()) return;
        bindCurrentWorld(game, "continuous tick check");
        SectorKey key = currentSector;
        if (key == null) return;
        if (!worldTurnManager.shouldTickContinuous(key.compact())) return;
        runAuthoritativeTurn(game, "continuous server tick", () -> {
            tickRegisteredActionsInsideAuthority(game, key, "continuous server tick");
            game.advanceTurnBody("passes under continuous server time.");
        });
    }


    @Override
    public AuthoritativeWorldSnapshot submitCommand(GamePanel game, WorldCommandRequest command) {
        if (command == null) return authoritativeWorldRuntime.latestSnapshot();
        if (!LOCAL_PLAYER_ID.equals(command.playerId())) {
            return rejectCommand(game, command, "stale or foreign player/session id");
        }
        bindCurrentWorld(game, command.reason());
        String rejection = command.rejectionReason(game);
        if (rejection != null && !rejection.isBlank()) return rejectCommand(game, command, rejection);
        if (actionRegistry.isGated(command.playerId()) && command.requiresUngatedPlayer()) {
            if (command instanceof WaitCommand) {
                return advanceGatedActionOneStep(game, "long-action wait command");
            }
            PlayerActionRegistry.ActiveAction active = actionRegistry.activeAction(command.playerId());
            String busy = active == null ? "player-gated" : active.compact();
            if (game != null) game.logEvent("ACTION BUSY: " + busy);
            return rejectCommand(game, command, "player gated by action " + busy);
        }
        DebugLog.audit("WORLD_COMMAND_REQUEST", command.auditName());
        return mutate(game, command.reason(), () -> command.apply(game));
    }

    private AuthoritativeWorldSnapshot rejectCommand(GamePanel game, WorldCommandRequest command, String reason) {
        String line = "reason=" + safe(reason) + " command=" + (command == null ? "none" : command.auditName());
        lastRejectedCommand = line;
        DebugLog.warn("AUTHORITATIVE_COMMAND_REJECTED", line);
        if (game != null && command != null && !(command.requiresAdminAuthority())) game.logEvent("Command rejected by local server authority: " + safe(reason) + ".");
        return authoritativeWorldRuntime.latestSnapshot();
    }

    String submitConsoleCommand(GamePanel game, ConsoleCommandRequest request) {
        ConsoleCommandRequest use = request == null ? new ConsoleCommandRequest(LOCAL_PLAYER_ID, "") : request;
        bindSession(game == null ? null : game.userProfile);
        bindCurrentWorld(game, "console command context");
        if (!LOCAL_PLAYER_ID.equals(use.playerId())) {
            lastRejectedCommand = "reason=foreign-console-session request=" + use.auditName();
            DebugLog.warn("ADMIN_COMMAND_REJECTED", lastRejectedCommand);
            return "Access denied: console request does not belong to the mounted local session.";
        }
        InternalServerSessionAuthority.CommandContext context = sessionAuthority == null ? null : sessionAuthority.commandContext(currentSector);
        String result = adminDispatcher == null ? "Admin dispatcher is not mounted." : adminDispatcher.executeCommand(game, context, use);
        DebugLog.audit("ADMIN_COMMAND", "request=" + use.auditName() + " result=" + result);
        return result;
    }

    AuthoritativeWorldSnapshot advanceGatedActionOneStep(GamePanel game, String reason) {
        if (!shouldOwnAuthoritativeTurns(game) || insideAuthoritativeTurn()) return authoritativeWorldRuntime.latestSnapshot();
        bindCurrentWorld(game, reason);
        SectorKey key = currentSector;
        if (key == null) return authoritativeWorldRuntime.latestSnapshot();
        return mutate(game, reason, () -> {
            PlayerActionRegistry.TickSummary summary = tickRegisteredActionsInsideAuthority(game, key, reason);
            if (summary.completedCommands().isEmpty()) {
                game.advanceTurnBody("works through the current server-gated action.");
                game.settlePlayerMotionAfterNoMoveTurn("server-gated-action");
            }
        });
    }

    private PlayerActionRegistry.TickSummary tickRegisteredActionsInsideAuthority(GamePanel game, SectorKey key, String trigger) {
        PlayerActionRegistry.TickSummary summary = actionRegistry.tickWorldActions(key == null ? "local-world" : key.compact());
        if (summary.activeBefore() > 0 || !summary.completedCommands().isEmpty()) {
            DebugLog.audit("PLAYER_ACTION_TICK", "trigger=" + safe(trigger) + " " + summary.compact());
            if (game != null) {
                for (String line : summary.progressLines()) game.logEvent("ACTION PROGRESS: " + line);
            }
        }
        for (WorldCommandRequest completed : summary.completedCommands()) {
            if (completed == null) continue;
            DebugLog.audit("PLAYER_ACTION_COMPLETION_COMMAND", completed.auditName());
            completed.apply(game);
        }
        return summary;
    }

    @Override
    public AuthoritativeWorldSnapshot mutate(GamePanel game, String reason, Runnable body) {
        if (body == null) return authoritativeWorldRuntime.latestSnapshot();
        if (!shouldOwnAuthoritativeTurns(game) || insideAuthoritativeTurn()) {
            body.run();
            return authoritativeWorldRuntime.latestSnapshot();
        }
        bindCurrentWorld(game, reason);
        SectorKey key = currentSector;
        if (key == null) {
            body.run();
            return authoritativeWorldRuntime.latestSnapshot();
        }
        insideAuthoritativeTurn.set(true);
        try {
            return authoritativeWorldRuntime.submitAndJoin(game, LOCAL_PLAYER_ID, key, reason, () -> sectorManager.runLocalAuthoritativeTurn(LOCAL_PLAYER_ID, key, reason, () -> {
                body.run();
                routedTurns++;
                lastReason = safe(reason);
            }));
        } finally {
            insideAuthoritativeTurn.set(false);
        }
    }

    String executeAdminConsoleCommand(GamePanel game, String rawInput) {
        return submitConsoleCommand(game, new ConsoleCommandRequest(LOCAL_PLAYER_ID, rawInput));
    }

    public String statusLine() {
        return "authority=" + VERSION
                + " player=" + LOCAL_PLAYER_ID
                + " current=" + (currentSector == null ? "none" : currentSector.compact())
                + " routedTurns=" + routedTurns
                + " insideTurn=" + insideAuthoritativeTurn.get()
                + " lastReason=" + lastReason
                + " lastRejected=" + lastRejectedCommand
                + " lastRenderedSnapshot=" + lastRenderedSnapshotVersion.get()
                + " session={" + (sessionAuthority == null ? "none" : sessionAuthority.statusLine()) + "}"
                + " worldRuntime={" + authoritativeWorldRuntime.statusLine() + "}"
                + " turnManager={" + worldTurnManager.statusLine(currentSector == null ? "local-world" : currentSector.compact()) + "}"
                + " actionRegistry={" + actionRegistry.statusLine(LOCAL_PLAYER_ID) + "}"
                + " manager={" + sectorManager.statusLine() + "}";
    }

    static String auditSummary() {
        return "authority=" + VERSION + " mode=desktop-single-player-default manager=" + SectorManager.VERSION + " mutationThread=authoritative-world-single-writer snapshots=immutable-world-snapshot console=console-command-request";
    }

    static SectorKey keyFor(GamePanel game) {
        if (game == null || game.world == null) return null;
        int sx = game.atlas != null ? game.atlas.sectorX : game.world.sectorX;
        int sy = game.atlas != null ? game.atlas.sectorY : game.world.sectorY;
        int floor = game.atlas != null ? game.atlas.floor : game.world.floor;
        int zx = game.atlas != null ? game.atlas.zoneX : game.world.zoneX;
        int zy = game.atlas != null ? game.atlas.zoneY : game.world.zoneY;
        boolean sewer = game.atlas != null ? game.atlas.sewer : game.world.sewerLayer;
        String zoneType = game.world.zoneType == null ? "zone" : game.world.zoneType.name().toLowerCase(java.util.Locale.ROOT);
        String zoneId = "z" + zx + "_" + zy + "_" + (sewer ? "sewer" : "floor") + "_" + zoneType;
        return SectorKey.of(1, sx, sy, floor, zoneId);
    }

    AuthoritativeWorldSnapshot latestSnapshot() { return authoritativeWorldRuntime.latestSnapshot(); }
    WorldSnapshot latestWorldSnapshot() { return authoritativeWorldRuntime.latestWorldSnapshot(); }

    void noteSnapshotRendered(String surface) {
        WorldSnapshot snapshot = latestWorldSnapshot();
        if (snapshot == null) return;
        long previous = lastRenderedSnapshotVersion.get();
        long current = snapshot.version();
        if (current < previous) {
            DebugLog.warn("AUTHORITATIVE_SNAPSHOT_RENDER_ORDER", "surface=" + safe(surface) + " current=" + current + " previous=" + previous);
            return;
        }
        lastRenderedSnapshotVersion.set(current);
    }

    WorldTurnManager worldTurnManager() { return worldTurnManager; }
    PlayerActionRegistry actionRegistry() { return actionRegistry; }
    InternalServerSessionAuthority sessionAuthority() { return sessionAuthority; }
    String activeActionDisplayLine() { return actionRegistry.activeActionLine(LOCAL_PLAYER_ID); }
    String activeActionCountdownOverlay() { return actionRegistry.activeActionCountdownOverlay(LOCAL_PLAYER_ID); }
    int activeActionTicksRemaining() { return actionRegistry.activeActionTicksRemaining(LOCAL_PLAYER_ID); }
    boolean localPlayerActionGated() { return actionRegistry.isGated(LOCAL_PLAYER_ID); }

    void queueLongActionForLocalPlayer(String actionName, int ticks, WorldCommandRequest completionCommand, String source) {
        actionRegistry.assignLongAction(LOCAL_PLAYER_ID, actionName, ticks, completionCommand, source);
    }

    boolean clearLocalLongAction(String reason) {
        return actionRegistry.cancelAction(LOCAL_PLAYER_ID, reason);
    }

    private static String safe(String s) {
        return s == null || s.isBlank() ? "unspecified" : s.replace('\n', ' ');
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        try {
            if (currentSector != null) sectorManager.playerLeftCurrentSector(LOCAL_PLAYER_ID);
        } catch (Throwable t) {
            DebugLog.error("SINGLE_PLAYER_SECTOR_CLOSE", "Could not unbind local player from current sector.", t);
        } finally {
            currentSector = null;
            try { authoritativeWorldRuntime.close(); } catch (Throwable t) { DebugLog.error("AUTHORITATIVE_WORLD_CLOSE", "Could not close authoritative world runtime.", t); }
            sectorManager.close();
            DebugLog.audit("SINGLE_PLAYER_SECTOR_CLOSE", statusLine());
        }
    }
}
