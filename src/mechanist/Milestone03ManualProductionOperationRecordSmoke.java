package mechanist;

/** Smoke for recording successful manual Craft completions in shared operation history. */
final class Milestone03ManualProductionOperationRecordSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.turn = 20;
        BaseObject machine = new BaseObject("Test Forge", 'f', 4, 6, 0, 0);
        CraftingRecipe recipe = new CraftingRecipe("Test Tool Run", "Test Tool", Faction.HIVER,
                "Common Tool Patterns", 'f', 1, 0, 0, 2, 4, 3, 1,
                "Mechanics", "Test production operation record.");

        MachineOperationQueue.OperationRecord record = ProductionQueueRecordBridge.recordManualRecipeCompletion(
                game, machine, recipe, 3, 2);
        require(record != null, "manual recipe completion should create an operation record");
        require(record.state == MachineOperationQueue.State.COMPLETED, "operation record should be completed");
        require(record.completedTurn == 20, "completion turn should use the live final turn");
        require(record.startedTurn == 17, "start turn should reflect the recorded duration");
        require(record.targetId.contains("Test_Forge@4,6"), "target should identify the producing machine");
        require(record.status.contains("Test Tool Run") && record.status.contains("outputs=2"),
                "status should identify recipe and output count");
        require(game.machineOperationQueue.historyCount() == 1, "completion should enter shared history once");

        String resultReadback = "Production result: 1x Common Test Tool; quality Common; main limiter known doctrine, recipe pattern, machine ceiling; fatigue tired; batch passed inspection at 8% defect risk.";
        MachineOperationQueue.OperationRecord enriched = ProductionQueueRecordBridge.recordManualRecipeCompletion(
                game, machine, recipe, 4, 1, resultReadback);
        require(enriched != null, "manual completion with readback should create a record");
        requireContains(enriched.status, "Production result: 1x Common Test Tool", "recorded production result");
        requireContains(enriched.status, "quality Common", "recorded production quality");
        requireContains(enriched.status, "main limiter known doctrine", "recorded production limiter");
        requireContains(enriched.status, "fatigue tired", "recorded production fatigue");
        requireContains(enriched.status, "batch passed inspection at 8% defect risk", "recorded production batch risk");
        require(game.machineOperationQueue.historyCount() == 2, "completion readback should enter shared history once");

        MachineOperationQueue restored = new MachineOperationQueue();
        restored.restoreRecentHistory(java.util.List.of(enriched.saveLine()));
        String restoredLine = restored.recentHistoryLines(1).get(0);
        requireContains(restoredLine, "Production result: 1x Common Test Tool", "restored production result");
        requireContains(restoredLine, "quality Common", "restored production quality");
        requireContains(restoredLine, "batch passed inspection at 8% defect risk", "restored batch risk");

        InternalServerSessionAuthority.CommandContext player =
                new InternalServerSessionAuthority.CommandContext("player", "local-user", false, "local-world", "local-server");
        require(GameplayConsoleCommandAuthority.isKnown("production_history"), "production history command should be registered");
        String historyHelp = GameplayConsoleCommandAuthority.help(new String[]{"production_history"});
        requireContains(historyHelp, "production_history [count 1-5]", "production history help usage");
        requireContains(historyHelp, "saved result readbacks", "production history help readback");

        GamePanel emptyGame = new GamePanel();
        if (emptyGame.timer != null) emptyGame.timer.stop();
        String emptyHistory = GameplayConsoleCommandAuthority.execute(emptyGame, player, "production_history", new String[0]);
        requireContains(emptyHistory, "Production history: no completed production records yet.", "empty production history");
        String badCount = GameplayConsoleCommandAuthority.execute(game, player, "production_history", new String[]{"many"});
        requireContains(badCount, "Production history count must be an integer.", "bad production history count");
        String tooManyArgs = GameplayConsoleCommandAuthority.execute(game, player, "production_history", new String[]{"1", "2"});
        requireContains(tooManyArgs, "Usage: production_history [count 1-5]", "production history usage");

        String historyPacket = GameplayConsoleCommandAuthority.execute(game, player, "production_history", new String[0]);
        requireContains(historyPacket, "Production history: 2 completed record(s), showing last 2.", "production history packet count");
        requireContains(historyPacket, "Manual Craft completed: Test Tool Run outputs=1", "production history player wording");
        requireContains(historyPacket, "Production result: 1x Common Test Tool", "production history result readback");
        requireContains(historyPacket, "quality Common", "production history quality");
        requireContains(historyPacket, "main limiter known doctrine", "production history limiter");
        requireContains(historyPacket, "fatigue tired", "production history fatigue");
        requireContains(historyPacket, "batch passed inspection at 8% defect risk", "production history batch risk");
        requireContains(historyPacket, "Test Forge@4,6 Test Tool Run", "production history target");
        requireContains(historyPacket, "player manual operation", "production history actor");
        requireNotContains(historyPacket, "legacy production", "production history legacy wording");
        requireNotContains(historyPacket, "outcome authority", "production history outcome wording");

        String latestOnly = GameplayConsoleCommandAuthority.execute(game, player, "production_history", new String[]{"1"});
        requireContains(latestOnly, "Production history: 2 completed record(s), showing last 1.", "production history limit");
        requireContains(latestOnly, "Production result: 1x Common Test Tool", "production history limited result");
        requireNotContains(latestOnly, "outputs=2", "production history limit should hide older record");
        int eventStart = game.eventLog.size();
        game.reportProductionHistoryFromCraftingPanel();
        String craftingHistory = String.join(" | ", game.eventLog.subList(eventStart, game.eventLog.size()));
        requireContains(craftingHistory, "Production history: 2 completed record(s), showing last 2.",
                "crafting panel history header");
        requireContains(craftingHistory, "Test Forge@4,6 Test Tool Run", "crafting panel history target");
        requireContains(craftingHistory, "Production result: 1x Common Test Tool", "crafting panel history readback");

        BaseObject otherMachine = new BaseObject("Other Forge", 'f', 8, 8, 0, 0);
        CraftingRecipe otherRecipe = new CraftingRecipe("Other Tool Run", "Other Tool", Faction.HIVER,
                "Common Tool Patterns", 'f', 1, 0, 0, 2, 4, 3, 1,
                "Mechanics", "Other production operation record.");
        ProductionQueueRecordBridge.recordManualRecipeCompletion(game, otherMachine, otherRecipe, 2, 3,
                "Production result: 3x Common Other Tool; quality Common; main limiter known doctrine; fatigue fresh; batch passed inspection at 2% defect risk.");
        game.activeInteractionBaseObject = machine;
        eventStart = game.eventLog.size();
        game.reportActiveMachineProductionHistory();
        String interactionHistory = String.join(" | ", game.eventLog.subList(eventStart, game.eventLog.size()));
        requireContains(interactionHistory, "Production history for Test Forge at 4,6: 2 completed record(s), showing last 2.",
                "machine interaction history header");
        requireContains(interactionHistory, "Test Forge@4,6 Test Tool Run", "machine interaction history target");
        requireContains(interactionHistory, "Production result: 1x Common Test Tool", "machine interaction history readback");
        requireNotContains(interactionHistory, "Other Tool Run", "machine interaction history should filter other machines");
        game.panelMode = GamePanel.PanelMode.WORKBENCH;
        eventStart = game.eventLog.size();
        game.reportProductionHistoryFromCraftingPanel();
        String workbenchHistory = String.join(" | ", game.eventLog.subList(eventStart, game.eventLog.size()));
        requireContains(workbenchHistory, "Production history for Test Forge at 4,6: 2 completed record(s), showing last 2.",
                "workbench history header");
        requireContains(workbenchHistory, "Test Forge@4,6 Test Tool Run", "workbench history target");
        requireContains(workbenchHistory, "Production result: 1x Common Test Tool", "workbench history readback");
        requireNotContains(workbenchHistory, "Other Tool Run", "workbench history should filter other machines");
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireNotContains(String text, String forbidden, String label) {
        if (text == null || !text.contains(forbidden)) return;
        throw new AssertionError("Expected " + label + " not to contain '" + forbidden + "': " + text);
    }

    private Milestone03ManualProductionOperationRecordSmoke() { }
}
