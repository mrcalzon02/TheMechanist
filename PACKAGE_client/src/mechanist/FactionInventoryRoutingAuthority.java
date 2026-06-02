package mechanist;

import java.util.ArrayList;

/** Routes stock between faction-wide and zone/faction stock ledgers. */
final class FactionInventoryRoutingAuthority {
    private FactionInventoryRoutingAuthority() {}

    static boolean routeFactionToZone(FactionWideStockTracker factionStock, ZoneFactionStockTracker zoneStock, int locationKey, Faction faction, String item, int count) {
        if (factionStock == null || zoneStock == null || item == null || item.isBlank() || count <= 0) return false;
        if (!factionStock.consume(faction, item, count)) return false;
        zoneStock.add(locationKey, faction, item, count);
        return true;
    }

    static boolean routeZoneToFaction(ZoneFactionStockTracker zoneStock, FactionWideStockTracker factionStock, int locationKey, Faction faction, String item, int count) {
        if (factionStock == null || zoneStock == null || item == null || item.isBlank() || count <= 0) return false;
        if (!zoneStock.consume(locationKey, faction, item, count)) return false;
        factionStock.add(faction, item, count);
        return true;
    }

    static int satisfyInternalZoneDemand(FactionWideStockTracker factionStock, ZoneFactionStockTracker zoneStock, ZoneProductionTracker zoneProduction, int locationKey, Faction faction, String item, int requested) {
        if (requested <= 0) return 0;
        int moved = 0;
        while (moved < requested && routeFactionToZone(factionStock, zoneStock, locationKey, faction, item, 1)) moved++;
        if (zoneProduction != null) zoneProduction.recordInternalNeed(locationKey, faction, item, Math.max(0, requested - moved));
        return moved;
    }

    static ArrayList<String> routeableItems(FactionWideStockTracker factionStock, Faction faction) {
        return factionStock == null ? new ArrayList<>() : factionStock.availableItems(faction);
    }
}
