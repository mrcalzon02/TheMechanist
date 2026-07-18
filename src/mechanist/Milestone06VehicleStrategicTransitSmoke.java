package mechanist;

import java.util.List;
import java.util.Random;
import java.util.Set;

/** Focused smoke for cross-zone vehicle readiness and reservation without world-loader duplication. */
final class Milestone06VehicleStrategicTransitSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.worldTurn = 1_200L;
            game.inventory.clear();

            MapObjectState car = vehicle(game.world, 4, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    VehicleRuntimeAuthority.OwnerType.PLAYER,
                    Faction.NONE, "player-owned", 111L);
            game.world.mapObjects.add(car);

            VehicleStrategicTransitAuthority.Request blockedRequest =
                    new VehicleStrategicTransitAuthority.Request(
                            "sector-2/zone-3/floor-0",
                            Set.of(VehicleStrategicTransitAuthority.Infrastructure.ROAD,
                                    VehicleStrategicTransitAuthority.Infrastructure.CHECKPOINT,
                                    VehicleStrategicTransitAuthority.Infrastructure.PARKING_LOT),
                            18, 2, 0, false, 0, 0,
                            true, false, false, Faction.NONE,
                            "cross-district delivery");
            VehicleStrategicTransitAuthority.Readiness blocked =
                    VehicleStrategicTransitAuthority.evaluate(game, car,
                            blockedRequest);
            require(!blocked.allowed()
                            && blocked.status()
                            == VehicleStrategicTransitAuthority.Status.BLOCKED,
                    "uncrewed, unfueled, closed-checkpoint route should be blocked");
            requireContains(blocked.blockers(), "assign a driver",
                    "driver blocker");
            requireContains(blocked.blockers(), "assign 3 crew",
                    "crew blocker");
            requireContains(blocked.blockers(), "fuel or power units",
                    "fuel blocker");
            requireContains(blocked.blockers(), "checkpoint is closed",
                    "checkpoint blocker");

            VehicleStrategicTransitAuthority.Request readyRequest =
                    new VehicleStrategicTransitAuthority.Request(
                            "sector-2/zone-3/floor-0",
                            Set.of(VehicleStrategicTransitAuthority.Infrastructure.ROAD,
                                    VehicleStrategicTransitAuthority.Infrastructure.CHECKPOINT,
                                    VehicleStrategicTransitAuthority.Infrastructure.PARKING_LOT),
                            18, 2, 3, true, 8, 3,
                            true, true, false, Faction.NONE,
                            "cross-district delivery");
            VehicleStrategicTransitAuthority.Readiness ready =
                    VehicleStrategicTransitAuthority.evaluate(game, car,
                            readyRequest);
            require(ready.allowed()
                            && ready.status()
                            == VehicleStrategicTransitAuthority.Status.READY
                            && ready.crewRequired() == 3
                            && ready.fuelRequired() == 3,
                    "fully staffed civilian route should be ready: "
                            + ready.summary());

            int scriptBefore = game.carriedScript;
            int suppliesBefore = game.supplies;
            int machinePartsBefore = game.machineParts;
            List<String> inventoryBefore = List.copyOf(game.inventory);
            VehicleStrategicTransitAuthority.Reservation reservation =
                    VehicleStrategicTransitAuthority.reserve(game, car,
                            readyRequest);
            require(reservation.success() && reservation.changed()
                            && !reservation.reservationId().isBlank()
                            && reservation.readiness().status()
                            == VehicleStrategicTransitAuthority.Status.RESERVED,
                    "ready route should create one persistent reservation");
            require("reserved".equals(MapObjectState.stockValue(
                            car.stockState, "strategicTransitState"))
                            && "sector-2/zone-3/floor-0".equals(
                            MapObjectState.stockValue(car.stockState,
                                    "strategicTransitDestination"))
                            && "3".equals(MapObjectState.stockValue(
                            car.stockState, "strategicTransitFuelReserved")),
                    "reservation should persist destination and reserved fuel without moving worlds");
            require(game.carriedScript == scriptBefore
                            && game.supplies == suppliesBefore
                            && game.machineParts == machinePartsBefore
                            && inventoryBefore.equals(game.inventory),
                    "route reservation must not consume player resources before transition commit");
            require(game.world.mapObjects.contains(car)
                            && car.x == 4 && car.y == 5,
                    "route reservation must not teleport or remove the physical vehicle");

            VehicleStrategicTransitAuthority.Reservation duplicate =
                    VehicleStrategicTransitAuthority.reserve(game, car,
                            readyRequest);
            require(duplicate.success() && !duplicate.changed()
                            && duplicate.reservationId().equals(
                            reservation.reservationId()),
                    "repeat reservation should be idempotent");

            VehicleStrategicTransitAuthority.Reservation cancelled =
                    VehicleStrategicTransitAuthority.cancel(game, car,
                            "destination gate schedule changed");
            require(cancelled.success() && cancelled.changed()
                            && "cancelled".equals(MapObjectState.stockValue(
                            car.stockState, "strategicTransitState"))
                            && "0".equals(MapObjectState.stockValue(
                            car.stockState, "strategicTransitFuelReserved")),
                    "cancellation should release reserved fuel without consuming it");

            MapObjectState tank = vehicle(game.world, 8, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK,
                    VehicleRuntimeAuthority.OwnerType.PLAYER,
                    Faction.NONE, "player-owned", 112L);
            game.world.mapObjects.add(tank);
            VehicleStrategicTransitAuthority.Request weakTankRoute =
                    new VehicleStrategicTransitAuthority.Request(
                            "sector-4/vehicle-yard",
                            Set.of(VehicleStrategicTransitAuthority.Infrastructure.ROAD,
                                    VehicleStrategicTransitAuthority.Infrastructure.ALLEY,
                                    VehicleStrategicTransitAuthority.Infrastructure.PARKING_LOT),
                            10, 2, 18, true, 20, 5,
                            true, true, false, Faction.NONE,
                            "armored redeployment");
            VehicleStrategicTransitAuthority.Readiness weakTank =
                    VehicleStrategicTransitAuthority.evaluate(game, tank,
                            weakTankRoute);
            require(!weakTank.allowed(),
                    "tank should reject civilian-scale parking and alley routing");
            requireContains(weakTank.blockers(), "deployment order",
                    "restricted deployment blocker");
            requireContains(weakTank.blockers(), "cannot rely on an alley",
                    "heavy alley blocker");
            requireContains(weakTank.blockers(), "depot or vehicle yard",
                    "tank endpoint blocker");
            requireContains(weakTank.blockers(), "parking capacity",
                    "tank footprint blocker");

            game.inventory.add("Motor pool command");
            game.inventory.add("Civic Wardens transit permit");
            VehicleStrategicTransitAuthority.Request controlledTankRoute =
                    new VehicleStrategicTransitAuthority.Request(
                            "sector-4/vehicle-yard",
                            Set.of(VehicleStrategicTransitAuthority.Infrastructure.ROAD,
                                    VehicleStrategicTransitAuthority.Infrastructure.VEHICLE_LANE,
                                    VehicleStrategicTransitAuthority.Infrastructure.GATE,
                                    VehicleStrategicTransitAuthority.Infrastructure.CHECKPOINT,
                                    VehicleStrategicTransitAuthority.Infrastructure.VEHICLE_YARD),
                            10, 4, 18, true, 20, 5,
                            true, true, false, Faction.CIVIC_WARDENS,
                            "armored redeployment");
            VehicleStrategicTransitAuthority.Readiness controlledTank =
                    VehicleStrategicTransitAuthority.evaluate(game, tank,
                            controlledTankRoute);
            require(controlledTank.allowed(),
                    "commanded, permitted tank route with yard endpoint should be ready: "
                            + controlledTank.summary());

            VehicleStrategicTransitAuthority.Request closedSecurity =
                    new VehicleStrategicTransitAuthority.Request(
                            "sector-4/vehicle-yard",
                            controlledTankRoute.infrastructure(), 10, 4,
                            18, true, 20, 5, true, true, true,
                            Faction.CIVIC_WARDENS, "security lockdown");
            require(!VehicleStrategicTransitAuthority.evaluate(game, tank,
                            closedSecurity).allowed(),
                    "active security closure must block an otherwise valid route");

            VehicleRuntimeAuthority.applyDamage(tank,
                    VehicleRuntimeAuthority.Component.POWERPLANT, 100,
                    1_201, "strategic transit smoke engine loss");
            VehicleLossAuthority.resolve(game, tank,
                    VehicleLossAuthority.Cause.COMPONENT_FAILURE,
                    Faction.NONE, "engine failed before departure");
            VehicleStrategicTransitAuthority.Readiness hazardBlocked =
                    VehicleStrategicTransitAuthority.evaluate(game, tank,
                            controlledTankRoute);
            require(!hazardBlocked.allowed(),
                    "disabled leaking vehicle must not depart strategically");
            requireContains(hazardBlocked.blockers(), "obstruction or leak hazard",
                    "active loss-hazard blocker");

            for (String line : VehicleStrategicTransitAuthority.inspectionLines(
                    game, car, readyRequest)) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "strategic vehicle transit inspection leaked implementation text: "
                                + line);
            }

            System.out.println("Milestone 06 strategic vehicle transit smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
        Milestone06VehicleStrategicTransitCommitSmoke.main(args);
    }

    private static World world() {
        World world = new World(61027L, 18, 12);
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
                                          String type,
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
                world, x, y, profile, new Random(seed),
                "VEHICLE-STRATEGIC-TRANSIT-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle,
                faction == null ? Faction.NONE : faction,
                ownership, "strategic-transit-smoke", false,
                new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", ownerType.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", (faction == null ? Faction.NONE : faction).name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", ownerType == VehicleRuntimeAuthority.OwnerType.PLAYER
                        ? "Player" : faction == null || faction == Faction.NONE
                        ? ownerType.name().toLowerCase() : faction.label);
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06VehicleStrategicTransitSmoke() { }
}
