package mechanist;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Random;

/** Exercises real stair actions with a camp, stored units and an assigned facility. */
final class CampTravelIntegrationSmoke {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            var root = java.nio.file.Files.createTempDirectory("mechanist-camp-journey-").toAbsolutePath();
            String executable = java.nio.file.Path.of(System.getProperty("java.home"), "bin",
                    System.getProperty("os.name", "").toLowerCase().contains("win") ? "java.exe" : "java").toString();
            Process child = new ProcessBuilder(executable, "-Djava.awt.headless=true",
                    "-Dmechanist.storage.root=" + root,
                    "-Djava.io.tmpdir=" + java.nio.file.Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath(),
                    "-cp", System.getProperty("java.class.path"), CampTravelIntegrationSmoke.class.getName(), "--child")
                    .inheritIO().start();
            if (!child.waitFor(300, java.util.concurrent.TimeUnit.SECONDS)) {
                child.destroyForcibly().waitFor(); throw new AssertionError("camp journey timed out: " + root);
            }
            require(child.exitValue() == 0, "camp journey failed: " + root);
            try (var paths = java.nio.file.Files.walk(root)) {
                for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) java.nio.file.Files.delete(path);
            }
            return;
        }
        require(args.length == 1 && "--child".equals(args[0]), "unknown camp journey arguments");
        GamePanel game = new GamePanel();
        game.timer.stop();
        try {
            WorldSetupSettings setup = WorldSetupSettings.standard(); setup.zoneSize = 0;
            game.worldSetup = setup;
            game.seed = 731113L;
            game.atlas = WorldAtlas.preview(game.seed, setup);
            var sites = MultiFloorBuildingPlacementAuthority.planSites(game.seed, 1, 1, 2, 2, 500, 500);
            World lower = level(2, sliceSeed(game.seed, 2), sites), upper = level(3, sliceSeed(game.seed, 3), sites);
            game.atlas.slices[0][0][1][1][2] = lower;
            game.atlas.slices[0][0][1][1][4] = upper;
            game.atlas.floor = 2;
            game.world = lower;
            game.active = Candidate.random(new Random(73));
            game.screen = GamePanel.Screen.GAME;
            Rectangle stair = sites.get(0).translate(sites.get(0).blueprint().plate(2)
                    .space(VerticalBuildingBlueprintAuthority.Use.STAIRWELL).bounds());
            game.playerX = stair.x + 1; game.playerY = stair.y + 1;
            game.baseClaimed = true; game.claimedRoomId = 0;
            game.baseX = game.playerX; game.baseY = game.playerY;
            BaseObject cot = new BaseObject("Home cot", 'c', game.playerX + 3, game.playerY, 0, 0);
            game.baseObjects.add(cot);
            game.scavengeCooldownUntilTurn.put(42, 100);
            game.baseStorage.add("Machine part");
            game.rebuildItemContainersFromLegacyLists();
            String unit = game.itemContainers.get(GamePanel.CONTAINER_BASE_STORAGE).itemInstanceIds.get(0);
            String factionId = SaveEfficiencyAuthority.playerFactionIdFor(game, SaveEfficiencyAuthority.playerIdFor(game));
            require(MultiFloorBuildingTransitAuthority.handlePlayerFloorKey(game, 1), "stairs did not handle ascent");
            require(game.world == upper, "ascent did not reach the destination floor");
            require(game.baseObjects.isEmpty() && game.baseStorage.isEmpty() && !game.baseClaimed,
                    "camp, storage or room ownership teleported to another floor");
            require(game.itemInstances.containsKey(unit), "stored unit was destroyed when leaving the camp");
            require(game.playerFaction() == Faction.HIVER, "leaving camp erased faction membership");
            require(factionId.equals(SaveEfficiencyAuthority.playerFactionIdFor(game, SaveEfficiencyAuthority.playerIdFor(game))),
                    "leaving camp changed faction identity");
            require(game.scavengeCooldownUntilTurn.isEmpty(), "room-local cooldowns leaked between floors");
            game.addInventoryItem("Wire bundle", null);
            int carried = game.inventory.size(), instances = game.itemInstances.size();
            require(game.transferContainerItemByName(GamePanel.CONTAINER_PLAYER_INVENTORY, game.inventory,
                    "Wire bundle", GamePanel.CONTAINER_BASE_STORAGE, "Base storage", game.baseStorage, "stash") == null
                    && game.inventory.size() == carried && game.itemInstances.size() == instances,
                    "actual remote storage transfer bypassed access authority or mutated its source");
            require(ActorAccessAuthority.containerTransferProblem(game, GamePanel.CONTAINER_PLAYER_INVENTORY,
                    GamePanel.CONTAINER_BASE_STORAGE, "stash") != null, "remote camp storage accepted a deposit");
            game.writeSaveFile(1, false);
            var slot = new java.util.Properties();
            try (var in = java.nio.file.Files.newInputStream(SaveSlotSurfaceApi.savePathForSlot(1))) { slot.load(in); }
            String campKey = "base.camp.v1." + PlayerCampStateAuthority.key(lower);
            var corrupted = new java.util.Properties(); corrupted.putAll(slot);
            corrupted.setProperty(campKey, "not base64");
            requireRejectedCamp(game, corrupted, "malformed camp payload");
            String storedId = PlayerCampStateAuthority.storage(PlayerCampStateAuthority.key(lower));
            ContainerRecord removed = game.itemContainers.remove(storedId);
            try { requireRejectedCamp(game, slot, "missing stored camp container"); }
            finally { game.itemContainers.put(storedId, removed); }
            GamePanel resumed = new GamePanel(); resumed.timer.stop();
            try {
                require(resumed.loadSaveSlot(1, "camp journey from another floor"), "offsite campaign did not load");
                require(!resumed.baseClaimed && resumed.baseObjects.isEmpty() && resumed.baseStorage.isEmpty(),
                        "loading offsite materialized the home camp on the wrong floor");
                require(MultiFloorBuildingTransitAuthority.handlePlayerFloorKey(resumed, -1)
                        && resumed.world.floor == 2 && resumed.baseClaimed && resumed.baseObjects.size() == 1,
                        "real return after disk load lost the home camp");
                require(resumed.itemContainers.get(GamePanel.CONTAINER_BASE_STORAGE).itemInstanceIds.equals(java.util.List.of(unit))
                        && factionId.equals(SaveEfficiencyAuthority.playerFactionIdFor(resumed, SaveEfficiencyAuthority.playerIdFor(resumed))),
                        "disk load changed storage units or faction identity");
            } finally { resumed.shutdownRuntime(); }
            require(MultiFloorBuildingTransitAuthority.handlePlayerFloorKey(game, -1), "stairs did not handle descent");
            require(game.world == lower && game.baseClaimed && game.baseObjects.size() == 1
                    && "Home cot".equals(game.baseObjects.get(0).name), "returning lost the camp or cot");
            require(game.baseStorage.equals(java.util.List.of("Machine part"))
                    && game.itemContainers.get(GamePanel.CONTAINER_BASE_STORAGE).itemInstanceIds.equals(java.util.List.of(unit)),
                    "returning duplicated or replaced the stored unit");
            require(game.scavengeCooldownUntilTurn.get(42) == 100, "returning discarded room-local cooldowns");
            System.out.println("CampTravelIntegrationSmoke PASS realStairs=true localCamp=true exactStoredUnit=true factionContinuity=true offsiteDiskSave=true");
        } finally { game.shutdownRuntime(); }
    }

    private static void requireRejectedCamp(GamePanel game, java.util.Properties save, String description) {
        try { PlayerCampStateAuthority.read(game, save); }
        catch (IllegalStateException expected) { return; }
        throw new AssertionError("Accepted " + description);
    }

    private static long sliceSeed(long seed, int floor) {
        return seed ^ 1000003L ^ 9176L ^ (2*131071L) ^ (2*524287L) ^ (floor*8191L) ^ 0xFACEL;
    }

    private static World level(int floor, long seed, java.util.List<MultiFloorBuildingPlacementAuthority.Site> sites) {
        World world = new World(seed, 500, 500);
        world.sectorX = world.sectorY = 1; world.zoneX = world.zoneY = 2;
        world.floor = floor; world.zoneType = ZoneType.HAB_STACK;
        for (char[] column : world.tiles) Arrays.fill(column, '#');
        Rectangle plaza = world.centralPlazaRect(); world.carve(plaza); world.rooms.add(plaza);
        RoadGridIntegrationAuthority.apply(world, new Random(seed));
        world.buildingSites = sites;
        MultiFloorBuildingPlacementAuthority.stampFloor(world);
        return world;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
