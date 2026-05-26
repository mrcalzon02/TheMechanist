package mechanist;

/**
 * Gate 3 adapter layer for ordinary player-facing UI text.
 *
 * <p>Callers should route high-traffic panel copy through these helpers before drawing it in Look,
 * transition, inventory, construction, trade, save/load, and denial-message surfaces. The helpers
 * keep the player-facing wording readable while preserving detailed originals for diagnostics and
 * audit logs outside this class.</p>
 */
final class PlayerFacingUiText {
    private PlayerFacingUiText() { }

    static String lookDetail(String subject, String detail) {
        return titledLine(subject, detail, "Nothing notable is visible.");
    }

    static String transitionDenied(String reason) {
        return failureLine("Route unavailable", reason, "The way ahead is blocked or unavailable.");
    }

    static String inventoryDenied(String reason) {
        return failureLine("Inventory action unavailable", reason, "That item action cannot be completed right now.");
    }

    static String constructionDenied(String reason) {
        return failureLine("Construction unavailable", reason, "That construction order cannot be completed here.");
    }

    static String tradeDenied(String reason) {
        return failureLine("Trade unavailable", reason, "That trade cannot be completed right now.");
    }

    static String saveLoadSummary(String summary) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(summary);
        if (cleaned.isBlank()) return "Saved world position recorded.";
        return cleaned;
    }

    static String diagnosticNotice(String action) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(action);
        if (cleaned.isBlank()) return "Diagnostic details were recorded.";
        return cleaned + " Diagnostic details were recorded.";
    }

    static String titledLine(String title, String detail, String fallback) {
        String safeTitle = sanitizeTitle(title);
        String safeDetail = PlayerFacingCopySanitizer.forOrdinaryPlayer(detail);
        if (safeDetail.isBlank() || "No readable details are available yet.".equals(safeDetail)) return fallback;
        if (safeTitle.isBlank()) return safeDetail;
        return safeTitle + ": " + safeDetail;
    }

    private static String failureLine(String prefix, String reason, String fallback) {
        String safeReason = PlayerFacingCopySanitizer.forOrdinaryPlayer(reason);
        if (safeReason.isBlank() || "No readable details are available yet.".equals(safeReason)) return fallback;
        return prefix + ": " + safeReason;
    }

    private static String sanitizeTitle(String title) {
        String safe = PlayerFacingCopySanitizer.forOrdinaryPlayer(title);
        if ("No readable details are available yet.".equals(safe)) return "";
        return safe.replace(':', ' ').trim();
    }
}
