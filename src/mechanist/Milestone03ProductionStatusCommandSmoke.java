package mechanist;

/** Smoke for the player-rank live production status command. */
final class Milestone03ProductionStatusCommandSmoke {
    public static void main(String[] args) {
        InternalServerSessionAuthority.CommandContext player =
                new InternalServerSessionAuthority.CommandContext("player", "local-user", false, "local-world", "local-server");

        GamePanel empty = new GamePanel();
        if (empty.timer != null) empty.timer.stop();
        empty.baseObjects.clear();
        require(GameplayConsoleCommandAuthority.isKnown("production_status"), "production status command should be registered");
        String statusHelp = GameplayConsoleCommandAuthority.help(new String[]{"production_status"});
        requireContains(statusHelp, "production_status", "production status help usage");
        requireContains(statusHelp, "selected-machine readiness", "production status help selected machine");
        requireContains(GameplayConsoleCommandAuthority.execute(empty, player, "production_status", new String[]{"extra"}),
                "Usage: production_status", "production status usage");
        String emptyStatus = GameplayConsoleCommandAuthority.execute(empty, player, "production_status", new String[0]);
        requireContains(emptyStatus, "Production status: pending 0, active 0, completed history 0, assigned machines 0, selected none.",
                "empty production status summary");
        requireContains(emptyStatus, "Live production queue: no shared queued or active operations.", "empty live queue");
        requireContains(emptyStatus, "Base machines: no recruit-operable machines available.", "empty machine list");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.baseObjects.clear();
        game.baseClaimed = true;
        game.claimedRoomId = 3;
        game.baseX = 5;
        game.baseY = 5;
        game.turn = 44;

        FactionRecipeVariant variant = firstRunnableVariant(game);
        require(variant != null, "expected at least one generated staffed variant to be runnable");
        BaseObject machine = new BaseObject("Status Test Station", 'w', 5, 5, 0, 0);
        machine.qualityName = "Archeotech";
        machine.integrity = 5;
        machine.assignedWorker = "Hest Var";
        machine.assignedRecipe = ControlledProductionJobAuthority.assignmentKey(variant);
        machine.productionQueueTarget = 2;
        machine.productionQueueRemaining = 2;
        game.baseObjects.add(machine);
        game.factionRecruits.add(new RecruitWorker("Hest Var", "forge worker", Faction.HIVER, 3, 4));
        game.unlockedKnowledges.add(variant.requiredKnowledge);
        for (java.util.Map.Entry<String,Integer> input : variant.itemInputs.entrySet()) {
            for (int i = 0; i < input.getValue(); i++) game.baseStorage.add(input.getKey());
        }

        String readyStatus = GameplayConsoleCommandAuthority.execute(game, player, "production_status", new String[0]);
        requireContains(readyStatus, "Production status: pending 0, active 0, completed history 0, assigned machines 1, selected Status Test Station at 5,5.",
                "ready production status summary");
        requireContains(readyStatus, "Selected machine: Status Test Station at 5,5", "selected machine line");
        requireContains(readyStatus, "worker Hest Var", "selected worker");
        requireContains(readyStatus, "queue 2/2", "selected queue");
        requireContains(readyStatus, "crew-ready", "selected readiness");
        requireContains(readyStatus, "Background staffed production: ready for", "staffed forecast readiness");
        requireContains(readyStatus, "current run progress 0/", "staffed background progress");
        requireContains(readyStatus, "Queue policies: materials Wait / keep worker", "staffed queue policy readback");
        requireNotContains(readyStatus, "GENVAR::", "production status raw generated assignment key");

        StaffedProductionExecutionAuthority.StaffedRunResult result =
                StaffedProductionExecutionAuthority.executeOne(game, machine);
        require(result.success(), result.message());
        String completedStatus = GameplayConsoleCommandAuthority.execute(game, player, "production_status", new String[0]);
        requireContains(completedStatus, "completed history 1", "completed history count");
        requireContains(completedStatus, "Latest completed production:", "latest completion line");
        requireContains(completedStatus, "Staffed generated production completed", "latest completion player wording");
        requireContains(completedStatus, result.outputName(), "latest completion output");
        requireContains(completedStatus, "Use production_history to review recent completed production records.", "history command guidance");
        requireNotContains(completedStatus, "legacy production", "production status legacy wording");
        requireNotContains(completedStatus, "outcome authority", "production status outcome wording");
        requireNotContains(completedStatus, "migration", "production status migration wording");
        requireNotContains(completedStatus, "Record bridge", "production status bridge wording");

        MachineOperationQueue.OperationRecord live = game.machineOperationQueue.enqueue(
                "micro_forge_basic_part", "Hest Var", "player_base", "Status_Test_Station@5,5:queued_test", game.turn);
        require(live != null, "live operation should enqueue");
        String liveStatus = GameplayConsoleCommandAuthority.execute(game, player, "production_status", new String[0]);
        requireContains(liveStatus, "pending 1, active 0, completed history 1", "live queue counts");
        requireContains(liveStatus, "Live production queue:", "live queue header");
        requireContains(liveStatus, "state queued", "live queued state");
        requireContains(liveStatus, "Status Test Station", "live target");
        int eventStart = game.eventLog.size();
        game.reportProductionStatusFromCraftingPanel();
        java.util.List<String> panelStatus = game.eventLog.subList(eventStart, game.eventLog.size());
        requireContains(panelStatus, "Production status: pending 1, active 0, completed history 1, assigned machines 1, selected Status Test Station at 5,5.",
                "crafting panel status summary");
        requireContains(panelStatus, "Live production queue:", "crafting panel live queue");
        requireContains(panelStatus, "Status Test Station", "crafting panel selected machine");
        BaseObject idleFirstMachine = new BaseObject("Idle First Station", 'w', 1, 1, 0, 0);
        idleFirstMachine.qualityName = "Common";
        game.baseObjects.add(0, idleFirstMachine);
        MachineOperationQueue.OperationRecord otherLive = game.machineOperationQueue.enqueue(
                "micro_forge_basic_part", "Other Worker", "player_base", "Idle_First_Station@1,1:other_run", game.turn);
        require(otherLive != null, "other machine live operation should enqueue");
        MachineOperationQueue.OperationRecord otherCompleted = game.machineOperationQueue.recordExternalCompletion(
                "micro_forge_basic_part", "Other Worker", "player_base", "Idle_First_Station@1,1:other_run",
                game.turn, 2, "other station completion");
        require(otherCompleted != null, "other machine completion should record");
        String globalMultiMachineStatus = GameplayConsoleCommandAuthority.execute(game, player, "production_status", new String[0]);
        requireContains(globalMultiMachineStatus, "pending 2, active 0, completed history 2", "global multi-machine counts");
        requireContains(globalMultiMachineStatus, "Idle First Station@1,1", "global other machine live operation");
        requireContains(globalMultiMachineStatus, "Latest completed production: turn 44 Idle First Station@1,1 other run",
                "global latest completion remains base-wide");
        game.activeInteractionBaseObject = machine;
        eventStart = game.eventLog.size();
        game.reportActiveMachineProductionStatus();
        java.util.List<String> interactionStatus = game.eventLog.subList(eventStart, game.eventLog.size());
        requireContains(interactionStatus, "Production status: machine pending 1, active 0, completed history 1, selected Status Test Station at 5,5.",
                "machine interaction status summary");
        requireContains(interactionStatus, "Selected machine live queue:", "machine interaction scoped queue header");
        requireContains(interactionStatus, "Selected machine: Status Test Station at 5,5", "machine interaction selected line");
        requireContains(interactionStatus, "worker Hest Var", "machine interaction worker");
        requireContains(interactionStatus, "Latest completed production for selected machine:", "machine interaction scoped completion");
        requireNotContains(interactionStatus, "Idle First Station", "machine interaction should exclude other machine records");
        requireNotContains(interactionStatus, "Base machine snapshot", "machine interaction should omit global machine snapshot");
        game.panelMode = GamePanel.PanelMode.WORKBENCH;
        eventStart = game.eventLog.size();
        game.reportProductionStatusFromCraftingPanel();
        java.util.List<String> workbenchStatus = game.eventLog.subList(eventStart, game.eventLog.size());
        requireContains(workbenchStatus, "Production status: machine pending 1, active 0, completed history 1, selected Status Test Station at 5,5.",
                "workbench status summary");
        requireContains(workbenchStatus, "Selected machine live queue:", "workbench scoped queue header");
        requireContains(workbenchStatus, "Selected machine: Status Test Station at 5,5", "workbench selected machine line");
        requireContains(workbenchStatus, "worker Hest Var", "workbench selected worker");
        requireContains(workbenchStatus, "Latest completed production for selected machine:", "workbench scoped completion");
        requireNotContains(workbenchStatus, "Idle First Station", "workbench should exclude other machine records");
        requireNotContains(workbenchStatus, "Base machine snapshot", "workbench should omit global machine snapshot");

        if (empty.timer != null) empty.timer.stop();
        if (game.timer != null) game.timer.stop();
    }

