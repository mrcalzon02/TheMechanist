package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Locale;
import mechanist.assets.AssetManager;
import mechanist.assets.AssetType;

class PortraitSheetProfile {
    String label, path;
    int width, height, cellW, cellH, cols, rows, total, offsetX, offsetY;
    boolean playerAllowed;
    PortraitSheetProfile(String label, String path, int width, int height, int cellW, int cellH, int cols, int rows, int offsetX, int offsetY, boolean playerAllowed) {
        this.label=label; this.path=path; this.width=width; this.height=height; this.cellW=cellW; this.cellH=cellH; this.cols=cols; this.rows=rows; this.offsetX=offsetX; this.offsetY=offsetY; this.total=cols*rows; this.playerAllowed=playerAllowed;
    }
    static PortraitSheetProfile infer(String label, String path, int width, int height, boolean playerAllowed) {
        int[] candidates = new int[]{64, 96, 112, 120, 124, 128, 160};
        int bestW=128, bestH=128, bestCols=Math.max(1,width/128), bestRows=Math.max(1,height/128), bestWaste=Integer.MAX_VALUE;
        for (int cw: candidates) for (int ch: candidates) {
            int cols = width / cw, rows = height / ch;
            if (cols < 1 || rows < 1) continue;
            int waste = (width - cols*cw) + (height - rows*ch);
            int cells = cols*rows;
            int score = waste + Math.abs(cw-ch)*2 + Math.abs(80-cells);
            if (score < bestWaste) { bestWaste=score; bestW=cw; bestH=ch; bestCols=cols; bestRows=rows; }
        }
        // For the normal human sheet we deliberately rely on pre-sliced verified cells.
        // Inference here is diagnostic only, used for logs and reserved NPC sheet correction.
        int ox = Math.max(0, (width - bestCols*bestW)/2);
        int oy = Math.max(0, (height - bestRows*bestH)/2);
        return new PortraitSheetProfile(label, path, width, height, bestW, bestH, bestCols, bestRows, ox, oy, playerAllowed);
    }
    String toAuditLine() {
        return label + " path=" + path + " image=" + width + "x" + height + " cell=" + cellW + "x" + cellH + " cols=" + cols + " rows=" + rows + " total=" + total + " offset=" + offsetX + "," + offsetY + " playerAllowed=" + playerAllowed;
    }
}

class TileArtSystem {
    private final TileImageRegistry imageRegistry = new TileImageRegistry();
    String loadedQualityFolder = "low_32";

    void load(String rootPath) { load(rootPath, 0); }

    void load(String rootPath, int qualityIndex) {
        imageRegistry.clear();
        String resolvedRootPath = ArtPackManager.resolveInstalledArtRoot(rootPath);
        File root = new File(resolvedRootPath);
        if (!root.exists()) {
            DebugLog.warn("TILE_ART", "Tile art root does not exist. requestedRoot=" + rootPath + " resolvedRoot=" + resolvedRootPath + " runtime=" + RuntimePathResolver.workingDirectorySummary());
            return;
        }
        loadedQualityFolder = qualityFolderFor(qualityIndex);
        String cellRoot = qualityCellRoot(resolvedRootPath, loadedQualityFolder);
        int cellPngCount = ArtPackManager.countPngFiles(Paths.get(cellRoot));
        DebugLog.audit("TILE_ART", "tile root requested=" + rootPath + " resolved=" + resolvedRootPath + " quality=" + loadedQualityFolder + " cellRoot=" + cellRoot + " cellRootExists=" + Files.isDirectory(Paths.get(cellRoot)) + " pngCount=" + cellPngCount);
        try {
            bindFloorVariantAliases(cellRoot);
            bindSpecializedTerrainAliases(cellRoot);
            putAlias("floor_bare_underhive", cellRoot + "/floors/floor_panels/r1c1.png");
            putAlias("floor_trash_mutant_rough", cellRoot + "/floors/floor_panels/r4c2.png");
            putAlias("floor_sewer_room", cellRoot + "/floors/floor_panels/r3c4.png");
            putAlias("floor_industrial_corridor", cellRoot + "/Corridors/corridors_a/r1c2.png");
            putAlias("floor_maintenance_corridor", cellRoot + "/Corridors/corridorsb/r2c3.png");
            putAlias("floor_alleyway_cracked", cellRoot + "/floors/floor_panels/r5c1.png");
            putAlias("floor_sewer_pipe_corridor", cellRoot + "/Corridors/corridors_a/r3c1.png");
            putAlias("floor_padded_service_way", cellRoot + "/Corridors/corridorsb/r4c4.png");
            putAlias("floor_exterior_hivewall_maintenance", cellRoot + "/Corridors/corridorsb/r1c5.png");
            putAlias("void_space", cellRoot + "/floors/floor_panels/r5c5.png");
            putAlias("wall_bulkhead", cellRoot + "/Walls/walls/r1c1.png");
            putAlias("wall_exterior_maintenance_bulkhead", cellRoot + "/Walls/walls/r1c2.png");
            putAlias("wall_support_beam", cellRoot + "/Walls/walls/r1c4.png");
            putAlias("wall_gantry_lattice", cellRoot + "/Walls/walls/r3c1.png");
            putAlias("wall_buried_conveyor", cellRoot + "/Walls/walls/r2c5.png");
            putAlias("wall_pipe_bundle", cellRoot + "/Walls/walls/r2c1.png");
            putAlias("wall_cable_column", cellRoot + "/Walls/walls/r5c2.png");
            putAlias("door_archway", cellRoot + "/Doors/doors_o/doors_o_r1c1.png");
            putAlias("door_standard", cellRoot + "/Doors/coors_c/coors_c_r2c1.png");
            putAlias("door_locked", cellRoot + "/Doors/coors_c/coors_c_r1c5.png");
            putAlias("door_vent_panel", cellRoot + "/Doors/coors_c/coors_c_r4c4.png");
            putAlias("door_security", cellRoot + "/Doors/doors_o/doors_o_r5c5.png");
            putAlias("door_double", cellRoot + "/Doors/coors_c/coors_c_r2c5.png");
            putAlias("barricade", cellRoot + "/Tile_entities/junk/junk_r5c4.png");
            putAlias("debris", cellRoot + "/Tile_entities/junk/junk_r1c1.png");
            putAlias("buried_cache", cellRoot + "/Tile_entities/junk/junk_r3c2.png");
            putAlias("danger_marker", cellRoot + "/Tile_entities/machinery/machinery_r1c1.png");
            putAlias("rogue_machine", cellRoot + "/Tile_entities/machinery/machinery_r3c3.png");
            putAlias("noisy_machinery", cellRoot + "/Tile_entities/machinery/machinery_r4c4.png");
            putAlias("vending_food", cellRoot + "/Tile_entities/vending/vending_r1c1.png");
            putAlias("vending_armor", cellRoot + "/Tile_entities/vending/vending_r2c1.png");
            putAlias("vending_weapons", cellRoot + "/Tile_entities/vending/vending_r3c2.png");
            putAlias("vending_materials", cellRoot + "/Tile_entities/vending/vending_r4c2.png");
            putAlias("vending_survival", cellRoot + "/Tile_entities/vending/vending_r5c3.png");
            putAlias("water_condenser", cellRoot + "/Tile_entities/machinery/machinery_r2c1.png");
            putAlias("emergency_assembler", cellRoot + "/Tile_entities/machinery/machinery_r2c2.png");
            putAlias("emergency_boiler", cellRoot + "/Tile_entities/machinery/machinery_r2c3.png");
            putAlias("micro_lab", cellRoot + "/Tile_entities/labschem/labschem_r2c3.png");
            putAlias("emergency_miner", cellRoot + "/Tile_entities/machinery/machinery_r3c4.png");
            putAlias("relay_power_grid", cellRoot + "/Tile_entities/machinery/machinery_r1c5.png");
            putAlias("emergency_smelter", cellRoot + "/Tile_entities/machinery/machinery_r1c4.png");
            putAlias("steam_engine", cellRoot + "/Tile_entities/machinery/machinery_r5c1.png");
            putAlias("steam_engine_disabled", cellRoot + "/Tile_entities/machinery/machinery_r5c2.png");
            putAlias("scrap_workbench", cellRoot + "/Tile_entities/labschem/labschem_r1c1.png");
            putAlias("security_cogitator", cellRoot + "/Tile_entities/labschem/labschem_r4c4.png");
            putAlias("turret_or_trade", cellRoot + "/Tile_entities/shops_storefronts_counters/shops_storefronts_counters_r3c5.png");
            putAlias("shrine_or_shield", cellRoot + "/Tile_entities/machinery/machinery_r1c3.png");
            putAlias("logistics_center", cellRoot + "/Tile_entities/shops_storefronts_counters/shops_storefronts_counters_r1c2.png");
            putAlias("medicae_or_military", cellRoot + "/Tile_entities/labschem/labschem_r3c1.png");
            putAlias("carrying_station", cellRoot + "/Tile_entities/shops_storefronts_counters/shops_storefronts_counters_r4c1.png");
            putAlias("supply_post", cellRoot + "/Tile_entities/shops_storefronts_counters/shops_storefronts_counters_r2c4.png");
            putAlias("imperial_shrine", cellRoot + "/Tile_entities/machinery/machinery_r1c3.png");
            putAlias("donation_box", cellRoot + "/Tile_entities/shops_storefronts_counters/shops_storefronts_counters_r5c2.png");
            putAlias("saint_alcove", cellRoot + "/Doors/coors_c/coors_c_r1c1.png");
            putAlias("governor_dais", cellRoot + "/floors/posh_floors/posh_floors_r1c4.png");
            putAlias("clinic", cellRoot + "/Tile_entities/labschem/labschem_r3c2.png");
            putAlias("corpse_loot", cellRoot + "/Tile_entities/deceased_loot_container/deceased_loot_container_r3c3.png");
            putAlias("object_generic", cellRoot + "/Tile_entities/junk/junk_r2c2.png");
            putAlias("sump_fungus_mold", cellRoot + "/GraphicalUpgradeBase3/animal_pens_cloning_vats_algae_tank/animal_pens_cloning_vats_algae_tank_r4c4.png");
            putAlias("sewer_hatch", cellRoot + "/Walls/walls/walls_r4c1.png");
            putAlias("ladder_drain", cellRoot + "/Walls/walls/walls_r5c1.png");
            putAlias("elevator", cellRoot + "/Doors/doors_o/doors_o_r4c2.png");
            putAlias("storage_crate", cellRoot + "/Tile_entities/shops_storefronts_counters/shops_storefronts_counters_r1c1.png");
            putAlias("sleeping_cot", cellRoot + "/Tile_entities/deceased_loot_container/deceased_loot_container_r1c2.png");
            putAlias("water_barrel", cellRoot + "/Tile_entities/junk/junk_r1c5.png");
            putAlias("alarm_trap", cellRoot + "/Tile_entities/machinery/machinery_r5c5.png");
            putAlias("arbites_precinct", cellRoot + "/Doors/coors_c/coors_c_r3c4.png");
            putAlias("bandit_den", cellRoot + "/Tile_entities/junk/junk_r5c5.png");
            putAlias("hiver_block", cellRoot + "/floors/floor_panels/r3c5.png");
            putAlias("noble_secure", cellRoot + "/floors/posh_floors/r3c4.png");
            putAlias("table_prop", cellRoot + "/Tile_entities/shops_storefronts_counters/shops_storefronts_counters_r1c4.png");
            // Graphical Upgrade Base 3 atlas cells override older glyph aliases.
            // These are presentation bindings; semantic icons below handle build/entity-specific art.
            putAlias("clinic", cellRoot + "/GraphicalUpgradeBase3/medicae/medicae_r1c2.png");
            putAlias("medicae_or_military", cellRoot + "/GraphicalUpgradeBase3/medicae/medicae_r1c2.png");
            putAlias("storage_crate", cellRoot + "/GraphicalUpgradeBase3/goods/goods_r4c1.png");
            putAlias("water_barrel", cellRoot + "/AssetPack6/domestic/domestic_r2c4.png");
            putAlias("table_prop", cellRoot + "/GraphicalUpgradeBase3/tables/tables_r2c2.png");
            putAlias("sleeping_cot", cellRoot + "/AssetPack6/domestic/domestic_r1c1.png");
            putAlias("object_generic", cellRoot + "/GraphicalUpgradeBase3/public_service_items/public_service_items_r2c2.png");
            putAlias("hiver_block", cellRoot + "/GraphicalUpgradeBase3/roads/r5c1.png");
            bindRoadFamilyAliases(cellRoot + "/GraphicalUpgradeBase3/roads");
            bindItemIconAliases(cellRoot);
            putAlias("vehicle_car", cellRoot + "/GraphicalUpgradeBase3/vehicles/vehicles_r1c1.png");
            putAlias("vehicle_truck", cellRoot + "/GraphicalUpgradeBase3/vehicles/vehicles_r2c2.png");
            putAlias("vehicle_bike", cellRoot + "/GraphicalUpgradeBase3/vehicles/vehicles_r3c1.png");
            putAlias("vehicle_armored_car", cellRoot + "/GraphicalUpgradeBase3/vehicles/vehicles_r4c3.png");
            putAlias("vehicle_tank", cellRoot + "/GraphicalUpgradeBase3/vehicles/vehicles_r5c5.png");
            loadSemanticIcons(resolvedRootPath, loadedQualityFolder);
            bindGlyphs();
            DebugLog.audit("TILE_ART", "loaded quality=" + loadedQualityFolder + " aliases=" + imageRegistry.aliasCount() + " glyphs=" + imageRegistry.glyphCount() + " semantic=" + imageRegistry.semanticCount());
        } catch (Throwable t) {
            DebugLog.error("TILE_ART", "Tile art loading failed; ASCII fallback remains active.", t);
        }
    }

