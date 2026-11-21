package mechanist;

import java.util.Arrays;
import java.util.Random;
import java.nio.file.Files;

public final class SaveSlotJourneyProbe {
    public static void main(String[] args) {
        GamePanel game = new GamePanel(), resumed = new GamePanel();
        game.timer.stop(); resumed.timer.stop();
        try {
            game.seed = 94120L;
            game.worldSetup.zoneSize = 0; game.worldSetup.npcDensity = 0; game.worldSetup.simulationAge = 0;
            game.world = new World(game.seed, 24, 24);
            for (char[] column : game.world.tiles) Arrays.fill(column, '.');
            game.active = Candidate.random(new Random(game.seed));
            game.playerX = game.playerY = 12;
            game.unlockedKnowledges.add("Underhive Basics");
            // Completing the cot below earns another four XP through normal construction.
            game.gainXp("Mechanics", 20, "work before checkpoint");
            game.addInventoryItem("Construction supplies", null);
            game.selectedInventoryIndex = 0; game.useSelectedInventoryItemBody();
            game.addInventoryItem("Reclaimed textile bundle", null);
            BaseObject cot = ProgressiveConstructionAuthority.createSite(BuildRecipe.cot(), 13, 12, 3);
            game.baseObjects.add(cot); ProgressiveConstructionAuthority.contribute(game, cot, 3, true);
            ItemProvenanceRecord provenance = ItemProvenanceRecord.found("Machine part", game.world, game.turn, "save-slot supplier");
            game.addInventoryItem("Machine part", provenance);
            String id = game.itemContainers.get(GamePanel.CONTAINER_PLAYER_INVENTORY).itemInstanceIds.get(0);
            game.writeSaveFile(1, false);
            require(Files.isRegularFile(SaveSlotSurfaceApi.savePathForSlot(1)), "real slot file written");
            require(resumed.loadSaveSlot(1, "integration checkpoint"), "real slot file loaded");
            require(resumed.knowledgeXpProgress == 24 && resumed.xp == 24 && resumed.knowledgeCredits == 0, "mid-band earnings survive disk");
            require(resumed.itemInstances.get(id).provenance.unitId.equals(provenance.unitId), "supplier identity survives disk");
            require(resumed.baseObjects.stream().anyMatch(b -> b.symbol == 'c' && !b.underConstruction), "built cot survives disk");
            resumed.gainXp("Mechanics", 1, "work after resume");
            require(resumed.knowledgeCredits == 1 && resumed.knowledgeXpProgress == 0, "saved band awards once");
            resumed.writeSaveFile(1, false);
            require(game.loadSaveSlot(1, "second resume"), "updated checkpoint loads");
            game.gainXp("Mechanics", 0, "no new work");
            require(game.knowledgeCredits == 1 && game.knowledgeXpProgress == 0, "reload does not replay credit award");
            System.out.println("SaveSlotJourneyProbe PASS onDiskSlot=true builtCot=true itemIdentity=true knowledgeBandResume=true noReplay=true");
        } finally { game.shutdownRuntime(); resumed.shutdownRuntime(); }
    }
    static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
