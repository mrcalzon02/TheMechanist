package mechanist;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Local vehicle routing over roads and legal parking cells. This authority does
 * not grant free-form traversal: every committed move is backed by a contiguous
 * path through road infrastructure and a legal parking resolution.
 */
final class VehicleTransitAuthority {
    static final int MAX_LOCAL_ROUTE_STEPS = 48;
    static final int PARKING_SEARCH_RADIUS = 5;
    private static final int DEPLOYMENT_HISTORY_LIMIT = 12;
    private static final int VEHICLE_HISTORY_LIMIT = 8;

    record RoutePlan(boolean valid, List<Point> path, Point requestedTarget,
                     Point resolvedParking, String blocker, String summary) {
        static RoutePlan blocked(Point requested, String blocker, String summary) {
            return new RoutePlan(false, List.of(), requested, null,
                    clean(blocker, "route-blocked"),
                    clean(summary, "Vehicle route is blocked."));
        }

        int steps() { return Math.max(0, path.size() - 1); }
    }

    private record ParkingRoute(Point parking, List<Point> path) { }
    private record RoadSearch(ParkingRoute route, boolean hitRouteLimit) { }
    private record SearchStep(Point point, int depth) { }
    private static final int[][] CARDINAL_STEPS =
            {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    record TransitResult(boolean success, boolean changed, int steps,
                         int fromX, int fromY, int toX, int toY,
                         String message, RoutePlan plan) {
        static TransitResult blocked(RoutePlan plan, MapObjectState vehicle) {
            int x = vehicle == null ? -1 : vehicle.x;
            int y = vehicle == null ? -1 : vehicle.y;
            return new TransitResult(false, false, 0, x, y, x, y,
                    plan == null ? "Vehicle route is unavailable." : plan.summary(), plan);
        }
    }

    private VehicleTransitAuthority() { }

    static RoutePlan plan(GamePanel game, MapObjectState vehicle,
                          int targetX, int targetY) {
        Point requested = new Point(targetX, targetY);
        if (game == null || game.world == null) {
            return RoutePlan.blocked(requested, "no-loaded-world",
                    "Vehicle routing requires a loaded world.");
        }
        if (vehicle == null || !VehicleRuntimeAuthority.isVehicle(vehicle)
                || !game.world.mapObjects.contains(vehicle)) {
            return RoutePlan.blocked(requested, "vehicle-not-present",
                    "The selected vehicle is not present in the loaded zone.");
        }
        VehicleRuntimeAuthority.ensureInitialized(game.world, vehicle);
        if (!VehicleRuntimeAuthority.playerOwns(game, vehicle)) {
            return RoutePlan.blocked(requested, "vehicle-access-denied",
                    "Only a player-owned vehicle can use the player route command.");
        }
        String condition = MapObjectState.stockValue(vehicle.stockState, "condition");
        if ("disabled".equals(condition) || "wreck".equals(condition)
                || "salvaged".equals(condition)) {
            return RoutePlan.blocked(requested, "vehicle-not-operational",
                    "The vehicle is " + clean(condition, "not operational")
                            + " and cannot begin a route.");
        }
        int power = component(vehicle, VehicleRuntimeAuthority.Component.POWERPLANT);
        int drive = component(vehicle, VehicleRuntimeAuthority.Component.DRIVE);
        int mobility = component(vehicle, VehicleRuntimeAuthority.Component.MOBILITY);
        if (power <= 0 || drive <= 0 || mobility <= 0) {
            return RoutePlan.blocked(requested, "vehicle-mobility-failure",
                    "The vehicle needs a working powerplant, drive, and mobility assembly before routing.");
        }
        if (!game.world.inBounds(targetX, targetY)) {
            return RoutePlan.blocked(requested, "target-outside-zone",
                    "The requested vehicle destination is outside the loaded zone.");
        }

        // One occupancy snapshot per request avoids a linear NPC + fixture scan
        // on every road cell. No snapshot survives a change in world state.
        HashSet<Long> blockedTiles = occupiedTiles(game, vehicle);
        List<Point> options = parkingCandidates(game.world, blockedTiles, requested);
        if (options.isEmpty()) {
            return RoutePlan.blocked(requested, "no-legal-parking",
                    "No unoccupied legal parking cell is available near " + targetX + "," + targetY
                            + ". Move away from occupied parking, or choose marked parking or a clear curb-adjacent sidewalk.");
        }
        int minimumDistance = Integer.MAX_VALUE;
        for (Point option : options)
            minimumDistance = Math.min(minimumDistance,
                    Math.abs(option.x - vehicle.x) + Math.abs(option.y - vehicle.y));
        if (minimumDistance > MAX_LOCAL_ROUTE_STEPS) {
            return RoutePlan.blocked(requested, "route-too-long",
                    "The nearest legal parking is at least " + minimumDistance
                            + " tiles away; local travel is limited to "
                            + MAX_LOCAL_ROUTE_STEPS
                            + " road steps. Choose a closer destination or use strategic transit.");
        }
        VehicleRuntimeAuthority.VehicleClass definition =
                VehicleRuntimeAuthority.vehicleClass(vehicle.type);
        // This is a preview: never initialize/write a fuel ledger just because
        // the selected road, parking or travel budget proves invalid.
        VehicleFuelAuthority.Snapshot energy =
                VehicleFuelAuthority.preview(vehicle);
        // Resolve parking against actual *unreserved* travel range first.
        // A closer parking tile may require a much longer road detour than a
        // nearby alternative. Avoid refusing the whole trip in that case.
        int fuelStepLimit = Math.min(MAX_LOCAL_ROUTE_STEPS,
                VehicleFuelAuthority.localRoadRangeTiles(definition, energy.available()));
        RoadSearch search = roadPath(game.world, vehicle, options,
                definition, blockedTiles, fuelStepLimit);
        if (search.route() == null) {
            // Only when fuel-constrained search hits its boundary, retry the
            // bounded 48-step road graph to distinguish insufficient energy
            // from disconnected roads or an overlong route.
            if (fuelStepLimit < MAX_LOCAL_ROUTE_STEPS && search.hitRouteLimit()) {
                RoadSearch unrestricted = roadPath(game.world, vehicle, options,
                        definition, blockedTiles, MAX_LOCAL_ROUTE_STEPS);
                if (unrestricted.route() != null) {
                    return RoutePlan.blocked(requested, "insufficient-energy",
                            "No legal parking near " + targetX + "," + targetY
                                    + " is reachable with " + energy.available()
                                    + " unreserved fuel or power unit(s) ("
                                    + energy.reserved()
                                    + " reserved for strategic transit)."
                                    + " Refuel or choose a closer road destination.");
                }
                search = unrestricted;
            }
            if (search.hitRouteLimit()) {
                return RoutePlan.blocked(requested, "route-too-long",
                        "No parking could be reached within "
                                + MAX_LOCAL_ROUTE_STEPS
                                + " road steps. The road may be blocked or the route longer; "
                                + "choose a closer stop or check road access.");
            }
            return RoutePlan.blocked(requested, "no-road-route",
                    "No unobstructed road or parking route connects " + vehicle.x + ","
                            + vehicle.y + " to the requested area "
                            + targetX + "," + targetY
                            + ". The player, other people, fixtures, or a road break may obstruct the lane.");
        }
        ParkingRoute route = search.route();
        Point parking = route.parking();
        List<Point> path = route.path();
        if (path.size() - 1 > MAX_LOCAL_ROUTE_STEPS) {
            return RoutePlan.blocked(requested, "route-too-long",
                    "The local vehicle route is " + (path.size() - 1)
                            + " tiles; the current planning window supports at most "
                            + MAX_LOCAL_ROUTE_STEPS + ".");
        }
        int requiredFuel = VehicleFuelAuthority.routeEnergyCost(
                definition, path.size() - 1);
        // Defense in depth: a constrained path may never consume reserved fuel.
        if (energy.available() < requiredFuel) {
            return RoutePlan.blocked(requested, "insufficient-energy",
                    "This local vehicle route needs " + requiredFuel
                            + " fuel or power unit(s); only " + energy.available()
                            + " are available (" + energy.reserved()
                            + " reserved for strategic transit). Refuel or choose a shorter route.");
        }
        String summary = "Vehicle route ready: " + displayName(vehicle)
                + " travels " + (path.size() - 1) + " road tile(s) from "
                + vehicle.x + "," + vehicle.y + " to legal parking at "
                + parking.x + "," + parking.y
                + (parking.equals(requested) ? "." : " near requested "
                + requested.x + "," + requested.y + ".")
                + " Fuel or power: " + requiredFuel + " unit(s) required; "
                + energy.available() + " available, " + energy.reserved()
                + " reserved.";
        return new RoutePlan(true, List.copyOf(path), requested, parking, "", summary);
    }

    static TransitResult execute(GamePanel game, MapObjectState vehicle,
                                 RoutePlan plan) {
        if (plan == null || !plan.valid()) return TransitResult.blocked(plan, vehicle);
        if (game == null || game.world == null || vehicle == null
                || !game.world.mapObjects.contains(vehicle)) {
            return TransitResult.blocked(RoutePlan.blocked(
                    plan.requestedTarget(), "vehicle-not-present",
                    "The vehicle route cannot execute because the fixture is no longer present."),
                    vehicle);
        }
        RoutePlan refreshed = plan(game, vehicle,
                plan.requestedTarget().x, plan.requestedTarget().y);
        if (!refreshed.valid()) return TransitResult.blocked(refreshed, vehicle);
        if (!samePath(plan.path(), refreshed.path())) {
            return TransitResult.blocked(RoutePlan.blocked(
                    plan.requestedTarget(), "route-state-changed",
                    "The road or parking state changed after preview; review the vehicle route again."),
                    vehicle);
        }

        HashSet<Long> blockedTiles = occupiedTiles(game, vehicle);
        Point previous = plan.path().get(0);
        for (int i = 1; i < plan.path().size(); i++) {
            Point step = plan.path().get(i);
            boolean finalParkingCell = i == plan.path().size() - 1;
            if (Math.abs(step.x - previous.x) + Math.abs(step.y - previous.y) != 1
                    || !routeCell(game.world, blockedTiles, step.x, step.y,
                    finalParkingCell)) {
                return TransitResult.blocked(RoutePlan.blocked(
                        plan.requestedTarget(), "route-became-blocked",
                        "The planned route became blocked at " + step.x + "," + step.y
                                + "; the vehicle remains at its prior position."), vehicle);
            }
            previous = step;
        }

        int fromX = vehicle.x;
        int fromY = vehicle.y;
        Point destination = plan.resolvedParking();
        Point lastMove = plan.path().size() >= 2
                ? plan.path().get(plan.path().size() - 2)
                : new Point(fromX, fromY);
        int dx = Integer.signum(destination.x - lastMove.x);
        int dy = Integer.signum(destination.y - lastMove.y);
        if (plan.steps() == 0) {
            // An inspection of the already parked vehicle is not an engine start:
            // preserve its component state, audio cues, and deployment history.
            return new TransitResult(true, false, 0,
                    fromX, fromY, fromX, fromY,
                    "Vehicle already parked at " + fromX + "," + fromY
                            + "; no movement or engine operation was needed.", plan);
        }
        // Debit only after the current road, occupancy, and fuel checks pass,
        // and before the first movement or engine/lighting/audio effect.
        // The shared ledger refuses consumption of strategic reservations.
        int requiredFuel = VehicleFuelAuthority.routeEnergyCost(
                VehicleRuntimeAuthority.vehicleClass(vehicle.type), plan.steps());
        VehicleFuelAuthority.Result spent = VehicleFuelAuthority.consumeCommitted(
                game.world, vehicle, requiredFuel, game.turn, "local road transit");
        if (!spent.success()) {
            return TransitResult.blocked(RoutePlan.blocked(
                    plan.requestedTarget(), "insufficient-energy",
                    spent.message() + " The vehicle remains parked."), vehicle);
        }
        VehicleOperationFeedbackAuthority.begin(game, vehicle, plan.steps(),
                fromX, fromY, destination.x, destination.y, dx, dy);
        vehicle.x = destination.x;
        vehicle.y = destination.y;
        set(vehicle, "facingDx", Integer.toString(dx));
        set(vehicle, "facingDy", Integer.toString(dy));
        set(vehicle, "lastRoute", routeText(plan.path()));
        append(vehicle, "deploymentHistory",
                "Local road route " + fromX + "," + fromY + " -> "
                        + destination.x + "," + destination.y + " / "
                        + plan.steps() + " step(s) / fuel " + spent.amount()
                        + " / turn " + game.turn);
        append(vehicle, "vehicleHistory",
                "Parked after a validated local road route at turn " + game.turn);
        VehicleOperationFeedbackAuthority.Feedback feedback =
                VehicleOperationFeedbackAuthority.complete(game, vehicle,
                        plan.steps(), fromX, fromY, destination.x, destination.y,
                        dx, dy);
        game.markLocalDirtyRegion("vehicle transit", destination.x, destination.y,
                8, false, false, true, false);
        return new TransitResult(true,
                fromX != destination.x || fromY != destination.y,
                plan.steps(), fromX, fromY, destination.x, destination.y,
                "VEHICLE TRANSIT: " + displayName(vehicle) + " followed "
                        + plan.steps() + " validated road tile(s) and parked at "
                        + destination.x + "," + destination.y + ". "
                        + spent.message() + " " + feedback.summary(),
                plan);
    }

    static TransitResult executeManualPlan(GamePanel game, MapObjectState vehicle) {
        if (game == null || game.world == null || !game.manualMovementPlanActive) {
            RoutePlan blocked = RoutePlan.blocked(
                    new Point(vehicle == null ? -1 : vehicle.x,
                            vehicle == null ? -1 : vehicle.y),
                    "no-manual-route",
                    "Open manual movement planning, position the planning cursor on a road or parking destination, then interact with the player-owned vehicle.");
            return TransitResult.blocked(blocked, vehicle);
        }
        Point target = null;
        if (game.lookCursorActive && game.world.inBounds(game.lookX, game.lookY)) {
            target = new Point(game.lookX, game.lookY);
        } else if (game.manualMovementPlanPath != null
                && !game.manualMovementPlanPath.isEmpty()) {
            Point last = game.manualMovementPlanPath.get(
                    game.manualMovementPlanPath.size() - 1);
            if (last != null && game.world.inBounds(last.x, last.y))
                target = new Point(last.x, last.y);
        }
        // Never accept stale lookX/lookY when the target cursor is inactive
        // and no explicit path has been selected; no hidden vehicle movement.
        if (target == null) {
            RoutePlan blocked = RoutePlan.blocked(new Point(vehicle.x, vehicle.y),
                    "no-vehicle-route-target",
                    "The vehicle route cursor does not currently identify a destination inside this zone.");
            return TransitResult.blocked(blocked, vehicle);
        }
        RoutePlan plan = plan(game, vehicle, target.x, target.y);
        if (!plan.valid()) return TransitResult.blocked(plan, vehicle);
        TransitResult result = execute(game, vehicle, plan);
        if (result.success()) game.cancelManualMovementPlan("vehicle route completed");
        return result;
    }

    static List<String> inspectionLines(GamePanel game, MapObjectState vehicle) {
        ArrayList<String> lines = new ArrayList<>(
                VehicleInfopediaBridgeAuthority.liveDossier(game, vehicle));
        lines.add("LOCAL TRANSIT CONTROL");
        lines.add("Transit rule: roads and marked parking carry movement; clear curb-adjacent sidewalks may receive automatic parking, but they are not through-routes.");
        lines.add("Travel energy: local trips charge the shared class-scaled fuel or power ledger only when movement commits. The route planner tries reachable parking within unreserved energy first; a nearer parking bay with an unaffordable detour is skipped. Strategic reservations remain unavailable to local routes; checking an already parked vehicle costs nothing.");
        lines.add("Route control: open manual movement planning, aim its cursor at a road or parking destination, then interact with a nearby player-owned operational vehicle. No walking path is required; vehicle range is validated independently.");
        lines.add("Operation feedback: validated movement drives the same running state used by the bounded pulse, forward headlights, degraded-component feedback, and distance-limited ambient operating sound.");
        if (game != null && vehicle != null && VehicleRuntimeAuthority.playerOwns(game, vehicle)) {
            Point target = null;
            if (game.manualMovementPlanActive && game.lookCursorActive
                    && game.world != null && game.world.inBounds(game.lookX, game.lookY)) {
                target = new Point(game.lookX, game.lookY);
            } else if (game.manualMovementPlanActive
                    && game.manualMovementPlanPath != null
                    && !game.manualMovementPlanPath.isEmpty()) {
                target = game.manualMovementPlanPath.get(
                        game.manualMovementPlanPath.size() - 1);
            }
            if (target != null) lines.add(plan(game, vehicle, target.x, target.y).summary());
            else lines.add("No vehicle route is currently previewed.");
        }
        return List.copyOf(lines);
    }

    /**
     * A bounded BFS over roads with stable nearest-parking priority.
     * Sidewalks may terminate the path but are never through-routes.
     */
    private static RoadSearch roadPath(World world, MapObjectState vehicle,
                                       List<Point> options,
                                       VehicleRuntimeAuthority.VehicleClass vehicleClass,
                                       HashSet<Long> blockedTiles, int maxSteps) {
        if (options.isEmpty()) return new RoadSearch(null, false);
        Point start = new Point(vehicle.x, vehicle.y);
        HashMap<Long, Integer> priorityByCell = new HashMap<>();
        for (int i = 0; i < options.size(); i++) {
            Point p = options.get(i);
            priorityByCell.put(tileKey(p.x, p.y), i);
        }
        ArrayDeque<SearchStep> queue = new ArrayDeque<>();
        HashSet<Long> seen = new HashSet<>();
        HashMap<Long, Point> previous = new HashMap<>();
        queue.addLast(new SearchStep(start, 0));
        seen.add(tileKey(start.x, start.y));

        Point best = null;
        int bestPriority = Integer.MAX_VALUE;
        boolean hitLimit = false;
        while (!queue.isEmpty()) {
            SearchStep step = queue.removeFirst();
            Point current = step.point();
            Integer priority = priorityByCell.get(tileKey(current.x, current.y));
            if (priority != null && priority < bestPriority) {
                bestPriority = priority;
                best = current;
                if (priority == 0) break;
            }
            if (world.tiles[current.x][current.y] == RoadGridIntegrationAuthority.SIDEWALK
                    && !current.equals(start)) continue;
            for (int[] dir : CARDINAL_STEPS) {
                int nx = current.x + dir[0], ny = current.y + dir[1];
                if (!world.inBounds(nx, ny)) continue;
                long nextKey = tileKey(nx, ny);
                if (seen.contains(nextKey)) continue;
                boolean parkingTarget = priorityByCell.containsKey(nextKey);
                if (!routeCell(world, blockedTiles, nx, ny, parkingTarget)
                        || !sizeAllows(world, vehicleClass, nx, ny)) continue;
                if (step.depth() >= maxSteps) {
                    // Detect a continuation without visiting a step beyond
                    // the current energy/routing budget.
                    hitLimit = true;
                    continue;
                }
                seen.add(nextKey);
                Point next = new Point(nx, ny);
                previous.put(nextKey, current);
                queue.addLast(new SearchStep(next, step.depth() + 1));
            }
        }
        if (best == null) return new RoadSearch(null, hitLimit);
        List<Point> path = reconstruct(previous, start, best);
        return new RoadSearch(path.isEmpty() ? null : new ParkingRoute(best, path), hitLimit);
    }

    private static List<Point> reconstruct(Map<Long, Point> previous,
                                           Point start, Point target) {
        ArrayList<Point> path = new ArrayList<>();
        Point current = target;
        path.add(current);
        while (!current.equals(start)) {
            current = previous.get(tileKey(current.x, current.y));
            if (current == null) return List.of();
            path.add(current);
        }
        Collections.reverse(path);
        return path;
    }

    private static List<Point> parkingCandidates(World world,
                                                  HashSet<Long> blockedTiles,
                                                  Point requested) {
        ArrayList<Point> options = new ArrayList<>();
        for (int radius = 0; radius <= PARKING_SEARCH_RADIUS; radius++) {
            for (int x = requested.x - radius; x <= requested.x + radius; x++) {
                for (int y = requested.y - radius; y <= requested.y + radius; y++) {
                    if (Math.abs(x - requested.x) + Math.abs(y - requested.y) != radius) continue;
                    if (legalParking(world, blockedTiles, x, y)) options.add(new Point(x, y));
                }
            }
        }
        options.sort(Comparator
                .comparingInt((Point point) ->
                        Math.abs(point.x - requested.x) + Math.abs(point.y - requested.y))
                .thenComparingInt(point -> point.y)
                .thenComparingInt(point -> point.x));
        return List.copyOf(options);
    }

    private static boolean legalParking(World world, HashSet<Long> blockedTiles,
                                        int x, int y) {
        if (world == null || !world.inBounds(x, y)
                || blockedTiles.contains(tileKey(x, y))) return false;
        char tile = world.tiles[x][y];
        if (tile == RoadGridIntegrationAuthority.PARKING_SPACE) return true;
        if (tile != RoadGridIntegrationAuthority.SIDEWALK) return false;
        return adjacentRoad(world, x, y);
    }

    private static boolean routeCell(World world, HashSet<Long> blockedTiles,
                                     int x, int y, boolean targetCell) {
        if (world == null || !world.inBounds(x, y)
                || blockedTiles.contains(tileKey(x, y))) return false;
        char tile = world.tiles[x][y];
        if (tile == RoadGridIntegrationAuthority.ROAD_LANE
                || tile == RoadGridIntegrationAuthority.PARKING_SPACE) return true;
        return targetCell && tile == RoadGridIntegrationAuthority.SIDEWALK
                && adjacentRoad(world, x, y);
    }

    /** Recomputed on every plan/commit; no stale occupancy state is cached. */
    private static HashSet<Long> occupiedTiles(GamePanel game, MapObjectState vehicle) {
        HashSet<Long> tiles = new HashSet<>();
        if (game == null || game.world == null) return tiles;
        World world = game.world;
        // Player position is physically occupied, just like a live NPC.
        // Allow the selected vehicle's own start tile to remain available:
        // the player may already share it while performing a no-op inspection.
        if (world.inBounds(game.playerX, game.playerY)
                && (vehicle == null || game.playerX != vehicle.x
                        || game.playerY != vehicle.y)) {
            tiles.add(tileKey(game.playerX, game.playerY));
        }
        if (world.npcs != null) for (NpcEntity npc : world.npcs) {
            if (npc != null && npc.hp > 0 && world.inBounds(npc.x, npc.y))
                tiles.add(tileKey(npc.x, npc.y));
        }
        if (world.mapObjects != null) for (MapObjectState object : world.mapObjects) {
            if (object != null && object != vehicle && world.inBounds(object.x, object.y))
                tiles.add(tileKey(object.x, object.y));
        }
        return tiles;
    }

    private static long tileKey(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    private static boolean sizeAllows(World world,
                                      VehicleRuntimeAuthority.VehicleClass vehicleClass,
                                      int x, int y) {
        if (vehicleClass == VehicleRuntimeAuthority.VehicleClass.UTILITY_BIKE
                || vehicleClass == VehicleRuntimeAuthority.VehicleClass.CIVILIAN_CAR) return true;
        int roadNeighbors = 0;
        int[][] directions = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int[] direction : directions) {
            int nx = x + direction[0];
            int ny = y + direction[1];
            if (!world.inBounds(nx, ny)) continue;
            char tile = world.tiles[nx][ny];
            if (tile == RoadGridIntegrationAuthority.ROAD_LANE
                    || tile == RoadGridIntegrationAuthority.PARKING_SPACE) roadNeighbors++;
        }
        int required = vehicleClass == VehicleRuntimeAuthority.VehicleClass.TANK ? 2 : 1;
        return roadNeighbors >= required;
    }

    private static boolean adjacentRoad(World world, int x, int y) {
        return road(world, x + 1, y) || road(world, x - 1, y)
                || road(world, x, y + 1) || road(world, x, y - 1);
    }

    private static boolean road(World world, int x, int y) {
        if (world == null || !world.inBounds(x, y)) return false;
        char tile = world.tiles[x][y];
        return tile == RoadGridIntegrationAuthority.ROAD_LANE
                || tile == RoadGridIntegrationAuthority.PARKING_SPACE;
    }

    private static int component(MapObjectState vehicle,
                                 VehicleRuntimeAuthority.Component component) {
        String key = "vehicleComponent" + component.name().charAt(0)
                + component.name().substring(1).toLowerCase(Locale.ROOT);
        try {
            return Integer.parseInt(MapObjectState.stockValue(vehicle.stockState, key));
        } catch (Exception ignored) {
            return 100;
        }
    }

    private static boolean samePath(List<Point> a, List<Point> b) {
        if (a == null || b == null || a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!Objects.equals(a.get(i), b.get(i))) return false;
        }
        return true;
    }

