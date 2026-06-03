package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Tracks stock at the zone/faction level so local shops and vending can draw from local supply. */
final class ZoneFactionStockTracker {
    private final LinkedHashMap<String, LinkedHashMap<String, Integer>> stock = new LinkedHashMap<>();

    void seedZoneFaction(int locationKey, Faction faction, ZoneType zone, Random rng) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction == null ? FactionInventoryStockAuthority.factionForZone(zone) : faction);
        Random r = rng == null ? new Random(FactionInventoryStockAuthority.stableSeed(f, zone) ^ locationKey) : rng;
        for (String item : FactionInventoryStockAuthority.accessibleStock(f, zone, r)) {
            add(locationKey, f, item, FactionInventoryStockAuthority.limitedStockCount(f, zone, item, r));
        }
    }

    void add(int locationKey, Faction faction, String item, int count) {
        if (item == null || item.isBlank() || count <= 0 || ItemCatalog.get(item) == null) return;
        String k = key(locationKey, faction);
        LinkedHashMap<String, Integer> bucket = stock.computeIfAbsent(k, ignored -> new LinkedHashMap<>());
        bucket.put(item, Math.max(0, bucket.getOrDefault(item, 0)) + count);
    }

    boolean has(int locationKey, Faction faction, String item, int count) {
        LinkedHashMap<String, Integer> bucket = stock.get(key(locationKey, faction));
        return bucket != null && bucket.getOrDefault(item, 0) >= Math.max(1, count);
    }

    int count(int locationKey, Faction faction, String item) {
        LinkedHashMap<String, Integer> bucket = stock.get(key(locationKey, faction));
        return bucket == null ? 0 : Math.max(0, bucket.getOrDefault(item, 0));
    }

    boolean consume(int locationKey, Faction faction, String item, int count) {
        if (!has(locationKey, faction, item, count)) return false;
        LinkedHashMap<String, Integer> bucket = stock.get(key(locationKey, faction));
        bucket.put(item, bucket.getOrDefault(item, 0) - Math.max(1, count));
        return true;
    }

    int totalCount() {
        int total = 0;
        for (LinkedHashMap<String, Integer> bucket : stock.values()) {
            if (bucket == null) continue;
            for (Integer value : bucket.values()) total += Math.max(0, value == null ? 0 : value);
        }
        return total;
    }

    boolean isEmpty() {
        return totalCount() <= 0;
    }

    void clear() {
        stock.clear();
    }

    LinkedHashMap<String, LinkedHashMap<String, Integer>> snapshot() {
        LinkedHashMap<String, LinkedHashMap<String, Integer>> out = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<String, Integer>> zoneEntry : stock.entrySet()) {
            LinkedHashMap<String, Integer> bucket = new LinkedHashMap<>();
            if (zoneEntry.getValue() != null) {
                for (Map.Entry<String, Integer> e : zoneEntry.getValue().entrySet()) {
                    if (e.getKey() != null && e.getValue() != null && e.getValue() > 0) bucket.put(e.getKey(), e.getValue());
                }
            }
            if (!bucket.isEmpty()) out.put(zoneEntry.getKey(), bucket);
        }
        return out;
    }

    void restore(Map<String, ? extends Map<String, Integer>> saved) {
        stock.clear();
        if (saved == null) return;
        for (Map.Entry<String, ? extends Map<String, Integer>> zoneEntry : saved.entrySet()) {
            if (zoneEntry.getKey() == null || zoneEntry.getValue() == null) continue;
            LinkedHashMap<String, Integer> bucket = stock.computeIfAbsent(zoneEntry.getKey(), ignored -> new LinkedHashMap<>());
            for (Map.Entry<String, Integer> e : zoneEntry.getValue().entrySet()) {
                if (e.getKey() != null && e.getValue() != null && e.getValue() > 0 && ItemCatalog.get(e.getKey()) != null) bucket.put(e.getKey(), e.getValue());
            }
        }
    }

    void mergeFrom(ZoneFactionStockTracker other) {
        if (other == null) return;
        for (Map.Entry<String, LinkedHashMap<String, Integer>> zoneEntry : other.snapshot().entrySet()) {
            LinkedHashMap<String, Integer> bucket = stock.computeIfAbsent(zoneEntry.getKey(), ignored -> new LinkedHashMap<>());
            for (Map.Entry<String, Integer> e : zoneEntry.getValue().entrySet()) bucket.put(e.getKey(), Math.max(0, bucket.getOrDefault(e.getKey(), 0)) + e.getValue());
        }
    }

    ArrayList<String> availableItems(int locationKey, Faction faction) {
        LinkedHashMap<String, Integer> bucket = stock.get(key(locationKey, faction));
        ArrayList<String> out = new ArrayList<>();
        if (bucket == null) return out;
        for (Map.Entry<String, Integer> e : bucket.entrySet()) if (e.getValue() != null && e.getValue() > 0) out.add(e.getKey());
        return out;
    }

    String summary(int locationKey, Faction faction) {
        LinkedHashMap<String, Integer> bucket = stock.get(key(locationKey, faction));
        if (bucket == null || bucket.isEmpty()) return key(locationKey, faction) + ": no tracked stock";
        ArrayList<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : bucket.entrySet()) if (e.getValue() != null && e.getValue() > 0) parts.add(e.getKey() + " x" + e.getValue());
        return key(locationKey, faction) + ": " + String.join(", ", parts);
    }

    private static String key(int locationKey, Faction faction) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        return locationKey + ":" + f.name();
    }
}
