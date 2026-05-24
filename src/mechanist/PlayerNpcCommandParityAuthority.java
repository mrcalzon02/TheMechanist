package mechanist;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

/** Maintains parity between player command ranks and NPC command ranks without merging their rosters. */
final class PlayerNpcCommandParityAuthority {
    static final String VERSION = "player-npc-command-parity-authority-0.9.10gv";
    private static final String PREFIX = "world.playerFaction.commandParity.";

    static final int FOUNDER_TIER = 0;
    static final int HIGHEST_RECRUITED_PLAYER_TIER = 1;
    static final int LOWEST_COMMAND_TIER = 5;

    private PlayerNpcCommandParityAuthority() {}

    static void writeParityLedger(GamePanel game, Properties world) {
        if (game == null || world == null) return;
        String playerId = SaveEfficiencyAuthority.playerIdFor(game);
        String factionId = world.getProperty("world.playerFaction.id", SaveEfficiencyAuthority.playerFactionIdFor(game, playerId));
        boolean founded = Boolean.parseBoolean(world.getProperty("world.playerFaction.exists", Boolean.toString(game.baseClaimed)))
                && factionId != null && !factionId.isBlank() && !"none".equals(factionId);

        world.setProperty(PREFIX + "schema", "player-npc-command-parity-v1");
        world.setProperty(PREFIX + "authority", VERSION);
        world.setProperty(PREFIX + "factionId", factionId == null ? "none" : factionId);
        world.setProperty(PREFIX + "active", Boolean.toString(founded));
        world.setProperty(PREFIX + "founderTier", Integer.toString(FOUNDER_TIER));
        world.setProperty(PREFIX + "highestRecruitedPlayerTier", Integer.toString(HIGHEST_RECRUITED_PLAYER_TIER));
        world.setProperty(PREFIX + "tracksSeparated", "true");
        world.setProperty(PREFIX + "rule", "player-rank-and-npc-rank-share-command-tier; founder remains unique and unrecruitable");
        writeTierRows(world);
        if (founded) {
            normalizeMember(world, playerId, world.getProperty("world.playerFaction.member." + playerId + ".role", PlayerFactionWorldAuthority.ROLE_FOUNDER));
        }
        world.setProperty(PREFIX + "summary", summaryFromWorld(world));
    }

    static void normalizeMember(Properties world, String playerId, String role) {
        if (world == null || playerId == null || playerId.isBlank()) return;
        PlayerRank rank = playerRankFor(role);
        world.setProperty("world.playerFaction.member." + playerId + ".role", rank.canonicalRole);
        world.setProperty("world.playerFaction.member." + playerId + ".playerRankName", rank.playerName);
        world.setProperty("world.playerFaction.member." + playerId + ".commandTier", Integer.toString(rank.tier));
        world.setProperty("world.playerFaction.member." + playerId + ".npcEquivalentRank", npcRankName(rank.tier));
        world.setProperty("world.playerFaction.member." + playerId + ".npcCommandAuthority", npcAuthorityLine(rank.tier));
        world.setProperty("world.playerFaction.member." + playerId + ".founderOnly", Boolean.toString(rank.tier == FOUNDER_TIER));
        world.setProperty("world.playerFaction.member." + playerId + ".trackParity", "player-command-tier-" + rank.tier + "=npc-command-tier-" + rank.tier);
        world.setProperty("world.faction.playerMember." + playerId + ".role", rank.canonicalRole);
        world.setProperty("world.faction.playerMember." + playerId + ".commandTier", Integer.toString(rank.tier));
        world.setProperty("world.faction.playerMember." + playerId + ".npcEquivalentRank", npcRankName(rank.tier));
    }

    static String summary(GamePanel game) {
        return summaryFromWorld(SaveEfficiencyAuthority.worldRuntimeProperties(game));
    }

    static String summaryFromWorld(Properties world) {
        if (world == null) return "Command parity: unavailable.";
        String factionId = world.getProperty(PREFIX + "factionId", world.getProperty("world.playerFaction.id", "none"));
        String active = world.getProperty(PREFIX + "active", "false");
        String ids = world.getProperty("world.playerFaction.memberIds", "");
        int memberCount = 0;
        int founderCount = 0;
        int recruitedCount = 0;
        for (String raw : ids.split(",")) {
            String id = raw.trim();
            if (id.isEmpty()) continue;
            memberCount++;
            int tier = readInt(world, "world.playerFaction.member." + id + ".commandTier", LOWEST_COMMAND_TIER);
            if (tier == FOUNDER_TIER) founderCount++; else recruitedCount++;
        }
        return "Command parity: active=" + active + " faction=" + factionId
                + " tracks=separate sharedTierScale=true founderTier=0 recruitedPlayerTiers=1-5 npcTiers=1-5"
                + " members=" + memberCount + " founders=" + founderCount + " recruited=" + recruitedCount
                + " rule=rank-N-player-controls-rank-N-npc-authority.";
    }

