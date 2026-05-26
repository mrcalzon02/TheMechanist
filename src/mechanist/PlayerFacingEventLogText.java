package mechanist;

/**
 * Shared Gate 3 helper for readable event-log presentation.
 *
 * <p>This formatter is intended to unify ordinary gameplay event presentation across movement,
 * interaction, denial, combat, construction, and inspection systems while preventing raw runtime
 * implementation residue from leaking into visible player logs.</p>
 */
final class PlayerFacingEventLogText {
    private PlayerFacingEventLogText() { }

    static String event(String category, String body, int wrapWidth) {
        String safeCategory = PlayerFacingCopySanitizer.forOrdinaryPlayer(category)
                .replace(':', ' ')
                .trim();

        String formattedBody = PlayerFacingPanelBody.format(body, wrapWidth);

        boolean emptyCategory = safeCategory.isBlank()
                || "No readable details are available yet.".equals(safeCategory);

        if (emptyCategory) {
            return formattedBody;
        }

        if (formattedBody.isBlank()) {
            return safeCategory;
        }

        return "[" + safeCategory + "]\n" + formattedBody;
    }
}
