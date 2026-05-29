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
}
