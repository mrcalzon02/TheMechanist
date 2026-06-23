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
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03ManualProductionOperationRecordSmoke() { }
}
