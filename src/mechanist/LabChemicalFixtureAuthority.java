package mechanist;

import java.util.*;

/**
 * Runtime authority for the lab / chemical fixture bucket.
 *
 * Owns canonical laboratory and chemical-processing variants, art routing,
 * compact inspection lines, room-placement selection, and operation-handoff
 * metadata without opening a separate chemistry economy.
 */
final class LabChemicalFixtureAuthority {
    static final String VERSION = "0.9.10af";

    static final String[] CANONICAL_TYPES = {
            AssetIntegrationDisciplineAuthority.LABORATORY_ROOM_FIXTURE,
            AssetIntegrationDisciplineAuthority.CRUDE_CHEM_BENCH_FIXTURE,
            AssetIntegrationDisciplineAuthority.REAGENT_PREPARATION_BENCH_FIXTURE,
            AssetIntegrationDisciplineAuthority.DISTILLATION_COLUMN_FIXTURE,
            AssetIntegrationDisciplineAuthority.FUME_HOOD_FIXTURE,
            AssetIntegrationDisciplineAuthority.INJECTOR_FILLING_STATION_FIXTURE
    };

    enum Variant {
        MICRO_LAB(AssetIntegrationDisciplineAuthority.LABORATORY_ROOM_FIXTURE, 'K', "laboratory-room-fixture", "build_emm_micro_lab",
                "micro laboratory fixture", "sample tray, cracked optics, cogitator faceplate, and locked data slots",
                "operation-preview: sample assay, knowledge progress, lab staffing, and research handoff"),
        CRUDE_CHEM_BENCH(AssetIntegrationDisciplineAuthority.CRUDE_CHEM_BENCH_FIXTURE, 'L', "crude-chem-bench", "build_crude_chem_bench",
                "crude chem bench", "stained working surface, reagent bottle scars, pipe coupling, and a waste trap with opinions",
                "operation-preview: unsafe reagent mixing, crude batches, contamination risk, and waste output"),
        REAGENT_PREP_BENCH(AssetIntegrationDisciplineAuthority.REAGENT_PREPARATION_BENCH_FIXTURE, 'L', "reagent-preparation-bench", "build_reagent_preparation_bench",
                "reagent preparation bench", "labeled vial rack, measuring glass, sealed tray, and ordinary chemical obedience",
                "operation-preview: legal reagent preparation, medical precursors, stable compounds, and recipe handoff"),
        DISTILLATION_COLUMN(AssetIntegrationDisciplineAuthority.DISTILLATION_COLUMN_FIXTURE, 'D', "distillation-column", "build_distillation_column",
                "distillation column", "coil stack, sealed column, pressure hose, drip pan, and vapor lines that know too much",
                "operation-preview: solvent separation, alcohol refining, volatile heat/noise, and liquid-process handoff"),
        FUME_HOOD(AssetIntegrationDisciplineAuthority.FUME_HOOD_FIXTURE, 'F', "fume-hood", "build_fume_hood",
                "fume hood", "filter housing, armored sash, negative-pressure vent, and a residue tray that should not be touched",
                "operation-preview: toxic handling, aerosol work, volatile safety, and hazard-mitigation handoff"),
        INJECTOR_FILLING_STATION(AssetIntegrationDisciplineAuthority.INJECTOR_FILLING_STATION_FIXTURE, 'I', "injector-filling-station", "build_injector_filling_station",
                "injector filling station", "ampoule rack, dosing jig, sterile clips, and little labels pretending dose precision is mercy",
                "operation-preview: ampoule filling, injector packaging, medicae precursors, and dosing-station handoff");

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

    private LabChemicalFixtureAuthority() {}

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
        return v == null ? 'K' : v.glyph;
    }

