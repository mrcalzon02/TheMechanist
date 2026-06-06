package mechanist;

import java.util.ArrayList;
import java.util.List;

/** Player-facing controller prompt readiness and glyph fallback authority. */
final class ControllerGlyphPromptAuthority {
    static final String VERSION = "0.9.10kc";

    private ControllerGlyphPromptAuthority() { }

    static GlyphPrompt promptFor(int controlsTab, InputAction action) {
        String family = familyName(controlsTab);
        String textPrompt = ControlReferenceTextSubsystem.controllerPromptFor(controlsTab, action);
        boolean glyphAssetReady = false;
        String glyphStatus = "Text prompt fallback active; controller glyph art is not packaged yet.";
        String recovery = "Keyboard and mouse prompt remains visible beside controller prompts.";
        return new GlyphPrompt(family, action, textPrompt, glyphAssetReady, glyphStatus, recovery);
    }

    static List<String> auditLines(int controlsTab) {
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Controller prompt audit: " + familyName(controlsTab) + " uses readable button names now and text fallback until packaged glyph art exists.");
        lines.add("Fallback rule: never hide keyboard and mouse recovery prompts while controller glyphs are missing or unrecognized.");
        for (InputAction action : InputAction.values()) {
            lines.add(promptFor(controlsTab, action).playerFacingLine());
        }
        return List.copyOf(lines);
    }

    static String familyName(int controlsTab) {
        return switch (controlsTab) {
            case 1 -> "Xbox";
            case 2 -> "PlayStation";
            case 3 -> "Steam Deck";
            case 4 -> "Generic controller";
            default -> "Controller";
        };
    }

    static String auditSummary() {
        return "controllerGlyphPromptAuthority version=" + VERSION + " glyphAssets=notPackaged textFallback=true keyboardMouseRecovery=true families=xbox+playstation+steam+generic";
    }

    record GlyphPrompt(String family, InputAction action, String textPrompt, boolean glyphAssetReady,
                       String glyphStatus, String recoveryNote) {
        String playerFacingLine() {
            String actionLabel = ControlReferenceTextSubsystem.inputActionLabel(action);
            String readiness = glyphAssetReady ? "Glyph ready" : "Text fallback";
            return PlayerFacingText.sanitize(actionLabel + " | " + family + " prompt: " + textPrompt
                    + " | " + readiness + " | " + glyphStatus + " | " + recoveryNote);
        }
    }
}
