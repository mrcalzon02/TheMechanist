package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/** Generates the physical receiving point used by external cargo and personnel. */
final class FactionImportNodeGenerationAuthority {
    static final String TYPE = "faction-import-node";
    static final String TAG = "phase16-import-node";

    record Result(int roomsPromoted, int nodesPlaced, String kind, String faction) {
        String summary() {
            return "roomsPromoted=" + roomsPromoted + " nodesPlaced=" + nodesPlaced
                    + " kind=" + kind + " faction=" + faction;
        }
    }

    private record NodeKind(String id, String roomName, String markerName, String description) { }

    private static final NodeKind SECTOR_EXCHANGE = new NodeKind(
            "sector-exchange", "Sector Exchange Cargo Station", "Sector Exchange Cargo Gate",
            "a rail-linked cargo station with an import intake roster, freight staging, and receiving control");
    private static final NodeKind RAIL_CARGO = new NodeKind(
            "rail-cargo-station", "Rail Cargo Station", "Rail Cargo Platform",
            "a rail-linked cargo platform with an import intake roster, loading bay, and freight cages");
    private static final NodeKind FREIGHT_ELEVATOR = new NodeKind(
            "freight-elevator", "Freight Elevator Intake", "Freight Elevator Gate",
            "a heavy freight elevator with an import intake roster, cargo staging, and lift controls");
    private static final NodeKind SERVICE_LIFT = new NodeKind(
            "service-lift", "Service Lift Receiving Room", "Service Lift Gate",
            "a controlled service lift with an import intake roster and compact receiving bay");
    private static final NodeKind CUSTOMS = new NodeKind(
            "customs-checkpoint", "Customs and Import Checkpoint", "Customs Intake Gate",
            "a documented customs room with an import intake roster, inspection desk, and holding bay");
    private static final NodeKind ROAD_GATE = new NodeKind(
            "off-map-road-gate", "Off-Map Road Loading Bay", "Road Freight Gate",
            "a road-linked loading bay with an import intake roster and controlled exterior freight gate");
    private static final NodeKind VOID_DOCK = new NodeKind(
            "air-void-cargo-dock", "High-Level Cargo Dock", "Cargo Dock Airlock",
            "a high-level cargo dock with an import intake roster, sealed transfer lock, and freight staging");
    private static final NodeKind NOBLE_PRIVATE = new NodeKind(
            "noble-private-import", "Private Estate Import Room", "Private Estate Import Gate",
            "a discreet estate receiving room with an import intake roster, private lift, and bonded storage");
    private static final NodeKind SMUGGLING = new NodeKind(
            "smuggling-entry", "Concealed Smuggling Entry", "Concealed Freight Hatch",
            "a hidden black-market entry with an import intake roster, false wall, and unregistered loading space");
    private static final NodeKind SEWER_HOIST = new NodeKind(
            "sewer-freight-hoist", "Sewer Freight Hoist", "Sewer Freight Hoist Gate",
            "a runoff-side freight hoist with an import intake roster, cargo sling, and surface transfer cage");

    private FactionImportNodeGenerationAuthority() { }

    static Result promoteAndPlan(World world, Random random) {
        if (world == null) return new Result(0, 0, "none", "none");
        MapObjectState existing = primaryNode(world, Faction.NONE);
        int tagged = taggedRoom(world);
        if (tagged >= 0 || existing != null) {
            String kind = tagged >= 0 ? profileKind(world.roomProfiles.get(tagged))
                    : MapObjectState.stockValue(existing.stockState, "kind");
            Faction owner = tagged >= 0 ? roomFaction(world, tagged) : nodeFaction(existing);
            return new Result(0, 0, safe(kind, "existing"), owner.label);
        }

        Faction faction = preferredFaction(world);
        NodeKind kind = kindFor(world, faction);
        int roomId = bestRoom(world, faction);
        if (roomId < 0) return new Result(0, 0, kind.id, faction.label);

        RoomProfile old = world.roomProfiles.get(roomId);
        Faction exactOwner = roomFaction(world, roomId);
        if (exactOwner == Faction.NONE) exactOwner = faction;
        RoomProfile promoted = new RoomProfile(kind.roomName + " / " + exactOwner.label,
                kind.description, Math.max(48, old == null ? 48 : old.scavengeChance), exactOwner,
                old == null ? new String[]{"Trade chit", "Cargo seal"} : old.loot,
                old == null ? new char[]{'q'} : old.contents);
        promoted.featureText = kind.description + "; " + TAG + "; import-node-kind=" + kind.id
                + "; controlled-by=" + exactOwner.name() + ".";
        world.roomProfiles.set(roomId, promoted);
        world.roomFactions.set(roomId, exactOwner);
        world.roomSpecials.set(roomId, Boolean.TRUE);
        world.zoneFacilityHistory = append(world.zoneFacilityHistory, "Import node: " + promoted.name
                + " provides identifiable external cargo and personnel arrival through " + kind.markerName + ".");
        return new Result(1, 0, kind.id, exactOwner.label);
    }

