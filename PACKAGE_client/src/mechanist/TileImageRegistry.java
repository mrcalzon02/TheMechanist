package mechanist;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Runtime tile-art image registry.
 *
 * This class owns only the loaded in-memory image maps used by the client
 * renderer. It deliberately does not perform filesystem lookup, PNG loading,
 * asset-pack path selection, semantic TSV parsing, or package-root resolution.
 *
 * Do not merge this with mechanist.assets.AssetRegistry. That class is the
 * semantic metadata/index authority. This class is the runtime BufferedImage
 * storage authority for tile rendering.
 */
final class TileImageRegistry {
    private final Map<Character, BufferedImage> byGlyph = new LinkedHashMap<>();
    private final Map<String, BufferedImage> byAlias = new LinkedHashMap<>();
    private final Map<String, BufferedImage> bySemantic = new LinkedHashMap<>();
    private final Set<Character> northSouthCorridorGlyphs = new LinkedHashSet<>();

    void clear() {
        byGlyph.clear();
        byAlias.clear();
        bySemantic.clear();
        northSouthCorridorGlyphs.clear();
    }

    boolean isEmpty() { return byGlyph.isEmpty() && byAlias.isEmpty() && bySemantic.isEmpty(); }
    int glyphCount() { return byGlyph.size(); }
    int aliasCount() { return byAlias.size(); }
    int semanticCount() { return bySemantic.size(); }
    int loadedCount() { return byGlyph.size(); }

    void putGlyph(char glyph, BufferedImage image) { if (image != null) byGlyph.put(glyph, image); }

    void putAlias(String alias, BufferedImage image) {
        String key = normalizeAlias(alias);
        if (key != null && image != null) byAlias.put(key, image);
    }

    void putSemantic(String semanticId, BufferedImage image) {
        String key = normalizeSemanticId(semanticId);
        if (key != null && image != null) bySemantic.put(key, image);
    }

    BufferedImage getGlyph(char glyph) { return byGlyph.get(glyph); }
    BufferedImage getAlias(String alias) { String key = normalizeAlias(alias); return key == null ? null : byAlias.get(key); }
    BufferedImage getSemantic(String semanticId) { String key = normalizeSemanticId(semanticId); return key == null ? null : bySemantic.get(key); }

    Optional<BufferedImage> findGlyph(char glyph) { return Optional.ofNullable(getGlyph(glyph)); }
    Optional<BufferedImage> findAlias(String alias) { return Optional.ofNullable(getAlias(alias)); }
    Optional<BufferedImage> findSemantic(String semanticId) { return Optional.ofNullable(getSemantic(semanticId)); }

    boolean hasGlyph(char glyph) { return byGlyph.containsKey(glyph); }
    boolean hasAlias(String alias) { String key = normalizeAlias(alias); return key != null && byAlias.containsKey(key); }
    boolean hasSemantic(String semanticId) { String key = normalizeSemanticId(semanticId); return key != null && bySemantic.containsKey(key); }

    BufferedImage getTile(char glyph) { return byGlyph.get(glyph); }

    BufferedImage getTile(String semanticOrAlias, char fallbackGlyph) {
        BufferedImage semantic = getSemantic(semanticOrAlias);
        if (semantic != null) return semantic;
        BufferedImage alias = getAlias(semanticOrAlias);
        if (alias != null) return alias;
        return getGlyph(fallbackGlyph);
    }

    void bindGlyphToAlias(char glyph, String alias) {
        BufferedImage image = getAlias(alias);
        if (image != null) byGlyph.put(glyph, image);
    }

    void bindGlyphToFirstAvailableAlias(char glyph, String... aliases) {
        if (aliases == null) return;
        for (String alias : aliases) {
            BufferedImage image = getAlias(alias);
            if (image != null) { byGlyph.put(glyph, image); return; }
        }
    }

    void bindSemanticToAlias(String semanticId, String alias) {
        BufferedImage image = getAlias(alias);
        if (image != null) putSemantic(semanticId, image);
    }

    void bindSemanticToFirstAvailableAlias(String semanticId, String... aliases) {
        if (aliases == null) return;
        for (String alias : aliases) {
            BufferedImage image = getAlias(alias);
            if (image != null) { putSemantic(semanticId, image); return; }
        }
    }

    void markCorridorArtUsesNorthSouth(char glyph) { northSouthCorridorGlyphs.add(glyph); }
    void clearCorridorArtOrientationFlags() { northSouthCorridorGlyphs.clear(); }
    boolean corridorArtUsesNorthSouth(char glyph) { return northSouthCorridorGlyphs.contains(glyph); }

    Map<Character, BufferedImage> glyphView() { return Collections.unmodifiableMap(byGlyph); }
    Map<String, BufferedImage> aliasView() { return Collections.unmodifiableMap(byAlias); }
    Map<String, BufferedImage> semanticView() { return Collections.unmodifiableMap(bySemantic); }
    Set<Character> northSouthCorridorGlyphView() { return Collections.unmodifiableSet(northSouthCorridorGlyphs); }

    String auditSummary() {
        return "TileImageRegistry{" + "aliases=" + byAlias.size() + ", glyphs=" + byGlyph.size()
                + ", semantic=" + bySemantic.size() + ", northSouthCorridorGlyphs=" + northSouthCorridorGlyphs.size() + '}';
    }

    private static String normalizeAlias(String alias) {
        if (alias == null) return null;
        String key = alias.trim();
        return key.isEmpty() ? null : key.toLowerCase(Locale.ROOT);
    }

    private static String normalizeSemanticId(String semanticId) {
        if (semanticId == null) return null;
        String key = semanticId.trim().toUpperCase(Locale.ROOT);
        return key.isEmpty() ? null : key;
    }

    @Override public String toString() { return auditSummary(); }
    @Override public int hashCode() { return Objects.hash(byGlyph, byAlias, bySemantic, northSouthCorridorGlyphs); }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TileImageRegistry that)) return false;
        return byGlyph.equals(that.byGlyph) && byAlias.equals(that.byAlias)
                && bySemantic.equals(that.bySemantic) && northSouthCorridorGlyphs.equals(that.northSouthCorridorGlyphs);
    }
}
