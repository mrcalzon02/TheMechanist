package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Stage 5 semantic bridge between compiled tile descriptors and the registry.
 *
 * Generation is still allowed to decide that a tile is a road, wall, corridor,
 * fixture, or door. This authority only translates the already-compiled art
 * alias into a stable eight-character semantic asset ID so Zone Audit,
 * Infopedia, and the renderer can stop treating raw path aliases as meaning.
 */
final class TileSemanticAssetAuthority {
    static final String VERSION = "0.9.10jx";
    private static final LinkedHashMap<String, String> ALIAS_TO_ID = new LinkedHashMap<>();

    static {
        bindFloorPanels();
        bindNobleFloors();
        bindRoads();
        bindCorridors();
        bindWalls();
        bindDoors();
        bindFixturesAndOverlays();
    }

    private TileSemanticAssetAuthority() {}

    static Optional<String> assetIdForAlias(String alias) {
        if (alias == null || alias.isBlank()) return Optional.empty();
        String key = normalizeAlias(alias);
        String direct = ALIAS_TO_ID.get(key);
        if (direct != null) return Optional.of(direct);
        if (key.matches("road_.*_v[1-5]")) {
            String base = key.replaceAll("_v[1-5]$", "");
            String baseId = ALIAS_TO_ID.get(base);
            if (baseId != null) return Optional.of(baseId);
        }
        if (key.matches("wall_.*_v[1-5]")) {
            String base = key.replaceAll("_v[1-5]$", "");
            String baseId = ALIAS_TO_ID.get(base);
            if (baseId != null) return Optional.of(baseId);
        }
        if (key.matches("floor_.*_v[1-5]")) {
            String base = key.replaceAll("_v[1-5]$", "");
            String baseId = ALIAS_TO_ID.get(base);
            if (baseId != null) return Optional.of(baseId);
        }
        return Optional.empty();
    }

    static String assetIdOrMissing(String alias) {
        return assetIdForAlias(alias).orElse(null);
    }

    static Optional<AssetMetadata> metadataForAlias(String alias) {
        return assetIdForAlias(alias).flatMap(AssetManager::metadata);
    }

    static String semanticSummaryForAlias(String alias) {
        Optional<String> id = assetIdForAlias(alias);
        if (id.isEmpty()) return "assetId=unmapped alias=" + alias;
        Optional<AssetMetadata> meta = AssetManager.metadata(id.get());
        if (meta.isEmpty()) return "assetId=" + id.get() + " registry=missing alias=" + alias;
        AssetMetadata m = meta.get();
        return "assetId=" + m.id() + " type=" + m.type().displayName() + " name=" + m.name();
    }

    static Map<String, String> auditAliasMap() {
        return Map.copyOf(ALIAS_TO_ID);
    }

    static int mappingCount() {
        return ALIAS_TO_ID.size();
    }

    private static void bindFloorPanels() {
        for (int col = 1; col <= 5; col++) {
            bind("floor_bare_underhive_v" + col, "FLR-01" + two(col));
            bind("floor_industrial_room_v" + col, "FLR-02" + two(col));
            bind("floor_sewer_room_v" + col, "FLR-03" + two(col));
            bind("floor_trash_mutant_rough_v" + col, "FLR-04" + two(col));
            bind("floor_alleyway_cracked_v" + col, "FLR-05" + two(col));
        }
        bind("floor_bare_underhive", "FLR-0101");
        bind("floor_industrial_room", "FLR-0201");
        bind("floor_sewer_room", "FLR-0301");
        bind("floor_trash_mutant_rough", "FLR-0401");
        bind("floor_alleyway_cracked", "FLR-0501");
        bind("void_space", "FLR-0505");
    }

    private static void bindNobleFloors() {
        for (int col = 1; col <= 5; col++) {
            bind("floor_noble_room_v" + col, "POS-01" + two(col));
            bind("floor_noble_room_alt_v" + col, "POS-02" + two(col));
        }
        bind("floor_noble_room", "POS-0101");
        bind("floor_noble_corridor_north_south", "POS-0402");
        bind("floor_noble_corridor_east_west", "POS-0402");
        bind("floor_noble_corridor_junction", "POS-0303");
        bind("floor_noble_corridor_local", "POS-0101");
    }

