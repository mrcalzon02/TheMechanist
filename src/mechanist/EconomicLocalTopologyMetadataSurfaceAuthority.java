package mechanist;

import java.util.*;

final class EconomicLocalTopologyMetadataSurfaceAuthority {
    static final String VERSION = "0.9.10eo";

    static final class Surface {
        final ZoneType zone;
        final EconomicTopologyFramework.ZonePurposeProfile profile;
        final EconomicTopologyFramework.PressureType dominantPressure;
        final EconomicTopologyFramework.CirculationClass primaryCirculation;
        final EconomicTopologyFramework.CirculationClass[][] circulationAt;
        final String[] roomRoleByIndex;
        final EnumMap<EconomicTopologyFramework.CirculationClass, Integer> circulationCounts;
        final int roomsReviewed;
        final int roomsTagged;
        final int corridorCells;
        final int doorCells;
        final int openworkCells;
        final String summary;

        Surface(ZoneType zone,
                EconomicTopologyFramework.ZonePurposeProfile profile,
                EconomicTopologyFramework.PressureType dominantPressure,
                EconomicTopologyFramework.CirculationClass primaryCirculation,
                EconomicTopologyFramework.CirculationClass[][] circulationAt,
                String[] roomRoleByIndex,
                EnumMap<EconomicTopologyFramework.CirculationClass, Integer> circulationCounts,
                int roomsReviewed,
                int roomsTagged,
                int corridorCells,
                int doorCells,
                int openworkCells,
                String summary) {
            this.zone = zone;
            this.profile = profile;
            this.dominantPressure = dominantPressure;
            this.primaryCirculation = primaryCirculation;
            this.circulationAt = circulationAt;
            this.roomRoleByIndex = roomRoleByIndex;
            this.circulationCounts = circulationCounts;
            this.roomsReviewed = roomsReviewed;
            this.roomsTagged = roomsTagged;
            this.corridorCells = corridorCells;
            this.doorCells = doorCells;
            this.openworkCells = openworkCells;
            this.summary = summary;
        }

        String roomRole(int roomId) {
            if (roomId < 0 || roomRoleByIndex == null || roomId >= roomRoleByIndex.length) return null;
            return roomRoleByIndex[roomId];
        }

        EconomicTopologyFramework.CirculationClass circulationAt(int x, int y) {
            if (circulationAt == null || x < 0 || y < 0 || x >= circulationAt.length || circulationAt.length == 0 || y >= circulationAt[x].length) return null;
            return circulationAt[x][y];
        }
    }

    static final class Result {
        final Surface surface;
        Result(Surface surface) { this.surface = surface; }
        String summary() {
            if (surface == null) return "localTopologyMetadata version=" + VERSION + " status=no-world";
            String zoneLabel = surface.zone == null ? "unknown zone" : surface.zone.label;
            return "localTopologyMetadata version=" + VERSION
                    + " zone=" + zoneLabel
                    + " roomsTagged=" + surface.roomsTagged + "/" + surface.roomsReviewed
                    + " corridorCells=" + surface.corridorCells
                    + " doorCells=" + surface.doorCells
                    + " openworkCells=" + surface.openworkCells
                    + " primaryCirculation=" + surface.primaryCirculation.label
                    + " dominantPressure=" + surface.dominantPressure.label
                    + " liveEconomy=false";
        }
    }

