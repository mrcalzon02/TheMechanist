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
        requireContains(ready, "permanent base object", "placement consequence");

        List<String> blocked = ConstructionReadabilityAuthority.preview(recipe, 1, 0, 3, 4,
                "need 5 supplies, have 1", List.of("Sensor lens 0/1"));
        requireContains(blocked, "BLOCKED", "blocked status");
        requireContains(blocked, "need 5 supplies", "blocked reason");
        for (String line : ready) rejectLeaks(line);
        for (String line : blocked) rejectLeaks(line);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.toLowerCase().contains(expected.toLowerCase())) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Construction preview leaked implementation text: " + line);
    }

    private Milestone02ConstructionReadabilitySmoke() {}
}
