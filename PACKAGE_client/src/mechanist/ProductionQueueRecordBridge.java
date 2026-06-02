package mechanist;

import java.util.*;

/**
 * 0.9.08w — conservative production queue record bridge.
 *
 * Records completed manual/recruit/workbench production into MachineOperationQueue history
 * without allowing the queue to execute outcomes, consume inputs, or own save authority yet.
 * This gives UI/debug/status systems one shared operational vocabulary while preserving
 * legacy production as the source of truth.
 */
final class ProductionQueueRecordBridge {
    static final String VERSION = "0.9.08x";

    private ProductionQueueRecordBridge() {}

    static MachineOperationQueue.OperationRecord recordWorkbenchAction(GamePanel g, BaseObject machine, String actionName, int durationTurns, String actor) {
        if (g == null) return null;
        String op = operationTypeFor(machine, actionName);
        String owner = "player_base";
        String target = targetId(machine, actionName);
        String status = "completed legacy workbench action: " + clean(actionName) + "; outcome authority remains existing workbench code";
        return record(g, op, actorId(actor, true), owner, target, Math.max(1, durationTurns), status);
    }

    static MachineOperationQueue.OperationRecord recordManualRecipeCompletion(GamePanel g, BaseObject machine, CraftingRecipe recipe, int durationTurns, int outputCount) {
        if (g == null || recipe == null) return null;
        String op = operationTypeFor(machine, recipe.name + " " + recipe.outputBaseItem);
        String actor = actorId(g.active == null ? "player manual operation" : g.active.name + " manual operation", true);
        String status = "manual recipe completion recorded: " + recipe.name + " outputs=" + Math.max(1, outputCount) + "; legacy production consumed inputs and created outputs";
        return record(g, op, actor, "player_base", targetId(machine, recipe.name), Math.max(1, durationTurns), status);
    }

    static MachineOperationQueue.OperationRecord recordRecruitRecipeCompletion(GamePanel g, BaseObject machine, CraftingRecipe recipe, int durationTurns, int outputCount, String worker) {
        if (g == null || recipe == null) return null;
        String op = operationTypeFor(machine, recipe.name + " " + recipe.outputBaseItem);
        String status = "recruit recipe completion recorded: " + recipe.name + " outputs=" + Math.max(1, outputCount) + "; legacy crew shift remains outcome authority";
        return record(g, op, actorId(worker, false), "player_base", targetId(machine, recipe.name), Math.max(1, durationTurns), status);
    }

    static MachineOperationQueue.OperationRecord recordGeneratedCompletion(GamePanel g, BaseObject machine, FactionRecipeVariant variant, boolean manual, int durationTurns, int outputCount, String operator) {
        if (g == null || variant == null) return null;
        String seed = variant.outputName + " " + (variant.base == null ? "" : variant.base.processType);
        String op = operationTypeFor(machine, seed);
        String actor = actorId(operator, manual);
        String status = (manual ? "manual" : "recruit") + " generated production completion recorded: " + variant.outputName
                + " outputs=" + Math.max(1, outputCount) + "; generated production remains outcome authority";
        return record(g, op, actor, "player_base", targetId(machine, variant.outputName), Math.max(1, durationTurns), status);
    }

    private static MachineOperationQueue.OperationRecord record(GamePanel g, String op, String actor, String owner, String target, int durationTurns, String status) {
        if (g.machineOperationQueue == null) return null;
        MachineOperationQueue.OperationRecord r = g.machineOperationQueue.recordExternalCompletion(op, actor, owner, target, g.turn, durationTurns, status);
        if (r != null) DebugLog.audit("PRODUCTION_QUEUE_RECORD_BRIDGE", "op=" + r.operationId + " type=" + op + " actor=" + actor + " target=" + target + " duration=" + durationTurns + " status=" + status + " state=" + g.stateSummary());
        return r;
    }

    private static String operationTypeFor(BaseObject machine, String seed) {
        char sym = machine == null ? 'w' : machine.symbol;
        String low = (seed == null ? "" : seed).toLowerCase(Locale.ROOT);
        if (sym == 'l' || low.contains("lab") || low.contains("research") || low.contains("assay") || low.contains("knowledge")) return "micro_lab_assay";
        if (sym == 'M' || low.contains("medicae") || low.contains("medical") || low.contains("triage") || low.contains("wound")) return "medicae_triage_service";
        if (low.contains("sterile") || low.contains("clean bench")) return "sterile_clean_bench_preparation";
        if (sym == 'L' || low.contains("distill")) return low.contains("distill") ? "distillation_batch" : "crude_chem_batch";
        if (low.contains("chem") || low.contains("slurry") || low.contains("solvent") || low.contains("reagent")) return "crude_chem_batch";
        return "micro_forge_basic_part";
    }

    private static String targetId(BaseObject machine, String action) {
        String m = machine == null ? "workbench" : machine.name + "@" + machine.x + "," + machine.y;
        return sanitize(m + ":" + clean(action));
    }

    private static String actorId(String actor, boolean manual) {
        String a = clean(actor);
        if (a.isBlank()) a = manual ? "player_manual_operation" : "assigned_recruit_crew";
        return sanitize(a);
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static String sanitize(String s) { return clean(s).replace('|','/').replace(' ', '_'); }

    static String summary(GamePanel g) {
        if (g == null || g.machineOperationQueue == null) return "productionQueueRecordBridge version=" + VERSION + " unavailable";
        return "productionQueueRecordBridge version=" + VERSION + " queue=" + g.machineOperationQueue.auditSummary();
    }
}
