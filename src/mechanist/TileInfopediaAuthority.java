package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;

import java.util.*;

/**
 * Developer-facing tile reference ledger for the Infopedia. It lists the compact
 * glyph, compiled descriptor family, renderer alias, and allowed art variants so
 * Zone Audit can verify which tile/room/wall families are resolving to which
 * icons without relying on hidden loader state or a pile of debug filler text.
 */
final class TileInfopediaAuthority {
    static final String VERSION = "0.9.10ji";

    static final class Entry {
        final String label;
        final String category;
        final Character glyph;
        final String descriptor;
        final String roomUse;
        final String[] aliases;
        final String[] lines;

        Entry(String label, String category, Character glyph, String descriptor, String roomUse, String[] aliases, String... lines) {
            this.label = label;
            this.category = category;
            this.glyph = glyph;
            this.descriptor = descriptor;
            this.roomUse = roomUse;
            this.aliases = aliases == null ? new String[0] : aliases;
            this.lines = lines == null ? new String[0] : lines;
        }

        String primaryAlias() { return aliases.length == 0 ? null : aliases[0]; }
    }

    private static final ArrayList<Entry> ENTRIES = new ArrayList<>();
    private static final LinkedHashMap<String, Entry> BY_LABEL = new LinkedHashMap<>();

