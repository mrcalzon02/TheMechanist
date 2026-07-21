package mechanist;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Focused smoke for vehicle class dossiers, live dashboards, and bounded local-route history. */
final class Milestone06VehicleDashboardSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.playerX = 1;
            game.playerY = 5;
            game.turn = 2_000;
            game.worldTurn = 2_000L;
            game.options.soundEnabled = false;
            game.inventory.clear();
            VehicleOperationFeedbackAuthority.clearTransientFeedback();

            proveClassDossiers();

            MapObjectState dashboardCar = vehicle(game.world, 3, 3,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    221L);
            game.world.mapObjects.add(dashboardCar);
            require(VehicleManifestAuthority.assignDriver(game, dashboardCar,
                            "Dashboard Driver").success(),
                    "dashboard fixture should accept a driver");
            require(VehicleManifestAuthority.addCrew(game, dashboardCar,
                            "Dashboard Crew A").success(),
                    "dashboard fixture should accept first operational crew");
            require(VehicleManifestAuthority.addCrew(game, dashboardCar,
                            "Dashboard Crew B").success(),
                    "dashboard fixture should accept second operational crew");
            require(VehicleManifestAuthority.boardPassenger(game, dashboardCar,
                            "Dashboard Passenger").success(),
                    "dashboard fixture should accept a passenger");
            require(VehicleManifestAuthority.registerCargo(game, dashboardCar,
                            "Dashboard Parcel", "Dashboard Owner", 2).success(),
                    "dashboard fixture should accept registered cargo");
            require(VehicleFuelAuthority.consumeCommitted(game.world,
                            dashboardCar, 6, game.turn,
                            "dashboard fuel proof").success(),
                    "dashboard fixture should consume fuel through the canonical ledger");
            VehicleRuntimeAuthority.applyDamage(dashboardCar,
                    VehicleRuntimeAuthority.Component.LIGHTS, 35,
                    game.turn, "dashboard component proof");

            VehicleStrategicTransitAuthority.Request request =
                    VehicleManifestAuthority.strategicRequest(game,
                            dashboardCar, "sector-7/dashboard-depot",
                            Set.of(VehicleStrategicTransitAuthority.Infrastructure.ROAD,
                                    VehicleStrategicTransitAuthority.Infrastructure.CHECKPOINT,
                                    VehicleStrategicTransitAuthority.Infrastructure.PARKING_LOT),
                            12, 2, 2, true, true, false,
                            Faction.NONE, "dashboard reservation proof");
            VehicleStrategicTransitAuthority.Reservation reservation =
                    VehicleStrategicTransitAuthority.reserve(game,
                            dashboardCar, request);
            require(reservation.success() && reservation.changed(),
                    "dashboard fixture should create one real strategic reservation: "
                            + reservation.message());

            List<String> dashboard = VehicleTransitAuthority.inspectionLines(
                    game, dashboardCar);
            requireContains(dashboard, "VEHICLE DASHBOARD", "dashboard title");
            requireContains(dashboard, "civilian car", "class identity");
            requireContains(dashboard, "65%", "remaining component integrity");
            requireContains(dashboard, "18/24", "fuel ledger");
            requireContains(dashboard, "Dashboard Driver", "driver identity");
            requireContains(dashboard, "Dashboard Passenger", "passenger identity");
            requireContains(dashboard, "Dashboard Parcel", "cargo custody");
            requireContains(dashboard, "sector-7/dashboard-depot",
                    "strategic destination");
            requireContains(dashboard, "civilian car vehicle dossier",
                    "Infopedia dossier title");
            requireContains(dashboard, "LOCAL TRANSIT CONTROL",
                    "existing transit controls");
            for (String line : dashboard) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "vehicle dashboard leaked implementation text: " + line);
                require(!line.contains(dashboardCar.id)
                                && !line.contains("vehicle/civilian_car"),
                        "ordinary dashboard exposed a raw vehicle or dossier identifier: "
                                + line);
            }

            MapObjectState restored = copy(dashboardCar);
            List<String> restoredDashboard =
                    VehicleInfopediaBridgeAuthority.liveDossier(game, restored);
            requireContains(restoredDashboard, "Dashboard Driver",
                    "restored driver identity");
            requireContains(restoredDashboard, "Dashboard Parcel",
                    "restored cargo custody");
            requireContains(restoredDashboard, "sector-7/dashboard-depot",
                    "restored strategic reservation");
            require(VehicleInfopediaBridgeAuthority.liveDossier(game, null)
                            .equals(List.of("No live vehicle dashboard is available.")),
                    "missing fixture should produce one safe dashboard refusal line");

            MapObjectState routeCar = vehicle(game.world, 2, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    222L);
            game.world.mapObjects.add(routeCar);
            for (int route = 1; route <= 14; route++) {
                game.turn++;
                game.worldTurn++;
                int targetX = route % 2 == 1 ? 10 : 2;
                VehicleTransitAuthority.RoutePlan plan =
                        VehicleTransitAuthority.plan(game, routeCar,
                                targetX, 5);
                require(plan.valid(), "route " + route
                        + " should be valid: " + plan.summary());
                VehicleTransitAuthority.TransitResult result =
                        VehicleTransitAuthority.execute(game, routeCar, plan);
                require(result.success() && result.changed()
                                && routeCar.x == targetX && routeCar.y == 5,
                        "route " + route + " should commit to "
                                + targetX + ",5: " + result.message());
            }

            List<String> deployment = history(routeCar, "deploymentHistory");
            List<String> vehicleHistory = history(routeCar, "vehicleHistory");
            require(deployment.size() == 12,
                    "local deployment history should retain twelve entries: "
                            + deployment);
            require(vehicleHistory.size() == 8,
                    "local vehicle history should preserve its established eight-entry limit: "
                            + vehicleHistory);
            require(deployment.get(0).contains("turn 2003")
                            && deployment.get(deployment.size() - 1)
                            .contains("turn 2014"),
                    "deployment retention should preserve turns 2003 through 2014: "
                            + deployment);
            require(vehicleHistory.get(0).contains("turn 2007")
                            && vehicleHistory.get(vehicleHistory.size() - 1)
                            .contains("turn 2014"),
                    "vehicle retention should preserve turns 2007 through 2014: "
                            + vehicleHistory);
            require(deployment.stream().noneMatch(line -> line.contains("turn 2001")
                            || line.contains("turn 2002"))
                            && vehicleHistory.stream().noneMatch(line -> line.contains("turn 2006")
                            || line.contains("turn 2001")),
                    "old local-route records must retire at their authoritative boundaries");

            System.out.println("Milestone 06 vehicle dashboard smoke passed.");
        } finally {
            VehicleOperationFeedbackAuthority.clearTransientFeedback();
            game.shutdownRuntime();
        }
    }

    private static void proveClassDossiers() {
        List<VehicleInfopediaBridgeAuthority.ClassDossier> dossiers =
                VehicleInfopediaBridgeAuthority.catalogDossiers();
        require(dossiers.size() == VehicleRuntimeAuthority.VehicleClass.values().length,
                "every vehicle class should expose exactly one stable dossier");
        HashSet<String> keys = new HashSet<>();
        for (VehicleInfopediaBridgeAuthority.ClassDossier dossier : dossiers) {
            require(dossier != null && keys.add(dossier.key())
                            && dossier.title() != null && !dossier.title().isBlank()
                            && dossier.lines().size() >= 8,
                    "vehicle class dossier should have a unique key and complete content: "
                            + dossier);
            for (String line : dossier.lines()) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "vehicle class dossier leaked implementation text: " + line);
            }
        }
        for (VehicleRuntimeAuthority.VehicleClass definition
                : VehicleRuntimeAuthority.VehicleClass.values()) {
            String expectedKey = "vehicle/" + definition.name().toLowerCase();
            require(keys.contains(expectedKey),
                    "missing vehicle class dossier key " + expectedKey);
            require(VehicleInfopediaBridgeAuthority.description(definition)
                            .contains(definition.label)
                            && VehicleInfopediaBridgeAuthority.useLine(definition)
                            .contains("grants no ownership"),
                    "vehicle class description/use boundary is incomplete for "
                            + definition.label);
        }
    }

    private static List<String> history(MapObjectState vehicle, String key) {
        String value = MapObjectState.stockValue(vehicle.stockState, key);
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split("~"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static World world() {
        World world = new World(61051L, 16, 10);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.sectorX = 3;
        world.sectorY = 2;
        world.zoneX = 1;
        world.zoneY = 1;
        world.floor = 0;
        world.mapObjects.clear();
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '.';
        }
        for (int x = 2; x <= 10; x++) {
            world.tiles[x][5] = RoadGridIntegrationAuthority.ROAD_LANE;
        }
        world.tiles[2][5] = RoadGridIntegrationAuthority.PARKING_SPACE;
        world.tiles[10][5] = RoadGridIntegrationAuthority.PARKING_SPACE;
        return world;
    }

    private static MapObjectState vehicle(World world, int x, int y,
                                          String type, long seed) {
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "VEHICLE-DASHBOARD-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, Faction.NONE,
                "player-owned", "dashboard-smoke", false,
                new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", VehicleRuntimeAuthority.OwnerType.PLAYER.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", Faction.NONE.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", "Player");
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", "player-owned");
        return vehicle;
    }

    private static MapObjectState copy(MapObjectState source) {
        MapObjectState copy = new MapObjectState();
        copy.id = source.id;
        copy.type = source.type;
        copy.label = source.label;
        copy.glyph = source.glyph;
        copy.x = source.x;
        copy.y = source.y;
        copy.stockState = source.stockState;
        return copy;
    }

    private static void requireContains(List<String> lines, String expected,
                                        String label) {
        for (String line : lines) {
            if (line != null && line.contains(expected)) return;
        }
        throw new AssertionError("Expected " + label + " to contain '"
                + expected + "': " + lines);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06VehicleDashboardSmoke() { }
}
