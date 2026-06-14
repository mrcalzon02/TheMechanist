package mechanist;

import java.util.List;
import java.util.Random;

final class Milestone02BodyConditionReadabilitySmoke {
    public static void main(String[] args) {
        Candidate candidate = Candidate.random(new Random(42L));
        BodyPart arm = candidate.body.get("L Lower Arm");
        arm.health = arm.maxHealth() * 0.25;
        List<String> injured = BodyConditionReadabilityAuthority.summary(candidate, 5, 3, 6, 5,
                52, 20, 44, 30, Clothing.arbitesCoat(true), "Stub pistol", "Knife");
        requireContains(injured, "badly injured", "overall injury band");
        requireContains(injured, "unfit for sustained combat", "combat readiness");
        requireContains(injured, "ongoing bleeding", "bleeding band");
        requireContains(injured, "infection risk", "medical risk");
        requireContains(injured, "L Lower Arm critically damaged", "affected region");
        requireContains(injured, "damaged", "damaged protection");

        Candidate healthy = Candidate.random(new Random(84L));
        List<String> ready = BodyConditionReadabilityAuthority.summary(healthy, 0, 0, 0, 0,
                0, 0, 100, 100, Clothing.scavengerRags(), null, null);
        requireContains(ready, "healthy and physically intact", "healthy band");
        requireContains(ready, "Combat readiness: ready", "ready status");
        requireContains(ready, "all tracked regions appear intact", "intact body plan");

        for (String line : injured) rejectRawStats(line);
        for (String line : ready) rejectRawStats(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectRawStats(String line) {
        if (line.contains("END ") || line.contains("AGI ") || line.contains("HP ")) {
            throw new AssertionError("Body summary exposed raw body stats: " + line);
        }
        if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Body summary leaked implementation text: " + line);
    }

    private Milestone02BodyConditionReadabilitySmoke() {}
}
