package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Stage 5 semantic bridge between compiled tile descriptors and the registry.
 *
 * Generation is still allowed to decide that a tile is a road, wall, corridor,
 * fixture, or door. This authority translates the already-compiled art alias
 * into an asset ID that actually exists in the active runtime registry. Legacy
 * hand-authored IDs remain useful hints, but they are never returned unless the
 * loaded registry contains a semantically compatible entry.
 */
final class TileSemanticAssetAuthority {
    static final String VERSION = "0.9.10ka-runtime-registry";
    private static final LinkedHashMap<String, String> ALIAS_TO_ID = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Optional<String>> RUNTIME_ID_CACHE = new LinkedHashMap<>();
    private static final Set<String> TOKEN_STOPWORDS = Set.of(
            "tile", "floor", "wall", "road", "room", "prop", "object",
            "generic", "standard", "alt", "or", "the", "of",
            "v1", "v2", "v3", "v4", "v5"
    );

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
        String mapped = mappedIdForKey(key);
        if (mapped == null) return Optional.empty();
        return runtimeAssetId(key, mapped);
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

    static Map<String, String> auditResolvedAliasMap() {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        for (String alias : ALIAS_TO_ID.keySet()) {
            assetIdForAlias(alias).ifPresent(id -> out.put(alias, id));
        }
        return Map.copyOf(out);
    }

    static int mappingCount() {
        return ALIAS_TO_ID.size();
    }

    static int resolvedMappingCount() {
        return auditResolvedAliasMap().size();
    }

    private static String mappedIdForKey(String key) {
        String direct = ALIAS_TO_ID.get(key);
        if (direct != null) return direct;
        if (key.matches("(road|wall|floor)_.*_v[1-5]")) {
            return ALIAS_TO_ID.get(key.replaceAll("_v[1-5]$", ""));
        }
        return null;
    }

    private static Optional<String> runtimeAssetId(String key, String mappedId) {
        synchronized (RUNTIME_ID_CACHE) {
            if (RUNTIME_ID_CACHE.containsKey(key)) return RUNTIME_ID_CACHE.get(key);
        }

        Optional<String> resolved = compatibleMetadata(mappedId, key).map(AssetMetadata::id);
        if (resolved.isEmpty()) {
            for (String translated : translatedIdCandidates(mappedId)) {
                resolved = compatibleMetadata(translated, key).map(AssetMetadata::id);
                if (resolved.isPresent()) break;
            }
        }
        if (resolved.isEmpty()) resolved = findRuntimeRegistryMatch(key);

        synchronized (RUNTIME_ID_CACHE) {
            RUNTIME_ID_CACHE.put(key, resolved);
        }
        return resolved;
    }

    private static Optional<AssetMetadata> compatibleMetadata(String assetId, String key) {
        if (assetId == null || assetId.isBlank()) return Optional.empty();
        return AssetManager.metadata(assetId).filter(asset -> compatible(asset, key));
    }

    private static List<String> translatedIdCandidates(String mappedId) {
        if (mappedId == null || mappedId.isBlank()) return List.of();
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        translatePrefix(ids, mappedId, "FLR-", "FLO-");
        translatePrefix(ids, mappedId, "WALL-", "WAL-");
        translatePrefix(ids, mappedId, "ROAD-", "ROA-");
        translatePrefix(ids, mappedId, "ROD-", "ROA-");
        translatePrefix(ids, mappedId, "SIDE-", "SID-");
        translatePrefix(ids, mappedId, "CORR-", "COR-");
        translatePrefix(ids, mappedId, "MACH-", "MAC-");
        translatePrefix(ids, mappedId, "ITEM-", "ITE-");
        return List.copyOf(ids);
    }

    private static void translatePrefix(Set<String> out, String value, String oldPrefix, String newPrefix) {
        if (value.startsWith(oldPrefix)) out.add(newPrefix + value.substring(oldPrefix.length()));
    }

    private static Optional<String> findRuntimeRegistryMatch(String key) {
        ArrayList<Candidate> candidates = new ArrayList<>();
        for (AssetMetadata asset : AssetManager.registry().all()) {
            if (!compatible(asset, key)) continue;
            int score = semanticScore(asset, key);
            if (score >= 12) candidates.add(new Candidate(asset, score));
        }
        if (candidates.isEmpty()) return Optional.empty();

        candidates.sort(Comparator.comparingInt(Candidate::score).reversed()
                .thenComparing(candidate -> candidate.asset().name(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(candidate -> candidate.asset().id()));

        int best = candidates.get(0).score();
        ArrayList<Candidate> top = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate.score() < best - 2 || top.size() >= 25) break;
            top.add(candidate);
        }
        int variant = variantNumber(key);
        int index = variant <= 1 ? 0 : Math.floorMod(variant - 1, top.size());
        return Optional.of(top.get(index).asset().id());
    }

