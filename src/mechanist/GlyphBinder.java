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
     * Map a renderer-facing logical alias to the first valid physical atlas cell alias.
     */
    static void bindAliasToFirstAvailableAlias(TileImageRegistry registry, String targetAlias, String... sourceAliases) {
        if (registry == null || sourceAliases == null) return;
        registry.bindAliasToFirstAvailableAlias(targetAlias, sourceAliases);
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

        applyCompiledAtlasLogicalBindings(registry);

        bindGlyphToFirstAvailableAlias(registry, '#',
                "wall_bulkhead_v3", "bulkhead_walls_doors_r03c03_32px", "bulkhead_bulkhead_walls_doors_r03c03_32px", "bulkhead walls doors r03c03 32px",
                "bulkhead_walls_doors_r03c02_32px", "bulkhead_bulkhead_walls_doors_r03c02_32px", "wall", "bulkhead");
        bindGlyphToFirstAvailableAlias(registry, '.',
                "floor_bare_underhive_v3", "corridors_a_r03c03_32px", "corridors_r03c03_32px", "corridors_corridors_r03c03_32px", "floor", "corridor", "corridors");
        bindGlyphToFirstAvailableAlias(registry, ',',
                "floor_alleyway_cracked_v2", "corridors_a_r03c02_32px", "corridors_r03c02_32px", "corridors_corridors_r03c02_32px", "floor", "corridor", "corridors");
        bindGlyphToFirstAvailableAlias(registry, ':',
                "floor_padded_service_way", "corridorsb_r03c03_32px", "corridors_corridorsb_r03c03_32px", "corridor", "corridors");
        bindGlyphToFirstAvailableAlias(registry, ';',
                "road_north_south_v3", "roads_r03c03_32px", "roads_roads_r03c03_32px", "road", "roads", "corridors_a_r03c03_32px");
        bindGlyphToFirstAvailableAlias(registry, '_',
                "road_sidewalk_v3", "roads_r03c02_32px", "roads_roads_r03c02_32px", "sidewalk", "roads", "corridors_a_r03c02_32px");
        bindGlyphToFirstAvailableAlias(registry, '+',
                "door_standard", "bulkhead_walls_doors_r03c04_32px", "bulkhead_bulkhead_walls_doors_r03c04_32px", "door", "bulkhead");
        bindGlyphToFirstAvailableAlias(registry, 'D',
                "door_double", "bulkhead_walls_doors_r03c04_32px", "bulkhead_bulkhead_walls_doors_r03c04_32px", "door", "bulkhead");
        bindGlyphToFirstAvailableAlias(registry, '/',
                "door_archway", "bulkhead_walls_doors_open_r03c04_32px", "bulkhead_bulkhead_walls_doors_open_r03c04_32px", "door_open", "bulkhead");
        bindGlyphToFirstAvailableAlias(registry, '\\',
                "door_archway", "bulkhead_walls_doors_open_r03c02_32px", "bulkhead_bulkhead_walls_doors_open_r03c02_32px", "door_open", "bulkhead");
        bindGlyphToFirstAvailableAlias(registry, '~',
                "floor_sewer_corridor_north_south", "corridorsb_r04c03_32px", "corridors_corridorsb_r04c03_32px", "water", "sewer", "corridors");
        bindGlyphToFirstAvailableAlias(registry, '=',
                "floor_maintenance_corridor", "defenses_r03c03_32px", "defenses_defenses_r03c03_32px", "defense", "corridors_a_r03c03_32px");
        bindGlyphToFirstAvailableAlias(registry, 'E',
                "elevator", "defenses_r03c04_32px", "defenses_defenses_r03c04_32px", "terminal", "defense");
        bindGlyphToFirstAvailableAlias(registry, 'S',
                "sewer_hatch", "objects_r03c03_32px", "objects_objects_r03c03_32px", "object", "objects");
        bindGlyphToFirstAvailableAlias(registry, 'v',
                "ladder_drain", "objects_r03c02_32px", "objects_objects_r03c02_32px", "object", "objects");

        // Register default corridor rendering rules
        markCorridorArtUsesNorthSouth(registry, '|');
        markCorridorArtUsesNorthSouth(registry, '║');

        DebugLog.audit("GLYPH_BINDER", "Applied generated-tile alias bindings: " + registry.auditSummary());
    }

    private static void applyCompiledAtlasLogicalBindings(TileImageRegistry registry) {
        for (int v = 1; v <= 5; v++) {
            bindAtlasCell(registry, "floor_bare_underhive_v" + v, "floor_panels", 1, v);
            bindAtlasCell(registry, "floor_industrial_room_v" + v, "factory_floors", 1, v);
            bindAtlasCell(registry, "floor_sewer_room_v" + v, "sewer_floors", 1, v);
            bindAtlasCell(registry, "floor_trash_mutant_rough_v" + v, "gangfloors", 1, v);
            bindAtlasCell(registry, "floor_alleyway_cracked_v" + v, "assorted_floors", 1, v);
            bindAtlasCell(registry, "floor_noble_room_v" + v, "posh_floors", 1, v);
            bindAtlasCell(registry, "floor_noble_room_alt_v" + v, "posh_floors", 2, v);

            bindAtlasCell(registry, "wall_bulkhead_v" + v, "bulkhead_walls_doors", 3, v);
            bindAtlasCell(registry, "wall_noble_bulkhead_v" + v, "noble_and_noble_security_walls", 3, v);
            bindAtlasCell(registry, "wall_sewer_bulkhead_v" + v, "sewerwalls", 3, v);

            bindAtlasCell(registry, "road_north_south_v" + v, "n_s_e_w_r_i_s", v, 1);
            bindAtlasCell(registry, "road_east_west_v" + v, "n_s_e_w_r_i_s", v, 2);
            bindAtlasCell(registry, "road_round_v" + v, "n_s_e_w_r_i_s", v, 3);
            bindAtlasCell(registry, "road_intersection_v" + v, "n_s_e_w_r_i_s", v, 4);
            bindAtlasCell(registry, "road_sidewalk_v" + v, "n_s_e_w_r_i_s", v, 5);
            bindAtlasCell(registry, "tile_road_north_south_v" + v, "n_s_e_w_r_i_s", v, 1);
            bindAtlasCell(registry, "tile_road_east_west_v" + v, "n_s_e_w_r_i_s", v, 2);
            bindAtlasCell(registry, "tile_road_intersection_v" + v, "n_s_e_w_r_i_s", v, 4);
            bindAtlasCell(registry, "tile_road_sidewalk_v" + v, "n_s_e_w_r_i_s", v, 5);
        }

        bindAliasToFirstAvailableAlias(registry, "floor_bare_underhive", "floor_bare_underhive_v1");
        bindAliasToFirstAvailableAlias(registry, "floor_industrial_room", "floor_industrial_room_v1");
        bindAliasToFirstAvailableAlias(registry, "floor_sewer_room", "floor_sewer_room_v1");
        bindAliasToFirstAvailableAlias(registry, "floor_trash_mutant_rough", "floor_trash_mutant_rough_v1");
        bindAliasToFirstAvailableAlias(registry, "floor_alleyway_cracked", "floor_alleyway_cracked_v1");
        bindAliasToFirstAvailableAlias(registry, "floor_noble_room", "floor_noble_room_v1");
        bindAtlasCell(registry, "void_space", "level5_darkcity", 3, 3);

        bindAtlasCell(registry, "floor_industrial_corridor", "corridors_a", 3, 3);
        bindAtlasCell(registry, "floor_maintenance_corridor", "corridorsb", 3, 3);
        bindAtlasCell(registry, "floor_padded_service_way", "corridorsb", 4, 4);
        bindAtlasCell(registry, "floor_exterior_hivewall_maintenance", "corridorsb", 5, 5);
        bindAtlasCell(registry, "floor_exterior_maintenance_corridor_north_south", "corridorsb", 5, 1);
        bindAtlasCell(registry, "floor_exterior_maintenance_corridor_east_west", "corridorsb", 5, 2);
        bindAtlasCell(registry, "floor_exterior_maintenance_corridor_junction", "corridorsb", 5, 4);
        bindAliasToFirstAvailableAlias(registry, "floor_exterior_maintenance_corridor_local", "floor_exterior_hivewall_maintenance");
        bindAtlasCell(registry, "floor_sewer_pipe_corridor", "sewer_floors", 3, 3);
        bindAtlasCell(registry, "floor_sewer_corridor_north_south", "sewer_floors", 3, 1);
        bindAtlasCell(registry, "floor_sewer_corridor_east_west", "sewer_floors", 3, 2);
        bindAtlasCell(registry, "floor_sewer_corridor_junction", "sewer_floors", 3, 4);
        bindAliasToFirstAvailableAlias(registry, "floor_sewer_corridor_intersection", "floor_sewer_corridor_junction");
        bindAliasToFirstAvailableAlias(registry, "floor_sewer_corridor_local", "floor_sewer_pipe_corridor");
        bindAtlasCell(registry, "floor_noble_corridor_north_south", "posh_floors", 4, 2);
        bindAtlasCell(registry, "floor_noble_corridor_east_west", "posh_floors", 4, 3);
        bindAtlasCell(registry, "floor_noble_corridor_junction", "posh_floors", 4, 4);
        bindAliasToFirstAvailableAlias(registry, "floor_noble_corridor_local", "floor_noble_room_v1");

        bindAliasToFirstAvailableAlias(registry, "road_north_south", "road_north_south_v1");
        bindAliasToFirstAvailableAlias(registry, "road_east_west", "road_east_west_v1");
        bindAliasToFirstAvailableAlias(registry, "road_intersection", "road_intersection_v1");
        bindAliasToFirstAvailableAlias(registry, "road_sidewalk", "road_sidewalk_v1");
        bindAliasToFirstAvailableAlias(registry, "tile_road_intersection", "road_intersection_v1");
        bindAliasToFirstAvailableAlias(registry, "tile_road_sidewalk", "road_sidewalk_v1");
        bindAliasToFirstAvailableAlias(registry, "road_corner_west_north", "road_round_v1");
        bindAliasToFirstAvailableAlias(registry, "road_corner_west_south", "road_round_v1");
        bindAliasToFirstAvailableAlias(registry, "road_corner_east_north", "road_round_v1");
        bindAliasToFirstAvailableAlias(registry, "road_corner_east_south", "road_round_v1");
        bindAliasToFirstAvailableAlias(registry, "road_corner", "road_round_v1");
        bindAliasToFirstAvailableAlias(registry, "tile_road_corner", "road_round_v1");
        bindAliasToFirstAvailableAlias(registry, "road_round", "road_round_v1");
        bindAliasToFirstAvailableAlias(registry, "tile_road_round", "road_round_v1");
        bindAliasToFirstAvailableAlias(registry, "road_end_north", "road_round_v1");
        bindAliasToFirstAvailableAlias(registry, "road_end_south", "road_round_v1");
        bindAliasToFirstAvailableAlias(registry, "road_end_east", "road_round_v1");
        bindAliasToFirstAvailableAlias(registry, "road_end_west", "road_round_v1");

        bindAliasToFirstAvailableAlias(registry, "wall_bulkhead", "wall_bulkhead_v1");
        bindAliasToFirstAvailableAlias(registry, "wall_noble_bulkhead", "wall_noble_bulkhead_v1");
        bindAliasToFirstAvailableAlias(registry, "wall_sewer_bulkhead", "wall_sewer_bulkhead_v1");
        bindAtlasCell(registry, "wall_exterior_maintenance_bulkhead_v1", "interwall", 3, 1);
        bindAtlasCell(registry, "wall_exterior_maintenance_bulkhead_v2", "interwall", 3, 2);
        bindAtlasCell(registry, "wall_exterior_maintenance_bulkhead_v3", "interwall", 3, 3);
        bindAliasToFirstAvailableAlias(registry, "wall_exterior_maintenance_bulkhead", "wall_exterior_maintenance_bulkhead_v1");
        bindAtlasCell(registry, "wall_support_beam", "walls1", 4, 2);
        bindAtlasCell(registry, "wall_gantry_lattice", "walls1", 4, 3);
        bindAtlasCell(registry, "wall_buried_conveyor", "walls2", 2, 5);
        bindAtlasCell(registry, "wall_pipe_bundle", "sewerwalls", 2, 5);
        bindAtlasCell(registry, "wall_cable_column", "interwall", 5, 2);

        bindAtlasCell(registry, "door_archway", "bulkhead_walls_doors_open", 3, 4);
        bindAtlasCell(registry, "door_standard", "doors_c", 3, 3);
        bindAtlasCell(registry, "door_locked", "doors_c", 3, 4);
        bindAtlasCell(registry, "door_vent_panel", "doors_c", 2, 2);
        bindAtlasCell(registry, "door_security", "doors_c", 4, 4);
        bindAtlasCell(registry, "door_double", "bulkhead_walls_doors", 3, 4);

        bindAtlasCell(registry, "water_barrel", "domestic", 2, 5);
        bindAtlasCell(registry, "sleeping_cot", "domestic", 1, 2);
        bindAtlasCell(registry, "supply_post", "service_items", 2, 5);
        bindAtlasCell(registry, "storage_crate", "generic_items", 1, 1);
        bindAtlasCell(registry, "table_prop", "tables", 3, 3);
        bindAtlasCell(registry, "corpse_loot", "loot_bodies", 3, 3);
        bindAtlasCell(registry, "water_condenser", "emm_01", 1, 1);
        bindAtlasCell(registry, "emergency_assembler", "emm_02", 1, 1);
        bindAtlasCell(registry, "emergency_boiler", "emm_03", 1, 1);
        bindAtlasCell(registry, "emergency_smelter", "emm_04", 1, 1);
        bindAtlasCell(registry, "micro_lab", "labschem", 3, 3);
        bindAtlasCell(registry, "emergency_miner", "emm_05", 1, 1);
        bindAtlasCell(registry, "relay_power_grid", "machinery", 1, 5);
        bindAtlasCell(registry, "steam_engine", "machinery", 5, 1);
        bindAtlasCell(registry, "steam_engine_disabled", "machinery", 5, 2);
        bindAtlasCell(registry, "scrap_workbench", "machinery", 2, 2);
        bindAtlasCell(registry, "security_cogitator", "frameless_system_icons", 1, 5);
        bindAtlasCell(registry, "turret_or_trade", "defenses", 3, 3);
        bindAtlasCell(registry, "shrine_or_shield", "temple_assets", 1, 3);
        bindAtlasCell(registry, "logistics_center", "service_items", 1, 2);
        bindAtlasCell(registry, "medicae_or_military", "medical_items_and_drugs", 4, 1);
        bindAtlasCell(registry, "carrying_station", "service_items", 4, 1);
        bindAtlasCell(registry, "imperial_shrine", "temple_assets", 1, 3);
        bindAtlasCell(registry, "donation_box", "goods", 5, 2);
        bindAtlasCell(registry, "clinic", "medical_items_and_drugs", 4, 2);
        bindAtlasCell(registry, "barricade", "defenses", 2, 1);
        bindAtlasCell(registry, "debris", "junk_rgba", 3, 3);
        bindAtlasCell(registry, "buried_cache", "paperwork", 2, 2);
        bindAtlasCell(registry, "danger_marker", "frameless_system_icons", 2, 5);
        bindAtlasCell(registry, "rogue_machine", "machinery", 3, 3);
        bindAtlasCell(registry, "noisy_machinery", "machinery", 4, 4);
        bindAtlasCell(registry, "vending_food", "vending", 1, 1);
        bindAtlasCell(registry, "vending_armor", "vending", 1, 2);
        bindAtlasCell(registry, "vending_weapons", "vending", 1, 3);
        bindAtlasCell(registry, "vending_materials", "vending", 1, 4);
        bindAtlasCell(registry, "vending_survival", "vending", 1, 5);
        bindAtlasCell(registry, "arbites_precinct", "precinct_defenses", 3, 3);
        bindAtlasCell(registry, "bandit_den", "gangers", 3, 3);
        bindAtlasCell(registry, "hiver_block", "hiver_clothing", 3, 3);
        bindAtlasCell(registry, "noble_secure", "nobles", 3, 3);
        bindAtlasCell(registry, "object_generic", "generic_items", 3, 3);
        bindAtlasCell(registry, "sump_fungus_mold", "flesh_cult", 3, 3);
        bindAtlasCell(registry, "sewer_hatch", "stairs_stairs_elevator_platforms_hatches_and_ladders", 1, 4);
        bindAtlasCell(registry, "ladder_drain", "stairs_stairs_elevator_platforms_hatches_and_ladders", 1, 5);
        bindAtlasCell(registry, "elevator", "stairs_stairs_elevator_platforms_hatches_and_ladders", 1, 3);
        bindAliasToFirstAvailableAlias(registry, "alarm_trap", "danger_marker");
        bindAliasToFirstAvailableAlias(registry, "saint_alcove", "door_archway");
        bindAliasToFirstAvailableAlias(registry, "governor_dais", "floor_noble_room_v4");

        applyCompiledAtlasSemanticBindings(registry);
    }

    private static void applyCompiledAtlasSemanticBindings(TileImageRegistry registry) {
        for (int v = 1; v <= 5; v++) {
            bindSemantic(registry, "FLR-01" + two(v), "floor_bare_underhive_v" + v);
            bindSemantic(registry, "FLR-02" + two(v), "floor_industrial_room_v" + v);
            bindSemantic(registry, "FLR-03" + two(v), "floor_sewer_room_v" + v);
            bindSemantic(registry, "FLR-04" + two(v), "floor_trash_mutant_rough_v" + v);
            bindSemantic(registry, "FLR-05" + two(v), v == 5 ? "void_space" : "floor_alleyway_cracked_v" + v);
            bindSemantic(registry, "POS-01" + two(v), "floor_noble_room_v" + v);
            bindSemantic(registry, "POS-02" + two(v), "floor_noble_room_alt_v" + v);
            bindSemantic(registry, "ROD-01" + two(v), "road_north_south_v" + v);
            bindSemantic(registry, "ROD-02" + two(v), "road_east_west_v" + v);
            bindSemantic(registry, "ROD-03" + two(v), "road_round_v" + v);
            bindSemantic(registry, "ROD-04" + two(v), "road_intersection_v" + v);
            bindSemantic(registry, "ROD-05" + two(v), "road_sidewalk_v" + v);
        }

        bindSemantic(registry, "ROAD-N01", "road_north_south");
        bindSemantic(registry, "ROAD-E01", "road_east_west");
        bindSemantic(registry, "SIDE-A01", "road_sidewalk");
        bindSemantic(registry, "POS-0303", "floor_noble_corridor_junction");
        bindSemantic(registry, "POS-0304", "noble_secure");
        bindSemantic(registry, "POS-0402", "floor_noble_corridor_north_south", "floor_noble_corridor_east_west");
        bindSemantic(registry, "CORR-A01", "floor_industrial_corridor");
        bindSemantic(registry, "CORR-M01", "floor_exterior_hivewall_maintenance");
        bindSemantic(registry, "CORR-S01", "floor_sewer_pipe_corridor");
        bindSemantic(registry, "CRA-0303", "floor_sewer_corridor_north_south", "floor_sewer_corridor_east_west");
        bindSemantic(registry, "CRA-0305", "floor_sewer_corridor_junction");
        bindSemantic(registry, "CRB-0203", "floor_maintenance_corridor");
        bindSemantic(registry, "CRB-0404", "floor_padded_service_way");
        bindSemantic(registry, "CRB-0405", "floor_exterior_maintenance_corridor_junction");
        bindSemantic(registry, "CRB-0505", "floor_exterior_maintenance_corridor_north_south", "floor_exterior_maintenance_corridor_east_west");

        bindSemantic(registry, "WALL-A01", "wall_bulkhead");
        bindSemantic(registry, "WALL-M01", "wall_exterior_maintenance_bulkhead");
        bindSemantic(registry, "WALL-S01", "wall_sewer_bulkhead");
        bindSemantic(registry, "WAL-0102", "wall_noble_bulkhead_v1");
        bindSemantic(registry, "WAL-0103", "wall_noble_bulkhead_v2");
        bindSemantic(registry, "WAL-0104", "wall_support_beam");
        bindSemantic(registry, "WAL-0105", "wall_noble_bulkhead_v3");
        bindSemantic(registry, "WAL-0205", "wall_buried_conveyor");
        bindSemantic(registry, "WAL-0301", "wall_gantry_lattice");
        bindSemantic(registry, "WAL-0303", "wall_noble_bulkhead_v4");
        bindSemantic(registry, "WAL-0401", "sewer_hatch");
        bindSemantic(registry, "WAL-0402", "wall_sewer_bulkhead_v3");
        bindSemantic(registry, "WAL-0404", "wall_noble_bulkhead_v5");
        bindSemantic(registry, "WAL-0405", "wall_exterior_maintenance_bulkhead_v2");
        bindSemantic(registry, "WAL-0501", "ladder_drain");
        bindSemantic(registry, "WAL-0502", "wall_cable_column");
        bindSemantic(registry, "WAL-0503", "wall_sewer_bulkhead_v4");
        bindSemantic(registry, "DOR-0001", "door_archway");
        bindSemantic(registry, "DOR-0002", "door_standard");
        bindSemantic(registry, "DOR-0003", "door_locked");
        bindSemantic(registry, "DOR-0004", "door_vent_panel");
        bindSemantic(registry, "DOR-0005", "door_security", "elevator");
        bindSemantic(registry, "DOR-0006", "door_double");

        bindSemantic(registry, "OBJ-WB01", "water_barrel");
        bindSemantic(registry, "OBJ-WD01", "water_barrel");
        bindSemantic(registry, "OBJ-SH01", "supply_post", "storage_crate");
        bindSemantic(registry, "OBJ-CT01", "sleeping_cot");
        bindSemantic(registry, "OBJ-BD01", "sleeping_cot");
        bindSemantic(registry, "DOM-0102", "sleeping_cot");
        bindSemantic(registry, "DOM-0105", "sleeping_cot");
        bindSemantic(registry, "DOM-0205", "water_barrel");
        bindSemantic(registry, "DOM-0302", "storage_crate");
        bindSemantic(registry, "DOM-0303", "storage_crate");
        bindSemantic(registry, "DOM-0401", "water_barrel");
        bindSemantic(registry, "DOM-0402", "table_prop");
        bindSemantic(registry, "DOM-0501", "table_prop");
        bindSemantic(registry, "DOM-0503", "table_prop");
        bindSemantic(registry, "MACH-C01", "water_condenser");
        bindSemantic(registry, "MACH-A01", "scrap_workbench", "emergency_assembler");
        bindSemantic(registry, "MACH-B01", "emergency_boiler");
        bindSemantic(registry, "MACH-F01", "emergency_smelter");
        bindSemantic(registry, "MAC-0101", "danger_marker");
        bindSemantic(registry, "MAC-0103", "imperial_shrine");
        bindSemantic(registry, "MAC-0105", "relay_power_grid");
        bindSemantic(registry, "MAC-0205", "sump_fungus_mold");
        bindSemantic(registry, "MAC-0302", "micro_lab");
        bindSemantic(registry, "MAC-0303", "rogue_machine");
        bindSemantic(registry, "MAC-0304", "emergency_miner");
        bindSemantic(registry, "MAC-0305", "micro_lab");
        bindSemantic(registry, "MAC-0401", "medicae_or_military");
        bindSemantic(registry, "MAC-0402", "clinic");
        bindSemantic(registry, "MAC-0403", "micro_lab");
        bindSemantic(registry, "MAC-0404", "noisy_machinery");
        bindSemantic(registry, "MAC-0501", "steam_engine");
        bindSemantic(registry, "MAC-0502", "steam_engine_disabled");
        bindSemantic(registry, "MAC-0505", "security_cogitator");

        bindSemantic(registry, "SHF-0101", "vending_food", "storage_crate");
        bindSemantic(registry, "SHF-0102", "logistics_center");
        bindSemantic(registry, "SHF-0201", "vending_armor");
        bindSemantic(registry, "SHF-0205", "supply_post");
        bindSemantic(registry, "SHF-0302", "vending_weapons");
        bindSemantic(registry, "SHF-0305", "turret_or_trade");
        bindSemantic(registry, "SHF-0401", "carrying_station");
        bindSemantic(registry, "SHF-0402", "vending_materials");
        bindSemantic(registry, "SHF-0502", "donation_box");
        bindSemantic(registry, "SHF-0503", "vending_survival");
        bindSemantic(registry, "BLD-0001", "storage_crate");
        bindSemantic(registry, "BLD-0002", "scrap_workbench");
        bindSemantic(registry, "BLD-0003", "barricade");
        bindSemantic(registry, "BLD-0004", "alarm_trap");
        bindSemantic(registry, "BLD-0005", "supply_post", "storage_crate");
        bindSemantic(registry, "BLD-0006", "barricade");
        bindSemantic(registry, "BLD-0007", "danger_marker");
        bindSemantic(registry, "BLD-0008", "turret_or_trade");
        bindSemantic(registry, "BLD-0009", "turret_or_trade");
        bindSemantic(registry, "BLD-0010", "turret_or_trade");
        bindSemantic(registry, "BLD-0011", "turret_or_trade");
        bindSemantic(registry, "BLD-0012", "turret_or_trade");
        bindSemantic(registry, "BLD-0013", "micro_lab");
        bindSemantic(registry, "BLD-0014", "micro_lab");
        bindSemantic(registry, "FTR-0001", "danger_marker");
        bindSemantic(registry, "FTR-0003", "danger_marker");
        bindSemantic(registry, "FTR-0004", "security_cogitator");
        bindSemantic(registry, "FTR-0005", "danger_marker");
        bindSemantic(registry, "FTR-0006", "danger_marker");
        bindSemantic(registry, "FTR-0007", "clinic");
        bindSemantic(registry, "FTR-0008", "security_cogitator");
        bindSemantic(registry, "FTR-0011", "supply_post");

        bindSemantic(registry, "ITEM-G01", "object_generic", "storage_crate");
        bindSemantic(registry, "ITEM-N01", "buried_cache");
        bindSemantic(registry, "WEAP-K01", "vending_weapons");
        bindSemantic(registry, "WEAP-B01", "vending_weapons");
        bindSemantic(registry, "WP1-0202", "vending_weapons");
        bindSemantic(registry, "WP1-0204", "vending_weapons");
        bindSemantic(registry, "WP1-0205", "vending_weapons");
        bindSemantic(registry, "WP1-0302", "vending_weapons");
        bindSemantic(registry, "WP1-0402", "vending_weapons");
        bindSemantic(registry, "WP2-0105", "vending_weapons");
        bindSemantic(registry, "WP2-0202", "vending_weapons");
        bindSemantic(registry, "WP3-0101", "vending_weapons");
        bindSemantic(registry, "WP3-0102", "vending_weapons");
        bindSemantic(registry, "WP3-0103", "vending_weapons");
        bindSemantic(registry, "WP3-0104", "vending_weapons");
        bindSemantic(registry, "WP3-0105", "vending_weapons");
        bindSemantic(registry, "WP3-0201", "vending_weapons");
        bindSemantic(registry, "WP3-0202", "vending_weapons");
        bindSemantic(registry, "WP3-0203", "vending_weapons");
        bindSemantic(registry, "WP3-0204", "vending_weapons");
        bindSemantic(registry, "WP3-0205", "vending_weapons");
        bindSemantic(registry, "WP3-0302", "hiver_block");
        bindSemantic(registry, "WP3-0304", "vending_armor");
        bindSemantic(registry, "WP3-0305", "hiver_block");
        bindSemantic(registry, "ARMR-A01", "vending_armor");
    }

    private static void bindAtlasCell(TileImageRegistry registry, String targetAlias, String atlasAlias, int row, int col) {
        bindAliasToFirstAvailableAlias(registry, targetAlias,
                atlasCell(atlasAlias, row, col, 32),
                atlasCell(atlasAlias, row, col, 64),
                atlasCell(atlasAlias, row, col, 128),
                atlasCell(atlasAlias, row, col, 256));
    }

    private static void bindSemantic(TileImageRegistry registry, String semanticId, String... aliases) {
        bindSemanticToFirstAvailableAlias(registry, semanticId, aliases);
    }

    private static String atlasCell(String atlasAlias, int row, int col, int resolution) {
        return String.format(java.util.Locale.ROOT, "%s_r%02dc%02d_%dpx", atlasAlias, row, col, resolution);
    }

    private static String two(int value) {
        return String.format(java.util.Locale.ROOT, "%02d", value);
    }
}
