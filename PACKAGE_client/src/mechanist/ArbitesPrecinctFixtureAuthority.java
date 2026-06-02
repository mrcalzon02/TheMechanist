package mechanist;

import java.util.*;

/**
 * Runtime authority for the Arbites precinct fixture bucket.
 *
 * Owns canonical precinct desks, custody furniture, lockers, alarm surfaces,
 * signs, doors, and service-clutter variants. These are readable security and
 * custody fixtures only; live enforcement, arrest procedure, and combat behavior
 * remain outside this bucket.
 */
final class ArbitesPrecinctFixtureAuthority {
    static final String VERSION = "0.9.10af";

    static final String[] CANONICAL_TYPES = {
            AssetIntegrationDisciplineAuthority.ARBITES_COMMAND_DESK_FIXTURE,
            AssetIntegrationDisciplineAuthority.ARBITES_SERGEANT_DESK_FIXTURE,
            AssetIntegrationDisciplineAuthority.ARBITES_COFFEE_MAKER_FIXTURE,
            AssetIntegrationDisciplineAuthority.ARBITES_WEAPON_LOCKER_FIXTURE,
            AssetIntegrationDisciplineAuthority.ARBITES_PERP_BENCH_FIXTURE,
            AssetIntegrationDisciplineAuthority.ARBITES_INTERROGATION_TABLE_FIXTURE,
            AssetIntegrationDisciplineAuthority.ARBITES_HOLDING_CELL_FIXTURE,
            AssetIntegrationDisciplineAuthority.ARBITES_PRECINCT_DOOR_FIXTURE,
            AssetIntegrationDisciplineAuthority.ARBITES_PRECINCT_SIGN_FIXTURE,
            AssetIntegrationDisciplineAuthority.ARBITES_ALARM_PANEL_FIXTURE,
            AssetIntegrationDisciplineAuthority.ARBITES_EVIDENCE_LOCKER_FIXTURE
    };

