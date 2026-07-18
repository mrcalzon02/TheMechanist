package mechanist;

import java.util.List;
import java.util.Random;
import java.util.Set;

/** Focused smoke for faction route-control order selection, handoff, and lifecycle. */
final class Milestone06FactionVehicleRouteControlSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.turn = 1_580;
            game.worldTurn = 1_580L;
            game.npcFactionSites.clear();
            game.factionStrategicPlans.clear();
            game.inventory.clear();
            game.carriedScript = 740;
            game.gangHeat = 2;
            game.suspicion = 3;

            NpcFactionSite mechanist = site("Mechanist Route Yard",
                    Faction.MECHANIST_COLLEGIA, game.world);
            mechanist.stock = 100;
            mechanist.workers = 8;
            mechanist.baseLevel = 3;
            mechanist.machineLevel = 4;
            game.npcFactionSites.add(mechanist);

            MapObjectState cargo = vehicle(game.world, 4, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    mechanist.faction, "production freight convoy", 401L);
            MapObjectState armored = vehicle(game.world, 7, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR,
                    mechanist.faction, "checkpoint security patrol", 402L);
            MapObjectState guardTank = vehicle(game.world, 12, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK,
                    Faction.IMPERIAL_GUARD, "armored route defense", 403L);
            staff(cargo, "Driver Lume", 6);
            staff(armored, "Warden-Driver Kest", 9);
            staff(guardTank, "Commander Holt", 17);
            game.world.mapObjects.add(cargo);
            game.world.mapObjects.add(armored);
            game.world.mapObjects.add(guardTank);
            require(VehicleMotorPoolAuthority.assign(game, cargo, mechanist,
                    "production freight convoy",
                    "route-control smoke registration").success()
                            && VehicleMotorPoolAuthority.assign(game, armored,
                            mechanist, "checkpoint security patrol",
                            "route-control smoke registration").success(),
                    "mechanist route yard should register both fleet candidates");

            FactionVehicleBalanceAuthority.Contest beforeOrder =
                    FactionVehicleBalanceAuthority.compare(game,
                            mechanist.faction, Faction.IMPERIAL_GUARD,
                            mechanist);
            require(beforeOrder.attackerRouteCommitment() == 0,
                    "fleet balance should begin without committed route orders");

            FactionVehicleRouteControlAuthority.Request patrol = request(
                    FactionVehicleRouteControlAuthority.Mission.PATROL,
                    "sector-5/east-checkpoint", Faction.NONE,
                    18, 4, 0, 0, "stabilize the east checkpoint");
            FactionVehicleRouteControlAuthority.Result assigned =
                    FactionVehicleRouteControlAuthority.assign(game,
                            mechanist, patrol);
            require(assigned.success() && assigned.changed()
                            && assigned.vehicle() == armored
                            && assigned.snapshot().assigned()
                            && assigned.snapshot().mission()
                            == FactionVehicleRouteControlAuthority.Mission.PATROL
                            && assigned.snapshot().destinationKey().equals(
                            patrol.destinationKey())
                            && assigned.snapshot().strength() > 0,
                    "patrol doctrine should select the armored car over the cargo truck");
            requireContains(MapObjectState.stockValue(armored.stockState,
                            "deploymentHistory"),
                    "Route-control order", "route-control deployment history");

            FactionVehicleBalanceAuthority.Contest assignedBalance =
                    FactionVehicleBalanceAuthority.compare(game,
                            mechanist.faction, Faction.IMPERIAL_GUARD,
                            mechanist);
            require(assignedBalance.attackerRouteCommitment() > 0
                            && assignedBalance.attackerPower()
                            > beforeOrder.attackerPower()
                            && assignedBalance.routeControlDelta()
                            > beforeOrder.routeControlDelta(),
                    "assigned physical patrol order should immediately increase route-control balance");

            String armoredAfterAssignment = armored.stockState;
            FactionVehicleRouteControlAuthority.Result duplicate =
                    FactionVehicleRouteControlAuthority.assign(game,
                            mechanist, patrol);
            require(duplicate.success() && !duplicate.changed()
                            && duplicate.vehicle() == armored
                            && armoredAfterAssignment.equals(armored.stockState),
                    "identical route-control assignment should be idempotent");

            staff(cargo, "", 0);
            String cargoBeforeBlocked = cargo.stockState;
            String armoredBeforeBlocked = armored.stockState;
            int scriptBeforeBlocked = game.carriedScript;
            int heatBeforeBlocked = game.gangHeat;
            int suspicionBeforeBlocked = game.suspicion;
            FactionVehicleRouteControlAuthority.Request checkpoint = request(
                    FactionVehicleRouteControlAuthority.Mission.CHECKPOINT_REINFORCEMENT,
                    "sector-5/north-gate", Faction.NONE,
                    12, 4, 0, 0, "reinforce the north gate");
            FactionVehicleRouteControlAuthority.Result blockedCandidate =
                    FactionVehicleRouteControlAuthority.assign(game,
                            mechanist, checkpoint);
            require(!blockedCandidate.success()
                            && cargoBeforeBlocked.equals(cargo.stockState)
                            && armoredBeforeBlocked.equals(armored.stockState)
                            && game.carriedScript == scriptBeforeBlocked
                            && game.gangHeat == heatBeforeBlocked
                            && game.suspicion == suspicionBeforeBlocked,
                    "under-crewed reserve plus already-assigned patrol must block a second order without mutation");

            VehicleStrategicTransitAuthority.Request transit =
                    FactionVehicleRouteControlAuthority.transitRequest(
                            game, armored, patrol);
            require(transit.destinationKey().equals(patrol.destinationKey())
                            && transit.assignedCrew() == 10
                            && transit.driverAssigned()
                            && transit.fuelOrPowerAvailable()
                            >= transit.fuelOrPowerRequired()
                            && transit.fuelOrPowerRequired()
                            == assigned.snapshot().requiredFuel(),
                    "route-control order should produce an exact strategic-transit handoff request");

            String beforePrematureActivation = armored.stockState;
            FactionVehicleRouteControlAuthority.Result premature =
                    FactionVehicleRouteControlAuthority.activate(game,
                            mechanist, armored);
            require(!premature.success()
                            && beforePrematureActivation.equals(
                            armored.stockState),
                    "route-control activation must wait for the matching transit reservation");

            markTransitReserved(armored, patrol.destinationKey(),
                    assigned.snapshot().requiredFuel());
            FactionVehicleRouteControlAuthority.Result activated =
                    FactionVehicleRouteControlAuthority.activate(game,
                            mechanist, armored);
            require(activated.success() && activated.changed()
                            && "active".equals(activated.snapshot().state()),
                    "matching strategic-transit reservation should activate the deployment order");
            FactionVehicleBalanceAuthority.Contest activeBalance =
                    FactionVehicleBalanceAuthority.compare(game,
                            mechanist.faction, Faction.IMPERIAL_GUARD,
                            mechanist);
            require(activeBalance.attackerRouteCommitment()
                            > assignedBalance.attackerRouteCommitment()
                            && activeBalance.attackerPower()
                            > assignedBalance.attackerPower(),
                    "active route deployment should contribute more than a staged assignment");

            String beforeReservedCancel = armored.stockState;
            FactionVehicleRouteControlAuthority.Result reservedCancel =
                    FactionVehicleRouteControlAuthority.cancel(game,
                            mechanist, armored,
                            "invalid cancellation before transit closure");
            require(!reservedCancel.success()
                            && beforeReservedCancel.equals(armored.stockState),
                    "active transit reservation must block route-order cancellation without mutation");
            armored.stockState = MapObjectState.setStockFlag(
                    armored.stockState, "strategicTransitState", "cancelled");
            armored.stockState = MapObjectState.setStockFlag(
                    armored.stockState, "strategicTransitFuelReserved", "0");
            FactionVehicleRouteControlAuthority.Result cancelled =
                    FactionVehicleRouteControlAuthority.cancel(game,
                            mechanist, armored,
                            "checkpoint patrol stood down");
            require(cancelled.success() && cancelled.changed()
                            && "cancelled".equals(cancelled.snapshot().state())
                            && !cancelled.snapshot().assigned(),
                    "closed transit should allow the local motor pool to cancel the deployment order");
            FactionVehicleBalanceAuthority.Contest afterCancel =
                    FactionVehicleBalanceAuthority.compare(game,
                            mechanist.faction, Faction.IMPERIAL_GUARD,
                            mechanist);
            require(afterCancel.attackerRouteCommitment() == 0
                            && afterCancel.attackerPower()
                            < activeBalance.attackerPower(),
                    "cancelled deployment should stop contributing route-control strength");

            FactionVehicleRouteControlAuthority.Request lowContest = request(
                    FactionVehicleRouteControlAuthority.Mission.ROUTE_CONTEST,
                    "sector-5/guard-controlled-route",
                    Faction.IMPERIAL_GUARD, 20, 4, 10, 10,
                    "contest guard control of the freight route");
            String armoredBeforeDeterrence = armored.stockState;
            String cargoBeforeDeterrence = cargo.stockState;
            FactionVehicleRouteControlAuthority.Result deterred =
                    FactionVehicleRouteControlAuthority.assign(game,
                            mechanist, lowContest);
            require(!deterred.success()
                            && armoredBeforeDeterrence.equals(armored.stockState)
                            && cargoBeforeDeterrence.equals(cargo.stockState),
                    "low-commitment contested route order should respect fleet deterrence without mutation");

            FactionVehicleRouteControlAuthority.Request committedContest =
                    request(FactionVehicleRouteControlAuthority.Mission.ROUTE_CONTEST,
                            "sector-5/guard-controlled-route",
                            Faction.IMPERIAL_GUARD, 20, 4, 100, 100,
                            "commit armored force to the freight route");
            FactionVehicleRouteControlAuthority.Result contest =
                    FactionVehicleRouteControlAuthority.assign(game,
                            mechanist, committedContest);
            require(contest.success() && contest.changed()
                            && contest.vehicle() == armored
                            && contest.snapshot().targetFaction()
                            == Faction.IMPERIAL_GUARD
                            && contest.snapshot().mission()
                            == FactionVehicleRouteControlAuthority.Mission.ROUTE_CONTEST,
                    "high-commitment route contest should reserve the best available armored vehicle");
            FactionVehicleRouteControlAuthority.Result completed =
                    FactionVehicleRouteControlAuthority.complete(game,
                            mechanist, armored,
                            "freight route pressure established");
            require(completed.success() && completed.changed()
                            && "completed".equals(completed.snapshot().state())
                            && !completed.snapshot().history().isEmpty(),
                    "completed route contest should preserve its lifecycle history and release strategic contribution");

            List<String> inspection =
                    FactionVehicleRouteControlAuthority.inspectionLines(armored);
            requireContains(inspection, "contested route operation",
                    "route-control mission inspection");
            requireContains(inspection, "completed",
                    "route-control state inspection");
            for (String line : inspection) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "route-control inspection leaked implementation text: "
                                + line);
            }

            System.out.println("Milestone 06 faction vehicle route-control smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static FactionVehicleRouteControlAuthority.Request request(
            FactionVehicleRouteControlAuthority.Mission mission,
            String destination, Faction target, int distance,
            int parking, int aggression, int ambition, String reason) {
        return new FactionVehicleRouteControlAuthority.Request(
                mission, destination, target,
                Set.of(VehicleStrategicTransitAuthority.Infrastructure.ROAD,
                        VehicleStrategicTransitAuthority.Infrastructure.VEHICLE_LANE,
                        VehicleStrategicTransitAuthority.Infrastructure.GATE,
                        VehicleStrategicTransitAuthority.Infrastructure.CHECKPOINT,
                        VehicleStrategicTransitAuthority.Infrastructure.PARKING_LOT,
                        VehicleStrategicTransitAuthority.Infrastructure.VEHICLE_YARD),
                distance, parking, true, true, false,
                aggression, ambition, reason);
    }

    private static void markTransitReserved(MapObjectState vehicle,
                                            String destination,
                                            int requiredFuel) {
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitState", "reserved");
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitReservationId", "ROUTE-SMOKE-RESERVATION");
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitDestination", destination);
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "strategicTransitFuelReserved", Integer.toString(requiredFuel));
    }

    private static NpcFactionSite site(String name, Faction faction,
                                       World world) {
        NpcFactionSite site = NpcFactionSite.create(name, faction,
                "motor pool", world.sectorX, world.sectorY,
                world.zoneX, world.zoneY, world.floor,
                "Machine part", "Tool bundle", "Route Control Doctrine");
        site.stock = 60;
        site.workers = 4;
        site.baseLevel = 2;
        site.machineLevel = 2;
        return site;
    }

    private static World world() {
        World world = new World(61040L, 24, 14);
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.sectorX = 5;
        world.sectorY = 1;
        world.zoneX = 3;
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
                                          String type, Faction faction,
                                          String role, long seed) {
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "VEHICLE-ROUTE-CONTROL-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, faction,
                "faction", role, false, new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", VehicleRuntimeAuthority.OwnerType.FACTION.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", faction.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", faction.label);
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", "faction");
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "vehicleRole", role);
        VehicleFuelAuthority.ensureInitialized(world, vehicle);
        return vehicle;
    }

    private static void staff(MapObjectState vehicle, String driver,
                              int supportCrew) {
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "vehicleDriver", driver);
        StringBuilder crew = new StringBuilder();
        for (int i = 1; i <= supportCrew; i++) {
            if (crew.length() > 0) crew.append('~');
            crew.append("Route Crew ").append(i);
        }
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "vehicleCrewManifest", crew.toString());
    }

    private static void requireContains(List<String> lines, String expected,
                                        String label) {
        for (String line : lines) {
            if (line != null && line.toLowerCase().contains(
                    expected.toLowerCase())) return;
        }
        throw new AssertionError("Expected " + label + " to contain '"
                + expected + "': " + lines);
    }

    private static void requireContains(String text, String expected,
                                        String label) {
        if (text != null && text.toLowerCase().contains(
                expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '"
                + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06FactionVehicleRouteControlSmoke() { }
}