    static String qualityFolderFor(int qualityIndex) {
        switch(Math.max(0, Math.min(3, qualityIndex))) {
            case 0: return "low_32";
            case 2: return "intermediate_128";
            case 3: return "high_native";
            default: return "standard_64";
        }
    }

    String qualityCellRoot(String rootPath, String folder) {
        return ArtPackManager.resolveQualityCellsRoot(rootPath, folder);
    }

    void putAlias(String key, String path) throws IOException {
        File f = resolveRuntimeCellFile(path);
        if (f == null || !f.exists()) { DebugLog.warn("TILE_ART", "Missing tile alias " + key + " path=" + path); return; }
        BufferedImage img = ImageIO.read(f);
        imageRegistry.putAlias(key, img);
    }

    void putAliasRotated(String key, String path, int quarterTurns) throws IOException {
        File f = resolveRuntimeCellFile(path);
        if (f == null || !f.exists()) { DebugLog.warn("TILE_ART", "Missing rotated tile alias " + key + " path=" + path); return; }
        BufferedImage img = ImageIO.read(f);
        if (img == null) return;
        imageRegistry.putAlias(key, rotateQuarterTurns(img, quarterTurns));
    }

    static BufferedImage rotateQuarterTurns(BufferedImage img, int quarterTurns) {
        int turns = Math.floorMod(quarterTurns, 4);
        if (turns == 0) return img;
        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(turns % 2 == 0 ? w : h, turns % 2 == 0 ? h : w, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int argb = img.getRGB(x, y);
            if (turns == 1) out.setRGB(h - 1 - y, x, argb);
            else if (turns == 2) out.setRGB(w - 1 - x, h - 1 - y, argb);
            else out.setRGB(y, w - 1 - x, argb);
        }
        return out;
    }

    File resolveRuntimeCellFile(String path) {
        if (path == null || path.isBlank()) return null;
        String p = path.replace('\\', '/');
        File resolved = RuntimePathResolver.resolveAssetFile(p);
        if (resolved.exists()) return resolved;
        if (p.startsWith("assets/")) {
            File packagedClientOwned = new File("packages/client", p);
            if (packagedClientOwned.exists()) return packagedClientOwned;
            File clientOwned = new File("client", p);
            if (clientOwned.exists()) return clientOwned;
        }
        File direct = new File(path);
        if (direct.exists()) return direct;
        ArrayList<String> candidates = new ArrayList<>();
        candidates.add(p);
        candidates.add(p.replace("assets/art/rebase_0_9_06d", "assets/a/r"));
        ArrayList<String> expanded = new ArrayList<>();
        for (String c : candidates) {
            if (c == null || c.isBlank()) continue;
            expanded.add(c);
            expanded.add(c.replace("/roads_ns_ew_round_intersection_straight/", "/roads/"));
        }
        ArrayList<String> finalCandidates = new ArrayList<>();
        for (String c : expanded) {
            finalCandidates.add(c);
            int slash = c.lastIndexOf('/');
            String dir = slash >= 0 ? c.substring(0, slash + 1) : "";
            String file = slash >= 0 ? c.substring(slash + 1) : c;
            String shortFile = file.replaceAll("(?i)^.*_r([0-9]+)c([0-9]+)\\.png$", "r$1c$2.png");
            if (!shortFile.equals(file)) finalCandidates.add(dir + shortFile);
        }
        for (String c : finalCandidates) {
            File f = new File(c);
            if (f.exists()) return f;
        }
        return direct;
    }

    void bindFloorVariantAliases(String cellRoot) throws IOException {
        String floorStem = cellRoot + "/floors/floor_panels/";
        for (int col = 1; col <= 5; col++) putAlias("floor_bare_underhive_v" + col, floorStem + "r1c" + col + ".png");
        for (int col = 1; col <= 5; col++) putAlias("floor_industrial_room_v" + col, floorStem + "r2c" + col + ".png");
        for (int col = 1; col <= 5; col++) putAlias("floor_sewer_room_v" + col, floorStem + "r3c" + col + ".png");
        for (int col = 1; col <= 5; col++) putAlias("floor_trash_mutant_rough_v" + col, floorStem + "r4c" + col + ".png");
        for (int col = 1; col <= 5; col++) putAlias("floor_alleyway_cracked_v" + col, floorStem + "r5c" + col + ".png");
        BufferedImage bare = imageRegistry.getAlias("floor_bare_underhive_v1");
        if (bare != null) imageRegistry.putAlias("floor_bare_underhive", bare);
    }

