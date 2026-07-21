package mechanist;

import java.util.List;
import java.util.Random;

/** Focused smoke for vehicle balance-of-power, deterrence, escalation, and contracts. */
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

            verifyVehicleContractOutcomes(game, mechanist, cargo, guardTank);
            System.out.println("Milestone 06 faction vehicle balance and contract smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
        Milestone06FactionVehicleRouteControlSmoke.main(args);
        Milestone06FactionVehicleStrategicRouteControlSmoke.main(args);
    }

    private static void verifyVehicleContractOutcomes(
            GamePanel game, NpcFactionSite mechanist,
            MapObjectState cargo, MapObjectState previouslyCapturedTank) {
        clearCompetingContractSources(game);
        VehicleRuntimeAuthority.applyDamage(cargo,
                VehicleRuntimeAuthority.Component.POWERPLANT,
                90, game.turn, "contract smoke engine damage");
        cargo.stockState = MapObjectState.setStockFlag(cargo.stockState,
                "operationState", "disabled");
        cargo.stockState = MapObjectState.setStockFlag(cargo.stockState,
                "roadObstacle", "true");
        cargo.stockState = MapObjectState.setStockFlag(cargo.stockState,
                "fuelOrPowerLeak", "true");
        cargo.stockState = MapObjectState.setStockFlag(cargo.stockState,
                "hazardState", "fuel leak");
        cargo.stockState = MapObjectState.setStockFlag(cargo.stockState,
                "burnedOut", "true");
        cargo.stockState = MapObjectState.setStockFlag(cargo.stockState,
                "lossOutcomeTags", "road obstruction~fuel leak~burned out");
        cargo.stockState = MapObjectState.setStockFlag(cargo.stockState,
                "lossHistory", "catastrophic logistics loss retained for audit");
        VehicleRuntimeAuthority.Snapshot damaged =
                VehicleRuntimeAuthority.inspect(game.world, cargo);
        int powerplantBefore = damaged.components().get(
                VehicleRuntimeAuthority.Component.POWERPLANT);
        NpcEntity representative = representative(mechanist.faction);

        String generatedRepairOffer =
                FactionMarketContractAuthority.representativeLine(
                        game, representative);
        require(generatedRepairOffer.contains("damaged vehicle repair backlog")
                        && generatedRepairOffer.contains(damaged.model()),
                "live faction work should offer the most damaged retainable vehicle repair: "
                        + generatedRepairOffer);
        FactionMarketContractAuthority.WorkResult acceptedRepair =
                FactionMarketContractAuthority.accept(game, representative);
        FactionContract repair = acceptedRepair.contract();
        require(acceptedRepair.success() && repair != null
                        && "MARKET".equals(repair.type)
                        && repair.targetEntityId.startsWith(
                        "MARKET:VEHICLE_REPAIR:")
                        && repair.targetEntityId.endsWith(cargo.id)
                        && ItemCatalog.get(repair.requiredTurnInItem) != null,
                "accepted vehicle repair work should retain the exact persistent vehicle identity and catalog-backed proof: "
                        + acceptedRepair.message());
        carryContractProof(game, repair);
        String repairPreview = ContractTurnInAuthority.representativeLine(
                game, representative);
        require(repairPreview.contains("Vehicle outcome: completion will repair")
                        && repairPreview.contains(damaged.model()),
                "vehicle repair contract should preview its exact persistent target: "
                        + repairPreview);

        int scriptBeforeRepair = game.carriedScript;
        int standingBeforeRepair = game.factionStanding.getOrDefault(
                mechanist.faction, 0);
        ContractTurnInAuthority.TurnInResult repairResult =
                ContractTurnInAuthority.turnInFirst(game, representative);
        VehicleRuntimeAuthority.Snapshot repaired =
                VehicleRuntimeAuthority.inspect(game.world, cargo);
        int powerplantAfter = repaired.components().get(
                VehicleRuntimeAuthority.Component.POWERPLANT);
        String recoveredHistory = MapObjectState.stockValue(cargo.stockState,
                "lossHistory");
        require(repairResult.success() && repair.completed
                        && powerplantAfter > powerplantBefore
                        && powerplantAfter <= 100
                        && !game.inventory.contains(repair.requiredTurnInItem)
                        && game.carriedScript == scriptBeforeRepair + repair.payout
                        && game.factionStanding.getOrDefault(
                        mechanist.faction, 0)
                        == standingBeforeRepair + repair.repReward,
                "generated vehicle repair turn-in should atomically consume proof, repair the target, and preserve ordinary rewards: "
                        + repairResult.message());
        require("parked".equals(MapObjectState.stockValue(
                        cargo.stockState, "operationState"))
                        && "false".equals(MapObjectState.stockValue(
                        cargo.stockState, "roadObstacle"))
                        && "false".equals(MapObjectState.stockValue(
                        cargo.stockState, "fuelOrPowerLeak"))
                        && "none".equals(MapObjectState.stockValue(
                        cargo.stockState, "hazardState"))
                        && "false".equals(MapObjectState.stockValue(
                        cargo.stockState, "burnedOut"))
                        && "recovered".equals(MapObjectState.stockValue(
                        cargo.stockState, "lossRecoveryState"))
                        && recoveredHistory.contains(
                        "catastrophic logistics loss retained for audit")
                        && recoveredHistory.contains(
                        "active loss hazards cleared"),
                "faction repair should clear active loss hazards while retaining historical loss records");
        require(repairResult.message().contains("Vehicle contract outcome:")
                        && repairResult.message().contains(damaged.model())
                        && repairResult.message().contains(
                        "Motor-pool repair materials were accepted")
                        && repairResult.message().contains(
                        "Critical systems returned to service"),
                "vehicle repair completion should explain market, physical, and loss-recovery outcomes: "
                        + repairResult.message());

        verifyVehicleFuelContract(game, mechanist, cargo, representative);

        MapObjectState salvageTarget = vehicle(game.world, 14, 8,
                AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR,
                Faction.IMPERIAL_GUARD, "captured patrol vehicle", 395L);
        game.world.mapObjects.add(salvageTarget);
        require(VehicleRuntimeAuthority.transferToFaction(salvageTarget,
                mechanist.faction, game.turn,
                "contract smoke capture").success(),
                "salvage fixture should become a seized faction vehicle");
        for (VehicleRuntimeAuthority.Component component
                : VehicleRuntimeAuthority.Component.values()) {
            VehicleRuntimeAuthority.applyDamage(salvageTarget, component,
                    100, game.turn,
                    "contract smoke catastrophic wreck damage");
        }
        VehicleRuntimeAuthority.Snapshot salvageBefore =
                VehicleRuntimeAuthority.inspect(game.world, salvageTarget);
        FactionVehicleDoctrineAuthority.VehicleAssessment salvageAssessment =
                FactionVehicleDoctrineAuthority.assess(game, salvageTarget,
                        null, mechanist.faction);
        require(salvageAssessment.salvageRecommended(),
                "catastrophic seized fixture should be doctrine-approved for salvage: "
                        + salvageAssessment.reasons());
        String otherCapturedBefore = previouslyCapturedTank.stockState;

        game.factionContracts.clear();
        game.inventory.clear();
        String generatedSalvageOffer =
                FactionMarketContractAuthority.representativeLine(
                        game, representative);
        require(generatedSalvageOffer.contains(
                        "seized vehicle salvage backlog")
                        && generatedSalvageOffer.contains(
                        salvageBefore.model()),
                "live faction work should prioritize doctrine-approved vehicle salvage: "
                        + generatedSalvageOffer);
        FactionMarketContractAuthority.WorkResult acceptedSalvage =
                FactionMarketContractAuthority.accept(game, representative);
        FactionContract salvage = acceptedSalvage.contract();
        require(acceptedSalvage.success() && salvage != null
                        && salvage.targetEntityId.startsWith(
                        "MARKET:VEHICLE_SALVAGE:")
                        && salvage.targetEntityId.endsWith(
                        salvageTarget.id)
                        && ItemCatalog.get(salvage.requiredTurnInItem) != null,
                "accepted vehicle salvage work should retain the exact persistent vehicle identity and catalog-backed proof: "
                        + acceptedSalvage.message());
        carryContractProof(game, salvage);
        String salvagePreview = ContractTurnInAuthority.representativeLine(
                game, representative);
        require(salvagePreview.contains(
                        "Vehicle outcome: completion will salvage")
                        && salvagePreview.contains(salvageBefore.model()),
                "vehicle salvage contract should select the named seized vehicle: "
                        + salvagePreview);

        ContractTurnInAuthority.TurnInResult salvageResult =
                ContractTurnInAuthority.turnInFirst(game, representative);
        VehicleRuntimeAuthority.Snapshot salvageAfter =
                VehicleRuntimeAuthority.inspect(game.world, salvageTarget);
        require(salvageResult.success() && salvage.completed
                        && "salvaged".equals(salvageAfter.condition())
                        && otherCapturedBefore.equals(
                        previouslyCapturedTank.stockState)
                        && !game.inventory.contains(
                        salvage.requiredTurnInItem),
                "generated vehicle salvage contract should strip only the named target and consume one proof item: "
                        + salvageResult.message());
        require(salvageResult.message().contains(
                        "Vehicle contract outcome:")
                        && salvageResult.message().contains(
                        salvageBefore.model())
                        && salvageResult.message().contains(
                        "Motor-pool salvage authorization was accepted"),
                "vehicle salvage completion should explain both market and physical outcomes: "
                        + salvageResult.message());

        String cargoBeforeOrdinary = cargo.stockState;
        String salvageBeforeOrdinary = salvageTarget.stockState;
        FactionContract ordinary = contract("CONTRACT-ORDINARY-EVIDENCE",
                mechanist.faction,
                "Return sealed evidence from a local investigation.",
                "Sealed evidence parcel");
        prepareContract(game, ordinary);
        String ordinaryPreview = ContractTurnInAuthority.representativeLine(
                game, representative);
        require(!ordinaryPreview.contains("Vehicle outcome:"),
                "ordinary contracts must not advertise vehicle mutations: "
                        + ordinaryPreview);
        ContractTurnInAuthority.TurnInResult ordinaryResult =
                ContractTurnInAuthority.turnInFirst(game, representative);
        require(ordinaryResult.success() && ordinary.completed
                        && cargoBeforeOrdinary.equals(cargo.stockState)
                        && salvageBeforeOrdinary.equals(
                        salvageTarget.stockState)
                        && !ordinaryResult.message().contains(
                        "Vehicle contract outcome"),
                "ordinary contract completion must leave vehicle state untouched");

        String cargoBeforeMissing = cargo.stockState;
        String tankBeforeMissing = previouslyCapturedTank.stockState;
        FactionContract missingTarget = contract(
                "CONTRACT-VEHICLE-MISSING-TARGET",
                Faction.CIVIC_WARDENS,
                "Repair the damaged vehicle engine for the civic motor pool.",
                "Civic vehicle machine part");
        prepareContract(game, missingTarget);
        NpcEntity civicRepresentative = representative(
                Faction.CIVIC_WARDENS);
        ContractTurnInAuthority.TurnInResult missingResult =
                ContractTurnInAuthority.turnInFirst(game,
                        civicRepresentative);
        require(!missingResult.success() && !missingTarget.completed
                        && game.inventory.contains(
                        missingTarget.requiredTurnInItem)
                        && cargoBeforeMissing.equals(cargo.stockState)
                        && tankBeforeMissing.equals(
                        previouslyCapturedTank.stockState)
                        && missingResult.message().contains(
                        "no eligible local damaged faction vehicle"),
                "missing vehicle targets must block before proof consumption or unrelated mutation: "
                        + missingResult.message());
    }

    private static void verifyVehicleFuelContract(
            GamePanel game, NpcFactionSite mechanist, MapObjectState cargo,
            NpcEntity representative) {
        MapObjectState fuelTarget = vehicle(game.world, 18, 8,
                AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                mechanist.faction, "low-energy reserve hauler", 396L);
        game.world.mapObjects.add(fuelTarget);
        VehicleFuelAuthority.ensureInitialized(game.world, fuelTarget);
        fuelTarget.stockState = MapObjectState.setStockFlag(
                fuelTarget.stockState, "fuelOrPowerCurrent", "0");
        VehicleFuelAuthority.Snapshot fuelBefore =
                VehicleFuelAuthority.inspect(game.world, fuelTarget);
        String cargoBeforeFuel = cargo.stockState;

        game.factionContracts.clear();
        game.inventory.clear();
        String offer = FactionMarketContractAuthority.representativeLine(
                game, representative);
        VehicleRuntimeAuthority.Snapshot targetRuntime =
                VehicleRuntimeAuthority.inspect(game.world, fuelTarget);
        require(offer.contains("empty or low vehicle fuel or power reserve")
                        && offer.contains(targetRuntime.model()),
                "live faction work should prioritize the largest critical vehicle energy deficit: "
                        + offer);

        FactionMarketContractAuthority.WorkResult accepted =
                FactionMarketContractAuthority.accept(game, representative);
        FactionContract contract = accepted.contract();
        require(accepted.success() && contract != null
                        && contract.targetEntityId.startsWith(
                        "MARKET:VEHICLE_FUEL:")
                        && contract.targetEntityId.endsWith(fuelTarget.id)
                        && ItemCatalog.get(contract.requiredTurnInItem) != null,
                "accepted fuel support work should bind the exact vehicle and require catalog-backed proof: "
                        + accepted.message());
        carryContractProof(game, contract);
        String preview = ContractTurnInAuthority.representativeLine(
                game, representative);
        require(preview.contains("Vehicle outcome: completion will refuel")
                        && preview.contains(targetRuntime.model()),
                "fuel support turn-in should preview the exact persistent target: "
                        + preview);

        int scriptBefore = game.carriedScript;
        int standingBefore = game.factionStanding.getOrDefault(
                mechanist.faction, 0);
        ContractTurnInAuthority.TurnInResult result =
                ContractTurnInAuthority.turnInFirst(game, representative);
        VehicleFuelAuthority.Snapshot fuelAfter =
                VehicleFuelAuthority.inspect(game.world, fuelTarget);
        String history = MapObjectState.stockValue(fuelTarget.stockState,
                "fuelOrPowerHistory");
        require(result.success() && contract.completed
                        && fuelAfter.current() > fuelBefore.current()
                        && fuelAfter.current() <= fuelAfter.capacity()
                        && fuelAfter.current()
                        == Math.min(fuelBefore.capacity(), 16)
                        && !game.inventory.contains(
                        contract.requiredTurnInItem)
                        && game.carriedScript == scriptBefore + contract.payout
                        && game.factionStanding.getOrDefault(
                        mechanist.faction, 0)
                        == standingBefore + contract.repReward
                        && cargoBeforeFuel.equals(cargo.stockState),
                "generated fuel support should atomically increase only the bound ledger and preserve ordinary rewards: "
                        + result.message());
        require(history.contains(contract.id)
                        && history.contains(mechanist.faction.label)
                        && result.message().contains(
                        "Motor-pool fuel or power supplies were accepted")
                        && result.message().contains(
                        "Vehicle contract outcome: VEHICLE ENERGY: added"),
                "fuel support should retain faction, contract, market, and ledger evidence: "
                        + result.message() + " / " + history);
    }

    private static void clearCompetingContractSources(GamePanel game) {
        game.factionContracts.clear();
        game.inventory.clear();
        game.factionMarketPressure.clear();
        game.world.topDownWorldEvents.clear();
        game.world.shipmentRecords.clear();
        game.world.replacementQueue.clear();
        game.world.essentialSupplyReserves.clear();
        game.world.rawMaterialSupplyReserves.clear();
    }

    private static void carryContractProof(GamePanel game,
                                           FactionContract contract) {
        game.inventory.clear();
        game.inventory.add(contract.requiredTurnInItem);
        satisfyContractProof(game, contract);
    }

    private static void prepareContract(GamePanel game,
                                        FactionContract contract) {
        game.factionContracts.clear();
        game.inventory.clear();
        game.factionContracts.add(contract);
        game.inventory.add(contract.requiredTurnInItem);
        satisfyContractProof(game, contract);
    }

    private static void satisfyContractProof(GamePanel game,
                                             FactionContract contract) {
        game.unlockedSkillNodes.addAll(
                ContractObjectiveReadabilityAuthority.requiredCapabilityKeys(
                        contract));
        game.unlockedKnowledges.addAll(
                ContractObjectiveReadabilityAuthority.requiredKnowledgeNames(
                        contract));
    }

    private static FactionContract contract(String id, Faction faction,
                                             String description,
                                             String proofItem) {
        FactionContract contract = new FactionContract();
        contract.id = id;
        contract.type = "FETCH";
        contract.faction = faction;
        contract.targetName = description;
        contract.requiredTurnInItem = proofItem;
        contract.description = description;
        contract.payout = 95;
        contract.repReward = 2;
        contract.skillXpReward = 4;
        contract.spawned = true;
        return contract;
    }

    private static NpcEntity representative(Faction faction) {
        NpcEntity representative = new NpcEntity();
        representative.id = "VEHICLE-CONTRACT-REP-" + faction.name();
        representative.name = faction.label + " Motor Pool Clerk";
        representative.role = "Faction Representative";
        representative.faction = faction;
        representative.hp = 10;
        return representative;
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
