package mechanist;

import javax.swing.SwingUtilities;

/**
 * Owns the package-client boot screen handoff back into the main menu.
 *
 * The temporary GamePanel bridge currently rebuilds the menu in its constructor.
 * This authority lets the application entry point re-arm BOOT after construction,
 * then advances to the existing menu route without letting paint code perform a
 * direct state mutation on the render call stack.
 */
final class BootMenuFlowAuthority {
    static final String VERSION = "boot-menu-flow-authority-0.9.10la";
    static final long MIN_BOOT_MILLIS = 3400L;

    private BootMenuFlowAuthority() {}

    static void startBootSequence(GamePanel panel, String reason) {
        if (panel == null) return;
        panel.bootStartMillis = System.currentTimeMillis();
        panel.selectedButton = 0;
        panel.panelMode = GamePanel.PanelMode.NONE;
        panel.screen = GamePanel.Screen.BOOT;
        panel.logEvent("Boot sequence started: " + safe(reason) + ".");
        DebugLog.audit("BOOT_MENU_FLOW", "start authority=" + VERSION + " reason=" + safe(reason));
    }

    static void maybeAdvanceFromBootPaint(GamePanel panel, long elapsedMillis) {
        if (panel == null || panel.screen != GamePanel.Screen.BOOT || panel.eulaGateActive) return;
        if (elapsedMillis < MIN_BOOT_MILLIS) return;
        SwingUtilities.invokeLater(() -> finishBootSequence(panel, "boot-timeout"));
    }

    static void finishBootSequence(GamePanel panel, String reason) {
        if (panel == null || panel.screen != GamePanel.Screen.BOOT) return;
        String safeReason = safe(reason);
        try {
            panel.setScreen(GamePanel.Screen.MENU);
            panel.panelMode = GamePanel.PanelMode.NONE;
            panel.selectedButton = 0;
            panel.newGameSetupActive = false;
            panel.characterNameEditActive = false;
            panel.graphicsDropdown = -1;
            panel.logEvent("Boot sequence finished: " + safeReason + ".");
            DebugLog.audit("BOOT_MENU_FLOW", "finish authority=" + VERSION + " reason=" + safeReason + " screen=" + panel.screen);
            panel.repaint();
            panel.requestFocusInWindow();
        } catch (Throwable t) {
            panel.screen = GamePanel.Screen.MENU;
            panel.panelMode = GamePanel.PanelMode.NONE;
            panel.selectedButton = 0;
            panel.logEvent("Boot sequence recovered to main menu after handoff failure.");
            DebugLog.error("BOOT_MENU_FLOW", "Boot finish failed; recovered to MENU.", t);
        }
    }

    static String auditSummary() {
        return "authority=" + VERSION + " flow=application-entry->BOOT->MENU minimumMillis=" + MIN_BOOT_MILLIS;
    }

    private static String safe(String reason) {
        return reason == null || reason.isBlank() ? "unspecified" : reason.replace('\n', ' ').trim();
    }
}
