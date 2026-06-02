package mechanist;

import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.swing.JPanel;

/**
 * Temporary compile bridge for post-shard GamePanel retirement.
 *
 * The old monolithic GamePanel.java file remains intentionally absent.  This
 * package-private compatibility type preserves the old context surface while
 * extracted subsystems are retargeted toward narrower interfaces/managers.
 */
@SuppressWarnings({"serial", "unused"})
class GamePanel extends LegacyPanelBridgeBase {
    static final int MOTION_STATIONARY = 0;
    static final int MOTION_SNEAK = 1;
    static final int MOTION_WALK = 2;
    static final int MOTION_RUN = 3;
    static final int MOTION_SPRINT = 4;
    static final int TURNS_PER_HOUR = 100;
    static final int HOURS_PER_DAY = 24;
    static final int MAX_FOOD_WATER = 100;
    static final int AUTOSAVE_HOURLY_SLOT = 1001;
    static final int AUTOSAVE_ZONE_SLOT = 1002;
    static final String CONTAINER_BASE_STORAGE = "base-storage";
    static final String CONTAINER_PLAYER_INVENTORY = "player-inventory";
    static final String CONTAINER_MACHINE_INPUT_PREFIX = "machine-input-";
    static final String CONTAINER_TRADER_SHELF_PREFIX = "trader-shelf-";
    static final String CONTAINER_FACTION_STOCK_PREFIX = "faction-stock-";
    static final String CONTAINER_CONTRACT_OBJECT_PREFIX = "contract-object-";
    static final String CONTAINER_CORPSE_LOOT_PREFIX = "corpse-loot-";
    static final String CONTAINER_ROOM_CACHE_PREFIX = "room-cache-";

    enum Screen { BOOT, INTRO_CRAWL, ZONE_SPLASH, CAPTURE, MENU, MAIN, CHARACTER, GAME, PANEL, OPTIONS, INVENTORY, INFO, MAP, PAUSE, MODS, KNOWLEDGE, MULTIPLAYER, SECTOR_AUDIT, EDITOR }
    enum PanelMode { NONE, CHARACTER, INVENTORY, CONTAINER, TRADE, LOOK, INTERACT, COMBAT, AUSPEX, CONSOLE, INFO, INFOPEDIA, BUILD, WORKBENCH, MAP, CRAFTING, SCAVENGE }

    World world;
    WorldAtlas atlas;
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
    LegacyFirstPersonRenderViewport firstPersonRenderViewport = new LegacyFirstPersonRenderViewport();
    LegacyRenderStressTest renderStressTest = new LegacyRenderStressTest();
    JvmRuntimeProfileAuthority.RuntimeConfig jvmRuntimeProfile = JvmRuntimeProfileAuthority.load();
    javax.swing.Timer timer;

