package mechanist;

import java.util.ArrayList;
import java.util.Comparator;

/** Builds bounded player-facing readback for the base-wide staffed-production board. */
final class ProductionControlBoardAuthority {
    private ProductionControlBoardAuthority() { }

    static ArrayList<BaseObject> machines(GamePanel game) {
        ArrayList<BaseObject> machines = game == null ? new ArrayList<>() : game.recruitOperableMachines();
        machines.removeIf(machine -> machine == null || machine.underConstruction);
        machines.sort(Comparator
                .comparingInt(ProductionControlBoardAuthority::attentionRank)
                .thenComparing(machine -> safe(machine.name, "machine"), String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(machine -> machine.x)
                .thenComparingInt(machine -> machine.y));
        return machines;
    }

    static String rowLabel(GamePanel game, BaseObject machine) {
        if (machine == null) return "No machine";
        String state = stateLabel(game, machine);
        int remaining = Math.max(0, machine.productionQueueRemaining);
        return safe(machine.name, "Machine") + " - " + state + " - queue " + remaining;
    }

    static ArrayList<String> detailLines(GamePanel game, BaseObject machine) {
        ArrayList<String> lines = new ArrayList<>();
        if (machine == null) {
            lines.add("No built production machines are available.");
            lines.add("Build a production station before configuring staffed background work.");
            return lines;
        }
        String assignment = ControlledProductionJobAuthority.assignmentLabel(machine.assignedRecipe);
        String worker = safe(machine.assignedWorker, "unassigned");
        lines.add("State: " + stateLabel(game, machine) + "; location " + machine.x + "," + machine.y + ".");
        lines.add("Job: " + assignment + "; worker " + worker + ".");

        FactionRecipeVariant variant = ControlledProductionJobAuthority.findVariantByAssignmentKey(machine.assignedRecipe);
        int requiredTurns = variant == null ? 0 : StaffedProductionExecutionAuthority.requiredTurns(game, machine, variant);
        String progress = requiredTurns <= 0 ? "not running"
                : Math.max(0, Math.min(requiredTurns, machine.productionProgressTurns)) + "/" + requiredTurns + " turns";
        lines.add("Queue: " + Math.max(0, machine.productionQueueRemaining) + "/"
                + Math.max(1, machine.productionQueueTarget) + " runs remaining/target; current run " + progress + ".");
        lines.add(ProductionQueuePolicyAuthority.policyLine(machine));
        if (variant == null) {
            lines.add("Open Workbench to choose a compatible staffed job before adding runs.");
        } else {
            String problem = currentProblem(game, machine, variant);
            lines.add(problem == null
                    ? "Next run is ready and advances automatically as game turns pass."
                    : "Next run cannot advance: " + problem + ".");
        }
        return lines;
    }

    static String stateLabel(GamePanel game, BaseObject machine) {
        if (machine == null) return "unavailable";
        FactionRecipeVariant variant = ControlledProductionJobAuthority.findVariantByAssignmentKey(machine.assignedRecipe);
        if (variant == null) {
            if (machine.assignedRecipe == null || machine.assignedRecipe.isBlank()) return "unconfigured";
            return ControlledProductionJobAuthority.isGeneratedAssignment(machine.assignedRecipe)
                    ? "invalid job" : "not staffed";
        }
        if (machine.productionQueueRemaining <= 0) return "idle";
        String problem = currentProblem(game, machine, variant);
        if (problem != null) return "blocked";
        if (machine.productionProgressTurns > 0) return "running";
        return "ready";
    }

    private static String currentProblem(GamePanel game, BaseObject machine, FactionRecipeVariant variant) {
        String workerProblem = ManualStaffingAssignmentAuthority.activeAssignmentProblem(game, machine);
        if (workerProblem != null) return workerProblem;
        return ControlledProductionJobAuthority.operationProblem(game, machine, variant, false);
    }

    private static int attentionRank(BaseObject machine) {
        if (machine == null) return 5;
        if (machine.productionQueueRemaining > 0
                && machine.productionLastBlocker != null && !machine.productionLastBlocker.isBlank()) return 0;
        if (machine.productionQueueRemaining > 0 && machine.productionProgressTurns > 0) return 1;
        if (machine.productionQueueRemaining > 0) return 2;
        if (ControlledProductionJobAuthority.isGeneratedAssignment(machine.assignedRecipe)) return 3;
        return 4;
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
