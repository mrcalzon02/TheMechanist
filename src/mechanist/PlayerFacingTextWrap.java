package mechanist;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared Gate 3 helper for predictable readable line wrapping.
 *
 * <p>This utility exists to keep ordinary gameplay panels readable and bounded without forcing UI
 * callsites to implement their own inconsistent wrapping logic. It intentionally performs simple
 * whitespace wrapping suitable for logs, inspection panels, event windows, and tooltip bodies.</p>
 */
final class PlayerFacingTextWrap {
    private PlayerFacingTextWrap() { }

    static List<String> wrap(String text, int maxWidth) {
        String sanitized = PlayerFacingCopySanitizer.forOrdinaryPlayer(text);

        int width = Math.max(12, maxWidth);
        List<String> lines = new ArrayList<>();

        if (sanitized.isBlank()) {
            lines.add("");
            return lines;
        }

        String[] words = sanitized.trim().split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() == 0) {
                current.append(word);
                continue;
            }

            if (current.length() + 1 + word.length() > width) {
                lines.add(current.toString());
                current.setLength(0);
                current.append(word);
            } else {
                current.append(' ').append(word);
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }
}
