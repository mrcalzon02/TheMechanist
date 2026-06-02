package mechanist.assets;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable semantic description for one graphical asset.
 *
 * The ID is the public, stable handle used by game systems and the Infopedia.
 * The path/URI is deliberately kept as metadata behind that handle so callers
 * do not spread raw image paths through gameplay and UI code.
 */
public record AssetMetadata(
        String id,
        String pathOrUri,
        String name,
        AssetType type,
        String semanticDescription
) {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Z0-9]{3,4}-[A-Z0-9]{3,4}");

    public AssetMetadata {
        id = normalizeId(id);
        pathOrUri = requireText(pathOrUri, "Asset path/URI");
        name = requireText(name, "Asset name");
        semanticDescription = requireText(semanticDescription, "Semantic description");
        type = Objects.requireNonNullElse(type, AssetType.UNKNOWN);

        if (id.length() != 8) {
            throw new IllegalArgumentException("Semantic asset ID must be exactly 8 characters: " + id);
        }
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Semantic asset ID must match forms like TILE-A01, OBJ-WB01, or WEAP-K01: " + id);
        }
    }

    public boolean matchesFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }
        String q = filter.toLowerCase(Locale.ROOT);
        return id.toLowerCase(Locale.ROOT).contains(q)
                || pathOrUri.toLowerCase(Locale.ROOT).contains(q)
                || name.toLowerCase(Locale.ROOT).contains(q)
                || type.displayName().toLowerCase(Locale.ROOT).contains(q)
                || semanticDescription.toLowerCase(Locale.ROOT).contains(q);
    }

    private static String normalizeId(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Semantic asset ID cannot be null");
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be blank");
        }
        return value.trim();
    }
}