    private static boolean compatible(AssetMetadata asset, String key) {
        if (asset == null || key == null || key.isBlank()) return false;
        if (!expectedTypes(key).contains(asset.type())) return false;

        String h = haystack(asset);
        for (List<String> concept : requiredConceptGroups(key)) {
            if (!containsAny(h, concept)) return false;
        }

        if (key.startsWith("door_") && !key.contains("archway") && containsAny(h, List.of("door open", "doors open", "opened"))) {
            return false;
        }
        if (key.contains("sewer") && containsAny(h, List.of("noble", "luxury", "palace"))) return false;
        if (key.contains("noble") && containsAny(h, List.of("sewer", "sump", "trash", "slum"))) return false;
        if (key.contains("industrial") && containsAny(h, List.of("noble", "luxury", "sewer"))) return false;
        return true;
    }

    private static Set<AssetType> expectedTypes(String key) {
        if (key.startsWith("floor_") && key.contains("corridor")) {
            return Set.of(AssetType.CORRIDOR_TILE, AssetType.FLOOR_TILE);
        }
        if (key.startsWith("floor_") || "void_space".equals(key)) {
            return Set.of(AssetType.FLOOR_TILE);
        }
        if (key.startsWith("wall_")) {
            return Set.of(AssetType.WALL_TILE);
        }
        if (key.contains("sidewalk") || "hiver_block".equals(key)) {
            return Set.of(AssetType.SIDEWALK_TILE, AssetType.ROAD_TILE, AssetType.FLOOR_TILE);
        }
        if (key.startsWith("road_") || key.startsWith("tile_road_")) {
            return Set.of(AssetType.ROAD_TILE, AssetType.SIDEWALK_TILE);
        }
        if (key.startsWith("door_") || key.contains("hatch") || "elevator".equals(key)
                || "arbites_precinct".equals(key) || "saint_alcove".equals(key)) {
            return Set.of(AssetType.FIXTURE, AssetType.WALL_TILE, AssetType.CORRIDOR_TILE, AssetType.FLOOR_TILE);
        }
        if (key.contains("corpse")) {
            return Set.of(AssetType.CORPSE_DECAY, AssetType.OBJECT, AssetType.ITEM_ICON);
        }
        if (containsAny(key, List.of("engine", "machine", "assembler", "boiler", "smelter",
                "condenser", "lab", "grid", "cogitator", "clinic", "shrine", "turret"))) {
            return Set.of(AssetType.MACHINE, AssetType.FIXTURE, AssetType.OBJECT);
        }
        return Set.of(AssetType.OBJECT, AssetType.FIXTURE, AssetType.MACHINE,
                AssetType.ITEM_ICON, AssetType.ARMOR_ICON, AssetType.WEAPON_ICON,
                AssetType.WALL_TILE, AssetType.FLOOR_TILE, AssetType.SIDEWALK_TILE);
    }

    private static List<List<String>> requiredConceptGroups(String key) {
        ArrayList<List<String>> groups = new ArrayList<>();

        if (key.startsWith("floor_") && key.contains("corridor")) {
            groups.add(List.of("corridor", "walkway", "service way", "utility tunnel"));
        } else if (key.startsWith("floor_")) {
            groups.add(List.of("floor", "floors", "ground", "surface"));
        } else if ("void_space".equals(key)) {
            groups.add(List.of("void", "space"));
        } else if (key.startsWith("wall_")) {
            groups.add(List.of("wall", "walls", "bulkhead", "barrier", "structure",
                    "beam", "lattice", "column", "pipe", "conveyor"));
        } else if (key.contains("sidewalk")) {
            groups.add(List.of("sidewalk", "pavement"));
        } else if (key.contains("parking")) {
            groups.add(List.of("parking", "curb"));
        } else if (key.startsWith("road_") || key.startsWith("tile_road_")) {
            groups.add(List.of("road", "street"));
        } else if (key.startsWith("door_")) {
            groups.add(List.of("door", "doors", "entry", "hatch"));
        }

        addConcept(groups, key, "bare_underhive", "underhive", "bare", "floor panels");
        addConcept(groups, key, "industrial", "industrial", "factory", "workshop", "machine shop");
        addConcept(groups, key, "sewer", "sewer", "sump", "drain");
        if (containsAny(key, List.of("trash", "mutant", "rough"))) {
            groups.add(List.of("trash", "mutant", "rough", "scrap", "slum", "waste"));
        }
        if (containsAny(key, List.of("alleyway", "cracked"))) {
            groups.add(List.of("alley", "alleyway", "cracked"));
        }
        addConcept(groups, key, "noble", "noble", "luxury", "estate", "manor", "palace");
        if (key.contains("maintenance")) {
            groups.add(List.of("maintenance", "service", "utility", "exterior"));
        }
        addConcept(groups, key, "padded", "padded");
        if (key.contains("bulkhead") && !containsAny(key, List.of("sewer", "noble", "maintenance", "exterior"))) {
            groups.add(List.of("bulkhead"));
        }
        addConcept(groups, key, "support_beam", "support beam", "beam");
        addConcept(groups, key, "gantry_lattice", "gantry", "lattice");
        addConcept(groups, key, "buried_conveyor", "conveyor");
        addConcept(groups, key, "pipe_bundle", "pipe", "pipes");
        addConcept(groups, key, "cable_column", "cable", "column");
        addConcept(groups, key, "door_archway", "open", "archway");
        addConcept(groups, key, "door_locked", "locked", "lock");
        addConcept(groups, key, "door_vent_panel", "vent");
        addConcept(groups, key, "door_security", "security", "reinforced", "armored", "armoured");
        addConcept(groups, key, "door_double", "double");

        if (groups.isEmpty()) {
            List<String> tokens = aliasTokens(key);
            if (!tokens.isEmpty()) groups.add(tokens);
        }
        return List.copyOf(groups);
    }

