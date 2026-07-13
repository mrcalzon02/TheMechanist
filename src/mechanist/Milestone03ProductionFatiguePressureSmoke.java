package mechanist;

import java.util.List;

/** Smoke for fatigue-pressure forecasting, blocking, batch risk, and provenance. */
final class Milestone03ProductionFatiguePressureSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.fatigue = 19;
        require(ProductionFatiguePressureAuthority.evaluate(game, 2).defectRiskAdd() == 0,
                "ready fatigue should not add defect risk");
        game.fatigue = 20;
        require(ProductionFatiguePressureAuthority.evaluate(game, 2).defectRiskAdd() == 2,
                "slightly tired fatigue should add two defect points");
        game.fatigue = 45;
        ProductionFatiguePressureAuthority.FatiguePressure tired =
                ProductionFatiguePressureAuthority.evaluate(game, 3);
        require(tired.defectRiskAdd() == 5 && !tired.blocked(), "tired fatigue should add five points without blocking");
        game.fatigue = 75;
        require(ProductionFatiguePressureAuthority.evaluate(game, 1).blocked(),
                "established exhausted band should block manual Craft");

        ProductionRecipe recipe = ProductionRecipe.create("Test Tool", Faction.HIVER, "Common",
                "Common Tool Patterns", "Test Forge");
        BaseObject machine = new BaseObject("Test Forge", 'f', 0, 0, 0, 0);
        machine.integrity = 3;
        ProductionOperatorSkillAuthority.OperatorSkill operator = new ProductionOperatorSkillAuthority.OperatorSkill(
                "Mechanics", "Mechanics", 8, "skilled", 0, 4, "Fine");
        int baseRisk = recipe.estimatedDefectPercent(machine, operator.defectRiskAdjust());
        ProductionBatchAuthority.BatchDisposition batch = ProductionBatchAuthority.assess(
                recipe, machine, operator, tired, 12, 100, 99L);
        require(batch.defectRiskPercent() == baseRisk + 5, "batch risk should include fatigue pressure");

        ItemProvenanceRecord made = ItemProvenanceRecord.produced(
                recipe, machine, null, 12, "Test Operator", null, operator, null, batch, tired);
        requireContains(made.qualityContextLines(), "Production fatigue pressure: tired", "provenance fatigue pressure");
        ItemProvenanceRecord decoded = ItemProvenanceRecord.decode(made.encode());
        require(decoded != null && made.productionPressure.equals(decoded.productionPressure),
                "fatigue pressure should survive save encoding");
        require(made.productionPressure.equals(ItemProvenanceRecord.transferred(
                decoded, made.itemName, null, 13, "moved to storage").productionPressure),
                "fatigue pressure should survive transfer");

        GamePanel craftGame = craftGame();
        craftGame.fatigue = 45;
        int fatigueBeforeCraft = craftGame.fatigue;
        int expectedFatigueCost = ControlledProductionJobAuthority.manualFatigueCost(craftGame,
                craftGame.baseObjects.get(0), craftGame.selectedCraftingRecipe);
        craftGame.craftSelectedRecipe();
        require(craftGame.supplies == 0, "successful Craft should consume supplies");
        require(craftGame.inventory.size() == 1, "successful Craft should add output");
        require(craftGame.fatigue == Math.min(GamePanel.MAX_FOOD_WATER, fatigueBeforeCraft + expectedFatigueCost),
                "successful Craft should add authority fatigue");
        require(craftGame.baseObjects.get(0).integrity == 2, "successful Craft should apply machine wear");
        requireEventContains(craftGame, "Production result:", "completion result event");
        requireEventContains(craftGame, "quality Common", "completion quality event");
        requireEventContains(craftGame, "main limiter", "completion limiter event");
        requireEventContains(craftGame, "fatigue tired", "completion fatigue event");
        requireEventContains(craftGame, "defect risk", "completion defect risk event");
        if (craftGame.timer != null) craftGame.timer.stop();

        GamePanel exhaustedGame = craftGame();
        exhaustedGame.fatigue = 75;
        int turnBefore = exhaustedGame.turn;
        exhaustedGame.craftSelectedRecipe();
        require(exhaustedGame.turn == turnBefore, "exhausted Craft should not spend a turn");
        require(exhaustedGame.supplies == 1, "exhausted Craft should not consume supplies");
        require(exhaustedGame.inventory.isEmpty(), "exhausted Craft should not add output");
        require(exhaustedGame.fatigue == 75, "exhausted Craft should not add fatigue");
        require(exhaustedGame.baseObjects.get(0).integrity == 3, "exhausted Craft should not wear the machine");
        requireContains(lastEvent(exhaustedGame), "player is exhausted; rest before operating machinery",
                "exhausted Craft event");
        if (exhaustedGame.timer != null) exhaustedGame.timer.stop();

        GamePanel workbenchGame = craftGame();
        BaseObject defaultForge = workbenchGame.baseObjects.get(0);
        defaultForge.name = "Default Forge";
        BaseObject operatedForge = new BaseObject("Operated Forge", 'f', workbenchGame.playerX + 1, workbenchGame.playerY, 0, 0);
        operatedForge.qualityName = "Common";
        operatedForge.integrity = 3;
        workbenchGame.baseObjects.add(operatedForge);
        workbenchGame.activeInteractionBaseObject = operatedForge;
        workbenchGame.panelMode = GamePanel.PanelMode.WORKBENCH;
        workbenchGame.selectCraftingRecipeFromPanel(workbenchGame.selectedCraftingRecipe);
        require(workbenchGame.panelMode == GamePanel.PanelMode.WORKBENCH,
                "selecting a workbench recipe should preserve workbench mode");
        List<String> workbenchForecast = ProductionReadabilityAuthority.detailLines(
                workbenchGame, workbenchGame.selectedCraftingRecipe);
        requireContains(workbenchForecast, "Machine: Operated Forge", "workbench forecast operated machine");
        requireNotContains(workbenchForecast, "Machine: Default Forge", "workbench forecast should exclude default machine");
        CraftingRecipe labRecipe = new CraftingRecipe("Test Lab Run", "Test Notes", Faction.HIVER,
                "Common Tool Patterns", 'l', 1, 0, 1, 1, 2, 1, 0,
                "Knowledge", "Test incompatible workbench recipe.");
        require(workbenchGame.craftingRecipeVisibleOnCurrentSurface(workbenchGame.selectedCraftingRecipe),
                "operated forge should list forge recipes");
        require(!workbenchGame.craftingRecipeVisibleOnCurrentSurface(labRecipe),
                "operated forge should hide lab recipes");
        workbenchGame.craftSelectedRecipe();
        require(defaultForge.integrity == 3, "workbench Craft should not wear the default forge");
        require(operatedForge.integrity == 2, "workbench Craft should wear the operated forge");
        require(workbenchGame.machineOperationQueue.historyCount() == 1,
                "workbench Craft should record one completion");
        requireContains(workbenchGame.machineOperationQueue.recentHistory().get(0).targetId,
                "Operated_Forge@" + operatedForge.x + "," + operatedForge.y,
                "workbench completion operated machine target");
        if (workbenchGame.timer != null) workbenchGame.timer.stop();

        GamePanel incompatibleWorkbench = craftGame();
        BaseObject compatibleForge = incompatibleWorkbench.baseObjects.get(0);
        BaseObject operatedLab = new BaseObject("Operated Lab", 'l', incompatibleWorkbench.playerX + 1,
                incompatibleWorkbench.playerY, 0, 0);
        operatedLab.integrity = 3;
        incompatibleWorkbench.baseObjects.add(operatedLab);
        incompatibleWorkbench.activeInteractionBaseObject = operatedLab;
        incompatibleWorkbench.panelMode = GamePanel.PanelMode.WORKBENCH;
        int incompatibleTurnBefore = incompatibleWorkbench.turn;
        incompatibleWorkbench.craftSelectedRecipe();
        require(incompatibleWorkbench.turn == incompatibleTurnBefore,
                "incompatible workbench Craft should not spend a turn");
        require(incompatibleWorkbench.supplies == 1,
                "incompatible workbench Craft should not consume supplies");
        require(incompatibleWorkbench.inventory.isEmpty(),
                "incompatible workbench Craft should not add output");
        require(compatibleForge.integrity == 3 && operatedLab.integrity == 3,
                "incompatible workbench Craft should not wear any machine");
        requireContains(lastEvent(incompatibleWorkbench), "requires EMM Micro Forge",
                "incompatible workbench machine guidance");
        if (incompatibleWorkbench.timer != null) incompatibleWorkbench.timer.stop();

        GamePanel routedWorkbench = craftGame();
        BaseObject routedForge = routedWorkbench.baseObjects.get(0);
        routedWorkbench.activeInteractionBaseObject = routedForge;
        routedWorkbench.panelMode = GamePanel.PanelMode.OBJECT;
        routedWorkbench.openActiveMachineWorkbench();
        require(routedWorkbench.panelMode == GamePanel.PanelMode.WORKBENCH,
                "base-machine Craft route should open workbench mode");
        require(routedWorkbench.requiredMachineFor(routedWorkbench.selectedCraftingRecipe) == routedForge,
                "base-machine Craft route should retain the interaction machine");
        requireContains(lastEvent(routedWorkbench), "Opened Test Forge operation surface",
                "base-machine Craft route event");
        if (routedWorkbench.timer != null) routedWorkbench.timer.stop();

        GamePanel emptyWorkbench = craftGame();
        emptyWorkbench.unlockedKnowledges.clear();
        BaseObject unmatchedMachine = new BaseObject("Unmatched Station", 'z', emptyWorkbench.playerX,
                emptyWorkbench.playerY, 0, 0);
        emptyWorkbench.activeInteractionBaseObject = unmatchedMachine;
        emptyWorkbench.panelMode = GamePanel.PanelMode.WORKBENCH;
        List<CraftingRecipe> emptyRecipes = emptyWorkbench.visibleCraftingRecipesForCurrentSurface();
        require(emptyRecipes.size() == 1 && emptyRecipes.get(0).disabled,
                "empty workbench should expose one disabled guidance row");
        requireContains(emptyRecipes.get(0).description,
                "No known recipes are compatible with Unmatched Station",
                "empty workbench machine guidance");
        if (emptyWorkbench.timer != null) emptyWorkbench.timer.stop();
        if (game.timer != null) game.timer.stop();
    }

    private static GamePanel craftGame() {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.eventLog.clear();
        game.baseObjects.clear();
        game.inventory.clear();
        game.baseStorage.clear();
        game.supplies = 1;
        game.machineParts = 0;
        game.turn = 20;
        game.worldTurn = 20;
        game.unlockedKnowledges.add("Common Tool Patterns");
        BaseObject machine = new BaseObject("Test Forge", 'f', game.playerX, game.playerY, 0, 0);
        machine.qualityName = "Common";
        machine.integrity = 3;
        game.baseObjects.add(machine);
        game.selectedCraftingRecipe = new CraftingRecipe("Test Tool Run", "Test Tool", Faction.HIVER,
                "Common Tool Patterns", 'f', 1, 0, 1, 2, 3, 1, 1,
                "Mechanics", "Test production completion readback.");
        return game;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void requireNotContains(List<String> lines, String forbidden, String label) {
        for (String line : lines) {
            if (line != null && line.contains(forbidden)) {
                throw new AssertionError("Expected " + label + " not to contain '" + forbidden + "': " + lines);
            }
        }
    }

    private static void requireContains(String line, String expected, String label) {
        if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + line);
    }

    private static void requireEventContains(GamePanel game, String expected, String label) {
        if (game != null) {
            for (String line : game.eventLog) {
                if (line != null && line.contains(expected)) return;
            }
        }
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': "
                + (game == null ? "null game" : game.eventLog));
    }

    private static String lastEvent(GamePanel game) {
        if (game == null || game.eventLog.isEmpty()) return "";
        return game.eventLog.get(game.eventLog.size() - 1);
    }

    private Milestone03ProductionFatiguePressureSmoke() { }
}
