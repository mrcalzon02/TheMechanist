package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared Gate 3 helper for predictable readable line wrapping.
 *
 * <p>This utility exists to keep ordinary gameplay panels readable and bounded without forcing UI
 * callsites to implement their own inconsistent wrapping logic. It intentionally performs simple
 * whitespace wrapping suitable for logs, inspection panels, event windows, and tooltip bodies.</p>
 */
final class PlayerFacingTextWrap {
    private static final int MIN_WIDTH = 12;
    private static final String NO_READABLE_DETAILS = "No readable details are available yet.";

    private PlayerFacingTextWrap() { }

    static List<String> wrap(String text, int maxWidth) {
        int width = Math.max(MIN_WIDTH, maxWidth);
        List<String> lines = new ArrayList<>();

        String sanitized = cleanWrapText(text);
        if (sanitized.isBlank()) {
            lines.add("");
            return lines;
        }

        String[] words = sanitized.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String rawWord : words) {
            String word = cleanWrapText(rawWord);
            if (word.isBlank()) continue;

            while (word.length() > width) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                int sliceEnd = Math.max(1, width - 1);
                lines.add(word.substring(0, sliceEnd) + "…");
                word = word.substring(sliceEnd);
            }

            if (current.length() == 0) {
                current.append(word);
            } else if (current.length() + 1 + word.length() > width) {
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

        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private static String cleanWrapText(String text) {
        String cleaned = PlayerFacingCopySanitizer.forOrdinaryPlayer(text)
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ')
                .trim();

        if (isEmptyWrapText(cleaned)) return "";
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private static boolean isEmptyWrapText(String text) {
        if (text == null || text.isBlank()) return true;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        return normalized.equals(NO_READABLE_DETAILS.toLowerCase(Locale.ROOT))
                || normalized.equals("internal record")
                || normalized.equals("diagnostic details")
                || normalized.equals("runtime service")
                || normalized.equals("catalog")
                || normalized.equals("the marked route")
                || normalized.equals("debug")
                || normalized.equals("layout")
                || normalized.equals("panel")
                || normalized.equals("window");
    }
}
