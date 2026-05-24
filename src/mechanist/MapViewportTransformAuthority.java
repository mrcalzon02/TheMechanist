package mechanist;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Locale;

/**
 * Shared map viewport geometry for rendering and mouse targeting.
 * The same rectangle, cell size, camera origin, and draw origin must be used by
 * drawGame and screenPointToWorldTile so zoom/downscale/letterbox changes cannot
 * create cursor drift.
 */
final class MapViewportTransformAuthority {
    static final String VERSION = "map-viewport-transform-0.9.10gf";

    static final class Viewport {
        final Rectangle frame;
        final int worldW;
        final int worldH;
        final int focusX;
        final int focusY;
        final int cellW;
        final int cellH;
        final int cols;
        final int rows;
        final int camX;
        final int camY;
        final int ox;
        final int oy;
        final int drawW;
        final int drawH;

        Viewport(Rectangle frame, int worldW, int worldH, int focusX, int focusY,
                 int cellW, int cellH, int cols, int rows, int camX, int camY,
                 int ox, int oy, int drawW, int drawH) {
            this.frame = new Rectangle(frame);
            this.worldW = worldW;
            this.worldH = worldH;
            this.focusX = focusX;
            this.focusY = focusY;
            this.cellW = Math.max(1, cellW);
            this.cellH = Math.max(1, cellH);
            this.cols = Math.max(1, cols);
            this.rows = Math.max(1, rows);
            this.camX = Math.max(0, camX);
            this.camY = Math.max(0, camY);
            this.ox = ox;
            this.oy = oy;
            this.drawW = Math.max(1, drawW);
            this.drawH = Math.max(1, drawH);
        }

        Point pointToTile(int sx, int sy) {
            if (worldW <= 0 || worldH <= 0) return null;
            if (sx < ox || sy < oy || sx >= ox + drawW || sy >= oy + drawH) return null;
            int tx = camX + Math.floorDiv(sx - ox, cellW);
            int ty = camY + Math.floorDiv(sy - oy, cellH);
            if (tx < 0 || ty < 0 || tx >= worldW || ty >= worldH) return null;
            return new Point(tx, ty);
        }

        String auditSummary() {
            return String.format(Locale.US,
                    "frame=%dx%d@%d,%d cell=%dx%d colsRows=%dx%d cam=%d,%d focus=%d,%d origin=%d,%d draw=%dx%d world=%dx%d",
                    frame.width, frame.height, frame.x, frame.y,
                    cellW, cellH, cols, rows, camX, camY, focusX, focusY, ox, oy, drawW, drawH, worldW, worldH);
        }
    }

    static Viewport compute(Rectangle frame, int worldW, int worldH, int focusX, int focusY,
                            int cellW, int cellH, int originInsetX, int originInsetY) {
        Rectangle r = frame == null ? new Rectangle(0, 0, 1, 1) : new Rectangle(frame);
        int cw = Math.max(1, cellW);
        int ch = Math.max(1, cellH);
        int usableW = Math.max(1, r.width - 18);
        int usableH = Math.max(1, r.height - 18);
        int cols = Math.max(1, usableW / cw);
        int rows = Math.max(1, usableH / ch);
        int ww = Math.max(0, worldW);
        int wh = Math.max(0, worldH);
        if (ww > 0) cols = Math.min(cols, ww);
        if (wh > 0) rows = Math.min(rows, wh);
        int fx = ww <= 0 ? 0 : Math.max(0, Math.min(ww - 1, focusX));
        int fy = wh <= 0 ? 0 : Math.max(0, Math.min(wh - 1, focusY));
        int camX = ww <= 0 ? 0 : Math.max(0, Math.min(Math.max(0, ww - cols), fx - cols / 2));
        int camY = wh <= 0 ? 0 : Math.max(0, Math.min(Math.max(0, wh - rows), fy - rows / 2));
        int ox = r.x + Math.max(0, originInsetX);
        int oy = r.y + Math.max(0, originInsetY);
        int drawW = cols * cw;
        int drawH = rows * ch;
        return new Viewport(r, ww, wh, fx, fy, cw, ch, cols, rows, camX, camY, ox, oy, drawW, drawH);
    }

    static String auditSummary() {
        return "authority=" + VERSION + " shared-render-input-map-viewport=true hardcoded-offsets=false";
    }
}
