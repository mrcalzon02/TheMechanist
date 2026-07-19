package mechanist;

import java.util.List;
import java.util.Random;

/** Focused smoke for resolved-world faction route transfer and plan finalization. */
final class Milestone06FactionVehicleRouteTransitCommitSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            World source = world(61042L, ZoneType.MECHANICUS_FORGE_CLOISTER,
                    7, 1, 1, 1, 0);
            World destination = world(61043L, ZoneType.NEUTRAL_CIVILIAN_FLOOR,
                    7, 1, 2, 1, 0);
            World wrongDestination = world(61044L,
                    ZoneType.NEUTRAL_CIVILIAN_FLOOR,
                    7, 1, 3, 1, 0);
            game.world = source;
            game.turn = 1_700;
            game.worldTurn = 1_700L;
            game.npcFactionSites.clear();
            game.factionStrategicPlans.clear();
            game.inventory.clear();
            game.carriedScript = 920;
            game.supplies = 15;
            game.machineParts = 11;
            game.gangHeat = 3;
            game.suspicion = 4;

            NpcFactionSite site = NpcFactionSite.create(
                    "Mechanist Resolved Transit Yard",
                    Faction.MECHANIST_COLLEGIA, "motor pool",
                    source.sectorX, source.sectorY,
                    source.zoneX, source.zoneY, source.floor,
                    "Machine part", "Tool bundle",
                    "Resolved Route Transit Doctrine");
            site.stock = 90;
            site.workers = 10;
            site.baseLevel = 3;
            site.machineLevel = 4;
            game.npcFactionSites.add(site);

            MapObjectState armored = vehicle(source, 6, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR,
                    site.faction, "checkpoint reinforcement", 601L);
            MapObjectState cargo = vehicle(source, 10, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    site.faction, "freight reserve", 602L);
            staff(armored, "Transit Commander Kest", 9);
            staff(cargo, "Transit Driver Vale", 6);
            source.mapObjects.add(armored);
            source.mapObjects.add(cargo);
            require(VehicleMotorPoolAuthority.assign(game, armored, site,
                    "checkpoint reinforcement",
                    "resolved transfer smoke registration").success()
                            && VehicleMotorPoolAuthority.assign(game, cargo,
                            site, "freight reserve",
                            "resolved transfer smoke registration").success(),
                    "resolved transit yard should register both vehicles");

            int scriptBefore = game.carriedScript;
            int suppliesBefore = game.supplies;
            int partsBefore = game.machineParts;
            int heatBefore = game.gangHeat;
            int suspicionBefore = game.suspicion;
            List<String> inventoryBefore = List.copyOf(game.inventory);

            FactionStrategicPlan plan = new FactionStrategicPlan();
            plan.id = "STRAT-ROUTE-RESOLVED-COMMIT";
            plan.faction = Faction.MECHANICUS;
            plan.schemeTargetFaction = Faction.NONE;
            plan.phase = "PLANNING";
            plan.immediateGoal = "stockpile a strategic item";
            plan.scheme = "reinforce the destination checkpoint gate";
            plan.targetRoom = destination.locationKey();
            plan.targetItem = "Fuel reserve";
            plan.secrecy = 60;
            plan.aggression = 55;
            plan.ambition = 65;
            plan.phaseUntilTurn = game.turn;
            plan.nextDecisionTurn = game.turn;
            game.factionStrategicPlans.add(plan);

            int stockBeforeStage = site.stock;
            int stagedResolved = FactionStrategicAssetTickAuthority.tick(game);
            MapObjectState staged = vehicleForPlan(source, plan.id);
            require(stagedResolved == 0
                            && staged == armored
                            && "EXECUTION".equals(plan.phase)
                            && plan.success == 0 && plan.failure == 0
                            && "active".equals(
                            FactionVehicleRouteControlAuthority.inspect(
                            staged).state())
                            && "reserved".equals(value(staged,
                            "strategicTransitState"))
                            && destination.locationKey().equals(value(staged,
                            "strategicTransitDestination"))
                            && site.stock < stockBeforeStage,
                    "planning should stage the armored vehicle for the exact resolved destination");
            int stockAfterStage = site.stock;
            int fuelBeforeCommit = VehicleFuelAuthority.inspect(
                    source, staged).current();
            int reservedFuel = Integer.parseInt(value(staged,
                    "strategicTransitFuelReserved"));
            String stagedStock = staged.stockState;

            FactionVehicleRouteTransitCommitAuthority.Result wrong =
                    FactionVehicleRouteTransitCommitAuthority.commit(
                            game, plan, site, staged,
                            new VehicleStrategicTransitCommitAuthority.TransferRequest(
                                    source, wrongDestination, 14, 5,
                                    "mismatched resolved destination"));
            require(!wrong.success()
                            && wrong.status()
                            == FactionVehicleRouteTransitCommitAuthority.Status.BLOCKED
                            && game.world == source
                            && source.mapObjects.contains(staged)
                            && !wrongDestination.mapObjects.contains(staged)
                            && stagedStock.equals(staged.stockState)
                            && site.stock == stockAfterStage
                            && plan.success == 0 && plan.failure == 0
                            && "EXECUTION".equals(plan.phase),
                    "mismatched resolved destination must preserve the pending plan and vehicle ledger");

            FactionVehicleRouteTransitCommitAuthority.Result badParking =
                    FactionVehicleRouteTransitCommitAuthority.commit(
                            game, plan, site, staged,
                            new VehicleStrategicTransitCommitAuthority.TransferRequest(
                                    source, destination, 8, 8,
                                    "illegal destination parking"));
            require(!badParking.success()
                            && badParking.status()
                            == FactionVehicleRouteTransitCommitAuthority.Status.BLOCKED
                            && game.world == source
                            && source.mapObjects.contains(staged)
                            && !destination.mapObjects.contains(staged)
                            && stagedStock.equals(staged.stockState)
                            && VehicleFuelAuthority.inspect(source,
                            staged).current() == fuelBeforeCommit
                            && site.stock == stockAfterStage
                            && plan.success == 0 && plan.failure == 0,
                    "illegal parking must leave source placement, fuel, reservation, and strategic counters untouched");

            String cargoBeforeForeignCommit = cargo.stockState;
            FactionVehicleRouteTransitCommitAuthority.Result wrongVehicle =
                    FactionVehicleRouteTransitCommitAuthority.commit(
                            game, plan, site, cargo,
                            new VehicleStrategicTransitCommitAuthority.TransferRequest(
                                    source, destination, 14, 5,
                                    "wrong bound vehicle"));
            require(!wrongVehicle.success()
                            && cargoBeforeForeignCommit.equals(cargo.stockState)
                            && source.mapObjects.contains(cargo)
                            && plan.success == 0 && plan.failure == 0,
                    "an unbound fleet vehicle cannot commit another plan's route transfer");

            FactionVehicleRouteTransitCommitAuthority.Result committed =
                    FactionVehicleRouteTransitCommitAuthority.commit(
                            game, plan, site, staged,
                            new VehicleStrategicTransitCommitAuthority.TransferRequest(
                                    source, destination, 14, 5,
                                    "resolved checkpoint reinforcement"));
            FactionVehicleRouteControlAuthority.Snapshot completedOrder =
                    FactionVehicleRouteControlAuthority.inspect(staged);
            require(committed.success()
                            && committed.status()
                            == FactionVehicleRouteTransitCommitAuthority.Status.COMMITTED
                            && committed.finalized()
                            && committed.resolvedPlans() == 1
                            && committed.transfer() != null
                            && committed.transfer().success()
                            && committed.transfer().status()
                            == VehicleStrategicTransitCommitAuthority.Status.COMMITTED
                            && committed.progress() != null
                            && committed.progress().terminal()
                            && committed.progress().success()
                            && game.world == source
                            && !source.mapObjects.contains(staged)
                            && destination.mapObjects.contains(staged)
                            && staged.x == 14 && staged.y == 5
                            && VehicleFuelAuthority.inspect(destination,
                            staged).current() == fuelBeforeCommit - reservedFuel
                            && "completed".equals(value(staged,
                            "strategicTransitState"))
                            && "completed".equals(value(staged,
                            "routeControlStrategicState"))
                            && "completed".equals(completedOrder.state())
                            && !completedOrder.assigned()
                            && plan.success == 1 && plan.failure == 0
                            && "COOLDOWN".equals(plan.phase)
                            && site.stock == stockAfterStage,
                    "valid resolved transfer must move the same fixture, consume fuel once, complete the order, and close the plan");
            requireContains(plan.lastOutcome,
                    "completed checkpoint reinforcement",
                    "resolved route completion outcome");
            requireContains(value(staged, "deploymentHistory"),
                    "Strategic transfer", "resolved transfer provenance");

            String completedStock = staged.stockState;
            int completedFuel = VehicleFuelAuthority.inspect(
                    destination, staged).current();
            int successAfterCommit = plan.success;
            FactionVehicleRouteTransitCommitAuthority.Result duplicate =
                    FactionVehicleRouteTransitCommitAuthority.commit(
                            game, plan, site, staged,
                            new VehicleStrategicTransitCommitAuthority.TransferRequest(
                                    source, destination, 14, 5,
                                    "duplicate resolved commit"));
            require(!duplicate.success()
                            && duplicate.status()
                            == FactionVehicleRouteTransitCommitAuthority.Status.BLOCKED
                            && completedStock.equals(staged.stockState)
                            && VehicleFuelAuthority.inspect(destination,
                            staged).current() == completedFuel
                            && !source.mapObjects.contains(staged)
                            && destination.mapObjects.contains(staged)
                            && plan.success == successAfterCommit
                            && plan.failure == 0
                            && site.stock == stockAfterStage,
                    "completed plan must reject duplicate transfer without double fuel, stock, or success");

            require(game.carriedScript == scriptBefore
                            && game.supplies == suppliesBefore
                            && game.machineParts == partsBefore
                            && game.gangHeat == heatBefore
                            && game.suspicion == suspicionBefore
                            && inventoryBefore.equals(game.inventory),
                    "background resolved route transfer must not mutate player resources or attention");
            require(!PlayerFacingText.containsLikelyLeak(plan.lastOutcome)
                            && !PlayerFacingText.containsLikelyLeak(
                            committed.message())
                            && !PlayerFacingText.containsLikelyLeak(
                            wrong.message())
                            && !PlayerFacingText.containsLikelyLeak(
                            badParking.message()),
                    "resolved route transfer outcomes should remain player-facing and implementation-safe");

            System.out.println("Milestone 06 faction vehicle route transit commit smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World world(long seed, ZoneType type,
                               int sectorX, int sectorY,
                               int zoneX, int zoneY, int floor) {
        World world = new World(seed, 28, 14);
        world.zoneType = type;
        world.sectorX = sectorX;
        world.sectorY = sectorY;
        world.zoneX = zoneX;
        world.zoneY = zoneY;
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
                "VEHICLE-ROUTE-TRANSIT-COMMIT-SMOKE");
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
            crew.append("Resolved Transit Crew ").append(i);
        }
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "vehicleCrewManifest", crew.toString());
    }

    private static MapObjectState vehicleForPlan(World world,
                                                  String planId) {
        if (world == null || world.mapObjects == null) return null;
        for (MapObjectState vehicle : world.mapObjects) {
            if (VehicleRuntimeAuthority.isVehicle(vehicle)
                    && planId.equals(value(vehicle,
                    "routeControlStrategicPlanId"))) return vehicle;
        }
        return null;
    }

    private static String value(MapObjectState vehicle, String key) {
        return vehicle == null ? ""
                : MapObjectState.stockValue(vehicle.stockState, key);
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

    private Milestone06FactionVehicleRouteTransitCommitSmoke() { }
}
