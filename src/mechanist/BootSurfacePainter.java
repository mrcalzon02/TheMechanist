package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Stateless extracted rendering surface for boot screen.
 *
 * Source: GamePanel.drawBoot(Graphics2D g)
 */
final class BootSurfacePainter implements ScreenPainter {
    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        bootSurfacePainter.paint(g, this);
    }
}
