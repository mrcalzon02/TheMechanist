package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Central trace for the quality caps used by immediate manual production. */
final class ProductionQualityTraceAuthority {
    record QualityTrace(int outputTier, int doctrineTier, int recipeTier, int machineTier, int materialTier,
                        String outputQuality, String limiterLabel, List<String> lines) { }

    private ProductionQualityTraceAuthority() { }

    static QualityTrace evaluate(Set<String> unlockedKnowledge, String requiredKnowledge, String machineQuality) {
        return evaluate(unlockedKnowledge, requiredKnowledge, machineQuality, -1);
    }

    static QualityTrace evaluate(Set<String> unlockedKnowledge, String requiredKnowledge, String machineQuality, int materialTier) {
        int doctrineTier = QualityAuthorityApi.bestKnownTier(unlockedKnowledge);
        int recipeTier = QualityAuthorityApi.requiredTierForRecipeKnowledge(requiredKnowledge);
        int machineTier = QualityAuthorityApi.tierIndex(machineQuality);
        int outputTier = QualityAuthorityApi.cappedTier(
                doctrineTier, recipeTier, machineTier, materialTier, -1, -1);
        String outputQuality = QualityAuthorityApi.qualityName(outputTier);

        ArrayList<String> limiters = new ArrayList<>();
        if (doctrineTier == outputTier) limiters.add("known doctrine");
        if (recipeTier == outputTier) limiters.add("recipe pattern");
        if (machineTier == outputTier) limiters.add("machine ceiling");
        if (materialTier >= 0 && materialTier == outputTier) limiters.add("named input material");

        ArrayList<String> lines = new ArrayList<>();
        lines.add("Expected quality: " + outputQuality + ".");
        lines.add("Active quality caps: doctrine " + QualityAuthorityApi.qualityName(doctrineTier)
                + "; recipe " + QualityAuthorityApi.qualityName(recipeTier)
                + "; machine " + QualityAuthorityApi.qualityName(machineTier)
                + "; material " + (materialTier < 0 ? "open" : QualityAuthorityApi.qualityName(materialTier)) + ".");
        String limiterLabel = String.join(", ", limiters);
        lines.add("Main quality limiter: " + limiterLabel + ".");
        lines.add("Open quality hooks: facility and worker quality do not reduce this manual forecast until their ledgers are active.");
        return new QualityTrace(outputTier, doctrineTier, recipeTier, machineTier, materialTier,
                outputQuality, limiterLabel, List.copyOf(lines));
    }
}
