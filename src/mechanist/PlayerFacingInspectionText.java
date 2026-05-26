package mechanist;

/**
 * Shared Gate 3 helper for readable Look/Auspex inspection summaries.
 *
 * <p>This helper intentionally favors short readable observations over raw implementation dumps.
 * Detailed diagnostics and save-state internals should remain in audit or debug surfaces outside
 * ordinary gameplay inspection panels.</p>
 */
final class PlayerFacingInspectionText {
    private PlayerFacingInspectionText() { }

    static String tile(String tileName, String detail) {
        return describe(tileName, detail, "The area reveals nothing unusual.");
    }

    static String fixture(String fixtureName, String detail) {
        return describe(fixtureName, detail, "The fixture appears inactive.");
    }

    static String actor(String actorName, String detail) {
        return describe(actorName, detail, "No readable details are available.");
    }

    static String item(String itemName, String detail) {
        return describe(itemName, detail, "The item appears ordinary.");
    }

    static String route(String routeName, String detail) {
        return describe(routeName, detail, "The route ahead is unclear.");
    }

    private static String describe(String title, String detail, String fallback) {
        String safeTitle = PlayerFacingCopySanitizer.forOrdinaryPlayer(title)
                .replace(':', ' ')
                .trim();

        String safeDetail = PlayerFacingCopySanitizer.forOrdinaryPlayer(detail);

        if (safeDetail.isBlank() || "No readable details are available yet.".equals(safeDetail)) {
            return fallback;
        }

        if (safeTitle.isBlank() || "No readable details are available yet.".equals(safeTitle)) {
            return safeDetail;
        }

        return safeTitle + ": " + safeDetail;
    }
}
