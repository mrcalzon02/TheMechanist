package mechanist;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** Owns world-persistent player faction membership and separated player/NPC command structures. */
final class PlayerFactionWorldAuthority {
    static final String VERSION = "player-faction-world-authority-0.9.10gv";
    static final String ROLE_FOUNDER = "founder";
    static final String ROLE_COMMANDER = "commander";
    static final String ROLE_OFFICER = "officer";
    static final String ROLE_SPECIALIST = "specialist";
    static final String ROLE_MEMBER = "member";
    static final String ROLE_OBSERVER = "observer";

    private PlayerFactionWorldAuthority() {}

    static void writeWorldFactionLedger(GamePanel game, Properties world) {
        if (game == null || world == null) return;
        String playerId = SaveEfficiencyAuthority.playerIdFor(game);
        String factionId = SaveEfficiencyAuthority.playerFactionIdFor(game, playerId);
        String playerName = game.active == null ? "unknown" : SaveEfficiencyAuthority.cleanRecord(game.active.name);
        boolean founded = game.baseClaimed && !"none".equals(factionId);
        String factionName = founded ? SaveEfficiencyAuthority.cleanRecord(playerName + "'s Freehold") : "none";
        String role = founded ? ROLE_FOUNDER : "unaffiliated";

        world.setProperty("world.playerFaction.schema", "player-faction-world-ledger-v1");
        world.setProperty("world.playerFaction.authority", VERSION);
        world.setProperty("world.playerFaction.id", factionId);
        world.setProperty("world.playerFaction.name", factionName);
        world.setProperty("world.playerFaction.exists", Boolean.toString(founded));
        world.setProperty("world.playerFaction.baseRoom", Integer.toString(game.claimedRoomId));
        world.setProperty("world.playerFaction.baseX", Integer.toString(game.baseX));
        world.setProperty("world.playerFaction.baseY", Integer.toString(game.baseY));
        world.setProperty("world.playerFaction.autonomyPolicy", founded ? "continue-production-trade-defense-without-player" : "no-player-faction-founded");
        world.setProperty("world.playerFaction.playerCommandTrack", "player-ranks");
        world.setProperty("world.playerFaction.npcCommandTrack", "npc-ranks");
        world.setProperty("world.playerFaction.commandTracksSeparated", "true");
        world.setProperty("world.playerFaction.memberIds", founded ? playerId : "");
        world.setProperty("world.playerFaction.member." + playerId + ".name", playerName);
        world.setProperty("world.playerFaction.member." + playerId + ".role", role);
        world.setProperty("world.playerFaction.member." + playerId + ".reserved", Boolean.toString(founded));
        world.setProperty("world.playerFaction.member." + playerId + ".status", founded ? "founder-present-or-resumable" : "unaffiliated");
        world.setProperty("world.playerFaction.member." + playerId + ".rankTrack", "player-command");
        world.setProperty("world.playerFaction.member." + playerId + ".lastKnownCharacter", playerName);
        world.setProperty("world.playerFaction.member." + playerId + ".lastKnownTurn", Integer.toString(game.turn));
        world.setProperty("world.playerFaction.npcLeadership.mode", founded ? "autonomous-while-player-absent" : "none");
        world.setProperty("world.playerFaction.npcLeadership.recruitCount", Integer.toString(Math.max(0, game.factionRecruits.size())));
        world.setProperty("world.playerFaction.productionContinuity", founded ? "owned-by-world-faction-ledger" : "none");
        world.setProperty("world.playerFaction.defenseContinuity", founded ? "owned-by-world-faction-ledger" : "none");
        world.setProperty("world.playerFaction.tradeContinuity", founded ? "owned-by-world-faction-ledger" : "none");
        world.setProperty("world.playerFaction.autonomyAuthority", PlayerFactionAutonomyAuthority.VERSION);
        world.setProperty("world.playerFaction.autonomousTickAuthority", PlayerFactionAutonomousTickAuthority.VERSION);
        world.setProperty("world.playerFaction.commandParityAuthority", PlayerNpcCommandParityAuthority.VERSION);
        PlayerNpcCommandParityAuthority.writeParityLedger(game, world);

        // Compatibility mirrors for older diagnostics/readers that still look under world.faction.*.
        world.setProperty("world.faction.playerFactionId", factionId);
        world.setProperty("world.faction.playerFactionName", factionName);
        world.setProperty("world.faction.playerCommandStructure", "player-ranks-separate-from-npc-ranks");
        world.setProperty("world.faction.playerMembershipRecord", compactMembershipRecord(playerId, playerName, factionId, role, founded));
        world.setProperty("world.faction.playerMember." + playerId + ".name", playerName);
        world.setProperty("world.faction.playerMember." + playerId + ".role", role);
        world.setProperty("world.faction.playerMember." + playerId + ".reserved", Boolean.toString(founded));
        world.setProperty("world.faction.playerMember." + playerId + ".rankTrack", "player-command");
        world.setProperty("world.faction.npcCommandTrack", "separate-npc-command-structure");
        world.setProperty("world.faction.commandParity", "player-rank-N-equates-to-npc-rank-N-command-authority;founder-is-unique");
        world.setProperty("world.faction.autonomyPolicy", founded ? "continues-without-player-present" : "no-player-faction-founded");
    }