    void bindSpecializedTerrainAliases(String cellRoot) throws IOException {
        // Use the imported folder architecture as the semantic authority. Sewer
        // corridors, exterior-maintenance void corridors, noble corridors, noble floors,
        // and noble walls are separate art families; they must not all collapse through
        // generic floor/wall fallback aliases.
        String corrA = cellRoot + "/Corridors/corridors_a/";
        String corrB = cellRoot + "/Corridors/corridorsb/";
        String posh = cellRoot + "/floors/posh_floors/";
        String walls = cellRoot + "/Walls/walls/";

        putAlias("floor_sewer_corridor_north_south", corrA + "r3c3.png");
        putAliasRotated("floor_sewer_corridor_east_west", corrA + "r3c3.png", 1);
        putAlias("floor_sewer_corridor_junction", corrA + "r3c5.png");
        putAlias("floor_sewer_corridor_local", corrA + "r3c1.png");
        putAlias("floor_sewer_corridor_intersection", corrA + "r3c5.png");

        putAlias("floor_exterior_maintenance_corridor_north_south", corrB + "r5c5.png");
        putAliasRotated("floor_exterior_maintenance_corridor_east_west", corrB + "r5c5.png", 1);
        putAlias("floor_exterior_maintenance_corridor_junction", corrB + "r4c5.png");
        putAlias("floor_exterior_maintenance_corridor_local", corrB + "r1c5.png");

        putAlias("floor_noble_corridor_north_south", posh + "r4c2.png");
        putAliasRotated("floor_noble_corridor_east_west", posh + "r4c2.png", 1);
        putAlias("floor_noble_corridor_junction", posh + "r3c3.png");
        putAlias("floor_noble_corridor_local", posh + "r1c1.png");
        for (int col = 1; col <= 5; col++) putAlias("floor_noble_room_v" + col, posh + "r1c" + col + ".png");
        for (int col = 1; col <= 5; col++) putAlias("floor_noble_room_alt_v" + col, posh + "r2c" + col + ".png");
        putAlias("floor_noble_room", posh + "r1c1.png");

        putAlias("wall_noble_bulkhead_v1", walls + "r1c2.png");
        putAlias("wall_noble_bulkhead_v2", walls + "r1c3.png");
        putAlias("wall_noble_bulkhead_v3", walls + "r1c5.png");
        putAlias("wall_noble_bulkhead_v4", walls + "r3c3.png");
        putAlias("wall_noble_bulkhead_v5", walls + "r4c4.png");
        putAlias("wall_noble_bulkhead", walls + "r1c2.png");
        putAlias("wall_sewer_bulkhead_v1", walls + "r2c1.png");
        putAlias("wall_sewer_bulkhead_v2", walls + "r3c1.png");
        putAlias("wall_sewer_bulkhead_v3", walls + "r4c2.png");
        putAlias("wall_sewer_bulkhead_v4", walls + "r5c3.png");
        putAlias("wall_sewer_bulkhead_v5", walls + "r2c5.png");
        putAlias("wall_sewer_bulkhead", walls + "r2c1.png");
        for (int col = 1; col <= 5; col++) putAlias("wall_bulkhead_v" + col, walls + "r1c" + col + ".png");
        putAlias("wall_exterior_maintenance_bulkhead_v1", walls + "r5c5.png");
        putAlias("wall_exterior_maintenance_bulkhead_v2", walls + "r4c5.png");
        putAlias("wall_exterior_maintenance_bulkhead_v3", walls + "r3c5.png");
    }

    void bindRoadFamilyAliases(String roadDir) throws IOException {
        // Rows 1,2,4,5 are variant rows. Row 3 is not a random variant row; it contains
        // direction-specific rounded/corner cells. Binding row 3 as five interchangeable
        // variants made every rounded road end face whichever column the hash selected.
        String stem = roadDir + "/";
        bindRoadFamilyRow("road_north_south", stem, 1);
        bindRoadFamilyRow("tile_road_north_south", stem, 1);
        bindRoadFamilyRow("road_east_west", stem, 2);
        bindRoadFamilyRow("tile_road_east_west", stem, 2);
        bindRoadFamilyRow("road_intersection", stem, 4);
        bindRoadFamilyRow("tile_road_intersection", stem, 4);
        bindRoadFamilyRow("road_sidewalk", stem, 5);
        bindRoadFamilyRow("tile_road_sidewalk", stem, 5);
        putAlias("road_corner_west_north", stem + "r3c1.png");
        putAlias("road_corner_west_south", stem + "r3c2.png");
        putAlias("road_corner_east_north", stem + "r3c3.png");
        putAlias("road_corner_east_south", stem + "r3c4.png");
        putAlias("road_corner", stem + "r3c1.png");
        putAlias("road_round", stem + "r3c5.png");
        putAlias("tile_road_corner", stem + "r3c1.png");
        putAlias("tile_road_round", stem + "r3c5.png");
        // Use the fifth rounded cell as the cap source and rotate it for one-neighbor ends.
        putAliasRotated("road_end_east", stem + "r3c5.png", 0);
        putAliasRotated("road_end_south", stem + "r3c5.png", 1);
        putAliasRotated("road_end_west", stem + "r3c5.png", 2);
        putAliasRotated("road_end_north", stem + "r3c5.png", 3);
    }

    void bindRoadFamilyRow(String baseKey, String stem, int row) throws IOException {
        for (int col = 1; col <= 5; col++) {
            putAlias(baseKey + "_v" + col, stem + "r" + row + "c" + col + ".png");
        }
        BufferedImage first = imageRegistry.getAlias(baseKey + "_v1");
        if (first != null) imageRegistry.putAlias(baseKey, first);
    }


    void bindItemIconAliases(String cellRoot) throws IOException {
        // 0.9.10fs: inventory thumbnails must use the named icon art pool instead
        // of collapsing every carried item to the legacy '?' / scrap glyph. These
        // aliases are broad semantic buckets; item names are classified below and
        // resolved through byAlias just like tile/render descriptors.
        String base = cellRoot + "/AssetPack6";
        putAlias("item_icon_generic", base + "/generic_items/generic_items_r1c1.png");
        putAlias("item_icon_scrap", base + "/generic_items/generic_items_r3c2.png");
        putAlias("item_icon_component", base + "/generic_items/generic_items_r2c3.png");
        putAlias("item_icon_tool", base + "/generic_items/generic_items_r2c5.png");
        putAlias("item_icon_food", base + "/generic_items/generic_items_r1c2.png");
        putAlias("item_icon_water", base + "/generic_items/generic_items_r1c5.png");
        putAlias("item_icon_medical", base + "/generic_items/generic_items_r4c3.png");
        putAlias("item_icon_light", base + "/generic_items/generic_items_r4c5.png");
        putAlias("item_icon_chemical", base + "/generic_items/generic_items_r5c2.png");
        putAlias("item_icon_currency", base + "/generic_items/generic_items_r5c5.png");
        putAlias("item_icon_container", base + "/domestic/domestic_r3c3.png");
        putAlias("item_icon_clothing", base + "/weapons_3_newspaper_armors/weapons_3_newspaper_armors_r5c1.png");
        putAlias("item_icon_armor", base + "/weapons_3_newspaper_armors/weapons_3_newspaper_armors_r3c1.png");
        putAlias("item_icon_headgear", base + "/weapons_3_newspaper_armors/weapons_3_newspaper_armors_r4c2.png");
        putAlias("item_icon_newspaper", base + "/weapons_3_newspaper_armors/weapons_3_newspaper_armors_r1c5.png");
        putAlias("item_icon_weapon_knife", base + "/weapons_1/weapons_1_r3c1.png");
        putAlias("item_icon_weapon_sword", base + "/weapons_1/weapons_1_r4c1.png");
        putAlias("item_icon_weapon_axe", base + "/weapons_1/weapons_1_r1c1.png");
        putAlias("item_icon_weapon_blunt", base + "/weapons_1/weapons_1_r2c2.png");
        putAlias("item_icon_weapon_spear", base + "/weapons_1/weapons_1_r4c2.png");
        putAlias("item_icon_weapon_melee", base + "/weapons_1/weapons_1_r3c1.png");
        putAlias("item_icon_weapon_pistol", base + "/weapons_2/weapons_2_r2c2.png");
        putAlias("item_icon_weapon_revolver", base + "/weapons_2/weapons_2_r3c2.png");
        putAlias("item_icon_weapon_shotgun", base + "/weapons_2/weapons_2_r1c1.png");
        putAlias("item_icon_weapon_bolter", base + "/weapons_2/weapons_2_r4c4.png");
        putAlias("item_icon_weapon_las", base + "/weapons_2/weapons_2_r4c5.png");
        putAlias("item_icon_weapon_rifle", base + "/weapons_2/weapons_2_r5c1.png");
        putAlias("item_icon_weapon_heavy", base + "/weapons_2/weapons_2_r4c3.png");
        putAlias("item_icon_grenade", base + "/weapons_2/weapons_2_r5c3.png");
        putAlias("item_icon_ammo", base + "/weapons_2/weapons_2_r4c2.png");
        putAlias("item_icon_book", base + "/domestic/domestic_r2c4.png");
    }

