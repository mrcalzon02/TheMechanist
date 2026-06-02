package mechanist;

import java.awt.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Locale;
import javax.swing.*;

/**
 * Stable subsystem for JVM runtime profile changes and restart orchestration.
 */
final class JvmRuntimeOptionsSubsystem {
    private JvmRuntimeOptionsSubsystem() {
    }

    static void markJvmProfileChanged(GamePanel panel, String message) {
        JvmRuntimeProfileAuthority.save(panel.jvmRuntimeProfile);
        panel.jvmRuntimeRestartPending = true;
        panel.jvmRuntimeNotice = message + " Saved to settings/jvm_runtime.properties. Restart required for full effect.";
        panel.logEvent(panel.jvmRuntimeNotice);
        DebugLog.audit("JVM_RUNTIME_PROFILE", JvmRuntimeProfileAuthority.auditSummary(panel.jvmRuntimeProfile));
        panel.repaint();
    }

    static void cycleJvmRuntimeProfile(GamePanel panel) {
        JvmRuntimeProfileAuthority.cycleTarget(panel.jvmRuntimeProfile);
        markJvmProfileChanged(panel, "JVM runtime profile now " + panel.jvmRuntimeProfile.targetLabel() + ".");
    }

    static void cycleJvmGarbageCollector(GamePanel panel) {
        JvmRuntimeProfileAuthority.cycleGc(panel.jvmRuntimeProfile);
        markJvmProfileChanged(panel, "JVM garbage collector profile now " + panel.jvmRuntimeProfile.gc.label + ".");
    }

    static void cycleJvmPipelineProfile(GamePanel panel) {
        JvmRuntimeProfileAuthority.cyclePipeline(panel.jvmRuntimeProfile);
        markJvmProfileChanged(panel, "Java2D pipeline profile now " + panel.jvmRuntimeProfile.pipelineLabel() + ".");
    }

    static void changeJvmMemory(GamePanel panel, int deltaMb) {
        JvmRuntimeProfileAuthority.changeMemory(panel.jvmRuntimeProfile, deltaMb);
        markJvmProfileChanged(panel, "JVM max heap profile now " + panel.jvmRuntimeProfile.maxRamMb + " MB.");
    }

    static void toggleJvmStringDeduplication(GamePanel panel) {
        panel.jvmRuntimeProfile.stringDeduplication = !panel.jvmRuntimeProfile.stringDeduplication;
        markJvmProfileChanged(panel, "JVM string deduplication " + (panel.jvmRuntimeProfile.stringDeduplication ? "enabled" : "disabled") + ".");
    }

    static void toggleJvmTransparentAcceleration(GamePanel panel) {
        panel.jvmRuntimeProfile.transparentAcceleration = !panel.jvmRuntimeProfile.transparentAcceleration;
        markJvmProfileChanged(panel, "Java2D transparent blit acceleration " + (panel.jvmRuntimeProfile.transparentAcceleration ? "enabled" : "disabled") + ".");
    }

    static void toggleJvmNoAa(GamePanel panel) {
        panel.jvmRuntimeProfile.disableVectorAntialiasing = !panel.jvmRuntimeProfile.disableVectorAntialiasing;
        markJvmProfileChanged(panel, "Java2D vector antialias suppression " + (panel.jvmRuntimeProfile.disableVectorAntialiasing ? "enabled" : "disabled") + ".");
    }

    static void acceptJvmSettingsAndRestart(GamePanel panel) {
        JvmRuntimeProfileAuthority.save(panel.jvmRuntimeProfile);
        java.util.List<String> command = new ArrayList<>();
        String javaExe = Paths.get(System.getProperty("java.home", ""), "bin", isWindowsHost() ? "java.exe" : "java").toString();
        command.add(javaExe);
        command.addAll(panel.jvmRuntimeProfile.buildJvmArgs());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(TheMechanist.class.getName());
        DebugLog.audit("JVM_RESTART", "Restarting with command=" + command);
        panel.logEvent("Restarting with accepted JVM profile: " + panel.jvmRuntimeProfile.targetLabel() + ".");
        try {
            new ProcessBuilder(command).inheritIO().start();
            panel.shutdownRuntime();
            Window win = SwingUtilities.getWindowAncestor(panel);
            if (win != null) win.dispose();
            System.exit(0);
        } catch (Throwable t) {
            DebugLog.error("JVM_RESTART", "Failed to restart with selected JVM profile.", t);
            panel.jvmRuntimeNotice = "Restart failed: " + t.getMessage();
            panel.logEvent(panel.jvmRuntimeNotice);
            panel.repaint();
        }
    }

    static boolean isWindowsHost() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
