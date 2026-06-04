package mechanist;

/** @deprecated Use ViewportAssetOptionsRuntime. */
@Deprecated
public class LayerG {
    public LayerG() {}

    static void cycleWindowMode(GamePanel panel) { ViewportAssetOptionsRuntime.cycleWindowMode(panel); }
    static void changeResolution(GamePanel panel, int delta) { ViewportAssetOptionsRuntime.changeResolution(panel, delta); }
    static void applyWindowMode(GamePanel panel) { ViewportAssetOptionsRuntime.applyWindowMode(panel); }
    static void cycleArtQuality(GamePanel panel) { ViewportAssetOptionsRuntime.cycleArtQuality(panel); }
    static void chooseGeneratedAssetPayloadRoot(GamePanel panel) { ViewportAssetOptionsRuntime.chooseGeneratedAssetPayloadRoot(panel); }
    static void clearGeneratedAssetPayloadRoot(GamePanel panel) { ViewportAssetOptionsRuntime.clearGeneratedAssetPayloadRoot(panel); }
    static void cycleMapTileSize(GamePanel panel) { ViewportAssetOptionsRuntime.cycleMapTileSize(panel); }
    static boolean worldZoomControlActive(GamePanel panel) { return ViewportAssetOptionsRuntime.worldZoomControlActive(panel); }
    static void changeWorldZoom(GamePanel panel, int delta, String source) { ViewportAssetOptionsRuntime.changeWorldZoom(panel, delta, source); }
}
