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

    boolean consume(int locationKey, Faction faction, String item, int count) {
        if (!has(locationKey, faction, item, count)) return false;
        LinkedHashMap<String, Integer> bucket = stock.get(key(locationKey, faction));
        bucket.put(item, bucket.getOrDefault(item, 0) - Math.max(1, count));
        return true;
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
