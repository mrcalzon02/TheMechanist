package mechanist;

import mechanist.input.InputDevice;
import mechanist.input.InputToken;
import mechanist.input.KeyBindingManager;

import java.awt.event.KeyEvent;
import java.util.List;

/** Smoke for Milestone 02 input rebinding audit/readiness wording. */
final class Milestone02InputRebindingAuditSmoke {
    public static void main(String[] args) {
        KeyBindingManager manager = KeyBindingManager.getInstance();
        manager.resetAllToDefaults();
        List<InputRebindingAuditAuthority.ActionAudit> rows = InputRebindingAuditAuthority.auditRows(4);
        if (rows.size() != InputAction.values().length) {
            throw new AssertionError("Input audit should cover every action.");
        }

        List<String> lines = InputRebindingAuditAuthority.playerFacingAuditLines(4);
        requireContains(lines, "Input audit covers", "input audit summary");
        requireContains(lines, "Required recovery action", "required recovery marker");
        requireContains(lines, "Conflict:", "conflict notes");
        requireContains(lines, "Reset to defaults", "reset recovery rule");
        requireContains(lines, "Last-good behavior", "last-good recovery rule");
        requireContains(lines, "Move / nudge up", "movement action row");
        requireContains(lines, "Cancel / back", "cancel action row");
        requireContains(lines, "Keyboard:", "keyboard fallback");
        requireContains(lines, "Generic:", "controller prompt");
        requireContains(lines, "Baseline default:", "baseline default label");
        requireContains(lines, "Current keyboard:", "current keyboard label");

        manager.rebind(InputDevice.KEYBOARD, "confirm", InputToken.keyboard(KeyEvent.VK_U, 0),
                KeyBindingManager.DuplicatePolicy.REJECT);
        List<String> rebound = InputRebindingAuditAuthority.playerFacingAuditLines(4);
        requireContains(rebound, "Baseline default: Keyboard: Enter / Space", "confirm baseline after rebind");
        requireContains(rebound, "Current keyboard: Keyboard: U", "live confirm binding after rebind");
        manager.resetAllToDefaults();

        for (String line : lines) {
            rejectLeaks(line, "input audit line");
            rejectContains(line, "InputAction", "input audit should not expose enum type");
            rejectContains(line, "MOVE_UP", "input audit should not expose enum constants");
            rejectContains(line, "VK_", "input audit should not expose key constants");
            rejectContains(line, "action=", "input audit should not expose compact field names");
        }

        List<String> entryRows = SemanticAssetInfopediaAuthority.mechanicEntryRows("rebinding");
        requireContains(entryRows, "Input Rebinding", "input rebinding mechanic row");
        List<String> detail = SemanticAssetInfopediaAuthority.mechanicDetailLinesByKey("input-rebinding");
        requireContains(detail, "Controls screen groups actions by context", "input rebinding controls screen explanation");
        requireContains(detail, "Required recovery actions keep a usable default path", "input rebinding recovery explanation");
        requireContains(detail, "Conflict notes explain", "input rebinding conflict explanation");
        for (String line : detail) {
            rejectLeaks(line, "input rebinding infopedia detail");
            rejectProcessLanguage(line, "input rebinding detail");
        }

        List<String> related = SemanticAssetInfopediaAuthority.relatedRowsForEntry(null, "MECHANIC - Input Rebinding [Controls]", null);
        requireContains(related, "Context Prompts", "input audit related context prompts");
        manager.resetAllToDefaults();
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) {
            if (line != null && line.contains(expected)) return;
        }
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void rejectContains(String text, String forbidden, String label) {
        if (text != null && text.contains(forbidden)) throw new AssertionError(label + ": " + text);
    }

    private static void rejectProcessLanguage(String text, String label) {
        if (text == null) return;
        String l = text.toLowerCase();
        String[] forbidden = {"guard", "smoke", "milestone", "authority", "audit", "future", "owner=", "raw-id", "raw id"};
        for (String token : forbidden) {
            if (l.contains(token)) throw new AssertionError(label + " contains process language '" + token + "': " + text);
        }
    }

    private static void rejectLeaks(String text, String label) {
        if (PlayerFacingText.containsLikelyLeak(text)) {
            throw new AssertionError("Player-facing leak in " + label + ": " + text);
        }
    }

    private Milestone02InputRebindingAuditSmoke() { }
}
