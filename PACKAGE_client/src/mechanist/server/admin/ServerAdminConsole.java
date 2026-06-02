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
import java.util.List;

public final class ServerAdminConsole {
    private ServerAdminConsole() {}

    public static void show(Path gameRoot, Path logRoot, String channel, ServerUpdateService updates, ServerAdminLog adminLog) {
        Path serverRoot = logRoot.getParent() == null ? gameRoot.resolve("server") : logRoot.getParent();
        ServerMaintenanceService maintenance = new ServerMaintenanceService(serverRoot, adminLog);
        ServerSafeRestartService restartService = new ServerSafeRestartService(serverRoot, updates, maintenance, adminLog);
        ServerRuntimeStatusProvider statusProvider = new ServerRuntimeStatusProvider(gameRoot, serverRoot, maintenance, channel);
        JFrame frame = new JFrame("The Mechanist Server Administrator");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(980, 680));
        frame.setLocationByPlatform(true);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Status", statusPanel(gameRoot, logRoot, channel, statusProvider));
        tabs.addTab("Users / Connections", placeholderPanel("No live client/session adapter is attached yet. This surface is reserved for connected users, session activity, latency, and stream-safe address display."));
        tabs.addTab("World State", worldStatePanel(statusProvider, maintenance));
        tabs.addTab("Backups", backupPanel(maintenance));
        tabs.addTab("Activity Log", activityPanel(adminLog));
        tabs.addTab("Admin Commands", commandsPanel(channel, updates, maintenance, restartService, statusProvider));

