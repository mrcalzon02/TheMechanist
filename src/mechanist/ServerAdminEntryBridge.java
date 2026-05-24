package mechanist;

import mechanist.server.admin.ServerAdminConsole;
import mechanist.server.admin.ServerAdminEvent;
import mechanist.server.admin.ServerAdminLog;
import mechanist.server.admin.ServerUpdateService;
import mechanist.server.admin.ServerUpdateStatus;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

final class ServerAdminEntryBridge {
    private ServerAdminEntryBridge() {}

    static boolean handleIfRequested(String[] args) throws Exception {
        String[] safeArgs = args == null ? new String[0] : args;
        boolean checkUpdates = hasFlag(safeArgs, "--check-updates");
        boolean update = hasFlag(safeArgs, "--update");
        boolean adminGui = hasFlag(safeArgs, "--admin-gui");
        if (!checkUpdates && !update && !adminGui) return false;

        ServerRuntimePaths.ensureServerDirectories();
        Path gameRoot = pathValue(safeArgs, "--game-root=", Path.of(System.getProperty("user.dir")));
        Path logRoot = pathValue(safeArgs, "--log-root=", ServerRuntimePaths.serverRoot().resolve("logs"));
        String channel = valueOr(valueOf(safeArgs, "--channel="), "main");
        ServerAdminLog adminLog = new ServerAdminLog(logRoot.resolve("server-admin.log"));
        ServerUpdateService updates = new ServerUpdateService(gameRoot, adminLog);

        if (checkUpdates) {
            ServerUpdateStatus status = updates.checkForUpdates(channel);
            System.out.println(status.summary());
            return true;
        }
        if (update) {
            boolean clientsConnected = hasFlag(safeArgs, "--clients-connected");
            boolean savingActive = hasFlag(safeArgs, "--saving-active");
            ServerUpdateStatus status = updates.applyUpdate(channel, clientsConnected, savingActive);
            System.out.println(status.summary());
            return true;
        }
        if (adminGui) {
            if (GraphicsEnvironment.isHeadless()) {
                System.out.println("Headless environment detected. Use --check-updates, --update, --host, or --help.");
                return true;
            }
            Files.createDirectories(logRoot);
            adminLog.record(new ServerAdminEvent(ServerAdminEvent.Kind.SERVER_START, "admin", "Opening local server administrator console."));
            SwingUtilities.invokeLater(() -> ServerAdminConsole.show(gameRoot, logRoot, channel, updates, adminLog));
            return true;
        }
        return false;
    }

    static String usageAddendum() {
        return "\nAdmin/update commands:\n"
                + "  --admin-gui [--game-root=PATH] [--log-root=PATH] [--channel=main]\n"
                + "  --check-updates [--game-root=PATH] [--log-root=PATH] [--channel=main]\n"
                + "  --update [--game-root=PATH] [--log-root=PATH] [--channel=main] [--clients-connected] [--saving-active]\n"
                + "Update application is guarded and should be routed through safe shutdown/restart policy once live clients are present.\n";
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) if (flag.equalsIgnoreCase(arg == null ? "" : arg.trim())) return true;
        return false;
    }

    private static String valueOf(String[] args, String prefix) {
        for (String arg : args) if (arg != null && arg.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) return arg.substring(prefix.length());
        return null;
    }

    private static String valueOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    private static Path pathValue(String[] args, String prefix, Path fallback) {
        String value = valueOf(args, prefix);
        return value == null || value.isBlank() ? fallback : Path.of(value).toAbsolutePath().normalize();
    }
}
