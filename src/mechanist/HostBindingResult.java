package mechanist;

/** Result of a multiplayer host binding attempt. */
record HostBindingResult(ServerConfig config,
                         boolean success,
                         MultiplayerProtocolState protocolState,
                         String boundAddress,
                         int port,
                         String transportName,
                         String message,
                         AutoCloseable session) implements AutoCloseable {
    static HostBindingResult failure(ServerConfig config, MultiplayerProtocolState protocol, String message) {
        ServerConfig safe = config == null ? ServerConfig.fromWorldSettings(System.currentTimeMillis(), "Unnamed Hive World", "local-world", WorldSetupSettings.standard(), ServerConfig.DEFAULT_MAX_PLAYERS, NetworkPortAuthority.DEFAULT_GAME_PORT, "::", MultiplayerProtocolState.CLOSED, false) : config;
        return new HostBindingResult(safe, false, protocol, safe.boundAddress(), safe.port(), protocol.label, message, null);
    }

    static HostBindingResult success(ServerConfig config, MultiplayerProtocolState protocol, String boundAddress, int port, String transportName, String message, AutoCloseable session) {
        ServerConfig bound = config.withBinding(boundAddress, port, protocol);
        return new HostBindingResult(bound, true, protocol, boundAddress, port, transportName, message, session);
    }

    String compactLine() {
        return (success ? "BOUND" : "FAILED") + " protocol=" + protocolState + " address=" + boundAddress + " port=" + port + " transport=" + transportName + " message=" + message;
    }

    @Override public void close() throws Exception {
        if (session != null) session.close();
    }
}
