package mechanist;

import java.util.Arrays;
import java.util.Random;

/** Real client transactions and a second run in the same panel. */
final class CoreGameplayBoundarySmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        game.timer.stop();
        try {
            game.world = new World(941L, 24, 24);
            game.seed = 941L;
            game.worldSetup.zoneSize = 0;
            game.worldSetup.npcDensity = 0;
            game.worldSetup.simulationAge = 0;
            for (char[] column : game.world.tiles) Arrays.fill(column, '.');
            game.active = Candidate.random(new Random(941));
            game.playerX = game.playerY = 12;
            ItemProvenanceRecord first = ItemProvenanceRecord.found("Machine part", game.world, 0, "first supplier");
            ItemProvenanceRecord second = ItemProvenanceRecord.found("Machine part", game.world, 0, "second supplier");
            game.addInventoryItem("Machine part", first);
            game.addInventoryItem("Machine part", second);
            check(ProductionContainerAuthority.countAvailable(game, "Machine part") == 2, "added units must be usable production inputs");
            var player = game.itemContainers.get(GamePanel.CONTAINER_PLAYER_INVENTORY);
            String firstId = player.itemInstanceIds.get(0);
            game.rebuildItemContainersFromLegacyLists();
            game.rebuildItemContainersFromLegacyLists();
            check(player.itemInstanceIds.size() == 2 && player.itemInstanceIds.get(0).equals(firstId), "rebuild preserves exact identities and multiplicity");
            check(game.itemInstances.get(firstId).provenance == first, "supplier provenance survives admission/rebuild");
            ItemInstance moved = game.transferContainerItemByName(GamePanel.CONTAINER_PLAYER_INVENTORY,
                    game.inventory, "Machine part", GamePanel.CONTAINER_BASE_STORAGE, "Base storage", game.baseStorage, "test transfer");
            game.rebuildItemContainersFromLegacyLists();
            check(moved != null && moved.id.equals(firstId), "transfer must preserve identity");
            check(game.inventory.size() == 1 && game.baseStorage.size() == 1 && game.itemInstances.size() == 2, "transfer/rebuild must not duplicate units");
            check(ProductionContainerAuthority.consumeOne(game, "Machine part", "test consumption").success, "consume transferred material");
            game.addInventoryItem("Wire bundle", null);
            check(game.itemInstances.size() == 2, "adding after deletion must not overwrite a surviving ID");
            check(game.itemInstances.values().stream().anyMatch(i -> i.provenance == second), "remaining supplier unit retained");
            game.inventory.clear();
            game.inventory.add("Ration wrapper");
            game.rebuildItemContainersFromLegacyLists();
            game.selectedInventoryIndex = 0;
            game.food = 10;
            int turn = game.turn;
            game.useSelectedInventoryItemBody();
            check(game.food == 10 && game.inventory.contains("Ration wrapper") && game.turn == turn, "junk wrappers must not become meals");
            game.inventory.clear();
            game.inventory.add("Emergency rations");
            game.rebuildItemContainersFromLegacyLists();
            game.useSelectedInventoryItemBody();
            check(game.food > 10 && game.inventory.isEmpty() && player.itemInstanceIds.isEmpty(), "real food consumes one unit in both views");
            game.addInventoryItem("Construction supplies", null);
            game.selectedInventoryIndex = 0;
            game.useSelectedInventoryItemBody();
            check(game.supplies == 1 && game.inventory.isEmpty() && player.itemInstanceIds.isEmpty(), "carried supplies pack into the construction pool exactly once");
            game.addInventoryItem("Reclaimed textile bundle", null);
            BaseObject cot = ProgressiveConstructionAuthority.createSite(BuildRecipe.cot(), 13, 12, 3);
            game.baseObjects.add(cot);
            ProgressiveConstructionAuthority.contribute(game, cot, 3, true);
            check(!cot.underConstruction && game.supplies == 0 && !game.inventory.contains("Reclaimed textile bundle"), "first cot can consume packed supplies, a registered textile and real labor");
            ItemProvenanceRecord carried = ItemProvenanceRecord.found("Machine part", game.world, game.turn, "saved supplier");
            game.addInventoryItem("Machine part", carried);
            String carriedId = player.itemInstanceIds.get(0);
            java.util.Properties checkpoint = new java.util.Properties();
            Persistence.writeCore(game, checkpoint);
            GamePanel resumed = new GamePanel();
            resumed.timer.stop();
            try {
                Persistence.readCore(resumed, checkpoint);
                check(resumed.baseObjects.stream().anyMatch(b -> b.x == 13 && b.y == 12 && !b.underConstruction && b.symbol == 'c'), "completed cot survives full save/load");
                check(resumed.inventory.size() == 1 && resumed.containerItemCount(GamePanel.CONTAINER_PLAYER_INVENTORY) == 1, "saved inventory views resume with one exact unit");
                ItemInstance restoredUnit = resumed.itemInstances.get(carriedId);
                check(restoredUnit != null && restoredUnit.provenance != null && restoredUnit.provenance.unitId.equals(carried.unitId), "unit identity and supplier provenance survive full save/load");
                resumed.addInventoryItem("Wire bundle", null);
                check(resumed.itemInstances.size() == 2 && resumed.itemInstances.get(carriedId) == restoredUnit, "post-load additions cannot overwrite the restored unit");
                BaseObject restoredCot = resumed.baseObjects.stream().filter(b -> b.x == 13 && b.y == 12).findFirst().orElseThrow();
                resumed.fatigue = 8; resumed.sleepNeed = 30;
                resumed.setSize(1280, 720);
                resumed.lookX = restoredCot.x; resumed.lookY = restoredCot.y;
                resumed.confirmInteractionBody();
                java.awt.image.BufferedImage canvas = new java.awt.image.BufferedImage(1280, 720, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                var graphics = canvas.createGraphics();
                try { resumed.paintComponent(graphics); } finally { graphics.dispose(); }
                check(resumed.buttons.stream().noneMatch(b -> "Craft".equals(b.label)), "cot must not offer a machine workbench");
                ButtonBox rest = resumed.buttons.stream().filter(b -> "Rest".equals(b.label)).findFirst().orElseThrow();
                int beforeRest = resumed.turn;
                rest.action.run();
                check(resumed.fatigue == 6 && resumed.sleepNeed == 20 && resumed.turn == beforeRest + 1, "rest action on the persisted cot uses existing restoration rates and one turn");
                resumed.playerX = resumed.playerY = 0;
                rest.action.run();
                check(resumed.fatigue == 6 && resumed.sleepNeed == 20 && resumed.turn == beforeRest + 1, "remote cot use is refused without mutation");
            } finally { resumed.shutdownRuntime(); }

            game.seed = 941;
            game.worldSetup = WorldSetupSettings.standard();
            game.atlas = WorldAtlas.preview(game.seed, game.worldSetup);
            game.atlas.slices[0][0][1][1][6] = game.world;
            game.unlockedSkillNodes.add("investigation-trace-reading");
            game.factionStanding.put(Faction.BANDIT, 87);
            game.baseClaimed = true; game.claimedRoomId = 17;
            game.equippedRightHandItem = "Old weapon";
            game.loadedWeaponShots.put("Old weapon", 99);
            game.knowledgeXpProgress = 24;
            game.startPackagedClientNewGameWith(Candidate.random(new Random(942)), game.worldSetup);
            check(game.unlockedSkillNodes.isEmpty() && game.factionStanding.get(Faction.BANDIT) == 0, "new candidate must not inherit capabilities/standing");
            check(!game.baseClaimed && game.claimedRoomId == -1 && game.baseX == -1, "new run must not inherit base ownership");
            check(game.loadedWeaponShots.isEmpty() && game.equippedRightHandItem.equals("RIGHT EMPTY"), "new candidate must not inherit equipment/ammunition");
            check(game.knowledgeXpProgress == 0 && game.knowledgeCredits == 0, "new candidate must not inherit knowledge earning progress");
            check(game.containerItemCount(GamePanel.CONTAINER_PLAYER_INVENTORY) == game.inventory.size(), "starting kit must be registered");
            System.out.println("CoreGameplayBoundarySmoke PASS inventoryIdentity=true provenance=true transfers=true noJunkMeals=true newRunIsolation=true");
        } finally {
            game.shutdownRuntime();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