    static Result apply(World world) {
        if (world == null) return new Result(null);
        EconomicTopologyFramework.ZonePurposeProfile profile = EconomicTopologyFramework.profileFor(world.zoneType);
        EconomicTopologyFramework.PressureType dominantPressure = dominantPressure(profile);
        EconomicTopologyFramework.CirculationClass primaryCirculation = primaryCirculation(profile);
        EconomicTopologyFramework.CirculationClass[][] circulationAt = new EconomicTopologyFramework.CirculationClass[world.w][world.h];
        EnumMap<EconomicTopologyFramework.CirculationClass, Integer> counts = new EnumMap<>(EconomicTopologyFramework.CirculationClass.class);
        for (EconomicTopologyFramework.CirculationClass c : EconomicTopologyFramework.CirculationClass.values()) counts.put(c, 0);

        int corridorCells = 0;
        int doorCells = 0;
        int openworkCells = 0;
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) {
                char tile = world.tiles[x][y];
                if (world.roomIds[x][y] >= 0) continue;
                boolean corridor = world.isCorridorGlyph(tile) || tile == '=' || tile == ':' || tile == '+' || tile == '/' || tile == 'D';
                boolean door = world.isDoorSymbol(tile) || tile == 'D';
                if (!corridor && !door) continue;
                EconomicTopologyFramework.CirculationClass cc = classifyCorridor(profile, tile);
                circulationAt[x][y] = cc;
                counts.put(cc, counts.getOrDefault(cc, 0) + 1);
                if (door) doorCells++;
                else if (tile == '+' || tile == ';' || tile == '_' || tile == '~') openworkCells++;
                else corridorCells++;
            }
        }

        int reviewed = world.roomProfiles == null ? 0 : world.roomProfiles.size();
        String[] roomRoles = new String[reviewed];
        int tagged = 0;
        ArrayList<String> notes = new ArrayList<>();
        for (int i = 0; i < reviewed; i++) {
            RoomProfile rp = world.roomProfiles.get(i);
            Faction owner = i < world.roomFactions.size() ? world.roomFactions.get(i) : Faction.NONE;
            boolean special = i < world.roomSpecials.size() && Boolean.TRUE.equals(world.roomSpecials.get(i));
            String role = classifyRoom(profile, dominantPressure, primaryCirculation, rp, i, owner, special);
            roomRoles[i] = role;
            if (role != null && !role.isBlank()) {
                tagged++;
                if (notes.size() < 12) notes.add("room " + i + " topology tag: " + role);
            }
        }

        String summary = summaryLine(world.zoneType, profile, dominantPressure, primaryCirculation, tagged, reviewed, corridorCells, doorCells, openworkCells, counts);
        Surface surface = new Surface(world.zoneType, profile, dominantPressure, primaryCirculation, circulationAt, roomRoles, counts, reviewed, tagged, corridorCells, doorCells, openworkCells, summary);
        world.localTopologyMetadataSurface = surface;
        world.localTopologyMetadataSummary = summary;
        world.localTopologyMetadataNotes.clear();
        world.localTopologyMetadataNotes.addAll(notes);
        return new Result(surface);
    }

    static ArrayList<String> infopediaLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Local Topology Metadata Surface Authority " + VERSION);
        lines.add("Purpose: consolidates the active zone's Phase 3.5 purpose, pressure, room-role, and corridor-circulation readings into one cached local metadata surface.");
        lines.add("Why it exists: map rendering, Look inspection, construction validation, route previews, logistics planning, and later ledger work should query the same local reading instead of reinterpreting rooms and corridors separately.");
        lines.add("Current behavior: after world generation and repair, rooms receive cached role tags and corridor/openwork cells receive semantic circulation classes. The cache is descriptive and local to the loaded zone.");
        lines.add("Boundary: no live hauling reservations, no production scheduling, no worker assignment, no pathfinding, no district conversion, and no global pressure propagation.");
        lines.add("Next dependency: construction and logistics preview authorities can start reading this surface for warnings and intent language before any live economy gate opens.");
        return lines;
    }

    static ArrayList<String> statusLines(World world) {
        ArrayList<String> lines = new ArrayList<>();
        if (world == null) return lines;
        ensure(world);
        if (world.localTopologyMetadataSummary != null && !world.localTopologyMetadataSummary.isBlank()) lines.add("Local topology metadata: " + world.localTopologyMetadataSummary);
        if (world.localTopologyMetadataNotes != null && !world.localTopologyMetadataNotes.isEmpty()) {
            lines.add("Cached topology samples:");
            int limit = Math.min(8, world.localTopologyMetadataNotes.size());
            for (int i = 0; i < limit; i++) lines.add(world.localTopologyMetadataNotes.get(i));
        }
        return lines;
    }

    static ArrayList<String> auditLines(World world) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Local Topology Metadata Surface audit");
        lines.add("Authority version: " + VERSION + ".");
        if (world == null) {
            lines.add("No active world loaded for local topology cache inspection.");
        } else {
            ensure(world);
            Surface s = world.localTopologyMetadataSurface;
            lines.add(world.localTopologyMetadataSummary == null ? "No local topology metadata summary recorded." : world.localTopologyMetadataSummary);
            if (s != null) {
                lines.add("Room role tags cached: " + s.roomsTagged + "/" + s.roomsReviewed + ".");
                lines.add("Corridor cells cached: " + s.corridorCells + "; door/openwork cells cached: " + (s.doorCells + s.openworkCells) + ".");
                lines.add("Circulation counts: " + circulationCountsLine(s));
            }
            if (world.localTopologyMetadataNotes != null && !world.localTopologyMetadataNotes.isEmpty()) {
                lines.add("Sample cached room roles:");
                lines.addAll(world.localTopologyMetadataNotes);
            }
        }
        lines.add("Rule check: this authority stores local descriptive metadata only. It does not schedule labor, move stock, mutate districts, or compute global pressure.");
        return lines;
    }

    static ArrayList<String> worldSummaryLines(World world) {
        ArrayList<String> lines = new ArrayList<>();
        if (world == null) return lines;
        ensure(world);
        if (world.localTopologyMetadataSummary != null && !world.localTopologyMetadataSummary.isBlank()) lines.add("Local topology cache: " + world.localTopologyMetadataSummary);
        return lines;
    }

    static String roomInspectionLine(World world, int roomId) {
        if (world == null || roomId < 0) return null;
        ensure(world);
        Surface s = world.localTopologyMetadataSurface;
        if (s == null) return null;
        String role = s.roomRole(roomId);
        if (role == null || role.isBlank()) return null;
        return "LOCAL TOPOLOGY ROLE: " + role + " This is cached descriptive metadata, not a live production assignment.";
    }

    static String corridorInspectionLine(World world, int x, int y) {
        if (world == null || !world.inBounds(x, y)) return null;
        ensure(world);
        Surface s = world.localTopologyMetadataSurface;
        if (s == null) return null;
        EconomicTopologyFramework.CirculationClass c = s.circulationAt(x, y);
        if (c == null) return null;
        return "LOCAL TOPOLOGY CIRCULATION: " + c.label + " under " + s.dominantPressure.label + " pressure; cached local routing metadata, not a hauling reservation.";
    }

    private static void ensure(World world) {
        if (world != null && world.localTopologyMetadataSurface == null) apply(world);
    }

    private static String summaryLine(ZoneType zone,
                                      EconomicTopologyFramework.ZonePurposeProfile profile,
                                      EconomicTopologyFramework.PressureType dominantPressure,
                                      EconomicTopologyFramework.CirculationClass primaryCirculation,
                                      int tagged,
                                      int reviewed,
                                      int corridorCells,
                                      int doorCells,
                                      int openworkCells,
                                      EnumMap<EconomicTopologyFramework.CirculationClass, Integer> counts) {
        String zoneLabel = zone == null ? "unknown zone" : zone.label;
        String purpose = profile == null || profile.purpose == null ? "unclassified purpose" : profile.purpose.label;
        String age = profile == null || profile.ageBand == null ? "unknown infrastructure age" : profile.ageBand.label;
        EconomicTopologyFramework.CirculationClass second = secondaryCirculation(counts, primaryCirculation);
        return zoneLabel + " cached as " + purpose + "; " + age + "; dominant pressure " + dominantPressure.label + "; primary circulation " + primaryCirculation.label + (second == null ? "" : "; secondary circulation " + second.label) + "; rooms tagged " + tagged + "/" + reviewed + "; corridor/openwork cells " + corridorCells + "/" + openworkCells + "; doors " + doorCells + ".";
    }

    private static EconomicTopologyFramework.CirculationClass secondaryCirculation(EnumMap<EconomicTopologyFramework.CirculationClass, Integer> counts, EconomicTopologyFramework.CirculationClass primary) {
        EconomicTopologyFramework.CirculationClass best = null;
        int bestValue = 0;
        if (counts == null) return null;
        for (EconomicTopologyFramework.CirculationClass c : EconomicTopologyFramework.CirculationClass.values()) {
            if (c == primary) continue;
            int v = counts.getOrDefault(c, 0);
            if (v > bestValue) { bestValue = v; best = c; }
        }
        return bestValue > 0 ? best : null;
    }

    private static String circulationCountsLine(Surface s) {
        if (s == null || s.circulationCounts == null) return "none";
        ArrayList<String> parts = new ArrayList<>();
        for (EconomicTopologyFramework.CirculationClass c : EconomicTopologyFramework.CirculationClass.values()) {
            int v = s.circulationCounts.getOrDefault(c, 0);
            if (v > 0) parts.add(c.label + " " + v);
        }
        return parts.isEmpty() ? "none" : String.join(", ", parts);
    }

    private static String classifyRoom(EconomicTopologyFramework.ZonePurposeProfile profile,
                                       EconomicTopologyFramework.PressureType dominantPressure,
                                       EconomicTopologyFramework.CirculationClass primaryCirculation,
                                       RoomProfile rp,
                                       int roomIndex,
                                       Faction owner,
                                       boolean special) {
        if (profile == null || rp == null) return null;
        String text = roomText(rp);
        String ownerText = owner == null || owner == Faction.NONE ? "neutral" : owner.label;
        if (roomIndex == 0) return "central district nexus for " + profile.purpose.label + "; anchors local movement, inspection, and future route intent.";
        if (special) return "special-purpose node inside " + profile.purpose.label + "; preserve as a named exception before automated systems read the room.";
        if (containsAny(text, "warehouse", "storehouse", "depot", "cargo", "rail", "ration", "armory", "freight")) return "supply/throughput room; future logistics previews may treat it as a stock or transfer candidate for " + primaryCirculation.label + ".";
        if (containsAny(text, "forge", "machine", "workshop", "assembler", "smelter", "relay", "boiler", "laboratorium", "maintenance")) return "technical support room; future construction and production previews should read it through " + dominantPressure.label + " pressure.";
        if (containsAny(text, "dormitory", "barracks", "hab", "servant", "kitchen", "laundry", "mess", "clinic")) return "labor-support room; it explains where workforce pressure is housed, fed, inspected, or exhausted.";
        if (containsAny(text, "security", "watch", "holding", "checkpoint", "evidence", "guard", "barricade")) return "access-control room; future route intent can treat it as a permissions or inspection chokepoint.";
        if (containsAny(text, "shrine", "chapel", "temple", "relic", "ritual", "pilgrim")) return "ritual legitimacy room; social and religious pressure should be visible before any later simulation consumes it.";
        if (containsAny(text, "trash", "scrap", "sewer", "sluice", "recycler", "fungus", "sanitation")) return "decay/reclamation room; it belongs to the local waste, salvage, or maintenance ecology.";
        if (containsAny(text, "storefront", "counter", "pawn", "debt", "barter", "market", "trade")) return "exchange-facing room; future market and contraband previews can read it without creating live stock churn.";
        return ownerText + " room carrying background " + dominantPressure.label + " pressure from the " + profile.purpose.label + ".";
    }

    private static EconomicTopologyFramework.PressureType dominantPressure(EconomicTopologyFramework.ZonePurposeProfile profile) {
        EconomicTopologyFramework.PressureType best = EconomicTopologyFramework.PressureType.INDUSTRIAL;
        int bestValue = -1;
        if (profile == null || profile.pressure == null) return best;
        for (EconomicTopologyFramework.PressureType t : EconomicTopologyFramework.PressureType.values()) {
            int v = profile.pressure.get(t);
            if (v > bestValue) { bestValue = v; best = t; }
        }
        return best;
    }

    private static EconomicTopologyFramework.CirculationClass primaryCirculation(EconomicTopologyFramework.ZonePurposeProfile profile) {
        if (profile != null && profile.circulation != null && !profile.circulation.isEmpty()) return profile.circulation.get(0);
        return EconomicTopologyFramework.CirculationClass.PUBLIC_SERVICE_SPINE;
    }

    private static EconomicTopologyFramework.CirculationClass classifyCorridor(EconomicTopologyFramework.ZonePurposeProfile profile, char tile) {
        if (tile == '=' || tile == ':' || tile == 'T') {
            if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.FREIGHT_ARTERY)) return EconomicTopologyFramework.CirculationClass.FREIGHT_ARTERY;
            if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.CARGO_CORRIDOR)) return EconomicTopologyFramework.CirculationClass.CARGO_CORRIDOR;
            if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.NOBLE_BOULEVARD)) return EconomicTopologyFramework.CirculationClass.NOBLE_BOULEVARD;
            if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.BARRACKS_ACCESS_GRID)) return EconomicTopologyFramework.CirculationClass.BARRACKS_ACCESS_GRID;
        }
        if (tile == '~' || tile == ';' || tile == '_') {
            if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.SEWER_TRUNK)) return EconomicTopologyFramework.CirculationClass.SEWER_TRUNK;
            if (usesCirculation(profile, EconomicTopologyFramework.CirculationClass.MAINTENANCE_TUNNEL)) return EconomicTopologyFramework.CirculationClass.MAINTENANCE_TUNNEL;
        }
        if (tile == '+' || tile == '/' || tile == 'D' || tile == 'X' || tile == 'V') return primaryCirculation(profile);
        return primaryCirculation(profile);
    }

    private static boolean usesCirculation(EconomicTopologyFramework.ZonePurposeProfile profile, EconomicTopologyFramework.CirculationClass c) {
        return profile != null && profile.circulation != null && profile.circulation.contains(c);
    }

    private static boolean containsAny(String haystack, String... needles) {
        if (haystack == null) return false;
        for (String n : needles) if (n != null && haystack.contains(n)) return true;
        return false;
    }

    private static String roomText(RoomProfile rp) {
        if (rp == null) return "";
        return ((rp.name == null ? "" : rp.name) + " "
                + (rp.descriptor == null ? "" : rp.descriptor) + " "
                + (rp.featureText == null ? "" : rp.featureText)).toLowerCase(Locale.ROOT);
    }

    private EconomicLocalTopologyMetadataSurfaceAuthority() {}
}
