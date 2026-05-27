package mechanist;

import java.util.Locale;

/**
 * Gate 3 adapter layer for ordinary player-facing UI text.
 *
 * <p>Callers should route high-traffic panel copy through these helpers before drawing it in Look,
 * transition, inventory, construction, trade, save/load, and denial-message surfaces. The helpers
 * keep the player-facing wording readable while preserving detailed originals for diagnostics and
 * audit logs outside this class.</p>
 */
final class PlayerFacingUiText {
    private static final String NO_READABLE_DETAILS = "No readable details are available yet.";

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
        String cleaned = cleanStatusPart(summary, false);
        if (cleaned.isBlank()) return "Save status updated.";
        return cleaned;
    }

    static String diagnosticNotice(String action) {
        String cleaned = cleanStatusPart(action, false);
        if (cleaned.isBlank()) return "The issue was recorded for review.";
        return cleaned + " The issue was recorded for review.";
    }

    static String controlHint(String action, String keyName, String detail) {
        String safeAction = cleanGuidancePart(action, true);
        String safeKey = cleanGuidancePart(keyName, true);
        String safeDetail = cleanGuidancePart(detail, false);

        if (safeAction.isBlank() && safeKey.isBlank() && safeDetail.isBlank()) {
            return "Open the controls menu for available commands.";
        }

        StringBuilder line = new StringBuilder();
        if (!safeAction.isBlank()) line.append(safeAction);
        if (!safeKey.isBlank()) {
            if (line.length() > 0) line.append(" — ");
            line.append(safeKey);
        }
        if (!safeDetail.isBlank()) {
            if (line.length() > 0) line.append("\n");
            line.append(safeDetail);
        }
        return line.toString();
    }

    static String helpPanel(String title, String body, int wrapWidth) {
        String safeTitle = cleanGuidancePart(title, true);
        String safeBody = cleanGuidancePart(body, false);
        if (safeTitle.isBlank() && safeBody.isBlank()) return "Controls\nOpen the controls menu for available commands.";
        if (safeTitle.isBlank()) return PlayerFacingPanelBody.format(safeBody, wrapWidth);
        if (safeBody.isBlank()) return safeTitle;
        return safeTitle + "\n" + PlayerFacingPanelBody.format(safeBody, wrapWidth);
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

    private static String cleanGuidancePart(String text, boolean title) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(text)
                .replace(':', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();
        if (isEmptyGuidanceText(cleaned)) return "";
        if (title) return cleaned;
        return cleaned.endsWith(".") ? cleaned : cleaned + ".";
    }

    private static String cleanStatusPart(String text, boolean title) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(text)
                .replace(':', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();
        if (isEmptyStatusText(cleaned)) return "";
        if (title) return cleaned;
        return cleaned.endsWith(".") ? cleaned : cleaned + ".";
    }

    private static boolean isEmptyGuidanceText(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(NO_READABLE_DETAILS.toLowerCase(Locale.ROOT))
                || normalized.equals("internal record")
                || normalized.equals("diagnostic details")
                || normalized.equals("runtime service")
                || normalized.equals("catalog")
                || normalized.equals("the marked route")
                || normalized.equals("debug")
                || normalized.equals("input")
                || normalized.equals("control")
                || normalized.equals("keybinding")
                || normalized.equals("command")
                || normalized.equals("binding")
                || normalized.equals("handler")
                || normalized.equals("listener");
    }

    private static boolean isEmptyStatusText(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(NO_READABLE_DETAILS.toLowerCase(Locale.ROOT))
                || normalized.equals("internal record")
                || normalized.equals("diagnostic details")
                || normalized.equals("runtime service")
                || normalized.equals("debug")
                || normalized.equals("error")
                || normalized.equals("exception")
                || normalized.equals("stack trace")
                || normalized.equals("save file")
                || normalized.equals("load file")
                || normalized.equals("filesystem")
                || normalized.equals("diagnostic log")
                || normalized.equals("audit log")
                || normalized.equals("system state");
    }
}
