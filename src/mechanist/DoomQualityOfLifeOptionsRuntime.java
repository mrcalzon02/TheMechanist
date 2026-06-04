package mechanist;

final class DoomQualityOfLifeOptionsRuntime {
    private DoomQualityOfLifeOptionsRuntime() {}

    static void applyQoL(GamePanel panel, String message) {
        panel.logEvent(message);
        DebugLog.audit("GAMEPLAY_QOL_OPTIONS", GameplayQualityOfLifeAuthority.auditSummary(panel.options));
        panel.repaint();
    }

    static void changeDoomFov(GamePanel panel, int delta) {
        if (panel.options == null) {
            panel.logEvent("doom mode FOV unchanged: options unavailable.");
            return;
        }
        applyQoL(panel, GameplayQualityOfLifeAuthority.setDoomFov(panel.options, panel.options.doomModeFovDegrees + delta));
    }

    static void requestDoomModeToggle(GamePanel panel) {
        DoomRuntimeOptionsSubsystem.requestDoomModeToggle(panel);
    }

    static void cycleDoomFogMode(GamePanel panel) {
        DoomRuntimeOptionsSubsystem.cycleDoomFogMode(panel);
    }
}
