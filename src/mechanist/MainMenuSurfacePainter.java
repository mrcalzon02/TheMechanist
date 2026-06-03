package mechanist;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

final class MainMenuSurfacePainter implements ScreenPainter {
    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        int W = panel.getWidth();
        int H = panel.getHeight();
        
        BufferedImage title = panel.images.get("title_mechanist_rebase");
        if (title == null) title = panel.images.get("title_mechanist");
        int cx = W / 2;
        int titleTop = panel.mainMenuTitleTop(H);
        int drawnTitleBottom = titleTop + 112;
        
        if (title != null) {
            Dimension titleSize = panel.mainMenuTitleDrawSize(title, W, H);
            int tw = titleSize.width;
            int th = titleSize.height;
            g.drawImage(title, cx - tw / 2, titleTop, tw, th, null);
            panel.stampUiFrameId(g, "I", "title-mechanist", cx - tw / 2, titleTop, tw, th);
            drawnTitleBottom = titleTop + th;
        } else {
            g.setFont(panel.titleFont.deriveFont(Font.BOLD, Math.max(34f, Math.min(56f, H / 9f))));
            g.setColor(new Color(200, 184, 132));
            panel.center(g, "THE MECHANIST", cx, titleTop + 68);
        }
        
        BufferedImage subtitle = panel.images.get("subtitle_rebase");
        if (subtitle != null) {
            Dimension subSize = panel.mainMenuSubtitleDrawSize(subtitle, W, H);
            int sw = subSize.width;
            int sh = subSize.height;
            int sy = drawnTitleBottom + 3;
            g.drawImage(subtitle, cx - sw / 2, sy, sw, sh, null);
            panel.stampUiFrameId(g, "I", "subtitle", cx - sw / 2, sy, sw, sh);
        }
        
        Rectangle buttonFrame = panel.mainMenuButtonFrameRect();
        g.setColor(new Color(0, 0, 0, 172));
        g.fillRoundRect(buttonFrame.x, buttonFrame.y, buttonFrame.width, buttonFrame.height, 14, 14);
        panel.drawSlicedFrame(g, buttonFrame.x, buttonFrame.y, buttonFrame.width, buttonFrame.height, "inner");
        panel.stampUiFrameId(g, "F", "main-menu-route-frame", buttonFrame.x, buttonFrame.y, buttonFrame.width, buttonFrame.height);
        
        java.util.List<String> shellLines = panel.launcherShell.displayLines(panel.launcherRuntime, panel.userProfile);
        int panelW = Math.min(W - 220, 245);
        int panelH = Math.max(22, Math.min(26, H / 24));
        int panelX = (W - panelW) / 2;
        int panelY = H - panelH - 38;
        
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(panelX, panelY, panelW, panelH, 9, 9);
        g.setColor(new Color(130, 105, 55, 150));
        g.drawRoundRect(panelX, panelY, panelW, panelH, 9, 9);
        panel.stampUiFrameId(g, "F", "launcher-runtime-compact", panelX, panelY, panelW, panelH);
        
        g.setFont(panel.smallFont.deriveFont(Math.max(7f, Math.min(8.5f, panel.smallFont.getSize2D() - 4f))));
        FontMetrics fm = g.getFontMetrics();
        int lineY = panelY + 14;
        int maxLines = Math.max(1, (panelH - 8) / Math.max(10, fm.getHeight()));
        
        for (int i = 0; i < shellLines.size() && i < maxLines; i++) {
            g.setColor(i == 0 ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : panel.optionColor(GameOptions.TEXT_DIM));
            panel.center(g, GuiLayoutApi.fitLabel(shellLines.get(i), fm, panelW - 20), cx, lineY);
            lineY += Math.max(10, fm.getHeight());
        }
        g.setFont(panel.smallFont);
    }
}
