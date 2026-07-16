package mechanist;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/** Focused smoke for live Phase 17.2 faction blueprint requisition and site upgrades. */
final class Milestone05FactionBlueprintSiteUpgradeSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            BuildRecipe recipe = BuildRecipe.microForge();
            String blueprintId = ConstructionBlueprintOwnershipAuthority.blueprintId(recipe);
            int cost = FactionFacilityBlueprintUpgradeAuthority.aggregateStockCost(recipe);
            require(cost == 12, "EMM Micro Forge aggregate stock cost should be 4 supplies + 3 parts + 5 components");

            PlayerSnapshot playerBefore = PlayerSnapshot.capture(game);

            NpcFactionSite ignoredSite = site(Faction.MECHANIST_COLLEGIA, cost);
            SiteSnapshot ignoredBefore = SiteSnapshot.capture(ignoredSite);
            FactionFacilityBlueprintUpgradeAuthority.Outcome ignored =
                    FactionFacilityBlueprintUpgradeAuthority.attempt(game,
                            plan(Faction.MECHANICUS, "repair damaged facility"), ignoredSite);
            require(!ignored.handled() && !ignored.success(),
                    "authority should leave non-factory strategic goals to their existing owner");
            requireUnchanged(ignoredSite, ignoredBefore, "non-handled goal");

            configureControlledRoom(game.world);
            configureWorkforce(game.world, 4);
            game.world.npcs.clear();
            NpcEntity worksFactor = vendor("Cloister Works Factor Rho", Faction.MECHANICUS_CLOISTER_RED,
                    FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL, 12);
            game.world.npcs.add(worksFactor);
            NpcFactionSite liveSite = site(Faction.MECHANIST_COLLEGIA, cost * 3);
            FactionStrategicPlan livePlan = plan(Faction.MECHANICUS,
                    FactionFacilityBlueprintUpgradeAuthority.FACTORY_UPGRADE_GOAL);

            FactionFacilityBlueprintUpgradeAuthority.Outcome acquired =
                    FactionFacilityBlueprintUpgradeAuthority.attempt(game, livePlan, liveSite);
            require(acquired.handled() && acquired.success(), "alias-backed first requisition should succeed: " + acquired);
            require(acquired.acquiredBlueprint() && !acquired.reusedKnownBlueprint(),
                    "first requisition should acquire rather than reuse the plan");
            require(blueprintId.equals(acquired.blueprintId()) && "EMM Micro Forge".equals(acquired.blueprintName()),
                    "successful upgrade should identify the stable EMM Micro Forge plan");
            requireContains(acquired.acquisitionSource(), "Cloister Works Factor Rho", "physical acquisition source");
            requireContains(acquired.acquisitionSource(), "Industrial Blueprint Trader", "physical acquisition role");
            require(acquired.effectiveWorkers() == 4 && acquired.materialStockCost() == cost,
                    "successful upgrade should report exact workers and aggregate stock cost");
            require(acquired.stockBefore() == cost * 3 && acquired.stockAfter() == cost * 2,
                    "first upgrade should deduct the exact site stock cost once");
            require(acquired.baseLevelBefore() == 1 && acquired.baseLevelAfter() == 2
                            && acquired.machineLevelBefore() == 2 && acquired.machineLevelAfter() == 3,
                    "first upgrade should advance base and machine levels once");
            require(liveSite.knowsConstructionBlueprint(blueprintId)
                            && liveSite.knownConstructionBlueprints.size() == 1,
                    "first upgrade should record one stable plan ID in the faction-site ledger");
            requireContains(acquired.message(), "requisitioned EMM Micro Forge", "first acquisition outcome");
            requireContains(acquired.message(), "stock " + (cost * 3) + " -> " + (cost * 2),
                    "first acquisition stock readback");
            require(livePlan.success == 0 && livePlan.failure == 0 && livePlan.history.isEmpty(),
                    "direct authority should leave strategic counters/history to its integration caller");

            game.world.npcs.clear();
            FactionFacilityBlueprintUpgradeAuthority.Outcome reused =
                    FactionFacilityBlueprintUpgradeAuthority.attempt(game, livePlan, liveSite);
            require(reused.handled() && reused.success(),
                    "known faction plan should remain usable after its physical vendor leaves the zone: " + reused);
            require(!reused.acquiredBlueprint() && reused.reusedKnownBlueprint(),
                    "repeat upgrade should explicitly reuse the known plan");
            require("known faction plan".equals(reused.acquisitionSource()),
                    "repeat upgrade should name the known-plan acquisition path");
            require(liveSite.knownConstructionBlueprints.size() == 1,
                    "known-plan reuse should not duplicate the stable blueprint ID");
            require(reused.stockBefore() == cost * 2 && reused.stockAfter() == cost,
                    "known-plan upgrade should charge only the same construction stock cost");
            require(liveSite.baseLevel == 3 && liveSite.machineLevel == 4,
                    "known-plan reuse should advance both supported levels once more");
            requireContains(reused.message(), "reused known faction plan EMM Micro Forge",
                    "known-plan outcome");
            requireContains(liveSite.summaryLine(), "construction plans EMM Micro Forge",
                    "inspectable faction-site plan readback");
            require(!liveSite.summaryLine().contains(blueprintId),
                    "ordinary faction-site summary should not expose the stable internal plan ID");

            String siteSaveLine = liveSite.saveLine();
            NpcFactionSite parsedSite = NpcFactionSite.parse(siteSaveLine);
            require(parsedSite != null && parsedSite.knowsConstructionBlueprint(blueprintId),
                    "new faction-site save line should retain the known construction plan");
            require(parsedSite.stock == liveSite.stock && parsedSite.baseLevel == liveSite.baseLevel
                            && parsedSite.machineLevel == liveSite.machineLevel,
                    "new faction-site save line should retain stock and upgraded levels");
            String planLedgerSaveLine = siteSaveLine.substring(0, siteSaveLine.lastIndexOf('|'));
            NpcFactionSite planLedgerSite = NpcFactionSite.parse(planLedgerSaveLine);
            require(planLedgerSite != null && planLedgerSite.knowsConstructionBlueprint(blueprintId)
                            && planLedgerSite.completedConstructionJobs.isEmpty(),
                    "16-field faction sites should retain plans and default the completion receipt ledger empty");
            NpcFactionSite legacySite = NpcFactionSite.parse(
                    planLedgerSaveLine.substring(0, planLedgerSaveLine.lastIndexOf('|')));
            require(legacySite != null && legacySite.knownConstructionBlueprints.isEmpty()
                            && legacySite.completedConstructionJobs.isEmpty(),
                    "legacy 15-field faction sites should still load with empty plan and completion ledgers");

            game.npcFactionSites.clear();
            game.npcFactionSites.add(liveSite);
            Properties saved = new Properties();
            Persistence.writeCore(game, saved);
            GamePanel restoredGame = new GamePanel();
            restoredGame.shutdownRuntime();
            try {
                Persistence.readCore(restoredGame, saved);
                NpcFactionSite restoredSite = null;
                for (NpcFactionSite candidate : restoredGame.npcFactionSites) {
                    if (candidate != null && liveSite.name.equals(candidate.name)
                            && candidate.knowsConstructionBlueprint(blueprintId)) {
                        restoredSite = candidate;
                        break;
                    }
                }
                require(restoredSite != null,
                        "full core persistence should restore the upgraded faction site among "
                                + restoredGame.npcFactionSites.size() + " loaded site(s)");
                require(restoredSite.knowsConstructionBlueprint(blueprintId)
                                && restoredSite.stock == liveSite.stock
                                && restoredSite.baseLevel == liveSite.baseLevel
                                && restoredSite.machineLevel == liveSite.machineLevel,
                        "full core persistence should retain faction plan, stock, and levels");
            } finally {
                restoredGame.shutdownRuntime();
            }

            // Reinstall the deterministic local fixture after the full-core
            // persistence exercise so live integration owns an exact room.
            game.world = world();
            game.playerX = 0;
            game.playerY = 0;
            configureControlledRoom(game.world);
            configureWorkforce(game.world, 4);
            game.world.npcs.clear();
            game.world.npcs.add(vendor("Integration Works Factor", Faction.MECHANIST_COLLEGIA,
                    FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL, 12));
            NpcFactionSite integratedSite = site(Faction.MECHANIST_COLLEGIA, cost);
            FactionStrategicPlan integratedPlan = plan(Faction.MECHANICUS,
                    FactionFacilityBlueprintUpgradeAuthority.FACTORY_UPGRADE_GOAL);
            integratedPlan.id = "plan.phase17.physical-micro-forge";
            game.baseObjects.clear();
            game.npcFactionSites.clear();
            game.npcFactionSites.add(integratedSite);
            game.factionStrategicPlans.clear();
            game.factionStrategicPlans.add(integratedPlan);
            require(game.world.rooms.size() == 1 && game.world.roomIdAt(3, 3) == 0
                            && FactionIdentityAuthority.sameFamily(
                            game.world.roomFaction(0), integratedPlan.faction)
                            && !Boolean.TRUE.equals(game.world.roomSpecials.get(0)),
                    "live physical fixture should expose one exact non-special Mechanist-controlled room: rooms="
                            + game.world.rooms.size() + " room@3,3=" + game.world.roomIdAt(3, 3)
                            + " owner=" + game.world.roomFaction(0) + " plan=" + integratedPlan.faction
                            + " special=" + (game.world.roomSpecials.isEmpty()
                            ? "missing" : game.world.roomSpecials.get(0)));
            int integratedStockBefore = integratedSite.stock;
            int integratedBaseBefore = integratedSite.baseLevel;
            int integratedMachineBefore = integratedSite.machineLevel;
            require(FactionStrategySimulationApi.tryApplyLiveFactoryUpgrade(game, integratedPlan, integratedSite),
                    "Mechanist factory strategy should route through live blueprint construction");
            require(integratedPlan.success == 0 && integratedPlan.failure == 0,
                    "staging physical construction must remain in progress without strategic success or failure: success="
                            + integratedPlan.success + " failure=" + integratedPlan.failure
                            + " outcome=" + integratedPlan.lastOutcome);
            requireContains(integratedPlan.lastOutcome, "IN PROGRESS:",
                    "physical construction staging readback");
            require(!integratedPlan.history.isEmpty()
                            && integratedPlan.history.get(integratedPlan.history.size() - 1)
                            .contains("IN PROGRESS:")
                            && integratedPlan.history.stream().noneMatch(line -> line.contains("SUCCESS:")),
                    "staging history should remain explicitly in progress without a success entry");
            require(integratedSite.stock == integratedStockBefore - cost,
                    "physical staging should debit the exact aggregate faction stock cost once");
            require(integratedSite.baseLevel == integratedBaseBefore
                            && integratedSite.machineLevel == integratedMachineBefore,
                    "physical staging must leave facility levels unchanged until labor completes");
            require(integratedSite.knowsConstructionBlueprint(blueprintId),
                    "physical staging should retain the requisitioned Micro Forge plan on the site");

            BaseObject stagedForge = FactionPhysicalConstructionAuthority.activeSite(game, integratedSite);
            require(stagedForge != null && stagedForge.underConstruction,
                    "live factory strategy should stage one physical unfinished Micro Forge");
            require(FactionPhysicalConstructionAuthority.isFactionManaged(stagedForge)
                            && FactionPhysicalConstructionAuthority.belongsToSite(stagedForge, integratedSite),
                    "staged forge should carry durable faction ownership and its exact faction-site link");
            require("EMM Micro Forge".equals(stagedForge.assignedRecipe)
                            && stagedForge.finalSymbol == recipe.symbol,
                    "staged physical object should retain the Micro Forge recipe and final glyph");
            require(stagedForge.constructionRequiredItems.equals(stagedForge.constructionInsertedItems)
                            && stagedForge.constructionLaborDone == 0
                            && stagedForge.constructionLaborRequired == recipe.baseTurns,
                    "faction stock should prepay every recipe material while labor remains unstarted");
            require(game.world.roomIdAt(stagedForge.x, stagedForge.y) == 0
                            && FactionIdentityAuthority.sameFamily(game.world.roomFaction(0), integratedPlan.faction),
                    "physical forge should be inside the exact same-family controlled room");
            requireContains(stagedForge.assignedWorker, "4 assigned worker(s)",
                    "room-local construction workforce custody");
            requireContains(stagedForge.assignedWorker, "Phase 17 Mechanist Machine Room roster",
                    "exact construction-room roster custody");
            require(game.world.tiles[stagedForge.x][stagedForge.y] == '?',
                    "staged faction construction should reserve its world tile with the unfinished glyph");

            int stagedObjectCount = game.baseObjects.size();
            int stagedHistoryCount = integratedPlan.history.size();
            require(FactionStrategySimulationApi.tryApplyLiveFactoryUpgrade(game, integratedPlan, integratedSite),
                    "repeated live strategy resolution should resume the existing physical site");
            require(FactionPhysicalConstructionAuthority.activeSite(game, integratedSite) == stagedForge
                            && game.baseObjects.size() == stagedObjectCount,
                    "repeat strategy resolution must not create a duplicate Micro Forge site");
            require(integratedSite.stock == integratedStockBefore - cost,
                    "repeat strategy resolution must not debit faction stock a second time");
            require(integratedSite.baseLevel == integratedBaseBefore
                            && integratedSite.machineLevel == integratedMachineBefore
                            && integratedPlan.success == 0 && integratedPlan.failure == 0,
                    "resuming staged work must remain in progress without early levels or strategic success");
            require(integratedPlan.history.size() >= stagedHistoryCount
                            && integratedPlan.history.stream().noneMatch(line -> line.contains("SUCCESS:")),
                    "in-progress resume may add readback history but must not record early success");

            game.turn = GamePanel.TURNS_PER_HOUR;
            game.tickNpcFactionSiteProduction();
            require(stagedForge.underConstruction && stagedForge.constructionLaborDone == 4,
                    "first faction hour should add exactly the four room-local workers as labor");
            require(integratedPlan.success == 0 && integratedSite.baseLevel == integratedBaseBefore
                            && integratedSite.machineLevel == integratedMachineBefore,
                    "partial hourly labor must not record strategic success or facility levels");

            game.turn = 2 * GamePanel.TURNS_PER_HOUR;
            game.tickNpcFactionSiteProduction();
            require(stagedForge.underConstruction && stagedForge.constructionLaborDone == 8,
                    "second faction hour should continue the same physical site to eight labor");
            require(integratedPlan.success == 0 && integratedSite.baseLevel == integratedBaseBefore
                            && integratedSite.machineLevel == integratedMachineBefore,
                    "near-complete physical work must still leave the upgrade pending");

            game.turn = 3 * GamePanel.TURNS_PER_HOUR;
            game.tickNpcFactionSiteProduction();
            require(!stagedForge.underConstruction && stagedForge.symbol == recipe.symbol
                            && stagedForge.constructionLaborDone == recipe.baseTurns,
                    "third faction hour should finish the physical Micro Forge exactly at required labor");
            require(FactionPhysicalConstructionAuthority.activeSite(game, integratedSite) == null,
                    "completed physical construction must leave no unfinished job for the linked site");
            require(integratedPlan.success == 1 && integratedPlan.failure == 0,
                    "physical completion should record exactly one strategic success");
            require(integratedSite.baseLevel == integratedBaseBefore + 1
                            && integratedSite.machineLevel == integratedMachineBefore + 1,
                    "physical completion should advance base and machine levels exactly once");
            require(integratedSite.stock == integratedStockBefore - cost
                            && integratedSite.completedConstructionJobs.size() == 1,
                    "completion should retain the single stock debit and one durable completion receipt");
            requireContains(integratedPlan.lastOutcome, "SUCCESS:",
                    "physical construction completion readback");
            requireContains(integratedPlan.lastOutcome, "completed physical EMM Micro Forge construction",
                    "physical construction completion detail");
            require(!integratedPlan.history.isEmpty()
                            && integratedPlan.history.get(integratedPlan.history.size() - 1).contains("SUCCESS:"),
                    "completed physical upgrade should enter strategic history only after labor finishes");

            NpcFactionSite integratedBlockedSite = site(Faction.MECHANIST_COLLEGIA, cost - 1);
            FactionStrategicPlan integratedBlockedPlan = plan(Faction.MECHANICUS,
                    FactionFacilityBlueprintUpgradeAuthority.FACTORY_UPGRADE_GOAL);
            require(FactionStrategySimulationApi.tryApplyLiveFactoryUpgrade(
                            game, integratedBlockedPlan, integratedBlockedSite),
                    "handled resource blocker should remain in the live factory-upgrade route");
            require(integratedBlockedPlan.success == 0 && integratedBlockedPlan.failure == 1,
                    "resource-blocked factory plan must record failure instead of false success");
            requireContains(integratedBlockedPlan.lastOutcome, "FAILURE:", "strategic blocker outcome");
            requireContains(integratedBlockedPlan.lastOutcome, "requires " + cost + " site material stock",
                    "strategic blocker reason");
            require(integratedBlockedPlan.history.size() == 1
                            && integratedBlockedPlan.history.get(0).contains("FAILURE:"),
                    "resource blocker should enter strategic history");

            NpcFactionSite baselineSite = site(Faction.MECHANIST_COLLEGIA, 0);
            baselineSite.workers = 4;
            NpcFactionSite cadenceSite = site(Faction.MECHANIST_COLLEGIA, cost);
            cadenceSite.workers = 4;
            cadenceSite.learnConstructionBlueprint(blueprintId);
            FactionFacilityBlueprintUpgradeAuthority.Outcome cadenceUpgrade =
                    FactionFacilityBlueprintUpgradeAuthority.attempt(game, livePlan, cadenceSite);
            require(cadenceUpgrade.success(), "known-plan cadence fixture should upgrade successfully");
            int earlierProductionTurn = 2 * GamePanel.TURNS_PER_HOUR;
            require(!baselineSite.produceHour(earlierProductionTurn, game.rng),
                    "level-one baseline site should not produce at the two-hour mark");
            require(cadenceSite.produceHour(earlierProductionTurn, game.rng),
                    "upgraded level-two site should produce at the earlier two-hour mark");

            configureWorkforce(game.world, 3);
            game.world.npcs.clear();
            game.world.npcs.add(vendor("Dead Correct Works Factor", Faction.MECHANIST_COLLEGIA,
                    FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL, 0));
            game.world.npcs.add(vendor("Living Wrong Counter", Faction.MECHANICUS,
                    FactionCriticalVendorPlacementAuthority.Category.MEDICAL, 12));
            game.world.npcs.add(vendor("Living Wrong Faction", Faction.NOBLE,
                    FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL, 12));
            NpcFactionSite vendorBlockedSite = site(Faction.MECHANICUS_CLOISTER_RUST, cost);
            SiteSnapshot vendorBlockedBefore = SiteSnapshot.capture(vendorBlockedSite);
            FactionFacilityBlueprintUpgradeAuthority.Outcome vendorBlocked =
                    FactionFacilityBlueprintUpgradeAuthority.attempt(game,
                            plan(Faction.MECHANIST_COLLEGIA,
                                    FactionFacilityBlueprintUpgradeAuthority.FACTORY_UPGRADE_GOAL),
                            vendorBlockedSite);
            require(!vendorBlocked.success() && "missing-blueprint-vendor".equals(vendorBlocked.blocker()),
                    "dead, wrong-category, and wrong-family vendors must not issue the first plan: " + vendorBlocked);
            requireContains(vendorBlocked.message(), "no living Mechanist Collegia Industrial Blueprint Trader",
                    "missing vendor blocker");
            requireUnchanged(vendorBlockedSite, vendorBlockedBefore, "vendor-blocked first acquisition");

            configureWorkforce(game.world, 0);
            game.world.npcs.clear();
            game.world.npcs.add(vendor("Staffed Blueprint Counter", Faction.MECHANIST_COLLEGIA,
                    FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL, 12));
            NpcFactionSite workforceBlockedSite = site(Faction.MECHANICUS, cost);
            SiteSnapshot workforceBlockedBefore = SiteSnapshot.capture(workforceBlockedSite);
            FactionFacilityBlueprintUpgradeAuthority.Outcome workforceBlocked =
                    FactionFacilityBlueprintUpgradeAuthority.attempt(game,
                            plan(Faction.MECHANIST_COLLEGIA,
                                    FactionFacilityBlueprintUpgradeAuthority.FACTORY_UPGRADE_GOAL),
                            workforceBlockedSite);
            require(!workforceBlocked.success() && "no-workforce".equals(workforceBlocked.blocker())
                            && workforceBlocked.effectiveWorkers() == 0,
                    "matching unstaffed local roster must block construction: " + workforceBlocked);
            requireUnchanged(workforceBlockedSite, workforceBlockedBefore, "workforce-blocked first acquisition");

            configureWorkforce(game.world, 2);
            NpcFactionSite stockBlockedSite = site(Faction.MECHANIST_COLLEGIA, cost - 1);
            SiteSnapshot stockBlockedBefore = SiteSnapshot.capture(stockBlockedSite);
            FactionFacilityBlueprintUpgradeAuthority.Outcome stockBlocked =
                    FactionFacilityBlueprintUpgradeAuthority.attempt(game,
                            plan(Faction.MECHANICUS,
                                    FactionFacilityBlueprintUpgradeAuthority.FACTORY_UPGRADE_GOAL),
                            stockBlockedSite);
            require(!stockBlocked.success() && "insufficient-site-stock".equals(stockBlocked.blocker()),
                    "insufficient site stock must block construction: " + stockBlocked);
            requireContains(stockBlocked.message(), "requires " + cost + " site material stock, has " + (cost - 1),
                    "stock blocker readback");
            requireUnchanged(stockBlockedSite, stockBlockedBefore, "stock-blocked first acquisition");

            NpcFactionSite cappedSite = site(Faction.MECHANIST_COLLEGIA, cost);
            cappedSite.baseLevel = FactionFacilityBlueprintUpgradeAuthority.BASE_LEVEL_CAP;
            cappedSite.machineLevel = FactionFacilityBlueprintUpgradeAuthority.MACHINE_LEVEL_CAP;
            SiteSnapshot cappedBefore = SiteSnapshot.capture(cappedSite);
            FactionFacilityBlueprintUpgradeAuthority.Outcome capped =
                    FactionFacilityBlueprintUpgradeAuthority.attempt(game,
                            plan(Faction.MECHANICUS,
                                    FactionFacilityBlueprintUpgradeAuthority.FACTORY_UPGRADE_GOAL), cappedSite);
            require(!capped.success() && "facility-level-cap".equals(capped.blocker()),
                    "fully capped site must reject an upgrade: " + capped);
            requireUnchanged(cappedSite, cappedBefore, "level-capped first acquisition");

            NpcFactionSite wrongSite = site(Faction.NOBLE, cost);
            SiteSnapshot wrongSiteBefore = SiteSnapshot.capture(wrongSite);
            FactionFacilityBlueprintUpgradeAuthority.Outcome wrongFaction =
                    FactionFacilityBlueprintUpgradeAuthority.attempt(game,
                            plan(Faction.MECHANICUS,
                                    FactionFacilityBlueprintUpgradeAuthority.FACTORY_UPGRADE_GOAL), wrongSite);
            require(!wrongFaction.success() && "wrong-faction-site".equals(wrongFaction.blocker()),
                    "non-Mechanist production site must not use the Micro Forge upgrade path: " + wrongFaction);
            requireUnchanged(wrongSite, wrongSiteBefore, "wrong-faction site");

            playerBefore.requireSame(game);
            System.out.println("Milestone 05 faction blueprint site upgrade smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static World world() {
        World world = new World(170205L, 24, 24);
        world.sectorX = 1;
        world.sectorY = 1;
        world.zoneX = 2;
        world.zoneY = 2;
        world.floor = 4;
        world.npcs.clear();
        world.roomPopulationLedgers.clear();
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) {
            world.tiles[x][y] = '#';
            world.roomIds[x][y] = -1;
        }
        configureControlledRoom(world);
        return world;
    }

    private static void configureControlledRoom(World world) {
        if (world.rooms.isEmpty()) {
            Rectangle room = new Rectangle(2, 2, 12, 12);
            RoomProfile profile = RoomProfile.generic();
            profile.name = "Phase 17 Mechanist Machine Room";
            profile.descriptor = "deterministic exact-room physical construction fixture";
            profile.featureText = "open controlled interior for staged Micro Forge construction";
            profile.faction = Faction.MECHANIST_COLLEGIA;
            world.rooms.add(room);
            world.roomProfiles.add(profile);
            world.roomFactions.add(Faction.MECHANIST_COLLEGIA);
            world.roomSpecials.add(Boolean.FALSE);
            for (int x = room.x; x < room.x + room.width; x++) {
                for (int y = room.y; y < room.y + room.height; y++) {
                    boolean edge = x == room.x || y == room.y
                            || x == room.x + room.width - 1 || y == room.y + room.height - 1;
                    world.tiles[x][y] = edge ? '#' : '.';
                    world.roomIds[x][y] = edge ? -1 : 0;
                }
            }
        }
        world.roomFactions.set(0, Faction.MECHANIST_COLLEGIA);
        world.roomProfiles.get(0).faction = Faction.MECHANIST_COLLEGIA;
        world.roomProfiles.get(0).name = "Phase 17 Mechanist Machine Room";
        world.roomSpecials.set(0, Boolean.FALSE);
    }

    private static void configureWorkforce(World world, int assigned) {
        world.roomPopulationLedgers.clear();
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = "pop.phase17.factory";
        ledger.roomId = 0;
        ledger.roomName = "Mechanist Forge Workforce";
        ledger.faction = Faction.MECHANIST_COLLEGIA;
        ledger.capacity = 8;
        ledger.available = Math.max(0, 8 - assigned);
        ledger.assigned = Math.max(0, assigned);
        world.roomPopulationLedgers.add(ledger);
    }

    private static FactionStrategicPlan plan(Faction faction, String goal) {
        FactionStrategicPlan plan = new FactionStrategicPlan();
        plan.faction = faction;
        plan.immediateGoal = goal;
        plan.targetRoom = "machine room";
        plan.targetItem = "Machine part";
        return plan;
    }

    private static NpcFactionSite site(Faction faction, int stock) {
        NpcFactionSite site = NpcFactionSite.create("Phase 17 Forge Site", faction,
                "machine shop", 1, 1, 2, 2, 4,
                "Machine part", "Tool bundle", "Scrap-Forging Doctrine");
        site.stock = stock;
        site.baseLevel = 1;
        site.machineLevel = 2;
        return site;
    }

    private static NpcEntity vendor(String name, Faction faction,
                                    FactionCriticalVendorPlacementAuthority.Category category, int hp) {
        NpcEntity vendor = new NpcEntity();
        vendor.id = "PHASE17-" + name.replace(' ', '-').toUpperCase(java.util.Locale.ROOT);
        vendor.name = name;
        vendor.faction = faction;
        vendor.role = category.role;
        vendor.state = "Trade";
        vendor.hp = hp;
        vendor.x = 5;
        vendor.y = 5;
        return vendor;
    }

    private static void requireUnchanged(NpcFactionSite site, SiteSnapshot before, String label) {
        SiteSnapshot after = SiteSnapshot.capture(site);
        require(before.equals(after), label + " mutated faction-site state: before=" + before + " after=" + after);
    }

    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected), label + " missing '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record SiteSnapshot(int stock, int baseLevel, int machineLevel, Set<String> blueprints) {
        static SiteSnapshot capture(NpcFactionSite site) {
            return new SiteSnapshot(site.stock, site.baseLevel, site.machineLevel,
                    new LinkedHashSet<>(site.knownConstructionBlueprints));
        }
    }

    private record PlayerSnapshot(int carriedScript, int bankedScript, int supplies, int machineParts,
                                  int xp, int runCrafted, int suspicion, int gangHeat,
                                  List<String> inventory, Set<String> constructionBlueprints) {
        static PlayerSnapshot capture(GamePanel game) {
            return new PlayerSnapshot(game.carriedScript, game.baseStashedScript, game.supplies,
                    game.machineParts, game.xp, game.runCrafted, game.suspicion, game.gangHeat,
                    new ArrayList<>(game.inventory), new LinkedHashSet<>(game.unlockedConstructionBlueprints));
        }

        void requireSame(GamePanel game) {
            require(carriedScript == game.carriedScript, "faction construction must not spend player script");
            require(bankedScript == game.baseStashedScript, "faction construction must not spend banked script");
            require(supplies == game.supplies && machineParts == game.machineParts,
                    "faction construction must not consume player construction materials");
            require(xp == game.xp && runCrafted == game.runCrafted,
                    "faction construction must not grant player XP or crafted count");
            require(suspicion == game.suspicion && gangHeat == game.gangHeat,
                    "faction construction must not mutate player suspicion or heat");
            require(inventory.equals(game.inventory), "faction construction must not mutate player inventory");
            require(constructionBlueprints.equals(game.unlockedConstructionBlueprints),
                    "faction construction must not unlock the player's construction blueprint ledger");
        }
    }

    private Milestone05FactionBlueprintSiteUpgradeSmoke() { }
}
