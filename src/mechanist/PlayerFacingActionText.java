package mechanist;

/**
 * Shared Gate 3 helper for readable positive-action feedback.
 *
 * <p>Gameplay systems should continue storing authoritative results internally. This helper only
 * prepares concise readable player-facing summaries for logs, toast messages, event panels, and
 * ordinary gameplay feedback surfaces.</p>
 */
final class PlayerFacingActionText {
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
        String safeAction = PlayerFacingCopySanitizer.forOrdinaryPlayer(action)
                .replace(':', ' ')
                .trim();

        String safeDetail = PlayerFacingCopySanitizer.forOrdinaryPlayer(detail);

        boolean emptyAction = safeAction.isBlank()
                || "No readable details are available yet.".equals(safeAction);

        boolean emptyDetail = safeDetail.isBlank()
                || "No readable details are available yet.".equals(safeDetail);

        if (emptyAction && emptyDetail) {
            return fallback;
        }

        if (emptyDetail) {
            return safeAction;
        }

        if (emptyAction) {
            return safeDetail;
        }

        return safeAction + ": " + safeDetail;
    }
}
