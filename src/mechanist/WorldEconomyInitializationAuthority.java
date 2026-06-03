package mechanist;

import java.util.Random;

/**
 * Bridges generated world history into the new stock, production, population,
 * logistics, and price-index authorities.
 *
 * This is the safe seam for the next World.generate wiring pass: call apply(world,
 * rng) after room factions/population ledgers exist and before final tile compile.
 */
final class WorldEconomyInitializationAuthority {
    private WorldEconomyInitializationAuthority() {}

    static Result apply(World world, Random rng) {
        if (world == null) return new Result(0, 0, 0, 0, 0, "no world supplied");
        Random r = rng == null ? new Random(world.seed ^ 0xEC0A11L) : rng;
        FactionWideStockTracker factionStock = new FactionWideStockTracker();
        ZoneFactionStockTracker zoneStock = new ZoneFactionStockTracker();
        FactionProductionTracker factionProduction = new FactionProductionTracker();
        ZoneProductionTracker zoneProduction = new ZoneProductionTracker();
        FactionPopulationTracker factionPopulation = new FactionPopulationTracker();
        ZonePopulationTracker zonePopulation = new ZonePopulationTracker();
        PriceIndexControlAuthority priceIndex = new PriceIndexControlAuthority();

        int locationKey = locationKey(world);
        Faction zoneFaction = FactionInventoryStockAuthority.factionForZone(world.zoneType);
        factionStock.seedFaction(zoneFaction, world.zoneType, r);
        zoneStock.seedZoneFaction(locationKey, zoneFaction, world.zoneType, r);

        int roomSignals = 0;
        for (int i = 0; i < world.roomFactions.size(); i++) {
            Faction f = FactionInventoryStockAuthority.normalizeFaction(world.roomFactions.get(i));
            factionStock.seedFaction(f, world.zoneType, r);
            zoneStock.seedZoneFaction(locationKey, f, world.zoneType, r);
            factionPopulation.add(f, 1);
            zonePopulation.add(locationKey, f, 1);
            seedNeedSignals(factionProduction, zoneProduction, locationKey, f, world.zoneType);
            roomSignals++;
        }

        int npcSignals = 0;
        for (NpcEntity npc : world.npcs) {
            if (npc == null) continue;
            Faction f = FactionInventoryStockAuthority.factionForTrader(npc, world.zoneType);
            factionPopulation.add(f, 1);
            zonePopulation.add(locationKey, f, 1);
            npcSignals++;
        }

        int productionSignals = seedProductionHistory(world, locationKey, zoneFaction, factionProduction, zoneProduction);
        int stockRoutes = seedInternalRoutes(locationKey, zoneFaction, factionStock, zoneStock, zoneProduction);
        int priceSignals = seedPriceIndexes(zoneFaction, factionProduction, priceIndex);
        LogisticsAssetPool pool = LogisticsAssetPool.fromPopulationAndVehicles(zoneFaction, Math.max(1, zonePopulation.totalInZone(locationKey)), Math.max(0, world.rooms.size() / 12), java.util.Collections.emptyList());
        LogisticsExecutionPlan logisticsPlan = FactionLogisticsExecutionManager.plan(zoneFaction, locationKey, locationKey, "Emergency rations", Math.max(1, world.rooms.size() / 6), pool);

        String summary = "zone=" + world.zoneType.label + " location=" + locationKey + " faction=" + zoneFaction.label + " rooms=" + roomSignals + " npcs=" + npcSignals + " productionSignals=" + productionSignals + " stockRoutes=" + stockRoutes + " priceSignals=" + priceSignals + " logistics=" + logisticsPlan.summary();
        DebugLog.audit("WORLD_ECONOMY_INITIALIZATION", summary);
        return new Result(roomSignals, npcSignals, productionSignals, stockRoutes, priceSignals, summary);
    }

    private static void seedNeedSignals(FactionProductionTracker factionProduction, ZoneProductionTracker zoneProduction, int locationKey, Faction faction, ZoneType zone) {
        String need = baselineNeedItem(zone);
        factionProduction.recordInternalNeed(faction, need, 2);
        zoneProduction.recordInternalNeed(locationKey, faction, need, 2);
        if (zone == ZoneType.SUMP_MARKET || zone == ZoneType.NEUTRAL_RAIL_DEPOT || zone == ZoneType.TRAIN_SERVICE_YARD) {
            factionProduction.recordExternalDemand(faction, need, 1);
            zoneProduction.recordExternalDemand(locationKey, faction, need, 1);
        }
    }

