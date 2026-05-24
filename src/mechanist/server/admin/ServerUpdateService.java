package mechanist.server.admin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class ServerUpdateService {
    private final Path gameRoot;
    private final ServerAdminLog adminLog;

    public ServerUpdateService(Path gameRoot, ServerAdminLog adminLog) {
        this.gameRoot = gameRoot;
        this.adminLog = adminLog;
    }

    public ServerUpdateStatus checkForUpdates(String channel) {
        String safeChannel = channel == null || channel.isBlank() ? "main" : channel.trim();
        adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.UPDATE_CHECK, "admin", "Checking server updates for channel=" + safeChannel));
        if (!Files.isDirectory(gameRoot)) {
            ServerUpdateStatus status = new ServerUpdateStatus(ServerUpdateStatus.State.CHECK_FAILED,
                    currentVersion(), "<unknown>", safeChannel, "Game root missing: " + gameRoot, false);
            adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.UPDATE_CHECK, "system", status.summary()));
            return status;
        }
        try {
            int fetch = run(gameRoot, "git", "fetch", "origin", safeChannel);
            if (fetch != 0) return failed(safeChannel, "git fetch failed with exit code " + fetch);
            String local = output(gameRoot, "git", "rev-parse", "HEAD");
            String remote = output(gameRoot, "git", "rev-parse", "origin/" + safeChannel);
            if (local == null || remote == null) return failed(safeChannel, "Could not resolve local or remote revision.");
            boolean update = !local.trim().equals(remote.trim());
            ServerUpdateStatus status = new ServerUpdateStatus(
                    update ? ServerUpdateStatus.State.UPDATE_AVAILABLE : ServerUpdateStatus.State.CURRENT,
                    local.trim(), remote.trim(), safeChannel,
                    update ? "Server package update is available." : "Server package is current.",
                    update);
            adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.UPDATE_CHECK, "system", status.summary()));
            return status;
        } catch (Exception ex) {
            return failed(safeChannel, ex.getMessage());
        }
    }

    public ServerUpdateStatus applyUpdate(String channel, boolean clientsConnected, boolean savingActive) {
        String safeChannel = channel == null || channel.isBlank() ? "main" : channel.trim();
        if (clientsConnected || savingActive) {
            String reason = "Update deferred: clientsConnected=" + clientsConnected + " savingActive=" + savingActive;
            ServerUpdateStatus status = new ServerUpdateStatus(ServerUpdateStatus.State.UPDATE_DEFERRED,
                    currentVersion(), "<unknown>", safeChannel, reason, true);
            adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.UPDATE_APPLY, "system", reason));
            return status;
        }
        adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.UPDATE_APPLY, "admin", "Applying server update channel=" + safeChannel));
        try {
            int fetch = run(gameRoot, "git", "fetch", "origin", safeChannel);
            if (fetch != 0) return failed(safeChannel, "git fetch failed with exit code " + fetch);
            int reset = run(gameRoot, "git", "reset", "--hard", "origin/" + safeChannel);
            if (reset != 0) return failed(safeChannel, "git reset failed with exit code " + reset);
            ServerUpdateStatus status = new ServerUpdateStatus(ServerUpdateStatus.State.UPDATE_READY_FOR_RESTART,
                    currentVersion(), output(gameRoot, "git", "rev-parse", "HEAD"), safeChannel,
                    "Server package updated. Restart is required before running the new executable/runtime state.", true);
            adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.UPDATE_APPLY, "system", status.summary()));
            return status;
        } catch (Exception ex) {
            return failed(safeChannel, ex.getMessage());
        }
    }

    private ServerUpdateStatus failed(String channel, String message) {
        ServerUpdateStatus status = new ServerUpdateStatus(ServerUpdateStatus.State.CHECK_FAILED,
                currentVersion(), "<unknown>", channel, message, false);
        adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.UPDATE_CHECK, "system", status.summary()));
        return status;
    }

    private String currentVersion() {
        try {
            String out = output(gameRoot, "git", "rev-parse", "HEAD");
            return out == null || out.isBlank() ? "<unknown>" : out.trim();
        } catch (Exception ex) {
            return "<unknown>";
        }
    }

    private static int run(Path cwd, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(cwd.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.getInputStream().transferTo(OutputStreamSink.INSTANCE);
        boolean finished = process.waitFor(120, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return 124;
        }
        return process.exitValue();
    }

    private static String output(Path cwd, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(cwd.toFile());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
        }
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return null;
        }
        return process.exitValue() == 0 ? String.join("\n", lines) : null;
    }

    private static final class OutputStreamSink extends java.io.OutputStream {
        static final OutputStreamSink INSTANCE = new OutputStreamSink();
        @Override public void write(int b) { }
    }
}
