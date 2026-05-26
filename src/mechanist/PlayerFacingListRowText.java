package mechanist;

/**
 * Shared Gate 3 helper for compact row/list presentation.
 *
 * <p>This formatter is intended for inventory rows, contract rows, menu lists, crafting lists,
 * and other dense multi-entry UI surfaces where short readable summaries are preferable to raw
 * implementation-oriented output.</p>
 */
final class PlayerFacingListRowText {
    private PlayerFacingListRowText() { }

    static String row(String primary, String secondary, int wrapWidth) {
        String safePrimary = PlayerFacingCopySanitizer.forOrdinaryPlayer(primary)
                .replace(':', ' ')
                .trim();

        String safeSecondary = PlayerFacingPanelBody.format(secondary, wrapWidth)
                .trim();

        boolean emptyPrimary = safePrimary.isBlank()
                || "No readable details are available yet.".equals(safePrimary);

        if (emptyPrimary) {
            return safeSecondary;
        }

        if (safeSecondary.isBlank()) {
            return safePrimary;
        }

        return safePrimary + " — " + safeSecondary;
    }
}
