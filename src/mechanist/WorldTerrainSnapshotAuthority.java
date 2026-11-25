package mechanist;

import java.io.*;
import java.util.Base64;
import java.util.Properties;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Saves the physical tile grid, including excavations and removed overlays. */
final class WorldTerrainSnapshotAuthority {
    static final String KEY = "world.terrain.gzip.v1";

    static void write(World world, Properties properties) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
                out.writeInt(world.w);
                out.writeInt(world.h);
                for (char[] column : world.tiles) for (char tile : column) out.writeChar(tile);
            }
            properties.setProperty(KEY, Base64.getEncoder().encodeToString(bytes.toByteArray()));
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not encode world terrain", ex);
        }
    }

    static void read(World world, Properties properties) {
        String encoded = properties.getProperty(KEY);
        if (encoded == null) return; // Older saves retain generated terrain and saved object overlays.
        try (DataInputStream in = new DataInputStream(new GZIPInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(encoded))))) {
            if (in.readInt() != world.w || in.readInt() != world.h)
                throw new IOException("Saved terrain dimensions differ from the world slice");
            char[][] restored = new char[world.w][world.h];
            for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++)
                restored[x][y] = in.readChar();
            if (in.read() != -1) throw new IOException("Unexpected trailing terrain data");
            world.tiles = restored;
            world.compiledTileDescriptors = null;
            world.dirtyVisionRevision++;
            world.dirtyLightRevision++;
            if (StructuredZoneGridRuntime.worldHasStructuredGrid(world))
                StructuredZoneGridRuntime.rebuildGridForWorld(world);
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Saved terrain is corrupt; refusing regenerated terrain", ex);
        }
    }

    private WorldTerrainSnapshotAuthority() { }
}
