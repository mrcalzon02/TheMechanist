package mechanist;

/**
 * Registry binding and semantic alias configuration authority.
 *
 * This class establishes rule-based relationships between character glyphs,
 * semantic reference IDs, and text aliases inside a TileImageRegistry.
 *
 * It must not:
 * - execute filesystem I/O or path calculations,
 * - load PNG files or handle BufferedImage bytes directly.
 */
final class GlyphBinder {

    private GlyphBinder() {
    }

    /**
     * Map a character glyph directly to a string alias inside the registry.
     */
    static void bindGlyphToAlias(TileImageRegistry registry, char glyph, String alias) {
        if (registry == null) return;
        registry.bindGlyphToAlias(glyph, alias);
    }

    /**
     * Map a character glyph to the first valid graphical alias found in a priority array.
     */
    static void bindGlyphToFirstAvailableAlias(TileImageRegistry registry, char glyph, String... aliases) {
        if (registry == null || aliases == null) return;
        registry.bindGlyphToFirstAvailableAlias(glyph, aliases);
    }

    /**
     * Link an 8-character semantic asset ID to an image map alias.
     */
    static void bindSemanticToAlias(TileImageRegistry registry, String semanticId, String alias) {
        if (registry == null) return;
        registry.bindSemanticToAlias(semanticId, alias);
    }

    /**
     * Link an 8-character semantic asset ID to the first matching image map alias.
     */
    static void bindSemanticToFirstAvailableAlias(TileImageRegistry registry, String semanticId, String... aliases) {
        if (registry == null || aliases == null) return;
        registry.bindSemanticToFirstAvailableAlias(semanticId, aliases);
    }

    /**
     * Flag specific corridor or road layout glyphs that require rigid North-South orientation logic.
     */
    static void markCorridorArtUsesNorthSouth(TileImageRegistry registry, char glyph) {
        if (registry == null) return;
        registry.markCorridorArtUsesNorthSouth(glyph);
    }

    /**
     * Establish the default foundational engine layout maps for standard underhive rendering.
     */
    static void applyDefaultEngineBindings(TileImageRegistry registry) {
        if (registry == null) return;

        // Register default corridor rendering rules
        markCorridorArtUsesNorthSouth(registry, '|');
        markCorridorArtUsesNorthSouth(registry, '║');
        
        // Semantic map linkages can be declared or loaded dynamically here
    }
}
