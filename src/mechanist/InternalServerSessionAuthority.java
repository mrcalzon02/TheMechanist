package mechanist;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

/** Local internal-server identity and host/admin authority boundary. */
final class InternalServerSessionAuthority {
    static final String VERSION = "internal-server-session-authority-0.9.10gj";

    private final String serverId;
    private final String worldGeneratorUserId;
    private final String localUserId;
    private final String localPlayerId;

    InternalServerSessionAuthority(UserProfileAuthority.Profile profile, String localPlayerId) {
        String profileId = profile == null ? "MECH-LOCAL" : safe(profile.identifier, "MECH-LOCAL");
        this.localUserId = profileId;
        this.worldGeneratorUserId = profileId;
        this.localPlayerId = safe(localPlayerId, "single-player-local");
        this.serverId = "srv-" + UUID.nameUUIDFromBytes((profileId + "|local-internal-server").getBytes(StandardCharsets.UTF_8)).toString();
    }

    boolean isLocalAdmin(String userId) {
        return worldGeneratorUserId.equals(safe(userId, ""));
    }

    CommandContext commandContext(SectorKey sector) {
        String worldId = sector == null ? "local-world" : sector.compact();
        return new CommandContext(localPlayerId, localUserId, isLocalAdmin(localUserId), worldId, serverId);
    }

    String localPlayerId() { return localPlayerId; }
    String localUserId() { return localUserId; }
    String worldGeneratorUserId() { return worldGeneratorUserId; }
    String serverId() { return serverId; }

    String statusLine() {
        return "authority=" + VERSION
                + " server=" + shortToken(serverId)
                + " localPlayer=" + localPlayerId
                + " localUser=" + shortToken(localUserId)
                + " owner=" + shortToken(worldGeneratorUserId)
                + " localAdmin=" + isLocalAdmin(localUserId);
    }

    static String auditSummary() {
        return "authority=" + VERSION + " singlePlayerHostAdmin=local-profile-equals-world-generator-user remoteAdmin=closed";
    }

    record CommandContext(String playerId, String userId, boolean isAdmin, String currentWorldId, String serverId) { }

    private static String shortToken(String value) {
        String v = safe(value, "none");
        return v.length() <= 18 ? v : v.substring(0, 18);
    }

    private static String safe(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t.replace('\n', ' ');
    }
}
