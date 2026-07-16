package mechanist;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.Random;

/**
 * Trader-facing commerce authority.
 *
 * Traders are person/faction access points.  Their shelf offers are filtered
 * through faction-accessible inventory instead of being treated as the same
 * thing as a vending-machine stock list.
 */
final class TraderTradeActionAuthority {
    private TraderTradeActionAuthority() {}

    static TraderSession createSessionForNpc(NpcEntity npc, ZoneType zone, Random rng) {
        Random r = rng == null ? new Random(0) : rng;
        TraderSession t = new TraderSession();
        t.name = npc == null ? "Nameless Counter" : npc.name;
        t.archetype = npc == null ? "Trader" : npc.role;
        t.zoneLabel = zone == null ? "Unknown Zone" : zone.label;
        populateAccessibleFactionStock(t, npc, zone, r);
        t.applyStockQuality(zone == null ? ZoneType.NEUTRAL_CIVILIAN_FLOOR : zone, r);
        return t;
    }

    static void populateAccessibleFactionStock(TraderSession t, NpcEntity npc, ZoneType zone, Random rng) {
        if (t == null) return;
        Random r = rng == null ? new Random(0) : rng;
        Faction faction = FactionInventoryStockAuthority.factionForTrader(npc, zone);
        t.marketFaction = faction;
        FactionCriticalVendorPlacementAuthority.Category vendorCategory =
                FactionCriticalVendorPlacementAuthority.categoryForRole(npc == null ? null : npc.role);
        if (vendorCategory != null) t.marketCategory = vendorCategory.id;
        ArrayList<String> stock = FactionInventoryStockAuthority.accessibleStock(faction, zone, r);
        for (String item : stock) addOffer(t, faction, zone, item, "faction-accessible trader stock.", null, r);
        removeInaccessibleOffers(t, faction, zone);
        FactionCriticalVendorPlacementAuthority.applyVendorStock(t, npc);
        if (t.supplyChainSummary == null || t.supplyChainSummary.isBlank()) {
            t.supplyChainSummary = "Faction shelf access: " + faction.label + " / " + (zone == null ? "Unknown Zone" : zone.label) + "; " + t.offers.size() + " accessible offer(s).";
        }
    }

    static boolean addOffer(TraderSession t, Faction faction, ZoneType zone, String item, String description, ItemProvenanceRecord provenance, Random rng) {
        if (t == null || item == null || item.isBlank() || ItemCatalog.get(item) == null) return false;
        if (!FactionInventoryStockAuthority.canAccessItem(faction, zone, item)) return false;
        int count = FactionInventoryStockAuthority.limitedStockCount(faction, zone, item, rng);
        String desc = description == null || description.isBlank() ? "faction-accessible trader stock." : description;
        desc = desc + " Stock available: " + count + ".";
        for (TradeOffer offer : t.offers) {
            if (offer.name.equalsIgnoreCase(item)) {
                if (offer.provenance == null) offer.provenance = provenance;
                if (offer.description == null || !offer.description.toLowerCase(Locale.ROOT).contains("stock available")) offer.description = offer.description + " Stock available: " + count + ".";
                return false;
            }
        }
        t.offers.add(new TradeOffer(item, categoryFor(item), Math.max(1, ItemCatalog.priceFor(item)), desc, provenance));
        return true;
    }

    static void removeInaccessibleOffers(TraderSession t, Faction faction, ZoneType zone) {
        if (t == null) return;
        Iterator<TradeOffer> it = t.offers.iterator();
        while (it.hasNext()) {
            TradeOffer offer = it.next();
            if (offer == null || !FactionInventoryStockAuthority.canAccessItem(faction, zone, offer.name)) it.remove();
        }
    }

    static void attachNpcSiteStock(TraderSession t, NpcFactionSite site, Random rng, World world, int turn) {
        if (t == null || site == null) return;
        t.sourceSite = site;
        Random r = rng == null ? new Random(FactionInventoryStockAuthority.stableSeed(site.faction, null)) : rng;
        Faction faction = FactionInventoryStockAuthority.normalizeFaction(site.faction);
        ArrayList<String> exports = site.exportSample(r);
        if (exports.isEmpty()) {
            String reason = site.workers <= 0 ? "site production is unstaffed" : "site export stock is depleted";
            String line = "Site supply: " + site.name + " contributed no shelf stock because " + reason + ".";
            t.supplyChainSummary = t.supplyChainSummary == null || t.supplyChainSummary.isBlank()
                    ? line : t.supplyChainSummary + " " + line;
        }
        for (String item : exports) {
            if (item == null || ItemCatalog.get(item) == null) continue;
            ItemProvenanceRecord made = ItemProvenanceRecord.of(item, site.faction, site.name, world, Math.max(0, site.lastProductionTurn), site.recipeSummaryFor(item), "produced into faction stock ledger");
            ItemProvenanceRecord shelf = ItemProvenanceRecord.transferred(made, item, world, turn, "loaded from " + site.name + " onto trader shelf for " + t.name);
            addSiteOffer(t, faction, item, "traceable site stock from " + site.name + ".", shelf, r);
        }
    }

    private static boolean addSiteOffer(TraderSession t, Faction faction, String item, String description,
                                        ItemProvenanceRecord provenance, Random rng) {
        if (t == null || item == null || item.isBlank() || ItemCatalog.get(item) == null) return false;
        int count = FactionInventoryStockAuthority.limitedStockCount(faction, null, item, rng);
        String desc = description + " Stock available: " + count + ".";
        for (TradeOffer offer : t.offers) {
            if (offer != null && ItemQuality.namesMatch(offer.name, item)) {
                if (offer.provenance == null) offer.provenance = provenance;
                return false;
            }
        }
        t.offers.add(new TradeOffer(item, categoryFor(item), Math.max(1, ItemCatalog.priceFor(item)), desc, provenance));
        return true;
    }

    private static String categoryFor(String item) {
        ItemDef d = ItemCatalog.get(item);
        return d == null ? "faction stock" : d.category;
    }
}
