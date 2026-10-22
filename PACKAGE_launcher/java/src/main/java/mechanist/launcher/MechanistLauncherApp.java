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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class MechanistLauncherApp {
    private final LauncherConfig config = LauncherConfig.defaults();
    private final PackageInstallService packages = new PackageInstallService(config);
    private final GitHubReleaseUpdateService releaseUpdates = new GitHubReleaseUpdateService();
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
    private JButton updateButton;
    private boolean launcherTaskBusy;
    private JButton launch;
    private JButton repair;
    private String lastUpdateStatus = "No package verification has been run in this launcher session.";
    private String lastLaunchStatus = "No launch has been run in this launcher session.";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MechanistLauncherApp().show());
    }

    private void show() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
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
        appendLog("Distribution policy: " + LauncherDistributionPolicy.auditSummary(config));
        refreshState();
        checkForUpdates(false);
    }

    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setOpaque(false);
        JLabel title = LauncherTheme.title("THE MECHANIST");
        JLabel subtitle = LauncherTheme.subtitle(
                "Limited-alpha launcher - bundled package verification, diagnostics, package selection, and runtime launch");
        panel.add(title, BorderLayout.NORTH);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildCenter() {
        JPanel container = new JPanel(new BorderLayout(12, 12));
        container.setOpaque(false);
        container.add(buildDashboardColumns(), BorderLayout.NORTH);

        log = LauncherTheme.logArea();
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createLineBorder(LauncherTheme.PANEL_EDGE));
        LauncherTheme.tooltip(scroll,
                "Compact launcher log. Package verification, rollback checks, diagnostics, and launch details appear here.");
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildDashboardColumns() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 0, 0, 10);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridy = 0;
        constraints.weighty = 0;

        constraints.gridx = 0;
        constraints.weightx = 0.28;
        grid.add(buildStatusColumn(), constraints);
        constraints.gridx = 1;
        constraints.weightx = 0.38;
        grid.add(buildPackageColumn(), constraints);
        constraints.gridx = 2;
        constraints.weightx = 0.34;
        constraints.insets = new Insets(0, 0, 0, 0);
        grid.add(buildPathColumn(), constraints);
        return grid;
    }

    private JPanel buildStatusColumn() {
        JPanel panel = LauncherTheme.panel();
        panel.add(sectionTitle("Status"), BorderLayout.NORTH);
        JPanel rows = formRows();
        GridBagConstraints constraints = rowConstraints();
        channel = new JComboBox<>(LauncherDistributionPolicy.selectableSources());
        channel.setEditable(false);
        channel.setEnabled(false);
        LauncherTheme.tooltip(channel,
                "This limited alpha uses the bundled verified package set. Remote main/testing/dev acquisition is not active.");
        addRow(rows, constraints, "Package source", channel);
        effectiveGraphics = LauncherTheme.value("pending verification");
        effectiveAudio = LauncherTheme.value("pending verification");
        addRow(rows, constraints, "Graphics used", effectiveGraphics);
        addRow(rows, constraints, "Audio used", effectiveAudio);
        panel.add(rows, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPackageColumn() {
        JPanel panel = LauncherTheme.panel();
        panel.add(sectionTitle("Packages"), BorderLayout.NORTH);
        JPanel rows = formRows();
        GridBagConstraints constraints = rowConstraints();

        graphicsTier = new JComboBox<>(graphicsTiers.toArray(new PackageTier[0]));
        PackageTier defaultGraphics = PackageCatalog.defaultTier(graphicsTiers);
        if (defaultGraphics != null) graphicsTier.setSelectedItem(defaultGraphics);
        graphicsTier.addActionListener(event -> {
            sound.panel();
            refreshState();
        });
        LauncherTheme.tooltip(graphicsTier,
                "Choose the desired graphics package. If it is absent, the launcher falls back toward the bundled low_32 tier.");
        addRow(rows, constraints, "Graphics", graphicsTier);

        audioTier = new JComboBox<>(audioTiers.toArray(new PackageTier[0]));
        PackageTier defaultAudio = PackageCatalog.defaultTier(audioTiers);
        if (defaultAudio != null) audioTier.setSelectedItem(defaultAudio);
        audioTier.addActionListener(event -> {
            sound.panel();
            refreshState();
        });
        LauncherTheme.tooltip(audioTier,
                "Choose the desired audio package. Missing optional audio falls back toward the bundled core or silence path.");
        addRow(rows, constraints, "Audio", audioTier);

        JLabel hint = LauncherTheme.subtitle(
                "Selections are written only after the bundled package set passes verification.");
        constraints.gridx = 0;
        constraints.gridy++;
        constraints.gridwidth = 2;
        constraints.weightx = 1;
        rows.add(hint, constraints);
        panel.add(rows, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPathColumn() {
        JPanel panel = LauncherTheme.panel();
        panel.add(sectionTitle("Install paths"), BorderLayout.NORTH);
        JPanel rows = formRows();
        GridBagConstraints constraints = rowConstraints();
        addRow(rows, constraints, "Install", LauncherTheme.value(config.installRoot.toString()));
        addRow(rows, constraints, "Packages", LauncherTheme.value(config.packageRoot.toString()));
        addRow(rows, constraints, "Saves", LauncherTheme.value(config.saveDir.toString()));
        addRow(rows, constraints, "Logs", LauncherTheme.value(config.logsDir.toString()));
        panel.add(rows, BorderLayout.CENTER);
        LauncherTheme.tooltip(panel,
                "Application files are separated from saves, settings, profiles, logs, and cache. Mutable data is not written into Program Files or the app image.");
        return panel;
    }

    private JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(LauncherTheme.TEXT);
        label.setFont(LauncherTheme.SECTION);
        return label;
    }

    private JPanel formRows() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        return panel;
    }

    private GridBagConstraints rowConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(5, 4, 5, 4);
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = -1;
        return constraints;
    }

    private void addRow(JPanel panel, GridBagConstraints constraints,
                        String label, java.awt.Component value) {
        constraints.gridy++;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0;
        panel.add(LauncherTheme.label(label), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1;
        panel.add(value, constraints);
    }

    private JPanel buildFooter() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setOpaque(false);
        status = new JLabel("Ready.");
        status.setForeground(LauncherTheme.TEXT);
        progress = new JProgressBar();
        LauncherTheme.progress(progress);
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(0, 5, 0, 5);
        updateButton = button(
                "Check Updates",
                "Check verified alpha installers published in GitHub Releases; download and install only with confirmation.",
                () -> checkForUpdates(true));
        installUpdate = button(
                "Verify Packages",
                "Verify the bundled manifest-described client, server, and support package set without cloning the development repository.",
                () -> runTask("Verify Packages", false));
        repair = button(
                "Verify / Rollback",
                "Re-run package verification and restore the newest verified local rollback when one exists. Reinstall the alpha package when no rollback or local seed is available.",
                () -> runTask("Verify / Rollback", true));
        JButton writeSelections = button(
                "Apply Package Settings",
                "Write the selected graphics/audio package and runtime paths without updating or launching.",
                this::writePackageSelections);
        JButton runtimeInfo = button(
                "Runtime Info",
                "Show Java, OS, path, package, modified-content, and runtime information for support and verification.",
                this::showRuntimeInfo);
        JButton reportIssue = button(
                "Diagnostics",
                "Prepare a redacted diagnostic report with package identity, install state, errors, and bounded log excerpts.",
                this::prepareDiagnostics);
        launch = button(
                "Launch Game",
                "Write current selections, then start the verified client package.",
                this::launchGame);
        JButton openFolder = button(
                "Open Folder",
                "Open the detected installation folder in the operating system file browser.",
                this::openInstallFolder);
        buttons.add(updateButton, constraints);
        buttons.add(installUpdate, constraints);
        buttons.add(repair, constraints);
        buttons.add(writeSelections, constraints);
        buttons.add(runtimeInfo, constraints);
        buttons.add(reportIssue, constraints);
        buttons.add(launch, constraints);
        buttons.add(openFolder, constraints);
        panel.add(status, BorderLayout.NORTH);
        panel.add(progress, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JButton button(String text, String tooltip, Runnable action) {
        JButton button = new JButton(text);
        LauncherTheme.tooltip(button, tooltip);
        button.addActionListener(event -> {
            sound.button();
            action.run();
        });
        return button;
    }

    private String selectedBranch() {
        Object selected = channel == null ? null : channel.getSelectedItem();
        return LauncherDistributionPolicy.normalizeSource(
                selected == null ? LauncherConfig.DEFAULT_BRANCH : selected.toString());
    }

    private PackageTier selectedGraphicsTier() {
        return (PackageTier) graphicsTier.getSelectedItem();
    }

    private PackageTier selectedAudioTier() {
        return (PackageTier) audioTier.getSelectedItem();
    }

    private void runTask(String name, boolean doRepair) {
        setBusy(true, name + " running...");
        appendLog("=== " + name + " ===");
        appendLog(LauncherDistributionPolicy.sourceStatus(config));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                if (doRepair) {
                    packages.repair(selectedBranch());
                } else {
                    packages.installOrUpdate(selectedBranch());
                }
                RuntimeSelectionWriter.writeSelections(
                        config,
                        PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier()),
                        PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier()));
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    appendLog(name + " complete. Runtime manifest and package hashes verified.");
                    lastUpdateStatus = name + " complete.";
                    status.setText(name + " complete.");
                } catch (Exception exception) {
                    sound.warning();
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
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


    /** Network lookup is asynchronous; the bundled game remains playable offline. */
    private void checkForUpdates(boolean interactive) {
        updateButton.setEnabled(false);
        updateButton.setText("Checking...");
        SwingWorker<GitHubReleaseUpdateService.Candidate, Void> worker = new SwingWorker<>() {
            @Override
            protected GitHubReleaseUpdateService.Candidate doInBackground() throws Exception {
                String installedCommit;
                try {
                    installedCommit = LauncherBuildIdentity.read(config).commit();
                } catch (java.io.IOException missingInstallation) {
                    installedCommit = "unknown";
                }
                String os = System.getProperty("os.name", "").toLowerCase();
                String platform = (os.contains("win") ? "windows-" :
                        os.contains("linux") ? "linux-" : "unsupported-")
                        + (System.getProperty("os.arch", "").contains("64") ? "x64" : "other");
                return releaseUpdates.check(platform, installedCommit);
            }

            @Override
            protected void done() {
                try {
                    GitHubReleaseUpdateService.Candidate candidate = get();
                    if (candidate == null) {
                        updateButton.setText("Check Updates");
                        if (interactive) {
                            JOptionPane.showMessageDialog(frame,
                                    "No newer compatible verified alpha installer is available.",
                                    "The Mechanist Updates", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        updateButton.setText("Install Update");
                        status.setText("New verified alpha installer: " + candidate.version());
                        appendLog("GitHub update: " + candidate.tag() + " / "
                                + candidate.assetName() + " / SHA-256: " + candidate.sha256());
                        if (interactive) {
                            int choice = JOptionPane.showConfirmDialog(frame,
                                    "A verified installer is available: " + candidate.version()
                                            + "\nPlatform package: " + candidate.assetName()
                                            + "\nDownload, verify SHA-256, and open the OS installer?\n"
                                            + "The launcher will close so it can be upgraded.",
                                    "Install The Mechanist Update",
                                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                            if (choice == JOptionPane.YES_OPTION) {
                                downloadAndLaunchUpdate(candidate);
                            }
                        }
                    }
                } catch (Exception exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    appendLog("GitHub update check unavailable: " + cause.getMessage());
                    updateButton.setText("Check Updates");
                    if (interactive) {
                        JOptionPane.showMessageDialog(frame,
                                "Update check failed: " + cause.getMessage()
                                        + "\nExisting game files were not changed.",
                                "Update check failed", JOptionPane.WARNING_MESSAGE);
                    }
                } finally {
                    updateButton.setEnabled(!launcherTaskBusy);
                }
            }
        };
        worker.execute();
    }

    /** Installer runs as a separate process, never overlaid onto running JARs. */
    private void downloadAndLaunchUpdate(GitHubReleaseUpdateService.Candidate candidate) {
        setBusy(true, "Downloading verified installer...");
        SwingWorker<Path, Void> worker = new SwingWorker<>() {
            @Override
            protected Path doInBackground() throws Exception {
                return releaseUpdates.download(candidate, config.cacheDir.resolve("installer-updates"));
            }

            @Override
            protected void done() {
                try {
                    Path installer = get();
                    releaseUpdates.launchInstaller(installer, candidate);
                    appendLog("Verified installer opened: " + installer);
                    status.setText("Installer opened. Close the launcher for installation.");
                    frame.dispose();
                    System.exit(0);
                } catch (Exception exception) {
                    sound.warning();
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    appendLog("Update aborted (existing installation intact): " + cause.getMessage());
                    setBusy(false, "Installer update failed; original game remains available.");
                }
            }
        };
        worker.execute();
    }

    private void writePackageSelections() {
        try {
            RuntimeSelectionWriter.writeSelections(
                    config,
                    PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier()),
                    PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier()));
            appendLog("Package selections written to "
                    + config.settingsDir.resolve("options.properties"));
            refreshState();
        } catch (Exception exception) {
            sound.warning();
            appendLog("ERROR writing package selections: " + exception.getMessage());
            status.setText("Package settings write failed.");
        }
    }

    private void showRuntimeInfo() {
        RuntimeInfoDialog.show(
                frame,
                config,
                selectedBranch(),
                PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier()),
                PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier()),
                "lastUpdate=" + lastUpdateStatus + "; lastLaunch=" + lastLaunchStatus
                        + "; distributionPolicy=" + LauncherDistributionPolicy.auditSummary(config));
    }

    private void prepareDiagnostics() {
        setBusy(true, "Preparing diagnostic report...");
        SwingWorker<DiagnosticReporter.DiagnosticReport, Void> worker = new SwingWorker<>() {
            @Override
            protected DiagnosticReporter.DiagnosticReport doInBackground() throws Exception {
                return diagnostics.prepare(
                        PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier()),
                        PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier()),
                        selectedBranch(),
                        (log == null ? "" : log.getText())
                                + System.lineSeparator()
                                + LauncherDistributionPolicy.auditSummary(config));
            }

            @Override
            protected void done() {
                try {
                    DiagnosticReporter.DiagnosticReport report = get();
                    appendLog("Diagnostic report prepared: " + report.reportFile());
                    if (report.modded()) {
                        sound.warning();
                        String message = ModStateDetector.supportWarning()
                                + "\n\nA local diagnostic report was still written here:\n"
                                + report.reportFile()
                                + "\n\nThe launcher will not open a base-game issue draft for this report while modified content is detected.";
                        JOptionPane.showMessageDialog(
                                frame,
                                message,
                                "Modified content detected",
                                JOptionPane.WARNING_MESSAGE);
                        status.setText(
                                "Modified content detected. Diagnostic report written locally only.");
                    } else {
                        diagnostics.openIssueDraft(report);
                        status.setText("Diagnostic report prepared. Browser opened issue draft.");
                    }
                } catch (Exception exception) {
                    sound.warning();
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    appendLog("ERROR preparing diagnostic report: " + cause.getMessage());
                    status.setText("Diagnostic report failed. See log output.");
                } finally {
                    setBusy(false, status.getText());
                }
            }
        };
        worker.execute();
    }

    private void launchGame() {
        setBusy(true, "Launching game...");
        appendLog("Launching verified client package from " + config.packageRoot);
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                RuntimeSelectionWriter.writeSelections(
                        config,
                        PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier()),
                        PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier()));
                Path clientJar = packages.clientJar();
                ArrayList<String> classpath = new ArrayList<>();
                classpath.add(clientJar.toString());
                for (Path support : packages.supportJars()) classpath.add(support.toString());
                ProcessRunner.run(
                        config.installRoot,
                        MechanistLauncherApp.this::appendLog,
                        javaCommand(),
                        "-cp",
                        String.join(System.getProperty("path.separator"), classpath),
                        "mechanist.launcher.ThinLauncherMain");
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    lastLaunchStatus = "Game process ended.";
                    status.setText("Game process ended.");
                } catch (Exception exception) {
                    sound.warning();
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    appendLog("ERROR: " + cause.getMessage());
                    lastLaunchStatus = "Launch failed: " + cause.getMessage();
                    status.setText("Launch failed. See log output.");
                } finally {
                    setBusy(false, status.getText());
                }
            }
        };
        worker.execute();
    }

    private void openInstallFolder() {
        try {
            Files.createDirectories(config.installRoot);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(config.installRoot.toFile());
            }
        } catch (Exception exception) {
            sound.warning();
            appendLog("ERROR opening folder: " + exception.getMessage());
        }
    }

    private void refreshState() {
        PackageTier graphics = PackageCatalog.effectiveGraphicsTier(
                graphicsTiers, selectedGraphicsTier());
        PackageTier audio = PackageCatalog.effectiveAudioTier(
                audioTiers, selectedAudioTier());
        if (effectiveGraphics != null) {
            effectiveGraphics.setText(
                    graphics == null ? "missing low_32 fallback" : graphics.toString());
        }
        if (effectiveAudio != null) {
            effectiveAudio.setText(audio == null ? "silence fallback" : audio.toString());
        }
        boolean ready = packages.gameLauncherPresent();
        launch.setEnabled(ready);
        if (ready) {
            status.setText(
                    "Bundled packages verified. Ready to apply settings, collect diagnostics, or launch.");
        } else {
            status.setText(
                    "No verified bundled package set. Use the complete portable/native alpha package or a verified local seed; remote acquisition is disabled.");
        }
    }

    private void setBusy(boolean busy, String message) {
        launcherTaskBusy = busy;
        installUpdate.setEnabled(!busy);
        updateButton.setEnabled(!busy);
        repair.setEnabled(!busy);
        launch.setEnabled(!busy && packages.gameLauncherPresent());
        progress.setIndeterminate(busy);
        progress.setString(busy ? message : "Idle");
        status.setText(message);
    }

    private void appendLog(String text) {
        if (log == null) return;
        log.append(text + System.lineSeparator());
        log.setCaretPosition(log.getDocument().getLength());
    }

    private static String javaCommand() {
        Path javaHome = Path.of(System.getProperty("java.home", ""));
        Path candidate = javaHome.resolve("bin").resolve(isWindows() ? "javaw.exe" : "java");
        return Files.isRegularFile(candidate)
                ? candidate.toString()
                : (isWindows() ? "javaw.exe" : "java");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