    static {
        add(new Entry("Floor / Bare Underhive", "FLOORS", '.', "tile.floor/bare_underhive/v1-5", "ordinary civilian rooms, room fallback, legal room fill", variants("floor_bare_underhive_v", 5),
                "Base layer: floor.", "Allowed variants: floor_bare_underhive_v1 through v5.", "Assigned when a room has no stronger faction/floor family."));
        add(new Entry("Floor / Industrial Room", "FLOORS", '.', "tile.floor/industrial_room/v1-5", "industrial/civic room support and fallback industrial surfaces", variants("floor_industrial_room_v", 5),
                "Base layer: floor.", "Allowed variants: floor_industrial_room_v1 through v5.", "Used as an industrial room-material family where explicitly selected."));
        add(new Entry("Floor / Sewer Room", "FLOORS", '.', "tile.floor/sewer_room/v1-5", "sewer rooms, mutant sewer camps, cultist sewer camps", variants("floor_sewer_room_v", 5),
                "Base layer: floor.", "Allowed variants: floor_sewer_room_v1 through v5.", "Room floor family for sewer-side rooms; separate from sewer corridor art."));
        add(new Entry("Floor / Trash Mutant Rough", "FLOORS", '`', "tile.floor/gang_or_trash_rough/v1-5", "gang turf, mutant rough rooms, trash pockets", variants("floor_trash_mutant_rough_v", 5),
                "Base layer: floor.", "Allowed variants: floor_trash_mutant_rough_v1 through v5.", "Used for degraded rooms and trash/mutant context."));
        add(new Entry("Floor / Alleyway Cracked", "FLOORS", ',', "tile.floor/alleyway_cracked/v1-5", "alleys, cracked approaches, exterior civilian floor breakup", variants("floor_alleyway_cracked_v", 5),
                "Base layer: floor.", "Allowed variants: floor_alleyway_cracked_v1 through v5.", "Used for cracked approach tiles and alley material."));
        add(new Entry("Floor / Noble Room", "FLOORS", '.', "tile.floor/noble_room/v1-5", "noble rooms, governor mansion, noble service spine", variants("floor_noble_room_v", 5),
                "Base layer: floor.", "Allowed variants: floor_noble_room_v1 through v5.", "Room-owned noble floors override generic interwall floor selection."));
        add(new Entry("Void / Empty Black", "FLOORS", ' ', "tile.floor/void/v1", "outside playable envelope and beyond outer bulkheads", new String[]{"void_space"},
                "Base layer: void floor.", "Allowed variant: void_space.", "This should read as black empty nothingness, not a generic floor tile."));

        add(new Entry("Corridor / Industrial", "CORRIDORS", '+', "tile.corridor/industrial/north_south|east_west|junction|local", "ordinary internal circulation", new String[]{"floor_industrial_corridor"},
                "Base layer: corridor.", "Orientation is resolved by neighboring corridor-like cells.", "Used for normal interior corridors unless sewer/noble/exterior context overrides it."));
        add(new Entry("Corridor / Maintenance", "CORRIDORS", '=', "tile.corridor/maintenance/north_south|east_west|junction|local", "service corridors and maintenance access", new String[]{"floor_maintenance_corridor"},
                "Base layer: corridor.", "Uses the maintenance corridor family, not road art."));
        add(new Entry("Corridor / Padded Service Way", "CORRIDORS", ':', "tile.corridor/padded_service_way/north_south|east_west|junction|local", "soft service access, institutional interiors", new String[]{"floor_padded_service_way"},
                "Base layer: corridor.", "Used where the generator writes the padded service glyph."));
        add(new Entry("Corridor / Sewer North-South", "CORRIDORS", '~', "tile.corridor/sewer_pipe/north_south", "sewer conduits and sewer camps", new String[]{"floor_sewer_corridor_north_south"},
                "Base layer: sewer corridor.", "Sewer corridors use the imported sewer corridor art, not generic industrial corridor art."));
        add(new Entry("Corridor / Sewer East-West", "CORRIDORS", '~', "tile.corridor/sewer_pipe/east_west", "sewer conduits and sewer camps", new String[]{"floor_sewer_corridor_east_west"},
                "Base layer: sewer corridor.", "This is the rotated east/west sewer run."));
        add(new Entry("Corridor / Sewer Junction", "CORRIDORS", '~', "tile.corridor/sewer_pipe/junction", "sewer intersections", new String[]{"floor_sewer_corridor_junction", "floor_sewer_corridor_intersection"},
                "Base layer: sewer corridor.", "Allowed sewer intersection aliases: sewer junction/intersection."));
        add(new Entry("Corridor / Exterior Maintenance North-South", "CORRIDORS", '-', "tile.corridor/exterior_hivewall_maintenance/north_south", "void-edge maintenance corridor", new String[]{"floor_exterior_maintenance_corridor_north_south"},
                "Base layer: exterior maintenance corridor.", "Uses the imported column-5 external/void corridor family."));
        add(new Entry("Corridor / Exterior Maintenance East-West", "CORRIDORS", '-', "tile.corridor/exterior_hivewall_maintenance/east_west", "void-edge maintenance corridor", new String[]{"floor_exterior_maintenance_corridor_east_west"},
                "Base layer: exterior maintenance corridor.", "Rotated external maintenance corridor run."));
        add(new Entry("Corridor / Exterior Maintenance Junction", "CORRIDORS", '-', "tile.corridor/exterior_hivewall_maintenance/junction", "void-edge maintenance corridor intersections", new String[]{"floor_exterior_maintenance_corridor_junction", "floor_exterior_maintenance_corridor_local"},
                "Base layer: exterior maintenance corridor.", "Allowed aliases: exterior maintenance junction/local."));
        add(new Entry("Corridor / Noble North-South", "CORRIDORS", '+', "tile.corridor/noble_corridor/north_south", "noble corridors and governor mansion service routes", new String[]{"floor_noble_corridor_north_south"},
                "Base layer: noble corridor.", "Noble corridor context overrides generic corridor art."));
        add(new Entry("Corridor / Noble East-West", "CORRIDORS", '+', "tile.corridor/noble_corridor/east_west", "noble corridors and governor mansion service routes", new String[]{"floor_noble_corridor_east_west"},
                "Base layer: noble corridor.", "Rotated noble corridor run."));
        add(new Entry("Corridor / Noble Junction", "CORRIDORS", '+', "tile.corridor/noble_corridor/junction|local", "noble corridor intersections", new String[]{"floor_noble_corridor_junction", "floor_noble_corridor_local"},
                "Base layer: noble corridor.", "Allowed aliases: noble junction/local."));

        add(new Entry("Road / North-South", "ROADS", ';', "tile.road/north_south/v1-5", "map-wide street lanes", variants("road_north_south_v", 5),
                "Base layer: road.", "Allowed variants: road_north_south_v1 through v5.", "Variant set is selected once per map for cohesion."));
        add(new Entry("Road / East-West", "ROADS", ';', "tile.road/east_west/v1-5", "map-wide street lanes", variants("road_east_west_v", 5),
                "Base layer: road.", "Allowed variants: road_east_west_v1 through v5.", "Variant set is selected once per map for cohesion."));
        add(new Entry("Road / Intersection", "ROADS", ';', "tile.road/intersection/v1-5", "true opposed continuity on both road axes", variants("road_intersection_v", 5),
                "Base layer: road.", "Allowed variants: road_intersection_v1 through v5.", "Only true road-lane continuity on both axes may produce an intersection."));
        add(new Entry("Road / Sidewalk", "ROADS", '_', "tile.road/sidewalk/v1-5", "street shoulders and pedestrian edges", variants("road_sidewalk_v", 5),
                "Base layer: road family, sidewalk flag true.", "Allowed variants: road_sidewalk_v1 through v5.", "Sidewalks do not count as ordinary road-neighbor connectors."));
        add(new Entry("Road / Corner West-North", "ROADS", ';', "tile.road/corner_west_north/v1", "direction-aware rounded street corner", new String[]{"road_corner_west_north"},
                "Base layer: road.", "Direction-specific corner alias; not a random row-3 road variant."));
        add(new Entry("Road / Corner West-South", "ROADS", ';', "tile.road/corner_west_south/v1", "direction-aware rounded street corner", new String[]{"road_corner_west_south"},
                "Base layer: road.", "Direction-specific corner alias; not a random row-3 road variant."));
        add(new Entry("Road / Corner East-North", "ROADS", ';', "tile.road/corner_east_north/v1", "direction-aware rounded street corner", new String[]{"road_corner_east_north"},
                "Base layer: road.", "Direction-specific corner alias; not a random row-3 road variant."));
        add(new Entry("Road / Corner East-South", "ROADS", ';', "tile.road/corner_east_south/v1", "direction-aware rounded street corner", new String[]{"road_corner_east_south"},
                "Base layer: road.", "Direction-specific corner alias; not a random row-3 road variant."));
        add(new Entry("Road / End North", "ROADS", ';', "tile.road/end_north/v1", "direction-aware road cap", new String[]{"road_end_north"},
                "Base layer: road.", "Rounded cap rotated to face the single connected road side."));
        add(new Entry("Road / End South", "ROADS", ';', "tile.road/end_south/v1", "direction-aware road cap", new String[]{"road_end_south"},
                "Base layer: road.", "Rounded cap rotated to face the single connected road side."));
        add(new Entry("Road / End East", "ROADS", ';', "tile.road/end_east/v1", "direction-aware road cap", new String[]{"road_end_east"},
                "Base layer: road.", "Rounded cap rotated to face the single connected road side."));
        add(new Entry("Road / End West", "ROADS", ';', "tile.road/end_west/v1", "direction-aware road cap", new String[]{"road_end_west"},
                "Base layer: road.", "Rounded cap rotated to face the single connected road side."));

        add(new Entry("Wall / Generic Bulkhead", "WALLS", '#', "tile.wall/bulkhead/v1-5", "ordinary room walls and interwall mesh", variants("wall_bulkhead_v", 5),
                "Base layer: wall.", "Allowed variants: wall_bulkhead_v1 through v5.", "Room-owned walls should resolve from room context before interwall fallback."));
        add(new Entry("Wall / Noble Bulkhead", "WALLS", '#', "tile.wall/noble_bulkhead/v1-5", "noble rooms and noble zones", variants("wall_noble_bulkhead_v", 5),
                "Base layer: wall.", "Allowed variants: wall_noble_bulkhead_v1 through v5.", "Assigned by adjacent noble room or noble zone context."));
        add(new Entry("Wall / Sewer Bulkhead", "WALLS", '#', "tile.wall/sewer_bulkhead/v1-5", "sewer rooms and sewer-adjacent rooms", variants("wall_sewer_bulkhead_v", 5),
                "Base layer: wall.", "Allowed variants: wall_sewer_bulkhead_v1 through v5.", "Assigned by sewer layer/zone/corridor context."));
        add(new Entry("Wall / Exterior Maintenance Bulkhead", "WALLS", '#', "tile.wall/exterior_maintenance_bulkhead/v1-3", "inner and outer exterior maintenance envelope", variants("wall_exterior_maintenance_bulkhead_v", 3),
                "Base layer: wall.", "Allowed variants: wall_exterior_maintenance_bulkhead_v1 through v3.", "Used near exterior maintenance corridor/void boundary."));
        add(new Entry("Wall / Support Beam", "WALLS", '%', "tile.wall/support_beam", "structural support feature", new String[]{"wall_support_beam"},
                "Base layer: wall.", "Single structural wall alias."));
        add(new Entry("Wall / Gantry Lattice", "WALLS", '&', "tile.wall/gantry_lattice", "industrial lattice wall", new String[]{"wall_gantry_lattice"},
                "Base layer: wall.", "Single lattice wall alias."));
        add(new Entry("Wall / Pipe Bundle", "WALLS", '8', "tile.wall/pipe_bundle", "service wall and industrial pipe obstruction", new String[]{"wall_pipe_bundle"},
                "Base layer: wall.", "Single pipe-wall alias."));
        add(new Entry("Wall / Cable Column", "WALLS", '0', "tile.wall/cable_column", "service column obstruction", new String[]{"wall_cable_column"},
                "Base layer: wall.", "Single cable-column alias."));
        add(new Entry("Wall / Buried Conveyor", "WALLS", '^', "tile.wall/buried_conveyor", "industrial buried-conveyor obstruction", new String[]{"wall_buried_conveyor"},
                "Base layer: wall.", "Single conveyor-wall alias."));

        add(new Entry("Door / Archway", "DOORS", '/', "tile.door/door_archway", "open arch/room threshold", new String[]{"door_archway"},
                "Base layer: door.", "Used by archway glyphs."));
        add(new Entry("Door / Standard", "DOORS", '|', "tile.door/door_standard", "standard room door", new String[]{"door_standard"},
                "Base layer: door.", "Used by standard door glyphs."));
        add(new Entry("Door / Locked", "DOORS", 'L', "tile.door/door_locked", "locked room door", new String[]{"door_locked"},
                "Base layer: door.", "Used by locked door glyphs."));
        add(new Entry("Door / Vent Panel", "DOORS", 'V', "tile.door/door_vent_panel", "vent/service hatch", new String[]{"door_vent_panel"},
                "Base layer: door.", "Used by vent panel glyphs."));
        add(new Entry("Door / Security", "DOORS", 'X', "tile.door/door_security", "secured threshold", new String[]{"door_security"},
                "Base layer: door.", "Used by security door glyphs."));
        add(new Entry("Door / Double Sector", "DOORS", 'D', "tile.door/door_double", "maintenance-bulkhead double doors and major transition doors", new String[]{"door_double"},
                "Base layer: door.", "Used for road-end double doors at the inner maintenance bulkhead and other major transition doors."));

        add(new Entry("Fixture / Concord Shrine", "FIXTURES", 'I', "tile.overlay/imperial_shrine", "roadside shrine, chapel room, morale fixture", new String[]{"imperial_shrine", "shrine_or_shield"},
                "Overlay/fixture tile.", "Draws over an inferred underlay; should not render on blackness unless the underlay is void."));
        add(new Entry("Fixture / Vending Food", "FIXTURES", '1', "tile.overlay/vending_food", "food source, civilian support, faction room support", new String[]{"vending_food"},
                "Overlay/fixture tile.", "Provides visible food-source support for rooms and frontage."));
        add(new Entry("Fixture / Vending Survival", "FIXTURES", '5', "tile.overlay/vending_survival", "survival vending and support store", new String[]{"vending_survival"},
                "Overlay/fixture tile.", "Used by survival vending placement."));
        add(new Entry("Fixture / Water Barrel", "FIXTURES", 'u', "tile.overlay/water_barrel", "room and survival water support", new String[]{"water_barrel"},
                "Overlay/fixture tile.", "Water support fixture."));
        add(new Entry("Fixture / Sleeping Cot", "FIXTURES", 'c', "tile.overlay/sleeping_cot", "habitation and resident support", new String[]{"sleeping_cot"},
                "Overlay/fixture tile.", "Resident rooms should use this or equivalent sleep support when population exists."));
        add(new Entry("Fixture / Storage Crate", "FIXTURES", 's', "tile.overlay/storage_crate", "logistics rooms and storage support", new String[]{"storage_crate"},
                "Overlay/fixture tile.", "Storage support for logistics and production rooms."));
        add(new Entry("Fixture / Table Prop", "FIXTURES", 't', "tile.overlay/table_prop", "workrooms, domestic rooms, counters", new String[]{"table_prop"},
                "Overlay/fixture tile.", "Generic table/work-surface fixture."));
        add(new Entry("Fixture / Medicae or Military", "FIXTURES", 'M', "tile.overlay/medicae_or_military", "clinic, medicae, military service fixtures", new String[]{"medicae_or_military"},
                "Overlay/fixture tile.", "Medical/military service fixture bucket."));
        add(new Entry("Fixture / Logistics Center", "FIXTURES", 'G', "tile.overlay/logistics_center", "logistics room, stockroom, faction supply", new String[]{"logistics_center"},
                "Overlay/fixture tile.", "Faction logistics support fixture."));
    }

