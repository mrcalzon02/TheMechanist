package mechanist.modapi;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Mutable, single-thread-confined context passed to mod lifecycle hooks by the simulation runtime. */
public final class SimulationContext {
    private final LinkedHashMap<String, SectorInstance> sectors = new LinkedHashMap<>();
    private final LinkedHashMap<String, RoomNode> rooms = new LinkedHashMap<>();
    private final LinkedHashMap<String, FactionProfile> factions = new LinkedHashMap<>();
    private final LinkedHashMap<String, ItemTemplate> items = new LinkedHashMap<>();
    private final ResearchTree researchTree = new ResearchTree("mechanist-runtime-research");
    private final LoreDatabase loreDatabase = new LoreDatabase("mechanist-runtime-lore");
    private final ArrayList<SimulationEvent> events = new ArrayList<>();
    private long tick;

    public long tick() { return tick; }
    public void setTick(long tick) { this.tick = Math.max(0L, tick); }
    public void advanceTick() { tick = Math.addExact(tick, 1L); }

    public void registerSector(SectorInstance sector) { sectors.put(Objects.requireNonNull(sector, "sector").id(), sector); }
    public void registerRoom(RoomNode room) { rooms.put(Objects.requireNonNull(room, "room").id(), room); }
    public void registerFaction(FactionProfile faction) { factions.put(Objects.requireNonNull(faction, "faction").id(), faction); }
    public void registerItem(ItemTemplate item) { items.put(Objects.requireNonNull(item, "item").id(), item); }

    public Optional<SectorInstance> sector(String id) { return Optional.ofNullable(sectors.get(cleanId(id))); }
    public Optional<RoomNode> room(String id) { return Optional.ofNullable(rooms.get(cleanId(id))); }
    public Optional<FactionProfile> faction(String id) { return Optional.ofNullable(factions.get(cleanId(id))); }
    public Optional<ItemTemplate> item(String id) { return Optional.ofNullable(items.get(cleanId(id))); }

    public Map<String, SectorInstance> sectors() { return Collections.unmodifiableMap(sectors); }
    public Map<String, RoomNode> rooms() { return Collections.unmodifiableMap(rooms); }
    public Map<String, FactionProfile> factions() { return Collections.unmodifiableMap(factions); }
    public Map<String, ItemTemplate> items() { return Collections.unmodifiableMap(items); }
    public ResearchTree researchTree() { return researchTree; }
    public LoreDatabase loreDatabase() { return loreDatabase; }

    public void emit(SimulationEvent event) {
        if (event != null) events.add(event);
    }

    public void audit(String source, String message) {
        emit(new SimulationEvent.AuditLog(cleanId(source), safe(message), Instant.now().toString(), tick));
    }

    public List<SimulationEvent> events() { return List.copyOf(events); }
    public void clearEvents() { events.clear(); }

    public static String cleanId(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty()) throw new IllegalArgumentException("Identifier must not be blank.");
        return clean;
    }

    public static String safe(String value) { return value == null ? "" : value.trim(); }
}
