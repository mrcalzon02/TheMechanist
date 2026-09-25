package mechanist;

import java.util.LinkedHashMap;
import java.util.Random;
import java.util.Properties;

/** Economy initialization ownership shell for staged world-generation extraction. */
final class ZoneEconomyInitializationManager {
    static final String VERSION = "zone-economy-initialization-manager-0.9.10kx";
    private static final LinkedHashMap<String, EconomyRuntimeState> STATES_BY_HIVE = new LinkedHashMap<>();

    private ZoneEconomyInitializationManager() {}

    static WorldEconomyInitializationAuthority.Result initialize(World world) {
        if (world == null) return new WorldEconomyInitializationAuthority.Result(0, 0, 0, 0, 0, "no world supplied");
        return WorldEconomyInitializationAuthority.apply(world, world.r, stateFor(world));
    }

    static EconomyRuntimeState stateFor(World world) {
        String key = hiveKey(world);
        return STATES_BY_HIVE.computeIfAbsent(key, ignored -> new EconomyRuntimeState());
    }

    static EconomyRuntimeState.ExpansionTickResult slowGameplayExpansionTick(World world, long tickId, Random rng) {
        if (world == null) return new EconomyRuntimeState.ExpansionTickResult(false, 0, 0, 0, 0, "no world supplied");
        EconomyRuntimeState state = stateFor(world);
        Faction faction = FactionInventoryStockAuthority.factionForZone(world.zoneType);
        return state.slowExpansionTick(tickId, world, faction, rng == null ? world.r : rng);
    }

    static String summary(World world) {
        if (world == null) return "Zone economy manager unavailable: no world supplied.";
        int locationKey = WorldEconomyInitializationAuthority.locationKey(world);
        Faction faction = FactionInventoryStockAuthority.factionForZone(world.zoneType);
        return stateFor(world).summary(locationKey, faction);
    }

    static void writePersistence(World world, Properties p) {
        if (world == null || p == null) return;
        stateFor(world).writePersistence(p, "world.economy.");
    }

    static boolean readPersistence(World world, Properties p) {
        if (world == null) return false;
        String key = hiveKey(world);
        if (p == null || p.getProperty("world.economy.version") == null) {
            STATES_BY_HIVE.remove(key);
            return false;
        }
        EconomyRuntimeState restored = new EconomyRuntimeState();
        if (!restored.readPersistence(p, "world.economy.")) {
            STATES_BY_HIVE.remove(key);
            return false;
        }
        STATES_BY_HIVE.put(key, restored);
        return true;
    }

    static String auditSummary() {
        return "authority=" + VERSION + " persistentEconomyStates=" + STATES_BY_HIVE.size() + " managers=factionPopulation,zonePopulation,factionStock,zoneStock,factionProduction,zoneProduction,priceIndex";
    }

    private static String hiveKey(World world) {
        if (world == null) return "no-world";
        String hive = world.hiveName == null || world.hiveName.isBlank() ? "unnamed" : world.hiveName;
        return "hive=" + hive;
    }
}
