package mechanist;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/** Immutable bridge from world-generation settings to a hostable multiplayer runtime envelope. */
record ServerConfig(
        long seed,
        String worldName,
        String worldId,
        String difficulty,
        int maxPlayers,
        int port,
        String boundAddress,
        MultiplayerProtocolState protocolState,
        String worldSetupEncoded,
        boolean steamPreferred,
        String createdAtIso
) {
    static final int DEFAULT_MAX_PLAYERS = 8;

    ServerConfig {
        worldName = clean(worldName, "Unnamed Hive World");
        worldId = clean(worldId, "local-world");
        difficulty = clean(difficulty, "Standard");
        maxPlayers = Math.max(1, Math.min(64, maxPlayers));
        if (!NetworkPortAuthority.portWithinAllowedGameRange(port)) {
            throw new IllegalArgumentException("Port " + port + " is outside the configured game range "
                    + NetworkPortAuthority.CUSTOM_GAME_PORT_MIN + "-" + NetworkPortAuthority.CUSTOM_GAME_PORT_MAX);
        }
        boundAddress = clean(boundAddress, "::");
        protocolState = Objects.requireNonNullElse(protocolState, MultiplayerProtocolState.CLOSED);
        worldSetupEncoded = clean(worldSetupEncoded, WorldSetupSettings.standard().encode());
        createdAtIso = clean(createdAtIso, Instant.now().toString());
    }

    static ServerConfig fromWorldSettings(long seed,
                                          String worldName,
                                          String worldId,
                                          WorldSetupSettings settings,
                                          int maxPlayers,
                                          int requestedPort,
                                          String requestedAddress,
                                          MultiplayerProtocolState protocolState,
                                          boolean steamPreferred) {
        WorldSetupSettings use = settings == null ? WorldSetupSettings.standard() : settings.copy();
        int port = requestedPort > 0 ? requestedPort : NetworkPortAuthority.firstAvailableGamePort();
        return new ServerConfig(seed,
                worldName,
                worldId,
                difficultyLabel(use),
                maxPlayers,
                port,
                requestedAddress == null || requestedAddress.isBlank() ? "::" : requestedAddress.trim(),
                protocolState,
                use.encode(),
                steamPreferred,
                Instant.now().toString());
    }

    ServerConfig withBinding(String address, int selectedPort, MultiplayerProtocolState protocol) {
        return new ServerConfig(seed, worldName, worldId, difficulty, maxPlayers, selectedPort, address, protocol, worldSetupEncoded, steamPreferred, createdAtIso);
    }

    WorldSetupSettings worldSettings() { return WorldSetupSettings.decode(worldSetupEncoded); }

    String compactLine() {
        return worldName + " seed=" + seed + " difficulty=" + difficulty + " maxPlayers=" + maxPlayers
                + " bind=" + boundAddress + ":" + port + " protocol=" + protocolState.label;
    }

    private static String difficultyLabel(WorldSetupSettings settings) {
        if (settings == null) return "Standard";
        String price = WorldSetupSettings.PRICE[Math.max(0, Math.min(3, settings.priceDifficulty))];
        String craft = WorldSetupSettings.CRAFT[Math.max(0, Math.min(3, settings.craftDifficulty))];
        String density = WorldSetupSettings.ZONE_DENSITY[Math.max(0, Math.min(3, settings.zoneDensity))];
        return (price + " economy / " + craft + " craft / " + density + " density").toUpperCase(Locale.ROOT);
    }

    private static String clean(String value, String fallback) {
        if (value == null) return fallback;
        String trimmed = value.trim().replace('\n', ' ');
        return trimmed.isEmpty() ? fallback : trimmed;
    }
}
