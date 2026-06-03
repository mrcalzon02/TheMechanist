package mechanist;

import java.util.ArrayList;

/** Routes stock between faction-wide and zone/faction stock ledgers. */
final class FactionInventoryRoutingAuthority {
    private FactionInventoryRoutingAuthority() {}

    static boolean routeFactionToZone(FactionWideStockTracker factionStock, ZoneFactionStockTracker zoneStock, int locationKey, Faction faction, String item, int count) {
        return transferFactionToZone(factionStock, zoneStock, locationKey, faction, item, count) == Math.max(1, count);
    }

    static int transferFactionToZone(FactionWideStockTracker factionStock, ZoneFactionStockTracker zoneStock, int locationKey, Faction faction, String item, int requested) {
        if (factionStock == null || zoneStock == null || item == null || item.isBlank() || requested <= 0) return 0;
        int moved = Math.min(Math.max(0, requested), factionStock.count(faction, item));
        if (moved <= 0) return 0;
        if (!factionStock.consume(faction, item, moved)) return 0;
        zoneStock.add(locationKey, faction, item, moved);
        return moved;
    }

    static boolean routeZoneToFaction(ZoneFactionStockTracker zoneStock, FactionWideStockTracker factionStock, int locationKey, Faction faction, String item, int count) {
        return transferZoneToFaction(zoneStock, factionStock, locationKey, faction, item, count) == Math.max(1, count);
    }

    static int transferZoneToFaction(ZoneFactionStockTracker zoneStock, FactionWideStockTracker factionStock, int locationKey, Faction faction, String item, int requested) {
        if (factionStock == null || zoneStock == null || item == null || item.isBlank() || requested <= 0) return 0;
        int moved = Math.min(Math.max(0, requested), zoneStock.count(locationKey, faction, item));
        if (moved <= 0) return 0;
        if (!zoneStock.consume(locationKey, faction, item, moved)) return 0;
        factionStock.add(faction, item, moved);
        return moved;
    }

    static int satisfyInternalZoneDemand(FactionWideStockTracker factionStock, ZoneFactionStockTracker zoneStock, ZoneProductionTracker zoneProduction, int locationKey, Faction faction, String item, int requested) {
        if (requested <= 0) return 0;
        int moved = transferFactionToZone(factionStock, zoneStock, locationKey, faction, item, requested);
        if (zoneProduction != null) zoneProduction.recordInternalNeed(locationKey, faction, item, Math.max(0, requested - moved));
        return moved;
    }

    static int returnZoneSurplusToFaction(ZoneFactionStockTracker zoneStock, FactionWideStockTracker factionStock, int locationKey, Faction faction, String item, int reserveLocal, int maxReturn) {
        if (zoneStock == null || factionStock == null || item == null || item.isBlank() || maxReturn <= 0) return 0;
        int available = Math.max(0, zoneStock.count(locationKey, faction, item) - Math.max(0, reserveLocal));
        return transferZoneToFaction(zoneStock, factionStock, locationKey, faction, item, Math.min(available, maxReturn));
    }

    static ArrayList<String> routeableItems(FactionWideStockTracker factionStock, Faction faction) {
        return factionStock == null ? new ArrayList<>() : factionStock.availableItems(faction);
    }
}
