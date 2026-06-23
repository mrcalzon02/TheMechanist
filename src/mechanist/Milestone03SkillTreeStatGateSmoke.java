package mechanist;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;

/** Smoke for skill-tree stat prerequisites and bounded stat effects. */
final class Milestone03SkillTreeStatGateSmoke {
    public static void main(String[] args) {
        HashSet<String> unlocked = new HashSet<>();
        unlocked.add("machine-safe-start");

        SkillTreeProgressionAuthority.SpendResult blocked = SkillTreeProgressionAuthority.spendXp(
                unlocked, 100, "machine-pressure-discipline", SkillTreeProgressionAuthority.SkillAccessContext.open(),
                Map.of("Nerve", 6));
        require(!blocked.success() && blocked.message().contains("Nerve 6/7"),
                "pressure discipline should require Nerve 7");

        SkillTreeProgressionAuthority.SpendResult allowed = SkillTreeProgressionAuthority.spendXp(
                unlocked, 100, "machine-pressure-discipline", SkillTreeProgressionAuthority.SkillAccessContext.open(),
                Map.of("Nerve", 7));
        require(allowed.success() && "Nerve:+1".equals(allowed.statEffect()) && allowed.remainingXp() == 55,
                "stat-qualified spend should expose bounded Nerve effect");

        Candidate candidate = Candidate.random(new java.util.Random(42));
        candidate.stats.put("Nerve", 7);
        require(SkillTreeProgressionAuthority.applyStatEffect(candidate, allowed.statEffect()),
                "stat effect should mutate candidate");
        require(candidate.stats.get("Nerve") == 8, "Nerve should increase by one");

        GamePanel game = new GamePanel();
        if (game.timer != null) game.timer.stop();
        game.active = candidate;
        game.xp = 100;
        game.unlockedSkillNodes.add("machine-safe-start");
        String result = GameplayConsoleCommandAuthority.execute(game, null, "skill_unlock",
                new String[]{"machine-pressure-discipline"});
        require(result.contains("Stat effect applied: Nerve:+1"), result);
        require(game.active.stats.get("Nerve") == 9, "console route should apply stat effect");
        Properties props = new Properties();
        Persistence.writeCore(game, props);
        require(Persistence.decList(props.getProperty("char.stats", "")).stream().anyMatch(line -> line.equals("Nerve:9")),
                "candidate stat effect should persist through char.stats");
        if (game.timer != null) game.timer.stop();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone03SkillTreeStatGateSmoke() { }
}