    static String chooseRoomFixtureType(World w, RoomProfile rp, int roomId, Random r) {
        if (r == null) r = new Random(0);
        ZoneType z = w == null ? null : w.zoneType;
        String text = roomText(rp).toLowerCase(Locale.ROOT);
        if (contains(text, "injector", "ampoule", "dosing", "aid station", "military aid", "combat drug", "stimm")) return AssetIntegrationDisciplineAuthority.INJECTOR_FILLING_STATION_FIXTURE;
        if (contains(text, "fume", "toxin", "aerosol", "volatile", "security lab", "riot", "poison")) return AssetIntegrationDisciplineAuthority.FUME_HOOD_FIXTURE;
        if (contains(text, "distill", "still", "solvent", "amasec", "column", "refining", "cellar")) return AssetIntegrationDisciplineAuthority.DISTILLATION_COLUMN_FIXTURE;
        if (contains(text, "reagent", "preparation", "compound", "clinic", "medicae", "apothec", "stable")) return AssetIntegrationDisciplineAuthority.REAGENT_PREPARATION_BENCH_FIXTURE;
        if (contains(text, "chem", "chemical", "drug", "narcotic", "slurry", "gang", "sump")) return AssetIntegrationDisciplineAuthority.CRUDE_CHEM_BENCH_FIXTURE;
        if (contains(text, "laboratorium", "laboratory", "sample", "research", "analysis", "assay")) {
            int roll = r.nextInt(100);
            if (z == ZoneType.MECHANICUS_FORGE_CLOISTER || z == ZoneType.MECHANICUS_RELIC_DUCT) {
                if (roll < 45) return AssetIntegrationDisciplineAuthority.LABORATORY_ROOM_FIXTURE;
                if (roll < 70) return AssetIntegrationDisciplineAuthority.REAGENT_PREPARATION_BENCH_FIXTURE;
                if (roll < 88) return AssetIntegrationDisciplineAuthority.FUME_HOOD_FIXTURE;
                return AssetIntegrationDisciplineAuthority.DISTILLATION_COLUMN_FIXTURE;
            }
            if (z == ZoneType.SUMP_MARKET || z == ZoneType.MUTANT_SEWER_CAMP || z == ZoneType.TRASH_WARREN || z == ZoneType.SEWER_CONDUIT) {
                if (roll < 55) return AssetIntegrationDisciplineAuthority.CRUDE_CHEM_BENCH_FIXTURE;
                if (roll < 78) return AssetIntegrationDisciplineAuthority.DISTILLATION_COLUMN_FIXTURE;
                return AssetIntegrationDisciplineAuthority.FUME_HOOD_FIXTURE;
            }
            if (z == ZoneType.IMPERIAL_GUARD_BILLET || z == ZoneType.ARBITES_PRECINCT_EDGE) {
                if (roll < 45) return AssetIntegrationDisciplineAuthority.INJECTOR_FILLING_STATION_FIXTURE;
                if (roll < 70) return AssetIntegrationDisciplineAuthority.REAGENT_PREPARATION_BENCH_FIXTURE;
                return AssetIntegrationDisciplineAuthority.FUME_HOOD_FIXTURE;
            }
            if (roll < 35) return AssetIntegrationDisciplineAuthority.LABORATORY_ROOM_FIXTURE;
            if (roll < 58) return AssetIntegrationDisciplineAuthority.REAGENT_PREPARATION_BENCH_FIXTURE;
            if (roll < 76) return AssetIntegrationDisciplineAuthority.CRUDE_CHEM_BENCH_FIXTURE;
            if (roll < 90) return AssetIntegrationDisciplineAuthority.DISTILLATION_COLUMN_FIXTURE;
            return AssetIntegrationDisciplineAuthority.FUME_HOOD_FIXTURE;
        }
        return AssetIntegrationDisciplineAuthority.LABORATORY_ROOM_FIXTURE;
    }

    static String roomLabel(String type, RoomProfile rp, ZoneType z) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "unclassified room" : rp.name;
        String zone = z == null ? "Unknown zone" : z.label;
        return (v == null ? "Laboratory fixture" : title(v.label)) + " / " + room + " / " + zone;
    }

    static String roomStock(String type, RoomProfile rp, World w, int roomId, char under) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "room" : safe(rp.name);
        String semantic = AssetIntegrationDisciplineAuthority.semanticKeyForType(type);
        String token = v == null ? "laboratory-room-fixture" : v.token;
        return "room-fixture;lab-chemical-bucket;variant=" + token + ";roomId=" + roomId + ";room=" + room +
                ";handoff=operation-profile;semantic=" + safe(semantic) + ";under=" + (int)under;
    }

    static String inspectionLine(String type) {
        Variant v = variantFor(type);
        if (v == null) return "LABORATORY FIXTURE: sample slots, reagent marks, glassware, and an operation-profile handoff surface.";
        return "LABORATORY FIXTURE: " + v.inspection + ". " + v.operationPreview + ".";
    }

    static String interactionVerb(String type) {
        Variant v = variantFor(type);
        if (v == null) return "inspects a laboratory fixture.";
        switch (v) {
            case CRUDE_CHEM_BENCH: return "inspects a crude chem bench.";
            case REAGENT_PREP_BENCH: return "inspects a reagent preparation bench.";
            case DISTILLATION_COLUMN: return "inspects a distillation column.";
            case FUME_HOOD: return "inspects a fume hood.";
            case INJECTOR_FILLING_STATION: return "inspects an injector filling station.";
            default: return "inspects a micro laboratory fixture.";
        }
    }

    static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "laboratory fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "laboratory fixture" : s;
    }

    static String auditSummary() {
        return "labChemicalFixture version=" + VERSION + " promotedTypes=" + CANONICAL_TYPES.length +
                " rule=canonical lab/chemical variants; operation-profile handoff only";
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

    private static String title(String s) { return s == null || s.isBlank() ? "Laboratory fixture" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static String safe(String s) { return s == null ? "" : s.replace(';', ','); }
}
