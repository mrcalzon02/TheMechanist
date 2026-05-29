package mechanist;

public class LayerC {
    public LayerC() {}

    static void cycleLightingFx(GamePanel panel) {
        int next = (panel.options.lightingFxIndex + 1) % GameOptions.LIGHTING_FX_LABELS.length;
        panel.logEvent(OptionsBoundaryAuthority.setLightingFxIndex(panel.options, next));
        panel.repaint();
    }

    static void toggleReducedMotion(GamePanel panel) {
        panel.logEvent(OptionsBoundaryAuthority.toggleReducedMotion(panel.options));
        DisplayScaleOptionsSubsystem.applyOptions(panel);
        panel.repaint();
    }
}
