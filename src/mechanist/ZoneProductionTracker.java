package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tracks production, needs, and excess at zone/faction granularity. */
final class ZoneProductionTracker {
    private final LinkedHashMap<String, LinkedHashMap<String, ProductionNeedBalance>> balances = new LinkedHashMap<>();

    void recordProduction(int locationKey, Faction faction, String item, int amount) {
        balance(locationKey, faction, item).produced += Math.max(0, amount);
    }

    void recordInternalNeed(int locationKey, Faction faction, String item, int amount) {
        balance(locationKey, faction, item).internalNeed += Math.max(0, amount);
    }

    void recordExternalDemand(int locationKey, Faction faction, String item, int amount) {
        balance(locationKey, faction, item).externalDemand += Math.max(0, amount);
    }

    ProductionNeedBalance balance(int locationKey, Faction faction, String item) {
        String zoneKey = key(locationKey, faction);
        String itemKey = item == null || item.isBlank() ? "Unknown item" : item;
        LinkedHashMap<String, ProductionNeedBalance> byItem = balances.computeIfAbsent(zoneKey, ignored -> new LinkedHashMap<>());
        return byItem.computeIfAbsent(itemKey, ignored -> new ProductionNeedBalance(FactionInventoryStockAuthority.normalizeFaction(faction), itemKey));
    }

    ArrayList<ProductionNeedBalance> balances(int locationKey, Faction faction) {
        LinkedHashMap<String, ProductionNeedBalance> byItem = balances.get(key(locationKey, faction));
        return byItem == null ? new ArrayList<>() : new ArrayList<>(byItem.values());
    }

    int signalCount() {
        int total = 0;
        for (LinkedHashMap<String, ProductionNeedBalance> byItem : balances.values()) if (byItem != null) total += byItem.size();
        return total;
    }

    boolean isEmpty() {
        return signalCount() <= 0;
    }

    void clear() {
        balances.clear();
    }

    LinkedHashMap<String, LinkedHashMap<String, ProductionNeedBalance>> snapshot() {
        LinkedHashMap<String, LinkedHashMap<String, ProductionNeedBalance>> out = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashMap<String, ProductionNeedBalance>> zoneEntry : balances.entrySet()) {
            LinkedHashMap<String, ProductionNeedBalance> byItem = new LinkedHashMap<>();
            if (zoneEntry.getValue() != null) {
                for (Map.Entry<String, ProductionNeedBalance> e : zoneEntry.getValue().entrySet()) {
                    if (e.getKey() != null && e.getValue() != null && e.getValue().hasSignal()) byItem.put(e.getKey(), e.getValue().copy());
                }
            }
            if (!byItem.isEmpty()) out.put(zoneEntry.getKey(), byItem);
        }
        return out;
    }

    void restore(Map<String, ? extends Map<String, ProductionNeedBalance>> saved) {
        balances.clear();
        if (saved == null) return;
        for (Map.Entry<String, ? extends Map<String, ProductionNeedBalance>> zoneEntry : saved.entrySet()) {
            if (zoneEntry.getKey() == null || zoneEntry.getValue() == null) continue;
            LinkedHashMap<String, ProductionNeedBalance> byItem = balances.computeIfAbsent(zoneEntry.getKey(), ignored -> new LinkedHashMap<>());
            for (Map.Entry<String, ProductionNeedBalance> e : zoneEntry.getValue().entrySet()) {
                ProductionNeedBalance source = e.getValue();
                if (source == null || e.getKey() == null) continue;
                ProductionNeedBalance copy = source.copy();
                byItem.put(e.getKey(), copy);
            }
        }
    }

    void mergeFrom(ZoneProductionTracker other) {
        if (other == null) return;
        for (Map.Entry<String, LinkedHashMap<String, ProductionNeedBalance>> zoneEntry : other.snapshot().entrySet()) {
            LinkedHashMap<String, ProductionNeedBalance> byItem = balances.computeIfAbsent(zoneEntry.getKey(), ignored -> new LinkedHashMap<>());
            for (Map.Entry<String, ProductionNeedBalance> e : zoneEntry.getValue().entrySet()) {
                ProductionNeedBalance source = e.getValue();
                ProductionNeedBalance target = byItem.computeIfAbsent(e.getKey(), ignored -> new ProductionNeedBalance(source.faction, source.item));
                target.produced += Math.max(0, source.produced);
                target.internalNeed += Math.max(0, source.internalNeed);
                target.externalDemand += Math.max(0, source.externalDemand);
            }
        }
    }

    String summary(int locationKey, Faction faction) {
        ArrayList<String> parts = new ArrayList<>();
        for (ProductionNeedBalance b : balances(locationKey, faction)) parts.add(b.summary());
        return parts.isEmpty() ? key(locationKey, faction) + ": no production tracked" : key(locationKey, faction) + ": " + String.join("; ", parts);
    }

    private static String key(int locationKey, Faction faction) {
        return locationKey + ":" + FactionInventoryStockAuthority.normalizeFaction(faction).name();
    }
}
