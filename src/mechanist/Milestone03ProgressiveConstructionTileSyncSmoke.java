package mechanist;

import java.awt.Point;
import java.util.List;

/** Smoke for live staged-construction world-tile synchronization. */
final class Milestone03ProgressiveConstructionTileSyncSmoke {
    public static void main(String[] args) {
        List<String> audit = ProgressiveConstructionAuthority.definitionAuditLines();
        requireContains(audit, "live placement reserves the world tile", "placement tile sync audit");
        requireContains(audit, "completion restores the final built symbol", "completion tile sync audit");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.seed = 255145L;
        game.startPackagedClientNewGameWith(null, WorldSetupSettings.standard());
        game.baseObjects.clear();
        game.supplies = 1;
        game.machineParts = 0;
        BuildRecipe recipe = new BuildRecipe("Tile Sync Test Frame", 'r', 1, 0, 0, 0, 0, 2, 10, false,
                Faction.NONE, null, "test fixture for staged construction tile sync");
        Point target = firstOpenBuildTile(game);
        game.pendingBuildRecipe = recipe;
        game.buildX = target.x;
        game.buildY = target.y;

        require("ok".equalsIgnoreCase(game.rawCanPlacePendingBuildAt(game.buildX, game.buildY)),
                "fixture tile should accept initial construction placement");
        game.confirmBuildPlacement();
        require(game.baseObjects.size() == 1, "placement should create one staged site");
        BaseObject site = game.baseObjects.get(0);
        require(site.underConstruction, "placed object should remain under construction");
        require(site.symbol == '?', "placed staged site should use placeholder symbol");
        require(site.constructionOriginalTile != 0, "live placement should remember the original tile");
        require(game.world.tiles[target.x][target.y] == '?', "live placement should reserve world tile with placeholder");

        game.pendingBuildRecipe = recipe;
        game.buildX = target.x;
        game.buildY = target.y;
        String blocked = game.rawCanPlacePendingBuildAt(target.x, target.y);
        require(!"ok".equalsIgnoreCase(blocked), "same tile should remain blocked by the reserved construction tile");
        requireContains(blocked, "tile is not walkable", "same tile should report reserved construction tile");

        ProgressiveConstructionAuthority.contribute(game, site, 1, false);
        require(site.underConstruction, "one labor turn should not complete two-turn site");
        require(game.world.tiles[target.x][target.y] == '?', "unfinished site should keep placeholder");
        ProgressiveConstructionAuthority.contribute(game, site, 1, false);
        require(!site.underConstruction, "second labor turn should complete the site");
        require(site.symbol == 'r', "completed site should use final symbol");
        require(game.world.tiles[target.x][target.y] == 'r', "completion should restore final built symbol to world tile");
        requireContains(ProgressiveConstructionAuthority.contributionResultLine(site, 0, true),
                "Construction complete: Tile Sync Test Frame", "completion summary");

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
        throw new AssertionError("No open build tile found for staged construction tile sync smoke");
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

    private Milestone03ProgressiveConstructionTileSyncSmoke() { }
}
