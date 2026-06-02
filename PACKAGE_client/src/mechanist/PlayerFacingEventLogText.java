package mechanist;

import java.util.Locale;

/**
 * Shared Gate 3 helper for readable event-log presentation.
 *
 * <p>This formatter is intended to unify ordinary gameplay event presentation across movement,
 * interaction, denial, combat, construction, and inspection systems while preventing raw runtime
 * implementation residue from leaking into visible player logs.</p>
 */
final class PlayerFacingEventLogText {
    private static final String NO_READABLE_DETAILS = "No readable details are available yet.";

    private PlayerFacingEventLogText() { }

    static String event(String category, String body, int wrapWidth) {
        String safeCategory = cleanEventPart(category, true, wrapWidth);
        String safeBody = cleanEventPart(body, false, wrapWidth);

        if (safeCategory.isBlank() && safeBody.isBlank()) {
            return "";
        }

        if (safeCategory.isBlank()) {
            return safeBody;
        }

        if (safeBody.isBlank() || safeBody.equalsIgnoreCase(safeCategory)) {
            return safeCategory;
        }

        return "[" + safeCategory + "]\n" + safeBody;
    }

    private static String cleanEventPart(String text, boolean category, int wrapWidth) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(text)
                .replace(':', ' ')
                .replace('\r', ' ')
                .trim();

        if (category) {
            cleaned = cleaned.replace('\n', ' ').trim();
        }

        if (isEmptyEventText(cleaned)) return "";

        if (category) {
            return cleaned;
        }

        String formatted = PlayerFacingPanelBody.format(cleaned, wrapWidth).trim();
        if (isEmptyEventText(formatted)) return "";
        return formatted.endsWith(".") ? formatted : formatted + ".";
    }

    private static boolean isEmptyEventText(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(NO_READABLE_DETAILS.toLowerCase(Locale.ROOT))
                || normalized.equals("internal record")
                || normalized.equals("diagnostic details")
                || normalized.equals("runtime service")
                || normalized.equals("catalog")
                || normalized.equals("the marked route")
                || normalized.equals("debug")
                || normalized.equals("debug log")
                || normalized.equals("log")
                || normalized.equals("event")
                || normalized.equals("combat log")
                || normalized.equals("system event");
    }
}
