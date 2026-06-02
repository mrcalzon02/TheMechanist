package mechanist;

import java.util.*;

/**
 * Runtime authority for the bar / market / social fixture bucket.
 *
 * Owns canonical tavern, counter, seating, shelf, keg, and market-counter
 * variants, semantic art routing, compact inspection lines, placement choices,
 * and service-preview metadata without opening a full social economy.
 */
final class BarMarketSocialFixtureAuthority {
    static final String VERSION = "0.9.10af";

    static final String[] CANONICAL_TYPES = {
            AssetIntegrationDisciplineAuthority.FACTION_BAR_INTERIOR_FIXTURE,
            AssetIntegrationDisciplineAuthority.BAR_COUNTER_LONG_FIXTURE,
            AssetIntegrationDisciplineAuthority.BAR_BOOTH_FIXTURE,
            AssetIntegrationDisciplineAuthority.BAR_STOOL_FIXTURE,
            AssetIntegrationDisciplineAuthority.BAR_BOTTLE_SHELF_FIXTURE,
            AssetIntegrationDisciplineAuthority.SERVICE_KEG_FIXTURE,
            AssetIntegrationDisciplineAuthority.MARKET_COUNTER_FIXTURE
    };

    enum Variant {
        FACTION_BAR(AssetIntegrationDisciplineAuthority.FACTION_BAR_INTERIOR_FIXTURE, 'T', "faction-bar-interior", "feature_faction_rep_bar_counter",
                "faction bar interior fixture", "counter stains, representative sightline, patron marks, cheap vox, and a private angle on public loyalty",
                "service-preview: faction representative contact, rumor exchange, recovery anchor, and reputation handoff"),
        BAR_COUNTER(AssetIntegrationDisciplineAuthority.BAR_COUNTER_LONG_FIXTURE, 'T', "bar-counter-long", "feature_bar_counter_long",
                "long bar counter", "scored service top, chipped glasses, ration scales, spill trough, and a ledger nobody wants audited",
                "service-preview: drinks, paid rumors, black-market introductions, and social camouflage"),
        BAR_BOOTH(AssetIntegrationDisciplineAuthority.BAR_BOOTH_FIXTURE, 'b', "bar-booth", "feature_bar_booth",
                "bar booth", "tight seating, scratched table, privacy shadows, and upholstery trained by generations of bad decisions",
                "service-preview: quiet meetings, rumor checks, faction approaches, and recovery pauses"),
        BAR_STOOL(AssetIntegrationDisciplineAuthority.BAR_STOOL_FIXTURE, 'b', "bar-stool", "feature_bar_stool",
                "bar stool cluster", "bolted stools, boot scuffs, elbow space, and just enough instability to count as ambiance",
                "service-preview: patron density, listening posts, fatigue relief, and low-risk social hooks"),
        BOTTLE_SHELF(AssetIntegrationDisciplineAuthority.BAR_BOTTLE_SHELF_FIXTURE, 'N', "bar-bottle-shelf", "feature_bar_bottle_shelf",
                "bottle shelf", "labeled bottles, dust rings, locked measures, colored glass, and a shelf that knows too much",
                "service-preview: stock identity, amasec quality cues, contraband suspicion, and service inventory handoff"),
        SERVICE_KEG(AssetIntegrationDisciplineAuthority.SERVICE_KEG_FIXTURE, 'N', "service-keg", "feature_service_keg",
                "service keg", "tap lines, pressure collar, drip tray, slosh marks, and the industrial humility of public refreshment",
                "service-preview: drink stock, fatigue relief, spoilage checks, and bar-supply handoff"),
        MARKET_COUNTER(AssetIntegrationDisciplineAuthority.MARKET_COUNTER_FIXTURE, 'B', "market-counter", "build_licensed_shop_counter",
                "market counter", "counter plank, lockbox, posted prices, trade tokens, and merchandise arranged to survive negotiation",
                "service-preview: barter, licensed shop frontage, informal market stock, and commerce handoff");

        final String type;
        final char glyph;
        final String token;
        final String artKey;
        final String label;
        final String inspection;
        final String servicePreview;

