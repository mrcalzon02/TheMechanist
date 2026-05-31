package mechanist;

final class OptionsScreenPainter {
    private OptionsScreenPainter() {}

    static final class Layout {
        final int width;
        final int height;
        final int panelX;
        final int panelY;
        final int panelW;
        final int panelH;

        Layout(int width, int height, int panelX, int panelY, int panelW, int panelH) {
            this.width = width;
            this.height = height;
            this.panelX = panelX;
            this.panelY = panelY;
            this.panelW = panelW;
            this.panelH = panelH;
        }
    }

    static Layout layout(GamePanel panel) {
        int width = panel.getWidth();
        int height = panel.getHeight();
        int panelX = Math.max(34, width / 2 - Math.min(620, width / 2 - 34));
        int panelY = Math.max(34, height / 22);
        int panelW = Math.min(width - panelX * 2, Math.max(980, width - 90));
        int panelH = Math.max(430, height - panelY - 64);
        return new Layout(width, height, panelX, panelY, panelW, panelH);
    }

    static java.awt.Rectangle controlsBox(Layout layout) {
        int x = layout.panelX + 44;
        int y = layout.panelY + 108;
        int w = layout.panelW - 88;
        int h = Math.max(116, Math.min(188, layout.panelH / 4));
        return new java.awt.Rectangle(x, y, w, h);
    }

    static java.awt.Rectangle infoBox(Layout layout) {
        java.awt.Rectangle controls = controlsBox(layout);
        int x = layout.panelX + 44;
        int y = controls.y + controls.height + 12;
        int w = layout.panelW - 88;
        int h = Math.max(96, layout.panelY + layout.panelH - y - 50);
        return new java.awt.Rectangle(x, y, w, h);
    }

    static String subtitle(int optionsTab) {
        if (optionsTab == 0) return "Display mode, detected resolution, text density, and interface scale";
        if (optionsTab == 1) return "Text size, text crispness, interface scale, and hover-help density";
        if (optionsTab == 2) return "Sound channels and volumes";
        if (optionsTab == 3) return "Controls and command bindings";
        if (optionsTab == 4) return "Graphics rendering, frame pacing, art quality, motion, and color treatment";
        if (optionsTab == 5) return "RESTART REQUIRED: JVM heap, GC, Java2D pipeline, client/server/thin-client/single-player profiles";
        if (optionsTab == 6) return "Accessibility compatibility, color vision correction, readable text, narration hooks, and reduced motion";
        return "Quality-of-life defaults for storage, construction, logistics, item safety, production, market, and notification friction";
    }
}