    private static void bindRoads() {
        bind("road_north_south", "ROAD-N01");
        bind("road_east_west", "ROAD-E01");
        bind("road_sidewalk", "SIDE-A01");
        for (int col = 1; col <= 5; col++) {
            bind("road_north_south_v" + col, col == 1 ? "ROAD-N01" : "ROD-01" + two(col));
            bind("tile_road_north_south_v" + col, col == 1 ? "ROAD-N01" : "ROD-01" + two(col));
            bind("road_east_west_v" + col, col == 1 ? "ROAD-E01" : "ROD-02" + two(col));
            bind("tile_road_east_west_v" + col, col == 1 ? "ROAD-E01" : "ROD-02" + two(col));
            bind("road_intersection_v" + col, "ROD-04" + two(col));
            bind("tile_road_intersection_v" + col, "ROD-04" + two(col));
            bind("road_sidewalk_v" + col, col == 1 ? "SIDE-A01" : "ROD-05" + two(col));
            bind("tile_road_sidewalk_v" + col, col == 1 ? "SIDE-A01" : "ROD-05" + two(col));
        }
        bind("road_intersection", "ROD-0401");
        bind("tile_road_intersection", "ROD-0401");
        bind("tile_road_sidewalk", "SIDE-A01");
        bind("road_corner_west_north", "ROD-0301");
        bind("road_corner_west_south", "ROD-0302");
        bind("road_corner_east_north", "ROD-0303");
        bind("road_corner_east_south", "ROD-0304");
        bind("road_corner", "ROD-0301");
        bind("tile_road_corner", "ROD-0301");
        bind("road_round", "ROD-0305");
        bind("tile_road_round", "ROD-0305");
        bind("road_end_north", "ROD-0305");
        bind("road_end_south", "ROD-0305");
        bind("road_end_east", "ROD-0305");
        bind("road_end_west", "ROD-0305");
    }

    private static void bindCorridors() {
        bind("floor_industrial_corridor", "CORR-A01");
        bind("floor_maintenance_corridor", "CRB-0203");
        bind("floor_padded_service_way", "CRB-0404");
        bind("floor_exterior_hivewall_maintenance", "CORR-M01");
        bind("floor_exterior_maintenance_corridor_north_south", "CRB-0505");
        bind("floor_exterior_maintenance_corridor_east_west", "CRB-0505");
        bind("floor_exterior_maintenance_corridor_junction", "CRB-0405");
        bind("floor_exterior_maintenance_corridor_local", "CORR-M01");
        bind("floor_sewer_pipe_corridor", "CORR-S01");
        bind("floor_sewer_corridor_north_south", "CRA-0303");
        bind("floor_sewer_corridor_east_west", "CRA-0303");
        bind("floor_sewer_corridor_junction", "CRA-0305");
        bind("floor_sewer_corridor_intersection", "CRA-0305");
        bind("floor_sewer_corridor_local", "CORR-S01");
    }

    private static void bindWalls() {
        bind("wall_bulkhead", "WALL-A01");
        for (int col = 1; col <= 5; col++) bind("wall_bulkhead_v" + col, col == 1 ? "WALL-A01" : "WAL-01" + two(col));
        bind("wall_exterior_maintenance_bulkhead", "WALL-M01");
        bind("wall_exterior_maintenance_bulkhead_v1", "WALL-M01");
        bind("wall_exterior_maintenance_bulkhead_v2", "WAL-0405");
        bind("wall_exterior_maintenance_bulkhead_v3", "WAL-0305");
        bind("wall_noble_bulkhead", "WAL-0102");
        bind("wall_noble_bulkhead_v1", "WAL-0102");
        bind("wall_noble_bulkhead_v2", "WAL-0103");
        bind("wall_noble_bulkhead_v3", "WAL-0105");
        bind("wall_noble_bulkhead_v4", "WAL-0303");
        bind("wall_noble_bulkhead_v5", "WAL-0404");
        bind("wall_sewer_bulkhead", "WALL-S01");
        bind("wall_sewer_bulkhead_v1", "WALL-S01");
        bind("wall_sewer_bulkhead_v2", "WAL-0301");
        bind("wall_sewer_bulkhead_v3", "WAL-0402");
        bind("wall_sewer_bulkhead_v4", "WAL-0503");
        bind("wall_sewer_bulkhead_v5", "WAL-0205");
        bind("wall_support_beam", "WAL-0104");
        bind("wall_gantry_lattice", "WAL-0301");
        bind("wall_buried_conveyor", "WAL-0205");
        bind("wall_pipe_bundle", "WALL-S01");
        bind("wall_cable_column", "WAL-0502");
    }

