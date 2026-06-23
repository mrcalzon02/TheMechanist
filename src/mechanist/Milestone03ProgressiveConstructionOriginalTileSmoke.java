package mechanist;

import java.awt.Point;
import java.util.List;

/** Smoke for preserving and restoring the tile under an unfinished construction site. */
final class Milestone03ProgressiveConstructionOriginalTileSmoke {
    public static void main(String[] args) {
        List<String> audit = ProgressiveConstructionAuthority.definitionAuditLines();
        requireContains(audit, "live placement preserves the original walkable tile", "original tile audit");
        requireContains(audit, "dismantle restores the original tile", "dismantle tile audit");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.seed = 255145L;
        game.startPackagedClientNewGameWith(null, WorldSetupSettings.standard());
        game.baseObjects.clear();

        Point target = firstOpenBuildTile(game);
        char original = game.world.tiles[target.x][target.y];
        require(original != '?', "fixture tile should not start as a construction placeholder");

        BuildRecipe recipe = new BuildRecipe("Original Tile Test Frame", 'o', 1, 0, 0, 0, 0, 2, 10, false,
                Faction.NONE, null, "test fixture for staged construction original tile restoration");
        game.pendingBuildRecipe = recipe;
        game.buildX = target.x;
        game.buildY = target.y;
        game.supplies = 1;
        game.confirmBuildPlacement();

        require(game.baseObjects.size() == 1, "placement should create one staged site");
        BaseObject site = game.baseObjects.get(0);
        require(site.constructionOriginalTile == original, "staged site should preserve the replaced tile");
        require(game.world.tiles[target.x][target.y] == '?', "staged site should reserve the tile with placeholder");

        ProgressiveConstructionAuthority.DismantleResult result = ProgressiveConstructionAuthority.dismantle(game, site);
        require(result.removed(), "dismantle should remove the staged site");
        require(game.world.tiles[target.x][target.y] == original, "dismantle should restore the original tile");
        require(!game.baseObjects.contains(site), "dismantled site should be removed");

        BaseObject loaded = ProgressiveConstructionAuthority.createSite(recipe, target.x, target.y, 2);
        loaded.constructionOriginalTile = original;
        game.baseObjects.add(loaded);
        ProgressiveConstructionAuthority.syncSiteTile(game, loaded);
        require(loaded.constructionOriginalTile == original, "sync should not overwrite a saved original tile");
        require(game.world.tiles[target.x][target.y] == '?', "loaded staged site should reserve placeholder");
        ProgressiveConstructionAuthority.dismantle(game, loaded);
        require(game.world.tiles[target.x][target.y] == original, "loaded staged site should restore saved original tile");

        if (game.timer != null) game.timer.stop();
    }

    private static Point firstOpenBuildTile(GamePanel game) {
        for (int x = 1; x < game.world.w - 1; x++) {
            for (int y = 1; y < game.world.h - 1; y++) {
                if (x == game.playerX && y == game.playerY) continue;
                if (game.world.walkable(x, y) && game.world.npcAt(x, y) == null && game.world.mapObjectAt(x, y) == null
                        && game.baseObjectAt(x, y) == null) {
                    return new Point(x, y);
                }
            }
        }
        throw new AssertionError("No open build tile found for original-tile smoke");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03ProgressiveConstructionOriginalTileSmoke() { }
}
