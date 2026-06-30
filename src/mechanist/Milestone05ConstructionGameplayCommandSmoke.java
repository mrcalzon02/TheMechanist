package mechanist;

/** Smoke for player-rank staged construction status and adjacent work commands. */
final class Milestone05ConstructionGameplayCommandSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.baseObjects.clear();
        game.supplies = 1;
        game.machineParts = 0;

        InternalServerSessionAuthority.CommandContext player =
                new InternalServerSessionAuthority.CommandContext("player", "local-user", false, "local-world", "local-server");

        BuildRecipe recipe = new BuildRecipe(
                "Console Test Frame", 's', 1, 0, 0, 0, 0, 3, 10, false,
                Faction.NONE, null, "adjacent staged construction command fixture");
        int siteX = game.playerX + 1;
        int siteY = game.playerY;
        if (game.world != null && !game.world.inBounds(siteX, siteY)) siteX = game.playerX - 1;
        BaseObject site = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY, 3);
        game.baseObjects.add(site);

        require(GameplayConsoleCommandAuthority.isKnown("construction_status"), "construction status command should be registered");
        require(GameplayConsoleCommandAuthority.isKnown("construction_work"), "construction work command should be registered");
        requireContains(GameplayConsoleCommandAuthority.help(new String[]{"construction_work"}),
                "construction_work [turns]", "construction work help");

        String status = GameplayConsoleCommandAuthority.execute(game, player, "construction_status", new String[0]);
        requireContains(status, "active staged sites=1", "construction status count");
        requireContains(status, "blocked by materials=1", "construction status blocked count");
        requireContains(status, "material ready=1", "construction status material-ready count");
        requireContains(status, "in work reach=1", "construction status in-reach count");
        requireContains(status, "access in work reach", "construction status reach guidance");

        String worked = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[]{"1"});
        requireContains(worked, "staged 1 material unit(s)", "material staging result");
        requireContains(worked, "added 1 labor", "labor contribution result");
        require(site.constructionLaborDone == 1, "one adjacent work turn should add one labor");
        require(site.constructionVisualProgress > 65, "material and labor contribution should advance visible progress");
        require(game.supplies == 0, "construction work should consume staged supplies");

        game.baseObjects.clear();
        BaseObject blocked = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY, 3);
        game.baseObjects.add(blocked);
        String blockedResult = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[0]);
        requireContains(blockedResult, "Construction blocked", "blocked construction result");
        requireContains(blockedResult, "next action: stage Construction supplies x1", "blocked next action");
        require(blocked.constructionLaborDone == 0, "blocked work should not invent labor progress");

        game.supplies = 1;
        BaseObject materialReady = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY, 3);
        game.baseObjects.clear();
        game.baseObjects.add(materialReady);
        String materialReadyStatus = GameplayConsoleCommandAuthority.execute(game, player, "construction_status", new String[0]);
        requireContains(materialReadyStatus, "next action: stage available Construction supplies x1", "material-ready status guidance");

        game.supplies = 1;
        game.machineParts = 0;
        game.baseObjects.clear();
        BuildRecipe partBlockedRecipe = new BuildRecipe(
                "Console Part Frame", 's', 0, 1, 0, 0, 0, 3, 10, false,
                Faction.NONE, null, "adjacent staged construction unavailable part fixture");
        BaseObject ordinaryBlocked = ProgressiveConstructionAuthority.createSite(partBlockedRecipe, siteX, siteY, 3);
        BaseObject actionableBlocked = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY + 1, 3);
        game.baseObjects.add(ordinaryBlocked);
        game.baseObjects.add(actionableBlocked);
        String prioritizedWork = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[0]);
        requireContains(prioritizedWork, "staged 1 material unit(s)", "work command should pick material-ready site first");
        require(actionableBlocked.constructionLaborDone == 1, "work command should add labor to the material-ready adjacent site");
        require(ordinaryBlocked.constructionLaborDone == 0, "work command should leave the less actionable adjacent site alone");

        game.baseObjects.clear();
        BaseObject farSite = ProgressiveConstructionAuthority.createSite(recipe, siteX + 20, siteY + 20, 3);
        game.baseObjects.add(farSite);
        String farStatus = GameplayConsoleCommandAuthority.execute(game, player, "construction_status", new String[0]);
        requireContains(farStatus, "access stand adjacent to work", "distant status reach guidance");

        game.baseObjects.clear();
        String outOfReach = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[0]);
        requireContains(outOfReach, "No staged construction site is within reach", "out-of-reach guidance");

        String badTurns = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[]{"many"});
        requireContains(badTurns, "turns must be an integer", "invalid turn guidance");

        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone05ConstructionGameplayCommandSmoke() { }
}
