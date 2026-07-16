package mechanist;

import java.awt.Point;
import java.util.List;
import java.util.Random;

/** Focused smoke for local road routing, legal parking, and manual-plan execution. */
final class Milestone06VehicleTransitSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.playerX = 1;
            game.playerY = 1;
            game.turn = 700;
            game.worldTurn = 700L;
            MapObjectState vehicle = vehicle(game.world, 2, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    VehicleRuntimeAuthority.OwnerType.PLAYER,
                    Faction.NONE, "player-owned", 31L);
            game.world.mapObjects.add(vehicle);

            VehicleTransitAuthority.RoutePlan plan =
                    VehicleTransitAuthority.plan(game, vehicle, 25, 4);
            require(plan.valid() && plan.steps() == 23
                            && plan.resolvedParking().equals(new Point(25, 4)),
                    "contiguous road route should reach the marked destination parking cell: "
                            + plan.summary());
            require(contiguous(plan.path()),
                    "vehicle route preview must be a contiguous one-tile path");
            int playerX = game.playerX;
            int playerY = game.playerY;
            VehicleTransitAuthority.TransitResult moved =
                    VehicleTransitAuthority.execute(game, vehicle, plan);
            require(moved.success() && moved.changed()
                            && moved.fromX() == 2 && moved.fromY() == 4
                            && moved.toX() == 25 && moved.toY() == 4
                            && vehicle.x == 25 && vehicle.y == 4,
                    "validated route should move the physical vehicle fixture to legal parking");
            require(game.playerX == playerX && game.playerY == playerY,
                    "vehicle transit must not silently teleport the player actor");
            require("parked".equals(MapObjectState.stockValue(
                            vehicle.stockState, "operationState"))
                            && "false".equals(MapObjectState.stockValue(
                            vehicle.stockState, "headlightsActive")),
                    "completed route should stop the running state and headlights");
            requireContains(MapObjectState.stockValue(vehicle.stockState, "lastRoute"),
                    "2,4", "vehicle route origin history");
            requireContains(MapObjectState.stockValue(vehicle.stockState, "lastRoute"),
                    "25,4", "vehicle route destination history");
            requireContains(MapObjectState.stockValue(
                            vehicle.stockState, "deploymentHistory"),
                    "Local road route", "vehicle deployment history");

            // A clear sidewalk immediately beside the road is a legal resolved
            // parking target but never becomes an intermediate through-route.
            game.world.tiles[22][3] = RoadGridIntegrationAuthority.SIDEWALK;
            VehicleTransitAuthority.RoutePlan curb =
                    VehicleTransitAuthority.plan(game, vehicle, 22, 3);
            require(curb.valid() && curb.resolvedParking().equals(new Point(22, 3))
                            && curb.path().get(curb.path().size() - 1)
                            .equals(new Point(22, 3)),
                    "clear curb-adjacent sidewalk should resolve as final parking");
            int sidewalkCount = 0;
            for (int i = 0; i < curb.path().size(); i++) {
                Point point = curb.path().get(i);
                if (game.world.tiles[point.x][point.y]
                        == RoadGridIntegrationAuthority.SIDEWALK) sidewalkCount++;
            }
            require(sidewalkCount == 1,
                    "sidewalk may be the final parking cell but not a vehicle through-route");

            VehicleTransitAuthority.RoutePlan remoteOffRoad =
                    VehicleTransitAuthority.plan(game, vehicle, 27, 12);
            require(!remoteOffRoad.valid()
                            && "no-legal-parking".equals(remoteOffRoad.blocker()),
                    "remote off-road target should fail without a nearby legal parking cell");

            // Return to the west parking cell before obstacle tests.
            VehicleTransitAuthority.RoutePlan west =
                    VehicleTransitAuthority.plan(game, vehicle, 2, 4);
            require(west.valid(), "return route should be available");
            require(VehicleTransitAuthority.execute(game, vehicle, west).success(),
                    "return route should execute");

            VehicleTransitAuthority.RoutePlan stale =
                    VehicleTransitAuthority.plan(game, vehicle, 25, 4);
            require(stale.valid(), "stale-route fixture should begin valid");
            MapObjectState obstruction = new MapObjectState();
            obstruction.id = "VEHICLE-ROUTE-OBSTRUCTION";
            obstruction.type = "road-work-obstruction";
            obstruction.label = "Road works barrier";
            obstruction.glyph = 'X';
            obstruction.x = 13;
            obstruction.y = 4;
            obstruction.stockState = "route-blocking=true";
            game.world.mapObjects.add(obstruction);
            VehicleTransitAuthority.TransitResult staleResult =
                    VehicleTransitAuthority.execute(game, vehicle, stale);
            require(!staleResult.success()
                            && vehicle.x == 2 && vehicle.y == 4
                            && ("route-state-changed".equals(staleResult.plan().blocker())
                            || "no-road-route".equals(staleResult.plan().blocker())),
                    "changed road occupancy must invalidate preview before movement");
            require(!VehicleTransitAuthority.plan(game, vehicle, 25, 4).valid(),
                    "single-lane route should remain blocked while the obstacle exists");
            game.world.mapObjects.remove(obstruction);

            game.manualMovementPlanActive = true;
            game.manualMovementPlanPath.clear();
            game.manualMovementPlanPath.add(new Point(25, 4));
            VehicleTransitAuthority.TransitResult manual =
                    VehicleTransitAuthority.executeManualPlan(game, vehicle);
            require(manual.success() && vehicle.x == 25 && vehicle.y == 4
                            && !game.manualMovementPlanActive,
                    "manual movement endpoint should execute through vehicle validation and clear after success");

            VehicleRuntimeAuthority.applyDamage(vehicle,
                    VehicleRuntimeAuthority.Component.MOBILITY, 100,
                    game.turn, "smoke disabled wheels");
            VehicleTransitAuthority.RoutePlan disabled =
                    VehicleTransitAuthority.plan(game, vehicle, 2, 4);
            require(!disabled.valid()
                            && ("vehicle-not-operational".equals(disabled.blocker())
                            || "vehicle-mobility-failure".equals(disabled.blocker())),
                    "disabled mobility assembly must block vehicle routing");

            MapObjectState factionTruck = vehicle(game.world, 2, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    VehicleRuntimeAuthority.OwnerType.FACTION,
                    Faction.NOBLE, "faction", 32L);
            game.world.mapObjects.add(factionTruck);
            VehicleTransitAuthority.RoutePlan denied =
                    VehicleTransitAuthority.plan(game, factionTruck, 25, 4);
            require(!denied.valid()
                            && "vehicle-access-denied".equals(denied.blocker()),
                    "player route command must not operate faction-owned vehicles");

            List<String> inspection =
                    VehicleTransitAuthority.inspectionLines(game, vehicle);
            requireContains(inspection, "roads and marked parking",
                    "vehicle transit rule inspection");
            requireContains(inspection, "manual movement plan",
                    "vehicle transit control inspection");
            for (String line : inspection) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "vehicle transit inspection leaked implementation text: " + line);
            }

            System.out.println("Milestone 06 vehicle transit smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World world() {
        World world = new World(61009L, 30, 15);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.sectorX = 1;
        world.sectorY = 1;
        world.zoneX = 2;
        world.zoneY = 2;
        world.floor = 4;
        world.mapObjects.clear();
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '#';
        }
        world.tiles[2][4] = RoadGridIntegrationAuthority.PARKING_SPACE;
        for (int x = 3; x < 25; x++) {
            world.tiles[x][4] = RoadGridIntegrationAuthority.ROAD_LANE;
        }
        world.tiles[25][4] = RoadGridIntegrationAuthority.PARKING_SPACE;
        return world;
    }

    private static MapObjectState vehicle(World world, int x, int y, String type,
                                          VehicleRuntimeAuthority.OwnerType ownerType,
                                          Faction faction, String ownership,
                                          long seed) {
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed), "VEHICLE-TRANSIT-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle,
                faction == null ? Faction.NONE : faction,
                ownership, "local-transit", false, new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", ownerType.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", (faction == null ? Faction.NONE : faction).name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", ownership);
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", ownerType == VehicleRuntimeAuthority.OwnerType.PLAYER
                        ? "Player" : faction == null ? "Unassigned" : faction.label);
        return vehicle;
    }

    private static boolean contiguous(List<Point> path) {
        if (path == null || path.isEmpty()) return false;
        for (int i = 1; i < path.size(); i++) {
            Point before = path.get(i - 1);
            Point after = path.get(i);
            if (Math.abs(before.x - after.x) + Math.abs(before.y - after.y) != 1) return false;
        }
        return true;
    }

    private static void requireContains(List<String> lines, String expected,
                                        String label) {
        for (String line : lines) {
            if (line != null && line.contains(expected)) return;
        }
        throw new AssertionError("Expected " + label + " to contain '"
                + expected + "': " + lines);
    }

    private static void requireContains(String text, String expected,
                                        String label) {
        require(text != null && text.contains(expected),
                "Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06VehicleTransitSmoke() { }
}
