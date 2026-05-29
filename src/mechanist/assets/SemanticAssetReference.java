package mechanist.assets;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Stable 8-character semantic asset handle.
 *
 * Current canonical form is four characters, a hyphen, then three characters,
 * for example TILE-A01, OBJ-WB01, or WEAP-K01. The hyphen is part of the public
 * handle, so the stored string length is exactly eight characters.
 */
public record SemanticAssetReference(String id) {
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Z0-9]{3,4}-[A-Z0-9]{3,4}");

    public SemanticAssetReference {
        if (id == null) {
            throw new IllegalArgumentException("Semantic asset reference cannot be null");
        }
        id = id.trim().toUpperCase(Locale.ROOT);
        if (id.length() != 8) {
            throw new IllegalArgumentException("Semantic asset reference must be exactly 8 characters: " + id);
        }
        if (!ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Semantic asset reference must match forms like TILE-A01, OBJ-WB01, or WEAP-K01: " + id);
        }
    }

    public static SemanticAssetReference of(String raw) {
        return new SemanticAssetReference(raw);
    }
}
