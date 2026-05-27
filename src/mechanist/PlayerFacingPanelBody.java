package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared Gate 3 formatter for bounded ordinary panel body text.
 *
 * <p>This helper combines local cleanup and deterministic wrapping so gameplay/UI callsites can
 * migrate away from scattered formatting behavior. The output is intended for event logs,
 * inspection panels, conversation bodies, contract detail panes, inventory descriptions, and
 * ordinary overlay windows.</p>
 */
final class PlayerFacingPanelBody {
    private static final int MIN_WIDTH = 12;
    private static final String NO_READABLE_DETAILS = "No readable details are available yet.";

    private PlayerFacingPanelBody() { }

    static String format(String body, int wrapWidth) {
        String cleanBody = cleanPanelPart(body, false);
        if (cleanBody.isBlank()) return "";

        List<String> wrapped = wrap(cleanBody, wrapWidth);
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

    private static List<String> wrap(String text, int maxWidth) {
        int width = Math.max(MIN_WIDTH, maxWidth);
        List<String> lines = new ArrayList<>();
        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String rawWord : words) {
            String word = cleanPanelPart(rawWord, false);
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

        if (current.length() > 0) lines.add(current.toString());
        if (lines.isEmpty()) lines.add("");
        return lines;
    }

    private static String cleanPanelPart(String text, boolean title) {
        if (text == null || text.isBlank()) return "";

        String cleaned = text
                .replaceAll("(?i)targetZoneKey\\s*[=:]\\s*\\S+", " ")
                .replaceAll("(?i)registryKey\\s*[=:]\\s*\\S+", " ")
                .replaceAll("(?i)uuid\\s*[=:]\\s*[0-9a-f\\-]{8,}", " ")
                .replaceAll("(?i)className\\s*[=:]\\s*[A-Za-z0-9_.$]+", " ")
                .replaceAll("[A-Za-z]:\\\\[^\\s]+", " ")
                .replaceAll("/[^\\s]+", " ")
                .replaceAll("\\b[0-9]+,[0-9]+,[0-9]+,[0-9]+,[^\\s]+", " ")
                .replaceAll("\\b[A-Za-z_$][A-Za-z0-9_$]*(?:\\.[A-Za-z_$][A-Za-z0-9_$]*){2,}\\b", " ")
                .replace(':', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .trim();

        if (title) cleaned = cleaned.replace('\n', ' ').trim();
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
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
