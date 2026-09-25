package mechanist;

import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
        if (lastExpansionTick != Long.MIN_VALUE && safeTick >= lastExpansionTick && safeTick - lastExpansionTick < 120L) {
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


    void writePersistence(Properties p, String prefix) {
        if (p == null) return;
        String k = prefix == null ? "world.economy." : prefix;
        p.setProperty(k + "version", VERSION);
        p.setProperty(k + "initializedLocations", joinInts(initializedLocations));
        p.setProperty(k + "lastExpansionTick", Long.toString(lastExpansionTick));
        p.setProperty(k + "lastSummary", enc(lastSummary));

        int n = 0;
        for (Map.Entry<Faction,Integer> e : factionPopulation.snapshot().entrySet())
            p.setProperty(k + "factionPopulation." + n++, e.getKey().name() + "|" + e.getValue());
        p.setProperty(k + "factionPopulation.count", Integer.toString(n));

        n = 0;
        for (Map.Entry<String,Integer> e : zonePopulation.snapshot().entrySet())
            p.setProperty(k + "zonePopulation." + n++, enc(e.getKey()) + "|" + e.getValue());
        p.setProperty(k + "zonePopulation.count", Integer.toString(n));

        n = 0;
        for (Map.Entry<Faction,LinkedHashMap<String,Integer>> fe : factionStock.snapshot().entrySet())
            for (Map.Entry<String,Integer> e : fe.getValue().entrySet())
                p.setProperty(k + "factionStock." + n++, fe.getKey().name() + "|" + enc(e.getKey()) + "|" + e.getValue());
        p.setProperty(k + "factionStock.count", Integer.toString(n));

        n = 0;
        for (Map.Entry<String,LinkedHashMap<String,Integer>> ze : zoneStock.snapshot().entrySet())
            for (Map.Entry<String,Integer> e : ze.getValue().entrySet())
                p.setProperty(k + "zoneStock." + n++, enc(ze.getKey()) + "|" + enc(e.getKey()) + "|" + e.getValue());
        p.setProperty(k + "zoneStock.count", Integer.toString(n));

        n = 0;
        for (Map.Entry<Faction,LinkedHashMap<String,ProductionNeedBalance>> fe : factionProduction.snapshot().entrySet())
            for (ProductionNeedBalance b : fe.getValue().values())
                p.setProperty(k + "factionProduction." + n++, fe.getKey().name() + "|" + enc(b.item) + "|" + b.produced + "|" + b.internalNeed + "|" + b.externalDemand);
        p.setProperty(k + "factionProduction.count", Integer.toString(n));

        n = 0;
        for (Map.Entry<String,LinkedHashMap<String,ProductionNeedBalance>> ze : zoneProduction.snapshot().entrySet())
            for (ProductionNeedBalance b : ze.getValue().values())
                p.setProperty(k + "zoneProduction." + n++, enc(ze.getKey()) + "|" + b.faction.name() + "|" + enc(b.item) + "|" + b.produced + "|" + b.internalNeed + "|" + b.externalDemand);
        p.setProperty(k + "zoneProduction.count", Integer.toString(n));

        n = 0;
        for (Map.Entry<String,Double> e : priceIndex.snapshot().entrySet())
            p.setProperty(k + "priceIndex." + n++, enc(e.getKey()) + "|" + Double.toString(e.getValue()));
        p.setProperty(k + "priceIndex.count", Integer.toString(n));
    }

    boolean readPersistence(Properties p, String prefix) {
        if (p == null) return false;
        String k = prefix == null ? "world.economy." : prefix;
        if (p.getProperty(k + "version") == null) return false;

        initializedLocations.clear();
        for (String s : p.getProperty(k + "initializedLocations", "").split(",")) {
            if (s.isBlank()) continue;
            try { initializedLocations.add(Integer.parseInt(s)); } catch (NumberFormatException ignored) {}
        }
        try { lastExpansionTick = Long.parseLong(p.getProperty(k + "lastExpansionTick", Long.toString(Long.MIN_VALUE))); }
        catch (NumberFormatException ignored) { lastExpansionTick = Long.MIN_VALUE; }
        String savedSummary = dec(p.getProperty(k + "lastSummary", ""));
        lastSummary = savedSummary.isBlank() ? "Economy runtime state has not been initialized." : savedSummary;

        EnumMap<Faction,Integer> fp = new EnumMap<>(Faction.class);
        for (String line : numbered(p, k + "factionPopulation")) {
            String[] a = line.split("\\|", 2);
            if (a.length == 2) try { fp.put(Faction.valueOf(a[0]), Integer.parseInt(a[1])); } catch (Exception ignored) {}
        }
        factionPopulation.restore(fp);

        LinkedHashMap<String,Integer> zp = new LinkedHashMap<>();
        for (String line : numbered(p, k + "zonePopulation")) {
            String[] a = line.split("\\|", 2);
            if (a.length == 2) try { zp.put(dec(a[0]), Integer.parseInt(a[1])); } catch (Exception ignored) {}
        }
        zonePopulation.restore(zp);

        EnumMap<Faction,LinkedHashMap<String,Integer>> fs = new EnumMap<>(Faction.class);
        for (String line : numbered(p, k + "factionStock")) {
            String[] a = line.split("\\|", 3);
            if (a.length == 3) try { fs.computeIfAbsent(Faction.valueOf(a[0]), ignored -> new LinkedHashMap<>()).put(dec(a[1]), Integer.parseInt(a[2])); } catch (Exception ignored) {}
        }
        factionStock.restore(fs);

        LinkedHashMap<String,LinkedHashMap<String,Integer>> zs = new LinkedHashMap<>();
        for (String line : numbered(p, k + "zoneStock")) {
            String[] a = line.split("\\|", 3);
            if (a.length == 3) try { zs.computeIfAbsent(dec(a[0]), ignored -> new LinkedHashMap<>()).put(dec(a[1]), Integer.parseInt(a[2])); } catch (Exception ignored) {}
        }
        zoneStock.restore(zs);

        EnumMap<Faction,LinkedHashMap<String,ProductionNeedBalance>> fprod = new EnumMap<>(Faction.class);
        for (String line : numbered(p, k + "factionProduction")) {
            String[] a = line.split("\\|", 5);
            if (a.length == 5) try {
                Faction f = Faction.valueOf(a[0]); ProductionNeedBalance b = new ProductionNeedBalance(f, dec(a[1]));
                b.produced = Integer.parseInt(a[2]); b.internalNeed = Integer.parseInt(a[3]); b.externalDemand = Integer.parseInt(a[4]);
                fprod.computeIfAbsent(f, ignored -> new LinkedHashMap<>()).put(b.item, b);
            } catch (Exception ignored) {}
        }
        factionProduction.restore(fprod);

        LinkedHashMap<String,LinkedHashMap<String,ProductionNeedBalance>> zprod = new LinkedHashMap<>();
        for (String line : numbered(p, k + "zoneProduction")) {
            String[] a = line.split("\\|", 6);
            if (a.length == 6) try {
                String zoneKey = dec(a[0]); Faction f = Faction.valueOf(a[1]); ProductionNeedBalance b = new ProductionNeedBalance(f, dec(a[2]));
                b.produced = Integer.parseInt(a[3]); b.internalNeed = Integer.parseInt(a[4]); b.externalDemand = Integer.parseInt(a[5]);
                zprod.computeIfAbsent(zoneKey, ignored -> new LinkedHashMap<>()).put(b.item, b);
            } catch (Exception ignored) {}
        }
        zoneProduction.restore(zprod);

        LinkedHashMap<String,Double> prices = new LinkedHashMap<>();
        for (String line : numbered(p, k + "priceIndex")) {
            String[] a = line.split("\\|", 2);
            if (a.length == 2) try { prices.put(dec(a[0]), Double.parseDouble(a[1])); } catch (Exception ignored) {}
        }
        priceIndex.restore(prices);
        return true;
    }

    private static java.util.List<String> numbered(Properties p, String base) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        int count = 0;
        try { count = Integer.parseInt(p.getProperty(base + ".count", "0")); } catch (NumberFormatException ignored) {}
        for (int i = 0; i < count; i++) {
            String line = p.getProperty(base + "." + i);
            if (line != null) out.add(line);
        }
        return out;
    }

    private static String joinInts(Iterable<Integer> values) {
        StringBuilder sb = new StringBuilder();
        for (Integer value : values) { if (value == null) continue; if (sb.length() > 0) sb.append(','); sb.append(value); }
        return sb.toString();
    }

    private static String enc(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String value) {
        try { return new String(Base64.getUrlDecoder().decode(value == null ? "" : value), StandardCharsets.UTF_8); }
        catch (IllegalArgumentException ex) { return ""; }
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
