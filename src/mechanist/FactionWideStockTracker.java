package mechanist;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/** Tracks faction-wide item access and quantities for commerce and world generation. */
final class FactionWideStockTracker {
    private final EnumMap<Faction, LinkedHashMap<String, Integer>> stock = new EnumMap<>(Faction.class);

    void seedFaction(Faction faction, ZoneType representativeZone, Random rng) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        stock.computeIfAbsent(f, k -> new LinkedHashMap<>());
        Random r = rng == null ? new Random(FactionInventoryStockAuthority.stableSeed(f, representativeZone)) : rng;
        for (String item : FactionInventoryStockAuthority.accessibleStock(f, representativeZone, r)) {
            add(f, item, FactionInventoryStockAuthority.limitedStockCount(f, representativeZone, item, r));
        }
    }

    void add(Faction faction, String item, int count) {
        if (item == null || item.isBlank() || count <= 0 || ItemCatalog.get(item) == null) return;
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        LinkedHashMap<String, Integer> bucket = stock.computeIfAbsent(f, k -> new LinkedHashMap<>());
        bucket.put(item, Math.max(0, bucket.getOrDefault(item, 0)) + count);
    }

    boolean has(Faction faction, String item, int count) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        LinkedHashMap<String, Integer> bucket = stock.get(f);
        return bucket != null && bucket.getOrDefault(item, 0) >= Math.max(1, count);
    }

    int count(Faction faction, String item) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        LinkedHashMap<String, Integer> bucket = stock.get(f);
        return bucket == null ? 0 : Math.max(0, bucket.getOrDefault(item, 0));
    }

    boolean consume(Faction faction, String item, int count) {
        if (!has(faction, item, count)) return false;
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        LinkedHashMap<String, Integer> bucket = stock.get(f);
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

    EnumMap<Faction, LinkedHashMap<String, Integer>> snapshot() {
        EnumMap<Faction, LinkedHashMap<String, Integer>> out = new EnumMap<>(Faction.class);
        for (Map.Entry<Faction, LinkedHashMap<String, Integer>> factionEntry : stock.entrySet()) {
            LinkedHashMap<String, Integer> bucket = new LinkedHashMap<>();
            if (factionEntry.getValue() != null) {
                for (Map.Entry<String, Integer> e : factionEntry.getValue().entrySet()) {
                    if (e.getKey() != null && e.getValue() != null && e.getValue() > 0) bucket.put(e.getKey(), e.getValue());
                }
            }
            if (!bucket.isEmpty()) out.put(factionEntry.getKey(), bucket);
        }
        return out;
    }

    void restore(Map<Faction, ? extends Map<String, Integer>> saved) {
        stock.clear();
        if (saved == null) return;
        for (Map.Entry<Faction, ? extends Map<String, Integer>> factionEntry : saved.entrySet()) {
            if (factionEntry.getValue() == null) continue;
            for (Map.Entry<String, Integer> e : factionEntry.getValue().entrySet()) add(factionEntry.getKey(), e.getKey(), e.getValue() == null ? 0 : e.getValue());
        }
    }

    void mergeFrom(FactionWideStockTracker other) {
        if (other == null) return;
        for (Map.Entry<Faction, LinkedHashMap<String, Integer>> factionEntry : other.snapshot().entrySet()) {
            for (Map.Entry<String, Integer> e : factionEntry.getValue().entrySet()) add(factionEntry.getKey(), e.getKey(), e.getValue());
        }
    }

    ArrayList<String> availableItems(Faction faction) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        LinkedHashMap<String, Integer> bucket = stock.get(f);
        ArrayList<String> out = new ArrayList<>();
        if (bucket == null) return out;
        for (Map.Entry<String, Integer> e : bucket.entrySet()) if (e.getValue() != null && e.getValue() > 0) out.add(e.getKey());
        return out;
    }

    String summary(Faction faction) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        LinkedHashMap<String, Integer> bucket = stock.get(f);
        if (bucket == null || bucket.isEmpty()) return f.label + ": no tracked stock";
        ArrayList<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : bucket.entrySet()) if (e.getValue() != null && e.getValue() > 0) parts.add(e.getKey() + " x" + e.getValue());
        return f.label + ": " + String.join(", ", parts);
    }
}
