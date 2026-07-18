package mechanist;

import java.util.List;
import java.util.Random;

/** Focused smoke for persistent motor-pool assignment, readiness, strategy reconciliation, and release. */
final class Milestone06VehicleMotorPoolSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.turn = 1_380;
            game.worldTurn = 1_380L;
            game.npcFactionSites.clear();
            game.factionStrategicPlans.clear();
            game.inventory.clear();

            NpcFactionSite site = NpcFactionSite.create(
                    "Mechanist Motor Pool",
                    Faction.MECHANIST_COLLEGIA, "motor pool",
                    game.world.sectorX, game.world.sectorY,
                    game.world.zoneX, game.world.zoneY, game.world.floor,
                    "Machine part", "Tool bundle",
                    "Vehicle Maintenance Doctrine");
            site.stock = 80;
            site.workers = 6;
            site.baseLevel = 2;
            site.machineLevel = 3;
            game.npcFactionSites.add(site);

            MapObjectState truck = vehicle(game.world, 5, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                    site.faction, "faction", 151L);
            game.world.mapObjects.add(truck);

            VehicleMotorPoolAuthority.Result assigned =
                    VehicleMotorPoolAuthority.assign(game, truck, site,
                            "freight logistics", "scheduled fleet registration");
            require(assigned.success() && assigned.changed()
                            && assigned.snapshot().assigned()
                            && assigned.snapshot().siteName().equals(site.name)
                            && "uncrewed".equals(assigned.snapshot().state()),
                    "faction truck should receive a persistent local assignment and begin uncrewed");
            String assignedState = truck.stockState;
            VehicleMotorPoolAuthority.Result duplicate =
                    VehicleMotorPoolAuthority.assign(game, truck, site,
                            "freight logistics", "duplicate registration");
            require(duplicate.success() && !duplicate.changed()
                            && assignedState.equals(truck.stockState),
                    "identical motor-pool assignment should be idempotent");

            staff(truck, "Driver Octa Venn", 6);
            VehicleMotorPoolAuthority.Result crewReady =
                    VehicleMotorPoolAuthority.reconcile(game, truck, site,
                            "crew roster completed");
            require(crewReady.success() && crewReady.changed()
                            && "ready".equals(crewReady.snapshot().state())
                            && crewReady.snapshot().assignedCrew() == 7
                            && crewReady.snapshot().requiredCrew() == 7,
                    "driver plus six support crew should make the cargo truck ready");

            int capacity = VehicleFuelAuthority.inspect(game.world, truck).capacity();
            truck.stockState = MapObjectState.setStockFlag(truck.stockState,
                    "fuelOrPowerCurrent", "0");
            VehicleMotorPoolAuthority.Result empty =
                    VehicleMotorPoolAuthority.reconcile(game, truck, site,
                            "fuel reserve exhausted");
            require(empty.success() && empty.changed()
                            && "refuel".equals(empty.snapshot().state()),
                    "empty fuel ledger should move an otherwise ready vehicle into refuel state");
            truck.stockState = MapObjectState.setStockFlag(truck.stockState,
                    "fuelOrPowerCurrent", Integer.toString(capacity));
            require("ready".equals(VehicleMotorPoolAuthority.reconcile(
                    game, truck, site, "fuel reserve restored").snapshot().state()),
                    "restored fuel should return the staffed truck to ready state");

            VehicleRuntimeAuthority.applyDamage(truck,
                    VehicleRuntimeAuthority.Component.MOBILITY, 20,
                    game.turn, "motor-pool smoke mobility damage");
            VehicleMotorPoolAuthority.Result maintenance =
                    VehicleMotorPoolAuthority.reconcile(game, truck, site,
                            "mobility damage reported");
            require(maintenance.success() && maintenance.changed()
                            && "maintenance".equals(maintenance.snapshot().state()),
                    "component damage should move the assigned vehicle into maintenance");
            require(VehicleRuntimeAuthority.repairForFaction(truck, 35,
                    game.turn, site.faction,
                    site.name + " smoke repair").success(),
                    "same-family motor pool should repair its assigned truck");
            require("ready".equals(VehicleMotorPoolAuthority.reconcile(
                    game, truck, site, "maintenance completed").snapshot().state()),
                    "completed component repair should restore motor-pool readiness");

            truck.stockState = MapObjectState.setStockFlag(truck.stockState,
                    "strategicTransitState", "reserved");
            VehicleMotorPoolAuthority.Result reserved =
                    VehicleMotorPoolAuthority.reconcile(game, truck, site,
                            "strategic deployment reserved");
            require(reserved.success() && reserved.changed()
                            && "deployment-reserved".equals(
                            reserved.snapshot().state()),
                    "strategic reservation should be visible in the motor-pool readiness ledger");
            String beforeBlockedRelease = truck.stockState;
            VehicleMotorPoolAuthority.Result blockedRelease =
                    VehicleMotorPoolAuthority.release(game, truck, site,
                            "invalid release during deployment");
            require(!blockedRelease.success()
                            && beforeBlockedRelease.equals(truck.stockState),
                    "active strategic deployment must block assignment release without mutation");
            truck.stockState = MapObjectState.setStockFlag(truck.stockState,
                    "strategicTransitState", "cancelled");
            VehicleMotorPoolAuthority.Result released =
                    VehicleMotorPoolAuthority.release(game, truck, site,
                            "freight rotation ended");
            require(released.success() && released.changed()
                            && !released.snapshot().assigned()
                            && "released".equals(released.snapshot().state())
                            && site.name.equals(MapObjectState.stockValue(
                            truck.stockState, "lastMotorPoolSiteName")),
                    "completed stand-down should clear the active assignment while retaining its last pool");
            require(VehicleMotorPoolAuthority.assign(game, truck, site,
                    "freight reserve", "returned to fleet reserve").success(),
                    "released vehicle should be assignable again");

            MapObjectState foreign = vehicle(game.world, 7, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    Faction.NOBLE, "faction", 152L);
            game.world.mapObjects.add(foreign);
            String foreignBefore = foreign.stockState;
            VehicleMotorPoolAuthority.Result denied =
                    VehicleMotorPoolAuthority.assign(game, foreign, site,
                            "staff transport", "unauthorized appropriation");
            require(!denied.success() && foreignBefore.equals(foreign.stockState),
                    "foreign faction vehicle must reject motor-pool assignment without ownership transfer");

            NpcFactionSite annex = NpcFactionSite.create(
                    "Mechanist Reserve Annex",
                    Faction.MECHANIST_COLLEGIA, "motor pool annex",
                    game.world.sectorX, game.world.sectorY,
                    game.world.zoneX, game.world.zoneY, game.world.floor,
                    "Machine part", "Tool bundle",
                    "Reserve Fleet Doctrine");
            annex.stock = 30;
            annex.workers = 3;
            MapObjectState annexCar = vehicle(game.world, 9, 5,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    annex.faction, "faction", 153L);
            game.world.mapObjects.add(annexCar);
            require(VehicleMotorPoolAuthority.assign(game, annexCar, annex,
                    "reserve staff transport", "annex registration").success(),
                    "same-family annex should own its own explicit vehicle assignment");
            String annexBefore = annexCar.stockState;
            int unrelatedChanges = VehicleMotorPoolAuthority.reconcileSiteFleet(
                    game, site, "primary pool activity");
            VehicleMotorPoolAuthority.Snapshot annexAfter =
                    VehicleMotorPoolAuthority.inspect(game, annexCar, annex);
            require(unrelatedChanges == 0
                            && annexBefore.equals(annexCar.stockState)
                            && annexAfter.assigned()
                            && annexAfter.siteName().equals(annex.name),
                    "primary pool reconciliation must not steal a vehicle already assigned to another same-family pool");

            FactionStrategicPlan seizure = new FactionStrategicPlan();
            seizure.id = "STRAT-MOTOR-POOL-SEIZURE";
            seizure.faction = Faction.MECHANICUS;
            seizure.schemeTargetFaction = Faction.NOBLE;
            seizure.phase = "EXECUTION";
            seizure.immediateGoal =
                    FactionVehicleStrategicAuthority.VEHICLE_SEIZURE_GOAL;
            seizure.scheme = "divert rival staff transport";
            seizure.secrecy = 60;
            seizure.aggression = 45;
            seizure.ambition = 55;
            seizure.phaseUntilTurn = game.turn;
            seizure.nextDecisionTurn = game.turn;
            game.factionStrategicPlans.add(seizure);
            int stockBeforeSeizure = site.stock;
            int resolved = FactionStrategicAssetTickAuthority.tick(game);
            VehicleMotorPoolAuthority.Snapshot seized =
                    VehicleMotorPoolAuthority.inspect(game, foreign, site);
            require(resolved == 1 && seizure.success == 1
                            && VehicleRuntimeAuthority.factionOwns(foreign,
                            site.faction)
                            && seized.assigned()
                            && seized.siteName().equals(site.name)
                            && site.stock < stockBeforeSeizure,
                    "successful physical seizure should automatically register the transferred vehicle with the local motor pool");

            seizure.phase = "EXECUTION";
            seizure.immediateGoal =
                    FactionVehicleStrategicAuthority.VEHICLE_SALVAGE_GOAL;
            seizure.phaseUntilTurn = game.turn;
            seizure.nextDecisionTurn = game.turn;
            int stockBeforeSalvage = site.stock;
            resolved = FactionStrategicAssetTickAuthority.tick(game);
            VehicleMotorPoolAuthority.Snapshot salvaged =
                    VehicleMotorPoolAuthority.inspect(game, foreign, site);
            require(resolved == 1 && seizure.success == 2
                            && "salvaged".equals(MapObjectState.stockValue(
                            foreign.stockState, "condition"))
                            && !salvaged.assigned()
                            && "released".equals(salvaged.state())
                            && site.stock > stockBeforeSalvage,
                    "successful faction salvage should release the stripped hulk from the active motor pool");

            MapObjectState restored = copy(truck);
            VehicleMotorPoolAuthority.Snapshot restoredPool =
                    VehicleMotorPoolAuthority.inspect(game, restored, site);
            require(restoredPool.assigned()
                            && restoredPool.siteName().equals(site.name)
                            && restoredPool.role().equals("freight reserve")
                            && !restoredPool.history().isEmpty(),
                    "motor-pool assignment and history should reconstruct from persisted vehicle stock state");

            List<String> inspection =
                    VehicleMotorPoolAuthority.inspectionLines(game, truck, site);
            requireContains(inspection, "Mechanist Motor Pool",
                    "motor-pool inspection site");
            requireContains(inspection, "freight reserve",
                    "motor-pool inspection role");
            requireContains(inspection, "operational crew 7/7",
                    "motor-pool inspection crew");
            requireContains(inspection, "fuel or power",
                    "motor-pool inspection energy");
            for (String line : inspection) {
                require(!PlayerFacingText.containsLikelyLeak(line),
                        "motor-pool inspection leaked implementation text: " + line);
            }

            System.out.println("Milestone 06 vehicle motor-pool smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World world() {
        World world = new World(61037L, 20, 12);
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.sectorX = 3;
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
                                          String ownership, long seed) {
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type,
                                world.tiles[x][y]));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed),
                "VEHICLE-MOTOR-POOL-SMOKE");
        VehicleRuntimeAuthority.initialize(world, vehicle, faction,
                ownership, "motor-pool-smoke", false, new Random(seed));
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerType", VehicleRuntimeAuthority.OwnerType.FACTION.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerFaction", faction.name());
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownerName", faction.label);
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "ownership", ownership);
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
            crew.append("Motor Crew ").append(i);
        }
        vehicle.stockState = MapObjectState.setStockFlag(vehicle.stockState,
                "vehicleCrewManifest", crew.toString());
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

    private Milestone06VehicleMotorPoolSmoke() { }
}
