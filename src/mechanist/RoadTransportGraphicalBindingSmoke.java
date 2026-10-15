package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

import java.awt.image.BufferedImage;

/**
 * Verifies that every parked road-vehicle class and both road-bay markers
 * bind to its own real, correctly typed compiled graphical asset. Must
 * never pass merely because a generic missing-art indicator was drawn.
 */
public final class RoadTransportGraphicalBindingSmoke {
    private RoadTransportGraphicalBindingSmoke() {}

    public static void main(String[] args) {
        if (AssetManager.registry().size() == 0) {
            throw new AssertionError("No packaged semantic image registry");
        }
        Object[][] cases = {
                {AssetIntegrationDisciplineAuthority.PARKED_CIVILIAN_CAR,
                        "vehicles_r01c01_", AssetType.MACHINE},
                {AssetIntegrationDisciplineAuthority.PARKED_CARGO_TRUCK,
                        "vehicles2_r02c02_", AssetType.MACHINE},
                {AssetIntegrationDisciplineAuthority.PARKED_UTILITY_BIKE,
                        "vehicles_r03c01_", AssetType.MACHINE},
                {AssetIntegrationDisciplineAuthority.PARKED_ARMORED_CAR,
                        "vehicles2_r01c01_", AssetType.MACHINE},
                {AssetIntegrationDisciplineAuthority.PARKED_TANK,
                        "vehicles2_r05c01_", AssetType.MACHINE},
                {AssetIntegrationDisciplineAuthority.PARKING_LOT_MARKER,
                        "road_infrastructure_r02c03_", AssetType.ROAD_TILE},
                {AssetIntegrationDisciplineAuthority.ROAD_VEHICLE_STAGING_MARKER,
                        "road_infrastructure_r02c05_", AssetType.ROAD_TILE}
        };
        for (Object[] test : cases) {
            MapObjectState object = new MapObjectState();
            object.type = (String) test[0];
            object.label = "Runtime authored vehicle art verification";
            object.stockState = "none";
            String id = ObjectSemanticAssetAuthority.runtimeAssetIdForMapObject(object)
                    .orElseThrow(() -> new AssertionError("Unbound road object " + object.type));
            require(!ObjectSemanticAssetAuthority.MISSING_RECOGNIZED_OBJECT_ID.equals(id),
                    "Missing authored sprite for road object " + object.type);
            AssetMetadata metadata = AssetManager.metadata(id)
                    .orElseThrow(() -> new AssertionError("Escaped asset registry " + id));
            require(metadata.type() == test[2],
                    "Wrong asset category for " + object.type + ": " + metadata.type());
            require(metadata.pathOrUri().toLowerCase(java.util.Locale.ROOT)
                            .contains((String) test[1]),
                    "Wrong atlas cell for " + object.type + " : " + metadata.pathOrUri());
            javax.swing.ImageIcon icon = AssetManager.getAsset(id);
            require(icon != null && !AssetManager.isMissingAssetIcon(icon)
                            && icon.getIconWidth() > 0 && icon.getIconHeight() > 0,
                    "Road object asset did not decode: " + object.type);
        }
        MapObjectState unknown = new MapObjectState();
        unknown.type = "unrelated-object-without-road-art";
        require(ObjectSemanticAssetAuthority.runtimeRoadTransportAssetId(unknown).isEmpty(),
                "Road mapping improperly hijacked unrelated map-object semantics");
        System.out.println("RoadTransportGraphicalBindingSmoke PASS bound=" + cases.length);
    }

    private static void require(boolean good, String message) {
        if (!good) throw new AssertionError(message);
    }
}
