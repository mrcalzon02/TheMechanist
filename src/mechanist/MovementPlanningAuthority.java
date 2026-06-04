package mechanist;

import java.awt.Color;
import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Movement planning rules for the game panel. Keeps pathfinding and movement
 * mode numbers out of the Swing surface.
 */
final class MovementPlanningAuthority {
    private MovementPlanningAuthority() {}

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

    static boolean pathReaches(java.util.List<Point> path, int targetX, int targetY) {
        if (path == null || path.isEmpty()) return false;
        Point end = path.get(path.size() - 1);
        return end != null && end.x == targetX && end.y == targetY;
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

    private static long targetScore(int x, int y, int tx, int ty) {
        long dx = x - tx;
        long dy = y - ty;
        return dx * dx + dy * dy;
    }
}
