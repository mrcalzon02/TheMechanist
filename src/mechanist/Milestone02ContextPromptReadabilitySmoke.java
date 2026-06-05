package mechanist;

import java.util.ArrayList;

/** Smoke for Milestone 02 context-sensitive prompt wording. */
final class Milestone02ContextPromptReadabilitySmoke {
    public static void main(String[] args) {
        ArrayList<ControlReferenceTextSubsystem.ContextPrompt> prompts = ControlReferenceTextSubsystem.defaultContextPrompts();
        if (prompts.size() < 8) {
            throw new AssertionError("Expected default context prompt coverage for major panels.");
        }

        for (int tab = 0; tab <= 4; tab++) {
            ArrayList<String> lines = ControlReferenceTextSubsystem.contextPromptLines(tab, prompts);
            if (lines.size() != prompts.size()) {
                throw new AssertionError("Context prompt line count mismatch for controls tab " + tab);
            }
            for (String line : lines) {
                requireContains(line, ":", "context separator");
                requireContains(line, "[", "binding bracket");
                rejectLeaks(line, "context prompt tab " + tab);
                rejectContains(line, "VK_", "context prompt should not expose key constants");
                rejectContains(line, "InputAction", "context prompt should not expose enum type");
            }
        }

        String look = ControlReferenceTextSubsystem.contextPromptLine(
                "Look", 0, InputAction.EXAMINE, InputAction.CANCEL,
                "className=mechanist.runtime.Scanner uuid=123e4567-e89b-12d3-a456-426614174000"
        );
        requireContains(look, "Examine visible target", "look primary action");
        requireContains(look, "Keyboard: E while looking", "look keyboard prompt");
        rejectLeaks(look, "look context prompt");

        String controller = ControlReferenceTextSubsystem.contextPromptLine(
                "Movement planning", 2, InputAction.CONFIRM, InputAction.CANCEL,
                "Confirm the ghost target or cancel safely."
        );
        requireContains(controller, "PlayStation:", "controller prompt fallback");
        requireContains(controller, "Keyboard:", "keyboard fallback inside controller prompt");
        requireContains(controller, "Cancel / back", "safe recovery action");
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

    private Milestone02ContextPromptReadabilitySmoke() { }
}
