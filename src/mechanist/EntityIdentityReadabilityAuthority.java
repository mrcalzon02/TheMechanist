package mechanist;

import java.util.ArrayList;
import java.util.List;

final class EntityIdentityReadabilityAuthority {
    private EntityIdentityReadabilityAuthority() {}

    static List<String> summary(NpcEntity npc) {
        ArrayList<String> lines = new ArrayList<>();
        if (npc == null) {
            lines.add("No entity identity is available.");
            return lines;
        }
        if (npc.isAnimalActor()) {
            lines.add(npc.animalLine() + ".");
            lines.add("Identity confidence: visible animal type, behavior, and handler relationship only.");
            return lines;
        }

        npc.ensureRankIdentity(null);
        lines.add("Identity: " + safe(npc.name, "Unknown person") + ", " + safe(npc.role, "local resident") + ".");
        lines.add("Affiliation: " + (npc.faction == null ? Faction.NONE.label : npc.faction.label)
                + "; " + safe(npc.factionRankTitle, "faction member") + ".");
        lines.add("Authority: " + safe(npc.factionRankScope, "no formal command authority is apparent") + ".");
        lines.add("Current activity: " + safe(npc.state, "local routine") + ".");
        lines.add("Location: " + locationLine(npc) + ".");
        lines.add("Visible condition: " + conditionBand(npc.hp) + "; " + npc.ageLine() + ".");
        lines.add("Visible threat: " + threatBand(npc) + ".");
        if (npc.provenance == null) {
            lines.add("Background confidence: no reliable personnel history is known.");
        } else {
            lines.add("Known background: " + safe(npc.provenance.originRoom, "unrecorded room") + " in "
                    + safe(npc.provenance.originZone, "an unknown zone") + "; "
                    + safe(npc.provenance.populationPool, "local population") + ".");
            lines.add("Background confidence: recorded personnel provenance; private motives remain unknown.");
        }
        return lines;
    }

    static String conditionBand(int hp) {
        if (hp >= 1000) return "protected or non-combat service figure";
        if (hp <= 0) return "down or apparently dead";
        if (hp <= 3) return "critically injured";
        if (hp <= 6) return "badly injured";
        if (hp <= 9) return "wounded but mobile";
        return "appears physically capable";
    }

    private static String locationLine(NpcEntity npc) {
        int distance = Math.abs(npc.x - npc.homeX) + Math.abs(npc.y - npc.homeY);
        if (distance == 0) return "at their recorded home or duty position";
        if (distance <= 2) return "near their recorded home or duty position";
        return "away from their recorded home or duty position";
    }

    private static String threatBand(NpcEntity npc) {
        ArrayList<String> kit = new ArrayList<>();
        if (!blank(npc.equippedRangedWeapon)) kit.add(npc.equippedRangedWeapon);
        if (!blank(npc.equippedMeleeWeapon)) kit.add(npc.equippedMeleeWeapon);
        if (!blank(npc.equippedArmor)) kit.add(npc.equippedArmor);
        if (!blank(npc.equippedExplosive)) kit.add(npc.equippedExplosive);
        if (kit.isEmpty()) return "no obvious weapon or armor";
        String band = npc.equipmentTier >= 5 ? "heavily equipped" : npc.equipmentTier >= 3 ? "well equipped" : "armed";
        return band + " with " + String.join(", ", kit);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String safe(String value, String fallback) {
        return blank(value) ? fallback : value;
    }
}
