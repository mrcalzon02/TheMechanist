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
        requireContains(audit, "unfinished staged sites can be dismantled", "dismantle action audit");
        requireContains(audit, "inserted materials are recovered", "dismantle recovery audit");
        requireContains(audit, "reports active staged-site count", "status packet audit");
        requireContains(audit, "ready-for-labor count", "status ready count audit");
        requireContains(audit, "material-blocked count", "status blocked count audit");
        requireContains(audit, "material-ready count", "status available-material audit");
        requireContains(audit, "in-work-reach count", "status reach audit");
        requireContains(audit, "command target priority", "status command priority audit");
        requireContains(audit, "prioritized site progress lines", "status priority audit");
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
        if (availabilityGame.timer != null) availabilityGame.timer.stop();
        requireContains(ProgressiveConstructionAuthority.siteStatusLine(prepaid), "next action: work to add labor", "site status labor next action");
        require(ProgressiveConstructionAuthority.workCommandPriority(null, prepaid)
                        < ProgressiveConstructionAuthority.workCommandPriority(null, site),
                "work command priority should prefer labor-ready sites over material-blocked sites");
        requireContains(ProgressiveConstructionAuthority.contributionResultLine(prepaid, 0, false),
                "Construction work added labor", "contribution progress text");
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
        requireContains(ProgressiveConstructionAuthority.contributionResultLine(partial, inserted, false),
                "staged 4 material unit(s)", "material staging text");
        if (materialGame.timer != null) materialGame.timer.stop();

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

    private Milestone03ProgressiveConstructionDefinitionAuditSmoke() { }
}