    static Result placePhysicalNodes(World world, Random random) {
        if (world == null) return new Result(0, 0, "none", "none");
        MapObjectState existing = primaryNode(world, Faction.NONE);
        if (existing != null) return new Result(0, 0,
                MapObjectState.stockValue(existing.stockState, "kind"), nodeFaction(existing).label);
        int roomId = taggedRoom(world);
        if (roomId < 0) {
            Result promotion = promoteAndPlan(world, random);
            roomId = taggedRoom(world);
            if (roomId < 0) return promotion;
        }
        RoomProfile profile = world.roomProfiles.get(roomId);
        NodeKind kind = kindById(profileKind(profile));
        Faction faction = roomFaction(world, roomId);
        Point point = world.randomObjectPointInRoom(world.rooms.get(roomId));
        if (point == null) return new Result(0, 0, kind.id, faction.label);
        char underlying = world.tiles[point.x][point.y];
        MapObjectState node = new MapObjectState();
        node.x = point.x;
        node.y = point.y;
        node.glyph = 'q';
        node.type = TYPE;
        node.label = faction.label + " " + kind.markerName + " / " + safe(world.zoneName, world.zoneType.label);
        node.stockState = "kind=" + kind.id + ";faction=" + faction.name() + ";roomId=" + roomId
                + ";status=open;under=" + (int) underlying;
        node.id = "IMPORT-NODE-" + Math.abs(Objects.hash(world.seed, world.locationKey(), kind.id, faction.name(), roomId));
        world.mapObjects.add(node);
        world.tiles[point.x][point.y] = node.glyph;
        return new Result(0, 1, kind.id, faction.label);
    }

    static boolean isImportNode(MapObjectState object) {
        return object != null && TYPE.equals(object.type);
    }

    static MapObjectState primaryNode(World world, Faction faction) {
        if (world == null || world.mapObjects == null) return null;
        Faction wanted = FactionInventoryStockAuthority.normalizeFaction(faction);
        return world.mapObjects.stream().filter(FactionImportNodeGenerationAuthority::isImportNode)
                .filter(node -> faction == null || faction == Faction.NONE
                        || FactionInventoryStockAuthority.normalizeFaction(nodeFaction(node)) == wanted)
                .min(Comparator.comparingInt((MapObjectState node) -> nodeScore(node, world, wanted))
                        .thenComparing(node -> safe(node.label, ""))).orElse(null);
    }

    static MapObjectState nodeForRoom(World world, int roomId) {
        if (world == null) return null;
        for (MapObjectState node : world.mapObjects) {
            if (isImportNode(node) && roomId == parseInt(MapObjectState.stockValue(node.stockState, "roomId"), -1)) return node;
        }
        return null;
    }

    static Point arrivalPoint(World world, Faction faction) {
        MapObjectState node = primaryNode(world, faction);
        if (node == null || !isOperational(node)) return null;
        for (int radius = 1; radius <= 3; radius++) {
            for (int dy = -radius; dy <= radius; dy++) for (int dx = -radius; dx <= radius; dx++) {
                if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                int x = node.x + dx, y = node.y + dy;
                if (world.inBounds(x, y) && world.walkable(x, y) && world.mapObjectAt(x, y) == null
                        && world.npcAt(x, y) == null) return new Point(x, y);
            }
        }
        return null;
    }

    static boolean isOperational(MapObjectState node) {
        return isImportNode(node) && !"true".equalsIgnoreCase(MapObjectState.stockValue(node.stockState,"eventClosed"));
    }

    static boolean routeOperational(World world, Faction faction) {
        MapObjectState node=primaryNode(world,faction);
        return node==null||isOperational(node);
    }

