package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Central trace for the quality caps used by immediate manual production. */
final class ProductionQualityTraceAuthority {
    record QualityTrace(int outputTier, int doctrineTier, int recipeTier, int machineTier, int materialTier, int facilityTier, int toolTier, int operatorTier,
                        String outputQuality, String limiterLabel, List<String> lines) { }

    private ProductionQualityTraceAuthority() { }

    static QualityTrace evaluate(Set<String> unlockedKnowledge, String requiredKnowledge, String machineQuality) {
        return evaluate(unlockedKnowledge, requiredKnowledge, machineQuality, -1);
    }

    static QualityTrace evaluate(Set<String> unlockedKnowledge, String requiredKnowledge, String machineQuality, int materialTier) {
        return evaluate(unlockedKnowledge, requiredKnowledge, machineQuality, materialTier, -1);
    }

    static QualityTrace evaluate(Set<String> unlockedKnowledge, String requiredKnowledge, String machineQuality,
                                 int materialTier, int facilityTier) {
        return evaluate(unlockedKnowledge, requiredKnowledge, machineQuality, materialTier, facilityTier, -1);
    }

    static QualityTrace evaluate(Set<String> unlockedKnowledge, String requiredKnowledge, String machineQuality,
                                 int materialTier, int facilityTier, int toolTier) {
        return evaluate(unlockedKnowledge, requiredKnowledge, machineQuality, materialTier, facilityTier, toolTier, -1);
    }

    static QualityTrace evaluate(Set<String> unlockedKnowledge, String requiredKnowledge, String machineQuality,
                                 int materialTier, int facilityTier, int toolTier, int operatorTier) {
        int doctrineTier = QualityAuthorityApi.bestKnownTier(unlockedKnowledge);
        int recipeTier = QualityAuthorityApi.requiredTierForRecipeKnowledge(requiredKnowledge);
        int machineTier = QualityAuthorityApi.tierIndex(machineQuality);
        int outputTier = QualityAuthorityApi.cappedTier(
                doctrineTier, recipeTier, machineTier, materialTier, facilityTier, -1);
        if (toolTier >= 0) outputTier = Math.min(outputTier, toolTier);
        if (operatorTier >= 0) outputTier = Math.min(outputTier, operatorTier);
        String outputQuality = QualityAuthorityApi.qualityName(outputTier);

        ArrayList<String> limiters = new ArrayList<>();
        if (doctrineTier == outputTier) limiters.add("known doctrine");
        if (recipeTier == outputTier) limiters.add("recipe pattern");
        if (machineTier == outputTier) limiters.add("machine ceiling");
        if (materialTier >= 0 && materialTier == outputTier) limiters.add("named input material");
        if (facilityTier >= 0 && facilityTier == outputTier) limiters.add("production facility");
        if (toolTier >= 0 && toolTier == outputTier) limiters.add("equipped tool");
        if (operatorTier >= 0 && operatorTier == outputTier) limiters.add("manual operator skill");

        ArrayList<String> lines = new ArrayList<>();
        lines.add("Expected quality: " + outputQuality + ".");
        lines.add("Active quality caps: doctrine " + QualityAuthorityApi.qualityName(doctrineTier)
                + "; recipe " + QualityAuthorityApi.qualityName(recipeTier)
                + "; machine " + QualityAuthorityApi.qualityName(machineTier)
                + "; material " + (materialTier < 0 ? "open" : QualityAuthorityApi.qualityName(materialTier))
                + "; facility " + (facilityTier < 0 ? "open" : QualityAuthorityApi.qualityName(facilityTier))
                + "; tool " + (toolTier < 0 ? "open" : QualityAuthorityApi.qualityName(toolTier))
                + "; operator " + (operatorTier < 0 ? "open" : QualityAuthorityApi.qualityName(operatorTier)) + ".");
        String limiterLabel = String.join(", ", limiters);
        lines.add("Main quality limiter: " + limiterLabel + ".");
        lines.add("Open quality hook: worker quality does not reduce immediate manual Craft because the player is the operator.");
        return new QualityTrace(outputTier, doctrineTier, recipeTier, machineTier, materialTier, facilityTier, toolTier, operatorTier,
                outputQuality, limiterLabel, List.copyOf(lines));
    }

    static List<String> definitionAuditLines() {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Production quality definition audit: owner=ProductionQualityTraceAuthority, capInputs=doctrine+recipe+machine+material+facility+tool+manual operator, limiterOwner=ProductionQualityTraceAuthority, batchOwner=ProductionBatchAuthority, issueTagOwner=ProductionBatchIssueAuthority, provenanceOwner=ItemProvenanceRecord.");
        lines.add("Quality cap audit: doctrine comes from known recipe doctrine; recipe comes from required recipe pattern; machine comes from selected station quality; material comes from named consumed input units; facility comes from serviceable claimed-room stations; tool comes from equipped qualifying production tool; manual operator comes from the player-operated skill band.");
        lines.add("Quality boundary audit: worker quality does not cap immediate manual Craft; assigned-worker quality only becomes active when a staffed queued-production owner executes the run.");
        lines.add("Batch definition audit: one manual Craft action creates one batch ID and one inspection disposition for all units from that action.");
        lines.add("Batch consequence audit: defect appraisal can reduce ordinary resale value by 40%; item statistics, law enforcement, contamination effects, recalls, and counterfeit enforcement remain future owners.");
        lines.add("Batch issue tag audit: supported tags include good batch, defective batch, contaminated batch, unstable batch, restricted batch, counterfeit batch, stolen-risk batch, and faction-certified batch when inspection, recipe, law, or source metadata provides evidence.");
        lines.add("Provenance field audit: produced items preserve output quality, knowledge source/provider, machine quality/condition, quality limiter, operator skill/band, material quality, facility quality, tool quality, faction mutation, fatigue pressure, producing room/facility/machine/operator, workforce mode, legal status, production source, batch issue tags, and repair/modification history.");
        lines.add("Guard: Milestone03ProductionQualityDefinitionAuditSmoke checks cap inputs, owner boundaries, batch consequences, issue tags, provenance fields, and ordinary-effect limits.");
        return lines;
    }
}
