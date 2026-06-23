package mechanist;

import java.util.List;

/** Smoke for room-blueprint exit-route and no-self-entombment warnings. */
final class Milestone03BlueprintNoSelfEntombmentAuditSmoke {
    public static void main(String[] args) {
        BlueprintConstructionAuthority.RoomBlueprint sealed =
                BlueprintConstructionAuthority.hollowBox("sealed-test-room", "Sealed Test Room", 4, 4, false);
        BlueprintConstructionAuthority.ValidationResult sealedResult =
                BlueprintConstructionAuthority.preflight(sealed, 10, 20, targets(sealed, 10, 20), BlueprintConstructionAuthority.estimateCost(sealed));

        require(sealedResult.canPlace(), "anchorless sample should remain a warning-only planning issue");
        requireContains(sealedResult.issues(), "no connection anchor or doorway", "anchorless warning");
        requireContains(sealedResult.issues(), "no-self-entombment validation must keep an exit route", "entombment warning");

        BlueprintConstructionAuthority.RoomBlueprint open =
                BlueprintConstructionAuthority.hollowBox("open-test-room", "Open Test Room", 4, 4, true);
        BlueprintConstructionAuthority.ValidationResult openResult =
                BlueprintConstructionAuthority.preflight(open, 10, 20, targets(open, 10, 20), BlueprintConstructionAuthority.estimateCost(open));
        require(!contains(openResult.issues(), "no-self-entombment"), "doorway anchor should satisfy exit-route warning");

        List<String> audit = BlueprintConstructionAuthority.definitionAuditLines();
        requireContains(audit, "no-self-entombment exit warnings", "definition audit");
        requireContains(audit, "exit route must remain open", "ghost audit");
        requireContains(audit, "Milestone03BlueprintNoSelfEntombmentAuditSmoke", "guard reference");

        for (String line : audit) rejectLeaks(line);
        for (BlueprintConstructionAuthority.ValidationIssue issue : sealedResult.issues()) rejectLeaks(issue.reason());
    }

    private static List<BlueprintConstructionAuthority.TargetTile> targets(
            BlueprintConstructionAuthority.RoomBlueprint blueprint, int originX, int originY) {
        java.util.ArrayList<BlueprintConstructionAuthority.TargetTile> out = new java.util.ArrayList<>();
        for (BlueprintConstructionAuthority.BlueprintCell cell : blueprint.cells()) {
            out.add(new BlueprintConstructionAuthority.TargetTile(originX + cell.x(), originY + cell.y(),
                    false, false, false, false, true, "clear buildable floor"));
        }
        return out;
    }

    private static boolean contains(List<BlueprintConstructionAuthority.ValidationIssue> issues, String expected) {
        for (BlueprintConstructionAuthority.ValidationIssue issue : issues) {
            if (issue != null && issue.reason() != null && issue.reason().toLowerCase().contains(expected.toLowerCase())) return true;
        }
        return false;
    }

    private static void requireContains(List<?> lines, String expected, String label) {
        for (Object line : lines) {
            String text = line instanceof BlueprintConstructionAuthority.ValidationIssue issue ? issue.reason() : String.valueOf(line);
            if (text != null && text.toLowerCase().contains(expected.toLowerCase())) return;
        }
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + lines);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void rejectLeaks(String line) {
        if (PlayerFacingText.containsLikelyLeak(line)) {
            throw new AssertionError("No-self-entombment audit leaked implementation text: " + line);
        }
    }

    private Milestone03BlueprintNoSelfEntombmentAuditSmoke() { }
}
