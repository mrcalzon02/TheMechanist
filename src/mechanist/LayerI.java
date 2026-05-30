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
        if (panel.screen == Screen.SECTOR_AUDIT) return panel.auditWorld != null;
        return panel.world != null && (panel.screen == Screen.GAME || (panel.screen == Screen.PANEL && (panel.panelMode == PanelMode.LOOK || panel.panelMode == PanelMode.COMBAT || panel.panelMode == PanelMode.INTERACT || panel.panelMode == PanelMode.SCAVENGE || panel.panelMode == PanelMode.BUILD || panel.panelMode == PanelMode.WORKBENCH)));
    }
}
