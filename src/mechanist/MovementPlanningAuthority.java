package mechanist;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Movement planning rules for the game panel. Keeps pathfinding and movement
 * mode numbers out of the Swing surface.
 */
final class MovementPlanningAuthority {
    private MovementPlanningAuthority() {}

    record MovementPlanReadout(boolean reachable, boolean exact, String summary) { }
    record HazardRouteReadout(boolean hazardous, int hazardousTiles, int highestSeverity, String summary) { }
    record OccupiedTileRoutingReadout(boolean hardBlocked, boolean pushSqueezeEligible, String debugSummary) { }
    record StandableTileSearchResult(boolean found, int x, int y, int radiusUsed, String summary) { }
    record MovementRecoveryApplicationResult(boolean applied, int fromX, int fromY, int toX, int toY, String summary) { }

    static ArrayList<Point> buildPathTo(GamePanel game, int targetX, int targetY, int maxSteps) {
        ArrayList<Point> out = new ArrayList<>();
        if (game == null || game.world == null || !game.world.inBounds(game.playerX, game.playerY) || maxSteps <= 0) return out;
        int tx = Math.max(0, Math.min(game.world.w - 1, targetX));
        int ty = Math.max(0, Math.min(game.world.h - 1, targetY));
        if (tx == game.playerX && ty == game.playerY) return out;
        boolean[][] seen = new boolean[game.world.w][game.world.h];
        int[][] prevX = new int[game.world.w][game.world.h];
        int[][] prevY = new int[game.world.w][game.world.h];
        int[][] dist = new int[game.world.w][game.world.h];
        for (int x = 0; x < game.world.w; x++) {
            Arrays.fill(prevX[x], -1);
            Arrays.fill(prevY[x], -1);
            Arrays.fill(dist[x], -1);
        }
        ArrayDeque<Point> q = new ArrayDeque<>();
        q.add(new Point(game.playerX, game.playerY));
        seen[game.playerX][game.playerY] = true;
        dist[game.playerX][game.playerY] = 0;
        int bestX = game.playerX;
        int bestY = game.playerY;
        long bestScore = targetScore(game.playerX, game.playerY, tx, ty);
        int bestDist = 0;
        int[][] dirs = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
        while (!q.isEmpty()) {
            Point p = q.removeFirst();
            int nextDist = dist[p.x][p.y] + 1;
            if (nextDist > maxSteps) continue;
            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];
                if (!canEnter(game, nx, ny) || seen[nx][ny]) continue;
                seen[nx][ny] = true;
                prevX[nx][ny] = p.x;
                prevY[nx][ny] = p.y;
                dist[nx][ny] = nextDist;
                long score = targetScore(nx, ny, tx, ty);
                if (score < bestScore || (score == bestScore && nextDist > bestDist)) {
                    bestScore = score;
                    bestDist = nextDist;
                    bestX = nx;
                    bestY = ny;
                }
                if (nx == tx && ny == ty) {
                    bestX = nx;
                    bestY = ny;
                    q.clear();
                    break;
                }
                q.addLast(new Point(nx, ny));
            }
        }
        if (bestX == game.playerX && bestY == game.playerY) return out;
        ArrayList<Point> reversed = new ArrayList<>();
        int cx = bestX;
        int cy = bestY;
        while (!(cx == game.playerX && cy == game.playerY)) {
            reversed.add(new Point(cx, cy));
            int px = prevX[cx][cy];
            int py = prevY[cx][cy];
            if (px < 0 || py < 0) break;
            cx = px;
            cy = py;
        }
        for (int i = reversed.size() - 1; i >= 0; i--) out.add(reversed.get(i));
        return out;
    }

    static boolean[][] reachableTiles(GamePanel game, int maxSteps) {
        if (game == null || game.world == null || !game.world.inBounds(game.playerX, game.playerY) || maxSteps <= 0) return null;
        boolean[][] seen = new boolean[game.world.w][game.world.h];
        int[][] dist = new int[game.world.w][game.world.h];
        for (int x = 0; x < game.world.w; x++) Arrays.fill(dist[x], -1);
        ArrayDeque<Point> q = new ArrayDeque<>();
        q.add(new Point(game.playerX, game.playerY));
        seen[game.playerX][game.playerY] = true;
        dist[game.playerX][game.playerY] = 0;
        int[][] dirs = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
        while (!q.isEmpty()) {
            Point p = q.removeFirst();
            if (dist[p.x][p.y] >= maxSteps) continue;
            for (int[] d : dirs) {
                int nx = p.x + d[0];
                int ny = p.y + d[1];
                if (!canEnter(game, nx, ny) || seen[nx][ny]) continue;
                seen[nx][ny] = true;
                dist[nx][ny] = dist[p.x][p.y] + 1;
                q.addLast(new Point(nx, ny));
            }
        }
        return seen;
    }

    static boolean canEnter(GamePanel game, int x, int y) {
        return game != null && game.world != null && game.world.inBounds(x, y) && game.world.walkable(x, y)
                && ((x == game.playerX && y == game.playerY) || game.world.npcAt(x, y) == null);
    }

    static OccupiedTileRoutingReadout occupiedTileRoutingForPlanning(boolean inBounds, boolean walkable, boolean occupied, boolean actorLayerPushSqueezeAvailable) {
        if (!inBounds) return new OccupiedTileRoutingReadout(true, false, "Destination is outside the current area.");
        if (!walkable) return new OccupiedTileRoutingReadout(true, false, "Destination is not walkable.");
        if (!occupied) return new OccupiedTileRoutingReadout(false, false, "Destination is open.");
        if (actorLayerPushSqueezeAvailable) {
            return new OccupiedTileRoutingReadout(false, true, "Destination occupied; route through actor-layer push/squeeze resolver before final movement commit.");
        }
        return new OccupiedTileRoutingReadout(true, false, "Destination occupied and no actor-layer push/squeeze resolver is available.");
    }

    static boolean canEnterForMovementCommit(boolean inBounds, boolean walkable, boolean occupied, boolean actorLayerPushSqueezeAvailable) {
        OccupiedTileRoutingReadout readout = occupiedTileRoutingForPlanning(inBounds, walkable, occupied, actorLayerPushSqueezeAvailable);
        return !readout.hardBlocked();
    }

    static StandableTileSearchResult nearestStandableTile(ZoneTileState[][] tiles, int startX, int startY, int startRadius, int maxRadius) {
        if (tiles == null || tiles.length == 0) {
            return new StandableTileSearchResult(false, startX, startY, 0, "No tile grid is available.");
        }
        if (isStandableWithExit(tiles, startX, startY)) {
            return new StandableTileSearchResult(false, startX, startY, 0, "Current tile is already standable and has an exit.");
        }
        int first = Math.max(1, startRadius);
        int last = Math.max(first, maxRadius);
        for (int radius = first; radius <= last; radius++) {
            ArrayList<Point> candidates = standableCandidatesAtRadius(tiles, startX, startY, radius);
            if (!candidates.isEmpty()) {
                Point selected = candidates.get(0);
                return new StandableTileSearchResult(true, selected.x, selected.y, radius,
                        "Nearest standable tile selected at " + selected.x + "," + selected.y + " using radius " + radius + ".");
            }
        }
        return new StandableTileSearchResult(false, startX, startY, last, "No standable tile found in search radius.");
    }

    static MovementRecoveryApplicationResult applyNearestStandableRecovery(GamePanel game, int startRadius, int maxRadius) {
        if (game == null || game.world == null) {
            return new MovementRecoveryApplicationResult(false, 0, 0, 0, 0, "No world is loaded for movement recovery.");
        }
        int fromX = game.playerX;
        int fromY = game.playerY;
        ZoneTileState[][] snapshot = legacyWorldTileStateSnapshot(game);
        StandableTileSearchResult search = nearestStandableTile(snapshot, fromX, fromY, startRadius, maxRadius);
        if (!search.found()) {
            String message = "Movement recovery did not move the player: " + search.summary();
            game.lastTargetingReport = message;
            game.logEvent(message);
            return new MovementRecoveryApplicationResult(false, fromX, fromY, fromX, fromY, message);
        }
        game.playerX = search.x();
        game.playerY = search.y();
        game.playerMotionFromX = search.x();
        game.playerMotionFromY = search.y();
        game.playerMotionToX = search.x();
        game.playerMotionToY = search.y();
        game.playerMotionDurationMillis = 0;
        game.playerMotionStartedMillis = 0L;
        game.manualMovementPlanActive = false;
        game.manualMovementPlanHazardous = false;
        game.mouseMovePreviewActive = false;
        game.mouseMovePreviewHazardous = false;
        game.lookCursorActive = false;
        game.interactCursorActive = false;
        game.combatCursorActive = false;
        game.manualMovementPlanPath.clear();
        game.mouseMovePreviewPath.clear();
        if (game.world.inBounds(game.playerX, game.playerY)) {
            game.lookX = game.playerX;
            game.lookY = game.playerY;
            game.combatX = game.playerX;
            game.combatY = game.playerY;
        }
        String message = "Movement recovery moved the player from " + fromX + "," + fromY + " to " + search.x() + "," + search.y() + ".";
        game.lastTargetingReport = message;
        game.logEvent(message);
        game.markZoneVisitedAndCheckFirstType();
        game.markLocalDirtyRegion("pause movement recovery", game.playerX, game.playerY,
                Math.max(6, game.visionRange() + 2), true, true, true, false);
        game.updateSensoryModel("pause movement recovery");
        ProgressiveLookAuthority.reset(game, "pause movement recovery");
        return new MovementRecoveryApplicationResult(true, fromX, fromY, search.x(), search.y(), message);
    }

    static boolean standableZoneTile(ZoneTileState[][] tiles, int x, int y) {
        return inZoneTileBounds(tiles, x, y)
                && tiles[x][y] != null
                && !tiles[x][y].hasFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT)
                && !tiles[x][y].hasSlot(ZoneTileState.TileSlot.ENTITY)
                && !tiles[x][y].hasSlot(ZoneTileState.TileSlot.PET)
                && !tiles[x][y].hasSlot(ZoneTileState.TileSlot.VEHICLE);
    }

    static String movementRecoveryAuditSummary() {
        return "movementRecoverySearch=nearestStandableTile search=expandingRadius requires=walkable+unoccupied+adjacentExit mutation=callerAppliesResult runtimeBridge=applyNearestStandableRecovery";
    }

    static boolean pathReaches(java.util.List<Point> path, int targetX, int targetY) {
        if (path == null || path.isEmpty()) return false;
        Point end = path.get(path.size() - 1);
        return end != null && end.x == targetX && end.y == targetY;
    }

    static MovementPlanReadout describePlan(GamePanel game, int targetX, int targetY, int maxSteps, String modeLabel) {
        if (game == null || game.world == null) {
            return new MovementPlanReadout(false, false, PlayerFacingText.denial(PlayerFacingDenialText.Context.MOVEMENT, "No world is loaded."));
        }
        boolean inBounds = game.world.inBounds(targetX, targetY);
        boolean walkable = inBounds && game.world.walkable(targetX, targetY);
        boolean occupied = inBounds && game.world.npcAt(targetX, targetY) != null
                && !(targetX == game.playerX && targetY == game.playerY);
        ArrayList<Point> path = buildPathTo(game, targetX, targetY, maxSteps);
        MovementPlanReadout plan = describePlan(modeLabel, maxSteps, targetX, targetY, path, inBounds, walkable, occupied);
        if (!plan.reachable()) return plan;
        HazardRouteReadout hazards = inspectRouteHazards(game.world, path);
        if (!hazards.hazardous()) return plan;
        return new MovementPlanReadout(plan.reachable(), plan.exact(), plan.summary() + " " + hazards.summary());
    }

    static HazardRouteReadout inspectRouteHazards(World world, List<Point> path) {
        if (world == null || world.hazardWarnings == null || world.hazardWarnings.isEmpty() || path == null || path.isEmpty()) {
            return new HazardRouteReadout(false, 0, 0, "No recorded hazards cross this route.");
        }
        int tiles = 0;
        int highest = 0;
        String highestLabel = "hazard";
        for (Point point : path) {
            if (point == null) continue;
            EnvironmentalHazardRecord strongest = null;
            for (EnvironmentalHazardRecord hazard : world.hazardWarnings) {
                if (hazard == null || hazard.x != point.x || hazard.y != point.y) continue;
                if (strongest == null || hazard.severity > strongest.severity) strongest = hazard;
            }
            if (strongest == null) continue;
            tiles++;
            if (strongest.severity > highest) {
                highest = strongest.severity;
                highestLabel = strongest.label == null || strongest.label.isBlank() ? "hazard" : strongest.label;
            }
        }
        if (tiles == 0) return new HazardRouteReadout(false, 0, 0, "No recorded hazards cross this route.");
        String severity = highest >= 75 ? "extreme" : highest >= 50 ? "severe" : highest >= 30 ? "dangerous" : "suspect";
        String summary = "Hazard warning: route crosses " + tiles + " hazardous tile" + (tiles == 1 ? "" : "s")
                + "; highest concern is " + PlayerFacingText.sanitize(highestLabel) + " (" + severity
                + "). Movement remains available; confirm only if intended.";
        return new HazardRouteReadout(true, tiles, highest, summary);
    }

    static boolean requiresHazardConfirmation(World world, int x, int y) {
        return inspectRouteHazards(world, List.of(new Point(x, y))).hazardous();
    }

    static MovementPlanReadout describePlan(String modeLabel, int maxSteps, int targetX, int targetY,
                                            List<Point> path, boolean targetInBounds,
                                            boolean targetWalkable, boolean targetOccupied) {
        return describePlan(modeLabel, maxSteps, targetX, targetY, path, targetInBounds, targetWalkable, targetOccupied, false);
    }

    static MovementPlanReadout describePlan(String modeLabel, int maxSteps, int targetX, int targetY,
                                            List<Point> path, boolean targetInBounds,
                                            boolean targetWalkable, boolean targetOccupied,
                                            boolean actorLayerPushSqueezeAvailable) {
        String mode = modeLabel == null || modeLabel.isBlank() ? "Movement" : PlayerFacingText.sanitize(modeLabel);
        int range = Math.max(0, maxSteps);
        OccupiedTileRoutingReadout occupiedRouting = occupiedTileRoutingForPlanning(targetInBounds, targetWalkable, targetOccupied, actorLayerPushSqueezeAvailable);
        if (occupiedRouting.hardBlocked()) {
            if (!targetInBounds) return denied("Destination is outside the current area.");
            if (!targetWalkable) return denied("Path blocked.");
            return denied("Destination occupied.");
        }
        if (targetOccupied && occupiedRouting.pushSqueezeEligible()) {
            return new MovementPlanReadout(true, true, PlayerFacingText.actionTravel(
                    "Movement target selected",
                    mode + " route targets tile " + targetX + "," + targetY
                            + "; occupied tile will be resolved by shove/squeeze before final movement commit."
            ));
        }
        if (path == null || path.isEmpty()) {
            return denied("Cannot reach from here.");
        }
        Point end = path.get(path.size() - 1);
        boolean exact = end != null && end.x == targetX && end.y == targetY;
        int steps = path.size();
        if (!exact) {
            return new MovementPlanReadout(true, false, PlayerFacingText.inspectionRoute(
                    "Partial route",
                    mode + " can move " + steps + " step(s) toward the target, ending near " + readableCoord(end)
                            + ". Too far for one movement chain; range " + range + "."
            ));
        }
        return new MovementPlanReadout(true, true, PlayerFacingText.actionTravel(
                "Movement target selected",
                mode + " route reaches " + readableCoord(end) + " in " + steps + " step(s); range " + range + "."
        ));
    }

    static int rangeForMode(int mode) {
        return switch (mode) {
            case GamePanel.MOTION_RUN -> 4;
            case GamePanel.MOTION_SPRINT -> 7;
            case GamePanel.MOTION_SNEAK, GamePanel.MOTION_WALK -> 1;
            default -> 1;
        };
    }

    static int fatigueCost(int mode, int steps) {
        int s = Math.max(1, steps);
        return switch (mode) {
            case GamePanel.MOTION_SPRINT -> Math.max(2, (s + 1) / 2);
            case GamePanel.MOTION_RUN -> Math.max(1, s / 3);
            case GamePanel.MOTION_SNEAK -> s > 0 ? 1 : 0;
            default -> 0;
        };
    }

    static Color modeColor(int mode, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return switch (mode) {
            case GamePanel.MOTION_SNEAK -> new Color(122, 205, 156, a);
            case GamePanel.MOTION_RUN -> new Color(245, 184, 76, a);
            case GamePanel.MOTION_SPRINT -> new Color(236, 92, 82, a);
            case GamePanel.MOTION_WALK -> new Color(118, 214, 224, a);
            default -> new Color(118, 214, 224, a);
        };
    }

    private static ZoneTileState[][] legacyWorldTileStateSnapshot(GamePanel game) {
        if (game == null || game.world == null || game.world.w <= 0 || game.world.h <= 0) return new ZoneTileState[0][0];
        ZoneTileState[][] tiles = new ZoneTileState[game.world.w][game.world.h];
        for (int x = 0; x < game.world.w; x++) {
            for (int y = 0; y < game.world.h; y++) {
                char glyph = ' ';
                if (game.world.tiles != null && x < game.world.tiles.length && game.world.tiles[x] != null && y < game.world.tiles[x].length) {
                    glyph = game.world.tiles[x][y];
                }
                ZoneTileState state = ZoneTileState.fromLegacyGlyph(glyph);
                if (!game.world.inBounds(x, y) || !game.world.walkable(x, y)) state.addFlag(ZoneTileState.TileFlag.BLOCKS_MOVEMENT);
                tiles[x][y] = state;
            }
        }
        if (game.world.npcs != null) {
            int i = 0;
            for (NpcEntity npc : game.world.npcs) {
                if (npc != null && game.world.inBounds(npc.x, npc.y) && !(npc.x == game.playerX && npc.y == game.playerY)) {
                    tiles[npc.x][npc.y].setOccupantEntityId("npc-" + i + "-" + npc.x + "-" + npc.y);
                }
                i++;
            }
        }
        return tiles;
    }

    private static ArrayList<Point> standableCandidatesAtRadius(ZoneTileState[][] tiles, int sx, int sy, int radius) {
        ArrayList<Point> candidates = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                int x = sx + dx;
                int y = sy + dy;
                if (isStandableWithExit(tiles, x, y)) candidates.add(new Point(x, y));
            }
        }
        candidates.sort(Comparator
                .comparingLong((Point p) -> targetScore(p.x, p.y, sx, sy))
                .thenComparingInt(p -> stableCoordinateTieBreak(sx, sy, p.x, p.y))
                .thenComparingInt(p -> p.x)
                .thenComparingInt(p -> p.y));
        return candidates;
    }

    private static boolean isStandableWithExit(ZoneTileState[][] tiles, int x, int y) {
        if (!standableZoneTile(tiles, x, y)) return false;
        int[][] dirs = { {1, 0}, {-1, 0}, {0, 1}, {0, -1} };
        for (int[] d : dirs) if (standableZoneTile(tiles, x + d[0], y + d[1])) return true;
        return false;
    }

    private static boolean inZoneTileBounds(ZoneTileState[][] tiles, int x, int y) {
        return x >= 0 && y >= 0 && tiles != null && x < tiles.length && tiles[x] != null && y < tiles[x].length;
    }

    private static int stableCoordinateTieBreak(int sx, int sy, int x, int y) {
        int h = 17;
        h = 31 * h + sx;
        h = 31 * h + sy;
        h = 31 * h + x;
        h = 31 * h + y;
        return h & 0x7fffffff;
    }

    private static long targetScore(int x, int y, int tx, int ty) {
        long dx = x - tx;
        long dy = y - ty;
        return dx * dx + dy * dy;
    }

    private static MovementPlanReadout denied(String reason) {
        return new MovementPlanReadout(false, false, PlayerFacingText.denial(PlayerFacingDenialText.Context.MOVEMENT, reason));
    }

    private static String readableCoord(Point point) {
        if (point == null) return "the current position";
        return "tile " + point.x + "," + point.y;
    }
}
