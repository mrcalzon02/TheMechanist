package mechanist;

import java.util.*;

/**
 * 0.9.08w — lightweight machine-operation display bridge.
 *
 * This authority does not execute production and does not change production outcomes.
 * It translates the existing MachineOperationQueue authority and current base-machine
 * assignment state into bounded, player-facing status lines suitable for the
 * UniversalWindow machine operation surface. Keep it cheap: no map scans, no inventory
 * rescans, no background loops.
 */
final class MachineOperationStatusBridge {
    static final String VERSION = "0.9.08x";
    static final int MAX_LINES = 10;

    private MachineOperationStatusBridge() {}

    static String compactSummary(GamePanel g) {
        if (g == null || g.machineOperationQueue == null) return "machine operations unavailable";
        return "machineOperationDisplayBridge version=" + VERSION
                + " queue[pending=" + g.machineOperationQueue.pendingCount()
                + ", active=" + g.machineOperationQueue.activeCount()
                + ", history=" + g.machineOperationQueue.historyCount() + "]"
                + " assignedMachines=" + assignedMachineCount(g)
                + " selected=" + selectedMachineLabel(g);
    }

    static ArrayList<String> statusLines(GamePanel g) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Shared machine operation surface: " + compactSummary(g));
        if (g == null) return lines;

        UniversalWindowAuthority.RuntimeWindowState win = g.universalWindowAuthority == null ? null : g.universalWindowAuthority.state("machine_operations");
        if (win != null) lines.add("UniversalWindow route: " + win.auditLine());

        MachineOperationQueue q = g.machineOperationQueue;
        if (q == null) {
            lines.add("Queue authority not present.");
        } else {
            List<MachineOperationQueue.OperationRecord> live = q.liveOperations();
            if (live.isEmpty()) lines.add("Live shared queue: empty. Existing production still runs through legacy/manual pathways until migration is proven safe.");
            else {
                lines.add("Live shared queue records:");
                int shown = 0;
                for (MachineOperationQueue.OperationRecord r : live) {
                    if (shown >= 4) break;
                    lines.add("  " + r.auditLine());
                    shown++;
                }
                if (live.size() > shown) lines.add("  ... " + (live.size() - shown) + " additional live operation(s) hidden by bounded display.");
            }
            List<MachineOperationQueue.OperationRecord> history = q.recentHistory();
            if (!history.isEmpty()) {
                MachineOperationQueue.OperationRecord last = history.get(history.size() - 1);
                lines.add("Last completed/shared record: " + last.auditLine());
                lines.add("Persisted queue history sample:");
                for (String line : q.recentHistoryLines(3)) lines.add("  " + line);
                lines.add("Record bridge: " + ProductionQueueRecordBridge.summary(g));
            }
        }

        BaseObject selected = g.selectedWorkerMachine();
        if (selected != null) {
            lines.add("Selected machine bridge: " + machineLine(g, selected));
            lines.addAll(StaffedProductionExecutionAuthority.forecastLines(g, selected));
        }

        ArrayList<BaseObject> machines = g.recruitOperableMachines();
        if (machines.isEmpty()) lines.add("Base machine bridge: no recruit-operable machines available.");
        else {
            lines.add("Base machine bridge snapshot:");
            int shown = 0;
            for (BaseObject machine : machines) {
                if (shown >= 5) break;
                lines.add("  " + machineLine(g, machine));
                shown++;
            }
            if (machines.size() > shown) lines.add("  ... " + (machines.size() - shown) + " additional machine(s) hidden by bounded display.");
        }

        while (lines.size() > MAX_LINES) lines.remove(lines.size() - 1);
        return lines;
    }

    static String machineLine(GamePanel g, BaseObject machine) {
        if (machine == null) return "none";
        String recipeName = machine.assignedRecipe == null || machine.assignedRecipe.isBlank() ? "none" : machine.assignedRecipe;
        String worker = machine.assignedWorker == null || machine.assignedWorker.isBlank() ? "unassigned" : machine.assignedWorker;
        String status = "idle";
        if (!"none".equals(recipeName)) {
            if (ControlledProductionJobAuthority.isGeneratedAssignment(recipeName)) {
                FactionRecipeVariant v = ControlledProductionJobAuthority.findVariantByAssignmentKey(recipeName);
                if (v == null) status = "invalid generated assignment";
                else {
                    String problem = ControlledProductionJobAuthority.operationProblem(g, machine, v, false);
                    status = problem == null ? "crew-ready" : "blocked: " + problem;
                }
            } else {
                CraftingRecipe r = CraftingRecipe.byName(recipeName);
                if (r == null) status = "invalid recipe assignment";
                else {
                    String problem = r.blockingProblemForMachine(g, machine);
                    status = problem == null ? "recipe-ready" : "blocked: " + problem;
                }
            }
        }
        return machine.name + "@" + machine.x + "," + machine.y
                + " recipe=" + recipeName
                + " worker=" + worker
                + " queue=" + Math.max(0, machine.productionQueueRemaining) + "/" + Math.max(1, machine.productionQueueTarget)
                + " status=" + status;
    }

    private static int assignedMachineCount(GamePanel g) {
        if (g == null) return 0;
        int n = 0;
        for (BaseObject machine : g.recruitOperableMachines()) {
            if (machine.assignedRecipe != null && !machine.assignedRecipe.isBlank()) n++;
        }
        return n;
    }

    private static String selectedMachineLabel(GamePanel g) {
        if (g == null) return "none";
        BaseObject m = g.selectedWorkerMachine();
        return m == null ? "none" : m.name + "@" + m.x + "," + m.y;
    }
}