    final ArrayList<String> inventory = new ArrayList<>();
    final ArrayList<String> eventLog = new ArrayList<>();
    final ArrayList<BaseObject> baseObjects = new ArrayList<>();
    final ArrayList<String> baseStorage = new ArrayList<>();
    final ArrayList<Candidate> candidates = new ArrayList<>();
    final ArrayList<RecruitWorker> factionRecruits = new ArrayList<>();
    final ArrayList<ButtonBox> buttons = new ArrayList<>();
    final LinkedHashMap<String, ContainerRecord> itemContainers = new LinkedHashMap<>();
    final LinkedHashMap<String, ItemInstance> itemInstances = new LinkedHashMap<>();
    final HashSet<ZoneType> visitedZoneTypes = new HashSet<>();
    final HashSet<String> visitedZoneInstances = new HashSet<>();
    final HashSet<String> unlockedKnowledges = new HashSet<>();
    final ArrayDeque<LogisticsRouteIntentAuthority.RouteIntentRecord> logisticsRouteIntentHistory = new ArrayDeque<>();
    final ArrayDeque<LogisticsDeliveryIntentAuthority.DeliveryIntentRecord> logisticsDeliveryIntentHistory = new ArrayDeque<>();
    final ArrayDeque<LogisticsSourceReservationAuthority.SourceReservationRecord> logisticsSourceReservationHistory = new ArrayDeque<>();
    final ArrayDeque<LogisticsManualHaulContractAuthority.ManualHaulContractRecord> logisticsHaulContractHistory = new ArrayDeque<>();
    final ArrayDeque<LogisticsRouteReadinessPreviewAuthority.ManualHaulPreviewRecord> logisticsRoutePreviewHistory = new ArrayDeque<>();
    final ArrayDeque<LogisticsContractLifecycleAuthority.ContractLifecycleRecord> logisticsContractLifecycleHistory = new ArrayDeque<>();
    final ArrayDeque<LogisticsHaulFulfillmentPreflightAuthority.FulfillmentPreflightRecord> logisticsHaulPreflightHistory = new ArrayDeque<>();
    final ArrayDeque<LogisticsManualHaulExecutionAuthority.ManualHaulExecutionRecord> logisticsHaulExecutionHistory = new ArrayDeque<>();
    final ArrayList<FactionStrategicPlan> factionStrategicPlans = new ArrayList<>();
    final ArrayList<PlayerNewsEvent> playerNewsEvents = new ArrayList<>();
    final ArrayList<Point> mouseMovePreviewPath = new ArrayList<>();
    final HashMap<Integer, String> innDailyIssues = new HashMap<>();
    final HashMap<String, Boolean> consoleFlags = new HashMap<>();
    final HashMap<String, Float> consoleNumericFlags = new HashMap<>();
    final HashMap<String, String> consoleStringFlags = new HashMap<>();
    WorldSetupSettings worldSetup = WorldSetupSettings.standard();
    Clothing equippedClothing = Clothing.scavengerRags();
    VisualLightingAuthority visualLighting = new VisualLightingAuthority();
    LegacyGamepadInputEngine gamepadInputEngine = new LegacyGamepadInputEngine();
    int optionsTab;
    int stimulantStrain;
    int nextItemInstanceSeq = 1;
    int activePortableLightExpiresTurn;
    int authorityFacilityInspectionCooldownUntilTurn;
    int bankHeistAlarmCooldownUntilTurn;
    int arbitesCaptureCooldownUntilTurn;
    int arbitesInspectionCooldownUntilTurn;
    String activePortableLightItem = "";
    String lastPortableLightReport = "No portable light has been activated.";
    String lastLogisticsDeliveryIntentReport = "No logistics delivery intent has been recorded.";
    String lastLogisticsSourceReservationReport = "No logistics source reservation has been recorded.";
    String lastLogisticsRouteIntentReport = "No logistics route intent has been recorded.";
    String lastLogisticsRoutePreviewReport = "No logistics route preview has been recorded.";
    String lastLogisticsHaulContractReport = "No manual haul contract has been recorded.";
    String lastLogisticsHaulPreflightReport = "No manual haul preflight has been recorded.";
    String lastLogisticsContractLifecycleReport = "No logistics contract lifecycle has been recorded.";
    String lastPlayerNewsReport = "No player-facing news has been generated.";
    String lastAuthorityFacilityInspectionReport = "No authority facility inspection has occurred.";
    String lastBankReport = "No banking report has been generated.";
    String lastBankHeistReport = "No bank heist report has been generated.";
    String lastBankAlarmReport = "No bank alarm report has been generated.";
    String lastBankLockboxContractReport = "No bank lockbox contract report has been generated.";
    String lastCrimePunishmentReport = "No crime punishment report has been generated.";
    String lastCustodyReportDetailed = "No custody report has been generated.";
    String lastArbitesPatrolReport = "No Arbites patrol report has been generated.";
    String lastItemLedgerAuditReport = "No item ledger audit has been generated.";
    boolean activePortableLightWorn;
    boolean combatCursorActive;
    boolean[][] visibleTiles = new boolean[1][1];
    boolean[][] rememberedTiles = new boolean[1][1];
    final ArrayList<PortableLightInstance> portableLights = new ArrayList<>();
    final ArrayList<FactionContract> factionContracts = new ArrayList<>();
    final ArrayList<NpcFactionSite> npcFactionSites = new ArrayList<>();
    final LinkedHashMap<String, Integer> loadedWeaponShots = new LinkedHashMap<>();
    final LinkedHashMap<String, Integer> terrainIntegrity = new LinkedHashMap<>();
    final LinkedHashMap<String, ArrayDeque<ItemProvenanceRecord>> itemProvenance = new LinkedHashMap<>();
    final EnumMap<Faction, Integer> factionStanding = new EnumMap<>(Faction.class);
    final EnumMap<Faction, Integer> temporaryHostileTurns = new EnumMap<>(Faction.class);
    final EnumMap<Faction, Integer> factionMarketPressure = new EnumMap<>(Faction.class);
    final LinkedHashMap<String, Integer> bankBalances = new LinkedHashMap<>();
    final LinkedHashMap<Integer, Integer> scavengeCooldownUntilTurn = new LinkedHashMap<>();
    final HashSet<String> openBankAccounts = new HashSet<>();
    final HashSet<String> lootedBankVaultIds = new HashSet<>();
    final HashSet<String> disabledBankAlarmPanelIds = new HashSet<>();

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
    boolean eulaGateActive;
    boolean jvmRuntimeRestartPending;
    int turn;
    long worldTurn;
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
    int nextLogisticsRoutePreviewSeq = 1;
    int nextLogisticsHaulContractSeq = 1;
    int nextLogisticsContractLifecycleSeq = 1;
    int nextLogisticsHaulPreflightSeq = 1;
    int nextLogisticsHaulExecutionSeq = 1;
    int lastFactionSimulationDay = -1;
    int innLastIssueDay = -1;
    int eulaScroll;
    int eulaMaxScroll;
    int graphicsDropdown = -1;
    int generatedJobCategoryFilterIndex;
    int generatedJobReadinessFilterIndex;
    int mouseX = -1;
    int mouseY = -1;
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
    String jvmRuntimeNotice = "";
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
    String lastLogisticsHaulExecutionReport = "No manual haul execution has been recorded.";
    long bootStartMillis = System.currentTimeMillis();
    long lastInputMillis = System.currentTimeMillis();
    Font titleFont = new Font("Monospaced", Font.BOLD, 36);
    Font smallFont = new Font("Monospaced", Font.PLAIN, 14);
    Font uiFont = new Font("Monospaced", Font.BOLD, 16);
    Font asciiFont = new Font("Monospaced", Font.BOLD, 13);

