package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared Gate 3 formatter for bounded ordinary panel body text.
 *
 * <p>This helper combines sanitization and deterministic wrapping so gameplay/UI callsites can
 * migrate away from scattered local formatting behavior. The output is intended for event logs,
 * inspection panels, conversation bodies, contract detail panes, inventory descriptions, and
 * ordinary overlay windows.</p>
 */
final class PlayerFacingPanelBody {
    private static final String NO_READABLE_DETAILS = "No readable details are available yet.";

    private PlayerFacingPanelBody() { }

    static String format(String body, int wrapWidth) {
<<<<<<< HEAD
        if (body == null || body.isBlank()) {
            return "";
        }

        List<String> wrapped = PlayerFacingTextWrap.wrap(body, wrapWidth);
=======
        String cleanBody = cleanPanelPart(body, false);
        if (cleanBody.isBlank()) return "";
>>>>>>> origin/main

        List<String> wrapped = PlayerFacingTextWrap.wrap(cleanBody, wrapWidth);
        List<String> visible = new ArrayList<>();
        for (String line : wrapped) {
            String cleanLine = cleanPanelPart(line, false);
            if (!cleanLine.isBlank()) visible.add(cleanLine);
        }
        if (visible.isEmpty()) return "";
        return String.join("\n", visible);
    }

    static String formatWithTitle(String title, String body, int wrapWidth) {
        String safeTitle = cleanPanelPart(title, true);
        String wrappedBody = format(body, wrapWidth);

        if (safeTitle.isBlank()) return wrappedBody;
        if (wrappedBody.isBlank() || wrappedBody.equalsIgnoreCase(safeTitle)) return safeTitle;
        return safeTitle + "\n" + wrappedBody;
    }

    private static String cleanPanelPart(String text, boolean title) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(text)
                .replace(':', ' ')
                .replace('\r', ' ')
                .trim();

        if (title) cleaned = cleaned.replace('\n', ' ').trim();
        if (isEmptyPanelText(cleaned)) return "";
        return cleaned;
    }

    private static boolean isEmptyPanelText(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(NO_READABLE_DETAILS.toLowerCase(Locale.ROOT))
                || normalized.equals("internal record")
                || normalized.equals("diagnostic details")
                || normalized.equals("runtime service")
                || normalized.equals("catalog")
                || normalized.equals("the marked route")
                || normalized.equals("debug")
                || normalized.equals("debug panel")
                || normalized.equals("panel")
                || normalized.equals("body")
                || normalized.equals("layout")
                || normalized.equals("window");
    }
}
