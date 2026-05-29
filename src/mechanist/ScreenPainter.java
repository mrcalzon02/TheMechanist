package mechanist;

import java.awt.Graphics2D;

/**
 * Universal contract for stateless immediate-mode UI rendering surfaces.
 */
interface ScreenPainter {
    void paint(Graphics2D g, GamePanel panel);
}
