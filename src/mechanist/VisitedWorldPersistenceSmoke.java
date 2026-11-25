package mechanist;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/** Saves on another floor and proves the original visited floor survives restart. */
final class VisitedWorldPersistenceSmoke {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            Path root = Files.createTempDirectory("mechanist-visited-world-").toAbsolutePath();
            String javaExecutable = Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java").toString();
            Process child = new ProcessBuilder(javaExecutable, "-Djava.awt.headless=true",
                    "-Dmechanist.storage.root=" + root,
                    "-Djava.io.tmpdir=" + Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath(),
                    "-cp", System.getProperty("java.class.path"),
                    VisitedWorldPersistenceSmoke.class.getName(), "--isolated-child").inheritIO().start();
            if (!child.waitFor(300, TimeUnit.SECONDS)) {
                child.destroyForcibly().waitFor();
                throw new AssertionError("visited-world persistence child timed out; evidence=" + root);
            }
            require(child.exitValue() == 0, "visited-world persistence child failed; evidence=" + root);
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
            return;
        }
        require(args.length == 1 && "--isolated-child".equals(args[0]), "unknown probe arguments");
        require(!System.getProperty(GameStorageManager.OVERRIDE_PROPERTY, "").isBlank(), "isolated storage required");
        require(!Files.exists(SaveSlotSurfaceApi.savePathForSlot(1)), "refusing to overwrite an existing slot");
        GamePanel game = new GamePanel();
        game.timer.stop();
        int x = -1, y = -1;
        char[][] expected;
        try {
            WorldSetupSettings setup = WorldSetupSettings.standard();
            setup.zoneSize = 0; setup.npcDensity = 0; setup.simulationAge = 0;
            game.startPackagedClientNewGameWith(Candidate.random(new Random(27181)), setup);
            World original = game.world;
            outer: for (int a = 1; a < original.w - 1; a++) for (int b = 1; b < original.h - 1; b++) {
                if (original.tiles[a][b] == '#' && original.tiles[a + 1][b] == '.') {
                    x = a; y = b; break outer;
                }
            }
            require(x >= 0, "no excavatable wall fixture found");
            original.tiles[x][y] = '.';
            original.npcs.clear(); // An empty population is authoritative, too.
            expected = Arrays.stream(original.tiles).map(char[]::clone).toArray(char[][]::new);
            game.atlas.floor = 5;
            game.world = game.atlas.currentWorld();
            var start = game.world.startPoint();
            game.playerX = start.x; game.playerY = start.y;
            game.writeSaveFile(1, false);
        } finally { game.shutdownRuntime(); }
        GamePanel resumed = new GamePanel();
        resumed.timer.stop();
        try {
            require(resumed.loadSaveSlot(1, "visited floor persistence"), "saved floor did not load");
            require(resumed.atlas.slices[0][0][1][1][6] == null, "inactive floor should restore lazily");
            resumed.writeSaveFile(2, false);
            require(resumed.loadSaveSlot(2, "unvisited snapshot carry-forward"), "resaved floor did not load");
            require(resumed.atlas.slices[0][0][1][1][6] == null, "resaving should not eagerly generate inactive floors");
            String key = VisitedWorldPersistenceAuthority.key(1, 1, 2, 2, 6);
            try {
                VisitedWorldPersistenceAuthority.restore(resumed.atlas, resumed.world, key);
                throw new AssertionError("a visited snapshot was accepted for the wrong floor");
            } catch (IllegalStateException expectedFailure) { }
            String snapshot = resumed.atlas.savedSliceStates.put(key, "invalid snapshot");
            try {
                try {
                    VisitedWorldPersistenceAuthority.restore(resumed.atlas, resumed.world, key);
                    throw new AssertionError("corrupt visited snapshot was accepted");
                } catch (IllegalStateException expectedFailure) { }
            } finally { resumed.atlas.savedSliceStates.put(key, snapshot); }
            resumed.atlas.floor = 4;
            World previous = resumed.atlas.currentWorld();
            require(Arrays.deepEquals(expected, previous.tiles), "previous floor terrain changed after restart");
            require(previous.tiles[x][y] == '.', "previously excavated wall was lost");
            require(previous.npcs.isEmpty(), "cleared previous-floor NPCs respawned");
            var malformed = new java.util.Properties();
            malformed.setProperty(VisitedWorldPersistenceAuthority.PREFIX + "4-1-2-2-6", snapshot);
            try {
                VisitedWorldPersistenceAuthority.register(resumed.atlas, malformed);
                throw new AssertionError("invalid visited-world coordinates were accepted");
            } catch (IllegalStateException expectedFailure) { }
            System.out.println("VisitedWorldPersistenceSmoke PASS inactiveFloor=true exactTiles=true clearedPopulation=true lazyRestore=true carryForward=true invalidRejected=true");
        } finally { resumed.shutdownRuntime(); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
