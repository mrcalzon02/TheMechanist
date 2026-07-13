package mechanist;

import java.awt.Color;
import java.util.List;

/** Smoke for the staged construction definition audit surface. */
final class Milestone03ProgressiveConstructionDefinitionAuditSmoke {
    public static void main(String[] args) {
        List<String> audit = ProgressiveConstructionAuthority.definitionAuditLines();
        requireContains(audit, "owner=ProgressiveConstructionAuthority", "progressive construction owner");
        requireContains(audit, "siteOwner=BaseObject", "site owner");
        requireContains(audit, "recipeOwner=BuildRecipe", "recipe owner");
        requireContains(audit, "persistenceOwner=BaseObject save/load fields", "persistence owner");
        requireContains(audit, "ordinaryUiRawIds=false", "raw-ID boundary");
        requireContains(audit, "underConstruction=true", "site state");
        requireContains(audit, "finalSymbol preserved", "final symbol");
        requireContains(audit, "requiredMaterials stored", "required materials");
        requireContains(audit, "insertedMaterials stored", "inserted materials");
        requireContains(audit, "laborRequired and laborDone stored", "labor fields");
        requireContains(audit, "visualProgress stored", "visual progress field");
        requireContains(audit, "originalTile preserved", "original tile field");
        requireContains(audit, "material progress contributes most", "material progress weighting");
        requireContains(audit, "placement can create a prepaid or partial construction site", "prepaid placement bridge");
        requireContains(audit, "labor completes the remainder", "labor progress weighting");
        requireContains(audit, "live placement preserves the original walkable tile", "original tile sync");
        requireContains(audit, "reserves the world tile", "placement tile sync");
        requireContains(audit, "completion restores the final built symbol", "completion tile sync");
        requireContains(audit, "dismantle restores the original tile", "dismantle tile restoration");
        requireContains(audit, "pale blue ghost construction", "ghost visual");
        requireContains(audit, "object inspection reports staged-site status", "inspection audit");
        requireContains(audit, "can stage available missing materials", "material staging action audit");
        requireContains(audit, "contribute one turn of labor when materials are complete", "labor action audit");
        requireContains(audit, "require the selected staged site to still exist and be adjacent", "interaction work live-site audit");
        requireContains(audit, "spend a turn only when Work changes staged materials or labor progress", "interaction work time-cost audit");
        requireContains(audit, "construction_work spends productive command turns", "work command time-cost audit");
        requireContains(audit, "location and next-action readback", "labor action readback audit");
        requireContains(audit, "unfinished staged sites can be dismantled", "dismantle action audit");
        requireContains(audit, "interaction panel requires the selected staged site to still exist and be adjacent",
                "dismantle interaction live-site audit");
        requireContains(audit, "gameplay command requires an adjacent staged site", "dismantle command reach audit");
        requireContains(audit, "construction_dismantle spends one turn when it removes a site", "dismantle command time-cost audit");
        requireContains(audit, "command prefers the least-complete adjacent staged site", "dismantle command priority audit");
        requireContains(audit, "command no-target guidance names the nearest staged site", "dismantle command no-target audit");
        requireContains(audit, "dismantle summaries identify the site location", "dismantle location audit");
        requireContains(audit, "inserted materials are recovered", "dismantle recovery audit");
        requireContains(audit, "construction progress and construction status commands share the same player packet", "status command alias audit");
        requireContains(audit, "active staged-site count", "status packet audit");
        requireContains(audit, "ready-for-labor count", "status ready count audit");
        requireContains(audit, "material-blocked count", "status blocked count audit");
        requireContains(audit, "material-ready count", "status available-material audit");
        requireContains(audit, "in-work-reach count", "status reach audit");
        requireContains(audit, "command target priority", "status command priority audit");
        requireContains(audit, "adjacent work target readback with next action", "status work target audit");
        requireContains(audit, "nearest out-of-reach work guidance", "status nearest work guidance audit");
        requireContains(audit, "empty-state work target readback", "status empty work target audit");
        requireContains(audit, "directional distance guidance", "status directional guidance audit");
        requireContains(audit, "prioritized site progress lines", "status priority audit");
        requireContains(audit, "overflow next-site readback", "status overflow audit");
        requireContains(audit, "adjacent dismantle target readback", "status dismantle target audit");
        requireContains(audit, "no-target dismantle guidance", "status dismantle no-target audit");
        requireContains(audit, "empty-state dismantle target readback", "status empty dismantle audit");
        requireContains(audit, "next action without exposing raw identifiers", "status next-action audit");
        requireContains(audit, "held-tool multiplier", "tool timing");
        requireContains(audit, "saved with base objects", "save persistence");
        requireContains(audit, "restored before completed objects", "load persistence");
        requireContains(audit, "write/read round-trip smoke", "round-trip persistence guard");
        requireContains(audit, "Under construction: Licensed Shop Counter", "sample site");
        requireContains(audit, "Construction supplies 0/3", "sample supplies");
        requireContains(audit, "Machine part 0/1", "sample machine part");
        requireContains(audit, "labor=0/7", "sample labor");
        requireContains(audit, "prepaid=Under construction: Licensed Shop Counter progress=65%", "prepaid sample");
        requireContains(audit, "overlayAlphaStart=120", "overlay alpha");
        requireContains(audit, "overlayMovesTowardBuilt=true", "overlay color movement");
        requireContains(audit, "does not dispatch workers, mutate room ownership", "mutation boundary");
        requireContains(audit, "Milestone03ProgressiveConstructionDefinitionAuditSmoke", "guard reference");
        requireContains(audit, "Milestone03ProgressiveConstructionPersistenceSmoke", "persistence guard reference");
        requireContains(audit, "Milestone03ProgressiveConstructionDismantleSmoke", "dismantle guard reference");
        requireContains(audit, "Milestone03ProgressiveConstructionInteractionWorkSmoke", "interaction work guard reference");
        requireContains(audit, "Work and Dismantle interaction reach and turn costs", "interaction reach guard detail");
        requireContains(audit, "Milestone03ProgressiveConstructionTileSyncSmoke", "tile sync guard reference");
        requireContains(audit, "Milestone03ProgressiveConstructionOriginalTileSmoke", "original tile guard reference");

        BaseObject site = ProgressiveConstructionAuthority.createSite(BuildRecipe.shopCounter(), 12, 18, 7);
        require(site.underConstruction, "site should be under construction");
        require(site.symbol == '?', "site should use placeholder symbol");
        require(site.finalSymbol == 'B', "site should preserve final build symbol");
        require("Licensed Shop Counter".equals(site.assignedRecipe), "site should preserve recipe name");
        require(site.constructionRequiredItems.contains("Construction supplies=3"), "site should require supplies");
        require(site.constructionRequiredItems.contains("Machine part=1"), "site should require machine part");
        require(site.constructionLaborRequired == 7, "site should preserve labor requirement");
        require(site.constructionLaborDone == 0, "site should start with no labor");
        require(site.constructionVisualProgress == 0, "site should start at zero visual progress");
        require(site.constructionOriginalTile == 0, "site should not invent an original tile before placement");
        require(site.qualityName.equals("Common"), "site should preserve quality");
        require(ProgressiveConstructionAuthority.availableMaterialUnits(null, BuildRecipe.shopCounter()) == 0,
                "null game should report no available materials");
        GamePanel emptyGame = new GamePanel();
        if (emptyGame.timer != null) emptyGame.timer.stop();
        emptyGame.baseObjects.clear();
        String emptyStatus = ProgressiveConstructionAuthority.statusPacket(emptyGame);
        requireContains(emptyStatus, "active staged sites=0", "empty status count");
        requireContains(emptyStatus, "Construction next action: no staged construction sites are waiting.", "empty next action");
        requireContains(emptyStatus, "Construction work target: none.", "empty work target");
        requireContains(emptyStatus, "Construction dismantle target: none.", "empty dismantle target");
        if (emptyGame.timer != null) emptyGame.timer.stop();

        BaseObject prepaid = ProgressiveConstructionAuthority.createPrepaidSite(BuildRecipe.shopCounter(), 12, 18);
        require(prepaid.underConstruction, "prepaid site should remain staged");
        require(prepaid.symbol == '?', "prepaid site should not be completed immediately");
        require(prepaid.finalSymbol == 'B', "prepaid site should preserve final symbol");
        require(prepaid.constructionRequiredItems.equals(prepaid.constructionInsertedItems), "prepaid site should stage all required materials");
        require(prepaid.constructionLaborRequired == BuildRecipe.shopCounter().baseTurns, "prepaid site should use recipe turns as labor");
        require(prepaid.constructionLaborDone == 0, "prepaid site should still need labor");
        require(prepaid.constructionVisualProgress == 65, "prepaid site should show material progress only");

        String line = ProgressiveConstructionAuthority.progressLine(site);
        requireContains(line, "progress=0%", "progress line");
        requireContains(line, "missing=", "missing-material line");
        List<String> inspection = ProgressiveConstructionAuthority.inspectionLines(prepaid);
        requireContains(inspection, "staged site, not a completed facility", "inspection status");
        requireContains(inspection, "progress=65%", "inspection progress");
        requireContains(inspection, "labor=0/7", "inspection labor");
        requireContains(inspection, "finished work becomes B", "inspection target");
        requireContains(ProgressiveConstructionAuthority.statusPacketLines(null), "active staged sites=0", "null status count");
        requireContains(ProgressiveConstructionAuthority.statusPacketLines(null), "no staged construction sites are waiting", "null status next action");
        requireContains(ProgressiveConstructionAuthority.siteStatusLine(site), "Under construction: Licensed Shop Counter at 12,18", "site status location");
        requireContains(ProgressiveConstructionAuthority.siteStatusLine(site),
                "next action: stage Construction supplies x3, Fastener button card x1, Machine part x1, Warehouse inventory tag bundle x1",
                "site status blocked next action");
        GamePanel availabilityGame = new GamePanel();
        if (availabilityGame.timer != null) availabilityGame.timer.stop();
        availabilityGame.supplies = 1;
        availabilityGame.machineParts = 0;
        requireContains(ProgressiveConstructionAuthority.siteStatusLine(availabilityGame, site),
                "next action: stage available Construction supplies x1; still missing Construction supplies x2",
                "site status available material next action");
        BaseObject reachableSite = ProgressiveConstructionAuthority.createSite(BuildRecipe.shopCounter(), availabilityGame.playerX + 1, availabilityGame.playerY, 7);
        requireContains(ProgressiveConstructionAuthority.siteStatusLine(availabilityGame, reachableSite),
                "access in work reach", "site status reachable guidance");
        requireContains(ProgressiveConstructionAuthority.siteStatusLine(availabilityGame, site),
                "access stand adjacent to work", "site status distant guidance");
        requireContains(ProgressiveConstructionAuthority.siteStatusLine(availabilityGame, site),
                "tiles away", "site status distant distance");
        requireContains(ProgressiveConstructionAuthority.siteStatusLine(availabilityGame, site),
                "move east/south", "site status distant direction");
        if (availabilityGame.timer != null) availabilityGame.timer.stop();
        requireContains(ProgressiveConstructionAuthority.siteStatusLine(prepaid), "next action: work to add labor", "site status labor next action");
        require(ProgressiveConstructionAuthority.workCommandPriority(null, prepaid)
                        < ProgressiveConstructionAuthority.workCommandPriority(null, site),
                "work command priority should prefer labor-ready sites over material-blocked sites");
        String laborResult = ProgressiveConstructionAuthority.contributionResultLine(availabilityGame, prepaid, 0, 1, false);
        requireContains(laborResult, "Construction work added 1 labor", "contribution progress text");
        requireContains(laborResult, "at 12,18", "contribution location text");
        requireContains(laborResult, "next action: work to add labor", "contribution next action text");
        requireContains(ProgressiveConstructionAuthority.auditSummary(null), "activeSites=0", "empty audit");
        require(ProgressiveConstructionAuthority.deconstructionTurnsForTile('#', null) == 5, "wall deconstruction turns");
        require(ProgressiveConstructionAuthority.deconstructionTurnsForTile('.', null) == 2, "ordinary tile deconstruction turns");

        Color start = ProgressiveConstructionAuthority.constructionOverlayColor(site, new Color(90, 210, 120));
        site.constructionVisualProgress = 100;
        Color finish = ProgressiveConstructionAuthority.constructionOverlayColor(site, new Color(90, 210, 120));
        require(start.getAlpha() == 120 && finish.getAlpha() == 120, "overlay alpha should stay compact");
        require(finish.getGreen() < start.getGreen(), "overlay should move away from ghost blue");
        require(finish.getRed() < start.getRed(), "overlay should move toward built color");

        GamePanel materialGame = new GamePanel();
        if (materialGame.timer != null) materialGame.timer.stop();
        materialGame.baseObjects.clear();
        materialGame.supplies = 3;
        materialGame.machineParts = 1;
        BuildRecipe simple = new BuildRecipe("Simple Test Frame", 's', 3, 1, 0, 0, 0, 7, 10, false,
                Faction.NONE, null, "simple staged construction fixture");
        BaseObject partial = ProgressiveConstructionAuthority.createSite(simple, 14, 18, 7);
        materialGame.baseObjects.add(partial);
        int inserted = ProgressiveConstructionAuthority.contribute(materialGame, partial, 1, true);
        require(inserted == 4, "available materials should stage into partial site");
        require(partial.underConstruction, "material staging should not complete labor");
        require(partial.constructionLaborDone == 1, "labor should start after materials complete in the same action");
        require(partial.constructionVisualProgress == 69, "partial site should combine material and one labor turn progress");
        require(materialGame.supplies == 0, "staging should consume supplies");
        require(materialGame.machineParts == 0, "staging should consume machine parts");
        String mixedResult = ProgressiveConstructionAuthority.contributionResultLine(materialGame, partial, inserted, 1, false);
        requireContains(mixedResult, "staged 4 material unit(s) and added 1 labor", "material staging text");
        requireContains(mixedResult, "at 14,18", "material staging location");
        requireContains(mixedResult, "next action: work to add labor", "material staging next action");
        if (materialGame.timer != null) materialGame.timer.stop();

        GamePanel materialOnlyGame = new GamePanel();
        if (materialOnlyGame.timer != null) materialOnlyGame.timer.stop();
        materialOnlyGame.baseObjects.clear();
        materialOnlyGame.supplies = 1;
        materialOnlyGame.machineParts = 0;
        BuildRecipe mixedNeeds = new BuildRecipe("Material Only Test Frame", 'm', 1, 1, 0, 0, 0, 4, 10, false,
                Faction.NONE, null, "material-only staged construction fixture");
        BaseObject materialOnly = ProgressiveConstructionAuthority.createSite(mixedNeeds, 15, 18, 4);
        materialOnlyGame.baseObjects.add(materialOnly);
        int materialOnlyLaborBefore = Math.max(0, materialOnly.constructionLaborDone);
        int materialOnlyInserted = ProgressiveConstructionAuthority.contribute(materialOnlyGame, materialOnly, 1, true);
        int materialOnlyLaborAdded = Math.max(0, materialOnly.constructionLaborDone - materialOnlyLaborBefore);
        require(materialOnlyInserted == 1, "material-only staging should insert the available supply");
        require(materialOnlyLaborAdded == 0, "material-only staging should not report invented labor");
        String materialOnlyResult = ProgressiveConstructionAuthority.contributionResultLine(materialOnlyGame,
                materialOnly, materialOnlyInserted, materialOnlyLaborAdded, false);
        requireContains(materialOnlyResult, "Construction work staged 1 material unit(s).", "material-only result");
        requireContains(materialOnlyResult, "at 15,18", "material-only location");
        requireContains(materialOnlyResult, "next action: stage Machine part x1", "material-only next action");
        requireNotContains(materialOnlyResult, "added 1 labor", "material-only labor wording");
        if (materialOnlyGame.timer != null) materialOnlyGame.timer.stop();

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.baseObjects.clear();
        game.supplies = 1;
        game.machineParts = 0;
        BaseObject reachableMaterial = ProgressiveConstructionAuthority.createSite(BuildRecipe.shopCounter(), game.playerX + 1, game.playerY, 7);
        BaseObject nearlyDone = ProgressiveConstructionAuthority.createPrepaidSite(BuildRecipe.shopCounter(), 9, 16);
        nearlyDone.constructionLaborDone = 6;
        nearlyDone.constructionVisualProgress = 95;
        game.baseObjects.add(prepaid);
        game.baseObjects.add(site);
        game.baseObjects.add(reachableMaterial);
        game.baseObjects.add(nearlyDone);
        List<String> status = ProgressiveConstructionAuthority.statusPacketLines(game);
        requireContains(status, "active staged sites=4", "status active count");
        requireContains(status, "ready for labor=2", "status ready count");
        requireContains(status, "blocked by materials=2", "status blocked count");
        requireContains(status, "material ready=2", "status material-ready count");
        requireContains(status, "in work reach=1", "status work-reach count");
        requireContains(status, "nearly complete=1", "status nearly complete count");
        require(status.size() > 1 && status.get(1).contains("94% complete"),
                "status packet should prioritize nearly complete staged sites");
        require(status.size() > 3 && status.get(3).contains("access in work reach")
                        && status.get(3).contains("stage available Construction supplies x1"),
                "status packet should show reachable available staged materials before distant blocked materials");
        requireContains(status, "Under construction: Licensed Shop Counter at 12,18: 0% complete", "status blocked site");
        requireContains(status, "Under construction: Licensed Shop Counter at 12,18: 65% complete", "status prepaid site");
        requireContains(status, "Construction work target: Under construction: Licensed Shop Counter at "
                        + reachableMaterial.x + "," + reachableMaterial.y,
                "status work target");
        requireContains(status, "Construction work target: Under construction: Licensed Shop Counter at "
                        + reachableMaterial.x + "," + reachableMaterial.y
                        + ", 0% complete; next action: stage available Construction supplies x1",
                "status work target next action");
        requireContains(status, "command uses construction progress priority among adjacent staged sites",
                "status work target rule");
        requireContains(status, "Construction dismantle target: Under construction: Licensed Shop Counter at "
                        + reachableMaterial.x + "," + reachableMaterial.y,
                "status dismantle target");
        requireContains(status, "least-complete adjacent staged site first", "status dismantle target rule");
        GamePanel overflowGame = new GamePanel();
        if (overflowGame.timer != null) overflowGame.timer.stop();
        overflowGame.baseObjects.clear();
        for (int i = 0; i < 6; i++) {
            overflowGame.baseObjects.add(ProgressiveConstructionAuthority.createSite(BuildRecipe.shopCounter(),
                    30 + i, 40 + i, 7));
        }
        List<String> overflowStatus = ProgressiveConstructionAuthority.statusPacketLines(overflowGame);
        requireContains(overflowStatus, "active staged sites=6", "overflow active count");
        requireContains(overflowStatus, "1 additional staged site(s) not shown", "overflow additional count");
        requireContains(overflowStatus, "next unlisted site: Under construction: Licensed Shop Counter at 35,45",
                "overflow next site location");
        requireContains(overflowStatus, "next action: stage Construction supplies x3", "overflow next action");
        if (overflowGame.timer != null) overflowGame.timer.stop();
        GamePanel farWorkGame = new GamePanel();
        if (farWorkGame.timer != null) farWorkGame.timer.stop();
        farWorkGame.baseObjects.clear();
        BaseObject farWorkSite = ProgressiveConstructionAuthority.createSite(BuildRecipe.shopCounter(),
                farWorkGame.playerX + 20, farWorkGame.playerY + 20, 7);
        farWorkGame.baseObjects.add(farWorkSite);
        List<String> farStatus = ProgressiveConstructionAuthority.statusPacketLines(farWorkGame);
        requireContains(farStatus, "Construction work target: none in reach", "status work no-target lead");
        requireContains(farStatus, "stand adjacent to work a staged site", "status work no-target action");
        requireContains(farStatus, "Nearest staged site: Under construction: Licensed Shop Counter", "status work nearest site");
        requireContains(farStatus, "Construction dismantle target: none in reach", "status dismantle no-target lead");
        requireContains(farStatus, "stand adjacent to remove a staged site", "status dismantle no-target action");
        requireContains(farStatus, "Nearest staged site: Under construction: Licensed Shop Counter", "status dismantle nearest site");
        requireContains(farStatus, "tiles away", "status dismantle nearest distance");
        requireContains(farStatus, "move east/south", "status dismantle nearest direction");
        String workReachFailure = ProgressiveConstructionAuthority.workReachFailureLine(farWorkGame);
        requireContains(workReachFailure, "No staged construction site is within reach", "work reach failure lead");
        requireContains(workReachFailure, "Nearest staged site: Under construction: Licensed Shop Counter", "work reach nearest site");
        requireContains(workReachFailure, "tiles away", "work reach distance");
        requireContains(workReachFailure, "move east/south", "work reach direction");
        requireContains(workReachFailure, "next action:", "work reach next action");
        if (farWorkGame.timer != null) farWorkGame.timer.stop();
        String commandStatus = new AdminCommandDispatcher(null, null, null)
                .executeCommand(game, new InternalServerSessionAuthority.CommandContext("admin", "local-user", true, "local-world", "local-server"),
                        new ConsoleCommandRequest("admin", "/construction_progress"));
        requireContains(commandStatus, "Construction progress: active staged sites=4", "admin construction progress status");
        requireContains(commandStatus, "next action: work to add labor", "admin construction next action");
        game.baseObjects.remove(site);
        game.baseObjects.remove(reachableMaterial);
        game.baseObjects.remove(nearlyDone);
        for (int i = 0; i < 7; i++) {
            ProgressiveConstructionAuthority.contribute(game, prepaid, 1, false);
        }
        require(!prepaid.underConstruction, "labor should complete the prepaid site");
        require(prepaid.symbol == 'B', "completed site should use final symbol");
        require(prepaid.constructionVisualProgress == 100, "completed site should show full progress");
        require("Licensed Shop Counter".equals(prepaid.name), "completed site should drop construction prefix");
        requireContains(ProgressiveConstructionAuthority.contributionResultLine(prepaid, 0, true),
                "Construction complete: Licensed Shop Counter", "completion text");
        requireContains(ProgressiveConstructionAuthority.contributionResultLine(prepaid, 0, true),
                "at 12,18", "completion location text");
        if (game.timer != null) game.timer.stop();

        for (String auditLine : audit) {
            if (PlayerFacingText.containsLikelyLeak(auditLine)) {
                throw new AssertionError("Progressive construction audit leaked implementation text: " + auditLine);
            }
        }
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

    private static void requireNotContains(String text, String forbidden, String label) {
        if (text == null || !text.contains(forbidden)) return;
        throw new AssertionError("Expected " + label + " not to contain '" + forbidden + "': " + text);
    }

    private Milestone03ProgressiveConstructionDefinitionAuditSmoke() { }
}
