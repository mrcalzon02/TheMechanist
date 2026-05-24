package mechanist.launcher;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MechanistLauncherApp().show());
    }

    private void show() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        frame = new JFrame(LauncherConfig.APP_NAME + " Launcher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 640));
        frame.setLocationByPlatform(true);

        JPanel root = new JPanel(new BorderLayout(14, 14));
        root.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        root.setBackground(new Color(22, 22, 24));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.pack();
        frame.setVisible(true);
        refreshState();
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout(10, 8));
        p.setOpaque(false);
        JLabel title = new JLabel("THE MECHANIST");
        title.setForeground(new Color(226, 220, 198));
        title.setFont(new Font(Font.SERIF, Font.BOLD, 34));
        JLabel sub = new JLabel("StellarCore launcher — install, update, repair, and select graphics/audio packages");
        sub.setForeground(new Color(170, 168, 158));
        sub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        p.add(title, BorderLayout.NORTH);
        p.add(sub, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        p.add(label("Channel"), g);
        channel = new JComboBox<>(new String[] {"main", "testing", "dev"});
        channel.setEditable(true);
        g.gridx = 1; g.weightx = 1;
        p.add(channel, g);

        g.gridx = 0; g.gridy++; g.weightx = 0;
        p.add(label("Graphics package"), g);
        graphicsTier = new JComboBox<>(graphicsTiers.toArray(new PackageTier[0]));
        PackageTier defaultGraphics = PackageCatalog.defaultTier(graphicsTiers);
        if (defaultGraphics != null) graphicsTier.setSelectedItem(defaultGraphics);
        graphicsTier.addActionListener(e -> refreshState());
        g.gridx = 1; g.weightx = 1;
        p.add(graphicsTier, g);

        g.gridx = 0; g.gridy++; g.weightx = 0;
        p.add(label("Audio package"), g);
        audioTier = new JComboBox<>(audioTiers.toArray(new PackageTier[0]));
        PackageTier defaultAudio = PackageCatalog.defaultTier(audioTiers);
        if (defaultAudio != null) audioTier.setSelectedItem(defaultAudio);
        audioTier.addActionListener(e -> refreshState());
        g.gridx = 1; g.weightx = 1;
        p.add(audioTier, g);

        g.gridx = 0; g.gridy++; g.weightx = 0;
        p.add(label("Effective graphics"), g);
        effectiveGraphics = value("pending install");
        g.gridx = 1; g.weightx = 1;
        p.add(effectiveGraphics, g);

        g.gridx = 0; g.gridy++; g.weightx = 0;
        p.add(label("Effective audio"), g);
        effectiveAudio = value("pending install");
        g.gridx = 1; g.weightx = 1;
        p.add(effectiveAudio, g);

        g.gridx = 0; g.gridy++; g.weightx = 0;
        p.add(label("Install root"), g);
        g.gridx = 1; g.weightx = 1;
        p.add(value(config.installRoot.toString()), g);

        g.gridx = 0; g.gridy++; g.weightx = 0;
        p.add(label("Game payload"), g);
        g.gridx = 1; g.weightx = 1;
        p.add(value(config.repoDir.toString()), g);

        g.gridx = 0; g.gridy++; g.weightx = 0;
        p.add(label("Logs"), g);
        g.gridx = 1; g.weightx = 1;
        p.add(value(config.logsDir.toString()), g);

        log = new JTextArea(16, 80);
        log.setEditable(false);
        log.setLineWrap(true);
        log.setWrapStyleWord(true);
        log.setBackground(new Color(10, 12, 12));
        log.setForeground(new Color(180, 236, 190));
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(log);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(72, 78, 72)));
        g.gridx = 0; g.gridy++; g.gridwidth = 2; g.weightx = 1; g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        p.add(scroll, g);
        return p;
    }

    private JPanel buildFooter() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setOpaque(false);
        status = new JLabel("Ready.");
        status.setForeground(new Color(210, 205, 188));
        progress = new JProgressBar();
        progress.setIndeterminate(false);
        JPanel buttons = new JPanel(new GridBagLayout());
        buttons.setOpaque(false);
        GridBagConstraints b = new GridBagConstraints();
        b.insets = new Insets(0, 5, 0, 5);
        installUpdate = button("Install / Update", () -> runTask("Install / Update", false));
        repair = button("Repair", () -> runTask("Repair", true));
        launch = button("Launch Game", this::launchGame);
        JButton writeSelections = button("Apply Package Settings", this::writePackageSelections);
        JButton openFolder = button("Open Folder", this::openInstallFolder);
        buttons.add(installUpdate, b);
        buttons.add(repair, b);
        buttons.add(writeSelections, b);
        buttons.add(launch, b);
        buttons.add(openFolder, b);
        p.add(status, BorderLayout.NORTH);
        p.add(progress, BorderLayout.CENTER);
        p.add(buttons, BorderLayout.SOUTH);
        return p;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(180, 176, 158));
        return l;
    }

    private JLabel value(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(new Color(224, 224, 214));
        l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return l;
    }

    private JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        return b;
    }

    private String selectedBranch() {
        Object selected = channel.getSelectedItem();
        String branch = selected == null ? LauncherConfig.DEFAULT_BRANCH : selected.toString().trim();
        return branch.isBlank() ? LauncherConfig.DEFAULT_BRANCH : branch;
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
                    status.setText(name + " complete.");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    appendLog("ERROR: " + cause.getMessage());
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
            appendLog("ERROR writing package selections: " + ex.getMessage());
            status.setText("Package settings write failed.");
        }
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
                if (Files.isRegularFile(bat)) {
                    ProcessRunner.run(config.repoDir, MechanistLauncherApp.this::appendLog, "cmd", "/c", bat.toString());
                } else if (Files.isRegularFile(ps1)) {
                    ProcessRunner.run(config.repoDir, MechanistLauncherApp.this::appendLog, "powershell", "-ExecutionPolicy", "Bypass", "-File", ps1.toString());
                } else if (Files.isRegularFile(linux)) {
                    ProcessRunner.run(config.repoDir, MechanistLauncherApp.this::appendLog, "bash", linux.toString());
                } else {
                    throw new IOException("No platform game launcher was found. Run Install / Update first.");
                }
                return null;
            }
            @Override protected void done() {
                try { get(); status.setText("Game process ended."); }
                catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    appendLog("ERROR: " + cause.getMessage());
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
            appendLog("ERROR opening folder: " + ex.getMessage());
        }
    }

    private void refreshState() {
        PackageTier graphics = PackageCatalog.effectiveGraphicsTier(graphicsTiers, selectedGraphicsTier());
        PackageTier audio = PackageCatalog.effectiveAudioTier(audioTiers, selectedAudioTier());
        if (effectiveGraphics != null) effectiveGraphics.setText(graphics == null ? "missing low_32 fallback" : graphics.toString());
        if (effectiveAudio != null) effectiveAudio.setText(audio == null ? "silence fallback" : audio.toString());
        launch.setEnabled(git.gameLauncherPresent());
        if (git.gameLauncherPresent()) status.setText("Game installed. Ready to update, apply package settings, or launch.");
        else status.setText("Game not installed yet. Press Install / Update.");
    }

    private void setBusy(boolean busy, String message) {
        installUpdate.setEnabled(!busy);
        repair.setEnabled(!busy);
        launch.setEnabled(!busy && git.gameLauncherPresent());
        progress.setIndeterminate(busy);
        status.setText(message);
    }

    private void appendLog(String text) {
        if (log == null) return;
        log.append(text + System.lineSeparator());
        log.setCaretPosition(log.getDocument().getLength());
    }
}
