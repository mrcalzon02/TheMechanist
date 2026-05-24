package mechanist.launcher;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class MechanistLauncherApp {
    private final LauncherConfig config = LauncherConfig.defaults();
    private final GitInstallService git = new GitInstallService(config, this::appendLog);
    private final LauncherSoundFeedback sound = new LauncherSoundFeedback(config);
    private final DiagnosticReporter diagnostics = new DiagnosticReporter(config);
    private final List<PackageTier> graphicsTiers = PackageCatalog.defaultGraphicsTiers(config);
    private final List<PackageTier> audioTiers = PackageCatalog.defaultAudioTiers(config);

    private JFrame frame;
    private JTextArea log;
    private JLabel status;
    private JProgressBar progress;
    private JComboBox<String> channel;
    private JComboBox<PackageTier> graphicsTier;
    private JComboBox<PackageTier> audioTier;
    private JLabel effectiveGraphics;
    private JLabel effectiveAudio;
    private JButton installUpdate;
    private JButton launch;
    private JButton repair;
    private String lastUpdateStatus = "No update has been run in this launcher session.";
    private String lastLaunchStatus = "No launch has been run in this launcher session.";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MechanistLauncherApp().show());
    }

    private void show() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        frame = new JFrame(LauncherConfig.APP_NAME + " Launcher");
        List<java.awt.Image> icons = LauncherIconAuthority.loadWindowIcons(config);
        if (!icons.isEmpty()) frame.setIconImages(icons);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(980, 680));
        frame.setLocationByPlatform(true);

        JPanel root = LauncherTheme.rootPanel();
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.pack();
        frame.setVisible(true);
        appendLog("Icon authority: " + LauncherIconAuthority.status(config));
        refreshState();
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(10, 8));
        p.setOpaque(false);
        JLabel title = LauncherTheme.title("THE MECHANIST");
        JLabel sub = LauncherTheme.subtitle("StellarCore launcher — install, update, repair, diagnostics, package selection, and runtime verification");
        p.add(title, BorderLayout.NORTH);
        p.add(sub, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildCenter() {
        JPanel container = new JPanel(new BorderLayout(12, 12));
        container.setOpaque(false);
        container.add(buildDashboardColumns(), BorderLayout.NORTH);

        log = LauncherTheme.logArea();
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createLineBorder(LauncherTheme.PANEL_EDGE));
        LauncherTheme.tooltip(scroll, "Compact launcher log. Routine users should not need this, but install/update/repair details appear here when something needs attention.");
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildDashboardColumns() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 0, 10);
        g.fill = GridBagConstraints.BOTH;
        g.gridy = 0;
        g.weighty = 0;

        g.gridx = 0; g.weightx = 0.28;
        grid.add(buildStatusColumn(), g);
        g.gridx = 1; g.weightx = 0.38;
        grid.add(buildPackageColumn(), g);
        g.gridx = 2; g.weightx = 0.34; g.insets = new Insets(0, 0, 0, 0);
        grid.add(buildPathColumn(), g);
        return grid;
    }

    private JPanel buildStatusColumn() {
        JPanel p = LauncherTheme.panel();
        p.add(sectionTitle("Status"), BorderLayout.NORTH);
        JPanel rows = formRows();
        GridBagConstraints g = rowConstraints();
        addRow(rows, g, "Channel", channel = new JComboBox<>(new String[] {"main", "testing", "dev"}));
        channel.setEditable(true);
        LauncherTheme.tooltip(channel, "Select the update channel. Main is the normal stable development target; testing/dev are future channel hooks.");
        effectiveGraphics = LauncherTheme.value("pending install");
        effectiveAudio = LauncherTheme.value("pending install");
        addRow(rows, g, "Graphics used", effectiveGraphics);
        addRow(rows, g, "Audio used", effectiveAudio);
        p.add(rows, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildPackageColumn() {
        JPanel p = LauncherTheme.panel();
        p.add(sectionTitle("Packages"), BorderLayout.NORTH);
        JPanel rows = formRows();
        GridBagConstraints g = rowConstraints();

        graphicsTier = new JComboBox<>(graphicsTiers.toArray(new PackageTier[0]));
        PackageTier defaultGraphics = PackageCatalog.defaultTier(graphicsTiers);
        if (defaultGraphics != null) graphicsTier.setSelectedItem(defaultGraphics);
        graphicsTier.addActionListener(e -> { sound.panel(); refreshState(); });
        LauncherTheme.tooltip(graphicsTier, "Choose the desired graphics package. If the selected package is missing, the launcher will fall back stepwise until low_32 is available.");
        addRow(rows, g, "Graphics", graphicsTier);

        audioTier = new JComboBox<>(audioTiers.toArray(new PackageTier[0]));
        PackageTier defaultAudio = PackageCatalog.defaultTier(audioTiers);
        if (defaultAudio != null) audioTier.setSelectedItem(defaultAudio);
        audioTier.addActionListener(e -> { sound.panel(); refreshState(); });
        LauncherTheme.tooltip(audioTier, "Choose the desired music/audio package. Core includes the main menu music target; half maps one song per major zone; full includes variants.");
        addRow(rows, g, "Audio", audioTier);

        JLabel hint = LauncherTheme.subtitle("Selections are written before launch and during install/update.");
        g.gridx = 0; g.gridy++; g.gridwidth = 2; g.weightx = 1;
        rows.add(hint, g);
        p.add(rows, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildPathColumn() {
        JPanel p = LauncherTheme.panel();
        p.add(sectionTitle("Install paths"), BorderLayout.NORTH);
        JPanel rows = formRows();
        GridBagConstraints g = rowConstraints();
        addRow(rows, g, "Install", LauncherTheme.value(config.installRoot.toString()));
        addRow(rows, g, "Game", LauncherTheme.value(config.repoDir.toString()));
        addRow(rows, g, "Saves", LauncherTheme.value(config.saveDir.toString()));
        addRow(rows, g, "Logs", LauncherTheme.value(config.logsDir.toString()));
        p.add(rows, BorderLayout.CENTER);
        LauncherTheme.tooltip(p, "Application files, saves, settings, logs, and cache are separated. Program Files is not used for mutable logs or saves.");
        return p;
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(LauncherTheme.TEXT);
        l.setFont(LauncherTheme.SECTION);
        return l;
    }

    private JPanel formRows() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        return p;
    }

    private GridBagConstraints rowConstraints() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = -1;
        return g;
    }

    private void addRow(JPanel p, GridBagConstraints g, String label, java.awt.Component value) {
        g.gridy++;
        g.gridx = 0;
        g.gridwidth = 1;
        g.weightx = 0;
        p.add(LauncherTheme.label(label), g);
        g.gridx = 1;
        g.weightx = 1;
        p.add(value, g);
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setOpaque(false);
        status = new JLabel("Ready.");
        status.setForeground(LauncherTheme.TEXT);
        progress = new JProgressBar();
        LauncherTheme.progress(progress);
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints b = new GridBagConstraints();
        b.insets = new Insets(0, 5, 0, 5);
        installUpdate = button("Install / Update", "Download or fast-forward the selected game channel and then write package/path selections.", () -> runTask("Install / Update", false));
        repair = button("Repair", "Clean and reset the local game payload from the selected channel. Use when files are missing or local edits block updates.", () -> runTask("Repair", true));
        JButton writeSelections = button("Apply Package Settings", "Write the selected graphics/audio package and runtime paths without updating or launching.", this::writePackageSelections);
        JButton runtimeInfo = button("Runtime Info", "Show Java, OS, path, package, modified-content, and icon/runtime information for support and verification.", this::showRuntimeInfo);
        JButton reportIssue = button("Diagnostics", "Prepare a redacted diagnostic report with client hash, install state, errors, and bounded log excerpts. If active mods are detected, the launcher warns that modded reports are not accepted for base-game triage.", this::prepareDiagnostics);
        launch = button("Launch Game", "Write current selections, then start the game menu from the installed payload.", this::launchGame);
        JButton openFolder = button("Open Folder", "Open the StellarCore install folder in the operating system file browser.", this::openInstallFolder);
        buttons.add(installUpdate, b);
        buttons.add(repair, b);
        buttons.add(writeSelections, b);
        buttons.add(runtimeInfo, b);
        buttons.add(reportIssue, b);
        buttons.add(launch, b);
        buttons.add(openFolder, b);
        p.add(status, BorderLayout.NORTH);
        p.add(progress, BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private JButton button(String text, String tooltip, Runnable action) {
        JButton b = new JButton(text);
        LauncherTheme.tooltip(b, tooltip);
        b.addActionListener(e -> { sound.button(); action.run(); });
        return b;
    }

    private String selectedBranch() {
        Object selected = channel.getSelectedItem();
        String branch = selected == null ? LauncherConfig.DEFAULT_BRANCH : selected.toString().trim();
        return branch.isBlank() ? LauncherConfig.DEFAULT_BRANCH : branch;
    }

    private PackageTier selectedGraphicsTier() { return (PackageTier) graphicsTier.getSelectedItem(); }
    private PackageTier selectedAudioTier() { return (PackageTier) audioTier.getSelectedItem(); }

    private void runTask(String name, boolean doRepair) {
        setBusy(true, name + " running...");
        appendLog("=== " + name + " ===");
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                if (!git.gitAvailable()) throw new IOException("Git is required. Install Git for Windows or your platform package manager, then sign in for private repo access.");
                if (doRepair) git.repair(selectedBranch()); else git.installOrUpdate(selectedBranch());
                RuntimeSelectionWriter.writeSelections(config,
                        PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier()),
                        PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier()));
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    appendLog(name + " complete. Package selections written to settings/options.properties.");
                    lastUpdateStatus = name + " complete.";
                    status.setText(name + " complete.");
                } catch (Exception ex) {
                    sound.warning();
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    appendLog("ERROR: " + cause.getMessage());
                    lastUpdateStatus = name + " failed: " + cause.getMessage();
                    status.setText(name + " failed. See log output.");
                } finally {
                    setBusy(false, status.getText());
                    refreshState();
                }
            }
        };
        worker.execute();
    }

    private void writePackageSelections() {
        try {
            RuntimeSelectionWriter.writeSelections(config,
                    PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier()),
                    PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier()));
            appendLog("Package selections written to " + config.repoDir.resolve("settings/options.properties"));
            refreshState();
        } catch (Exception ex) {
            sound.warning();
            appendLog("ERROR writing package selections: " + ex.getMessage());
            status.setText("Package settings write failed.");
        }
    }

    private void showRuntimeInfo() {
        RuntimeInfoDialog.show(frame, config, selectedBranch(),
                PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier()),
                PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier()),
                "lastUpdate=" + lastUpdateStatus + "; lastLaunch=" + lastLaunchStatus);
    }

    private void prepareDiagnostics() {
        setBusy(true, "Preparing diagnostic report...");
        SwingWorker<DiagnosticReporter.DiagnosticReport, Void> worker = new SwingWorker<>() {
            @Override protected DiagnosticReporter.DiagnosticReport doInBackground() throws Exception {
                return diagnostics.prepare(
                        PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier()),
                        PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier()),
                        selectedBranch(),
                        log == null ? "" : log.getText());
            }
            @Override protected void done() {
                try {
                    DiagnosticReporter.DiagnosticReport report = get();
                    appendLog("Diagnostic report prepared: " + report.reportFile());
                    if (report.modded()) {
                        sound.warning();
                        String message = ModStateDetector.supportWarning() + "\n\nA local diagnostic report was still written here:\n" + report.reportFile() + "\n\nThe launcher will not open a base-game issue draft for this report while modified content is detected.";
                        JOptionPane.showMessageDialog(frame, message, "Modified content detected", JOptionPane.WARNING_MESSAGE);
                        status.setText("Modified content detected. Diagnostic report written locally only.");
                    } else {
                        diagnostics.openIssueDraft(report);
                        status.setText("Diagnostic report prepared. Browser opened issue draft.");
                    }
                } catch (Exception ex) {
                    sound.warning();
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    appendLog("ERROR preparing diagnostic report: " + cause.getMessage());
                    status.setText("Diagnostic report failed. See log output.");
                } finally { setBusy(false, status.getText()); }
            }
        };
        worker.execute();
    }

    private void launchGame() {
        setBusy(true, "Launching game...");
        appendLog("Launching from " + config.repoDir);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                RuntimeSelectionWriter.writeSelections(config,
                        PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier()),
                        PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier()));
                Path bat = config.repoDir.resolve("RUN_THE_MECHANIST_WINDOWS.bat");
                Path ps1 = config.repoDir.resolve("RUN_THE_MECHANIST_WINDOWS.ps1");
                Path linux = config.repoDir.resolve("PLAY_THE_MECHANIST_LINUX.sh");
                if (Files.isRegularFile(bat)) ProcessRunner.run(config.repoDir, MechanistLauncherApp.this::appendLog, "cmd", "/c", bat.toString());
                else if (Files.isRegularFile(ps1)) ProcessRunner.run(config.repoDir, MechanistLauncherApp.this::appendLog, "powershell", "-ExecutionPolicy", "Bypass", "-File", ps1.toString());
                else if (Files.isRegularFile(linux)) ProcessRunner.run(config.repoDir, MechanistLauncherApp.this::appendLog, "bash", linux.toString());
                else throw new IOException("No platform game launcher was found. Run Install / Update first.");
                return null;
            }
            @Override protected void done() {
                try { get(); lastLaunchStatus = "Game process ended."; status.setText("Game process ended."); }
                catch (Exception ex) {
                    sound.warning();
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    appendLog("ERROR: " + cause.getMessage());
                    lastLaunchStatus = "Launch failed: " + cause.getMessage();
                    status.setText("Launch failed. See log output.");
                } finally { setBusy(false, status.getText()); }
            }
        };
        worker.execute();
    }

    private void openInstallFolder() {
        try {
            Files.createDirectories(config.installRoot);
            if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(config.installRoot.toFile());
        } catch (Exception ex) {
            sound.warning();
            appendLog("ERROR opening folder: " + ex.getMessage());
        }
    }

    private void refreshState() {
        PackageTier graphics = PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier());
        PackageTier audio = PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier());
        if (effectiveGraphics != null) effectiveGraphics.setText(graphics == null ? "missing low_32 fallback" : graphics.toString());
        if (effectiveAudio != null) effectiveAudio.setText(audio == null ? "silence fallback" : audio.toString());
        launch.setEnabled(git.gameLauncherPresent());
        if (git.gameLauncherPresent()) status.setText("Game installed. Ready to update, apply package settings, report diagnostics, or launch.");
        else status.setText("Game not installed yet. Press Install / Update.");
    }

    private void setBusy(boolean busy, String message) {
        installUpdate.setEnabled(!busy);
        repair.setEnabled(!busy);
        launch.setEnabled(!busy && git.gameLauncherPresent());
        progress.setIndeterminate(busy);
        progress.setString(busy ? message : "Idle");
        status.setText(message);
    }

    private void appendLog(String text) {
        if (log == null) return;
        log.append(text + System.lineSeparator());
        log.setCaretPosition(log.getDocument().getLength());
    }
}
