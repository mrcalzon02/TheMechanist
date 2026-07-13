package mechanist;

import java.util.ArrayList;

/** Advances assigned staffed production as ordinary game turns pass. */
final class StaffedProductionBackgroundAuthority {
    private StaffedProductionBackgroundAuthority() { }

    static int tick(GamePanel game, int elapsedTurns) {
        if (game == null || elapsedTurns <= 0) return 0;
        int completions = 0;
        for (int step = 0; step < elapsedTurns; step++) {
            for (BaseObject machine : new ArrayList<>(game.baseObjects)) {
                completions += tickMachine(game, machine);
            }
        }
        return completions;
    }

    private static int tickMachine(GamePanel game, BaseObject machine) {
        if (machine == null || machine.productionQueueRemaining <= 0) {
            if (machine != null) machine.productionProgressTurns = 0;
            return 0;
        }
        if (!ControlledProductionJobAuthority.isGeneratedAssignment(machine.assignedRecipe)) {
            machine.productionProgressTurns = 0;
            return 0;
        }
        FactionRecipeVariant variant = ControlledProductionJobAuthority.findVariantByAssignmentKey(machine.assignedRecipe);
        if (variant == null) {
            machine.productionProgressTurns = 0;
            return 0;
        }
        String workerProblem = ManualStaffingAssignmentAuthority.activeAssignmentProblem(game, machine);
        if (workerProblem != null) {
            setBlocker(game, machine, workerProblem);
            return 0;
        }
        if (ControlledProductionJobAuthority.inputShortageCount(game, variant) > 0) {
            handleMaterialShortage(game, machine, variant);
            return 0;
        }
        String operationProblem = ControlledProductionJobAuthority.operationProblem(game, machine, variant, false);
        if (operationProblem != null) {
            setBlocker(game, machine, operationProblem);
            return 0;
        }

        FacilityOutputRunEstimate estimate = FacilityOutputModifierAuthority.estimate(game, machine, variant, false);
        ProductionQueuePolicyAuthority.DestinationPlan destination =
                ProductionQueuePolicyAuthority.destinationPlan(game, machine, Math.max(1, estimate.outputCount));
        if (!destination.ready()) {
            destination = handleNoRoom(game, machine, destination.blocker());
            if (destination == null || !destination.ready()) return 0;
        }
        machine.productionLastBlocker = "";

        int required = StaffedProductionExecutionAuthority.requiredTurns(game, machine, variant);
        machine.productionProgressTurns = Math.min(required, Math.max(0, machine.productionProgressTurns) + 1);
        if (machine.productionProgressTurns < required) return 0;

        StaffedProductionExecutionAuthority.StaffedRunResult result =
                StaffedProductionExecutionAuthority.executeOne(game, machine, destination);
        if (!result.success()) {
            machine.productionProgressTurns = Math.max(0, required - 1);
            return 0;
        }
        machine.productionProgressTurns = 0;
        return 1;
    }

    private static void handleMaterialShortage(GamePanel game, BaseObject machine, FactionRecipeVariant variant) {
        String reason = ControlledProductionJobAuthority.operationProblem(game, machine, variant, false);
        if (reason == null || reason.isBlank()) reason = "required production materials are unavailable";
        ProductionQueuePolicyAuthority.MaterialPolicy policy = ProductionQueuePolicyAuthority.materialPolicy(machine);
        if (policy == ProductionQueuePolicyAuthority.MaterialPolicy.RELEASE) {
            setBlocker(game, machine, reason + "; worker released by material policy");
            machine.assignedWorker = "";
        } else if (policy == ProductionQueuePolicyAuthority.MaterialPolicy.CANCEL) {
            setBlocker(game, machine, reason + "; remaining queue cancelled by material policy");
            cancelQueue(machine);
        } else {
            setBlocker(game, machine, reason + "; waiting for materials");
        }
    }

    private static ProductionQueuePolicyAuthority.DestinationPlan handleNoRoom(
            GamePanel game, BaseObject machine, String reason) {
        ProductionQueuePolicyAuthority.NoRoomPolicy policy = ProductionQueuePolicyAuthority.noRoomPolicy(machine);
        if (policy == ProductionQueuePolicyAuthority.NoRoomPolicy.FLOOR) {
            ProductionQueuePolicyAuthority.DestinationPlan floor =
                    ProductionQueuePolicyAuthority.floorPlan(game, machine);
            if (floor.ready()) {
                setBlocker(game, machine, reason + "; routing output to floor by policy");
                return floor;
            }
            setBlocker(game, machine, reason + "; floor fallback also unavailable: " + floor.blocker());
            return null;
        }
        if (policy == ProductionQueuePolicyAuthority.NoRoomPolicy.RELEASE) {
            setBlocker(game, machine, reason + "; worker released by no-room policy");
            machine.assignedWorker = "";
        } else if (policy == ProductionQueuePolicyAuthority.NoRoomPolicy.CANCEL) {
            setBlocker(game, machine, reason + "; remaining queue cancelled by no-room policy");
            cancelQueue(machine);
        } else {
            setBlocker(game, machine, reason + "; waiting for output space");
        }
        return null;
    }

    private static void cancelQueue(BaseObject machine) {
        machine.productionQueueTarget = 1;
        machine.productionQueueRemaining = 0;
        machine.productionProgressTurns = 0;
    }

    private static void setBlocker(GamePanel game, BaseObject machine, String reason) {
        String clean = reason == null ? "background production blocked" : reason;
        if (!clean.equals(machine.productionLastBlocker)) game.logEvent(machine.name + " background production: " + clean + ".");
        machine.productionLastBlocker = clean;
    }
}
