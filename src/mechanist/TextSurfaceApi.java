package mechanist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TextSurfaceApi {
    private TextSurfaceApi() {}

    static List<String> wrap(String text, int maxChars) {
        if (text == null || text.isBlank()) return Collections.singletonList("");
        int limit = Math.max(1, maxChars);
        ArrayList<String> out = new ArrayList<>();
        for (String paragraph : text.split("\\R", -1)) {
            wrapParagraph(paragraph, limit, out);
        }
        return out.isEmpty() ? Collections.singletonList("") : out;
    }

    static List<String> wrap(List<String> lines, int maxChars) {
        if (lines == null || lines.isEmpty()) return Collections.emptyList();
        ArrayList<String> out = new ArrayList<>();
        for (String line : lines) out.addAll(wrap(line, maxChars));
        return out;
    }

    private static void wrapParagraph(String text, int limit, List<String> out) {
        if (text == null || text.isEmpty()) {
            out.add("");
            return;
        }
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.length() > limit) {
                flush(line, out);
                splitLongWord(word, limit, out);
            } else if (line.length() == 0) {
                line.append(word);
            } else if (line.length() + 1 + word.length() <= limit) {
                line.append(' ').append(word);
            } else {
                flush(line, out);
                line.append(word);
            }
        }
        flush(line, out);
    }

    private static void splitLongWord(String word, int limit, List<String> out) {
        int start = 0;
        while (start < word.length()) {
            int end = Math.min(word.length(), start + limit);
            out.add(word.substring(start, end));
            start = end;
        }
    }

    private static void flush(StringBuilder line, List<String> out) {
        if (line.length() == 0) return;
        out.add(line.toString());
        line.setLength(0);
    }
}