    private static void bindDoors() {
        bind("door_archway", "DOR-0001");
        bind("door_standard", "DOR-0002");
        bind("door_locked", "DOR-0003");
        bind("door_vent_panel", "DOR-0004");
        bind("door_security", "DOR-0005");
        bind("door_double", "DOR-0006");
    }

    private static void bindFixturesAndOverlays() {
        bind("water_barrel", "OBJ-WB01");
        bind("sleeping_cot", "OBJ-CT01");
        bind("supply_post", "OBJ-SH01");
        bind("storage_crate", "SHF-0101");
        bind("table_prop", "DOM-0501");
        bind("corpse_loot", "CRPS-A01");
        bind("water_condenser", "MACH-C01");
        bind("emergency_assembler", "MACH-A01");
        bind("emergency_boiler", "MACH-B01");
        bind("emergency_smelter", "MACH-F01");
        bind("steam_engine", "MAC-0501");
        bind("steam_engine_disabled", "MAC-0502");
        bind("micro_lab", "MAC-0403");
        bind("emergency_miner", "MAC-0304");
        bind("relay_power_grid", "MAC-0105");
        bind("scrap_workbench", "MACH-A01");
        bind("security_cogitator", "MAC-0505");
        bind("turret_or_trade", "SHF-0305");
        bind("logistics_center", "SHF-0102");
        bind("medicae_or_military", "MAC-0401");
        bind("clinic", "MAC-0402");
        bind("carrying_station", "SHF-0401");
        bind("donation_box", "SHF-0502");
        bind("imperial_shrine", "MAC-0103");
        bind("shrine_or_shield", "MAC-0103");
        bind("sewer_hatch", "WAL-0401");
        bind("ladder_drain", "WAL-0501");
        bind("elevator", "DOR-0005");
        bind("object_generic", "ITEM-G01");
        bind("sump_fungus_mold", "ITEM-G01");
        bind("barricade", "ITEM-G01");
        bind("debris", "ITEM-G01");
        bind("buried_cache", "ITEM-G01");
        bind("danger_marker", "MAC-0101");
        bind("rogue_machine", "MAC-0303");
        bind("noisy_machinery", "MAC-0404");
        bind("vending_food", "SHF-0101");
        bind("vending_armor", "SHF-0201");
        bind("vending_weapons", "SHF-0302");
        bind("vending_materials", "SHF-0402");
        bind("vending_survival", "SHF-0503");
        bind("arbites_precinct", "DOR-0005");
        bind("bandit_den", "ITEM-G01");
        bind("hiver_block", "SIDE-A01");
        bind("noble_secure", "POS-0304");
        bind("governor_dais", "POS-0104");
        bind("saint_alcove", "DOR-0001");
    }

    private static void bind(String alias, String assetId) {
        if (alias == null || alias.isBlank() || assetId == null || assetId.isBlank()) return;
        ALIAS_TO_ID.put(normalizeAlias(alias), assetId.trim().toUpperCase(Locale.ROOT));
    }

    private static String normalizeAlias(String alias) {
        return alias.trim().toLowerCase(Locale.ROOT);
    }

    private static String two(int value) {
        return String.format(Locale.ROOT, "%02d", value);
    }
}
