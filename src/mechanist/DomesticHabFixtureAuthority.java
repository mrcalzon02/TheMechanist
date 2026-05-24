package mechanist;

import java.util.*;

/**
 * Runtime authority for the domestic hab fixture bucket.
 *
 * Owns canonical bed, water, cold-storage, kitchen, storage, and table variants
 * with passive inspection, storage/service metadata, and room-placement support
 * only. Housing ownership, tenancy, rent, family simulation, and sleep overhaul
 * remain outside this asset-promotion boundary.
 */
final class DomesticHabFixtureAuthority {
    static final String VERSION = "0.9.10ak";

    static final String[] CANONICAL_TYPES = {
            AssetIntegrationDisciplineAuthority.HAB_COT_FIXTURE,
            AssetIntegrationDisciplineAuthority.GUARD_COT_FIXTURE,
            AssetIntegrationDisciplineAuthority.WORN_BED_FIXTURE,
            AssetIntegrationDisciplineAuthority.NOBLE_BED_FIXTURE,
            AssetIntegrationDisciplineAuthority.BUNK_BED_FIXTURE,
            AssetIntegrationDisciplineAuthority.DOMESTIC_WATER_STORAGE_FIXTURE,
            AssetIntegrationDisciplineAuthority.DOMESTIC_REFRIGERATOR_FIXTURE,
            AssetIntegrationDisciplineAuthority.DOMESTIC_SINK_FIXTURE,
            AssetIntegrationDisciplineAuthority.DOMESTIC_STOVE_FIXTURE,
            AssetIntegrationDisciplineAuthority.DOMESTIC_PREP_COUNTER_FIXTURE,
            AssetIntegrationDisciplineAuthority.DOMESTIC_STORAGE_CABINET_FIXTURE,
            AssetIntegrationDisciplineAuthority.DOMESTIC_PLANK_TABLE_FIXTURE,
            AssetIntegrationDisciplineAuthority.DOMESTIC_ROUND_TABLE_FIXTURE,
            AssetIntegrationDisciplineAuthority.DOMESTIC_MESS_TABLE_FIXTURE,
            AssetIntegrationDisciplineAuthority.DOMESTIC_ORNATE_TABLE_FIXTURE
    };

