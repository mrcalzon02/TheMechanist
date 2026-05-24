package mechanist;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.util.Locale;

/** Port-policy and address parsing authority for multiplayer host/join surfaces. */
final class NetworkPortAuthority {
    static final String VERSION = "network-port-authority-0.9.10hs";
    static final int WELL_KNOWN_PORT_MAX = 1023;
    static final int STEAM_QUERY_PORT_MIN = 27015;
    static final int STEAM_QUERY_PORT_MAX = 27050;
    static final int CUSTOM_GAME_PORT_MIN = 25500;
    static final int CUSTOM_GAME_PORT_MAX = 25599;
    static final int DEFAULT_GAME_PORT = 25565;

    private NetworkPortAuthority() { }

    static boolean portWithinAllowedGameRange(int port) {
        return port >= CUSTOM_GAME_PORT_MIN && port <= CUSTOM_GAME_PORT_MAX && !isPolicyReserved(port);
    }

    static boolean isPolicyReserved(int port) {
        return port >= 0 && port <= WELL_KNOWN_PORT_MAX || (port >= STEAM_QUERY_PORT_MIN && port <= STEAM_QUERY_PORT_MAX);
    }

    static int firstAvailableGamePort() {
        return firstAvailableGamePort(CUSTOM_GAME_PORT_MIN, CUSTOM_GAME_PORT_MAX);
    }

    static int firstAvailableGamePort(int minInclusive, int maxInclusive) {
        int min = Math.max(CUSTOM_GAME_PORT_MIN, minInclusive);
        int max = Math.min(CUSTOM_GAME_PORT_MAX, maxInclusive);
        for (int port = min; port <= max; port++) {
            if (portWithinAllowedGameRange(port) && tcpPortAvailable(port)) return port;
        }
        throw new IllegalStateException("No available TCP port in configured game range " + min + "-" + max);
    }

    static boolean tcpPortAvailable(int port) {
        if (!portWithinAllowedGameRange(port)) return false;
        boolean ipv6Checked = false;
        try (ServerSocketChannel channel = ServerSocketChannel.open(java.net.StandardProtocolFamily.INET6)) {
            channel.setOption(java.net.StandardSocketOptions.SO_REUSEADDR, Boolean.FALSE);
            channel.bind(new InetSocketAddress(InetAddress.getByName("::"), port));
            ipv6Checked = true;
        } catch (IOException | RuntimeException ex) {
            DebugLog.audit("PORT_SCAN", "IPv6 availability check failed for port=" + port + " reason=" + ex.getMessage());
        }
        try (ServerSocket socket = new ServerSocket()) {
            socket.setReuseAddress(false);
            socket.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port));
            return true;
        } catch (IOException | RuntimeException ex) {
            if (!ipv6Checked) DebugLog.audit("PORT_SCAN", "IPv4 availability check failed for port=" + port + " reason=" + ex.getMessage());
            return false;
        }
    }

    static Endpoint parseEndpoint(String raw) {
        return parseEndpoint(raw, DEFAULT_GAME_PORT);
    }

    static Endpoint parseEndpoint(String raw, int fallbackPort) {
        String text = raw == null ? "" : raw.trim();
        int defaultPort = portWithinAllowedGameRange(fallbackPort) ? fallbackPort : DEFAULT_GAME_PORT;
        if (text.isEmpty()) return new Endpoint("127.0.0.1", defaultPort, "127.0.0.1:" + defaultPort, false);
        if (text.startsWith("[")) {
            int close = text.indexOf(']');
            if (close > 1) {
                String host = text.substring(1, close).trim();
                int port = defaultPort;
                if (close + 1 < text.length() && text.charAt(close + 1) == ':') {
                    port = parsePort(text.substring(close + 2), defaultPort);
                }
                return new Endpoint(host, port, "[" + host + "]:" + port, true);
            }
        }
        int colonCount = 0;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) == ':') colonCount++;
        if (colonCount == 0) return new Endpoint(text, defaultPort, text + ":" + defaultPort, false);
        if (colonCount == 1) {
            int idx = text.lastIndexOf(':');
            String host = idx <= 0 ? "127.0.0.1" : text.substring(0, idx).trim();
            int port = parsePort(text.substring(idx + 1), defaultPort);
            return new Endpoint(host, port, host + ":" + port, false);
        }
        // Bracketless IPv6 literals are accepted as host-only and use the default port.
        return new Endpoint(text, defaultPort, "[" + text + "]:" + defaultPort, true);
    }

    static int parsePort(String raw, int fallback) {
        try {
            int port = Integer.parseInt(raw == null ? "" : raw.trim());
            return portWithinAllowedGameRange(port) ? port : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    static String policySummary() {
        return "authority=" + VERSION + " gameRange=" + CUSTOM_GAME_PORT_MIN + "-" + CUSTOM_GAME_PORT_MAX
                + " avoidsWellKnown=0-" + WELL_KNOWN_PORT_MAX + " avoidsSteamQuery=" + STEAM_QUERY_PORT_MIN + "-" + STEAM_QUERY_PORT_MAX;
    }

    record Endpoint(String host, int port, String display, boolean ipv6Literal) {
        Endpoint {
            host = host == null || host.isBlank() ? "127.0.0.1" : host.trim();
            port = NetworkPortAuthority.portWithinAllowedGameRange(port) ? port : DEFAULT_GAME_PORT;
            display = display == null || display.isBlank()
                    ? (ipv6Literal ? "[" + host + "]:" + port : host + ":" + port)
                    : display.trim();
        }
        String normalizedHostKey() { return host.toLowerCase(Locale.ROOT) + ":" + port; }
    }
}
