package mechanist;

import java.util.List;

/** Smoke for dismantling unfinished staged construction sites. */
final class Milestone03ProgressiveConstructionDismantleSmoke {
    public static void main(String[] args) {
        List<String> audit = ProgressiveConstructionAuthority.definitionAuditLines();
        requireContains(audit, "unfinished staged sites can be dismantled", "dismantle audit");
        requireContains(audit, "dismantle summaries identify the site location", "dismantle location audit");
        requireContains(audit, "inserted materials are recovered", "material recovery audit");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.baseObjects.clear();
        game.supplies = 1;
        game.machineParts = 2;
        game.baseStorage.clear();

        BaseObject site = ProgressiveConstructionAuthority.createSite(BuildRecipe.shopCounter(), 12, 18, 7);
        site.constructionInsertedItems = "Construction supplies=2;Machine part=1;Sensor lens=1";
        site.constructionLaborDone = 4;
        site.constructionVisualProgress = 58;
        game.baseObjects.add(site);
        if (game.world != null && game.world.inBounds(site.x, site.y)) game.world.tiles[site.x][site.y] = site.symbol;

        ProgressiveConstructionAuthority.DismantleResult result = ProgressiveConstructionAuthority.dismantle(game, site);
        require(result.removed(), "dismantle should remove the staged site");
        require(result.recoveredSupplies() == 2, "dismantle should recover inserted supplies");
        require(result.recoveredMachineParts() == 1, "dismantle should recover inserted machine parts");
        require(result.recoveredNamedItems() == 1, "dismantle should recover named components");
        require(game.supplies == 3, "recovered supplies should return to pooled supplies");
        require(game.machineParts == 3, "recovered parts should return to pooled machine parts");
        require(game.baseStorage.contains("Sensor lens"), "named component should return to base storage");
        require(!game.baseObjects.contains(site), "site should not remain in base objects");
        require(game.world == null || !game.world.inBounds(site.x, site.y) || game.world.tiles[site.x][site.y] == '.',
                "placeholder tile should be cleared when it belongs to the staged site");
        requireContains(result.summary(), "Dismantled Under construction: Licensed Shop Counter", "dismantle summary");
        requireContains(result.summary(), "at 12,18", "dismantle location summary");
        requireContains(result.summary(), "Recovered 2 construction supplies, 1 machine part(s), and 1 named component(s)", "recovery summary");
        requireContains(result.summary(), "Labor progress was not recoverable", "labor loss summary");

        ProgressiveConstructionAuthority.DismantleResult missing = ProgressiveConstructionAuthority.dismantle(game, site);
        require(!missing.removed(), "already removed site should not dismantle again");
        requireContains(missing.summary(), "No unfinished construction site is selected", "missing-site summary");

        BaseObject completed = new BaseObject("Licensed Shop Counter", 'B', 4, 4, 0, 0);
        game.baseObjects.add(completed);
        ProgressiveConstructionAuthority.DismantleResult completedResult = ProgressiveConstructionAuthority.dismantle(game, completed);
        require(!completedResult.removed(), "completed objects should not be dismantled by staged owner");
        require(game.baseObjects.contains(completed), "completed object should remain");

        for (String line : List.of(result.summary(), missing.summary(), completedResult.summary())) {
            if (PlayerFacingText.containsLikelyLeak(line)) {
                throw new AssertionError("Dismantle text leaked implementation wording: " + line);
            }
        }
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProgressiveConstructionDismantleSmoke() { }
}
