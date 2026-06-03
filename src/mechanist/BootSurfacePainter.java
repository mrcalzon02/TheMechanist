package mechanist;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Renders the recovered media-backed boot sequence before the main menu. */
final class BootSurfacePainter implements ScreenPainter {
    @Override
    public void paint(Graphics2D g, GamePanel panel) {
        int w = Math.max(1, panel.getWidth());
        int h = Math.max(1, panel.getHeight());
        long elapsed = Math.max(0L, System.currentTimeMillis() - panel.bootStartMillis);

        ensureBootMediaLoaded(panel);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, w, h);

        drawBackdrop(g, panel, w, h, elapsed);
        drawTitle(g, panel, w, h);
        drawSpinner(g, panel, w, h, elapsed);
        drawBootText(g, panel, w, h, elapsed);

        BootMenuFlowAuthority.maybeAdvanceFromBootPaint(panel, elapsed);
    }

    private void ensureBootMediaLoaded(GamePanel panel) {
        if (panel == null || panel.images == null) return;
        if (!panel.images.bootFrames.isEmpty() && panel.images.get("title_mechanist_rebase") != null) return;
        try {
            panel.images.load(panel.options);
            DebugLog.audit("BOOT_SURFACE", "Loaded boot media lazily bootFrames=" + panel.images.bootFrames.size());
        } catch (Throwable t) {
            DebugLog.error("BOOT_SURFACE", "Failed lazy boot-media load; falling back to text boot surface.", t);
        }
    }

    private void drawBackdrop(Graphics2D g, GamePanel panel, int w, int h, long elapsed) {
        BufferedImage backdrop = panel.images == null ? null : panel.images.get("new_world_backdrop_rebase");
        if (backdrop != null) {
            drawCover(g, backdrop, 0, 0, w, h);
            g.setColor(new Color(0, 0, 0, 128));
            g.fillRect(0, 0, w, h);
        } else {
            GradientPaint gp = new GradientPaint(0, 0, new Color(5, 8, 7), 0, h, new Color(22, 18, 10));
            Paint old = g.getPaint();
            g.setPaint(gp);
            g.fillRect(0, 0, w, h);
            g.setPaint(old);
        }

        BufferedImage slow = panel.images == null ? null : panel.images.get("clouds_slow_rebase");
        BufferedImage fast = panel.images == null ? null : panel.images.get("clouds_fast_rebase");
        drawScrollingLayer(g, slow, w, h, elapsed, 12000L, 48, 0.35f);
        drawScrollingLayer(g, fast, w, h, elapsed, 6500L, -36, 0.28f);

        g.setColor(new Color(0, 0, 0, 92));
        g.fillRect(0, 0, w, h);
    }

    private void drawScrollingLayer(Graphics2D g, BufferedImage img, int w, int h, long elapsed, long period, int yOffset, float alpha) {
        if (img == null || img.getWidth() <= 0 || img.getHeight() <= 0) return;
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
        int drawH = Math.max(h / 2, h);
        int drawW = Math.max(w, (int)Math.round(img.getWidth() * (drawH / (double)Math.max(1, img.getHeight()))));
        int offset = (int)Math.floorMod(elapsed * drawW / Math.max(1L, period), drawW);
        int y = Math.max(-drawH / 4, yOffset);
        for (int x = -offset - drawW; x < w + drawW; x += drawW) g.drawImage(img, x, y, drawW, drawH, null);
        g.setComposite(old);
    }

    private void drawTitle(Graphics2D g, GamePanel panel, int w, int h) {
        BufferedImage title = panel.images == null ? null : first(panel.images.get("title_mechanist_rebase"), panel.images.get("title_mechanist"));
        BufferedImage subtitle = panel.images == null ? null : panel.images.get("subtitle_rebase");
        int titleY = Math.max(42, h / 8);
        if (title != null) {
            Dimension d = panel.mainMenuTitleDrawSize(title, w, h);
            g.drawImage(title, w / 2 - d.width / 2, titleY, d.width, d.height, null);
            titleY += d.height + 18;
        } else {
            g.setFont(panel.titleFont == null ? new Font("Monospaced", Font.BOLD, 42) : panel.titleFont.deriveFont(Font.BOLD, 44f));
            g.setColor(new Color(225, 208, 140));
            drawCentered(g, "THE MECHANIST", w / 2, titleY + 48);
            titleY += 82;
        }
        if (subtitle != null) {
            Dimension d = panel.mainMenuSubtitleDrawSize(subtitle, w, h);
            g.drawImage(subtitle, w / 2 - d.width / 2, titleY, d.width, d.height, null);
        } else {
            g.setFont(panel.smallFont == null ? new Font("Monospaced", Font.BOLD, 14) : panel.smallFont);
            g.setColor(new Color(152, 160, 128));
            drawCentered(g, "RUNTIME INITIALIZATION", w / 2, titleY + 24);
        }
    }

    private void drawSpinner(Graphics2D g, GamePanel panel, int w, int h, long elapsed) {
        BufferedImage frame = null;
        if (panel.images != null && !panel.images.bootFrames.isEmpty()) {
            int idx = (int)((elapsed / 110L) % panel.images.bootFrames.size());
            frame = panel.images.bootFrames.get(idx);
        }
        if (frame == null && panel.images != null) frame = panel.images.get("mechanical_skull_gear_emblem");
        int size = Math.max(96, Math.min(220, Math.min(w, h) / 4));
        int x = w / 2 - size / 2;
        int y = Math.max(h / 2 - size / 2, h / 2 - 80);
        if (frame != null) {
            g.drawImage(frame, x, y, size, size, null);
        } else {
            g.setColor(new Color(185, 225, 150));
            g.setStroke(new BasicStroke(4f));
            int arc = (int)((elapsed / 8L) % 360L);
            g.drawArc(x, y, size, size, arc, 260);
            g.drawOval(x + size / 4, y + size / 4, size / 2, size / 2);
        }
    }

    private void drawBootText(Graphics2D g, GamePanel panel, int w, int h, long elapsed) {
        g.setFont(panel.smallFont == null ? new Font("Monospaced", Font.BOLD, 14) : panel.smallFont);
        String[] steps = {
                "loading cogitator frame slices",
                "binding tile art registries",
                "indexing portrait pools",
                "arming menu authorities",
                "opening main menu"
        };
        int step = Math.min(steps.length - 1, (int)(elapsed / 650L));
        String dots = switch ((int)((elapsed / 250L) % 4L)) { case 1 -> ".."; case 2 -> "..."; case 3 -> "...."; default -> "."; };
        g.setColor(new Color(170, 205, 145));
        drawCentered(g, "boot sequence // " + steps[step] + dots, w / 2, h - 92);
        g.setColor(new Color(120, 145, 120));
        drawCentered(g, "press Enter / Space to skip", w / 2, h - 64);
    }

    private static BufferedImage first(BufferedImage a, BufferedImage b) { return a != null ? a : b; }

    private void drawCover(Graphics2D g, BufferedImage img, int x, int y, int w, int h) {
        double scale = Math.max(w / (double)Math.max(1, img.getWidth()), h / (double)Math.max(1, img.getHeight()));
        int dw = Math.max(1, (int)Math.round(img.getWidth() * scale));
        int dh = Math.max(1, (int)Math.round(img.getHeight() * scale));
        g.drawImage(img, x + w / 2 - dw / 2, y + h / 2 - dh / 2, dw, dh, null);
    }

    private void drawCentered(Graphics2D g, String text, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        String s = text == null ? "" : text;
        g.drawString(s, x - fm.stringWidth(s) / 2, y);
    }
}
