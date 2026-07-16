package mechanist;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/** Seeds faction-owned vehicle and vehicle-commerce hooks into the street frontage pass. */
final class VehicleEconomyFrontageAuthority {
    static final String VERSION = "0.9.10jf";

    static final class Result {
        int dealerships;
        int partsStores;
        int serviceGarages;
        int transitStops;
        int motorPools;
        int personalVehicles;
        int publicVehicles;
        int factionVehicles;

        String summary() {
            return "dealerships=" + dealerships + " partsStores=" + partsStores +
                    " serviceGarages=" + serviceGarages + " transitStops=" + transitStops +
                    " motorPools=" + motorPools + " personalVehicles=" + personalVehicles +
                    " publicVehicles=" + publicVehicles + " factionVehicles=" + factionVehicles;
        }
    }

    private static final String[] PARTS = {
            "Machine part", "Spare bolts", "Wire bundle", "Machine oil vial", "Tool bundle",
            "Gear train", "Motor coil pack", "Pressure hose bundle"
    };

    private VehicleEconomyFrontageAuthority() {}

    static Result apply(World w, ArrayList<Point> candidates, Random r) {
        Result result = new Result();
        if (w == null || candidates == null || candidates.isEmpty()) return result;
        if (r == null) r = new Random(w.seed ^ 0x5EEDCA7L);
        ArrayList<Point> open = new ArrayList<>(candidates);
        Collections.shuffle(open, r);
        List<Faction> factions = factionsForZone(w.zoneType);

        result.dealerships += seedFrontage(w, open, r, AssetIntegrationDisciplineAuthority.VEHICLE_DEALERSHIP_FRONTAGE,
                "Licensed vehicle dealership", "service=vehicle-purchase;inventory=personal-vehicles;priceBand=90-360", Faction.CIVIC_LEDGER_OFFICE);
        result.partsStores += seedFrontage(w, open, r, AssetIntegrationDisciplineAuthority.VEHICLE_PARTS_STORE_FRONTAGE,
                "Vehicle parts and consumables store", "service=parts-purchase;inventory=vehicle-parts;priceBand=2-18", primaryFaction(factions));
        result.serviceGarages += seedFrontage(w, open, r, AssetIntegrationDisciplineAuthority.VEHICLE_SERVICE_GARAGE_FRONTAGE,
                "Vehicle maintenance garage", "service=vehicle-maintenance;repairCost=8;partsAccepted=true", primaryFaction(factions));
        result.transitStops += seedFrontage(w, open, r, AssetIntegrationDisciplineAuthority.PUBLIC_TRANSIT_STOP_FRONTAGE,
                "Public transit and taxi stand", "service=public-transit;fareBand=2-8;routes=local-zone", Faction.CIVIC_LEDGER_OFFICE);

        for (Faction faction : factions) {
            if (faction == null || faction == Faction.NONE) continue;
            int pool = seedFrontage(w, open, r, AssetIntegrationDisciplineAuthority.FACTION_MOTOR_POOL_FRONTAGE,
                    faction.label + " motor pool", "service=faction-motor-pool;access=faction-controlled", faction);
            result.motorPools += pool;
            result.factionVehicles += seedVehicle(w, open, r, faction, "faction-motor-pool", "faction", false);
            result.personalVehicles += seedVehicle(w, open, r, faction, "personal-transport", "private", true);
        }
        result.publicVehicles += seedVehicle(w, open, r, Faction.CIVIC_LEDGER_OFFICE, "public-transit", "public", false);
        return result;
    }

