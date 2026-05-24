package mechanist;

import java.util.*;

/**
 * Runtime authority for the food / farm / bio production fixture bucket.
 *
 * Owns canonical agri-food and vat-surface variants, semantic art routing,
 * compact inspection lines, room-placement selection, and operation/service
 * handoff metadata without creating the full food economy.
 */
final class FoodBioProductionFixtureAuthority {
    static final String VERSION = "0.9.10ai";

    static final String[] CANONICAL_TYPES = {
            AssetIntegrationDisciplineAuthority.ALGAE_TANK_FIXTURE,
            AssetIntegrationDisciplineAuthority.HYDROPONICS_BED_FIXTURE,
            AssetIntegrationDisciplineAuthority.ANIMAL_PEN_FIXTURE,
            AssetIntegrationDisciplineAuthority.CLONING_VAT_FIXTURE,
            AssetIntegrationDisciplineAuthority.FUNGAL_GROW_TRAY_FIXTURE,
            AssetIntegrationDisciplineAuthority.REFRIGERATED_FOOD_STORE_FIXTURE,
            AssetIntegrationDisciplineAuthority.NUTRIENT_VAT_FIXTURE
    };

    enum Variant {
        ALGAE_TANK(AssetIntegrationDisciplineAuthority.ALGAE_TANK_FIXTURE, 'Y', "algae-tank", "feature_algae_tank",
                "algae tank fixture", "green culture tank, feed lines, filter valves, and a light rack trying to industrialize pond scum",
                "operation-preview: algae culture, soylens feedstock, water/nutrient handling, and vat-production handoff"),
        HYDROPONICS_BED(AssetIntegrationDisciplineAuthority.HYDROPONICS_BED_FIXTURE, 'Y', "hydroponics-bed", "feature_hydroponics_bed",
                "hydroponics bed fixture", "growth tray, root bath, nutrient sprayer, sun-lamp bracket, and carefully rationed optimism",
                "operation-preview: hydroponic crop stock, leaf bundles, grain trays, and agriculture-staffing handoff"),
        ANIMAL_PEN(AssetIntegrationDisciplineAuthority.ANIMAL_PEN_FIXTURE, 'A', "animal-pen", "feature_animal_pen",
                "animal pen fixture", "fenced stall, feed trough, drain grating, and claw marks filed under acceptable loss",
                "operation-preview: livestock holding, farm-beast care, meat/stock custody, and animal-handler handoff"),
        CLONING_VAT(AssetIntegrationDisciplineAuthority.CLONING_VAT_FIXTURE, 'v', "cloning-vat", "feature_cloning_vat",
                "cloning vat fixture", "sealed bio-vessel, nutrient hose, growth lamp, sample port, and a morality panel nobody reads",
                "operation-preview: bio-growth, tissue culture, medical-food crossover, and specialist-vat handoff"),
        FUNGAL_GROW_TRAY(AssetIntegrationDisciplineAuthority.FUNGAL_GROW_TRAY_FIXTURE, 'm', "fungal-grow-tray", "feature_hydroponics_bed",
                "fungal grow tray fixture", "damp stacked trays, spore cloth, compost bed, and heat-darkness calibrated by smell",
                "operation-preview: fungus culture, sump food, spore stock, and underhive agriculture handoff"),
        REFRIGERATED_STORE(AssetIntegrationDisciplineAuthority.REFRIGERATED_FOOD_STORE_FIXTURE, 'u', "refrigerated-food-store", "feature_refrigerator",
                "refrigerated food store fixture", "sealed cooler, frost-rimmed latch, ration tags, and enough hum to imply maintenance debt",
                "operation-preview: cold storage, spoilage control, ration custody, and pantry-service handoff"),
        NUTRIENT_VAT(AssetIntegrationDisciplineAuthority.NUTRIENT_VAT_FIXTURE, 'v', "nutrient-vat", "feature_algae_tank",
                "nutrient vat fixture", "slurry tank, agitator housing, portion valve, and a pipe color best not understood",
                "operation-preview: vat nutrient slurry, ration paste inputs, synthetic food base, and provisioning-vat handoff");

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

    private FoodBioProductionFixtureAuthority() {}

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
        return v == null ? 'Y' : v.glyph;
    }

