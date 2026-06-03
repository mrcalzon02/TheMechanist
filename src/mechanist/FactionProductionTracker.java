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

    String summary() {
        return item + " produced=" + produced + " internalNeed=" + internalNeed + " externalDemand=" + externalDemand + " surplus=" + internalSurplus() + " externalPressure=" + externalPressure();
    }
}
