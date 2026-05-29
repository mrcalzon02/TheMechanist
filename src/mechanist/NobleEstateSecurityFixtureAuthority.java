package mechanist;

import java.util.*;

/**
 * Runtime authority for the noble estate security fixture bucket.
 *
 * Owns canonical estate wall, gate, tower, turret, shield, pylon, fence, and
 * control-panel variants. These are readable private-security fixtures only;
 * live traps, autonomous targeting, burglary resolution, and alarm escalation
 * remain owned by later security/combat authorities.
 */
final class NobleEstateSecurityFixtureAuthority {
    static final String VERSION = "0.9.10af";

    static final String[] CANONICAL_TYPES = {
            AssetIntegrationDisciplineAuthority.NOBLE_WALL_PANEL_FIXTURE,
            AssetIntegrationDisciplineAuthority.NOBLE_GATE_FIXTURE,
            AssetIntegrationDisciplineAuthority.NOBLE_CORNER_TOWER_FIXTURE,
            AssetIntegrationDisciplineAuthority.NOBLE_GILDED_SENTRY_FIXTURE,
            AssetIntegrationDisciplineAuthority.NOBLE_SHIELD_RELAY_FIXTURE,
            AssetIntegrationDisciplineAuthority.NOBLE_VOID_SHIELD_DOME_FIXTURE,
            AssetIntegrationDisciplineAuthority.NOBLE_LASER_PYLON_FIXTURE,
            AssetIntegrationDisciplineAuthority.NOBLE_ENERGY_FENCE_FIXTURE,
            AssetIntegrationDisciplineAuthority.NOBLE_SECURITY_PANEL_FIXTURE
    };

    enum Variant {
        WALL_PANEL(AssetIntegrationDisciplineAuthority.NOBLE_WALL_PANEL_FIXTURE, 'W', "noble-wall-panel", "build_noble_wall_panel",
                "noble wall panel", "gilded defensive wall panel, polished hardpoint ribbing, household crest plate, and a price tag that doubles as intimidation",
                "security-preview: estate perimeter, hard-room partitioning, noble boundary identity, and defensive construction handoff"),
        GATE(AssetIntegrationDisciplineAuthority.NOBLE_GATE_FIXTURE, 'G', "noble-gate", "build_noble_gate",
                "noble security gate", "ornate gate frame, coded lockbox, gilt hinge caps, honor-seal warning plate, and a servant-sized gap in human sympathy",
                "security-preview: estate access control, service checkpoint, guest filtering, and reinforced-door handoff"),
        CORNER_TOWER(AssetIntegrationDisciplineAuthority.NOBLE_CORNER_TOWER_FIXTURE, 'T', "noble-corner-tower", "build_noble_corner_tower",
                "noble corner tower", "compact overwatch tower, sculpted parapet, sensor slit, family heraldry, and architectural contempt for ground-level life",
                "security-preview: guard overwatch, estate sightline, patrol anchor, and watch-post handoff"),
        GILDED_SENTRY(AssetIntegrationDisciplineAuthority.NOBLE_GILDED_SENTRY_FIXTURE, 'Z', "noble-gilded-sentry", "build_noble_turret",
                "gilded sentry turret", "polished sentry turret, inlaid casing, sealed traverse ring, private-house serial tag, and expensive restraint pretending to be taste",
                "security-preview: private turret placement, controlled firing arc metadata, ownership binding, and defense-profile handoff"),
        SHIELD_RELAY(AssetIntegrationDisciplineAuthority.NOBLE_SHIELD_RELAY_FIXTURE, 'H', "noble-shield-relay", "build_noble_shield_relay",
                "noble shield relay", "humming relay plinth, brass coil cage, jewel-like status lamps, and a small shrine to not paying for repairs twice",
                "security-preview: shield relay coverage, protected-room identity, utility draw metadata, and ward handoff"),
        VOID_SHIELD_DOME(AssetIntegrationDisciplineAuthority.NOBLE_VOID_SHIELD_DOME_FIXTURE, 'O', "noble-void-shield-dome", "build_noble_void_shield_dome",
                "noble void-shield dome", "domed field emitter, layered trim, aristocratic field plaques, and the quiet promise that poverty will bounce off first",
                "security-preview: high-tier warding surface, estate panic-room identity, power dependency, and shield-system handoff"),
        LASER_PYLON(AssetIntegrationDisciplineAuthority.NOBLE_LASER_PYLON_FIXTURE, 'L', "noble-laser-pylon", "build_noble_laser_pylon",
                "noble laser pylon", "slender pylon, las focusing head, ornamental finials, target mark etching, and all the subtlety of inherited violence",
                "security-preview: beam-denial hardpoint, power/line-of-sight metadata, turret-family handoff, and private security identity"),
        ENERGY_FENCE(AssetIntegrationDisciplineAuthority.NOBLE_ENERGY_FENCE_FIXTURE, 'E', "noble-energy-fence", "build_noble_energy_fence",
                "noble energy fence", "waist-high emitter rail, shimmering warning bead, gilded anchor caps, and enough current to make trespass doctrinally educational",
                "security-preview: perimeter denial, access-channel shaping, hazard metadata, and estate-defense handoff"),
        SECURITY_PANEL(AssetIntegrationDisciplineAuthority.NOBLE_SECURITY_PANEL_FIXTURE, 'x', "noble-security-panel", "feature_noble_security_panel",
                "noble security panel", "private alarm panel, house cipher pad, servant override label, and a maintenance socket surrounded by legal threats",
                "security-preview: alarm routing, estate lock coordination, sensor status, and service-panel handoff");

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

