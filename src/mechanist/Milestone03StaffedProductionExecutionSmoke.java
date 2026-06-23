package mechanist;

/** Smoke for one-run staffed generated-production execution. */
final class Milestone03StaffedProductionExecutionSmoke {
    public static void main(String[] args) {
        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.baseClaimed = true;
        game.claimedRoomId = 3;
        game.baseX = 5;
        game.baseY = 5;
        game.turn = 44;

        FactionRecipeVariant variant = firstRunnableVariant(game);
        require(variant != null, "expected at least one generated staffed variant to be runnable");
        BaseObject machine = new BaseObject("Staffed Test Station", 'w', 5, 5, 0, 0);
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

        StaffedProductionExecutionAuthority.StaffedRunResult result =
                StaffedProductionExecutionAuthority.executeOne(game, machine);
        require(result.success(), result.message());
        require(game.baseStorage.stream().anyMatch(item -> ItemQuality.namesMatch(item, result.outputName())),
                "staffed output should enter base storage");
        require(machine.productionQueueRemaining == 1, "queue remaining should decrement by one");
        require(game.machineOperationQueue.historyCount() == 1, "shared operation history should record completion");
        ItemProvenanceRecord provenance = game.peekProvenanceForItem(result.outputName());
        require(provenance != null, "staffed output should have provenance");
        requireContains(provenance.qualityContextLines(), "Producing operator: Hest Var", "worker provenance");
        requireContains(provenance.qualityContextLines(),
                "Production workforce: staffed queued production / assigned worker Hest Var", "staffed workforce mode");
        requireContains(provenance.qualityContextLines(), "Production legal status: generated variant law status: "
                + variant.lawStatus, "generated legal status");
        requireContains(provenance.qualityContextLines(), "Production source: generated recipe",
                "generated recipe source");
        requireContains(provenance.qualityContextLines(), "Producing machine: Staffed Test Station", "machine provenance");
        require(StaffedProductionExecutionAuthority.forecastLines(game, machine).get(0).contains("queue 1/2"),
                "forecast should show decremented queue state");
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

    private static void requireContains(java.util.List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03StaffedProductionExecutionSmoke() { }
}
