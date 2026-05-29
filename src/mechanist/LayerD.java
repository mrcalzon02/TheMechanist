package mechanist;

public class LayerD {
    public LayerD() {}

    static void toggleGraphicsDropdown(GamePanel panel, int which) {
        panel.graphicsDropdown = (panel.graphicsDropdown == which) ? -1 : which;
        panel.logEvent("Graphics dropdown " + (panel.graphicsDropdown < 0 ? "closed" : "opened") + ".");
        panel.repaint();
    }

    static boolean isGraphicsDropdownButton(GamePanel panel, ButtonBox b) {
        if (b == null || b.label == null) return false;
        String label = b.label.trim();
        if (label.startsWith("> ")) label = label.substring(2).trim();
        if (panel.graphicsDropdown == 0) {
            return label.equals("Windowed") || label.equals("Borderless Windowed") || label.equals("Exclusive Fullscreen");
        }
        if (panel.graphicsDropdown == 1) {
            for (int i = 0; i < DisplayResolutionAuthority.choiceCount(); i++) {
                if (label.equals(DisplayResolutionAuthority.modeLabel(i))) return true;
            }
            return false;
        }
        if (panel.graphicsDropdown == 2) {
            for (String name : GameOptions.PALETTE_NAMES) if (label.equals(name)) return true;
            return false;
        }
        if (panel.graphicsDropdown == 3) {
            for (String name : GameOptions.DOWNSCALE_LABELS) if (label.equals(name)) return true;
            return false;
        }
        if (panel.graphicsDropdown == 4) {
            for (String name : GameOptions.TARGET_FPS_LABELS) if (label.equals(name)) return true;
            return false;
        }
        if (panel.graphicsDropdown == 5) {
            for (String name : GameOptions.RENDER_QUALITY_LABELS) if (label.equals(name)) return true;
            return false;
        }
        return false;
    }

    static void setWindowMode(GamePanel panel, int mode) {
        panel.logEvent(OptionsBoundaryAuthority.setWindowMode(panel.options, mode));
        panel.graphicsDropdown = -1;
        panel.repaint();
    }

    static void applyWindowMode(GamePanel panel) {
        panel.renderScaling.applyOptions(panel.options);
        panel.logEvent(WindowModeSurfaceAuthority.apply(panel, panel.options));
        DebugLog.audit("GRAPHICS_APPLY", panel.renderScaling.auditSummary());
        panel.repaint();
    }

    static void setResolutionIndex(GamePanel panel, int idx) {
        panel.logEvent(OptionsBoundaryAuthority.setResolutionIndex(panel.options, idx));
        panel.graphicsDropdown = -1;
        applyWindowMode(panel);
        panel.repaint();
    }

    static void setDownscaleIndex(GamePanel panel, int idx) {
        panel.logEvent(OptionsBoundaryAuthority.setDownscaleIndex(panel.options, idx));
        panel.renderScaling.applyOptions(panel.options);
        panel.graphicsDropdown = -1;
        DebugLog.audit("RENDER_DOWNSCALE", panel.renderScaling.auditSummary());
        panel.repaint();
    }

    static void setColorPreset(GamePanel panel, int idx) {
        panel.logEvent(OptionsBoundaryAuthority.setColorPreset(panel.options, idx));
        panel.graphicsDropdown = -1;
        panel.repaint();
    }

    static void setTargetFpsIndex(GamePanel panel, int idx) {
        panel.logEvent(OptionsBoundaryAuthority.setTargetFpsIndex(panel.options, idx));
        panel.graphicsDropdown = -1;
        panel.repaint();
    }

    static void setRenderQualityIndex(GamePanel panel, int idx) {
        LayerC.setRenderQualityIndex(panel, idx);
    }
}
