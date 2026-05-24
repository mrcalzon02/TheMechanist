package mechanist;

import java.util.*;

/**
 * Compiles legacy single-character map glyphs into renderer-facing tile descriptors.
 *
 * This is the first deliberate break from treating the visible ASCII glyph as the
 * complete tile identity. Legacy generation may still write compact chars into
 * World.tiles for now, but the renderer and inspect surfaces consume the composed
 * descriptor: terrain layer, road/corridor/wall family, fixture/semantic overlay,
 * underlay, variant, and a readable combined key.
 */
final class TileDataCompilationAuthority {
    static final String VERSION = "0.9.10jq";
    static final char ROAD_LANE = RoadGridIntegrationAuthority.ROAD_LANE;
    static final char SIDEWALK = RoadGridIntegrationAuthority.SIDEWALK;

    static final class Result {
        int total;
        int roads;
        int sidewalks;
        int walls;
        int floors;
        int corridors;
        int doors;
        int overlays;
        int fixtures;
        int fallback;
        String summary() {
            return "version=" + VERSION +
                    " total=" + total +
                    " floors=" + floors +
                    " corridors=" + corridors +
                    " roads=" + roads +
                    " sidewalks=" + sidewalks +
                    " walls=" + walls +
                    " doors=" + doors +
                    " overlays=" + overlays +
                    " fixtures=" + fixtures +
                    " fallback=" + fallback +
                    " rule=legacy glyphs compile into composed terrain+shape+variant+overlay keys before rendering";
        }
    }

    private TileDataCompilationAuthority() {}

    static Result compile(World w) {
        Result out = new Result();
        if (w == null || w.tiles == null || w.w <= 0 || w.h <= 0) return out;
        w.compiledTileDescriptors = new CompiledTileDescriptor[w.w][w.h];
        for (int x = 0; x < w.w; x++) {
            for (int y = 0; y < w.h; y++) {
                CompiledTileDescriptor d = resolveFresh(w, x, y, w.tiles[x][y]);
                w.compiledTileDescriptors[x][y] = d;
                tally(out, d);
            }
        }
        w.compiledTileDescriptorSummary = out.summary();
        return out;
    }

    static CompiledTileDescriptor resolve(World w, int x, int y, char glyph) {
        if (w != null && w.compiledTileDescriptors != null && x >= 0 && y >= 0 && x < w.w && y < w.h) {
            CompiledTileDescriptor cached = w.compiledTileDescriptors[x][y];
            if (cached != null && cached.sourceGlyph == glyph) return cached;
        }
        return resolveFresh(w, x, y, glyph);
    }

    static CompiledTileDescriptor resolveFresh(World w, int x, int y, char glyph) {
        if (isWallGlyph(glyph)) return wallDescriptor(w, glyph, x, y);
        if (glyph == ROAD_LANE || glyph == SIDEWALK) return roadDescriptor(w, x, y, glyph);
        if (isDoorGlyph(glyph)) return doorDescriptor(glyph);
        if (isFloorGlyph(glyph)) return floorDescriptor(w, x, y, glyph);
        if (isCorridorGlyph(glyph)) return corridorDescriptor(w, x, y, glyph);
        String overlay = overlayAliasForGlyph(glyph);
        if (overlay != null) {
            char underGlyph = inferredUnderlayGlyph(w, x, y, glyph);
            if (underGlyph == glyph || overlayAliasForGlyph(underGlyph) != null) underGlyph = '.';
            CompiledTileDescriptor under = resolveFresh(w, x, y, underGlyph);
            return CompiledTileDescriptor.overlay(glyph, underGlyph, under.primaryArtKey, overlay, semanticTagForGlyph(glyph),
                    "tile.overlay/" + overlay + "+underlay/" + safe(under.composedKey));
        }
        String key = "tile.legacy/fallback/glyph_" + ((int) glyph);
        return new CompiledTileDescriptor(glyph, "fallback", "legacy", null, 0,
                key, null, null, null, null, false, false, false, false, false, false, key);
    }

