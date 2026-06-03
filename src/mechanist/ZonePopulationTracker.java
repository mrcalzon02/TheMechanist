package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tracks population counts per zone and faction. */
final class ZonePopulationTracker {
    private final LinkedHashMap<String, Integer> population = new LinkedHashMap<>();

    void add(int locationKey, Faction faction, int count) {
        if (count <= 0) return;
        String k = key(locationKey, faction);
        population.put(k, Math.max(0, population.getOrDefault(k, 0)) + count);
    }

    boolean remove(int locationKey, Faction faction, int count) {
        if (count <= 0) return true;
        String k = key(locationKey, faction);
        int current = population.getOrDefault(k, 0);
        if (current < count) return false;
        population.put(k, current - count);
        return true;
    }

    int count(int locationKey, Faction faction) {
        return population.getOrDefault(key(locationKey, faction), 0);
    }

    int totalInZone(int locationKey) {
        String prefix = locationKey + ":";
        int total = 0;
        for (Map.Entry<String, Integer> e : population.entrySet()) if (e.getKey().startsWith(prefix) && e.getValue() != null) total += Math.max(0, e.getValue());
        return total;
    }

    String summary(int locationKey) {
        String prefix = locationKey + ":";
        ArrayList<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : population.entrySet()) {
            if (e.getKey().startsWith(prefix) && e.getValue() != null && e.getValue() > 0) parts.add(e.getKey().substring(prefix.length()) + "=" + e.getValue());
        }
        return parts.isEmpty() ? "Zone " + locationKey + ": no population tracked" : "Zone " + locationKey + ": " + String.join("; ", parts);
    }

    private static String key(int locationKey, Faction faction) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        return locationKey + ":" + f.name();
    }
}
