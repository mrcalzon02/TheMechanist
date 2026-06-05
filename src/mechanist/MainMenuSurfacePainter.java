package mechanist;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

final class MainMenuSurfacePainter implements ScreenPainter {
    private static final String[] ROUTE_KEYS = {
            "menu.main.route.new_game",
            "menu.main.route.continue",
            "menu.main.route.load_game",
            "menu.main.route.options",
            "menu.main.route.infopedia",
            "menu.main.route.multiplayer",
            "menu.main.route.tools",
            "menu.main.route.exit"
    };

    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        int W = panel.getWidth();
        int H = panel.getHeight();

        BufferedImage backdrop = panel.images.get("new_world_backdrop_rebase");
        if (backdrop != null) {
            drawCover(g, backdrop, 0, 0, W, H);
            g.setColor(new Color(0, 0, 0, 88));
            g.fillRect(0, 0, W, H);
        }
        
        BufferedImage title = panel.images.get("title_mechanist_rebase");
        if (title == null) title = panel.images.get("title_mechanist");
        int cx = W / 2;
        int titleTop = panel.mainMenuTitleTop(H);
        int drawnTitleBottom = titleTop + 112;
        Rectangle buttonFrame = panel.mainMenuButtonFrameRect();
        
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
            panel.center(g, MenuTextAuthority.text("menu.main.title", "THE MECHANIST"), cx, titleTop + 68);
        }
        
        BufferedImage subtitle = panel.images.get("subtitle_rebase");
        if (subtitle != null) {
            Dimension subSize = panel.mainMenuSubtitleDrawSize(subtitle, W, H);
            int sw = subSize.width;
            int sh = subSize.height;
            int minSubtitleW = Math.max(buttonFrame.width, Math.min(W - 48, buttonFrame.width));
            if (sw < minSubtitleW && subtitle.getWidth() > 0) {
                sw = Math.min(W - 48, minSubtitleW);
                sh = Math.max(1, (int)Math.round(subtitle.getHeight() * (sw / (double) subtitle.getWidth())));
            }
            sh = Math.min(sh, Math.max(40, H / 7));
            int sy = drawnTitleBottom + 3;
            g.drawImage(subtitle, cx - sw / 2, sy, sw, sh, null);
            panel.stampUiFrameId(g, "I", "subtitle", cx - sw / 2, sy, sw, sh);
        }
        
        g.setColor(new Color(0, 0, 0, 172));
        g.fillRoundRect(buttonFrame.x, buttonFrame.y, buttonFrame.width, buttonFrame.height, 14, 14);
        panel.drawSlicedFrame(g, buttonFrame.x, buttonFrame.y, buttonFrame.width, buttonFrame.height, "inner");
        panel.stampUiFrameId(g, "F", "main-menu-route-frame", buttonFrame.x, buttonFrame.y, buttonFrame.width, buttonFrame.height);
        MenuTextAuthority.drawMenuReference(g, panel, buttonFrame, "M001", "menu.main.title", "Main Menu");

        java.util.List<String> labels = panel.mainMenuRouteLabels();
        g.setFont(panel.uiFont.deriveFont(Font.BOLD, Math.max(17f, Math.min(23f, H / 32f))));
        FontMetrics routeFm = g.getFontMetrics();
        for (int i = 0; i < labels.size(); i++) {
            Rectangle r = panel.mainMenuRouteRect(i);
            boolean selected = i == panel.selectedButton || r.contains(panel.mouseX, panel.mouseY);
            BufferedImage button = panel.images.get(selected ? "button_hover" : "button_normal");
            if (button != null) {
                g.drawImage(button, r.x, r.y, r.width, r.height, null);
            } else {
                g.setColor(selected ? new Color(64, 52, 26, 220) : new Color(24, 24, 20, 215));
                g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
                g.setColor(selected ? new Color(220, 190, 92, 230) : new Color(112, 95, 54, 190));
                g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
            }
            String fallbackLabel = labels.get(i);
            String label = MenuTextAuthority.text(i < ROUTE_KEYS.length ? ROUTE_KEYS[i] : "", fallbackLabel);
            BufferedImage icon = panel.systemButtonIconForLabel(fallbackLabel);
            int iconSize = icon == null ? 0 : Math.max(36, Math.min((int)Math.round(r.height * 1.16), 57));
            int iconX = r.x + 12;
            int textX = r.x + 18;
            int textW = r.width - 36;
            if (icon != null) {
                int iconY = r.y + (r.height - iconSize) / 2;
                g.drawImage(icon, iconX, iconY, iconSize, iconSize, null);
                textX = iconX + iconSize + 15;
                textW = Math.max(80, r.x + r.width - textX - 16);
            }
            g.setColor(selected ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : panel.optionColor(GameOptions.TEXT_MAIN));
            g.drawString(GuiLayoutApi.fitLabel(label, routeFm, textW), textX, r.y + (r.height + routeFm.getAscent() - routeFm.getDescent()) / 2);
        }
        
        java.util.List<String> shellLines = panel.launcherShell.displayLines(panel.launcherRuntime, panel.userProfile);
        int panelW = Math.min(W - 220, 245);
        int panelH = Math.max(22, Math.min(26, H / 24));
        int panelX = (W - panelW) / 2;
        int panelY = Math.max(8, Math.min(H - panelH - 10, buttonFrame.y - panelH - 10));
        
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

    private static void drawCover(Graphics2D g, BufferedImage img, int x, int y, int w, int h) {
        if (g == null || img == null || w <= 0 || h <= 0 || img.getWidth() <= 0 || img.getHeight() <= 0) return;
        double scale = Math.max(w / (double) img.getWidth(), h / (double) img.getHeight());
        int dw = Math.max(1, (int)Math.round(img.getWidth() * scale));
        int dh = Math.max(1, (int)Math.round(img.getHeight() * scale));
        int dx = x + (w - dw) / 2;
        int dy = y + (h - dh) / 2;
        g.drawImage(img, dx, dy, dw, dh, null);
    }
}
