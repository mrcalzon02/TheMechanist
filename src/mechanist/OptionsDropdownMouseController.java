package mechanist;

final class OptionsDropdownMouseController {
    private OptionsDropdownMouseController() {}

    static boolean handleOptionsDropdownClick(GamePanel panel, int mx, int my) {
        if (panel.screen != GamePanel.Screen.OPTIONS || panel.graphicsDropdown < 0) return false;
        for (int i = panel.buttons.size() - 1; i >= 0; i--) {
            ButtonBox button = panel.buttons.get(i);
            if (LayerD.isGraphicsDropdownButton(panel, button) && button.contains(mx, my)) {
                panel.selectedButton = i;
                panel.sounds.play("button", panel.options);
                panel.runGuarded("MOUSE", "click dropdown " + button.label, button.action);
                panel.repaint();
                return true;
            }
        }
        panel.graphicsDropdown = -1;
        panel.repaint();
        panel.requestFocusInWindow();
        return true;
    }
}
