package mechanist;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Canonical stock-access authority for faction-bound commerce.
 *
 * Traders and vending machines should not invent arbitrary shelves at point of
 * sale. They should draw from the faction/zone inventory they can plausibly
 * access, with deterministic local fallbacks only when a faction has no known
 * stock family yet.
 */
final class FactionInventoryStockAuthority {
    private FactionInventoryStockAuthority() {}

    static Faction factionForTrader(NpcEntity npc, ZoneType zone) {
        if (npc != null && npc.faction != null) return normalizeFaction(npc.faction);
        return factionForZone(zone);
    }

    static Faction factionForZone(ZoneType zone) {
        if (zone == null) return Faction.SCAVENGER;
        switch (zone) {
            case IMPERIAL_GUARD_BILLET: return Faction.IMPERIAL_GUARD;
            case ARBITES_PRECINCT_EDGE: return Faction.CIVIC_WARDENS;
            case MECHANICUS_FORGE_CLOISTER:
            case MECHANICUS_RELIC_DUCT: return Faction.MECHANIST_COLLEGIA;
            case NOBLE_SERVICE_SPINE:
            case SECTOR_GOVERNORS_MANSION: return Faction.NOBLE;
            case GANGER_TURF: return Faction.BANDIT;
            case CULTIST_SEWER_CAMP: return Faction.CULTIST;
            case MUTANT_WARRENS:
            case MUTANT_SEWER_CAMP: return Faction.MUTANT;
            case ADMINISTRATUM_ARCHIVE: return Faction.CIVIC_LEDGER_OFFICE;
            case IMPERIAL_NEWS_NETWORK: return Faction.INN;
            case SUMP_MARKET:
            case NEUTRAL_RAIL_DEPOT:
            case TRAIN_SERVICE_YARD: return Faction.SCAVENGER;
            default: return Faction.SCAVENGER;
        }
    }

    static Faction normalizeFaction(Faction faction) {
        if (faction == null) return Faction.SCAVENGER;
        if (faction == Faction.ARBITES) return Faction.CIVIC_WARDENS;
        if (faction == Faction.ADMINISTRATUM) return Faction.CIVIC_LEDGER_OFFICE;
        if (faction == Faction.MECHANICUS) return Faction.MECHANIST_COLLEGIA;
        if (faction.name().startsWith("GANGER")) return Faction.BANDIT;
        if (faction.name().startsWith("NOBLE_HOUSE")) return Faction.NOBLE;
        if (faction.name().startsWith("MECHANICUS_CLOISTER")) return Faction.MECHANIST_COLLEGIA;
        if (faction.name().startsWith("HIVER_BLOCK")) return Faction.HIVER;
        return faction;
    }

    static ArrayList<String> accessibleStock(Faction faction, ZoneType zone, Random rng) {
        Random r = rng == null ? new Random(0) : rng;
        Faction f = normalizeFaction(faction == null ? factionForZone(zone) : faction);
        LinkedHashSet<String> out = new LinkedHashSet<>();
        addCoreSurvival(out);
        switch (f) {
            case IMPERIAL_GUARD:
                add(out, "Guard field ration tin", "Plain ration pack", "Guard entrenching tool", "Guard flak vest", "Las charge pack", "Stub cartridge box", "Field dressings", "Medi-Stimm");
                chance(out, r, 35, "Frag grenade");
                chance(out, r, 25, "Light Rifle");
                break;
            case CIVIC_WARDENS:
                add(out, "Permit form", "Citation slate", "Civic Wardens restraint kit", "Civic Wardens riot visor", "Civic Wardens suppression shells", "Plain ration pack", "Water ration");
                chance(out, r, 25, "Data spike");
                chance(out, r, 18, "Webber");
                break;
            case MECHANIST_COLLEGIA:
                add(out, "Machine part", "Wire bundle", "Sacred wire bundle", "Mechanist Collegia tool roll", "Mechanist Collegia calibration probe", "Mechanist Collegia nutrient ampoule", "Machine oil vial");
                chance(out, r, 40, "Data spike");
                chance(out, r, 28, "Mechanist Collegia arc prod");
                chance(out, r, 12, "Arc Rifle");
                break;
            case NOBLE:
                add(out, "Noble preserved delicacy", "Noble signet wax kit", "Noble fur-lined coat", "Spire perfumery glassware", "Pearl Obscura", "Dueling pistol cartridge box");
                chance(out, r, 18, "Needle Pistol");
                break;
            case BANDIT:
                add(out, "Lockpicks", "Pipe shotgun", "Shot shell handful", "Damaged ganger coat", "Ganger chain cleaver", "Street Stimm", "Market vendor sash");
                chance(out, r, 22, "Tripwire mine");
                break;
            case CULTIST:
            case HERETIC:
                add(out, "Cult offering wafer", "Cult ritual blade", "Heretic nail flail", "Cult hooded wrap", "Witchsalt", "Ritual censer kiln");
                chance(out, r, 16, "Satchel charge");
                break;
            case MUTANT:
                add(out, "Sump fungus loaf", "Filter mask", "Mutant scrap axe", "Trash-hook spear", "Dirty canteen", "Sumpkalm");
                break;
            case CIVIC_LEDGER_OFFICE:
                add(out, "Permit form", "Civic Ledger Office stamp matrix", "Primer slate", "Data spike", "Trade chit", "Water ration");
                break;
            case INN:
                add(out, "Fresh INN newspaper", "Old INN newspaper", "Primer slate", "Recaf", "Lho-Sticks", "Water bottle");
                break;
            case HIVER:
                add(out, "Cheap lunch tin", "Water bottle", "Plain civilian coat", "Laundry token bundle", "Padded coat", "Stub light");
                break;
            case SCAVENGER:
            case NONE:
            default:
                add(out, "Emergency rations", "Water bottle", "Construction supplies", "Tool bundle", "Market scale set", "Rail cargo stencil kit", "Trade chit", "Bandage roll");
                chance(out, r, 20, "Data spike");
                chance(out, r, 25, "Zip pistol");
                break;
        }
        addZoneOverlay(out, zone, r);
        return catalogOnly(out);
    }

