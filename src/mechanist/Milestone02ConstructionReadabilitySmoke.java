package mechanist;

import java.util.List;

final class Milestone02ConstructionReadabilitySmoke {
    public static void main(String[] args) {
        BuildRecipe recipe = BuildRecipe.securitySensorMast();
        List<String> ready = ConstructionReadabilityAuthority.preview(recipe, 8, 5, 12, 7, "ok",
                List.of("Sensor lens 1/1", "Circuit wafer 2/1"));
        requireContains(ready, "READY at 12,7", "placement readiness");
        requireContains(ready, "supplies 8/5", "supply forecast");
        requireContains(ready, "Sensor lens 1/1", "component forecast");
        requireContains(ready, "Scrap Workbench", "workbench requirement");
        requireContains(ready, "Effort preview", "effort preview");
        requireContains(ready, "labor turns", "labor turn forecast");
        requireContains(ready, "Mechanics", "mechanics forecast");
        requireContains(ready, "Attention preview: heat", "attention preview");
        requireContains(ready, "suspicion", "suspicion preview");
        requireContains(ready, "armed defenses", "attention driver");
        requireContains(ready, "owning the blueprint is separate from permission", "blueprint ownership boundary");
        requireContains(ready, "permanent base object", "placement consequence");
        List<String> staged = ConstructionReadabilityAuthority.preview(recipe, 1, 0, 3, 4,
                "staged start: 1 material unit(s) available now; missing materials can be added later with Work.",
                List.of("Sensor lens 0/1"));
        requireContains(staged, "STAGED START", "staged-start status");
        requireContains(staged, "missing materials can be added later", "staged-start explanation");
        String stagedStartSummary = ConstructionReadabilityAuthority.startSummary(recipe, 3, 4, true);
        requireContains(stagedStartSummary, "Construction summary: Security Sensor Mast at 3,4", "start summary location");
        requireContains(stagedStartSummary, "partial materials staged", "staged materials summary");
        requireContains(stagedStartSummary, "labor 10 turns", "start labor summary");
        requireContains(stagedStartSummary, "mishap risk 42%", "start risk summary");
        requireContains(stagedStartSummary, "heat", "start heat summary");
        requireContains(stagedStartSummary, "suspicion", "start suspicion summary");

        List<String> blocked = ConstructionReadabilityAuthority.preview(recipe, 1, 0, 3, 4,
                "need 5 supplies, have 1", List.of("Sensor lens 0/1"));
        requireContains(blocked, "BLOCKED", "blocked status");
        requireContains(blocked, "need 5 supplies", "blocked reason");
        requireContains(blocked, "Next step: Construction unavailable", "blocked guidance prefix");
        requireContains(blocked, "Gather the listed materials", "blocked guidance path");
        for (String line : ready) rejectLeaks(line);
        for (String line : staged) rejectLeaks(line);
        for (String line : blocked) rejectLeaks(line);
        rejectLeaks(stagedStartSummary);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void requireContains(String line, String expected, String label) {
        if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + line);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Construction preview leaked implementation text: " + line);
    }

    private Milestone02ConstructionReadabilitySmoke() {}
}
