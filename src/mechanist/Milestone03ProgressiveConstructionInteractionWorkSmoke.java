package mechanist;

/** Smoke for staged-construction Work interaction turn costs. */
final class Milestone03ProgressiveConstructionInteractionWorkSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.baseObjects.clear();
        game.supplies = 0;
        game.machineParts = 0;
        game.baseStorage.clear();

        BuildRecipe recipe = new BuildRecipe(
                "Interaction Work Frame", 'i', 1, 0, 0, 0, 0, 3, 10, false,
                Faction.NONE, null, "interaction staged construction work fixture");
        int siteX = game.playerX + 1;
        int siteY = game.playerY;
        if (game.world != null && !game.world.inBounds(siteX, siteY)) siteX = game.playerX - 1;

        BaseObject blocked = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY, 3);
        game.baseObjects.add(blocked);
        game.activeInteractionBaseObject = blocked;
        int turnBeforeBlocked = game.turn;
        game.workActiveConstructionSite();
        require(game.turn == turnBeforeBlocked, "blocked Work interaction should not spend a turn");
        require(blocked.constructionLaborDone == 0, "blocked Work interaction should not add labor");
        requireContains(lastEvent(game), "Construction work made no progress", "blocked Work event");
        requireContains(lastEvent(game), "next action: stage Construction supplies x1", "blocked Work next action");

        game.supplies = 1;
        game.baseObjects.clear();
        BaseObject productive = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY, 3);
        game.baseObjects.add(productive);
        game.activeInteractionBaseObject = productive;
        int turnBeforeProductive = game.turn;
        game.workActiveConstructionSite();
        require(game.turn == turnBeforeProductive + 1, "productive Work interaction should spend one turn");
        require(productive.constructionLaborDone == 1, "productive Work interaction should add labor");
        require(game.supplies == 0, "productive Work interaction should stage supplies");
        requireEventContains(game, "Construction work staged 1 material unit(s) and added 1 labor", "productive Work result");

        BaseObject finishing = ProgressiveConstructionAuthority.createPrepaidSite(recipe, siteX, siteY);
        finishing.constructionLaborRequired = 2;
        finishing.constructionLaborDone = 1;
        finishing.constructionVisualProgress = 82;
        game.baseObjects.clear();
        game.baseObjects.add(finishing);
        game.activeInteractionBaseObject = finishing;
        int turnBeforeFinish = game.turn;
        game.workActiveConstructionSite();
        require(game.turn == turnBeforeFinish + 1, "finishing Work interaction should spend one turn");
        require(!finishing.underConstruction, "finishing Work interaction should complete the site");
        requireEventContains(game, "Construction complete: Interaction Work Frame", "finishing Work result");

        game.supplies = 1;
        game.machineParts = 0;
        game.baseStorage.clear();
        game.baseObjects.clear();
        BaseObject distantWork = ProgressiveConstructionAuthority.createSite(recipe, distantX(game), game.playerY, 3);
        game.baseObjects.add(distantWork);
        game.activeInteractionBaseObject = distantWork;
        int turnBeforeDistantWork = game.turn;
        game.workActiveConstructionSite();
        require(game.turn == turnBeforeDistantWork, "distant Work interaction should not spend a turn");
        require(distantWork.constructionLaborDone == 0, "distant Work interaction should not add labor");
        require(game.supplies == 1, "distant Work interaction should not stage supplies");
        requireContains(lastEvent(game), "no longer within reach", "distant Work reach event");
        requireContains(lastEvent(game), "Stand adjacent to work this staged site", "distant Work reach guidance");

        game.supplies = 0;
        game.machineParts = 0;
        game.baseStorage.clear();
        game.baseObjects.clear();
        BaseObject distantDismantle = ProgressiveConstructionAuthority.createSite(recipe, distantX(game), game.playerY, 3);
        distantDismantle.constructionInsertedItems = "Construction supplies=1;Sensor lens=1";
        game.baseObjects.add(distantDismantle);
        game.activeInteractionBaseObject = distantDismantle;
        int turnBeforeDistantDismantle = game.turn;
        game.dismantleActiveConstructionSite();
        require(game.turn == turnBeforeDistantDismantle, "distant Dismantle interaction should not spend a turn");
        require(game.baseObjects.contains(distantDismantle), "distant Dismantle interaction should not remove the site");
        require(game.supplies == 0, "distant Dismantle interaction should not recover supplies");
        require(!game.baseStorage.contains("Sensor lens"), "distant Dismantle interaction should not recover named components");
        requireContains(lastEvent(game), "no longer within reach", "distant Dismantle reach event");
        requireContains(lastEvent(game), "Stand adjacent to remove this staged site", "distant Dismantle reach guidance");

        game.supplies = 1;
        game.machineParts = 0;
        game.baseStorage.clear();
        game.baseObjects.clear();
        BaseObject staleWork = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY, 3);
        game.activeInteractionBaseObject = staleWork;
        int turnBeforeStaleWork = game.turn;
        game.workActiveConstructionSite();
        require(game.turn == turnBeforeStaleWork, "stale Work interaction should not spend a turn");
        require(staleWork.constructionLaborDone == 0, "stale Work interaction should not add labor");
        require(game.supplies == 1, "stale Work interaction should not stage supplies");
        requireContains(lastEvent(game), "Construction site is no longer available", "stale Work live-site event");
        requireContains(lastEvent(game), "Re-open a staged site before working", "stale Work live-site guidance");

        game.supplies = 0;
        game.machineParts = 0;
        game.baseStorage.clear();
        game.baseObjects.clear();
        BaseObject staleDismantle = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY, 3);
        staleDismantle.constructionInsertedItems = "Construction supplies=1;Sensor lens=1";
        game.activeInteractionBaseObject = staleDismantle;
        int turnBeforeStaleDismantle = game.turn;
        game.dismantleActiveConstructionSite();
        require(game.turn == turnBeforeStaleDismantle, "stale Dismantle interaction should not spend a turn");
        require(game.supplies == 0, "stale Dismantle interaction should not recover supplies");
        require(!game.baseStorage.contains("Sensor lens"), "stale Dismantle interaction should not recover named components");
        requireContains(lastEvent(game), "Construction site is no longer available", "stale Dismantle live-site event");
        requireContains(lastEvent(game), "Re-open a staged site before removing it", "stale Dismantle live-site guidance");

        if (game.timer != null) game.timer.stop();
    }

    private static int distantX(GamePanel game) {
        int right = game.playerX + 3;
        if (game.world == null || game.world.inBounds(right, game.playerY)) return right;
        int left = game.playerX - 3;
        if (game.world.inBounds(left, game.playerY)) return left;
        return Math.max(0, Math.min(game.world.w - 1, game.playerX + (game.playerX <= 1 ? 3 : -3)));
    }

    private static String lastEvent(GamePanel game) {
        if (game == null || game.eventLog.isEmpty()) return "";
        return game.eventLog.get(game.eventLog.size() - 1);
    }

    private static void requireEventContains(GamePanel game, String expected, String label) {
        if (game != null) {
            for (String line : game.eventLog) {
                if (line != null && line.contains(expected)) return;
            }
        }
        throw new AssertionError("Expected " + label + " event to contain '" + expected + "': "
                + (game == null ? "null game" : game.eventLog));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone03ProgressiveConstructionInteractionWorkSmoke() { }
}
