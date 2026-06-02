package mechanist;

import java.awt.*;
import java.util.*;

/**
 * 0.9.09d construction access safety authority.
 * Prevents the player from cask-of-Amontillado-ing themselves into a claimed room
 * by placing hard defenses or major machines across the last reachable door/exit path.
 *
 * This authority is deliberately called only during placement checks/confirmation.
 * It does not run during rendering, turn advancement, or background simulation.
 */
final class SelfEntombmentConstructionAuthority {
    static final String VERSION = "0.9.09f";

    private SelfEntombmentConstructionAuthority() {}

    static String validate(GamePanel g, BuildRecipe recipe, int proposedX, int proposedY) {
        if (g == null || g.world == null || recipe == null) return "OK";
        if (!placementBlocksAccess(recipe)) return "OK";
        if (!g.baseClaimed || g.claimedRoomId < 0) return "OK";
        if (!g.isInClaimedRoom(proposedX, proposedY)) return "OK";
        if (!g.isInClaimedRoom(g.playerX, g.playerY)) return "OK";
        if (proposedX == g.playerX && proposedY == g.playerY) return "Move off the target tile before placing the object.";

        Result result = hasReachableExitAfterPlacement(g, proposedX, proposedY);
        if (result.ok) return "OK";
        return "No-self-entombment rule: placing " + recipe.name + " there would leave no valid access path from your position to a room door/exit. Move it away from the only route; the underhive already has enough tombs.";
    }

    static boolean placementBlocksAccess(BuildRecipe recipe) {
        if (recipe == null) return false;
        char s = recipe.symbol;
        if ("dDSRWNYYJUZTHeeflLMBwkqGgpPa".indexOf(s) >= 0) return true;
        String name = recipe.name == null ? "" : recipe.name.toLowerCase(Locale.ROOT);
        return name.contains("wall") || name.contains("barricade") || name.contains("sandbag") || name.contains("wire")
                || name.contains("door") || name.contains("turret") || name.contains("forge") || name.contains("lab")
                || name.contains("workbench") || name.contains("condenser") || name.contains("machine") || name.contains("post")
                || name.contains("barracks") || name.contains("relay") || name.contains("sensor") || name.contains("counter")
                || name.contains("center") || name.contains("stall");
    }

    static boolean objectBlocksAccess(BaseObject obj) {
        if (obj == null) return false;
        char s = obj.symbol;
        if ("dDSRWNYYJUZTHeeflLMBwkqGgpPa".indexOf(s) >= 0) return true;
        String name = obj.name == null ? "" : obj.name.toLowerCase(Locale.ROOT);
        return name.contains("wall") || name.contains("barricade") || name.contains("sandbag") || name.contains("wire")
                || name.contains("door") || name.contains("turret") || name.contains("forge") || name.contains("lab")
                || name.contains("workbench") || name.contains("condenser") || name.contains("machine") || name.contains("post")
                || name.contains("barracks") || name.contains("relay") || name.contains("sensor") || name.contains("counter")
                || name.contains("center") || name.contains("stall");
    }

    private static Result hasReachableExitAfterPlacement(GamePanel g, int blockX, int blockY) {
        Rectangle rr = g.world.roomRect(g.claimedRoomId);
        if (rr == null) return Result.ok("no claimed room rectangle");
        int w = g.world.w, h = g.world.h;
        boolean[][] seen = new boolean[w][h];
        ArrayDeque<Point> q = new ArrayDeque<>();
        q.add(new Point(g.playerX, g.playerY));
        seen[g.playerX][g.playerY] = true;
        int visited = 0;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!q.isEmpty()) {
            Point p = q.removeFirst();
            visited++;
            if (isRoomExitAnchor(g, p.x, p.y)) return Result.ok("reachable exit after " + visited + " tiles");
            for (int[] d : dirs) {
                int nx = p.x + d[0], ny = p.y + d[1];
                if (nx < 0 || ny < 0 || nx >= w || ny >= h || seen[nx][ny]) continue;
                if (!g.isInClaimedRoom(nx, ny)) continue;
                if (!tilePassableForAccess(g, nx, ny, blockX, blockY)) continue;
                seen[nx][ny] = true;
                q.add(new Point(nx, ny));
            }
        }
        return Result.blocked("visited=" + visited);
    }

    private static boolean tilePassableForAccess(GamePanel g, int x, int y, int blockX, int blockY) {
        if (x == blockX && y == blockY) return false;
        if (!g.world.walkable(x, y)) return false;
        BaseObject obj = g.baseObjectAt(x, y);
        return obj == null || !objectBlocksAccess(obj);
    }

    private static boolean isRoomExitAnchor(GamePanel g, int x, int y) {
        if (g == null || g.world == null || !g.world.inBounds(x,y)) return false;
        char here = g.world.tiles[x][y];
        if (isUsableDoorGlyph(here)) return true;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (!g.world.inBounds(nx, ny)) continue;
            char n = g.world.tiles[nx][ny];
            if (isUsableDoorGlyph(n)) return true;
            if (g.world.roomIdAt(nx, ny) != g.claimedRoomId && g.world.walkable(nx, ny)) return true;
        }
        return false;
    }

    private static boolean isUsableDoorGlyph(char ch) {
        // Locked/sealed doors are still access anchors because they are intentional door infrastructure.
        return ch == '/' || ch == '|' || ch == 'L' || ch == 'X' || ch == 'V';
    }

    static String auditSummary() {
        return "selfEntombment version=" + VERSION + " validation=placement-only bfs=claimed-room door-path blockerSymbols=defenses+major-machines";
    }

    private static final class Result {
        final boolean ok;
        final String note;
        private Result(boolean ok, String note) { this.ok = ok; this.note = note; }
        static Result ok(String note) { return new Result(true, note); }
        static Result blocked(String note) { return new Result(false, note); }
    }
}
