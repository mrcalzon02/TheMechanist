package mechanist;

import java.util.Arrays;
import java.util.List;

/** Headless-safe positional mixer and live world emitter binding regression. */
final class SpatialAudioSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        var east = SpatialAudioAuthority.mix(10, 10, 0, -1,
                12, 10, 80, 16, false);
        var west = SpatialAudioAuthority.mix(10, 10, 0, -1,
                8, 10, 80, 16, false);
        var forward = SpatialAudioAuthority.mix(10, 10, 0, -1,
                10, 8, 80, 16, false);
        var turned = SpatialAudioAuthority.mix(10, 10, 0, 1,
                12, 10, 80, 16, false);
        require(east.right() > east.left() * 2.0
                        && east.pan() > 0
                        && west.left() > west.right() * 2.0
                        && west.pan() < 0,
                "left/right positioning must follow the player's facing");
        require(close(forward.left(), forward.right())
                        && turned.left() > turned.right() * 2.0,
                "sound in front should be centered and turning should reverse perceived side");
        var near = SpatialAudioAuthority.mix(10, 10, 1, 0,
                11, 10, 80, 16, false);
        var far = SpatialAudioAuthority.mix(10, 10, 1, 0,
                17, 10, 80, 16, false);
        var muffled = SpatialAudioAuthority.mix(10, 10, 1, 0,
                11, 10, 80, 16, true);
        var silent = SpatialAudioAuthority.mix(10, 10, 1, 0,
                30, 10, 80, 16, false);
        require(near.audible() && far.audible()
                        && near.left() > far.left()
                        && near.right() > far.right()
                        && muffled.left() < near.left()
                        && muffled.right() < near.right()
                        && !silent.audible(),
                "attenuation, occlusion and distance cutoff must be monotonic");
        require(!SpatialAudioAuthority.mix(10, 10, 1, 0,
                        11, 10, 0, 16, false).audible(),
                "muted positional sound must not play");

        byte[] originalMono = { 0, 64, 0, -64 };
        byte[] stereo = SpatialAudioAuthority.stereoPcm16(
                originalMono, 1, east);
        require(stereo.length == 8
                        && Math.abs(sample(stereo, 2)) > Math.abs(sample(stereo, 0))
                        && sample(stereo, 0) > 0
                        && sample(stereo, 4) < 0
                        && sample(stereo, 6) < 0,
                "mono sound must become genuine signed stereo samples with panning");
        byte[] stereoOriginal = { 0, 64, 0, 0 };
        byte[] pannedStereo = SpatialAudioAuthority.stereoPcm16(
                stereoOriginal, 2, west);
        require(pannedStereo.length == 4
                        && Math.abs(sample(pannedStereo, 0))
                        > Math.abs(sample(pannedStereo, 2)),
                "two-channel recordings must also move toward the actual source side");
        require(Arrays.equals(new byte[0],
                        SpatialAudioAuthority.stereoPcm16(new byte[0], 1, east)),
                "empty PCM stream must remain empty");

        byte[] reflected = SpatialAudioAuthority.roomReflectionsPcm16(
                stereo, 44100, 1.0);
        require(reflected.length > stereo.length
                        && sample(reflected, 3219 * 4) != 0
                        && sample(reflected, 3219 * 4 + 2) != 0
                        && Arrays.equals(stereo, Arrays.copyOf(reflected, stereo.length))
                        && Arrays.equals(stereo, SpatialAudioAuthority.roomReflectionsPcm16(
                                stereo, 44100, 0)),
                "indoor reverberation must preserve the dry source while adding bounded stereo reflections");

        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            World world = new World(7164L, 24, 20);
            for (int x = 0; x < world.w; x++)
                for (int y = 0; y < world.h; y++)
                    world.tiles[x][y] = '.';
            world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
            game.world = world;
            game.playerX = 10;
            game.playerY = 10;
            game.facingDx = 0;
            game.facingDy = -1;
            MapObjectState ignored = new MapObjectState();
            ignored.type = "supply_crate";
            ignored.x = 10;
            ignored.y = 10;
            world.mapObjects.add(ignored);
            MapObjectState nearbyMachine = new MapObjectState();
            nearbyMachine.type = "industrial_machine";
            nearbyMachine.x = 12;
            nearbyMachine.y = 10;
            world.mapObjects.add(nearbyMachine);
            MapObjectState fartherRadio = new MapObjectState();
            fartherRadio.type = "radio_broadcast";
            fartherRadio.x = 15;
            fartherRadio.y = 10;
            world.mapObjects.add(fartherRadio);

            GamePanel.PassiveAmbientCue emitted =
                    game.passiveAmbientCueForCurrentView();
            require(emitted != null && emitted.positioned()
                            && "ambient_machine".equals(emitted.key())
                            && emitted.sourceX() == 12 && emitted.sourceY() == 10
                            && emitted.distance() == 2,
                    "ambient fixture choice must carry exact nearby machine coordinates");
            world.mapObjects.remove(nearbyMachine);
            emitted = game.passiveAmbientCueForCurrentView();
            require(emitted != null && emitted.positioned()
                            && "ambient_radio".equals(emitted.key())
                            && emitted.sourceX() == 15,
                    "ambient source must change when the nearest valid emitter disappears");
            world.mapObjects.clear();
            emitted = game.passiveAmbientCueForCurrentView();
            require(emitted != null && !emitted.positioned()
                            && "ambient_press".equals(emitted.key()),
                    "zone-wide ambience must remain diffuse, not incorrectly panned");

            ZoneNoiseSourceRecord gunfire = new ZoneNoiseSourceRecord(
                    "sfx-1", "distant gunfire", "gunfire", 14, 10,
                    12, 40, true, false, 0, 5, "live-combat");
            world.noiseSources.add(gunfire);
            emitted = game.passiveAmbientCueForCurrentView();
            require(emitted != null && emitted.positioned()
                            && "weapon_shot".equals(emitted.key())
                            && emitted.sourceX() == 14 && emitted.sourceY() == 10,
                    "active simulated gunfire must retain its actual source location");
            gunfire.x = 8;
            emitted = game.passiveAmbientCueForCurrentView();
            require(emitted != null && emitted.positioned()
                            && emitted.sourceX() == 8,
                    "moving sound sources must use live coordinates rather than cached positions");
            gunfire.active = false;
            emitted = game.passiveAmbientCueForCurrentView();
            require(emitted != null && !emitted.positioned(),
                    "inactive noise records must not continue sounding");

            NpcEntity moving = new NpcEntity();
            moving.id = "moving-guard";
            moving.hp = 12;
            moving.x = 11;
            moving.y = 10;
            moving.activeMotionStateIndex = GamePanel.MOTION_RUN;
            world.tiles[11][10] = '+';
            var step = WorldPositionalSoundAuthority.movedNpc(
                    world, moving, 10, 10, game.playerX, game.playerY);
            require(step != null && step.distance() == 1
                            && "footstep_grate".equals(step.cue()),
                    "actual mobile NPC movement must emit material-aware positional footsteps");
            require(WorldPositionalSoundAuthority.movedNpc(
                    world, moving, 11, 10, game.playerX, game.playerY) == null,
                    "stationary NPCs must not emit a footstep");
            var farStep = new WorldPositionalSoundAuthority.Footstep(
                    "distant", 20, 10, GamePanel.MOTION_WALK, "footstep_metal", 10);
            var nearest = WorldPositionalSoundAuthority.nearestSteps(
                    List.of(farStep, step, farStep), 1);
            require(nearest.size() == 1 && nearest.get(0) == step,
                    "crowd footsteps must be capped and favor nearby actors");

            world.roomIds[10][10] = 3;
            world.roomIds[11][10] = 3;
            require(WorldPositionalSoundAuthority.sameRoom(world, 10, 10, 11, 10),
                    "shared enclosed room must be eligible for early reflections");
            world.roomIds[11][10] = 4;
            require(!WorldPositionalSoundAuthority.sameRoom(world, 10, 10, 11, 10),
                    "separate rooms must not sound like a shared reverberant chamber");
        } finally {
            game.shutdownRuntime();
        }
        System.out.println("SpatialAudioSmoke PASS stereo position, range, facing, room reflections, NPC footsteps, gunfire and moving emitter binding");
    }

    private static short sample(byte[] data, int index) {
        return (short) ((data[index] & 0xff)
                | ((data[index + 1] & 0xff) << 8));
    }

    private static boolean close(double a, double b) {
        return Math.abs(a - b) < 1.0e-9;
    }

    private static void require(boolean check, String message) {
        if (!check) throw new AssertionError(message);
    }

    private SpatialAudioSmoke() { }
}
