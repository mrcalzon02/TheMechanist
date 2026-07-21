package mechanist;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Focused smoke for bounded persistent strategic vehicle transit history. */
final class Milestone06VehicleStrategicTransitHistorySmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.worldTurn = 1_500L;
            game.inventory.clear();

            MapObjectState car = vehicle(game.world, 4, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    181L);
            game.world.mapObjects.add(car);

            int scriptBefore = game.carriedScript;
            int suppliesBefore = game.supplies;
            int partsBefore = game.machineParts;
            int startX = car.x;
            int startY = car.y;

            for (int cycle = 0; cycle < 19; cycle++) {
                game.worldTurn++;
                VehicleStrategicTransitAuthority.Request request =
                        request(cycle);
                VehicleStrategicTransitAuthority.Reservation reserved =
                        VehicleStrategicTransitAuthority.reserve(game, car,
                                request);
                require(reserved.success() && reserved.changed()
                                && reserved.readiness().status()
                                == VehicleStrategicTransitAuthority.Status.RESERVED,
                        "cycle " + cycle + " should create a strategic reservation");

                game.worldTurn++;
                VehicleStrategicTransitAuthority.Reservation cancelled =
                        VehicleStrategicTransitAuthority.cancel(game, car,
                                "cycle cancel " + cycle);
                require(cancelled.success() && cancelled.changed()
                                && "cancelled".equals(MapObjectState.stockValue(
                                car.stockState, "strategicTransitState"))
                                && "0".equals(MapObjectState.stockValue(
                                car.stockState, "strategicTransitFuelReserved")),
                        "cycle " + cycle + " should cancel without consuming fuel");
            }

            game.worldTurn++;
            VehicleStrategicTransitAuthority.Reservation finalReservation =
                    VehicleStrategicTransitAuthority.reserve(game, car,
                            request(19));
            require(finalReservation.success() && finalReservation.changed()
                            && "reserved".equals(MapObjectState.stockValue(
                            car.stockState, "strategicTransitState"))
                            && "sector-2/zone-19/floor-0".equals(
                            MapObjectState.stockValue(car.stockState,
                                    "strategicTransitDestination"))
                            && "3".equals(MapObjectState.stockValue(
                            car.stockState, "strategicTransitFuelReserved")),
                    "final cycle should leave one exact active reservation");

            List<String> history = tokens(MapObjectState.stockValue(
                    car.stockState, "deploymentHistory"));
            require(history.size() == 12,
                    "strategic transit history should retain exactly twelve entries: "
                            + history);
            require(history.get(0).contains("cycle cancel 13"),
                    "oldest retained strategic transit entry should be cycle 13 cancellation: "
                            + history.get(0));
            require(history.get(history.size() - 1).contains(
                            "sector-2/zone-19/floor-0"),
                    "newest strategic transit entry should be the final reservation: "
                            + history.get(history.size() - 1));
            require(history.stream().noneMatch(line -> line.contains(
                            "cycle cancel 12")
                            || line.contains("sector-2/zone-13/floor-0")),
                    "retired strategic transit history must not survive the retention boundary");

            require(game.carriedScript == scriptBefore
                            && game.supplies == suppliesBefore
                            && game.machineParts == partsBefore
                            && car.x == startX && car.y == startY
                            && game.world.mapObjects.contains(car),
                    "reservation history stress must not consume resources or move the physical vehicle");

            MapObjectState restored = copy(car);
            require("reserved".equals(MapObjectState.stockValue(
                            restored.stockState, "strategicTransitState"))
                            && "sector-2/zone-19/floor-0".equals(
                            MapObjectState.stockValue(restored.stockState,
                                    "strategicTransitDestination"))
                            && tokens(MapObjectState.stockValue(restored.stockState,
                            "deploymentHistory")).equals(history),
                    "bounded reservation state and history should reconstruct from persisted stock state");

            System.out.println("Milestone 06 strategic vehicle transit history smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static VehicleStrategicTransitAuthority.Request request(int cycle) {
        return new VehicleStrategicTransitAuthority.Request(
                "sector-2/zone-" + cycle + "/floor-0",
                Set.of(VehicleStrategicTransitAuthority.Infrastructure.ROAD,
                        VehicleStrategicTransitAuthority.Infrastructure.CHECKPOINT,
                        VehicleStrategicTransitAuthority.Infrastructure.PARKING_LOT),
                18, 2, 3, true, 8, 3,
                true, true, false, Faction.NONE,
                "history stress route " + cycle);
    }

    private static List<String> tokens(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split("~"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private static World world() {
        World world = new World(61031L, 18, 12);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.sectorX = 1;
        world.sectorY = 1;
        world.zoneX = 2;
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
                "VEHICLE-STRATEGIC-TRANSIT-HISTORY-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, Faction.NONE,
                "player-owned", "strategic-transit-history-smoke", false,
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

    private Milestone06VehicleStrategicTransitHistorySmoke() { }
}
