package mechanist;

import mechanist.assets.AssetManager;
import java.util.List;

public final class ObjectSemanticRuntimeCoverageSmoke {
    public static void main(String[] args) {
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
        var floor = ObjectSemanticAssetAuthority.runtimeAssetIdForEditorPalette("floor", "bare underhive floor");
        var wall = ObjectSemanticAssetAuthority.runtimeAssetIdForEditorPalette("wall", "bulkhead wall");
        if (floor.isEmpty() || wall.isEmpty()) throw new AssertionError("editor floor/wall semantic resolution missing");
        System.out.println("ObjectSemanticRuntimeCoverageSmoke PASS registry=" + AssetManager.registry().size()
                + " representativeResolved=" + resolved + " authority=" + ObjectSemanticAssetAuthority.VERSION);
    }
    private ObjectSemanticRuntimeCoverageSmoke() {}
}
