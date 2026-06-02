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

    static int readableButtonHeight(GamePanel panel, int preferred) {
        return Math.max(28, Math.min(preferred, Math.max(24, panel.getHeight() / 11)));
    }

    static int readableGap(GamePanel panel, int preferred) {
        return Math.max(8, Math.min(preferred, Math.max(6, panel.getHeight() / 42)));
    }

    static boolean worldZoomControlActive(GamePanel panel) {
        return MapViewportOptionsSubsystem.worldZoomControlActive(panel);
    }

    static void changeWorldZoom(GamePanel panel, int delta, String source) {
        MapViewportOptionsSubsystem.changeWorldZoom(panel, delta, source);
    }

    static void changeFontScale(GamePanel panel, int delta) {
        panel.options.fontScale = Math.max(50, Math.min(200, panel.options.fontScale + delta * 5));
        DisplayScaleOptionsSubsystem.applyOptions(panel);
        panel.options.save();
        panel.sounds.play("tab", panel.options);
        panel.logEvent("Text scale now " + panel.options.fontScale + "%.");
        panel.repaint();
    }

    static void changeUiScale(GamePanel panel, int delta) {
        panel.options.uiScale = Math.max(50, Math.min(200, panel.options.uiScale + delta * 5));
        DisplayScaleOptionsSubsystem.applyOptions(panel);
        panel.options.save();
        panel.sounds.play("tab", panel.options);
        panel.logEvent("GUI scale now " + panel.options.uiScale + "%.");
        panel.repaint();
    }
}
