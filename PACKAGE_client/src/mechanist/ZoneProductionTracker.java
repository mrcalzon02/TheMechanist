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

    String summary(int locationKey, Faction faction) {
        ArrayList<String> parts = new ArrayList<>();
        for (ProductionNeedBalance b : balances(locationKey, faction)) parts.add(b.summary());
        return parts.isEmpty() ? key(locationKey, faction) + ": no production tracked" : key(locationKey, faction) + ": " + String.join("; ", parts);
    }

    private static String key(int locationKey, Faction faction) {
        return locationKey + ":" + FactionInventoryStockAuthority.normalizeFaction(faction).name();
    }
}