    private NobleEstateSecurityFixtureAuthority() {}

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
        return v == null ? 'N' : v.glyph;
    }

    static String chooseRoomFixtureType(World w, RoomProfile rp, int roomId, Random r) {
        if (r == null) r = new Random(0);
        String text = roomText(rp).toLowerCase(Locale.ROOT);
        if (contains(text, "gate", "entry", "foyer", "vestibule", "checkpoint", "servant access", "service access")) return pickType(r, Variant.GATE, Variant.SECURITY_PANEL, Variant.WALL_PANEL, Variant.ENERGY_FENCE);
        if (contains(text, "vault", "safe", "panic", "treasury", "relic", "heirloom", "lockbox")) return pickType(r, Variant.VOID_SHIELD_DOME, Variant.SHIELD_RELAY, Variant.SECURITY_PANEL, Variant.GILDED_SENTRY);
        if (contains(text, "gallery", "hunting", "dueling", "trophy", "armory", "weapon", "guard")) return pickType(r, Variant.GILDED_SENTRY, Variant.LASER_PYLON, Variant.SECURITY_PANEL, Variant.CORNER_TOWER);
        if (contains(text, "garden", "orchard", "atrium", "conservatory", "courtyard", "perimeter")) return pickType(r, Variant.ENERGY_FENCE, Variant.CORNER_TOWER, Variant.WALL_PANEL, Variant.SHIELD_RELAY);
        if (contains(text, "security", "alarm", "sensor", "camera", "control", "logic Engine")) return pickType(r, Variant.SECURITY_PANEL, Variant.SHIELD_RELAY, Variant.LASER_PYLON, Variant.GILDED_SENTRY);
        if (contains(text, "wall", "partition", "barricade", "hardpoint", "fortified")) return pickType(r, Variant.WALL_PANEL, Variant.GATE, Variant.ENERGY_FENCE, Variant.CORNER_TOWER);
        if (w != null && (w.zoneType == ZoneType.SECTOR_GOVERNORS_MANSION || w.zoneType == ZoneType.NOBLE_SERVICE_SPINE)) {
            int roll = r.nextInt(100);
            if (roll < 16) return AssetIntegrationDisciplineAuthority.NOBLE_SECURITY_PANEL_FIXTURE;
            if (roll < 29) return AssetIntegrationDisciplineAuthority.NOBLE_WALL_PANEL_FIXTURE;
            if (roll < 42) return AssetIntegrationDisciplineAuthority.NOBLE_GATE_FIXTURE;
            if (roll < 54) return AssetIntegrationDisciplineAuthority.NOBLE_GILDED_SENTRY_FIXTURE;
            if (roll < 66) return AssetIntegrationDisciplineAuthority.NOBLE_SHIELD_RELAY_FIXTURE;
            if (roll < 76) return AssetIntegrationDisciplineAuthority.NOBLE_ENERGY_FENCE_FIXTURE;
            if (roll < 86) return AssetIntegrationDisciplineAuthority.NOBLE_LASER_PYLON_FIXTURE;
            if (roll < 94) return AssetIntegrationDisciplineAuthority.NOBLE_CORNER_TOWER_FIXTURE;
            return AssetIntegrationDisciplineAuthority.NOBLE_VOID_SHIELD_DOME_FIXTURE;
        }
        return AssetIntegrationDisciplineAuthority.NOBLE_SECURITY_PANEL_FIXTURE;
    }