    GamePanel() {
        setOpaque(true);
        setBackground(new java.awt.Color(18, 17, 14));
        setFocusable(true);
        logEvent("GamePanel compatibility bridge initialized.");
    }
    GamePanel(RuntimeProfile runtimeProfile) {
        this();
        if (runtimeProfile != null) logEvent("Runtime profile attached: " + runtimeProfile.compactLine());
    }
    GamePanel(JvmRuntimeProfileAuthority.RuntimeConfig runtimeProfile) {
        this();
        if (runtimeProfile != null) this.jvmRuntimeProfile = runtimeProfile;
    }

    @Override
    protected void paintComponent(java.awt.Graphics graphics) {
        super.paintComponent(graphics);
        java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
        try {
            int w = Math.max(1, getWidth());
            int h = Math.max(1, getHeight());
            g.setColor(new java.awt.Color(18, 17, 14));
            g.fillRect(0, 0, w, h);
            g.setColor(new java.awt.Color(72, 61, 38));
            for (int x = 0; x < w; x += 32) g.drawLine(x, 0, x, h);
            for (int y = 0; y < h; y += 32) g.drawLine(0, y, w, y);
            try {
                if (screen == Screen.MENU || screen == Screen.MAIN || screen == Screen.BOOT) {
                    new MainMenuSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.OPTIONS) {
                    OptionsScreenPainter.paintShell(this, g);
                    OptionsScreenPainter.paintBody(this, g);
                    OptionsScreenPainter.paintGraphicsDropdownPopup(this, g);
                    return;
                }
                if (screen == Screen.GAME || screen == Screen.PANEL || screen == Screen.INVENTORY || screen == Screen.CHARACTER || screen == Screen.INFO || screen == Screen.MAP || screen == Screen.KNOWLEDGE) {
                    paintGameBridgeSurface(g, w, h);
                    return;
                }
            } catch (Throwable t) {
                drawBridgeException(g, w, h, t);
                return;
            }
            drawVisibleBootStatus(g, w, h);
        } finally {
            g.dispose();
        }
    }