    static boolean isCommerceType(String type) {
        String t = AssetIntegrationDisciplineAuthority.canonicalType(type);
        return AssetIntegrationDisciplineAuthority.VEHICLE_DEALERSHIP_FRONTAGE.equals(t) ||
                AssetIntegrationDisciplineAuthority.VEHICLE_PARTS_STORE_FRONTAGE.equals(t) ||
                AssetIntegrationDisciplineAuthority.VEHICLE_SERVICE_GARAGE_FRONTAGE.equals(t) ||
                AssetIntegrationDisciplineAuthority.PUBLIC_TRANSIT_STOP_FRONTAGE.equals(t) ||
                AssetIntegrationDisciplineAuthority.FACTION_MOTOR_POOL_FRONTAGE.equals(t);
    }

    static String interaction(GamePanel g, MapObjectState m) {
        String type = AssetIntegrationDisciplineAuthority.canonicalType(m.type);
        if (AssetIntegrationDisciplineAuthority.VEHICLE_DEALERSHIP_FRONTAGE.equals(type)) return buyVehicleTitle(g, m);
        if (AssetIntegrationDisciplineAuthority.VEHICLE_PARTS_STORE_FRONTAGE.equals(type)) return buyPart(g, m);
        if (AssetIntegrationDisciplineAuthority.VEHICLE_SERVICE_GARAGE_FRONTAGE.equals(type)) return serviceVehicle(g, m);
        if (AssetIntegrationDisciplineAuthority.PUBLIC_TRANSIT_STOP_FRONTAGE.equals(type)) {
            return "PUBLIC TRANSIT: local taxi and mass-transit routes are posted here. Fare execution and boarding remain reserved for the transit runtime.";
        }
        return "FACTION MOTOR POOL: " + MapObjectState.stockValue(m.stockState, "ownerFaction") +
                " vehicles are assigned here. Vehicle identity, component condition, ownership history, repair, seizure, and salvage records are live; operation remains access-controlled.";
    }

    static List<Faction> factionsForZone(ZoneType zone) {
        LinkedHashSet<Faction> out = new LinkedHashSet<>();
        if (zone == ZoneType.GANGER_TURF) Collections.addAll(out, Faction.GANGER_IRON_RATS, Faction.GANGER_BLACK_SUMP, Faction.GANGER_CANDLE_JACKS, Faction.GANGER_RED_GRIN, Faction.GANGER_CHAIN_SAINTS, Faction.GANGER_ASH_MARKET, Faction.GANGER_WIRE_WOLVES, Faction.GANGER_DROWNED_9TH);
        else if (zone == ZoneType.NOBLE_SERVICE_SPINE || zone == ZoneType.SECTOR_GOVERNORS_MANSION) Collections.addAll(out, Faction.NOBLE_HOUSE_VARN, Faction.NOBLE_HOUSE_KASTOR, Faction.NOBLE_HOUSE_MORVAIN, Faction.NOBLE_HOUSE_CYRA, Faction.NOBLE_HOUSE_DRAKE, Faction.NOBLE_HOUSE_TOLL, Faction.NOBLE_HOUSE_OSSUARY);
        else if (zone == ZoneType.MECHANICUS_FORGE_CLOISTER || zone == ZoneType.MECHANICUS_RELIC_DUCT) Collections.addAll(out, Faction.MECHANICUS_CLOISTER_RED, Faction.MECHANICUS_CLOISTER_RUST, Faction.MECHANICUS_CLOISTER_VOID, Faction.MECHANIST_COLLEGIA);
        else if (zone == ZoneType.HAB_STACK) Collections.addAll(out, Faction.HIVER_BLOCK_AUREL, Faction.HIVER_BLOCK_MARROW, Faction.HIVER_BLOCK_SUMPLEDGER);
        else if (zone == ZoneType.ARBITES_PRECINCT_EDGE) out.add(Faction.CIVIC_WARDENS);
        else if (zone == ZoneType.IMPERIAL_GUARD_BILLET) out.add(Faction.IMPERIAL_GUARD);
        else if (zone == ZoneType.MUTANT_SEWER_CAMP || zone == ZoneType.MUTANT_WARRENS) out.add(Faction.MUTANT);
        else if (zone == ZoneType.CULTIST_SEWER_CAMP) out.add(Faction.CULTIST);
        else if (zone == ZoneType.IMPERIAL_NEWS_NETWORK) out.add(Faction.INN);
        else Collections.addAll(out, Faction.CIVIC_LEDGER_OFFICE, Faction.CIVIC_WARDENS, Faction.HIVER, Faction.SCAVENGER);
        return List.copyOf(out);
    }

