package mechanist;

import java.awt.Color;
import java.util.ArrayList;

/**
 * Shared source list for small intrinsic lights that are not inventory items or
 * generated zone fixtures. Gameplay light and render-only bloom both read this.
 */
final class WorldLightEmitterAuthority {
    private WorldLightEmitterAuthority() {}

    static final class Emitter {
        final String id;
        final int x;
        final int y;
        final int radius;
        final int intensity;
        final Color color;
        final boolean flicker;

        Emitter(String id, int x, int y, int radius, int intensity, Color color, boolean flicker) {
            this.id = id == null || id.isBlank() ? "intrinsic-light" : id;
            this.x = x;
            this.y = y;
            this.radius = Math.max(1, radius);
            this.intensity = Math.max(1, Math.min(100, intensity));
            this.color = color == null ? new Color(235, 198, 126) : color;
            this.flicker = flicker;
        }
    }

    static ArrayList<Emitter> gameplayEmitters(GamePanel game) {
        ArrayList<Emitter> out = new ArrayList<>();
        if (game == null || game.world == null) return out;
        addPlayerEmitter(game, out);
        addDoorEmitters(game, out, 0, 0, game.world.w, game.world.h, Integer.MAX_VALUE);
        return out;
    }

    static ArrayList<Emitter> viewportEmitters(GamePanel game, int camX, int camY, int cols, int rows) {
        ArrayList<Emitter> out = new ArrayList<>();
        if (game == null || game.world == null) return out;
        addPlayerEmitter(game, out);
        addDoorEmitters(game, out, camX, camY, cols, rows, 3);
        return out;
    }

    private static void addPlayerEmitter(GamePanel game, ArrayList<Emitter> out) {
        if (!game.world.inBounds(game.playerX, game.playerY)) return;
        int vision = game.stat("Vision", game.active == null ? 8 : game.active.stats.getOrDefault("Vision", 8));
        int radius = Math.max(2, Math.min(3, 2 + Math.max(0, vision - 10) / 8));
        int intensity = Math.max(12, Math.min(24, 15 + Math.max(0, vision - 8) / 2));
        out.add(new Emitter("player-presence-glow", game.playerX, game.playerY, radius, intensity, new Color(224, 206, 150), false));
    }

    private static void addDoorEmitters(GamePanel game, ArrayList<Emitter> out, int camX, int camY, int cols, int rows, int pad) {
        int minX = Math.max(0, camX - Math.max(0, pad));
        int minY = Math.max(0, camY - Math.max(0, pad));
        int maxX = Math.min(game.world.w - 1, camX + Math.max(0, cols) + Math.max(0, pad));
        int maxY = Math.min(game.world.h - 1, camY + Math.max(0, rows) + Math.max(0, pad));
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                char ch = game.world.tiles[x][y];
                if (!game.isDoorTile(ch)) continue;
                int intensity = ch == '+' ? 18 : 13;
                out.add(new Emitter("door-glow-" + x + "-" + y, x, y, 2, intensity, new Color(232, 176, 92), false));
            }
        }
    }
}
