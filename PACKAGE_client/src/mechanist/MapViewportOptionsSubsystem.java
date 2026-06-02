package mechanist;

final class MapViewportOptionsSubsystem {
    private MapViewportOptionsSubsystem() {
    }

    static void cycleMapTileSize(GamePanel panel) {
        panel.options.mapTileSizeIndex = Math.floorMod(panel.options.mapTileSizeIndex + 1, GameOptions.MAP_TILE_SIZE_LABELS.length);
        panel.options.save();
        panel.sounds.play("tab", panel.options);
        panel.logEvent("Map tile size now " + panel.options.mapTileSizeLabel() + " (" + panel.options.mapTilePixelSize() + "px before GUI scale).");
        panel.repaint();
    }

    static boolean worldZoomControlActive(GamePanel panel) {
        if (panel.screen == GamePanel.Screen.SECTOR_AUDIT) return panel.auditWorld != null;
        return panel.world != null && (panel.screen == GamePanel.Screen.GAME ||
                (panel.screen == GamePanel.Screen.PANEL &&
                        (panel.panelMode == GamePanel.PanelMode.LOOK ||
                                panel.panelMode == GamePanel.PanelMode.COMBAT ||
                                panel.panelMode == GamePanel.PanelMode.INTERACT ||
                                panel.panelMode == GamePanel.PanelMode.SCAVENGE ||
                                panel.panelMode == GamePanel.PanelMode.BUILD ||
                                panel.panelMode == GamePanel.PanelMode.WORKBENCH)));
    }

    static void changeWorldZoom(GamePanel panel, int delta, String source) {
        int before = panel.options.worldZoomIndex;
        panel.options.worldZoomIndex = Math.max(0, Math.min(GameOptions.WORLD_ZOOM_LABELS.length - 1, panel.options.worldZoomIndex + delta));
        if (panel.options.worldZoomIndex != before) {
            panel.options.save();
            panel.sounds.play("tab", panel.options);
            panel.logEvent("Viewport zoom " + panel.options.worldZoomLabel() + " via " + source + ".");
            DebugLog.audit("VIEWPORT_ZOOM", "source=" + source + " index=" + panel.options.worldZoomIndex + " label=" + panel.options.worldZoomLabel() + " mode=" + panel.screen + "/" + panel.panelMode + " state=" + panel.stateSummary());
        } else {
            panel.logEvent("Viewport zoom already at " + panel.options.worldZoomLabel() + ".");
        }
        panel.repaint();
    }
}
