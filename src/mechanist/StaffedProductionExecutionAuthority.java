package mechanist;

import java.util.List;

/** Executes one already-assigned staffed generated-production run. */
final class StaffedProductionExecutionAuthority {
    record StaffedRunResult(boolean success, String message, String outputName, int outputCount,
                            int remainingQueue, ProductionInputConsumptionRecord consumed) { }

    private StaffedProductionExecutionAuthority() { }

    static StaffedRunResult executeOne(GamePanel game, BaseObject machine) {
        if (game == null) return fail("No game context for staffed production.");
        if (machine == null) return fail("No selected machine for staffed production.");
        if (machine.assignedRecipe == null || machine.assignedRecipe.isBlank()) {
            return fail("No assigned generated production job.");
        }
        FactionRecipeVariant variant = ControlledProductionJobAuthority.findVariantByAssignmentKey(machine.assignedRecipe);
        if (variant == null) return fail("Assigned job is not a generated production variant.");
        if (machine.assignedWorker == null || machine.assignedWorker.isBlank()) {
            return fail("No assigned worker for staffed production.");
        }
        if (machine.productionQueueRemaining <= 0) {
            return fail("No queued staffed production run remains.");
        }
        String problem = ControlledProductionJobAuthority.operationProblem(game, machine, variant, false);
        if (problem != null) return fail("Staffed production blocked: " + problem + ".");

        FacilityOutputRunEstimate estimate = FacilityOutputModifierAuthority.estimate(game, machine, variant, false);
        ProductionInputConsumptionRecord consumed = ControlledProductionJobAuthority.consumeInputsWithTrace(
                game, variant, "staffed queued production by " + machine.assignedWorker);
        if (consumed == null || !consumed.success) {
            return new StaffedRunResult(false,
                    consumed == null || consumed.failureReason == null ? "Staffed production input consumption failed." : consumed.failureReason,
                    "", 0, Math.max(0, machine.productionQueueRemaining), consumed);
        }

        ProductionRecipe production = ProductionRecipe.create(variant.base.outputBaseItem, variant.faction,
                variant.qualityName, variant.requiredKnowledge, variant.machineHint);
        String output = production.outputItemName();
        int count = Math.max(1, estimate.outputCount);
        ProductionLocationAuthority.ProductionLocation location = ProductionLocationAuthority.evaluate(game, machine);
        for (int i = 0; i < count; i++) {
            ItemProvenanceRecord provenance = ItemProvenanceRecord.produced(
                    production, machine, game.world, game.turn, machine.assignedWorker,
                    null, null, null, null, null, location);
            provenance.productionMode = ProductionWorkforceModeAuthority.staffedLabel(machine.assignedWorker);
            provenance.productionLegalStatus = ProductionLegalStatusAuthority.provenanceLabel(variant);
            provenance.productionSource = ProductionSourceProvenanceAuthority.generatedSource(variant);
            provenance.batchIssueTags = ProductionBatchIssueAuthority.tagsFor(variant, null);
            game.baseStorage.add(output);
            game.rememberItemProvenance(output, provenance);
        }
        if (estimate.wear > 0) machine.integrity = Math.max(0, machine.integrity - estimate.wear);
        machine.productionQueueRemaining = Math.max(0, machine.productionQueueRemaining - 1);
        ProductionQueueRecordBridge.recordGeneratedCompletion(game, machine, variant, false,
                Math.max(1, estimate.turns), count, machine.assignedWorker);
        String message = "Staffed production completed " + count + "x " + output
                + " via " + machine.assignedWorker + "; queue remaining "
                + machine.productionQueueRemaining + "/" + Math.max(1, machine.productionQueueTarget) + ".";
        game.logEvent(message);
        return new StaffedRunResult(true, message, output, count, machine.productionQueueRemaining, consumed);
    }

    static List<String> forecastLines(GamePanel game, BaseObject machine) {
        if (machine == null) return List.of("Staffed production execution: no selected machine.");
        if (machine.assignedRecipe == null || machine.assignedRecipe.isBlank()) {
            return List.of("Staffed production execution: no generated assignment queued.");
        }
        FactionRecipeVariant variant = ControlledProductionJobAuthority.findVariantByAssignmentKey(machine.assignedRecipe);
        if (variant == null) return List.of("Staffed production execution: assignment is not a generated job.");
        String problem = ControlledProductionJobAuthority.operationProblem(game, machine, variant, false);
        String ready = problem == null && machine.productionQueueRemaining > 0 ? "ready" : "blocked";
        return List.of("Staffed production execution: " + ready + " for " + variant.outputName
                + "; worker " + (machine.assignedWorker == null || machine.assignedWorker.isBlank() ? "unassigned" : machine.assignedWorker)
                + "; queue " + Math.max(0, machine.productionQueueRemaining) + "/" + Math.max(1, machine.productionQueueTarget)
                + (problem == null ? "." : "; " + problem + "."));
    }

    private static StaffedRunResult fail(String message) {
        return new StaffedRunResult(false, message, "", 0, 0, null);
    }
}