    private static void add(Entry e) {
        ENTRIES.add(e);
        BY_LABEL.put(e.label, e);
    }

    private static String[] variants(String prefix, int count) {
        String[] out = new String[Math.max(0, count)];
        for (int i = 0; i < out.length; i++) out[i] = prefix + (i + 1);
        return out;
    }

    static List<String> entryLabels() {
        ArrayList<String> out = new ArrayList<>();
        String last = "";
        for (Entry e : ENTRIES) {
            if (!Objects.equals(last, e.category)) {
                out.add("-- " + e.category + " --");
                last = e.category;
            }
            out.add(e.label);
        }
        return out;
    }

    static boolean isHeader(String label) { return label != null && label.startsWith("-- "); }

    static Entry find(String label) {
        if (label == null) return null;
        Entry e = BY_LABEL.get(label);
        if (e != null) return e;
        for (Entry x : ENTRIES) if (label.endsWith(x.label)) return x;
        return null;
    }

    static List<String> detailLines(String label, TileArtSystem art) {
        Entry e = find(label);
        ArrayList<String> lines = new ArrayList<>();
        if (e == null) {
            lines.add("Select a tile entry to inspect its glyph, descriptor, art aliases, and room/faction assignment rule.");
            return lines;
        }
        lines.add(e.label);
        lines.add("Category: " + e.category);
        lines.add("Glyph: " + (e.glyph == null ? "descriptor-only" : printableGlyph(e.glyph)));
        lines.add("Descriptor: " + e.descriptor);
        lines.add("Assignment: " + e.roomUse);
        lines.add("Icon source: " + (e.primaryAlias() == null ? "none" : e.primaryAlias()));
        lines.add("Allowed variants: " + (e.aliases.length == 0 ? "none" : String.join(", ", e.aliases)));
        lines.add("Loaded aliases: " + loadedAliasSummary(e, art));
        lines.add("Semantic IDs: " + semanticAliasSummary(e));
        lines.add("");
        Collections.addAll(lines, e.lines);
        return lines;
    }


