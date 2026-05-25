package mechanist.server.admin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

public final class ServerSafeRestartService {
    private final Path serverRoot;
    private final ServerUpdateService updates;
    private final ServerMaintenanceService maintenance;
    private final ServerAdminLog adminLog;

    public ServerSafeRestartService(Path serverRoot, ServerUpdateService updates, ServerMaintenanceService maintenance, ServerAdminLog adminLog) {
        this.serverRoot = serverRoot == null ? Path.of(System.getProperty("user.dir"), "server").toAbsolutePath().normalize() : serverRoot.toAbsolutePath().normalize();
        this.updates = updates;
        this.maintenance = maintenance;
        this.adminLog = adminLog;
    }

    public ServerRestartPlan prepareUpdateRestart(String channel, ServerRuntimeStatus status) {
        ServerRuntimeStatus safeStatus = status == null
                ? new ServerRuntimeStatus(false, false, "<unknown>", null, 0, 0, "<unknown>", channel, "missing status; conservative fallback")
                : status;
        if (safeStatus.clientsConnected()) {
            return blocked(ServerRestartPlan.State.BLOCKED_CLIENTS_CONNECTED, "Connected clients are present. Use a graceful shutdown/drain step before updating.");
        }
        if (safeStatus.savingActive()) {
            return blocked(ServerRestartPlan.State.BLOCKED_SAVE_ACTIVE, "A save is active. Wait for save completion before updating.");
        }

        ServerMaintenanceResult backup = maintenance.createBackup();
        if (backup.state() == ServerMaintenanceResult.State.FAILED) {
            return failed("Pre-update backup failed: " + backup.message());
        }

        ServerUpdateStatus update = updates.applyUpdate(channel, safeStatus.clientsConnected(), safeStatus.savingActive());
        if (update.state() == ServerUpdateStatus.State.CHECK_FAILED || update.state() == ServerUpdateStatus.State.UPDATE_DEFERRED) {
            return failed("Update did not apply: " + update.message());
        }
        if (update.state() == ServerUpdateStatus.State.CURRENT) {
            return new ServerRestartPlan(ServerRestartPlan.State.NOT_REQUIRED, "Server package is already current.", false, false);
        }

        try {
            writeRestartMarker(channel, update, backup);
            ServerRestartPlan plan = new ServerRestartPlan(ServerRestartPlan.State.READY,
                    "Update applied and restart marker written. Close and relaunch the server executable to enter the updated runtime.", true, true);
            adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.UPDATE_APPLY, "system", plan.summary()));
            return plan;
        } catch (IOException ex) {
            return failed("Update applied, but restart marker failed: " + ex.getMessage());
        }
    }

    public Path restartMarkerFile() {
        return serverRoot.resolve("restart-required.properties");
    }

    private void writeRestartMarker(String channel, ServerUpdateStatus update, ServerMaintenanceResult backup) throws IOException {
        Files.createDirectories(serverRoot);
        Properties p = new Properties();
        p.setProperty("restart.required", "true");
        p.setProperty("restart.createdAt", Instant.now().toString());
        p.setProperty("restart.channel", channel == null || channel.isBlank() ? "main" : channel);
        p.setProperty("restart.updateState", update.state().name());
        p.setProperty("restart.currentVersion", update.currentVersion());
        p.setProperty("restart.availableVersion", update.availableVersion());
        p.setProperty("restart.backupPath", backup.path() == null ? "" : backup.path().toString());
        try (var out = Files.newOutputStream(restartMarkerFile())) {
            p.store(out, "The Mechanist server restart marker");
        }
        Path note = serverRoot.resolve("restart-required.txt");
        Files.writeString(note,
                "The Mechanist server package was updated and must be restarted before the updated runtime is active.\n"
                        + "Created: " + Instant.now() + "\n"
                        + "Channel: " + p.getProperty("restart.channel") + "\n"
                        + "Backup: " + p.getProperty("restart.backupPath") + "\n",
                StandardCharsets.UTF_8);
    }

    private ServerRestartPlan blocked(ServerRestartPlan.State state, String reason) {
        ServerRestartPlan plan = new ServerRestartPlan(state, reason, false, false);
        adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.COMMAND_DENIED, "system", plan.summary()));
        return plan;
    }

    private ServerRestartPlan failed(String reason) {
        ServerRestartPlan plan = new ServerRestartPlan(ServerRestartPlan.State.FAILED, reason, false, false);
        adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.COMMAND_DENIED, "system", plan.summary()));
        return plan;
    }
}
