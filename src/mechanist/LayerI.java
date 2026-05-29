package mechanist;

public class LayerI {
    public LayerI() {}

    static String stateText(boolean value) {
        if (value) return "ON";
        return "OFF";
    }

    static int atLeastOne(int value) {
        return Math.max(1, value);
    }

    static int boundedPercent(int value) {
        return Math.max(0, Math.min(100, value));
    }

    static double uiScaleFactor(GamePanel panel) {
        return DisplayDensityAuthority.clampScale(panel.options.uiScale / 100.0f);
    }

    static int scaled(GamePanel panel, int value) {
        return Math.max(1, (int)Math.round(value * uiScaleFactor(panel)));
    }
}
