package mechanist;

/**
 * Shared Gate 3 helper for readable menu and option presentation.
 *
 * <p>This formatter is intended for command menus, settings panels, interaction choices,
 * construction options, dialogue responses, and other selectable gameplay entries. It favors
 * compact readable presentation while routing visible text through the shared sanitization path.</p>
 */
final class PlayerFacingMenuOptionText {
    private PlayerFacingMenuOptionText() { }

    static String option(String label, String detail, boolean enabled, int wrapWidth) {
        String safeLabel = PlayerFacingCopySanitizer.forOrdinaryPlayer(label)
                .replace(':', ' ')
                .trim();

        String safeDetail = PlayerFacingPanelBody.format(detail, wrapWidth)
                .trim();

        String prefix = enabled ? "[Available] " : "[Unavailable] ";

        boolean emptyLabel = safeLabel.isBlank()
                || "No readable details are available yet.".equals(safeLabel);

        if (emptyLabel) {
            return safeDetail;
        }

        if (safeDetail.isBlank()) {
            return prefix + safeLabel;
        }

        return prefix + safeLabel + "\n" + safeDetail;
    }
}
