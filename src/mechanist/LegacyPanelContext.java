package mechanist;

import java.util.ArrayList;
import javax.swing.JPanel;

@SuppressWarnings({"serial", "unused"})
class GamePanel extends JPanel {
    World world;
    Object atlas;
    GameOptions options;
    LegacyPanelProfile active;
    SinglePlayerSectorRuntimeBridge singlePlayerSectorBridge;

    final ArrayList<String> inventory = new ArrayList<>();
    final ArrayList<String> eventLog = new ArrayList<>();
    final ArrayList<BaseObject> baseObjects = new ArrayList<>();

    int playerX;
    int playerY;
    int lookX;
    int lookY;
    boolean lookCursorActive;
    long turn;
    long worldTurn;
    int food;
    int water;
    int sleepNeed;
    int gangHeat;
    int suspicion;
    Object screen;
    Object panelMode;

    void executePacedMovementBody(int dx, int dy, String source) { playerX += dx; playerY += dy; lookX = playerX; lookY = playerY; }
    void clearPendingMovementInput(String reason) {}
    void advanceTurnBody(String line) { turn++; worldTurn++; if (line != null && !line.isBlank()) logEvent(line); }
    void settlePlayerMotionAfterNoMoveTurn(String reason) {}
    void confirmInteractionBody() {}
    void confirmCombatTargetBody() {}
    void useSelectedInventoryItemBody() {}
    void unequipSelectedEquipmentSlotBody() {}
    void addImperialScript(int amount) {}
    void markLocalDirtyRegion(String reason, int x, int y, int radius, boolean tiles, boolean npcs, boolean objects, boolean full) {}
    void updateSensoryModel(String reason) {}
    int visionRange() { return 8; }
    boolean isVisible(int x, int y) { return true; }
    boolean isRemembered(int x, int y) { return true; }
    int countMoney() { return 0; }
    String facingLabel() { return "E"; }
    String activeMotionStateLabel() { return "stationary"; }
    void logEvent(String line) { if (line != null && !line.isBlank()) eventLog.add(line); }
}

final class LegacyPanelProfile {
    String name = "none";
}
