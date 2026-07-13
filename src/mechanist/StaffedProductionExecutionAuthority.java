package mechanist;

import java.util.List;

/** Completes one ready staffed generated-production run for the background scheduler. */
final class StaffedProductionExecutionAuthority {
    record StaffedRunResult(boolean success, String message, String outputName, int outputCount,
                            int remainingQueue, ProductionInputConsumptionRecord consumed) { }

    private StaffedProductionExecutionAuthority() { }

    static StaffedRunResult executeOne(GamePanel game, BaseObject machine) {
        return executeOne(game, machine, true, null);
    }

    static StaffedRunResult executeOne(GamePanel game, BaseObject machine,
                                       ProductionQueuePolicyAuthority.DestinationPlan destination) {
        return executeOne(game, machine, true, destination);
    }

    private static StaffedRunResult executeOne(GamePanel game, BaseObject machine, boolean logCompletion,
                                                ProductionQueuePolicyAuthority.DestinationPlan destination) {
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
        String workerProblem = ManualStaffingAssignmentAuthority.activeAssignmentProblem(game, machine);
        if (workerProblem != null) return fail("Staffed production blocked: " + workerProblem + ".");
        if (machine.productionQueueRemaining <= 0) {
            return fail("No queued staffed production run remains.");
        }
        String problem = ControlledProductionJobAuthority.operationProblem(game, machine, variant, false);
        if (problem != null) return fail("Staffed production blocked: " + problem + ".");

        FacilityOutputRunEstimate estimate = FacilityOutputModifierAuthority.estimate(game, machine, variant, false);
        if (destination == null) {
            destination = ProductionQueuePolicyAuthority.destinationPlan(game, machine, Math.max(1, estimate.outputCount));
        }
        if (!destination.ready()) return fail("Staffed production blocked: " + destination.blocker() + ".");
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
            ProductionQueuePolicyAuthority.routeOutput(game, machine, destination, output, provenance);
        }
        if (estimate.wear > 0) machine.integrity = Math.max(0, machine.integrity - estimate.wear);
        machine.productionQueueRemaining = Math.max(0, machine.productionQueueRemaining - 1);
        ProductionQueueRecordBridge.recordGeneratedCompletion(game, machine, variant, false,
                Math.max(1, estimate.turns), count, machine.assignedWorker);
        String message = "Staffed production completed " + count + "x " + output
                + " via " + machine.assignedWorker + " to " + destination.label() + "; queue remaining "
                + machine.productionQueueRemaining + "/" + Math.max(1, machine.productionQueueTarget) + ".";
        if (logCompletion) game.logEvent(message);
        return new StaffedRunResult(true, message, output, count, machine.productionQueueRemaining, consumed);
    }

    static int requiredTurns(GamePanel game, BaseObject machine, FactionRecipeVariant variant) {
        return Math.max(1, FacilityOutputModifierAuthority.estimate(game, machine, variant, false).turns);
    }

    static List<String> forecastLines(GamePanel game, BaseObject machine) {
        if (machine == null) return List.of("Staffed production execution: no selected machine.");
        if (machine.assignedRecipe == null || machine.assignedRecipe.isBlank()) {
            return List.of("Staffed production execution: no generated assignment queued.");
        }
        FactionRecipeVariant variant = ControlledProductionJobAuthority.findVariantByAssignmentKey(machine.assignedRecipe);
        if (variant == null) return List.of("Staffed production execution: assignment is not a generated job.");
        String workerProblem = ManualStaffingAssignmentAuthority.activeAssignmentProblem(game, machine);
        String problem = workerProblem == null
                ? ControlledProductionJobAuthority.operationProblem(game, machine, variant, false)
                : workerProblem;
        String ready = problem == null && machine.productionQueueRemaining > 0 ? "ready" : "blocked";
        int runTurns = requiredTurns(game, machine, variant);
        int progress = Math.max(0, Math.min(runTurns, machine.productionProgressTurns));
        return List.of("Background staffed production: " + ready + " for " + variant.outputName
                + "; worker " + (machine.assignedWorker == null || machine.assignedWorker.isBlank() ? "unassigned" : machine.assignedWorker)
                + "; queue " + Math.max(0, machine.productionQueueRemaining) + "/" + Math.max(1, machine.productionQueueTarget)
                + "; current run progress " + progress + "/" + runTurns + " turns"
                + (problem == null ? "." : "; " + problem + "."),
                "Queued crew work advances automatically as game turns pass, including while the player leaves the workbench; blockers follow this machine's material and output policies.",
                ProductionQueuePolicyAuthority.policyLine(machine));
    }

    private static StaffedRunResult fail(String message) {
        return new StaffedRunResult(false, message, "", 0, 0, null);
    }
}
