package mechanist;

import java.io.IOException;

/** Layered host-binding authority: Steam relay, then IPv6, then IPv4. */
final class MultiplayerHostBindingService {
    static final String VERSION = "multiplayer-host-binding-service-0.9.10hs";

    private MultiplayerHostBindingService() { }

    static HostBindingResult bind(ServerConfig requestedConfig) {
        ServerConfig config = requestedConfig == null
                ? ServerConfig.fromWorldSettings(System.currentTimeMillis(), "Unnamed Hive World", "local-world", WorldSetupSettings.standard(), ServerConfig.DEFAULT_MAX_PLAYERS, 0, "::", MultiplayerProtocolState.CLOSED, true)
                : requestedConfig;
        if (config.steamPreferred()) {
            HostBindingResult steam = SteamNetworkingBridge.trySteamRelay(config.withBinding("steam-relay", config.port(), MultiplayerProtocolState.STEAM_RELAY));
            DebugLog.audit("MULTIPLAYER_BIND_STEAM", steam.compactLine());
            if (steam.success()) return steam;
        }
        HostBindingResult netty6 = ReflectiveNettyBindingService.tryBind(config.withBinding("::", config.port(), MultiplayerProtocolState.NETTY_IPV6), true);
        DebugLog.audit("MULTIPLAYER_BIND_NETTY_IPV6", netty6.compactLine());
        if (netty6.success()) return netty6;
        try {
            NativeTcpRelayServer native6 = NativeTcpRelayServer.bind(config.withBinding("::", config.port(), MultiplayerProtocolState.NATIVE_IPV6), true);
            return HostBindingResult.success(config, MultiplayerProtocolState.NATIVE_IPV6, "::", config.port(), "java.nio ServerSocketChannel", "Native IPv6 TCP relay bound after Netty path declined.", native6);
        } catch (IOException | RuntimeException ex) {
            DebugLog.warn("MULTIPLAYER_BIND_NATIVE_IPV6", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
        HostBindingResult netty4 = ReflectiveNettyBindingService.tryBind(config.withBinding("0.0.0.0", config.port(), MultiplayerProtocolState.NETTY_IPV4), false);
        DebugLog.audit("MULTIPLAYER_BIND_NETTY_IPV4", netty4.compactLine());
        if (netty4.success()) return netty4;
        try {
            NativeTcpRelayServer native4 = NativeTcpRelayServer.bind(config.withBinding("0.0.0.0", config.port(), MultiplayerProtocolState.NATIVE_IPV4), false);
            return HostBindingResult.success(config, MultiplayerProtocolState.NATIVE_IPV4, "0.0.0.0", config.port(), "java.nio ServerSocketChannel", "Native IPv4 TCP relay bound after IPv6 and Netty paths declined.", native4);
        } catch (IOException | RuntimeException ex) {
            DebugLog.warn("MULTIPLAYER_BIND_NATIVE_IPV4", ex.getClass().getSimpleName() + ": " + ex.getMessage());
            return HostBindingResult.failure(config, MultiplayerProtocolState.CLOSED, "All host-binding strategies failed: " + ex.getMessage());
        }
    }

    static ServerConfig configFromWorld(long seed, String worldName, String worldId, WorldSetupSettings settings, int maxPlayers, int requestedPort, boolean preferSteam) {
        int port = requestedPort > 0 ? requestedPort : NetworkPortAuthority.firstAvailableGamePort();
        return ServerConfig.fromWorldSettings(seed, worldName, worldId, settings, maxPlayers, port, "::", MultiplayerProtocolState.CLOSED, preferSteam);
    }

    static String auditSummary() {
        return "authority=" + VERSION + " " + NetworkPortAuthority.policySummary()
                + " steam=" + SteamNetworkingBridge.auditSummary()
                + " netty=" + ReflectiveNettyBindingService.auditSummary();
    }
}
