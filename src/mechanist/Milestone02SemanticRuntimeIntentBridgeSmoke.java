package mechanist;

import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetRegistry;
import mechanist.assets.AssetType;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Smoke for the live item/object bridges into strict semantic render families. */
final class Milestone02SemanticRuntimeIntentBridgeSmoke {
    public static void main(String[] args) throws Exception {
        AssetRegistry registry = registry();

        requireIntent(SemanticRenderIntentAuthority.itemIntent("Field trauma bandage"),
                SemanticRenderAssetResolver.RenderIntent.MEDICAL_ITEM_ICON, "medical item intent");
        requireIntent(SemanticRenderIntentAuthority.itemIntent("Combat stimulant dose"),
                SemanticRenderAssetResolver.RenderIntent.DRUG_ITEM_ICON, "drug item intent");
        requireIntent(SemanticRenderIntentAuthority.itemIntent("Machine bearing component"),
                SemanticRenderAssetResolver.RenderIntent.INDUSTRIAL_COMPONENT_ITEM_ICON, "component item intent");
        requireIntent(SemanticRenderIntentAuthority.itemIntent("Encrypted datapad"),
                SemanticRenderAssetResolver.RenderIntent.DATA_DEVICE_ITEM_ICON, "data item intent");
        requireIntent(SemanticRenderIntentAuthority.itemIntent("Export trade goods"),
                SemanticRenderAssetResolver.RenderIntent.TRADE_GOOD_ITEM_ICON, "trade item intent");

        requireResolved(SemanticRenderIntentAuthority.resolveItemFamily(registry, "Field trauma bandage"),
                "ITM-MED1", "medical family resolution");
        requireResolved(SemanticRenderIntentAuthority.resolveItemFamily(registry, "Combat stimulant dose"),
                "ITM-DRG1", "drug family resolution");
        requireResolved(SemanticRenderIntentAuthority.resolveItemFamily(registry, "Machine bearing component"),
                "ITM-IND1", "component family resolution");
        requireResolved(SemanticRenderIntentAuthority.resolveItemFamily(registry, "Encrypted datapad"),
                "ITM-DAT1", "data family resolution");
        requireResolved(SemanticRenderIntentAuthority.resolveItemFamily(registry, "Export trade goods"),
                "ITM-TRD1", "trade family resolution");

        requireIntent(SemanticRenderIntentAuthority.objectIntent("Open service door"),
                SemanticRenderAssetResolver.RenderIntent.DOOR_OPEN, "open door intent");
        requireIntent(SemanticRenderIntentAuthority.objectIntent("Sealed reinforced door"),
                SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED, "closed door intent");
        requireIntent(SemanticRenderIntentAuthority.objectIntent("Medical cabinet"),
                SemanticRenderAssetResolver.RenderIntent.MEDICAL_CABINET_CONTAINER, "medical cabinet intent");
        requireIntent(SemanticRenderIntentAuthority.objectIntent("Workshop table"),
                SemanticRenderAssetResolver.RenderIntent.WORKSHOP_TABLE, "workshop furniture intent");
        requireIntent(SemanticRenderIntentAuthority.objectIntent("Streetlight pole"),
                SemanticRenderAssetResolver.RenderIntent.STREETLIGHT_FIXTURE, "streetlight intent");

        requireResolved(SemanticRenderIntentAuthority.resolveObjectFamily(registry, "Open service door"),
                "OBJ-DOP1", "open door family resolution");
        requireResolved(SemanticRenderIntentAuthority.resolveObjectFamily(registry, "Sealed reinforced door"),
                "OBJ-DCL1", "closed door family resolution");
        requireResolved(SemanticRenderIntentAuthority.resolveObjectFamily(registry, "Medical cabinet"),
                "OBJ-MED1", "medical cabinet family resolution");
        requireResolved(SemanticRenderIntentAuthority.resolveObjectFamily(registry, "Workshop table"),
                "OBJ-WRK1", "workshop furniture family resolution");
        requireResolved(SemanticRenderIntentAuthority.resolveObjectFamily(registry, "Streetlight pole"),
                "OBJ-LGT1", "streetlight family resolution");

        require(SemanticRenderIntentAuthority.itemIntent("Unclassified keepsake").isEmpty(),
                "unknown item text should not invent a semantic family");
        require(SemanticRenderIntentAuthority.objectIntent("Unclassified decoration").isEmpty(),
                "unknown object text should not invent a semantic family");
        requireContains(SemanticRenderIntentAuthority.auditSummary(), "authoredHintsRemainFirst=true",
                "authored-hint boundary");
        requireContains(SemanticRenderIntentAuthority.auditSummary(), "strictFamilyFallback=true",
                "strict family boundary");

        System.out.println("Milestone02SemanticRuntimeIntentBridgeSmoke PASS "
                + SemanticRenderIntentAuthority.VERSION);
    }

