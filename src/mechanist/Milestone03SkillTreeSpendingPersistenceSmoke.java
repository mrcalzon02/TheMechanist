package mechanist;

import java.util.HashSet;
import java.util.Properties;

/** Smoke for durable XP spending into skill nodes without touching knowledge unlocks. */
final class Milestone03SkillTreeSpendingPersistenceSmoke {
    public static void main(String[] args) {
        HashSet<String> unlocked = new HashSet<>();
        SkillTreeProgressionAuthority.SpendResult blocked = SkillTreeProgressionAuthority.spendXp(
                unlocked, 100, "fab-repair-material-eye");
        require(!blocked.success() && blocked.message().contains("Field Repair Discipline"),
                "dependent node should require prerequisite");

        SkillTreeProgressionAuthority.SpendResult unlockedFirst = SkillTreeProgressionAuthority.spendXp(
                unlocked, 70, "fab-repair-field-tech");
        require(unlockedFirst.success(), unlockedFirst.message());
        require(unlockedFirst.remainingXp() == 45, "spend should deduct XP");
        unlocked.add(unlockedFirst.unlockedNodeId());
        SkillTreeProgressionAuthority.SpendResult duplicate = SkillTreeProgressionAuthority.spendXp(
                unlocked, unlockedFirst.remainingXp(), "Field Repair Discipline");
        require(!duplicate.success() && duplicate.message().contains("already unlocked"),
                "duplicate unlock should be refused");

        SkillTreeProgressionAuthority.SpendResult second = SkillTreeProgressionAuthority.spendXp(
                unlocked, unlockedFirst.remainingXp(), "Material Eye");
        require(second.success() && second.remainingXp() == 5, "prerequisite should allow second spend");
        unlocked.add(second.unlockedNodeId());
        requireContains(SkillTreeProgressionAuthority.statusLines(second.remainingXp(), unlocked),
                "fab-repair-material-eye | unlocked", "status should show unlocked node");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.xp = 30;
        int knowledgesBefore = game.unlockedKnowledges.size();
        String result = GameplayConsoleCommandAuthority.execute(game, null, "skill_unlock",
                new String[]{"machine-safe-start"});
        require(result.contains("Unlocked skill node"), result);
        require(game.xp == 0, "console route should spend XP");
        require(game.unlockedSkillNodes.contains("machine-safe-start"), "console route should persist node in game state");
        require(game.unlockedKnowledges.size() == knowledgesBefore, "skill spending must not unlock knowledge");
        Properties props = new Properties();
        Persistence.writeCore(game, props);
        require(Persistence.decList(props.getProperty("run.skillNodes", "")).contains("machine-safe-start"),
                "save properties should include unlocked skill node");
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(java.util.List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03SkillTreeSpendingPersistenceSmoke() { }
}
