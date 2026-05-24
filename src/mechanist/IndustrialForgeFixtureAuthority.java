package mechanist;

import java.util.*;

/**
 * Runtime authority for the industrial / forge machinery bucket.
 *
 * Owns canonical workshop, forge, and condenser-adjacent machinery variants,
 * semantic art routing, compact inspection lines, room-placement selection,
 * and shared operation-handoff metadata without creating a separate
 * manufacturing system.
 */
final class IndustrialForgeFixtureAuthority {
    static final String VERSION = "0.9.10af";

    static final String[] CANONICAL_TYPES = {
            AssetIntegrationDisciplineAuthority.SCRAP_WORKBENCH_FIXTURE,
            AssetIntegrationDisciplineAuthority.EMM_MICRO_FORGE_FIXTURE,
            AssetIntegrationDisciplineAuthority.EMM_ATMOSPHERIC_CONDENSER_FIXTURE,
            AssetIntegrationDisciplineAuthority.MACHINE_MAINTENANCE_RACK_FIXTURE
    };

    enum Variant {
        SCRAP_WORKBENCH(AssetIntegrationDisciplineAuthority.SCRAP_WORKBENCH_FIXTURE, 'w', "scrap-workbench", "build_scrap_workbench",
                "scrap workbench fixture", "scarred bench plate, hand tools, rivet tray, and a vise that has seen ethical compromise",
                "operation-preview: basic repair, salvage sorting, scrap work, and manual fabrication handoff"),
        MICRO_FORGE(AssetIntegrationDisciplineAuthority.EMM_MICRO_FORGE_FIXTURE, 'f', "emm-micro-forge", "build_emm_micro_forge",
                "EMM micro forge fixture", "heat shielding, gear train, forge throat, motor housing, and a shrine-marked burn scar",
                "operation-preview: scrap-forging, construction supplies, machine parts, maintenance, and worker-machine handoff"),
        ATMOSPHERIC_CONDENSER(AssetIntegrationDisciplineAuthority.EMM_ATMOSPHERIC_CONDENSER_FIXTURE, 'e', "emm-atmospheric-condenser", "build_emm_atmospheric_condenser",
                "EMM atmospheric condenser fixture", "intake vanes, drip pan, filter cartridge seat, pipe couplings, and damp authority over bad air",
                "operation-preview: water capture, filtration support, pipe-room service, and reclamation handoff"),
        MAINTENANCE_RACK(AssetIntegrationDisciplineAuthority.MACHINE_MAINTENANCE_RACK_FIXTURE, 'N', "machine-maintenance-rack", "build_business_addon_fixture",
                "machine maintenance rack", "tool rack, grease cup, fastener bins, calibration tags, and parts nobody admits are missing",
                "operation-preview: machine upkeep, repair staging, component issue, and workshop support handoff");

        final String type;
        final char glyph;
        final String token;
        final String artKey;
        final String label;
        final String inspection;
        final String operationPreview;

        Variant(String type, char glyph, String token, String artKey, String label, String inspection, String operationPreview) {
            this.type = type;
            this.glyph = glyph;
            this.token = token;
            this.artKey = artKey;
            this.label = label;
            this.inspection = inspection;
            this.operationPreview = operationPreview;
        }
    }

    private static final LinkedHashMap<String, Variant> BY_TYPE = new LinkedHashMap<>();
    static {
        for (Variant v : Variant.values()) BY_TYPE.put(v.type, v);
    }

