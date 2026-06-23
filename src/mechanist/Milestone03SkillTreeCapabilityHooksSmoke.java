package mechanist;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

/** Smoke for durable skill capability keys plus passive and active hooks. */
final class Milestone03SkillTreeCapabilityHooksSmoke {
    public static void main(String[] args) {
        HashSet<String> unlocked = new HashSet<>();
        unlocked.add("machine-safe-start");
        unlocked.add("fab-repair-field-tech");
        unlocked.add("fab-repair-material-eye");
        unlocked.add("trade-batch-appraisal");
        unlocked.add("trade-streetwise-appraisal");

        LinkedHashSet<String> keys = SkillTreeProgressionAuthority.capabilityKeys(unlocked);
        require(keys.contains("machine-safe-start"), "safe-start capability key should be exposed");
        require(keys.contains("fab-repair-material-eye"), "material-eye capability key should be exposed");
        require(keys.contains("trade-streetwise-appraisal"), "streetwise capability key should be exposed");
        require(SkillTreeProgressionAuthority.hasCapability(unlocked, "machine-safe-start"),
                "hasCapability should find unlocked key");
        require(!SkillTreeProgressionAuthority.hasCapability(unlocked, "trade-guilder-certification"),
                "hasCapability should not report locked sibling specialization");

        List<String> passive = SkillTreeProgressionAuthority.passiveBonusLines(unlocked);
        requireContains(passive, "production-limiter-readability:+1", "passive production limiter hook");

        List<String> active = SkillTreeProgressionAuthority.activeAbilityLines(unlocked);
        requireContains(active, "machine-readiness-preview", "active machine preview hook");
        requireContains(active, "street-market-risk-appraisal", "active street-market hook");

        requireContains(SkillTreeProgressionAuthority.statusLines(100, unlocked), "active machine-readiness-preview",
                "status should expose active ability hook");
        requireContains(SkillTreeProgressionAuthority.infopediaLines(), "capability keys",
                "Infopedia should explain capability hooks");
        require(SkillTreeProgressionAuthority.auditLine().contains("capabilityHooks=true"),
                "audit should expose capability hook contract");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private Milestone03SkillTreeCapabilityHooksSmoke() { }
}
