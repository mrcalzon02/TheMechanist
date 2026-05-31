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
}
