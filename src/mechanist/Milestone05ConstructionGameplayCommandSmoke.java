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
        int alternateSiteX = game.playerX;
        int alternateSiteY = game.playerY + 1;
        if (game.world != null && !game.world.inBounds(alternateSiteX, alternateSiteY)) alternateSiteY = game.playerY - 1;
        BaseObject site = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY, 3);
        game.baseObjects.add(site);

        require(GameplayConsoleCommandAuthority.isKnown("construction_status"), "construction status command should be registered");
        require(GameplayConsoleCommandAuthority.isKnown("construction_progress"), "construction progress command should be registered");
        require(GameplayConsoleCommandAuthority.isKnown("construction_work"), "construction work command should be registered");
        require(GameplayConsoleCommandAuthority.isKnown("construction_dismantle"), "construction dismantle command should be registered");
        String statusHelp = GameplayConsoleCommandAuthority.help(new String[]{"construction_status"});
        requireContains(statusHelp, "work target", "construction status help work target");
        requireContains(statusHelp, "dismantle target guidance", "construction status help guidance");
        String progressHelp = GameplayConsoleCommandAuthority.help(new String[]{"construction_progress"});
        requireContains(progressHelp, "construction_progress", "construction progress help");
        requireContains(progressHelp, "work target", "construction progress help work target");
        requireContains(progressHelp, "dismantle target packet", "construction progress help guidance");
        String workHelp = GameplayConsoleCommandAuthority.help(new String[]{"construction_work"});
        requireContains(workHelp, "construction_work [turns 1-20]", "construction work help");
        requireContains(workHelp, "adjacent staged site named by construction_progress", "construction work help priority");
        requireContains(workHelp, "spends productive work turns", "construction work help time cost");
        requireContains(workHelp, "accepts 1-20 turns", "construction work help turn range");
        requireContains(workHelp, "points to the nearest staged site when none are adjacent", "construction work help no-target");
        String dismantleHelp = GameplayConsoleCommandAuthority.help(new String[]{"construction_dismantle"});
        requireContains(dismantleHelp, "construction_dismantle", "construction dismantle help");
        requireContains(dismantleHelp, "least-complete adjacent unfinished staged site", "construction dismantle help target");
        requireContains(dismantleHelp, "spends one turn when a site is removed", "construction dismantle help time cost");
        requireContains(dismantleHelp, "points to the nearest staged site when none are adjacent", "construction dismantle help no-target");

        String status = GameplayConsoleCommandAuthority.execute(game, player, "construction_status", new String[0]);
        requireContains(status, "active staged sites=1", "construction status count");
        requireContains(status, "blocked by materials=1", "construction status blocked count");
        requireContains(status, "material ready=1", "construction status material-ready count");
        requireContains(status, "in work reach=1", "construction status in-reach count");
        requireContains(status, "access in work reach", "construction status reach guidance");
        requireContains(status, "Construction work target: Under construction: Console Test Frame at "
                        + siteX + "," + siteY,
                "construction status work target");
        requireContains(status, "Construction work target: Under construction: Console Test Frame at "
                        + siteX + "," + siteY + ", 0% complete; next action: stage available Construction supplies x1",
                "construction status work target next action");
        String progress = GameplayConsoleCommandAuthority.execute(game, player, "construction_progress", new String[0]);
        requireContains(progress, "active staged sites=1", "construction progress count");
        requireContains(progress, "next action: stage available Construction supplies x1", "construction progress next action");
        requireContains(progress, "access in work reach", "construction progress reach guidance");
        requireContains(progress, "Construction work target: Under construction: Console Test Frame at "
                        + siteX + "," + siteY,
                "construction progress work target");
        requireContains(progress, "Construction work target: Under construction: Console Test Frame at "
                        + siteX + "," + siteY + ", 0% complete; next action: stage available Construction supplies x1",
                "construction progress work target next action");
        requireContains(GameplayConsoleCommandAuthority.execute(game, player, "construction_status", new String[]{"extra"}),
                "Usage: construction_status", "construction status args usage");
        requireContains(GameplayConsoleCommandAuthority.execute(game, player, "construction_progress", new String[]{"extra"}),
                "Usage: construction_progress", "construction progress args usage");

        int turnBeforeWork = game.turn;
        String worked = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[]{"1"});
        requireContains(worked, "staged 1 material unit(s)", "material staging result");
        requireContains(worked, "added 1 labor", "labor contribution result");
        requireContains(worked, "at " + siteX + "," + siteY, "work result location");
        requireContains(worked, "next action: work to add labor", "work result next action");
        requireContains(worked, "Construction time spent: 1 turn.", "work command time spent");
        require(site.constructionLaborDone == 1, "one adjacent work turn should add one labor");
        require(site.constructionVisualProgress > 65, "material and labor contribution should advance visible progress");
        require(game.supplies == 0, "construction work should consume staged supplies");
        require(game.turn == turnBeforeWork + 1, "construction work should advance one productive turn");

        game.baseObjects.clear();
        BaseObject blocked = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY, 3);
        game.baseObjects.add(blocked);
        int turnBeforeBlocked = game.turn;
        String blockedResult = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[0]);
        requireContains(blockedResult, "Construction blocked", "blocked construction result");
        requireContains(blockedResult, "next action: stage Construction supplies x1", "blocked next action");
        require(blocked.constructionLaborDone == 0, "blocked work should not invent labor progress");
        require(game.turn == turnBeforeBlocked, "blocked construction work should not advance time");

        game.baseObjects.clear();
        game.supplies = 0;
        game.machineParts = 0;
        game.baseStorage.clear();
        BaseObject dismantleSite = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY, 3);
        dismantleSite.constructionInsertedItems = "Construction supplies=1;Sensor lens=1";
        dismantleSite.constructionLaborDone = 2;
        game.baseObjects.add(dismantleSite);
        int turnBeforeDismantle = game.turn;
        String dismantled = GameplayConsoleCommandAuthority.execute(game, player, "construction_dismantle", new String[0]);
        requireContains(dismantled, "Dismantled Under construction: Console Test Frame", "dismantle command result");
        requireContains(dismantled, "at " + siteX + "," + siteY, "dismantle command location");
        requireContains(dismantled, "Recovered 1 construction supplies, 0 machine part(s), and 1 named component(s)", "dismantle command recovery");
        requireContains(dismantled, "Labor progress was not recoverable", "dismantle command labor loss");
        requireContains(dismantled, "Construction time spent: 1 turn.", "dismantle command time spent");
        require(!game.baseObjects.contains(dismantleSite), "dismantle command should remove the adjacent staged site");
        require(game.supplies == 1, "dismantle command should recover supplies");
        require(game.baseStorage.contains("Sensor lens"), "dismantle command should recover named components");
        require(game.turn == turnBeforeDismantle + 1, "dismantle command should advance one turn when removing a site");

        game.baseObjects.clear();
        game.supplies = 0;
        game.machineParts = 0;
        game.baseStorage.clear();
        BaseObject nearlyFinishedDismantleSite = ProgressiveConstructionAuthority.createPrepaidSite(recipe, siteX, siteY);
        nearlyFinishedDismantleSite.constructionLaborRequired = 5;
        nearlyFinishedDismantleSite.constructionLaborDone = 4;
        nearlyFinishedDismantleSite.constructionVisualProgress = 93;
        BaseObject roughDismantleSite = ProgressiveConstructionAuthority.createSite(recipe, alternateSiteX, alternateSiteY, 3);
        roughDismantleSite.constructionInsertedItems = "Construction supplies=1";
        roughDismantleSite.constructionVisualProgress = 10;
        game.baseObjects.add(nearlyFinishedDismantleSite);
        game.baseObjects.add(roughDismantleSite);
        String dismantleStatus = GameplayConsoleCommandAuthority.execute(game, player, "construction_status", new String[0]);
        requireContains(dismantleStatus, "Construction dismantle target: Under construction: Console Test Frame at "
                        + alternateSiteX + "," + alternateSiteY,
                "construction status dismantle target");
        requireContains(dismantleStatus, "least-complete adjacent staged site first",
                "construction status dismantle target rule");
        String dismantleProgress = GameplayConsoleCommandAuthority.execute(game, player, "construction_progress", new String[0]);
        requireContains(dismantleProgress, "Construction dismantle target: Under construction: Console Test Frame at "
                        + alternateSiteX + "," + alternateSiteY,
                "construction progress dismantle target");
        int turnBeforePrioritizedDismantle = game.turn;
        String prioritizedDismantle = GameplayConsoleCommandAuthority.execute(game, player, "construction_dismantle", new String[0]);
        requireContains(prioritizedDismantle, "at " + alternateSiteX + "," + alternateSiteY,
                "dismantle command least-complete target");
        requireContains(prioritizedDismantle, "Construction time spent: 1 turn.", "prioritized dismantle time spent");
        require(!game.baseObjects.contains(roughDismantleSite), "dismantle command should remove the least-complete adjacent site");
        require(game.baseObjects.contains(nearlyFinishedDismantleSite), "dismantle command should preserve the nearly finished adjacent site");
        require(game.turn == turnBeforePrioritizedDismantle + 1, "prioritized dismantle should spend one turn");

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
        int turnBeforePrioritizedWork = game.turn;
        String prioritizedWork = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[0]);
        requireContains(prioritizedWork, "staged 1 material unit(s)", "work command should pick material-ready site first");
        requireContains(prioritizedWork, "Construction time spent: 1 turn.", "prioritized work time spent");
        require(actionableBlocked.constructionLaborDone == 1, "work command should add labor to the material-ready adjacent site");
        require(ordinaryBlocked.constructionLaborDone == 0, "work command should leave the less actionable adjacent site alone");
        require(game.turn == turnBeforePrioritizedWork + 1, "prioritized work should advance one productive turn");

        game.baseObjects.clear();
        BaseObject farSite = ProgressiveConstructionAuthority.createSite(recipe, siteX + 20, siteY + 20, 3);
        game.baseObjects.add(farSite);
        String farStatus = GameplayConsoleCommandAuthority.execute(game, player, "construction_status", new String[0]);
        requireContains(farStatus, "access stand adjacent to work", "distant status reach guidance");
        requireContains(farStatus, "tiles away", "distant status distance guidance");
        requireContains(farStatus, "move east/south", "distant status direction guidance");
        requireContains(farStatus, "Construction work target: none in reach", "distant status work no-target");
        requireContains(farStatus, "stand adjacent to work a staged site", "distant status work action");
        requireContains(farStatus, "Construction dismantle target: none in reach", "distant status dismantle no-target");
        requireContains(farStatus, "Nearest staged site: Under construction: Console Test Frame", "distant status dismantle nearest");
        String farProgress = GameplayConsoleCommandAuthority.execute(game, player, "construction_progress", new String[0]);
        requireContains(farProgress, "Construction work target: none in reach", "distant progress work no-target");
        requireContains(farProgress, "stand adjacent to work a staged site", "distant progress work action");
        requireContains(farProgress, "Construction dismantle target: none in reach", "distant progress dismantle no-target");
        requireContains(farProgress, "stand adjacent to remove a staged site", "distant progress dismantle action");

        int turnBeforeFarWork = game.turn;
        String farWork = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[0]);
        requireContains(farWork, "No staged construction site is within reach", "far work reach failure");
        requireContains(farWork, "Nearest staged site: Under construction: Console Test Frame", "far work nearest guidance");
        requireContains(farWork, "tiles away", "far work distance guidance");
        requireContains(farWork, "move east/south", "far work direction guidance");
        requireContains(farWork, "next action:", "far work next action guidance");
        require(game.turn == turnBeforeFarWork, "far construction work should not advance time");

        int turnBeforeFarDismantle = game.turn;
        String farDismantle = GameplayConsoleCommandAuthority.execute(game, player, "construction_dismantle", new String[0]);
        requireContains(farDismantle, "No unfinished staged construction site is within reach", "far dismantle reach failure");
        requireContains(farDismantle, "Stand adjacent to remove a staged site", "far dismantle adjacent action");
        requireContains(farDismantle, "Nearest staged site: Under construction: Console Test Frame", "far dismantle nearest guidance");
        requireContains(farDismantle, "tiles away", "far dismantle distance guidance");
        requireContains(farDismantle, "move east/south", "far dismantle direction guidance");
        require(game.turn == turnBeforeFarDismantle, "far dismantle should not advance time");
        String badDismantleArgs = GameplayConsoleCommandAuthority.execute(game, player, "construction_dismantle", new String[]{"now"});
        requireContains(badDismantleArgs, "Usage: construction_dismantle", "dismantle args usage");

        int turnBeforeClampedFarWork = game.turn;
        String clampedFarWork = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[]{"0"});
        requireContains(clampedFarWork, "Construction work turns adjusted to 1 (allowed 1-20).", "low turn clamp guidance");
        requireContains(clampedFarWork, "Nearest staged site:", "low turn clamp should preserve nearest guidance");
        require(game.turn == turnBeforeClampedFarWork, "clamped far work should not advance time");

        game.baseObjects.clear();
        game.supplies = 1;
        BaseObject longTurnSite = ProgressiveConstructionAuthority.createSite(recipe, siteX, siteY, 30);
        game.baseObjects.add(longTurnSite);
        int turnBeforeLongWork = game.turn;
        String longTurnWork = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[]{"99"});
        requireContains(longTurnWork, "Construction work turns adjusted to 20 (allowed 1-20).", "high turn clamp guidance");
        requireContains(longTurnWork, "staged 1 material unit(s)", "high turn material staging");
        requireContains(longTurnWork, "Construction time spent: 20 turns.", "high turn time spent");
        require(longTurnSite.constructionLaborDone == 20, "high turn clamp should contribute the allowed maximum labor");
        require(game.turn == turnBeforeLongWork + 20, "high turn work should spend the productive clamped turns");

        game.baseObjects.clear();
        BaseObject finishingSite = ProgressiveConstructionAuthority.createPrepaidSite(recipe, siteX, siteY);
        finishingSite.constructionLaborRequired = 3;
        finishingSite.constructionLaborDone = 2;
        finishingSite.constructionVisualProgress = 88;
        game.baseObjects.add(finishingSite);
        int turnBeforeFinishedWork = game.turn;
        String finishedWork = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[]{"1"});
        requireContains(finishedWork, "Construction complete: Console Test Frame", "completion result");
        requireContains(finishedWork, "at " + siteX + "," + siteY, "completion result location");
        requireContains(finishedWork, "Construction time spent: 1 turn.", "completion time spent");
        require(!finishingSite.underConstruction, "completion command should finish prepaid adjacent site");
        require(game.turn == turnBeforeFinishedWork + 1, "completion work should advance one productive turn");

        game.baseObjects.clear();
        String outOfReach = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[0]);
        requireContains(outOfReach, "No staged construction site is within reach", "out-of-reach guidance");
        requireContains(outOfReach, "No staged construction sites are waiting", "out-of-reach empty-site guidance");
        String emptyStatus = GameplayConsoleCommandAuthority.execute(game, player, "construction_status", new String[0]);
        requireContains(emptyStatus, "Construction next action: no staged construction sites are waiting.", "empty status next action");
        requireContains(emptyStatus, "Construction work target: none.", "empty status work target");
        requireContains(emptyStatus, "Construction dismantle target: none.", "empty status dismantle target");
        String emptyProgress = GameplayConsoleCommandAuthority.execute(game, player, "construction_progress", new String[0]);
        requireContains(emptyProgress, "Construction next action: no staged construction sites are waiting.", "empty progress next action");
        requireContains(emptyProgress, "Construction work target: none.", "empty progress work target");
        requireContains(emptyProgress, "Construction dismantle target: none.", "empty progress dismantle target");

        String badTurns = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[]{"many"});
        requireContains(badTurns, "turns must be an integer", "invalid turn guidance");
        String tooManyTurnArgs = GameplayConsoleCommandAuthority.execute(game, player, "construction_work", new String[]{"1", "2"});
        requireContains(tooManyTurnArgs, "Usage: construction_work [turns 1-20]", "too many turn args usage");

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
