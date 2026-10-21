package mechanist;

import java.awt.Rectangle;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Building-owned vertical travel is distinct from general zone or sewer
 * transitions. It is allowed only between physically stamped, matching shaft
 * landings within the same building, and never across an open atrium volume.
 */
final class MultiFloorBuildingTransitAuthority {
    // A different atlas means a new game. Keep per-world fog isolated for the
    // current session without extending the legacy save format implicitly.
    private record Exploration(WorldAtlas atlas, IdentityHashMap<World,boolean[][]> memory) { }
    private static final Map<GamePanel, Exploration> EXPLORATION = new WeakHashMap<>();
    private MultiFloorBuildingTransitAuthority() { }

    static boolean[][] rememberedFor(Map<World,boolean[][]> memory,
                                     World from, boolean[][] visibleHistory, World destination) {
        if (memory == null || from == null || destination == null)
            throw new IllegalArgumentException("World and exploration cache required");
        if (visibleHistory != null && visibleHistory.length == from.w
                && visibleHistory[0].length == from.h)
            memory.put(from, visibleHistory);
        return memory.computeIfAbsent(destination, w -> new boolean[w.w][w.h]);
    }

    record Landing(String buildingId, VerticalBuildingBlueprintAuthority.LinkKind kind,
                   int floor, int x, int y, VerticalBuildingBlueprintAuthority.Rect shaft) { }

    record Resolution(boolean allowed, Landing from, Landing to, String reason) {
        static Resolution denied(Landing from, String reason) {
            return new Resolution(false, from, null, reason);
        }
    }

    static Landing landingAt(World world, int x, int y) {
        if (world == null || world.sewerLayer || !world.inBounds(x, y)
                || world.buildingSites == null) return null;
        for (var site : world.buildingSites) {
            var plate = site.blueprint().plate(world.floor);
            if (plate == null || !isPhysicallyStamped(world, site.blueprint().buildingId())) continue;
            for (var kind : VerticalBuildingBlueprintAuthority.LinkKind.values()) {
                var use = kind == VerticalBuildingBlueprintAuthority.LinkKind.LIFT
                        ? VerticalBuildingBlueprintAuthority.Use.ELEVATOR
                        : VerticalBuildingBlueprintAuthority.Use.STAIRWELL;
                var space = plate.space(use);
                if (space == null) continue;
                Rectangle rect = site.translate(space.bounds());
                if (x != rect.x + 1 || y != rect.y + 1 || world.roomIds[x][y] >= 0) continue;
                char tile = world.tiles[x][y];
                if (kind == VerticalBuildingBlueprintAuthority.LinkKind.LIFT
                        ? tile != 'E' : !world.isCorridorGlyph(tile)) continue;
                return new Landing(site.blueprint().buildingId(), kind,
                        world.floor, x, y, space.bounds());
            }
        }
        return null;
    }

    private static boolean isPhysicallyStamped(World world, String buildingId) {
        if (world.buildingRoomIds == null) return false;
        String prefix = buildingId + "/F" + world.floor + "/";
        for (var room : world.buildingRoomIds.entrySet()) {
            Integer index = room.getValue();
            if (room.getKey().startsWith(prefix) && index != null
                    && index >= 0 && index < world.rooms.size()) return true;
        }
        return false;
    }

    static Resolution resolve(World from, World destination, int x, int y, int floorDelta) {
        Landing departure = landingAt(from, x, y);
        if (departure == null) return Resolution.denied(null, "Stand on a building stair or lift landing.");
        if (floorDelta != -1 && floorDelta != 1)
            return Resolution.denied(departure, "Vertical travel must be exactly one floor.");
        int destinationFloor = from.floor + floorDelta;
        if (destinationFloor < VerticalBuildingBlueprintAuthority.FIRST_FLOOR
                || destinationFloor > VerticalBuildingBlueprintAuthority.LAST_FLOOR)
            return Resolution.denied(departure, "No destination floor exists.");
        if (destination == null || destination.sewerLayer || destination.floor != destinationFloor
                || from.sectorX != destination.sectorX || from.sectorY != destination.sectorY
                || from.zoneX != destination.zoneX || from.zoneY != destination.zoneY)
            return Resolution.denied(departure, "The matching destination zone is unavailable.");
        Landing arrival = landingAt(destination, x, y);
        if (arrival == null || !departure.buildingId().equals(arrival.buildingId())
                || departure.kind() != arrival.kind() || !departure.shaft().equals(arrival.shaft()))
            return Resolution.denied(departure, "Matching building landing was not built on the other floor.");
        var site = siteFor(from, departure.buildingId());
        if (site == null || siteFor(destination, departure.buildingId()) == null)
            return Resolution.denied(departure, "Persistent building identity is not available on both levels.");
        // An aligned shaft still cannot receive a player when an actor or
        // fixture physically occupies its destination tile.
        if (!destination.walkable(x, y))
            return Resolution.denied(departure, "The destination landing is not walkable.");
        if (destination.npcAt(x, y) != null)
            return Resolution.denied(departure, "Someone is occupying the destination landing.");
        if (destination.mapObjectAt(x, y) != null)
            return Resolution.denied(departure, "An object is blocking the destination landing.");
        int lower = Math.min(from.floor, destination.floor);
        for (var link : site.blueprint().connections()) {
            if (link.kind() == departure.kind() && link.lowerFloor() == lower
                    && link.upperFloor() == lower + 1 && link.shaft().equals(departure.shaft()))
                return new Resolution(true, departure, arrival, "Confirmed aligned building transfer.");
        }
        return Resolution.denied(departure, "The building has no authorized connection between these floors.");
    }