    private static String routeText(List<Point> path) {
        ArrayList<String> points = new ArrayList<>();
        if (path != null) for (Point point : path) {
            points.add(point.x + "," + point.y);
        }
        return String.join("~", points);
    }

    private static void append(MapObjectState vehicle, String key, String entry) {
        ArrayList<String> entries = new ArrayList<>();
        String existing = MapObjectState.stockValue(vehicle.stockState, key);
        if (existing != null && !existing.isBlank()) {
            for (String token : existing.split("~")) {
                String cleaned = clean(token, "");
                if (!cleaned.isBlank()) entries.add(cleaned);
            }
        }
        String cleanedEntry = clean(entry, "");
        if (!cleanedEntry.isBlank()) entries.add(cleanedEntry);
        int limit = "vehicleHistory".equals(key)
                ? VEHICLE_HISTORY_LIMIT : DEPLOYMENT_HISTORY_LIMIT;
        while (entries.size() > limit) entries.remove(0);
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                key, String.join("~", entries).replace(';', ',').replace('|', '/'));
    }

    private static void set(MapObjectState vehicle, String key, String value) {
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                key, clean(value, "").replace(';', ',').replace('|', '/'));
    }

    private static String displayName(MapObjectState vehicle) {
        VehicleRuntimeAuthority.Snapshot snapshot =
                VehicleRuntimeAuthority.inspect(null, vehicle);
        if (snapshot == null) return "vehicle";
        return (clean(snapshot.manufacturer(), "") + " "
                + clean(snapshot.model(), snapshot.vehicleClass().label)).trim();
    }

    private static String key(int x, int y) { return x + ":" + y; }

    private static String clean(String value, String fallback) {
        String text = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        return text.isBlank() ? fallback : text;
    }
}
