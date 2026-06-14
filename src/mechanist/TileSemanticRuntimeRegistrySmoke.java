package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

        List<Expectation> required = List.of(
                expected("floor_bare_underhive_v1", Set.of(AssetType.FLOOR_TILE), "floor"),
                expected("floor_industrial_room_v1", Set.of(AssetType.FLOOR_TILE), "industrial"),
                expected("floor_sewer_room_v1", Set.of(AssetType.FLOOR_TILE), "sewer", "noble"),
                expected("floor_noble_room_v1", Set.of(AssetType.FLOOR_TILE), "noble", "sewer", "sump"),
                expected("floor_noble_corridor_north_south", Set.of(AssetType.CORRIDOR_TILE, AssetType.FLOOR_TILE), "noble", "sewer", "sump"),
                expected("wall_bulkhead_v1", Set.of(AssetType.WALL_TILE), "bulkhead"),
                expected("wall_sewer_bulkhead_v1", Set.of(AssetType.WALL_TILE), "sewer", "noble"),
                expected("wall_noble_bulkhead_v1", Set.of(AssetType.WALL_TILE), "noble", "sewer", "sump"),
                expected("road_north_south_v1", Set.of(AssetType.ROAD_TILE), "road"),
                expected("door_standard", Set.of(AssetType.FIXTURE), "closed", "open"),
                expected("door_archway", Set.of(AssetType.FIXTURE), "open", "closed")
        );
        for (Expectation expectation : required) {
            String id = TileSemanticAssetAuthority.assetIdForAlias(expectation.alias)
                    .orElseThrow(() -> new AssertionError("representative alias did not resolve: " + expectation.alias));
            AssetMetadata metadata = AssetManager.metadata(id)
                    .orElseThrow(() -> new AssertionError("representative alias resolved outside registry: "
                            + expectation.alias + " -> " + id));
            if (!expectation.types.contains(metadata.type())) {
                throw new AssertionError("representative alias resolved to wrong asset type: "
                        + expectation.alias + " -> " + metadata.type() + " / " + id);
            }
            String semantic = (metadata.name() + " " + metadata.pathOrUri() + " "
                    + metadata.semanticDescription()).toLowerCase(Locale.ROOT);
            if (!semantic.contains(expectation.requiredTheme)) {
                throw new AssertionError("representative alias lost its theme: "
                        + expectation.alias + " -> " + id + " / " + metadata.name());
            }
            for (String forbidden : expectation.forbiddenThemes) {
                if (semantic.contains(forbidden)) {
                    throw new AssertionError("representative alias crossed theme families: "
                            + expectation.alias + " -> " + id + " / " + metadata.name());
                }
            }
        }

        String closedDoor = TileSemanticAssetAuthority.assetIdForAlias("door_standard").orElseThrow();
        String openDoor = TileSemanticAssetAuthority.assetIdForAlias("door_archway").orElseThrow();
        if (closedDoor.equals(openDoor)) {
            throw new AssertionError("open and closed door states resolved to the same asset: " + closedDoor);
        }
        assertDoorResolverAgreement(SemanticRenderAssetResolver.RenderIntent.DOOR_CLOSED, closedDoor);
        assertDoorResolverAgreement(SemanticRenderAssetResolver.RenderIntent.DOOR_OPEN, openDoor);
        assertStreetlightResolution();
        assertAssetAuditDevRoomCycling();

        System.out.println("TileSemanticRuntimeRegistrySmoke PASS registry="
                + AssetManager.registry().size()
                + " aliases=" + TileSemanticAssetAuthority.mappingCount()
                + " resolved=" + TileSemanticAssetAuthority.resolvedMappingCount());
    }

    private static Expectation expected(String alias, Set<AssetType> types,
                                        String requiredTheme, String... forbiddenThemes) {
        return new Expectation(alias, types, requiredTheme, List.of(forbiddenThemes));
    }

    private static void assertDoorResolverAgreement(SemanticRenderAssetResolver.RenderIntent intent,
                                                    String expectedId) {
        SemanticRenderAssetResolver.Resolution resolution =
                SemanticRenderAssetResolver.resolve(AssetManager.registry(), intent);
        if (!resolution.found() || !expectedId.equals(resolution.assetIdOrMissing())) {
            throw new AssertionError(intent + " resolver disagreed with live tile alias: expected "
                    + expectedId + " but got " + resolution.assetIdOrMissing());
        }
    }

    private static void assertStreetlightResolution() {
        SemanticRenderAssetResolver.Resolution resolution = SemanticRenderAssetResolver.resolve(
                AssetManager.registry(), SemanticRenderAssetResolver.RenderIntent.STREETLIGHT_FIXTURE);
        if (!resolution.found() || resolution.asset.type() != AssetType.FIXTURE) {
            throw new AssertionError("streetlight did not resolve to infrastructure fixture art: "
                    + resolution.assetIdOrMissing());
        }
        String semantic = (resolution.asset.name() + " " + resolution.asset.pathOrUri() + " "
                + resolution.asset.semanticDescription()).toLowerCase(Locale.ROOT);
        if (!semantic.contains("streetlight") || semantic.contains("system inventory")
                || semantic.contains("item icon") || semantic.contains("ui icon")) {
            throw new AssertionError("streetlight resolved through a forbidden semantic surface: "
                    + resolution.assetIdOrMissing());
        }
    }

    private static void assertAssetAuditDevRoomCycling() {
        World room = AssetAuditDevRoomAuthority.build(0xA55E7L);
        if (!AssetAuditDevRoomAuthority.isDevRoom(room)) {
            throw new AssertionError("asset audit dev room was not registered");
        }
        int x = 4;
        int y = 4;
        CompiledTileDescriptor descriptor = TileDataCompilationAuthority.resolve(room, x, y, room.tiles[x][y]);
        List<String> candidates = AssetAuditDevRoomAuthority.candidateAssetIds(descriptor);
        if (candidates.size() < 2) {
            throw new AssertionError("asset audit floor tile did not expose a useful cycle: " + candidates);
        }
        String before = AssetAuditDevRoomAuthority.assetIdFor(room, x, y, descriptor);
        AssetAuditDevRoomAuthority.cycle(room, x, y, 1);
        String after = AssetAuditDevRoomAuthority.assetIdFor(room, x, y, descriptor);
        if (after == null || after.equals(before) || AssetManager.metadata(after).isEmpty()) {
            throw new AssertionError("asset audit tile cycle did not install a valid manual override: " + before + " -> " + after);
        }
    }

    private record Expectation(String alias, Set<AssetType> types,
                               String requiredTheme, List<String> forbiddenThemes) {}

    private TileSemanticRuntimeRegistrySmoke() {}
}
