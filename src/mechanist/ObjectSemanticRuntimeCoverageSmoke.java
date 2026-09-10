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

        var floor = ObjectSemanticAssetAuthority.runtimeAssetIdForEditorPalette("floor", "bare underhive floor");
        var wall = ObjectSemanticAssetAuthority.runtimeAssetIdForEditorPalette("wall", "bulkhead wall");
        if (floor.isEmpty() || wall.isEmpty()) throw new AssertionError("editor floor/wall semantic resolution missing");
        System.out.println("ObjectSemanticRuntimeCoverageSmoke PASS registry=" + AssetManager.registry().size()
                + " representativeResolved=" + resolved + " mapObjectTypes=" + mapObjectTypes.size()
                + " structuralTilesExcluded=true authority=" + ObjectSemanticAssetAuthority.VERSION);
    }
    private ObjectSemanticRuntimeCoverageSmoke() {}
}
