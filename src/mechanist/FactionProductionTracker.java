package mechanist;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** Tracks faction-wide production, needs, and excess by item key. */
final class FactionProductionTracker {
    private final EnumMap<Faction, LinkedHashMap<String, ProductionNeedBalance>> balances = new EnumMap<>(Faction.class);

    void recordProduction(Faction faction, String item, int amount) {
        balance(faction, item).produced += Math.max(0, amount);
    }

    void recordInternalNeed(Faction faction, String item, int amount) {
        balance(faction, item).internalNeed += Math.max(0, amount);
    }

    void recordExternalDemand(Faction faction, String item, int amount) {
        balance(faction, item).externalDemand += Math.max(0, amount);
    }

    ProductionNeedBalance balance(Faction faction, String item) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        String key = item == null || item.isBlank() ? "Unknown item" : item;
        LinkedHashMap<String, ProductionNeedBalance> byItem = balances.computeIfAbsent(f, ignored -> new LinkedHashMap<>());
        return byItem.computeIfAbsent(key, ignored -> new ProductionNeedBalance(f, key));
    }

    ArrayList<ProductionNeedBalance> balances(Faction faction) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        LinkedHashMap<String, ProductionNeedBalance> byItem = balances.get(f);
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

    EnumMap<Faction, LinkedHashMap<String, ProductionNeedBalance>> snapshot() {
        EnumMap<Faction, LinkedHashMap<String, ProductionNeedBalance>> out = new EnumMap<>(Faction.class);
        for (Map.Entry<Faction, LinkedHashMap<String, ProductionNeedBalance>> factionEntry : balances.entrySet()) {
            LinkedHashMap<String, ProductionNeedBalance> byItem = new LinkedHashMap<>();
            if (factionEntry.getValue() != null) {
                for (Map.Entry<String, ProductionNeedBalance> e : factionEntry.getValue().entrySet()) {
                    if (e.getKey() != null && e.getValue() != null && e.getValue().hasSignal()) byItem.put(e.getKey(), e.getValue().copy());
                }
            }
            if (!byItem.isEmpty()) out.put(factionEntry.getKey(), byItem);
        }
        return out;
    }

    void restore(Map<Faction, ? extends Map<String, ProductionNeedBalance>> saved) {
        balances.clear();
        if (saved == null) return;
        for (Map.Entry<Faction, ? extends Map<String, ProductionNeedBalance>> factionEntry : saved.entrySet()) {
            if (factionEntry.getValue() == null) continue;
            for (Map.Entry<String, ProductionNeedBalance> e : factionEntry.getValue().entrySet()) {
                ProductionNeedBalance source = e.getValue();
                if (source == null) continue;
                ProductionNeedBalance target = balance(factionEntry.getKey(), source.item == null ? e.getKey() : source.item);
                target.produced = Math.max(0, source.produced);
                target.internalNeed = Math.max(0, source.internalNeed);
                target.externalDemand = Math.max(0, source.externalDemand);
            }
        }
    }

    void mergeFrom(FactionProductionTracker other) {
        if (other == null) return;
        for (Map.Entry<Faction, LinkedHashMap<String, ProductionNeedBalance>> factionEntry : other.snapshot().entrySet()) {
            for (ProductionNeedBalance source : factionEntry.getValue().values()) {
                ProductionNeedBalance target = balance(factionEntry.getKey(), source.item);
                target.produced += Math.max(0, source.produced);
                target.internalNeed += Math.max(0, source.internalNeed);
                target.externalDemand += Math.max(0, source.externalDemand);
            }
        }
    }

    String summary(Faction faction) {
        ArrayList<String> parts = new ArrayList<>();
        for (ProductionNeedBalance b : balances(faction)) parts.add(b.summary());
        return parts.isEmpty() ? FactionInventoryStockAuthority.normalizeFaction(faction).label + ": no production tracked" : String.join("; ", parts);
    }
}

final class ProductionNeedBalance {
    final Faction faction;
    final String item;
    int produced;
    int internalNeed;
    int externalDemand;

    ProductionNeedBalance(Faction faction, String item) {
        this.faction = faction;
        this.item = item;
    }

    int internalSurplus() {
        return produced - internalNeed;
    }

    int externalPressure() {
        return externalDemand - Math.max(0, internalSurplus());
    }

    boolean hasSignal() {
        return produced > 0 || internalNeed > 0 || externalDemand > 0;
    }

    ProductionNeedBalance copy() {
        ProductionNeedBalance copy = new ProductionNeedBalance(faction, item);
        copy.produced = produced;
        copy.internalNeed = internalNeed;
        copy.externalDemand = externalDemand;
        return copy;
    }

    String summary() {
        return item + " produced=" + produced + " internalNeed=" + internalNeed + " externalDemand=" + externalDemand + " surplus=" + internalSurplus() + " externalPressure=" + externalPressure();
    }
}
