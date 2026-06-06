package mechanist;

import java.util.List;

/** Smoke for controller prompt glyph fallback and keyboard/mouse recovery wording. */
final class Milestone02ControllerGlyphPromptSmoke {
    public static void main(String[] args) {
        ControllerGlyphPromptAuthority.GlyphPrompt xboxConfirm = ControllerGlyphPromptAuthority.promptFor(1, InputAction.CONFIRM);
        requireContains(xboxConfirm.playerFacingLine(), "Xbox prompt: Xbox: A", "Xbox confirm prompt");
        requireContains(xboxConfirm.playerFacingLine(), "Text fallback", "Xbox text fallback");
        requireContains(xboxConfirm.playerFacingLine(), "Keyboard and mouse prompt remains visible", "Xbox recovery wording");

        ControllerGlyphPromptAuthority.GlyphPrompt playstationCancel = ControllerGlyphPromptAuthority.promptFor(2, InputAction.CANCEL);
        requireContains(playstationCancel.playerFacingLine(), "PlayStation prompt: PlayStation: Circle", "PlayStation cancel prompt");

        ControllerGlyphPromptAuthority.GlyphPrompt genericPlanMove = ControllerGlyphPromptAuthority.promptFor(4, InputAction.PLAN_MOVE);
        requireContains(genericPlanMove.playerFacingLine(), "Generic controller prompt", "Generic family prompt");
        requireContains(genericPlanMove.playerFacingLine(), "right stick down", "Generic plan move prompt");

        List<String> audit = ControllerGlyphPromptAuthority.auditLines(4);
        require(audit.size() >= InputAction.values().length, "glyph audit should cover input actions");
        requireContains(audit.get(0), "text fallback", "audit summary fallback");
        requireContains(ControllerGlyphPromptAuthority.auditSummary(), "glyphAssets=notPackaged", "glyph audit summary");
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