    private static FactionRecipeVariant firstRunnableVariant(GamePanel game) {
        BaseObject auditMachine = new BaseObject("Audit Workbench", 'w', 5, 5, 0, 0);
        auditMachine.qualityName = "Archeotech";
        auditMachine.assignedWorker = "Hest Var";
        game.baseClaimed = true;
        game.claimedRoomId = 3;
        game.baseX = 5;
        game.baseY = 5;
        for (FactionRecipeVariant variant : FactionRecipeVariantApi.generatedFactionVariants()) {
            if (variant == null || variant.itemInputs.isEmpty() || !variant.equipmentRequirements.isEmpty()) continue;
            game.unlockedKnowledges.add(variant.requiredKnowledge);
            if (ControlledProductionJobAuthority.assignmentProblem(game, auditMachine, variant) == null
                    && ControlledProductionJobAuthority.machineAcceptsVariant(auditMachine, variant)) return variant;
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireContains(java.util.List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void requireNotContains(String text, String forbidden, String label) {
        if (text == null || !text.contains(forbidden)) return;
        throw new AssertionError("Expected " + label + " not to contain '" + forbidden + "': " + text);
    }

    private static void requireNotContains(java.util.List<String> lines, String forbidden, String label) {
        for (String line : lines) {
            if (line != null && line.contains(forbidden)) {
                throw new AssertionError("Expected " + label + " not to contain '" + forbidden + "': " + lines);
            }
        }
    }

    private Milestone03ProductionStatusCommandSmoke() { }
}