    enum Variant {
        HAB_COT(AssetIntegrationDisciplineAuthority.HAB_COT_FIXTURE, 'c', "hab-cot", "feature_hab_cot_plain",
                "hab cot fixture", "plain cot, trunk, pipe frame, and a mattress that has heard too much coughing",
                "storage-preview: sleep surface, personal effects trunk, and hab-cell staging metadata"),
        GUARD_COT(AssetIntegrationDisciplineAuthority.GUARD_COT_FIXTURE, 'c', "guard-cot", "feature_hab_cot_guard",
                "Guard cot fixture", "olive-issue cot, stamped blanket, kit bracket, and barracks-grade denial of comfort",
                "storage-preview: billet sleep surface, uniform kit slot, and barracks staging metadata"),
        WORN_BED(AssetIntegrationDisciplineAuthority.WORN_BED_FIXTURE, 'c', "worn-bed", "feature_hab_cot_worn",
                "worn bed fixture", "frayed bedding, patched frame, hanging effects, and the civilian luxury of technically lying down",
                "storage-preview: underhive sleep surface, personal stash point, and dormitory staging metadata"),
        NOBLE_BED(AssetIntegrationDisciplineAuthority.NOBLE_BED_FIXTURE, 'c', "noble-bed", "feature_hab_bed_noble",
                "noble bed fixture", "gilded bed, side cabinet, fine coverlet, and a crest loud enough to disturb the poor from two districts away",
                "service-preview: noble rest surface, servant-access staging, and estate-room metadata"),
        BUNK_BED(AssetIntegrationDisciplineAuthority.BUNK_BED_FIXTURE, 'c', "bunk-bed", "feature_hab_bunk_bed",
                "bunk bed fixture", "stacked bunks, ladder, footlockers, and vertical poverty optimized for floor-plan efficiency",
                "storage-preview: dormitory sleep surface, shared locker staging, and capacity metadata"),
        WATER_STORAGE(AssetIntegrationDisciplineAuthority.DOMESTIC_WATER_STORAGE_FIXTURE, 'u', "water-storage", "feature_domestic_water_dispenser",
                "domestic water-storage fixture", "bottle, barrel, tap, filter stool, and enough plumbing to imply a previous leak",
                "storage-preview: potable water custody, wash access, and domestic utility metadata"),
        REFRIGERATOR(AssetIntegrationDisciplineAuthority.DOMESTIC_REFRIGERATOR_FIXTURE, 'u', "domestic-refrigerator", "feature_domestic_refrigerator_white",
                "domestic refrigerator fixture", "sealed cold box, worn latch, compressor panel, and a hum aspiring to become a scream",
                "storage-preview: household cold storage, ration custody, and kitchenette metadata"),
        SINK(AssetIntegrationDisciplineAuthority.DOMESTIC_SINK_FIXTURE, 'u', "sink-counter", "feature_domestic_sink_counter",
                "sink counter fixture", "metal basin, drain board, lower cabinet, and a water valve with institutional doubts",
                "service-preview: washing surface, water access, and domestic prep metadata"),
        STOVE(AssetIntegrationDisciplineAuthority.DOMESTIC_STOVE_FIXTURE, 't', "stove-counter", "feature_domestic_stove_counter",
                "stove counter fixture", "burner plate, battered pan, cabinet drawers, and heat marks from meals better left unnamed",
                "service-preview: cooking surface, domestic ration prep, and kitchen staging metadata"),
        PREP_COUNTER(AssetIntegrationDisciplineAuthority.DOMESTIC_PREP_COUNTER_FIXTURE, 't', "prep-counter", "feature_domestic_prep_counter",
                "prep counter fixture", "chopping board, tin, knife marks, tray space, and a lantern trying to look sanitary",
                "service-preview: food prep, small crafting surface, and domestic worktop metadata"),
        STORAGE_CABINET(AssetIntegrationDisciplineAuthority.DOMESTIC_STORAGE_CABINET_FIXTURE, 's', "storage-cabinet", "feature_domestic_cabinet_counter",
                "domestic storage cabinet fixture", "cabinet doors, crockery, worktop clutter, and the last organized square meter in the room",
                "storage-preview: personal/domestic goods custody, small-container metadata, and household staging"),
        PLANK_TABLE(AssetIntegrationDisciplineAuthority.DOMESTIC_PLANK_TABLE_FIXTURE, 't', "plank-table", "feature_domestic_plank_table",
                "plank table fixture", "rough planks, tin cup, scarred boards, and enough splinters to file a complaint nobody reads",
                "service-preview: common meal surface, card/game/social hook, and hab-room metadata"),
        ROUND_TABLE(AssetIntegrationDisciplineAuthority.DOMESTIC_ROUND_TABLE_FIXTURE, 't', "round-table", "feature_domestic_round_table",
                "round metal table fixture", "round metal tabletop, cup, small device, and seating radius calibrated for arguments",
                "service-preview: apartment meal surface, social hook, and shared-room metadata"),
        MESS_TABLE(AssetIntegrationDisciplineAuthority.DOMESTIC_MESS_TABLE_FIXTURE, 't', "mess-table", "feature_domestic_square_mess_table",
                "square mess table fixture", "square table, stools, candle, metal cup, and the hard geometry of communal meals",
                "service-preview: mess seating, ration handoff, and barracks/apartment metadata"),
        ORNATE_TABLE(AssetIntegrationDisciplineAuthority.DOMESTIC_ORNATE_TABLE_FIXTURE, 't', "ornate-table", "feature_domestic_ornate_dining_table",
                "ornate dining table fixture", "polished table, high-backed chairs, skull crest, and decorative proof that somebody else is hungry",
                "service-preview: noble dining surface, servant route staging, and estate service metadata");

        final String type;
        final char glyph;
        final String token;
        final String artKey;
        final String label;
        final String inspection;
        final String servicePreview;

        Variant(String type, char glyph, String token, String artKey, String label, String inspection, String servicePreview) {
            this.type = type; this.glyph = glyph; this.token = token; this.artKey = artKey; this.label = label;
            this.inspection = inspection; this.servicePreview = servicePreview;
        }
    }

    private static final LinkedHashMap<String, Variant> BY_TYPE = new LinkedHashMap<>();
    static { for (Variant v : Variant.values()) BY_TYPE.put(v.type, v); }

