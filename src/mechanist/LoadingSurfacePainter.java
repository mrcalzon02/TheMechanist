package mechanist;

import java.awt.*;

/**
 * Artwork-backed loading transitions. Reuses the boot painter's real packaged
 * background, title, cloud and spinner bindings without advancing the boot
 * state, so zone/capture transitions cannot silently become text-only screens.
 */
final class LoadingSurfacePainter implements ScreenPainter {
    private final BootSurfacePainter art = new BootSurfacePainter();

    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        int w = Math.max(1, panel.getWidth());
        int h = Math.max(1, panel.getHeight());
        long elapsed = Math.max(0L, System.currentTimeMillis() - panel.bootStartMillis);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);
        art.drawBackdrop(g, panel, w, h, elapsed);
        art.drawTitle(g, panel, w, h);
        art.drawSpinner(g, panel, w, h, elapsed);

        g.setFont(panel.titleFont == null ? new Font("Monospaced", Font.BOLD, 32)
                : panel.titleFont.deriveFont(Font.BOLD,
                        Math.max(24f, Math.min(40f, h / 16f))));
        g.setColor(new Color(225, 207, 148));
        EarlyScreenTextAuthority.drawCentered(g, "LOADING", w / 2,
                Math.max(90, h - 118), Math.max(1, w - 48));

        g.setFont(panel.smallFont == null ? new Font("Monospaced", Font.BOLD, 14)
                : panel.smallFont);
        g.setColor(new Color(168, 190, 145));
        EarlyScreenTextAuthority.drawCentered(g, "please stand by", w / 2,
                Math.max(120, h - 82), Math.max(1, w - 48));
    }
}