    private static AssetRegistry registry() throws Exception {
        Map<String, AssetMetadata> entries = new LinkedHashMap<>();
        put(entries, asset("ITM-MED1", AssetType.ITEM_ICON, "Medical Bandage",
                "assets/items/medical_bandage.png", "medical medkit bandage suture medicine icon"));
        put(entries, asset("ITM-DRG1", AssetType.ITEM_ICON, "Stimulant Dose",
                "assets/items/stimulant.png", "drug narcotic stimulant dose icon"));
        put(entries, asset("ITM-IND1", AssetType.ITEM_ICON, "Machine Component",
                "assets/items/component.png", "industrial component machine part bearing icon"));
        put(entries, asset("ITM-DAT1", AssetType.ITEM_ICON, "Data Device",
                "assets/items/datapad.png", "data device datapad terminal chip icon"));
        put(entries, asset("ITM-TRD1", AssetType.ITEM_ICON, "Trade Goods",
                "assets/items/trade_goods.png", "trade good goods commodity barter cargo merchandise wares"));

        put(entries, asset("OBJ-DOP1", AssetType.FIXTURE, "Open Door Variant",
                "assets/doors/open.png", "door open opened semantic state variant"));
        put(entries, asset("OBJ-DCL1", AssetType.FIXTURE, "Closed Door Variant",
                "assets/doors/closed.png", "door closed shut semantic state variant"));
        put(entries, asset("OBJ-MED1", AssetType.OBJECT, "Medical Cabinet",
                "assets/containers/medical_cabinet.png", "medical cabinet medicine cabinet clinic cabinet"));
        put(entries, asset("OBJ-WRK1", AssetType.FIXTURE, "Workshop Table",
                "assets/furniture/workshop_table.png", "workshop table workbench fabrication table"));
        put(entries, asset("OBJ-LGT1", AssetType.FIXTURE, "Streetlight Pole",
                "assets/infrastructure/streetlight.png", "streetlight fixture street light lamp post street lamp"));

        Constructor<AssetRegistry> ctor = AssetRegistry.class.getDeclaredConstructor(
                java.nio.file.Path.class, java.nio.file.Path.class, Map.class);
        ctor.setAccessible(true);
        return ctor.newInstance(java.nio.file.Path.of("."), null, entries);
    }

    private static AssetMetadata asset(String id, AssetType type, String name, String path, String description) {
        return new AssetMetadata(id, path, name, type, description);
    }

    private static void put(Map<String, AssetMetadata> entries, AssetMetadata asset) {
        entries.put(asset.id(), asset);
    }

    private static void requireIntent(Optional<SemanticRenderAssetResolver.RenderIntent> actual,
                                      SemanticRenderAssetResolver.RenderIntent expected,
                                      String label) {
        if (actual.isPresent() && actual.get() == expected) return;
        throw new AssertionError("Expected " + label + " to be " + expected + ": " + actual);
    }

    private static void requireResolved(Optional<String> actual, String expected, String label) {
        if (actual.isPresent() && expected.equals(actual.get())) return;
        throw new AssertionError("Expected " + label + " to resolve " + expected + ": " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void requireContains(String text, String expected, String label) {
        if (text != null && text.contains(expected)) return;
        throw new AssertionError("Expected " + label + " to contain '" + expected + "': " + text);
    }

    private Milestone02SemanticRuntimeIntentBridgeSmoke() { }
}
