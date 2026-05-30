package mechanist;

public class LayerG {
    public LayerG() {}

    static void cycleWindowMode(GamePanel panel) {
        panel.logEvent(OptionsBoundaryAuthority.cycleWindowMode(panel.options));
        panel.repaint();
    }

    static void changeResolution(GamePanel panel, int delta) {
        panel.logEvent(OptionsBoundaryAuthority.changeResolution(panel.options, delta));
        panel.repaint();
    }

    static void applyWindowMode(GamePanel panel) {
        LayerD.applyWindowMode(panel);
    }

    static void cycleArtQuality(GamePanel panel) {
        panel.options.artQualityIndex = Math.floorMod(panel.options.artQualityIndex + 1, GameOptions.ART_QUALITY_LABELS.length);
        panel.options.applyGeneratedAssetRuntimeProperties();
        panel.options.save();
        AssetManager.reloadGeneratedAssetRuntime();
        panel.images.reloadArtQuality(panel.options);
        panel.sounds.play("tab", panel.options);
        panel.logEvent("Art quality cache now " + panel.options.artQualityLabel() + ".");
        panel.repaint();
    }

    static void chooseGeneratedAssetPayloadRoot(GamePanel panel) {
        GeneratedArtPayloadOptionsSubsystem.chooseGeneratedAssetPayloadRoot(panel);
    }

    static void clearGeneratedAssetPayloadRoot(GamePanel panel) {
        GeneratedArtPayloadOptionsSubsystem.clearGeneratedAssetPayloadRoot(panel);
    }

    static void cycleMapTileSize(GamePanel panel) {
        MapViewportOptionsSubsystem.cycleMapTileSize(panel);
    }

    static boolean worldZoomControlActive(GamePanel panel) {
        return MapViewportOptionsSubsystem.worldZoomControlActive(panel);
    }

    static void changeWorldZoom(GamePanel panel, int delta, String source) {
        MapViewportOptionsSubsystem.changeWorldZoom(panel, delta, source);
    }
}
