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
        return assess(recipe, machine, operator, null, rng, turn);
    }

    static BatchDisposition assess(ProductionRecipe recipe, BaseObject machine,
                                   ProductionOperatorSkillAuthority.OperatorSkill operator,
                                   ProductionFatiguePressureAuthority.FatiguePressure pressure,
                                   Random rng, int turn) {
        Random useRng = rng == null ? new Random(Objects.hash(turn, recipe == null ? "" : recipe.baseItem)) : rng;
        return assess(recipe, machine, operator, pressure, turn, useRng.nextInt(100) + 1, useRng.nextLong());
    }

    static BatchDisposition assess(ProductionRecipe recipe, BaseObject machine,
                                   ProductionOperatorSkillAuthority.OperatorSkill operator,
                                   int turn, int inspectionRoll, long nonce) {
        return assess(recipe, machine, operator, null, turn, inspectionRoll, nonce);
    }

    static BatchDisposition assess(ProductionRecipe recipe, BaseObject machine,
                                   ProductionOperatorSkillAuthority.OperatorSkill operator,
                                   ProductionFatiguePressureAuthority.FatiguePressure pressure,
                                   int turn, int inspectionRoll, long nonce) {
        int adjustment = (operator == null ? 0 : operator.defectRiskAdjust())
                + (pressure == null ? 0 : pressure.defectRiskAdd());
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
                defective
                        ? "Effect: ordinary traders apply a 40% resale penalty; item statistics remain unchanged."
                        : "Effect: passed inspection carries no batch appraisal penalty.",
                "Batch issue tags: " + ProductionBatchIssueAuthority.tagsFor(recipe, new BatchDisposition(id, risk, roll, state, List.of())) + "."));
    }
}
