package mechanist.assets;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Deterministic path resolver for generated source-first graphical cells.
 *
 * This is a bridge class, not the final semantic registry replacement.  Existing
 * systems can keep using AssetManager/AssetRegistry while migration code asks
 * this resolver for canonical generated-tier paths.
 */
public final class GeneratedAssetPathResolver {
    public static final String GENERATED_ROOT = "assets/graphics/generated";

    private GeneratedAssetPathResolver() {}

    public static String canonicalPath(AssetQualityTier tier, String sheetId, String filename) {
        AssetQualityTier safeTier = tier == null ? AssetQualityTier.LOW_32 : tier;
        return GENERATED_ROOT + "/" + safeTier.directoryName() + "/" + requireSegment(sheetId, "sheetId") + "/" + requireSegment(filename, "filename");
    }

    public static String canonicalCellPath(AssetQualityTier tier, String sheetId, int row, int col) {
        String safeSheet = requireSegment(sheetId, "sheetId");
        String filename = safeSheet + "_r" + row + "c" + col + ".png";
        return canonicalPath(tier, safeSheet, filename);
    }

    public static Path resolve(Path projectRoot, AssetQualityTier tier, String sheetId, String filename) {
        Path root = projectRoot == null ? Path.of(".") : projectRoot;
        return root.resolve(canonicalPath(tier, sheetId, filename)).normalize();
    }

    public static String logicalCellId(String sheetId, int row, int col) {
        String normalized = requireSegment(sheetId, "sheetId").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        return normalized + "__r" + String.format(Locale.ROOT, "%02d", row) + "c" + String.format(Locale.ROOT, "%02d", col);
    }

    private static String requireSegment(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        String trimmed = value.trim().replace('\\', '/');
        if (trimmed.contains("..") || trimmed.startsWith("/") || trimmed.contains("//")) {
            throw new IllegalArgumentException(label + " must be a relative safe path segment: " + value);
        }
        return trimmed;
    }
}