    static String semanticAliasSummary(Entry e) {
        if (e == null || e.aliases.length == 0) return "none";
        ArrayList<String> out = new ArrayList<>();
        for (String alias : e.aliases) {
            Optional<String> id = TileSemanticAssetAuthority.assetIdForAlias(alias);
            if (id.isEmpty()) {
                out.add(alias + "=unmapped");
                continue;
            }
            Optional<AssetMetadata> meta = AssetManager.metadata(id.get());
            if (meta.isPresent()) out.add(alias + "=" + id.get() + " (" + meta.get().type().displayName() + ")");
            else out.add(alias + "=" + id.get() + " (missing registry row)");
        }
        return String.join(", ", out);
    }

    static String loadedAliasSummary(Entry e, TileArtSystem art) {
        if (e == null || art == null || e.aliases.length == 0) return "no runtime art loaded";
        TileImageRegistry registry = art.getRegistry();
        if (registry == null) return "no runtime art loaded";
        int loaded = 0;
        ArrayList<String> missing = new ArrayList<>();
        for (String a : e.aliases) {
            if (registry.getAlias(a) != null) loaded++;
            else missing.add(a);
        }
        if (missing.isEmpty()) return loaded + "/" + e.aliases.length + " present";
        return loaded + "/" + e.aliases.length + " present; missing " + String.join(", ", missing);
    }

    static String printableGlyph(char glyph) {
        if (glyph == ' ') return "space / void";
        return "'" + glyph + "' / code " + ((int) glyph);
    }
}