    enum Variant {
        COMMAND_DESK(AssetIntegrationDisciplineAuthority.ARBITES_COMMAND_DESK_FIXTURE, 'A', "arbites-command-desk", "feature_arbites_command_desk",
                "Arbites command desk", "sealed duty ledger, charge slate, vox handset, precinct seal, and a desk built to make panic queue properly",
                "security-preview: complaint intake, custody routing, bounty notice, patrol assignment, and faction service handoff"),
        SERGEANT_DESK(AssetIntegrationDisciplineAuthority.ARBITES_SERGEANT_DESK_FIXTURE, 'A', "arbites-sergeant-desk", "feature_arbites_sergeant_desk",
                "Arbites sergeant desk", "rank marker, baton tray, shift rota, citation stack, and a chair that has judged entire bloodlines",
                "security-preview: patrol orders, disciplinary routing, checkpoint ownership, and precinct staffing handoff"),
        COFFEE_MAKER(AssetIntegrationDisciplineAuthority.ARBITES_COFFEE_MAKER_FIXTURE, 'N', "arbites-coffee-maker", "feature_arbites_coffee_maker",
                "precinct recaf maker", "burnt recaf tin, cracked cup rail, service cable, and the smell of sleepless public order",
                "service-preview: duty-room fatigue relief, mess-room identity, shift continuity, and morale handoff"),
        WEAPON_LOCKER(AssetIntegrationDisciplineAuthority.ARBITES_WEAPON_LOCKER_FIXTURE, 'X', "arbites-weapon-locker", "feature_arbites_weapon_locker",
                "Arbites weapon locker", "serialized locks, shotgun brackets, shock-maul clips, ammunition tags, and legal violence in tidy rows",
                "security-preview: armory custody, controlled weapon issue, gear audit, and faction security handoff"),
        PERP_BENCH(AssetIntegrationDisciplineAuthority.ARBITES_PERP_BENCH_FIXTURE, 'b', "arbites-perp-bench", "feature_arbites_perp_bench",
                "perp bench", "bolted bench, wrist ring, floor scuffs, spit marks, and a strong implication that nobody sits by choice",
                "custody-preview: detention intake, witness waiting, complaint overflow, and restraint handoff"),
        INTERROGATION_TABLE(AssetIntegrationDisciplineAuthority.ARBITES_INTERROGATION_TABLE_FIXTURE, 'T', "arbites-interrogation-table", "feature_arbites_interrogation_table",
                "interrogation table", "single table, two chairs, lamp mount, drain mark, and the architecture of official discomfort",
                "custody-preview: questioning surface, evidence review, intimidation context, and investigation handoff"),
        HOLDING_CELL(AssetIntegrationDisciplineAuthority.ARBITES_HOLDING_CELL_FIXTURE, 'X', "arbites-holding-cell", "feature_arbites_holding_cell",
                "holding cell fixture", "barred cell face, hard bunk, numbered tag, floor drain, and scratched civic outcomes",
                "custody-preview: detainment space, prisoner overflow, cell assignment, and noncombat security handoff"),
        PRECINCT_DOOR(AssetIntegrationDisciplineAuthority.ARBITES_PRECINCT_DOOR_FIXTURE, 'X', "arbites-precinct-door", "feature_arbites_precinct_door",
                "precinct access door", "reinforced frame, serialized lock plate, warning stamp, and hinge geometry with no public sympathy",
                "security-preview: access-control identity, checkpoint partitioning, door-hardening, and defense handoff"),
        PRECINCT_SIGN(AssetIntegrationDisciplineAuthority.ARBITES_PRECINCT_SIGN_FIXTURE, 'A', "arbites-precinct-sign", "feature_arbites_precinct_sign",
                "precinct sign", "official notice plate, jurisdiction line, faded seal, and public instruction written as threat furniture",
                "service-preview: precinct identification, complaint routing, citation payment, and civic fear handoff"),
        ALARM_PANEL(AssetIntegrationDisciplineAuthority.ARBITES_ALARM_PANEL_FIXTURE, 'a', "arbites-alarm-panel", "feature_arbites_alarm_panel",
                "precinct alarm panel", "red warning lens, sealed switch cover, wire chase, and labels that assume guilt moves quickly",
                "security-preview: alarm routing, sensor reporting, lockdown context, and later defense-system handoff"),
        EVIDENCE_LOCKER(AssetIntegrationDisciplineAuthority.ARBITES_EVIDENCE_LOCKER_FIXTURE, 's', "arbites-evidence-locker", "feature_arbites_evidence_locker",
                "evidence locker", "tagged drawers, property bags, chain-of-custody seals, and truth filed behind a lock core",
                "security-preview: evidence custody, contraband storage, property audit, and investigation-service handoff");

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

    private ArbitesPrecinctFixtureAuthority() {}

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
        return v == null ? 'A' : v.glyph;
    }

    static String chooseRoomFixtureType(World w, RoomProfile rp, int roomId, Random r) {
        if (r == null) r = new Random(0);
        String text = roomText(rp).toLowerCase(Locale.ROOT);
        if (contains(text, "holding", "cell", "detention", "custody pen", "prisoner")) return pickType(r, Variant.HOLDING_CELL, Variant.PERP_BENCH, Variant.PRECINCT_DOOR, Variant.ALARM_PANEL);
        if (contains(text, "interrogation", "questioning", "confession", "black room")) return pickType(r, Variant.INTERROGATION_TABLE, Variant.ALARM_PANEL, Variant.EVIDENCE_LOCKER, Variant.SERGEANT_DESK);
        if (contains(text, "evidence", "contraband", "property", "warehouse", "storehouse", "vault")) return pickType(r, Variant.EVIDENCE_LOCKER, Variant.WEAPON_LOCKER, Variant.ALARM_PANEL, Variant.PRECINCT_DOOR);
        if (contains(text, "armory", "weapon", "baton", "ammo", "riot", "checkpoint")) return pickType(r, Variant.WEAPON_LOCKER, Variant.ALARM_PANEL, Variant.SERGEANT_DESK, Variant.PRECINCT_DOOR);
        if (contains(text, "complaint", "counter", "lobby", "public", "intake", "queue", "front desk")) return pickType(r, Variant.COMMAND_DESK, Variant.SERGEANT_DESK, Variant.PERP_BENCH, Variant.PRECINCT_SIGN, Variant.ALARM_PANEL);
        if (contains(text, "mess", "cafeteria", "duty barracks", "barracks", "dorm", "shift")) return pickType(r, Variant.COFFEE_MAKER, Variant.SERGEANT_DESK, Variant.WEAPON_LOCKER, Variant.PRECINCT_SIGN);
        if (w != null && w.zoneType == ZoneType.ARBITES_PRECINCT_EDGE) {
            int roll = r.nextInt(100);
            if (roll < 18) return AssetIntegrationDisciplineAuthority.ARBITES_COMMAND_DESK_FIXTURE;
            if (roll < 32) return AssetIntegrationDisciplineAuthority.ARBITES_SERGEANT_DESK_FIXTURE;
            if (roll < 44) return AssetIntegrationDisciplineAuthority.ARBITES_EVIDENCE_LOCKER_FIXTURE;
            if (roll < 56) return AssetIntegrationDisciplineAuthority.ARBITES_WEAPON_LOCKER_FIXTURE;
            if (roll < 68) return AssetIntegrationDisciplineAuthority.ARBITES_PERP_BENCH_FIXTURE;
            if (roll < 80) return AssetIntegrationDisciplineAuthority.ARBITES_HOLDING_CELL_FIXTURE;
            if (roll < 90) return AssetIntegrationDisciplineAuthority.ARBITES_ALARM_PANEL_FIXTURE;
            return AssetIntegrationDisciplineAuthority.ARBITES_COFFEE_MAKER_FIXTURE;
        }
        return AssetIntegrationDisciplineAuthority.ARBITES_SERGEANT_DESK_FIXTURE;
    }

