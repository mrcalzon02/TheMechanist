package mechanist;

import java.util.Locale;

/**
 * Shared Gate 3 helper for compact row/list presentation.
 *
 * <p>This formatter is intended for inventory rows, contract rows, menu lists, crafting lists,
 * and other dense multi-entry UI surfaces where short readable summaries are preferable to raw
 * implementation-oriented output.</p>
 */
final class PlayerFacingListRowText {
    private static final String NO_READABLE_DETAILS = "No readable details are available yet.";
    private static final int MAX_ROW_WIDTH = 80;

    private PlayerFacingListRowText() { }

    static String row(String primary, String secondary, int wrapWidth) {
        String safePrimary = cleanRowPart(primary, true, wrapWidth);
        String safeSecondary = cleanRowPart(secondary, false, wrapWidth);

        if (safePrimary.isBlank() && safeSecondary.isBlank()) {
            return "";
        }

        if (safePrimary.isBlank()) {
            return safeSecondary;
        }

        if (safeSecondary.isBlank() || safeSecondary.equalsIgnoreCase(safePrimary)) {
            return safePrimary;
        }

        return clampRow(safePrimary + " — " + safeSecondary);
    }

    private static String cleanRowPart(String text, boolean primary, int wrapWidth) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(text)
                .replace(':', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();

        if (isEmptyRowText(cleaned)) return "";

        if (primary) {
            return clampRow(cleaned);
        }

        String wrapped = PlayerFacingPanelBody.format(cleaned, wrapWidth)
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
        if (isEmptyRowText(wrapped)) return "";
        return clampRow(wrapped.endsWith(".") ? wrapped : wrapped + ".");
    }

    private static String clampRow(String row) {
        if (row == null) return "";
        String collapsed = row.replaceAll("\\s+", " ").trim();
        if (collapsed.length() <= MAX_ROW_WIDTH) return collapsed;
        return collapsed.substring(0, Math.max(0, MAX_ROW_WIDTH - 1)).trim() + "…";
    }

    private static boolean isEmptyRowText(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(NO_READABLE_DETAILS.toLowerCase(Locale.ROOT))
                || normalized.equals("internal record")
                || normalized.equals("diagnostic details")
                || normalized.equals("runtime service")
                || normalized.equals("catalog")
                || normalized.equals("the marked route")
                || normalized.equals("debug")
                || normalized.equals("debug contract")
                || normalized.equals("contract")
                || normalized.equals("row")
                || normalized.equals("list");
    }
}
