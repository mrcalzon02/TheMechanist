package mechanist;

import mechanist.assets.AssetManager;
import mechanist.assets.AssetMetadata;
import mechanist.assets.AssetType;

/** Checks specific real packaged graphics for parks, radios and pict displays. */
public final class PublicFixtureGraphicalBindingSmoke {
    private PublicFixtureGraphicalBindingSmoke() {}

    public static void main(String[] args) {
        String[][] fixtures = {
                {AssetIntegrationDisciplineAuthority.PARK_OPEN_SPACE, "Roadside park", "", "parks-things_r03c01_"},
                {AssetIntegrationDisciplineAuthority.PARK_OPEN_SPACE, "Pocket park", "", "parks-things_r03c02_"},
                {AssetIntegrationDisciplineAuthority.PARK_OPEN_SPACE, "Civic breathing space", "", "parks-things_r04c03_"},
                {"broadcast-device", "Roadside broadcast receiver", "radio receiver", "service_items_r05c01_"},
                {"broadcast-device", "Public pict screen", "pict display", "service_items_r03c03_"}
        };
        for (String[] fixture : fixtures) {
            MapObjectState object = new MapObjectState();
            object.type = fixture[0];
            object.label = fixture[1];
            object.stockState = fixture[2];
            String id = ObjectSemanticAssetAuthority.runtimeAssetIdForMapObject(object)
                    .orElseThrow(() -> new AssertionError("Unbound civic fixture: " + object.label));
            require(!ObjectSemanticAssetAuthority.MISSING_RECOGNIZED_OBJECT_ID.equals(id),
                    "Missing compiled fixture art: " + object.label);
            AssetMetadata meta = AssetManager.metadata(id)
                    .orElseThrow(() -> new AssertionError("Escaped image registry: " + object.label));
            require(meta.type() == AssetType.OBJECT
                            && meta.pathOrUri().toLowerCase(java.util.Locale.ROOT).contains(fixture[3]),
                    "Incorrect authored atlas image for " + object.label + ": " + meta.pathOrUri());
            javax.swing.ImageIcon image = AssetManager.getAsset(id);
            require(image != null && !AssetManager.isMissingAssetIcon(image)
                            && image.getIconWidth() > 0 && image.getIconHeight() > 0,
                    "Image was absent or undecodable: " + object.label);
        }
        MapObjectState unrelated = new MapObjectState();
        unrelated.type = "unrelated-machine";
        require(ObjectSemanticAssetAuthority.runtimePublicFixtureAssetId(unrelated).isEmpty(),
                "Civic image mapping hijacked unrelated objects");
        System.out.println("PublicFixtureGraphicalBindingSmoke PASS fixtures=" + fixtures.length);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