    static String semanticKeyForItemName(String name) {
        if (name == null || name.isBlank()) return "item_icon_generic";
        String s = ItemQuality.stripManufacturingIdentity(ItemQuality.stripQuality(name)).toLowerCase(Locale.ROOT);
        if (s.contains("floor tile:")) return null;
        if (s.matches(".*(ammo|ammunition|magazine|cartridge|round|shell|charge pack|power cell|battery pack).*")) return "item_icon_ammo";
        if (s.matches(".*(grenade|bomb|mine|satchel|detonator|explosive).*")) return "item_icon_grenade";
        if (s.matches(".*(knife|shiv|scalpel|dagger).*")) return "item_icon_weapon_knife";
        if (s.matches(".*(sword|blade|chainblade|chainsword).*")) return "item_icon_weapon_sword";
        if (s.matches(".*(axe|hatchet).*")) return "item_icon_weapon_axe";
        if (s.matches(".*(maul|club|baton|hammer|mace).*")) return "item_icon_weapon_blunt";
        if (s.matches(".*(spear|polearm).*")) return "item_icon_weapon_spear";
        if (s.matches(".*(heavy bolter|heavy flamer|multi-melta|autocannon|lascannon|stubber|flamer|melta|plasma).*")) return "item_icon_weapon_heavy";
        if (s.matches(".*(bolter|bolt pistol).*")) return "item_icon_weapon_bolter";
        if (s.matches(".*(shotgun).*")) return "item_icon_weapon_shotgun";
        if (s.matches(".*(lasgun|laspistol|hellgun|hot-shot|lascannon).*")) return "item_icon_weapon_las";
        if (s.matches(".*(rifle|carbine|autogun|marksman|sniper).*")) return "item_icon_weapon_rifle";
        if (s.matches(".*(revolver).*")) return "item_icon_weapon_revolver";
        if (s.matches(".*(pistol|stub gun|handgun).*")) return "item_icon_weapon_pistol";
        if (s.matches(".*(armor|armour|flak|carapace|vest|mail|plate|shield|pauldron).*")) return "item_icon_armor";
        if (s.matches(".*(helmet|helm|hat|hood|mask|goggles|visor).*")) return "item_icon_headgear";
        if (s.matches(".*(newspaper|pamphlet|book|ledger|journal|manual|dossier|map|scroll|slate|data).*")) return s.contains("newspaper") ? "item_icon_newspaper" : "item_icon_book";
        if (s.matches(".*(water|canteen|bottle|flask|thermos).*")) return "item_icon_water";
        if (s.matches(".*(ration|food|meat|starch|bread|stew|meal|protein|tin|grub|amasec snack).*")) return "item_icon_food";
        if (s.matches(".*(medicae|medkit|bandage|stimm|stim|injector|medicine|salve|suture).*")) return "item_icon_medical";
        if (s.matches(".*(lantern|lamp|flashlight|torch|glow stick|stub light|phosphor|bulb).*")) return "item_icon_light";
        if (s.matches(".*(chemical|reagent|acid|alkali|drug|powder|solvent|oil|fuel).*")) return "item_icon_chemical";
        if (s.matches(".*(coin|scrip|script|throne gelt|credit|chit|currency|cash).*")) return "item_icon_currency";
        if (s.matches(".*(crate|box|pouch|pack|canister|container|bundle|case).*")) return "item_icon_container";
        if (s.matches(".*(clothing|coat|robe|uniform|rags|servant|scavenger|coverall|shirt|pants|boots).*")) return "item_icon_clothing";
        if (s.matches(".*(wire|gear|cog|component|part|plate|rod|spring|pipe|scrap|circuit|tube|metal|ceramic).*")) return s.contains("scrap") ? "item_icon_scrap" : "item_icon_component";
        if (s.matches(".*(tool|wrench|spanner|screwdriver|kit|repair|pick|shovel|crowbar).*")) return "item_icon_tool";
        return "item_icon_generic";
    }

    void loadSemanticIcons(String rootPath, String folder) {
        File dir = ArtPackManager.resolveQualityRoot(rootPath, folder).resolve("semantic").toFile();
        File[] files = dir.exists() ? dir.listFiles((d,n) -> n.toLowerCase(Locale.US).endsWith(".png")) : null;
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File f : files) {
            try {
                String name = f.getName();
                String key = name.substring(0, name.length()-4);
                BufferedImage img = ImageIO.read(f);
                imageRegistry.putSemantic(key, img);
            } catch (Throwable t) {
                DebugLog.warn("TILE_ART", "Could not load semantic icon " + f.getPath());
            }
        }
    }

    static String semanticKeyForBuildName(String name) {
        if (name == null) return null;
        String s = name.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (s.isEmpty()) return null;
        return "build_" + s;
    }


    static String semanticKeyForMapObject(MapObjectState obj) {
        return AssetIntegrationDisciplineAuthority.semanticKeyForMapObject(obj);
    }

    void bind(char glyph, String alias) { BufferedImage img = imageRegistry.getAlias(alias); if (img != null) imageRegistry.putGlyph(glyph, img); }

    void bindGlyphs() {
        bind('.', "floor_bare_underhive"); bind('`', "floor_trash_mutant_rough"); bind(';', "road_north_south"); bind('_', "road_sidewalk"); bind('+', "floor_industrial_corridor"); bind('=', "floor_maintenance_corridor"); bind(',', "floor_alleyway_cracked"); bind('~', "floor_sewer_pipe_corridor"); bind(':', "floor_padded_service_way"); bind('-', "floor_exterior_hivewall_maintenance"); bind(' ', "void_space");
        bind('#', "wall_bulkhead"); bind('%', "wall_support_beam"); bind('&', "wall_gantry_lattice"); bind('^', "wall_buried_conveyor"); bind('8', "wall_pipe_bundle"); bind('0', "wall_cable_column"); bind('/', "door_archway"); bind('|', "door_standard"); bind('L', "door_locked"); bind('V', "door_vent_panel"); bind('X', "door_security"); bind('D', "door_double"); bind('d', "barricade");
        bind('*', "debris"); bind('?', "buried_cache"); bind('!', "danger_marker"); bind('R', "rogue_machine"); bind('N', "noisy_machinery"); bind('1', "vending_food"); bind('2', "vending_armor"); bind('3', "vending_weapons"); bind('4', "vending_materials"); bind('5', "vending_survival");
        bind('Y', "water_condenser"); bind('J', "emergency_assembler"); bind('B', "emergency_boiler"); bind('K', "micro_lab"); bind('O', "emergency_miner"); bind('Z', "relay_power_grid"); bind('P', "emergency_smelter"); bind('F', "steam_engine"); bind('U', "steam_engine_disabled"); bind('w', "scrap_workbench"); bind('e', "water_condenser"); bind('f', "emergency_smelter"); bind('l', "micro_lab"); bind('x', "security_cogitator"); bind('T', "turret_or_trade"); bind('H', "shrine_or_shield"); bind('G', "logistics_center"); bind('M', "medicae_or_military"); bind('k', "carrying_station"); bind('q', "supply_post");
        bind('I', "imperial_shrine"); bind('$', "donation_box"); bind('W', "saint_alcove"); bind('Q', "governor_dais"); bind('C', "clinic"); bind('r', "corpse_loot"); bind('o', "object_generic"); bind('S', "sewer_hatch"); bind('v', "ladder_drain"); bind('E', "elevator"); bind('s', "storage_crate"); bind('c', "sleeping_cot"); bind('u', "water_barrel"); bind('a', "alarm_trap"); bind('p', "arbites_precinct"); bind('b', "bandit_den"); bind('h', "hiver_block"); bind('n', "noble_secure"); bind('t', "table_prop");
        bind('g', "bandit_den"); bind('m', "sump_fungus_mold"); bind('A', "arbites_precinct");
    }

    boolean corridorArtUsesNorthSouth(char glyph) {
        // The imported corridor sheets contain several top-down cells with a clear north/south long-axis.
        // These glyphs are treated as oriented art: east/west corridor runs rotate the drawn image at render
        // time, while intersections and omni floor clutter keep the unrotated fallback.
        return glyph == '+' || glyph == '=' || glyph == '~' || glyph == ':' || glyph == '-' || glyph == ';' || glyph == '_';
    }

    BufferedImage getTile(char glyph) { return imageRegistry.getTile(glyph); }
    BufferedImage getTile(String semanticKey, char fallback) { return imageRegistry.getTile(semanticKey, fallback); }
    BufferedImage get(char glyph) { return getTile(glyph); }
    BufferedImage get(String semanticKey, char fallback) { return getTile(semanticKey, fallback); }
    int loadedCount() { return imageRegistry.loadedCount(); }
    int semanticCount() { return imageRegistry.semanticCount(); }
}


