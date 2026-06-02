package mechanist;

import java.util.Locale;

/**
 * Shared Gate 3 helper for readable positive-action feedback.
 *
 * <p>Gameplay systems should continue storing authoritative results internally. This helper only
 * prepares concise readable player-facing summaries for logs, toast messages, event panels, and
 * ordinary gameplay feedback surfaces.</p>
 */
final class PlayerFacingActionText {
    private static final String NO_READABLE_DETAILS = "No readable details are available yet.";

    private PlayerFacingActionText() { }

    static String interaction(String action, String detail) {
        return describe(action, detail, "Action completed.");
    }

    static String inventory(String action, String detail) {
        return describe(action, detail, "Inventory updated.");
    }

    static String construction(String action, String detail) {
        return describe(action, detail, "Construction task updated.");
    }

    static String trade(String action, String detail) {
        return describe(action, detail, "Trade completed.");
    }

    static String travel(String action, String detail) {
        return describe(action, detail, "Travel completed.");
    }

    static String combat(String action, String detail) {
        return describe(action, detail, "Combat action completed.");
    }

    private static String describe(String action, String detail, String fallback) {
        String safeAction = cleanActionPart(action, true);
        String safeDetail = cleanActionPart(detail, false);

        if (safeAction.isBlank() && safeDetail.isBlank()) return fallback;
        if (safeDetail.isBlank()) return safeAction;
        if (safeAction.isBlank()) return safeDetail;
        if (safeDetail.equalsIgnoreCase(safeAction)) return safeAction;
        return safeAction + ": " + safeDetail;
    }

    private static String cleanActionPart(String text, boolean title) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(text)
                .replace(':', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();

        if (isEmptyActionText(cleaned)) return "";
        if (title) return cleaned;
        return cleaned.endsWith(".") ? cleaned : cleaned + ".";
    }

    private static boolean isEmptyActionText(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(NO_READABLE_DETAILS.toLowerCase(Locale.ROOT))
                || normalized.equals("internal record")
                || normalized.equals("diagnostic details")
                || normalized.equals("runtime service")
                || normalized.equals("catalog")
                || normalized.equals("the marked route")
                || normalized.equals("debug")
                || normalized.equals("action")
                || normalized.equals("result")
                || normalized.equals("success")
                || normalized.equals("completed");
    }
}