    private void paintGameBridgeSurface(java.awt.Graphics2D g, int w, int h) {
        g.setColor(new java.awt.Color(12, 12, 10));
        g.fillRect(0, 0, w, h);
        int tile = Math.max(16, Math.min(32, Math.min(w, h) / 32));
        for (int y = 0; y < h; y += tile) {
            for (int x = 0; x < w; x += tile) {
                boolean alt = ((x / tile) + (y / tile)) % 2 == 0;
                g.setColor(alt ? new java.awt.Color(28, 29, 25) : new java.awt.Color(22, 23, 20));
                g.fillRect(x, y, tile, tile);
            }
        }
        g.setColor(new java.awt.Color(94, 76, 42));
        for (int x = 0; x < w; x += tile) g.drawLine(x, 0, x, h);
        for (int y = 0; y < h; y += tile) g.drawLine(0, y, w, y);
        g.setFont(uiFont.deriveFont(java.awt.Font.BOLD, 18f));
        g.setColor(new java.awt.Color(225, 205, 140));
        g.drawString("THE MECHANIST - CLIENT SURFACE", 24, 36);
        g.setFont(smallFont);
        g.setColor(new java.awt.Color(205, 210, 195));
        int y = 66;
        for (String line : java.util.List.of(
                "Game surface bridge is painting from GamePanel state.",
                "Screen=" + screen + " Panel=" + panelMode + " Turn=" + turn + " WorldTurn=" + worldTurn,
                "Position=" + playerX + "," + playerY + "  Inventory=" + inventory.size() + "  Log=" + eventLog.size(),
                "Assets root property=" + System.getProperty("mechanist.assetRoot", "."),
                "Generated asset root property=" + System.getProperty("mechanist.generatedAssetRoot", "."))) {
            g.drawString(line, 24, y);
            y += 20;
        }
    }
    private void drawVisibleBootStatus(java.awt.Graphics2D g, int w, int h) {
        g.setFont(titleFont.deriveFont(java.awt.Font.BOLD, Math.max(28f, Math.min(52f, h / 10f))));
        g.setColor(new java.awt.Color(218, 198, 126));
        center(g, "THE MECHANIST", w / 2, Math.max(78, h / 5));
        g.setFont(uiFont);
        g.setColor(new java.awt.Color(205, 210, 195));
        int y = Math.max(140, h / 5 + 54);
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add("Compatibility bridge boot surface active.");
        lines.add("Screen: " + screen + "  Panel: " + panelMode + "  Turn: " + turn);
        lines.add("Runtime: " + (jvmRuntimeProfile == null ? "none" : jvmRuntimeProfile.targetLabel()));
        lines.add("Events: " + eventLog.size() + "  Assets facade: " + (images == null ? "missing" : "ready"));
        lines.add("This fallback confirms Swing is painting while the full client surface is reconnected.");
        for (String line : lines) {
            center(g, line, w / 2, y);
            y += Math.max(22, g.getFontMetrics().getHeight() + 4);
        }
    }

    private void drawBridgeException(java.awt.Graphics2D g, int w, int h, Throwable t) {
        g.setColor(new java.awt.Color(36, 8, 8));
        g.fillRect(0, 0, w, h);
        g.setFont(uiFont);
        g.setColor(new java.awt.Color(255, 180, 150));
        int y = 48;
        g.drawString("The Mechanist bridge renderer caught an exception:", 32, y);
        y += 28;
        g.drawString(t.getClass().getName() + ": " + String.valueOf(t.getMessage()), 32, y);
        y += 28;
        for (StackTraceElement element : t.getStackTrace()) {
            if (y > h - 24) break;
            g.drawString("  at " + element.toString(), 32, y);
            y += 18;
        }
    }
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
    void throwSelectedPortableLight() { logEvent("A portable light is thrown into the dark."); }
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
    void shutdownRuntime() {}

