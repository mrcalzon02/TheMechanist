package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/** Focused smoke for Phase 17.2 physical faction room-shell construction. */
final class Milestone05FactionRoomShellConstructionSmoke {
    private static final Rectangle SOURCE_RECT = new Rectangle(1, 1, 9, 9);
    private static final int EXPECTED_ORIGIN_X = 3;
    private static final int EXPECTED_ORIGIN_Y = 11;
    private static final int EXPECTED_WIDTH = 5;
    private static final int EXPECTED_HEIGHT = 4;
    private static final int SOURCE_DOOR_X = 5;
    private static final int SOURCE_DOOR_Y = 9;
    private static final int CONNECTOR_X = 5;
    private static final int CONNECTOR_Y = 10;
    private static final int ANNEX_DOOR_X = 5;
    private static final int ANNEX_DOOR_Y = 11;
    private static final int MATERIAL_STOCK_COST = 49;
    private static final int LABOR_REQUIRED = 35;
    private static final int ROOM_WORKERS = 3;
    private static final String SOURCE_LEDGER_ID = "pop.room-shell.source";

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        GamePanel game = fixtureGame();
        try {
            int sourceRoomId = 0;
            NpcFactionSite site = site(MATERIAL_STOCK_COST * 2);
            FactionStrategicPlan plan = plan();
            game.npcFactionSites.clear();
            game.npcFactionSites.add(site);
            game.factionStrategicPlans.clear();
            game.factionStrategicPlans.add(plan);

            require(game.world.rooms.size() == 1 && sourceRoomId == game.world.roomIdAt(5, 5),
                    "fixture should begin with one exact controlled source room");
            require(noLegalMicroForgeTile(game, sourceRoomId),
                    "source-room fixtures should force the factory plan into a physical room annex");

            RoomArraysSnapshot roomArraysBefore = RoomArraysSnapshot.capture(game.world);
            LedgerSnapshot workforceBefore = LedgerSnapshot.capture(game.world);
            PlayerSnapshot playerBefore = PlayerSnapshot.capture(game);
            String tilesBefore = tileFingerprint(game.world);
            int baseObjectsBefore = game.baseObjects.size();
            int npcCountBefore = game.world.npcs.size();
            int siteBaseLevelBefore = site.baseLevel;
            int siteMachineLevelBefore = site.machineLevel;

            FactionRoomShellConstructionAuthority.Outcome staged =
                    FactionRoomShellConstructionAuthority.attempt(game, plan, site);
            require(staged.handled() && staged.success(),
                    "valid staffed Mechanist expansion should stage a physical room shell: " + staged);
            require(staged.sourceRoomId() == sourceRoomId
                            && "Exact Forge Source Room".equals(staged.sourceRoomName()),
                    "staging should identify the exact controlled source room");
            require(staged.originX() == EXPECTED_ORIGIN_X && staged.originY() == EXPECTED_ORIGIN_Y,
                    "deterministic source room should select the exact south annex origin");
            require(staged.workers() == ROOM_WORKERS,
                    "staging should report the exact source-room workforce");
            require(staged.materialStockCost() == MATERIAL_STOCK_COST
                            && staged.laborRequired() == LABOR_REQUIRED,
                    "room shell should report exact 49 stock and 35 labor requirements");
            require(staged.stockBefore() == MATERIAL_STOCK_COST * 2
                            && staged.stockAfter() == MATERIAL_STOCK_COST
                            && site.stock == MATERIAL_STOCK_COST,
                    "staging should debit exactly 49 faction stock once");
            require(site.baseLevel == siteBaseLevelBefore && site.machineLevel == siteMachineLevelBefore,
                    "room-shell staging must not grant an instant facility-level upgrade");

            BaseObject marker = staged.marker();
            require(marker != null && game.baseObjects.size() == baseObjectsBefore + 1,
                    "staging should add exactly one physical room-shell marker");
            require(marker.underConstruction && marker.symbol == '?'
                            && marker.constructionLaborRequired == LABOR_REQUIRED
                            && marker.constructionLaborDone == 0,
                    "room-shell marker should begin as a 0/35 staged construction ghost");
            require(FactionRoomShellConstructionAuthority.isRoomShellMarker(marker)
                            && FactionPhysicalConstructionAuthority.isFactionManaged(marker),
                    "marker should carry durable room-shell identity and faction custody");
            requireContains(marker.assignedRecipe, "Forge Annex", "room-shell assigned recipe");
            require(marker.constructionRequiredItems.equals(marker.constructionInsertedItems),
                    "all faction room materials should be prepaid at staging");
            require(exactMaterialMap().equals(decodeRequirements(marker.constructionRequiredItems)),
                    "prepaid marker should retain the exact 20/14/14/1 room material bill: "
                            + marker.constructionRequiredItems);
            require(FactionRoomShellConstructionAuthority.activeSite(game, site) == marker,
                    "active-site lookup should resolve the same persisted marker");

            roomArraysBefore.requireSame(game.world,
                    "staging before room-shell completion");
            workforceBefore.requireSame(game.world,
                    "staging before workforce transfer");
            requireOnlyMarkerTileChanged(game.world, tilesBefore, marker,
                    "staging before room-shell completion");
            playerBefore.requireSame(game);

            int stockAfterStage = site.stock;
            FactionRoomShellConstructionAuthority.Outcome resumed =
                    FactionRoomShellConstructionAuthority.attempt(game, plan, site);
            require(resumed.handled() && resumed.success() && resumed.marker() == marker,
                    "duplicate strategic resolution should resume the existing room-shell marker");
            require(resumed.stockBefore() == stockAfterStage && resumed.stockAfter() == stockAfterStage
                            && site.stock == stockAfterStage
                            && game.baseObjects.size() == baseObjectsBefore + 1,
                    "duplicate resolution must not debit stock or add another marker");
            requireContains(resumed.message(), "IN PROGRESS", "duplicate room-shell readback");
            roomArraysBefore.requireSame(game.world,
                    "duplicate room-shell resume");
            workforceBefore.requireSame(game.world,
                    "duplicate room-shell resume workforce");
            requireOnlyMarkerTileChanged(game.world, tilesBefore, marker,
                    "duplicate room-shell resume");
            playerBefore.requireSame(game);

            int partialHours = 0;
            while (marker.constructionLaborDone + staged.workers() < staged.laborRequired()) {
                ProgressiveConstructionAuthority.FactionWorkResult partial =
                        FactionRoomShellConstructionAuthority.advanceHourly(game, site);
                partialHours++;
                require(partial.advanced() && partial.laborAdded() == ROOM_WORKERS
                                && !partial.completed() && marker.underConstruction,
                        "each unobstructed partial hour should add three absolute faction labor: "
                                + partial);
                roomArraysBefore.requireSame(game.world,
                        "partial room-shell labor hour " + partialHours);
                workforceBefore.requireSame(game.world,
                        "partial room-shell labor hour " + partialHours);
                requireOnlyMarkerTileChanged(game.world, tilesBefore, marker,
                        "partial room-shell labor hour " + partialHours);
                require(site.stock == stockAfterStage,
                        "hourly room labor must not debit faction stock again");
                playerBefore.requireSame(game);
                require(partialHours < 20, "room-shell labor loop should remain bounded");
            }
            require(partialHours == 11 && marker.constructionLaborDone == 33,
                    "three source-room workers should reach 33/35 after eleven hours");

            NpcEntity obstruction = obstruction(staged.originX() + 1, staged.originY() + 1);
            game.world.npcs.add(obstruction);
            RoomArraysSnapshot arraysBeforeBlockedCompletion = RoomArraysSnapshot.capture(game.world);
            LedgerSnapshot workforceBeforeBlockedCompletion = LedgerSnapshot.capture(game.world);
            String tilesBeforeBlockedCompletion = tileFingerprint(game.world);
            int laborBeforeBlockedCompletion = marker.constructionLaborDone;

            ProgressiveConstructionAuthority.FactionWorkResult blockedCompletion =
                    FactionRoomShellConstructionAuthority.advanceHourly(game, site);
            require(!blockedCompletion.completed() && !blockedCompletion.advanced()
                            && marker.underConstruction
                            && marker.constructionLaborDone == laborBeforeBlockedCompletion,
                    "completion-time obstruction must leave the 33/35 marker active and unchanged: "
                            + blockedCompletion);
            requireContainsAny(blockedCompletion.summary(),
                    List.of("obstruct", "blocked", "occupied"),
                    "completion-time obstruction readback");
            arraysBeforeBlockedCompletion.requireSame(game.world,
                    "blocked completion room arrays");
            workforceBeforeBlockedCompletion.requireSame(game.world,
                    "blocked completion workforce");
            require(tilesBeforeBlockedCompletion.equals(tileFingerprint(game.world)),
                    "blocked completion must not partially stamp a room, door, or connector");
            require(site.stock == stockAfterStage
                            && FactionRoomShellConstructionAuthority.activeSite(game, site) == marker,
                    "blocked completion must retain the prepaid active marker without another debit");
            playerBefore.requireSame(game);

            require(game.world.npcs.remove(obstruction),
                    "fixture obstruction should be removable for recovery");
            ProgressiveConstructionAuthority.FactionWorkResult completion =
                    FactionRoomShellConstructionAuthority.advanceHourly(game, site);
            require(completion.completed() && completion.laborAdded() == 2
                            && !marker.underConstruction
                            && marker.constructionLaborDone == LABOR_REQUIRED,
                    "clearing the obstruction should atomically recover and complete exact 35 labor: "
                            + completion);
            require(FactionRoomShellConstructionAuthority.activeSite(game, site) == null,
                    "completed annex must no longer remain an active construction target");
            require(site.stock == stockAfterStage,
                    "completion must use prepaid stock without a second debit");

            int annexRoomId = roomArraysBefore.roomCount();
            assertCompletedAnnex(game.world, sourceRoomId, annexRoomId,
                    staged.originX(), staged.originY());
            assertWorkforceTransfer(game.world, sourceRoomId, annexRoomId, ROOM_WORKERS);
            require(game.world.npcs.size() == npcCountBefore,
                    "room completion must transfer aggregate workforce without spawning NPCs");
            require(site.baseLevel == siteBaseLevelBefore && site.machineLevel == siteMachineLevelBefore,
                    "constructing space alone must not invent a completed machine upgrade");
            playerBefore.requireSame(game);

            assertCompletedReplay(game, site, plan, marker, sourceRoomId,
                    staged.originX(), staged.originY());
            playerBefore.requireSame(game);

            System.out.println("Milestone 05 faction room shell construction smoke passed.");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void assertCompletedReplay(GamePanel completedGame, NpcFactionSite completedSite,
                                              FactionStrategicPlan completedPlan, BaseObject marker,
                                              int sourceRoomId, int originX, int originY) {
        Properties saved = new Properties();
        Persistence.writeCore(completedGame, saved);

        BaseObject persistedMarker = null;
        int persistedMarkers = 0;
        for (String line : Persistence.decList(saved.getProperty("base.objects", ""))) {
            BaseObject candidate = parseBaseObject(line);
            if (candidate != null && FactionRoomShellConstructionAuthority.isRoomShellMarker(candidate)) {
                persistedMarker = candidate;
                persistedMarkers++;
            }
        }
        require(persistedMarkers == 1 && persistedMarker != null && !persistedMarker.underConstruction,
                "completed save should retain exactly one replayable room-shell marker");
        require(Objects.equals(marker.constructionLinkedSiteName,
                        persistedMarker.constructionLinkedSiteName)
                        && Objects.equals(marker.constructionLinkedPlanId,
                        persistedMarker.constructionLinkedPlanId),
                "room-shell save should retain linked site and strategic plan identity");

        GamePanel replay = new GamePanel();
        replay.shutdownRuntime();
        try {
            replay.world = blankWorld();
            addSourceRoom(replay.world);
            replay.playerX = 5;
            replay.playerY = 5;
            replay.baseObjects.clear();
            replay.baseObjects.add(persistedMarker);
            replay.world.roomPopulationLedgers.clear();
            for (String line : Persistence.decList(
                    saved.getProperty("world.populationLedgers", ""))) {
                RoomPopulationLedger ledger = RoomPopulationLedger.parse(line);
                if (ledger != null) replay.world.roomPopulationLedgers.add(ledger);
            }
            replay.npcFactionSites.clear();
            for (String line : Persistence.decList(saved.getProperty("npc.faction.sites", ""))) {
                NpcFactionSite loaded = NpcFactionSite.parse(line);
                if (loaded != null) replay.npcFactionSites.add(loaded);
            }
            replay.factionStrategicPlans.clear();
            for (String line : Persistence.decList(
                    saved.getProperty("factions.strategicPlans", ""))) {
                FactionStrategicPlan loaded = FactionStrategicPlan.parse(line);
                if (loaded != null) replay.factionStrategicPlans.add(loaded);
            }

            NpcFactionSite replaySite = findSite(replay, completedSite.name);
            FactionStrategicPlan replayPlan = findPlan(replay, completedPlan.id);
            require(replaySite != null && replayPlan != null,
                    "core save should retain the linked site and strategic plan for replay");
            require(replay.world.rooms.size() == 1,
                    "replay fixture should begin from the pristine generated source-room geometry");
            int stockBeforeReplay = replaySite.stock;
            int planSuccessBeforeReplay = replayPlan.success;

            FactionRoomShellConstructionAuthority.restoreCompletedRooms(replay);
            int annexRoomId = 1;
            assertCompletedAnnex(replay.world, sourceRoomId, annexRoomId, originX, originY);
            assertWorkforceTransfer(replay.world, sourceRoomId, annexRoomId, ROOM_WORKERS);
            require(replay.baseObjects.size() == 1
                            && FactionRoomShellConstructionAuthority.isRoomShellMarker(
                            replay.baseObjects.get(0)),
                    "replay should reuse the saved completion marker instead of creating another");
            require(replaySite.stock == stockBeforeReplay
                            && replayPlan.success == planSuccessBeforeReplay,
                    "completion replay must not debit stock or duplicate strategic success");

            RoomArraysSnapshot once = RoomArraysSnapshot.capture(replay.world);
            LedgerSnapshot onceWorkforce = LedgerSnapshot.capture(replay.world);
            String onceTiles = tileFingerprint(replay.world);
            int onceObjects = replay.baseObjects.size();
            int onceNpcs = replay.world.npcs.size();
            FactionRoomShellConstructionAuthority.restoreCompletedRooms(replay);
            once.requireSame(replay.world, "second completed-room replay");
            onceWorkforce.requireSame(replay.world, "second completed-room workforce replay");
            require(onceTiles.equals(tileFingerprint(replay.world))
                            && replay.baseObjects.size() == onceObjects
                            && replay.world.npcs.size() == onceNpcs
                            && replaySite.stock == stockBeforeReplay
                            && replayPlan.success == planSuccessBeforeReplay,
                    "repeated replay must not duplicate rooms, tiles, workers, markers, NPCs, stock, or success");
        } finally {
            replay.shutdownRuntime();
        }
    }

    private static void assertCompletedAnnex(World world, int sourceRoomId, int annexRoomId,
                                             int originX, int originY) {
        require(world.rooms.size() == annexRoomId + 1
                        && world.roomProfiles.size() == world.rooms.size()
                        && world.roomFactions.size() == world.rooms.size()
                        && world.roomSpecials.size() == world.rooms.size(),
                "completion should append exactly one synchronized room record");
        Rectangle annex = world.roomRect(annexRoomId);
        require(new Rectangle(originX, originY, EXPECTED_WIDTH, EXPECTED_HEIGHT).equals(annex),
                "completed annex should have exact 5x4 geometry at the staged origin: " + annex);
        require(FactionIdentityAuthority.sameFamily(
                        world.roomFaction(annexRoomId), Faction.MECHANIST_COLLEGIA)
                        && !Boolean.TRUE.equals(world.roomSpecials.get(annexRoomId)),
                "completed annex should be an ordinary same-family controlled room");
        requireContains(RoomOwnershipAuthority.roomName(world, annexRoomId),
                "Forge Annex", "completed annex name");
        require(FactionIdentityAuthority.sameFamily(
                        world.roomProfile(annexRoomId).faction, Faction.MECHANIST_COLLEGIA),
                "completed annex profile should retain Mechanist construction origin");

        int perimeterWalls = 0;
        int perimeterDoors = 0;
        int interiorFloors = 0;
        for (int x = originX; x < originX + EXPECTED_WIDTH; x++) {
            for (int y = originY; y < originY + EXPECTED_HEIGHT; y++) {
                require(world.roomIdAt(x, y) == annexRoomId,
                        "every annex blueprint cell should use the appended authoritative room ID at "
                                + x + "," + y);
                boolean edge = x == originX || y == originY
                        || x == originX + EXPECTED_WIDTH - 1
                        || y == originY + EXPECTED_HEIGHT - 1;
                if (edge) {
                    if (world.tiles[x][y] == '#') perimeterWalls++;
                    else if (TileDataCompilationAuthority.isDoorGlyph(world.tiles[x][y])) {
                        perimeterDoors++;
                    } else {
                        throw new AssertionError("annex perimeter contains neither wall nor door at "
                                + x + "," + y + ": " + world.tiles[x][y]);
                    }
                } else {
                    require(world.walkable(x, y),
                            "each of the six annex interior cells should be walkable at " + x + "," + y);
                    interiorFloors++;
                }
            }
        }
        require(perimeterWalls == 13 && perimeterDoors == 1 && interiorFloors == 6,
                "5x4 annex should contain exactly 13 walls, one annex door, and six interior cells");
        require(TileDataCompilationAuthority.isDoorGlyph(
                        world.tiles[SOURCE_DOOR_X][SOURCE_DOOR_Y])
                        && world.roomIdAt(SOURCE_DOOR_X, SOURCE_DOOR_Y) == sourceRoomId,
                "source-room south wall should become one source-owned door");
        require(world.walkable(CONNECTOR_X, CONNECTOR_Y)
                        && world.isCorridorGlyph(world.tiles[CONNECTOR_X][CONNECTOR_Y])
                        && world.roomIdAt(CONNECTOR_X, CONNECTOR_Y) == -1,
                "one walkable roomless corridor cell should connect source and annex doors");
        require(TileDataCompilationAuthority.isDoorGlyph(
                        world.tiles[ANNEX_DOOR_X][ANNEX_DOOR_Y])
                        && world.roomIdAt(ANNEX_DOOR_X, ANNEX_DOOR_Y) == annexRoomId,
                "annex north wall should contain the exact annex-owned door");
        require(topologicallyReachable(world,
                        new Point(SOURCE_DOOR_X, SOURCE_DOOR_Y - 1),
                        new Point(ANNEX_DOOR_X, ANNEX_DOOR_Y + 1)),
                "source interior, both doors, connector, and annex interior should form one path");
    }

    private static void assertWorkforceTransfer(World world, int sourceRoomId,
                                                int annexRoomId, int expectedWorkers) {
        RoomPopulationLedger source = findLedger(world, SOURCE_LEDGER_ID);
        require(source != null && source.roomId == sourceRoomId && source.assigned == 0,
                "completion should release all three source-room workers from their old ledger");
        ArrayList<RoomPopulationLedger> annexLedgers = new ArrayList<>();
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger != null && ledger.roomId == annexRoomId
                    && FactionIdentityAuthority.sameFamily(
                    ledger.faction, Faction.MECHANIST_COLLEGIA)) {
                annexLedgers.add(ledger);
            }
        }
        require(annexLedgers.size() == 1,
                "completion should create or reuse exactly one same-family annex workforce ledger");
        RoomPopulationLedger annex = annexLedgers.get(0);
        require(annex.assigned == expectedWorkers && annex.available == 0
                        && annex.capacity >= expectedWorkers,
                "annex ledger should receive all three assigned workers without inventing availability");
        requireContains(annex.roomName, "Forge Annex", "annex workforce room name");
        require(totalAssigned(world, Faction.MECHANIST_COLLEGIA) == expectedWorkers,
                "workforce transfer must conserve the exact same-family assigned headcount");
    }

    private static GamePanel fixtureGame() {
        GamePanel game = new GamePanel();
        game.shutdownRuntime();
        game.world = blankWorld();
        game.baseObjects.clear();
        game.npcFactionSites.clear();
        game.factionStrategicPlans.clear();
        game.playerX = 5;
        game.playerY = 5;
        game.turn = 217;
        game.worldTurn = 217L;
        game.carriedScript = 83;
        game.baseStashedScript = 47;
        game.supplies = 31;
        game.machineParts = 29;
        game.food = 23;
        game.water = 19;
        game.xp = 17;
        game.knowledgeCredits = 11;
        game.runCrafted = 6;
        game.suspicion = 9;
        game.gangHeat = 7;
        game.equippedLeftHandItem = "Masterwork servo welder";
        game.inventory.add("Fine survey auspex");
        game.baseStorage.add("Common spare component crate");
        addSourceRoom(game.world);
        configureSourceWorkforce(game.world);
        saturateSourceRoom(game);
        return game;
    }

    private static World blankWorld() {
        World world = new World(17022172L, 24, 20);
        world.sectorX = 1;
        world.sectorY = 1;
        world.zoneX = 2;
        world.zoneY = 2;
        world.floor = 4;
        world.zoneType = ZoneType.MECHANICUS_FORGE_CLOISTER;
        world.npcs.clear();
        world.mapObjects.clear();
        world.roomPopulationLedgers.clear();
        for (int x = 0; x < world.w; x++) {
            for (int y = 0; y < world.h; y++) {
                world.tiles[x][y] = '#';
                world.roomIds[x][y] = -1;
            }
        }
        return world;
    }

    private static int addSourceRoom(World world) {
        int roomId = world.rooms.size();
        Rectangle room = new Rectangle(SOURCE_RECT);
        RoomProfile profile = RoomProfile.generic();
        profile.name = "Exact Forge Source Room";
        profile.descriptor = "crowded Mechanist production source";
        profile.featureText = "existing fixtures leave no legal Micro Forge placement tile";
        profile.faction = Faction.MECHANICUS_CLOISTER_RED;
        world.rooms.add(room);
        world.roomProfiles.add(profile);
        world.roomFactions.add(Faction.MECHANICUS_CLOISTER_RED);
        world.roomSpecials.add(Boolean.FALSE);
        for (int x = room.x; x < room.x + room.width; x++) {
            for (int y = room.y; y < room.y + room.height; y++) {
                boolean edge = x == room.x || y == room.y
                        || x == room.x + room.width - 1
                        || y == room.y + room.height - 1;
                world.tiles[x][y] = edge ? '#' : '.';
                world.roomIds[x][y] = roomId;
            }
        }
        return roomId;
    }

    private static void configureSourceWorkforce(World world) {
        world.roomPopulationLedgers.clear();
        RoomPopulationLedger ledger = new RoomPopulationLedger();
        ledger.id = SOURCE_LEDGER_ID;
        ledger.roomId = 0;
        ledger.roomName = "Exact Forge Source Room";
        ledger.faction = Faction.MECHANIST_COLLEGIA;
        ledger.sourceKind = "staffed production roster";
        ledger.sourceLabel = "exact source-room construction crew";
        ledger.capacity = ROOM_WORKERS;
        ledger.available = 0;
        ledger.assigned = ROOM_WORKERS;
        world.roomPopulationLedgers.add(ledger);
    }

    private static void saturateSourceRoom(GamePanel game) {
        Rectangle room = game.world.roomRect(0);
        for (int x = room.x + 1; x < room.x + room.width - 1; x++) {
            for (int y = room.y + 1; y < room.y + room.height - 1; y++) {
                if (x == game.playerX && y == game.playerY) continue;
                MapObjectState fixture = new MapObjectState();
                fixture.id = "ROOM-SHELL-SOURCE-FIXTURE-" + x + "-" + y;
                fixture.type = "source-room-fixture";
                fixture.label = "Existing forge fixture";
                fixture.x = x;
                fixture.y = y;
                fixture.glyph = 'q';
                fixture.stockState = "occupied-source-floor";
                game.world.mapObjects.add(fixture);
            }
        }
    }

    private static boolean noLegalMicroForgeTile(GamePanel game, int roomId) {
        Rectangle room = game.world.roomRect(roomId);
        for (int x = room.x + 1; x < room.x + room.width - 1; x++) {
            for (int y = room.y + 1; y < room.y + room.height - 1; y++) {
                if (game.world.roomIdAt(x, y) != roomId || !game.world.walkable(x, y)) continue;
                if (x == game.playerX && y == game.playerY) continue;
                if (game.world.npcAt(x, y) != null || game.world.mapObjectAt(x, y) != null) continue;
                if (game.baseObjectAt(x, y) != null
                        || game.world.isDoorAccessReservedForObject(x, y)) continue;
                return false;
            }
        }
        return true;
    }

    private static NpcFactionSite site(int stock) {
        NpcFactionSite site = NpcFactionSite.create("Phase 17 Room Shell Site",
                Faction.MECHANIST_COLLEGIA, "machine shop", 1, 1, 2, 2, 4,
                "Machine part", "Tool bundle", "Scrap-Forging Doctrine");
        site.stock = stock;
        site.baseLevel = 1;
        site.machineLevel = 2;
        site.learnConstructionBlueprint(
                ConstructionBlueprintOwnershipAuthority.blueprintId(BuildRecipe.microForge()));
        return site;
    }

    private static FactionStrategicPlan plan() {
        FactionStrategicPlan plan = new FactionStrategicPlan();
        plan.id = "STRAT-ROOM-SHELL-SMOKE";
        plan.faction = Faction.MECHANICUS;
        plan.immediateGoal = FactionFacilityBlueprintUpgradeAuthority.FACTORY_UPGRADE_GOAL;
        plan.targetRoom = "Basic Forge Annex";
        plan.targetItem = "Machine part";
        return plan;
    }

    private static NpcEntity obstruction(int x, int y) {
        NpcEntity npc = new NpcEntity();
        npc.id = "ROOM-SHELL-COMPLETION-OBSTRUCTION";
        npc.name = "Passing freight inspector";
        npc.faction = Faction.HIVER;
        npc.role = "Freight Inspector";
        npc.state = "Idle";
        npc.hp = 12;
        npc.x = x;
        npc.y = y;
        return npc;
    }

    private static Map<String, Integer> exactMaterialMap() {
        LinkedHashMap<String, Integer> expected = new LinkedHashMap<>();
        expected.put("Construction supplies", 20);
        expected.put("Scrap plate", 14);
        expected.put("Rivet set", 14);
        expected.put("Bearing set", 1);
        return expected;
    }

    private static Map<String, Integer> decodeRequirements(String encoded) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        if (encoded == null || encoded.isBlank()) return out;
        for (String part : encoded.split(";")) {
            String[] fields = part.split("=", 2);
            if (fields.length != 2) continue;
            try {
                String item = fields[0].replace("%3B", ";").replace("%3D", "=");
                out.put(item, Math.max(0, Integer.parseInt(fields[1])));
            } catch (NumberFormatException ignored) {
                // Assertion below reports any malformed or missing material entry.
            }
        }
        return out;
    }

    private static BaseObject parseBaseObject(String line) {
        if (line == null) return null;
        String[] a = line.split("\\|", -1);
        if (a.length < 36) return null;
        try {
            BaseObject b = new BaseObject(a[0], a[1].isEmpty() ? '?' : a[1].charAt(0),
                    Integer.parseInt(a[2]), Integer.parseInt(a[3]), 0, 0);
            b.capacity = Integer.parseInt(a[4]);
            b.qualityName = a[5];
            b.faction = Faction.valueOf(a[6]);
            b.charges = Integer.parseInt(a[7]);
            b.integrity = Integer.parseInt(a[8]);
            b.assignedRecipe = BaseObject.decodeDelimitedField(a[9]);
            b.assignedWorker = a[10];
            b.businessOpen = Boolean.parseBoolean(a[11]);
            b.permittedBusiness = Boolean.parseBoolean(a[12]);
            b.businessHeat = Integer.parseInt(a[13]);
            b.productionQueueTarget = Integer.parseInt(a[14]);
            b.productionQueueRemaining = Integer.parseInt(a[15]);
            b.underConstruction = Boolean.parseBoolean(a[16]);
            if (!a[17].isBlank()) b.finalSymbol = a[17].charAt(0);
            b.constructionRequiredItems = a[18];
            b.constructionInsertedItems = a[19];
            b.constructionLaborRequired = Integer.parseInt(a[20]);
            b.constructionLaborDone = Integer.parseInt(a[21]);
            b.constructionVisualProgress = Integer.parseInt(a[22]);
            b.machineKnowledge = a[23];
            b.machineRepairHistory = a[24];
            if (!a[25].isBlank()) b.constructionOriginalTile = a[25].charAt(0);
            b.productionProgressTurns = Integer.parseInt(a[26]);
            b.productionMaterialPolicy = a[27];
            b.productionOutputPolicy = a[28];
            b.productionNoRoomPolicy = a[29];
            b.productionLastBlocker = BaseObject.decodeDelimitedField(a[30]);
            b.constructionOwnerMode = BaseObject.decodeDelimitedField(a[31]);
            b.constructionMaterialSource = BaseObject.decodeDelimitedField(a[32]);
            b.constructionPlanSource = BaseObject.decodeDelimitedField(a[33]);
            b.constructionLinkedSiteName = BaseObject.decodeDelimitedField(a[34]);
            b.constructionLinkedPlanId = BaseObject.decodeDelimitedField(a[35]);
            return b;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean topologicallyReachable(World world, Point start, Point goal) {
        if (world == null || start == null || goal == null
                || !world.inBounds(start.x, start.y) || !world.inBounds(goal.x, goal.y)) return false;
        boolean[][] seen = new boolean[world.w][world.h];
        ArrayDeque<Point> queue = new ArrayDeque<>();
        queue.add(start);
        seen[start.x][start.y] = true;
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            Point point = queue.removeFirst();
            if (point.equals(goal)) return true;
            for (int[] direction : directions) {
                int nx = point.x + direction[0];
                int ny = point.y + direction[1];
                if (!world.inBounds(nx, ny) || seen[nx][ny]) continue;
                char tile = world.tiles[nx][ny];
                if (!world.walkable(nx, ny)
                        && !TileDataCompilationAuthority.isDoorGlyph(tile)) continue;
                seen[nx][ny] = true;
                queue.addLast(new Point(nx, ny));
            }
        }
        return false;
    }

    private static RoomPopulationLedger findLedger(World world, String id) {
        if (world == null || id == null) return null;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger != null && id.equals(ledger.id)) return ledger;
        }
        return null;
    }

    private static NpcFactionSite findSite(GamePanel game, String name) {
        for (NpcFactionSite site : game.npcFactionSites) {
            if (site != null && Objects.equals(name, site.name)) return site;
        }
        return null;
    }

    private static FactionStrategicPlan findPlan(GamePanel game, String id) {
        for (FactionStrategicPlan plan : game.factionStrategicPlans) {
            if (plan != null && Objects.equals(id, plan.id)) return plan;
        }
        return null;
    }

    private static int totalAssigned(World world, Faction faction) {
        long total = 0;
        for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
            if (ledger != null && FactionIdentityAuthority.sameFamily(ledger.faction, faction)) {
                total += Math.max(0, ledger.assigned);
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, total);
    }

    private static String tileFingerprint(World world) {
        StringBuilder out = new StringBuilder(world.w * world.h);
        for (int y = 0; y < world.h; y++) {
            for (int x = 0; x < world.w; x++) out.append(world.tiles[x][y]);
        }
        return out.toString();
    }

    private static void requireOnlyMarkerTileChanged(World world, String before,
                                                     BaseObject marker, String label) {
        int changes = 0;
        for (int y = 0; y < world.h; y++) {
            for (int x = 0; x < world.w; x++) {
                int index = y * world.w + x;
                if (before.charAt(index) == world.tiles[x][y]) continue;
                changes++;
                require(x == marker.x && y == marker.y && world.tiles[x][y] == '?',
                        label + " changed a non-marker tile at " + x + "," + y);
            }
        }
        require(changes == 1,
                label + " should reserve exactly one visible marker tile, changed=" + changes);
    }

    private static void requireContains(String text, String expected, String label) {
        require(text != null && text.contains(expected),
                "Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContainsAny(String text, List<String> expected, String label) {
        String lower = text == null ? "" : text.toLowerCase();
        for (String candidate : expected) {
            if (candidate != null && lower.contains(candidate.toLowerCase())) return;
        }
        throw new AssertionError("Expected " + label + " to contain one of " + expected + ": " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private record RoomArraysSnapshot(int roomCount, List<String> rectangles,
                                      List<String> profileFacts, List<Faction> factions,
                                      List<Boolean> specials, String roomIds) {
        static RoomArraysSnapshot capture(World world) {
            ArrayList<String> rectangles = new ArrayList<>();
            for (Rectangle room : world.rooms) {
                rectangles.add(room == null ? "null"
                        : room.x + "," + room.y + "," + room.width + "," + room.height);
            }
            ArrayList<String> profiles = new ArrayList<>();
            for (RoomProfile profile : world.roomProfiles) {
                profiles.add(profile == null ? "null" : profile.name + "|" + profile.descriptor
                        + "|" + profile.featureText + "|"
                        + (profile.faction == null ? Faction.NONE.name() : profile.faction.name()));
            }
            StringBuilder ids = new StringBuilder(world.w * world.h * 2);
            for (int y = 0; y < world.h; y++) {
                for (int x = 0; x < world.w; x++) ids.append(world.roomIds[x][y]).append(',');
            }
            return new RoomArraysSnapshot(world.rooms.size(), rectangles, profiles,
                    new ArrayList<>(world.roomFactions),
                    new ArrayList<>(world.roomSpecials), ids.toString());
        }

        void requireSame(World world, String label) {
            RoomArraysSnapshot after = capture(world);
            require(equals(after), label + " mutated room arrays before an atomic completion: before="
                    + this + " after=" + after);
        }
    }

    private record LedgerSnapshot(List<String> lines) {
        static LedgerSnapshot capture(World world) {
            ArrayList<String> lines = new ArrayList<>();
            for (RoomPopulationLedger ledger : world.roomPopulationLedgers) {
                if (ledger != null) lines.add(ledger.saveLine());
            }
            return new LedgerSnapshot(lines);
        }

        void requireSame(World world, String label) {
            LedgerSnapshot after = capture(world);
            require(equals(after), label + " mutated workforce ledgers: before="
                    + this + " after=" + after);
        }
    }

    private record PlayerSnapshot(int carriedScript, int bankedScript, int supplies, int machineParts,
                                  int food, int water, int xp, int knowledgeCredits, int runCrafted,
                                  int suspicion, int gangHeat, int turn, long worldTurn,
                                  String leftHand, String rightHand,
                                  List<String> inventory, List<String> baseStorage,
                                  Set<String> constructionBlueprints,
                                  Set<String> expansionReactions) {
        static PlayerSnapshot capture(GamePanel game) {
            return new PlayerSnapshot(game.carriedScript, game.baseStashedScript, game.supplies,
                    game.machineParts, game.food, game.water, game.xp, game.knowledgeCredits,
                    game.runCrafted, game.suspicion, game.gangHeat, game.turn, game.worldTurn,
                    game.equippedLeftHandItem, game.equippedRightHandItem,
                    new ArrayList<>(game.inventory), new ArrayList<>(game.baseStorage),
                    new LinkedHashSet<>(game.unlockedConstructionBlueprints),
                    new LinkedHashSet<>(game.constructionExpansionReactions));
        }

        void requireSame(GamePanel game) {
            require(carriedScript == game.carriedScript && bankedScript == game.baseStashedScript,
                    "faction room construction must not spend player script");
            require(supplies == game.supplies && machineParts == game.machineParts,
                    "faction room construction must not consume player materials");
            require(food == game.food && water == game.water,
                    "faction room construction must not consume player food or water");
            require(xp == game.xp && knowledgeCredits == game.knowledgeCredits
                            && runCrafted == game.runCrafted,
                    "faction room construction must not grant player XP, knowledge, or crafted count");
            require(suspicion == game.suspicion && gangHeat == game.gangHeat,
                    "faction room construction must not mutate player suspicion or heat");
            require(turn == game.turn && worldTurn == game.worldTurn,
                    "faction room construction must not spend player turns");
            require(inventory.equals(game.inventory) && baseStorage.equals(game.baseStorage),
                    "faction room construction must not mutate player inventory or storage");
            require(Objects.equals(leftHand, game.equippedLeftHandItem)
                            && Objects.equals(rightHand, game.equippedRightHandItem),
                    "faction room construction must not change player equipment");
            require(constructionBlueprints.equals(game.unlockedConstructionBlueprints)
                            && expansionReactions.equals(game.constructionExpansionReactions),
                    "faction room construction must not alter player blueprint or expansion receipts");
        }
    }

    private Milestone05FactionRoomShellConstructionSmoke() { }
}
