package mechanist;

public class LayerD {
    public LayerD() {}

    static void toggleGraphicsDropdown(GamePanel panel, int which) {
        panel.graphicsDropdown = (panel.graphicsDropdown == which) ? -1 : which;
        panel.logEvent("Graphics dropdown " + (panel.graphicsDropdown < 0 ? "closed" : "opened") + ".");
        panel.repaint();
    }
}
