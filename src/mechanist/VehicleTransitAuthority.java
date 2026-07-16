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

    record RoutePlan(boolean valid, List<Point> path, Point requestedTarget,
                     Point resolvedParking, String blocker, String summary) {
        static RoutePlan blocked(Point requested, String blocker, String summary) {
            return new RoutePlan(false, List.of(), requested, null,
                    clean(blocker, "route-blocked"),
                    clean(summary, "Vehicle route is blocked."));
        }

        int steps() { return Math.max(0, path.size() - 1); }
    }

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

        Point parking = resolveParking(game.world, vehicle, requested);
        if (parking == null) {
            return RoutePlan.blocked(requested, "no-legal-parking",
                    "No legal parking cell is available near " + targetX + "," + targetY
                            + ". Vehicles must stop on marked parking or a clear curb-adjacent sidewalk.");
        }
        List<Point> path = roadPath(game.world, vehicle, parking,
                VehicleRuntimeAuthority.vehicleClass(vehicle.type));
        if (path.isEmpty()) {
            return RoutePlan.blocked(requested, "no-road-route",
                    "No contiguous road or parking route connects " + vehicle.x + ","
                            + vehicle.y + " to the resolved parking cell "
                            + parking.x + "," + parking.y + ".");
        }
        if (path.size() - 1 > MAX_LOCAL_ROUTE_STEPS) {
            return RoutePlan.blocked(requested, "route-too-long",
                    "The local vehicle route is " + (path.size() - 1)
                            + " tiles; the current planning window supports at most "
                            + MAX_LOCAL_ROUTE_STEPS + ".");
        }
        String summary = "Vehicle route ready: " + displayName(vehicle)
                + " travels " + (path.size() - 1) + " road tile(s) from "
                + vehicle.x + "," + vehicle.y + " to legal parking at "
                + parking.x + "," + parking.y
                + (parking.equals(requested) ? "." : " near requested "
                + requested.x + "," + requested.y + ".");
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

        int fromX = vehicle.x;
        int fromY = vehicle.y;
        set(vehicle, "operationState", "running");
        set(vehicle, "headlightsActive", Boolean.toString(
                component(vehicle, VehicleRuntimeAuthority.Component.LIGHTS) > 0));
        Point previous = plan.path().get(0);
        for (int i = 1; i < plan.path().size(); i++) {
            Point step = plan.path().get(i);
            boolean finalParkingCell = i == plan.path().size() - 1;
            if (Math.abs(step.x - previous.x) + Math.abs(step.y - previous.y) != 1
                    || !routeCell(game.world, vehicle, step.x, step.y,
                    finalParkingCell)) {
                set(vehicle, "operationState", "parked");
                return TransitResult.blocked(RoutePlan.blocked(
                        plan.requestedTarget(), "route-became-blocked",
                        "The planned route became blocked at " + step.x + "," + step.y
                                + "; the vehicle remains at its prior position."), vehicle);
            }
            previous = step;
        }

        Point destination = plan.resolvedParking();
        Point lastMove = plan.path().size() >= 2
                ? plan.path().get(plan.path().size() - 2)
                : new Point(fromX, fromY);
        int dx = Integer.signum(destination.x - lastMove.x);
        int dy = Integer.signum(destination.y - lastMove.y);
        vehicle.x = destination.x;
        vehicle.y = destination.y;
        set(vehicle, "facingDx", Integer.toString(dx));
        set(vehicle, "facingDy", Integer.toString(dy));
        set(vehicle, "operationState", "parked");
        set(vehicle, "headlightsActive", "false");
        set(vehicle, "lastRoute", routeText(plan.path()));
        append(vehicle, "deploymentHistory",
                "Local road route " + fromX + "," + fromY + " -> "
                        + destination.x + "," + destination.y + " / "
                        + plan.steps() + " step(s) / turn " + game.turn);
        append(vehicle, "vehicleHistory",
                "Parked after a validated local road route at turn " + game.turn);
        game.markLocalDirtyRegion("vehicle transit", destination.x, destination.y,
                8, false, false, true, false);
        return new TransitResult(true,
                fromX != destination.x || fromY != destination.y,
                plan.steps(), fromX, fromY, destination.x, destination.y,
                "VEHICLE TRANSIT: " + displayName(vehicle) + " followed "
                        + plan.steps() + " validated road tile(s) and parked at "
                        + destination.x + "," + destination.y + ".",
                plan);
    }

    static TransitResult executeManualPlan(GamePanel game, MapObjectState vehicle) {
        if (game == null || !game.manualMovementPlanActive
                || game.manualMovementPlanPath == null
                || game.manualMovementPlanPath.isEmpty()) {
            RoutePlan blocked = RoutePlan.blocked(
                    new Point(vehicle == null ? -1 : vehicle.x,
                            vehicle == null ? -1 : vehicle.y),
                    "no-manual-route",
                    "Create a manual movement plan to a road or parking destination, then interact with the player-owned vehicle to preview and commit its route.");
            return TransitResult.blocked(blocked, vehicle);
        }
        Point target = game.manualMovementPlanPath.get(
                game.manualMovementPlanPath.size() - 1);
        RoutePlan plan = plan(game, vehicle, target.x, target.y);
        if (!plan.valid()) return TransitResult.blocked(plan, vehicle);
        TransitResult result = execute(game, vehicle, plan);
        if (result.success()) game.cancelManualMovementPlan("vehicle route completed");
        return result;
    }

    static List<String> inspectionLines(GamePanel game, MapObjectState vehicle) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add(VehicleRuntimeAuthority.inspectionLine(
                game == null ? null : game.world, vehicle));
        lines.add("Transit rule: roads and marked parking carry movement; clear curb-adjacent sidewalks may receive automatic parking, but they are not through-routes.");
        lines.add("Route control: create a manual movement plan, then interact with a player-owned operational vehicle. The vehicle authority rechecks ownership, components, road continuity, occupancy, distance, and legal parking before movement.");
        if (game != null && vehicle != null && VehicleRuntimeAuthority.playerOwns(game, vehicle)) {
            if (game.manualMovementPlanActive && !game.manualMovementPlanPath.isEmpty()) {
                Point target = game.manualMovementPlanPath.get(
                        game.manualMovementPlanPath.size() - 1);
                lines.add(plan(game, vehicle, target.x, target.y).summary());
            } else {
                lines.add("No vehicle route is currently previewed.");
            }
        }
        return List.copyOf(lines);
    }

    private static List<Point> roadPath(World world, MapObjectState vehicle,
                                        Point target,
                                        VehicleRuntimeAuthority.VehicleClass vehicleClass) {
        Point start = new Point(vehicle.x, vehicle.y);
        if (start.equals(target)) return List.of(start);
        ArrayDeque<Point> queue = new ArrayDeque<>();
        HashSet<String> seen = new HashSet<>();
        HashMap<String, Point> previous = new HashMap<>();
        queue.add(start);
        seen.add(key(start.x, start.y));
        int[][] directions = {{1,0},{-1,0},{0,1},{0,-1}};
        while (!queue.isEmpty()) {
            Point current = queue.removeFirst();
            for (int[] direction : directions) {
                int nx = current.x + direction[0];
                int ny = current.y + direction[1];
                String key = key(nx, ny);
                if (seen.contains(key) || !world.inBounds(nx, ny)) continue;
                boolean targetCell = nx == target.x && ny == target.y;
                if (!routeCell(world, vehicle, nx, ny, targetCell)) continue;
                if (!sizeAllows(world, vehicleClass, nx, ny)) continue;
                Point next = new Point(nx, ny);
                previous.put(key, current);
                if (targetCell) return reconstruct(previous, start, next);
                seen.add(key);
                queue.addLast(next);
                if (seen.size() > world.w * world.h) return List.of();
            }
        }
        return List.of();
    }

    private static List<Point> reconstruct(Map<String, Point> previous,
                                           Point start, Point target) {
        ArrayList<Point> path = new ArrayList<>();
        Point current = target;
        path.add(current);
        while (!current.equals(start)) {
            current = previous.get(key(current.x, current.y));
            if (current == null) return List.of();
            path.add(current);
        }
        Collections.reverse(path);
        return path;
    }

    private static Point resolveParking(World world, MapObjectState vehicle,
                                        Point requested) {
        if (legalParking(world, vehicle, requested.x, requested.y)) return requested;
        ArrayList<Point> candidates = new ArrayList<>();
        for (int radius = 1; radius <= PARKING_SEARCH_RADIUS; radius++) {
            for (int x = requested.x - radius; x <= requested.x + radius; x++) {
                for (int y = requested.y - radius; y <= requested.y + radius; y++) {
                    if (Math.abs(x - requested.x) + Math.abs(y - requested.y) != radius) continue;
                    if (legalParking(world, vehicle, x, y)) candidates.add(new Point(x, y));
                }
            }
            if (!candidates.isEmpty()) break;
        }
        candidates.sort(Comparator
                .comparingInt((Point point) ->
                        Math.abs(point.x - requested.x) + Math.abs(point.y - requested.y))
                .thenComparingInt(point -> point.y)
                .thenComparingInt(point -> point.x));
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private static boolean legalParking(World world, MapObjectState vehicle,
                                        int x, int y) {
        if (world == null || !world.inBounds(x, y)
                || occupied(world, vehicle, x, y)) return false;
        char tile = world.tiles[x][y];
        if (tile == RoadGridIntegrationAuthority.PARKING_SPACE) return true;
        if (tile != RoadGridIntegrationAuthority.SIDEWALK) return false;
        return adjacentRoad(world, x, y);
    }

    private static boolean routeCell(World world, MapObjectState vehicle,
                                     int x, int y, boolean targetCell) {
        if (world == null || !world.inBounds(x, y)
                || occupied(world, vehicle, x, y)) return false;
        char tile = world.tiles[x][y];
        if (tile == RoadGridIntegrationAuthority.ROAD_LANE
                || tile == RoadGridIntegrationAuthority.PARKING_SPACE) return true;
        return targetCell && tile == RoadGridIntegrationAuthority.SIDEWALK
                && adjacentRoad(world, x, y);
    }

    private static boolean occupied(World world, MapObjectState vehicle,
                                    int x, int y) {
        if (world.npcAt(x, y) != null) return true;
        MapObjectState object = world.mapObjectAt(x, y);
        return object != null && object != vehicle;
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
        String existing = MapObjectState.stockValue(vehicle.stockState, key);
        String next = existing == null || existing.isBlank()
                ? clean(entry, "") : existing + "~" + clean(entry, "");
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                key, next.replace(';', ',').replace('|', '/'));
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
