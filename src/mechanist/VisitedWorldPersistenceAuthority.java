package mechanist;

import java.io.*;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Persists inactive visited slices and restores them when their physical map is requested. */
final class VisitedWorldPersistenceAuthority {
    static final String PREFIX = "world.visitedSlice.v1.";
    private static final int MAX_EXPANDED_BYTES = 128 * 1024 * 1024;

    static String key(int sx, int sy, int zx, int zy, int layer) {
        return sx + "-" + sy + "-" + zx + "-" + zy + "-" + layer;
    }

    static void write(GamePanel game, Properties save) {
        save.keySet().removeIf(key -> key instanceof String text && text.startsWith(PREFIX));
        if (game.atlas == null) return;
        WorldAtlas atlas = game.atlas;
        var snapshots = new LinkedHashMap<>(atlas.savedSliceStates);
        for (int sx = 0; sx < 3; sx++) for (int sy = 0; sy < 3; sy++)
            for (int zx = 0; zx < 3; zx++) for (int zy = 0; zy < 3; zy++)
                for (int layer = 0; layer < 20; layer++) {
                    World world = atlas.slices[sx][sy][zx][zy][layer];
                    if (world == null) continue;
                    String key = key(sx + 1, sy + 1, zx + 1, zy + 1, layer);
                    if (world == game.world) {
                        snapshots.remove(key); // The active slice is already stored by writeWorldState.
                        continue;
                    }
                    Properties state = new Properties();
                    Persistence.writeWorldState(world, state);
                    snapshots.put(key, encode(state));
                }
        snapshots.forEach((key, value) -> save.setProperty(PREFIX + key, value));
    }

    static void register(WorldAtlas atlas, Properties save) {
        atlas.savedSliceStates.clear();
        for (String name : save.stringPropertyNames()) if (name.startsWith(PREFIX)) {
            String key = name.substring(PREFIX.length());
            if (!key.matches("[1-3]-[1-3]-[1-3]-[1-3]-(?:[0-9]|1[0-9])"))
                throw new IllegalStateException("Invalid visited-world coordinates: " + key);
            atlas.savedSliceStates.put(key, save.getProperty(name));
        }
    }

    static void restore(WorldAtlas atlas, World world, String key) {
        String encoded = atlas.savedSliceStates.get(key);
        if (encoded == null) return;
        try (var in = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(encoded)))) {
            byte[] bytes = in.readNBytes(MAX_EXPANDED_BYTES + 1);
            if (bytes.length > MAX_EXPANDED_BYTES) throw new IOException("Visited-world snapshot is too large");
            Properties state = new Properties();
            state.load(new ByteArrayInputStream(bytes));
            String coordinates = world.sectorX + "," + world.sectorY + "," + world.zoneX + ","
                    + world.zoneY + "," + world.floor + "," + world.sewerLayer;
            if (!Long.toString(world.seed).equals(state.getProperty("world.seed"))
                    || !coordinates.equals(state.getProperty("world.coord"))
                    || !state.containsKey(WorldTerrainSnapshotAuthority.KEY))
                throw new IOException("Visited-world identity differs from the requested slice");
            Persistence.readWorldState(world, state);
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Could not restore visited-world snapshot " + key, ex);
        }
    }

    private static String encode(Properties state) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (var out = new GZIPOutputStream(bytes)) {
                state.store(new FilterOutputStream(out) {
                    private int written;
                    @Override public void write(int value) throws IOException {
                        if (written == MAX_EXPANDED_BYTES) throw new IOException("Visited-world snapshot is too large");
                        out.write(value); written++;
                    }
                    @Override public void write(byte[] data, int offset, int length) throws IOException {
                        if (length > MAX_EXPANDED_BYTES - written) throw new IOException("Visited-world snapshot is too large");
                        out.write(data, offset, length); written += length;
                    }
                }, "Visited world slice");
            }
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not save visited world", ex);
        }
    }

    private VisitedWorldPersistenceAuthority() { }
}
