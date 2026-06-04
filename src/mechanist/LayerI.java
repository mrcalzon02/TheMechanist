package mechanist;

/** @deprecated Use DisplayScaleOptionsRuntime. */
@Deprecated
public class LayerI {
    public LayerI() {}

    static String stateText(boolean value) { return DisplayScaleOptionsRuntime.stateText(value); }
    static int atLeastOne(int value) { return DisplayScaleOptionsRuntime.atLeastOne(value); }
    static int boundedPercent(int value) { return DisplayScaleOptionsRuntime.boundedPercent(value); }
    static double uiScaleFactor(GamePanel panel) { return DisplayScaleOptionsRuntime.uiScaleFactor(panel); }
    static int scaled(GamePanel panel, int value) { return DisplayScaleOptionsRuntime.scaled(panel, value); }
    static int readableButtonHeight(GamePanel panel, int preferred) { return DisplayScaleOptionsRuntime.readableButtonHeight(panel, preferred); }
    static int readableGap(GamePanel panel, int preferred) { return DisplayScaleOptionsRuntime.readableGap(panel, preferred); }
    static boolean worldZoomControlActive(GamePanel panel) { return DisplayScaleOptionsRuntime.worldZoomControlActive(panel); }
    static void changeWorldZoom(GamePanel panel, int delta, String source) { DisplayScaleOptionsRuntime.changeWorldZoom(panel, delta, source); }
    static void changeFontScale(GamePanel panel, int delta) { DisplayScaleOptionsRuntime.changeFontScale(panel, delta); }
    static void changeUiScale(GamePanel panel, int delta) { DisplayScaleOptionsRuntime.changeUiScale(panel, delta); }
}
