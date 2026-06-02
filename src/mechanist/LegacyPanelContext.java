package mechanist;

import java.awt.Font;
import java.awt.event.KeyEvent;
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

    enum Screen { BOOT, MENU, MAIN, CHARACTER, GAME, PANEL, OPTIONS, INVENTORY, INFO, MAP, PAUSE, MODS, KNOWLEDGE, MULTIPLAYER, SECTOR_AUDIT, EDITOR }
    enum PanelMode { NONE, INVENTORY, CONTAINER, TRADE, LOOK, INTERACT, COMBAT, AUSPEX, CONSOLE, INFO, INFOPEDIA, BUILD, WORKBENCH, MAP, CRAFTING }

    World world;
    LegacyPanelAtlas atlas;
    GameOptions options = new GameOptions();
    Candidate active;
    UserProfileAuthority.Profile userProfile = UserProfileAuthority.detect();
    SinglePlayerSectorRuntimeBridge singlePlayerSectorBridge;
    LegacyPerformanceDiagnostics performanceDiagnostics = new LegacyPerformanceDiagnostics();
    LegacyKeyboardInputBridge keyboardInputBridge = new LegacyKeyboardInputBridge();
    LegacyMultiplayerMenu multiplayerMenu = new LegacyMultiplayerMenu();
    LegacySoundSurface sounds = new LegacySoundSurface();

    final ArrayList<String> inventory = new ArrayList<>();
    final ArrayList<String> eventLog = new ArrayList<>();
    final ArrayList<BaseObject> baseObjects = new ArrayList<>();
    final ArrayList<String> baseStorage = new ArrayList<>();
    final ArrayList<Candidate> candidates = new ArrayList<>();
    final ArrayList<Object> factionRecruits = new ArrayList<>();
    final ArrayList<Object> buttons = new ArrayList<>();
    final HashSet<String> visitedZoneTypes = new HashSet<>();
    final HashSet<String> visitedZoneInstances = new HashSet<>();
    final HashSet<String> unlockedKnowledges = new HashSet<>();
    final ArrayDeque<LogisticsRouteIntentAuthority.RouteIntentRecord> logisticsRouteIntentHistory = new ArrayDeque<>();

    Random rng = new Random(0);
    long seed;
    int playerX;
    int playerY;
    int lookX;
    int lookY;
    int baseX;
    int baseY;
    boolean lookCursorActive;
    boolean interactCursorActive;
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
    int selectedButton;
    int infopediaTab;
    int lookStackIndex;
    int lookStackScroll;
    int jobIndex;
    int jobDossierScroll;
    int jobDossierTab;
    boolean baseClaimed;
    boolean characterNameEditActive;
    boolean manualMovementPlanActive;
    boolean buildPlacementActive;
    Screen screen = Screen.MENU;
    PanelMode panelMode = PanelMode.NONE;
    String lastAccessibleNarration = "";
    String activeScrollTag = "";
    long bootStartMillis = System.currentTimeMillis();
    long lastInputMillis = System.currentTimeMillis();
    Font titleFont = new Font("Monospaced", Font.BOLD, 36);
    Font smallFont = new Font("Monospaced", Font.PLAIN, 14);

    void runGuarded(String tag, String reason, Runnable body) { if (body != null) body.run(); }
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

    void setScreen(Screen next) { screen = next == null ? Screen.MENU : next; }
    void closePanel() { panelMode = PanelMode.NONE; screen = Screen.GAME; }
    void closeKnowledgeScreen() { screen = Screen.GAME; }
    void handleKnowledgeKeyPressed(int code) {}
    void cancelManualMovementPlan(String reason) { manualMovementPlanActive = false; }
    void rerollSectorAudit() {}
    void cycleAuditZoneType(int delta) {}
    void cycleAuditZoneDensity() {}
    void cycleAuditOverlay() {}
    void jumpAuditFinding(int delta) {}
    void moveAuditCursor(int dx, int dy) {}
    boolean isAssetInfopediaTab(int tab) { return false; }
    void backspaceInfopediaAssetFilter() {}
    void cycleInfopediaTab(int delta) {}
    void moveInfopediaSelection(int delta) {}
    boolean scrollActivePanel(int delta, boolean page) { return false; }
    void cycleFireMode() {}
    void throwSelectedExplosiveAtCursor() {}
    void reloadCurrentRangedWeapon() {}
    void confirmCombatTarget() { confirmCombatTargetBody(); }
    void moveCombatCursor(int dx, int dy) { lookX += dx; lookY += dy; }
    void confirmInteraction() { confirmInteractionBody(); }
    void examineSelectedLookTarget() {}
    void moveInteractCursor(int dx, int dy) { lookX += dx; lookY += dy; }
    ArrayList<String> tileStackAt(int x, int y) { return new ArrayList<>(); }
    void moveBuildCursor(int dx, int dy) {}
    void confirmBuildPlacement() {}
    void moveSelectedButton(int delta) { if (!buttons.isEmpty()) selectedButton = Math.floorMod(selectedButton + delta, buttons.size()); }
    void activateSelectedButtonUniversal() {}
    void cycleMovementMode() {}
    void beginManualMovementPlan() { manualMovementPlanActive = true; }
    void waitOneTurn() { advanceTurnBody("waits."); }
    void beginInteractMode() { panelMode = PanelMode.INTERACT; screen = Screen.PANEL; }
    void openPanel(PanelMode mode) { panelMode = mode == null ? PanelMode.NONE : mode; screen = Screen.PANEL; }
    void beginCombatTargeting() { panelMode = PanelMode.COMBAT; screen = Screen.PANEL; }
    void beginExplosiveTargeting() { panelMode = PanelMode.COMBAT; screen = Screen.PANEL; }
    void queueOrExecuteMovementInput(int dx, int dy) { executePacedMovementBody(dx, dy, "legacy-queue"); }
    void sanityCheck(String phase) {}
    void nudgeManualMovementPlan(int dx, int dy) { lookX += dx; lookY += dy; }
    void confirmManualMovementPlan() { manualMovementPlanActive = false; }
    void createNewInGameEditorEntry() {}
    void inGameEditorUndo() {}
    void inGameEditorRedo() {}
}

final class LegacyPanelAtlas {
    HiveWorldDefinition hiveWorld;
    int sectorX;
    int sectorY;
    int floor;
    int zoneX;
    int zoneY;
    boolean sewer;
}

final class LegacyKeyboardInputBridge {
    void keyPressed(KeyEvent event) {}
}

final class LegacyMultiplayerMenu {
    boolean handleKeyPressed(int code) { return false; }
    void endDirectEdit() {}
}

final class LegacySoundSurface {
    void play(String key, GameOptions options) {}
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
