package mechanist;

import java.util.List;

/** Smoke for controller prompt glyph fallback, prompt modes, and keyboard/mouse recovery wording. */
final class Milestone02ControllerGlyphPromptSmoke {
    public static void main(String[] args) {
        ControllerGlyphPromptAuthority.GlyphPrompt keyboardConfirm = ControllerGlyphPromptAuthority.promptFor(0, InputAction.CONFIRM);
        require(keyboardConfirm.mode() == ControllerGlyphPromptAuthority.PromptMode.KEYBOARD_MOUSE_ONLY, "keyboard tab should use keyboard/mouse mode");
        requireContains(keyboardConfirm.playerFacingLine(), "Keyboard and mouse prompt mode active", "keyboard mode line");
        requireContains(ControllerGlyphPromptAuthority.promptModeSummary(0), "keyboard and mouse only", "keyboard mode summary");

        ControllerGlyphPromptAuthority.GlyphPrompt xboxConfirm = ControllerGlyphPromptAuthority.promptFor(1, InputAction.CONFIRM);
        require(xboxConfirm.mode() == ControllerGlyphPromptAuthority.PromptMode.TEXT_CONTROLLER, "Xbox tab should use text-controller fallback mode");
        requireContains(xboxConfirm.playerFacingLine(), "Xbox prompt: Xbox: A", "Xbox confirm prompt");
        requireContains(xboxConfirm.playerFacingLine(), "Text fallback", "Xbox text fallback");
        requireContains(xboxConfirm.playerFacingLine(), "Keyboard and mouse prompt remains visible", "Xbox recovery wording");

        ControllerGlyphPromptAuthority.GlyphPrompt playstationCancel = ControllerGlyphPromptAuthority.promptFor(2, InputAction.CANCEL);
        requireContains(playstationCancel.playerFacingLine(), "PlayStation prompt: PlayStation: Circle", "PlayStation cancel prompt");

        ControllerGlyphPromptAuthority.GlyphPrompt genericPlanMove = ControllerGlyphPromptAuthority.promptFor(4, InputAction.PLAN_MOVE);
        requireContains(genericPlanMove.playerFacingLine(), "Generic controller prompt", "Generic family prompt");
        requireContains(genericPlanMove.playerFacingLine(), "right stick down", "Generic plan move prompt");
        requireContains(ControllerGlyphPromptAuthority.promptModeSummary(4), "controller text fallback", "controller mode summary");

        List<String> audit = ControllerGlyphPromptAuthority.auditLines(4);
        require(audit.size() >= InputAction.values().length, "glyph audit should cover input actions");
        requireContains(audit.get(0), "controller text fallback mode", "audit summary fallback");
        requireContains(audit.get(1), "Prompt mode", "audit prompt mode line");
        requireContains(ControllerGlyphPromptAuthority.auditSummary(), "modes=keyboardMouseOnly+textController+packagedGlyphController", "glyph audit summary modes");
        requireContains(ControllerGlyphPromptAuthority.auditSummary(), "glyphAssets=notPackaged", "glyph audit summary assets");
        requireContains(InputRebindingAuditAuthority.infopediaLines().toString(), "controller glyph art", "input audit should mention glyph limitation");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) {
            throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
        }
    }

    private Milestone02ControllerGlyphPromptSmoke() { }
}
