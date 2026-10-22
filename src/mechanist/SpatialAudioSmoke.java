package mechanist;

import java.util.Arrays;

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
        } finally {
            game.shutdownRuntime();
        }
        System.out.println("SpatialAudioSmoke PASS stereo position, range, facing, occlusion, PCM and live emitter binding");
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
