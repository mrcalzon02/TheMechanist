package mechanist;

/**
 * Owns player-facing vision geometry. The visible footprint is an off-centered
 * forward oblong: the narrow end sits on the character and the long body extends
 * in the current facing direction.
 */
final class DirectionalVisionAuthority {
    private DirectionalVisionAuthority() {}

    static int range(GamePanel game) {
        if (game == null) return 6;
        int baseVision = game.active == null ? 8 : game.active.stats.getOrDefault("Vision", 8);
        int statBoost = Math.max(0, game.stat("Vision", baseVision)) / 3;
        int lightBoost = game.activePortableLightRadius() > 0 ? 2 : 0;
        return Math.max(4, Math.min(14, 6 + statBoost + lightBoost));
    }

    static boolean contains(GamePanel game, int x, int y, int range) {
        if (game == null) return false;
        return containsAt(game, x, y, range, game.playerX, game.playerY);
    }

    static boolean containsAt(GamePanel game, int x, int y, int range, double originX, double originY) {
        if (game == null) return false;
        double dx = x - originX;
        double dy = y - originY;
        if (Math.hypot(dx, dy) <= 0.35) return true;
        double distance = Math.hypot(dx, dy);
        if (distance <= 1.05) return true;

        int fdx = game.facingDx == 0 && game.facingDy == 0 ? 1 : game.facingDx;
        int fdy = game.facingDx == 0 && game.facingDy == 0 ? 0 : game.facingDy;
        double forward = dx * fdx + dy * fdy;
        double lateral = Math.abs(dx * -fdy + dy * fdx);

        double backCap = Math.max(1.25, Math.min(2.25, range * 0.18));
        if (forward < -backCap || forward > range + 0.25) return false;

        double semiMajor = (range + backCap) / 2.0;
        double center = (range - backCap) / 2.0;
        double semiMinor = Math.max(1.55, range * 0.44);
        double nx = (forward - center) / Math.max(0.001, semiMajor);
        double ny = lateral / Math.max(0.001, semiMinor);
        return nx * nx + ny * ny <= 1.0;
    }

    static String summary(GamePanel game) {
        int range = range(game);
        int width = Math.max(3, (int)Math.round(range * 0.88));
        return "forward-oblong range=" + range + " width~" + width + " facing=" + (game == null ? "E" : game.facingLabel());
    }
}