    private static MultiFloorBuildingPlacementAuthority.Site siteFor(World world, String buildingId) {
        if (world == null || world.buildingSites == null) return null;
        for (var site : world.buildingSites)
            if (site.blueprint().buildingId().equals(buildingId)) return site;
        return null;
    }

    static boolean handlePlayerFloorKey(GamePanel game, int direction) {
        if (game == null || game.screen != GamePanel.Screen.GAME || game.world == null) return false;
        World from = game.world;
        Landing departure = landingAt(from, game.playerX, game.playerY);
        if (departure == null) return false;
        WorldAtlas atlas = game.atlas;
        if (atlas == null || atlas.floor != from.floor || atlas.sewer != from.sewerLayer
                || atlas.sectorX != from.sectorX || atlas.sectorY != from.sectorY
                || atlas.zoneX != from.zoneX || atlas.zoneY != from.zoneY) {
            report(game, "Cannot change floors: the active atlas does not match this building.");
            return true;
        }
        if (direction != 1 && direction != -1
                || atlas.floor + direction < VerticalBuildingBlueprintAuthority.FIRST_FLOOR
                || atlas.floor + direction > VerticalBuildingBlueprintAuthority.LAST_FLOOR) {
            report(game, "No accessible building landing in that direction.");
            return true;
        }
        int originalFloor = atlas.floor;
        World target;
        try {
            atlas.floor = originalFloor + direction;
            target = atlas.currentWorld();
        } catch (RuntimeException failure) {
            atlas.floor = originalFloor;
            String message = "Building destination load failed; original level retained: "
                    + failure.getClass().getSimpleName();
            DebugLog.warn("BUILDING_TRANSIT", message + " " + failure.getMessage());
            report(game, message);
            return true;
        }
        Resolution check = resolve(from, target, game.playerX, game.playerY, direction);
        if (!check.allowed()) {
            atlas.floor = originalFloor;
            report(game, "Cannot change floors: " + check.reason());
            return true;
        }

        // Only after both landings and the authoritative link validate do we
        // commit atlas/world state, exploration visibility and simulation time.
        Exploration exploration = EXPLORATION.get(game);
        if (exploration == null || exploration.atlas() != atlas) {
            exploration = new Exploration(atlas, new IdentityHashMap<>());
            EXPLORATION.put(game, exploration);
        }
        boolean[][] remembered = rememberedFor(exploration.memory(), from,
                game.rememberedTiles, target);
        game.world = target;
        game.rememberedTiles = remembered;
        game.visibleTiles = new boolean[target.w][target.h];
        game.lightLevels = new int[target.w][target.h];
        game.sensoryWorldRef = null;
        game.lookX = game.playerX;
        game.lookY = game.playerY;
        game.combatX = game.playerX;
        game.combatY = game.playerY;
        game.buildX = game.playerX;
        game.buildY = game.playerY;
        game.markZoneVisitedAndCheckFirstType();
        game.advanceTurnBody(null);
        report(game, (departure.kind() == VerticalBuildingBlueprintAuthority.LinkKind.LIFT
                ? "Building lift" : "Building stairs")
                + ": arrived on floor " + target.floor + " in " + departure.buildingId() + ".");
        return true;
    }

    private static void report(GamePanel game, String message) {
        game.lastTargetingReport = message;
        game.logEvent(message);
        game.repaint();
    }
}
