package mechanist;

import java.util.Objects;

/** Balances deterministic NPC portrait slots within a generated zone and faction pool. */
final class NpcPortraitSelectionAuthority {
    static final String VERSION = "npc-portrait-selection-0.9.10";

    private NpcPortraitSelectionAuthority() { }

    static void assignForSpawn(NpcEntity npc, World world) {
        if (npc == null || world == null) return;
        int ordinal = 0;
        for (NpcEntity existing : world.npcs) {
            if (existing != null && samePool(existing, npc)) ordinal++;
        }
        npc.portraitIndex = balancedIndex(world.seed, npc.faction, npc.role, ordinal);
    }

    static int balancedIndex(long worldSeed, Faction faction, String role, int poolOrdinal) {
        int poolSalt = Math.floorMod(Objects.hash(worldSeed, faction == null ? Faction.NONE : faction, portraitRoleFamily(role)), 4096);
        // Consecutive low bits deliberately walk every small portrait bucket before repeating.
        return poolSalt * 257 + Math.max(0, poolOrdinal);
    }

    private static boolean samePool(NpcEntity a, NpcEntity b) {
        Faction af = a.faction == null ? Faction.NONE : a.faction;
        Faction bf = b.faction == null ? Faction.NONE : b.faction;
        return af == bf && portraitRoleFamily(a.role).equals(portraitRoleFamily(b.role));
    }

    private static String portraitRoleFamily(String role) {
        String text = role == null ? "civilian" : role.toLowerCase(java.util.Locale.ROOT);
        if (text.contains("servant") || text.contains("chef") || text.contains("butler") || text.contains("household") || text.contains("laundry") || text.contains("retainer")) return "household";
        if (text.contains("medicae") || text.contains("hospital") || text.contains("clinic")) return "medical";
        if (text.contains("guard") || text.contains("soldier") || text.contains("security")) return "security";
        if (text.contains("worker") || text.contains("labor") || text.contains("factory") || text.contains("dock")) return "worker";
        return "civilian";
    }
}