        Variant(String type, char glyph, String token, String artKey, String label, String inspection, String servicePreview) {
            this.type = type;
            this.glyph = glyph;
            this.token = token;
            this.artKey = artKey;
            this.label = label;
            this.inspection = inspection;
            this.servicePreview = servicePreview;
        }
    }

    private static final LinkedHashMap<String, Variant> BY_TYPE = new LinkedHashMap<>();
    static { for (Variant v : Variant.values()) BY_TYPE.put(v.type, v); }

    private BarMarketSocialFixtureAuthority() {}

    static boolean isFamilyType(String type) { return variantFor(type) != null; }

    static Variant variantFor(String type) {
        String c = AssetIntegrationDisciplineAuthority.canonicalType(type);
        return c == null ? null : BY_TYPE.get(c);
    }

    static String artKeyForType(String type) {
        Variant v = variantFor(type);
        return v == null ? null : v.artKey;
    }

    static char glyphForType(String type) {
        Variant v = variantFor(type);
        return v == null ? 'T' : v.glyph;
    }

    static String chooseRoomFixtureType(World w, RoomProfile rp, int roomId, Random r) {
        if (r == null) r = new Random(0);
        ZoneType z = w == null ? null : w.zoneType;
        String text = roomText(rp).toLowerCase(Locale.ROOT);
        if (contains(text, "market", "storefront", "shop", "vendor", "barter", "trade", "stall", "counter row", "storehouse")) {
            int roll = r.nextInt(100);
            if (roll < 58) return AssetIntegrationDisciplineAuthority.MARKET_COUNTER_FIXTURE;
            if (roll < 76) return AssetIntegrationDisciplineAuthority.BAR_COUNTER_LONG_FIXTURE;
            if (roll < 88) return AssetIntegrationDisciplineAuthority.BAR_BOTTLE_SHELF_FIXTURE;
            return AssetIntegrationDisciplineAuthority.BAR_BOOTH_FIXTURE;
        }
        if (contains(text, "bar", "tavern", "amasec", "drinking", "patron", "representative", "recovery anchor", "rumor")) {
            int roll = r.nextInt(100);
            if (roll < 32) return AssetIntegrationDisciplineAuthority.FACTION_BAR_INTERIOR_FIXTURE;
            if (roll < 56) return AssetIntegrationDisciplineAuthority.BAR_COUNTER_LONG_FIXTURE;
            if (roll < 72) return AssetIntegrationDisciplineAuthority.SERVICE_KEG_FIXTURE;
            if (roll < 86) return AssetIntegrationDisciplineAuthority.BAR_BOTTLE_SHELF_FIXTURE;
            return AssetIntegrationDisciplineAuthority.BAR_BOOTH_FIXTURE;
        }
        if (contains(text, "canteen", "mess", "cafeteria", "food court", "dining", "galley", "service dining", "ration counter")) {
            int roll = r.nextInt(100);
            if (roll < 42) return AssetIntegrationDisciplineAuthority.BAR_COUNTER_LONG_FIXTURE;
            if (roll < 64) return AssetIntegrationDisciplineAuthority.BAR_STOOL_FIXTURE;
            if (roll < 82) return AssetIntegrationDisciplineAuthority.SERVICE_KEG_FIXTURE;
            return AssetIntegrationDisciplineAuthority.MARKET_COUNTER_FIXTURE;
        }
        if (z == ZoneType.SUMP_MARKET) return r.nextInt(100) < 65 ? AssetIntegrationDisciplineAuthority.MARKET_COUNTER_FIXTURE : AssetIntegrationDisciplineAuthority.BAR_COUNTER_LONG_FIXTURE;
        if (z == ZoneType.NEUTRAL_CIVILIAN_FLOOR || z == ZoneType.HAB_STACK) return r.nextInt(100) < 55 ? AssetIntegrationDisciplineAuthority.BAR_BOOTH_FIXTURE : AssetIntegrationDisciplineAuthority.BAR_STOOL_FIXTURE;
        if (z == ZoneType.NOBLE_SERVICE_SPINE || z == ZoneType.SECTOR_GOVERNORS_MANSION) return r.nextInt(100) < 50 ? AssetIntegrationDisciplineAuthority.BAR_BOTTLE_SHELF_FIXTURE : AssetIntegrationDisciplineAuthority.BAR_BOOTH_FIXTURE;
        return AssetIntegrationDisciplineAuthority.FACTION_BAR_INTERIOR_FIXTURE;
    }

