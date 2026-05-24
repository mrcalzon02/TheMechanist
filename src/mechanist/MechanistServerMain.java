package mechanist;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/** Headless server executable entry point, save-path initializer, and optional host binder. */
public final class MechanistServerMain {
    static final String VERSION = "mechanist-server-main-0.9.10ib";

    public static void main(String[] args) {
        DebugLog.init("0.9.10ib-server");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> DebugLog.shutdown("server JVM shutdown hook executed."), "mechanist-server-log-shutdown"));
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> DebugLog.error("SERVER_UNHANDLED_THREAD", "Thread " + t.getName() + " threw outside guarded execution.", e));
        int exit = 0;
        ServerRuntime runtime = null;
        try {
            runtime = ServerRuntime.initialize(args == null ? new String[0] : args);
            System.out.println(runtime.statusLine());
            if (runtime.helpRequested()) System.out.println(runtime.usageText());
            runtime.awaitIfHosting();
        } catch (Throwable t) {
            exit = 1;
            DebugLog.error("SERVER_BOOT", "Headless server executable failed.", t);
            System.err.println("The Mechanist server failed: " + t.getMessage());
        } finally {
            if (runtime != null) runtime.closeQuietly();
            DebugLog.shutdown("server main completed exit=" + exit);
        }
        if (exit != 0) System.exit(exit);
    }

    private MechanistServerMain() { }
}

final class ServerRuntime {
    private final Properties state;
    private final boolean helpRequested;
    private final boolean hosting;
    private final boolean hostOnce;
    private final HostBindingResult hostBinding;
    private final SecureServerNetworkingCore securityCore;
    private final CountDownLatch keepAlive = new CountDownLatch(1);

    private ServerRuntime(Properties state, boolean helpRequested, boolean hosting, boolean hostOnce, HostBindingResult hostBinding, SecureServerNetworkingCore securityCore) {
        this.state = state;
        this.helpRequested = helpRequested;
        this.hosting = hosting;
        this.hostOnce = hostOnce;
        this.hostBinding = hostBinding;
        this.securityCore = securityCore;
    }

