package mechanist;

import java.util.HashSet;
import java.util.Map;

/** Smoke for mutually exclusive skill-tree specialization groups. */
final class Milestone03SkillTreeMutualExclusionSmoke {
    public static void main(String[] args) {
        HashSet<String> unlocked = new HashSet<>();
        unlocked.add("trade-batch-appraisal");

        SkillTreeProgressionAuthority.SpendResult street = SkillTreeProgressionAuthority.spendXp(
                unlocked, 100, "trade-streetwise-appraisal", SkillTreeProgressionAuthority.SkillAccessContext.open(),
                Map.of());
        require(street.success() && street.remainingXp() == 45, "streetwise specialization should unlock first");
        unlocked.add(street.unlockedNodeId());

        SkillTreeProgressionAuthority.SpendResult certifiedBlocked = SkillTreeProgressionAuthority.spendXp(
                unlocked, 100, "trade-guilder-certification",
                new SkillTreeProgressionAuthority.SkillAccessContext(java.util.Set.of(),
                        Map.of(Faction.MECHANIST_COLLEGIA, 30), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), false),
                Map.of());
        require(!certifiedBlocked.success() && certifiedBlocked.message().contains("Streetwise Appraisal is already selected"),
                "certified appraisal should be blocked by selected streetwise specialization");
        require(!certifiedBlocked.message().contains("faction"), "exclusivity should fail before faction gate when sibling is selected");

        HashSet<String> otherUnlocked = new HashSet<>();
        otherUnlocked.add("trade-batch-appraisal");
        SkillTreeProgressionAuthority.SpendResult certifiedAllowed = SkillTreeProgressionAuthority.spendXp(
                otherUnlocked, 100, "trade-guilder-certification",
                new SkillTreeProgressionAuthority.SkillAccessContext(java.util.Set.of(),
                        Map.of(Faction.MECHANIST_COLLEGIA, 30), java.util.Set.of(), java.util.Set.of(), java.util.Set.of(), false),
                Map.of());
        require(certifiedAllowed.success(), certifiedAllowed.message());

        requireContains(SkillTreeProgressionAuthority.statusLines(100, unlocked), "exclusive trade-appraisal-specialization",
                "status lines should expose specialization group");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(java.util.List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03SkillTreeMutualExclusionSmoke() { }
}
