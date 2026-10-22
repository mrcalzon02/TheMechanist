package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Bridges actual game-world motion, noise sources, and confirmed combat events
 * into the existing spatial renderer. No synthetic combat or simulation RNG.
 */
final class WorldPositionalSoundAuthority {
    record Footstep(String actorId, int x, int y, int mode, String cue,
                    int distance) { }

    private WorldPositionalSoundAuthority() { }

    static Footstep movedNpc(World world, NpcEntity npc, int previousX, int previousY,
                             int listenerX, int listenerY) {
        if (world == null || npc == null || npc.hp <= 0
                || (npc.x == previousX && npc.y == previousY)
                || !world.inBounds(npc.x, npc.y)) return null;
        int distance = Math.abs(listenerX - npc.x) + Math.abs(listenerY - npc.y);
        if (distance > 15) return null;
        return new Footstep(npc.id, npc.x, npc.y,
                npc.activeMotionStateIndex, footstepKey(world, npc.x, npc.y),
                distance);
    }

    static String footstepKey(World world, int x, int y) {
        if (world == null || !world.inBounds(x, y)) return "footstep_metal";
        char tile = world.tiles[x][y];
        if (tile == '~' || tile == ';') return "footstep_sludge";
        if (tile == ',' || tile == ':') return "footstep_debris";
        if (tile == '+' || tile == '=') return "footstep_grate";
        return "footstep_metal";
    }

    static List<Footstep> nearestSteps(List<Footstep> actualMoves, int maxVoices) {
        if (actualMoves == null || maxVoices <= 0) return List.of();
        ArrayList<Footstep> ranked = new ArrayList<>();
        for (Footstep step : actualMoves) {
            if (step != null && step.mode() != GamePanel.MOTION_STATIONARY)
                ranked.add(step);
        }
        ranked.sort(Comparator.comparingInt(Footstep::distance)
                .thenComparing(step -> step.actorId() == null ? "" : step.actorId()));
        return List.copyOf(ranked.subList(0, Math.min(maxVoices, ranked.size())));
    }

    static void playNpcSteps(GamePanel game, List<Footstep> actualMoves) {
        if (!active(game)) return;
        for (Footstep step : nearestSteps(actualMoves, 2)) {
            // Sneaking is intentionally quiet; sprinting is audibly heavy.
            double loudness = switch (step.mode()) {
                case GamePanel.MOTION_SNEAK -> 0.24;
                case GamePanel.MOTION_RUN -> 0.85;
                case GamePanel.MOTION_SPRINT -> 1.0;
                default -> 0.52;
            };
            play(game, step.cue(), step.x(), step.y(), loudness);
        }
    }

    static void playPlayerStep(GamePanel game, int previousX, int previousY) {
        if (!active(game) || (previousX == game.playerX
                && previousY == game.playerY)) return;
        int mode = game.selectedMovementModeIndex;
        double strength = switch (mode) {
            case GamePanel.MOTION_SNEAK -> 0.20;
            case GamePanel.MOTION_RUN -> 0.72;
            case GamePanel.MOTION_SPRINT -> 0.95;
            default -> 0.45;
        };
        play(game, footstepKey(game.world, game.playerX, game.playerY),
                game.playerX, game.playerY, strength);
    }

    static void playConfirmedAttack(GamePanel game,
                                    StructuralScaleCombatAuthority.WeaponProfile weapon) {
        if (!active(game) || weapon == null) return;
        String key = weapon.firearm()
                ? new SoundManager().weaponEffectKey(weapon.name(), true)
                : new SoundManager().weaponEffectKey(weapon.name(), false);
        play(game, key, game.playerX, game.playerY, 1.0);
    }

    static String noiseCue(ZoneNoiseSourceRecord noise) {
        if (noise == null || !noise.active) return null;
        String kind = (noise.kind == null ? "" : noise.kind).toLowerCase(Locale.ROOT);
        String label = (noise.label == null ? "" : noise.label).toLowerCase(Locale.ROOT);
        if (kind.contains("gunfire") || kind.contains("gunshot")
                || label.contains("gunfire")) return "weapon_shot";
        if (kind.contains("explosion") || kind.contains("artillery")
                || label.contains("detonation")) return "weapon_thundering";
        if (kind.contains("electr") || kind.contains("hazard-wire"))
            return "ambient_spark";
        if (kind.contains("machin") || kind.startsWith("tile-"))
            return "ambient_machine";
        if (kind.contains("broadcast")) return "ambient_radio";
        if (kind.contains("trap")) return "ambient_servo";
        return null;
    }

    static boolean noiseActiveThisTurn(ZoneNoiseSourceRecord noise, int turn) {
        return noise != null && noise.active
                && (!noise.intermittent || Math.floorMod(turn + noise.phase,
                       Math.max(2, noise.period)) != 0);
    }

    static boolean sameRoom(World world, int listenerX, int listenerY,
                            int sourceX, int sourceY) {
        if (world == null || world.roomIds == null
                || !world.inBounds(listenerX, listenerY)
                || !world.inBounds(sourceX, sourceY)) return false;
        int a = world.roomIds[listenerX][listenerY];
        int b = world.roomIds[sourceX][sourceY];
        return a >= 0 && a == b;
    }

    static void play(GamePanel game, String cue, int x, int y, double strength) {
        if (!active(game) || cue == null || strength <= 0
                || !game.world.inBounds(x, y)) return;
        boolean occluded = !game.hasLineOfSight(
                game.playerX, game.playerY, x, y);
        boolean echo = sameRoom(game.world,
                game.playerX, game.playerY, x, y);
        game.sounds.playPositioned(cue, x, y,
                game.playerX, game.playerY, game.facingDx, game.facingDy,
                occluded, echo, strength, game.options);
    }

    private static boolean active(GamePanel game) {
        return game != null && game.world != null && game.options != null
                && game.options.soundEnabled && game.options.volume > 0
                && game.options.sfxVolume > 0
                && (game.screen == GamePanel.Screen.GAME
                    || game.screen == GamePanel.Screen.PANEL);
    }
}
