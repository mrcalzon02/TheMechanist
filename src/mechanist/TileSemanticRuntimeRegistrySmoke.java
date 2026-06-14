package mechanist;

import mechanist.assets.AssetManager;

import java.util.List;
import java.util.Map;

/** Verifies that tile aliases never publish IDs absent from the active runtime registry. */
public final class TileSemanticRuntimeRegistrySmoke {
    public static void main(String[] args) {
        if (AssetManager.registry().size() == 0) {
            throw new AssertionError("active semantic asset registry was empty");
        }

        Map<String, String> resolved = TileSemanticAssetAuthority.auditResolvedAliasMap();
        if (resolved.isEmpty()) {
            throw new AssertionError("no tile aliases resolved against the active registry");
        }

        for (Map.Entry<String, String> entry : resolved.entrySet()) {
            if (AssetManager.metadata(entry.getValue()).isEmpty()) {
                throw new AssertionError("alias published a missing registry ID: "
                        + entry.getKey() + " -> " + entry.getValue());
            }
        }

        List<String> required = List.of(
                "floor_bare_underhive_v1",
                "floor_industrial_room_v1",
                "floor_sewer_room_v1",
                "wall_bulkhead_v1",
                "wall_sewer_bulkhead_v1",
                "road_north_south_v1",
                "door_standard"
        );
        for (String alias : required) {
            String id = TileSemanticAssetAuthority.assetIdForAlias(alias)
                    .orElseThrow(() -> new AssertionError("representative alias did not resolve: " + alias));
            if (AssetManager.metadata(id).isEmpty()) {
                throw new AssertionError("representative alias resolved outside registry: " + alias + " -> " + id);
            }
        }

        System.out.println("TileSemanticRuntimeRegistrySmoke PASS registry="
                + AssetManager.registry().size()
                + " aliases=" + TileSemanticAssetAuthority.mappingCount()
                + " resolved=" + TileSemanticAssetAuthority.resolvedMappingCount());
    }

    private TileSemanticRuntimeRegistrySmoke() {}
}
