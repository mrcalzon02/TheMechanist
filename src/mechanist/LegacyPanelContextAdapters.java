package mechanist;

import java.util.ArrayList;

/**
 * Adapter layer for retiring direct GamePanel parameters.
 *
 * These wrappers let subsystems migrate from `GamePanel` to narrow context
 * interfaces without forcing the temporary bridge class itself to grow into a
 * second monolith.  Each adapter should disappear once the corresponding
 * subsystem owns a real context object.
 */
final class LegacyPanelContextAdapters {
    private LegacyPanelContextAdapters() {}

    static PlayerCommandContext playerCommand(GamePanel panel) { return new PlayerCommandPanelAdapter(panel); }
    static UiRenderContext uiRender(GamePanel panel) { return new UiRenderPanelAdapter(panel); }
    static WorldRuntimeContext worldRuntime(GamePanel panel) { return new WorldRuntimePanelAdapter(panel); }
    static InventoryPersistenceContext inventory(GamePanel panel) { return new InventoryPersistencePanelAdapter(panel); }
    static AdminCommandContext adminCommand(GamePanel panel) { return new AdminCommandPanelAdapter(panel); }
}

final class PlayerCommandPanelAdapter implements PlayerCommandContext {
    private final GamePanel panel;
    PlayerCommandPanelAdapter(GamePanel panel) { this.panel = panel; }
    public World commandWorld() { return panel == null ? null : panel.world; }
    public int playerX() { return panel == null ? 0 : panel.playerX; }
    public int playerY() { return panel == null ? 0 : panel.playerY; }
    public void queueOrExecuteMovementInput(int dx, int dy) { if (panel != null) panel.queueOrExecuteMovementInput(dx, dy); }
    public void logEvent(String line) { if (panel != null) panel.logEvent(line); }
}

final class UiRenderPanelAdapter implements UiRenderContext {
    private final GamePanel panel;
    UiRenderPanelAdapter(GamePanel panel) { this.panel = panel; }
    public GameOptions renderOptions() { return panel == null ? new GameOptions() : panel.options; }
    public String accessibleNarration() { return panel == null ? "" : panel.lastAccessibleNarration; }
    public void setAccessibleNarration(String narration) { if (panel != null) panel.lastAccessibleNarration = narration == null ? "" : narration; }
    public void requestUiRepaint() { if (panel != null) panel.repaint(); }
}

final class WorldRuntimePanelAdapter implements WorldRuntimeContext {
    private final GamePanel panel;
    WorldRuntimePanelAdapter(GamePanel panel) { this.panel = panel; }
    public World runtimeWorld() { return panel == null ? null : panel.world; }
    public int runtimeTurn() { return panel == null ? 0 : panel.turn; }
    public int runtimeWorldTurn() { return panel == null ? 0 : panel.worldTurn; }
    public void advanceRuntimeTurn(String eventLine) { if (panel != null) panel.advanceTurnBody(eventLine); }
}

final class InventoryPersistencePanelAdapter implements InventoryPersistenceContext {
    private final GamePanel panel;
    InventoryPersistencePanelAdapter(GamePanel panel) { this.panel = panel; }
    public ArrayList<String> carriedInventory() { return panel == null ? new ArrayList<>() : panel.inventory; }
    public int carriedScript() { return panel == null ? 0 : panel.carriedScript; }
    public int bankedScript() { return panel == null ? 0 : panel.baseStashedScript; }
    public void addImperialScript(int amount) { if (panel != null) panel.addImperialScript(amount); }
}

final class AdminCommandPanelAdapter implements AdminCommandContext {
    private final GamePanel panel;
    AdminCommandPanelAdapter(GamePanel panel) { this.panel = panel; }
    public World adminWorld() { return panel == null ? null : panel.world; }
    public void adminLog(String line) { if (panel != null) panel.logEvent(line); }
    public void markAdminDirty(String reason) {
        if (panel != null) panel.markLocalDirtyRegion(reason, panel.playerX, panel.playerY, 8, true, true, true, false);
    }
}
