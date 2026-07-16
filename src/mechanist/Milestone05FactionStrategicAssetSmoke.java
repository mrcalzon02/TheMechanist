package mechanist;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Focused Phase 17.2 smoke for physical strategic assets and vendor closure rules. */
final class Milestone05FactionStrategicAssetSmoke {
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        try {
            game.world = world();
            game.baseObjects.clear();
            game.npcFactionSites.clear();
            game.factionStrategicPlans.clear();
            game.playerX = 2;
            game.playerY = 2;
            game.turn = 240;
            game.worldTurn = 240L;
            game.carriedScript = 77;
            game.machineParts = 29;
            game.supplies = 31;
            game.inventory.clear();
            game.inventory.add("Canteen");

            int homeRoom = addRoom(game.world, new Rectangle(1, 1, 9, 9),
                    "Mechanist Operations Hall", Faction.MECHANIST_COLLEGIA);
            int rivalRoom = addRoom(game.world, new Rectangle(12, 1, 10, 9),
                    "Captured Noble Workshop", Faction.NOBLE);
            require(homeRoom == 0 && rivalRoom == 1,
                    "fixture should expose deterministic home and rival room IDs");
            setWorkforce(game.world, homeRoom, Faction.MECHANIST_COLLEGIA, 3);
            setWorkforce(game.world, rivalRoom, Faction.NOBLE, 2);

            BaseObject homeFacility = machine("Operations Micro Forge", 'f', 4, 4,
                    Faction.MECHANIST_COLLEGIA, 3);
            homeFacility.constructionOwnerMode = FactionPhysicalConstructionAuthority.FACTION_OWNER_MODE;
            BaseObject capturedMachine = machine("Captured Noble Wire Mill", 'w', 15, 4,
                    Faction.NOBLE, 2);
            game.baseObjects.add(homeFacility);
            game.baseObjects.add(capturedMachine);
            game.world.tiles[homeFacility.x][homeFacility.y] = homeFacility.symbol;
            game.world.tiles[capturedMachine.x][capturedMachine.y] = capturedMachine.symbol;

            NpcFactionSite site = site("Mechanist Strategic Asset Site", 20);
            game.npcFactionSites.add(site);
            FactionStrategicPlan plan = seizurePlan("STRAT-ASSET-SMOKE");
            game.factionStrategicPlans.add(plan);
            PlayerSnapshot playerBefore = PlayerSnapshot.capture(game);

            FactionStrategicPlan noScheme = seizurePlan("STRAT-NO-SCHEME");
            noScheme.scheme = "";
            FactionStrategicAssetAuthority.Outcome blocked =
                    FactionStrategicAssetAuthority.attempt(game, noScheme, site);
            require(!blocked.success() && "no-seizure-scheme".equals(blocked.blocker()),
                    "property seizure must require an actual scheme");
            require(game.world.roomFaction(rivalRoom) == Faction.NOBLE && site.stock == 20,
                    "scheme blocker must leave room ownership and site stock unchanged");

            int productionCompletions = StaffedProductionBackgroundAuthority.tick(game, 1);
            require(productionCompletions == 0,
                    "strategic asset execution should not impersonate production completion");
            require(game.world.roomFaction(rivalRoom) == site.faction,
                    "execution-phase room seizure should transfer the physical room ledger");
            require(site.stock == 18,
                    "secrecy-sixty room seizure should reserve exactly two site stock");
            require(plan.success == 1 && plan.failure == 0
                            && "COOLDOWN".equals(plan.phase),
                    "physical room seizure should record one success and skip the abstract resolver");
            requireContains(plan.lastOutcome, "seized Captured Noble Workshop",
                    "seizure strategic outcome");
            requireContains(game.world.zoneConflictLossHistory, "LIVE-SEIZURE",
                    "persistent live seizure conflict ledger");
            requireContains(game.world.roomProfile(rivalRoom).featureText,
                    "Property control seized", "room control provenance");
            playerBefore.requireSame(game);

            // Once the captured room receives same-family workers, the next
            // ordinary planning cycle should redirect into physical salvage.
            setWorkforce(game.world, rivalRoom, site.faction, 2);
            preparePlanning(plan, "stockpile a strategic item");
            StaffedProductionBackgroundAuthority.tick(game, 1);
            require(FactionStrategicAssetAuthority.CAPTURED_ASSET_SALVAGE_GOAL
                            .equals(plan.immediateGoal),
                    "captured foreign machinery should promote the next planning cycle into salvage");
            require("Captured Noble Workshop".equals(plan.targetRoom)
                            && "Machine part".equals(plan.targetItem),
                    "salvage promotion should identify the exact captured room and recovery item");

            int stockBeforeSalvage = site.stock;
            prepareExecution(plan);
            StaffedProductionBackgroundAuthority.tick(game, 1);
            require(!game.baseObjects.contains(capturedMachine)
                            && game.world.tiles[capturedMachine.x][capturedMachine.y] == '.',
                    "salvage should remove the exact captured machine and restore its floor tile");
            require(site.stock == stockBeforeSalvage + 7,
                    "integrity-two captured machine should return seven faction-site stock");
            require(plan.success == 2 && plan.failure == 0
                            && "COOLDOWN".equals(plan.phase),
                    "captured-asset salvage should record one additional physical success");
            requireContains(game.world.zoneConflictLossHistory, "LIVE-SALVAGE",
                    "persistent live salvage conflict ledger");
            require(hasFactionStockMachinePart(game, site),
                    "salvage should materialize provenance-aware Machine part recovery");
            playerBefore.requireSame(game);

            // With no captured machine left, the next planning cycle promotes
            // the staffed operational forge into specialist deployment.
            preparePlanning(plan, "secure ammunition reserves");
            StaffedProductionBackgroundAuthority.tick(game, 1);
            require(FactionStrategicAssetAuthority.FACILITY_SPECIALIST_GOAL
                            .equals(plan.immediateGoal),
                    "staffed facility without a specialist should become the next physical objective");
            int rosterBeforeSpecialist = assignedWorkers(game.world, homeRoom, site.faction);
            int npcBeforeSpecialist = game.world.npcs.size();
            prepareExecution(plan);
            StaffedProductionBackgroundAuthority.tick(game, 1);
            NpcEntity specialist = facilitySpecialist(game.world);
            require(specialist != null
                            && "Industrial Facility Specialist".equals(specialist.role)
                            && game.world.roomIdAt(specialist.x, specialist.y) == homeRoom,
                    "specialist deployment should materialize an industrial operator in the forge room");
            require(game.world.npcs.size() == npcBeforeSpecialist + 1
                            && assignedWorkers(game.world, homeRoom, site.faction)
                            == rosterBeforeSpecialist,
                    "specialist should represent an existing assigned worker without inflating population");
            require(plan.success == 3 && plan.failure == 0
                            && "COOLDOWN".equals(plan.phase),
                    "specialist deployment should record one additional physical success");

            FactionStrategicPlan repeatSpecialist = new FactionStrategicPlan();
            repeatSpecialist.id = "STRAT-SPECIALIST-REPEAT";
            repeatSpecialist.faction = Faction.MECHANICUS;
            repeatSpecialist.immediateGoal =
                    FactionStrategicAssetAuthority.FACILITY_SPECIALIST_GOAL;
            FactionStrategicAssetAuthority.Outcome repeat =
                    FactionStrategicAssetAuthority.attempt(game, repeatSpecialist, site);
            require(repeat.success() && repeat.specialist() == specialist
                            && game.world.npcs.size() == npcBeforeSpecialist + 1,
                    "repeat specialist deployment should reuse the stable visible specialist");
            require(assignedWorkers(game.world, homeRoom, site.faction)
                            == rosterBeforeSpecialist,
                    "repeat specialist deployment must leave the workforce ledger unchanged");
            playerBefore.requireSame(game);

            verifyVendorRestrictions(game, site);
            playerBefore.requireSame(game);

            System.out.println("Milestone 05 faction strategic asset smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void verifyVendorRestrictions(GamePanel game, NpcFactionSite site) {
        TraderSession trader = new TraderSession();
        trader.name = "Mechanist Strategic Counter";
        trader.archetype = FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL.role;
        trader.marketFaction = site.faction;
        trader.marketCategory = FactionCriticalVendorPlacementAuthority.Category.INDUSTRIAL.id;
        trader.sourceSite = site;

        TradeOffer blueprint = new TradeOffer("Machine blueprint slate", "blueprint", 80,
                "licensed construction blueprint from the completed facility");
        TradeOffer tool = new TradeOffer("Tool bundle", "tool", 12,
                "ordinary industrial tool stock");
        TradeOffer ration = new TradeOffer("Emergency rations", "food", 4,
                "basic emergency ration stock");

        site.workers = 0;
        site.stock = 12;
        FactionMarketAccessAuthority.Decision unstaffed = evaluate(game, trader, blueprint,
                Faction.HIVER, 20);
        require(!unstaffed.allowed()
                        && "unstaffed faction facility".equals(unstaffed.legalClass()),
                "unstaffed source site should close all vendor sales");

        site.workers = 3;
        site.stock = 0;
        FactionMarketAccessAuthority.Decision depleted = evaluate(game, trader, tool,
                Faction.HIVER, 20);
        require(!depleted.allowed()
                        && "depleted faction-site stock".equals(depleted.legalClass()),
                "depleted source site should reserve nonessential stock");
        FactionMarketAccessAuthority.Decision relief = evaluate(game, trader, ration,
                Faction.HIVER, 0);
        require(relief.allowed(),
                "depleted source site should keep basic emergency rations available");

        site.stock = 2;
        FactionMarketAccessAuthority.Decision scarcity = evaluate(game, trader, blueprint,
                Faction.HIVER, 20);
        require(!scarcity.allowed()
                        && "scarcity-restricted strategic stock".equals(scarcity.legalClass()),
                "critical stock should suspend blueprint and other strategic transfers");

        site.stock = 12;
        game.worldTurn = game.turn;
        FactionMarketAccessAuthority.Decision conflict = evaluate(game, trader, blueprint,
                Faction.HIVER, 20);
        require(!conflict.allowed()
                        && "conflict-restricted faction market".equals(conflict.legalClass()),
                "recent seizure/salvage conflict should restrict strategic stock to non-members");
        FactionMarketAccessAuthority.Decision member = evaluate(game, trader, blueprint,
                Faction.MECHANICUS, -40);
        require(member.allowed(),
                "same-family members should retain controlled access during faction conflict");

        game.world.zoneConflictLossHistory = "";
        FactionMarketAccessAuthority.Decision hostile = evaluate(game, trader, tool,
                Faction.HIVER, -25);
        require(!hostile.allowed()
                        && "faction-closed market".equals(hostile.legalClass()),
                "existing reputation gate should fully close a hostile faction market");
    }

    private static FactionMarketAccessAuthority.Decision evaluate(
            GamePanel game, TraderSession trader, TradeOffer offer,
            Faction playerFaction, int standing) {
        return FactionMarketAccessAuthority.evaluate(trader, offer,
                new FactionMarketAccessAuthority.AccessContext(
                        playerFaction, standing, 0, 0,
                        Set.of(), Set.of(), List.of(), game.world, game.worldTurn));
    }

    private static World world() {
        World world = new World(5172L, 26, 16);
        world.sectorX = 1;
        world.sectorY = 1;
        world.zoneX = 2;
        world.zoneY = 2;
        world.floor = 4;
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.npcs.clear();
        world.mapObjects.clear();
        world.roomPopulationLedgers.clear();
        world.zoneConflictLossHistory = "";
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) {
                world.tiles[x][y] = '#';
                world.roomIds[x][y] = -1;
            }
        }
        return world;
    }

    private static int addRoom(World world, Rectangle room, String name,
                               Faction faction) {
        int roomId = world.rooms.size();
        RoomProfile profile = RoomProfile.generic();
        profile.name = name;
        profile.descriptor = "deterministic strategic asset smoke room";
        profile.featureText = "physical ownership, salvage, staffing, and vendor verification";
        profile.faction = faction;
        world.rooms.add(room);
        world.roomProfiles.add(profile);
        world.roomFactions.add(faction);
        world.roomSpecials.add(false);
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                boolean edge = x == room.x || y == room.y
                        || x == room.x + room.width - 1
                        || y == room.y + room.height - 1;
                world.tiles[x][y] = edge ? '#' : '.';
                world.roomIds[x][y] = edge ? -1 : roomId;
            }
        }
        return roomId;
    }

    private static void setWorkforce(World world, int roomId,
                                     Faction faction, int assigned) {
        world.roomPopulationLedgers.removeIf(ledger -> ledger != null
                && ledger.roomId == roomId);
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = "pop.strategic.asset." + roomId;
        ledger.roomId = roomId;
        ledger.roomName = RoomOwnershipAuthority.roomName(world, roomId);
        ledger.faction = faction;
        ledger.capacity = Math.max(8, assigned);
        ledger.available = Math.max(0, ledger.capacity - assigned);
        ledger.assigned = Math.max(0, assigned);
        world.roomPopulationLedgers.add(ledger);
    }

    private static BaseObject machine(String name, char symbol, int x, int y,
                                      Faction faction, int integrity) {
        BaseObject object = new BaseObject(name, symbol, x, y, 0, 0);
        object.faction = faction;
        object.integrity = integrity;
        object.capacity = 6;
        object.constructionOriginalTile = '.';
        object.underConstruction = false;
        return object;
    }

    private static NpcFactionSite site(String name, int stock) {
        NpcFactionSite site = NpcFactionSite.create(name,
                Faction.MECHANIST_COLLEGIA, "machine shop",
                1, 1, 2, 2, 4,
                "Machine part", "Tool bundle", "Scrap-Forging Doctrine");
        site.stock = stock;
        site.workers = 3;
        site.baseLevel = 2;
        site.machineLevel = 3;
        return site;
    }

    private static FactionStrategicPlan seizurePlan(String id) {
        FactionStrategicPlan plan = new FactionStrategicPlan();
        plan.id = id;
        plan.faction = Faction.MECHANICUS;
        plan.schemeTargetFaction = Faction.NOBLE;
        plan.phase = "EXECUTION";
        plan.immediateGoal = FactionStrategicAssetAuthority.ROOM_SEIZURE_GOAL;
        plan.scheme = "make a private deal";
        plan.targetRoom = "Captured Noble Workshop";
        plan.targetItem = "Trade chit";
        plan.secrecy = 60;
        plan.aggression = 45;
        plan.ambition = 60;
        plan.phaseUntilTurn = 240;
        plan.nextDecisionTurn = 240;
        return plan;
    }

    private static void preparePlanning(FactionStrategicPlan plan, String goal) {
        plan.phase = "PLANNING";
        plan.immediateGoal = goal;
        plan.phaseUntilTurn = Integer.MAX_VALUE;
        plan.nextDecisionTurn = Integer.MAX_VALUE;
    }

    private static void prepareExecution(FactionStrategicPlan plan) {
        plan.phase = "EXECUTION";
        plan.phaseUntilTurn = 0;
        plan.nextDecisionTurn = 0;
    }

    private static NpcEntity facilitySpecialist(World world) {
        NpcEntity found = null;
        for (NpcEntity npc : world.npcs) {
            if (npc == null || npc.id == null
                    || !npc.id.startsWith("FACTION-FACILITY-SPECIALIST-")) continue;
            require(found == null, "only one facility specialist should exist in the fixture");
            found = npc;
        }
        return found;
    }

    private static int assignedWorkers(World world, int roomId, Faction faction) {
        int assigned = 0;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger == null || ledger.roomId != roomId
                    || !FactionIdentityAuthority.sameFamily(ledger.faction, faction)) continue;
            assigned += Math.max(0, ledger.assigned);
        }
        return assigned;
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

    private record PlayerSnapshot(int script, int machineParts, int supplies,
                                  int turn, long worldTurn,
                                  List<String> inventory) {
        static PlayerSnapshot capture(GamePanel game) {
            return new PlayerSnapshot(game.carriedScript, game.machineParts,
                    game.supplies, game.turn, game.worldTurn,
                    new ArrayList<>(game.inventory));
        }

        void requireSame(GamePanel game) {
            require(script == game.carriedScript,
                    "faction strategic assets must not spend player script");
            require(machineParts == game.machineParts && supplies == game.supplies,
                    "faction strategic assets must not consume player construction resources");
            require(turn == game.turn && worldTurn == game.worldTurn,
                    "background faction asset resolution must not spend player turns");
            require(inventory.equals(game.inventory),
                    "faction strategic assets must not mutate player inventory");
        }
    }

    private Milestone05FactionStrategicAssetSmoke() { }
}
