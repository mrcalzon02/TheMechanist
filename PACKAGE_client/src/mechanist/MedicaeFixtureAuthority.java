package mechanist;

import java.util.*;

/**
 * Runtime authority for the medicae fixture bucket.
 *
 * Owns canonical medicae fixture variants, compact inspection lines, art keys,
 * and room/frontage placement choices without opening the full healing or
 * treatment operation economy.
 */
final class MedicaeFixtureAuthority {
    static final String VERSION = "0.9.10af";

    static final String[] CANONICAL_TYPES = {
            AssetIntegrationDisciplineAuthority.MEDICAE_FRONTAGE,
            AssetIntegrationDisciplineAuthority.MEDICAE_ROOM_FIXTURE,
            AssetIntegrationDisciplineAuthority.MEDICAE_CLINIC_STALL,
            AssetIntegrationDisciplineAuthority.MEDICAE_BACKROOM_STALL,
            AssetIntegrationDisciplineAuthority.STERILE_MEDICAE_CLEAN_BENCH
    };

    enum Variant {
        FRONTAGE(AssetIntegrationDisciplineAuthority.MEDICAE_FRONTAGE, 'N', "medicae-frontage", "feature_public_notice_wall",
                "roadside medicae frontage", "triage sign, locked cabinet, ownership plate, and antiseptic stink",
                "service-preview: triage anchor, clinic ownership, drug-supply surface, and recovery handoff"),
        ROOM_FIXTURE(AssetIntegrationDisciplineAuthority.MEDICAE_ROOM_FIXTURE, 'u', "medicae-room-fixture", "build_backroom_medicae_stall",
                "general medicae fixture", "cot, cabinet, scanner housing, and boiled instrument tray",
                "service-preview: minor recovery anchor, medicae stock surface, and clinic-room identity"),
        CLINIC_STALL(AssetIntegrationDisciplineAuthority.MEDICAE_CLINIC_STALL, 'M', "clinic-stall", "build_clinic_stall",
                "clinic treatment stall", "curtain rail, treatment stool, sealed water pan, and ration-grade privacy",
                "service-preview: patch-up counter, bandage issue, paid treatment queue, and wound-room handoff"),
        BACKROOM_STALL(AssetIntegrationDisciplineAuthority.MEDICAE_BACKROOM_STALL, 'M', "backroom-medicae-stall", "build_backroom_medicae_stall",
                "backroom medicae stall", "field cabinet, filter cloth, vial rack, hard cot, and stains pretending to be historical",
                "service-preview: field treatment stall, recovery surface, infection treatment, and medicae-loot category"),
        STERILE_CLEAN_BENCH(AssetIntegrationDisciplineAuthority.STERILE_MEDICAE_CLEAN_BENCH, 'U', "sterile-medicae-clean-bench", "build_sterile_medicae_clean_bench",
                "sterile medicae clean bench", "sealed surface, clean lamp, ampoule rack, filter housing, and expensive obedience to hygiene",
                "service-preview: sterile compounding, ampoule preparation, clinic-grade production, and staffing handoff");

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
    static {
        for (Variant v : Variant.values()) BY_TYPE.put(v.type, v);
    }

    private MedicaeFixtureAuthority() {}

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
        return v == null ? 'u' : v.glyph;
    }

