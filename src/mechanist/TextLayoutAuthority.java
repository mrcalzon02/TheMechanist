package mechanist;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class TextLayoutAuthority {
    private TextLayoutAuthority() {}

    static List<String> wrapPixels(Graphics2D g, Font font, String text, int maxPixels) {
        if (text == null) return Collections.singletonList("");
        return wrapAllPixels(g, font, Collections.singletonList(text), maxPixels);
    }

    static List<String> wrapAllPixels(Graphics2D g, Font font, List<String> lines, int maxPixels) {
        if (lines == null || lines.isEmpty()) return Collections.emptyList();
        Font oldFont = g.getFont();
        if (font != null) g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int limit = Math.max(8, maxPixels);
        ArrayList<String> out = new ArrayList<>();
        for (String source : lines) wrapLinePixels(fm, source == null ? "" : source, limit, out);
        if (font != null) g.setFont(oldFont);
        return out;
    }

    private static void wrapLinePixels(FontMetrics fm, String text, int limit, List<String> out) {
        if (text == null || text.isEmpty()) {
            out.add("");
            return;
        }
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (fm.stringWidth(word) > limit) {
                flush(line, out);
                splitLongWordPixels(fm, word, limit, out);
            } else if (line.length() == 0) {
                line.append(word);
            } else {
                String candidate = line + " " + word;
                if (fm.stringWidth(candidate) <= limit) line.append(' ').append(word);
                else {
                    flush(line, out);
                    line.append(word);
                }
            }
        }
        flush(line, out);
    }

    private static void splitLongWordPixels(FontMetrics fm, String word, int limit, List<String> out) {
        StringBuilder piece = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (piece.length() > 0 && fm.stringWidth(piece.toString() + ch) > limit) {
                out.add(piece.toString());
                piece.setLength(0);
            }
            piece.append(ch);
        }
        flush(piece, out);
    }

    private static void flush(StringBuilder line, List<String> out) {
        if (line.length() == 0) return;
        out.add(line.toString());
        line.setLength(0);
    }
}
