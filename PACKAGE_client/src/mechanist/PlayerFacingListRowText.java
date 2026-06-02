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

    static String contractRow(String mission, String details, int wrapWidth) {
        String safeMission = cleanContractPart(mission, true, wrapWidth);
        String safeDetails = cleanContractPart(details, false, wrapWidth);

        if (safeMission.isBlank() && safeDetails.isBlank()) return "Mission details unavailable.";
        if (safeMission.isBlank()) return safeDetails;
        if (safeDetails.isBlank() || safeDetails.equalsIgnoreCase(safeMission)) return safeMission;
        return clampRow(safeMission + " — " + safeDetails);
    }

    static String contractDetail(String title, String body, int wrapWidth) {
        String safeTitle = cleanContractPart(title, true, wrapWidth);
        String safeBody = cleanContractPart(body, false, wrapWidth);

        if (safeTitle.isBlank() && safeBody.isBlank()) return "Mission details unavailable.";
        if (safeTitle.isBlank()) return safeBody;
        if (safeBody.isBlank() || safeBody.equalsIgnoreCase(safeTitle)) return safeTitle;
        return safeTitle + "\n" + safeBody;
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

    private static String cleanContractPart(String text, boolean title, int wrapWidth) {
        String cleaned = cleanRowPart(text, title, wrapWidth);
        if (isEmptyContractText(cleaned)) return "";
        return cleaned;
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

    private static boolean isEmptyContractText(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("objective id")
                || normalized.equals("objective key")
                || normalized.equals("contract id")
                || normalized.equals("reward token")
                || normalized.equals("route state")
                || normalized.equals("faction key")
                || normalized.equals("mission token")
                || normalized.equals("target token")
                || normalized.equals("turn in token")
                || normalized.equals("debug mission")
                || normalized.equals("debug objective");
    }
}
