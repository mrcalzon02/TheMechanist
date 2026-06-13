package mechanist;

import java.awt.Polygon;

final class FacingIndicatorAuthority {
    private FacingIndicatorAuthority() {}

    static Polygon tileTriangle(int tileX, int tileY, int tileSize, int framePad, int dx, int dy) {
        int[] direction = cardinalDirection(dx, dy);
        if (direction[0] == 0 && direction[1] == 0) return null;
        int pad = Math.max(1, Math.min(Math.max(1, tileSize / 3), framePad));
        int left = tileX + pad;
        int top = tileY + pad;
        int right = tileX + Math.max(pad + 2, tileSize - pad);
        int bottom = tileY + Math.max(pad + 2, tileSize - pad);
        int cx = (left + right) / 2;
        int cy = (top + bottom) / 2;
        int halfBase = Math.max(2, tileSize / 5);

        if (direction[1] < 0) return new Polygon(new int[]{cx - halfBase, cx + halfBase, cx}, new int[]{top, top, tileY}, 3);
        if (direction[1] > 0) return new Polygon(new int[]{cx - halfBase, cx + halfBase, cx}, new int[]{bottom, bottom, tileY + tileSize}, 3);
        if (direction[0] < 0) return new Polygon(new int[]{left, left, tileX}, new int[]{cy - halfBase, cy + halfBase, cy}, 3);
        return new Polygon(new int[]{right, right, tileX + tileSize}, new int[]{cy - halfBase, cy + halfBase, cy}, 3);
    }

    static int[] cardinalDirection(int dx, int dy) {
        if (dx == 0 && dy == 0) return new int[]{0, 0};
        if (Math.abs(dx) >= Math.abs(dy)) return new int[]{Integer.compare(dx, 0), 0};
        return new int[]{0, Integer.compare(dy, 0)};
    }
}
