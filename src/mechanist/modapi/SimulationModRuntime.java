package mechanist.modapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Deterministic dispatcher that lets tests, servers, and editor previews exercise mod lifecycle hooks. */
public final class SimulationModRuntime {
    private final SimulationContext context;
    private final ArrayList<ModIntegrationHook> hooks = new ArrayList<>();

    public SimulationModRuntime(SimulationContext context) { this.context = Objects.requireNonNull(context, "context"); }
    public SimulationContext context() { return context; }
    public List<ModIntegrationHook> hooks() { return List.copyOf(hooks); }

    public void register(ModIntegrationHook hook) {
        ModIntegrationHook safe = Objects.requireNonNull(hook, "hook");
        hooks.add(safe);
        safe.onRegister(context);
        context.audit(safe.modId(), "registered mod hook");
    }

    public void enterSector(String sectorId) {
        SectorInstance sector = context.sector(sectorId).orElseThrow(() -> new IllegalArgumentException("Unknown sector: " + sectorId));
        for (ModIntegrationHook hook : hooks) hook.onSectorEnter(context, sector);
    }

    public void tickSector(String sectorId) {
        SectorInstance sector = context.sector(sectorId).orElseThrow(() -> new IllegalArgumentException("Unknown sector: " + sectorId));
        context.advanceTick();
        for (ModIntegrationHook hook : hooks) hook.onSectorTick(context, sector);
    }

    public void tickRoom(String roomId) {
        RoomNode room = context.room(roomId).orElseThrow(() -> new IllegalArgumentException("Unknown room: " + roomId));
        context.advanceTick();
        for (ModIntegrationHook hook : hooks) hook.onRoomTick(context, room);
    }

    public void consumeItem(String itemId) {
        ItemTemplate item = context.item(itemId).orElseThrow(() -> new IllegalArgumentException("Unknown item: " + itemId));
        for (ModIntegrationHook hook : hooks) hook.onItemConsumed(context, item);
    }

    public void diplomacyChange(DiplomacyChange change) {
        FactionProfile faction = context.faction(change.sourceFactionId()).orElseThrow(() -> new IllegalArgumentException("Unknown faction: " + change.sourceFactionId()));
        for (ModIntegrationHook hook : hooks) hook.onFactionDiplomacyChange(context, faction, change);
    }

    public List<String> unlockResearch(String nodeId) {
        List<String> blueprints = context.researchTree().unlock(nodeId);
        context.researchTree().node(nodeId).ifPresent(node -> {
            if (node.unlocked()) for (ModIntegrationHook hook : hooks) hook.onResearchNodeUnlocked(context, context.researchTree(), node);
        });
        return blueprints;
    }

    public List<LoreEntry> queryLore(LoreQuery query) {
        ArrayList<LoreEntry> results = new ArrayList<>();
        for (ModIntegrationHook hook : hooks) results.addAll(hook.onLoreQuery(context, context.loreDatabase(), query));
        return List.copyOf(results);
    }
}
