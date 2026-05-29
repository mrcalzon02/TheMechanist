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
        long elapsed = System.currentTimeMillis() - bootStartMillis;
        int w = panel.getWidth(), h = panel.getHeight();
        g.setColor(new Color(4, 6, 5)); g.fillRect(0,0,w,h);
        g.setColor(new Color(13, 22, 16));
        for (int y=0;y<h;y+=18) g.drawLine(0,y,w,y);
        for (int x=0;x<w;x+=18) g.drawLine(x,0,x,h);
        panel.drawSlicedFrame(g, 24, 24, w-48, h-48, "outer");
        int bootPanelX = Math.max(72, w / 10);
        int bootPanelY = Math.max(188, h / 3);
        int bootPanelW = Math.max(320, w - bootPanelX * 2);
        int bootPanelH = Math.max(170, h - bootPanelY - 130);
        panel.drawSlicedFrame(g, bootPanelX, bootPanelY, bootPanelW, bootPanelH, "inner");
        
        int cx = w/2;
        g.setFont(titleFont); g.setColor(new Color(210, 190, 130));
        panel.center(g, "THE MECHANIST", cx, 78);
        panel.drawBootGear(g, Math.min(w-100, cx + 260), 82, Math.max(68, Math.min(104, h/6)), elapsed);
        g.setFont(uiFont); g.setColor(new Color(145, 214, 145));
        panel.center(g, "UNDERHIVE LOGIC ENGINE NODE // COLD WAKE SEQUENCE", cx, 122);
        
        String[] lines = {
            "[0001] Power feed accepted. Brownout ghosts contained.",
            "[0002] Memory shrine cycling. Three sectors protest. Ignore them.",
            "[0003] Loading underhive cartography seed lattice.",
            "[0004] Establishing hostile faction ledger and bribe tolerances.",
            "[0005] Testing fog-of-war auspex and line-of-sight obstruction.",
            "[0006] Spooling hearing model. Noisy machinery granted veto authority.",
            "[0007] Inspecting imported cog, button, portrait, and machine-icon relics.",
            "[0008] WARNING: Legal ownership paperwork not found. This is normal.",
            "[0009] Awaiting doomed operator input. Mercy module unavailable."
        };
        int bx = bootPanelX + 28, by = bootPanelY + 46;
        int bootTextBottom = h - 112;
        g.setFont(smallFont);
        long cursor = elapsed;
        long lineDelay = 900;       // deliberate dramatic pause between boot lines
        long charDelay = 38;        // slower typewriter cadence
        for (int i=0;i<lines.length;i++) {
            long lineStart = i * lineDelay;
            if (cursor < lineStart) break;
            int chars = (int)Math.min(lines[i].length(), Math.max(0, (cursor-lineStart)/charDelay));
            String shown = lines[i].substring(0, Math.max(0, Math.min(chars, lines[i].length())));
            boolean activeLine = chars < lines[i].length();
            if (activeLine && ((elapsed/420)%2)==0) shown += "_";
            g.setColor(activeLine ? new Color(235, 210, 125) : new Color(160, 220, 150));
            int yy = by + i*26;
            if (yy < bootTextBottom) panel.drawUiTextLine(g, shown, bx, yy);
        }
        int barW = w - 220;
        double totalBoot = 15200.0;
        int fill = Math.min(barW, (int)(barW * Math.min(1.0, elapsed / totalBoot)));
        int barY = h - 78;
        g.setColor(new Color(18,18,18)); g.fillRect(110, barY, barW, 24);
        g.setColor(new Color(120, 104, 72)); g.drawRect(110, barY, barW, 24);
        g.setColor(new Color(150, 220, 130)); g.fillRect(112, barY+2, Math.max(0, fill-4), 20);
        g.setFont(smallFont); g.setColor(new Color(205,195,160));
        panel.center(g, "ENTER / SPACE / ESC skips boot. Automatic transfer follows checksum completion.", cx, barY - 16);
        if (elapsed > 15800) panel.finishBootSequence("auto-checksum");
    }
}
