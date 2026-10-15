package mechanist;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

/** Headless regression coverage for text and control-label pixel containment. */
final class GuiLayoutApiSmoke {
    private GuiLayoutApiSmoke() {}

    private static void check(boolean valid, String description) {
        if (!valid) throw new AssertionError(description);
    }

    public static void main(String[] args) {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setFont(new Font("Dialog", Font.PLAIN, 14));
            FontMetrics metrics = graphics.getFontMetrics();
            int width = metrics.stringWidth("MMMMM");
            String longToken = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
            List<String> chunks = GuiLayoutApi.wrapText(longToken, metrics, width);
            check(chunks.size() > 1, "Long text should wrap");
            check(String.join("", chunks).equals(longToken), "Long text must not be truncated");
            for (String chunk : chunks) {
                check(metrics.stringWidth(chunk) <= width, "Line exceeds pixel width: " + chunk);
            }

            List<String> whitespace = GuiLayoutApi.wrapText(
                    "alpha\tbeta\ngamma  delta", metrics, metrics.stringWidth("alpha beta gamma delta"));
            check(String.join(" ", whitespace).equals("alpha beta gamma delta"),
                    "Tabs and line breaks should be treated as whitespace");

            String emojiText = "😀😀😀😀😀";
            int emojiWidth = metrics.stringWidth("😀😀");
            List<String> emojiLines = GuiLayoutApi.wrapText(emojiText, metrics, emojiWidth);
            check(String.join("", emojiLines).equals(emojiText), "Unicode text must be preserved");
            for (String line : emojiLines) {
                check(metrics.stringWidth(line) <= emojiWidth, "Unicode line exceeds pixel width");
                checkValidSurrogates(line);
            }

            int labelWidth = metrics.stringWidth("Open😀…");
            String label = GuiLayoutApi.fitLabel("Open😀😀😀 Storage", metrics, labelWidth);
            check(metrics.stringWidth(label) <= labelWidth, "Button label exceeds width");
            checkValidSurrogates(label);
            check(GuiLayoutApi.wrapText("", metrics, width).equals(List.of("")),
                    "Blank text should retain a blank line");
            check(GuiLayoutApi.fitLabel("sample", metrics, 0).isEmpty(),
                    "Zero-width labels should not overflow");
        } finally {
            graphics.dispose();
        }
    }

    private static void checkValidSurrogates(String text) {
        for (int index = 0; index < text.length(); index++) {
            char ch = text.charAt(index);
            if (Character.isHighSurrogate(ch)) {
                check(index + 1 < text.length() && Character.isLowSurrogate(text.charAt(index + 1)),
                        "Split high Unicode surrogate");
            }
            if (Character.isLowSurrogate(ch)) {
                check(index > 0 && Character.isHighSurrogate(text.charAt(index - 1)),
                        "Split low Unicode surrogate");
            }
        }
    }
}
