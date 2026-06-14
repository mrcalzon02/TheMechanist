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
        System.out.println("NpcPortraitSelectionSmoke OK slots=" + residues.size());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
