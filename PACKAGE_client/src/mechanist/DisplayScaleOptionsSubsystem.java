package mechanist;

import java.awt.*;

/**
 * Stable subsystem for display density, font scaling, GUI scaling, and option application.
 * Keep below connector-safe file size limits.
 */
final class DisplayScaleOptionsSubsystem {
    private DisplayScaleOptionsSubsystem() {
    }

    static double uiScaleFactor(GamePanel panel) {
        return DisplayDensityAuthority.clampScale(panel.options.uiScale / 100.0f);
    }

    static int scaled(GamePanel panel, int value) {
        return Math.max(1, (int) Math.round(value * uiScaleFactor(panel)));
    }

    static int readableButtonHeight(GamePanel panel, int preferred) {
        return Math.max(28, Math.min(preferred, Math.max(24, panel.getHeight() / 11)));
    }

    static int readableGap(GamePanel panel, int preferred) {
        return Math.max(8, Math.min(preferred, Math.max(6, panel.getHeight() / 42)));
    }

    static String onOff(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    static void applyOptions(GamePanel panel) {
        double f = DisplayDensityAuthority.clampScale(panel.options.fontScale / 100.0f);
        DisplayDensityAuthority.applyGlobalSwingTextScale(panel.options);
        panel.titleFont = new Font("Monospaced", Font.BOLD, Math.max(20, (int) Math.round(36 * f)));
        panel.uiFont = new Font("Monospaced", Font.BOLD, Math.max(10, (int) Math.round(16 * f)));
        panel.smallFont = new Font("Monospaced", Font.BOLD, Math.max(8, (int) Math.round(12 * f)));
        panel.asciiFont = new Font("Monospaced", Font.BOLD, Math.max(8, (int) Math.round(13 * f)));
        panel.renderScaling.applyOptions(panel.options);
        if (panel.timer != null) panel.timer.setDelay(panel.options.targetTimerDelayMs());
        panel.frameLimiter.configure(panel.options);
        DebugLog.audit("DISPLAY_DENSITY", DisplayDensityAuthority.auditSummary(panel.options));
        DebugLog.audit("DISPLAY_RESOLUTION", DisplayResolutionAuthority.auditSummary(panel.options));
        DebugLog.audit("OPTIONS", "Applied options fontScale=" + panel.options.fontScale
                + " uiScale=" + panel.options.uiScale
                + " sfx=" + panel.options.sfxVolume
                + " music=" + panel.options.musicVolume
                + " conversation=" + panel.options.conversationVolume
                + " windowMode=" + panel.options.windowMode
                + " resolution=" + panel.options.resolutionLabel()
                + " downscale=" + panel.options.downscaleLabel()
                + " targetFps=" + panel.options.targetFpsLabel()
                + " frameLimited=" + panel.options.frameLimitLabel()
                + " renderQuality=" + panel.options.renderQualityLabel()
                + " reducedMotion=" + panel.options.reducedMotion
                + " artQuality=" + panel.options.artQualityLabel()
                + " generatedPayload=" + panel.options.generatedAssetPayloadRootShortLabel()
                + " mapTileSize=" + panel.options.mapTileSizeLabel()
                + " worldZoom=" + panel.options.worldZoomLabel());
    }

    static void changeFontScale(GamePanel panel, int delta) {
        panel.options.fontScale = Math.max(50, Math.min(200, panel.options.fontScale + delta * 5));
        applyOptions(panel);
        panel.options.save();
        panel.sounds.play("tab", panel.options);
        panel.logEvent("Text scale now " + panel.options.fontScale + "%. Compact default is " + GameOptions.COMPACT_DEFAULT_FONT_SCALE + "%; larger text remains available as opt-in.");
        panel.repaint();
    }

    static void changeUiScale(GamePanel panel, int delta) {
        panel.options.uiScale = Math.max(50, Math.min(200, panel.options.uiScale + delta * 5));
        applyOptions(panel);
        panel.options.save();
        panel.sounds.play("tab", panel.options);
        panel.logEvent("GUI scale now " + panel.options.uiScale + "%. Compact default is " + GameOptions.COMPACT_DEFAULT_UI_SCALE + "%; larger chrome remains available as opt-in.");
        panel.repaint();
    }
}