    private static String pickType(Random r, Variant... variants) {
        if (variants == null || variants.length == 0) return AssetIntegrationDisciplineAuthority.ARBITES_SERGEANT_DESK_FIXTURE;
        Variant v = variants[Math.floorMod(r.nextInt(), variants.length)];
        return v.type;
    }

    static String roomLabel(String type, RoomProfile rp, ZoneType z) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "unclassified room" : rp.name;
        String zone = z == null ? "Unknown zone" : z.label;
        return (v == null ? "Arbites precinct fixture" : title(v.label)) + " / " + room + " / " + zone;
    }

    static String roomStock(String type, RoomProfile rp, World w, int roomId, char under) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "room" : safe(rp.name);
        String semantic = AssetIntegrationDisciplineAuthority.semanticKeyForType(type);
        String token = v == null ? "arbites-precinct-fixture" : v.token;
        return "room-fixture;arbites-precinct-bucket;variant=" + token + ";roomId=" + roomId + ";room=" + room +
                ";handoff=security-service-preview;semantic=" + safe(semantic) + ";under=" + (int)under;
    }

    static String inspectionLine(String type) {
        Variant v = variantFor(type);
        if (v == null) return "ARBITES PRECINCT FIXTURE: serialized furniture, custody tags, official surfaces, and security-service preview metadata.";
        return "ARBITES PRECINCT FIXTURE: " + v.inspection + ". " + v.servicePreview + ".";
    }

    static String interactionVerb(String type) {
        Variant v = variantFor(type);
        if (v == null) return "inspects an Arbites precinct fixture.";
        switch (v) {
            case COMMAND_DESK: return "inspects an Arbites command desk.";
            case SERGEANT_DESK: return "inspects an Arbites sergeant desk.";
            case COFFEE_MAKER: return "inspects a precinct recaf maker.";
            case WEAPON_LOCKER: return "inspects an Arbites weapon locker.";
            case PERP_BENCH: return "inspects a perp bench.";
            case INTERROGATION_TABLE: return "inspects an interrogation table.";
            case HOLDING_CELL: return "inspects a holding cell fixture.";
            case PRECINCT_DOOR: return "inspects a precinct access door.";
            case PRECINCT_SIGN: return "inspects a precinct sign.";
            case ALARM_PANEL: return "inspects a precinct alarm panel.";
            case EVIDENCE_LOCKER: return "inspects an evidence locker.";
            default: return "inspects an Arbites precinct fixture.";
        }
    }

    static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "Arbites precinct fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "Arbites precinct fixture" : s;
    }

    static String auditSummary() {
        return "arbitesPrecinctFixture version=" + VERSION + " promotedTypes=" + CANONICAL_TYPES.length +
                " rule=precinct/custody/security fixtures; service-preview only";
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

    private static String title(String s) { return s == null || s.isBlank() ? "Arbites precinct fixture" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static String safe(String s) { return s == null ? "" : s.replace(';', ','); }
}
