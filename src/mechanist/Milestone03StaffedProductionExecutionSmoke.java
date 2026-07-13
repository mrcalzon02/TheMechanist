package mechanist;

/** Smoke for configured staffed generated-production queue execution. */
final class Milestone03StaffedProductionExecutionSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.baseClaimed = true;
        game.claimedRoomId = 3;
        game.baseX = 5;
        game.baseY = 5;
        game.turn = 44;
        game.lastStaffedProductionBackgroundTurn = game.turn;

        FactionRecipeVariant variant = firstRunnableVariant(game);
        require(variant != null, "expected at least one generated staffed variant to be runnable");
        BaseObject unrelatedMachine = new BaseObject("Unrelated Station", 'w', 1, 1, 0, 0);
        unrelatedMachine.qualityName = "Common";
        unrelatedMachine.productionQueueTarget = 3;
        unrelatedMachine.productionQueueRemaining = 3;
        unrelatedMachine.assignedWorker = "Hest Var";
        game.baseObjects.add(unrelatedMachine);
        BaseObject machine = new BaseObject("Staffed Test Station", 'w', 5, 5, 0, 0);
        machine.qualityName = "Archeotech";
        machine.integrity = 5;
        game.baseObjects.add(machine);
        game.activeInteractionBaseObject = machine;
        game.panelMode = GamePanel.PanelMode.WORKBENCH;
        game.factionRecruits.add(new RecruitWorker("Hest Var", "forge worker", Faction.HIVER, 3, 4));
        game.unlockedKnowledges.add(variant.requiredKnowledge);
        game.openStaffedWorkbenchJobs();
        require(game.workbenchStaffedJobsActive, "workbench should enter staffed-job setup mode");
        String variantKey = ControlledProductionJobAuthority.assignmentKey(variant);
        require(game.visibleWorkbenchStaffedJobs().stream()
                        .anyMatch(candidate -> variantKey.equals(ControlledProductionJobAuthority.assignmentKey(candidate))),
                "compatible known generated job should appear in staffed setup");
        game.selectWorkbenchStaffedJob(variant);
        requireContains(game.assignSelectedWorkbenchStaffedJob(), "Staffed job assigned",
                "workbench job assignment");
        require(variantKey.equals(machine.assignedRecipe),
                "workbench assignment should persist the generated job key");
        FactionRecipeVariant alternate = null;
        for (FactionRecipeVariant candidate : game.visibleWorkbenchStaffedJobs()) {
            String candidateKey = ControlledProductionJobAuthority.assignmentKey(candidate);
            if (!variantKey.equals(candidateKey)
                    && ControlledProductionJobAuthority.assignmentProblem(game, machine, candidate) == null) {
                alternate = candidate;
                break;
            }
        }
        require(alternate != null, "expected a second assignable staffed job for queue-reset coverage");
        machine.productionQueueTarget = 3;
        machine.productionQueueRemaining = 3;
        game.selectWorkbenchStaffedJob(alternate);
        game.assignSelectedWorkbenchStaffedJob();
        require(machine.productionQueueTarget == 1 && machine.productionQueueRemaining == 0,
                "changing staffed jobs should clear the prior queue");
        game.selectWorkbenchStaffedJob(variant);
        game.assignSelectedWorkbenchStaffedJob();
        requireContains(game.cycleActiveWorkbenchWorker(), "Hest Var assigned",
                "workbench worker assignment");
        require("Hest Var".equals(machine.assignedWorker),
                "workbench worker cycle should assign recruited worker");
        require(unrelatedMachine.assignedWorker.isBlank(),
                "workbench worker assignment should clear the recruit from another station");
        requireContains(game.adjustActiveWorkbenchQueue(1), "1/1", "workbench first queued run");
        requireContains(game.adjustActiveWorkbenchQueue(1), "2/2", "workbench second queued run");
        requireContains(game.adjustActiveWorkbenchQueue(-1), "1/1", "workbench queued run removal");
        for (int i = 0; i < 25; i++) game.adjustActiveWorkbenchQueue(1);
        require(machine.productionQueueTarget == 20 && machine.productionQueueRemaining == 20,
                "workbench queue should cap at twenty runs");
        for (int i = 0; i < 20; i++) game.adjustActiveWorkbenchQueue(-1);
        require(machine.productionQueueTarget == 1 && machine.productionQueueRemaining == 0,
                "workbench queue removal should reach an empty 0/1 state");
        game.adjustActiveWorkbenchQueue(1);
        require(machine.productionQueueTarget == 1 && machine.productionQueueRemaining == 1,
                "workbench queue controls should leave one queued run");
        String setupReadback = String.join(" | ", game.staffedWorkbenchDetailLines(machine, variant, 1, 1));
        requireContains(setupReadback, variant.outputName, "workbench readable generated assignment label");
        requireNotContains(setupReadback, "GENVAR::", "workbench raw generated assignment key");
        int persistedProgress = Math.min(2, Math.max(0,
                StaffedProductionExecutionAuthority.requiredTurns(game, machine, variant) - 1));
        machine.productionProgressTurns = persistedProgress;
        machine.productionMaterialPolicy = "RELEASE";
        machine.productionOutputPolicy = "FLOOR";
        machine.productionNoRoomPolicy = "CANCEL";
        machine.productionLastBlocker = "save/load blocker | detail";
        java.util.Properties savedSetup = new java.util.Properties();
        Persistence.writeCore(game, savedSetup);
        GamePanel restoredSetup = new GamePanel();
        if (restoredSetup.timer != null) restoredSetup.timer.stop();
        restoredSetup.baseObjects.clear();
        Persistence.readCore(restoredSetup, savedSetup);
        BaseObject restoredMachine = restoredSetup.baseObjects.stream()
                .filter(candidate -> candidate != null && "Staffed Test Station".equals(candidate.name))
                .findFirst().orElse(null);
        require(restoredMachine != null, "configured staffed machine should survive save/load");
        require(variantKey.equals(restoredMachine.assignedRecipe),
                "staffed generated job should survive save/load");
        require("Hest Var".equals(restoredMachine.assignedWorker),
                "staffed worker should survive save/load");
        require(restoredMachine.productionQueueTarget == 1 && restoredMachine.productionQueueRemaining == 1,
                "staffed queue should survive save/load");
        require(restoredMachine.productionProgressTurns == persistedProgress,
                "staffed background progress should survive save/load");
        require("RELEASE".equals(restoredMachine.productionMaterialPolicy)
                        && "FLOOR".equals(restoredMachine.productionOutputPolicy)
                        && "CANCEL".equals(restoredMachine.productionNoRoomPolicy),
                "staffed queue policies should survive save/load");
        require("save/load blocker | detail".equals(restoredMachine.productionLastBlocker),
                "staffed queue blocker readback should survive delimiter-safe save/load");
        require(restoredSetup.lastStaffedProductionBackgroundTurn == restoredSetup.turn,
                "save load should synchronize background production time without offline catch-up");
        game.world = restoredSetup.world;
        if (restoredSetup.timer != null) restoredSetup.timer.stop();
        machine.productionMaterialPolicy = "WAIT";
        machine.productionOutputPolicy = "BASE";
        machine.productionNoRoomPolicy = "WAIT";
        machine.productionLastBlocker = "";
        machine.productionQueueTarget = 3;
        machine.productionQueueRemaining = 3;
        machine.productionProgressTurns = 0;
        addInputs(game, variant, 2);
        int requiredTurns = StaffedProductionExecutionAuthority.requiredTurns(game, machine, variant);
        requireContains(StaffedProductionExecutionAuthority.forecastLines(game, machine),
                "Queued crew work advances automatically as game turns pass",
                "background production forecast");
        game.panelMode = GamePanel.PanelMode.NONE;
        game.activeInteractionBaseObject = null;
        int backgroundStartTurn = game.turn;
        int historyBeforeBackground = game.machineOperationQueue.historyCount();
        String outputName = ProductionRecipe.create(variant.base.outputBaseItem, variant.faction,
                variant.qualityName, variant.requiredKnowledge, variant.machineHint).outputItemName();
        long outputBeforeBackground = countMatches(game, outputName);

        advanceTurns(game, requiredTurns - 1);
        require(machine.productionQueueRemaining == 3,
                "background production should not complete before the required turns");
        require(machine.productionProgressTurns == Math.max(0, requiredTurns - 1),
                "background production should expose current-run progress");
        require(game.machineOperationQueue.historyCount() == historyBeforeBackground,
                "in-progress background work should not add completion history");

        advanceTurns(game, 1);
        require(machine.productionQueueRemaining == 2 && machine.productionProgressTurns == 0,
                "first staffed batch should complete after its background duration");
        require(countMatches(game, outputName) > outputBeforeBackground,
                "background staffed output should enter base storage");
        require(game.machineOperationQueue.historyCount() == historyBeforeBackground + 1,
                "first background completion should enter shared history");
        MachineOperationQueue.OperationRecord firstBackgroundRun = game.machineOperationQueue.recentHistory()
                .get(game.machineOperationQueue.historyCount() - 1);
        require(firstBackgroundRun.startedTurn == backgroundStartTurn
                        && firstBackgroundRun.completedTurn == game.turn,
                "first background record should span the production duration");

        advanceTurns(game, requiredTurns);
        require(machine.productionQueueRemaining == 1 && machine.productionProgressTurns == 0,
                "second staffed batch should continue while the player remains away");
        require(game.machineOperationQueue.historyCount() == historyBeforeBackground + 2,
                "second background completion should enter shared history");
        int blockedTurn = game.turn;
        advanceTurns(game, requiredTurns + 2);
        require(machine.productionQueueRemaining == 1 && machine.productionProgressTurns == 0,
                "missing inputs should pause the remaining background run without consuming it");
        require(game.machineOperationQueue.historyCount() == historyBeforeBackground + 2,
                "blocked background turns should not add completion history");
        require(game.turn == blockedTurn + requiredTurns + 2,
                "player time should continue while a staffed queue is blocked");

        addInputs(game, variant, 1);
        int resumedTurn = game.turn;
        advanceTurns(game, requiredTurns);
        require(machine.productionQueueRemaining == 0 && machine.productionProgressTurns == 0,
                "resupplied background production should resume and finish the queue");
        require(game.machineOperationQueue.historyCount() == historyBeforeBackground + 3,
                "resumed background completion should enter shared history");
        MachineOperationQueue.OperationRecord resumedRun = game.machineOperationQueue.recentHistory()
                .get(game.machineOperationQueue.historyCount() - 1);
        require(resumedRun.startedTurn == resumedTurn && resumedRun.completedTurn == game.turn,
                "resumed background history should span only active production turns");
        require(unrelatedMachine.productionQueueRemaining == 3,
                "background scheduler should not alter an unconfigured machine");

        ItemProvenanceRecord provenance = game.peekProvenanceForItem(outputName);
        require(provenance != null, "background staffed output should have provenance");
        requireContains(provenance.qualityContextLines(), "Producing operator: Hest Var", "worker provenance");
        requireContains(provenance.qualityContextLines(),
                "Production workforce: staffed queued production / assigned worker Hest Var", "staffed workforce mode");
        requireContains(provenance.qualityContextLines(), "Production legal status: generated variant law status: "
                + variant.lawStatus, "generated legal status");
        requireContains(provenance.qualityContextLines(), "Production source: generated recipe",
                "generated recipe source");
        requireContains(provenance.qualityContextLines(), "Producing machine: Staffed Test Station", "machine provenance");

        game.panelMode = GamePanel.PanelMode.WORKBENCH;
        game.activeInteractionBaseObject = machine;
        machine.productionQueueTarget = 2;
        machine.productionQueueRemaining = 2;
        machine.productionProgressTurns = 1;
        requireContains(game.clearActiveWorkbenchQueue(), "background production is idle",
                "workbench clear queue action");
        require(machine.productionQueueRemaining == 0 && machine.productionQueueTarget == 1
                        && machine.productionProgressTurns == 0,
                "Clear Queue should remove remaining runs and current progress");

        requireContains(game.cycleActiveWorkbenchMaterialPolicy(), "Pause / release worker",
                "material policy cycle");
        requireContains(game.cycleActiveWorkbenchOutputPolicy(), "Nearest Cache",
                "output policy cycle");
        requireContains(game.cycleActiveWorkbenchNoRoomPolicy(), "Pause / release worker",
                "no-room policy cycle");
        machine.productionNoRoomPolicy = "WAIT";

        machine.assignedWorker = "Hest Var";
        machine.productionQueueTarget = 1;
        machine.productionQueueRemaining = 1;
        advanceTurns(game, 1);
        require(machine.assignedWorker.isBlank() && machine.productionQueueRemaining == 1,
                "material release policy should free the worker and preserve the queued order");
        requireContains(machine.productionLastBlocker, "worker released by material policy",
                "material release blocker readback");

        ManualStaffingAssignmentAuthority.assign(game, machine, game.factionRecruits.get(0));
        machine.productionMaterialPolicy = "CANCEL";
        advanceTurns(game, 1);
        require(machine.productionQueueRemaining == 0 && machine.productionProgressTurns == 0,
                "material cancel policy should disband the remaining order");
        requireContains(machine.productionLastBlocker, "remaining queue cancelled by material policy",
                "material cancel blocker readback");

        ManualStaffingAssignmentAuthority.assign(game, machine, game.factionRecruits.get(0));
        machine.productionMaterialPolicy = "WAIT";
        machine.productionOutputPolicy = "NEAREST";
        machine.productionNoRoomPolicy = "WAIT";
        machine.productionQueueTarget = 1;
        machine.productionQueueRemaining = 1;
        machine.productionProgressTurns = 0;
        fillClaimedContainers(game);
        addInputs(game, variant, 1);
        int heldInputs = game.baseStorage.size();
        advanceTurns(game, requiredTurns + 1);
        require(machine.productionQueueRemaining == 1 && machine.productionProgressTurns == 0
                        && "Hest Var".equals(machine.assignedWorker),
                "no-room wait policy should retain worker and order without progressing");
        require(game.baseStorage.size() == heldInputs,
                "no-room wait policy should not consume production inputs");
        requireContains(machine.productionLastBlocker, "waiting for output space",
                "no-room wait blocker readback");

        machine.productionNoRoomPolicy = "FLOOR";
        advanceTurns(game, requiredTurns);
        MapObjectState floorPile = productionOutputPile(game, machine);
        require(floorPile != null, "floor fallback should create an interactable production output container");
        require(game.containerItemCount(game.persistentContainerIdForObject(floorPile)) > 0,
                "floor fallback should route completed output into the floor container ledger");
        require(machine.productionQueueRemaining == 0,
                "floor fallback should complete the queued order");

        MapObjectState factionCache = factionContainer(game, machine, 20);
        ManualStaffingAssignmentAuthority.assign(game, machine, game.factionRecruits.get(0));
        machine.productionOutputPolicy = "NEAREST";
        machine.productionNoRoomPolicy = "WAIT";
        machine.productionQueueTarget = 1;
        machine.productionQueueRemaining = 1;
        addInputs(game, variant, 1);
        advanceTurns(game, requiredTurns);
        require(machine.productionQueueRemaining == 0,
                "available nearest-cache routing should complete the queued order");
        require(game.containerItemCount(game.persistentContainerIdForObject(factionCache)) > 0,
                "nearest-cache routing should write output to the faction container ledger");

        GamePanel unassigned = new GamePanel();
        if (unassigned.timer != null) unassigned.timer.stop();
        unassigned.lastStaffedProductionBackgroundTurn = unassigned.turn;
        unassigned.baseClaimed = true;
        unassigned.claimedRoomId = 4;
        unassigned.baseX = 2;
        unassigned.baseY = 2;
        BaseObject unassignedMachine = new BaseObject("Unassigned Station", 'w', 2, 2, 0, 0);
        unassignedMachine.qualityName = "Archeotech";
        unassignedMachine.assignedRecipe = variantKey;
        unassignedMachine.assignedWorker = "Departed Worker";
        unassignedMachine.productionQueueTarget = 1;
        unassignedMachine.productionQueueRemaining = 1;
        unassigned.baseObjects.add(unassignedMachine);
        unassigned.factionRecruits.add(new RecruitWorker("Available Substitute", "forge worker", Faction.HIVER, 4, 4));
        unassigned.unlockedKnowledges.add(variant.requiredKnowledge);
        addInputs(unassigned, variant, 1);
        int staleStorage = unassigned.baseStorage.size();
        advanceTurns(unassigned, requiredTurns + 2);
        requireContains(StaffingLaborBridgeAuthority.readinessLine(unassigned, unassignedMachine),
                "staffing=invalid", "stale worker staffing readiness");
        requireContains(StaffedProductionExecutionAuthority.forecastLines(unassigned, unassignedMachine),
                "assigned worker Departed Worker is no longer recruited", "stale worker production forecast");
        require(unassigned.baseStorage.size() == staleStorage,
                "stale assigned worker should not consume inputs or add output");
        require(unassignedMachine.productionQueueRemaining == 1 && unassignedMachine.productionProgressTurns == 0,
                "stale assigned worker should pause without decrementing or progressing the queue");
        require(unassigned.machineOperationQueue.historyCount() == 0,
                "stale assigned worker should not add completion history");
        if (unassigned.timer != null) unassigned.timer.stop();
        if (game.timer != null) game.timer.stop();
    }

    private static void addInputs(GamePanel game, FactionRecipeVariant variant, int runs) {
        for (java.util.Map.Entry<String,Integer> input : variant.itemInputs.entrySet()) {
            for (int i = 0; i < input.getValue() * Math.max(0, runs); i++) game.baseStorage.add(input.getKey());
        }
    }

    private static void advanceTurns(GamePanel game, int turns) {
        for (int i = 0; i < Math.max(0, turns); i++) game.advanceTurn(null);
    }

    private static long countMatches(GamePanel game, String item) {
        return game.baseStorage.stream().filter(candidate -> ItemQuality.namesMatch(candidate, item)).count();
    }

    private static void fillClaimedContainers(GamePanel game) {
        for (MapObjectState object : game.world.mapObjects) {
            if (object == null || object.type == null || !object.type.toLowerCase().contains("container")
                    || object.type.toLowerCase().contains("production-output")
                    || !game.isInClaimedRoom(object.x, object.y)) continue;
            int count = game.containerItemCount(game.persistentContainerIdForObject(object));
            object.stockState = MapObjectState.setStockFlag(object.stockState, "capacity", Integer.toString(count));
        }
    }

    private static MapObjectState productionOutputPile(GamePanel game, BaseObject machine) {
        for (MapObjectState object : game.world.mapObjects) {
            if (object != null && object.type != null && object.type.equals("production-output-container")
                    && object.id != null && object.id.contains(Integer.toString(Math.abs(
                    java.util.Objects.hash(machine.name, machine.x, machine.y))))) return object;
        }
        return null;
    }

    private static MapObjectState factionContainer(GamePanel game, BaseObject machine, int capacity) {
        MapObjectState cache = new MapObjectState();
        cache.id = "STAFFED-SMOKE-CACHE";
        cache.type = "faction-container";
        cache.label = "Staffed smoke faction cache";
        cache.x = machine.x + 1;
        cache.y = machine.y;
        cache.glyph = 'o';
        cache.stockState = "ownerFaction=PLAYER;capacity=" + Math.max(1, capacity);
        game.world.mapObjects.add(cache);
        game.ensureContainer(game.persistentContainerIdForObject(cache), cache.label);
        return cache;
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

    private static void requireContains(java.util.List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private static void requireNotContains(String text, String forbidden, String label) {
        if (text == null || !text.contains(forbidden)) return;
        throw new AssertionError("Expected " + label + " not to contain '" + forbidden + "': " + text);
    }

    private static String lastEvent(GamePanel game) {
        if (game == null || game.eventLog.isEmpty()) return "";
        return game.eventLog.get(game.eventLog.size() - 1);
    }

    private Milestone03StaffedProductionExecutionSmoke() { }
}
