package mechanist;

import java.util.LinkedHashSet;
import java.util.Random;

/**
 * Persistent economy-state container for the faction/zone economy managers.
 *
 * The thin tracker classes hold the ledgers.  This class owns their lifetime so
 * generated-zone economy initialization no longer creates throwaway trackers.
 */
final class EconomyRuntimeState {
    static final String VERSION = "economy-runtime-state-0.9.10kx";

    final FactionPopulationTracker factionPopulation = new FactionPopulationTracker();
    final ZonePopulationTracker zonePopulation = new ZonePopulationTracker();
    final FactionWideStockTracker factionStock = new FactionWideStockTracker();
    final ZoneFactionStockTracker zoneStock = new ZoneFactionStockTracker();
    final FactionProductionTracker factionProduction = new FactionProductionTracker();
    final ZoneProductionTracker zoneProduction = new ZoneProductionTracker();
    final PriceIndexControlAuthority priceIndex = new PriceIndexControlAuthority();

    private final LinkedHashSet<Integer> initializedLocations = new LinkedHashSet<>();
    private long lastExpansionTick = Long.MIN_VALUE;
    private String lastSummary = "Economy runtime state has not been initialized.";

    boolean markInitialized(int locationKey) {
        return initializedLocations.add(locationKey);
    }

    boolean isInitialized(int locationKey) {
        return initializedLocations.contains(locationKey);
    }

    int initializedLocationCount() {
        return initializedLocations.size();
    }

    String lastSummary() {
        return lastSummary;
    }

    void setLastSummary(String summary) {
        lastSummary = summary == null || summary.isBlank() ? lastSummary : summary;
    }

    int routeInternalDemand(int locationKey, Faction faction, String item, int requested) {
        return FactionInventoryRoutingAuthority.satisfyInternalZoneDemand(factionStock, zoneStock, zoneProduction, locationKey, faction, item, requested);
    }

    ExpansionTickResult slowExpansionTick(long tickId, World world, Faction faction, Random rng) {
        if (world == null) return new ExpansionTickResult(false, 0, 0, 0, 0, "no world supplied");
        long safeTick = Math.max(0L, tickId);
        if (lastExpansionTick != Long.MIN_VALUE && safeTick - lastExpansionTick < 120L) {
            return new ExpansionTickResult(false, 0, 0, 0, 0, "slow economy expansion tick gated previous=" + lastExpansionTick + " current=" + safeTick);
        }
        lastExpansionTick = safeTick;
        int locationKey = WorldEconomyInitializationAuthority.locationKey(world);
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction == null ? FactionInventoryStockAuthority.factionForZone(world.zoneType) : faction);
        Random r = rng == null ? new Random(world.seed ^ safeTick ^ 0x51A0EC0A11L) : rng;

        int produced = 0;
        int routed = 0;
        int populationGrowth = 0;
        int pressure = 0;

        String producedItem = productionItem(world.zoneType);
        if (producedItem != null) {
            produced = 1 + Math.max(0, world.rooms.size() / 40);
            factionProduction.recordProduction(f, producedItem, produced);
            zoneProduction.recordProduction(locationKey, f, producedItem, produced);
            zoneStock.add(locationKey, f, producedItem, Math.max(1, produced / 2));
            factionStock.add(f, producedItem, Math.max(1, produced - Math.max(1, produced / 2)));
        }

        String need = baselineNeedItem(world.zoneType);
        int requested = 1 + Math.max(0, zonePopulation.totalInZone(locationKey) / 12);
        routed = routeInternalDemand(locationKey, f, need, requested);
        if (routed < requested) {
            pressure = requested - routed;
            factionProduction.recordInternalNeed(f, need, pressure);
            zoneProduction.recordInternalNeed(locationKey, f, need, pressure);
        }

        if (r.nextInt(100) < 18 && zoneStock.totalCount() > 0) {
            populationGrowth = 1;
            factionPopulation.add(f, 1);
            zonePopulation.add(locationKey, f, 1);
        }

        for (ProductionNeedBalance balance : factionProduction.balances(f)) {
            priceIndex.absorbBalance(balance, Math.max(0, balance.produced - balance.internalNeed), Math.max(0, balance.externalDemand));
        }

        String summary = "slow economy expansion tick=" + safeTick
                + " location=" + locationKey
                + " faction=" + f.label
                + " produced=" + produced
                + " routed=" + routed
                + " populationGrowth=" + populationGrowth
                + " unmetNeed=" + pressure
                + " factionStock=" + factionStock.totalCount()
                + " zoneStock=" + zoneStock.totalCount();
        setLastSummary(summary);
        return new ExpansionTickResult(true, produced, routed, populationGrowth, pressure, summary);
    }

    String summary(int locationKey, Faction faction) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        return "economyState=" + VERSION
                + " initializedLocations=" + initializedLocationCount()
                + " location=" + locationKey
                + " faction=" + f.label
                + " factionPopulation=" + factionPopulation.count(f)
                + " zonePopulation=" + zonePopulation.count(locationKey, f)
                + " factionStock=" + factionStock.totalCount()
                + " zoneStock=" + zoneStock.totalCount()
                + " factionProductionSignals=" + factionProduction.signalCount()
                + " zoneProductionSignals=" + zoneProduction.signalCount()
                + " prices=" + priceIndex.summary()
                + " last=" + lastSummary;
    }

    static String baselineNeedItem(ZoneType zone) {
        if (zone == ZoneType.MECHANICUS_FORGE_CLOISTER || zone == ZoneType.MECHANICUS_RELIC_DUCT) return "Machine part";
        if (zone == ZoneType.SUMP_MARKET || zone == ZoneType.NEUTRAL_RAIL_DEPOT || zone == ZoneType.TRAIN_SERVICE_YARD) return "Water bottle";
        if (zone == ZoneType.IMPERIAL_GUARD_BILLET) return "Plain ration pack";
        return "Emergency rations";
    }

    static String productionItem(ZoneType zone) {
        if (zone == ZoneType.MECHANICUS_FORGE_CLOISTER || zone == ZoneType.MECHANICUS_RELIC_DUCT) return "Machine part";
        if (zone == ZoneType.SUMP_MARKET) return "Trade chit";
        if (zone == ZoneType.NEUTRAL_RAIL_DEPOT || zone == ZoneType.TRAIN_SERVICE_YARD) return "Rail cargo stencil kit";
        if (zone == ZoneType.ADMINISTRATUM_ARCHIVE) return "Permit form";
        return null;
    }

    record ExpansionTickResult(boolean applied, int produced, int routed, int populationGrowth, int unmetNeed, String summary) { }
}
