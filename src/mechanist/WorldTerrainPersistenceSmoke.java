package mechanist;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;

/** Terrain round-trip, legacy compatibility and failed-write preservation checks. */
final class WorldTerrainPersistenceSmoke {
    public static void main(String[] args) throws Exception {
        World world = new World(17, 24, 24);
        for (char[] column : world.tiles) Arrays.fill(column, '#');
        world.tiles[4][5] = '.'; // An excavated wall.
        world.tiles[6][7] = ' '; // A cleared space.
        world.tiles[8][9] = '\u2591';
        Properties saved = new Properties();
        WorldTerrainSnapshotAuthority.write(world, saved);
        World resumed = new World(17, 24, 24);
        WorldTerrainSnapshotAuthority.read(resumed, saved);
        require(Arrays.deepEquals(world.tiles, resumed.tiles), "physical terrain did not round-trip");
        WorldTerrainSnapshotAuthority.read(resumed, new Properties());
        require(Arrays.deepEquals(world.tiles, resumed.tiles), "legacy save overwrote generated terrain");
        Properties corrupt = new Properties();
        corrupt.setProperty(WorldTerrainSnapshotAuthority.KEY, "invalid terrain");
        expectCorrupt(resumed, corrupt);
        require(Arrays.deepEquals(world.tiles, resumed.tiles), "corrupt terrain partly replaced the live grid");
        expectCorrupt(new World(17, 25, 24), saved);

        var root = Files.createTempDirectory("mechanist-save-atomic-");
        var slot = root.resolve("slot.mechsave");
        var temporary = root.resolve("slot.tmp");
        try {
            byte[] original = "previous complete campaign".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            GameStorageManager.writeAtomic(temporary, slot, original);
            Properties broken = new Properties() {
                @Override public void store(java.io.OutputStream out, String comment) throws java.io.IOException {
                    out.write(1);
                    throw new java.io.IOException("injected serialization failure");
                }
            };
            try {
                GameStorageManager.writePropertiesAtomic(slot, broken, "failed save");
                throw new AssertionError("serialization failure was accepted");
            } catch (java.io.IOException expected) {
                require(Arrays.equals(original, Files.readAllBytes(slot)), "serialization failure destroyed the previous save");
            }
            Files.createDirectory(temporary);
            var blocker = Files.writeString(temporary.resolve("blocker"), "owned failure fixture");
            try {
                GameStorageManager.writeAtomic(temporary, slot, new byte[] {1, 2, 3});
                throw new AssertionError("failed temporary write was accepted");
            } catch (java.io.IOException expected) {
                require(Arrays.equals(original, Files.readAllBytes(slot)), "failed write destroyed the previous save");
            } finally {
                Files.delete(blocker);
                Files.delete(temporary);
            }
            byte[] replacement = "next complete campaign".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            GameStorageManager.writeAtomic(temporary, slot, replacement);
            require(Arrays.equals(replacement, Files.readAllBytes(slot)), "replacement was not committed");
            require(!Files.exists(temporary), "temporary save leaked after success");
        } finally {
            Files.deleteIfExists(slot);
            Files.deleteIfExists(root);
        }
        System.out.println("WorldTerrainPersistenceSmoke PASS exactTiles=true legacy=true corruptRejected=true failedWritePreserved=true");
    }

    private static void expectCorrupt(World world, Properties properties) {
        try {
            WorldTerrainSnapshotAuthority.read(world, properties);
            throw new AssertionError("corrupt or mismatched terrain was accepted");
        } catch (IllegalStateException expected) { }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
