package mechanist;

import java.util.Objects;

/** Stable, type-aware NPC portrait assignment. Saved portraitIndex remains authoritative. */
final class NpcPortraitSelectionAuthority {
    static final String VERSION = "npc-portrait-selection-identity-seeded-1";

    private NpcPortraitSelectionAuthority() { }

    static void assignForSpawn(NpcEntity npc, World world) {
        if (npc == null || world == null) return;
        // No mutable spawn-order dependency: the same actor and world seed
        // always resolve to the same index, even if staffing order changes.
        npc.portraitIndex = stableIndex(world.seed, npc);
    }

    /**
     * Stable randomized selection for NEW actors. The stored portraitIndex is
     * the save-file authority thereafter; never recalculate on load or paint.
     * Including actor identity, faction and kind gives variety inside the
     * family resolved by ImageCache, without depending on shared RNG calls.
     */
    static int stableIndex(long worldSeed, NpcEntity npc) {
        if (npc == null) return 0;
        String id = npc.id == null || npc.id.isBlank()
                ? (npc.name == null ? "" : npc.name) + "@" + npc.homeX + "," + npc.homeY
                : npc.id;
        long value = 0xcbf29ce484222325L ^ worldSeed;
        value = hashField(value, id);
        value = hashField(value, npc.faction == null ? "NONE" : npc.faction.name());
        value = hashField(value, npc.creatureKind);
        value = hashField(value, npc.animalProfileId);
        value = hashField(value, portraitRoleFamily(npc.role));
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (int) (value & Integer.MAX_VALUE);
    }

    private static long hashField(long hash, String field) {
        String text = field == null ? "" : field.toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < text.length(); i++) {
            hash ^= text.charAt(i);
            hash *= 0x100000001b3L;
        }
        return (hash ^ 0xffL) * 0x100000001b3L;
    }

    /** Legacy ordinal scheme retained only for interpreting older diagnostic expectations. */
    static int balancedIndex(long worldSeed, Faction faction, String role, int poolOrdinal) {
        int poolSalt = Math.floorMod(Objects.hash(worldSeed, faction == null ? Faction.NONE : faction, portraitRoleFamily(role)), 4096);
        // Consecutive low bits deliberately walk every small portrait bucket before repeating.
        return poolSalt * 257 + Math.max(0, poolOrdinal);
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
