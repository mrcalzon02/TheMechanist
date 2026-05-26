package mechanist;

/**
 * Shared Gate 3 helper for short tooltip and hover-text presentation.
 *
 * <p>This formatter is intended for compact UI surfaces such as inventory hover text, fixture
 * hover details, command hints, button descriptions, and quick-look summaries. It favors concise
 * readable wording while routing all visible text through the shared sanitization path.</p>
 */
final class PlayerFacingTooltipText {
    private PlayerFacingTooltipText() { }

    static String tooltip(String title, String detail, int wrapWidth) {
        String safeTitle = PlayerFacingCopySanitizer.forOrdinaryPlayer(title)
                .replace(':', ' ')
                .trim();

        String wrapped = PlayerFacingPanelBody.format(detail, wrapWidth);

        boolean emptyTitle = safeTitle.isBlank()
                || "No readable details are available yet.".equals(safeTitle);

        if (emptyTitle) {
            return wrapped;
        }

        if (wrapped.isBlank()) {
            return safeTitle;
        }

        return safeTitle + "\n" + wrapped;
    }
}