    void toggleTacticalSlate() { openPanel(PanelMode.INFO); }
    void openChatWindow() { openPanel(PanelMode.CONSOLE); }
    boolean worldZoomControlActive() { return true; }
    void changeWorldZoom(int delta, String source) { logEvent("World zoom changed by " + delta + " via " + source + "."); }
    void continueFromIntroCrawl() { setScreen(Screen.GAME); }
    void continueFromZoneSplash() { setScreen(Screen.GAME); }
    void finishBootSequence(String source) { setScreen(Screen.MENU); logEvent("Boot sequence finished by " + source + "."); }
    void acceptEulaGate() { eulaGateActive = false; logEvent("EULA accepted."); }
    void scrollEulaGate(int delta, boolean page) { eulaScroll = Math.max(0, Math.min(Math.max(0, eulaMaxScroll), eulaScroll + delta * (page ? 10 : 1))); }
    void openKnowledgeMenu() { setScreen(Screen.KNOWLEDGE); }
    Rectangle graphicsDropdownInnerRect() { return new Rectangle(0, 0, Math.max(1, getWidth()), Math.max(1, getHeight())); }
    int scaled(int value) { return value; }
    void clampInteractCursorToAdjacent() { lookX = Math.max(playerX - 1, Math.min(playerX + 1, lookX)); lookY = Math.max(playerY - 1, Math.min(playerY + 1, lookY)); }
    void updatePendingInteractionSummary() { lastTargetingReport = "Interaction target " + lookX + "," + lookY; }
    void auditItemLedgers(String reason) { lastItemLedgerAuditReport = "Item ledger audit: " + (reason == null ? "unspecified" : reason); }
    void migrateLegacyPhysicalScript(String reason) {}
    void rebuildItemContainersFromLegacyLists() { ensureContainer(CONTAINER_PLAYER_INVENTORY, "Player inventory"); ensureContainer(CONTAINER_BASE_STORAGE, "Base storage"); }
    void repairLegacyListsFromContainersIfNeeded() {}
    void initFactionState() {}
    void seedNpcFactionProductionSites() {}
    void configureBaseObject(BaseObject b) {}
    int activePortableLightRadius() { return activePortableLightItem == null || activePortableLightItem.isBlank() ? 0 : 6; }
    boolean sameWorldLocation(String worldKey) { return true; }
    int portableLightIntensity(String itemName) { return 100; }
    int ambientLightLevelForWorld() { return 50; }