    static List<String> inspectionLines(World world, MapObjectState node, long worldTurn) {
        if (world == null || !isImportNode(node)) return List.of();
        ShipmentProvenanceAuthority.refreshForInspection(world, worldTurn);
        int scheduled = 0, delayed = 0, arrived = 0, intercepted = 0, delivered = 0, cargo = 0;
        for (ShipmentProvenanceRecord shipment : world.shipmentRecords) {
            if (shipment == null || !safe(shipment.arrivalNode, "").equals(node.label)) continue;
            cargo += Math.max(0, shipment.remaining);
            switch (safe(shipment.status, "")) {
                case "SCHEDULED" -> scheduled++;
                case "DELAYED" -> delayed++;
                case "ARRIVED" -> arrived++;
                case "INTERCEPTED" -> intercepted++;
                case "DELIVERED" -> delivered++;
                default -> { }
            }
        }
        int inbound = 0, ready = 0;
        long next = Long.MAX_VALUE;
        for (PersonnelReplacementRequest request : world.replacementQueue) {
            if (request == null || !safe(request.source, "").contains(node.label)) continue;
            if (request.dueTurn <= worldTurn) ready++; else inbound++;
            next = Math.min(next, request.dueTurn);
        }
        String kind = MapObjectState.stockValue(node.stockState, "kind").replace('-', ' ');
        String status = safe(MapObjectState.stockValue(node.stockState, "status"), "open");
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Arrival node: " + node.label + "; " + kind + "; route " + status
                + (isOperational(node) ? "." : "; personnel intake closed by an active world event."));
        lines.add("Cargo traffic: " + cargo + " unit(s) waiting; " + arrived + " arrived manifest(s), "
                + delayed + " delayed, " + scheduled + " scheduled, " + intercepted + " intercepted, "
                + delivered + " completed.");
        lines.add("Personnel traffic: " + ready + " reinforcement(s) ready, " + inbound + " inbound"
                + (next == Long.MAX_VALUE ? "." : "; next availability turn " + next + "."));
        lines.addAll(DeferredOutOfSectorSimulationAuthority.summaryLines(world, nodeFaction(node), worldTurn));
        return List.copyOf(lines);
    }

    static boolean tryInteract(GamePanel game, int x, int y) {
        if (game == null || game.world == null) return false;
        MapObjectState node = game.world.mapObjectAt(x, y);
        if (!isImportNode(node)) return false;
        for (String line : inspectionLines(game.world, node, game.worldTurn)) game.logEvent(line);
        node.vendCount++;
        game.advanceTurn("checks the arrival manifest at " + node.label + ".");
        game.repaint();
        return true;
    }

    private static int taggedRoom(World world) {
        for (int i = 1; i < world.roomProfiles.size(); i++) {
            if (roomText(world.roomProfiles.get(i)).contains(TAG)) return i;
        }
        return -1;
    }

    private static int bestRoom(World world, Faction preferred) {
        int best = -1, score = Integer.MIN_VALUE;
        Faction normalized = FactionInventoryStockAuthority.normalizeFaction(preferred);
        for (int i = 1; i < world.rooms.size(); i++) {
            Rectangle room = world.rooms.get(i);
            RoomProfile profile = i < world.roomProfiles.size() ? world.roomProfiles.get(i) : null;
            if (room == null || profile == null || room.width * room.height < 20 || roomText(profile).contains(TAG)) continue;
            Faction owner = roomFaction(world, i);
            Faction ownerNormalized = FactionInventoryStockAuthority.normalizeFaction(owner);
            if (owner != Faction.NONE && ownerNormalized != normalized) continue;
            String low = roomText(profile).toLowerCase(Locale.ROOT);
            int candidate = room.width * room.height;
            if (ownerNormalized == normalized) candidate += 300;
            if (contains(low, "cargo", "warehouse", "store", "rail", "loading", "service", "checkpoint", "transit")) candidate += 180;
            if (contains(low, "bed", "creche", "clinic", "shrine", "vault", "farm")) candidate -= 120;
            if (i < world.roomSpecials.size() && Boolean.TRUE.equals(world.roomSpecials.get(i))) candidate -= 60;
            if (candidate > score) { score = candidate; best = i; }
        }
        return best;
    }

    private static NodeKind kindFor(World world, Faction faction) {
        Faction normalized = FactionInventoryStockAuthority.normalizeFaction(faction);
        ZoneType zone = world.zoneType;
        if (world.sewerLayer || zone == ZoneType.SEWER_CONDUIT || zone == ZoneType.MUTANT_SEWER_CAMP
                || zone == ZoneType.CULTIST_SEWER_CAMP) return SEWER_HOIST;
        if (normalized == Faction.NOBLE) return NOBLE_PRIVATE;
        if (normalized == Faction.BANDIT || normalized == Faction.CULTIST || normalized == Faction.HERETIC) return SMUGGLING;
        if (zone == ZoneType.NEUTRAL_RAIL_DEPOT && world.floor == 5 && world.zoneX == 2 && world.zoneY == 2) return SECTOR_EXCHANGE;
        if (zone == ZoneType.NEUTRAL_RAIL_DEPOT || zone == ZoneType.TRAIN_SERVICE_YARD) return RAIL_CARGO;
        if (normalized == Faction.CIVIC_WARDENS || normalized == Faction.IMPERIAL_GUARD
                || normalized == Faction.CIVIC_LEDGER_OFFICE) return CUSTOMS;
        if (world.floor >= 9) return VOID_DOCK;
        if (world.floor <= 1) return ROAD_GATE;
        if (normalized == Faction.MECHANIST_COLLEGIA) return FREIGHT_ELEVATOR;
        return Math.floorMod(Objects.hash(world.seed, world.floor, world.zoneX, world.zoneY), 2) == 0
                ? FREIGHT_ELEVATOR : SERVICE_LIFT;
    }

    private static NodeKind kindById(String id) {
        for (NodeKind kind : List.of(SECTOR_EXCHANGE, RAIL_CARGO, FREIGHT_ELEVATOR, SERVICE_LIFT, CUSTOMS,
                ROAD_GATE, VOID_DOCK, NOBLE_PRIVATE, SMUGGLING, SEWER_HOIST)) if (kind.id.equals(id)) return kind;
        return SERVICE_LIFT;
    }

    private static String profileKind(RoomProfile profile) {
        String text = roomText(profile);
        int start = text.indexOf("import-node-kind=");
        if (start < 0) return "service-lift";
        start += "import-node-kind=".length();
        int end = text.indexOf(';', start);
        return (end < 0 ? text.substring(start) : text.substring(start, end)).trim();
    }

    private static Faction preferredFaction(World world) {
        Faction dominant = world.dominantContinuityFactionForZone();
        return dominant == null || dominant == Faction.NONE ? FactionInventoryStockAuthority.factionForZone(world.zoneType) : dominant;
    }

    private static Faction roomFaction(World world, int roomId) {
        if (world == null || roomId < 0) return Faction.NONE;
        Faction faction = roomId < world.roomFactions.size() ? world.roomFactions.get(roomId) : Faction.NONE;
        if ((faction == null || faction == Faction.NONE) && roomId < world.roomProfiles.size()) faction = world.roomProfiles.get(roomId).faction;
        return faction == null ? Faction.NONE : faction;
    }

    private static Faction nodeFaction(MapObjectState node) {
        try { return Faction.valueOf(MapObjectState.stockValue(node.stockState, "faction")); }
        catch (Exception ignored) { return Faction.NONE; }
    }

    private static int nodeScore(MapObjectState node, World world, Faction wanted) {
        Faction owner = FactionInventoryStockAuthority.normalizeFaction(nodeFaction(node));
        int score = owner == wanted ? 0 : 1000;
        int roomId = parseInt(MapObjectState.stockValue(node.stockState, "roomId"), -1);
        if (roomId < 0 || roomId >= world.rooms.size()) score += 500;
        return score;
    }

    private static String roomText(RoomProfile profile) {
        if (profile == null) return "";
        return safe(profile.name, "") + " " + safe(profile.descriptor, "") + " " + safe(profile.featureText, "");
    }

    private static boolean contains(String text, String... needles) {
        String low = safe(text, "").toLowerCase(Locale.ROOT);
        for (String needle : needles) if (low.contains(needle.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    private static String append(String text, String line) {
        if (safe(text, "").contains(line)) return text;
        return safe(text, "").isBlank() ? line : text + "\n" + line;
    }

    private static int parseInt(String value, int fallback) {
        try { return Integer.parseInt(value); } catch (Exception ignored) { return fallback; }
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
