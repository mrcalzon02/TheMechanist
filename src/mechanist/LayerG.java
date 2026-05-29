package mechanist;

public class LayerG {
    public LayerG() {}

    static void cycleWindowMode(GamePanel panel) {
        panel.logEvent(OptionsBoundaryAuthority.cycleWindowMode(panel.options));
        panel.repaint();
    }

    static void changeResolution(GamePanel panel, int delta) {
        panel.logEvent(OptionsBoundaryAuthority.changeResolution(panel.options, delta));
        panel.repaint();
    }

    static void applyWindowMode(GamePanel panel) {
        LayerD.applyWindowMode(panel);
    }
}
