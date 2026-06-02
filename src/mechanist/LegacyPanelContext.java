package mechanist;

import java.awt.Font;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import javax.swing.JPanel;

/**
 * Temporary compile bridge for post-shard GamePanel retirement.
 *
 * The old monolithic GamePanel.java file remains intentionally absent.  This
 * package-private compatibility type preserves the old context surface while
 * extracted subsystems are retargeted toward narrower interfaces/managers.
 */
@SuppressWarnings({"serial", "unused"})
class GamePanel extends JPanel {
    static final int MOTION_STATIONARY = 0;
    static final int MOTION_SNEAK = 1;
    static final int MOTION_WALK = 2;
    static final int MOTION_RUN = 3;
    static final int MOTION_SPRINT = 4;
    static final int TURNS_PER_HOUR = 100;
    static final int HOURS_PER_DAY = 24;

    enum Screen { BOOT, MAIN, CHARACTER, GAME, OPTIONS, INVENTORY, INFO, MAP, PAUSE }
    enum PanelMode { NONE, INVENTORY, CONTAINER, TRADE, LOOK, AUSPEX, CONSOLE, INFO }

    World world;
    LegacyPanelAtlas atlas;
    GameOptions options = new GameOptions();
    Candidate active;
    UserProfileAuthority.Profile userProfile = UserProfileAuthority.detect();
    SinglePlayerSectorRuntimeBridge singlePlayerSectorBridge;
    LegacyPerformanceDiagnostics performanceDiagnostics = new LegacyPerformanceDiagnostics();

    final ArrayList<String> inventory = new ArrayList<>();
    final ArrayList<String> eventLog = new ArrayList<>();
    final ArrayList<BaseObject> baseObjects = new ArrayList<>();
    final ArrayList<String> baseStorage = new ArrayList<>();
    final ArrayList<Candidate> candidates = new ArrayList<>();
    final ArrayList<Object> factionRecruits = new ArrayList<>();
    final HashSet<String> visitedZoneTypes = new HashSet<>();
    final HashSet<String> visitedZoneInstances = new HashSet<>();
    final HashSet<String> unlockedKnowledges = new HashSet<>();
    final ArrayDeque<LogisticsRouteIntentAuthority.RouteIntentRecord> logisticsRouteIntentHistory = new ArrayDeque<>();

    Random rng = new Random(0);
    int playerX;
    int playerY;
    int lookX;
    int lookY;
    boolean lookCursorActive;
    int turn;
    int worldTurn;
    int food;
    int water;
    int sleepNeed;
    int gangHeat;
    int suspicion;
    int supplies;
    int machineParts;
    int carriedScript;
    int baseStashedScript;
    int xp;
    int runKills;
    int runCrafted;
    int runNpcTalkedTo;
    int runUnconsciousEvents;
    int candidateIndex;
    int claimedRoomId = -1;
    boolean baseClaimed;
    boolean characterNameEditActive;
    Screen screen = Screen.MAIN;
    PanelMode panelMode = PanelMode.NONE;
    String lastAccessibleNarration = "";
    long bootStartMillis = System.currentTimeMillis();
    Font titleFont = new Font("Monospaced", Font.BOLD, 36);
    Font smallFont = new Font("Monospaced", Font.PLAIN, 14);

    void executePacedMovementBody(int dx, int dy, String source) { playerX += dx; playerY += dy; lookX = playerX; lookY = playerY; }
    void clearPendingMovementInput(String reason) {}
    void advanceTurnBody(String line) { turn++; worldTurn++; if (line != null && !line.isBlank()) logEvent(line); }
    void settlePlayerMotionAfterNoMoveTurn(String reason) {}
    void confirmInteractionBody() {}
    void confirmCombatTargetBody() {}
    void useSelectedInventoryItemBody() {}
    void unequipSelectedEquipmentSlotBody() {}
    void addImperialScript(int amount) { carriedScript += Math.max(0, amount); }
    void markLocalDirtyRegion(String reason, int x, int y, int radius, boolean tiles, boolean npcs, boolean objects, boolean full) {}
    void updateSensoryModel(String reason) {}
    void refreshNameLockedCandidateState(Candidate candidate) {}
    int visionRange() { return 8; }
    boolean isVisible(int x, int y) { return true; }
    boolean isRemembered(int x, int y) { return true; }
    int countMoney() { return Math.max(0, carriedScript); }
    int totalBankedCash() { return Math.max(0, baseStashedScript); }
    String facingLabel() { return "E"; }
    String activeMotionStateLabel() { return "stationary"; }
    void logEvent(String line) { if (line != null && !line.isBlank()) eventLog.add(line); }
}

final class LegacyPanelAtlas {
    int sectorX;
    int sectorY;
    int floor;
    int zoneX;
    int zoneY;
    boolean sewer;
}

final class LegacyPerformanceDiagnostics {
    private boolean visible;
    String toggle() { visible = !visible; return visible ? "Performance diagnostics visible." : "Performance diagnostics hidden."; }
    boolean visible() { return visible; }
    String auditSummary() { return "legacyPerformanceDiagnostics visible=" + visible; }
}

final class LegacyPanelProfile {
    String name = "none";
}
