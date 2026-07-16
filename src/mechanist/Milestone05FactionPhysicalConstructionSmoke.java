package mechanist;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/** Focused smoke for Phase 17.2 physical faction construction and attribution. */
final class Milestone05FactionPhysicalConstructionSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.baseObjects.clear();
            game.playerX = 2;
            game.playerY = 2;
            game.turn = 120;
            game.worldTurn = 120L;
            game.carriedScript = 73;
            game.baseStashedScript = 41;
            game.supplies = 29;
            game.machineParts = 31;
            game.food = 17;
            game.water = 19;
            game.xp = 13;
            game.knowledgeCredits = 7;
            game.runCrafted = 4;
            game.equippedLeftHandItem = "Masterwork servo welder";

            int forgeRoom = addRoom(game.world, new Rectangle(1, 1, 9, 9),
                    "Exact Forge Assembly Room", Faction.MECHANICUS_CLOISTER_RED, false);
            int distantCrewRoom = addRoom(game.world, new Rectangle(12, 1, 9, 9),
                    "Uncontrolled Crew Annex", Faction.NOBLE, false);
            require(forgeRoom == 0 && distantCrewRoom == 1,
                    "deterministic fixture should expose controlled room zero and unrelated room one");
            configureWorkforce(game.world, forgeRoom, 3);
            game.world.npcs.add(vendor("Physical Works Factor", 8, 8));

            BuildRecipe recipe = BuildRecipe.microForge();
            int materialCost = FactionFacilityBlueprintUpgradeAuthority.aggregateStockCost(recipe);
            require(materialCost == 12, "physical Micro Forge should reserve twelve faction stock");

            NpcFactionSite site = site("Phase 17 Physical Forge Site", materialCost * 2);
            FactionStrategicPlan plan = plan("STRAT-PHYSICAL-SMOKE");
            game.npcFactionSites.clear();
            game.npcFactionSites.add(site);
            game.factionStrategicPlans.clear();
            game.factionStrategicPlans.add(plan);

            PlayerSnapshot playerBefore = PlayerSnapshot.capture(game);
            int baseLevelBefore = site.baseLevel;
            int machineLevelBefore = site.machineLevel;
            FactionPhysicalConstructionAuthority.Outcome staged =
                    FactionPhysicalConstructionAuthority.attempt(game, plan, site);

            require(staged.handled() && staged.success(),
                    "valid local Mechanist construction should stage physically: " + staged);
            require(staged.roomId() == forgeRoom
                            && "Exact Forge Assembly Room".equals(staged.roomName()),
                    "placement should select the exact same-family controlled room");
            require(staged.effectiveWorkers() == 3,
                    "physical outcome should report the exact room-local assigned workforce");
            require(staged.stockBefore() == materialCost * 2 && staged.stockAfter() == materialCost
                            && site.stock == materialCost,
                    "staging should reserve the exact faction stock cost once");
            require(site.baseLevel == baseLevelBefore && site.machineLevel == machineLevelBefore,
                    "stock reservation must not advance facility levels before physical completion");
            require(plan.success == 0 && plan.failure == 0 && plan.history.isEmpty(),
                    "direct staging must leave strategic success/history pending completion");

            BaseObject construction = staged.constructionSite();
            require(construction != null && game.baseObjects.size() == 1
                            && game.baseObjects.get(0) == construction,
                    "successful staging should add exactly one physical construction object");
            require(construction.underConstruction && construction.symbol == '?'
                            && construction.finalSymbol == recipe.symbol,
                    "physical object should remain a staged Micro Forge ghost");
            require(construction.x == staged.tileX() && construction.y == staged.tileY()
                            && game.world.roomIdAt(construction.x, construction.y) == forgeRoom,
                    "outcome tile and live object should identify the controlled room interior");
            require(construction.constructionOriginalTile == '.'
                            && game.world.tiles[construction.x][construction.y] == '?',
                    "placement should reserve a formerly walkable interior floor tile");
            require(construction.x != game.playerX || construction.y != game.playerY,
                    "physical preflight must not place on the player");
            require(game.world.npcAt(construction.x, construction.y) == null
                            && game.world.mapObjectAt(construction.x, construction.y) == null,
                    "physical preflight must avoid NPCs and map objects");
            require(construction.constructionInsertedItems.equals(construction.constructionRequiredItems),
                    "faction stock reservation should create a fully prepaid staged site");
            require(FactionPhysicalConstructionAuthority.isFactionManaged(construction)
                            && FactionPhysicalConstructionAuthority.belongsToSite(construction, site),
                    "staged site should carry a stable faction owner and linked-site job key");
            require("FACTION".equals(construction.constructionOwnerMode)
                            && plan.id.equals(construction.constructionLinkedPlanId),
                    "physical site should retain explicit faction ownership and strategic plan ID");
            requireContains(construction.constructionLinkedSiteName, site.name,
                    "stable linked-site job key");
            requireContains(construction.constructionLinkedSiteName, "sector 1-1 zone 2-2 floor 4",
                    "stable linked-site location");
            requireContains(construction.constructionPlanSource, "EMM Micro Forge",
                    "physical plan provenance");
            requireContains(construction.constructionMaterialSource, "reserved faction-site stock",
                    "physical material provenance");
            requireContains(FactionPhysicalConstructionAuthority.crewReadback(construction),
                    "3 assigned worker(s) from the Exact Forge Assembly Room roster",
                    "room-local crew readback");
            playerBefore.requireSame(game);

            int stockAfterStage = site.stock;
            FactionPhysicalConstructionAuthority.Outcome resumed =
                    FactionPhysicalConstructionAuthority.attempt(game, plan, site);
            require(resumed.handled() && resumed.success() && resumed.constructionSite() == construction,
                    "duplicate strategic attempt should resume the existing physical job");
            require(game.baseObjects.size() == 1 && site.stock == stockAfterStage
                            && resumed.stockBefore() == stockAfterStage
                            && resumed.stockAfter() == stockAfterStage,
                    "duplicate resume must not add an object or debit faction stock again");
            requireContains(resumed.message(), "IN PROGRESS:", "duplicate resume state");
            require(site.baseLevel == baseLevelBefore && site.machineLevel == machineLevelBefore,
                    "duplicate resume must not advance facility levels");

            List<String> look = ProgressiveLookAuthority.tileStackAt(
                    game, construction.x, construction.y, 2);
            requireContains(look, "Construction status: staged site", "ordinary Look construction state");
            requireContains(look, "Construction owner: Mechanist", "ordinary Look faction owner");
            requireContains(look, "3 assigned worker(s) from the Exact Forge Assembly Room roster",
                    "ordinary Look assigned workforce");
            requireContains(look, "reserved faction-site stock", "ordinary Look material source");
            requireContains(look, "EMM Micro Forge", "ordinary Look plan source");
            requireContains(look, "player materials and labor are not used",
                    "ordinary Look player isolation");

            List<String> status = ProgressiveConstructionAuthority.statusPacketLines(game);
            requireContains(status, "faction-managed=1", "construction status faction count");
            requireContains(status, "assigned faction crew will add labor",
                    "construction status faction next action");

            int laborBeforePlayerAttempt = construction.constructionLaborDone;
            int inserted = ProgressiveConstructionAuthority.contribute(game, construction, 99, true);
            require(inserted == 0 && construction.constructionLaborDone == laborBeforePlayerAttempt,
                    "ordinary contribute path must reject faction site materials and player/tool labor");
            ProgressiveConstructionAuthority.DismantleResult denied =
                    ProgressiveConstructionAuthority.dismantle(game, construction);
            require(!denied.removed() && denied.recoveredSupplies() == 0
                            && denied.recoveredMachineParts() == 0 && denied.recoveredNamedItems() == 0
                            && game.baseObjects.contains(construction),
                    "ordinary dismantle path must not recover faction stock into player storage");
            game.activeInteractionBaseObject = construction;
            game.workActiveConstructionSite();
            game.dismantleActiveConstructionSite();
            require(construction.constructionLaborDone == laborBeforePlayerAttempt
                            && game.baseObjects.contains(construction),
                    "player Work and Dismantle controls must leave the faction site untouched");
            playerBefore.requireSame(game);

            assertFactionMetadataRoundTrip(game, construction);

            int roomRosterBeforeVendor = roomAssignedWorkers(game.world, forgeRoom, site.faction);
            game.world.npcs.removeIf(npc -> npc != null
                    && "PHYSICAL-WORKS-FACTOR".equals(npc.id));
            require(facilityVendor(game.world) == null,
                    "blueprint-acquisition vendor should be absent before the facility completes");

            ProgressiveConstructionAuthority.FactionWorkResult firstHour =
                    FactionPhysicalConstructionAuthority.advanceHourly(game, site);
            require(firstHour.advanced() && firstHour.laborAdded() == 3 && !firstHour.completed(),
                    "first faction hour should add exact room workers, unaffected by player servo tools: "
                            + firstHour);
            require(construction.constructionLaborDone == 3,
                    "first faction hour should add three absolute labor turns");
            require(site.baseLevel == baseLevelBefore && site.machineLevel == machineLevelBefore
                            && plan.success == 0,
                    "partial faction labor must leave levels and strategic success pending");
            require(facilityVendor(game.world) == null,
                    "an unfinished facility must not open a replacement vendor");

            ProgressiveConstructionAuthority.FactionWorkResult completion = firstHour;
            int hourlyCalls = 1;
            while (construction.underConstruction && hourlyCalls < 10) {
                completion = FactionPhysicalConstructionAuthority.advanceHourly(game, site);
                hourlyCalls++;
            }
            require(hourlyCalls == 4 && completion.completed() && !construction.underConstruction,
                    "three-worker room crew should complete ten labor in four hourly calls: " + completion);
            require(construction.symbol == recipe.symbol
                            && game.world.tiles[construction.x][construction.y] == recipe.symbol,
                    "completion should convert the staged tile into the physical Micro Forge");
            require(site.baseLevel == baseLevelBefore + 1
                            && site.machineLevel == machineLevelBefore + 1,
                    "physical completion should advance linked site levels once");
            require(site.completedConstructionJobs.size() == 1
                            && site.hasCompletedConstructionJob(construction.constructionLinkedSiteName),
                    "completion should persist one stable job receipt");
            require(plan.success == 1 && plan.failure == 0 && plan.history.size() == 1,
                    "physical completion should record exactly one linked strategic success");
            requireContains(plan.lastOutcome, "SUCCESS:", "completed strategic outcome");
            requireContains(plan.history.get(0), "reserved factory upgrade is now operational",
                    "completed strategic history");
            requireContains(completion.summary(), "facility levels advanced once",
                    "completion level readback");
            requireContains(completion.summary(), "opened a staffed Industrial Blueprint Trader counter",
                    "facility vendor completion readback");

            NpcEntity facilityVendor = facilityVendor(game.world);
            require(facilityVendor != null
                            && FactionCriticalVendorPlacementAuthority.isFacilityVendor(facilityVendor),
                    "completed Micro Forge should open one stable facility-backed vendor");
            require(FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL.role
                            .equals(facilityVendor.role)
                            && game.world.roomIdAt(facilityVendor.x, facilityVendor.y) == forgeRoom,
                    "facility vendor should serve the completed forge room as an industrial trader");
            MapObjectState facilityMarket = game.world.mapObjectAt(facilityVendor.x, facilityVendor.y);
            require(facilityMarket != null && "faction-market".equals(facilityMarket.type),
                    "facility vendor should have a physical faction-market counter");
            require("true".equals(MapObjectState.stockValue(
                            facilityMarket.stockState, "facilityVendor"))
                            && FactionCriticalVendorPlacementAuthority.siteToken(site).equals(
                            MapObjectState.stockValue(facilityMarket.stockState, "sourceSite"))
                            && !MapObjectState.stockValue(
                            facilityMarket.stockState, "sourceFacility").isBlank(),
                    "facility counter should retain stable site and completed-facility linkage");
            require(Integer.toString(roomRosterBeforeVendor).equals(MapObjectState.stockValue(
                            facilityMarket.stockState, "assignedWorkers"))
                            && roomAssignedWorkers(game.world, forgeRoom, site.faction)
                            == roomRosterBeforeVendor,
                    "opening the vendor must represent assigned room staff without inventing population");

            int npcCountAfterOpen = game.world.npcs.size();
            int marketCountAfterOpen = game.world.mapObjects.size();
            FactionCriticalVendorPlacementAuthority.FacilityActivation repeatedVendor =
                    FactionCriticalVendorPlacementAuthority.activateCompletedFacility(
                            game, site, construction);
            require(repeatedVendor.handled() && repeatedVendor.existing()
                            && !repeatedVendor.opened()
                            && repeatedVendor.vendor() == facilityVendor,
                    "repeat facility activation should reuse the stable vendor");
            require(game.world.npcs.size() == npcCountAfterOpen
                            && game.world.mapObjects.size() == marketCountAfterOpen
                            && FactionCriticalVendorPlacementAuthority
                            .reconcileCompletedFacilities(game) == 0,
                    "repeat activation and reconciliation must not duplicate vendors or counters");

            FactionSiteWorkforceAuthority.sync(site, game.world);
            TraderSession facilityTrade = TraderSession.forNpc(
                    facilityVendor, game.world.zoneType, new Random(1702172L));
            TraderTradeActionAuthority.attachNpcSiteStock(
                    facilityTrade, site, new Random(1702173L), game.world, game.turn);
            require(facilityTrade.sourceSite == site,
                    "facility vendor trade should attach the exact local faction site");
            require(hasTracedSiteOffer(facilityTrade, site.name),
                    "facility vendor shelf should include stock with faction-site provenance");
            requireContains(facilityTrade.supplyChainSummary,
                    "completed staffed faction facility", "facility vendor remit");
            playerBefore.requireSame(game);

            int completedBaseLevel = site.baseLevel;
            int completedMachineLevel = site.machineLevel;
            int completedSuccess = plan.success;
            int completedHistory = plan.history.size();
            ProgressiveConstructionAuthority.FactionWorkResult afterCompletion =
                    FactionPhysicalConstructionAuthority.advanceHourly(game, site);
            require(!afterCompletion.advanced() && !afterCompletion.completed(),
                    "completed physical job should no longer be an active labor target");
            require(!FactionFacilityBlueprintUpgradeAuthority.applyCompletedPhysicalUpgrade(
                            site, construction.constructionLinkedSiteName),
                    "duplicate completion receipt should be rejected");
            require(site.baseLevel == completedBaseLevel && site.machineLevel == completedMachineLevel
                            && plan.success == completedSuccess && plan.history.size() == completedHistory,
                    "repeat ticks/receipts must not advance levels or strategic success twice");
            require(game.world.npcs.size() == npcCountAfterOpen
                            && game.world.mapObjects.size() == marketCountAfterOpen,
                    "completed construction ticks must not duplicate the facility vendor");
            playerBefore.requireSame(game);

            verifyPlacementBlockerAtomicity(game, forgeRoom, distantCrewRoom, materialCost);
            playerBefore.requireSame(game);

            System.out.println("Milestone 05 faction physical construction smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void assertFactionMetadataRoundTrip(GamePanel game, BaseObject construction) {
        String[] fields = construction.saveLine().split("\\|", -1);
        require(fields.length == 36, "faction BaseObject should retain the 36-field append-only save shape");
        require("FACTION".equals(BaseObject.decodeDelimitedField(fields[31])),
                "save line should retain faction owner mode");
        require(construction.constructionMaterialSource.equals(BaseObject.decodeDelimitedField(fields[32]))
                        && construction.constructionPlanSource.equals(BaseObject.decodeDelimitedField(fields[33]))
                        && construction.constructionLinkedSiteName.equals(BaseObject.decodeDelimitedField(fields[34]))
                        && construction.constructionLinkedPlanId.equals(BaseObject.decodeDelimitedField(fields[35])),
                "save line should retain faction material, plan, job, and strategic-plan metadata");

        Properties saved = new Properties();
        Persistence.writeCore(game, saved);
        GamePanel reader = new GamePanel();
        reader.shutdownRuntime();
        try {
            Persistence.readCore(reader, saved);
            BaseObject loaded = null;
            for (BaseObject candidate : reader.baseObjects) {
                if (candidate != null
                        && construction.constructionLinkedSiteName.equals(candidate.constructionLinkedSiteName)) {
                    loaded = candidate;
                    break;
                }
            }
            require(loaded != null && loaded.underConstruction,
                    "core persistence should reload the unfinished faction construction object");
            require(FactionPhysicalConstructionAuthority.isFactionManaged(loaded)
                            && construction.assignedWorker.equals(loaded.assignedWorker)
                            && construction.constructionMaterialSource.equals(loaded.constructionMaterialSource)
                            && construction.constructionPlanSource.equals(loaded.constructionPlanSource)
                            && construction.constructionLinkedSiteName.equals(loaded.constructionLinkedSiteName)
                            && construction.constructionLinkedPlanId.equals(loaded.constructionLinkedPlanId),
                    "core persistence should retain faction owner, crew, material, plan, job, and plan ID");
            require(loaded.constructionLaborDone == construction.constructionLaborDone
                            && loaded.constructionLaborRequired == construction.constructionLaborRequired
                            && loaded.constructionInsertedItems.equals(loaded.constructionRequiredItems),
                    "core persistence should retain prepaid material and labor state");
        } finally {
            reader.shutdownRuntime();
        }
    }

    private static void verifyPlacementBlockerAtomicity(GamePanel game, int forgeRoom,
                                                         int distantCrewRoom, int materialCost) {
        Faction originalOwner = game.world.roomFaction(forgeRoom);
        game.world.roomFactions.set(forgeRoom, Faction.NOBLE);
        configureWorkforce(game.world, forgeRoom, 3);
        NpcFactionSite noRoomSite = site("No Controlled Room Site", materialCost);
        FactionStrategicPlan noRoomPlan = plan("STRAT-NO-CONTROLLED-ROOM");
        SiteSnapshot noRoomBefore = SiteSnapshot.capture(noRoomSite);
        int objectsBefore = game.baseObjects.size();
        String tilesBefore = tileFingerprint(game.world);
        FactionPhysicalConstructionAuthority.Outcome noRoom =
                FactionPhysicalConstructionAuthority.attempt(game, noRoomPlan, noRoomSite);
        require(!noRoom.success() && "no-controlled-room".equals(noRoom.blocker()),
                "unavailable controlled-room preflight should block before reservation: " + noRoom);
        noRoomBefore.requireSame(noRoomSite, "no-controlled-room blocker");
        require(game.baseObjects.size() == objectsBefore && tilesBefore.equals(tileFingerprint(game.world)),
                "no-controlled-room blocker must not add an object or mutate world tiles");
        require(noRoomPlan.success == 0 && noRoomPlan.failure == 0 && noRoomPlan.history.isEmpty(),
                "direct controlled-room blocker should not mutate strategic counters/history");

        game.world.roomFactions.set(forgeRoom, originalOwner);
        configureWorkforce(game.world, distantCrewRoom, 4);
        NpcFactionSite noCrewSite = site("No Room Local Crew Site", materialCost);
        FactionStrategicPlan noCrewPlan = plan("STRAT-NO-ROOM-CREW");
        require(FactionSiteWorkforceAuthority.evaluate(noCrewSite, game.world).effectiveWorkers() == 4,
                "fixture should expose broader site workers located in the wrong room");
        SiteSnapshot noCrewBefore = SiteSnapshot.capture(noCrewSite);
        objectsBefore = game.baseObjects.size();
        tilesBefore = tileFingerprint(game.world);
        FactionPhysicalConstructionAuthority.Outcome noCrew =
                FactionPhysicalConstructionAuthority.attempt(game, noCrewPlan, noCrewSite);
        require(!noCrew.success() && "no-room-workforce".equals(noCrew.blocker())
                        && noCrew.effectiveWorkers() == 0,
                "workers assigned elsewhere must not staff the controlled physical room: " + noCrew);
        noCrewBefore.requireSame(noCrewSite, "no-room-workforce blocker");
        require(game.baseObjects.size() == objectsBefore && tilesBefore.equals(tileFingerprint(game.world)),
                "no-room-workforce blocker must not add an object or mutate world tiles");
        require(noCrewPlan.success == 0 && noCrewPlan.failure == 0 && noCrewPlan.history.isEmpty(),
                "direct room-workforce blocker should not mutate strategic counters/history");
    }

    private static World world() {
        World world = new World(1702172L, 24, 16);
        world.sectorX = 1;
        world.sectorY = 1;
        world.zoneX = 2;
        world.zoneY = 2;
        world.floor = 4;
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.npcs.clear();
        world.mapObjects.clear();
        world.roomPopulationLedgers.clear();
        for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) {
            world.tiles[x][y] = '#';
            world.roomIds[x][y] = -1;
        }
        return world;
    }

    private static int addRoom(World world, Rectangle room, String name,
                               Faction faction, boolean special) {
        int roomId = world.rooms.size();
        RoomProfile profile = RoomProfile.generic();
        profile.name = name;
        profile.descriptor = "deterministic physical construction smoke room";
        profile.featureText = "open interior reserved for deterministic construction verification";
        profile.faction = faction;
        world.rooms.add(room);
        world.roomProfiles.add(profile);
        world.roomFactions.add(faction);
        world.roomSpecials.add(special);
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                boolean edge = x == room.x || y == room.y
                        || x == room.x + room.width - 1 || y == room.y + room.height - 1;
                world.tiles[x][y] = edge ? '#' : '.';
                world.roomIds[x][y] = edge ? -1 : roomId;
            }
        }
        return roomId;
    }

    private static void configureWorkforce(World world, int roomId, int assigned) {
        world.roomPopulationLedgers.clear();
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = "pop.physical.room." + roomId;
        ledger.roomId = roomId;
        ledger.roomName = roomId >= 0 && roomId < world.roomProfiles.size()
                ? world.roomProfile(roomId).name : "Unrecognized room";
        ledger.faction = Faction.MECHANIST_COLLEGIA;
        ledger.capacity = Math.max(8, assigned);
        ledger.available = Math.max(0, ledger.capacity - assigned);
        ledger.assigned = Math.max(0, assigned);
        world.roomPopulationLedgers.add(ledger);
    }

    private static NpcFactionSite site(String name, int stock) {
        NpcFactionSite site = NpcFactionSite.create(name, Faction.MECHANIST_COLLEGIA,
                "machine shop", 1, 1, 2, 2, 4,
                "Machine part", "Tool bundle", "Scrap-Forging Doctrine");
        site.stock = stock;
        site.baseLevel = 1;
        site.machineLevel = 2;
        return site;
    }

    private static FactionStrategicPlan plan(String id) {
        FactionStrategicPlan plan = new FactionStrategicPlan();
        plan.id = id;
        plan.faction = Faction.MECHANICUS;
        plan.immediateGoal = FactionFacilityBlueprintUpgradeAuthority.FACTORY_UPGRADE_GOAL;
        plan.targetRoom = "Exact Forge Assembly Room";
        plan.targetItem = "Machine part";
        return plan;
    }

    private static NpcEntity vendor(String name, int x, int y) {
        NpcEntity vendor = new NpcEntity();
        vendor.id = "PHYSICAL-WORKS-FACTOR";
        vendor.name = name;
        vendor.faction = Faction.MECHANICUS_CLOISTER_RED;
        vendor.role = FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL.role;
        vendor.state = "Trade";
        vendor.hp = 12;
        vendor.x = x;
        vendor.y = y;
        return vendor;
    }

    private static NpcEntity facilityVendor(World world) {
        if (world == null || world.npcs == null) return null;
        NpcEntity found = null;
        for (NpcEntity npc : world.npcs) {
            if (!FactionCriticalVendorPlacementAuthority.isFacilityVendor(npc)) continue;
            require(found == null, "only one facility-backed vendor should exist in the smoke world");
            found = npc;
        }
        return found;
    }

    private static int roomAssignedWorkers(World world, int roomId, Faction faction) {
        if (world == null || world.roomPopulationLedgers == null) return 0;
        int assigned = 0;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || ledger.roomId != roomId
                    || !FactionIdentityAuthority.sameFamily(ledger.faction, faction)) continue;
            assigned += Math.max(0, ledger.assigned);
        }
        return assigned;
    }

    private static boolean hasTracedSiteOffer(TraderSession trader, String siteName) {
        if (trader == null || trader.offers == null) return false;
        for (TradeOffer offer : trader.offers) {
            if (offer == null || offer.provenance == null) continue;
            String trace = offer.provenance.shortChain();
            if ((trace != null && trace.contains(siteName))
                    || (offer.description != null && offer.description.contains(siteName))) return true;
        }
        return false;
    }

    private static String tileFingerprint(World world) {
        StringBuilder out = new StringBuilder(world.w * world.h);
        for (int y = 0; y < world.h; y++) for (int x = 0; x < world.w; x++) {
            out.append(world.tiles[x][y]);
        }
        return out.toString();
    }

    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected),
                "Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record SiteSnapshot(int stock, int baseLevel, int machineLevel,
                                Set<String> blueprints, Set<String> completedJobs) {
        static SiteSnapshot capture(NpcFactionSite site) {
            return new SiteSnapshot(site.stock, site.baseLevel, site.machineLevel,
                    new LinkedHashSet<>(site.knownConstructionBlueprints),
                    new LinkedHashSet<>(site.completedConstructionJobs));
        }

        void requireSame(NpcFactionSite site, String label) {
            SiteSnapshot after = capture(site);
            require(equals(after), label + " mutated faction-site state: before="
                    + this + " after=" + after);
        }
    }

    private record PlayerSnapshot(int carriedScript, int bankedScript, int supplies, int machineParts,
                                  int food, int water, int xp, int knowledgeCredits, int runCrafted,
                                  int suspicion, int gangHeat, int turn, long worldTurn,
                                  String leftHand, String rightHand,
                                  List<String> inventory, List<String> baseStorage,
                                  Set<String> constructionBlueprints) {
        static PlayerSnapshot capture(GamePanel game) {
            return new PlayerSnapshot(game.carriedScript, game.baseStashedScript, game.supplies,
                    game.machineParts, game.food, game.water, game.xp, game.knowledgeCredits,
                    game.runCrafted, game.suspicion, game.gangHeat, game.turn, game.worldTurn,
                    game.equippedLeftHandItem, game.equippedRightHandItem,
                    new ArrayList<>(game.inventory),
                    new ArrayList<>(game.baseStorage),
                    new LinkedHashSet<>(game.unlockedConstructionBlueprints));
        }

        void requireSame(GamePanel game) {
            require(carriedScript == game.carriedScript && bankedScript == game.baseStashedScript,
                    "faction construction must not spend player script");
            require(supplies == game.supplies && machineParts == game.machineParts,
                    "faction construction must not consume player construction resources");
            require(food == game.food && water == game.water,
                    "faction construction must not consume player food or water");
            require(xp == game.xp && knowledgeCredits == game.knowledgeCredits
                            && runCrafted == game.runCrafted,
                    "faction construction must not grant player XP, knowledge, or crafted count");
            require(suspicion == game.suspicion && gangHeat == game.gangHeat,
                    "faction construction must not mutate player suspicion or heat");
            require(turn == game.turn && worldTurn == game.worldTurn,
                    "faction construction and denied player controls must not spend player turns");
            require(inventory.equals(game.inventory),
                    "faction construction must not mutate player inventory");
            require(baseStorage.equals(game.baseStorage),
                    "faction construction must not mutate player base storage");
            require(java.util.Objects.equals(leftHand, game.equippedLeftHandItem)
                            && java.util.Objects.equals(rightHand, game.equippedRightHandItem),
                    "faction construction must not change player-equipped tools");
            require(constructionBlueprints.equals(game.unlockedConstructionBlueprints),
                    "faction construction must not unlock the player's blueprint ledger");
        }
    }

    private Milestone05FactionPhysicalConstructionSmoke() { }
}
