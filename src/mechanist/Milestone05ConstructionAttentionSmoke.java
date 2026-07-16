package mechanist;

import java.util.List;

/** Smoke for live Phase 12.3 construction heat and suspicion mutation. */
final class Milestone05ConstructionAttentionSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        BuildRecipe recipe = BuildRecipe.storage();
        BlueprintExpansionHeatAuthority.HeatProfile profile = BlueprintExpansionHeatAuthority.profileFor(recipe);

        game.gangHeat = 3;
        game.suspicion = 5;
        BlueprintExpansionHeatAuthority.AppliedAttention applied =
                BlueprintExpansionHeatAuthority.applyConstructionStart(game, recipe);
        require(game.gangHeat == 3 + profile.heatImpact(), "construction start should add previewed heat once");
        require(game.suspicion == 5 + profile.suspicionImpact(), "construction start should add previewed suspicion once");
        require(applied.heatBefore() == 3 && applied.heatAfter() == game.gangHeat,
                "heat result should retain exact before and after values");
        require(applied.suspicionBefore() == 5 && applied.suspicionAfter() == game.suspicion,
                "suspicion result should retain exact before and after values");
        requireContains(applied.summary(), "Construction attention applied: gang heat 3->" + game.gangHeat,
                "heat feedback");
        requireContains(applied.summary(), "suspicion 5->" + game.suspicion, "suspicion feedback");
        requireContains(applied.summary(), "drivers " + profile.driverSummary(), "driver feedback");

        List<String> preview = ConstructionReadabilityAuthority.detailLines(game, recipe, game.playerX, game.playerY);
        requireLineContains(preview, "Attention preview: heat", "construction attention preview");
        requireLineContains(ExpansionHeatReadabilityAuthority.summary(game.suspicion, game.gangHeat, game.baseObjects),
                "Construction rule: starting a player construction site adds its previewed exposure",
                "global attention readback");

        GamePanel runtime = new GamePanel();
        if (runtime.timer != null) runtime.timer.stop();
        runtime.seed = 255145L;
        runtime.startPackagedClientNewGameWith(null, WorldSetupSettings.standard());
        runtime.baseObjects.clear();
        runtime.supplies = 100;
        runtime.machineParts = 100;
        runtime.inventory.add("Rivet set");
        runtime.rebuildItemContainersFromLegacyLists();
        runtime.pendingBuildRecipe = recipe;
        runtime.buildPlacementActive = true;
        int[] target = legalTarget(runtime);
        runtime.buildX = target[0];
        runtime.buildY = target[1];
        runtime.gangHeat = 5;
        int runtimeHeatBefore = runtime.gangHeat;
        int runtimeSuspicionBefore = runtime.suspicion;
        int sitesBefore = runtime.baseObjects.size();
        runtime.confirmBuildPlacement();
        require(runtime.baseObjects.size() == sitesBefore + 1,
                "successful confirmation should create one staged construction site");
        require(runtime.gangHeat == runtimeHeatBefore + profile.heatImpact(),
                "successful confirmation should route through live heat mutation");
        require(runtime.suspicion == runtimeSuspicionBefore + profile.suspicionImpact(),
                "successful confirmation should route through live suspicion mutation");
        requireLineContains(runtime.eventLog, "Construction attention applied: gang heat "
                        + runtimeHeatBefore + "->" + runtime.gangHeat,
                "successful confirmation feedback");
        requireLineContains(runtime.eventLog, "Expansion response - Local notice",
                "successful threshold-crossing response");
        require(runtime.factionMarketPressure.getOrDefault(Faction.CIVIC_WARDENS, 0) == 1,
                "live threshold crossing should raise civic market pressure");

        int heatBeforeBlocked = game.gangHeat;
        int suspicionBeforeBlocked = game.suspicion;
        game.pendingBuildRecipe = recipe;
        game.buildPlacementActive = true;
        game.buildX = game.playerX;
        game.buildY = game.playerY;
        game.supplies = 0;
        game.machineParts = 0;
        game.confirmBuildPlacement();
        require(game.gangHeat == heatBeforeBlocked && game.suspicion == suspicionBeforeBlocked,
                "blocked construction must not add attention");

        if (game.timer != null) game.timer.stop();
        if (runtime.timer != null) runtime.timer.stop();
        System.out.println("Milestone 05 construction attention smoke passed.");
    }

    private static int[] legalTarget(GamePanel game) {
        for (int x = 1; x < game.world.w - 1; x++) {
            for (int y = 1; y < game.world.h - 1; y++) {
                String result = game.rawCanPlacePendingBuildAt(x, y);
                if ("ok".equalsIgnoreCase(result)
                        || result != null && result.toLowerCase().startsWith("staged start:")) {
                    return new int[]{x, y};
                }
            }
        }
        throw new AssertionError("Could not find a legal nearby Storage Crate construction target");
    }

    private static void requireLineContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone05ConstructionAttentionSmoke() { }
}