    static void tally(Result r, CompiledTileDescriptor d) {
        if (r == null || d == null) return;
        r.total++;
        if (d.isRoad) r.roads++;
        if (d.isSidewalk) r.sidewalks++;
        if (d.isWall) r.walls++;
        if ("floor".equals(d.baseLayer)) r.floors++;
        if ("corridor".equals(d.baseLayer)) r.corridors++;
        if ("door".equals(d.baseLayer)) r.doors++;
        if (d.hasOverlay()) r.overlays++;
        if ("fixture".equals(d.baseLayer)) r.fixtures++;
        if ("fallback".equals(d.baseLayer)) r.fallback++;
    }

    static CompiledTileDescriptor wallDescriptor(World w, char glyph, int x, int y) {
        if (glyph == '#' && exteriorMaintenanceBulkheadContext(w, x, y)) {
            int variant = 1 + Math.floorMod(Objects.hash(w == null ? 0 : w.seed, "exterior-maintenance-bulkhead"), 3);
            return new CompiledTileDescriptor(glyph, "wall", "exterior_maintenance_bulkhead", null, variant,
                    "wall_exterior_maintenance_bulkhead_v" + variant, null, null, null, null, true, false, false, false, false, false,
                    "tile.wall/exterior_maintenance_bulkhead/v" + variant);
        }
        Faction roomFaction = adjacentRoomFaction(w, x, y);
        String family = wallFamilyForGlyph(glyph);
        String art;
        int variant;
        if (roomFaction == Faction.NOBLE || isNobleZone(w)) {
            family = "noble_bulkhead";
            variant = wallVariantForRoomOrZone(w, x, y, "noble_wall", 5);
            art = "wall_noble_bulkhead_v" + variant;
        } else if (sewerContext(w, x, y)) {
            family = "sewer_bulkhead";
            variant = wallVariantForRoomOrZone(w, x, y, "sewer_wall", 5);
            art = "wall_sewer_bulkhead_v" + variant;
        } else if ("bulkhead".equals(family)) {
            variant = wallVariantForRoomOrZone(w, x, y, "bulkhead", 5);
            art = "wall_bulkhead_v" + variant;
        } else {
            variant = 1;
            art = "wall_" + family;
        }
        return new CompiledTileDescriptor(glyph, "wall", family, null, variant,
                art, null, null, null, null, true, false, false, false, false, false,
                "tile.wall/" + family + "/v" + variant);
    }

    static int wallVariantForRoomOrZone(World w, int x, int y, String family, int count) {
        int rid = adjacentRoomId(w, x, y);
        if (rid >= 0) return 1 + Math.floorMod(Objects.hash(rid, family), Math.max(1, count));
        long seed = w == null ? 0L : w.seed;
        Object zone = w == null ? "none" : w.zoneType;
        return 1 + Math.floorMod(Objects.hash(seed, zone, family), Math.max(1, count));
    }

