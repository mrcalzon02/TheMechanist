package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Focused Milestone 06 smoke for vehicle identity, ownership, repair, and salvage parity. */
final class Milestone06VehicleRuntimeFoundationSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.world.mapObjects.clear();
            game.npcFactionSites.clear();
            game.factionStrategicPlans.clear();
            game.inventory.clear();
            game.itemContainers.clear();
            game.itemInstances.clear();
            game.itemProvenance.clear();
            game.carriedScript = 1400;
            game.gangHeat = 0;
            game.suspicion = 0;
            game.turn = 600;
            game.worldTurn = 600L;

            verifyCatalog();

            MapObjectState dealership = frontage(game.world, 2, 2,
                    AssetIntegrationDisciplineAuthority.VEHICLE_DEALERSHIP_FRONTAGE,
                    "Licensed vehicle dealership");
            MapObjectState garage = frontage(game.world, 3, 2,
                    AssetIntegrationDisciplineAuthority.VEHICLE_SERVICE_GARAGE_FRONTAGE,
                    "Vehicle maintenance garage");
            MapObjectState saleCar = vehicle(game.world, 6, 2,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    Faction.CIVIC_LEDGER_OFFICE, "private", "personal-transport", true, 11L);
            MapObjectState abandonedBike = vehicle(game.world, 8, 2,
                    AssetIntegrationDisciplineAuthority.PARKED_UTILITY_BIKE,
                    Faction.NONE, "abandoned", "light-transit", false, 12L);
            MapObjectState wreck = vehicle(game.world, 10, 2,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    Faction.NONE, "abandoned", "salvage", false, 13L);
            game.world.mapObjects.add(dealership);
            game.world.mapObjects.add(garage);
            game.world.mapObjects.add(saleCar);
            game.world.mapObjects.add(abandonedBike);
            game.world.mapObjects.add(wreck);

            VehicleRuntimeAuthority.Snapshot saleBefore =
                    VehicleRuntimeAuthority.inspect(game.world, saleCar);
            require(saleBefore != null && saleBefore.components().size() == 8,
                    "initialized vehicle should expose all eight major component areas");
            require(saleBefore.ownerType() == VehicleRuntimeAuthority.OwnerType.PRIVATE
                            && saleBefore.forSale(),
                    "dealership stock should begin as titled private property for sale");
            require(!saleBefore.manufacturer().isBlank()
                            && !saleBefore.model().isBlank()
                            && !saleBefore.productionBatch().isBlank(),
                    "vehicle identity should include manufacturer, model, and production batch");

            int scriptBefore = game.carriedScript;
            VehicleRuntimeAuthority.Result purchase =
                    VehicleRuntimeAuthority.purchaseNearestForSale(game, dealership);
            require(purchase.success() && purchase.changed() && purchase.vehicle() == saleCar,
                    "dealership should transfer the nearest real for-sale vehicle fixture");
            VehicleRuntimeAuthority.Snapshot purchased =
                    VehicleRuntimeAuthority.inspect(game.world, saleCar);
            require(purchased.ownerType() == VehicleRuntimeAuthority.OwnerType.PLAYER
                            && !purchased.forSale()
                            && "player-owned".equals(purchased.ownership()),
                    "purchase should replace private sale custody with player ownership");
            require(game.carriedScript == scriptBefore - saleBefore.purchasePrice(),
                    "purchase should spend the exact vehicle catalog price once");
            require(hasTitle(game, purchased),
                    "purchase should issue a readable title receipt for the transferred fixture");
            require(game.gangHeat > 0,
                    "vehicle acquisition should create class-scaled expansion attention");

            VehicleRuntimeAuthority.Result damage = VehicleRuntimeAuthority.applyDamage(
                    saleCar, VehicleRuntimeAuthority.Component.POWERPLANT,
                    60, game.turn, "smoke engine damage");
            require(damage.success() && component(saleCar,
                    VehicleRuntimeAuthority.Component.POWERPLANT) == 40,
                    "component damage should mutate the exact vehicle powerplant");
            game.inventory.add("Machine part");
            VehicleRuntimeAuthority.Result repair =
                    VehicleRuntimeAuthority.serviceNearestPlayerVehicle(game, garage);
            require(repair.success() && repair.changed()
                            && component(saleCar,
                            VehicleRuntimeAuthority.Component.POWERPLANT) == 75,
                    "garage service should consume one part and repair the worst component");
            require(!game.inventory.contains("Machine part"),
                    "owner-supplied garage repair should consume exactly one Machine part");
            requireContains(MapObjectState.stockValue(saleCar.stockState, "repairHistory"),
                    "Engine / powerplant", "vehicle repair history");

            int heatBeforeClaim = game.gangHeat;
            VehicleRuntimeAuthority.Result claim =
                    VehicleRuntimeAuthority.claimAbandoned(game, abandonedBike);
            require(claim.success() && VehicleRuntimeAuthority.playerOwns(game, abandonedBike),
                    "legal abandoned civilian vehicle should support a player salvage claim");
            require(game.gangHeat > heatBeforeClaim,
                    "abandoned vehicle claim should still add visible fleet attention");

            VehicleRuntimeAuthority.applyDamage(wreck,
                    VehicleRuntimeAuthority.Component.FRAME, 100,
                    game.turn, "smoke catastrophic frame damage");
            require("wreck".equals(MapObjectState.stockValue(wreck.stockState, "condition")),
                    "zero frame integrity should create a persistent wreck condition");
            int partsBefore = count(game.inventory, "Machine part");
            VehicleRuntimeAuthority.Result salvage =
                    VehicleRuntimeAuthority.salvageForPlayer(game, wreck);
            require(salvage.success() && salvage.amount() > 0,
                    "eligible wreck should yield a bounded player salvage amount");
            require(count(game.inventory, "Machine part")
                            == partsBefore + salvage.amount(),
                    "player salvage should materialize the exact Machine part yield");
            require("salvaged".equals(MapObjectState.stockValue(
                    wreck.stockState, "condition")) && wreck.glyph == 'o',
                    "salvage should leave a persistent stripped-hulk fixture");
            VehicleRuntimeAuthority.Result repeatSalvage =
                    VehicleRuntimeAuthority.salvageForPlayer(game, wreck);
            require(!repeatSalvage.success()
                            && count(game.inventory, "Machine part")
                            == partsBefore + salvage.amount(),
                    "stripped vehicle hulk must not grant repeat salvage");

            MapObjectState restored = new MapObjectState();
            restored.id = saleCar.id;
            restored.type = saleCar.type;
            restored.label = saleCar.label;
            restored.x = saleCar.x;
            restored.y = saleCar.y;
            restored.glyph = saleCar.glyph;
            restored.stockState = saleCar.stockState;
            VehicleRuntimeAuthority.Snapshot restoredSnapshot =
                    VehicleRuntimeAuthority.inspect(game.world, restored);
            require(restoredSnapshot.ownerType() == VehicleRuntimeAuthority.OwnerType.PLAYER
                            && restoredSnapshot.components().get(
                            VehicleRuntimeAuthority.Component.POWERPLANT) == 75,
                    "vehicle authority should reconstruct ownership and component truth from persisted stock state");
            require(!restoredSnapshot.history().isEmpty(),
                    "persisted vehicle state should retain its lifecycle history");

            verifyFactionLifecycle(game);
            requireContains(VehicleExpansionAttentionAuthority.fleetSummary(game),
                    "Player vehicle footprint", "fleet attention readback");
            require(!PlayerFacingText.containsLikelyLeak(
                    VehicleRuntimeAuthority.inspectionLine(game.world, saleCar)),
                    "ordinary vehicle inspection should not leak raw implementation identifiers");

            System.out.println("Milestone 06 vehicle runtime foundation smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void verifyFactionLifecycle(GamePanel game) {
        NpcFactionSite site = NpcFactionSite.create("Mechanist Motor Pool",
                Faction.MECHANIST_COLLEGIA, "motor pool",
                game.world.sectorX, game.world.sectorY,
                game.world.zoneX, game.world.zoneY, game.world.floor,
                "Machine part", "Tool bundle", "Vehicle Maintenance Doctrine");
        site.stock = 50;
        site.workers = 4;
        site.baseLevel = 2;
        site.machineLevel = 3;
        game.npcFactionSites.add(site);

        MapObjectState nobleTruck = vehicle(game.world, 12, 8,
                AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                Faction.NOBLE, "faction", "commercial-freight", false, 21L);
        game.world.mapObjects.add(nobleTruck);

        FactionStrategicPlan plan = new FactionStrategicPlan();
        plan.id = "STRAT-VEHICLE-SMOKE";
        plan.faction = Faction.MECHANICUS;
        plan.schemeTargetFaction = Faction.NOBLE;
        plan.phase = "EXECUTION";
        plan.immediateGoal = FactionVehicleStrategicAuthority.VEHICLE_SEIZURE_GOAL;
        plan.scheme = "divert faction stock";
        plan.secrecy = 60;
        plan.aggression = 50;
        plan.ambition = 55;
        plan.phaseUntilTurn = game.turn;
        plan.nextDecisionTurn = game.turn;
        game.factionStrategicPlans.add(plan);

        PlayerState playerBefore = PlayerState.capture(game);
        int stockBeforeSeizure = site.stock;
        int resolved = FactionStrategicAssetTickAuthority.tick(game);
        require(resolved == 1 && "COOLDOWN".equals(plan.phase)
                        && plan.success == 1 && plan.failure == 0,
                "strategy tick should resolve one physical vehicle seizure and skip abstract duplication");
        require(VehicleRuntimeAuthority.factionOwns(nobleTruck, site.faction)
                        && VehicleRuntimeAuthority.seized(nobleTruck)
                        && site.stock < stockBeforeSeizure,
                "scheme seizure should transfer the physical vehicle and spend faction stock");
        requireContains(MapObjectState.stockValue(nobleTruck.stockState, "captureHistory"),
                "Mechanist", "faction vehicle capture history");
        require(VehicleMotorPoolAuthority.inspect(game, nobleTruck, site).assigned(),
                "successful seizure should register the useful cargo truck with the local motor pool");
        playerBefore.requireSame(game);

        preparePlanning(plan, "stockpile a strategic item");
        FactionStrategicAssetTickAuthority.tick(game);
        require(!FactionVehicleStrategicAuthority.VEHICLE_SALVAGE_GOAL
                        .equals(plan.immediateGoal)
                        && !"salvaged".equals(MapObjectState.stockValue(
                        nobleTruck.stockState, "condition"))
                        && !FactionVehicleDoctrineAuthority.shouldSalvageCaptured(
                        game, nobleTruck, site),
                "useful captured cargo truck should remain a doctrine-aligned fleet asset");

        MapObjectState capturedWreck = vehicle(game.world, 18, 8,
                AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                Faction.NOBLE, "faction", "damaged-staff-transport", false, 24L);
        game.world.mapObjects.add(capturedWreck);
        require(VehicleRuntimeAuthority.transferToFaction(capturedWreck,
                site.faction, game.turn, "smoke catastrophic capture").success(),
                "catastrophic salvage fixture should transfer into faction custody");
        VehicleRuntimeAuthority.applyDamage(capturedWreck,
                VehicleRuntimeAuthority.Component.FRAME, 100,
                game.turn, "captured vehicle frame collapse");
        require(FactionVehicleDoctrineAuthority.shouldSalvageCaptured(
                game, capturedWreck, site),
                "catastrophic captured civilian vehicle should be recommended for salvage");

        preparePlanning(plan, "stockpile a strategic item");
        FactionStrategicAssetTickAuthority.tick(game);
        require(FactionVehicleStrategicAuthority.VEHICLE_SALVAGE_GOAL
                        .equals(plan.immediateGoal),
                "catastrophic captured vehicle should promote the next planning cycle into salvage");
        int stockBeforeSalvage = site.stock;
        prepareExecution(plan, game.turn);
        FactionStrategicAssetTickAuthority.tick(game);
        require("salvaged".equals(MapObjectState.stockValue(
                        capturedWreck.stockState, "condition"))
                        && !"salvaged".equals(MapObjectState.stockValue(
                        nobleTruck.stockState, "condition"))
                        && site.stock > stockBeforeSalvage
                        && plan.success == 2,
                "faction salvage should strip the poor-fit wreck while retaining the useful cargo truck");
        require(hasFactionStockMachinePart(game, site),
                "faction vehicle salvage should materialize provenance-aware Machine part stock");
        playerBefore.requireSame(game);

        MapObjectState factionArmored = vehicle(game.world, 14, 8,
                AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR,
                site.faction, "faction", "motor-pool", false, 22L);
        game.world.mapObjects.add(factionArmored);
        VehicleRuntimeAuthority.applyDamage(factionArmored,
                VehicleRuntimeAuthority.Component.MOBILITY, 55,
                game.turn, "smoke track damage");
        int mobilityBefore = component(factionArmored,
                VehicleRuntimeAuthority.Component.MOBILITY);
        preparePlanning(plan, "secure ammunition reserves");
        FactionStrategicAssetTickAuthority.tick(game);
        require(FactionVehicleStrategicAuthority.VEHICLE_REPAIR_GOAL
                        .equals(plan.immediateGoal),
                "damaged faction-owned vehicle should promote a physical repair objective");
        int stockBeforeRepair = site.stock;
        prepareExecution(plan, game.turn);
        FactionStrategicAssetTickAuthority.tick(game);
        require(component(factionArmored,
                        VehicleRuntimeAuthority.Component.MOBILITY) > mobilityBefore
                        && site.stock < stockBeforeRepair
                        && plan.success == 3,
                "faction repair should spend site stock and restore the worst component");
        playerBefore.requireSame(game);

        MapObjectState seizedTank = vehicle(game.world, 16, 8,
                AssetIntegrationDisciplineAuthority.PARKED_TANK,
                site.faction, "faction", "military-armor", false, 23L);
        game.world.mapObjects.add(seizedTank);
        VehicleRuntimeAuthority.transferToFaction(seizedTank, site.faction,
                game.turn, "smoke seizure setup");
        seizedTank.stockState = MapObjectState.setStockFlag(
                seizedTank.stockState, "ownership", "seized");
        site.stock = 160;
        FactionStrategicPlan capacityPlan = new FactionStrategicPlan();
        capacityPlan.id = "STRAT-VEHICLE-CAPACITY";
        capacityPlan.faction = Faction.MECHANICUS;
        capacityPlan.phase = "EXECUTION";
        capacityPlan.immediateGoal =
                FactionVehicleStrategicAuthority.VEHICLE_SALVAGE_GOAL;
        FactionStrategicAssetAuthority.Outcome capacity =
                FactionVehicleStrategicAuthority.attempt(game, capacityPlan, site);
        require(!capacity.success()
                        && "vehicle-salvage-stock-capacity".equals(capacity.blocker())
                        && !"salvaged".equals(MapObjectState.stockValue(
                        seizedTank.stockState, "condition")),
                "full faction stock must block salvage before destroying the vehicle");
    }

    private static void verifyCatalog() {
        require(VehicleRuntimeAuthority.catalog().size() == 5,
                "vehicle taxonomy should expose five stable runtime classes");
        for (VehicleRuntimeAuthority.VehicleClass vehicleClass
                : VehicleRuntimeAuthority.catalog()) {
            require(!vehicleClass.type.isBlank() && !vehicleClass.label.isBlank()
                            && vehicleClass.seats > 0
                            && vehicleClass.crewRequired > 0
                            && vehicleClass.purchasePrice > 0
                            && vehicleClass.salvageBase > 0
                            && !vehicleClass.legalClass.isBlank()
                            && !vehicleClass.manufacturers.isEmpty()
                            && !vehicleClass.models.isEmpty(),
                    "vehicle class is structurally incomplete: " + vehicleClass);
        }
    }

    private static World world() {
        World world = new World(60106L, 24, 14);
        world.sectorX = 1;
        world.sectorY = 1;
        world.zoneX = 2;
        world.zoneY = 2;
        world.floor = 4;
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.npcs.clear();
        world.mapObjects.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) world.tiles[x][y] = '.';
        }
        return world;
    }

    private static MapObjectState frontage(World world, int x, int y,
                                           String type, String label) {
        MapObjectState object = new MapObjectState();
        object.id = "VEHICLE-FRONTAGE-SMOKE-" + x + "-" + y;
        object.type = type;
        object.label = label;
        object.glyph = 'T';
        object.x = x;
        object.y = y;
        object.stockState = "ownerFaction=" + Faction.CIVIC_LEDGER_OFFICE.name();
        return object;
    }

    private static MapObjectState vehicle(World world, int x, int y, String type,
                                          Faction faction, String ownership,
                                          String role, boolean forSale, long seed) {
        RoadTransitFixtureAuthority.VehicleProfile profile =
                new RoadTransitFixtureAuthority.VehicleProfile(type,
                        RoadTransitFixtureAuthority.labelForType(type,
                                world.zoneType, new Random(seed)),
                        RoadTransitFixtureAuthority.stockForType(type, '.'));
        MapObjectState vehicle = RoadTransitFixtureAuthority.newVehicleFixture(
                world, x, y, profile, new Random(seed), "VEHICLE-SMOKE");
        vehicle.stockState = MapObjectState.setStockFlag(
                vehicle.stockState, "ownerFaction",
                (faction == null ? Faction.NONE : faction).name());
        vehicle.stockState = MapObjectState.setStockFlag(
                vehicle.stockState, "ownership", ownership);
        vehicle.stockState = MapObjectState.setStockFlag(
                vehicle.stockState, "vehicleRole", role);
        vehicle.stockState = MapObjectState.setStockFlag(
                vehicle.stockState, "forSale", Boolean.toString(forSale));
        VehicleRuntimeAuthority.initialize(world, vehicle,
                faction == null ? Faction.NONE : faction,
                ownership, role, forSale, new Random(seed));
        return vehicle;
    }

    private static void preparePlanning(FactionStrategicPlan plan, String goal) {
        plan.phase = "PLANNING";
        plan.immediateGoal = goal;
        plan.phaseUntilTurn = Integer.MAX_VALUE;
        plan.nextDecisionTurn = Integer.MAX_VALUE;
    }

    private static void prepareExecution(FactionStrategicPlan plan, int turn) {
        plan.phase = "EXECUTION";
        plan.phaseUntilTurn = turn;
        plan.nextDecisionTurn = turn;
    }

    private static int component(MapObjectState vehicle,
                                 VehicleRuntimeAuthority.Component component) {
        String key = "vehicleComponent" + component.name().charAt(0)
                + component.name().substring(1).toLowerCase(java.util.Locale.ROOT);
        try {
            return Integer.parseInt(MapObjectState.stockValue(vehicle.stockState, key));
        } catch (Exception ignored) {
            return -1;
        }
    }

    private static boolean hasTitle(GamePanel game,
                                    VehicleRuntimeAuthority.Snapshot snapshot) {
        String expected = "Vehicle title: " + snapshot.manufacturer()
                + " " + snapshot.model();
        return game.inventory.contains(expected);
    }

    private static int count(List<String> items, String name) {
        int count = 0;
        for (String item : items) if (ItemQuality.namesMatch(item, name)) count++;
        return count;
    }

    private static boolean hasFactionStockMachinePart(GamePanel game,
                                                       NpcFactionSite site) {
        String containerId = game.factionStockContainerId(site);
        ContainerRecord container = game.itemContainers.get(containerId);
        if (container == null) return false;
        for (String itemId : container.itemInstanceIds) {
            ItemInstance instance = game.itemInstances.get(itemId);
            if (instance != null && ItemQuality.namesMatch(
                    instance.displayName, "Machine part")
                    && instance.provenance != null
                    && instance.provenance.shortChain().contains(site.name)) return true;
        }
        return false;
    }

    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected),
                "Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record PlayerState(int script, int supplies, int machineParts,
                               int heat, int suspicion, List<String> inventory) {
        static PlayerState capture(GamePanel game) {
            return new PlayerState(game.carriedScript, game.supplies,
                    game.machineParts, game.gangHeat, game.suspicion,
                    new ArrayList<>(game.inventory));
        }

        void requireSame(GamePanel game) {
            require(script == game.carriedScript
                            && supplies == game.supplies
                            && machineParts == game.machineParts
                            && heat == game.gangHeat
                            && suspicion == game.suspicion
                            && inventory.equals(game.inventory),
                    "background faction vehicle operations must not mutate player resources or attention");
        }
    }

    private Milestone06VehicleRuntimeFoundationSmoke() { }
}
