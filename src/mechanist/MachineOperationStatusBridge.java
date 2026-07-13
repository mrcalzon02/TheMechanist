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
    static final String VERSION = "0.9.10a";
    static final int MAX_LINES = 10;

    private MachineOperationStatusBridge() {}

    static String compactSummary(GamePanel g) {
        return compactSummary(g, g == null ? null : g.selectedWorkerMachine());
    }

    static String compactSummary(GamePanel g, BaseObject selected) {
        if (g == null || g.machineOperationQueue == null) return "machine operations unavailable";
        return "pending " + g.machineOperationQueue.pendingCount()
                + ", active " + g.machineOperationQueue.activeCount()
                + ", completed history " + g.machineOperationQueue.historyCount()
                + ", assigned machines " + assignedMachineCount(g)
                + ", selected " + selectedMachineLabel(selected);
    }

    static ArrayList<String> statusLines(GamePanel g) {
        return statusLines(g, g == null ? null : g.selectedWorkerMachine(), false);
    }

    static ArrayList<String> statusLinesForMachine(GamePanel g, BaseObject selected) {
        if (selected == null) return statusLines(g);
        return statusLines(g, selected, true);
    }

    private static ArrayList<String> statusLines(GamePanel g, BaseObject selected, boolean machineScoped) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Production status: " + (machineScoped ? compactMachineSummary(g, selected) : compactSummary(g, selected)) + ".");
        if (g == null) return lines;

        MachineOperationQueue q = g.machineOperationQueue;
        if (q == null) {
            lines.add("Production queue status is unavailable.");
        } else {
            List<MachineOperationQueue.OperationRecord> live = q.liveOperations();
            if (machineScoped) live = filteredRecordsForMachine(live, selected);
            if (live.isEmpty()) lines.add(machineScoped
                    ? "Selected machine live queue: no queued or active operations."
                    : "Live production queue: no shared queued or active operations.");
            else {
                lines.add(machineScoped ? "Selected machine live queue:" : "Live production queue:");
                int shown = 0;
                for (MachineOperationQueue.OperationRecord r : live) {
                    if (shown >= 4) break;
                    lines.add("  " + operationLine(r));
                    shown++;
                }
                if (live.size() > shown) lines.add("  ... " + (live.size() - shown) + " additional live operation(s) hidden by bounded display.");
            }
            List<MachineOperationQueue.OperationRecord> history = q.recentHistory();
            if (machineScoped) history = filteredRecordsForMachine(history, selected);
            if (machineScoped && history.isEmpty()) {
                lines.add("Selected machine completed history: no completed production records yet.");
            } else if (!history.isEmpty()) {
                MachineOperationQueue.OperationRecord last = history.get(history.size() - 1);
                lines.add((machineScoped ? "Latest completed production for selected machine: " : "Latest completed production: ") + operationLine(last));
                lines.add(machineScoped
                        ? "Use this machine's History action to review its recent completed production records."
                        : "Use production_history to review recent completed production records.");
            }
        }

        if (selected != null) {
            lines.add("Selected machine: " + machineLine(g, selected));
            lines.addAll(StaffedProductionExecutionAuthority.forecastLines(g, selected));
        }

        if (!machineScoped) {
            ArrayList<BaseObject> machines = g.recruitOperableMachines();
            if (machines.isEmpty()) lines.add("Base machines: no recruit-operable machines available.");
            else {
                lines.add("Base machine snapshot:");
                int shown = 0;
                for (BaseObject machine : machines) {
                    if (shown >= 5) break;
                    lines.add("  " + machineLine(g, machine));
                    shown++;
                }
                if (machines.size() > shown) lines.add("  ... " + (machines.size() - shown) + " additional machine(s) hidden by bounded display.");
            }
        }

        while (lines.size() > MAX_LINES) lines.remove(lines.size() - 1);
        return lines;
    }

    static ArrayList<String> historyLines(GamePanel g, int limit) {
        return historyLinesForMachine(g, null, limit);
    }

    static ArrayList<String> historyLinesForMachine(GamePanel g, BaseObject machine, int limit) {
        ArrayList<String> lines = new ArrayList<>();
        if (g == null || g.machineOperationQueue == null) {
            lines.add("Production history unavailable until a run is active.");
            return lines;
        }
        List<MachineOperationQueue.OperationRecord> history = g.machineOperationQueue.recentHistory();
        if (machine != null) history = filteredRecordsForMachine(history, machine);
        String prefix = machine == null ? "Production history" : "Production history for " + selectedMachineLabel(machine);
        if (history.isEmpty()) {
            lines.add(prefix + ": no completed production records yet.");
            return lines;
        }
        int boundedLimit = Math.max(1, Math.min(5, limit));
        int start = Math.max(0, history.size() - boundedLimit);
        lines.add(prefix + ": " + history.size() + " completed record(s), showing last " + (history.size() - start) + ".");
        for (int i = start; i < history.size(); i++) lines.add(historyLine(history.get(i)));
        return lines;
    }

    static String machineLine(GamePanel g, BaseObject machine) {
        if (machine == null) return "none";
        String recipeName = machine.assignedRecipe == null || machine.assignedRecipe.isBlank() ? "none" : machine.assignedRecipe;
        String recipeLabel = ControlledProductionJobAuthority.assignmentLabel(recipeName);
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
        return machine.name + " at " + machine.x + "," + machine.y
                + "; assignment " + recipeLabel
                + "; worker " + worker
                + "; queue " + Math.max(0, machine.productionQueueRemaining) + "/" + Math.max(1, machine.productionQueueTarget)
                + "; background progress " + Math.max(0, machine.productionProgressTurns) + " turn(s)"
                + "; " + status + ".";
    }

    private static String compactMachineSummary(GamePanel g, BaseObject machine) {
        if (g == null || g.machineOperationQueue == null) return "machine operations unavailable";
        MachineOperationQueue q = g.machineOperationQueue;
        return "machine pending " + filteredRecordsForMachine(q.pendingOperations(), machine).size()
                + ", active " + filteredRecordsForMachine(q.activeOperations(), machine).size()
                + ", completed history " + filteredRecordsForMachine(q.recentHistory(), machine).size()
                + ", selected " + selectedMachineLabel(machine);
    }

    private static ArrayList<MachineOperationQueue.OperationRecord> filteredRecordsForMachine(
            List<MachineOperationQueue.OperationRecord> records, BaseObject machine) {
        ArrayList<MachineOperationQueue.OperationRecord> out = new ArrayList<>();
        String token = machineTargetToken(machine);
        for (MachineOperationQueue.OperationRecord record : records) {
            String target = record == null ? "" : record.targetId;
            if (!token.isBlank() && target != null && target.contains(token)) out.add(record);
        }
        return out;
    }

    private static String machineTargetToken(BaseObject machine) {
        if (machine == null) return "";
        return (machine.name + "@" + machine.x + "," + machine.y).replace('|','/').replace(' ', '_');
    }

    private static String historyLine(MachineOperationQueue.OperationRecord record) {
        if (record == null) return "Production record unavailable.";
        return "Turn " + Math.max(0, record.completedTurn)
                + " " + readable(record.targetId)
                + " by " + readable(record.actorId)
                + ": " + historyStatus(record.status);
    }

    private static String historyStatus(String status) {
        String s = statusSummary(status);
        return "production record".equals(s) ? "completed production record" : s;
    }

    private static String operationLine(MachineOperationQueue.OperationRecord record) {
        if (record == null || record.profile == null) return "production record unavailable";
        return "turn " + Math.max(0, record.completedTurn)
                + " " + readable(record.targetId)
                + " by " + readable(record.actorId)
                + " state " + stateLabel(record.state)
                + " progress " + Math.max(0, record.progressTurns) + "/" + Math.max(1, record.profile.nominalTurns)
                + "; " + statusSummary(record.status);
    }

    private static String statusSummary(String status) {
        String s = ChatRuntimeAuthority.ChatSecurity.sanitizeLogLine(status == null ? "" : status).trim();
        s = s.replace("; legacy production consumed inputs and created outputs", "");
        s = s.replace("; legacy crew shift remains outcome authority", "");
        s = s.replace("; generated production remains outcome authority", "");
        s = s.replace("; outcome authority remains existing workbench code", "");
        if (s.startsWith("manual recipe completion recorded: ")) s = "Manual Craft completed: " + s.substring("manual recipe completion recorded: ".length());
        else if (s.startsWith("recruit recipe completion recorded: ")) s = "Staffed recipe completed: " + s.substring("recruit recipe completion recorded: ".length());
        else if (s.startsWith("recruit generated production completion recorded: ")) s = "Staffed generated production completed: " + s.substring("recruit generated production completion recorded: ".length());
        else if (s.startsWith("manual generated production completion recorded: ")) s = "Manual generated production completed: " + s.substring("manual generated production completion recorded: ".length());
        else if (s.startsWith("completed legacy workbench action: ")) s = "Workbench action completed: " + s.substring("completed legacy workbench action: ".length());
        return s.isBlank() ? "production record" : s;
    }

    private static String stateLabel(MachineOperationQueue.State state) {
        return state == null ? "unknown" : state.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private static String readable(String token) {
        String s = ChatRuntimeAuthority.ChatSecurity.sanitizeLogLine(token == null ? "" : token).trim();
        if (s.isBlank()) return "unknown";
        return s.replace('_', ' ').replace(':', ' ').replaceAll("\\s+", " ");
    }

    private static int assignedMachineCount(GamePanel g) {
        if (g == null) return 0;
        int n = 0;
        for (BaseObject machine : g.recruitOperableMachines()) {
            if (machine.assignedRecipe != null && !machine.assignedRecipe.isBlank()) n++;
        }
        return n;
    }

    private static String selectedMachineLabel(BaseObject machine) {
        return machine == null ? "none" : machine.name + " at " + machine.x + "," + machine.y;
    }
}
