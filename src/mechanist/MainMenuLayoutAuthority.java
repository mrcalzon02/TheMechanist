package mechanist;

import java.awt.Rectangle;

/** Shared first-screen button frame and hitbox geometry. */
final class MainMenuLayoutAuthority {
    private MainMenuLayoutAuthority() {}

    static Rectangle frame(int width, int height, int routes) {
        int w = Math.max(1, width), h = Math.max(1, height), count = Math.max(1, routes);
        int marginX = Math.min(32, Math.max(8, w / 24));
        int frameW = Math.max(1, Math.min(650, w - marginX * 2));
        int gap = 6, pad = 12;
        int targetRowH = h < 600 ? 25 : h < 800 ? 29 : 36;
        int neededH = count * targetRowH + Math.max(0, count - 1) * gap + 2 * pad;
        int maxHeight = Math.max(1, h - 30);
        int frameH = Math.min(maxHeight, neededH);
        int desiredY = Math.max(8, (int) Math.round(h * 0.43));
        int y = Math.max(0, Math.min(desiredY, h - 12 - frameH));
        return new Rectangle((w - frameW) / 2, y, frameW, Math.min(frameH, h - y));
    }

    static Rectangle route(Rectangle frame, int routes, int index) {
        if (frame == null || routes <= 0 || index < 0 || index >= routes) return new Rectangle();
        int pad = Math.min(12, Math.max(2, frame.height / 8));
        int gap = 6;
        int rowH = Math.max(1, (frame.height - pad * 2 - Math.max(0, routes - 1) * gap) / routes);
        int x = frame.x + Math.min(18, Math.max(2, frame.width / 12));
        int right = frame.x + frame.width - (x - frame.x);
        int y = frame.y + pad + index * (rowH + gap);
        return new Rectangle(x, y, Math.max(1, right - x), rowH);
    }
}
