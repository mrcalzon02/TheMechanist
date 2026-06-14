package mechanist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class BodyConditionReadabilityAuthority {
    private BodyConditionReadabilityAuthority() {}

    static List<String> summary(Candidate candidate, int wounds, int bleeding, int infectionRisk, int pain,
                                int fatigue, int sleepNeed, int food, int water, Clothing clothing,
                                String leftHand, String rightHand) {
        ArrayList<String> lines = new ArrayList<>();
        if (candidate == null) {
            lines.add("No active character condition is available.");
            return lines;
        }

        ArrayList<BodyPart> parts = new ArrayList<>(candidate.body.values());
        parts.sort(Comparator.comparingDouble(BodyConditionReadabilityAuthority::healthRatio));
        long disabled = parts.stream().filter(BodyPart::destroyed).count();
        double average = parts.isEmpty() ? 1.0 : parts.stream().mapToDouble(BodyConditionReadabilityAuthority::healthRatio).average().orElse(1.0);

        lines.add("Overall condition: " + overallBand(average, disabled, wounds) + ".");
        lines.add("Combat readiness: " + readinessBand(disabled, wounds, bleeding, pain, fatigue, sleepNeed, food, water) + ".");
        lines.add("Trauma: " + traumaBand(wounds, bleeding, pain) + ".");
        lines.add("Medical risk: " + medicalRiskBand(bleeding, infectionRisk) + ".");
        lines.add("Stamina: " + staminaBand(fatigue, sleepNeed, food, water) + ".");

        if (parts.isEmpty()) {
            lines.add("Body plan: no tracked regions.");
        } else if (healthRatio(parts.get(0)) >= 0.995) {
            lines.add("Body plan: all tracked regions appear intact.");
        } else {
            int shown = Math.min(3, parts.size());
            ArrayList<String> affected = new ArrayList<>();
            for (int i = 0; i < shown && healthRatio(parts.get(i)) < 0.995; i++) {
                BodyPart part = parts.get(i);
                affected.add(part.name + " " + regionBand(healthRatio(part)));
            }
            lines.add("Affected regions: " + String.join("; ", affected) + ".");
        }

        lines.add("Protection: " + protectionBand(clothing) + ".");
        lines.add("Hands: left " + safe(leftHand, "empty") + "; right " + safe(rightHand, "empty") + ".");
        return lines;
    }

    private static double healthRatio(BodyPart part) {
        return part == null ? 0.0 : part.currentHealth() / part.maxHealth();
    }

    private static String overallBand(double average, long disabled, int wounds) {
        if (disabled > 0 || average < 0.35 || wounds >= 8) return "critical injuries; immediate treatment strongly advised";
        if (average < 0.60 || wounds >= 5) return "badly injured and operationally compromised";
        if (average < 0.85 || wounds >= 2) return "injured but mobile";
        if (wounds > 0 || average < 0.995) return "lightly injured";
        return "healthy and physically intact";
    }

    private static String readinessBand(long disabled, int wounds, int bleeding, int pain, int fatigue,
                                        int sleepNeed, int food, int water) {
        int burden = wounds * 2 + bleeding * 2 + pain + fatigue / 12 + sleepNeed / 12;
        if (food <= 15 || water <= 15) burden += 4;
        if (disabled > 0 || burden >= 18) return "unfit for sustained combat";
        if (burden >= 10) return "severely impaired";
        if (burden >= 5) return "impaired; avoid prolonged fighting";
        if (burden > 0) return "serviceable with minor impairment";
        return "ready";
    }

    private static String traumaBand(int wounds, int bleeding, int pain) {
        if (wounds <= 0 && bleeding <= 0 && pain <= 0) return "no active wound, bleeding, or pain burden";
        String bleed = bleeding <= 0 ? "no active bleeding" : bleeding >= 5 ? "heavy bleeding" : bleeding >= 2 ? "ongoing bleeding" : "minor bleeding";
        String painBand = pain <= 0 ? "little reported pain" : pain >= 7 ? "severe pain" : pain >= 3 ? "significant pain" : "mild pain";
        String woundBand = wounds >= 8 ? "critical wound burden" : wounds >= 5 ? "major wound burden" : wounds >= 2 ? "moderate wound burden" : "minor wound burden";
        return woundBand + ", " + bleed + ", " + painBand;
    }

    private static String medicalRiskBand(int bleeding, int infectionRisk) {
        if (bleeding >= 5) return "urgent bleeding control required";
        if (infectionRisk >= 7) return "high infection risk; seek proper treatment";
        if (bleeding > 0 || infectionRisk >= 3) return "active bleeding or infection risk; treatment recommended before the condition worsens";
        if (infectionRisk > 0) return "low but present infection risk";
        return "no immediate bleeding or infection warning";
    }

    private static String staminaBand(int fatigue, int sleepNeed, int food, int water) {
        if (water <= 15) return "dangerously dehydrated";
        if (food <= 15) return "dangerously hungry";
        if (fatigue >= 75 || sleepNeed >= 75) return "exhausted";
        if (fatigue >= 45 || sleepNeed >= 45) return "tired; rest soon";
        if (food <= 35 || water <= 35) return "supply-starved and losing endurance";
        if (fatigue >= 20 || sleepNeed >= 20) return "slightly tired";
        return "rested and supplied";
    }

    private static String regionBand(double ratio) {
        if (ratio <= 0.0) return "disabled";
        if (ratio < 0.35) return "critically damaged";
        if (ratio < 0.65) return "badly injured";
        if (ratio < 0.90) return "injured";
        return "lightly hurt";
    }

    private static String protectionBand(Clothing clothing) {
        if (clothing == null) return "no protective clothing equipped";
        String protection = clothing.defense >= 4 ? "strong protection" : clothing.defense >= 2 ? "modest protection" : "minimal protection";
        return clothing.name + " provides " + protection + (clothing.damaged ? " but is damaged" : "");
    }

    private static String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
