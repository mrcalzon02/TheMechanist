package mechanist;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;

/** Tracks faction-wide population counts outside individual zone placement. */
final class FactionPopulationTracker {
    private final EnumMap<Faction, Integer> population = new EnumMap<>(Faction.class);

    void add(Faction faction, int count) {
        if (count <= 0) return;
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        population.put(f, Math.max(0, population.getOrDefault(f, 0)) + count);
    }

    boolean remove(Faction faction, int count) {
        if (count <= 0) return true;
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        int current = population.getOrDefault(f, 0);
        if (current < count) return false;
        population.put(f, current - count);
        return true;
    }

    int count(Faction faction) {
        return population.getOrDefault(FactionInventoryStockAuthority.normalizeFaction(faction), 0);
    }

    int total() {
        int total = 0;
        for (Integer value : population.values()) total += Math.max(0, value == null ? 0 : value);
        return total;
    }

    boolean isEmpty() {
        return total() <= 0;
    }

    void clear() {
        population.clear();
    }

    EnumMap<Faction, Integer> snapshot() {
        EnumMap<Faction, Integer> out = new EnumMap<>(Faction.class);
        for (Map.Entry<Faction, Integer> e : population.entrySet()) {
            if (e.getKey() != null && e.getValue() != null && e.getValue() > 0) out.put(e.getKey(), e.getValue());
        }
        return out;
    }

    void restore(Map<Faction, Integer> saved) {
        population.clear();
        if (saved == null) return;
        for (Map.Entry<Faction, Integer> e : saved.entrySet()) {
            if (e.getKey() != null && e.getValue() != null && e.getValue() > 0) add(e.getKey(), e.getValue());
        }
    }

    void mergeFrom(FactionPopulationTracker other) {
        if (other == null) return;
        for (Map.Entry<Faction, Integer> e : other.snapshot().entrySet()) add(e.getKey(), e.getValue());
    }

    String summary() {
        ArrayList<String> parts = new ArrayList<>();
        for (Map.Entry<Faction, Integer> e : population.entrySet()) if (e.getValue() != null && e.getValue() > 0) parts.add(e.getKey().label + "=" + e.getValue());
        return parts.isEmpty() ? "No faction population tracked" : String.join("; ", parts);
    }
}
