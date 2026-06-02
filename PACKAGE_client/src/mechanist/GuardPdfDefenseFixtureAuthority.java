package mechanist;

import java.util.*;

/**
 * Runtime authority for the Astra Militarum / PDF defense fixture bucket.
 *
 * Owns canonical Guard/PDF wall, gate, damaged-wall, turret, sandbag, watch,
 * barracks, and supply-post variants. These surfaces are readable passive
 * defense and construction-handoff fixtures only; live turret targeting,
 * active patrol response, and battlefield simulation remain owned by later
 * combat/security authorities.
 */
final class GuardPdfDefenseFixtureAuthority {
    static final String VERSION = "0.9.10ai";

    static final String[] CANONICAL_TYPES = {
            AssetIntegrationDisciplineAuthority.PDF_WALL_PANEL_FIXTURE,
            AssetIntegrationDisciplineAuthority.PDF_WALL_CORNER_FIXTURE,
            AssetIntegrationDisciplineAuthority.PDF_GATE_FIXTURE,
            AssetIntegrationDisciplineAuthority.PDF_WALL_DAMAGED_FIXTURE,
            AssetIntegrationDisciplineAuthority.PDF_TURRET_MK1_FIXTURE,
            AssetIntegrationDisciplineAuthority.PDF_TURRET_MK2_FIXTURE,
            AssetIntegrationDisciplineAuthority.PDF_TURRET_MK3_FIXTURE,
            AssetIntegrationDisciplineAuthority.PDF_SANDBAG_BARRICADE_FIXTURE,
            AssetIntegrationDisciplineAuthority.PDF_SANDBAG_CORNER_FIXTURE,
            AssetIntegrationDisciplineAuthority.GUARD_BARRACKS_ANCHOR_FIXTURE,
            AssetIntegrationDisciplineAuthority.GUARD_WATCH_POST_FIXTURE,
            AssetIntegrationDisciplineAuthority.GUARD_SUPPLY_POST_FIXTURE
    };

