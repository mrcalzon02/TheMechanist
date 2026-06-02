package mechanist;

import java.util.Locale;

/**
 * Shared Gate 3 helper for readable Look/Auspex inspection summaries.
 *
 * <p>This helper intentionally favors short readable observations over raw implementation dumps.
 * Detailed diagnostics and save-state internals should remain in audit or debug surfaces outside
 * ordinary gameplay inspection panels.</p>
 */
final class PlayerFacingInspectionText {
    private static final String NO_READABLE_DETAILS = "No readable details are available yet.";

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
        String safeTitle = cleanTitle(title);
        String safeDetail = cleanDetail(detail);

        if (safeDetail.isBlank()) {
            return fallback;
        }

        if (safeTitle.isBlank()) {
            return safeDetail;
        }

        if (safeDetail.equalsIgnoreCase(safeTitle)) {
            return safeTitle;
        }

        return safeTitle + ": " + safeDetail;
    }

    private static String cleanTitle(String title) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(title)
                .replace(':', ' ')
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();
        if (isEmptyInspectionText(cleaned)) return "";
        return cleaned;
    }

    private static String cleanDetail(String detail) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(detail)
                .replace('\r', ' ')
                .trim();
        if (isEmptyInspectionText(cleaned)) return "";
        return cleaned;
    }

    private static boolean isEmptyInspectionText(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(NO_READABLE_DETAILS.toLowerCase(Locale.ROOT))
                || normalized.equals("internal record")
                || normalized.equals("diagnostic details")
                || normalized.equals("runtime service")
                || normalized.equals("catalog")
                || normalized.equals("the marked route");
    }
}
