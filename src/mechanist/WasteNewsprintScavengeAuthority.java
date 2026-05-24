package mechanist;

import java.util.*;

/**
 * Runtime authority for the waste/newsprint/scavenge asset family.
 *
 * This class owns canonical scavenge-container handles, bounded loot selection,
 * and readable current-world feedback for promoted public refuse/newsprint
 * fixtures. It deliberately does not create a broader economy or logistics loop.
 */
final class WasteNewsprintScavengeAuthority {
    static final String VERSION = "0.9.10af";

    static final String[] CANONICAL_TYPES = {
            AssetIntegrationDisciplineAuthority.PUBLIC_TRASH_BIN,
            AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE,
            AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER,
            AssetIntegrationDisciplineAuthority.INN_NEWSPAPER_DISPENSER,
            AssetIntegrationDisciplineAuthority.DISCARDED_NEWSPRINT_SOURCE
    };

    private WasteNewsprintScavengeAuthority() {}

    static boolean isFamilyType(String type) {
        String c = AssetIntegrationDisciplineAuthority.canonicalType(type);
        for (String t : CANONICAL_TYPES) if (t.equals(c)) return true;
        return false;
    }

    static boolean isSearchableContainer(String type) {
        String c = AssetIntegrationDisciplineAuthority.canonicalType(type);
        return AssetIntegrationDisciplineAuthority.PUBLIC_TRASH_BIN.equals(c)
                || AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE.equals(c)
                || AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER.equals(c);
    }

    static String chooseFrontageContainerType(ZoneType z, Random r) {
        if (r == null) r = new Random(0);
        int roll = r.nextInt(100);
        if (z == ZoneType.TRASH_WARREN || z == ZoneType.SUMP_MARKET || z == ZoneType.SEWER_CONDUIT) {
            if (roll < 48) return AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER;
            if (roll < 82) return AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE;
            return AssetIntegrationDisciplineAuthority.PUBLIC_TRASH_BIN;
        }
        if (z == ZoneType.NOBLE_SERVICE_SPINE || z == ZoneType.SECTOR_GOVERNORS_MANSION) {
            if (roll < 62) return AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE;
            if (roll < 82) return AssetIntegrationDisciplineAuthority.PUBLIC_TRASH_BIN;
            return AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER;
        }
        if (roll < 45) return AssetIntegrationDisciplineAuthority.PUBLIC_TRASH_BIN;
        if (roll < 78) return AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE;
        return AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER;
    }

    static String frontageLabel(String type, ZoneType z, Random r) {
        if (r == null) r = new Random(0);
        String c = AssetIntegrationDisciplineAuthority.canonicalType(type);
        if (AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER.equals(c)) {
            String[] labels = {"Small bin cluster", "Overflow refuse cluster", "Stacked public bins", "Scavenger-picked bin cluster"};
            return labels[Math.floorMod(r.nextInt(), labels.length)] + " / newsprint and light salvage source";
        }
        if (AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE.equals(c)) {
            String[] labels = {"Municipal waste receptacle", "Roadside waste receptacle", "Stamped civic refuse can", "Public reclamation receptacle"};
            return labels[Math.floorMod(r.nextInt(), labels.length)] + " / bounded refuse search";
        }
        String[] labels = {"Public trash bin", "Overflowing trash bin", "Roadside refuse bin", "Public litter bin"};
        return labels[Math.floorMod(r.nextInt(), labels.length)] + " / INN newspaper recovery hook";
    }

    static String frontageStock(String type, char underlying) {
        String c = AssetIntegrationDisciplineAuthority.canonicalType(type);
        String semantic = AssetIntegrationDisciplineAuthority.semanticKeyForType(c);
        String yield = AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER.equals(c) ? "paper-scrap-refuse" :
                (AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE.equals(c) ? "refuse-paper-light-salvage" : "old-newsprint-small-refuse");
        return "road-frontage;waste-newsprint-scavenge;semantic=" + safe(semantic) +
                ";searchable-container;yield=" + yield + ";under=" + (int)underlying;
    }

    static int searchChance(GamePanel g, MapObjectState m) {
        int chance = 52;
        ZoneType z = g == null || g.world == null ? null : g.world.zoneType;
        String c = AssetIntegrationDisciplineAuthority.canonicalType(m == null ? null : m.type);
        if (AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER.equals(c)) chance += 8;
        if (AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE.equals(c)) chance += 3;
        if (z == ZoneType.TRASH_WARREN || z == ZoneType.SUMP_MARKET) chance += 16;
        if (z == ZoneType.SEWER_CONDUIT || z == ZoneType.MUTANT_SEWER_CAMP) chance += 10;
        if (z == ZoneType.NOBLE_SERVICE_SPINE || z == ZoneType.SECTOR_GOVERNORS_MANSION) chance -= 10;
        return Math.max(18, Math.min(86, chance));
    }

    static String chooseLoot(GamePanel g, MapObjectState m, Random r) {
        if (r == null) r = new Random(0);
        ZoneType z = g == null || g.world == null ? null : g.world.zoneType;
        String c = AssetIntegrationDisciplineAuthority.canonicalType(m == null ? null : m.type);
        String[] common = {"Used food tin", "Ration wrapper", "Old INN newspaper", "Yesterday's INN newspaper", "Useless paper mush", "Stub light", "Glow stick", "Wire bundle"};
        String[] receptacle = {"Used food tin", "Ration wrapper", "Old INN newspaper", "Water purification tab", "Dirty canteen", "Stub light", "Vended scrap"};
        String[] cluster = {"Scrap plate", "Mechanical detritus", "Wire bundle", "Old INN newspaper", "Useless paper mush", "Ferric scrap", "Cloth scrap"};
        String[] sump = {"Scrap plate", "Mechanical detritus", "Water purification tab", "Dirty canteen", "Sump fungus loaf", "Old INN newspaper"};
        String[] noble = {"Perfumed cloth", "Old INN newspaper", "Trade chit", "Clean water", "Ration wrapper"};
        String[] arr = common;
        if (AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE.equals(c)) arr = receptacle;
        if (AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER.equals(c)) arr = cluster;
        if (z == ZoneType.TRASH_WARREN || z == ZoneType.SUMP_MARKET || z == ZoneType.SEWER_CONDUIT) arr = sump;
        if (z == ZoneType.NOBLE_SERVICE_SPINE || z == ZoneType.SECTOR_GOVERNORS_MANSION) arr = noble;
        for (int i=0; i<10; i++) {
            String v = arr[r.nextInt(arr.length)];
            if (ItemCatalog.get(v) != null) return v;
        }
        return "Old INN newspaper";
    }

    static String searchVerb(String type) {
        String c = AssetIntegrationDisciplineAuthority.canonicalType(type);
        if (AssetIntegrationDisciplineAuthority.PUBLIC_SMALL_BIN_CLUSTER.equals(c)) return "searches a public bin cluster.";
        if (AssetIntegrationDisciplineAuthority.PUBLIC_WASTE_RECEPTACLE.equals(c)) return "searches a public waste receptacle.";
        return "searches a public trash bin.";
    }

    static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "public refuse fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "public refuse fixture" : s;
    }

    static String auditSummary() {
        return "wasteNewsprintScavenge version=" + VERSION + " promotedTypes=" + CANONICAL_TYPES.length +
                " rule=bounded searchable public refuse/newsprint fixtures; no broad economy expansion";
    }

    static String safe(String s) { return s == null ? "" : s.replace(';', ','); }
}
