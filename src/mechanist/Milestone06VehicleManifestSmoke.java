package mechanist;

import java.util.List;
import java.util.Random;
import java.util.Set;

/** Focused smoke for persistent vehicle driver, crew, passenger, cargo, and route-manifest integration. */
final class Milestone06VehicleManifestSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.turn = 1_340;
            game.worldTurn = 1_340L;
            game.inventory.clear();

            MapObjectState truck = vehicle(game.world, 5, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    VehicleRuntimeAuthority.OwnerType.PLAYER,
                    "player-owned", 141L);
            game.world.mapObjects.add(truck);
            VehicleFuelAuthority.ensureInitialized(game.world, truck);

            VehicleManifestAuthority.Result driver =
                    VehicleManifestAuthority.assignDriver(game, truck,
                            "Driver Mara Venn");
            require(driver.success() && driver.changed()
                            && "Driver Mara Venn".equals(
                            driver.snapshot().driver()),
                    "player-owned truck should accept a named driver");
            for (int i = 1; i <= 6; i++) {
                VehicleManifestAuthority.Result crew =
                        VehicleManifestAuthority.addCrew(game, truck,
                                "Haul Crew " + i);
                require(crew.success() && crew.changed(),
                        "truck should accept operational crew member " + i);
            }
            VehicleManifestAuthority.Snapshot staffed =
                    VehicleManifestAuthority.inspect(game.world, truck);
            require(staffed.assignedCrew() == 7
                            && staffed.crew().size() == 6,
                    "driver plus six support crew should satisfy the truck's seven-person operational requirement");
            VehicleManifestAuthority.Result duplicateCrew =
                    VehicleManifestAuthority.addCrew(game, truck,
                            "Haul Crew 1");
            require(duplicateCrew.success() && !duplicateCrew.changed()
                            && VehicleManifestAuthority.inspect(game.world,
                            truck).assignedCrew() == 7,
                    "duplicate crew assignment should be idempotent");

            require(VehicleManifestAuthority.boardPassenger(game, truck,
                    "Passenger Ilya").success(),
                    "first passenger should board the truck");
            require(VehicleManifestAuthority.boardPassenger(game, truck,
                    "Passenger Sol").success(),
                    "second passenger should board the truck");
            VehicleManifestAuthority.Result fullSeats =
                    VehicleManifestAuthority.boardPassenger(game, truck,
                            "Passenger Third");
            require(!fullSeats.success()
                            && fullSeats.snapshot().occupiedSeats() == 3
                            && fullSeats.snapshot().seatCapacity() == 3,
                    "driver plus two passengers should fill the three-seat cab");

            require(VehicleManifestAuthority.registerCargo(game, truck,
                    "Machine components", "Mechanist Collegia", 10).success(),
                    "first cargo custody record should fit");
            require(VehicleManifestAuthority.registerCargo(game, truck,
                    "Medical reserve", "Civic Clinic", 8).success(),
                    "second cargo custody record should fill remaining capacity");
            VehicleManifestAuthority.Snapshot loaded =
                    VehicleManifestAuthority.inspect(game.world, truck);
            require(loaded.cargoUnits() == 18
                            && loaded.cargoCapacity() == 18
                            && loaded.cargo().size() == 2,
                    "truck cargo manifest should preserve labels, owners, and exact capacity use");
            String cargoBeforeOverflow = truck.stockState;
            VehicleManifestAuthority.Result cargoOverflow =
                    VehicleManifestAuthority.registerCargo(game, truck,
                            "Overflow crate", "Unassigned", 1);
            require(!cargoOverflow.success()
                            && cargoBeforeOverflow.equals(truck.stockState),
                    "cargo overflow must leave the persistent manifest untouched");

            VehicleStrategicTransitAuthority.Request request =
                    VehicleManifestAuthority.strategicRequest(game, truck,
                            "sector-3/zone-2/floor-0",
                            Set.of(VehicleStrategicTransitAuthority.Infrastructure.ROAD,
                                    VehicleStrategicTransitAuthority.Infrastructure.GATE,
                                    VehicleStrategicTransitAuthority.Infrastructure.PARKING_LOT),
                            12, 3, 3, true, true, false,
                            Faction.NONE, "manifest-backed freight route");
            require(request.driverAssigned()
                            && request.assignedCrew() == 7
                            && request.fuelOrPowerAvailable()
                            == VehicleFuelAuthority.inspect(game.world,
                            truck).current(),
                    "strategic request should derive driver, crew, and available energy from persistent ledgers");
            VehicleStrategicTransitAuthority.Readiness readiness =
                    VehicleStrategicTransitAuthority.evaluate(game, truck,
                            request);
            require(readiness.allowed()
                            && readiness.crewRequired() == 7,
                    "fully staffed manifest-backed truck route should be ready: "
                            + readiness.summary());

            VehicleManifestAuthority.Result partialUnload =
                    VehicleManifestAuthority.unloadCargo(game, truck,
                            "Machine components", "Mechanist Collegia", 3,
                            "three units delivered to the destination workshop");
            require(partialUnload.success() && partialUnload.changed()
                            && partialUnload.snapshot().cargoUnits() == 15
                            && cargoUnits(partialUnload.snapshot(),
                            "Machine components", "Mechanist Collegia") == 7,
                    "partial unloading should preserve the remaining custody record");
            VehicleManifestAuthority.Result mergedReload =
                    VehicleManifestAuthority.registerCargo(game, truck,
                            "Machine components", "Mechanist Collegia", 2);
            require(mergedReload.success() && mergedReload.changed()
                            && mergedReload.snapshot().cargoUnits() == 17
                            && mergedReload.snapshot().cargo().size() == 2
                            && cargoUnits(mergedReload.snapshot(),
                            "Machine components", "Mechanist Collegia") == 9,
                    "matching cargo owner and label should merge into one canonical custody row");
            String beforeOverUnload = truck.stockState;
            VehicleManifestAuthority.Result overUnload =
                    VehicleManifestAuthority.unloadCargo(game, truck,
                            "Medical reserve", "Civic Clinic", 9,
                            "invalid over-unload attempt");
            require(!overUnload.success()
                            && beforeOverUnload.equals(truck.stockState),
                    "over-unloading must fail without changing the persistent manifest");

            VehicleManifestAuthority.Result disembarked =
                    VehicleManifestAuthority.disembarkPassenger(game, truck,
                            "Passenger Sol", "destination reached");
            require(disembarked.success() && disembarked.changed()
                            && disembarked.snapshot().occupiedSeats() == 2
                            && !disembarked.snapshot().passengers().contains(
                            "Passenger Sol"),
                    "named passenger disembark should free one seat");
            String beforeMissingPassenger = truck.stockState;
            VehicleManifestAuthority.Result missingPassenger =
                    VehicleManifestAuthority.disembarkPassenger(game, truck,
                            "Passenger Missing", "not aboard");
            require(missingPassenger.success() && !missingPassenger.changed()
                            && beforeMissingPassenger.equals(truck.stockState),
                    "releasing a passenger who is not aboard should be idempotent");
            require(VehicleManifestAuthority.boardPassenger(game, truck,
                    "Passenger Sol").success(),
                    "released passenger should be able to board again when a seat is free");

            VehicleManifestAuthority.Result crewReleased =
                    VehicleManifestAuthority.releaseCrew(game, truck,
                            "Haul Crew 6", "shift completed");
            require(crewReleased.success() && crewReleased.changed()
                            && crewReleased.snapshot().assignedCrew() == 6,
                    "named crew release should reduce the strategic crew count");
            VehicleStrategicTransitAuthority.Request underCrewRequest =
                    VehicleManifestAuthority.strategicRequest(game, truck,
                            "sector-3/zone-2/floor-0", request.infrastructure(),
                            12, 3, 3, true, true, false, Faction.NONE,
                            "under-crewed freight route");
            require(!VehicleStrategicTransitAuthority.evaluate(game, truck,
                            underCrewRequest).allowed(),
                    "releasing required crew should immediately block strategic departure");
            require(VehicleManifestAuthority.addCrew(game, truck,
                    "Haul Crew 6").success(),
                    "released crew member should be assignable again");

            VehicleManifestAuthority.Result driverReleased =
                    VehicleManifestAuthority.releaseDriver(game, truck,
                            "driver shift completed");
            require(driverReleased.success() && driverReleased.changed()
                            && driverReleased.snapshot().driver().isBlank(),
                    "driver release should clear the authoritative driver slot");
            VehicleStrategicTransitAuthority.Request noDriverRequest =
                    VehicleManifestAuthority.strategicRequest(game, truck,
                            "sector-3/zone-2/floor-0", request.infrastructure(),
                            12, 3, 3, true, true, false, Faction.NONE,
                            "driverless freight route");
            require(!VehicleStrategicTransitAuthority.evaluate(game, truck,
                            noDriverRequest).allowed(),
                    "a released driver should immediately block strategic departure");
            require(VehicleManifestAuthority.assignDriver(game, truck,
                    "Driver Mara Venn").success(),
                    "released driver should be assignable again");

            requireContains(MapObjectState.stockValue(truck.stockState,
                            "crewHistory"), "Crew released: Haul Crew 6",
                    "crew release history");
            requireContains(MapObjectState.stockValue(truck.stockState,
                            "crewHistory"), "Driver released: Driver Mara Venn",
                    "driver release history");
            requireContains(MapObjectState.stockValue(truck.stockState,
                            "passengerHistory"), "Passenger disembarked: Passenger Sol",
                    "passenger release history");
            requireContains(MapObjectState.stockValue(truck.stockState,
                            "cargoHistory"), "Cargo unloaded: Machine components",
                    "cargo unload history");

            List<String> inspection =
                    VehicleManifestAuthority.inspectionLines(game.world, truck);
            requireContains(inspection, "Driver Mara Venn",
                    "driver inspection");
            requireContains(inspection, "operational crew 7/7",
                    "crew readiness inspection");
            requireContains(inspection, "Passenger Ilya",
                    "passenger inspection");
            requireContains(inspection, "Mechanist Collegia",
                    "cargo owner inspection");
            requireContains(inspection, "9 unit(s)",
                    "remaining machine component count");
            requireContains(inspection, "Civic Clinic",
                    "second cargo owner inspection");
            for (String line : inspection) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "vehicle manifest inspection leaked implementation text: "
                                + line);
            }

            VehicleManifestAuthority.Result cleared =
                    VehicleManifestAuthority.clearManifest(game, truck,
                            "freight assignment completed");
            require(cleared.success() && cleared.changed()
                            && cleared.snapshot().driver().isBlank()
                            && cleared.snapshot().crew().isEmpty()
                            && cleared.snapshot().passengers().isEmpty()
                            && cleared.snapshot().cargo().isEmpty(),
                    "stand-down should clear all active assignments while preserving history");
            VehicleStrategicTransitAuthority.Request unstaffedRequest =
                    VehicleManifestAuthority.strategicRequest(game, truck,
                            "sector-3/zone-2/floor-0",
                            request.infrastructure(), 12, 3, 3,
                            true, true, false, Faction.NONE,
                            "cleared manifest route");
            VehicleStrategicTransitAuthority.Readiness unstaffed =
                    VehicleStrategicTransitAuthority.evaluate(game, truck,
                            unstaffedRequest);
            require(!unstaffed.allowed(),
                    "cleared driver and crew manifest should block departure");
            requireContains(unstaffed.blockers(), "assign a driver",
                    "cleared driver blocker");
            requireContains(unstaffed.blockers(), "assign 7 crew",
                    "cleared crew blocker");

            MapObjectState privateTruck = vehicle(game.world, 8, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    VehicleRuntimeAuthority.OwnerType.PRIVATE,
                    "private", 142L);
            game.world.mapObjects.add(privateTruck);
            String privateBefore = privateTruck.stockState;
            VehicleManifestAuthority.Result deniedDriver =
                    VehicleManifestAuthority.assignDriver(game, privateTruck,
                            "Unauthorized Driver");
            require(!deniedDriver.success()
                            && privateBefore.equals(privateTruck.stockState),
                    "private vehicle should reject driver assignment without operation authority");
            VehicleManifestAuthority.Result deniedCargo =
                    VehicleManifestAuthority.registerCargo(game, privateTruck,
                            "Unauthorized load", "Unknown", 1);
            require(!deniedCargo.success()
                            && privateBefore.equals(privateTruck.stockState),
                    "private vehicle should reject cargo registration without cargo authority");
            VehicleManifestAuthority.Result deniedRelease =
                    VehicleManifestAuthority.releaseDriver(game, privateTruck,
                            "unauthorized stand-down");
            require(!deniedRelease.success()
                            && privateBefore.equals(privateTruck.stockState),
                    "private vehicle should reject driver release without deployment authority");
            VehicleManifestAuthority.Result deniedUnload =
                    VehicleManifestAuthority.unloadCargo(game, privateTruck,
                            "Unauthorized load", "Unknown", 1,
                            "unauthorized unload");
            require(!deniedUnload.success()
                            && privateBefore.equals(privateTruck.stockState),
                    "private vehicle should reject cargo unloading without cargo authority");

            System.out.println("Milestone 06 vehicle manifest lifecycle smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static int cargoUnits(VehicleManifestAuthority.Snapshot snapshot,
                                  String label, String owner) {
        int units = 0;
        for (VehicleManifestAuthority.CargoEntry entry : snapshot.cargo()) {
            if (entry.label().equalsIgnoreCase(label)
                    && entry.owner().equalsIgnoreCase(owner)) {
                units += entry.units();
            }
        }
        return units;
    }

    private static World world() {
        World world = new World(61035L, 18, 12);
        world.zoneType = ZoneType.NEUTRAL_CIVILIAN_FLOOR;
        world.sectorX = 2;
        world.sectorY = 1;
        world.zoneX = 2;
        world.zoneY = 1;
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
                                          String ownership, long seed) {
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "VEHICLE-MANIFEST-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, Faction.NONE,
                ownership, "manifest-smoke", false, new Random(seed));
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

    private Milestone06VehicleManifestSmoke() { }
}
