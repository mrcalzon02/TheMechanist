package mechanist;

import java.util.ArrayList;
import java.util.List;

/**
 * Unified one-step movement execution bridge.
 *
 * This authority exists to stop preview, planning, and final movement commit from
 * drifting into separate rule sets. It can resolve against a structured
 * ZoneTileState grid for smokes and can bridge the current legacy GamePanel world
 * into that actor-layer resolver for runtime movement.
 */
final class MovementExecutionAuthority {
    static final String VERSION = "0.9.10kh";
    static final String PLAYER_ACTOR_ID = "player";

    private MovementExecutionAuthority() { }

    record MovementExecutionResult(boolean success, boolean applied, int fromX, int fromY, int toX, int toY,
                                   String reason, String debugSummary) { }

    static MovementExecutionResult resolveActorStepOnTiles(ZoneTileState[][] tiles, String actorId,
                                                           int fromX, int fromY, int toX, int toY,
                                                           boolean allowActorResolver) {
        String safeActor = actorId == null || actorId.isBlank() ? PLAYER_ACTOR_ID : actorId.trim();
        if (tiles == null || tiles.length == 0) return failed(false, fromX, fromY, fromX, fromY, "No tile grid is available.", "missing-grid");
        boolean inBounds = inBounds(tiles, toX, toY);
        boolean walkable = inBounds && tiles[toX][toY] != null && !tiles[toX][toY].hasFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT);
        boolean occupied = inBounds && tiles[toX][toY] != null && tiles[toX][toY].hasSlot(ZoneTileState.TileSlot.ENTITY)
                && !(fromX == toX && fromY == toY);
        MovementPlanningAuthority.OccupiedTileRoutingReadout routing = MovementPlanningAuthority.occupiedTileRoutingForPlanning(inBounds, walkable, occupied, allowActorResolver);
        if (routing.hardBlocked()) return failed(false, fromX, fromY, fromX, fromY, routing.debugSummary(), "planning-hard-block");
        if (occupied && !allowActorResolver) return failed(false, fromX, fromY, fromX, fromY, "Destination occupied and actor resolver is disabled.", "resolver-disabled");

