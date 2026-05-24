package mechanist.modapi;

import java.util.List;

/** Lifecycle hooks a Java mod may implement to inject data and behavior into safe simulation seams. */
public interface ModIntegrationHook {
    String modId();

    default void onRegister(SimulationContext context) { requireContext(context); }
    default void onSectorEnter(SimulationContext context, SectorInstance sector) { requireContext(context); requireSector(sector); }
    default void onSectorTick(SimulationContext context, SectorInstance sector) { requireContext(context); requireSector(sector); }
    default void onRoomTick(SimulationContext context, RoomNode room) { requireContext(context); requireRoom(room); }
    default void onItemConsumed(SimulationContext context, ItemTemplate item) { requireContext(context); requireItem(item); }
    default void onFactionDiplomacyChange(SimulationContext context, FactionProfile faction, DiplomacyChange change) { requireContext(context); requireFaction(faction); if (change == null) throw new IllegalArgumentException("change must not be null"); }
    default void onResearchNodeUnlocked(SimulationContext context, ResearchTree researchTree, ResearchNode node) { requireContext(context); if (researchTree == null || node == null) throw new IllegalArgumentException("researchTree and node are required"); }
    default List<LoreEntry> onLoreQuery(SimulationContext context, LoreDatabase loreDatabase, LoreQuery query) { requireContext(context); if (loreDatabase == null || query == null) throw new IllegalArgumentException("loreDatabase and query are required"); return loreDatabase.search(query.queryText()); }

    private static void requireContext(SimulationContext context) { if (context == null) throw new IllegalArgumentException("context must not be null"); }
    private static void requireSector(SectorInstance sector) { if (sector == null) throw new IllegalArgumentException("sector must not be null"); }
    private static void requireRoom(RoomNode room) { if (room == null) throw new IllegalArgumentException("room must not be null"); }
    private static void requireFaction(FactionProfile faction) { if (faction == null) throw new IllegalArgumentException("faction must not be null"); }
    private static void requireItem(ItemTemplate item) { if (item == null) throw new IllegalArgumentException("item must not be null"); }
}
