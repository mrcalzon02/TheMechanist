package mechanist;

import java.awt.Rectangle;

/** Pixel-contained geometry for the compact, custom-painted options surface. */
final class OptionsLayoutAuthority {
    static final int TAB_COUNT = 8;
    static final int PAD = 8;
    static final int GAP = 6;
    static final int TAB_HEIGHT = 28;
    static final int COMMAND_HEIGHT = 30;
    static final int HEADER_HEIGHT = 54;
    static final int FOOTER_HEIGHT = 16;

    record Layout(int viewportWidth, int viewportHeight, Rectangle panel, Rectangle controls,
                  Rectangle details, int tabColumns, int commandColumns, boolean commandsFit) { }

    private OptionsLayoutAuthority() { }

    static Layout measure(int width, int height, int commandCount, int detailLines) {
        int screenW = Math.max(1, width);
        int screenH = Math.max(1, height);
        int horizontalMargin = Math.min(30, Math.max(8, screenW / 40));
        int verticalMargin = Math.min(24, Math.max(8, screenH / 30));
        int panelWidth = Math.max(1, Math.min(1100, screenW - horizontalMargin * 2));
        int innerWidth = Math.max(1, panelWidth - 32);
        int tabColumns = innerWidth >= 660 ? 8 : innerWidth >= 210 ? 4 : 2;
        int commandColumns = innerWidth >= 880 ? 4 : innerWidth >= 530 ? 3
                : innerWidth >= 230 ? 2 : 1;
        int tabRows = (TAB_COUNT + tabColumns - 1) / tabColumns;
        int commandRows = Math.max(1, (Math.max(0, commandCount) + commandColumns - 1) / commandColumns);
        int tabsHeight = tabRows * TAB_HEIGHT + (tabRows - 1) * GAP;
        int commandsHeight = commandRows * COMMAND_HEIGHT + (commandRows - 1) * GAP;
        int requiredControlsHeight = PAD + tabsHeight + PAD + commandsHeight + PAD;
        int targetDetailsHeight = Math.max(70, Math.min(260, Math.max(0, detailLines) * 19 + 18));
        int availableHeight = Math.max(1, screenH - verticalMargin * 2);
        int controlsBudget = Math.max(0, availableHeight - HEADER_HEIGHT - FOOTER_HEIGHT);
        boolean commandsFit = requiredControlsHeight <= controlsBudget;
        int controlsHeight = Math.min(requiredControlsHeight, controlsBudget);
        int detailsHeight = commandsFit
                ? Math.max(0, Math.min(targetDetailsHeight,
                        availableHeight - HEADER_HEIGHT - FOOTER_HEIGHT - controlsHeight - GAP))
                : 0;
        if (detailsHeight < 34) detailsHeight = 0;
        int panelHeight = Math.min(availableHeight, HEADER_HEIGHT + controlsHeight + FOOTER_HEIGHT
                + (detailsHeight > 0 ? GAP + detailsHeight : 0));
        int panelX = Math.max(0, (screenW - panelWidth) / 2);
        int panelY = Math.max(0, (screenH - panelHeight) / 2);
        Rectangle panel = new Rectangle(panelX, panelY, panelWidth, panelHeight);
        Rectangle controls = new Rectangle(panelX + 16, panelY + HEADER_HEIGHT,
                innerWidth, controlsHeight);
        Rectangle details = new Rectangle(panelX + 16, controls.y + controlsHeight + GAP,
                innerWidth, detailsHeight);
        return new Layout(screenW, screenH, panel, controls, details,
                tabColumns, commandColumns, commandsFit);
    }
}