    private DomesticHabFixtureAuthority() {}

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
        return v == null ? 'c' : v.glyph;
    }

    static String chooseRoomFixtureType(World w, RoomProfile rp, int roomId, Random r) {
        if (r == null) r = new Random(0);
        ZoneType z = w == null ? null : w.zoneType;
        String text = roomText(rp).toLowerCase(Locale.ROOT);
        if (contains(text, "bunk", "dormitory", "dorm", "hostel", "barracks bunks", "sleeping bay")) return AssetIntegrationDisciplineAuthority.BUNK_BED_FIXTURE;
        if (contains(text, "guard cot", "billet", "muster room", "troop quarters")) return AssetIntegrationDisciplineAuthority.GUARD_COT_FIXTURE;
        if (contains(text, "noble bedroom", "master bedroom", "suite", "estate bedroom")) return AssetIntegrationDisciplineAuthority.NOBLE_BED_FIXTURE;
        if (contains(text, "sink", "wash", "bathroom", "washroom", "lavatory")) return AssetIntegrationDisciplineAuthority.DOMESTIC_SINK_FIXTURE;
        if (contains(text, "water", "cistern", "barrel", "pump", "tap")) return AssetIntegrationDisciplineAuthority.DOMESTIC_WATER_STORAGE_FIXTURE;
        if (contains(text, "fridge", "refrigerator", "cooler", "cold box")) return AssetIntegrationDisciplineAuthority.DOMESTIC_REFRIGERATOR_FIXTURE;
        if (contains(text, "stove", "cooker", "hot plate", "kitchenette")) return AssetIntegrationDisciplineAuthority.DOMESTIC_STOVE_FIXTURE;
        if (contains(text, "prep", "counter", "worktop")) return AssetIntegrationDisciplineAuthority.DOMESTIC_PREP_COUNTER_FIXTURE;
        if (contains(text, "cabinet", "dresser", "locker", "wardrobe", "storage")) return AssetIntegrationDisciplineAuthority.DOMESTIC_STORAGE_CABINET_FIXTURE;
        if (contains(text, "dining", "feast", "banquet")) return (z == ZoneType.SECTOR_GOVERNORS_MANSION || z == ZoneType.NOBLE_SERVICE_SPINE) ? AssetIntegrationDisciplineAuthority.DOMESTIC_ORNATE_TABLE_FIXTURE : AssetIntegrationDisciplineAuthority.DOMESTIC_MESS_TABLE_FIXTURE;
        if (contains(text, "table", "living room", "common room")) return r.nextBoolean() ? AssetIntegrationDisciplineAuthority.DOMESTIC_ROUND_TABLE_FIXTURE : AssetIntegrationDisciplineAuthority.DOMESTIC_PLANK_TABLE_FIXTURE;
        if (contains(text, "bed", "cot", "sleep", "hab", "apartment", "quarters", "cell", "room")) {
            if (z == ZoneType.SECTOR_GOVERNORS_MANSION || z == ZoneType.NOBLE_SERVICE_SPINE) return r.nextInt(100) < 42 ? AssetIntegrationDisciplineAuthority.NOBLE_BED_FIXTURE : AssetIntegrationDisciplineAuthority.DOMESTIC_ORNATE_TABLE_FIXTURE;
            if (z == ZoneType.IMPERIAL_GUARD_BILLET) return r.nextInt(100) < 56 ? AssetIntegrationDisciplineAuthority.GUARD_COT_FIXTURE : AssetIntegrationDisciplineAuthority.BUNK_BED_FIXTURE;
            if (z == ZoneType.SUMP_MARKET || z == ZoneType.TRASH_WARREN || z == ZoneType.MUTANT_WARRENS) return r.nextInt(100) < 62 ? AssetIntegrationDisciplineAuthority.WORN_BED_FIXTURE : AssetIntegrationDisciplineAuthority.DOMESTIC_PLANK_TABLE_FIXTURE;
            int roll = r.nextInt(100);
            if (roll < 30) return AssetIntegrationDisciplineAuthority.HAB_COT_FIXTURE;
            if (roll < 50) return AssetIntegrationDisciplineAuthority.WORN_BED_FIXTURE;
            if (roll < 66) return AssetIntegrationDisciplineAuthority.BUNK_BED_FIXTURE;
            if (roll < 80) return AssetIntegrationDisciplineAuthority.DOMESTIC_STORAGE_CABINET_FIXTURE;
            if (roll < 90) return AssetIntegrationDisciplineAuthority.DOMESTIC_WATER_STORAGE_FIXTURE;
            return AssetIntegrationDisciplineAuthority.DOMESTIC_ROUND_TABLE_FIXTURE;
        }
        int roll = r.nextInt(100);
        if (z == ZoneType.HAB_STACK || z == ZoneType.NEUTRAL_CIVILIAN_FLOOR) {
            if (roll < 24) return AssetIntegrationDisciplineAuthority.HAB_COT_FIXTURE;
            if (roll < 42) return AssetIntegrationDisciplineAuthority.WORN_BED_FIXTURE;
            if (roll < 56) return AssetIntegrationDisciplineAuthority.DOMESTIC_SINK_FIXTURE;
            if (roll < 70) return AssetIntegrationDisciplineAuthority.DOMESTIC_PREP_COUNTER_FIXTURE;
            if (roll < 82) return AssetIntegrationDisciplineAuthority.DOMESTIC_STORAGE_CABINET_FIXTURE;
            return AssetIntegrationDisciplineAuthority.DOMESTIC_PLANK_TABLE_FIXTURE;
        }
        return AssetIntegrationDisciplineAuthority.HAB_COT_FIXTURE;
    }

    static String roomLabel(String type, RoomProfile rp, ZoneType z) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "unclassified room" : rp.name;
        String zone = z == null ? "Unknown zone" : z.label;
        return (v == null ? "Domestic hab fixture" : title(v.label)) + " / " + room + " / " + zone;
    }

    static String roomStock(String type, RoomProfile rp, World w, int roomId, char under) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "room" : safe(rp.name);
        String semantic = AssetIntegrationDisciplineAuthority.semanticKeyForType(type);
        String token = v == null ? "domestic-hab-fixture" : v.token;
        return "room-fixture;domestic-hab-bucket;variant=" + token + ";roomId=" + roomId + ";room=" + room +
                ";handoff=storage-service-profile;semantic=" + safe(semantic) + ";under=" + (int)under;
    }

    static String inspectionLine(String type) {
        Variant v = variantFor(type);
        if (v == null) return "DOMESTIC HAB FIXTURE: household surface, service tag, storage edge, and hab-room handoff metadata.";
        return "DOMESTIC HAB FIXTURE: " + v.inspection + ". " + v.servicePreview + ".";
    }

    static String interactionVerb(String type) {
        Variant v = variantFor(type);
        if (v == null) return "inspects a domestic hab fixture.";
        switch (v) {
            case HAB_COT: return "inspects a hab cot.";
            case GUARD_COT: return "inspects a Guard cot.";
            case WORN_BED: return "inspects a worn bed.";
            case NOBLE_BED: return "inspects a noble bed.";
            case BUNK_BED: return "inspects a bunk bed.";
            case WATER_STORAGE: return "inspects domestic water storage.";
            case REFRIGERATOR: return "inspects a domestic refrigerator.";
            case SINK: return "inspects a sink counter.";
            case STOVE: return "inspects a stove counter.";
            case PREP_COUNTER: return "inspects a domestic prep counter.";
            case STORAGE_CABINET: return "inspects a storage cabinet.";
            case PLANK_TABLE: return "inspects a plank table.";
            case ROUND_TABLE: return "inspects a round table.";
            case MESS_TABLE: return "inspects a mess table.";
            case ORNATE_TABLE: return "inspects an ornate dining table.";
            default: return "inspects a domestic hab fixture.";
        }
    }

    static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "domestic hab fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "domestic hab fixture" : s;
    }

    static String auditSummary() {
        return "domesticHabFixture version=" + VERSION + " promotedTypes=" + CANONICAL_TYPES.length +
                " rule=canonical domestic hab variants; passive inspection and storage/service handoff only";
    }

    private static String roomText(RoomProfile rp) {
        return ((rp == null || rp.name == null) ? "" : rp.name) + " " +
                ((rp == null || rp.descriptor == null) ? "" : rp.descriptor) + " " +
                ((rp == null || rp.featureText == null) ? "" : rp.featureText);
    }

    private static boolean contains(String s, String... parts) {
        if (s == null) return false;
        for (String part : parts) if (part != null && !part.isBlank() && s.contains(part)) return true;
        return false;
    }

    private static String title(String s) { return s == null || s.isBlank() ? "Domestic hab fixture" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static String safe(String s) { return s == null ? "" : s.replace(';', ','); }
}
