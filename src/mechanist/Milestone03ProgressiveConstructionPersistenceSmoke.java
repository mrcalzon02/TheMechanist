package mechanist;

import java.util.List;
import java.util.Properties;

/** Smoke for staged construction save/load persistence. */
final class Milestone03ProgressiveConstructionPersistenceSmoke {
    public static void main(String[] args) {
        BuildRecipe recipe = BuildRecipe.shopCounter();
        BaseObject site = ProgressiveConstructionAuthority.createSite(recipe, 12, 18, 7);
        site.constructionInsertedItems = "Construction supplies=2;Machine part=1";
        site.constructionLaborDone = 3;
        site.constructionVisualProgress = 53;
        site.constructionOriginalTile = '/';
        site.machineKnowledge = "Retail Counter Assembly";
        site.machineRepairHistory = "field repair before staging";

        String[] saved = site.saveLine().split("\\|", -1);
        require(saved.length == 26, "base object save line should preserve 26 fields");
        require("true".equals(saved[16]), "under-construction field should be saved");
        require("B".equals(saved[17]), "final symbol should be saved");
        require(saved[18].contains("Construction supplies=3"), "required supplies should be saved");
        require(saved[18].contains("Machine part=1"), "required machine part should be saved");
        require(saved[19].contains("Construction supplies=2"), "inserted supplies should be saved");
        require(saved[20].equals("7"), "labor requirement should be saved");
        require(saved[21].equals("3"), "labor progress should be saved");
        require(saved[22].equals("53"), "visual progress should be saved");
        require(saved[23].equals("Retail Counter Assembly"), "machine knowledge field should remain after construction fields");
        require(saved[24].equals("field repair before staging"), "repair history field should remain after construction fields");
        require(saved[25].equals("/"), "original tile field should be saved after existing base-object fields");

        GamePanel writer = new GamePanel();
        if (writer.timer != null) writer.timer.stop();
        writer.baseObjects.clear();
        writer.baseObjects.add(site);
        writer.baseClaimed = true;
        writer.baseX = 12;
        writer.baseY = 18;
        writer.claimedRoomId = 4;
        Properties p = new Properties();
        Persistence.writeCore(writer, p);
        if (writer.timer != null) writer.timer.stop();

        GamePanel reader = new GamePanel();
        if (reader.timer != null) reader.timer.stop();
        reader.baseObjects.clear();
        Persistence.readCore(reader, p);
        if (reader.timer != null) reader.timer.stop();

        require(reader.baseObjects.size() == 1, "one staged site should load back");
        BaseObject loaded = reader.baseObjects.get(0);
        require(loaded.underConstruction, "loaded object should remain under construction");
        require(loaded.symbol == '?', "loaded staged site should keep placeholder symbol");
        require(loaded.finalSymbol == 'B', "loaded staged site should preserve final symbol");
        require("Under construction: Licensed Shop Counter".equals(loaded.name), "loaded site should keep construction name");
        require("Licensed Shop Counter".equals(loaded.assignedRecipe), "loaded site should preserve assigned recipe");
        require(loaded.constructionRequiredItems.contains("Construction supplies=3"), "loaded site should preserve required supplies");
        require(loaded.constructionRequiredItems.contains("Machine part=1"), "loaded site should preserve required part");
        require(loaded.constructionInsertedItems.contains("Construction supplies=2"), "loaded site should preserve inserted supplies");
        require(loaded.constructionInsertedItems.contains("Machine part=1"), "loaded site should preserve inserted part");
        require(loaded.constructionLaborRequired == 7, "loaded site should preserve labor requirement");
        require(loaded.constructionLaborDone == 3, "loaded site should preserve labor progress");
        require(loaded.constructionVisualProgress == 53, "loaded site should preserve visual progress");
        require(loaded.constructionOriginalTile == '/', "loaded site should preserve original walkable tile");
        require("Common".equals(loaded.qualityName), "loaded site should preserve quality");
        require(loaded.faction == Faction.NONE, "loaded site should preserve faction");
        require("Retail Counter Assembly".equals(loaded.machineKnowledge), "loaded site should preserve later machine knowledge field");
        require("field repair before staging".equals(loaded.machineRepairHistory), "loaded site should preserve later repair history field");
        require(reader.world != null && reader.world.inBounds(loaded.x, loaded.y), "loaded site should be in world bounds");
        require(reader.world.tiles[loaded.x][loaded.y] == '?', "loaded world tile should show staged placeholder");

        List<String> inspection = ProgressiveConstructionAuthority.inspectionLines(loaded);
        requireContains(inspection, "Construction status: staged site", "loaded inspection status");
        requireContains(inspection, "Construction supplies 2/3", "loaded material progress");
        requireContains(inspection, "labor=3/7", "loaded labor progress");
        requireContains(inspection, "finished work becomes B", "loaded completion target");

        for (String line : inspection) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Loaded construction inspection leaked implementation text: " + line);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProgressiveConstructionPersistenceSmoke() { }
}
