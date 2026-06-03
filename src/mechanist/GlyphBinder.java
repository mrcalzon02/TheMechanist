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

        bindGlyphToFirstAvailableAlias(registry, '#',
                "bulkhead_walls_doors_r03c03_32px", "bulkhead_bulkhead_walls_doors_r03c03_32px", "bulkhead walls doors r03c03 32px",
                "bulkhead_walls_doors_r03c02_32px", "bulkhead_bulkhead_walls_doors_r03c02_32px", "wall", "bulkhead");
        bindGlyphToFirstAvailableAlias(registry, '.',
                "corridors_r03c03_32px", "corridors_corridors_r03c03_32px", "floor", "corridor", "corridors");
        bindGlyphToFirstAvailableAlias(registry, ',',
                "corridors_r03c02_32px", "corridors_corridors_r03c02_32px", "floor", "corridor", "corridors");
        bindGlyphToFirstAvailableAlias(registry, ':',
                "corridorsb_r03c03_32px", "corridors_corridorsb_r03c03_32px", "corridor", "corridors");
        bindGlyphToFirstAvailableAlias(registry, ';',
                "roads_r03c03_32px", "roads_roads_r03c03_32px", "road", "roads", "corridors_r03c03_32px");
        bindGlyphToFirstAvailableAlias(registry, '_',
                "roads_r03c02_32px", "roads_roads_r03c02_32px", "sidewalk", "roads", "corridors_r03c02_32px");
        bindGlyphToFirstAvailableAlias(registry, '+',
                "bulkhead_walls_doors_r03c04_32px", "bulkhead_bulkhead_walls_doors_r03c04_32px", "door", "bulkhead");
        bindGlyphToFirstAvailableAlias(registry, 'D',
                "bulkhead_walls_doors_r03c04_32px", "bulkhead_bulkhead_walls_doors_r03c04_32px", "door", "bulkhead");
        bindGlyphToFirstAvailableAlias(registry, '/',
                "bulkhead_walls_doors_open_r03c04_32px", "bulkhead_bulkhead_walls_doors_open_r03c04_32px", "door_open", "bulkhead");
        bindGlyphToFirstAvailableAlias(registry, '\\',
                "bulkhead_walls_doors_open_r03c02_32px", "bulkhead_bulkhead_walls_doors_open_r03c02_32px", "door_open", "bulkhead");
        bindGlyphToFirstAvailableAlias(registry, '~',
                "corridorsb_r04c03_32px", "corridors_corridorsb_r04c03_32px", "water", "sewer", "corridors");
        bindGlyphToFirstAvailableAlias(registry, '=',
                "defenses_r03c03_32px", "defenses_defenses_r03c03_32px", "defense", "corridors_r03c03_32px");
        bindGlyphToFirstAvailableAlias(registry, 'E',
                "defenses_r03c04_32px", "defenses_defenses_r03c04_32px", "terminal", "defense");
        bindGlyphToFirstAvailableAlias(registry, 'S',
                "objects_r03c03_32px", "objects_objects_r03c03_32px", "object", "objects");
        bindGlyphToFirstAvailableAlias(registry, 'v',
                "objects_r03c02_32px", "objects_objects_r03c02_32px", "object", "objects");

        // Register default corridor rendering rules
        markCorridorArtUsesNorthSouth(registry, '|');
        markCorridorArtUsesNorthSouth(registry, '║');

        DebugLog.audit("GLYPH_BINDER", "Applied generated-tile alias bindings: " + registry.auditSummary());
    }
}