    private static int seedProductionHistory(World world, int locationKey, Faction faction, FactionProductionTracker factionProduction, ZoneProductionTracker zoneProduction) {
        int signals = 0;
        String produced = productionItem(world.zoneType);
        if (produced != null) {
            int amount = Math.max(1, world.rooms.size() / 5);
            factionProduction.recordProduction(faction, produced, amount);
            zoneProduction.recordProduction(locationKey, faction, produced, amount);
            signals++;
        }
        if (world.zoneProductionHistory != null && !world.zoneProductionHistory.toLowerCase(java.util.Locale.ROOT).contains("no production")) {
            factionProduction.recordProduction(faction, produced == null ? "Machine part" : produced, 1);
            zoneProduction.recordProduction(locationKey, faction, produced == null ? "Machine part" : produced, 1);
            signals++;
        }
        return signals;
    }

    private static int seedInternalRoutes(int locationKey, Faction faction, FactionWideStockTracker factionStock, ZoneFactionStockTracker zoneStock, ZoneProductionTracker zoneProduction) {
        int routes = 0;
        for (String item : factionStock.availableItems(faction)) {
            if (FactionInventoryRoutingAuthority.satisfyInternalZoneDemand(factionStock, zoneStock, zoneProduction, locationKey, faction, item, 1) > 0) routes++;
            if (routes >= 3) break;
        }
        return routes;
    }

    private static int seedPriceIndexes(Faction faction, FactionProductionTracker factionProduction, PriceIndexControlAuthority priceIndex) {
        int signals = 0;
        for (ProductionNeedBalance balance : factionProduction.balances(faction)) {
            priceIndex.absorbBalance(balance, Math.max(0, balance.produced - balance.internalNeed), Math.max(0, balance.externalDemand));
            signals++;
        }
        return signals;
    }

    private static String baselineNeedItem(ZoneType zone) {
        if (zone == ZoneType.MECHANICUS_FORGE_CLOISTER || zone == ZoneType.MECHANICUS_RELIC_DUCT) return "Machine part";
        if (zone == ZoneType.SUMP_MARKET || zone == ZoneType.NEUTRAL_RAIL_DEPOT || zone == ZoneType.TRAIN_SERVICE_YARD) return "Water bottle";
        if (zone == ZoneType.IMPERIAL_GUARD_BILLET) return "Plain ration pack";
        return "Emergency rations";
    }

    private static String productionItem(ZoneType zone) {
        if (zone == ZoneType.MECHANICUS_FORGE_CLOISTER || zone == ZoneType.MECHANICUS_RELIC_DUCT) return "Machine part";
        if (zone == ZoneType.SUMP_MARKET) return "Trade chit";
        if (zone == ZoneType.NEUTRAL_RAIL_DEPOT || zone == ZoneType.TRAIN_SERVICE_YARD) return "Rail cargo stencil kit";
        if (zone == ZoneType.ADMINISTRATUM_ARCHIVE) return "Permit form";
        return null;
    }

    static int locationKey(World world) {
        if (world == null) return 0;
        int key = 17;
        key = key * 31 + world.sectorX;
        key = key * 31 + world.sectorY;
        key = key * 31 + world.zoneX;
        key = key * 31 + world.zoneY;
        key = key * 31 + world.floor;
        key = key * 31 + (world.sewerLayer ? 1 : 0);
        return key;
    }

    static final class Result {
        final int roomSignals;
        final int npcSignals;
        final int productionSignals;
        final int stockRoutes;
        final int priceSignals;
        final String summary;

        Result(int roomSignals, int npcSignals, int productionSignals, int stockRoutes, int priceSignals, String summary) {
            this.roomSignals = roomSignals;
            this.npcSignals = npcSignals;
            this.productionSignals = productionSignals;
            this.stockRoutes = stockRoutes;
            this.priceSignals = priceSignals;
            this.summary = summary == null ? "" : summary;
        }

        String summary() { return summary; }
    }
}
