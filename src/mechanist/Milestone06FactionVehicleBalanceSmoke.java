package mechanist;

import java.util.List;
import java.util.Random;

/** Focused smoke for vehicle balance-of-power, deterrence, and escalation. */
final class Milestone06FactionVehicleBalanceSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.turn = 1_520;
            game.worldTurn = 1_520L;
            game.npcFactionSites.clear();
            game.factionStrategicPlans.clear();
            game.inventory.clear();
            game.carriedScript = 910;
            game.gangHeat = 3;
            game.suspicion = 4;

            NpcFactionSite mechanist = site("Mechanist Logistics Yard",
                    Faction.MECHANIST_COLLEGIA, game.world);
            mechanist.stock = 140;
            mechanist.workers = 8;
            mechanist.baseLevel = 3;
            mechanist.machineLevel = 4;
            game.npcFactionSites.add(mechanist);

            MapObjectState cargo = vehicle(game.world, 4, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    mechanist.faction, "industrial logistics", 301L);
            staff(cargo, "Driver Vale", 6);
            game.world.mapObjects.add(cargo);
            require(VehicleMotorPoolAuthority.assign(game, cargo, mechanist,
                    "production freight convoy",
                    "balance smoke logistics registration").success(),
                    "mechanist logistics truck should enter the motor pool");

            MapObjectState guardTank = vehicle(game.world, 10, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK,
                    Faction.IMPERIAL_GUARD, "armored route defense", 302L);
            staff(guardTank, "Commander Rusk", 6);
            game.world.mapObjects.add(guardTank);

            FactionVehicleBalanceAuthority.Contest initial =
                    FactionVehicleBalanceAuthority.compare(game,
                            mechanist.faction, Faction.IMPERIAL_GUARD,
                            mechanist);
            require((initial.posture()
                            == FactionVehicleBalanceAuthority.Posture.DETERRED
                            || initial.posture()
                            == FactionVehicleBalanceAuthority.Posture.OUTMATCHED)
                            && initial.defenderHeavy() == 1
                            && initial.defenderPower() > initial.attackerPower()
                            && initial.routeControlDelta() < 0,
                    "a logistics-only motor pool should be deterred by a ready guard tank: "
                            + initial.summary());

            FactionStrategicPlan lowCommitment = seizurePlan(
                    "STRAT-BALANCE-LOW", 20, 20, game.turn);
            FactionVehicleStrategicAuthority.Suggestion refusedPlanning =
                    FactionVehicleStrategicAuthority.nextSuggestion(game,
                            mechanist, lowCommitment);
            require(!refusedPlanning.available()
                            && !initial.canEscalate(lowCommitment.aggression,
                            lowCommitment.ambition),
                    "low-commitment planning should refuse a seizure against an armored deterrent");

            int stockBeforeBlock = mechanist.stock;
            String tankBeforeBlock = guardTank.stockState;
            int scriptBeforeBlock = game.carriedScript;
            int heatBeforeBlock = game.gangHeat;
            int suspicionBeforeBlock = game.suspicion;
            FactionStrategicAssetAuthority.Outcome blocked =
                    FactionVehicleStrategicAuthority.attempt(game,
                            lowCommitment, mechanist);
            require(blocked.handled() && !blocked.success()
                            && "vehicle-fleet-deterrence".equals(
                            blocked.blocker())
                            && mechanist.stock == stockBeforeBlock
                            && tankBeforeBlock.equals(guardTank.stockState)
                            && game.carriedScript == scriptBeforeBlock
                            && game.gangHeat == heatBeforeBlock
                            && game.suspicion == suspicionBeforeBlock
                            && VehicleRuntimeAuthority.factionOwns(
                            guardTank, Faction.IMPERIAL_GUARD),
                    "deterrence refusal must preserve stock, ownership, vehicle state, and player state");

            MapObjectState mechanistTankA = vehicle(game.world, 6, 7,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK,
                    mechanist.faction, "armored route control", 303L);
            MapObjectState mechanistTankB = vehicle(game.world, 8, 7,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK,
                    mechanist.faction, "armored strategic reserve", 304L);
            staff(mechanistTankA, "Commander Iron", 6);
            staff(mechanistTankB, "Commander Ash", 6);
            game.world.mapObjects.add(mechanistTankA);
            game.world.mapObjects.add(mechanistTankB);
            require(VehicleMotorPoolAuthority.assign(game, mechanistTankA,
                    mechanist, "armored route control",
                    "balance smoke reinforcement").success()
                            && VehicleMotorPoolAuthority.assign(game,
                            mechanistTankB, mechanist,
                            "armored strategic reserve",
                            "balance smoke reinforcement").success(),
                    "real armored reinforcements should enter the attacking fleet");

            FactionVehicleBalanceAuthority.Contest reinforced =
                    FactionVehicleBalanceAuthority.compare(game,
                            mechanist.faction, Faction.IMPERIAL_GUARD,
                            mechanist);
            require(reinforced.attackerHeavy() == 2
                            && reinforced.attackerPower()
                            > initial.attackerPower()
                            && reinforced.confidence() > initial.confidence()
                            && reinforced.deterrence() < initial.deterrence()
                            && (reinforced.posture()
                            == FactionVehicleBalanceAuthority.Posture.ADVANTAGED
                            || reinforced.posture()
                            == FactionVehicleBalanceAuthority.Posture.DOMINANT),
                    "two real tanks should materially reverse the local vehicle balance: "
                            + reinforced.summary());

            FactionStrategicPlan committed = seizurePlan(
                    "STRAT-BALANCE-COMMITTED", 85, 80, game.turn);
            FactionVehicleStrategicAuthority.Suggestion approvedPlanning =
                    FactionVehicleStrategicAuthority.nextSuggestion(game,
                            mechanist, committed);
            require(approvedPlanning.available()
                            && FactionVehicleStrategicAuthority.VEHICLE_SEIZURE_GOAL
                            .equals(approvedPlanning.goal())
                            && reinforced.canEscalate(committed.aggression,
                            committed.ambition),
                    "reinforced high-commitment leadership should approve the physical seizure plan");

            game.factionStrategicPlans.add(committed);
            int stockBeforeSuccess = mechanist.stock;
            int resolved = FactionStrategicAssetTickAuthority.tick(game);
            VehicleMotorPoolAuthority.Snapshot capturedPool =
                    VehicleMotorPoolAuthority.inspect(game, guardTank,
                            mechanist);
            require(resolved == 1 && committed.success == 1
                            && committed.failure == 0
                            && "COOLDOWN".equals(committed.phase)
                            && VehicleRuntimeAuthority.factionOwns(
                            guardTank, mechanist.faction)
                            && mechanist.stock < stockBeforeSuccess
                            && capturedPool.assigned()
                            && capturedPool.siteName().equals(mechanist.name)
                            && committed.lastOutcome.contains(
                            "fleet posture"),
                    "reinforced committed seizure should transfer and register the physical tank exactly once");
            require(game.carriedScript == scriptBeforeBlock
                            && game.suspicion == suspicionBeforeBlock,
                    "background faction escalation must not spend player script or alter suspicion");

            FactionVehicleBalanceAuthority.Contest afterCapture =
                    FactionVehicleBalanceAuthority.compare(game,
                            mechanist.faction, Faction.IMPERIAL_GUARD,
                            mechanist);
            require(afterCapture.attackerHeavy() == 3
                            && afterCapture.defenderHeavy() == 0
                            && afterCapture.attackerPower()
                            > reinforced.attackerPower(),
                    "captured armor should immediately move the derived balance of power");

            List<String> inspection =
                    FactionVehicleBalanceAuthority.inspectionLines(game,
                            mechanist.faction, Faction.IMPERIAL_GUARD,
                            mechanist);
            requireContains(inspection, "vehicle power",
                    "balance power inspection");
            requireContains(inspection, "heavy assets",
                    "balance heavy-asset inspection");
            requireContains(inspection, "leadership aggression and ambition",
                    "balance escalation inspection");
            for (String line : inspection) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "vehicle balance inspection leaked implementation text: "
                                + line);
            }

            System.out.println("Milestone 06 faction vehicle balance smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static FactionStrategicPlan seizurePlan(String id,
                                                     int aggression,
                                                     int ambition,
                                                     int turn) {
        FactionStrategicPlan plan = new FactionStrategicPlan();
        plan.id = id;
        plan.faction = Faction.MECHANICUS;
        plan.schemeTargetFaction = Faction.IMPERIAL_GUARD;
        plan.phase = "EXECUTION";
        plan.immediateGoal =
                FactionVehicleStrategicAuthority.VEHICLE_SEIZURE_GOAL;
        plan.scheme = "contest armored route control";
        plan.secrecy = 50;
        plan.aggression = aggression;
        plan.ambition = ambition;
        plan.phaseUntilTurn = turn;
        plan.nextDecisionTurn = turn;
        return plan;
    }

    private static NpcFactionSite site(String name, Faction faction,
                                       World world) {
        NpcFactionSite site = NpcFactionSite.create(name, faction,
                "motor pool", world.sectorX, world.sectorY,
                world.zoneX, world.zoneY, world.floor,
                "Machine part", "Tool bundle", "Fleet Balance Doctrine");
        site.stock = 60;
        site.workers = 4;
        site.baseLevel = 2;
        site.machineLevel = 2;
        return site;
    }

    private static World world() {
        World world = new World(61039L, 24, 14);
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.sectorX = 5;
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
                "VEHICLE-BALANCE-SMOKE");
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
            crew.append("Balance Crew ").append(i);
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

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06FactionVehicleBalanceSmoke() { }
}
