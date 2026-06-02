package mechanist;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

final class UiHoverHelpPainter {
    private UiHoverHelpPainter() {}

    static void drawHoverHelp(GamePanel panel, Graphics2D g, int hoverButtonIndex) {
        if (!panel.options.hoverHelp) return;
        String help = null;
        if (hoverButtonIndex >= 0 && hoverButtonIndex < panel.buttons.size()) help = panel.buttons.get(hoverButtonIndex).tip;
        else if (panel.mouseX < 0 && panel.selectedButton >= 0 && panel.selectedButton < panel.buttons.size() && panel.buttonIsModalInteractive(panel.buttons.get(panel.selectedButton))) help = panel.buttons.get(panel.selectedButton).tip;
        if (help == null || panel.mouseX < 0 || panel.mouseY < 0) return;
        g.setFont(panel.smallFont);
        List<String> lines = TextSurfaceApi.wrap(help, 34);
        FontMetrics fm = g.getFontMetrics();
        int tw = 0;
        for (String line : lines) tw = Math.max(tw, fm.stringWidth(line));
        int boxW = Math.min(340, Math.max(190, tw + 22));
        int lineH = fm.getHeight();
        int boxH = Math.max(34, lines.size() * lineH + 14);
        int x = panel.mouseX + 18;
        int y = panel.mouseY + 18;
        if (x + boxW > panel.getWidth() - 12) x = panel.mouseX - boxW - 18;
        if (y + boxH > panel.getHeight() - 12) y = panel.mouseY - boxH - 18;
        x = Math.max(8, x);
        y = Math.max(8, y);
        g.setColor(new Color(8, 8, 9, 238));
        g.fillRect(x, y, boxW, boxH);
        g.setColor(new Color(165, 138, 82));
        g.drawRect(x, y, boxW, boxH);
        g.setColor(new Color(220, 205, 160));
        int ty = y + 18;
        for (String line : lines) {
            UiTextSurfacePainter.drawUiTextLine(g, line, x + 11, ty);
            ty += lineH;
        }
    }
}
