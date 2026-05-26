package mechanist.launcher;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.nio.file.Files;

final class RuntimeInfoDialog {
    private RuntimeInfoDialog() {}

    static void show(JFrame owner, LauncherConfig config, String channel, PackageTier graphics, PackageTier audio, String launcherStatus) {
        JDialog dialog = new JDialog(owner, "The Mechanist Runtime Information", false);
        dialog.setMinimumSize(new java.awt.Dimension(760, 520));
        dialog.setLocationRelativeTo(owner);

        DefaultTableModel model = new DefaultTableModel(new Object[] {"Field", "Value"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        add(model, "Launcher", "The Mechanist Launcher");
        add(model, "Studio", LauncherConfig.STUDIO_NAME);
        add(model, "Channel", channel);
        add(model, "Java version", System.getProperty("java.version", "<unknown>"));
        add(model, "Java vendor", System.getProperty("java.vendor", "<unknown>"));
        add(model, "Java home", System.getProperty("java.home", "<unknown>"));
        add(model, "OS", System.getProperty("os.name", "<unknown>") + " " + System.getProperty("os.version", ""));
        add(model, "Architecture", System.getProperty("os.arch", "<unknown>"));
        add(model, "Install root", config.installRoot.toString());
        add(model, "Install root source", Files.isDirectory(config.manifestDir) && Files.isDirectory(config.packageRoot) ? "bundled package layout" : "default OS install layout");
        add(model, "Launcher dir", config.launcherDir.toString());
        add(model, "Package root", config.packageRoot.toString());
        add(model, "Manifest dir", config.manifestDir.toString());
        add(model, "Client package", config.clientPackageDir.toString());
        add(model, "Server package", config.serverPackageDir.toString());
        add(model, "Support libraries", config.supportLibraryDir.toString());
        add(model, "Runtime dir", config.runtimeDir.toString());
        add(model, "Saves", config.saveDir.toString());
        add(model, "Settings", config.settingsDir.toString());
        add(model, "Profiles", config.profilesDir.toString());
        add(model, "Logs", config.logsDir.toString());
        add(model, "Cache", config.cacheDir.toString());
        add(model, "Package seed", config.packageSeedRoot.toString());
        add(model, "Graphics tier", graphics == null ? "<none>" : graphics.toString());
        add(model, "Audio tier", audio == null ? "<none>" : audio.toString());
        add(model, "Modified content", Boolean.toString(ModStateDetector.inspect(config).modded()));
        add(model, "Icon authority", LauncherIconAuthority.status(config));
        add(model, "Client package present", Boolean.toString(Files.isDirectory(config.clientPackageDir)));
        add(model, "Music manifest present", Boolean.toString(Files.isRegularFile(config.clientPackageDir.resolve("assets/music/music_manifest.tsv"))));
        add(model, "Launcher status", launcherStatus == null ? "" : launcherStatus);

        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);
        table.getColumnModel().getColumn(0).setPreferredWidth(190);
        table.getColumnModel().getColumn(1).setPreferredWidth(520);
        JPanel root = LauncherTheme.panel();
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.setContentPane(root);
        dialog.pack();
        dialog.setVisible(true);
    }

    private static void add(DefaultTableModel model, String field, String value) {
        model.addRow(new Object[] {field, value == null ? "" : value});
    }
}
