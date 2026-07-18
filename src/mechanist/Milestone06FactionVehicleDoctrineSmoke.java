package mechanist;

import java.util.Random;

/** Focused smoke for faction vehicle doctrine, strategic fleet power, and doctrine-aware strategy selection. */
final class Milestone06FactionVehicleDoctrineSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.turn = 1_460;
            game.worldTurn = 1_460L;
            game.npcFactionSites.clear();
            game.factionStrategicPlans.clear();
            game.inventory.clear();

            NpcFactionSite mechanist = site("Mechanist Fleet Works",
                    Faction.MECHANIST_COLLEGIA, game.world);
            mechanist.stock = 120;
            mechanist.workers = 8;
            mechanist.baseLevel = 3;
            mechanist.machineLevel = 4;
            game.npcFactionSites.add(mechanist);
            NpcFactionSite wardens = site("Civic Warden Motor Pool",
                    Faction.CIVIC_WARDENS, game.world);
            NpcFactionSite nobles = site("House Vehicle Court",
                    Faction.NOBLE, game.world);
            NpcFactionSite guard = site("Concord Guard Armor Park",
                    Faction.IMPERIAL_GUARD, game.world);

            MapObjectState cargo = vehicle(game.world, 4, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    mechanist.faction, "faction", "industrial freight", 201L);
            staff(cargo, "Driver Kest", 6);
            game.world.mapObjects.add(cargo);
            require(VehicleMotorPoolAuthority.assign(game, cargo, mechanist,
                    "production freight convoy",
                    "doctrine smoke fleet registration").success(),
                    "mechanist cargo truck should receive a motor-pool assignment");

            MapObjectState armored = vehicle(game.world, 6, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR,
                    mechanist.faction, "faction", "security patrol", 202L);
            game.world.mapObjects.add(armored);
            require(VehicleMotorPoolAuthority.assign(game, armored, mechanist,
                    "yard security patrol",
                    "doctrine smoke security registration").success(),
                    "mechanist armored car should receive a motor-pool assignment");

            MapObjectState tank = vehicle(game.world, 8, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK,
                    guard.faction, "faction", "armored assault", 203L);
            game.world.mapObjects.add(tank);
            MapObjectState nobleCar = vehicle(game.world, 10, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    nobles.faction, "faction", "noble prestige transport", 204L);

            FactionVehicleDoctrineAuthority.VehicleAssessment cargoMechanist =
                    FactionVehicleDoctrineAuthority.assess(game, cargo,
                            mechanist);
            FactionVehicleDoctrineAuthority.VehicleAssessment cargoWarden =
                    FactionVehicleDoctrineAuthority.assess(game, cargo,
                            wardens);
            require(cargoMechanist.doctrineFit() > cargoWarden.doctrineFit()
                            && cargoMechanist.readiness() >= 75,
                    "industrial cargo truck should fit mechanist logistics doctrine better than warden patrol doctrine");

            FactionVehicleDoctrineAuthority.VehicleAssessment armoredMechanist =
                    FactionVehicleDoctrineAuthority.assess(game, armored,
                            mechanist);
            FactionVehicleDoctrineAuthority.VehicleAssessment armoredWarden =
                    FactionVehicleDoctrineAuthority.assess(game, armored,
                            wardens);
            require(armoredWarden.doctrineFit()
                            > armoredMechanist.doctrineFit(),
                    "armored patrol car should fit civic warden doctrine better than industrial logistics doctrine");

            FactionVehicleDoctrineAuthority.VehicleAssessment carNoble =
                    FactionVehicleDoctrineAuthority.assess(game, nobleCar,
                            nobles);
            FactionVehicleDoctrineAuthority.VehicleAssessment carMechanist =
                    FactionVehicleDoctrineAuthority.assess(game, nobleCar,
                            mechanist);
            require(carNoble.doctrineFit() > carMechanist.doctrineFit(),
                    "prestige passenger car should fit noble doctrine better than mechanist doctrine");

            FactionVehicleDoctrineAuthority.VehicleAssessment tankGuard =
                    FactionVehicleDoctrineAuthority.assess(game, tank, guard);
            require(tankGuard.strategicValue()
                            > cargoMechanist.strategicValue()
                            && tankGuard.contribution().getOrDefault(
                            FactionVehicleDoctrineAuthority.Dimension.STRATEGIC_PROJECTION,
                            0) > 0,
                    "tank should be a stronger strategic projection asset than a cargo truck");

            FactionVehicleDoctrineAuthority.FleetSnapshot fleet =
                    FactionVehicleDoctrineAuthority.fleet(game,
                            mechanist.faction, mechanist);
            require(fleet.vehicleCount() == 2
                            && fleet.readyVehicles() >= 1
                            && fleet.heavyVehicles() == 1
                            && fleet.totalPower() > 0
                            && fleet.power(FactionVehicleDoctrineAuthority.Dimension.LOGISTICS)
                            > fleet.power(FactionVehicleDoctrineAuthority.Dimension.ASSAULT),
                    "mechanist local fleet should report two assets with logistics exceeding assault power: "
                            + fleet.summary());

            MapObjectState capturedCargo = vehicle(game.world, 12, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    Faction.NOBLE, "faction", "commercial freight", 205L);
            game.world.mapObjects.add(capturedCargo);
            require(VehicleRuntimeAuthority.transferToFaction(capturedCargo,
                    mechanist.faction, game.turn,
                    "captured freight asset").success(),
                    "captured cargo fixture should transfer to mechanist custody");
            require(VehicleMotorPoolAuthority.assign(game, capturedCargo,
                    mechanist, "captured logistics reserve",
                    "retained after seizure").success(),
                    "captured cargo truck should be assignable to the fleet");
            require(!FactionVehicleDoctrineAuthority.shouldSalvageCaptured(
                    game, capturedCargo, mechanist),
                    "serviceable doctrine-aligned captured cargo truck should be retained");

            MapObjectState capturedWreck = vehicle(game.world, 14, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    Faction.NOBLE, "faction", "damaged staff transport", 206L);
            game.world.mapObjects.add(capturedWreck);
            require(VehicleRuntimeAuthority.transferToFaction(capturedWreck,
                    mechanist.faction, game.turn,
                    "captured wreck candidate").success(),
                    "wreck candidate should transfer to mechanist custody");
            VehicleRuntimeAuthority.applyDamage(capturedWreck,
                    VehicleRuntimeAuthority.Component.FRAME, 100,
                    game.turn, "catastrophic doctrine smoke damage");
            capturedWreck.stockState = MapObjectState.setStockFlag(
                    capturedWreck.stockState, "strategicTransitState",
                    "reserved");
            FactionVehicleDoctrineAuthority.VehicleAssessment wreckAssessment =
                    FactionVehicleDoctrineAuthority.assess(game,
                            capturedWreck, mechanist);
            require(wreckAssessment.readiness() <= 10
                            && wreckAssessment.salvageRecommended()
                            && wreckAssessment.strategicValue()
                            < FactionVehicleDoctrineAuthority.assess(
                            game, capturedCargo, mechanist).strategicValue(),
                    "stale transit reservation must not elevate a wreck above retained fleet assets");

            FactionVehicleStrategicAuthority.Suggestion suggestion =
                    FactionVehicleStrategicAuthority.nextSuggestion(game,
                            mechanist, null);
            require(suggestion.available()
                            && FactionVehicleStrategicAuthority.VEHICLE_SALVAGE_GOAL
                            .equals(suggestion.goal())
                            && suggestion.target().contains(
                            VehicleRuntimeAuthority.inspect(game.world,
                                    capturedWreck).model()),
                    "doctrine-aware planning should offer the catastrophic captured car for salvage");

            MapObjectState rivalCar = vehicle(game.world, 16, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    Faction.NOBLE, "faction", "staff transport", 207L);
            MapObjectState rivalCargo = vehicle(game.world, 18, 4,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    Faction.NOBLE, "faction", "industrial freight", 208L);
            game.world.mapObjects.add(rivalCar);
            game.world.mapObjects.add(rivalCargo);

            FactionStrategicPlan seizure = new FactionStrategicPlan();
            seizure.id = "STRAT-DOCTRINE-SEIZURE";
            seizure.faction = Faction.MECHANICUS;
            seizure.schemeTargetFaction = Faction.NOBLE;
            seizure.phase = "EXECUTION";
            seizure.immediateGoal =
                    FactionVehicleStrategicAuthority.VEHICLE_SEIZURE_GOAL;
            seizure.scheme = "capture rival logistics capacity";
            seizure.secrecy = 55;
            seizure.aggression = 45;
            seizure.ambition = 65;
            seizure.phaseUntilTurn = game.turn;
            seizure.nextDecisionTurn = game.turn;
            game.factionStrategicPlans.add(seizure);
            int resolved = FactionStrategicAssetTickAuthority.tick(game);
            require(resolved == 1 && seizure.success == 1
                            && VehicleRuntimeAuthority.factionOwns(
                            rivalCargo, mechanist.faction)
                            && !VehicleRuntimeAuthority.factionOwns(
                            rivalCar, mechanist.faction),
                    "mechanist seizure should choose the doctrine-aligned cargo truck over the passenger car");

            FactionVehicleDoctrineAuthority.FleetSnapshot expanded =
                    FactionVehicleDoctrineAuthority.fleet(game,
                            mechanist.faction, mechanist);
            require(expanded.vehicleCount() > fleet.vehicleCount()
                            && expanded.totalPower() > fleet.totalPower(),
                    "captured retained vehicles should increase derived faction fleet power");

            for (String line : FactionVehicleDoctrineAuthority.inspectionLines(
                    game, capturedCargo, mechanist)) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "vehicle doctrine inspection leaked implementation text: "
                                + line);
            }
            require(!PlayerFacingText.containsLikelyLeak(expanded.summary()),
                    "fleet doctrine summary leaked implementation text: "
                            + expanded.summary());

            System.out.println("Milestone 06 faction vehicle doctrine smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
        Milestone06FactionVehicleBalanceSmoke.main(args);
    }

    private static NpcFactionSite site(String name, Faction faction,
                                       World world) {
        NpcFactionSite site = NpcFactionSite.create(name, faction,
                "motor pool", world.sectorX, world.sectorY,
                world.zoneX, world.zoneY, world.floor,
                "Machine part", "Tool bundle", "Vehicle Doctrine");
        site.stock = 60;
        site.workers = 4;
        site.baseLevel = 2;
        site.machineLevel = 2;
        return site;
    }

    private static World world() {
        World world = new World(61038L, 24, 12);
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.sectorX = 4;
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
                                          String ownership, String role,
                                          long seed) {
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "VEHICLE-DOCTRINE-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, faction,
                ownership, role, false, new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", VehicleRuntimeAuthority.OwnerType.FACTION.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", faction.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", faction.label);
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", ownership);
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
            crew.append("Doctrine Crew ").append(i);
        }
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "vehicleCrewManifest", crew.toString());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone06FactionVehicleDoctrineSmoke() { }
}
