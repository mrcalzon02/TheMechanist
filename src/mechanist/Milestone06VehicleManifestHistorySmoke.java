package mechanist;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Focused smoke for bounded crew, passenger, and cargo manifest histories. */
final class Milestone06VehicleManifestHistorySmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.turn = 1_600;
            game.worldTurn = 1_600L;
            game.inventory.clear();

            MapObjectState crewCar = vehicle(game.world, 4, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    201L);
            MapObjectState passengerCar = vehicle(game.world, 7, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    202L);
            MapObjectState cargoTruck = vehicle(game.world, 10, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    203L);
            game.world.mapObjects.add(crewCar);
            game.world.mapObjects.add(passengerCar);
            game.world.mapObjects.add(cargoTruck);

            for (int cycle = 0; cycle < 19; cycle++) {
                game.turn++;
                VehicleManifestAuthority.Result assigned =
                        VehicleManifestAuthority.assignDriver(game, crewCar,
                                "Driver " + cycle);
                require(assigned.success() && assigned.changed(),
                        "crew cycle " + cycle + " should assign a driver");
                game.turn++;
                VehicleManifestAuthority.Result released =
                        VehicleManifestAuthority.releaseDriver(game, crewCar,
                                "crew release " + cycle);
                require(released.success() && released.changed()
                                && released.snapshot().driver().isBlank(),
                        "crew cycle " + cycle + " should release its driver");

                game.turn++;
                VehicleManifestAuthority.Result boarded =
                        VehicleManifestAuthority.boardPassenger(game,
                                passengerCar, "Passenger " + cycle);
                require(boarded.success() && boarded.changed(),
                        "passenger cycle " + cycle + " should board");
                game.turn++;
                VehicleManifestAuthority.Result disembarked =
                        VehicleManifestAuthority.disembarkPassenger(game,
                                passengerCar, "Passenger " + cycle,
                                "passenger release " + cycle);
                require(disembarked.success() && disembarked.changed()
                                && disembarked.snapshot().passengers().isEmpty(),
                        "passenger cycle " + cycle + " should disembark");

                game.turn++;
                VehicleManifestAuthority.Result loaded =
                        VehicleManifestAuthority.registerCargo(game,
                                cargoTruck, "Cargo " + cycle,
                                "History Owner", 1);
                require(loaded.success() && loaded.changed(),
                        "cargo cycle " + cycle + " should register one unit");
                game.turn++;
                VehicleManifestAuthority.Result unloaded =
                        VehicleManifestAuthority.unloadCargo(game,
                                cargoTruck, "Cargo " + cycle,
                                "History Owner", 1,
                                "cargo release " + cycle);
                require(unloaded.success() && unloaded.changed()
                                && unloaded.snapshot().cargo().isEmpty(),
                        "cargo cycle " + cycle + " should unload its unit");
            }

            game.turn++;
            require(VehicleManifestAuthority.assignDriver(game, crewCar,
                            "Driver 19").success(),
                    "final crew cycle should leave one assigned driver");
            game.turn++;
            require(VehicleManifestAuthority.boardPassenger(game, passengerCar,
                            "Passenger 19").success(),
                    "final passenger cycle should leave one passenger aboard");
            game.turn++;
            require(VehicleManifestAuthority.registerCargo(game, cargoTruck,
                            "Cargo 19", "History Owner", 1).success(),
                    "final cargo cycle should leave one registered unit");

            List<String> crewHistory = history(crewCar, "crewHistory");
            List<String> passengerHistory = history(passengerCar,
                    "passengerHistory");
            List<String> cargoHistory = history(cargoTruck, "cargoHistory");

            requireBoundary(crewHistory, "crew release 13",
                    "Driver assigned: Driver 19", "crew release 12",
                    "Driver assigned: Driver 13", "crew");
            requireBoundary(passengerHistory, "passenger release 13",
                    "Passenger boarded: Passenger 19",
                    "passenger release 12",
                    "Passenger boarded: Passenger 13", "passenger");
            requireBoundary(cargoHistory, "cargo release 13",
                    "Cargo registered: Cargo 19",
                    "cargo release 12", "Cargo registered: Cargo 13",
                    "cargo");

            VehicleManifestAuthority.Snapshot crew =
                    VehicleManifestAuthority.inspect(game.world, crewCar);
            VehicleManifestAuthority.Snapshot passengers =
                    VehicleManifestAuthority.inspect(game.world, passengerCar);
            VehicleManifestAuthority.Snapshot cargo =
                    VehicleManifestAuthority.inspect(game.world, cargoTruck);
            require("Driver 19".equals(crew.driver()) && crew.crew().isEmpty(),
                    "bounded crew history must not alter the final active driver");
            require(passengers.passengers().equals(List.of("Passenger 19")),
                    "bounded passenger history must not alter the active passenger");
            require(cargo.cargoUnits() == 1 && cargo.cargo().size() == 1
                            && "Cargo 19".equals(cargo.cargo().get(0).label())
                            && "History Owner".equals(cargo.cargo().get(0).owner()),
                    "bounded cargo history must not alter active cargo custody");

            MapObjectState restoredCrew = copy(crewCar);
            MapObjectState restoredPassenger = copy(passengerCar);
            MapObjectState restoredCargo = copy(cargoTruck);
            require("Driver 19".equals(VehicleManifestAuthority.inspect(
                            game.world, restoredCrew).driver())
                            && history(restoredCrew, "crewHistory").equals(
                            crewHistory),
                    "crew assignment and bounded history should reconstruct from stock state");
            require(VehicleManifestAuthority.inspect(game.world,
                            restoredPassenger).passengers().equals(
                            List.of("Passenger 19"))
                            && history(restoredPassenger,
                            "passengerHistory").equals(passengerHistory),
                    "passenger assignment and bounded history should reconstruct from stock state");
            require(VehicleManifestAuthority.inspect(game.world,
                            restoredCargo).cargoUnits() == 1
                            && history(restoredCargo, "cargoHistory").equals(
                            cargoHistory),
                    "cargo custody and bounded history should reconstruct from stock state");

            System.out.println("Milestone 06 vehicle manifest history smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
        Milestone06VehicleDashboardSmoke.main(args);
    }

    private static void requireBoundary(List<String> history,
                                        String oldestExpected,
                                        String newestExpected,
                                        String retiredFirst,
                                        String retiredSecond,
                                        String label) {
        require(history.size() == 12,
                label + " history should retain exactly twelve entries: "
                        + history);
        require(history.get(0).contains(oldestExpected),
                label + " oldest retained entry is wrong: " + history.get(0));
        require(history.get(history.size() - 1).contains(newestExpected),
                label + " newest retained entry is wrong: "
                        + history.get(history.size() - 1));
        require(history.stream().noneMatch(line -> line.contains(retiredFirst)
                        || line.contains(retiredSecond)),
                label + " retired entries survived the retention boundary: "
                        + history);
    }

    private static List<String> history(MapObjectState vehicle, String key) {
        String value = MapObjectState.stockValue(vehicle.stockState, key);
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("~"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static World world() {
        World world = new World(61047L, 18, 12);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.sectorX = 2;
        world.sectorY = 2;
        world.zoneX = 1;
        world.zoneY = 2;
        world.floor = 0;
        world.mapObjects.clear();
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '.';
        }
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
                "VEHICLE-MANIFEST-HISTORY-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, Faction.NONE,
                "player-owned", "manifest-history-smoke", false,
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06VehicleManifestHistorySmoke() { }
}
