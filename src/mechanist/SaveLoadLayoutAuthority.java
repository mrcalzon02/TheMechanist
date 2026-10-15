package mechanist;

import java.awt.Rectangle;

/** Shared pixel geometry for save slots, labels, and mouse/keyboard command hitboxes. */
final class SaveLoadLayoutAuthority {
    static final int SLOT_HEIGHT = 44;
    static final int SLOT_GAP = 6;

    record Layout(Rectangle panel, Rectangle status, Rectangle footer, Rectangle back,
                  Rectangle delete, Rectangle newGame, int slotTop, int rowWidth, boolean fits) {
        Rectangle slot(int index) {
            return !fits || index < 0 ? new Rectangle() :
                    new Rectangle(panel.x + 18, slotTop + index * (SLOT_HEIGHT + SLOT_GAP), rowWidth, SLOT_HEIGHT);
        }
    }

    private SaveLoadLayoutAuthority() { }

    static Layout measure(int viewportWidth, int viewportHeight, int slotCount) {
        int screenW = Math.max(1, viewportWidth);
        int screenH = Math.max(1, viewportHeight);
        int marginX = Math.min(24, Math.max(8, screenW / 32));
        int marginY = Math.min(22, Math.max(8, screenH / 30));
        int width = Math.max(1, Math.min(660, screenW - 2 * marginX));
        int rows = Math.max(0, slotCount);
        int slotBlockHeight = rows == 0 ? 0 : rows * SLOT_HEIGHT + (rows - 1) * SLOT_GAP;
        boolean narrowButtons = width < 510;
        int footerStart = 76 + slotBlockHeight + 8;
        int buttonsTop = footerStart + 32 + 8;
        int desiredHeight = buttonsTop + (narrowButtons ? 74 : 34) + 14;
        int availableHeight = Math.max(1, screenH - 2 * marginY);
        boolean fits = width >= 280 && availableHeight >= desiredHeight;
        int height = Math.min(desiredHeight, availableHeight);
        int left = Math.max(0, (screenW - width) / 2);
        int top = Math.max(0, (screenH - height) / 2);
        Rectangle panel = new Rectangle(left, top, width, height);
        Rectangle status = new Rectangle(left + 18, top + 43, Math.max(1, width - 36), 28);
        Rectangle footer = new Rectangle(left + 18, top + footerStart, Math.max(1, width - 36), 32);
        Rectangle back, delete, newGame;
        if (!fits) {
            back = new Rectangle(left + 12, top + Math.max(0, height - 47), Math.min(144, Math.max(1, width - 24)), 34);
            delete = new Rectangle();
            newGame = new Rectangle();
        } else if (narrowButtons) {
            int firstWidth = Math.max(1, (width - 44) / 2);
            back = new Rectangle(left + 18, top + buttonsTop, firstWidth, 34);
            delete = new Rectangle(left + 26 + firstWidth, top + buttonsTop, firstWidth, 34);
            newGame = new Rectangle(left + 18, top + buttonsTop + 40, width - 36, 34);
        } else {
            int buttonWidth = Math.max(1, (width - 52) / 3);
            back = new Rectangle(left + 18, top + buttonsTop, buttonWidth, 34);
            delete = new Rectangle(left + 26 + buttonWidth, top + buttonsTop, buttonWidth, 34);
            newGame = new Rectangle(left + 34 + buttonWidth * 2, top + buttonsTop, width - 52 - buttonWidth * 2, 34);
        }
        return new Layout(panel, status, footer, back, delete, newGame,
                top + 76, Math.max(1, width - 36), fits);
    }
}