    boolean hasLineOfSight(int x0, int y0, int x1, int y1) { return true; }
    boolean hasKnowledge(String knowledge) { return knowledge == null || knowledge.isBlank() || unlockedKnowledges.contains(knowledge); }
    BaseObject requiredMachineFor(CraftingRecipe recipe) {
        if (recipe == null) return null;
        char symbol = recipe.machineSymbol == ' ' ? 'w' : recipe.machineSymbol;
        BaseObject exact = firstBaseObject(symbol);
        if (exact != null) return exact;
        return baseObjects.isEmpty() ? null : baseObjects.get(0);
    }
    void consumeInventoryNamed(String item) {
        if (item == null || item.isBlank()) return;
        for (Iterator<String> it = inventory.iterator(); it.hasNext();) if (ItemQuality.namesMatch(it.next(), item)) { it.remove(); return; }
        for (Iterator<String> it = baseStorage.iterator(); it.hasNext();) if (ItemQuality.namesMatch(it.next(), item)) { it.remove(); return; }
    }
    String cappedProductionQuality(BaseObject machine, String requiredKnowledge) { return machine == null || machine.qualityName == null || machine.qualityName.isBlank() ? "Common" : machine.qualityName; }
    int availableRecruitLabor() { return Math.max(0, factionRecruits.size()); }
    int stat(String statName, int fallback) { return Math.max(0, fallback); }
    void rememberItemProvenance(String item, ItemProvenanceRecord record) {
        if (item == null || item.isBlank() || record == null) return;
        ArrayDeque<ItemProvenanceRecord> q = itemProvenance.computeIfAbsent(item, k -> new ArrayDeque<>());
        q.addLast(record);
        while (q.size() > 12) q.removeFirst();
    }
    ItemProvenanceRecord takeProvenanceForItem(String item) {
        if (item == null || item.isBlank()) return null;
        ArrayDeque<ItemProvenanceRecord> q = itemProvenance.get(item);
        return q == null || q.isEmpty() ? null : q.removeFirst();
    }
    String persistentContainerIdForObject(MapObjectState obj) { return obj == null ? "object-none" : "object-" + Math.abs(Objects.hash(obj.type, obj.x, obj.y, obj.label)); }
    int containerItemCount(String containerId) { ContainerRecord c = itemContainers.get(containerId); return c == null ? 0 : c.itemInstanceIds.size(); }
    String containerNextItemSummary(String containerId) {
        ContainerRecord c = itemContainers.get(containerId);
        if (c == null || c.itemInstanceIds.isEmpty()) return "empty";
        ItemInstance inst = itemInstances.get(c.itemInstanceIds.get(0));
        return inst == null ? "unknown item" : inst.displayName;
    }
    boolean isInClaimedRoom(int x, int y) { return !baseClaimed || claimedRoomId < 0 || (Math.abs(x - baseX) <= 12 && Math.abs(y - baseY) <= 12); }
    Faction playerFaction() { return baseClaimed ? Faction.HIVER : Faction.NONE; }
    boolean playerIsFactionMember(Faction faction) { return faction == null || faction == Faction.NONE || sameFactionFamily(faction, playerFaction()); }
    boolean sameFactionFamily(Faction a, Faction b) { return sameFactionFamilyStatic(a, b); }
    String baseDisplayName() { return claimedRoomId >= 0 ? "Claimed room " + claimedRoomId : "claimed base"; }
    int machineQualityTier(BaseObject machine) { return QualityAuthorityApi.tierIndex(machine == null ? "Common" : machine.qualityName); }
    ProductionInputConsumptionRecord consumeProductionInputNamedResult(String item, String route) { return ProductionContainerAuthority.consumeOne(this, item, route); }
    BaseObject firstBaseObject(char symbol) { for (BaseObject b : baseObjects) if (b != null && b.symbol == symbol) return b; return null; }
    void recordPlayerNewsEvent(String kind, String siteName, Faction faction, String text, int attention) { logEvent(text == null || text.isBlank() ? "Player news event recorded." : text); }
    Point nearestMedicalFacilityPoint() { return new Point(playerX, playerY); }
    BaseObject baseObjectAt(int x, int y) { for (BaseObject b : baseObjects) if (b != null && b.x == x && b.y == y) return b; return null; }
    int recruitCapacity() { return Math.max(4, factionRecruits.size()); }
    int securityStaffCount() { return 0; }
    boolean buttonIsModalInteractive(ButtonBox button) { return button != null; }
    void markZoneVisitedAndCheckFirstType() {
        if (atlas != null) visitedZoneInstances.add(atlas.sectorX + ":" + atlas.sectorY + ":" + atlas.floor + ":" + atlas.zoneX + ":" + atlas.zoneY + ":" + atlas.sewer);
    }
    boolean verifyItemOperationalParity(String context) { return true; }
    void purgePhysicalScriptInstances(String reason) {}

