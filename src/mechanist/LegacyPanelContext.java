package mechanist;

import java.awt.Font;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
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
    static final String CONTAINER_BASE_STORAGE = "base-storage";
    static final String CONTAINER_PLAYER_INVENTORY = "player-inventory";

    enum Screen { BOOT, MENU, MAIN, CHARACTER, GAME, PANEL, OPTIONS, INVENTORY, INFO, MAP, PAUSE, MODS, KNOWLEDGE, MULTIPLAYER, SECTOR_AUDIT, EDITOR }
    enum PanelMode { NONE, CHARACTER, INVENTORY, CONTAINER, TRADE, LOOK, INTERACT, COMBAT, AUSPEX, CONSOLE, INFO, INFOPEDIA, BUILD, WORKBENCH, MAP, CRAFTING }

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
    LegacyRenderScaling renderScaling = new LegacyRenderScaling();
    LegacyFrameLimiter frameLimiter = new LegacyFrameLimiter();
    LegacyImageSurface images = new LegacyImageSurface();
    javax.swing.Timer timer;

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
    final ArrayDeque<LogisticsDeliveryIntentAuthority.DeliveryIntentRecord> logisticsDeliveryIntentHistory = new ArrayDeque<>();
    final ArrayDeque<LogisticsSourceReservationAuthority.SourceReservationRecord> logisticsSourceReservationHistory = new ArrayDeque<>();
    final ArrayList<FactionStrategicPlan> factionStrategicPlans = new ArrayList<>();
    final ArrayList<PlayerNewsEvent> playerNewsEvents = new ArrayList<>();
    final ArrayList<Point> mouseMovePreviewPath = new ArrayList<>();
    final HashMap<Integer, String> innDailyIssues = new HashMap<>();
    final HashMap<String, Boolean> consoleFlags = new HashMap<>();
    final HashMap<String, Float> consoleNumericFlags = new HashMap<>();
    final HashMap<String, String> consoleStringFlags = new HashMap<>();

    Random rng = new Random(0);
    long seed;
    int playerX;
    int playerY;
    int lookX;
    int lookY;
    int combatX;
    int combatY;
    int baseX;
    int baseY;
    int buildX;
    int buildY;
    boolean lookCursorActive;
    boolean interactCursorActive;
    boolean mouseMovePreviewActive;
    boolean mouseMovePreviewValid;
    boolean inventoryTargetColumnActive;
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
    int knowledgeCredits;
    int runKills;
    int runCrafted;
    int runNpcTalkedTo;
    int runUnconsciousEvents;
    int candidateIndex;
    int claimedRoomId = -1;
    int selectedButton;
    int infopediaTab;
    int controlsTab;
    int lookStackIndex;
    int lookStackScroll;
    int inventoryItemDescriptionScroll;
    int selectedInventoryIndex;
    int selectedTargetInventoryIndex;
    int jobIndex;
    int jobDossierScroll;
    int jobDossierTab;
    int wounds;
    int bleeding;
    int infectionRisk;
    int pain;
    int fatigue;
    int nextLogisticsIntentSeq = 1;
    int nextLogisticsRouteIntentSeq = 1;
    int nextLogisticsSourceReservationSeq = 1;
    int lastFactionSimulationDay = -1;
    int innLastIssueDay = -1;
    boolean baseClaimed;
    boolean characterNameEditActive;
    boolean manualMovementPlanActive;
    boolean buildPlacementActive;
    Screen screen = Screen.MENU;
    PanelMode panelMode = PanelMode.NONE;
    BuildRecipe pendingBuildRecipe;
    String selectedKnowledgeNodeId;
    String lastAccessibleNarration = "";
    String activeScrollTag = "";
    String rebindingTarget = "";
    String lastTargetingReport = "No target selected.";
    String equippedLeftHandItem = "LEFT EMPTY";
    String equippedRightHandItem = "RIGHT EMPTY";
    String lastDefeatAttacker = "unknown attacker";
    String lastDefeatWeapon = "unknown weapon";
    String lastDefeatCause = "unknown cause";
    String lastDefeatLocation = "unknown location";
    String lastFactionSimulationReport = "No faction simulation has run.";
    String lastPublicNewsBulletin = "No public bulletin has been issued.";
    String lastInnNewsIssue = "No Imperial News Network issue has been generated.";
    String lastBroadcastReport = "No broadcast has been received.";
    long bootStartMillis = System.currentTimeMillis();
    long lastInputMillis = System.currentTimeMillis();
    Font titleFont = new Font("Monospaced", Font.BOLD, 36);
    Font smallFont = new Font("Monospaced", Font.PLAIN, 14);
    Font uiFont = new Font("Monospaced", Font.BOLD, 16);
    Font asciiFont = new Font("Monospaced", Font.BOLD, 13);

    void runGuarded(String tag, String reason, Runnable body) { if (body != null) body.run(); }
    void executePacedMovementBody(int dx, int dy, String source) { playerX += dx; playerY += dy; lookX = playerX; lookY = playerY; }
    void clearPendingMovementInput(String reason) {}
    void advanceTurnBody(String line) { turn++; worldTurn++; if (line != null && !line.isBlank()) logEvent(line); }
    void advanceTurn(String line) { advanceTurnBody(line); }
    void settlePlayerMotionAfterNoMoveTurn(String reason) {}
    void confirmInteractionBody() {}
    void confirmCombatTargetBody() {}
    void useSelectedInventoryItemBody() {}
    void unequipSelectedEquipmentSlotBody() {}
    void addImperialScript(int amount) { carriedScript += Math.max(0, amount); }
    boolean spendImperialScript(int amount) { if (amount <= 0) return true; if (carriedScript < amount) return false; carriedScript -= amount; return true; }
    void markLocalDirtyRegion(String reason, int x, int y, int radius, boolean tiles, boolean npcs, boolean objects, boolean full) {}
    void updateSensoryModel(String reason) {}
    void refreshNameLockedCandidateState(Candidate candidate) {}
    int visionRange() { return 8; }
    boolean isVisible(int x, int y) { return true; }
    boolean isRemembered(int x, int y) { return true; }
    int countMoney() { return Math.max(0, carriedScript); }
    int totalBankedCash() { return Math.max(0, baseStashedScript); }
    int inventoryWeight() { return inventory == null ? 0 : inventory.size(); }
    int carryCapacity() { return 40 + Math.max(0, supplies / 5); }
    void addInventoryItem(String item, ItemProvenanceRecord provenance) { if (item != null && !item.isBlank()) inventory.add(item); }
    void gainXp(String skill, int amount, String reason) { xp += Math.max(0, amount); }
    String facingLabel() { return "E"; }
    String activeMotionStateLabel() { return "stationary"; }
    String timeText() { return "day " + Math.max(0, turn / Math.max(1, TURNS_PER_HOUR * HOURS_PER_DAY)) + " hour " + Math.max(0, (turn / Math.max(1, TURNS_PER_HOUR)) % HOURS_PER_DAY); }
    String stateSummary() { return "turn=" + turn + " pos=" + playerX + "," + playerY + " screen=" + screen + " panel=" + panelMode; }
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
    void moveCombatCursor(int dx, int dy) { combatX += dx; combatY += dy; lookX = combatX; lookY = combatY; }
    LegacyTargetingSolution targetingSolutionAt(int x, int y) { return new LegacyTargetingSolution("target=" + x + "," + y); }
    void confirmInteraction() { confirmInteractionBody(); }
    void examineSelectedLookTarget() {}
    void moveInteractCursor(int dx, int dy) { lookX += dx; lookY += dy; }
    ArrayList<String> tileStackAt(int x, int y) { return new ArrayList<>(); }
    boolean isDoorTile(char tile) { return tile == '+' || tile == '/' || tile == '\\'; }
    void interactDoorAt(int x, int y, char tile) { logEvent("Door interaction at " + x + "," + y + "."); advanceTurn("interacts with a door."); }
    void enforceEntityOccupancy(String reason) {}
    void moveBuildCursor(int dx, int dy) { buildX += dx; buildY += dy; }
    void confirmBuildPlacement() {}
    void moveSelectedButton(int delta) { if (!buttons.isEmpty()) selectedButton = Math.floorMod(selectedButton + delta, buttons.size()); }
    void activateSelectedButtonUniversal() {}
    void cycleMovementMode() {}
    void beginManualMovementPlan() { manualMovementPlanActive = true; }
    void waitOneTurn() { advanceTurnBody("waits."); }
    void beginInteractMode() { panelMode = PanelMode.INTERACT; screen = Screen.PANEL; }
    void openPanel(PanelMode mode) { panelMode = mode == null ? PanelMode.NONE : mode; screen = mode == PanelMode.CHARACTER ? Screen.CHARACTER : Screen.PANEL; }
    void beginCombatTargeting() { panelMode = PanelMode.COMBAT; screen = Screen.PANEL; }
    void beginExplosiveTargeting() { panelMode = PanelMode.COMBAT; screen = Screen.PANEL; }
    void queueOrExecuteMovementInput(int dx, int dy) { executePacedMovementBody(dx, dy, "legacy-queue"); }
    void sanityCheck(String phase) {}
    void nudgeManualMovementPlan(int dx, int dy) { lookX += dx; lookY += dy; }
    void confirmManualMovementPlan() { manualMovementPlanActive = false; }
    void createNewInGameEditorEntry() {}
    void inGameEditorUndo() {}
    void inGameEditorRedo() {}

    void moveInventorySelection(int delta) { if (!inventory.isEmpty()) selectedInventoryIndex = Math.floorMod(selectedInventoryIndex + delta, inventory.size()); inventoryItemDescriptionScroll = 0; }
    void moveTargetInventorySelection(int delta) { if (!baseStorage.isEmpty()) selectedTargetInventoryIndex = Math.floorMod(selectedTargetInventoryIndex + delta, baseStorage.size()); inventoryItemDescriptionScroll = 0; }

    void beginWindowModeReconfigure() {}
    void endWindowModeReconfigure() {}
    void requestApplicationExit(String reason) { logEvent("Exit requested: " + (reason == null ? "unspecified" : reason)); }
    String togglePerformanceDiagnostics() { return performanceDiagnostics.toggle(); }
    boolean toggleConsoleFlag(String key) { String safe = key == null ? "" : key; boolean next = !Boolean.TRUE.equals(consoleFlags.get(safe)); consoleFlags.put(safe, next); return next; }
    void setConsoleNumericFlag(String key, float value) { consoleNumericFlags.put(key == null ? "" : key, value); }
    void setConsoleStringFlag(String key, String value) { consoleStringFlags.put(key == null ? "" : key, value == null ? "" : value); }
    void healWorstBodyPart(int amount) { wounds = Math.max(0, wounds - Math.max(0, amount)); }
    void triggerPlayerDeath(String cause, String attacker, String weapon, String location) { lastDefeatCause = cause; lastDefeatAttacker = attacker; lastDefeatWeapon = weapon; lastDefeatLocation = location; runUnconsciousEvents++; }
    void writeSaveFile(int slot, boolean quick) {}

    String rawCanPlacePendingBuildAtUncached(int x, int y) { return rawCanPlacePendingBuildAt(x, y); }
    String rawCanPlacePendingBuildAt(int x, int y) { return pendingBuildRecipe == null ? "no selected build" : "ok"; }
    String constructionPlacementResult(BuildRecipe recipe, int x, int y, String raw) { return raw == null || raw.isBlank() ? "ok" : raw; }
    String constructionBlueprintFor(BuildRecipe recipe) { return recipe == null ? "unselected blueprint" : recipe.getClass().getSimpleName(); }
    String buildComponentRequirementProblem(BuildRecipe recipe) { return "component requirements unresolved in legacy bridge"; }
    String buildRequirementProblem(BuildRecipe recipe) { return "build requirements unresolved in legacy bridge"; }

    int currentInnDay() { return Math.max(0, turn / Math.max(1, TURNS_PER_HOUR * HOURS_PER_DAY)); }
    NpcFactionSite siteForFaction(Faction faction, ZoneType zoneType) { return null; }
    void addFactionMarketPressure(Faction faction, int pressure, String reason) {}
    static boolean sameFactionFamilyStatic(Faction a, Faction b) { return a != null && b != null && (a == b || a.name().split("_")[0].equals(b.name().split("_")[0])); }
    String factionStockContainerId(NpcFactionSite site) { return site == null ? "faction-stock-none" : "faction-stock-" + Math.abs(site.name.hashCode()); }
    void ensureContainer(String containerId, String label) {}
    void addItemToContainerResult(String containerId, String label, String item, ItemProvenanceRecord provenance, Object source, String reason) {}
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

final class LegacyTargetingSolution {
    final String summary;
    LegacyTargetingSolution(String summary) { this.summary = summary == null ? "No target selected." : summary; }
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
    void playDistantCue(String key, int distance, GameOptions options) { play(key, options); }
}

final class LegacyRenderScaling {
    void applyOptions(GameOptions options) {}
    String auditSummary() { return "legacyRenderScaling active"; }
}

final class LegacyFrameLimiter {
    void configure(GameOptions options) {}
}

final class LegacyImageSurface {
    void reloadArtQuality(GameOptions options) {}
    BufferedImage getTile(char tile) { return null; }
    BufferedImage getNpcPortraitFor(Object npc) { return null; }
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
