package mechanist.server.admin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class ServerMaintenanceService {
    private final Path serverRoot;
    private final Path slotRoot;
    private final Path backupRoot;
    private final ServerAdminLog adminLog;

    public ServerMaintenanceService(Path serverRoot, ServerAdminLog adminLog) {
        this.serverRoot = normalize(serverRoot);
        this.slotRoot = this.serverRoot.resolve("slots").normalize();
        this.backupRoot = this.serverRoot.resolve("backups").normalize();
        this.adminLog = adminLog;
    }

    public ServerMaintenanceResult saveNow(boolean clientsConnected, boolean savingActive) {
        if (savingActive) return deferred("save-now", "A save is already active; refusing overlapping save request.", null);
        adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.SAVE_NOW, "admin", "Save-now requested; live runtime save adapter pending."));
        return new ServerMaintenanceResult(ServerMaintenanceResult.State.DEFERRED, "save-now",
                "Save request recorded. Live world save adapter is not attached yet.", slotRoot);
    }

    public ServerMaintenanceResult createBackup() {
        try {
            ensureRoots();
            String stamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-');
            Path backupDir = guard(backupRoot.resolve("backup-" + stamp));
            Files.createDirectories(backupDir);
            int copied = copyTree(slotRoot, backupDir.resolve("slots"));
            adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.BACKUP_CREATE, "admin", "Created backup files=" + copied + " path=" + backupDir));
            return new ServerMaintenanceResult(ServerMaintenanceResult.State.OK, "backup-create",
                    "Backup created with " + copied + " files.", backupDir);
        } catch (Exception ex) {
            return failed("backup-create", ex.getMessage(), backupRoot);
        }
    }

    public ServerMaintenanceResult restoreLatestBackup(boolean clientsConnected, boolean savingActive) {
        if (clientsConnected || savingActive) {
            return deferred("backup-restore", "Restore deferred: clientsConnected=" + clientsConnected + " savingActive=" + savingActive, backupRoot);
        }
        try {
            ensureRoots();
            Path latest = latestBackup();
            if (latest == null) return failed("backup-restore", "No backup directories found.", backupRoot);
            Path sourceSlots = guard(latest.resolve("slots"));
            if (!Files.isDirectory(sourceSlots)) return failed("backup-restore", "Backup has no slots directory: " + latest, latest);
            deleteChildren(slotRoot);
            int copied = copyTree(sourceSlots, slotRoot);
            adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.BACKUP_RESTORE, "admin", "Restored latest backup files=" + copied + " path=" + latest));
            return new ServerMaintenanceResult(ServerMaintenanceResult.State.OK, "backup-restore",
                    "Restored latest backup with " + copied + " files. Restart/reload recommended.", latest);
        } catch (Exception ex) {
            return failed("backup-restore", ex.getMessage(), backupRoot);
        }
    }

    public List<Path> backupIndex() {
        try {
            ensureRoots();
            try (Stream<Path> stream = Files.list(backupRoot)) {
                return stream.filter(Files::isDirectory)
                        .sorted(Comparator.comparing((Path p) -> modified(p)).reversed())
                        .toList();
            }
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void ensureRoots() throws IOException {
        Files.createDirectories(serverRoot);
        Files.createDirectories(slotRoot);
        Files.createDirectories(backupRoot);
    }

    private Path latestBackup() throws IOException {
        try (Stream<Path> stream = Files.list(backupRoot)) {
            return stream.filter(Files::isDirectory)
                    .max(Comparator.comparing(ServerMaintenanceService::modified))
                    .orElse(null);
        }
    }

    private int copyTree(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source)) return 0;
        Path guardedTarget = guard(target);
        Files.createDirectories(guardedTarget);
        ArrayList<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(source)) {
            stream.filter(Files::isRegularFile).forEach(files::add);
        }
        for (Path file : files) {
            Path rel = source.relativize(file);
            Path dest = guard(guardedTarget.resolve(rel));
            if (dest.getParent() != null) Files.createDirectories(dest.getParent());
            Files.copy(file, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        }
        return files.size();
    }

    private void deleteChildren(Path root) throws IOException {
        Path guardedRoot = guard(root);
        Files.createDirectories(guardedRoot);
        try (Stream<Path> stream = Files.walk(guardedRoot)) {
            List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            for (Path p : paths) {
                if (!p.equals(guardedRoot)) Files.deleteIfExists(p);
            }
        }
    }

    private Path guard(Path path) throws IOException {
        Path normalized = normalize(path);
        if (!normalized.startsWith(serverRoot)) throw new IOException("Refusing path outside server root: " + normalized);
        return normalized;
    }

    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }

    private ServerMaintenanceResult deferred(String action, String message, Path path) {
        ServerMaintenanceResult result = new ServerMaintenanceResult(ServerMaintenanceResult.State.DEFERRED, action, message, path);
        adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.COMMAND_DENIED, "system", result.summary()));
        return result;
    }

    private ServerMaintenanceResult failed(String action, String message, Path path) {
        ServerMaintenanceResult result = new ServerMaintenanceResult(ServerMaintenanceResult.State.FAILED, action, message, path);
        adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.COMMAND_DENIED, "system", result.summary()));
        return result;
    }

    private static long modified(Path p) {
        try { return Files.getLastModifiedTime(p).toMillis(); }
        catch (Exception ex) { return 0L; }
    }
}
