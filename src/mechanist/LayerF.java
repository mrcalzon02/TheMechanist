package mechanist;

public class LayerF {
    public LayerF() {}

    static void togglePerformanceDiagnostics(GamePanel panel) {
        String line = panel.performanceDiagnostics.toggle();
        panel.options.diagnosticsOverlay = panel.performanceDiagnostics.visible();
        panel.options.save();
        panel.logEvent(line);
        DebugLog.audit("PERFORMANCE_DIAGNOSTICS", panel.performanceDiagnostics.auditSummary());
        panel.repaint();
    }

    static void cycleCvdMode(GamePanel panel) {
        panel.options.cvdModeIndex = (panel.options.cvdModeIndex + 1) % AccessibilityCompatibilityAuthority.CvdMode.values().length;
        panel.options.save();
        panel.logEvent("Color vision correction: " + AccessibilityCompatibilityAuthority.cvdLabel(panel.options.cvdModeIndex) + ".");
        DebugLog.audit("ACCESSIBILITY_COMPATIBILITY", AccessibilityCompatibilityAuthority.auditSummary(panel.options));
        DebugLog.audit("FALLBACK_PROFILE", FallbackProfileManagementAuthority.auditSummary(panel.userProfile));
        panel.repaint();
    }

    static void toggleHighContrastText(GamePanel panel) {
        panel.options.highContrastText = !panel.options.highContrastText;
        panel.options.save();
        panel.logEvent("High contrast text containers " + (panel.options.highContrastText ? "enabled" : "disabled") + ".");
        DebugLog.audit("ACCESSIBILITY_COMPATIBILITY", AccessibilityCompatibilityAuthority.auditSummary(panel.options));
        DebugLog.audit("FALLBACK_PROFILE", FallbackProfileManagementAuthority.auditSummary(panel.userProfile));
        panel.repaint();
    }

    static void toggleInstantDialogueText(GamePanel panel) {
        panel.options.instantDialogueText = !panel.options.instantDialogueText;
        panel.options.save();
        panel.logEvent("Instant conversation text " + (panel.options.instantDialogueText ? "enabled" : "disabled") + ".");
        panel.repaint();
    }

    static void adjustScreenShake(GamePanel panel, int delta) {
        panel.options.screenShakePercent = Math.max(0, Math.min(100, panel.options.screenShakePercent + delta));
        panel.options.save();
        panel.logEvent("Screen shake intensity now " + panel.options.screenShakePercent + "%.");
        panel.repaint();
    }

    static void cycleColorTarget(GamePanel panel) {
        panel.logEvent(OptionsBoundaryAuthority.cycleColorTarget(panel.options));
        panel.repaint();
    }
}
