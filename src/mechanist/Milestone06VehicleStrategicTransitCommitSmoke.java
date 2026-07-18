package mechanist;

import java.util.List;
import java.util.Random;
import java.util.Set;

/** Focused smoke for strategic reservation commit, fuel accounting, rollback boundaries, and interrupted recovery. */
final class Milestone06VehicleStrategicTransitCommitSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            World source = world(61031L, 1, 1, 1);
            World destination = world(61032L, 2, 1, 1);
            game.world = source;
            game.turn = 1_260;
            game.worldTurn = 1_260L;
            game.inventory.clear();

            MapObjectState vehicle = vehicle(source, 3, 5, 131L,
                    VehicleRuntimeAuthority.OwnerType.PLAYER, "player-owned");
            source.mapObjects.add(vehicle);
            VehicleFuelAuthority.ensureInitialized(source, vehicle);
            VehicleFuelAuthority.Snapshot initialFuel =
                    VehicleFuelAuthority.inspect(source, vehicle);
            VehicleStrategicTransitAuthority.Request route = route(destination,
                    18, 1, 12, 4);
            VehicleStrategicTransitAuthority.Reservation reservation =
                    VehicleStrategicTransitAuthority.reserve(game, vehicle, route);
            require(reservation.success() && reservation.changed(),
                    "valid route should create a strategic reservation");
            int reservedFuel = Integer.parseInt(MapObjectState.stockValue(
                    vehicle.stockState, "strategicTransitFuelReserved"));
            require(reservedFuel > 0
                            && VehicleFuelAuthority.inspect(source, vehicle).current()
                            == initialFuel.current(),
                    "reservation should reserve but not consume persistent vehicle energy");

            VehicleStrategicTransitCommitAuthority.Result committed =
                    VehicleStrategicTransitCommitAuthority.commit(game, vehicle,
                            new VehicleStrategicTransitCommitAuthority.TransferRequest(
                                    source, destination, 14, 5,
                                    "district road transfer"));
            require(committed.success()
                            && committed.status()
                            == VehicleStrategicTransitCommitAuthority.Status.COMMITTED
                            && committed.fuelConsumed() == reservedFuel
                            && !source.mapObjects.contains(vehicle)
                            && destination.mapObjects.contains(vehicle)
                            && vehicle.x == 14 && vehicle.y == 5,
                    "commit should move the same physical vehicle fixture exactly once");
            VehicleFuelAuthority.Snapshot afterCommit =
                    VehicleFuelAuthority.inspect(destination, vehicle);
            require(afterCommit.current() == initialFuel.current() - reservedFuel
                            && afterCommit.reserved() == 0
                            && "completed".equals(MapObjectState.stockValue(
                            vehicle.stockState, "strategicTransitState")),
                    "commit should consume reserved energy exactly once and close reservation");
            requireContains(MapObjectState.stockValue(
                            vehicle.stockState, "deploymentHistory"),
                    "Strategic transfer", "committed transfer provenance");
            String committedStock = vehicle.stockState;
            VehicleStrategicTransitCommitAuthority.Result duplicateCommit =
                    VehicleStrategicTransitCommitAuthority.commit(game, vehicle,
                            new VehicleStrategicTransitCommitAuthority.TransferRequest(
                                    source, destination, 14, 5,
                                    "duplicate commit"));
            require(!duplicateCommit.success()
                            && committedStock.equals(vehicle.stockState)
                            && !source.mapObjects.contains(vehicle)
                            && destination.mapObjects.contains(vehicle),
                    "completed transfer must reject duplicate commit without double fuel use");

            MapObjectState blockedVehicle = vehicle(source, 5, 5, 132L,
                    VehicleRuntimeAuthority.OwnerType.PLAYER, "player-owned");
            source.mapObjects.add(blockedVehicle);
            VehicleFuelAuthority.ensureInitialized(source, blockedVehicle);
            require(VehicleStrategicTransitAuthority.reserve(game,
                    blockedVehicle, route(destination, 12, 1, 12, 3)).success(),
                    "blocked-destination fixture should reserve successfully");
            String blockedStock = blockedVehicle.stockState;
            VehicleStrategicTransitCommitAuthority.Result badParking =
                    VehicleStrategicTransitCommitAuthority.commit(game,
                            blockedVehicle,
                            new VehicleStrategicTransitCommitAuthority.TransferRequest(
                                    source, destination, 8, 8,
                                    "invalid destination test"));
            require(!badParking.success()
                            && source.mapObjects.contains(blockedVehicle)
                            && !destination.mapObjects.contains(blockedVehicle)
                            && blockedVehicle.x == 5 && blockedVehicle.y == 5
                            && blockedStock.equals(blockedVehicle.stockState),
                    "invalid destination should leave source placement, fuel, and reservation untouched");

            int blockedReserved = Integer.parseInt(MapObjectState.stockValue(
                    blockedVehicle.stockState, "strategicTransitFuelReserved"));
            blockedVehicle.stockState = MapObjectState.setStockFlag(
                    blockedVehicle.stockState, "fuelOrPowerCurrent",
                    Integer.toString(Math.max(0, blockedReserved - 1)));
            String staleFuelStock = blockedVehicle.stockState;
            VehicleStrategicTransitCommitAuthority.Result staleFuel =
                    VehicleStrategicTransitCommitAuthority.commit(game,
                            blockedVehicle,
                            new VehicleStrategicTransitCommitAuthority.TransferRequest(
                                    source, destination, 16, 5,
                                    "stale fuel test"));
            require(!staleFuel.success()
                            && source.mapObjects.contains(blockedVehicle)
                            && !destination.mapObjects.contains(blockedVehicle)
                            && staleFuelStock.equals(blockedVehicle.stockState),
                    "fuel loss after reservation should block transfer before world mutation");

            MapObjectState duplicateRecovery = vehicle(source, 7, 5, 133L,
                    VehicleRuntimeAuthority.OwnerType.PLAYER, "player-owned");
            source.mapObjects.add(duplicateRecovery);
            VehicleFuelAuthority.ensureInitialized(source, duplicateRecovery);
            markInterrupted(duplicateRecovery, source, destination, 7, 5, 18, 5, 3);
            destination.mapObjects.add(duplicateRecovery);
            duplicateRecovery.x = 18;
            duplicateRecovery.y = 5;
            VehicleStrategicTransitCommitAuthority.Result recoveredDuplicate =
                    VehicleStrategicTransitCommitAuthority.recoverInterrupted(
                            game, duplicateRecovery, source, destination);
            require(recoveredDuplicate.success()
                            && recoveredDuplicate.status()
                            == VehicleStrategicTransitCommitAuthority.Status.RECOVERED
                            && source.mapObjects.contains(duplicateRecovery)
                            && !destination.mapObjects.contains(duplicateRecovery)
                            && duplicateRecovery.x == 7 && duplicateRecovery.y == 5
                            && "reserved".equals(MapObjectState.stockValue(
                            duplicateRecovery.stockState,
                            "strategicTransitState")),
                    "duplicate placement recovery should retain source and reservation");

            MapObjectState destinationRecovery = vehicle(source, 9, 5, 134L,
                    VehicleRuntimeAuthority.OwnerType.PLAYER, "player-owned");
            source.mapObjects.add(destinationRecovery);
            VehicleFuelAuthority.ensureInitialized(source, destinationRecovery);
            int destinationFuelBefore = VehicleFuelAuthority.inspect(
                    source, destinationRecovery).current();
            markInterrupted(destinationRecovery, source, destination,
                    9, 5, 20, 5, 4);
            source.mapObjects.remove(destinationRecovery);
            destinationRecovery.x = 20;
            destinationRecovery.y = 5;
            destination.mapObjects.add(destinationRecovery);
            VehicleStrategicTransitCommitAuthority.Result recoveredDestination =
                    VehicleStrategicTransitCommitAuthority.recoverInterrupted(
                            game, destinationRecovery, source, destination);
            require(recoveredDestination.success()
                            && destination.mapObjects.contains(destinationRecovery)
                            && !source.mapObjects.contains(destinationRecovery)
                            && "completed".equals(MapObjectState.stockValue(
                            destinationRecovery.stockState,
                            "strategicTransitState"))
                            && VehicleFuelAuthority.inspect(destination,
                            destinationRecovery).current()
                            == destinationFuelBefore - 4,
                    "destination-only recovery should complete transfer and commit reserved energy");

            MapObjectState failedRecovery = vehicle(source, 11, 5, 135L,
                    VehicleRuntimeAuthority.OwnerType.PLAYER, "player-owned");
            source.mapObjects.add(failedRecovery);
            VehicleFuelAuthority.ensureInitialized(source, failedRecovery);
            markInterrupted(failedRecovery, source, destination,
                    11, 5, 22, 5, 5);
            failedRecovery.stockState = MapObjectState.setStockFlag(
                    failedRecovery.stockState, "fuelOrPowerCurrent", "0");
            source.mapObjects.remove(failedRecovery);
            failedRecovery.x = 22;
            failedRecovery.y = 5;
            destination.mapObjects.add(failedRecovery);
            VehicleStrategicTransitCommitAuthority.Result recoveredSource =
                    VehicleStrategicTransitCommitAuthority.recoverInterrupted(
                            game, failedRecovery, source, destination);
            require(recoveredSource.success()
                            && source.mapObjects.contains(failedRecovery)
                            && !destination.mapObjects.contains(failedRecovery)
                            && failedRecovery.x == 11 && failedRecovery.y == 5
                            && "reserved".equals(MapObjectState.stockValue(
                            failedRecovery.stockState,
                            "strategicTransitState")),
                    "destination recovery without committed energy should return vehicle to source coordinates");

            VehicleFuelAuthority.Result refueled = VehicleFuelAuthority.refuel(
                    game, blockedVehicle, 10, "motor-pool fuel test");
            require(refueled.success() && refueled.changed()
                            && refueled.after() > refueled.before(),
                    "player-owned vehicle should support permission-gated refueling");
            MapObjectState privateVehicle = vehicle(source, 13, 5, 136L,
                    VehicleRuntimeAuthority.OwnerType.PRIVATE, "private");
            source.mapObjects.add(privateVehicle);
            VehicleFuelAuthority.Result deniedRefuel = VehicleFuelAuthority.refuel(
                    game, privateVehicle, 4, "unauthorized refuel test");
            require(!deniedRefuel.success(),
                    "private vehicle should reject refueling without authorization");

            List<String> inspection =
                    VehicleStrategicTransitCommitAuthority.inspectionLines(
                            destination, vehicle);
            requireContains(inspection, "Strategic transfer state",
                    "transfer state inspection");
            requireContains(inspection, "Fuel or power",
                    "energy ledger inspection");
            requireContains(inspection, "failed commits restore",
                    "rollback guidance");
            for (String line : inspection) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "strategic transit commit inspection leaked implementation text: "
                                + line);
            }

            System.out.println("Milestone 06 vehicle strategic transit commit smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static VehicleStrategicTransitAuthority.Request route(
            World destination, int distance, int crew, int availableFuel,
            int requiredFuel) {
        return new VehicleStrategicTransitAuthority.Request(
                destination.locationKey(),
                Set.of(VehicleStrategicTransitAuthority.Infrastructure.ROAD,
                        VehicleStrategicTransitAuthority.Infrastructure.GATE,
                        VehicleStrategicTransitAuthority.Infrastructure.CHECKPOINT,
                        VehicleStrategicTransitAuthority.Infrastructure.PARKING_LOT),
                distance, 4, crew, true, availableFuel, requiredFuel,
                true, true, false, Faction.NONE,
                "strategic transit commit smoke");
    }

    private static void markInterrupted(MapObjectState vehicle,
                                        World source, World destination,
                                        int sourceX, int sourceY,
                                        int destinationX, int destinationY,
                                        int reservedFuel) {
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitState", "committing");
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitReservationId", "INTERRUPTED-"
                        + Math.abs(vehicle.id.hashCode()));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitCommitSource", source.locationKey());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitCommitDestination", destination.locationKey());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitSourceX", Integer.toString(sourceX));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitSourceY", Integer.toString(sourceY));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitCommitX", Integer.toString(destinationX));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitCommitY", Integer.toString(destinationY));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitFuelReserved", Integer.toString(reservedFuel));
    }

    private static World world(long seed, int sectorX, int zoneX, int floor) {
        World world = new World(seed, 28, 14);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.sectorX = sectorX;
        world.sectorY = 1;
        world.zoneX = zoneX;
        world.zoneY = 1;
        world.floor = floor;
        world.mapObjects.clear();
        world.npcs.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '.';
        }
        for (int x = 2; x < world.w - 2; x++) {
            world.tiles[x][5] = RoadGridIntegrationAuthority.ROAD_LANE;
        }
        for (int x = 4; x < world.w - 2; x += 2) {
            world.tiles[x][5] = RoadGridIntegrationAuthority.PARKING_SPACE;
        }
        return world;
    }

    private static MapObjectState vehicle(World world, int x, int y,
                                          long seed,
                                          VehicleRuntimeAuthority.OwnerType ownerType,
                                          String ownership) {
        String type = AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR;
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "VEHICLE-STRATEGIC-COMMIT-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, Faction.NONE,
                ownership, "strategic-transfer", false, new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", ownerType.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", ownerType == VehicleRuntimeAuthority.OwnerType.PLAYER
                        ? "Player" : "Registered private owner");
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", ownership);
        return vehicle;
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

    private Milestone06VehicleStrategicTransitCommitSmoke() { }
}