class ArtPackManager {
    static String resolveInstalledArtRoot(String requestedRoot) {
        ArrayList<Path> candidates = new ArrayList<>();
        if (requestedRoot != null && !requestedRoot.isBlank()) {
            candidates.add(Paths.get(requestedRoot));
            File runtimeResolved = RuntimePathResolver.resolveAssetFile(requestedRoot);
            if (runtimeResolved != null) candidates.add(runtimeResolved.toPath());
        }
        candidates.add(RuntimePathResolver.resolveAssetFile("assets/a/r").toPath());
        candidates.add(RuntimePathResolver.resolveAssetFile("PACKAGE_client/assets/a/r").toPath());
        candidates.add(RuntimePathResolver.resolveAssetFile("client/assets/a/r").toPath());
        candidates.add(Paths.get("assets/a/r"));

        for (Path candidate : candidates) {
            if (candidate == null) continue;
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized.resolve("tiles").resolve("quality").resolve("low_32").resolve("cells"))) {
                return normalized.toString();
            }
        }
        return requestedRoot;
    }

    static int countPngFiles(Path root) {
        if (root == null || !Files.isDirectory(root)) return 0;
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            return (int) stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName() != null && p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .count();
        } catch (Throwable ignored) {
            return 0;
        }
    }
    static String prepareAndResolveRoot(String packDirName, String cacheDirName, String bundledFallbackRoot) {
        try {
            Path packDir = Paths.get(packDirName);
            Path cacheDir = Paths.get(cacheDirName);
            Files.createDirectories(packDir);
            Files.createDirectories(cacheDir);
            ArrayList<Path> zips = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(packDir, "*.zip")) {
                for (Path p : stream) zips.add(p);
            } catch (Throwable ignored) {}
            Collections.sort(zips, Comparator.comparing(Path::toString));
            for (Path zip : zips) unpackIfNeeded(zip, cacheDir.resolve(safeStem(zip.getFileName().toString())));
            String resolved = findRuntimeRoot(cacheDir);
            if (resolved != null) { DebugLog.audit("ART_PACK", "Resolved external art root=" + resolved); return resolved; }
        } catch (Throwable t) {
            DebugLog.error("ART_PACK", "Art-pack prepare failed; falling back to bundled art if present.", t);
        }
        String fallbackRoot = resolveInstalledArtRoot(resolveBundledFallbackRoot(bundledFallbackRoot));
        if (Files.isDirectory(Paths.get(fallbackRoot))) {
            DebugLog.audit("ART_PACK", "Using bundled art root=" + fallbackRoot);
            return fallbackRoot;
        }
        DebugLog.warn("ART_PACK", "No external or bundled art pack found. Tile/icon rendering will fall back to ASCII and generated UI.");
        return fallbackRoot;
    }

    static String resolveBundledFallbackRoot(String bundledFallbackRoot) {
        if (bundledFallbackRoot == null || bundledFallbackRoot.isBlank()) return bundledFallbackRoot;
        if (Files.exists(Paths.get(bundledFallbackRoot))) return bundledFallbackRoot;
        String normalized = bundledFallbackRoot.replace('\\', '/');
        if (normalized.startsWith("packages/client/assets/")) {
            String suffix = normalized.substring("packages/client/assets/".length());
            String clientOwned = "client/assets/" + suffix;
            if (Files.exists(Paths.get(clientOwned))) return clientOwned;
            String legacyRoot = "assets/" + suffix;
            if (Files.exists(Paths.get(legacyRoot))) return legacyRoot;
        }
        return bundledFallbackRoot;
    }

    static Path resolveQualityRoot(String bundledRoot, String folder) {
        String wanted = (folder == null || folder.isBlank()) ? "low_32" : folder;
        String resolvedBundledRoot = resolveInstalledArtRoot(bundledRoot);
        Path local = Paths.get(resolvedBundledRoot, "tiles", "quality", wanted);
        if (Files.isDirectory(local.resolve("cells"))) return local;
        Path external = findQualityRoot(Paths.get("cache", "artpacks"), wanted);
        if (external != null) return external;
        Path bundledLow = Paths.get(resolvedBundledRoot, "tiles", "quality", "low_32");
        if (Files.isDirectory(bundledLow.resolve("cells"))) {
            DebugLog.warn("ART_PACK", "Requested art tier '" + wanted + "' is not installed; falling back to bundled low_32.");
            return bundledLow;
        }
        return local;
    }

    static String resolveQualityCellsRoot(String bundledRoot, String folder) {
        Path q = resolveQualityRoot(bundledRoot, folder);
        Path cells = q.resolve("cells");
        if (Files.isDirectory(cells)) return cells.toString();
        String resolvedBundledRoot = resolveInstalledArtRoot(bundledRoot);
        return Paths.get(resolvedBundledRoot, "tiles", "cells").toString();
    }

    static Path findQualityRoot(Path cacheDir, String folder) {
        if (!Files.isDirectory(cacheDir)) return null;
        ArrayList<Path> hits = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(cacheDir, 10)) {
            stream.filter(Files::isDirectory).forEach(p -> {
                try {
                    if (p.getFileName() != null
                            && p.getFileName().toString().equals(folder)
                            && Files.isDirectory(p.resolve("cells"))
                            && p.toString().replace('\\','/').contains("/tiles/quality/")) {
                        hits.add(p);
                    }
                } catch (Throwable ignored) {}
            });
        } catch (Throwable ignored) {}
        if (hits.isEmpty()) return null;
        hits.sort(Comparator.comparing(Path::toString));
        return hits.get(hits.size()-1);
    }

    static String safeStem(String filename) {
        String s = filename == null ? "artpack" : filename.replaceAll("(?i)\\.zip$", "");
        s = s.replaceAll("[^A-Za-z0-9._-]+", "_");
        return s.isBlank() ? "artpack" : s;
    }

    static void unpackIfNeeded(Path zip, Path dest) {
        try {
            Files.createDirectories(dest);
            Path marker = dest.resolve(".unpacked.marker");
            String stamp = zip.toAbsolutePath() + "|" + Files.size(zip) + "|" + Files.getLastModifiedTime(zip).toMillis();
            if (Files.exists(marker) && Files.readString(marker).equals(stamp)) return;
            deleteTreeContents(dest);
            Files.createDirectories(dest);
            try (java.util.zip.ZipInputStream zin = new java.util.zip.ZipInputStream(Files.newInputStream(zip))) {
                java.util.zip.ZipEntry entry;
                while ((entry = zin.getNextEntry()) != null) {
                    Path out = dest.resolve(entry.getName()).normalize();
                    if (!out.startsWith(dest)) { DebugLog.warn("ART_PACK", "Skipped suspicious zip entry " + entry.getName()); continue; }
                    if (entry.isDirectory()) Files.createDirectories(out);
                    else {
                        Files.createDirectories(out.getParent());
                        Files.copy(zin, out, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zin.closeEntry();
                }
            }
            Files.writeString(marker, stamp);
            DebugLog.audit("ART_PACK", "Unpacked " + zip + " -> " + dest);
        } catch (Throwable t) {
            DebugLog.error("ART_PACK", "Could not unpack art pack " + zip, t);
        }
    }

    static void deleteTreeContents(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        ArrayList<Path> paths = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(dir)) { stream.forEach(paths::add); }
        paths.sort(Comparator.reverseOrder());
        for (Path p : paths) if (!p.equals(dir)) Files.deleteIfExists(p);
    }

    static String findRuntimeRoot(Path cacheDir) {
        if (!Files.isDirectory(cacheDir)) return null;
        ArrayList<Path> hits = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(cacheDir, 6)) {
            stream.filter(Files::isDirectory).forEach(p -> {
                if (Files.isDirectory(p.resolve("tiles/quality")) && Files.isDirectory(p.resolve("source/Title"))) hits.add(p);
            });
        } catch (Throwable ignored) {}
        if (hits.isEmpty()) return null;
        hits.sort(Comparator.comparing(Path::toString));
        return hits.get(hits.size()-1).toString();
    }
}


class AudioPackManager {
    static String prepareAndResolveMusicRoot(String packDirName, String cacheDirName, String bundledFallbackRoot) {
        try {
            Path packDir = Paths.get(packDirName);
            Path cacheDir = Paths.get(cacheDirName);
            Files.createDirectories(packDir);
            Files.createDirectories(cacheDir);
            ArrayList<Path> zips = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(packDir, "*.zip")) {
                for (Path p : stream) zips.add(p);
            } catch (Throwable ignored) {}
            Collections.sort(zips, Comparator.comparing(Path::toString));
            for (Path zip : zips) ArtPackManager.unpackIfNeeded(zip, cacheDir.resolve(ArtPackManager.safeStem(zip.getFileName().toString())));
            String resolved = findWavRoot(cacheDir);
            if (resolved != null) { DebugLog.audit("AUDIO_PACK", "Resolved external music root=" + resolved); return resolved; }
        } catch (Throwable t) {
            DebugLog.error("AUDIO_PACK", "Audio-pack prepare failed; falling back to bundled music if present.", t);
        }
        String fallbackRoot = ArtPackManager.resolveBundledFallbackRoot(bundledFallbackRoot);
        if (Files.isDirectory(Paths.get(fallbackRoot))) {
            DebugLog.audit("AUDIO_PACK", "Using bundled music root=" + fallbackRoot);
            return fallbackRoot;
        }
        DebugLog.warn("AUDIO_PACK", "No external or bundled music pack found. Dynamic music will remain silent unless music assets are installed.");
        return fallbackRoot;
    }

    static String findWavRoot(Path cacheDir) {
        if (!Files.isDirectory(cacheDir)) return null;
        ArrayList<Path> hits = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.walk(cacheDir, 7)) {
            stream.filter(Files::isDirectory).forEach(p -> {
                try (DirectoryStream<Path> wavs = Files.newDirectoryStream(p, "*.wav")) {
                    Iterator<Path> it = wavs.iterator();
                    if (it.hasNext()) hits.add(p);
                } catch (Throwable ignored) {}
            });
        } catch (Throwable ignored) {}
        if (hits.isEmpty()) return null;
        hits.sort(Comparator.comparing(Path::toString));
        return hits.get(hits.size()-1).toString();
    }
}


