package mechanist;

import java.util.Random;

/**
 * Bridges generated world history into persistent stock, production, population,
 * logistics, and price-index managers.
 *
 * World generation may still call apply(world, rng), but the default path now
 * resolves the owning EconomyRuntimeState through ZoneEconomyInitializationManager
 * instead of creating throwaway trackers.
 */
final class WorldEconomyInitializationAuthority {
    private WorldEconomyInitializationAuthority() {}

    static Result apply(World world, Random rng) {
        if (world == null) return new Result(0, 0, 0, 0, 0, "no world supplied");
        return apply(world, rng, ZoneEconomyInitializationManager.stateFor(world));
    }

    static Result apply(World world, Random rng, EconomyRuntimeState state) {
        if (world == null) return new Result(0, 0, 0, 0, 0, "no world supplied");
        EconomyRuntimeState runtime = state == null ? ZoneEconomyInitializationManager.stateFor(world) : state;
        Random r = rng == null ? new Random(world.seed ^ 0xEC0A11L) : rng;

        int locationKey = locationKey(world);
        Faction zoneFaction = FactionInventoryStockAuthority.factionForZone(world.zoneType);
        if (runtime.isInitialized(locationKey)) {
            EconomyRuntimeState.ExpansionTickResult tick = runtime.slowExpansionTick(world.seed ^ locationKey ^ runtime.initializedLocationCount(), world, zoneFaction, r);
            String summary = "zone=" + world.zoneType.label + " location=" + locationKey + " faction=" + zoneFaction.label + " reusedPersistentManagers=true " + tick.summary() + " | " + runtime.summary(locationKey, zoneFaction);
            DebugLog.audit("WORLD_ECONOMY_INITIALIZATION", summary);
            return new Result(0, 0, 0, tick.routed(), 0, summary);
        }
        runtime.markInitialized(locationKey);

        runtime.factionStock.seedFaction(zoneFaction, world.zoneType, r);
        runtime.zoneStock.seedZoneFaction(locationKey, zoneFaction, world.zoneType, r);

        int roomSignals = 0;
        for (int i = 0; i < world.roomFactions.size(); i++) {
            Faction f = FactionInventoryStockAuthority.normalizeFaction(world.roomFactions.get(i));
            runtime.factionStock.seedFaction(f, world.zoneType, r);
            runtime.zoneStock.seedZoneFaction(locationKey, f, world.zoneType, r);
            runtime.factionPopulation.add(f, 1);
            runtime.zonePopulation.add(locationKey, f, 1);
            seedNeedSignals(runtime.factionProduction, runtime.zoneProduction, locationKey, f, world.zoneType);
            roomSignals++;
        }

        int npcSignals = 0;
        for (NpcEntity npc : world.npcs) {
            if (npc == null) continue;
            Faction f = FactionInventoryStockAuthority.factionForTrader(npc, world.zoneType);
            runtime.factionPopulation.add(f, 1);
            runtime.zonePopulation.add(locationKey, f, 1);
            npcSignals++;
        }

        int productionSignals = seedProductionHistory(world, locationKey, zoneFaction, runtime.factionProduction, runtime.zoneProduction);
        int stockRoutes = seedInternalRoutes(locationKey, zoneFaction, runtime.factionStock, runtime.zoneStock, runtime.zoneProduction);
        int priceSignals = seedPriceIndexes(zoneFaction, runtime.factionProduction, runtime.priceIndex);
        EconomyRuntimeState.ExpansionTickResult tick = runtime.slowExpansionTick(Math.max(1L, world.seed ^ locationKey), world, zoneFaction, r);
        LogisticsAssetPool pool = LogisticsAssetPool.fromPopulationAndVehicles(zoneFaction, Math.max(1, runtime.zonePopulation.totalInZone(locationKey)), Math.max(0, world.rooms.size() / 12), java.util.Collections.emptyList());
        LogisticsExecutionPlan logisticsPlan = FactionLogisticsExecutionManager.plan(zoneFaction, locationKey, locationKey, "Emergency rations", Math.max(1, world.rooms.size() / 6), pool);

        String summary = "zone=" + world.zoneType.label
                + " location=" + locationKey
                + " faction=" + zoneFaction.label
                + " rooms=" + roomSignals
                + " npcs=" + npcSignals
                + " productionSignals=" + productionSignals
                + " stockRoutes=" + stockRoutes
                + " priceSignals=" + priceSignals
                + " expansion=" + tick.summary()
                + " logistics=" + logisticsPlan.summary()
                + " | " + runtime.summary(locationKey, zoneFaction);
        runtime.setLastSummary(summary);
        DebugLog.audit("WORLD_ECONOMY_INITIALIZATION", summary);
        return new Result(roomSignals, npcSignals, productionSignals, stockRoutes, priceSignals, summary);
    }

    private static void seedNeedSignals(FactionProductionTracker factionProduction, ZoneProductionTracker zoneProduction, int locationKey, Faction faction, ZoneType zone) {
        String need = EconomyRuntimeState.baselineNeedItem(zone);
        factionProduction.recordInternalNeed(faction, need, 2);
        zoneProduction.recordInternalNeed(locationKey, faction, need, 2);
        if (zone == ZoneType.SUMP_MARKET || zone == ZoneType.NEUTRAL_RAIL_DEPOT || zone == ZoneType.TRAIN_SERVICE_YARD) {
            factionProduction.recordExternalDemand(faction, need, 1);
            zoneProduction.recordExternalDemand(locationKey, faction, need, 1);
        }
    }

    private static int seedProductionHistory(World world, int locationKey, Faction faction, FactionProductionTracker factionProduction, ZoneProductionTracker zoneProduction) {
        int signals = 0;
        String produced = EconomyRuntimeState.productionItem(world.zoneType);
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
