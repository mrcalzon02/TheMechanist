package mechanist;

import java.io.File;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Headless server entrypoint for the server artifact.
 *
 * This class deliberately does not touch Swing, ImageCache, SoundManager, or
 * client art/audio systems. It gives the packaging pipeline a real server main
 * class so the server jar can stop being a byte-for-byte copy of the client jar.
 */
public final class TheMechanistServer {
    private TheMechanistServer() {}

    public static void main(String[] args) {
        ServerOptions options = ServerOptions.parse(args);
        if (options.help) {
            printUsage();
            return;
        }

        System.out.println("The Mechanist headless server bootstrap");
        System.out.println("startedUtc=" + Instant.now());
        System.out.println("runtime=" + RuntimePathResolver.workingDirectorySummary());
        System.out.println("bindHost=" + options.host);
        System.out.println("port=" + options.port);
        System.out.println("world=" + options.worldName);

        File serverRoot = RuntimePathResolver.resolveAssetFile("server");
        System.out.println("serverRoot=" + serverRoot.getPath());
        System.out.println("status=SERVER_ENTRYPOINT_READY");
        System.out.println("note=Networking loop is intentionally separated from the Swing client artifact and will be attached here as the server layer is extracted.");
    }

    private static void printUsage() {
        System.out.println("TheMechanistServer usage:");
        System.out.println("  java -jar TheMechanistServer.jar [--host 0.0.0.0] [--port 28777] [--world default]");
        System.out.println("Options:");
        System.out.println("  --host <host>   Bind host. Default: 0.0.0.0");
        System.out.println("  --port <port>   Bind port. Default: 28777");
        System.out.println("  --world <name>  World/save identifier. Default: default");
        System.out.println("  --help          Show this help.");
    }

    static final class ServerOptions {
        final String host;
        final int port;
        final String worldName;
        final boolean help;

        ServerOptions(String host, int port, String worldName, boolean help) {
            this.host = host;
            this.port = port;
            this.worldName = worldName;
            this.help = help;
        }

        static ServerOptions parse(String[] args) {
            Map<String,String> values = new LinkedHashMap<>();
            boolean help = false;
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    String arg = args[i];
                    if ("--help".equals(arg) || "-h".equals(arg) || "/?".equals(arg)) {
                        help = true;
                    } else if (arg != null && arg.startsWith("--")) {
                        String key = arg.substring(2);
                        String value = "true";
                        if (i + 1 < args.length && args[i + 1] != null && !args[i + 1].startsWith("--")) {
                            value = args[++i];
                        }
                        values.put(key, value);
                    }
                }
            }
            String host = values.getOrDefault("host", "0.0.0.0");
            int port = parsePort(values.get("port"));
            String world = values.getOrDefault("world", "default");
            return new ServerOptions(host, port, world, help);
        }

        private static int parsePort(String raw) {
            if (raw == null || raw.isBlank()) return 28777;
            try {
                int value = Integer.parseInt(raw.trim());
                if (value < 1 || value > 65535) return 28777;
                return value;
            } catch (NumberFormatException ignored) {
                return 28777;
            }
        }
    }
}
