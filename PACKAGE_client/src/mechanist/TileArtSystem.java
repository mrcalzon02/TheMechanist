package mechanist;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 * Orchestration engine for tile-art loading and lifecycle management.
 *
 * This class handles coordinate reading, PNG I/O streaming, and links the other
 * decomposed architectural components together at runtime.
 */
final class TileArtSystem {
    private final TileImageRegistry registry;
    private String activeArtRoot = "";
    private String activeQualityTier = "low_32";

    public TileArtSystem(TileImageRegistry registry) {
        this.registry = registry != null ? registry : new TileImageRegistry();
    }

    /**
     * Coordinate full system loading from disk.
     */
    public boolean loadTileArt(String packDir, String cacheDir, String fallbackRoot, String targetQuality) {
        registry.clear();

        // 1. Resolve path authority
        this.activeArtRoot = ArtPackManager.prepareAndResolveRoot(packDir, cacheDir, fallbackRoot);
        this.activeQualityTier = ArtPackManager.normalizeQualityFolder(targetQuality);

        String cellsPathStr = ArtPackManager.resolveQualityCellsRoot(activeArtRoot, activeQualityTier);
        Path cellsDir = Paths.get(cellsPathStr);

        if (!Files.isDirectory(cellsDir)) {
            DebugLog.error("TILE_ART", "Cannot load tile art; cells directory missing: " + cellsPathStr);
            return false;
        }

        // 2. Discover and load raw PNG streams
        DebugLog.audit("TILE_ART", "Spawning image stream loader from path: " + cellsDir.toString());
        try (Stream<Path> stream = Files.walk(cellsDir, 3)) {
            stream.filter(Files::isRegularFile)
                  .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                  .forEach(this::loadSinglePngFile);
        } catch (IOException ex) {
            DebugLog.error("TILE_ART", "Critical I/O breakdown during stream discovery: " + ex.getMessage());
            return false;
        }

        // 3. Populate default bindings and layouts
        GlyphBinder.applyDefaultEngineBindings(registry);

        DebugLog.audit("TILE_ART", "Load cycle complete. Telemetry: " + registry.auditSummary());
        return !registry.isEmpty();
    }

    private void loadSinglePngFile(Path file) {
        try {
            String fileName = file.getFileName().toString();
            String baseName = fileName.substring(0, fileName.lastIndexOf('.'));

            BufferedImage img = ImageIO.read(file.toFile());
            if (img == null) {
                DebugLog.warn("TILE_ART", "Skipping corrupted file header payload: " + fileName);
                return;
            }

            // Route standard game assets cleanly into maps
            if (baseName.length() == 1) {
                registry.putGlyph(baseName.charAt(0), img);
            } else {
                registry.putAlias(baseName, img);
                // If it meets semantic format specifications, route it to semantic index
                if (baseName.length() == 8 && baseName.matches("^[A-Za-z0-9]+$")) {
                    registry.putSemantic(baseName, img);
                }
            }
        } catch (Exception ex) {
            DebugLog.warn("TILE_ART", "Failed to decode target image sequence: " + file + " -> " + ex.getMessage());
        }
    }

    static String semanticKeyForBuildName(String buildName) {
        if (buildName == null) return null;
        String normalized = buildName.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
        return normalized.isEmpty() ? null : normalized;
    }

    static String semanticKeyForMapObject(MapObjectState obj) {
        if (obj == null) return null;
        String key = firstNonBlank(obj.label, obj.type, obj.stockState);
        return semanticKeyForBuildName(key);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    public TileImageRegistry getRegistry() {
        return this.registry;
    }

    public String getActiveArtRoot() {
        return this.activeArtRoot;
    }

    public String getActiveQualityTier() {
        return this.activeQualityTier;
    }
}
