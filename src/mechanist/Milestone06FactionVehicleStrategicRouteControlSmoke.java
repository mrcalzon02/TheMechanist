package mechanist;

import java.util.List;
import java.util.Random;

/** Focused smoke for faction planning promotion into transactional route control. */
final class Milestone06FactionVehicleStrategicRouteControlSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.turn = 1_640;
            game.worldTurn = 1_640L;
            game.npcFactionSites.clear();
            game.factionStrategicPlans.clear();
            game.inventory.clear();
            game.carriedScript = 880;
            game.supplies = 13;
            game.machineParts = 9;
            game.gangHeat = 4;
            game.suspicion = 5;

            NpcFactionSite mechanist = site("Mechanist Strategic Route Yard",
                    Faction.MECHANIST_COLLEGIA, game.world);
            mechanist.stock = 80;
            mechanist.workers = 10;
            mechanist.baseLevel = 3;
            mechanist.machineLevel = 4;
            game.npcFactionSites.add(mechanist);

            MapObjectState armored = vehicle(game.world, 6, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR,
                    mechanist.faction, "armored checkpoint route control", 501L);
            MapObjectState cargo = vehicle(game.world, 9, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    mechanist.faction, "freight convoy reserve", 502L);
            MapObjectState guardTank = vehicle(game.world, 14, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK,
                    Faction.IMPERIAL_GUARD, "guard route deterrent", 503L);
            staff(armored, "Strategic Driver Kest", 9);
            staff(cargo, "Strategic Driver Vale", 6);
            staff(guardTank, "Guard Commander Holt", 17);
            game.world.mapObjects.add(armored);
            game.world.mapObjects.add(cargo);
            game.world.mapObjects.add(guardTank);
            require(VehicleMotorPoolAuthority.assign(game, armored, mechanist,
                    "armored checkpoint route control",
                    "strategic route smoke registration").success()
                            && VehicleMotorPoolAuthority.assign(game, cargo,
                            mechanist, "freight convoy reserve",
                            "strategic route smoke registration").success(),
                    "strategic route yard should register both faction vehicles");

            int scriptBefore = game.carriedScript;
            int suppliesBefore = game.supplies;
            int partsBefore = game.machineParts;
            int heatBefore = game.gangHeat;
            int suspicionBefore = game.suspicion;
            List<String> inventoryBefore = List.copyOf(game.inventory);
            int stockBefore = mechanist.stock;

            FactionStrategicPlan plan = planningPlan(
                    "STRAT-ROUTE-PROMOTION",
                    "contest guard control of the eastern freight route",
                    "sector-6/east-guard-checkpoint",
                    Faction.IMPERIAL_GUARD, 100, 100, game.turn);
            game.factionStrategicPlans.add(plan);
            int resolved = FactionStrategicAssetTickAuthority.tick(game);
            FactionVehicleRouteControlAuthority.Snapshot order =
                    FactionVehicleRouteControlAuthority.inspect(armored);
            require(resolved == 1
                            && plan.success == 1 && plan.failure == 0
                            && "COOLDOWN".equals(plan.phase)
                            && FactionVehicleRouteStrategicAuthority
                            .VEHICLE_ROUTE_CONTROL_GOAL.equals(
                            plan.immediateGoal)
                            && plan.targetRoom.equals(
                            "sector-6/east-guard-checkpoint")
                            && order.assigned()
                            && "active".equals(order.state())
                            && order.mission()
                            == FactionVehicleRouteControlAuthority.Mission.ROUTE_CONTEST
                            && order.targetFaction()
                            == Faction.IMPERIAL_GUARD
                            && order.destinationKey().equals(plan.targetRoom)
                            && "reserved".equals(MapObjectState.stockValue(
                            armored.stockState, "strategicTransitState"))
                            && mechanist.stock < stockBefore,
                    "ordinary planning should promote and stage one authoritative hostile route deployment");
            requireContains(plan.lastOutcome, "route-control balance",
                    "strategic route outcome");
            requireContains(MapObjectState.stockValue(armored.stockState,
                            "deploymentHistory"),
                    "Route-control order", "strategic deployment history");

            String armoredAfterSuccess = armored.stockState;
            String cargoAfterSuccess = cargo.stockState;
            int stockAfterSuccess = mechanist.stock;
            FactionStrategicPlan duplicate = executionPlan(
                    "STRAT-ROUTE-DUPLICATE",
                    "contest guard control of the eastern freight route",
                    "sector-6/east-guard-checkpoint",
                    Faction.IMPERIAL_GUARD, 100, 100, game.turn);
            game.factionStrategicPlans.add(duplicate);
            int duplicateResolved = FactionStrategicAssetTickAuthority.tick(game);
            require(duplicateResolved == 1
                            && duplicate.success == 1
                            && duplicate.failure == 0
                            && "COOLDOWN".equals(duplicate.phase)
                            && mechanist.stock == stockAfterSuccess
                            && armoredAfterSuccess.equals(armored.stockState)
                            && cargoAfterSuccess.equals(cargo.stockState),
                    "duplicate strategic route execution should be idempotent and spend no additional stock");

            FactionStrategicPlan deterred = planningPlan(
                    "STRAT-ROUTE-DETERRED",
                    "contest guard control of the western road corridor",
                    "sector-6/west-guard-corridor",
                    Faction.IMPERIAL_GUARD, 0, 0, game.turn + 50);
            String originalGoal = deterred.immediateGoal;
            game.factionStrategicPlans.add(deterred);
            int deterredResolved = FactionStrategicAssetTickAuthority.tick(game);
            require(deterredResolved == 0
                            && "PLANNING".equals(deterred.phase)
                            && originalGoal.equals(deterred.immediateGoal)
                            && deterred.success == 0 && deterred.failure == 0,
                    "zero-commitment hostile route planning should remain unpromoted under fleet deterrence");

            staff(cargo, "", 0);
            String armoredBeforeBlocked = armored.stockState;
            String cargoBeforeBlocked = cargo.stockState;
            String guardBeforeBlocked = guardTank.stockState;
            int stockBeforeBlocked = mechanist.stock;
            FactionStrategicPlan blocked = executionPlan(
                    "STRAT-ROUTE-BLOCKED",
                    "reinforce the northern checkpoint gate",
                    "sector-6/north-checkpoint",
                    Faction.NONE, 50, 50, game.turn);
            game.factionStrategicPlans.add(blocked);
            int blockedResolved = FactionStrategicAssetTickAuthority.tick(game);
            require(blockedResolved == 1
                            && blocked.success == 0 && blocked.failure == 1
                            && "COOLDOWN".equals(blocked.phase)
                            && mechanist.stock == stockBeforeBlocked
                            && armoredBeforeBlocked.equals(armored.stockState)
                            && cargoBeforeBlocked.equals(cargo.stockState)
                            && guardBeforeBlocked.equals(guardTank.stockState),
                    "failed strategic deployment must restore all vehicle ledgers and preserve site stock");
            requireContains(blocked.lastOutcome, "no vehicle ready",
                    "blocked strategic route explanation");

            require(game.carriedScript == scriptBefore
                            && game.supplies == suppliesBefore
                            && game.machineParts == partsBefore
                            && game.gangHeat == heatBefore
                            && game.suspicion == suspicionBefore
                            && inventoryBefore.equals(game.inventory),
                    "background strategic route operations must not mutate player resources or attention");
            require(!PlayerFacingText.containsLikelyLeak(plan.lastOutcome)
                            && !PlayerFacingText.containsLikelyLeak(
                            blocked.lastOutcome),
                    "strategic route outcomes should remain player-facing and implementation-safe");

            System.out.println("Milestone 06 faction strategic route-control smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static FactionStrategicPlan planningPlan(
            String id, String scheme, String destination,
            Faction target, int aggression, int ambition, int dueTurn) {
        FactionStrategicPlan plan = basePlan(id, scheme, destination,
                target, aggression, ambition);
        plan.phase = "PLANNING";
        plan.immediateGoal = "stockpile a strategic item";
        plan.phaseUntilTurn = dueTurn;
        plan.nextDecisionTurn = dueTurn;
        return plan;
    }

    private static FactionStrategicPlan executionPlan(
            String id, String scheme, String destination,
            Faction target, int aggression, int ambition, int turn) {
        FactionStrategicPlan plan = basePlan(id, scheme, destination,
                target, aggression, ambition);
        plan.phase = "EXECUTION";
        plan.immediateGoal = FactionVehicleRouteStrategicAuthority
                .VEHICLE_ROUTE_CONTROL_GOAL;
        plan.phaseUntilTurn = turn;
        plan.nextDecisionTurn = turn;
        return plan;
    }

    private static FactionStrategicPlan basePlan(
            String id, String scheme, String destination,
            Faction target, int aggression, int ambition) {
        FactionStrategicPlan plan = new FactionStrategicPlan();
        plan.id = id;
        plan.faction = Faction.MECHANICUS;
        plan.schemeTargetFaction = target;
        plan.scheme = scheme;
        plan.targetRoom = destination;
        plan.targetItem = "Fuel reserve";
        plan.secrecy = 55;
        plan.aggression = aggression;
        plan.ambition = ambition;
        return plan;
    }

    private static NpcFactionSite site(String name, Faction faction,
                                       World world) {
        NpcFactionSite site = NpcFactionSite.create(name, faction,
                "motor pool", world.sectorX, world.sectorY,
                world.zoneX, world.zoneY, world.floor,
                "Machine part", "Tool bundle", "Strategic Route Doctrine");
        site.stock = 60;
        site.workers = 4;
        site.baseLevel = 2;
        site.machineLevel = 2;
        return site;
    }

    private static World world() {
        World world = new World(61041L, 26, 14);
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.sectorX = 6;
        world.sectorY = 1;
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
                "VEHICLE-STRATEGIC-ROUTE-SMOKE");
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
            crew.append("Strategic Route Crew ").append(i);
        }
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "vehicleCrewManifest", crew.toString());
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

    private Milestone06FactionVehicleStrategicRouteControlSmoke() { }
}
