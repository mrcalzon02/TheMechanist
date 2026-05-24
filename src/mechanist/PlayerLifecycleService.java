package mechanist;

import java.awt.Point;

/** Server-side lifecycle authority for unconscious relocation, death, and later multiplayer respawn. */
final class PlayerLifecycleService {
    static final String VERSION = "player-lifecycle-service-0.9.10gm";

    enum DeathOutcome { SINGLE_PLAYER_GAME_OVER, SERVER_RESPAWN }
    enum RuntimeLifecycleMode { SINGLE_PLAYER, SERVER_WORLD }
    record DeathResolution(DeathOutcome outcome, Point respawnPoint, String summary, RuntimeLifecycleMode mode) { }

    DeathResolution resolveDeath(GamePanel game, String cause, String attacker, String weapon, String location) {
        return resolveDeath(game, RuntimeLifecycleMode.SINGLE_PLAYER, cause, attacker, weapon, location);
    }

    DeathResolution resolveDeath(GamePanel game, RuntimeLifecycleMode mode, String cause, String attacker, String weapon, String location) {
        RuntimeLifecycleMode useMode = mode == null ? RuntimeLifecycleMode.SINGLE_PLAYER : mode;
        if (useMode == RuntimeLifecycleMode.SERVER_WORLD) {
            Point respawn = resolveServerRespawnPoint(game);
            return new DeathResolution(DeathOutcome.SERVER_RESPAWN, respawn,
                    "server-world death resolves through owned bed/base first, then the compiled world-seed spawn", useMode);
        }
        return new DeathResolution(DeathOutcome.SINGLE_PLAYER_GAME_OVER, null,
                "single-player death resolves to the You Lost screen", useMode);
    }

    Point resolveServerRespawnPoint(GamePanel game) {
        if (game != null && game.baseClaimed && game.baseX >= 0 && game.baseY >= 0 && game.world != null && game.world.inBounds(game.baseX, game.baseY) && game.world.walkable(game.baseX, game.baseY)) {
            return new Point(game.baseX, game.baseY);
        }
        return worldSeedSpawnPoint(game);
    }

    Point resolveUnconsciousWakePoint(GamePanel game) {
        if (game == null) return new Point(1, 1);
        if (game.baseClaimed && game.baseX >= 0 && game.baseY >= 0 && game.world != null && game.world.inBounds(game.baseX, game.baseY) && game.world.walkable(game.baseX, game.baseY)) {
            return new Point(game.baseX, game.baseY);
        }
        Point med = game.nearestMedicalFacilityPoint();
        if (med != null) return med;
        return worldSeedSpawnPoint(game);
    }

    Point worldSeedSpawnPoint(GamePanel game) {
        if (game != null && game.world != null) {
            Point sp = game.world.startPoint();
            return new Point(Math.max(1, Math.min(game.world.w - 2, sp.x)), Math.max(1, Math.min(game.world.h - 2, sp.y)));
        }
        return new Point(Math.max(1, game == null ? 1 : game.playerX), Math.max(1, game == null ? 1 : game.playerY));
    }

    String worldSpawnDescriptorSummary(GamePanel game) {
        Point p = worldSeedSpawnPoint(game);
        if (game == null || game.world == null || !game.world.inBounds(p.x, p.y)) return "worldSpawn=none";
        CompiledTileDescriptor d = TileDataCompilationAuthority.resolve(game.world, p.x, p.y, game.world.tiles[p.x][p.y]);
        return "worldSpawn=" + p.x + "," + p.y + " descriptor=" + (d == null ? "none" : d.inspectLine());
    }

    String statusLine(GamePanel game) {
        return "authority=" + VERSION + " deathSinglePlayer=game-over serverRespawn=bed-base-or-world-spawn " + worldSpawnDescriptorSummary(game);
    }

    static String auditSummary() {
        return "authority=" + VERSION + " rules=single-player-game-over/unconscious-base-medicae-spawn/server-world-respawn-bed-base-or-spawn";
    }
}