        frame.setContentPane(tabs);
        frame.pack();
        frame.setVisible(true);
    }

    private static JPanel statusPanel(Path gameRoot, Path logRoot, String channel, ServerRuntimeStatusProvider statusProvider) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Field", "Value"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JButton refresh = new JButton("Refresh Status");
        refresh.addActionListener(e -> fillStatus(model, gameRoot, logRoot, channel, statusProvider.sample()));
        p.add(refresh, BorderLayout.NORTH);
        p.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        fillStatus(model, gameRoot, logRoot, channel, statusProvider.sample());
        return p;
    }

    private static void fillStatus(DefaultTableModel model, Path gameRoot, Path logRoot, String channel, ServerRuntimeStatus status) {
        model.setRowCount(0);
        model.addRow(new Object[] {"Channel", channel});
        model.addRow(new Object[] {"Game root", String.valueOf(gameRoot)});
        model.addRow(new Object[] {"Game root present", Boolean.toString(Files.isDirectory(gameRoot))});
        model.addRow(new Object[] {"Log root", String.valueOf(logRoot)});
        model.addRow(new Object[] {"Java", System.getProperty("java.version", "<unknown>") + " / " + System.getProperty("java.vendor", "<unknown>")});
        model.addRow(new Object[] {"OS", System.getProperty("os.name", "<unknown>") + " " + System.getProperty("os.version", "")});
        model.addRow(new Object[] {"Clients connected", Boolean.toString(status.clientsConnected())});
        model.addRow(new Object[] {"Save active", Boolean.toString(status.savingActive())});
        model.addRow(new Object[] {"Active world", status.activeWorld()});
        model.addRow(new Object[] {"Save slots", Integer.toString(status.saveSlotCount())});
        model.addRow(new Object[] {"Backups", Integer.toString(status.backupCount())});
        model.addRow(new Object[] {"Host status", status.hostStatus()});
        model.addRow(new Object[] {"Adapter", status.adapterStatus()});
        model.addRow(new Object[] {"Sampled", status.sampledAt().toString()});
        model.addRow(new Object[] {"Authority note", "Local admin surface only; public multiplayer readiness is not implied."});
    }

    private static JPanel worldStatePanel(ServerRuntimeStatusProvider statusProvider, ServerMaintenanceService maintenance) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Field", "Value"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JButton refresh = new JButton("Refresh World State");
        refresh.addActionListener(e -> fillWorldState(model, statusProvider.sample(), maintenance.backupIndex()));
        p.add(refresh, BorderLayout.NORTH);
        p.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        fillWorldState(model, statusProvider.sample(), maintenance.backupIndex());
        return p;
    }

    private static void fillWorldState(DefaultTableModel model, ServerRuntimeStatus status, List<Path> backups) {
        model.setRowCount(0);
        model.addRow(new Object[] {"Active world", status.activeWorld()});
        model.addRow(new Object[] {"Active world path", status.activeWorldPath() == null ? "<none>" : status.activeWorldPath().toString()});
        model.addRow(new Object[] {"Save slot count", Integer.toString(status.saveSlotCount())});
        model.addRow(new Object[] {"Backup count", Integer.toString(backups.size())});
        model.addRow(new Object[] {"Clients connected", Boolean.toString(status.clientsConnected())});
        model.addRow(new Object[] {"Save active", Boolean.toString(status.savingActive())});
        model.addRow(new Object[] {"Faction/production summary", "Runtime adapter pending."});
        model.addRow(new Object[] {"Ongoing faction plans", "Runtime adapter pending."});
    }

    private static JPanel backupPanel(ServerMaintenanceService maintenance) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Backup", "Path"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JButton refresh = new JButton("Refresh Backup Index");
        refresh.addActionListener(e -> fillBackups(model, maintenance.backupIndex()));
        p.add(refresh, BorderLayout.NORTH);
        p.add(new JScrollPane(new JTable(model)), BorderLayout.CENTER);
        fillBackups(model, maintenance.backupIndex());
        return p;
    }

    private static void fillBackups(DefaultTableModel model, List<Path> backups) {
        model.setRowCount(0);
        if (backups.isEmpty()) {
            model.addRow(new Object[] {"<none>", "No server backups found."});
            return;
        }
        for (Path backup : backups) model.addRow(new Object[] {backup.getFileName().toString(), backup.toString()});
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

    private static JPanel commandsPanel(String channel, ServerUpdateService updates, ServerMaintenanceService maintenance, ServerSafeRestartService restartService, ServerRuntimeStatusProvider statusProvider) {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = new DefaultTableModel(new Object[] {"Field", "Value"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable table = new JTable(model);
        JButton check = new JButton("Check for Updates");
        JButton update = new JButton("Apply Update If Safe");
        JButton safeUpdateRestart = new JButton("Safe Update + Restart Marker");
        JButton saveNow = new JButton("Save Now");
        JButton backup = new JButton("Create Backup");
        JButton restore = new JButton("Restore Latest Backup If Safe");

        check.addActionListener(e -> runUpdateStatus(model, () -> updates.checkForUpdates(channel)));
        update.addActionListener(e -> {
            ServerRuntimeStatus status = statusProvider.sample();
            runUpdateStatus(model, () -> updates.applyUpdate(channel, status.clientsConnected(), status.savingActive()));
        });
        safeUpdateRestart.addActionListener(e -> {
            ServerRuntimeStatus status = statusProvider.sample();
            runRestartStatus(model, () -> restartService.prepareUpdateRestart(channel, status));
        });
        saveNow.addActionListener(e -> {
            ServerRuntimeStatus status = statusProvider.sample();
            runMaintenanceStatus(model, () -> maintenance.saveNow(status.clientsConnected(), status.savingActive()));
        });
        backup.addActionListener(e -> runMaintenanceStatus(model, maintenance::createBackup));
        restore.addActionListener(e -> {
            ServerRuntimeStatus status = statusProvider.sample();
            runMaintenanceStatus(model, () -> maintenance.restoreLatestBackup(status.clientsConnected(), status.savingActive()));
        });

        JPanel buttons = new JPanel(new GridBagLayout());
        GridBagConstraints b = new GridBagConstraints();
        b.insets = new Insets(4, 4, 4, 4);
        b.gridx = 0; b.gridy = 0; buttons.add(check, b);
        b.gridx++; buttons.add(update, b);
        b.gridx++; buttons.add(safeUpdateRestart, b);
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

    private static void runRestartStatus(DefaultTableModel model, RestartStatusSupplier supplier) {
        model.setRowCount(0);
        model.addRow(new Object[] {"Status", "Running..."});
        new SwingWorker<ServerRestartPlan, Void>() {
            @Override protected ServerRestartPlan doInBackground() { return supplier.get(); }
            @Override protected void done() {
                model.setRowCount(0);
                try {
                    ServerRestartPlan plan = get();
                    model.addRow(new Object[] {"State", plan.state().toString()});
                    model.addRow(new Object[] {"Update applied", Boolean.toString(plan.updateApplied())});
                    model.addRow(new Object[] {"Restart required", Boolean.toString(plan.restartRequired())});
                    model.addRow(new Object[] {"Reason", plan.reason()});
                    model.addRow(new Object[] {"Created", plan.createdAt().toString()});
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

    private interface UpdateStatusSupplier { ServerUpdateStatus get(); }
    private interface MaintenanceStatusSupplier { ServerMaintenanceResult get(); }
    private interface RestartStatusSupplier { ServerRestartPlan get(); }
}
