package mechanist;

import java.util.List;

final class Milestone02MenuDefinitionAuditSmoke {
    public static void main(String[] args) {
        UniversalWindowAuthority windows = new UniversalWindowAuthority();
        List<MenuDefinitionAuditAuthority.MenuDefinition> definitions = MenuDefinitionAuditAuthority.definitions(windows);
        if (definitions.size() != windows.specs().size()) {
            throw new AssertionError("Every registered window must have a menu audit definition.");
        }
        List<String> lines = MenuDefinitionAuditAuthority.playerFacingLines(windows);
        requireContains(lines, "inventory and storage interface domain", "inventory owner");
        requireContains(lines, "blueprint catalog", "construction data source");
        requireContains(lines, "categories, entry list, detail, visual preview", "Infopedia panes");
        requireContains(lines, "domain permission and failure checks required", "transfer safeguard");
        requireContains(lines, "confirmation, permission, and failure rules remain owned", "domain boundary");
        requireContains(MenuDefinitionAuditAuthority.summary(windows), "raw IDs prohibited", "summary leakage rule");
        for (String line : lines) {
            if (PlayerFacingText.containsLikelyLeak(line)) throw new AssertionError("Menu definition audit leaked implementation text: " + line);
        }
    }

    private static void requireContains(List<String> lines, String expected, String label) {
        for (String line : lines) if (line != null && line.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text == null || !text.contains(expected)) throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone02MenuDefinitionAuditSmoke() { }
}
