package mechanist;

/**
 * Stable subsystem for accessibility, diagnostics, and quality-of-life toggles
 * formerly embedded directly in GamePanel.
 */
final class AccessibilityRuntimeOptionsSubsystem {
    private AccessibilityRuntimeOptionsSubsystem() {
    }

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

    static void pushCurrentScreenNarration(GamePanel panel) {
        String next = "The Mechanist screen: " + panel.screen
                + (panel.panelMode == GamePanel.PanelMode.NONE ? "" : ", panel " + panel.panelMode)
                + (panel.world == null ? "" : ", zone " + panel.world.zoneType.label + ", turn " + panel.turn) + ".";
        AccessibilityCompatibilityAuthority.pushNarration(panel, panel.lastAccessibleNarration, next);
        panel.lastAccessibleNarration = next;
        panel.logEvent("Accessibility narration updated for current screen.");
        panel.repaint();
    }

    static void cycleColorTarget(GamePanel panel) {
        panel.logEvent(OptionsBoundaryAuthority.cycleColorTarget(panel.options));
        panel.repaint();
    }

    static void cycleColorPreset(GamePanel panel) {
        panel.logEvent(OptionsBoundaryAuthority.cycleColorPreset(panel.options));
        panel.repaint();
    }

    static void adjustSelectedColor(GamePanel panel, int delta) {
        panel.logEvent(OptionsBoundaryAuthority.adjustSelectedColor(panel.options, delta));
        panel.repaint();
    }
}
