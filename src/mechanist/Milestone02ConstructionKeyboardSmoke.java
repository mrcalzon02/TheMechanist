package mechanist;

import java.awt.event.KeyEvent;

final class Milestone02ConstructionKeyboardSmoke {
    public static void main(String[] args) {
        GamePanel panel = new GamePanel();
        panel.timer.stop();
        panel.screen = GamePanel.Screen.PANEL;
        panel.panelMode = GamePanel.PanelMode.BUILD;
        panel.buildRecipeCategoryIndex = 0;
        panel.buildRecipePage = 0;
        require(panel.visibleBuildRecipeLabel(0, BuildRecipe.storage()).startsWith("1. "), "first visible build row should show the 1 shortcut");
        require(panel.visibleBuildRecipeLabel(9, BuildRecipe.storage()).startsWith("0. "), "tenth visible build row should show the 0 shortcut");
        panel.pendingBuildRecipe = BuildRecipe.storage();
        require(panel.visibleBuildRecipeLabel(0, BuildRecipe.storage()).startsWith("> 1. "), "selected build row should show the active blueprint marker");
        panel.pendingBuildRecipe = null;
        int beforeSelectEvents = panel.eventLog.size();
        press(panel, KeyEvent.VK_1);
        require(panel.pendingBuildRecipe != null, "number key should select the first visible construction blueprint");
        require(panel.buildPlacementActive, "number key selection should enter construction placement");
        require(panel.buildX == panel.playerX && panel.buildY == panel.playerY, "number key selection should target the player tile");
        require(panel.eventLog.size() >= beforeSelectEvents + 2, "blueprint selection should log selection and initial target status");
        String initialPlacement = panel.eventLog.get(panel.eventLog.size() - 1);
        require(initialPlacement.contains("Construction placement target: " + panel.playerX + "," + panel.playerY), "initial placement log should name the player tile target");
        require(initialPlacement.contains("initial placement"), "initial placement log should name the initial placement route");
        BuildRecipe firstRecipe = panel.pendingBuildRecipe;
        panel.cancelBuildPlacement("smoke reset");

        panel.buildRecipeCategoryIndex = 0;
        panel.buildRecipePage = 1;
        pressShift(panel, KeyEvent.VK_TAB);
        require(panel.buildRecipeCategoryIndex == ConstructionCategoryAuthority.CATEGORIES.length - 1, "Shift+Tab should cycle construction category backward");
        require(panel.buildRecipePage == 0, "backward category cycling should reset blueprint page");

        panel.buildRecipeCategoryIndex = 0;
        panel.buildRecipePage = 0;
        press(panel, KeyEvent.VK_C);
        require(panel.buildRecipeCategoryIndex == 1, "C should cycle construction category");
        require(panel.buildRecipePage == 0, "category cycling should reset blueprint page");

        panel.buildRecipeCategoryIndex = 0;
        panel.buildRecipePage = 0;
        press(panel, KeyEvent.VK_PAGE_DOWN);
        require(panel.buildRecipePage == 1, "Page Down should advance construction blueprint page");
        press(panel, KeyEvent.VK_END);
        require(panel.buildRecipePage == panel.constructionBlueprintPageCount() - 1, "End should jump to the final construction blueprint page");
        press(panel, KeyEvent.VK_HOME);
        require(panel.buildRecipePage == 0, "Home should jump to the first construction blueprint page");
        press(panel, KeyEvent.VK_PAGE_DOWN);
        require(panel.buildRecipePage == 1, "Page Down should advance after Home jump");
        press(panel, KeyEvent.VK_1);
        require(panel.pendingBuildRecipe != null && panel.pendingBuildRecipe != firstRecipe, "number key should select from the active construction page");
        panel.cancelBuildPlacement("smoke reset");
        press(panel, KeyEvent.VK_PAGE_UP);
        require(panel.buildRecipePage == 0, "Page Up should return to previous construction blueprint page");

        panel.pendingBuildRecipe = BuildRecipe.storage();
        panel.buildPlacementActive = true;
        panel.targetBuildPlacementAt(7, 9, "smoke");
        require(panel.buildX == 7 && panel.buildY == 9, "mouse/helper placement targeting should move build cursor");
        int beforeKeyboardMoveEvents = panel.eventLog.size();
        press(panel, KeyEvent.VK_RIGHT);
        require(panel.buildX == 8 && panel.buildY == 9, "keyboard placement movement should move build cursor");
        require(panel.eventLog.size() > beforeKeyboardMoveEvents, "keyboard placement movement should log target status");
        String keyboardMove = panel.eventLog.get(panel.eventLog.size() - 1);
        require(keyboardMove.contains("Construction placement target: 8,9"), "keyboard placement log should name target tile");
        require(keyboardMove.contains("keyboard placement"), "keyboard placement log should name input route");
        press(panel, KeyEvent.VK_ESCAPE);
        require(!panel.buildPlacementActive, "Escape should cancel construction placement");
        require(panel.pendingBuildRecipe == null, "cancel should clear selected construction placement recipe");
    }

    private static void press(GamePanel panel, int keyCode) {
        KeyEvent event = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, keyCode, KeyEvent.CHAR_UNDEFINED);
        GamePanelKeyController.handleKeyPressed(panel, event);
    }

    private static void pressShift(GamePanel panel, int keyCode) {
        KeyEvent event = new KeyEvent(panel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), KeyEvent.SHIFT_DOWN_MASK, keyCode, KeyEvent.CHAR_UNDEFINED);
        GamePanelKeyController.handleKeyPressed(panel, event);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private Milestone02ConstructionKeyboardSmoke() {}
}
