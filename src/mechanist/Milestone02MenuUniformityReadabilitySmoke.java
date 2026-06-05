package mechanist;

import java.util.List;

/** Smoke for Milestone 02 menu uniformity audit wording. */
final class Milestone02MenuUniformityReadabilitySmoke {
    public static void main(String[] args) {
        UniversalWindowAuthority authority = new UniversalWindowAuthority();
        List<String> lines = authority.playerFacingMenuAuditLines();
        requireContains(lines, "Inventory", "inventory menu audit");
        requireContains(lines, "Trade", "trade menu audit");
        requireContains(lines, "Construction", "construction menu audit");
        requireContains(lines, "Infopedia", "infopedia menu audit");
        requireContains(lines, "Save / Load", "save/load menu audit");
        requireContains(lines, "Transfer rule", "shared transfer rule");
        requireContains(lines, "Prompt rule", "shared prompt rule");

        String summary = authority.playerFacingSummary();
        requireContains(summary, "Menu audit covers", "menu summary");
        rejectLeaks(summary, "menu summary");

        for (String line : lines) {
            rejectLeaks(line, "menu audit line");
            rejectContains(line, "kind=", "menu audit should not expose compact spec fields");
            rejectContains(line, "EscapeBehavior", "menu audit should not expose enum type");
            rejectContains(line, "WindowKind", "menu audit should not expose enum type");
            rejectContains(line, "supportsInventoryTransfer", "menu audit should not expose field names");
        }

        List<String> menuEntry = SemanticAssetInfopediaAuthority.mechanicEntryRows("uniformity");
        requireContains(menuEntry, "Menu Uniformity", "menu uniformity mechanic row");
        List<String> detail = SemanticAssetInfopediaAuthority.mechanicDetailLinesByKey("menu-uniformity");
        requireContains(detail, "Back returns", "menu detail back behavior");
        requireContains(detail, "Milestone02MenuUniformityReadabilitySmoke", "menu detail smoke guard");
        for (String line : detail) rejectLeaks(line, "menu uniformity infopedia detail");
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) {
            if (line != null && line.contains(expected)) return;
        }
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
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

    private Milestone02MenuUniformityReadabilitySmoke() { }
}
