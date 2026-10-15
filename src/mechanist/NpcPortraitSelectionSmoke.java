package mechanist;

import java.util.HashSet;

public final class NpcPortraitSelectionSmoke {
    private NpcPortraitSelectionSmoke() { }

    public static void main(String[] args) {
        HashSet<Integer> residues = new HashSet<>();
        for (int i = 0; i < 16; i++) {
            int index = NpcPortraitSelectionAuthority.balancedIndex(99117L, Faction.HIVER, "Civilian Resident", i);
            require(residues.add(Math.floorMod(index, 16)), "portrait slot repeated before pool exhaustion at ordinal " + i);
        }
        int repeat = NpcPortraitSelectionAuthority.balancedIndex(99117L, Faction.HIVER, "Civilian Resident", 16);
        require(residues.contains(Math.floorMod(repeat, 16)), "portrait slot should cycle after pool exhaustion");
        World world = new World(99117L, 16, 16);
        NpcEntity first = npc("ID-105", "Factory Resident", Faction.HIVER, "worker");
        NpcEntity other = npc("ID-106", "Factory Resident", Faction.HIVER, "worker");
        NpcPortraitSelectionAuthority.assignForSpawn(first, world);
        int initial = first.portraitIndex;
        require(initial == NpcPortraitSelectionAuthority.stableIndex(world.seed, first),
                "seed/identity selection mismatch");
        require(initial == NpcPortraitSelectionAuthority.stableIndex(world.seed, npc(
                "ID-105", "Factory Resident", Faction.HIVER, "worker")),
                "identical identity was not deterministic");
        world.npcs.add(other);
        world.npcs.add(npc("ID-107", "Another Resident", Faction.HIVER, "worker"));
        NpcPortraitSelectionAuthority.assignForSpawn(first, world);
        require(initial == first.portraitIndex, "spawn-order change altered NPC image slot");
        require(initial != NpcPortraitSelectionAuthority.stableIndex(world.seed + 1L, first),
                "different world seeds unexpectedly chose identical image slot");
        NpcEntity counterpart = npc("ID-105", "Factory Resident", Faction.MUTANT, "worker");
        require(initial != NpcPortraitSelectionAuthority.stableIndex(world.seed, counterpart),
                "different actor category unexpectedly chose identical image slot");
        require(initial != NpcPortraitSelectionAuthority.stableIndex(world.seed,
                npc("ID-999", "Factory Resident", Faction.HIVER, "worker")),
                "different identity unexpectedly chose identical image slot");
        NpcEntity restored = NpcEntity.parseLine(first.saveLine(), world);
        require(restored != null && restored.portraitIndex == initial,
                "saved NPC image slot was changed during round-trip");
        first.x = 12;
        first.y = 8;
        require(initial == NpcPortraitSelectionAuthority.stableIndex(world.seed, first),
                "NPC movement changed identity portrait selection");

        NpcEntity createdA = NpcEntity.create(Faction.ARBITES,
                ZoneType.NEUTRAL_CIVILIAN_FLOOR, 5, 5, new java.util.Random(4242L));
        NpcEntity createdB = NpcEntity.create(Faction.ARBITES,
                ZoneType.NEUTRAL_CIVILIAN_FLOOR, 5, 5, new java.util.Random(4242L));
        require(createdA.id.equals(createdB.id)
                        && createdA.portraitIndex == createdB.portraitIndex,
                "repeat seeded NPC creation changed identity or portrait");
        require(createdA.portraitIndex
                        == NpcPortraitSelectionAuthority.stableIndex(0L, createdA),
                "ordinary generated NPC did not receive identity-based portrait index");
        System.out.println("NpcPortraitSelectionSmoke OK legacySlots=" + residues.size()
                + " identityIndex=" + initial + " createdIndex=" + createdA.portraitIndex);
    }

    private static NpcEntity npc(String id, String name, Faction faction, String role) {
        NpcEntity npc = new NpcEntity();
        npc.id = id;
        npc.name = name;
        npc.faction = faction;
        npc.role = role;
        npc.creatureKind = "humanoid";
        npc.homeX = 5;
        npc.homeY = 6;
        npc.x = 5;
        npc.y = 6;
        return npc;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