    static String chooseRoomFixtureType(World w, RoomProfile rp, int roomId, Random r) {
        if (r == null) r = new Random(0);
        ZoneType z = w == null ? null : w.zoneType;
        String text = roomText(rp).toLowerCase(Locale.ROOT);
        if (contains(text, "sterile", "surgery", "surgical", "hospital", "suite", "noble", "governor")) return AssetIntegrationDisciplineAuthority.STERILE_MEDICAE_CLEAN_BENCH;
        if (contains(text, "chop-shop", "backroom", "patchwork", "scavenger", "sewer", "sump", "mutant")) return AssetIntegrationDisciplineAuthority.MEDICAE_BACKROOM_STALL;
        if (contains(text, "clinic", "triage", "wound", "aid station", "care room", "medicae")) {
            int roll = r.nextInt(100);
            if (z == ZoneType.NOBLE_SERVICE_SPINE || z == ZoneType.SECTOR_GOVERNORS_MANSION) return roll < 70 ? AssetIntegrationDisciplineAuthority.STERILE_MEDICAE_CLEAN_BENCH : AssetIntegrationDisciplineAuthority.MEDICAE_CLINIC_STALL;
            if (z == ZoneType.SUMP_MARKET || z == ZoneType.MUTANT_SEWER_CAMP || z == ZoneType.SEWER_CONDUIT || z == ZoneType.TRASH_WARREN) return roll < 70 ? AssetIntegrationDisciplineAuthority.MEDICAE_BACKROOM_STALL : AssetIntegrationDisciplineAuthority.MEDICAE_CLINIC_STALL;
            if (z == ZoneType.IMPERIAL_GUARD_BILLET) return roll < 55 ? AssetIntegrationDisciplineAuthority.MEDICAE_CLINIC_STALL : AssetIntegrationDisciplineAuthority.MEDICAE_ROOM_FIXTURE;
            return roll < 45 ? AssetIntegrationDisciplineAuthority.MEDICAE_CLINIC_STALL : (roll < 78 ? AssetIntegrationDisciplineAuthority.MEDICAE_ROOM_FIXTURE : AssetIntegrationDisciplineAuthority.MEDICAE_BACKROOM_STALL);
        }
        return AssetIntegrationDisciplineAuthority.MEDICAE_ROOM_FIXTURE;
    }

    static String roomLabel(String type, RoomProfile rp, ZoneType z) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "unclassified room" : rp.name;
        String zone = z == null ? "Unknown zone" : z.label;
        return (v == null ? "Medicae fixture" : title(v.label)) + " / " + room + " / " + zone;
    }

    static String roomStock(String type, RoomProfile rp, World w, int roomId, char under) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "room" : safe(rp.name);
        String semantic = AssetIntegrationDisciplineAuthority.semanticKeyForType(type);
        String token = v == null ? "medicae-room-fixture" : v.token;
        return "room-fixture;medicae-bucket;variant=" + token + ";roomId=" + roomId + ";room=" + room +
                ";handoff=service-preview;semantic=" + safe(semantic) + ";under=" + (int)under;
    }

    static String frontageLabel(ZoneType z, Random r) {
        if (r == null) r = new Random(0);
        return pick(r, "Medicae frontage", "Roadside clinic sign", "Public triage marker", "Wound-room frontage");
    }

    static String frontageStock(char underlying) {
        return "road-frontage;medicae-bucket;variant=medicae-frontage;semantic=" +
                safe(AssetIntegrationDisciplineAuthority.semanticKeyForType(AssetIntegrationDisciplineAuthority.MEDICAE_FRONTAGE)) +
                ";handoff=service-preview;under=" + (int)underlying;
    }

    static String inspectionLine(String type) {
        Variant v = variantFor(type);
        if (v == null) return "MEDICAE FIXTURE: clinic hardware, sealed drawers, and a recovery surface. Service preview only.";
        return "MEDICAE FIXTURE: " + v.inspection + ". " + v.servicePreview + ".";
    }

    static String frontageInspectionLine() {
        Variant v = Variant.FRONTAGE;
        return "MEDICAE FRONTAGE: " + v.inspection + ". " + v.servicePreview + ".";
    }

    static String interactionVerb(String type) {
        Variant v = variantFor(type);
        if (v == null) return "inspects a medicae fixture.";
        switch (v) {
            case STERILE_CLEAN_BENCH: return "inspects a sterile medicae bench.";
            case BACKROOM_STALL: return "inspects a backroom medicae stall.";
            case CLINIC_STALL: return "inspects a clinic treatment stall.";
            case FRONTAGE: return "inspects medicae frontage.";
            default: return "inspects a medicae room fixture.";
        }
    }

    static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "medicae fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "medicae fixture" : s;
    }

    static String auditSummary() {
        return "medicaeFixture version=" + VERSION + " promotedTypes=" + CANONICAL_TYPES.length +
                " rule=canonical medicae variants; inspection and service-preview only";
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
    private static String title(String s) { return s == null || s.isBlank() ? "Medicae fixture" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static String safe(String s) { return s == null ? "" : s.replace(';', ','); }
}