    static int adjacentRoomId(World w, int x, int y) {
        if (w == null || w.roomIds == null) return -1;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (w.inBounds(nx, ny)) {
                int rid = w.roomIdAt(nx, ny);
                if (rid >= 0) return rid;
            }
        }
        return -1;
    }

    static Faction adjacentRoomFaction(World w, int x, int y) {
        int rid = adjacentRoomId(w, x, y);
        return w == null ? null : w.roomFaction(rid);
    }

    static boolean exteriorMaintenanceBulkheadContext(World w, int x, int y) {
        if (w == null || !w.inBounds(x, y)) return false;
        return neighborGlyph(w, x - 1, y, '-') || neighborGlyph(w, x + 1, y, '-') ||
               neighborGlyph(w, x, y - 1, '-') || neighborGlyph(w, x, y + 1, '-');
    }

    static boolean neighborGlyph(World w, int x, int y, char glyph) {
        return w != null && w.inBounds(x, y) && w.tiles[x][y] == glyph;
    }

    static CompiledTileDescriptor roadDescriptor(World w, int x, int y, char glyph) {
        boolean originalSidewalk = glyph == SIDEWALK;
        boolean promotedCrossing = originalSidewalk && sidewalkPromotesToRoad(w, x, y);
        boolean sidewalk = originalSidewalk && !promotedCrossing;
        String shape = sidewalk ? "sidewalk" : roadShape(w, x, y);
        int variant = roadVariantForShape(w, shape);
        String art = roadArtKeyForShape(shape, variant);
        return new CompiledTileDescriptor(glyph, "road", promotedCrossing ? "underhive_street_crossing" : "underhive_street", shape, variant,
                art, null, null, null, null, false, true, sidewalk, false, false, false,
                "tile.road/" + shape + "/v" + variant + (promotedCrossing ? "/promoted_sidewalk_crossing" : ""));
    }

    static String roadShape(World w, int x, int y) {
        boolean west = roadLane(w, x - 1, y) || sidewalkPromotesToRoad(w, x - 1, y);
        boolean east = roadLane(w, x + 1, y) || sidewalkPromotesToRoad(w, x + 1, y);
        boolean north = roadLane(w, x, y - 1) || sidewalkPromotesToRoad(w, x, y - 1);
        boolean south = roadLane(w, x, y + 1) || sidewalkPromotesToRoad(w, x, y + 1);
        boolean ewOpposed = west && east;
        boolean nsOpposed = north && south;
        boolean ewAny = west || east;
        boolean nsAny = north || south;
        // Three-neighbor road cells beside a four-wide street are still straight lanes
        // unless both axes have opposed continuity. Corners and caps must keep their
        // direction so the road atlas does not draw every rounded end facing right.
        if (ewOpposed && nsOpposed) return "intersection";
        if (nsOpposed) return "north_south";
        if (ewOpposed) return "east_west";
        if (west && north) return "corner_west_north";
        if (west && south) return "corner_west_south";
        if (east && north) return "corner_east_north";
        if (east && south) return "corner_east_south";
        if (west) return "end_west";
        if (east) return "end_east";
        if (north) return "end_north";
        if (south) return "end_south";
        if (ewAny) return "east_west";
        if (nsAny) return "north_south";
        return "north_south";
    }

    static int roadVariantForShape(World w, String shape) {
        if (shape == null) return 1;
        if (shape.startsWith("corner_") || shape.startsWith("end_")) return 1;
        return mapRoadVariant(w);
    }

    static int mapRoadVariant(World w) {
        if (w == null) return 1;
        // One road visual family per generated zone. Roads may change direction/shape,
        // but they must not hash into a patchwork of unrelated variant rows inside the
        // same slice.
        return 1 + Math.floorMod(Objects.hash(w.seed, w.zoneType, w.sewerLayer, "road-variant-set"), 5);
    }

    static String roadArtKeyForShape(String shape, int variant) {
        if (shape == null || shape.isBlank()) return "road_north_south_v1";
        if (shape.startsWith("corner_") || shape.startsWith("end_")) return "road_" + shape;
        return "road_" + shape + "_v" + Math.max(1, Math.min(5, variant));
    }

    static CompiledTileDescriptor doorDescriptor(char glyph) {
        String alias;
        switch (glyph) {
            case '/': alias = "door_archway"; break;
            case '|': alias = "door_standard"; break;
            case 'L': alias = "door_locked"; break;
            case 'V': alias = "door_vent_panel"; break;
            case 'X': alias = "door_security"; break;
            case 'D': alias = "door_double"; break;
            default: alias = "door_standard"; break;
        }
        return new CompiledTileDescriptor(glyph, "door", alias, null, 1,
                alias, null, null, null, null, false, false, false, false, true, false,
                "tile.door/" + alias);
    }

    static CompiledTileDescriptor floorDescriptor(World w, int x, int y, char glyph) {
        String art;
        String family;
        int variant = seededTileVariant(w, x, y, "floor_" + glyph + "_" + roomFloorFamily(w, x, y, glyph), 5, 7.0, 3);
        switch (glyph) {
            case ',': family = "alleyway_cracked"; art = "floor_alleyway_cracked_v" + variant; break;
            case '`': family = "gang_or_trash_rough"; art = "floor_trash_mutant_rough_v" + variant; break;
            case ' ': art = "void_space"; family = "void"; variant = 1; break;
            default:
                family = roomFloorFamily(w, x, y, glyph);
                if ("sewer_room".equals(family)) art = "floor_sewer_room_v" + variant;
                else if ("gang_or_trash_rough".equals(family)) art = "floor_trash_mutant_rough_v" + variant;
                else if ("noble_room".equals(family)) art = "floor_noble_room_v" + variant;
                else art = "floor_bare_underhive_v" + variant;
                break;
        }
        return new CompiledTileDescriptor(glyph, "floor", family, null, variant,
                art, null, null, null, null, false, false, false, false, false, false,
                "tile.floor/" + family + "/v" + variant);
    }

    static CompiledTileDescriptor corridorDescriptor(World w, int x, int y, char glyph) {
        String art;
        String family;
        String orientation = corridorOrientation(w, x, y, glyph);
        switch (glyph) {
            case '=':
                art = "floor_maintenance_corridor";
                family = "maintenance";
                break;
            case ':':
                art = "floor_padded_service_way";
                family = "padded_service_way";
                break;
            case '-':
                family = "exterior_hivewall_maintenance";
                art = "floor_exterior_maintenance_corridor_" + orientation;
                break;
            case '~':
                family = "sewer_pipe";
                art = "floor_sewer_corridor_" + orientation;
                break;
            default:
                if (nobleCorridorContext(w, x, y)) {
                    family = "noble_corridor";
                    art = "floor_noble_corridor_" + orientation;
                } else if (w != null && w.sewerLayer) {
                    family = "sewer_pipe";
                    art = "floor_sewer_corridor_" + orientation;
                } else {
                    art = "floor_industrial_corridor";
                    family = "industrial";
                }
                break;
        }
        return new CompiledTileDescriptor(glyph, "corridor", family, orientation, 1,
                art, null, null, null, null, false, false, false, true, false, false,
                "tile.corridor/" + family + "/" + orientation);
    }

    static boolean nobleCorridorContext(World w, int x, int y) {
        if (isNobleZone(w)) return true;
        if (w == null) return false;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{2,0},{-2,0},{0,2},{0,-2}};
        for (int[] d : dirs) {
            int nx = x + d[0], ny = y + d[1];
            if (w.inBounds(nx, ny) && w.roomFaction(w.roomIdAt(nx, ny)) == Faction.NOBLE) return true;
        }
        return false;
    }

    static boolean isNobleZone(World w) {
        return w != null && (w.zoneType == ZoneType.SECTOR_GOVERNORS_MANSION || w.zoneType == ZoneType.NOBLE_SERVICE_SPINE || w.floor >= 7);
    }

    static boolean sewerContext(World w, int x, int y) {
        if (w == null) return false;
        if (w.sewerLayer || w.zoneType == ZoneType.SEWER_CONDUIT || w.zoneType == ZoneType.MUTANT_SEWER_CAMP || w.zoneType == ZoneType.CULTIST_SEWER_CAMP) return true;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] d : dirs) if (neighborGlyph(w, x + d[0], y + d[1], '~')) return true;
        return false;
    }

    static String roomFloorFamily(World w, int x, int y, char glyph) {
        if (w == null || !w.inBounds(x, y)) return "bare_underhive";
        int rid = w.roomIdAt(x, y);
        Faction f = w.roomFaction(rid);
        if (f == Faction.NOBLE || isNobleZone(w)) return "noble_room";
        if (w.zoneType == ZoneType.MUTANT_SEWER_CAMP || w.zoneType == ZoneType.CULTIST_SEWER_CAMP) return "sewer_room";
        if (w.zoneType == ZoneType.GANGER_TURF || f == Faction.BANDIT || (f != null && f.name().startsWith("GANGER"))) return "gang_or_trash_rough";
        return "bare_underhive";
    }

    static String corridorOrientation(World w, int x, int y, char glyph) {
        boolean ew = corridorLikeSameFamily(w, x - 1, y, glyph) || corridorLikeSameFamily(w, x + 1, y, glyph);
        boolean ns = corridorLikeSameFamily(w, x, y - 1, glyph) || corridorLikeSameFamily(w, x, y + 1, glyph);
        if (ew && !ns) return "east_west";
        if (ns && !ew) return "north_south";
        if (ew && ns) return "junction";
        return "local";
    }

    static boolean corridorLikeSameFamily(World w, int x, int y, char glyph) {
        if (w == null || !w.inBounds(x, y) || w.roomIds[x][y] >= 0) return false;
        char t = w.tiles[x][y];
        if (t == glyph) return true;
        return t == 'D' || t == '/' || t == '|';
    }

    static boolean roadLane(World w, int x, int y) {
        if (w == null || !w.inBounds(x, y) || w.tiles[x][y] != ROAD_LANE) return false;
        if (w.roomIds != null && x >= 0 && y >= 0 && x < w.roomIds.length && y < w.roomIds[x].length && w.roomIds[x][y] >= 0) return false;
        return true;
    }

    static boolean sidewalk(World w, int x, int y) {
        return w != null && w.inBounds(x, y) && w.tiles[x][y] == SIDEWALK && w.roomIds[x][y] < 0;
    }

    static boolean roadLike(World w, int x, int y) {
        // 0.9.10fs: road connectivity means a true carriageway/lane only. Sidewalks
        // are street-family art but must not contribute to road-shape resolution;
        // otherwise every lane bordered by sidewalks degenerates into an intersection.
        return roadLane(w, x, y);
    }

    static boolean streetEdge(World w, int x, int y) {
        return roadLane(w, x, y) || sidewalk(w, x, y);
    }

    static boolean sidewalkPromotesToRoad(World w, int x, int y) {
        if (w == null || !w.inBounds(x, y) || w.tiles[x][y] != SIDEWALK) return false;
        if (w.roomIds != null && x >= 0 && y >= 0 && x < w.roomIds.length && y < w.roomIds[x].length && w.roomIds[x][y] >= 0) return false;
        boolean ew = roadLane(w, x - 1, y) && roadLane(w, x + 1, y);
        boolean ns = roadLane(w, x, y - 1) && roadLane(w, x, y + 1);
        // Sidewalks remain sidewalks for ordinary road shoulders. The only promoted
        // case is the tile trapped between opposed true road lanes at a crossing/median,
        // where leaving a sidewalk glyph creates the broken road gap seen in Sector Audit.
        return ew || ns;
    }

    static boolean corridorLike(World w, int x, int y) {
        if (w == null || !w.inBounds(x, y)) return false;
        char t = w.tiles[x][y];
        return w.roomIds[x][y] < 0 && (w.isCorridorGlyph(t) || t == 'D' || t == '/' || t == '|');
    }

    static int deterministicVariant(int x, int y, String family, int count) {
        return 1 + Math.floorMod(Objects.hash(x / 3, y / 3, family), Math.max(1, count));
    }

    static int seededTileVariant(World w, int x, int y, String family, int count, double scale, int octaves) {
        long seed = w == null ? 0L : w.seed;
        return PerlinNoiseAuthority.variant(seed, x, y, family, count, scale, octaves);
    }

    static char inferredUnderlayGlyph(World w, int x, int y, char overlayGlyph) {
        if (w == null || !w.inBounds(x, y)) return '.';
        Character explicit = explicitUnderlayGlyph(w, x, y);
        if (explicit != null) return explicit.charValue();
        int rid = w.roomIdAt(x, y);
        Faction f = w.roomFaction(rid);
        if (f == Faction.NOBLE) return ',';
        if (w.zoneType == ZoneType.MUTANT_SEWER_CAMP || w.zoneType == ZoneType.CULTIST_SEWER_CAMP) return '~';
        if (w.zoneType == ZoneType.GANGER_TURF || f == Faction.BANDIT || (f != null && f.name().startsWith("GANGER"))) return '`';
        boolean sidewalkNeighbor = sidewalk(w, x - 1, y) || sidewalk(w, x + 1, y) || sidewalk(w, x, y - 1) || sidewalk(w, x, y + 1);
        if (sidewalkNeighbor) return SIDEWALK;
        boolean roadNeighbor = roadLane(w, x - 1, y) || roadLane(w, x + 1, y) || roadLane(w, x, y - 1) || roadLane(w, x, y + 1);
        if (roadNeighbor) return ROAD_LANE;
        if (corridorLike(w, x - 1, y) || corridorLike(w, x + 1, y) || corridorLike(w, x, y - 1) || corridorLike(w, x, y + 1)) {
            return (w.zoneType == ZoneType.MUTANT_SEWER_CAMP || w.zoneType == ZoneType.CULTIST_SEWER_CAMP) ? '~' : '+';
        }
        return '.';
    }

    static Character explicitUnderlayGlyph(World w, int x, int y) {
        if (w == null || !w.inBounds(x, y)) return null;
        MapObjectState m = w.mapObjectAt(x, y);
        if (m == null || m.stockState == null) return null;
        String under = MapObjectState.stockValue(m.stockState, "under");
        if (under == null || under.isBlank()) return null;
        try {
            int code = Integer.parseInt(under.trim());
            if (code >= 0 && code <= Character.MAX_VALUE) {
                char ch = (char)code;
                return overlayAliasForGlyph(ch) == null ? Character.valueOf(ch) : null;
            }
        } catch (NumberFormatException ignored) {
            if (under.length() == 1) {
                char ch = under.charAt(0);
                return overlayAliasForGlyph(ch) == null ? Character.valueOf(ch) : null;
            }
        }
        return null;
    }

    static boolean isWallGlyph(char ch) { return ch == '#' || ch == '%' || ch == '&' || ch == '^' || ch == '8' || ch == '0'; }
    static boolean isDoorGlyph(char ch) { return ch == '/' || ch == '|' || ch == 'L' || ch == 'V' || ch == 'X' || ch == 'D'; }
    static boolean isFloorGlyph(char ch) { return ch == '.' || ch == ',' || ch == '`' || ch == ' '; }
    static boolean isCorridorGlyph(char ch) { return ch == '+' || ch == '=' || ch == ':' || ch == '-' || ch == '~'; }

    static String wallFamilyForGlyph(char ch) {
        switch (ch) {
            case '%': return "support_beam";
            case '&': return "gantry_lattice";
            case '^': return "buried_conveyor";
            case '8': return "pipe_bundle";
            case '0': return "cable_column";
            default: return "bulkhead";
        }
    }

    static String overlayAliasForGlyph(char glyph) {
        switch (glyph) {
            case 'd': return "barricade";
            case '*': return "debris";
            case '?': return "buried_cache";
            case '!': return "danger_marker";
            case 'R': return "rogue_machine";
            case 'N': return "noisy_machinery";
            case '1': return "vending_food";
            case '2': return "vending_armor";
            case '3': return "vending_weapons";
            case '4': return "vending_materials";
            case '5': return "vending_survival";
            case 'Y': return "water_condenser";
            case 'J': return "emergency_assembler";
            case 'B': return "emergency_boiler";
            case 'K': return "micro_lab";
            case 'O': return "emergency_miner";
            case 'Z': return "relay_power_grid";
            case 'P': return "emergency_smelter";
            case 'F': return "steam_engine";
            case 'U': return "steam_engine_disabled";
            case 'w': return "scrap_workbench";
            case 'e': return "water_condenser";
            case 'f': return "emergency_smelter";
            case 'l': return "micro_lab";
            case 'x': return "security_cogitator";
            case 'T': return "turret_or_trade";
            case 'H': return "shrine_or_shield";
            case 'G': return "logistics_center";
            case 'M': return "medicae_or_military";
            case 'k': return "carrying_station";
            case 'q': return "supply_post";
            case 'I': return "imperial_shrine";
            case '$': return "donation_box";
            case 'W': return "saint_alcove";
            case 'Q': return "governor_dais";
            case 'C': return "clinic";
            case 'r': return "corpse_loot";
            case 'o': return "object_generic";
            case 'm': return "sump_fungus_mold";
            case 'S': return "sewer_hatch";
            case 'v': return "ladder_drain";
            case 'E': return "elevator";
            case 's': return "storage_crate";
            case 'c': return "sleeping_cot";
            case 'u': return "water_barrel";
            case 'a': return "alarm_trap";
            case 'p': return "arbites_precinct";
            case 'b': return "bandit_den";
            case 'h': return "hiver_block";
            case 'n': return "noble_secure";
            case 't': return "table_prop";
            case 'g': return "bandit_den";
            case 'A': return "arbites_precinct";
            default: return null;
        }
    }

    static String semanticTagForGlyph(char glyph) {
        switch (glyph) {
            case 'b': case 'g': return "bandit_zone_marker";
            case 'p': case 'A': return "arbites_precinct_marker";
            case 'S': return "sewer_access_marker";
            case 'h': return "hab_block_marker";
            case 'n': return "noble_secure_marker";
            case 'm': return "sump_fungus_mold_marker";
            default: return overlayAliasForGlyph(glyph);
        }
    }

    static String safe(String s) {
        return s == null ? "none" : s.replace('+', '_').replace(' ', '_');
    }
}