    private static int seedFrontage(World w, ArrayList<Point> candidates, Random r, String type, String label, String stock, Faction faction) {
        for (Point p : candidates) {
            if (!available(w, p, 3)) continue;
            MapObjectState m = new MapObjectState();
            m.x = p.x; m.y = p.y; m.glyph = 'T'; m.type = type;
            m.label = label + " / " + (w.zoneType == null ? "Unknown zone" : w.zoneType.label);
            m.stockState = stock + ";layer=road-frontage;ownerFaction=" + faction.name() + ";under=" + (int) w.tiles[p.x][p.y];
            m.id = "VEHICLE-FRONTAGE-" + Math.abs(Objects.hash(type, w.locationKey(), p.x, p.y));
            w.mapObjects.add(m);
            return 1;
        }
        return 0;
    }

    private static int seedVehicle(World w, ArrayList<Point> candidates, Random r, Faction faction, String role, String ownership, boolean forSale) {
        for (Point p : candidates) {
            if (!available(w, p, 2)) continue;
            RoadTransitFixtureAuthority.VehicleProfile profile = RoadTransitFixtureAuthority.chooseVehicleProfile(r, w.zoneType);
            MapObjectState m = RoadTransitFixtureAuthority.newVehicleFixture(w, p.x, p.y, profile, r, "FACTION-VEHICLE");
            m.label = faction.label + " " + profile.label.toLowerCase(Locale.ROOT) + " / " + (w.zoneType == null ? "Unknown zone" : w.zoneType.label);
            m.stockState = MapObjectState.setStockFlag(m.stockState, "ownerFaction", faction.name());
            m.stockState = MapObjectState.setStockFlag(m.stockState, "ownership", ownership);
            m.stockState = MapObjectState.setStockFlag(m.stockState, "vehicleRole", role);
            m.stockState = MapObjectState.setStockFlag(m.stockState, "forSale", Boolean.toString(forSale));
            m.stockState = MapObjectState.setStockFlag(m.stockState, "condition", "serviceable");
            VehicleRuntimeAuthority.initialize(w, m, faction, ownership, role, forSale, r);
            w.mapObjects.add(m);
            return 1;
        }
        return 0;
    }

    private static boolean available(World w, Point p, int radius) {
        return p != null && w.inBounds(p.x, p.y) && w.mapObjectAt(p.x, p.y) == null &&
                !w.isDoorAccessReservedForObject(p.x, p.y) && !RoadFrontageFixtureAuthority.hasNearbyMapObject(w, p.x, p.y, radius);
    }

    private static Faction primaryFaction(List<Faction> factions) { return factions.isEmpty() ? Faction.CIVIC_LEDGER_OFFICE : factions.get(0); }

    private static String buyVehicleTitle(GamePanel g, MapObjectState m) {
        return VehicleRuntimeAuthority.purchaseNearestForSale(g, m).message();
    }

    private static String buyPart(GamePanel g, MapObjectState m) {
        String part = PARTS[Math.floorMod(m.vendCount, PARTS.length)];
        int price = Math.max(2, ItemCatalog.priceFor(part));
        if (!g.spendImperialScript(price)) return "PARTS STORE: " + part + " costs " + price + " script; you do not have enough carried script.";
        g.addInventoryItem(part, null);
        return "PARTS PURCHASE: paid " + price + " script for " + part + ".";
    }

    private static String serviceVehicle(GamePanel g, MapObjectState m) {
        return VehicleMaintenanceAuthority.serviceNearestPlayerVehicle(g, m).message();
    }
}