    static String assignmentParitySummary(GamePanel game) {
        Properties world = SaveEfficiencyAuthority.worldRuntimeProperties(game);
        if (world == null) return "Personnel parity: unavailable.";
        StringBuilder sb = new StringBuilder(summaryFromWorld(world));
        String ids = world.getProperty("world.playerFaction.memberIds", "");
        for (String raw : ids.split(",")) {
            String id = raw.trim();
            if (id.isEmpty()) continue;
            sb.append("\n - player ").append(id)
                    .append(" rank=").append(world.getProperty("world.playerFaction.member." + id + ".playerRankName", "unknown"))
                    .append(" tier=").append(world.getProperty("world.playerFaction.member." + id + ".commandTier", "?"))
                    .append(" npcEquivalent=").append(world.getProperty("world.playerFaction.member." + id + ".npcEquivalentRank", "unknown"))
                    .append(" authority=").append(world.getProperty("world.playerFaction.member." + id + ".npcCommandAuthority", "unknown"));
        }
        return sb.toString();
    }

    static boolean isValidPlayerRole(String role) {
        return playerRankFor(role) != null;
    }

    static int commandTierForPlayerRole(String role) {
        return playerRankFor(role).tier;
    }

    static String npcRankName(int tier) {
        return switch (tier) {
            case FOUNDER_TIER -> "founder-overrides-npc-chain";
            case 1 -> "npc-rank-1-overseer";
            case 2 -> "npc-rank-2-foreman";
            case 3 -> "npc-rank-3-senior-worker";
            case 4 -> "npc-rank-4-worker";
            default -> "npc-rank-5-observer";
        };
    }

    private static String npcAuthorityLine(int tier) {
        if (tier == FOUNDER_TIER) return "full-faction-command; founder slot is unique and cannot be assigned to recruited players";
        return "may-command-npc-rank-" + tier + "-and-lower; may-not-override-founder";
    }

    private static void writeTierRows(Properties world) {
        for (Map.Entry<Integer, String> e : tierRows().entrySet()) {
            int tier = e.getKey();
            world.setProperty(PREFIX + "tier." + tier + ".playerRank", playerNameForTier(tier));
            world.setProperty(PREFIX + "tier." + tier + ".npcRank", npcRankName(tier));
            world.setProperty(PREFIX + "tier." + tier + ".authority", npcAuthorityLine(tier));
            world.setProperty(PREFIX + "tier." + tier + ".note", e.getValue());
        }
    }

    private static Map<Integer, String> tierRows() {
        LinkedHashMap<Integer, String> rows = new LinkedHashMap<>();
        rows.put(0, "founder-only; not available to recruited players");
        rows.put(1, "highest recruited player command tier; equivalent to NPC rank 1 command authority");
        rows.put(2, "second recruited player command tier; equivalent to NPC rank 2 command authority");
        rows.put(3, "third recruited player command tier; equivalent to NPC rank 3 command authority");
        rows.put(4, "fourth recruited player command tier; equivalent to NPC rank 4 command authority");
        rows.put(5, "lowest recruited player command tier; equivalent to NPC rank 5 command authority");
        return rows;
    }

    private static String playerNameForTier(int tier) {
        return switch (tier) {
            case FOUNDER_TIER -> "player-founder";
            case 1 -> "player-rank-1-commander";
            case 2 -> "player-rank-2-officer";
            case 3 -> "player-rank-3-specialist";
            case 4 -> "player-rank-4-member";
            default -> "player-rank-5-observer";
        };
    }

    private static PlayerRank playerRankFor(String role) {
        String r = role == null ? "" : role.toLowerCase(Locale.ROOT).trim().replace(' ', '-').replace('_', '-');
        return switch (r) {
            case "founder", "leader" -> new PlayerRank(PlayerFactionWorldAuthority.ROLE_FOUNDER, "player-founder", FOUNDER_TIER);
            case "commander", "rank1", "rank-1", "player-rank-1", "player-rank-1-commander" -> new PlayerRank(PlayerFactionWorldAuthority.ROLE_COMMANDER, "player-rank-1-commander", 1);
            case "officer", "rank2", "rank-2", "player-rank-2", "player-rank-2-officer" -> new PlayerRank(PlayerFactionWorldAuthority.ROLE_OFFICER, "player-rank-2-officer", 2);
            case "specialist", "sergeant", "rank3", "rank-3", "player-rank-3", "player-rank-3-specialist", "member" -> new PlayerRank(PlayerFactionWorldAuthority.ROLE_SPECIALIST, "player-rank-3-specialist", 3);
            case "associate", "rank4", "rank-4", "player-rank-4", "player-rank-4-member" -> new PlayerRank(PlayerFactionWorldAuthority.ROLE_MEMBER, "player-rank-4-member", 4);
            case "observer", "rank5", "rank-5", "player-rank-5", "player-rank-5-observer" -> new PlayerRank(PlayerFactionWorldAuthority.ROLE_OBSERVER, "player-rank-5-observer", 5);
            default -> new PlayerRank(PlayerFactionWorldAuthority.ROLE_OBSERVER, "player-rank-5-observer", 5);
        };
    }

    private static int readInt(Properties p, String key, int fallback) {
        if (p == null || key == null) return fallback;
        try { return Integer.parseInt(p.getProperty(key, Integer.toString(fallback)).trim()); }
        catch (Exception ignored) { return fallback; }
    }

    private record PlayerRank(String canonicalRole, String playerName, int tier) { }
}
