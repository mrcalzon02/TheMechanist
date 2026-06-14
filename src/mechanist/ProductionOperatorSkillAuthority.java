package mechanist;

import java.util.List;
import java.util.Locale;

/** Maps recipe skill labels onto the current core-stat scale for manual production risk. */
final class ProductionOperatorSkillAuthority {
    record OperatorSkill(String recipeSkill, String coreStat, int value, String band, int defectRiskAdjust) {
        List<String> lines() {
            String sign = defectRiskAdjust > 0 ? "+" : "";
            return List.of(
                    "Operator skill: " + recipeSkill + " uses " + coreStat + " " + value + " / " + band + ".",
                    "Operator defect adjustment: " + sign + defectRiskAdjust + " percentage points; operator skill does not change the quality cap yet.");
        }
    }

    private ProductionOperatorSkillAuthority() { }

    static OperatorSkill evaluate(GamePanel game, String recipeSkill) {
        String skill = recipeSkill == null || recipeSkill.isBlank() ? "Mechanics" : recipeSkill;
        String core = coreStatFor(skill);
        int value = game == null ? 6 : game.stat(core, 6);
        if (value <= 5) return new OperatorSkill(skill, core, value, "novice", 6);
        if (value <= 7) return new OperatorSkill(skill, core, value, "practiced", 3);
        if (value <= 9) return new OperatorSkill(skill, core, value, "skilled", 0);
        return new OperatorSkill(skill, core, value, "expert", -3);
    }

    static String coreStatFor(String recipeSkill) {
        String value = recipeSkill == null ? "" : recipeSkill.toLowerCase(Locale.ROOT);
        if (value.contains("firearm") || value.contains("ballistic")) return "Firearms";
        if (value.contains("melee")) return "Melee";
        if (value.contains("commerce") || value.contains("social")) return "Charm";
        if (value.contains("survival")) return "Endurance";
        if (value.contains("medical") || value.contains("medicine") || value.contains("knowledge") || value.contains("security")) return "Intellect";
        return "Mechanics";
    }
}
