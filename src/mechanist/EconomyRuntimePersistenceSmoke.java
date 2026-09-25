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
        System.out.println("EconomyRuntimePersistenceSmoke OK keys=" + saved.size());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