class ImageCache {
    static final int PLAYER_BASELINE_HUMAN_POOL = 0;
    static final int NPC_AUGMENTED_POOL = 1;
    final Map<String, BufferedImage> cache = new HashMap<>();
    final ArrayList<BufferedImage> bootFrames = new ArrayList<>();
    final ArrayList<BufferedImage> portraitSheets = new ArrayList<>();
    final ArrayList<BufferedImage> playerHumanPortraitCells = new ArrayList<>();
    private BufferedImage generatedPlayerHumanFallbackPortrait;
    final ArrayList<BufferedImage> npcPortraitCells = new ArrayList<>();
    final LinkedHashMap<String, BufferedImage> nameLockedProfilePortraits = new LinkedHashMap<>();
    final LinkedHashMap<String,int[]> npcPortraitRanges = new LinkedHashMap<>();
    final ArrayList<PortraitSheetProfile> portraitProfiles = new ArrayList<>();
    final TileArtSystem tileArt = new TileArtSystem();
    final Map<String, BufferedImage> semanticAssetImageCache = new HashMap<>();
    String artRootPath = "assets/a/r";
    final String base = "assets/imported_tech_priests/graphics/gui/";

    void load(GameOptions options) {
        semanticAssetImageCache.clear();
        String sliceBase = base + "cogitator_frame_0536/slices_384/";
        String[] keys = {
            "corner_top_left", "corner_top_right", "corner_bottom_left", "corner_bottom_right",
            "top_rail_left_mid", "bottom_rail_left_mid", "left_column_mid", "right_column_mid",
            "inner_bezel_t", "inner_bezel_b", "inner_bezel_l", "inner_bezel_r", "inner_display_center"
        };
        for (String k : keys) load(k, sliceBase + k + ".png");
        String medallionBase = base + "rough-assets/medallion_spin/frame_";
        for (int i=1;i<=8;i++) {
            BufferedImage img = read(String.format(Locale.US, "%s%02d.png", medallionBase, i));
            if (img != null) bootFrames.add(img);
        }
        // Fallback only: the red gear is no longer the intended spinner.
        BufferedImage emblem = read(base + "rough-assets/source_sheets_cleaned/medallion_spin_sheet.png");
        if (emblem != null && bootFrames.isEmpty()) cache.put("mechanical_skull_gear_emblem", emblem);
        loadPortraitSheet("assets/imported_tech_priests/graphics/lean/gui/tech_priest_augmented_portrait_sheet_a__lean50.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/lean/gui/baseline_human_portrait_sheet__lean50.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/lean/gui/alternative_human_augmented_portrait_sheet_c__lean50.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/lean/gui/planetary_magos_portrait_sheet_a__lean50.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/gui/portraits/tech_priest_augmented_portrait_sheet_a.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/gui/portraits/baseline_human_portrait_sheet.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/gui/portraits/alternative_human_augmented_portrait_sheet_c.png");
        loadPortraitSheet("assets/imported_tech_priests/graphics/gui/portraits/planetary_magos_portrait_sheet_a.png");
        load("title_mechanist", "assets/generated/the_mechanist_title.png");
        artRootPath = ArtPackManager.prepareAndResolveRoot("packages/client/assets/artpacks", "cache/artpacks", "packages/client/assets/a/r");
        load("title_mechanist_rebase", artRootPath + "/source/Title/TITEL.png");
        load("subtitle_rebase", artRootPath + "/source/Title/Sub title.png");
        load("new_world_backdrop_rebase", artRootPath + "/source/Background/Backdrop.png");
        load("clouds_slow_rebase", artRootPath + "/source/Background/CLOUDS1slow.png");
        load("clouds_fast_rebase", artRootPath + "/source/Background/Clouds2fast.png");
        tileArt.load(artRootPath, options == null ? 0 : options.artQualityIndex);
        String portraitQuality = options == null ? "low_32" : options.artQualityFolder();
        loadPortraitCellTree(ArtPackManager.resolveQualityCellsRoot(artRootPath, portraitQuality) + "/Protraits");
        loadExplicitPlayerHumanPortraitPool(ArtPackManager.resolveQualityCellsRoot(artRootPath, portraitQuality) + "/Protraits");
        // Button authority: use the imported Tech Priests GUI controls folder.
        load("button_normal", base + "controls/normal/03_rect_button_off.png");
        load("button_hover", base + "controls/normal/04_rect_button_on.png");
        load("button_disabled", base + "controls/disabled/03_rect_button_off.png");
        load("button_round_normal", base + "controls/normal/01_round_button_off.png");
        load("button_round_hover", base + "controls/normal/02_round_button_on.png");
        loadPortraitCells("assets/imported_tech_priests/graphics/gui/portraits/cells_0560");
        loadNameLockedProfilePortraits();
        inspectPortraitSheet("PLAYER_BASELINE_ONLY", "assets/imported_tech_priests/graphics/gui/portraits/baseline_human_portrait_sheet.png", true);
        inspectPortraitSheet("NPC_ALT_AUGMENTED_C_DIAGNOSTIC", "assets/imported_tech_priests/graphics/gui/portraits/alternative_human_augmented_portrait_sheet_c.png", false);
        inspectPortraitSheet("NPC_PLANETARY_MAGOS_DIAGNOSTIC", "assets/imported_tech_priests/graphics/gui/portraits/planetary_magos_portrait_sheet_a.png", false);
        inspectPortraitSheet("NPC_TECH_PRIEST_AUGMENTED_DIAGNOSTIC", "assets/imported_tech_priests/graphics/gui/portraits/tech_priest_augmented_portrait_sheet_a.png", false);
        DebugLog.audit("ASSETS", "Loaded GUI frame slices=" + cache.size() + " bootFrames=" + bootFrames.size() + " portraitSheets=" + portraitSheets.size() + " playerHumanPortraitCells=" + playerHumanPortraitCells.size() + " npcPortraitCells=" + npcPortraitCells.size() + " tileArt=" + tileArt.loadedCount());
    }

