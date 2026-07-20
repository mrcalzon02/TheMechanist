package mechanist;

import java.io.IOException;

/** Layered host-binding authority: Steam relay, exact Netty seam, then exact native Java NIO. */
final class MultiplayerHostBindingService {
    static final String VERSION = "multiplayer-host-binding-service-0.9.10hs";

    private MultiplayerHostBindingService() { }

    static HostBindingResult bind(ServerConfig requestedConfig) {
        ServerConfig config = requestedConfig == null
                ? ServerConfig.fromWorldSettings(System.currentTimeMillis(), "Unnamed Hive World", "local-world",
                WorldSetupSettings.standard(), ServerConfig.DEFAULT_MAX_PLAYERS, 0, "::",
                MultiplayerProtocolState.CLOSED, true)
                : requestedConfig;

        String requestedAddress = normalizedAddress(config.boundAddress());
        boolean wildcard = isWildcard(requestedAddress);
        if (config.steamPreferred() && wildcard) {
            HostBindingResult steam = SteamNetworkingBridge.trySteamRelay(
                    config.withBinding("steam-relay", config.port(), MultiplayerProtocolState.STEAM_RELAY));
            DebugLog.audit("MULTIPLAYER_BIND_STEAM", steam.compactLine());
            if (steam.success()) return steam;
        }

        if ("::".equals(requestedAddress)) {
            HostBindingResult ipv6 = bindExact(config, "::", true);
            if (ipv6.success()) return ipv6;
            DebugLog.audit("MULTIPLAYER_BIND_IPV6_FALLBACK", ipv6.compactLine());
            return bindExact(config, "0.0.0.0", false);
        }
        if ("0.0.0.0".equals(requestedAddress)) {
            return bindExact(config, requestedAddress, false);
        }
        return bindExact(config, requestedAddress, requestedAddress.contains(":"));
    }

    private static HostBindingResult bindExact(ServerConfig base, String address, boolean ipv6) {
        MultiplayerProtocolState nettyProtocol = ipv6
                ? MultiplayerProtocolState.NETTY_IPV6
                : MultiplayerProtocolState.NETTY_IPV4;
        MultiplayerProtocolState nativeProtocol = ipv6
                ? MultiplayerProtocolState.NATIVE_IPV6
                : MultiplayerProtocolState.NATIVE_IPV4;
        ServerConfig exact = base.withBinding(address, base.port(), nettyProtocol);

        HostBindingResult netty = ReflectiveNettyBindingService.tryBind(exact, ipv6);
        DebugLog.audit(ipv6 ? "MULTIPLAYER_BIND_NETTY_IPV6" : "MULTIPLAYER_BIND_NETTY_IPV4",
                netty.compactLine());
        if (netty.success()) return netty;

        try {
            NativeTcpRelayServer nativeServer = NativeTcpRelayServer.bind(
                    base.withBinding(address, base.port(), nativeProtocol), ipv6);
            return HostBindingResult.success(
                    base,
                    nativeProtocol,
                    nativeServer.boundAddress(),
                    nativeServer.port(),
                    "java.nio ServerSocketChannel",
                    "Native " + (ipv6 ? "IPv6" : "IPv4")
                            + " TCP relay bound to the exact requested address after the Netty adapter declined.",
                    nativeServer);
        } catch (IOException | RuntimeException ex) {
            DebugLog.warn(ipv6 ? "MULTIPLAYER_BIND_NATIVE_IPV6" : "MULTIPLAYER_BIND_NATIVE_IPV4",
                    ex.getClass().getSimpleName() + ": " + ex.getMessage());
            ServerConfig failed = base.withBinding(address, base.port(), nativeProtocol);
            return HostBindingResult.failure(
                    failed,
                    MultiplayerProtocolState.CLOSED,
                    "Exact host bind failed for " + address + ":" + base.port() + ": " + ex.getMessage());
        }
    }

    private static String normalizedAddress(String value) {
        return value == null || value.isBlank() ? "::" : value.trim();
    }

    private static boolean isWildcard(String address) {
        return "::".equals(address) || "0.0.0.0".equals(address);
    }

    static ServerConfig configFromWorld(long seed, String worldName, String worldId,
                                        WorldSetupSettings settings, int maxPlayers,
                                        int requestedPort, boolean preferSteam) {
        int port = requestedPort > 0 ? requestedPort : NetworkPortAuthority.firstAvailableGamePort();
        return ServerConfig.fromWorldSettings(seed, worldName, worldId, settings,
                maxPlayers, port, "::", MultiplayerProtocolState.CLOSED, preferSteam);
    }

    static String auditSummary() {
        return "authority=" + VERSION + " " + NetworkPortAuthority.policySummary()
                + " explicitBind=exact-no-widening"
                + " steam=" + SteamNetworkingBridge.auditSummary()
                + " netty=" + ReflectiveNettyBindingService.auditSummary();
    }
}
