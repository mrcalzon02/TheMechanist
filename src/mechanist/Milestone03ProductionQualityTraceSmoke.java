package mechanist;

import java.util.List;
import java.util.Set;

/** Smoke for Milestone 03 production-quality cap enforcement and explanation. */
final class Milestone03ProductionQualityTraceSmoke {
    public static void main(String[] args) {
        ProductionQualityTraceAuthority.QualityTrace recipeLimited = ProductionQualityTraceAuthority.evaluate(
                Set.of("Fine Ballistics Patterns"), "Serviceable Ballistics Patterns", "Masterwork");
        require("Serviceable".equals(recipeLimited.outputQuality()), "recipe should cap otherwise better doctrine and machine quality");
        requireContains(recipeLimited.lines(), "recipe Serviceable", "recipe cap");
        requireContains(recipeLimited.lines(), "Main quality limiter: recipe pattern", "recipe limiter");

        ProductionQualityTraceAuthority.QualityTrace machineLimited = ProductionQualityTraceAuthority.evaluate(
                Set.of("Masterwork Tools Patterns"), "Masterwork Tools Patterns", "Common");
        require("Common".equals(machineLimited.outputQuality()), "machine should cap better doctrine and recipe quality");
        requireContains(machineLimited.lines(), "machine Common", "machine cap");
        requireContains(machineLimited.lines(), "worker quality does not reduce immediate manual Craft", "open hook honesty");

        ProductionQualityTraceAuthority.QualityTrace facilityLimited = ProductionQualityTraceAuthority.evaluate(
                Set.of("Masterwork Tools Patterns"), "Masterwork Tools Patterns", "Masterwork", -1, 2);
        require("Common".equals(facilityLimited.outputQuality()), "a Common facility should cap otherwise Masterwork production");
        requireContains(facilityLimited.lines(), "production facility", "facility limiter");

        for (String line : recipeLimited.lines()) rejectLeaks(line);
        for (String line : machineLimited.lines()) rejectLeaks(line);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Quality trace leaked implementation text: " + line);
    }

    private Milestone03ProductionQualityTraceSmoke() { }
}