    static boolean canAccessItem(Faction faction, ZoneType zone, String itemName) {
        if (itemName == null || itemName.isBlank()) return false;
        String base = ItemQuality.stripQuality(itemName);
        for (String item : accessibleStock(faction, zone, new Random(stableSeed(faction, zone)))) {
            if (item.equalsIgnoreCase(base) || item.equalsIgnoreCase(itemName)) return true;
        }
        return false;
    }

    static int limitedStockCount(Faction faction, ZoneType zone, String itemName, Random rng) {
        Random r = rng == null ? new Random(stableSeed(faction, zone) ^ (itemName == null ? 0 : itemName.hashCode())) : rng;
        int base = 1 + r.nextInt(3);
        Faction f = normalizeFaction(faction == null ? factionForZone(zone) : faction);
        if (f == Faction.IMPERIAL_GUARD || f == Faction.MECHANIST_COLLEGIA || f == Faction.CIVIC_WARDENS) base += 1;
        if (zone == ZoneType.SUMP_MARKET || zone == ZoneType.NEUTRAL_RAIL_DEPOT || zone == ZoneType.TRAIN_SERVICE_YARD) base += 1;
        if (itemName != null && (itemName.toLowerCase(Locale.ROOT).contains("grenade") || itemName.toLowerCase(Locale.ROOT).contains("rifle") || itemName.toLowerCase(Locale.ROOT).contains("pistol"))) base = Math.min(base, 2);
        return Math.max(1, base);
    }

    private static void addCoreSurvival(LinkedHashSet<String> out) {
        add(out, "Emergency rations", "Water bottle");
    }

    private static void addZoneOverlay(LinkedHashSet<String> out, ZoneType zone, Random r) {
        if (zone == null) return;
        switch (zone) {
            case SUMP_MARKET:
                add(out, "Water guild token", "Market scale set", "Lho-Sticks", "Recaf");
                break;
            case NEUTRAL_RAIL_DEPOT:
            case TRAIN_SERVICE_YARD:
                add(out, "Rail cargo stencil kit", "Cargo hook", "Machine part", "Work gloves");
                break;
            case SEWER_CONDUIT:
                add(out, "Filter canteen", "Water purification tab", "Dirty canteen", "Sump fungus loaf");
                break;
            case TRASH_WARREN:
                add(out, "Scrap recycler gasket set", "Machine part", "Wire bundle", "Used food tin");
                break;
            default:
                break;
        }
    }

    private static ArrayList<String> catalogOnly(LinkedHashSet<String> in) {
        ArrayList<String> out = new ArrayList<>();
        for (String item : in) if (item != null && ItemCatalog.get(item) != null) out.add(item);
        return out;
    }

    private static void add(LinkedHashSet<String> out, String... names) {
        for (String name : names) if (name != null && !name.isBlank()) out.add(name);
    }

    private static void chance(LinkedHashSet<String> out, Random r, int pct, String name) {
        if (r != null && r.nextInt(100) < pct) add(out, name);
    }

    static long stableSeed(Faction faction, ZoneType zone) {
        return 31L * (faction == null ? 0 : faction.name().hashCode()) + 17L * (zone == null ? 0 : zone.name().hashCode());
    }
}
