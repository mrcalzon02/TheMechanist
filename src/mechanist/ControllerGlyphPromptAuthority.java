package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Player-facing controller prompt readiness and glyph fallback authority. */
final class ControllerGlyphPromptAuthority {
    static final String VERSION = "0.9.10kd";

    private ControllerGlyphPromptAuthority() { }

    static GlyphPrompt promptFor(int controlsTab, InputAction action) {
        PromptMode mode = modeFor(controlsTab);
        String family = familyName(controlsTab);
        String textPrompt = mode == PromptMode.KEYBOARD_MOUSE_ONLY
                ? ControlReferenceTextSubsystem.keyboardPromptFor(action)
                : ControlReferenceTextSubsystem.controllerPromptFor(controlsTab, action);
        boolean glyphAssetReady = mode == PromptMode.PACKAGED_GLYPH_CONTROLLER;
        String glyphStatus = switch (mode) {
            case KEYBOARD_MOUSE_ONLY -> "Keyboard and mouse prompt mode active; controller prompts are hidden for this view.";
            case TEXT_CONTROLLER -> "Text prompt fallback active; controller glyph art is not packaged yet.";
            case PACKAGED_GLYPH_CONTROLLER -> "Controller glyph art packaged and ready.";
        };
        String recovery = mode == PromptMode.KEYBOARD_MOUSE_ONLY
                ? "Keyboard and mouse recovery prompts are the active control display."
                : "Keyboard and mouse prompt remains visible beside controller prompts.";
        return new GlyphPrompt(family, action, mode, textPrompt, glyphAssetReady, glyphStatus, recovery);
    }

    static PromptMode modeFor(int controlsTab) {
        if (controlsTab <= 0) return PromptMode.KEYBOARD_MOUSE_ONLY;
        return PromptMode.TEXT_CONTROLLER;
    }

    static String promptModeSummary(int controlsTab) {
        PromptMode mode = modeFor(controlsTab);
        return switch (mode) {
            case KEYBOARD_MOUSE_ONLY -> "Prompt mode: keyboard and mouse only; no controller glyphs needed.";
            case TEXT_CONTROLLER -> "Prompt mode: controller text fallback; packaged glyph art is not ready yet.";
            case PACKAGED_GLYPH_CONTROLLER -> "Prompt mode: packaged controller glyph art.";
        };
    }

    static List<String> auditLines(int controlsTab) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Controller prompt audit: " + familyName(controlsTab) + " uses " + modeFor(controlsTab).playerFacingName + ".");
        lines.add(promptModeSummary(controlsTab));
        lines.add("Fallback rule: never hide keyboard and mouse recovery prompts while controller glyphs are missing or unrecognized.");
        for (InputAction action : InputAction.values()) {
            lines.add(promptFor(controlsTab, action).playerFacingLine());
        }
        return List.copyOf(lines);
    }

    static String familyName(int controlsTab) {
        return switch (controlsTab) {
            case 0 -> "Keyboard and mouse";
            case 1 -> "Xbox";
            case 2 -> "PlayStation";
            case 3 -> "Steam Deck";
            case 4 -> "Generic controller";
            default -> "Controller";
        };
    }

    static String auditSummary() {
        return "controllerGlyphPromptAuthority version=" + VERSION
                + " modes=keyboardMouseOnly+textController+packagedGlyphController"
                + " activeControllerMode=textFallback glyphAssets=notPackaged textFallback=true keyboardMouseRecovery=true families=xbox+playstation+steam+generic";
    }

    enum PromptMode {
        KEYBOARD_MOUSE_ONLY("keyboard and mouse prompt mode"),
        TEXT_CONTROLLER("controller text fallback mode"),
        PACKAGED_GLYPH_CONTROLLER("controller glyph mode");

        final String playerFacingName;

        PromptMode(String playerFacingName) {
            this.playerFacingName = playerFacingName;
        }
    }

    record GlyphPrompt(String family, InputAction action, PromptMode mode, String textPrompt, boolean glyphAssetReady,
                       String glyphStatus, String recoveryNote) {
        String playerFacingLine() {
            String actionLabel = ControlReferenceTextSubsystem.inputActionLabel(action);
            String readiness = glyphAssetReady ? "Glyph ready" : (mode == PromptMode.KEYBOARD_MOUSE_ONLY ? "Keyboard and mouse" : "Text fallback");
            return PlayerFacingText.sanitize(actionLabel + " | " + family + " prompt: " + textPrompt
                    + " | Mode: " + mode.playerFacingName
                    + " | " + readiness + " | " + glyphStatus + " | " + recoveryNote);
        }
    }
}