    static String chooseRoomFixtureType(World w, RoomProfile rp, int roomId, Random r) {
        if (r == null) r = new Random(0);
        ZoneType z = w == null ? null : w.zoneType;
        String text = roomText(rp).toLowerCase(Locale.ROOT);
        if (contains(text, "refrigerator", "cooler", "cold store", "pantry", "freezer", "food storehouse", "ration store")) return AssetIntegrationDisciplineAuthority.REFRIGERATED_FOOD_STORE_FIXTURE;
        if (contains(text, "algae", "soylens", "reclamation tank", "green culture")) return AssetIntegrationDisciplineAuthority.ALGAE_TANK_FIXTURE;
        if (contains(text, "hydroponic", "greenhouse", "crop", "agri", "marsh-rice", "leaf", "sun-lamp", "orchard", "garden")) {
            if (z == ZoneType.SECTOR_GOVERNORS_MANSION || z == ZoneType.NOBLE_SERVICE_SPINE) return r.nextInt(100) < 70 ? AssetIntegrationDisciplineAuthority.HYDROPONICS_BED_FIXTURE : AssetIntegrationDisciplineAuthority.CLONING_VAT_FIXTURE;
            return AssetIntegrationDisciplineAuthority.HYDROPONICS_BED_FIXTURE;
        }
        if (contains(text, "fungus", "fungal", "mold", "spore", "sewer garden", "sump farm")) return AssetIntegrationDisciplineAuthority.FUNGAL_GROW_TRAY_FIXTURE;
        if (contains(text, "animal", "beast", "pen", "livestock", "farm beast", "stable")) return AssetIntegrationDisciplineAuthority.ANIMAL_PEN_FIXTURE;
        if (contains(text, "cloning", "clone", "tissue", "bio-vat", "gene", "growth vat")) return AssetIntegrationDisciplineAuthority.CLONING_VAT_FIXTURE;
        if (contains(text, "nutrient", "galley", "cafeteria", "canteen", "mess", "kitchen", "ration", "slurry", "paste", "porridge", "vat")) {
            int roll = r.nextInt(100);
            if (z == ZoneType.MECHANICUS_FORGE_CLOISTER || z == ZoneType.MECHANICUS_RELIC_DUCT) {
                if (roll < 62) return AssetIntegrationDisciplineAuthority.NUTRIENT_VAT_FIXTURE;
                if (roll < 82) return AssetIntegrationDisciplineAuthority.ALGAE_TANK_FIXTURE;
                return AssetIntegrationDisciplineAuthority.REFRIGERATED_FOOD_STORE_FIXTURE;
            }
            if (z == ZoneType.SUMP_MARKET || z == ZoneType.SEWER_CONDUIT || z == ZoneType.MUTANT_SEWER_CAMP || z == ZoneType.MUTANT_WARRENS || z == ZoneType.TRASH_WARREN) {
                if (roll < 55) return AssetIntegrationDisciplineAuthority.FUNGAL_GROW_TRAY_FIXTURE;
                if (roll < 75) return AssetIntegrationDisciplineAuthority.NUTRIENT_VAT_FIXTURE;
                return AssetIntegrationDisciplineAuthority.REFRIGERATED_FOOD_STORE_FIXTURE;
            }
            if (z == ZoneType.IMPERIAL_GUARD_BILLET) {
                if (roll < 55) return AssetIntegrationDisciplineAuthority.NUTRIENT_VAT_FIXTURE;
                if (roll < 78) return AssetIntegrationDisciplineAuthority.REFRIGERATED_FOOD_STORE_FIXTURE;
                return AssetIntegrationDisciplineAuthority.HYDROPONICS_BED_FIXTURE;
            }
            if (roll < 32) return AssetIntegrationDisciplineAuthority.NUTRIENT_VAT_FIXTURE;
            if (roll < 55) return AssetIntegrationDisciplineAuthority.REFRIGERATED_FOOD_STORE_FIXTURE;
            if (roll < 76) return AssetIntegrationDisciplineAuthority.HYDROPONICS_BED_FIXTURE;
            if (roll < 90) return AssetIntegrationDisciplineAuthority.ALGAE_TANK_FIXTURE;
            return AssetIntegrationDisciplineAuthority.ANIMAL_PEN_FIXTURE;
        }
        return AssetIntegrationDisciplineAuthority.HYDROPONICS_BED_FIXTURE;
    }

    static String roomLabel(String type, RoomProfile rp, ZoneType z) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "unclassified room" : rp.name;
        String zone = z == null ? "Unknown zone" : z.label;
        return (v == null ? "Food/bio fixture" : title(v.label)) + " / " + room + " / " + zone;
    }

    static String roomStock(String type, RoomProfile rp, World w, int roomId, char under) {
        Variant v = variantFor(type);
        String room = rp == null || rp.name == null ? "room" : safe(rp.name);
        String semantic = AssetIntegrationDisciplineAuthority.semanticKeyForType(type);
        String token = v == null ? "food-bio-fixture" : v.token;
        return "room-fixture;food-bio-bucket;variant=" + token + ";roomId=" + roomId + ";room=" + room +
                ";handoff=operation-profile;semantic=" + safe(semantic) + ";under=" + (int)under;
    }

    static String inspectionLine(String type) {
        Variant v = variantFor(type);
        if (v == null) return "FOOD/BIO FIXTURE: growth frame, storage casing, service tags, and food-production handoff metadata.";
        return "FOOD/BIO FIXTURE: " + v.inspection + ". " + v.operationPreview + ".";
    }

    static String interactionVerb(String type) {
        Variant v = variantFor(type);
        if (v == null) return "inspects a food production fixture.";
        switch (v) {
            case ALGAE_TANK: return "inspects an algae tank fixture.";
            case HYDROPONICS_BED: return "inspects a hydroponics bed fixture.";
            case ANIMAL_PEN: return "inspects an animal pen fixture.";
            case CLONING_VAT: return "inspects a cloning vat fixture.";
            case FUNGAL_GROW_TRAY: return "inspects a fungal grow tray fixture.";
            case REFRIGERATED_STORE: return "inspects a refrigerated food store.";
            case NUTRIENT_VAT: return "inspects a nutrient vat fixture.";
            default: return "inspects a food production fixture.";
        }
    }

    static String shortLabel(MapObjectState m) {
        String s = m == null || m.label == null ? "food/bio fixture" : m.label;
        int slash = s.indexOf('/');
        if (slash > 0) s = s.substring(0, slash).trim();
        return s.isBlank() ? "food/bio fixture" : s;
    }

    static String auditSummary() {
        return "foodBioProductionFixture version=" + VERSION + " promotedTypes=" + CANONICAL_TYPES.length +
                " rule=canonical agri-food/bio variants; operation-profile handoff only";
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

    private static String title(String s) { return s == null || s.isBlank() ? "Food/bio fixture" : Character.toUpperCase(s.charAt(0)) + s.substring(1); }
    private static String safe(String s) { return s == null ? "" : s.replace(';', ','); }
}