    enum Variant {
        WALL_PANEL(AssetIntegrationDisciplineAuthority.PDF_WALL_PANEL_FIXTURE, 'W', "pdf-wall-panel", "build_pdf_wall_panel",
                "PDF wall panel", "standardized ferrocrete wall panel, serial stencil, chipped drab paint, and a firing-line geometry that cares more about throughput than elegance",
                "security-preview: Guard/PDF perimeter hardening, reinforced-wall recipe handoff, cover identity, and passive raid-resistance metadata"),
        WALL_CORNER(AssetIntegrationDisciplineAuthority.PDF_WALL_CORNER_FIXTURE, 'W', "pdf-wall-corner", "build_pdf_wall_corner",
                "PDF wall corner", "right-angle hardpoint, corner brace, welded identification plate, and all the optimism of a defensive line expected to be flanked anyway",
                "security-preview: perimeter cornering, checkpoint block shaping, reinforced-wall recipe handoff, and passive cover metadata"),
        GATE(AssetIntegrationDisciplineAuthority.PDF_GATE_FIXTURE, 'G', "pdf-gate", "build_pdf_gate",
                "PDF checkpoint gate", "field gate frame, barred reinforcement, access stripe, checkpoint lockbox, and a hinge set designed for bad news and heavy boots",
                "security-preview: checkpoint access control, reinforced-door recipe handoff, vehicle-lane filtering, and passive guard-post metadata"),
        DAMAGED_WALL(AssetIntegrationDisciplineAuthority.PDF_WALL_DAMAGED_FIXTURE, 'w', "pdf-wall-damaged", "build_pdf_wall_damaged",
                "damaged PDF wall section", "cracked defense slab, patched rebar, scorched serial paint, and enough integrity left to make command call it serviceable",
                "security-preview: damaged perimeter identity, repair/decomposition handoff, cover metadata, and battlefield-wear readable state"),
        TURRET_MK1(AssetIntegrationDisciplineAuthority.PDF_TURRET_MK1_FIXTURE, 'U', "pdf-turret-mk1", "build_pdf_turret_mk1",
                "PDF turret Mk I", "light field turret, drum cowling, hand-cranked service plate, and a traverse ring still waiting for proper orders",
                "security-preview: dormant turret mount, heavy-stub recipe handoff, ammo/staffing metadata, and no autonomous targeting"),
        TURRET_MK2(AssetIntegrationDisciplineAuthority.PDF_TURRET_MK2_FIXTURE, 'U', "pdf-turret-mk2", "build_pdf_turret_mk2",
                "PDF turret Mk II", "reinforced field turret, heavier barrel shroud, armored feed box, and a doctrinal commitment to making corridors shorter",
                "security-preview: dormant turret mount, heavy-stub recipe handoff, firing-arc metadata, and no autonomous targeting"),
        TURRET_MK3(AssetIntegrationDisciplineAuthority.PDF_TURRET_MK3_FIXTURE, 'U', "pdf-turret-mk3", "build_pdf_turret_mk3",
                "PDF turret Mk III", "fortified turret nest, armored base collar, boxed feed chute, and a machine spirit told to wait its turn",
                "security-preview: dormant heavy mount, powered-defense handoff, staffing/power/ammo metadata, and no autonomous targeting"),
        SANDBAG_BARRICADE(AssetIntegrationDisciplineAuthority.PDF_SANDBAG_BARRICADE_FIXTURE, 'S', "pdf-sandbag-barricade", "build_sandbag_barricade",
                "field sandbag barricade", "stacked sandbags, boot-scuffed cloth, mud-dark seams, and the universal military belief that dirt can become policy",
                "security-preview: cheap ballistic cover, sandbag-line recipe handoff, roadblock identity, and passive cover metadata"),
        SANDBAG_CORNER(AssetIntegrationDisciplineAuthority.PDF_SANDBAG_CORNER_FIXTURE, 'S', "pdf-sandbag-corner", "build_sandbag_corner",
                "field sandbag corner", "angled sandbag corner, patched fill sacks, lane-marker tag, and enough cover to make retreat look scheduled",
                "security-preview: cornered cover, roadblock lane shaping, sandbag-line recipe handoff, and passive cover metadata"),
        BARRACKS_ANCHOR(AssetIntegrationDisciplineAuthority.GUARD_BARRACKS_ANCHOR_FIXTURE, 'g', "guard-barracks-anchor", "build_pdf_wall_panel",
                "Guard barracks defense anchor", "barracks-side security anchor, issued rack stencil, duty-board tag, and a square of floor where discipline tries to sleep standing up",
                "security-preview: barracks staffing anchor, guard-barracks recipe handoff, response-point metadata, and passive raid-readiness support"),
        WATCH_POST(AssetIntegrationDisciplineAuthority.GUARD_WATCH_POST_FIXTURE, 'p', "guard-watch-post", "feature_alarm_sensor_post",
                "Guard watch post", "raised watch marker, vox stub, lamp hood, sightline board, and a sentry position engineered for boredom and alarm",
                "security-preview: watch-post recipe handoff, warning/sensor metadata, sightline identity, and passive guard response support"),
        SUPPLY_POST(AssetIntegrationDisciplineAuthority.GUARD_SUPPLY_POST_FIXTURE, 'q', "guard-supply-post", "build_sandbag_barricade",
                "Guard supply post", "field supply marker, stamped ration crate base, sandbagged issue corner, and quartermaster ownership condensed into one obstruction",
                "security-preview: supply-post recipe handoff, ration/munition staging identity, checkpoint support metadata, and passive logistics-security support");

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

    private GuardPdfDefenseFixtureAuthority() {}

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
        return v == null ? 'S' : v.glyph;
    }

    static String chooseRoomFixtureType(World w, RoomProfile rp, int roomId, Random r) {
        if (r == null) r = new Random(0);
        String text = roomText(rp).toLowerCase(Locale.ROOT);
        if (contains(text, "gate", "checkpoint", "access", "vehicle", "entrance", "barrier")) return pickType(r, Variant.GATE, Variant.SANDBAG_BARRICADE, Variant.WALL_PANEL, Variant.WATCH_POST);
        if (contains(text, "wall", "perimeter", "fortified", "bunker", "defensive", "hardpoint")) return pickType(r, Variant.WALL_PANEL, Variant.WALL_CORNER, Variant.DAMAGED_WALL, Variant.SANDBAG_CORNER);
        if (contains(text, "turret", "gun", "firing", "munition", "armory", "weapon", "lane")) return pickType(r, Variant.TURRET_MK1, Variant.TURRET_MK2, Variant.TURRET_MK3, Variant.SANDBAG_BARRICADE);
        if (contains(text, "watch", "sentry", "lookout", "patrol", "drill", "training")) return pickType(r, Variant.WATCH_POST, Variant.SANDBAG_CORNER, Variant.WALL_CORNER, Variant.TURRET_MK1);
        if (contains(text, "barracks", "billet", "muster", "dorm", "sleeping", "shift")) return pickType(r, Variant.BARRACKS_ANCHOR, Variant.WATCH_POST, Variant.SANDBAG_BARRICADE, Variant.WALL_PANEL);
        if (contains(text, "supply", "store", "warehouse", "ration", "quartermaster", "commissary", "cafeteria", "kitchen")) return pickType(r, Variant.SUPPLY_POST, Variant.SANDBAG_BARRICADE, Variant.GATE, Variant.WALL_PANEL);
        if (w != null && w.zoneType == ZoneType.IMPERIAL_GUARD_BILLET) {
            int roll = r.nextInt(100);
            if (roll < 14) return AssetIntegrationDisciplineAuthority.GUARD_BARRACKS_ANCHOR_FIXTURE;
            if (roll < 26) return AssetIntegrationDisciplineAuthority.GUARD_WATCH_POST_FIXTURE;
            if (roll < 38) return AssetIntegrationDisciplineAuthority.GUARD_SUPPLY_POST_FIXTURE;
            if (roll < 51) return AssetIntegrationDisciplineAuthority.PDF_SANDBAG_BARRICADE_FIXTURE;
            if (roll < 62) return AssetIntegrationDisciplineAuthority.PDF_SANDBAG_CORNER_FIXTURE;
            if (roll < 73) return AssetIntegrationDisciplineAuthority.PDF_WALL_PANEL_FIXTURE;
            if (roll < 82) return AssetIntegrationDisciplineAuthority.PDF_WALL_CORNER_FIXTURE;
            if (roll < 90) return AssetIntegrationDisciplineAuthority.PDF_GATE_FIXTURE;
            if (roll < 96) return AssetIntegrationDisciplineAuthority.PDF_TURRET_MK1_FIXTURE;
            return AssetIntegrationDisciplineAuthority.PDF_TURRET_MK2_FIXTURE;
        }
        return AssetIntegrationDisciplineAuthority.PDF_SANDBAG_BARRICADE_FIXTURE;
    }

