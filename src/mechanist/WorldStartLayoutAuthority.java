package mechanist;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/** Shared bounds for world selection, world generation, and character setup. */
final class WorldStartLayoutAuthority {
    private WorldStartLayoutAuthority() {}
    static final int MIN_VIEW_WIDTH = 760;
    static final int MIN_VIEW_HEIGHT = 560;

    static boolean usable(int width, int height) {
        return width >= MIN_VIEW_WIDTH && height >= MIN_VIEW_HEIGHT;
    }

    static Rectangle panelRect(int width, int height, boolean character) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        int marginX = Math.min(character ? 24 : 32, Math.max(8, w / 30));
        int marginY = Math.min(24, Math.max(8, h / 30));
        int pw = Math.max(1, Math.min(character ? 1320 : 1060, w - marginX * 2));
        int ph = Math.max(1, Math.min(character ? 760 : 720, h - marginY * 2));
        return new Rectangle((w - pw) / 2, (h - ph) / 2, pw, ph);
    }

    static int pickerListWidth(Rectangle panel) {
        return Math.max(1, Math.min(440, panel.width - 24 - 18 - 24 - 270));
    }

    static Rectangle pickerPreview(Rectangle panel) {
        int x = panel.x + 24 + pickerListWidth(panel) + 18;
        return new Rectangle(x, panel.y + 130,
                Math.max(1, panel.x + panel.width - 24 - x),
                Math.max(1, panel.height - 245));
    }

    static int generationListWidth(Rectangle panel) {
        return Math.max(1, Math.min(520, (int) Math.floor(panel.width * 0.47)));
    }

    static Rectangle seed(Rectangle panel) {
        return new Rectangle(panel.x + 24, panel.y + 116, generationListWidth(panel), 34);
    }

    static List<Rectangle> generationRows(Rectangle panel) {
        ArrayList<Rectangle> rows = new ArrayList<>();
        for (int i = 0; i < 7; i++)
            rows.add(new Rectangle(panel.x + 24, panel.y + 158 + i * 40,
                    generationListWidth(panel), 34));
        return rows;
    }

    static Rectangle generationPreview(Rectangle panel) {
        int x = panel.x + 24 + generationListWidth(panel) + 18;
        return new Rectangle(x, panel.y + 116,
                Math.max(1, panel.x + panel.width - 24 - x),
                Math.max(1, panel.height - 228));
    }

    static Rectangle characterBody(Rectangle panel) {
        return new Rectangle(panel.x + 24, panel.y + 112,
                Math.max(1, panel.width - 48), Math.max(1, panel.height - 210));
    }

    static List<Rectangle> statCells(Rectangle area, int statCount) {
        ArrayList<Rectangle> cells = new ArrayList<>();
        if (statCount <= 0) return cells;
        final int gap = 6;
        int innerW = Math.max(1, area.width - 24);
        int innerH = Math.max(1, area.height - 46);
        int cols = Math.max(1, Math.min(statCount, (innerW + gap) / 56));
        int rows = (statCount + cols - 1) / cols;
        int cellW = Math.max(1, (innerW - Math.max(0, cols - 1) * gap) / cols);
        int cellH = Math.max(1, (innerH - Math.max(0, rows - 1) * gap) / rows);
        for (int i = 0; i < statCount; i++) {
            cells.add(new Rectangle(area.x + 12 + (i % cols) * (cellW + gap),
                    area.y + 34 + (i / cols) * (cellH + gap), cellW, cellH));
        }
        return cells;
    }
}
