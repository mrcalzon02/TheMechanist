package mechanist;

import java.awt.Point;
import java.util.List;

final class Milestone02InteractionApproachSmoke {
    public static void main(String[] args) {
        InteractionApproachAuthority.ApproachPlan missing = InteractionApproachAuthority.plan(null, 2, 2, "vendor");
        if (missing.available()) throw new AssertionError("Missing-world approach must not be available.");
        requireContains(missing.message(), "no world is loaded", "missing world guidance");

        InteractionApproachAuthority.ApproachPlan longPlan = new InteractionApproachAuthority.ApproachPlan(true, 4, 2,
                List.of(new Point(2, 3), new Point(3, 3), new Point(4, 3), new Point(4, 2)), "long");
        InteractionApproachAuthority.ApproachPlan plan = new InteractionApproachAuthority.ApproachPlan(true, 3, 3,
                List.of(new Point(2, 3), new Point(3, 3)), "Approach plan selected for machine at 3,3.");
        InteractionApproachAuthority.ApproachPlan selected = InteractionApproachAuthority.selectShortest(List.of(longPlan, plan));
        if (selected != plan) throw new AssertionError("Approach authority did not select the shortest reachable candidate.");
        if (Math.abs(selected.x() - 4) + Math.abs(selected.y() - 3) != 1) throw new AssertionError("Approach endpoint is not adjacent.");
        if (selected.path().isEmpty()) throw new AssertionError("Approach should provide a confirmation path.");
        requireContains(plan.message(), "Approach plan selected", "approach guidance");
        if (PlayerFacingText.containsLikelyLeak(plan.message())) throw new AssertionError("Approach guidance leaked implementation text.");
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.toLowerCase().contains(expected.toLowerCase())) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02InteractionApproachSmoke() {}
}
