package mechanist;

import java.util.Locale;

/**
 * Shared Gate 3 helper for readable menu and option presentation.
 *
 * <p>This formatter is intended for command menus, settings panels, interaction choices,
 * construction options, dialogue responses, and other selectable gameplay entries. It favors
 * compact readable presentation while routing visible text through the shared sanitization path.</p>
 */
final class PlayerFacingMenuOptionText {
    private static final String NO_READABLE_DETAILS = "No readable details are available yet.";

    private PlayerFacingMenuOptionText() { }

    static String option(String label, String detail, boolean enabled, int wrapWidth) {
        String safeLabel = cleanOptionPart(label, true, wrapWidth);
        String safeDetail = cleanOptionPart(detail, false, wrapWidth);
        String prefix = enabled ? "[Available] " : "[Unavailable] ";

        if (safeLabel.isBlank() && safeDetail.isBlank()) {
            return enabled ? "[Available] Option" : "[Unavailable] Option";
        }

        if (safeLabel.isBlank()) {
            return prefix + safeDetail;
        }

        if (safeDetail.isBlank() || safeDetail.equalsIgnoreCase(safeLabel)) {
            return prefix + safeLabel;
        }

        return prefix + safeLabel + "\n" + safeDetail;
    }

    private static String cleanOptionPart(String text, boolean label, int wrapWidth) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(text)
                .replace(':', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();

        if (isEmptyOptionText(cleaned)) return "";

        if (label) {
            return cleaned;
        }

        String wrapped = PlayerFacingPanelBody.format(cleaned, wrapWidth)
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
        if (isEmptyOptionText(wrapped)) return "";
        return wrapped.endsWith(".") ? wrapped : wrapped + ".";
    }

    private static boolean isEmptyOptionText(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(NO_READABLE_DETAILS.toLowerCase(Locale.ROOT))
                || normalized.equals("internal record")
                || normalized.equals("diagnostic details")
                || normalized.equals("runtime service")
                || normalized.equals("catalog")
                || normalized.equals("the marked route")
                || normalized.equals("debug")
                || normalized.equals("debug option")
                || normalized.equals("option")
                || normalized.equals("command")
                || normalized.equals("disabled")
                || normalized.equals("unavailable");
    }
}
