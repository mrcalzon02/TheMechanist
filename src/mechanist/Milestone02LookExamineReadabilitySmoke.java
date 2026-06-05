package mechanist;

/** Smoke for Milestone 02 Look/Examine readability and input prompt coverage. */
final class Milestone02LookExamineReadabilitySmoke {
    public static void main(String[] args) {
        String depth = ProgressiveLookAuthority.observationDepthLine(4);
        requireContains(depth, "close local examination complete", "full-depth examine copy");
        rejectLeaks(depth, "observation depth");

        String floor = ProgressiveLookAuthority.tileSurfaceLine(true, '.');
        requireContains(floor, "Floor", "floor surface label");
        requireContains(floor, "passable", "floor movement word");
        rejectContains(floor, "glyph", "floor surface should not mention glyphs");
        rejectContains(floor, "walkable", "floor surface should not expose walkable flag");

        String barrier = ProgressiveLookAuthority.tileSurfaceLine(false, '#');
        requireContains(barrier, "Barrier", "barrier surface label");
        requireContains(barrier, "blocked", "barrier movement word");
        rejectLeaks(barrier, "barrier surface");

        String route = PlayerFacingText.inspectionRoute(
                "targetZoneKey=1,2,3,4,5,false",
                "path=C:\\tmp\\mechanist\\route.map registryKey=debug.route"
        );
        rejectLeaks(route, "route inspection");
        rejectContains(route, "targetZoneKey", "route inspection should hide raw route keys");

        String examine = PlayerFacingText.actionExamine(
                "Examine",
                "className=mechanist.runtime.Scanner uuid=123e4567-e89b-12d3-a456-426614174000"
        );
        rejectLeaks(examine, "examine action");
        requireContains(examine, "Examine", "examine action label");

        for (InputAction action : InputAction.values()) {
            String label = ControlReferenceTextSubsystem.inputActionLabel(action);
            String context = ControlReferenceTextSubsystem.inputActionContext(action);
            String keyboard = ControlReferenceTextSubsystem.keyboardPromptFor(action);
            String controller = ControlReferenceTextSubsystem.genericPromptFor(action);
            if (label == null || label.isBlank() || label.equals(action.name())) {
                throw new AssertionError("Missing player-facing input action label for " + action);
            }
            requireContains(context, "", "input context " + action);
            requireContains(keyboard, "Keyboard:", "keyboard prompt " + action);
            requireContains(controller, "Generic:", "controller prompt " + action);
            rejectLeaks(label + " " + context + " " + keyboard + " " + controller, "input prompt " + action);
        }
    }

    private static void requireContains(String text, String expected, String label) {
        if (expected.isEmpty()) {
            if (text == null || text.isBlank()) throw new AssertionError("Blank " + label);
            return;
        }
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private static void rejectContains(String text, String forbidden, String label) {
        if (text != null && text.contains(forbidden)) {
            throw new AssertionError(label + ": " + text);
        }
    }

    private static void rejectLeaks(String text, String label) {
        if (PlayerFacingText.containsLikelyLeak(text)) {
            throw new AssertionError("Player-facing leak in " + label + ": " + text);
        }
    }

    private Milestone02LookExamineReadabilitySmoke() { }
}
