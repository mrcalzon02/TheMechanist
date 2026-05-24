package mechanist.server.admin;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ServerAdminConsole {
    private ServerAdminConsole() {}

    public static void show(Path gameRoot, Path logRoot, String channel, ServerUpdateService updates, ServerAdminLog adminLog) {
        ServerMaintenanceService maintenance = new ServerMaintenanceService(logRoot.getParent() == null ? gameRoot.resolve("server") : logRoot.getParent(), adminLog);
        JFrame frame = new JFrame("The Mechanist Server Administrator");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(900, 620));
        frame.setLocationByPlatform(true);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Status", statusPanel(gameRoot, logRoot, channel));
        tabs.addTab("Users / Connections", placeholderPanel("No live client/session adapter is attached yet. This surface is reserved for connected users, session activity, latency, and stream-safe address display."));
        tabs.addTab("World State", placeholderPanel("World-state adapter is not attached yet. This surface is reserved for active .mechworld, save index, faction plans, production, trade, defense, and autosave state."));
        tabs.addTab("Activity Log", activityPanel(adminLog));
        tabs.addTab("Admin Commands", commandsPanel(channel, updates, maintenance));

        frame.setContentPane(tabs);
        frame.pack();
        frame.setVisible(true);
    }

    private static JPanel statusPanel(Path gameRoot, Path logRoot, String channel) {
        JPanel p = new JPanel(new GridBagLayout());
        GridBagConstraints g = row();
        addRow(p, g, "Channel", channel);
        addRow(p, g, "Game root", String.valueOf(gameRoot));
        addRow(p, g, "Game root present", Boolean.toString(Files.isDirectory(gameRoot)));
        addRow(p, g, "Log root", String.valueOf(logRoot));
        addRow(p, g, "Java", System.getProperty("java.version", "<unknown>") + " / " + System.getProperty("java.vendor", "<unknown>"));
        addRow(p, g, "OS", System.getProperty("os.name", "<unknown>") + " " + System.getProperty("os.version", ""));
        addRow(p, g, "Authority note", "Local admin surface only; public multiplayer readiness is not implied.");
        return p;
    }

    private static JPanel activityPanel(ServerAdminLog adminLog) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        JTextArea area = new JTextArea();
        area.setEditable(false);
        Runnable refresh = () -> {
            StringBuilder sb = new StringBuilder();
            for (ServerAdminEvent event : adminLog.recentEvents()) sb.append(event.toLogLine()).append(System.lineSeparator());
            area.setText(sb.toString());
            area.setCaretPosition(area.getDocument().getLength());
        };
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refresh.run());
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        p.add(refreshButton, BorderLayout.SOUTH);
        refresh.run();
        return p;
    }

    private static JPanel commandsPanel(String channel, ServerUpdateService updates, ServerMaintenanceService maintenance) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Field", "Value"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        JButton check = new JButton("Check for Updates");
        JButton update = new JButton("Apply Update If Safe");
        JButton saveNow = new JButton("Save Now");
        JButton backup = new JButton("Create Backup");
        JButton restore = new JButton("Restore Latest Backup If Safe");

        check.addActionListener(e -> runUpdateStatus(model, () -> updates.checkForUpdates(channel)));
        update.addActionListener(e -> runUpdateStatus(model, () -> updates.applyUpdate(channel, false, false)));
        saveNow.addActionListener(e -> runMaintenanceStatus(model, () -> maintenance.saveNow(false, false)));
        backup.addActionListener(e -> runMaintenanceStatus(model, maintenance::createBackup));
        restore.addActionListener(e -> runMaintenanceStatus(model, () -> maintenance.restoreLatestBackup(false, false)));

        JPanel buttons = new JPanel(new GridBagLayout());
        GridBagConstraints b = new GridBagConstraints();
        b.insets = new Insets(4, 4, 4, 4);
        b.gridx = 0; b.gridy = 0; buttons.add(check, b);
        b.gridx++; buttons.add(update, b);
        b.gridx = 0; b.gridy++; buttons.add(saveNow, b);
        b.gridx++; buttons.add(backup, b);
        b.gridx++; buttons.add(restore, b);

        p.add(buttons, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private static void runUpdateStatus(DefaultTableModel model, UpdateStatusSupplier supplier) {
        model.setRowCount(0);
        model.addRow(new Object[] {"Status", "Running..."});
        new SwingWorker<ServerUpdateStatus, Void>() {
            @Override protected ServerUpdateStatus doInBackground() { return supplier.get(); }
            @Override protected void done() {
                model.setRowCount(0);
                try {
                    ServerUpdateStatus status = get();
                    model.addRow(new Object[] {"State", status.state().toString()});
                    model.addRow(new Object[] {"Current", status.currentVersion()});
                    model.addRow(new Object[] {"Available", status.availableVersion()});
                    model.addRow(new Object[] {"Channel", status.channel()});
                    model.addRow(new Object[] {"Restart required", Boolean.toString(status.restartRequired())});
                    model.addRow(new Object[] {"Message", status.message()});
                    model.addRow(new Object[] {"Checked", status.checkedAt().toString()});
                } catch (Exception ex) {
                    model.addRow(new Object[] {"Error", ex.getMessage()});
                }
            }
        }.execute();
    }

    private static void runMaintenanceStatus(DefaultTableModel model, MaintenanceStatusSupplier supplier) {
        model.setRowCount(0);
        model.addRow(new Object[] {"Status", "Running..."});
        new SwingWorker<ServerMaintenanceResult, Void>() {
            @Override protected ServerMaintenanceResult doInBackground() { return supplier.get(); }
            @Override protected void done() {
                model.setRowCount(0);
                try {
                    ServerMaintenanceResult result = get();
                    model.addRow(new Object[] {"State", result.state().toString()});
                    model.addRow(new Object[] {"Action", result.action()});
                    model.addRow(new Object[] {"Path", result.path() == null ? "<none>" : result.path().toString()});
                    model.addRow(new Object[] {"Message", result.message()});
                    model.addRow(new Object[] {"Time", result.timestamp().toString()});
                } catch (Exception ex) {
                    model.addRow(new Object[] {"Error", ex.getMessage()});
                }
            }
        }.execute();
    }

    private static JPanel placeholderPanel(String text) {
        JPanel p = new JPanel(new BorderLayout());
        JTextArea area = new JTextArea(text);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
    }

    private static GridBagConstraints row() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;
        g.gridy = -1;
        return g;
    }

    private static void addRow(JPanel p, GridBagConstraints g, String label, String value) {
        g.gridy++;
        g.gridx = 0;
        g.weightx = 0;
        p.add(new JLabel(label), g);
        g.gridx = 1;
        g.weightx = 1;
        p.add(new JLabel(value), g);
    }

    private interface UpdateStatusSupplier { ServerUpdateStatus get(); }
    private interface MaintenanceStatusSupplier { ServerMaintenanceResult get(); }
}
