package mechanist;

import java.util.List;

/**
 * Shared Gate 3 formatter for bounded ordinary panel body text.
 *
 * <p>This helper combines sanitization and deterministic wrapping so gameplay/UI callsites can
 * migrate away from scattered local formatting behavior. The output is intended for event logs,
 * inspection panels, conversation bodies, contract detail panes, inventory descriptions, and
 * ordinary overlay windows.</p>
 */
final class PlayerFacingPanelBody {
    private PlayerFacingPanelBody() { }

    static String format(String body, int wrapWidth) {
        List<String> wrapped = PlayerFacingTextWrap.wrap(body, wrapWidth);

        if (wrapped.isEmpty()) {
            return "";
        }

        return String.join("\n", wrapped);
    }

    static String formatWithTitle(String title, String body, int wrapWidth) {
        String safeTitle = PlayerFacingCopySanitizer.forOrdinaryPlayer(title)
                .replace(':', ' ')
                .trim();

        String wrappedBody = format(body, wrapWidth);

        boolean emptyTitle = safeTitle.isBlank()
                || "No readable details are available yet.".equals(safeTitle);

        if (emptyTitle) {
            return wrappedBody;
        }

        if (wrappedBody.isBlank()) {
            return safeTitle;
        }

        return safeTitle + "\n" + wrappedBody;
    }
}
