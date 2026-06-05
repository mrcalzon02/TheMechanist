package mechanist;

import java.util.List;

/** Smoke for Milestone 02 mechanic-reference entries in the Infopedia bridge. */
final class Milestone02InfopediaMechanicsReadabilitySmoke {
    public static void main(String[] args) {
        List<String> rows = SemanticAssetInfopediaAuthority.mechanicEntryRows("");
        requireContains(rows, "Look and Examine", "Look/Examine mechanic row");
        requireContains(rows, "Movement Planning", "movement mechanic row");
        requireContains(rows, "Context Prompts", "context prompt mechanic row");

        List<String> filtered = SemanticAssetInfopediaAuthority.mechanicEntryRows("ghost");
        requireContains(filtered, "Movement Planning", "movement filter row");

        checkDetail("look-examine", "Examine the selected visible target", "Milestone02LookExamineReadabilitySmoke");
        checkDetail("movement-planning", "Movement target selected", "Milestone02MovementPlanningReadabilitySmoke");
        checkDetail("context-prompts", "Generic:", "Milestone02ContextPromptReadabilitySmoke");

        List<String> detailFromRow = SemanticAssetInfopediaAuthority.detailLines(null, rows.get(0), null, "");
        requireContains(detailFromRow, "Reference:", "mechanic detail reference");
        for (String line : detailFromRow) rejectLeaks(line, "mechanic detail from row");
    }

    private static void checkDetail(String key, String expected, String guardName) {
        List<String> lines = SemanticAssetInfopediaAuthority.mechanicDetailLinesByKey(key);
        requireContains(lines, expected, key + " detail expected text");
        requireContains(lines, guardName, key + " smoke guard name");
        for (String line : lines) {
            rejectLeaks(line, key + " detail");
            rejectContains(line, "targetZoneKey", key + " raw route key");
            rejectContains(line, "className", key + " raw class key");
        }
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

    private static void rejectLeaks(String text, String label) {
        if (PlayerFacingText.containsLikelyLeak(text)) {
            throw new AssertionError("Player-facing leak in " + label + ": " + text);
        }
    }

    private Milestone02InfopediaMechanicsReadabilitySmoke() { }
}
