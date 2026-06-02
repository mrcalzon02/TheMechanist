package mechanist.modapi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Modder-facing lore/wiki database used by Infopedia Editor and lore search hooks. */
public final class LoreDatabase {
    private final String id;
    private final LinkedHashMap<String, LoreEntry> entries = new LinkedHashMap<>();

    public LoreDatabase(String id) { this.id = SimulationContext.cleanId(id); }
    public String id() { return id; }
    public Map<String, LoreEntry> entries() { return Map.copyOf(entries); }
    public Optional<LoreEntry> entry(String entryId) { return Optional.ofNullable(entries.get(SimulationContext.cleanId(entryId))); }
    public void addEntry(LoreEntry entry) { entries.put(entry.id(), entry); }

    public List<LoreEntry> search(String query) {
        String clean = SimulationContext.safe(query).toLowerCase(Locale.ROOT);
        if (clean.isEmpty()) return List.copyOf(entries.values());
        ArrayList<LoreEntry> out = new ArrayList<>();
        for (LoreEntry entry : entries.values()) {
            if (entry.title().toLowerCase(Locale.ROOT).contains(clean)
                    || entry.taxonomyPath().toLowerCase(Locale.ROOT).contains(clean)
                    || entry.body().toLowerCase(Locale.ROOT).contains(clean)
                    || entry.searchTags().stream().anyMatch(tag -> tag.toLowerCase(Locale.ROOT).contains(clean))) {
                out.add(entry);
            }
        }
        return List.copyOf(out);
    }
}