    void load(String key, String path) { BufferedImage img = read(path); if (img != null) cache.put(key, img); }
    void loadPortraitSheet(String path) { BufferedImage img = read(path); if (img != null) portraitSheets.add(img); }
    void loadPortraitCells(String dirPath) {
        File dir = new File(dirPath);
        File[] files = dir.exists() ? dir.listFiles((d,n) -> n.toLowerCase(Locale.US).endsWith(".png")) : null;
        if (files == null) return;
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File f: files) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img == null) continue;
                String name = f.getName().toLowerCase(Locale.ROOT);
                if (name.contains("baseline-human") || name.contains("baseline_human") || name.contains("base-human") || name.contains("base_human")) {
                    playerHumanPortraitCells.add(img);
                } else {
                    // NPC-only until source sheet slicing is verified.
                    npcPortraitCells.add(img);
                }
            } catch (Exception ex) {
                DebugLog.error("PORTRAIT_CELL_LOAD", "failed loading portrait cell " + f.getName(), ex);
            }
        }
        DebugLog.audit("PORTRAIT_AUTHORITY", "PLAYER_POOL=baseline-human-only count=" + playerHumanPortraitCells.size() + "; NPC_POOL=non-baseline count=" + npcPortraitCells.size());
    }

    void loadNameLockedProfilePortraits() {
        nameLockedProfilePortraits.clear();
        for (NameLockedProfilePortraitAuthority.Entry e : NameLockedProfilePortraitAuthority.entries()) {
            BufferedImage img = read(e.assetPath);
            if (img != null) nameLockedProfilePortraits.put(e.key, img);
            else DebugLog.warn("NAME_LOCKED_PROFILE_PORTRAIT", "missing asset for " + e.key + " path=" + e.assetPath);
        }
        DebugLog.audit("NAME_LOCKED_PROFILE_PORTRAIT", NameLockedProfilePortraitAuthority.auditSummary() + " loaded=" + nameLockedProfilePortraits.size());
    }

    BufferedImage getNameLockedProfilePortrait(String key) {
        if (key == null || key.isBlank()) return null;
        return nameLockedProfilePortraits.get(key);
    }
    void loadPortraitCellTree(String dirPath) {
        File root = new File(dirPath);
        if (!root.exists()) return;
        ArrayList<File> dirs = new ArrayList<>();
        File[] children = root.listFiles();
        if (children != null) for (File f : children) if (f.isDirectory()) dirs.add(f);
        Collections.sort(dirs, Comparator.comparing(File::getPath));
        int loaded = 0;
        for (File dir : dirs) {
            ArrayList<File> files = new ArrayList<>();
            collectPngFiles(dir, files);
            Collections.sort(files, Comparator.comparing(File::getPath));
            int start = npcPortraitCells.size();
            for (File f : files) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) { npcPortraitCells.add(img); loaded++; }
                } catch (Exception ex) {
                    DebugLog.error("PORTRAIT_REBASE_LOAD", "failed loading portrait cell " + f.getPath(), ex);
                }
            }
            int end = npcPortraitCells.size();
            String rangeKey = dir.getName().toLowerCase(Locale.ROOT);
            if (end > start) {
                npcPortraitRanges.put(rangeKey, new int[]{start, end});
            }
        }
        DebugLog.audit("PORTRAIT_REBASE", "loaded CRT portrait cells=" + loaded + " ranges=" + npcPortraitRanges.keySet() + " playerHumanPool=" + playerHumanPortraitCells.size());
    }

    void loadExplicitPlayerHumanPortraitPool(String protraitsRootPath) {
        // PLAYER PROFILE/CHARACTER PORTRAIT AUTHORITY:
        // Do not promote every entity/faction portrait folder into the player pool.
        // The player-human/profile pool is loaded only from explicit human/profile buckets.
        // The administratum bucket is the packaged ordinary-human fallback only when no
        // baseline_human/player_human/profile_human folder exists in the art pack.
        if (protraitsRootPath == null) return;
        File root = new File(protraitsRootPath);
        if (!root.isDirectory()) return;
        String[] preferred = new String[]{
            "human_profiles", "humans", "human", "baseline_human", "base_human",
            "player_human", "player_humans", "standard_human", "standard_profiles",
            "profile_human", "profile_humans", "profiles"
        };
        boolean loaded = false;
        for (String key : preferred) loaded |= addPortraitDirectoryToPlayerPool(new File(root, key));
        if (!loaded) {
            // Ordinary human fallback. This is still a single named folder, not the whole entity bin.
            loaded = addPortraitDirectoryToPlayerPool(new File(root, "administratum"));
        }
        DebugLog.audit("PLAYER_HUMAN_PORTRAIT_POOL", "explicitFolderLoaded=" + loaded + " count=" + playerHumanPortraitCells.size());
    }

    boolean addPortraitDirectoryToPlayerPool(File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        ArrayList<File> files = new ArrayList<>();
        collectPngFiles(dir, files);
        Collections.sort(files, Comparator.comparing(File::getPath));
        int before = playerHumanPortraitCells.size();
        for (File f : files) {
            try {
                BufferedImage img = ImageIO.read(f);
                if (img != null) playerHumanPortraitCells.add(img);
            } catch (Exception ex) {
                DebugLog.error("PLAYER_HUMAN_PORTRAIT_LOAD", "failed loading player-human portrait " + f.getPath(), ex);
            }
        }
        return playerHumanPortraitCells.size() > before;
    }

    boolean isPlayerHumanPortraitDirectory(String key) {
        if (key == null) return false;
        String k = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        // Player creation is restricted to an explicit baseline/base-human bucket only.
        // Faction folders such as administratum, gangers, nobles, PDF, Arbites,
        // Mechanicus, pets, beasts, mutants, and cultists are NPC/faction authority
        // pools and must never be silently promoted into the player pool.
        return k.equals("baseline_human") || k.equals("base_human") || k.equals("player_baseline_human") || k.equals("normal_human") || k.equals("humans_base");
    }

    void collectPngFiles(File dir, ArrayList<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) collectPngFiles(f, out);
            else if (f.getName().toLowerCase(Locale.US).endsWith(".png")) out.add(f);
        }
    }

    void inspectPortraitSheet(String label, String path, boolean playerAllowed) {
        BufferedImage img = read(path);
        if (img == null) { DebugLog.audit("PORTRAIT_PROFILE", label + " missing path=" + path); return; }
        PortraitSheetProfile best = PortraitSheetProfile.infer(label, path, img.getWidth(), img.getHeight(), playerAllowed);
        portraitProfiles.add(best);
        DebugLog.audit("PORTRAIT_PROFILE", best.toAuditLine());
    }
    void reloadArtQuality(GameOptions options) {
        semanticAssetImageCache.clear();
        artRootPath = ArtPackManager.prepareAndResolveRoot("packages/client/assets/artpacks", "cache/artpacks", "packages/client/assets/a/r");
        tileArt.load(artRootPath, options == null ? 0 : options.artQualityIndex);
        npcPortraitCells.clear();
        npcPortraitRanges.clear();
        playerHumanPortraitCells.clear();
        String portraitQuality = options == null ? "low_32" : options.artQualityFolder();
        loadPortraitCellTree(ArtPackManager.resolveQualityCellsRoot(artRootPath, portraitQuality) + "/Protraits");
        loadExplicitPlayerHumanPortraitPool(ArtPackManager.resolveQualityCellsRoot(artRootPath, portraitQuality) + "/Protraits");
        loadPortraitCells("assets/imported_tech_priests/graphics/gui/portraits/cells_0560");
        loadNameLockedProfilePortraits();
        DebugLog.audit("ART_QUALITY", "Reloaded art cache root=" + artRootPath + " quality=" + (options == null ? "low_32" : options.artQualityFolder()) + " tileGlyphs=" + tileArt.loadedCount() + " semantic=" + tileArt.semanticCount() + " npcPortraitCells=" + npcPortraitCells.size() + " nameLockedProfilePortraits=" + nameLockedProfilePortraits.size());
    }
    BufferedImage getPortrait(int sheetIndex, int portraitIndex) {
        // PLAYER CHARACTER/PROFILE DEFAULTS ARE LOCKED TO THE PLAYER-HUMAN POOL ONLY.
        // Never fall through into NPC faction buckets or name_locked celebrity portraits.
        if (!playerHumanPortraitCells.isEmpty()) return playerHumanPortraitCells.get(Math.floorMod(portraitIndex, playerHumanPortraitCells.size()));
        BufferedImage legacy = getLegacyPlayerHumanPortrait(portraitIndex);
        if (legacy != null) return legacy;
        return generatedPlayerHumanFallbackPortrait();
    }

    BufferedImage generatedPlayerHumanFallbackPortrait() {
        if (generatedPlayerHumanFallbackPortrait != null) return generatedPlayerHumanFallbackPortrait;
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(18, 20, 18, 255));
            g.fillRect(0, 0, 128, 128);
            g.setColor(new Color(70, 62, 42));
            g.fillOval(42, 20, 44, 44);
            g.setColor(new Color(115, 95, 58));
            g.fillRect(32, 70, 64, 44);
            g.setColor(new Color(210, 185, 105));
            g.drawOval(42, 20, 44, 44);
            g.drawRect(32, 70, 64, 44);
        } finally { g.dispose(); }
        generatedPlayerHumanFallbackPortrait = img;
        return img;
    }

    BufferedImage getLegacyPlayerHumanPortrait(int portraitIndex) {
        if (portraitSheets.isEmpty()) return null;
        BufferedImage sheet = null;
        if (portraitSheets.size() > 1) sheet = portraitSheets.get(1);
        else sheet = portraitSheets.get(0);
        if (sheet == null) return null;
        int cellW = 128, cellH = 128;
        int cols = Math.max(1, sheet.getWidth()/cellW);
        int rows = Math.max(1, sheet.getHeight()/cellH);
        int idx = Math.floorMod(portraitIndex, cols*rows);
        int sx = (idx % cols) * cellW, sy = (idx / cols) * cellH;
        if (sx+cellW > sheet.getWidth() || sy+cellH > sheet.getHeight()) return null;
        return sheet.getSubimage(sx, sy, cellW, cellH);
    }
    BufferedImage getNpcPortraitFor(NpcEntity npc) {
        if (npc == null) return getNpcPortrait(0);
        if (npc.nameLockedProfileKey != null && !npc.nameLockedProfileKey.isBlank()) {
            BufferedImage locked = getNameLockedProfilePortrait(npc.nameLockedProfileKey);
            if (locked != null) return locked;
        }
        int[] range = portraitRangeForNpc(npc);
        if (range != null && range[1] > range[0] && !npcPortraitCells.isEmpty()) return npcPortraitCells.get(range[0] + Math.floorMod(npc.portraitIndex, range[1]-range[0]));
        return getNpcPortrait(npc.portraitIndex);
    }

    int[] portraitRangeForNpc(NpcEntity npc) {
        if (npc == null) return null;
        String role = ((npc.creatureKind == null ? "" : npc.creatureKind) + " " + (npc.animalProfileId == null ? "" : npc.animalProfileId) + " " + (npc.role == null ? "" : npc.role) + " " + (npc.name == null ? "" : npc.name)).toLowerCase(Locale.ROOT);
        if (role.contains("servant") || role.contains("chef") || role.contains("butler") || role.contains("household") || role.contains("laundry") || role.contains("retainer") || role.contains("pantry")) return firstPortraitRangeContaining("servants_butlers_and_chefs");
        if (role.contains("medicae") || role.contains("hospital") || role.contains("clinic")) return firstPortraitRangeContaining("medicae", "sisters_hospital");
        if (npc.isAnimalActor()) {
            if (role.contains("farm") || role.contains("hog") || role.contains("goat") || role.contains("fowl") || role.contains("grub")) return firstPortraitRangeContaining("farm_beasts");
            if (role.contains("sump") || role.contains("sewer") || role.contains("swamp") || role.contains("eel") || role.contains("leech") || role.contains("corpse-feeder") || role.contains("fungus")) return firstPortraitRangeContaining("exotic_pets_swamp_creatures", "pets");
            if (role.contains("kennel") || role.contains("mastiff") || role.contains("hound") || role.contains("guard") || role.contains("pet") || role.contains("cat") || role.contains("rat") || role.contains("lizard") || role.contains("moth") || role.contains("glowfish")) return firstPortraitRangeContaining("pets", "exotic_pets_swamp_creatures");
            return firstPortraitRangeContaining("pets", "exotic_pets_swamp_creatures", "farm_beasts");
        }
        if (npc.isChildActor()) return firstPortraitRangeContaining("schola_children", "administratum");
        Faction f = npc.faction == null ? Faction.NONE : npc.faction;
        String fn = f.name().toLowerCase(Locale.ROOT);
        if (f == Faction.ADMINISTRATUM || f == Faction.INN) return firstPortraitRangeContaining("administratum");
        if (f == Faction.ARBITES) return firstPortraitRangeContaining("enforcer_arebites");
        if (f == Faction.IMPERIAL_GUARD) return firstPortraitRangeContaining("pdf_military");
        if (f == Faction.MINISTORUM) return firstPortraitRangeContaining("ecclesiarch");
        if (f == Faction.SORORITAS) return firstPortraitRangeContaining("sisters_hospital", "ecclesiarch");
        if (f == Faction.MECHANICUS || fn.startsWith("mechanicus")) return firstPortraitRangeContaining("mechanicus", "rogue_automata_servitors");
        if (f == Faction.ROGUE_MACHINE) return firstPortraitRangeContaining("rogue_automata_servitors", "mechanicus");
        if (f == Faction.MUTANT) return firstPortraitRangeContaining("mutants");
        if (f == Faction.CULTIST) return firstPortraitRangeContaining("cultists", "genestealer_cult", "heretics");
        if (f == Faction.HERETIC) return firstPortraitRangeContaining("heretics", "cultists");
        if (fn.startsWith("ganger") || f == Faction.BANDIT) return firstPortraitRangeContaining("gangers");
        if (fn.startsWith("noble") || f == Faction.NOBLE) return firstPortraitRangeContaining("nobles");
        if (fn.startsWith("hiver") || f == Faction.HIVER || f == Faction.SCAVENGER || f == Faction.NONE) return firstPortraitRangeContaining("administratum", "gangers");
        return firstPortraitRangeContaining("administratum", "nobles", "gangers");
    }

    int[] firstPortraitRangeContaining(String... keys) {
        if (keys == null) return null;
        for (String key : keys) {
            if (key == null) continue;
            String k = key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
            int[] exact = npcPortraitRanges.get(k);
            if (exact != null) return exact;
            // Hard folder authority: do not use substring matching here.
            // A request for pets must not accidentally match every folder with
            // the letters p/e/t, and a missing faction bucket must fail closed
            // rather than searching the whole portrait bin.
        }
        return null;
    }

    BufferedImage getAnyNameLockedProfilePortrait(int portraitIndex) {
        if (nameLockedProfilePortraits == null || nameLockedProfilePortraits.isEmpty()) return null;
        java.util.ArrayList<String> keys = new java.util.ArrayList<>(nameLockedProfilePortraits.keySet());
        java.util.Collections.sort(keys);
        return nameLockedProfilePortraits.get(keys.get(Math.floorMod(portraitIndex, keys.size())));
    }

    BufferedImage getNpcPortrait(int portraitIndex) {
        // Generic NPC fallback is deliberately conservative and celebrity-safe.
        // Name-locked portraits must never leak onto ordinary entities. Only an
        // NPC carrying nameLockedProfileKey, created by the noble-zone seeding rule,
        // may draw from the name_locked partition.
        int[] neutral = firstPortraitRangeContaining("administratum");
        if (neutral != null && neutral[1] > neutral[0] && !npcPortraitCells.isEmpty()) return npcPortraitCells.get(neutral[0] + Math.floorMod(portraitIndex, neutral[1]-neutral[0]));
        return generatedPlayerHumanFallbackPortrait();
    }
        ArrayList<String> loadIntroCrawlLines() {
        ArrayList<String> lines = new ArrayList<>();
        Path p = Paths.get(artRootPath, "source", "new game Intro crawl text", "Text crawl.txt");
        if (artRootPath != null && artRootPath.replace('\\', '/').startsWith("assets/")) {
            p = Paths.get("packages", "client", artRootPath, "source", "new game Intro crawl text", "Text crawl.txt");
            if (!Files.exists(p)) {
                p = Paths.get("client", artRootPath, "source", "new game Intro crawl text", "Text crawl.txt");
            }
            if (!Files.exists(p)) {
                p = Paths.get(artRootPath, "source", "new game Intro crawl text", "Text crawl.txt");
            }
        }
        try {
            if (Files.exists(p)) {
                for (String raw : Files.readAllLines(p)) {
                    String line = raw == null ? "" : raw.replace("\uFEFF", "").trim();
                    if (!line.isEmpty()) lines.add(line);
                }
            }
        } catch (Throwable t) {
            DebugLog.error("INTRO_CRAWL_TEXT", "Failed to read " + p, t);
        }
        if (lines.isEmpty()) {
            lines.add("The hive world resolves beneath the cloud layer: towers, ducts, drowned lights, and the small official fiction that any of this is governed.");
            lines.add("Your name has been entered into a ledger that will outlive you, misfile you, and perhaps tax your remains.");
            lines.add("Somewhere below, a door opens. Somewhere above, nobody important notices.");
        }
        return lines;
    }

    BufferedImage read(String path) {
        try {
            File f = new File(path);
            if (!f.exists() && path != null && path.replace('\\', '/').startsWith("assets/")) {
                File packagedClientOwned = new File("packages/client", path);
                if (packagedClientOwned.exists()) f = packagedClientOwned;
                File clientOwned = new File("client", path);
                if (!f.exists() && clientOwned.exists()) f = clientOwned;
            }
            if (!f.exists() && path != null) {
                File shortRoot = new File(path.replace('\\', '/').replace("assets/art/rebase_0_9_06d", "assets/a/r"));
                if (shortRoot.exists()) f = shortRoot;
            }
            if (!f.exists()) return null;
            return ImageIO.read(f);
        } catch (Throwable t) {
            DebugLog.error("ASSET_LOAD", "Failed to load image " + path, t);
            return null;
        }
    }
    BufferedImage get(String key) { return cache.get(key); }
    boolean hasFrameSlices() { return cache.get("corner_top_left") != null && cache.get("inner_display_center") != null; }
    boolean hasTileArt() { return tileArt.loadedCount() > 0; }
    BufferedImage getTile(char ch) { return tileArt.getTile(ch); }
    BufferedImage getTile(String semanticKey, char fallback) { return tileArt.getTile(semanticKey, fallback); }

    BufferedImage getSemanticAssetImage(String assetId) {
        if (assetId == null || assetId.isBlank()) return null;
        String id = assetId.trim().toUpperCase(Locale.ROOT);
        BufferedImage cached = semanticAssetImageCache.get(id);
        if (cached != null) return cached;
        ImageIcon icon = AssetManager.getAsset(id);
        if (icon == null || AssetManager.isMissingAssetIcon(icon)) return null;
        BufferedImage img = imageIconToBufferedImage(icon);
        if (img != null) semanticAssetImageCache.put(id, img);
        return img;
    }

    BufferedImage getItemIcon(String itemName) {
        String assetId = ItemSemanticAssetAuthority.semanticAssetIdForItemName(itemName);
        AssetType expectedType = AssetManager.metadata(assetId).map(m -> m.type()).orElse(AssetType.ITEM_ICON);
        ImageIcon icon = AssetManager.getAsset(assetId, expectedType);
        BufferedImage semantic = imageIconToBufferedImage(icon);
        return semantic != null ? semantic : AssetManager.missingAssetImage(expectedType);
    }

    private static BufferedImage imageIconToBufferedImage(ImageIcon icon) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0) return null;
        if (icon.getImage() instanceof BufferedImage bi) return bi;
        BufferedImage out = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(icon.getImage(), 0, 0, null);
        g.dispose();
        return out;
    }
    boolean corridorArtUsesNorthSouth(char ch) { return tileArt.corridorArtUsesNorthSouth(ch); }
    BufferedImage getBootSpinnerFrame(long tick) {
        if (!bootFrames.isEmpty()) return bootFrames.get((int)(Math.floorMod(tick, bootFrames.size())));
        return cache.get("mechanical_skull_gear_emblem");
    }
}