    static String roomLabel(String type, RoomProfile rp, ZoneType z) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "unclassified room" : rp.name;
        String zone = z == null ? "Unknown zone" : z.label;
        return (v == null ? "Bar / market fixture" : title(v.label)) + " / " + room + " / " + zone;
    }

    static String roomStock(String type, RoomProfile rp, World w, int roomId, char under) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "room" : safe(rp.name);
        String semantic = AssetIntegrationDisciplineAuthority.semanticKeyForType(type);
        String token = v == null ? "bar-market-social-fixture" : v.token;
        return "room-fixture;bar-market-social-bucket;variant=" + token + ";roomId=" + roomId + ";room=" + room +
                ";handoff=service-preview;semantic=" + safe(semantic) + ";under=" + (int)under;
    }

    static String frontageLabel(ZoneType z, Random r) {
        if (r == null) r = new Random(0);
        String zone = z == null ? "Unknown zone" : z.label;
        return pick(r, "Faction bar frontage", "Roadside recovery counter", "Licensed-ish social frontage", "Patron notice and bar marker") + " / " + zone;
    }

    static String frontageStock(char underlying) {
        return "road-frontage;bar-market-social;service=faction-rep-rumor-recovery;semantic=feature_faction_rep_bar_counter;under=" + (int)underlying;
    }

    static String frontageInspectionLine(Faction f) {
        Faction faction = f == null ? Faction.NONE : f;
        return "FACTION BAR FRONTAGE: recognizable recovery signage for " + faction.label +
                ". Service preview: representative contact, rumors, fatigue relief, reputation repair, patron density, and market introductions.";
    }

    static String inspectionLine(String type, Faction f) {
        Variant v = variantFor(type);
        Faction faction = f == null ? Faction.NONE : f;
        if (v == null) return "BAR / MARKET FIXTURE: social surface, trade counter, patron noise, and service-preview handoff for " + faction.label + ".";
        return "BAR / MARKET FIXTURE: " + v.inspection + ". " + v.servicePreview + ". Faction context: " + faction.label + ".";
    }

    static String interactionVerb(String type) {
        Variant v = variantFor(type);
        if (v == null) return "inspects a bar or market fixture.";
        switch (v) {
            case FACTION_BAR: return "inspects a faction bar fixture.";
            case BAR_COUNTER: return "inspects a long bar counter.";
            case BAR_BOOTH: return "inspects a bar booth.";
            case BAR_STOOL: return "inspects a bar stool cluster.";
            case BOTTLE_SHELF: return "inspects a bottle shelf.";
            case SERVICE_KEG: return "inspects a service keg.";
            case MARKET_COUNTER: return "inspects a market counter.";
            default: return "inspects a bar or market fixture.";
        }
    }

    static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "bar / market fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "bar / market fixture" : s;
    }

    static String auditSummary() {
        return "barMarketSocialFixture version=" + VERSION + " promotedTypes=" + CANONICAL_TYPES.length +
                " rule=canonical bar/market/social variants; service-preview handoff only";
    }

    private static String roomText(RoomProfile rp) {
        return ((rp == null || rp.name == null) ? "" : rp.name) + " " +
                ((rp == null || rp.descriptor == null) ? "" : rp.descriptor) + " " +
                ((rp == null || rp.featureText == null) ? "" : rp.featureText);
    }

    private static boolean contains(String s, String... parts) {
        if (s == null) return false;
        for (String p : parts) if (p != null && !p.isBlank() && s.contains(p)) return true;
        return false;
    }

    private static String pick(Random r, String... values) { return values[Math.floorMod(r.nextInt(), values.length)]; }
    private static String title(String s) { return s == null || s.isBlank() ? "Bar / market fixture" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static String safe(String s) { return s == null ? "" : s.replace(';', ','); }
}
