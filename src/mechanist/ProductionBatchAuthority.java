package mechanist;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/** Creates one traceable inspection disposition for each immediate production batch. */
final class ProductionBatchAuthority {
    record BatchDisposition(String batchId, int defectRiskPercent, int inspectionRoll,
                            String defectState, List<String> lines) { }

    private ProductionBatchAuthority() { }

    static BatchDisposition assess(ProductionRecipe recipe, BaseObject machine,
                                   ProductionOperatorSkillAuthority.OperatorSkill operator,
                                   Random rng, int turn) {
        Random useRng = rng == null ? new Random(Objects.hash(turn, recipe == null ? "" : recipe.baseItem)) : rng;
        return assess(recipe, machine, operator, turn, useRng.nextInt(100) + 1, useRng.nextLong());
    }

    static BatchDisposition assess(ProductionRecipe recipe, BaseObject machine,
                                   ProductionOperatorSkillAuthority.OperatorSkill operator,
                                   int turn, int inspectionRoll, long nonce) {
        int adjustment = operator == null ? 0 : operator.defectRiskAdjust();
        int risk = recipe == null ? 1 : recipe.estimatedDefectPercent(machine, adjustment);
        int roll = Math.max(1, Math.min(100, inspectionRoll));
        boolean defective = roll <= risk;
        String state = defective ? "defect flagged" : "passed inspection";
        String item = recipe == null ? "unknown" : recipe.outputItemName();
        String id = "BATCH-" + Math.max(0, turn) + "-" + Integer.toUnsignedString(
                Objects.hash(item, machine == null ? "manual" : machine.name, nonce), 36).toUpperCase();
        return new BatchDisposition(id, risk, roll, state, List.of(
                "Batch identity: " + id + ".",
                "Batch inspection: " + state + " (roll " + roll + " against " + risk + "% risk).",
                "Effect boundary: the disposition is recorded provenance; item statistics are not yet reduced by a flagged defect."));
    }
}
