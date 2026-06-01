package mechanist;

import java.util.ArrayList;
import java.util.Random;

/**
 * Vending-machine stock authority.
 *
 * Vending machines are mechanical limited-stock points, not traders.  They get a
 * generated stock list from faction/zone access and persist a compact stockState
 * payload on their MapObjectState.
 */
final class LimitedVendingStockAuthority {
    private LimitedVendingStockAuthority() {}

    static void seedMachineStock(MapObjectState machine, ZoneType zone, Random rng) {
        if (machine == null) return;
        Random r = rng == null ? new Random(stableMachineSeed(machine, zone)) : rng;
        Faction faction = FactionInventoryStockAuthority.factionForZone(zone);
        ArrayList<String> access = FactionInventoryStockAuthority.accessibleStock(faction, zone, r);
        ArrayList<String> picks = new ArrayList<>();
        int target = Math.max(2, Math.min(6, 2 + r.nextInt(5)));
        for (String item : access) {
            if (item == null || ItemCatalog.get(item) == null) continue;
            if (isVendingSuitable(item, zone) && !picks.contains(item)) picks.add(item);
            if (picks.size() >= target) break;
        }
        if (picks.isEmpty()) {
            addIfCataloged(picks, "Emergency rations");
            addIfCataloged(picks, "Water bottle");
        }
        machine.stockState = encode(machine.stockState, faction, picks, zone, r);
        machine.vendCount = 0;
    }

    static boolean hasStock(MapObjectState machine) {
        return remaining(machine) > 0 && !items(machine).isEmpty();
    }

    static int remaining(MapObjectState machine) {
        if (machine == null || machine.stockState == null) return 0;
        String raw = MapObjectState.stockValue(machine.stockState, "remaining");
        try { return Math.max(0, Integer.parseInt(raw)); } catch (Exception ignored) { return 0; }
    }

    static ArrayList<String> items(MapObjectState machine) {
        ArrayList<String> out = new ArrayList<>();
        if (machine == null || machine.stockState == null) return out;
        String raw = MapObjectState.stockValue(machine.stockState, "items");
        if (raw == null || raw.isBlank()) return out;
        for (String encoded : raw.split(",")) {
            String item = encoded.replace('_', ' ').trim();
            if (!item.isBlank() && ItemCatalog.get(item) != null) out.add(item);
        }
        return out;
    }

    static String vendOne(MapObjectState machine, Random rng) {
        if (!hasStock(machine)) return "";
        Random r = rng == null ? new Random(stableMachineSeed(machine, null) ^ machine.vendCount) : rng;
        ArrayList<String> stock = items(machine);
        if (stock.isEmpty()) return "";
        String item = stock.get(Math.floorMod(r.nextInt(), stock.size()));
        int rem = Math.max(0, remaining(machine) - 1);
        machine.vendCount++;
        machine.stockState = MapObjectState.setStockFlag(machine.stockState, "remaining", String.valueOf(rem));
        machine.stockState = MapObjectState.setStockFlag(machine.stockState, "lastVend", item.replace(' ', '_'));
        return item;
    }

    private static String encode(String previous, Faction faction, ArrayList<String> items, ZoneType zone, Random r) {
        String stock = previous == null || previous.isBlank() ? "seeded-stock" : previous;
        stock = MapObjectState.setStockFlag(stock, "mode", "limited-vending");
        stock = MapObjectState.setStockFlag(stock, "faction", FactionInventoryStockAuthority.normalizeFaction(faction).name());
        stock = MapObjectState.setStockFlag(stock, "zone", zone == null ? "UNKNOWN" : zone.name());
        stock = MapObjectState.setStockFlag(stock, "remaining", String.valueOf(Math.max(1, Math.min(8, items.size() + (r == null ? 0 : r.nextInt(3))))));
        stock = MapObjectState.setStockFlag(stock, "items", encodeItems(items));
        return stock;
    }

    private static String encodeItems(ArrayList<String> items) {
        ArrayList<String> out = new ArrayList<>();
        for (String item : items) if (item != null && ItemCatalog.get(item) != null) out.add(item.replace(' ', '_'));
        return String.join(",", out);
    }

    private static boolean isVendingSuitable(String item, ZoneType zone) {
        if (item == null) return false;
        ItemDef def = ItemCatalog.get(item);
        if (def == null) return false;
        String text = (item + " " + def.category + " " + def.use).toLowerCase();
        if (text.contains("food") || text.contains("water") || text.contains("medical") || text.contains("tool") || text.contains("component")) return true;
        if (zone == ZoneType.MECHANICUS_FORGE_CLOISTER || zone == ZoneType.MECHANICUS_RELIC_DUCT) return text.contains("machine") || text.contains("wire") || text.contains("security");
        if (zone == ZoneType.SUMP_MARKET || zone == ZoneType.NEUTRAL_RAIL_DEPOT || zone == ZoneType.TRAIN_SERVICE_YARD) return text.contains("trade") || text.contains("commerce") || text.contains("cargo");
        return false;
    }

    private static void addIfCataloged(ArrayList<String> out, String item) {
        if (ItemCatalog.get(item) != null) out.add(item);
    }

    private static long stableMachineSeed(MapObjectState machine, ZoneType zone) {
        long a = machine == null || machine.id == null ? 0 : machine.id.hashCode();
        long b = zone == null ? 0 : zone.name().hashCode();
        return a * 31L + b * 17L;
    }
}
