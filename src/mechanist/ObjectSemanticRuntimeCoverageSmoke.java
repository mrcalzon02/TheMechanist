package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetType;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public final class ObjectSemanticRuntimeCoverageSmoke {
    public static void main(String[] args) throws Exception {
        if (AssetManager.registry().size() == 0) throw new AssertionError("active semantic asset registry was empty");
        int resolved = 0;
        for (String name : List.of("storage crate", "scrap workbench", "generator", "terminal",
                "light fixture", "motion sensor", "clinic stall", "reinforced wall panel")) {
            var id = ObjectSemanticAssetAuthority.runtimeAssetIdForName(name);
            if (id.isPresent()) {
                resolved++;
                if (AssetManager.metadata(id.get()).isEmpty()) {
                    throw new AssertionError("object resolution escaped registry: " + name + " -> " + id.get());
                }
            }
        }
        if (resolved == 0) throw new AssertionError("no representative object hint resolved");

        Field mapObjectTypesField = ObjectSemanticAssetAuthority.class.getDeclaredField("MAP_OBJECT_ASSET_TYPES");
        mapObjectTypesField.setAccessible(true);
        Object mapObjectTypesValue = mapObjectTypesField.get(null);
        if (!(mapObjectTypesValue instanceof Set<?> mapObjectTypes)) {
            throw new AssertionError("map object semantic family authority was not a Set");
        }
        for (AssetType structuralType : List.of(AssetType.WALL_TILE, AssetType.FLOOR_TILE, AssetType.CORRIDOR_TILE)) {
            if (mapObjectTypes.contains(structuralType)) {
                throw new AssertionError("map object semantic family admitted structural tile type: " + structuralType);
            }
        }
        for (AssetType objectType : List.of(AssetType.OBJECT, AssetType.FIXTURE, AssetType.MACHINE)) {
            if (!mapObjectTypes.contains(objectType)) {
                throw new AssertionError("map object semantic family lost required object type: " + objectType);
            }
        }
        if (!ObjectSemanticAssetAuthority.auditSummary().contains("mapObjectStructuralTiles=false")) {
            throw new AssertionError("map object structural-tile exclusion missing from semantic audit summary");
        }

        int semanticIntentCases = 0;
        int structuralIntentCandidates = 0;
        int allowedIntentResolutions = 0;
        for (String semantic : List.of("bulkhead door", "sealed hatch", "reinforced door", "security bulkhead hatch")) {
            var intent = SemanticRenderIntentAuthority.objectIntent(semantic);
            if (intent.isEmpty()) continue;
            semanticIntentCases++;

            var rawResolved = SemanticRenderIntentAuthority.resolve(AssetManager.registry(), intent.get());
            AssetType rawType = rawResolved.flatMap(AssetManager::metadata)
                    .map(metadata -> metadata.type())
                    .orElse(null);
            boolean rawStructural = rawType == AssetType.WALL_TILE
                    || rawType == AssetType.FLOOR_TILE
                    || rawType == AssetType.CORRIDOR_TILE;
            if (rawStructural) structuralIntentCandidates++;

            var mapResolved = ObjectSemanticAssetAuthority.runtimeAssetIdForMapObjectSemantic(semantic);
            if (mapResolved.isEmpty()) {
                throw new AssertionError("recognized map-object semantic intent escaped validation: " + semantic);
            }
            String id = mapResolved.get();
            if (rawStructural) {
                if (!ObjectSemanticAssetAuthority.MISSING_RECOGNIZED_OBJECT_ID.equals(id)) {
                    throw new AssertionError("map-object semantic intent admitted structural tile: "
                            + semantic + " -> " + id + " / " + rawType);
                }
                continue;
            }
            if (ObjectSemanticAssetAuthority.MISSING_RECOGNIZED_OBJECT_ID.equals(id)) continue;

            var metadata = AssetManager.metadata(id)
                    .orElseThrow(() -> new AssertionError("map-object semantic intent escaped registry: "
                            + semantic + " -> " + id));
            allowedIntentResolutions++;
            if (!mapObjectTypes.contains(metadata.type())) {
                throw new AssertionError("map-object semantic intent admitted non-object type: "
                        + semantic + " -> " + id + " / " + metadata.type());
            }
        }
        if (semanticIntentCases == 0) {
            throw new AssertionError("no door/hatch map-object semantic intent was recognized");
        }

        var floor = ObjectSemanticAssetAuthority.runtimeAssetIdForEditorPalette("floor", "bare underhive floor");
        var wall = ObjectSemanticAssetAuthority.runtimeAssetIdForEditorPalette("wall", "bulkhead wall");
        if (floor.isEmpty() || wall.isEmpty()) throw new AssertionError("editor floor/wall semantic resolution missing");

        var editorObject = ObjectSemanticAssetAuthority.runtimeAssetIdForEditorPalette("objects", "pillar");
        if (editorObject.isEmpty()) {
            throw new AssertionError("non-structural editor category escaped semantic family validation");
        }
        if (!ObjectSemanticAssetAuthority.MISSING_RECOGNIZED_OBJECT_ID.equals(editorObject.get())) {
            var editorMetadata = AssetManager.metadata(editorObject.get())
                    .orElseThrow(() -> new AssertionError("editor object resolution escaped registry: " + editorObject.get()));
            if (editorMetadata.type() == AssetType.WALL_TILE
                    || editorMetadata.type() == AssetType.FLOOR_TILE
                    || editorMetadata.type() == AssetType.CORRIDOR_TILE) {
                throw new AssertionError("non-structural editor category admitted structural tile: "
                        + editorObject.get() + " / " + editorMetadata.type());
            }
        }
        if (!ObjectSemanticAssetAuthority.auditSummary().contains("editorObjectStructuralTiles=false")) {
            throw new AssertionError("editor object structural-tile exclusion missing from semantic audit summary");
        }

        var streetlight = ObjectSemanticAssetAuthority.runtimeAssetIdForLightSemantic("streetlight");
        if (streetlight.isEmpty()) {
            throw new AssertionError("recognized streetlight semantic intent escaped light-family validation");
        }
        if (!ObjectSemanticAssetAuthority.MISSING_RECOGNIZED_OBJECT_ID.equals(streetlight.get())) {
            var lightMetadata = AssetManager.metadata(streetlight.get())
                    .orElseThrow(() -> new AssertionError("light semantic resolution escaped registry: " + streetlight.get()));
            if (lightMetadata.type() != AssetType.FIXTURE
                    && lightMetadata.type() != AssetType.OBJECT
                    && lightMetadata.type() != AssetType.MACHINE) {
                throw new AssertionError("light semantic intent admitted non-light family: "
                        + streetlight.get() + " / " + lightMetadata.type());
            }
        }

        var structuralLightIntent = ObjectSemanticAssetAuthority.runtimeAssetIdForLightSemantic("sealed bulkhead door");
        if (structuralLightIntent.isEmpty()
                || !ObjectSemanticAssetAuthority.MISSING_RECOGNIZED_OBJECT_ID.equals(structuralLightIntent.get())) {
            throw new AssertionError("light semantic boundary admitted structural door art: " + structuralLightIntent);
        }
        if (!ObjectSemanticAssetAuthority.auditSummary().contains("lightStructuralTiles=false")) {
            throw new AssertionError("light structural-tile exclusion missing from semantic audit summary");
        }

        var streetlightIntent = SemanticRenderIntentAuthority.objectIntent("streetlight")
                .orElseThrow(() -> new AssertionError("streetlight intent missing for stable-variety coverage"));
        for (long variantKey : List.of(0L, 1L, 2L, 17L, 65537L)) {
            var expected = SemanticRenderIntentAuthority.resolve(AssetManager.registry(), streetlightIntent, variantKey);
            var actual = ObjectSemanticAssetAuthority.runtimeAssetIdForLightSemantic("streetlight", variantKey);
            if (actual.isEmpty()) {
                throw new AssertionError("stable light variant path escaped recognized semantic intent for key " + variantKey);
            }
            String expectedId = expected.orElse(ObjectSemanticAssetAuthority.MISSING_RECOGNIZED_OBJECT_ID);
            if (!expectedId.equals(actual.get())) {
                throw new AssertionError("light stable variant key was not forwarded to semantic resolver: key="
                        + variantKey + " expected=" + expectedId + " actual=" + actual.get());
            }
        }

        long alphaKey = ObjectSemanticAssetAuthority.stableLightVariantKey(
                "streetlight amber group-alpha light fixture");
        long alphaRepeatKey = ObjectSemanticAssetAuthority.stableLightVariantKey(
                "streetlight amber group-alpha light fixture");
        long betaKey = ObjectSemanticAssetAuthority.stableLightVariantKey(
                "streetlight amber group-beta light fixture");
        if (alphaKey != alphaRepeatKey) {
            throw new AssertionError("stable light semantic identity produced a non-deterministic variant key");
        }
        if (alphaKey == betaKey) {
            throw new AssertionError("distinct representative light semantic identities collapsed to one variant key");
        }
        if (!ObjectSemanticAssetAuthority.auditSummary().contains("lightStableVariety=true")) {
            throw new AssertionError("stable light variety missing from semantic audit summary");
        }

        System.out.println("ObjectSemanticRuntimeCoverageSmoke PASS registry=" + AssetManager.registry().size()
                + " representativeResolved=" + resolved + " mapObjectTypes=" + mapObjectTypes.size()
                + " semanticIntentCases=" + semanticIntentCases
                + " structuralIntentCandidates=" + structuralIntentCandidates
                + " allowedIntentResolutions=" + allowedIntentResolutions
                + " structuralTilesExcluded=true editorObjectStructuralTilesExcluded=true"
                + " lightStructuralTilesExcluded=true lightStableVarietyCovered=true"
                + " authority=" + ObjectSemanticAssetAuthority.VERSION);
    }
    private ObjectSemanticRuntimeCoverageSmoke() {}
}
