package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Central trace for the quality caps used by immediate manual production. */
final class ProductionQualityTraceAuthority {
    record QualityTrace(int outputTier, int doctrineTier, int recipeTier, int machineTier, int materialTier, int facilityTier, int toolTier,
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
        int doctrineTier = QualityAuthorityApi.bestKnownTier(unlockedKnowledge);
        int recipeTier = QualityAuthorityApi.requiredTierForRecipeKnowledge(requiredKnowledge);
        int machineTier = QualityAuthorityApi.tierIndex(machineQuality);
        int outputTier = QualityAuthorityApi.cappedTier(
                doctrineTier, recipeTier, machineTier, materialTier, facilityTier, -1);
        if (toolTier >= 0) outputTier = Math.min(outputTier, toolTier);
        String outputQuality = QualityAuthorityApi.qualityName(outputTier);

        ArrayList<String> limiters = new ArrayList<>();
        if (doctrineTier == outputTier) limiters.add("known doctrine");
        if (recipeTier == outputTier) limiters.add("recipe pattern");
        if (machineTier == outputTier) limiters.add("machine ceiling");
        if (materialTier >= 0 && materialTier == outputTier) limiters.add("named input material");
        if (facilityTier >= 0 && facilityTier == outputTier) limiters.add("production facility");
        if (toolTier >= 0 && toolTier == outputTier) limiters.add("equipped tool");

        ArrayList<String> lines = new ArrayList<>();
        lines.add("Expected quality: " + outputQuality + ".");
        lines.add("Active quality caps: doctrine " + QualityAuthorityApi.qualityName(doctrineTier)
                + "; recipe " + QualityAuthorityApi.qualityName(recipeTier)
                + "; machine " + QualityAuthorityApi.qualityName(machineTier)
                + "; material " + (materialTier < 0 ? "open" : QualityAuthorityApi.qualityName(materialTier))
                + "; facility " + (facilityTier < 0 ? "open" : QualityAuthorityApi.qualityName(facilityTier))
                + "; tool " + (toolTier < 0 ? "open" : QualityAuthorityApi.qualityName(toolTier)) + ".");
        String limiterLabel = String.join(", ", limiters);
        lines.add("Main quality limiter: " + limiterLabel + ".");
        lines.add("Open quality hook: worker quality does not reduce immediate manual Craft because the player is the operator.");
        return new QualityTrace(outputTier, doctrineTier, recipeTier, machineTier, materialTier, facilityTier, toolTier,
                outputQuality, limiterLabel, List.copyOf(lines));
    }
}