    static void writeCharacterFactionAttachment(GamePanel game, Properties slot) {
        if (game == null || slot == null) return;
        String playerId = SaveEfficiencyAuthority.playerIdFor(game);
        String factionId = SaveEfficiencyAuthority.playerFactionIdFor(game, playerId);
        slot.setProperty("char.playerId", playerId);
        slot.setProperty("char.factionWorldAuthority", "world-file");
        slot.setProperty("char.lastKnownPlayerFactionId", factionId);
        slot.setProperty("char.factionResumeRule", "resume-command-if-world-ledger-reserves-player-id");
    }

    static String summary(GamePanel game, Properties world) {
        String playerId = game == null ? "unknown-player" : SaveEfficiencyAuthority.playerIdFor(game);
        String factionId = world == null ? "unknown" : world.getProperty("world.playerFaction.id", "unknown");
        String name = world == null ? "unknown" : world.getProperty("world.playerFaction.name", "unknown");
        String memberRole = world == null ? "unknown" : world.getProperty("world.playerFaction.member." + playerId + ".role", "unknown");
        String continuity = world == null ? "unknown" : world.getProperty("world.playerFaction.autonomyPolicy", "unknown");
        int memberKeys = countPrefix(world, "world.playerFaction.member.");
        return "Player faction continuity: faction=" + factionId + " name=" + name + " player=" + playerId + " role=" + memberRole
                + " memberKeys=" + memberKeys + " playerCommand=separate npcCommand=separate autonomy=" + continuity + " autonomyAuthority=" + world.getProperty("world.playerFaction.autonomy.authority", "pending")
                + "; " + PlayerNpcCommandParityAuthority.summaryFromWorld(world) + ".";
    }

    static String managementSummary(GamePanel game) {
        Properties world = SaveEfficiencyAuthority.worldRuntimeProperties(game);
        return summary(game, world)
                + " Player assignment menu target: player slots/roles are world-owned by stable player ID; NPC assignment remains in recruit/NPC management.";
    }

    static boolean isValidPlayerRole(String role) {
        if (role == null) return false;
        String r = role.toLowerCase(Locale.ROOT);
        return ROLE_FOUNDER.equals(r) || ROLE_COMMANDER.equals(r) || ROLE_OFFICER.equals(r) || ROLE_SPECIALIST.equals(r) || ROLE_MEMBER.equals(r) || ROLE_OBSERVER.equals(r) || PlayerNpcCommandParityAuthority.isValidPlayerRole(r);
    }

    static List<String> reservedPlayerIds(Properties world) {
        ArrayList<String> out = new ArrayList<>();
        if (world == null) return out;
        String ids = world.getProperty("world.playerFaction.memberIds", "");
        for (String id : ids.split(",")) {
            String t = id.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static String compactMembershipRecord(String playerId, String playerName, String factionId, String role, boolean reserved) {
        return playerId + "|" + playerName + "|" + factionId + "|" + role + "|npc-chain=separate|rank-parity=player-npc-same-tier|reserved=" + reserved;
    }

    private static int countPrefix(Properties p, String prefix) {
        if (p == null || prefix == null) return 0;
        int count = 0;
        for (String key : p.stringPropertyNames()) if (key.startsWith(prefix)) count++;
        return count;
    }
}
