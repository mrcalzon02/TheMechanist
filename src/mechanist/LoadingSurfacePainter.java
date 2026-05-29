package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Stateless extracted rendering surface for loading screen.
 *
 * Source: GamePanel.drawLoading(Graphics2D g)
 */
final class LoadingSurfacePainter implements ScreenPainter {
    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        loadingSurfacePainter.paint(g, this);
    }
}
