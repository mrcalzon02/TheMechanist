package mechanist;

import java.awt.Point;
import java.util.List;

/** Smoke for Milestone 02 movement planning refusal and route-summary language. */
final class Milestone02MovementPlanningReadabilitySmoke {
    public static void main(String[] args) {
        MovementPlanningAuthority.MovementPlanReadout selected = MovementPlanningAuthority.describePlan(
                "Walk", 4, 3, 2, List.of(new Point(2, 2), new Point(3, 2)), true, true, false);
        require(selected.reachable(), "selected route should be reachable");
        require(selected.exact(), "selected route should be exact");
        requireContains(selected.summary(), "Movement target selected", "selected route action");
        rejectLeaks(selected.summary(), "selected route");

        MovementPlanningAuthority.MovementPlanReadout partial = MovementPlanningAuthority.describePlan(
                "Run", 2, 5, 2, List.of(new Point(2, 2), new Point(3, 2)), true, true, false);
        require(partial.reachable(), "partial route should still be reachable");
        require(!partial.exact(), "partial route should not be exact");
        requireContains(partial.summary(), "Partial route", "partial route label");
        requireContains(partial.summary(), "Too far for one movement chain", "partial route distance reason");
        rejectLeaks(partial.summary(), "partial route");

        MovementPlanningAuthority.MovementPlanReadout occupied = MovementPlanningAuthority.describePlan(
                "Sprint", 7, 4, 4, List.of(), true, true, true);
        require(!occupied.reachable(), "occupied destination should be refused");
        requireContains(occupied.summary(), "Destination occupied", "occupied destination reason");
        rejectLeaks(occupied.summary(), "occupied destination");

        MovementPlanningAuthority.MovementPlanReadout blocked = MovementPlanningAuthority.describePlan(
                "Sneak", 1, 1, 1, List.of(), true, false, false);
        require(!blocked.reachable(), "blocked destination should be refused");
        requireContains(blocked.summary(), "Path blocked", "blocked destination reason");

        MovementPlanningAuthority.MovementPlanReadout outside = MovementPlanningAuthority.describePlan(
                "Walk", 1, -1, 8, List.of(), false, false, false);
        require(!outside.reachable(), "outside destination should be refused");
        requireContains(outside.summary(), "outside the current area", "outside destination reason");

        MovementPlanningAuthority.MovementPlanReadout unreachable = MovementPlanningAuthority.describePlan(
                "Walk", 1, 2, 2, List.of(), true, true, false);
        require(!unreachable.reachable(), "empty path should be refused");
        requireContains(unreachable.summary(), "Cannot reach from here", "unreachable destination reason");
        rejectContains(unreachable.summary(), "targetZoneKey", "movement denial should not expose raw route keys");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private static void rejectContains(String text, String forbidden, String label) {
        if (text != null && text.contains(forbidden)) throw new AssertionError(label + ": " + text);
    }

    private static void rejectLeaks(String text, String label) {
        if (PlayerFacingText.containsLikelyLeak(text)) {
            throw new AssertionError("Player-facing leak in " + label + ": " + text);
        }
    }

    private Milestone02MovementPlanningReadabilitySmoke() { }
}