    ItemInstance transferContainerItemByName(String fromContainerId, ArrayList<String> fromLegacy, String itemName,
                                             String toContainerId, String toLabel, ArrayList<String> toLegacy,
                                             String reason) {
        if (itemName == null || itemName.isBlank()) return null;
        ensureContainer(fromContainerId, fromContainerId == null ? "source" : fromContainerId);
        ensureContainer(toContainerId, toLabel == null ? toContainerId : toLabel);
        ItemInstance found = null;
        ContainerRecord from = itemContainers.get(fromContainerId);
        if (from != null) {
            Iterator<String> it = from.itemInstanceIds.iterator();
            while (it.hasNext()) {
                String id = it.next();
                ItemInstance inst = itemInstances.get(id);
                if (inst != null && ItemQuality.namesMatch(inst.displayName, itemName)) {
                    found = inst;
                    it.remove();
                    break;
                }
            }
        }
        if (found == null && fromLegacy != null) {
            for (Iterator<String> it = fromLegacy.iterator(); it.hasNext();) {
                String s = it.next();
                if (ItemQuality.namesMatch(s, itemName)) {
                    it.remove();
                    String id = "legacy-transfer-" + (itemInstances.size() + 1);
                    found = new ItemInstance(id, s, toContainerId, "legacy-transfer", null);
                    itemInstances.put(id, found);
                    break;
                }
            }
        }
        if (found == null) return null;
        found.containerId = toContainerId == null ? "unknown.container" : toContainerId;
        ContainerRecord to = itemContainers.get(found.containerId);
        if (to != null && !to.itemInstanceIds.contains(found.id)) to.itemInstanceIds.add(found.id);
        if (toLegacy != null) toLegacy.add(found.displayName);
        return found;
    }

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
    void ensureContainer(String containerId, String label) { if (containerId != null && !containerId.isBlank()) itemContainers.putIfAbsent(containerId, new ContainerRecord(containerId, label == null ? containerId : label)); }
    void addItemToContainerResult(String containerId, String label, String item, ItemProvenanceRecord provenance, Object source, String reason) {
        ensureContainer(containerId, label);
        if (item == null || item.isBlank()) return;
        String id = "legacy-item-" + (itemInstances.size() + 1);
        ItemInstance inst = new ItemInstance(id, item, containerId, provenance == null ? "legacy" : provenance.unitId, provenance);
        itemInstances.put(id, inst);
        ContainerRecord c = itemContainers.get(containerId);
        if (c != null) c.itemInstanceIds.add(id);
    }
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
    boolean inputActive() { return false; }
    String directInput() { return ""; }
    java.util.List<MultiplayerMenuController.ConnectionHistoryItem> history() { return java.util.Collections.emptyList(); }
    int historyIndex() { return -1; }
    java.util.List<MultiplayerMenuController.FavoriteServer> favorites() { return java.util.Collections.emptyList(); }
    int favoriteIndex() { return -1; }
    String status() { return "Multiplayer menu unavailable in legacy bridge."; }
    boolean hasActiveHost() { return false; }
    String activeHostLine() { return "No active host."; }
}

final class LegacySoundSurface {
    void play(String key, GameOptions options) {}
    void playDistantCue(String key, int distance, GameOptions options) { play(key, options); }
    void setMusicVolume(GameOptions options) {}
}

final class LegacyRenderScaling {
    void applyOptions(GameOptions options) {}
    String auditSummary() { return "legacyRenderScaling active"; }
    int internalWidth() { return 1280; }
    int internalHeight() { return 720; }
    String profileLabel() { return "Legacy bridge"; }
    String downscaleLabel() { return "1x"; }
}

final class LegacyFrameLimiter {
    void configure(GameOptions options) {}
    LegacyFrameLimiterSnapshot snapshot(boolean stressActive) { return new LegacyFrameLimiterSnapshot(stressActive); }
}

final class LegacyFrameLimiterSnapshot {
    final boolean stressActive;
    LegacyFrameLimiterSnapshot(boolean stressActive) { this.stressActive = stressActive; }
    String compactLine() { return stressActive ? "stress test active" : "nominal"; }
}

final class LegacyImageSurface {
    private final java.util.Map<String, BufferedImage> cache = new java.util.HashMap<>();
    void reloadArtQuality(GameOptions options) { cache.clear(); }
    BufferedImage get(String key) {
        if (key == null || key.isBlank()) return null;
        return cache.computeIfAbsent(key, this::loadByKey);
    }
    BufferedImage getTile(char tile) { return get("tile_" + ((int) tile)); }
    BufferedImage getNpcPortraitFor(Object npc) { return get("portrait_" + (npc == null ? "unknown" : npc.getClass().getSimpleName())); }

    private BufferedImage loadByKey(String key) {
        java.util.List<String> names = candidateNames(key);
        java.util.List<java.nio.file.Path> roots = assetRoots();
        for (java.nio.file.Path root : roots) {
            for (String name : names) {
                java.nio.file.Path found = findCaseInsensitive(root, name);
                if (found != null) {
                    try {
                        BufferedImage image = javax.imageio.ImageIO.read(found.toFile());
                        if (image != null) return image;
                    } catch (java.io.IOException ignored) {}
                }
            }
        }
        if ("title_mechanist_rebase".equalsIgnoreCase(key) || "title_mechanist".equalsIgnoreCase(key)) return generatedTitle();
        if ("subtitle_rebase".equalsIgnoreCase(key)) return generatedSubtitle();
        return null;
    }

