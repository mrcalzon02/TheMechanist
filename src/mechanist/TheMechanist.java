package mechanist;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import mechanist.assets.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class TheMechanist {
    public static void main(String[] args) {
        if (ClientPackageLaunchAuthority.relaunchFromPackageRootIfRequired(args)) return;
        JvmRuntimeProfileAuthority.RuntimeConfig startupJvmProfile = JvmRuntimeProfileAuthority.load();
        JvmRuntimeProfileAuthority.configureEarlyJava2dProperties(startupJvmProfile);
        DisplayDensityAuthority.configureJvmDisplayPropertiesBeforeSwing();
        GameOptions startupOptions = GameOptions.load();
        SwingUtilities.invokeLater(() -> {
            DebugLog.init(BuildIdentityAuthority.debugBuildTag("client"));
            Runtime.getRuntime().addShutdownHook(new Thread(() -> DebugLog.shutdown("JVM shutdown hook executed."), "mechanist-log-shutdown"));
            Thread.setDefaultUncaughtExceptionHandler((t, e) -> DebugLog.error("UNHANDLED_THREAD", "Thread " + t.getName() + " threw outside guarded execution.", e));
            DebugLog.log("The Mechanist booting. " + BuildIdentityAuthority.auditSummary());
            RuntimeProfile runtimeProfile = RuntimeProfile.fromArgs(args);
            DebugLog.audit("BUILD_IDENTITY", BuildIdentityAuthority.auditSummary());
            DebugLog.audit("RUNTIME_SEPARATION", RuntimeSeparationAuthority.auditSummary(runtimeProfile));
            DisplayDensityAuthority.applyGlobalSwingTextScale(startupOptions);
            JFrame frame = new JFrame(BuildIdentityAuthority.clientWindowTitle());
            AppIconAuthority.applyTo(frame);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            GamePanel panel = new GamePanel(runtimeProfile);
            WorldStartFlowAuthority.install(panel);
            BootMenuFlowAuthority.startBootSequence(panel, "application-entry");
            frame.setContentPane(panel);
            DisplayDensityAuthority.refreshSwingTree(frame);
            frame.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { panel.shutdownRuntime(); }
                @Override public void windowClosed(WindowEvent e) { panel.shutdownRuntime(); }
            });
            WindowModeSurfaceAuthority.configureInitialFrame(frame, panel.options);
            frame.setVisible(true);
            WindowModeSurfaceAuthority.activateInitialFrame(frame, panel.options);
            panel.requestFocusInWindow();
        });
    }
}
