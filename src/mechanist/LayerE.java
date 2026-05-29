package mechanist;

public class LayerE {
    public LayerE() {}

    static void applyQoL(GamePanel panel, String message) {
        panel.logEvent(message);
        DebugLog.audit("GAMEPLAY_QOL_OPTIONS", GameplayQualityOfLifeAuthority.auditSummary(panel.options));
        panel.repaint();
    }
}
