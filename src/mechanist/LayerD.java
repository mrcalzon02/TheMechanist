package mechanist;

/** @deprecated Use GraphicsDropdownOptionsRuntime. */
@Deprecated
public class LayerD {
    public LayerD() {}

    static void toggleGraphicsDropdown(GamePanel panel, int which) { GraphicsDropdownOptionsRuntime.toggleGraphicsDropdown(panel, which); }
    static boolean isGraphicsDropdownButton(GamePanel panel, ButtonBox b) { return GraphicsDropdownOptionsRuntime.isGraphicsDropdownButton(panel, b); }
    static void addGraphicsDropdownButtons(GamePanel panel, int x, int y, int bw, int gap) { GraphicsDropdownOptionsRuntime.addGraphicsDropdownButtons(panel, x, y, bw, gap); }
    static void setWindowMode(GamePanel panel, int mode) { GraphicsDropdownOptionsRuntime.setWindowMode(panel, mode); }
    static void applyWindowMode(GamePanel panel) { GraphicsDropdownOptionsRuntime.applyWindowMode(panel); }
    static void setResolutionIndex(GamePanel panel, int idx) { GraphicsDropdownOptionsRuntime.setResolutionIndex(panel, idx); }
    static void setDownscaleIndex(GamePanel panel, int idx) { GraphicsDropdownOptionsRuntime.setDownscaleIndex(panel, idx); }
    static void setColorPreset(GamePanel panel, int idx) { GraphicsDropdownOptionsRuntime.setColorPreset(panel, idx); }
    static void setTargetFpsIndex(GamePanel panel, int idx) { GraphicsDropdownOptionsRuntime.setTargetFpsIndex(panel, idx); }
    static void setRenderQualityIndex(GamePanel panel, int idx) { GraphicsDropdownOptionsRuntime.setRenderQualityIndex(panel, idx); }
}
