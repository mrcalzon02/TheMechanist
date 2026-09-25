package mechanist;

import java.util.Properties;

/** Round-trip guard for the extracted economy authority at the save boundary. */
final class EconomyRuntimePersistenceSmoke {
    public static void main(String[] args) {
        EconomyRuntimeState before = new EconomyRuntimeState();
        Faction faction = Faction.SCAVENGER;
        int location = 4107;

        before.markInitialized(location);
        before.factionPopulation.add(faction, 17);
        before.zonePopulation.add(location, faction, 9);
        before.factionStock.add(faction, "Machine part", 6);
        before.zoneStock.add(location, faction, "Machine part", 4);
        before.factionProduction.recordProduction(faction, "Machine part", 11);
        before.factionProduction.recordInternalNeed(faction, "Machine part", 3);
        before.factionProduction.recordExternalDemand(faction, "Machine part", 5);
        before.zoneProduction.recordProduction(location, faction, "Machine part", 7);
        before.zoneProduction.recordInternalNeed(location, faction, "Machine part", 2);
        before.zoneProduction.recordExternalDemand(location, faction, "Machine part", 4);
        double expectedPrice = before.priceIndex.updateIndex("Machine part", 3, 12, 1, 2);
        before.setLastSummary("round-trip sentinel");

        Properties saved = new Properties();
        before.writePersistence(saved, "world.economy.");

        EconomyRuntimeState after = new EconomyRuntimeState();
        require(after.readPersistence(saved, "world.economy."), "saved economy marker was not recognized");
        require(after.isInitialized(location), "initialized location was lost");
        require(after.factionPopulation.count(faction) == 17, "faction population was lost");
        require(after.zonePopulation.count(location, faction) == 9, "zone population was lost");
        require(after.factionStock.count(faction, "Machine part") == 6, "faction stock was lost");
        require(after.zoneStock.count(location, faction, "Machine part") == 4, "zone stock was lost");

        ProductionNeedBalance factionBalance = after.factionProduction.balance(faction, "Machine part");
        require(factionBalance.produced == 11 && factionBalance.internalNeed == 3 && factionBalance.externalDemand == 5,
                "faction production/need balance was lost");
        ProductionNeedBalance zoneBalance = after.zoneProduction.balance(location, faction, "Machine part");
        require(zoneBalance.produced == 7 && zoneBalance.internalNeed == 2 && zoneBalance.externalDemand == 4,
                "zone production/need balance was lost");
        require(Math.abs(after.priceIndex.indexFor("Machine part") - expectedPrice) < 0.000001, "price index was lost");
        require("round-trip sentinel".equals(after.lastSummary()), "last summary was lost");

        Properties second = new Properties();
        after.writePersistence(second, "world.economy.");
        require(saved.getProperty("world.economy.lastExpansionTick").equals(second.getProperty("world.economy.lastExpansionTick")),
                "expansion tick persistence was not stable");

        // Legacy saves predate the economy persistence marker. Loading one into a
        // long-lived process must evict any economy state already cached under the
        // same hive key rather than leaking the previous world's simulation state.
        World legacyWorld = new World(9191L, 8, 8);
        legacyWorld.hiveName = "economy-legacy-reset-smoke";
        EconomyRuntimeState cached = ZoneEconomyInitializationManager.stateFor(legacyWorld);
        cached.factionPopulation.add(faction, 99);
        cached.factionStock.add(faction, "Machine part", 33);
        require(cached.factionPopulation.count(faction) == 99, "legacy reset precondition population was not cached");
        require(cached.factionStock.count(faction, "Machine part") == 33, "legacy reset precondition stock was not cached");

        Properties legacy = new Properties();
        require(!ZoneEconomyInitializationManager.readPersistence(legacyWorld, legacy),
                "markerless legacy properties should not report economy restoration");
        EconomyRuntimeState reset = ZoneEconomyInitializationManager.stateFor(legacyWorld);
        require(reset.factionPopulation.count(faction) == 0,
                "markerless legacy load retained stale faction population");
        require(reset.factionStock.count(faction, "Machine part") == 0,
                "markerless legacy load retained stale faction stock");

        System.out.println("EconomyRuntimePersistenceSmoke OK keys=" + saved.size() + " legacyReset=true");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