    private static String pickType(Random r, Variant... variants) {
        if (variants == null || variants.length == 0) return AssetIntegrationDisciplineAuthority.PDF_SANDBAG_BARRICADE_FIXTURE;
        Variant v = variants[Math.floorMod(r.nextInt(), variants.length)];
        return v.type;
    }

    static String roomLabel(String type, RoomProfile rp, ZoneType z) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "unclassified Guard room" : rp.name;
        String zone = z == null ? "unknown military zone" : z.label;
        String label = v == null ? "Guard/PDF defense fixture" : v.label;
        return title(label) + " / " + room + " / " + zone;
    }

    static String roomStock(String type, RoomProfile rp, World w, int roomId, char under) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "room" : safe(rp.name);
        String semantic = AssetIntegrationDisciplineAuthority.semanticKeyForType(type);
        String token = v == null ? "guard-pdf-defense-fixture" : v.token;
        return "room-fixture;guard-pdf-defense-bucket;variant=" + token + ";roomId=" + roomId + ";room=" + room +
                ";handoff=passive-defense-preview;semantic=" + safe(semantic) + ";under=" + (int)under;
    }

    static String inspectionLine(String type) {
        Variant v = variantFor(type);
        if (v == null) return "GUARD/PDF DEFENSE FIXTURE: field-issue hardware, passive cover semantics, and construction handoff metadata.";
        return "GUARD/PDF DEFENSE FIXTURE: " + v.inspection + ". " + v.servicePreview + ".";
    }

    static String interactionVerb(String type) {
        Variant v = variantFor(type);
        if (v == null) return "inspects a Guard/PDF defense fixture.";
        switch (v) {
            case WALL_PANEL: return "inspects a PDF wall panel.";
            case WALL_CORNER: return "inspects a PDF wall corner.";
            case GATE: return "inspects a PDF checkpoint gate.";
            case DAMAGED_WALL: return "inspects a damaged PDF wall section.";
            case TURRET_MK1: return "inspects a dormant PDF turret Mk I.";
            case TURRET_MK2: return "inspects a dormant PDF turret Mk II.";
            case TURRET_MK3: return "inspects a dormant PDF turret Mk III.";
            case SANDBAG_BARRICADE: return "inspects a field sandbag barricade.";
            case SANDBAG_CORNER: return "inspects a field sandbag corner.";
            case BARRACKS_ANCHOR: return "inspects a Guard barracks defense anchor.";
            case WATCH_POST: return "inspects a Guard watch post.";
            case SUPPLY_POST: return "inspects a Guard supply post.";
            default: return "inspects a Guard/PDF defense fixture.";
        }
    }

    static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "Guard/PDF defense fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "Guard/PDF defense fixture" : s;
    }

    static String auditSummary() {
        return "guardPdfDefenseFixture version=" + VERSION + " promotedTypes=" + CANONICAL_TYPES.length +
                " rule=field-issue defense art; passive/security-preview only";
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

    private static String title(String s) { return s == null || s.isBlank() ? "Guard/PDF defense fixture" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static String safe(String s) { return s == null ? "" : s.replace(';', ','); }
}
