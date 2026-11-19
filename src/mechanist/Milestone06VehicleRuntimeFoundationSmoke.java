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
            game.active = Candidate.random(new Random(60106L));
            game.active.name = "Mechanist Vehicle Tester";
            game.seed = 60106L;
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
            String trueOwnerId = SaveEfficiencyAuthority.playerIdFor(game);
            require(trueOwnerId.equals(MapObjectState.stockValue(saleCar.stockState, "ownerPlayerId"))
                            && VehicleRuntimeAuthority.playerOwns(game, saleCar),
                    "newly purchased title must bind to the current character identity");
            Candidate savedOwner = game.active;
            Candidate sameNameDifferentCharacter = Candidate.random(new Random(60107L));
            sameNameDifferentCharacter.name = savedOwner.name;
            sameNameDifferentCharacter.job = savedOwner.job;
            sameNameDifferentCharacter.birthWorldTurn = savedOwner.birthWorldTurn + 1;
            game.active = sameNameDifferentCharacter;
            String purchasedStock = saleCar.stockState;
            int otherScript = game.carriedScript;
            int otherParts = count(game.inventory, "Machine part");
            game.playerX = saleCar.x - 1;
            game.playerY = saleCar.y;
            require(!VehicleRuntimeAuthority.playerOwns(game, saleCar),
                    "same display name must not grant ownership to a different character");
            require(!VehicleAccessAuthority.evaluate(game, saleCar,
                            VehicleAccessAuthority.Permission.OPERATION).allowed(),
                    "unrelated character must not drive the player's titled car");
            require(!VehicleRuntimeAuthority.salvageForPlayer(game, saleCar).success()
                            && purchasedStock.equals(saleCar.stockState)
                            && game.carriedScript == otherScript
                            && count(game.inventory, "Machine part") == otherParts,
                    "other character cannot strip a titled vehicle or receive resources");
            game.active = savedOwner;
            require(VehicleRuntimeAuthority.playerOwns(game, saleCar),
                    "original character must regain ownership without retitling");
            MapObjectState legacy = vehicle(game.world, 19, 2,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    Faction.NONE, "player-owned", "legacy-title", false, 29L);
            legacy.stockState = MapObjectState.setStockFlag(legacy.stockState, "ownerType", "PLAYER");
            legacy.stockState = MapObjectState.setStockFlag(legacy.stockState, "ownerName", savedOwner.name);
            legacy.stockState = MapObjectState.setStockFlag(legacy.stockState, "ownerPlayerId", "");
            require(VehicleRuntimeAuthority.playerOwns(game, legacy),
                    "legacy same-name vehicle should remain accessible to its original character");
            game.active = sameNameDifferentCharacter;
            require(VehicleRuntimeAuthority.playerOwns(game, legacy),
                    "legacy name-only vehicle must preserve compatible legacy ownership behavior");
            game.active = savedOwner;
            require(game.gangHeat > 0,
                    "vehicle acquisition should create class-scaled expansion attention");

            // A spent dealership offer must not tick time, award XP, or rotate
            // the vendor on a refused purchase.
            game.playerX = dealership.x - 1;
            game.playerY = dealership.y;
            int refusedBuyTurn = game.turn;
            long refusedBuyWorldTurn = game.worldTurn;
            int refusedBuyXp = game.xp;
            int refusedBuyVend = dealership.vendCount;
            int refusedBuyCooldown = dealership.cooldownUntilTurn;
            int refusedBuyScript = game.carriedScript;
            require(RoadTransitFixtureInteractionAuthority.tryInteract(game, dealership.x, dealership.y),
                    "depleted dealership should remain an addressable physical fixture");
            require(game.turn == refusedBuyTurn && game.worldTurn == refusedBuyWorldTurn
                            && game.xp == refusedBuyXp
                            && dealership.vendCount == refusedBuyVend
                            && dealership.cooldownUntilTurn == refusedBuyCooldown
                            && game.carriedScript == refusedBuyScript,
                    "refused vehicle purchase must not spend time, XP, money, or fixture cooldown");

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
            game.playerX = 0;
            game.playerY = 0;
            String abandonedBeforeDistantClaim = abandonedBike.stockState;
            require(!VehicleRuntimeAuthority.claimAbandoned(game, abandonedBike).success()
                            && abandonedBeforeDistantClaim.equals(abandonedBike.stockState),
                    "remote abandoned vehicle cannot be claimed or silently retitled");
            game.playerX = abandonedBike.x - 1;
            game.playerY = abandonedBike.y;
            VehicleRuntimeAuthority.Result claim =
                    VehicleRuntimeAuthority.claimAbandoned(game, abandonedBike);
            require(claim.success() && VehicleRuntimeAuthority.playerOwns(game, abandonedBike)
                            && trueOwnerId.equals(MapObjectState.stockValue(
                                    abandonedBike.stockState, "ownerPlayerId")),
                    "legal abandoned civilian vehicle should record the current player's identity");
            require(game.gangHeat > heatBeforeClaim,
                    "abandoned vehicle claim should still add visible fleet attention");

            VehicleRuntimeAuthority.applyDamage(wreck,
                    VehicleRuntimeAuthority.Component.FRAME, 100,
                    game.turn, "smoke catastrophic frame damage");
            require("wreck".equals(MapObjectState.stockValue(wreck.stockState, "condition")),
                    "zero frame integrity should create a persistent wreck condition");
            int partsBefore = count(game.inventory, "Machine part");
            String wreckBeforeDistantSalvage = wreck.stockState;
            game.playerX = 0;
            game.playerY = 0;
            require(!VehicleRuntimeAuthority.salvageForPlayer(game, wreck).success()
                            && wreckBeforeDistantSalvage.equals(wreck.stockState)
                            && count(game.inventory, "Machine part") == partsBefore,
                    "remote salvage must leave the hulk and player resources unchanged");

            MapObjectState unlisted = vehicle(game.world, 14, 2,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    Faction.NONE, "abandoned", "unlisted", false, 23L);
            game.playerX = unlisted.x - 1;
            game.playerY = unlisted.y;
            String unlistedStock = unlisted.stockState;
            require(!VehicleRuntimeAuthority.salvageForPlayer(game, unlisted).success()
                            && unlistedStock.equals(unlisted.stockState)
                            && count(game.inventory, "Machine part") == partsBefore,
                    "detached vehicle references must not materialize salvage resources");

            MapObjectState titledWreck = vehicle(game.world, 12, 2,
                    AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                    Faction.NOBLE, "faction", "patrol", false, 25L);
            game.world.mapObjects.add(titledWreck);
            VehicleRuntimeAuthority.applyDamage(titledWreck,
                    VehicleRuntimeAuthority.Component.FRAME, 100,
                    game.turn, "smoke titled wreck damage");
            game.playerX = titledWreck.x - 1;
            game.playerY = titledWreck.y;
            String titledWreckStock = titledWreck.stockState;
            int titledWreckTurn = game.turn;
            long titledWreckWorldTurn = game.worldTurn;
            int titledWreckXp = game.xp;
            int titledWreckVend = titledWreck.vendCount;
            int titledWreckCooldown = titledWreck.cooldownUntilTurn;
            require(RoadTransitFixtureInteractionAuthority.tryInteract(game, titledWreck.x, titledWreck.y),
                    "faction-controlled wreck should remain inspectable");
            require(titledWreckStock.equals(titledWreck.stockState)
                            && game.turn == titledWreckTurn
                            && game.worldTurn == titledWreckWorldTurn
                            && game.xp == titledWreckXp
                            && titledWreck.vendCount == titledWreckVend
                            && titledWreck.cooldownUntilTurn == titledWreckCooldown
                            && count(game.inventory, "Machine part") == partsBefore,
                    "refused faction-wreck salvage must preserve custody, inventory, time, XP, and cooldown");
            require(!VehicleRuntimeAuthority.salvageForPlayer(game, titledWreck).success(),
                    "direct salvage cannot bypass faction wreck title");

            MapObjectState restrictedWreck = vehicle(game.world, 16, 2,
                    AssetIntegrationDisciplineAuthority.PARKED_TANK,
                    Faction.NONE, "abandoned", "restricted", false, 26L);
            game.world.mapObjects.add(restrictedWreck);
            VehicleRuntimeAuthority.applyDamage(restrictedWreck,
                    VehicleRuntimeAuthority.Component.FRAME, 100,
                    game.turn, "smoke restricted wreck damage");
            game.playerX = restrictedWreck.x - 1;
            game.playerY = restrictedWreck.y;
            String restrictedStock = restrictedWreck.stockState;
            require(!VehicleRuntimeAuthority.salvageForPlayer(game, restrictedWreck).success()
                            && restrictedStock.equals(restrictedWreck.stockState)
                            && count(game.inventory, "Machine part") == partsBefore,
                    "restricted abandoned military wreck must not bypass claim controls");

            game.playerX = wreck.x - 1;
            game.playerY = wreck.y;
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
                            && trueOwnerId.equals(MapObjectState.stockValue(restored.stockState, "ownerPlayerId"))
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
        // This deterministic scheme is a rare successful underdog diversion.
        // The competing loss path is covered by FactionVehicleBalanceSmoke.
        plan.id = "STRAT-VEHICLE-SMOKE-3";
        plan.faction = Faction.MECHANICUS;
        plan.schemeTargetFaction = Faction.NOBLE;
        plan.phase = "EXECUTION";
        plan.immediateGoal = FactionVehicleStrategicAuthority.VEHICLE_SEIZURE_GOAL;
        plan.scheme = "divert faction stock";
        plan.secrecy = 60;
        plan.aggression = 95;
        plan.ambition = 55;
        plan.phaseUntilTurn = game.turn;
        plan.nextDecisionTurn = game.turn;
        game.factionStrategicPlans.add(plan);

        PlayerState playerBefore = PlayerState.capture(game);
        FactionVehicleBalanceAuthority.Contest contest =
                FactionVehicleBalanceAuthority.compare(game, site.faction, Faction.NOBLE, site);
        require(contest.canEscalate(plan.aggression, plan.ambition),
                "committed vehicle operation must meet actual fleet escalation threshold: "
                        + contest.summary());
        int stockBeforeSeizure = site.stock;
        int resolved = FactionStrategicAssetTickAuthority.tick(game);
        require(resolved == 1 && "COOLDOWN".equals(plan.phase)
                        && plan.success == 1 && plan.failure == 0,
                "a lucky underdog diversion must still be able to seize a physical vehicle: "
                        + "resolved=" + resolved + " phase=" + plan.phase
                        + " success=" + plan.success + " failure=" + plan.failure
                        + " outcome=" + plan.lastOutcome);
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
