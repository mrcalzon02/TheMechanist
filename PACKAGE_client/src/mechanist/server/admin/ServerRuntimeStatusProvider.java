package mechanist.server.admin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.stream.Stream;

public final class ServerRuntimeStatusProvider {
    private final Path gameRoot;
    private final Path serverRoot;
    private final ServerMaintenanceService maintenance;
    private final String channel;

    public ServerRuntimeStatusProvider(Path gameRoot, Path serverRoot, ServerMaintenanceService maintenance, String channel) {
        this.gameRoot = gameRoot == null ? Path.of(System.getProperty("user.dir")) : gameRoot.toAbsolutePath().normalize();
        this.serverRoot = serverRoot == null ? this.gameRoot.resolve("server") : serverRoot.toAbsolutePath().normalize();
        this.maintenance = maintenance;
        this.channel = channel == null || channel.isBlank() ? "main" : channel;
    }

    public ServerRuntimeStatus sample() {
        Path stateFile = serverRoot.resolve("server_state.properties");
        Properties state = readProperties(stateFile);
        Path worldDir = serverRoot.resolve("worlds");
        Path slotDir = serverRoot.resolve("slots");
        String activeWorld = firstNonBlank(
                state.getProperty("server.worldName"),
                state.getProperty("server.worldId"),
                newestFileName(worldDir, ".mechworld"),
                "<none>");
        Path activeWorldPath = activeWorldPath(worldDir, activeWorld);
        int saveSlotCount = countFiles(slotDir, ".mechsave");
        int backupCount = maintenance == null ? countDirectories(serverRoot.resolve("backups")) : maintenance.backupIndex().size();
        boolean clientsConnected = Boolean.parseBoolean(state.getProperty("server.clientsConnected", "false"));
        boolean savingActive = Boolean.parseBoolean(state.getProperty("server.savingActive", "false"));
        String hostStatus = firstNonBlank(state.getProperty("server.hosting.success"), "false").equals("true")
                ? "hosting " + state.getProperty("server.hosting.address", "<unknown>") + ":" + state.getProperty("server.hosting.port", "?")
                : "not hosting / no live adapter attached";
        String adapter = Files.isRegularFile(stateFile) ? "state-file sampled" : "placeholder status; live runtime adapter not attached";
        return new ServerRuntimeStatus(clientsConnected, savingActive, activeWorld, activeWorldPath,
                saveSlotCount, backupCount, hostStatus, channel, adapter);
    }

    private static Properties readProperties(Path path) {
        Properties p = new Properties();
        if (path == null || !Files.isRegularFile(path)) return p;
        try (var in = Files.newInputStream(path)) { p.load(in); }
        catch (Exception ignored) { }
        return p;
    }

    private static Path activeWorldPath(Path worldDir, String activeWorld) {
        if (activeWorld == null || activeWorld.isBlank() || "<none>".equals(activeWorld)) return null;
        Path direct = worldDir.resolve(activeWorld);
        if (Files.isRegularFile(direct)) return direct;
        Path mech = worldDir.resolve(activeWorld + ".mechworld");
        return Files.isRegularFile(mech) ? mech : null;
    }

    private static String newestFileName(Path root, String suffix) {
        if (root == null || !Files.isDirectory(root)) return null;
        try (Stream<Path> stream = Files.list(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(p -> suffix == null || p.getFileName().toString().endsWith(suffix))
                    .max((a, b) -> Long.compare(modified(a), modified(b)))
                    .map(p -> p.getFileName().toString())
                    .orElse(null);
        } catch (Exception ex) { return null; }
    }

    private static int countFiles(Path root, String suffix) {
        if (root == null || !Files.isDirectory(root)) return 0;
        try (Stream<Path> stream = Files.list(root)) {
            return (int) stream.filter(Files::isRegularFile)
                    .filter(p -> suffix == null || p.getFileName().toString().endsWith(suffix))
                    .count();
        } catch (Exception ex) { return 0; }
    }

    private static int countDirectories(Path root) {
        if (root == null || !Files.isDirectory(root)) return 0;
        try (Stream<Path> stream = Files.list(root)) {
            return (int) stream.filter(Files::isDirectory).count();
        } catch (Exception ex) { return 0; }
    }

    private static long modified(Path p) {
        try { return Files.getLastModifiedTime(p).toMillis(); }
        catch (Exception ex) { return 0L; }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }
}
