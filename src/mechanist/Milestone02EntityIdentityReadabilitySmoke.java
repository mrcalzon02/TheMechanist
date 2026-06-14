package mechanist;

import java.util.List;
import java.util.Random;

final class Milestone02EntityIdentityReadabilitySmoke {
    public static void main(String[] args) {
        NpcEntity npc = NpcEntity.create(Faction.ARBITES, ZoneType.NEUTRAL_CIVILIAN_FLOOR, 4, 5, new Random(17L));
        npc.name = "Marshal Venn";
        npc.role = "Precinct Warden";
        npc.state = "Patrol";
        npc.factionRankTitle = "Proctor";
        npc.factionRankScope = "commands a local patrol detail";
        npc.homeX = 1;
        npc.homeY = 1;
        npc.hp = 5;
        npc.equipmentTier = 4;
        npc.equippedRangedWeapon = "Combat shotgun";
        npc.equippedArmor = "Carapace coat";

        List<String> lines = EntityIdentityReadabilityAuthority.summary(npc);
        requireContains(lines, "Marshal Venn", "name");
        requireContains(lines, "Precinct Warden", "role");
        requireContains(lines, Faction.ARBITES.label, "faction label");
        requireContains(lines, "commands a local patrol detail", "rank scope");
        requireContains(lines, "away from", "location relationship");
        requireContains(lines, "badly injured", "condition approximation");
        requireContains(lines, "well equipped", "threat approximation");
        requireContains(lines, "private motives remain unknown", "knowledge boundary");
        for (String line : lines) {
            if (line.contains("HP ") || line.contains("numericId") || line.contains("ARBITES")) {
                throw new AssertionError("Identity summary exposed raw state: " + line);
            }
            if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Identity summary leaked implementation text: " + line);
        }
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone02EntityIdentityReadabilitySmoke() {}
}
