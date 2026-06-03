package mechanist;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

final class MultiplayerSurfacePainter implements ScreenPainter {
    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        int W = panel.getWidth();
        Rectangle mainPanel = panel.multiplayerMenuPanelRect();
        Rectangle content = panel.multiplayerContentRect(mainPanel);
        Rectangle action = panel.multiplayerActionRect(mainPanel);
        
        g.setColor(new Color(0, 0, 0, 196));
        g.fillRoundRect(mainPanel.x, mainPanel.y, mainPanel.width, mainPanel.height, 16, 16);
        panel.drawSlicedFrame(g, mainPanel.x, mainPanel.y, mainPanel.width, mainPanel.height, "inner");
        panel.stampUiFrameId(g, "F", "multiplayer-menu", mainPanel.x, mainPanel.y, mainPanel.width, mainPanel.height);
        
        g.setFont(panel.titleFont.deriveFont(Font.BOLD, Math.max(24f, Math.min(38f, mainPanel.height / 12f))));
        g.setColor(panel.optionColor(GameOptions.TEXT_HIGHLIGHT));
        panel.center(g, "MULTIPLAYER CONNECTION", W / 2, mainPanel.y + 42);
        g.setFont(panel.smallFont);
        FontMetrics fm = g.getFontMetrics();

        g.setColor(new Color(12, 14, 13, 226));
        g.fillRoundRect(content.x, content.y, content.width, content.height, 12, 12);
        g.setColor(new Color(145, 118, 64, 135));
        g.drawRoundRect(content.x, content.y, content.width, content.height, 12, 12);
        
        g.setColor(new Color(12, 14, 13, 220));
        g.fillRoundRect(action.x, action.y, action.width, action.height, 12, 12);
        g.setColor(new Color(145, 118, 64, 125));
        g.drawRoundRect(action.x, action.y, action.width, action.height, 12, 12);

        int leftX = content.x + 14;
        int textW = Math.max(240, content.width - 28);
        int y = content.y + 26;
        Shape oldClip = g.getClip();
        
        g.setClip(content.x + 8, content.y + 8, content.width - 16, content.height - 16);
        g.setColor(panel.optionColor(GameOptions.TEXT_HIGHLIGHT));
        g.drawString("Direct address:", leftX, y); y += fm.getHeight() + 4;
        
        String direct = (panel.multiplayerMenu.inputActive() ? "> " : "  ") + panel.multiplayerMenu.directInput() + (panel.multiplayerMenu.inputActive() && (System.currentTimeMillis() / 450) % 2 == 0 ? "_" : "");
        g.setColor(new Color(18, 22, 20, 230));
        g.fillRoundRect(leftX, y - fm.getAscent() - 5, textW, fm.getHeight() + 12, 8, 8);
        g.setColor(panel.multiplayerMenu.inputActive() ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : panel.optionColor(GameOptions.TEXT_MAIN));
        g.drawString(GuiLayoutApi.fitLabel(direct, fm, Math.max(220, textW - 12)), leftX + 8, y);
        y += fm.getHeight() + 24;
        
        g.setColor(panel.optionColor(GameOptions.TEXT_DIM));
        g.drawString("IPv4 example: 192.168.1.10:25565", leftX, y); y += fm.getHeight();
        g.drawString("IPv6 example: [2001:db8::1]:25565", leftX, y); y += fm.getHeight() + 18;
        
        g.setColor(panel.optionColor(GameOptions.TEXT_HIGHLIGHT));
        g.drawString("Recent servers:", leftX, y); y += fm.getHeight() + 4;
        
        java.util.List<MultiplayerMenuController.ConnectionHistoryItem> history = panel.multiplayerMenu.history();
        for (int i = 0; i < Math.min(5, history.size()); i++) {
            boolean selected = i == panel.multiplayerMenu.historyIndex();
            g.setColor(selected ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : panel.optionColor(GameOptions.TEXT_MAIN));
            g.drawString((selected ? "> " : "  ") + GuiLayoutApi.fitLabel(history.get(i).shortLine(), fm, Math.max(220, textW - 8)), leftX, y);
            y += fm.getHeight() + 2;
        }
        if (history.isEmpty()) { 
            g.setColor(panel.optionColor(GameOptions.TEXT_DIM)); 
            g.drawString("  No connection history yet.", leftX, y); 
            y += fm.getHeight() + 2; 
        }
        y += 14;
        
        g.setColor(panel.optionColor(GameOptions.TEXT_HIGHLIGHT));
        g.drawString("Favorites:", leftX, y); y += fm.getHeight() + 4;
        
        java.util.List<MultiplayerMenuController.FavoriteServer> favs = panel.multiplayerMenu.favorites();
        for (int i = 0; i < Math.min(5, favs.size()); i++) {
            boolean selected = i == panel.multiplayerMenu.favoriteIndex();
            g.setColor(selected ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : panel.optionColor(GameOptions.TEXT_MAIN));
            g.drawString((selected ? "> " : "  ") + GuiLayoutApi.fitLabel(favs.get(i).shortLine(), fm, Math.max(220, textW - 8)), leftX, y);
            y += fm.getHeight() + 2;
        }
        if (favs.isEmpty()) { 
            g.setColor(panel.optionColor(GameOptions.TEXT_DIM)); 
            g.drawString("  No favorite servers saved.", leftX, y); 
            y += fm.getHeight() + 2; 
        }

        int statusY = Math.max(y + 14, content.y + content.height - 74);
        g.setColor(new Color(18, 22, 20, 225));
        g.fillRoundRect(leftX, statusY, textW, 60, 10, 10);
        g.setColor(new Color(145, 118, 64, 120));
        g.drawRoundRect(leftX, statusY, textW, 60, 10, 10);
        
        g.setColor(panel.optionColor(GameOptions.TEXT_DIM));
        java.util.List<String> wrapped = GuiLayoutApi.wrapText(panel.multiplayerMenu.status(), fm, Math.max(220, textW - 18));
        int yy = statusY + 20;
        for (int i = 0; i < Math.min(2, wrapped.size()); i++) { 
            g.drawString(wrapped.get(i), leftX + 9, yy); 
            yy += fm.getHeight(); 
        }
        g.setColor(panel.multiplayerMenu.hasActiveHost() ? panel.optionColor(GameOptions.TEXT_HIGHLIGHT) : panel.optionColor(GameOptions.TEXT_DIM));
        g.drawString(GuiLayoutApi.fitLabel(panel.multiplayerMenu.activeHostLine(), fm, Math.max(220, textW - 18)), leftX + 9, statusY + 50);
        g.setClip(oldClip);

        g.setFont(panel.smallFont.deriveFont(Font.BOLD, Math.max(9f, panel.smallFont.getSize2D() - 2f)));
        g.setColor(panel.optionColor(GameOptions.TEXT_HIGHLIGHT));
        panel.center(g, "CONNECTION COMMANDS", action.x + action.width / 2, action.y + 22);
        g.setFont(panel.smallFont.deriveFont(Font.PLAIN, Math.max(9f, panel.smallFont.getSize2D() - 3f)));
        g.setColor(panel.optionColor(GameOptions.TEXT_MAIN));
        panel.center(g, "I/E Edit  Enter Join  H Host  F Save Favorite  G Join Favorite  Arrows Select  T Steam  Esc Back",
                action.x + action.width / 2, action.y + 42);
    }
}
