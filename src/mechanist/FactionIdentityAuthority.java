package mechanist;

/** Shared strategic-family identity for stocks, population, and faction simulation. */
final class FactionIdentityAuthority {
    private FactionIdentityAuthority() { }

    static Faction strategicFamily(Faction faction) {
        if (faction == null || faction == Faction.NONE) return Faction.NONE;
        return FactionInventoryStockAuthority.normalizeFaction(faction);
    }

    static boolean sameFamily(Faction a, Faction b) {
        return a != null && b != null && strategicFamily(a) == strategicFamily(b);
    }

    static boolean aligned(Faction faction) {
        return faction != null && faction != Faction.NONE;
    }
}