        ZoneTileMovementResolutionAuthority.Resolution resolution = ZoneTileMovementResolutionAuthority.resolve(
                tiles,
                List.of(new ZoneTileMovementResolutionAuthority.MoveIntent(safeActor, fromX, fromY, toX, toY, 100))
        );
        ZoneTileMovementResolutionAuthority.MoveOutcome actorOutcome = resolution.outcomeFor(safeActor);
        if (actorOutcome == null) {
            return failed(false, fromX, fromY, fromX, fromY, "Movement resolver returned no outcome for " + safeActor + ".", resolution.summary());
        }
        if (!actorOutcome.moved()) {
            return failed(false, fromX, fromY, actorOutcome.finalX(), actorOutcome.finalY(), actorOutcome.reason(), resolution.summary());
        }
        return new MovementExecutionResult(true, false, fromX, fromY, actorOutcome.finalX(), actorOutcome.finalY(), actorOutcome.reason(), resolution.summary());
    }

    static MovementExecutionResult executeStep(GamePanel game, int dx, int dy, String source, boolean allowActorResolver) {
        if (game == null || game.world == null) return failed(false, 0, 0, 0, 0, "No world is loaded.", "missing-world");
        int fromX = game.playerX;
        int fromY = game.playerY;
        int toX = fromX + dx;
        int toY = fromY + dy;
        if (dx == 0 && dy == 0) return failed(false, fromX, fromY, fromX, fromY, "No movement direction supplied.", "zero-delta");

        ZoneTileState[][] snapshot = legacyWorldTileStateSnapshot(game);
        MovementExecutionResult preview = resolveActorStepOnTiles(snapshot, PLAYER_ACTOR_ID, fromX, fromY, toX, toY, allowActorResolver);
        if (!preview.success()) {
            String message = "Movement blocked: " + preview.reason();
            game.lastTargetingReport = message;
            game.logEvent(message);
            return new MovementExecutionResult(false, false, fromX, fromY, fromX, fromY, message, preview.debugSummary());
        }

        ZoneTileMovementResolutionAuthority.Resolution resolution = ZoneTileMovementResolutionAuthority.resolve(
                snapshot,
                List.of(new ZoneTileMovementResolutionAuthority.MoveIntent(PLAYER_ACTOR_ID, fromX, fromY, toX, toY, 100))
        );
        ZoneTileMovementResolutionAuthority.MoveOutcome playerOutcome = resolution.outcomeFor(PLAYER_ACTOR_ID);
        if (playerOutcome == null || !playerOutcome.moved()) {
            String message = playerOutcome == null ? "Movement resolver returned no player outcome." : playerOutcome.reason();
            game.lastTargetingReport = "Movement blocked: " + message;
            game.logEvent(game.lastTargetingReport);
            return new MovementExecutionResult(false, false, fromX, fromY, fromX, fromY, game.lastTargetingReport, resolution.summary());
        }

        applyNpcOutcomes(game, resolution.outcomes());
        applyPlayerOutcome(game, fromX, fromY, playerOutcome.finalX(), playerOutcome.finalY(), source);
        String message = "Movement executed through unified authority to " + playerOutcome.finalX() + "," + playerOutcome.finalY() + ".";
        game.lastTargetingReport = message;
        game.logEvent(message);
        return new MovementExecutionResult(true, true, fromX, fromY, playerOutcome.finalX(), playerOutcome.finalY(), message, resolution.summary());
    }

    static String auditSummary() {
        return "movementExecutionAuthority version=" + VERSION
                + " chain=planning+actorLayerResolver+singleCommit"
                + " applies=player+npcActorOutcomes"
                + " legacyGlyph=bridgeOnly";
    }

    private static void applyPlayerOutcome(GamePanel game, int fromX, int fromY, int toX, int toY, String source) {
        game.facingDx = Integer.compare(toX, fromX);
        game.facingDy = Integer.compare(toY, fromY);
        if (game.facingDx == 0 && game.facingDy == 0) game.facingDx = 1;
        game.playerMotionFromX = fromX;
        game.playerMotionFromY = fromY;
        game.playerMotionToX = toX;
        game.playerMotionToY = toY;
        game.playerMotionDurationMillis = 1;
        game.playerMotionStartedMillis = System.currentTimeMillis();
        game.playerX = toX;
        game.playerY = toY;
        game.lookX = toX;
        game.lookY = toY;
        game.combatX = toX;
        game.combatY = toY;
        game.lookCursorActive = false;
        game.interactCursorActive = false;
        game.manualMovementPlanActive = false;
        game.mouseMovePreviewActive = false;
        game.manualMovementPlanPath.clear();
        game.mouseMovePreviewPath.clear();
        game.advanceTurnBody(null);
        game.markZoneVisitedAndCheckFirstType();
        ProgressiveLookAuthority.reset(game, source == null || source.isBlank() ? "movement execution" : source);
    }

    private static void applyNpcOutcomes(GamePanel game, List<ZoneTileMovementResolutionAuthority.MoveOutcome> outcomes) {
        if (game == null || game.world == null || game.world.npcs == null || outcomes == null) return;
        for (ZoneTileMovementResolutionAuthority.MoveOutcome outcome : outcomes) {
            if (outcome == null || !outcome.moved() || outcome.actorId() == null || !outcome.actorId().startsWith("npc-")) continue;
            int idx = parseNpcIndex(outcome.actorId());
            if (idx >= 0 && idx < game.world.npcs.size()) {
                NpcEntity npc = game.world.npcs.get(idx);
                if (npc != null) {
                    npc.x = outcome.finalX();
                    npc.y = outcome.finalY();
                }
            }
        }
    }

    private static ZoneTileState[][] legacyWorldTileStateSnapshot(GamePanel game) {
        if (game == null || game.world == null || game.world.w <= 0 || game.world.h <= 0) return new ZoneTileState[0][0];
        ZoneTileState[][] tiles = new ZoneTileState[game.world.w][game.world.h];
        for (int x = 0; x < game.world.w; x++) {
            for (int y = 0; y < game.world.h; y++) {
                char glyph = ' ';
                if (game.world.tiles != null && x < game.world.tiles.length && game.world.tiles[x] != null && y < game.world.tiles[x].length) glyph = game.world.tiles[x][y];
                ZoneTileState state = ZoneTileState.fromLegacyGlyph(glyph);
                if (!game.world.inBounds(x, y) || !game.world.walkable(x, y)) state.addFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT);
                tiles[x][y] = state;
            }
        }
        if (game.world.inBounds(game.playerX, game.playerY)) tiles[game.playerX][game.playerY].setOccupantEntityId(PLAYER_ACTOR_ID);
        if (game.world.npcs != null) {
            for (int i = 0; i < game.world.npcs.size(); i++) {
                NpcEntity npc = game.world.npcs.get(i);
                if (npc != null && game.world.inBounds(npc.x, npc.y) && !(npc.x == game.playerX && npc.y == game.playerY)) {
                    tiles[npc.x][npc.y].setOccupantEntityId("npc-" + i);
                }
            }
        }
        return tiles;
    }

    private static boolean inBounds(ZoneTileState[][] tiles, int x, int y) {
        return x >= 0 && y >= 0 && tiles != null && x < tiles.length && tiles[x] != null && y < tiles[x].length;
    }

    private static int parseNpcIndex(String actorId) {
        if (actorId == null || !actorId.startsWith("npc-")) return -1;
        int start = 4;
        int end = start;
        while (end < actorId.length() && Character.isDigit(actorId.charAt(end))) end++;
        if (end == start) return -1;
        try { return Integer.parseInt(actorId.substring(start, end)); }
        catch (RuntimeException ex) { return -1; }
    }

    private static MovementExecutionResult failed(boolean applied, int fromX, int fromY, int toX, int toY, String reason, String debug) {
        return new MovementExecutionResult(false, applied, fromX, fromY, toX, toY, PlayerFacingText.sanitize(reason), PlayerFacingText.sanitize(debug));
    }
}
