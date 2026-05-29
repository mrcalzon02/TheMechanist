package mechanist;

import javax.swing.JOptionPane;

final class DoomRuntimeOptionsSubsystem {
    private DoomRuntimeOptionsSubsystem() {
    }

    static void requestDoomModeToggle(GamePanel panel) {
        if (panel.options == null) {
            panel.logEvent("doom mode unchanged: options unavailable.");
            return;
        }
        if (panel.options.doomModeEnabled) {
            applyQoL(panel, GameplayQualityOfLifeAuthority.setDoomMode(panel.options, false));
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                panel,
                "doom mode is highly experimental and may cause instability.\n\nEnable the first-person 3D viewport wrapper for this profile?",
                "Enable experimental doom mode?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice == JOptionPane.YES_OPTION) {
            applyQoL(panel, GameplayQualityOfLifeAuthority.setDoomMode(panel.options, true));
            panel.logEvent("doom mode assets/renderer unlocked. LWJGL dependency status: " + LwjglRenderBackendProbe.statusLine() + ".");
        } else {
            panel.options.doomModeEnabled = false;
            panel.options.save();
            applyQoL(panel, "doom mode remains OFF.");
        }
    }

    static void changeDoomFov(GamePanel panel, int delta) {
        if (panel.options == null) {
            panel.logEvent("doom mode FOV unchanged: options unavailable.");
            return;
        }
        applyQoL(panel, GameplayQualityOfLifeAuthority.setDoomFov(panel.options, panel.options.doomModeFovDegrees + delta));
    }

    static void cycleDoomFogMode(GamePanel panel) {
        if (panel.options == null) {
            panel.logEvent("doom mode fog unchanged: options unavailable.");
            return;
        }
        applyQoL(panel, GameplayQualityOfLifeAuthority.cycleDoomFogMode(panel.options));
    }

    static void applyQoL(GamePanel panel, String message) {
        panel.logEvent(message);
        DebugLog.audit("GAMEPLAY_QOL_OPTIONS", GameplayQualityOfLifeAuthority.auditSummary(panel.options));
        panel.repaint();
    }
}
