package mechanist;

import java.util.List;

/** Smoke for claimed-room production doctrine sharing. */
final class Milestone03ProductionFacilityKnowledgeSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.unlockedKnowledges.clear();
        game.baseObjects.clear();
        game.baseClaimed = true;
        game.claimedRoomId = 1;
        game.baseX = 10;
        game.baseY = 10;

        BaseObject forge = station("Selected Forge", 'f', 10, 10, 5);
        BaseObject archive = station("Pattern Archive", 'l', 11, 10, 5);
        archive.machineKnowledge = "Fine Tools Patterns";
        game.baseObjects.add(forge);
        game.baseObjects.add(archive);
        CraftingRecipe recipe = new CraftingRecipe("Fine Tool", "Tool", Faction.HIVER,
                "Fine Tools Patterns", 'f', 0, 0, 1, 0, 1, 0, 0, "Mechanics", "test");

        ProductionKnowledgeSourceAuthority.KnowledgeSource shared = ProductionKnowledgeSourceAuthority.evaluate(
                game, forge, recipe.requiredKnowledge);
        require(shared.available() && shared.facilitySupplied(), "claimed-room station should supply doctrine");
        require("Pattern Archive".equals(shared.facilityProvider()), "source should identify the provider station");
        requireContains(shared.lines(), "supplied by Pattern Archive", "facility source explanation");
        require(recipe.visibleTo(game), "facility doctrine should reveal the recipe");
        require(recipe.blockingProblemForMachine(game, forge) == null, "facility doctrine should satisfy execution gate");
        require(ProductionQualityTraceAuthority.evaluate(shared.effectiveKnowledge(), recipe.requiredKnowledge,
                "Fine", -1).doctrineTier() == 4, "facility doctrine should contribute its quality tier");

        archive.integrity = 0;
        require(!ProductionKnowledgeSourceAuthority.evaluate(game, forge, recipe.requiredKnowledge).available(),
                "broken provider must not share doctrine");
        archive.integrity = 5;
        game.baseClaimed = false;
        require(!ProductionKnowledgeSourceAuthority.evaluate(game, forge, recipe.requiredKnowledge).available(),
                "unclaimed work area must not share facility doctrine");
        if (game.timer != null) game.timer.stop();
    }

    private static BaseObject station(String name, char symbol, int x, int y, int integrity) {
        BaseObject station = new BaseObject(name, symbol, x, y, 0, 0);
        station.integrity = integrity;
        return station;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProductionFacilityKnowledgeSmoke() { }
}
