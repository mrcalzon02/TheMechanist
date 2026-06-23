package mechanist;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Smoke for world access requirements on skill-node spending. */
final class Milestone03SkillTreeAccessGateSmoke {
    public static void main(String[] args) {
        HashSet<String> repairUnlocked = new HashSet<>();
        repairUnlocked.add("fab-repair-field-tech");
        repairUnlocked.add("fab-repair-material-eye");

        SkillTreeProgressionAuthority.SkillAccessContext noWorkshop =
                new SkillTreeProgressionAuthority.SkillAccessContext(Set.of(), Map.of(), Set.of(), Set.of(), Set.of(), false);
        SkillTreeProgressionAuthority.SpendResult blockedWorkshop = SkillTreeProgressionAuthority.spendXp(
                repairUnlocked, 100, "fab-repair-master-workshop", noWorkshop);
        require(!blockedWorkshop.success() && blockedWorkshop.message().contains("facility forge-fabrication-stall"),
                "advanced fabrication should require a workshop facility");

        SkillTreeProgressionAuthority.SkillAccessContext withWorkshop =
                new SkillTreeProgressionAuthority.SkillAccessContext(Set.of(), Map.of(),
                        Set.of("forge-fabrication-stall"), Set.of(), Set.of(), false);
        SkillTreeProgressionAuthority.SpendResult allowedWorkshop = SkillTreeProgressionAuthority.spendXp(
                repairUnlocked, 100, "Master Workshop Practice", withWorkshop, Map.of("Mechanics", 8));
        require(allowedWorkshop.success() && allowedWorkshop.remainingXp() == 40,
                "facility gate should allow advanced fabrication spend");

        HashSet<String> tradeUnlocked = new HashSet<>();
        tradeUnlocked.add("trade-batch-appraisal");
        SkillTreeProgressionAuthority.SkillAccessContext lowStanding =
                new SkillTreeProgressionAuthority.SkillAccessContext(Set.of(),
                        Map.of(Faction.MECHANIST_COLLEGIA, 10), Set.of(), Set.of(), Set.of(), false);
        SkillTreeProgressionAuthority.SpendResult blockedFaction = SkillTreeProgressionAuthority.spendXp(
                tradeUnlocked, 100, "trade-guilder-certification", lowStanding);
        require(!blockedFaction.success() && blockedFaction.message().contains("faction MECHANIST_COLLEGIA standing 20"),
                "certified appraisal should require faction standing");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.active = Candidate.random(new java.util.Random(31));
        game.active.stats.put("Mechanics", 8);
        game.xp = 100;
        game.unlockedSkillNodes.add("fab-repair-field-tech");
        game.unlockedSkillNodes.add("fab-repair-material-eye");
        String blockedConsole = GameplayConsoleCommandAuthority.execute(game, null, "skill_unlock",
                new String[]{"fab-repair-master-workshop"});
        require(blockedConsole.contains("Skill access missing"), blockedConsole);
        game.baseObjects.add(new BaseObject("Forge and fabrication stall", 'f', 1, 1, 0, 0));
        String allowedConsole = GameplayConsoleCommandAuthority.execute(game, null, "skill_unlock",
                new String[]{"fab-repair-master-workshop"});
        require(allowedConsole.contains("Unlocked skill node"), allowedConsole);
        require(game.unlockedSkillNodes.contains("fab-repair-master-workshop"),
                "console path should unlock gated node after facility access exists");
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03SkillTreeAccessGateSmoke() { }
}