final class CompiledTileDescriptor {
    final char sourceGlyph;
    final String baseLayer;
    final String family;
    final String shape;
    final int variant;
    final String primaryArtKey;
    final Character underlayGlyph;
    final String underlayArtKey;
    final String overlayArtKey;
    final String semanticTag;
    final String primaryAssetId;
    final String underlayAssetId;
    final String overlayAssetId;
    final boolean isWall;
    final boolean isRoad;
    final boolean isSidewalk;
    final boolean isCorridor;
    final boolean isDoor;
    final boolean isFixture;
    final String composedKey;

    CompiledTileDescriptor(char sourceGlyph, String baseLayer, String family, String shape, int variant,
                           String primaryArtKey, Character underlayGlyph, String underlayArtKey,
                           String overlayArtKey, String semanticTag, boolean isWall, boolean isRoad,
                           boolean isSidewalk, boolean isCorridor, boolean isDoor, boolean isFixture,
                           String composedKey) {
        this.sourceGlyph = sourceGlyph;
        this.baseLayer = baseLayer;
        this.family = family;
        this.shape = shape;
        this.variant = variant;
        this.primaryArtKey = primaryArtKey;
        this.underlayGlyph = underlayGlyph;
        this.underlayArtKey = underlayArtKey;
        this.overlayArtKey = overlayArtKey;
        this.semanticTag = semanticTag;
        this.primaryAssetId = TileSemanticAssetAuthority.assetIdOrMissing(primaryArtKey);
        this.underlayAssetId = TileSemanticAssetAuthority.assetIdOrMissing(underlayArtKey);
        this.overlayAssetId = TileSemanticAssetAuthority.assetIdOrMissing(overlayArtKey);
        this.isWall = isWall;
        this.isRoad = isRoad;
        this.isSidewalk = isSidewalk;
        this.isCorridor = isCorridor;
        this.isDoor = isDoor;
        this.isFixture = isFixture;
        this.composedKey = composedKey;
    }

    static CompiledTileDescriptor overlay(char sourceGlyph, char underlayGlyph, String underlayArtKey,
                                          String overlayArtKey, String semanticTag, String composedKey) {
        return new CompiledTileDescriptor(sourceGlyph, "fixture", overlayArtKey, null, 1,
                overlayArtKey, underlayGlyph, underlayArtKey, overlayArtKey, semanticTag,
                false, false, false, false, false, true, composedKey);
    }

    boolean hasOverlay() { return overlayArtKey != null && underlayArtKey != null; }

    String inspectLine() {
        StringBuilder sb = new StringBuilder("TILE KEY: ").append(composedKey);
        if (underlayArtKey != null) sb.append(" underlay=").append(underlayArtKey);
        if (overlayArtKey != null) sb.append(" overlay=").append(overlayArtKey);
        if (semanticTag != null) sb.append(" semantic=").append(semanticTag);
        if (primaryAssetId != null) sb.append(" asset=").append(primaryAssetId);
        if (underlayAssetId != null) sb.append(" underlayAsset=").append(underlayAssetId);
        if (overlayAssetId != null) sb.append(" overlayAsset=").append(overlayAssetId);
        return sb.toString();
    }
}
