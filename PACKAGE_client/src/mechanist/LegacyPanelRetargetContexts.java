package mechanist;

import java.util.ArrayList;

/**
 * Narrow context interfaces for retiring the temporary GamePanel bridge.
 *
 * New code should prefer these interfaces, or a more specific domain context,
 * instead of accepting the legacy GamePanel compatibility type.  The bridge may
 * implement them during transition while callers are retargeted one subsystem at
 * a time.
 */
interface PlayerCommandContext {
    World commandWorld();
    int playerX();
    int playerY();
    void queueOrExecuteMovementInput(int dx, int dy);
    void logEvent(String line);
}

interface UiRenderContext {
    GameOptions renderOptions();
    String accessibleNarration();
    void setAccessibleNarration(String narration);
    void requestUiRepaint();
}

interface WorldRuntimeContext {
    World runtimeWorld();
    int runtimeTurn();
    int runtimeWorldTurn();
    void advanceRuntimeTurn(String eventLine);
}

interface InventoryPersistenceContext {
    ArrayList<String> carriedInventory();
    int carriedScript();
    int bankedScript();
    void addImperialScript(int amount);
}

interface AdminCommandContext {
    World adminWorld();
    void adminLog(String line);
    void markAdminDirty(String reason);
}
