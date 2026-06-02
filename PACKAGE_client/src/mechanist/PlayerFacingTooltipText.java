package mechanist;

import java.util.Locale;

/**
 * Shared Gate 3 helper for short tooltip and hover-text presentation.
 *
 * <p>This formatter is intended for compact UI surfaces such as inventory hover text, fixture
 * hover details, command hints, button descriptions, and quick-look summaries. It favors concise
 * readable wording while routing all visible text through the shared sanitization path.</p>
 */
final class PlayerFacingTooltipText {
    private static final String NO_READABLE_DETAILS = "No readable details are available yet.";

    private PlayerFacingTooltipText() { }

    static String tooltip(String title, String detail, int wrapWidth) {
        String safeTitle = cleanTooltipPart(title, true);
        String safeDetail = cleanTooltipPart(detail, false);

        if (safeTitle.isBlank() && safeDetail.isBlank()) {
            return "";
        }

        if (safeTitle.isBlank()) {
            return PlayerFacingPanelBody.format(safeDetail, wrapWidth);
        }

        if (safeDetail.isBlank() || safeDetail.equalsIgnoreCase(safeTitle)) {
            return safeTitle;
        }

        String wrapped = PlayerFacingPanelBody.format(safeDetail, wrapWidth);
        if (wrapped.isBlank()) {
            return safeTitle;
        }

        return safeTitle + "\n" + wrapped;
    }

    private static String cleanTooltipPart(String text, boolean title) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(text)
                .replace(':', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();

        if (isEmptyTooltipText(cleaned)) return "";
        if (title) return cleaned;
        return cleaned.endsWith(".") ? cleaned : cleaned + ".";
    }

    private static boolean isEmptyTooltipText(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(NO_READABLE_DETAILS.toLowerCase(Locale.ROOT))
                || normalized.equals("internal record")
                || normalized.equals("diagnostic details")
                || normalized.equals("runtime service")
                || normalized.equals("catalog")
                || normalized.equals("the marked route")
                || normalized.equals("debug")
                || normalized.equals("debug tooltip")
                || normalized.equals("tooltip");
    }
}