    private static String pickType(Random r, Variant... variants) {
        if (variants == null || variants.length == 0) return AssetIntegrationDisciplineAuthority.NOBLE_SECURITY_PANEL_FIXTURE;
        Variant v = variants[Math.floorMod(r.nextInt(), variants.length)];
        return v.type;
    }

    static String roomLabel(String type, RoomProfile rp, ZoneType z) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "unclassified estate room" : rp.name;
        String zone = z == null ? "unknown estate zone" : z.label;
        String label = v == null ? "noble estate security fixture" : v.label;
        return label + " / " + room + " / " + zone;
    }

    static String roomStock(String type, RoomProfile rp, World w, int roomId, char under) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "room" : rp.name.replace(';', ',');
        String token = v == null ? "noble-security" : v.token;
        String preview = v == null ? "security-preview: estate security surface" : v.servicePreview;
        return "room-fixture;family=noble-estate-security;token=" + token + ";roomId=" + roomId + ";room=" + room + ";handoff=" + preview + ";under=" + (int)under;
    }

    static String inspectionLine(String type) {
        Variant v = variantFor(type);
        if (v == null) return "NOBLE SECURITY FIXTURE: expensive defensive furniture with private-house access rules and no public-service promise.";
        return v.label.toUpperCase(Locale.ROOT) + ": " + v.inspection + ". " + v.servicePreview + ".";
    }

    static String interactionVerb(String type) {
        Variant v = variantFor(type);
        String label = v == null ? "noble security fixture" : v.label;
        return "inspects " + label + ".";
    }

    static String shortLabel(MapObjectState m) {
        Variant v = variantFor(m == null ? null : m.type);
        if (v != null) return v.label;
        return m == null || m.label == null || m.label.isBlank() ? "noble security fixture" : m.label;
    }

    static ArrayList<String> auditLines() {
        ArrayList<String> out = new ArrayList<>();
        out.add("nobleEstateSecurity version=" + VERSION + " variants=" + BY_TYPE.size() + " scope=inspectable private-security fixtures");
        for (Variant v : Variant.values()) out.add(v.type + " -> " + v.artKey + " :: " + v.servicePreview);
        return out;
    }

    private static String roomText(RoomProfile rp) {
        if (rp == null) return "";
        return (rp.name == null ? "" : rp.name) + " " + (rp.descriptor == null ? "" : rp.descriptor) + " " + (rp.featureText == null ? "" : rp.featureText);
    }

    private static boolean contains(String s, String... parts) {
        if (s == null) return false;
        for (String p : parts) if (p != null && !p.isBlank() && s.contains(p)) return true;
        return false;
    }
}