    private IndustrialForgeFixtureAuthority() {}

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
        ZoneType z = w == null ? null : w.zoneType;
        String text = roomText(rp).toLowerCase(Locale.ROOT);
        if (contains(text, "condenser", "reclamation", "water recovery", "damp coils", "filter", "pipe junction", "water recycler")) return AssetIntegrationDisciplineAuthority.EMM_ATMOSPHERIC_CONDENSER_FIXTURE;
        if (contains(text, "repair booth", "maintenance", "tool", "calibration", "spare fastener", "wire reel", "machine shrine")) {
            return r.nextInt(100) < 58 ? AssetIntegrationDisciplineAuthority.MACHINE_MAINTENANCE_RACK_FIXTURE : AssetIntegrationDisciplineAuthority.SCRAP_WORKBENCH_FIXTURE;
        }
        if (contains(text, "workshop", "workbench", "scrap", "chop shop", "salvage", "product warehouse", "component warehouse")) {
            return r.nextInt(100) < 62 ? AssetIntegrationDisciplineAuthority.SCRAP_WORKBENCH_FIXTURE : AssetIntegrationDisciplineAuthority.MACHINE_MAINTENANCE_RACK_FIXTURE;
        }
        if (contains(text, "forge", "smelter", "assembler", "machine", "press", "manufactorum", "industrial", "boiler")) {
            int roll = r.nextInt(100);
            if (z == ZoneType.MECHANICUS_FORGE_CLOISTER || z == ZoneType.MECHANICUS_RELIC_DUCT) {
                if (roll < 62) return AssetIntegrationDisciplineAuthority.EMM_MICRO_FORGE_FIXTURE;
                if (roll < 82) return AssetIntegrationDisciplineAuthority.MACHINE_MAINTENANCE_RACK_FIXTURE;
                return AssetIntegrationDisciplineAuthority.EMM_ATMOSPHERIC_CONDENSER_FIXTURE;
            }
            if (z == ZoneType.SEWER_CONDUIT || z == ZoneType.TRASH_WARREN || z == ZoneType.SUMP_MARKET || z == ZoneType.MUTANT_SEWER_CAMP) {
                if (roll < 48) return AssetIntegrationDisciplineAuthority.SCRAP_WORKBENCH_FIXTURE;
                if (roll < 78) return AssetIntegrationDisciplineAuthority.MACHINE_MAINTENANCE_RACK_FIXTURE;
                return AssetIntegrationDisciplineAuthority.EMM_ATMOSPHERIC_CONDENSER_FIXTURE;
            }
            if (roll < 45) return AssetIntegrationDisciplineAuthority.EMM_MICRO_FORGE_FIXTURE;
            if (roll < 70) return AssetIntegrationDisciplineAuthority.SCRAP_WORKBENCH_FIXTURE;
            if (roll < 88) return AssetIntegrationDisciplineAuthority.MACHINE_MAINTENANCE_RACK_FIXTURE;
            return AssetIntegrationDisciplineAuthority.EMM_ATMOSPHERIC_CONDENSER_FIXTURE;
        }
        return AssetIntegrationDisciplineAuthority.EMM_MICRO_FORGE_FIXTURE;
    }

    static String roomLabel(String type, RoomProfile rp, ZoneType z) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "unclassified room" : rp.name;
        String zone = z == null ? "Unknown zone" : z.label;
        return (v == null ? "Industrial fixture" : title(v.label)) + " / " + room + " / " + zone;
    }

    static String roomStock(String type, RoomProfile rp, World w, int roomId, char under) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "room" : safe(rp.name);
        String semantic = AssetIntegrationDisciplineAuthority.semanticKeyForType(type);
        String token = v == null ? "industrial-forge-fixture" : v.token;
        return "room-fixture;industrial-forge-bucket;variant=" + token + ";roomId=" + roomId + ";room=" + room +
                ";handoff=operation-profile;semantic=" + safe(semantic) + ";under=" + (int)under;
    }

    static String inspectionLine(String type) {
        Variant v = variantFor(type);
        if (v == null) return "INDUSTRIAL FIXTURE: heavy frame, tool scarring, service tags, and shared operation-profile handoff metadata.";
        return "INDUSTRIAL FIXTURE: " + v.inspection + ". " + v.operationPreview + ".";
    }

    static String interactionVerb(String type) {
        Variant v = variantFor(type);
        if (v == null) return "inspects an industrial machine fixture.";
        switch (v) {
            case SCRAP_WORKBENCH: return "inspects a scrap workbench fixture.";
            case MICRO_FORGE: return "inspects an EMM micro forge fixture.";
            case ATMOSPHERIC_CONDENSER: return "inspects an atmospheric condenser fixture.";
            case MAINTENANCE_RACK: return "inspects a machine maintenance rack.";
            default: return "inspects an industrial machine fixture.";
        }
    }

    static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "industrial fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "industrial fixture" : s;
    }

    static String auditSummary() {
        return "industrialForgeFixture version=" + VERSION + " promotedTypes=" + CANONICAL_TYPES.length +
                " rule=canonical industrial/forge variants; operation-profile handoff only";
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

    private static String title(String s) { return s == null || s.isBlank() ? "Industrial fixture" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static String safe(String s) { return s == null ? "" : s.replace(';', ','); }
}
