package mechanist;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Maps readable NPC professions to temporary in-person skill-training access. */
final class SkillTrainerAuthority {
    private SkillTrainerAuthority() { }

    static Set<String> trainerTokens(NpcEntity npc) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        if (npc == null || npc.isAnimalActor()) return tokens;
        String profile = ((npc.name == null ? "" : npc.name) + " "
                + (npc.role == null ? "" : npc.role) + " "
                + (npc.state == null ? "" : npc.state)).toLowerCase(Locale.ROOT);
        if (containsAny(profile, "forge tutor", "artificer", "mechanic", "machinist", "engineer", "repair master")) {
            tokens.add("forge-tutor");
        }
        return tokens;
    }

    static boolean canTrain(NpcEntity npc) {
        return !trainerTokens(npc).isEmpty();
    }

    static String trainerLine(NpcEntity npc) {
        if (!canTrain(npc)) return "Training: no skill instruction is offered.";
        return "Training: this specialist can teach Forge-Tutored Repair while the lesson is open.";
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