    private java.util.List<String> candidateNames(String key) {
        java.util.ArrayList<String> out = new java.util.ArrayList<>();
        String k = key.trim();
        String lower = k.toLowerCase(java.util.Locale.ROOT);
        for (String base : new String[] { k, lower, k.replace('_', '-'), lower.replace('_', '-') }) {
            out.add(base);
            for (String ext : new String[] { ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".webp" }) out.add(base + ext);
        }
        return out;
    }

    private java.util.List<java.nio.file.Path> assetRoots() {
        java.util.ArrayList<java.nio.file.Path> roots = new java.util.ArrayList<>();
        java.nio.file.Path cwd = java.nio.file.Paths.get("").toAbsolutePath().normalize();
        roots.add(cwd.resolve("assets"));
        roots.add(cwd.resolve("PACKAGE_client").resolve("assets"));
        roots.add(cwd.resolve("client").resolve("assets"));
        roots.add(cwd.resolve("resources").resolve("assets"));
        String configured = System.getProperty("mechanist.assetRoot", "");
        if (configured != null && !configured.isBlank()) roots.add(java.nio.file.Paths.get(configured).toAbsolutePath().normalize().resolve("assets"));
        return roots;
    }

    private java.nio.file.Path findCaseInsensitive(java.nio.file.Path root, String wanted) {
        if (root == null || wanted == null || !java.nio.file.Files.isDirectory(root)) return null;
        java.nio.file.Path direct = root.resolve(wanted);
        if (java.nio.file.Files.isRegularFile(direct)) return direct;
        String target = wanted.replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.walk(root)) {
            return stream.filter(java.nio.file.Files::isRegularFile)
                    .filter(p -> p.getFileName() != null)
                    .filter(p -> {
                        String file = p.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
                        String rel = root.relativize(p).toString().replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
                        return file.equals(target) || rel.equals(target) || file.startsWith(target + ".");
                    })
                    .findFirst().orElse(null);
        } catch (java.io.IOException ignored) { return null; }
    }

    private BufferedImage generatedTitle() {
        BufferedImage img = new BufferedImage(760, 130, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new java.awt.Color(20, 18, 14, 230));
        g.fillRoundRect(0, 0, img.getWidth(), img.getHeight(), 22, 22);
        g.setColor(new java.awt.Color(190, 155, 80));
        g.setStroke(new java.awt.BasicStroke(4f));
        g.drawRoundRect(4, 4, img.getWidth() - 9, img.getHeight() - 9, 22, 22);
        g.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 64));
        g.setColor(new java.awt.Color(225, 205, 140));
        String title = "THE MECHANIST";
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (img.getWidth() - fm.stringWidth(title)) / 2, 84);
        g.dispose();
        return img;
    }

    private BufferedImage generatedSubtitle() {
        BufferedImage img = new BufferedImage(500, 46, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new java.awt.Font("Monospaced", java.awt.Font.BOLD, 20));
        g.setColor(new java.awt.Color(170, 150, 105));
        String text = "UNFOLDED CLIENT BRIDGE";
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (img.getWidth() - fm.stringWidth(text)) / 2, 30);
        g.dispose();
        return img;
    }
}

final class LegacyFirstPersonRenderViewport {
    boolean handleKeyPressed(GamePanel panel, int code) { return false; }
    boolean handleMouseClicked(GamePanel panel, java.awt.event.MouseEvent event, int mx, int my) { return false; }
}

final class LegacyRenderStressTest {
    private boolean active;
    String toggle(LegacyFrameLimiter limiter) { active = !active; return active ? "Render stress test active." : "Render stress test inactive."; }
    boolean active() { return active; }
}

final class LegacyPerformanceDiagnostics {
    private boolean visible;
    String toggle() { visible = !visible; return visible ? "Performance diagnostics visible." : "Performance diagnostics hidden."; }
    void setVisible(boolean visible) { this.visible = visible; }
    boolean visible() { return visible; }
    String auditSummary() { return "legacyPerformanceDiagnostics visible=" + visible; }
}

final class LegacyPanelProfile {
    String name = "none";
}

final class LegacyGamepadInputEngine {
    String status() { return "not started"; }
}








