package mechanist;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Supply/demand price index controller.
 *
 * Internal faction demand is intentionally not used as a price pressure signal.
 * External demand, inter-faction trade pressure, and excess supply shape the
 * market index.  Internal demand should drive faction routing and production,
 * not public price inflation by itself.
 */
final class PriceIndexControlAuthority {
    private final LinkedHashMap<String, Double> indexByItem = new LinkedHashMap<>();

    double indexFor(String item) {
        if (item == null || item.isBlank()) return 1.0;
        return indexByItem.getOrDefault(item, 1.0);
    }

    double updateIndex(String item, int produced, int externalDemand, int externalSupply, int interFactionTradeVolume) {
        if (item == null || item.isBlank()) return 1.0;
        int demand = Math.max(0, externalDemand) + Math.max(0, interFactionTradeVolume);
        int supply = Math.max(0, produced) + Math.max(0, externalSupply);
        double current = indexFor(item);
        double pressure;
        if (demand == 0 && supply == 0) pressure = 0.0;
        else if (demand > supply) pressure = Math.min(0.35, (demand - supply) / (double)Math.max(4, demand + supply));
        else if (supply > demand) pressure = -Math.min(0.30, (supply - demand) / (double)Math.max(4, demand + supply));
        else pressure = 0.0;
        double next = clamp(current + pressure, 0.45, 2.75);
        indexByItem.put(item, next);
        return next;
    }

    int indexedPrice(String item, int basePrice) {
        return Math.max(1, (int)Math.round(Math.max(1, basePrice) * indexFor(item)));
    }

    void absorbBalance(ProductionNeedBalance balance, int externalSupply, int interFactionTradeVolume) {
        if (balance == null) return;
        updateIndex(balance.item, balance.produced, balance.externalDemand, externalSupply, interFactionTradeVolume);
    }

    String summary() {
        if (indexByItem.isEmpty()) return "No price indexes tracked";
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Double> e : indexByItem.entrySet()) {
            if (sb.length() > 0) sb.append("; ");
            sb.append(e.getKey()).append("=").append(String.format(java.util.Locale.ROOT, "%.2f", e.getValue()));
        }
        return sb.toString();
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