    static ServerRuntime initialize(String[] args) throws IOException {
        ServerRuntimePaths.ensureServerDirectories();
        boolean help = hasFlag(args, "--help") || hasFlag(args, "-h");
        boolean host = hasFlag(args, "--host") || hasFlag(args, "--serve") || hasFlag(args, "--host-once");
        boolean hostOnce = hasFlag(args, "--host-once") || hasFlag(args, "--bind-check");
        boolean preferSteam = !hasFlag(args, "--no-steam");
        Properties state = loadState();
        if (state.getProperty("server.id", "").isBlank()) state.setProperty("server.id", UUID.randomUUID().toString());
        state.setProperty("server.version", MechanistServerMain.VERSION);
        state.setProperty("server.updated", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        state.setProperty("server.saveFile", ServerRuntimePaths.serverStateFile().toString());
        state.setProperty("server.worldDir", ServerRuntimePaths.serverWorldDir().toString());
        state.setProperty("server.slotDir", ServerRuntimePaths.serverSlotDir().toString());
        state.setProperty("singlePlayer.saveDir", ServerRuntimePaths.singlePlayerRoot().toString());
        for (int i = 1; i <= GamePanel.SAVE_SLOT_COUNT; i++) state.setProperty("server.slot." + i, ServerRuntimePaths.serverSlotPath(i).toString());

        SecureServerNetworkingCore securityCore = SecureServerNetworkingCore.initialize(ServerRuntimePaths.serverRoot());
        HostBindingResult binding = null;
        if (host) {
            ServerConfig config = configFromArgs(args, state, preferSteam);
            binding = MultiplayerHostBindingService.bind(config);
            state.setProperty("server.hosting.requested", "true");
            state.setProperty("server.hosting.success", String.valueOf(binding.success()));
            state.setProperty("server.hosting.protocol", binding.protocolState().name());
            state.setProperty("server.hosting.address", binding.boundAddress());
            state.setProperty("server.hosting.port", String.valueOf(binding.port()));
            state.setProperty("server.hosting.transport", binding.transportName());
            state.setProperty("server.hosting.message", binding.message());
        } else {
            state.setProperty("server.hosting.requested", "false");
        }

        storeState(state);
        DebugLog.audit("SERVER_RUNTIME_PATHS", ServerRuntimePaths.auditSummary());
        DebugLog.audit("SERVER_NETWORK_POLICY", NetworkPortAuthority.policySummary());
        DebugLog.audit("SERVER_MULTIPLAYER_BINDING", MultiplayerHostBindingService.auditSummary());
        JvmRuntimeProfileAuthority.RuntimeConfig jvmProfile = JvmRuntimeProfileAuthority.effectiveServerProfile(JvmRuntimeProfileAuthority.load());
        DebugLog.audit("JVM_RUNTIME_PROFILE", JvmRuntimeProfileAuthority.auditSummary(jvmProfile));
        DebugLog.audit("SERVER_RUNTIME_INIT", "args=" + Arrays.toString(args) + " file=" + ServerRuntimePaths.serverStateFile().toAbsolutePath());
        return new ServerRuntime(state, help, host && binding != null && binding.success(), hostOnce, binding, securityCore);
    }

    boolean helpRequested() { return helpRequested; }

    void awaitIfHosting() throws InterruptedException {
        if (!hosting || hostOnce) return;
        Runtime.getRuntime().addShutdownHook(new Thread(keepAlive::countDown, "mechanist-server-keepalive-release"));
        System.out.println("The Mechanist server is hosting. Press Ctrl+C to stop.");
        keepAlive.await();
    }

    void closeQuietly() {
        if (hostBinding != null) {
            try { hostBinding.close(); } catch (Exception ex) { DebugLog.error("SERVER_HOST_CLOSE", "Could not close host binding.", ex); }
        }
        if (securityCore != null) {
            try { securityCore.close(); } catch (Exception ex) { DebugLog.error("SERVER_SECURITY_CLOSE", "Could not close secure server core.", ex); }
        }
    }

    String statusLine() {
        String hostLine = hostBinding == null ? " host=closed" : " host=" + hostBinding.compactLine();
        return "The Mechanist server runtime is initialized. saveFile=" + state.getProperty("server.saveFile")
                + " worldDir=" + state.getProperty("server.worldDir")
                + " slotDir=" + state.getProperty("server.slotDir")
                + " singlePlayerSaveDir=" + state.getProperty("singlePlayer.saveDir")
                + " slots=" + GamePanel.SAVE_SLOT_COUNT
                + hostLine
                + " effectiveJvmProfile=" + JvmRuntimeProfileAuthority.auditSummary(JvmRuntimeProfileAuthority.effectiveServerProfile(JvmRuntimeProfileAuthority.load()));
    }

    String usageText() {
        return "Usage: java -jar TheMechanistServer.jar [--status|--init|--help|--host|--host-once] [--world-name=NAME] [--seed=N] [--difficulty=TEXT] [--max-players=N] [--port=25500-25599] [--bind=::|0.0.0.0] [--setup=encoded] [--no-steam]\n"
                + "The server initializes the headless save namespace and can bind a blind encrypted-packet relay. It avoids system ports and Steam query ports.\n"
                + "Server state: " + ServerRuntimePaths.serverStateFile() + "\n"
                + "Server world definitions: " + ServerRuntimePaths.serverWorldDir() + "\n"
                + "Server save slots: " + ServerRuntimePaths.serverSlotDir() + "\n"
                + "Desktop single-player saves: " + ServerRuntimePaths.singlePlayerRoot() + "\n"
                + NetworkPortAuthority.policySummary();
    }

    private static ServerConfig configFromArgs(String[] args, Properties state, boolean preferSteam) {
        long seed = parseLong(valueOf(args, "--seed="), System.currentTimeMillis());
        String worldName = valueOr(valueOf(args, "--world-name="), state.getProperty("server.worldName", "Mechanist Hosted Hive"));
        String worldId = valueOr(valueOf(args, "--world-id="), state.getProperty("server.worldId", "server-world"));
        String setup = valueOr(valueOf(args, "--setup="), WorldSetupSettings.standard().encode());
        WorldSetupSettings settings = WorldSetupSettings.decode(setup);
        int maxPlayers = parseInt(valueOf(args, "--max-players="), ServerConfig.DEFAULT_MAX_PLAYERS, 1, 64);
        int requestedPort = parseInt(valueOf(args, "--port="), 0, NetworkPortAuthority.CUSTOM_GAME_PORT_MIN, NetworkPortAuthority.CUSTOM_GAME_PORT_MAX);
        if (requestedPort == 0) requestedPort = NetworkPortAuthority.firstAvailableGamePort();
        String bind = valueOr(valueOf(args, "--bind="), "::");
        String difficulty = valueOf(args, "--difficulty=");
        ServerConfig base = ServerConfig.fromWorldSettings(seed, worldName, worldId, settings, maxPlayers, requestedPort, bind, MultiplayerProtocolState.CLOSED, preferSteam);
        if (difficulty != null && !difficulty.isBlank()) {
            return new ServerConfig(base.seed(), base.worldName(), base.worldId(), difficulty, base.maxPlayers(), base.port(), base.boundAddress(), base.protocolState(), base.worldSetupEncoded(), base.steamPreferred(), base.createdAtIso());
        }
        return base;
    }

    private static boolean hasFlag(String[] args, String flag) {
        if (args == null) return false;
        for (String arg : args) if (flag.equalsIgnoreCase(arg == null ? "" : arg.trim())) return true;
        return false;
    }

    private static String valueOf(String[] args, String prefix) {
        if (args == null) return null;
        for (String arg : args) if (arg != null && arg.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) return arg.substring(prefix.length());
        return null;
    }

    private static String valueOr(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }

    private static int parseInt(String value, int fallback, int min, int max) {
        if (value == null || value.isBlank()) return fallback;
        try { return Math.max(min, Math.min(max, Integer.parseInt(value.trim()))); }
        catch (NumberFormatException ex) { return fallback; }
    }

    private static long parseLong(String value, long fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return Long.parseLong(value.trim()); }
        catch (NumberFormatException ex) { return fallback; }
    }

    private static Properties loadState() throws IOException {
        Path file = ServerRuntimePaths.serverStateFile();
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) { p.load(in); }
        } else {
            p.setProperty("server.created", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }
        return p;
    }

    private static void storeState(Properties p) throws IOException {
        ServerRuntimePaths.ensureServerDirectories();
        try (OutputStream out = Files.newOutputStream(ServerRuntimePaths.serverStateFile())) {
            p.store(out, "The Mechanist headless server runtime state");
        }
    }
}