    private static void addConcept(List<List<String>> groups, String key, String marker, String... alternatives) {
        if (key.contains(marker)) groups.add(List.of(alternatives));
    }

    private static int semanticScore(AssetMetadata asset, String key) {
        String h = haystack(asset);
        int score = 0;
        for (List<String> group : requiredConceptGroups(key)) {
            if (containsAny(h, group)) score += 12;
        }
        for (String token : aliasTokens(key)) {
            if (h.contains(token)) score += token.length() >= 6 ? 6 : 4;
        }
        String phrase = normalizeText(key.replaceAll("_v[1-5]$", "").replace('_', ' '));
        if (!phrase.isBlank() && h.contains(phrase)) score += 20;
        return score;
    }

    private static List<String> aliasTokens(String key) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        String normalized = normalizeText(key.replace('_', ' '));
        for (String token : normalized.split(" ")) {
            if (token.isBlank() || TOKEN_STOPWORDS.contains(token)) continue;
            tokens.add(token);
        }
        return List.copyOf(tokens);
    }

    private static String haystack(AssetMetadata asset) {
        return normalizeText(asset.id() + " " + asset.name() + " " + asset.pathOrUri()
                + " " + asset.type().displayName() + " " + asset.semanticDescription());
    }

    private static boolean containsAny(String text, List<String> alternatives) {
        if (text == null || text.isBlank()) return false;
        String normalized = normalizeText(text);
        for (String alternative : alternatives) {
            String needle = normalizeText(alternative);
            if (!needle.isBlank() && normalized.contains(needle)) return true;
        }
        return false;
    }

    private static int variantNumber(String key) {
        int marker = key.lastIndexOf("_v");
        if (marker < 0 || marker + 2 >= key.length()) return 1;
        try {
            return Math.max(1, Integer.parseInt(key.substring(marker + 2)));
        } catch (NumberFormatException ignored) {
            return 1;
        }
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
        bind("road_parking", "PRK-0101");
        for (int col = 1; col <= 5; col++) {
            bind("road_north_south_v" + col, col == 1 ? "ROAD-N01" : "ROD-01" + two(col));
            bind("tile_road_north_south_v" + col, col == 1 ? "ROAD-N01" : "ROD-01" + two(col));
            bind("road_east_west_v" + col, col == 1 ? "ROAD-E01" : "ROD-02" + two(col));
            bind("tile_road_east_west_v" + col, col == 1 ? "ROAD-E01" : "ROD-02" + two(col));
            bind("road_intersection_v" + col, "ROD-04" + two(col));
            bind("tile_road_intersection_v" + col, "ROD-04" + two(col));
            bind("road_sidewalk_v" + col, col == 1 ? "SIDE-A01" : "ROD-05" + two(col));
            bind("tile_road_sidewalk_v" + col, col == 1 ? "SIDE-A01" : "ROD-05" + two(col));
            bind("road_parking_v" + col, "PRK-01" + two(col));
            bind("tile_road_parking_v" + col, "PRK-01" + two(col));
        }
        bind("road_intersection", "ROD-0401");
        bind("tile_road_intersection", "ROD-0401");
        bind("tile_road_sidewalk", "SIDE-A01");
        bind("tile_road_parking", "PRK-0101");
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

    private static String normalizeText(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("[^a-z0-9+./ ]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String two(int value) {
        return String.format(Locale.ROOT, "%02d", value);
    }

    private record Candidate(AssetMetadata asset, int score) {}
}
