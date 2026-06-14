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
    static final long PASSIVE_TURN_INTERVAL_MILLIS = 2600L;
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
    static final int INFOPEDIA_TAB_ROW_HEIGHT = 28;
    static final int INFOPEDIA_TAB_GAP = 6;
    static final int INFOPEDIA_ROW_HEIGHT = 22;

    enum Screen { BOOT, INTRO_CRAWL, ZONE_SPLASH, CAPTURE, MENU, MAIN, CHARACTER, GAME, PANEL, OPTIONS, INVENTORY, INFO, MAP, PAUSE, MODS, KNOWLEDGE, MULTIPLAYER, SAVE_LOAD, SECTOR_AUDIT, EDITOR }
    enum PanelMode { NONE, CHARACTER, INVENTORY, CONTAINER, TRADE, DIALOGUE, OBJECT, LOOK, INTERACT, COMBAT, AUSPEX, CONSOLE, INFO, INFOPEDIA, BUILD, WORKBENCH, MAP, CRAFTING, SCAVENGE }

    World world;
    WorldAtlas atlas;
    GameOptions options = new GameOptions();
    Candidate active;
    UserProfileAuthority.Profile userProfile = UserProfileAuthority.detect();
    SinglePlayerSectorRuntimeBridge singlePlayerSectorBridge;
    LegacyPerformanceDiagnostics performanceDiagnostics = new LegacyPerformanceDiagnostics();
    LegacyKeyboardInputBridge keyboardInputBridge = new LegacyKeyboardInputBridge();
    MultiplayerMenuController multiplayerMenu = new MultiplayerMenuController();
    LegacySoundSurface sounds = new LegacySoundSurface();
    LegacyRenderScaling renderScaling = new LegacyRenderScaling();
    LegacyFrameLimiter frameLimiter = new LegacyFrameLimiter();
    LegacyImageSurface images = new LegacyImageSurface();
    FirstPersonRenderViewport firstPersonRenderViewport = new FirstPersonRenderViewport();
    LegacyRenderStressTest renderStressTest = new LegacyRenderStressTest();
    JvmRuntimeProfileAuthority.RuntimeConfig jvmRuntimeProfile = JvmRuntimeProfileAuthority.load();
    javax.swing.Timer timer;

    final ArrayList<String> inventory = new ArrayList<>();
    final ArrayList<String> eventLog = new ArrayList<>();
    final ArrayList<SectorGenerationTraceAuthority.Step> auditTraceSteps = new ArrayList<>();
    final ArrayList<BaseObject> baseObjects = new ArrayList<>();
    final ArrayList<String> baseStorage = new ArrayList<>();
    final ArrayList<Candidate> candidates = new ArrayList<>();
    final ArrayList<RecruitWorker> factionRecruits = new ArrayList<>();
    final ArrayList<ButtonBox> buttons = new ArrayList<>();
    final EditorEventBus inGameEditorEvents = new EditorEventBus();
    final EditorUndoRedoController inGameEditorHistory = new EditorUndoRedoController(inGameEditorEvents);
    final SimulationEditorRepository inGameEditorRepository = new SimulationEditorRepository();
    final ModDeploymentManager inGameModDeployment = new ModDeploymentManager(inGameEditorEvents);
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
    final ArrayList<Point> manualMovementPlanPath = new ArrayList<>();
    final HashMap<Integer, String> innDailyIssues = new HashMap<>();
    final HashMap<String, Boolean> consoleFlags = new HashMap<>();
    final HashMap<String, Float> consoleNumericFlags = new HashMap<>();
    final HashMap<String, String> consoleStringFlags = new HashMap<>();
    WorldSetupSettings worldSetup = WorldSetupSettings.standard();
    String inGameEditorName = SimulationToolSuiteRegistry.fallbackEditor();
    String inGameEditorStatus = "Live project defaults loaded; edits will be exported as a mod package.";
    int inGameEditorEntityIndex;
    int inGameEditorPropertyIndex;
    boolean inGameEditorTextEditActive;
    String inGameEditorTextBuffer = "";
    Clothing equippedClothing = Clothing.scavengerRags();
    VisualLightingAuthority visualLighting = new VisualLightingAuthority();
    NpcTurnBudgetScheduler npcTurnBudgetScheduler = new NpcTurnBudgetScheduler();
    AbstractDistantZoneSimulation abstractDistantZoneSimulation = new AbstractDistantZoneSimulation();
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
    int[][] lightLevels = new int[1][1];
    World sensoryWorldRef;
    int sensoryTurn = Integer.MIN_VALUE;
    int sensoryPlayerX = Integer.MIN_VALUE;
    int sensoryPlayerY = Integer.MIN_VALUE;
    int sensoryLightRevision = Integer.MIN_VALUE;
    int sensoryNoiseRevision = Integer.MIN_VALUE;
    int sensoryVisionRevision = Integer.MIN_VALUE;
    int sensoryPortableSignature = Integer.MIN_VALUE;
    int sensoryMovementMode = Integer.MIN_VALUE;
    int sensoryFacingDx = Integer.MIN_VALUE;
    int sensoryFacingDy = Integer.MIN_VALUE;
    String lastSensoryModelReport = "No visual sensory model has been rebuilt.";
    String lastNpcRuntimeReport = "No NPC needs/duty runtime tick has run.";
    String lastAbstractDistantZoneReport = "No abstract distant-zone simulation tick has run.";
    String lastEconomyRuntimeReport = "No persistent economy runtime tick has run.";
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
    boolean mouseMovePreviewHazardous;
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
    int infopediaSelectionIndex;
    int infopediaListScroll;
    int infopediaDetailScroll;
    int controlsTab;
    int lookStackIndex;
    int lookStackScroll;
    int lookFocusX = Integer.MIN_VALUE;
    int lookFocusY = Integer.MIN_VALUE;
    int lookFocusDepth;
    int inventoryItemDescriptionScroll;
    int selectedInventoryIndex;
    int selectedCharacterEquipmentSlot;
    int selectedTargetInventoryIndex;
    int selectedTradeOfferIndex;
    int selectedContainerItemIndex;
    int selectedMovementModeIndex = MOTION_WALK;
    int mouseMovePreviewTargetX;
    int mouseMovePreviewTargetY;
    int facingDx = 1;
    int facingDy = 0;
    int playerMotionFromX;
    int playerMotionFromY;
    int playerMotionToX;
    int playerMotionToY;
    int playerMotionDurationMillis;
    long playerMotionStartedMillis;
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
    int saveLoadSelectedIndex;
    int lastWorldViewTileSize = 16;
    int lastWorldViewOriginX;
    int lastWorldViewOriginY;
    int lastWorldViewMinX;
    int lastWorldViewMinY;
    int lastAuditViewTileSize = 16;
    int lastAuditViewOriginX;
    int lastAuditViewOriginY;
    int lastAuditViewMinX;
    int lastAuditViewMinY;
    int lastAuditViewCols = 1;
    int lastAuditViewRows = 1;
    int auditFindingIndex;
    int auditZoneTypeIndex;
    int auditZoneDensityIndex;
    int auditOverlayIndex;
    int auditTraceIndex;
    int auditTraceStepMillis = 900;
    int selectedFireModeIndex;
    int generatedJobCategoryFilterIndex;
    int generatedJobReadinessFilterIndex;
    int mouseX = -1;
    int mouseY = -1;
    boolean baseClaimed;
    boolean characterNameEditActive;
    boolean manualMovementPlanActive;
    boolean manualMovementPlanHazardous;
    boolean buildPlacementActive;
    boolean newGameSetupActive;
    boolean auditTracePlaying;
    Screen screen = Screen.BOOT;
    Screen optionsReturnScreen = Screen.MENU;
    PanelMode panelMode = PanelMode.NONE;
    PanelMode optionsReturnPanelMode = PanelMode.NONE;
    BuildRecipe pendingBuildRecipe;
    int buildRecipePage;
    int buildRecipeCategoryIndex;
    CraftingRecipe selectedCraftingRecipe;
    SectorAuditRuntimeAuthority.AuditSnapshot auditSnapshot;
    String selectedKnowledgeNodeId;
    String lastAccessibleNarration = "";
    String activeScrollTag = "";
    String infopediaAssetFilter = "";
    String activeInteractionTitle = "";
    String activeInteractionKind = "";
    String activeInteractionContainerId = "";
    String rebindingTarget = "";
    String saveLoadStatus = "Select a save slot.";
    String jvmRuntimeNotice = "";
    String lastTargetingReport = "No target selected.";
    MovementDebugOverlayAuthority.MovementDebugSnapshot movementDebugSnapshot = MovementDebugOverlayAuthority.MovementDebugSnapshot.idle();
    final ArrayList<QuestObjectiveGuidanceAuthority.ObjectiveGuidance> activeQuestGuidance = new ArrayList<>();
    String equippedLeftHandItem = "LEFT EMPTY";
    String equippedRightHandItem = "RIGHT EMPTY";
    int activeWeaponHandIndex = 1; // 0 left, 1 right.
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
    long lastPassiveAmbientMillis = 0L;
    long lastPassiveWorldTickMillis = 0L;
    long auditTraceStartedMillis;
    int worldSetupSelection;
    Font titleFont = new Font("Monospaced", Font.BOLD, 36);
    Font smallFont = new Font("Monospaced", Font.PLAIN, 14);
    Font uiFont = new Font("Monospaced", Font.BOLD, 16);
    Font asciiFont = new Font("Monospaced", Font.BOLD, 13);
    KnowledgeMenu knowledgeMenu;
    NpcEntity activeInteractionNpc;
    MapObjectState activeInteractionObject;
    BaseObject activeInteractionBaseObject;
    TraderSession activeTraderSession;

    GamePanel() {
        setOpaque(true);
        setBackground(new java.awt.Color(18, 17, 14));
        setFocusable(true);
        options = GameOptions.load();
        logEvent("GamePanel compatibility bridge initialized.");
        installExistingEarlyScreenInputBridge();
        installMainMenuInputBridge();
        selectedButton = 0;
        screen = Screen.MENU;
        panelMode = PanelMode.NONE;
        try {
            images.reloadArtQuality(options);
        } catch (Throwable t) {
            DebugLog.error("CLIENT_MENU_BOOT", "Menu media preload failed; menu will use drawn fallbacks.", t);
        }
        if (options.bootSound) sounds.play("boot", options);
        sounds.requestMusic("MAIN_MENU", options);
        timer = new javax.swing.Timer(33, e -> {
            tickPassiveAmbientSounds();
            tickSinglePlayerPassiveWorldTime();
            firstPersonRenderViewport.updateContinuousMotion(this);
            repaint();
        });
        timer.start();
        logEvent("Package client main menu restored.");
        DebugLog.audit("CLIENT_MENU_BOOT", "screen=" + screen + " routeCount=" + mainMenuRouteLabels().size() + " assets=preloaded");
    }
    GamePanel(RuntimeProfile runtimeProfile) {
        this();
        if (runtimeProfile != null) logEvent("Runtime profile attached: " + runtimeProfile.compactLine());
    }
    GamePanel(JvmRuntimeProfileAuthority.RuntimeConfig runtimeProfile) {
        this();
        if (runtimeProfile != null) this.jvmRuntimeProfile = runtimeProfile;
    }


    void installMainMenuInputBridge() {
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override public void mouseMoved(java.awt.event.MouseEvent e) {
                mouseX = e.getX(); mouseY = e.getY();
                if (firstPersonRenderViewport.handleMouseMoved(GamePanel.this, mouseX, mouseY)) return;
                if (screen == Screen.MENU || screen == Screen.MAIN || screen == Screen.BOOT) {
                    int idx = mainMenuRouteIndexAt(mouseX, mouseY);
                    if (idx >= 0 && idx != selectedButton) { selectedButton = idx; repaint(); }
                } else if (!buttons.isEmpty()) {
                    int idx = UiModalButtonController.activeHoverButtonIndex(GamePanel.this);
                    if (idx >= 0 && idx != selectedButton) { selectedButton = idx; repaint(); }
                }
            }

            @Override public void mouseDragged(java.awt.event.MouseEvent e) {
                mouseX = e.getX(); mouseY = e.getY();
                firstPersonRenderViewport.handleMouseDragged(GamePanel.this, mouseX, mouseY);
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (!(screen == Screen.MENU || screen == Screen.MAIN || screen == Screen.BOOT)) return;
                int idx = mainMenuRouteIndexAt(e.getX(), e.getY());
                if (idx >= 0) { selectedButton = idx; activateMainMenuRouteIndex(idx); repaint(); requestFocusInWindow(); }
            }
        });
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (!(screen == Screen.MENU || screen == Screen.MAIN || screen == Screen.BOOT)) return;
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_UP || e.getKeyCode() == java.awt.event.KeyEvent.VK_W) { selectedButton = Math.floorMod(selectedButton - 1, mainMenuRouteLabels().size()); repaint(); return; }
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN || e.getKeyCode() == java.awt.event.KeyEvent.VK_S) { selectedButton = Math.floorMod(selectedButton + 1, mainMenuRouteLabels().size()); repaint(); return; }
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER || e.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) { activateMainMenuRouteIndex(selectedButton); repaint(); return; }
                int digit = e.getKeyCode() - java.awt.event.KeyEvent.VK_1;
                if (digit >= 0 && digit < mainMenuRouteLabels().size()) { selectedButton = digit; activateMainMenuRouteIndex(digit); repaint(); }
            }

        });
        addMouseWheelListener(e -> {
            if (worldZoomControlActive()) {
                changeWorldZoom(e.getWheelRotation() < 0 ? 1 : -1, "mouse-wheel");
                e.consume();
            }
        });
    }

    java.util.List<String> mainMenuRouteLabels() {
        return java.util.List.of("New Game", "Continue", "Load Game", "Options", "InfoPedia", "Multiplayer", "Tools", "Quit");
    }

    Rectangle mainMenuRouteRect(int index) {
        Rectangle frame = mainMenuButtonFrameRect();
        java.util.List<String> labels = mainMenuRouteLabels();
        int count = Math.max(1, labels.size());
        int buttonW = Math.max(300, frame.width - 58);
        int gap = Math.max(7, Math.min(11, frame.height / 38));
        int pad = Math.max(16, Math.min(24, frame.height / 13));
        int availableH = Math.max(1, frame.height - (pad * 2) - (gap * Math.max(0, count - 1)));
        int buttonH = Math.max(26, Math.min(48, availableH / count));
        int usedH = buttonH * count + gap * Math.max(0, count - 1);
        int top = frame.y + Math.max(pad, (frame.height - usedH) / 2);
        int x = getWidth() / 2 - buttonW / 2;
        return new Rectangle(x, top + index * (buttonH + gap), buttonW, buttonH);
    }

    int mainMenuRouteIndexAt(int x, int y) {
        java.util.List<String> labels = mainMenuRouteLabels();
        for (int i = 0; i < labels.size(); i++) if (mainMenuRouteRect(i).contains(x, y)) return i;
        return -1;
    }

    void activateMainMenuRouteIndex(int index) {
        java.util.List<String> labels = mainMenuRouteLabels();
        if (labels.isEmpty()) return;
        index = Math.floorMod(index, labels.size());
        String route = labels.get(index);
        logEvent("Main menu route selected: " + route);
        switch (route) {
            case "New Game" -> openNewGameSetup();
            case "Continue" -> continueLatestSaveOrOpenPanel();
            case "Load Game" -> openSaveLoadPanel("Load Game selected.");
            case "Options" -> openOptionsScreen("main menu route");
            case "InfoPedia" -> openInfopediaPanel("main menu route");
            case "Multiplayer" -> openMultiplayerPanel();
            case "Tools" -> openToolsMenu();
            case "Quit" -> requestApplicationExit("main menu quit route");
            default -> logEvent("Unhandled main menu route: " + route);
        }
    }

    void openToolsMenu() {
        selectedButton = 0;
        setScreen(Screen.MODS);
        logEvent("Tools menu opened.");
    }

    void openOptionsScreen(String reason) {
        if (screen != Screen.OPTIONS) {
            optionsReturnScreen = (screen == null || screen == Screen.BOOT || screen == Screen.MAIN) ? Screen.MENU : screen;
            optionsReturnPanelMode = panelMode == null ? PanelMode.NONE : panelMode;
            if (WorldStartFlowAuthority.isActive(this)) {
                optionsReturnScreen = Screen.MENU;
                optionsReturnPanelMode = PanelMode.NONE;
            }
        }
        graphicsDropdown = -1;
        setScreen(Screen.OPTIONS);
        DebugLog.audit("CLIENT_MENU_ROUTE", "options opened reason=" + (reason == null ? "unspecified" : reason) + " return=" + optionsReturnScreen + "/" + optionsReturnPanelMode);
    }

    void closeOptionsScreen() {
        graphicsDropdown = -1;
        Screen target = optionsReturnScreen == null ? Screen.MENU : optionsReturnScreen;
        PanelMode targetPanel = optionsReturnPanelMode == null ? PanelMode.NONE : optionsReturnPanelMode;
        if (world == null && target != Screen.MENU && target != Screen.MAIN && target != Screen.SAVE_LOAD && target != Screen.MODS) {
            target = Screen.MENU;
            targetPanel = PanelMode.NONE;
        }
        setScreen(target);
        if (target == Screen.PANEL) panelMode = targetPanel;
        repaint();
        DebugLog.audit("CLIENT_MENU_ROUTE", "options closed return=" + target + "/" + panelMode);
    }

    void openNewGameSetup() {
        WorldStartFlowAuthority.install(this);
        WorldStartFlowAuthority.openWorldPicker(this, "new game route");
        if (WorldStartFlowAuthority.isActive(this)) return;
        seed = System.currentTimeMillis();
        rng = new Random(seed ^ 0x4E475345545550L);
        worldSetup = WorldSetupSettings.standard();
        prepareNewGameRoster(seed);
        candidateIndex = 0;
        active = selectedNewGameCandidate();
        newGameSetupActive = true;
        characterNameEditActive = false;
        panelMode = PanelMode.NONE;
        selectedButton = 0;
        setScreen(Screen.CHARACTER);
        logEvent("Legacy new game setup opened: choose a character and world profile.");
        DebugLog.audit("CLIENT_MENU_ROUTE", "legacyNewGameSetup opened candidates=" + candidates.size() + " setup=" + worldSetup.shortSummary());
    }

    void prepareNewGameRoster(long rosterSeed) {
        candidates.clear();
        Random rr = new Random(rosterSeed ^ 0xC0FFEE51L);
        for (int i = 0; i < 8; i++) {
            Candidate c = Candidate.random(new Random(rr.nextLong() ^ (i * 0x9E3779B97F4A7C15L)));
            AgeAndWorldTimeAuthority.initializeCandidateAge(c, new Random(rosterSeed ^ (0xA6E5L + i * 37L)));
            candidates.add(c);
        }
        candidateIndex = 0;
        active = selectedNewGameCandidate();
        DebugLog.audit("CHARACTER_CREATION", "prepared roster " + CharacterCreationAuditApi.audit(candidates).toLogBlock());
    }

    Candidate selectedNewGameCandidate() {
        if (candidates.isEmpty()) prepareNewGameRoster(System.currentTimeMillis());
        candidateIndex = Math.max(0, Math.min(candidateIndex, candidates.size() - 1));
        return candidates.get(candidateIndex);
    }

    void cycleSelectedCandidate(int delta) {
        if (candidates.isEmpty()) prepareNewGameRoster(System.currentTimeMillis());
        candidateIndex = Math.floorMod(candidateIndex + delta, candidates.size());
        active = selectedNewGameCandidate();
        characterNameEditActive = false;
        logEvent("Selected candidate " + active.name + ".");
        repaint();
    }

    void cycleSelectedCandidateJob(int delta) {
        Candidate c = selectedNewGameCandidate();
        if (c == null || c.jobs.isEmpty()) return;
        int idx = c.jobs.indexOf(c.job);
        if (idx < 0) idx = 0;
        c.job = c.jobs.get(Math.floorMod(idx + delta, c.jobs.size()));
        active = c;
        logEvent(c.name + " job set to " + c.job + ".");
        repaint();
    }

    void rerollSelectedCandidate() {
        if (candidates.isEmpty()) prepareNewGameRoster(System.currentTimeMillis());
        Random rr = new Random(System.nanoTime() ^ seed ^ candidateIndex);
        Candidate replacement = Candidate.random(rr);
        AgeAndWorldTimeAuthority.initializeCandidateAge(replacement, new Random(rr.nextLong()));
        candidates.set(candidateIndex, replacement);
        active = replacement;
        characterNameEditActive = false;
        logEvent("Rerolled candidate " + replacement.name + ".");
        repaint();
    }

    void rerollNewGameRoster() {
        seed = System.currentTimeMillis();
        rng = new Random(seed ^ 0x4E475345545550L);
        prepareNewGameRoster(seed);
        characterNameEditActive = false;
        logEvent("New candidate roster generated.");
        repaint();
    }

    void cycleWorldSetupOption(int option) {
        if (worldSetup == null) worldSetup = WorldSetupSettings.standard();
        worldSetupSelection = Math.max(0, option);
        switch (worldSetupSelection) {
            case 0 -> worldSetup.cycleNpcDensity();
            case 1 -> worldSetup.cycleZoneSize();
            case 2 -> worldSetup.cycleZoneDensity();
            case 3 -> worldSetup.cyclePriceDifficulty();
            case 4 -> worldSetup.cycleCraftDifficulty();
            case 5 -> worldSetup.hoarderMode = !worldSetup.hoarderMode;
            case 6 -> worldSetup.cycleSimulationAge();
            default -> {}
        }
        logEvent("World setup: " + worldSetup.shortSummary() + ".");
        repaint();
    }

    void startPackagedClientNewGame() {
        WorldSetupSettings setup = newGameSetupActive && worldSetup != null ? worldSetup.copy() : WorldSetupSettings.standard();
        Candidate chosen;
        if (newGameSetupActive) {
            Candidate selected = selectedNewGameCandidate();
            chosen = selected == null ? null : selected.copy();
        } else {
            chosen = Candidate.random(new Random(System.currentTimeMillis() ^ 0xC0FFEE));
            AgeAndWorldTimeAuthority.initializeCandidateAge(chosen, new Random(System.currentTimeMillis() ^ 0xA6E5L));
        }
        startPackagedClientNewGameWith(chosen, setup);
    }

    void startPackagedClientNewGameWith(Candidate chosen, WorldSetupSettings setup) {
        long newSeed = seed == 0L || (!newGameSetupActive && atlas == null) ? System.currentTimeMillis() : seed;
        seed = newSeed;
        rng = new Random(seed);
        worldSetup = setup == null ? WorldSetupSettings.standard() : setup.copy();
        WorldAtlas preparedAtlas = atlas;
        boolean reusedPreparedWorld = preparedAtlasMatches(preparedAtlas, seed, worldSetup);
        if (!reusedPreparedWorld) {
            preparedAtlas = WorldAtlas.createNew(seed, worldSetup);
            preparedAtlas.generateScaffold();
        }
        atlas = preparedAtlas;
        world = atlas.currentWorld();
        visibleTiles = new boolean[world.w][world.h];
        rememberedTiles = new boolean[world.w][world.h];
        lightLevels = new int[world.w][world.h];
        sensoryWorldRef = null;
        Point start = world.startPoint();
        playerX = Math.max(0, Math.min(world.w - 1, start.x));
        playerY = Math.max(0, Math.min(world.h - 1, start.y));
        lookX = playerX;
        lookY = playerY;
        combatX = playerX;
        combatY = playerY;
        buildX = playerX;
        buildY = playerY;
        active = chosen == null ? Candidate.random(new Random(seed ^ 0xC0FFEE)) : chosen;
        if (active.job == null || active.job.isBlank()) active.job = "Underhive Scavenger";
        inventory.clear();
        baseStorage.clear();
        baseObjects.clear();
        factionRecruits.clear();
        itemContainers.clear();
        itemInstances.clear();
        itemProvenance.clear();
        portableLights.clear();
        factionContracts.clear();
        npcFactionSites.clear();
        factionStrategicPlans.clear();
        playerNewsEvents.clear();
        visitedZoneInstances.clear();
        visitedZoneTypes.clear();
        unlockedKnowledges.clear();
        unlockedKnowledges.add("Underhive Basics");
        turn = 0;
        worldTurn = 0L;
        carriedScript = 15;
        baseStashedScript = 0;
        supplies = 0;
        machineParts = 0;
        xp = 0;
        knowledgeCredits = 0;
        food = MAX_FOOD_WATER;
        water = MAX_FOOD_WATER;
        fatigue = 0;
        wounds = 0;
        bleeding = 0;
        infectionRisk = 0;
        pain = 0;
        sleepNeed = 0;
        suspicion = 0;
        gangHeat = 0;
        inventory.add("Ration pack");
        inventory.add("Water flask");
        inventory.add("Scrap knife");
        JobProfile profile = JobProfile.get(active.job);
        if (profile != null) {
            for (String item : profile.startingItems()) if (item != null && !item.isBlank() && !inventory.contains(item)) inventory.add(item);
            equippedClothing = profile.clothing();
        }
        rebuildItemContainersFromLegacyLists();
        initFactionState();
        seedNpcFactionProductionSites();
        markZoneVisitedAndCheckFirstType();
        updateSensoryModel("new game");
        newGameSetupActive = false;
        characterNameEditActive = false;
        buttons.clear();
        screen = Screen.GAME;
        panelMode = PanelMode.NONE;
        logEvent("New game started: " + world.zoneCoordText() + " seed=" + seed + " / " + worldSetup.shortSummary() + ".");
        DebugLog.audit("CLIENT_MENU_ROUTE", "newGame seed=" + seed + " setup=" + worldSetup.shortSummary() + " reusedPreparedWorld=" + reusedPreparedWorld + " character=" + active.name + "/" + active.job + " world=" + world.zoneCoordText() + " player=" + playerX + "," + playerY);
    }

    boolean preparedAtlasMatches(WorldAtlas preparedAtlas, long requestedSeed, WorldSetupSettings requestedSetup) {
        if (preparedAtlas == null || preparedAtlas.hiveWorld == null) return false;
        if (preparedAtlas.seed != requestedSeed) return false;
        WorldSetupSettings safeSetup = requestedSetup == null ? WorldSetupSettings.standard() : requestedSetup;
        return preparedAtlas.hiveWorld.settings().encode().equals(safeSetup.encode());
    }

    void openMultiplayerPanel() {
        multiplayerMenu.activate(userProfile);
        screen = Screen.MULTIPLAYER;
        panelMode = PanelMode.NONE;
        logEvent("Multiplayer panel opened.");
        DebugLog.audit("CLIENT_MENU_ROUTE", "multiplayer opened " + MultiplayerMenuController.auditSummary());
    }

    int[] saveLoadSlots() {
        return new int[] { 1, 2, 3, AUTOSAVE_HOURLY_SLOT, AUTOSAVE_ZONE_SLOT };
    }

    int selectedSaveLoadSlot() {
        int[] slots = saveLoadSlots();
        if (slots.length == 0) return 1;
        saveLoadSelectedIndex = Math.max(0, Math.min(saveLoadSelectedIndex, slots.length - 1));
        return slots[saveLoadSelectedIndex];
    }

    void openSaveLoadPanel(String status) {
        MovementPlanningFocusResetAuthority.reset(this, "save/load menu");
        int latest = latestExistingSaveSlot();
        int[] slots = saveLoadSlots();
        saveLoadSelectedIndex = 0;
        for (int i = 0; i < slots.length; i++) if (slots[i] == latest) saveLoadSelectedIndex = i;
        saveLoadStatus = status == null || status.isBlank() ? "Select a save slot." : status;
        screen = Screen.SAVE_LOAD;
        panelMode = PanelMode.NONE;
        logEvent("Save/load panel opened.");
        DebugLog.audit("CLIENT_MENU_ROUTE", "saveLoad opened latest=" + latest + " status=" + saveLoadStatus);
    }

    void continueLatestSaveOrOpenPanel() {
        int latest = latestExistingSaveSlot();
        if (latest >= 0 && loadSaveSlot(latest, "Continue")) return;
        openSaveLoadPanel("No saved game was found. Choose a slot or start a new game.");
    }

    int latestExistingSaveSlot() {
        int latestSlot = -1;
        long latestMillis = Long.MIN_VALUE;
        for (int slot : saveLoadSlots()) {
            java.nio.file.Path path = SaveSlotSurfaceApi.savePathForSlot(slot);
            try {
                if (java.nio.file.Files.isRegularFile(path)) {
                    long millis = java.nio.file.Files.getLastModifiedTime(path).toMillis();
                    if (millis > latestMillis) {
                        latestMillis = millis;
                        latestSlot = slot;
                    }
                }
            } catch (Exception ex) {
                DebugLog.warn("SAVE_LOAD_MENU", "Could not inspect save slot " + slot + ": " + ex.getMessage());
            }
        }
        return latestSlot;
    }

    boolean loadSaveSlot(int slot, String reason) {
        java.nio.file.Path path = SaveSlotSurfaceApi.savePathForSlot(slot);
        if (!java.nio.file.Files.isRegularFile(path)) {
            saveLoadStatus = SaveSlotSurfaceApi.slotLabel(slot) + " is empty.";
            logEvent(saveLoadStatus);
            return false;
        }
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(path)) {
            Properties p = new Properties();
            p.load(in);
            Persistence.readCore(this, p);
            MovementPlanningFocusResetAuthority.reset(this, "loaded game");
            screen = Screen.GAME;
            panelMode = PanelMode.NONE;
            selectedButton = 0;
            saveLoadStatus = "Loaded " + SaveSlotSurfaceApi.slotLabel(slot) + ".";
            logEvent(saveLoadStatus);
            DebugLog.audit("CLIENT_MENU_ROUTE", "loaded slot=" + slot + " reason=" + (reason == null ? "unspecified" : reason) + " path=" + path);
            return true;
        } catch (Throwable t) {
            saveLoadStatus = "Could not load " + SaveSlotSurfaceApi.slotLabel(slot) + ": " + t.getMessage();
            logEvent(saveLoadStatus);
            DebugLog.error("SAVE_LOAD_MENU", saveLoadStatus, t);
            return false;
        }
    }

    boolean handleSaveLoadKey(int code) {
        if (screen != Screen.SAVE_LOAD) return false;
        int[] slots = saveLoadSlots();
        if (code == KeyEvent.VK_ESCAPE || code == KeyEvent.VK_Q) {
            setScreen(Screen.MENU);
            return true;
        }
        if (code == KeyEvent.VK_N) {
            openNewGameSetup();
            return true;
        }
        if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W || code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
            saveLoadSelectedIndex = Math.floorMod(saveLoadSelectedIndex - 1, slots.length);
            saveLoadStatus = "Selected " + SaveSlotSurfaceApi.slotLabel(selectedSaveLoadSlot()) + ".";
            return true;
        }
        if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S || code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
            saveLoadSelectedIndex = Math.floorMod(saveLoadSelectedIndex + 1, slots.length);
            saveLoadStatus = "Selected " + SaveSlotSurfaceApi.slotLabel(selectedSaveLoadSlot()) + ".";
            return true;
        }
        int digit = code - KeyEvent.VK_1;
        if (digit >= 0 && digit < Math.min(SAVE_SLOT_COUNT, slots.length)) {
            saveLoadSelectedIndex = digit;
            loadSaveSlot(selectedSaveLoadSlot(), "number key");
            return true;
        }
        if (code == KeyEvent.VK_ENTER || code == KeyEvent.VK_SPACE || code == KeyEvent.VK_E || code == KeyEvent.VK_L) {
            loadSaveSlot(selectedSaveLoadSlot(), "save/load screen");
            return true;
        }
        return false;
    }

    boolean handleSaveLoadClick(java.awt.event.MouseEvent e) {
        if (screen != Screen.SAVE_LOAD || e == null) return false;
        int[] slots = saveLoadSlots();
        for (int i = 0; i < slots.length; i++) {
            if (saveLoadSlotRect(i).contains(e.getX(), e.getY())) {
                saveLoadSelectedIndex = i;
                if (e.getClickCount() >= 2 || javax.swing.SwingUtilities.isLeftMouseButton(e)) loadSaveSlot(selectedSaveLoadSlot(), "mouse");
                else saveLoadStatus = "Selected " + SaveSlotSurfaceApi.slotLabel(selectedSaveLoadSlot()) + ".";
                return true;
            }
        }
        if (saveLoadBackRect().contains(e.getX(), e.getY())) {
            setScreen(Screen.MENU);
            return true;
        }
        if (saveLoadNewGameRect().contains(e.getX(), e.getY())) {
            openNewGameSetup();
            return true;
        }
        return false;
    }

    Rectangle saveLoadPanelRect() {
        int panelW = Math.max(420, Math.min(760, getWidth() - 60));
        int panelH = Math.max(360, Math.min(560, getHeight() - 80));
        return new Rectangle(Math.max(20, getWidth() / 2 - panelW / 2), Math.max(40, getHeight() / 2 - panelH / 2), panelW, panelH);
    }

    Rectangle saveLoadSlotRect(int index) {
        Rectangle p = saveLoadPanelRect();
        int rowH = 48;
        int gap = 8;
        int top = p.y + 92;
        return new Rectangle(p.x + 28, top + index * (rowH + gap), p.width - 56, rowH);
    }

    Rectangle saveLoadBackRect() {
        Rectangle p = saveLoadPanelRect();
        return new Rectangle(p.x + 28, p.y + p.height - 58, 148, 34);
    }

    Rectangle saveLoadNewGameRect() {
        Rectangle p = saveLoadPanelRect();
        return new Rectangle(p.x + p.width - 206, p.y + p.height - 58, 178, 34);
    }

    void installExistingEarlyScreenInputBridge() {
        addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyPressed(java.awt.event.KeyEvent e) {
                if (KeyEarlyScreenController.handleEarlyKey(GamePanel.this, e.getKeyCode())) {
                    repaint();
                    requestFocusInWindow();
                    return;
                }
                if (handleSaveLoadKey(e.getKeyCode())) {
                    repaint();
                    requestFocusInWindow();
                    return;
                }
                if (!(screen == Screen.MENU || screen == Screen.MAIN || screen == Screen.BOOT)) {
                    GamePanelKeyController.keyPressed(GamePanel.this, e);
                    repaint();
                    requestFocusInWindow();
                }
            }

            @Override public void keyTyped(java.awt.event.KeyEvent e) {
                if (handleInGameEditorTyped(e.getKeyChar())) {
                    repaint();
                    return;
                }
                if (CharacterNameKeyController.handleCharacterNameEditTyped(GamePanel.this, e.getKeyChar())) {
                    repaint();
                    return;
                }
                if (screen == Screen.MULTIPLAYER) {
                    multiplayerMenu.keyTyped(e.getKeyChar());
                    repaint();
                    return;
                }
                if (handleInfopediaTyped(e.getKeyChar())) {
                    repaint();
                }
            }

            @Override public void keyReleased(java.awt.event.KeyEvent e) {
                if (firstPersonRenderViewport.handleKeyReleased(GamePanel.this, e.getKeyCode())) {
                    repaint();
                    requestFocusInWindow();
                }
            }
        });
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (handleSaveLoadClick(e)) {
                    repaint();
                    requestFocusInWindow();
                    return;
                }
                if (screenUsesLateUiClickSurface() && MouseLateUiController.handleLateUiClick(GamePanel.this, e.getX(), e.getY())) {
                    repaint();
                    requestFocusInWindow();
                    return;
                }
                if (MouseGamePanelController.handleGameAndPanelTargeting(GamePanel.this, e, e.getX(), e.getY())) {
                    repaint();
                    requestFocusInWindow();
                    return;
                }
                if (MouseEarlyScreenController.handleIntroZoneEditorAndAudit(GamePanel.this, e, e.getX(), e.getY())) {
                    repaint();
                    requestFocusInWindow();
                }
            }
        });
    }

    boolean screenUsesLateUiClickSurface() {
        return screen == Screen.GAME || screen == Screen.OPTIONS || screen == Screen.CHARACTER || screen == Screen.PANEL
                || screen == Screen.INVENTORY || screen == Screen.INFO || screen == Screen.MAP
                || screen == Screen.PAUSE || screen == Screen.KNOWLEDGE || screen == Screen.MODS
                || screen == Screen.SECTOR_AUDIT || screen == Screen.EDITOR;
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
                if (screen == Screen.BOOT) {
                    new BootSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.INTRO_CRAWL) {
                    new IntroCrawlSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.ZONE_SPLASH || screen == Screen.CAPTURE) {
                    new LoadingSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.MENU || screen == Screen.MAIN) {
                    new MainMenuSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.OPTIONS) {
                    OptionsScreenPainter.paintShell(this, g);
                    OptionsScreenPainter.paintBody(this, g);
                    OptionsScreenPainter.paintGraphicsDropdownPopup(this, g);
                    return;
                }
                if (screen == Screen.MULTIPLAYER) {
                    new MultiplayerSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.SAVE_LOAD) {
                    new SaveLoadSurfacePainter().paint(g, this);
                    return;
                }
                if (screen == Screen.MODS) {
                    drawToolsMenuSurface(g, w, h);
                    return;
                }
                if (screen == Screen.SECTOR_AUDIT) {
                    drawSectorAuditSurface(g, w, h);
                    return;
                }
                if (screen == Screen.EDITOR) {
                    drawEditorRecoverySurface(g, w, h);
                    return;
                }
                if (screen == Screen.GAME || screen == Screen.PANEL || screen == Screen.INVENTORY || screen == Screen.CHARACTER || screen == Screen.INFO || screen == Screen.MAP || screen == Screen.KNOWLEDGE || screen == Screen.PAUSE) {
                    if (screen == Screen.GAME && world != null && firstPersonRenderViewport.isUnlocked(options)) {
                        firstPersonRenderViewport.render(g, this);
                        return;
                    }
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


    private void drawToolsMenuSurface(java.awt.Graphics2D g, int w, int h) {
        buttons.clear();
        Rectangle panel = new Rectangle(18, 48, Math.max(640, w - 36), Math.max(460, h - 86));
        drawOverlayFrame(g, panel, "TOOLS / EDITORS");
        Rectangle intro = new Rectangle(panel.x + 18, panel.y + 54, panel.width - 36, 76);
        drawDetailBox(g, intro, "Live Project Tooling", java.util.List.of(
                "Each editor opens against current project defaults. Changes remain isolated from base data.",
                "Save Mod writes the selected editor records to a randomized package under PACKAGE_client/mods."
        ), null);
        int gridTop = intro.y + intro.height + 12;
        int gridBottom = panel.y + panel.height - 54;
        int cols = panel.width >= 1080 ? 3 : 2;
        int gap = 10;
        int rows = (SimulationToolSuiteRegistry.specs().size() + 1 + cols - 1) / cols;
        int buttonW = Math.max(180, (panel.width - 36 - gap * (cols - 1)) / cols);
        int buttonH = Math.max(30, Math.min(42, (gridBottom - gridTop - gap * Math.max(0, rows - 1)) / Math.max(1, rows)));
        int index = 0;
        addOverlayButton("Zone Audit", panel.x + 18, gridTop, buttonW, buttonH, "Open the generated/current zone audit and replay surface.", this::openSectorAuditPanel);
        index++;
        for (SimulationToolSuiteRegistry.ToolSpec spec : SimulationToolSuiteRegistry.specs()) {
            int col = index % cols;
            int row = index / cols;
            int bx = panel.x + 18 + col * (buttonW + gap);
            int by = gridTop + row * (buttonH + gap);
            addOverlayButton(spec.editorName(), bx, by, buttonW, buttonH, spec.purpose(), () -> openInGameEditor(spec.editorName()));
            index++;
        }
        addOverlayButton("Mod Packaging Editor", panel.x + panel.width - buttonW - 18, gridBottom - buttonH, buttonW, buttonH,
                "Open mod scope and package controls.", () -> openInGameEditor(SimulationToolSuiteRegistry.MOD_PACKAGING_EDITOR));
        addOverlayButton("Back", panel.x + 18, panel.y + panel.height - 42, 110, 30, "Return to the main menu.", () -> setScreen(Screen.MENU));
        drawOverlayButtons(g);
    }

    private void drawSectorAuditSurface(java.awt.Graphics2D g, int w, int h) {
        buttons.clear();
        if (auditWorld == null) rerollSectorAudit();
        SectorGenerationTraceAuthority.Step step = currentAuditTraceStep();
        World viewWorld = auditViewWorld(step);
        Rectangle panel = new Rectangle(18, 48, Math.max(560, w - 36), Math.max(420, h - 86));
        drawOverlayFrame(g, panel, "ZONE AUDIT");
        Rectangle body = new Rectangle(panel.x + 18, panel.y + 54, panel.width - 36, panel.height - 72);
        int gap = 14;
        int commandW = Math.max(224, Math.min(286, panel.width / 5));
        Rectangle content = new Rectangle(body.x, body.y, Math.max(300, body.width - commandW - gap), body.height);
        Rectangle command = new Rectangle(content.x + content.width + gap, body.y, commandW, body.height);
        int stepH = Math.max(96, Math.min(150, body.height / 4));
        Rectangle mapBox = new Rectangle(content.x, content.y, content.width, Math.max(220, content.height - stepH - gap));
        Rectangle stepBox = new Rectangle(body.x, mapBox.y + mapBox.height + gap, mapBox.width, stepH);
        drawAuditReplayMap(g, mapBox, viewWorld, step);
        drawAuditStepBox(g, stepBox, step);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Zone: " + (viewWorld == null ? "none" : viewWorld.zoneType.label));
        lines.add("Density: " + WorldSetupSettings.ZONE_DENSITY[Math.max(0, Math.min(auditZoneDensityIndex, WorldSetupSettings.ZONE_DENSITY.length - 1))]);
        lines.add("Overlay: " + auditOverlayLabel());
        lines.add("Cursor: " + auditCursorX + "," + auditCursorY);
        lines.add("Replay: " + (auditTracePlaying ? "playing" : "paused") + " | Step " + (auditTraceSteps.isEmpty() ? 0 : auditTraceIndex + 1) + "/" + auditTraceSteps.size());
        if (auditSnapshot != null) lines.addAll(SectorAuditRuntimeAuthority.compactPanelLines(auditSnapshot, auditFindingIndex));
        else lines.add("Audit snapshot has not run.");
        int findingH = Math.max(150, Math.min(230, command.height * 2 / 5));
        drawDetailBox(g, new Rectangle(command.x, command.y, command.width, findingH), "Findings", lines, null);
        String[] labels = {"Reroll Zone", auditTracePlaying ? "Pause Replay" : "Play Replay", "Next Replay Step", "Finish Replay", "Choose Zone Type", "Cycle Density", "Cycle Overlay", "Previous Finding", "Next Finding", "Return to Tools"};
        Runnable[] actions = {this::rerollSectorAudit, this::toggleAuditReplay, () -> stepAuditReplay(1), this::finishAuditReplay,
                () -> cycleAuditZoneType(1), this::cycleAuditZoneDensity, this::cycleAuditOverlay, () -> jumpAuditFinding(-1), () -> jumpAuditFinding(1), () -> setScreen(Screen.MODS)};
        int buttonTop = command.y + findingH + 10;
        int buttonGap = 6;
        int buttonH = Math.max(28, Math.min(38, (command.y + command.height - buttonTop - buttonGap * (labels.length - 1)) / labels.length));
        for (int i = 0; i < labels.length; i++) {
            addOverlayButton(labels[i], command.x + 8, buttonTop + i * (buttonH + buttonGap), command.width - 16, buttonH, labels[i], actions[i]);
        }
        drawOverlayButtons(g);
    }

    private void drawAuditReplayMap(java.awt.Graphics2D g, Rectangle mapBox, World viewWorld, SectorGenerationTraceAuthority.Step step) {
        drawBox(g, mapBox, "Generation Replay");
        if (viewWorld == null || viewWorld.w <= 0 || viewWorld.h <= 0) {
            drawDetailBox(g, mapBox, "Generation Replay", java.util.List.of("No audit world is available."), null);
            return;
        }
        int header = 30;
        Rectangle grid = new Rectangle(mapBox.x + 10, mapBox.y + header, Math.max(1, mapBox.width - 20), Math.max(1, mapBox.height - header - 10));
        int baseTile = options == null ? 24 : options.mapTilePixelSize();
        int tile = MapViewportOptionsSubsystem.scaledTileSize(baseTile, options, 8, 64);
        int cols = Math.max(1, Math.min(viewWorld.w, grid.width / tile));
        int rows = Math.max(1, Math.min(viewWorld.h, grid.height / tile));
        int focusX = step == null ? auditCursorX : step.x;
        int focusY = step == null ? auditCursorY : step.y;
        lastAuditViewMinX = Math.max(0, Math.min(Math.max(0, viewWorld.w - cols), focusX - cols / 2));
        lastAuditViewMinY = Math.max(0, Math.min(Math.max(0, viewWorld.h - rows), focusY - rows / 2));
        lastAuditViewCols = cols;
        lastAuditViewRows = rows;
        lastAuditViewTileSize = tile;
        lastAuditViewOriginX = grid.x + Math.max(0, (grid.width - cols * tile) / 2);
        lastAuditViewOriginY = grid.y + Math.max(0, (grid.height - rows * tile) / 2);

        java.awt.Font glyphFont = asciiFont.deriveFont(java.awt.Font.BOLD, Math.max(10f, tile * 0.62f));
        g.setFont(glyphFont);
        java.awt.FontMetrics fm = g.getFontMetrics();
        for (int vy = 0; vy < rows; vy++) {
            for (int vx = 0; vx < cols; vx++) {
                int wx = lastAuditViewMinX + vx;
                int wy = lastAuditViewMinY + vy;
                int sx = lastAuditViewOriginX + vx * tile;
                int sy = lastAuditViewOriginY + vy * tile;
                if (!viewWorld.inBounds(wx, wy)) continue;
                char ch = viewWorld.tiles[wx][wy];
                CompiledTileDescriptor descriptor = TileDataCompilationAuthority.resolve(viewWorld, wx, wy, ch);
                g.setColor(auditTileColor(viewWorld, ch, wx, wy));
                g.fillRect(sx, sy, tile, tile);
                boolean drewArt = options != null && options.tileIconRendering && drawCompiledTile(g, descriptor, ch, sx, sy, tile);
                if (!drewArt && tile >= 13) {
                    g.setColor(tileGlyphColor(ch));
                    String s = Character.toString(ch);
                    g.drawString(s, sx + (tile - fm.stringWidth(s)) / 2, sy + (tile + fm.getAscent() - fm.getDescent()) / 2);
                }
                String overlay = auditOverlayLabel();
                if ("ROOMS".equals(overlay) && viewWorld.roomIds != null && viewWorld.roomIds[wx][wy] > 0) {
                    g.setColor(new java.awt.Color(116, 188, 235, 42));
                    g.fillRect(sx, sy, tile, tile);
                }
                if ("ROADS".equals(overlay) && ch == RoadGridIntegrationAuthority.ROAD_LANE) {
                    g.setColor(new java.awt.Color(245, 214, 118, 54));
                    g.fillRect(sx, sy, tile, tile);
                }
                if ("BOUNDARY".equals(overlay) && (ch == InterstitialInfrastructureApi.VOID_SPACE || ch == '#')) {
                    g.setColor(new java.awt.Color(172, 120, 235, 46));
                    g.fillRect(sx, sy, tile, tile);
                }
                if ("DESCRIPTORS".equals(overlay) && descriptor != null) {
                    g.setColor("fallback".equals(descriptor.baseLayer) ? new java.awt.Color(230, 82, 68, 72) : new java.awt.Color(124, 230, 144, 32));
                    g.fillRect(sx, sy, tile, tile);
                }
                g.setColor(new java.awt.Color(0, 0, 0, 70));
                g.drawRect(sx, sy, tile, tile);
            }
        }
        String overlay = auditOverlayLabel();
        if ("FINDINGS".equals(overlay) && auditSnapshot != null) {
            g.setColor(new java.awt.Color(245, 214, 118, 170));
            for (SectorAuditRuntimeAuthority.AuditFinding finding : auditSnapshot.findings) {
                if (finding == null || !auditPointInView(finding.x, finding.y)) continue;
                int sx = lastAuditViewOriginX + (finding.x - lastAuditViewMinX) * tile;
                int sy = lastAuditViewOriginY + (finding.y - lastAuditViewMinY) * tile;
                g.drawRect(sx + 2, sy + 2, Math.max(2, tile - 5), Math.max(2, tile - 5));
            }
        }
        if ("INTERACT".equals(overlay) || "CONTAINERS".equals(overlay)) paintAuditObjectOverlay(g, viewWorld, tile, overlay);
        if ("LIGHTS".equals(overlay)) paintAuditLightOverlay(g, viewWorld, tile);
        if ("TRAPS".equals(overlay)) paintAuditTrapOverlay(g, viewWorld, tile);
        if ("ENTITIES".equals(overlay)) paintAuditEntityOverlay(g, viewWorld, tile);
        if ("TRANSITIONS".equals(overlay)) paintAuditTransitionOverlay(g, viewWorld, tile);
        drawAuditGhostAndCursor(g, step, tile);
    }

    private void paintAuditObjectOverlay(java.awt.Graphics2D g, World viewWorld, int tile, String overlay) {
        if (viewWorld == null || viewWorld.mapObjects == null) return;
        g.setColor("CONTAINERS".equals(overlay) ? new java.awt.Color(124, 230, 144, 185) : new java.awt.Color(116, 188, 235, 185));
        for (MapObjectState object : viewWorld.mapObjects) {
            if (object == null || !auditPointInView(object.x, object.y)) continue;
            String type = object.type == null ? "" : object.type.toLowerCase(java.util.Locale.ROOT);
            if ("CONTAINERS".equals(overlay) && !(type.contains("container") || type.contains("vending") || type.contains("shop") || type.contains("vault"))) continue;
            int sx = lastAuditViewOriginX + (object.x - lastAuditViewMinX) * tile;
            int sy = lastAuditViewOriginY + (object.y - lastAuditViewMinY) * tile;
            g.fillOval(sx + Math.max(2, tile / 4), sy + Math.max(2, tile / 4), Math.max(3, tile / 2), Math.max(3, tile / 2));
        }
    }

    private void paintAuditLightOverlay(java.awt.Graphics2D g, World viewWorld, int tile) {
        if (viewWorld == null || viewWorld.lightSources == null) return;
        for (ZoneLightSourceRecord light : viewWorld.lightSources) {
            if (light == null || !auditPointInView(light.x, light.y)) continue;
            int sx = lastAuditViewOriginX + (light.x - lastAuditViewMinX) * tile;
            int sy = lastAuditViewOriginY + (light.y - lastAuditViewMinY) * tile;
            g.setColor(new java.awt.Color(245, 214, 118, 52));
            int radius = Math.max(tile, Math.min(tile * 5, light.radius * tile / 2));
            g.fillOval(sx + tile / 2 - radius / 2, sy + tile / 2 - radius / 2, radius, radius);
            g.setColor(light.color());
            g.fillOval(sx + Math.max(2, tile / 3), sy + Math.max(2, tile / 3), Math.max(4, tile / 3), Math.max(4, tile / 3));
        }
    }

    private void paintAuditTrapOverlay(java.awt.Graphics2D g, World viewWorld, int tile) {
        if (viewWorld == null || viewWorld.trapRecords == null) return;
        g.setColor(new java.awt.Color(230, 82, 68, 190));
        for (TrapRecord trap : viewWorld.trapRecords) {
            if (trap == null || !auditPointInView(trap.x, trap.y)) continue;
            int sx = lastAuditViewOriginX + (trap.x - lastAuditViewMinX) * tile;
            int sy = lastAuditViewOriginY + (trap.y - lastAuditViewMinY) * tile;
            g.drawLine(sx + 3, sy + 3, sx + tile - 4, sy + tile - 4);
            g.drawLine(sx + tile - 4, sy + 3, sx + 3, sy + tile - 4);
        }
    }

    private void paintAuditEntityOverlay(java.awt.Graphics2D g, World viewWorld, int tile) {
        if (viewWorld == null || viewWorld.npcs == null) return;
        g.setColor(new java.awt.Color(238, 215, 132, 205));
        for (NpcEntity npc : viewWorld.npcs) {
            if (npc == null || !auditPointInView(npc.x, npc.y)) continue;
            int sx = lastAuditViewOriginX + (npc.x - lastAuditViewMinX) * tile;
            int sy = lastAuditViewOriginY + (npc.y - lastAuditViewMinY) * tile;
            g.fillRect(sx + Math.max(2, tile / 4), sy + Math.max(2, tile / 4), Math.max(4, tile / 2), Math.max(4, tile / 2));
        }
    }

    private void paintAuditTransitionOverlay(java.awt.Graphics2D g, World viewWorld, int tile) {
        if (viewWorld == null) return;
        g.setColor(new java.awt.Color(172, 120, 235, 210));
        for (int vy = 0; vy < lastAuditViewRows; vy++) {
            for (int vx = 0; vx < lastAuditViewCols; vx++) {
                int wx = lastAuditViewMinX + vx;
                int wy = lastAuditViewMinY + vy;
                if (!viewWorld.inBounds(wx, wy)) continue;
                char ch = viewWorld.tiles[wx][wy];
                if (ch != '<' && ch != '>' && ch != 'D') continue;
                int sx = lastAuditViewOriginX + vx * tile;
                int sy = lastAuditViewOriginY + vy * tile;
                g.drawRect(sx + 2, sy + 2, Math.max(2, tile - 5), Math.max(2, tile - 5));
            }
        }
    }

    private void drawAuditGhostAndCursor(java.awt.Graphics2D g, SectorGenerationTraceAuthority.Step step, int tile) {
        java.awt.Stroke old = g.getStroke();
        if (step != null && step.ghostRect != null) {
            Rectangle ghost = step.ghostRect;
            int gx1 = Math.max(ghost.x, lastAuditViewMinX);
            int gy1 = Math.max(ghost.y, lastAuditViewMinY);
            int gx2 = Math.min(ghost.x + ghost.width, lastAuditViewMinX + lastAuditViewCols);
            int gy2 = Math.min(ghost.y + ghost.height, lastAuditViewMinY + lastAuditViewRows);
            if (gx2 > gx1 && gy2 > gy1) {
                int sx = lastAuditViewOriginX + (gx1 - lastAuditViewMinX) * tile;
                int sy = lastAuditViewOriginY + (gy1 - lastAuditViewMinY) * tile;
                int sw = Math.max(tile, (gx2 - gx1) * tile);
                int sh = Math.max(tile, (gy2 - gy1) * tile);
                g.setColor(step.rejected ? new java.awt.Color(230, 82, 68, 82) : new java.awt.Color(116, 188, 235, 70));
                g.fillRect(sx, sy, sw, sh);
                g.setStroke(new java.awt.BasicStroke(2f));
                g.setColor(step.rejected ? new java.awt.Color(235, 88, 74) : new java.awt.Color(116, 188, 235));
                g.drawRect(sx, sy, Math.max(1, sw - 1), Math.max(1, sh - 1));
            }
        }
        if (auditPointInView(auditCursorX, auditCursorY)) {
            int sx = lastAuditViewOriginX + (auditCursorX - lastAuditViewMinX) * tile;
            int sy = lastAuditViewOriginY + (auditCursorY - lastAuditViewMinY) * tile;
            g.setStroke(new java.awt.BasicStroke(Math.max(1f, tile / 10f)));
            g.setColor(new java.awt.Color(245, 214, 118));
            g.drawRect(sx + 1, sy + 1, Math.max(2, tile - 3), Math.max(2, tile - 3));
        }
        g.setStroke(old);
    }

    private void drawAuditStepBox(java.awt.Graphics2D g, Rectangle r, SectorGenerationTraceAuthority.Step step) {
        ArrayList<String> lines = new ArrayList<>();
        if (step == null) {
            lines.add("No generation trace captured for this audit slice.");
            lines.add("Reroll will regenerate the zone with trace capture enabled.");
        } else {
            lines.add(step.line(auditTraceSteps.size()));
            lines.add("Focus " + step.x + "," + step.y + (step.rejected ? " | rejected placement proposal" : ""));
            if (step.ghostRect != null) lines.add("Proposal " + step.ghostRect.x + "," + step.ghostRect.y + " " + step.ghostRect.width + "x" + step.ghostRect.height);
        }
        drawDetailBox(g, r, "Generation Step", lines, null);
    }

    private SectorGenerationTraceAuthority.Step currentAuditTraceStep() {
        if (auditTraceSteps.isEmpty()) return null;
        if (auditTracePlaying) {
            int millis = Math.max(120, auditTraceStepMillis);
            long elapsed = Math.max(0L, System.currentTimeMillis() - auditTraceStartedMillis);
            auditTraceIndex = Math.max(0, Math.min(auditTraceSteps.size() - 1, (int)(elapsed / millis)));
            if (auditTraceIndex >= auditTraceSteps.size() - 1) auditTracePlaying = false;
        }
        auditTraceIndex = Math.max(0, Math.min(auditTraceIndex, auditTraceSteps.size() - 1));
        SectorGenerationTraceAuthority.Step step = auditTraceSteps.get(auditTraceIndex);
        if (step != null) {
            auditCursorX = step.x;
            auditCursorY = step.y;
        }
        return step;
    }

    private World auditViewWorld(SectorGenerationTraceAuthority.Step step) {
        return step != null && step.world != null ? step.world : auditWorld;
    }

    private boolean auditPointInView(int x, int y) {
        return x >= lastAuditViewMinX && y >= lastAuditViewMinY && x < lastAuditViewMinX + lastAuditViewCols && y < lastAuditViewMinY + lastAuditViewRows;
    }

    private java.awt.Color auditTileColor(World viewWorld, char ch, int x, int y) {
        if (ch == '#') return new java.awt.Color(38, 39, 36);
        if (ch == '.' || ch == ',' || ch == ':' || ch == ';' || ch == '=') return new java.awt.Color(47, 48, 43);
        if (ch == '+' || ch == '/' || ch == '\\' || ch == 'D') return new java.awt.Color(91, 76, 43);
        if (ch == '~') return new java.awt.Color(35, 57, 58);
        if (viewWorld != null && viewWorld.inBounds(x, y) && viewWorld.walkable(x, y)) return new java.awt.Color(54, 50, 42);
        return new java.awt.Color(28, 29, 27);
    }

    private void drawEditorRecoverySurface(java.awt.Graphics2D g, int w, int h) {
        buttons.clear();
        Rectangle panel = new Rectangle(18, 48, Math.max(660, w - 36), Math.max(460, h - 86));
        drawOverlayFrame(g, panel, inGameEditorName.toUpperCase(Locale.ROOT));
        int commandW = Math.max(220, Math.min(286, panel.width / 5));
        Rectangle command = new Rectangle(panel.x + panel.width - commandW - 18, panel.y + 54, commandW, panel.height - 72);
        Rectangle body = new Rectangle(panel.x + 18, panel.y + 54, command.x - panel.x - 32, panel.height - 72);
        SimulationEditorRepository.EditableEntity entity = currentInGameEditorEntity();
        java.util.ArrayList<String> entityLines = new java.util.ArrayList<>();
        java.util.List<SimulationEditorRepository.EditableEntity> entities = inGameEditorRepository.entities(inGameEditorName);
        if (SimulationToolSuiteRegistry.MOD_PACKAGING_EDITOR.equals(inGameEditorName)) {
            SimulationEditorRepository.ModMetadata metadata = inGameEditorRepository.metadata();
            entityLines.add("Package: " + metadata.name());
            entityLines.add("Version: " + metadata.version());
            entityLines.add("Author: " + metadata.author());
            entityLines.add("Selected records: " + inGameEditorRepository.selectedEntities().size());
            entityLines.add("Default destination: PACKAGE_client/mods");
            entityLines.add("Save Mod creates a new randomized archive unless metadata is edited.");
        } else {
            entityLines.add("Source: current project defaults (read-through seed)");
            entityLines.add("Base files: read-only from this screen; edits: isolated mod overlay");
            entityLines.add("Record " + (entities.isEmpty() ? 0 : inGameEditorEntityIndex + 1) + "/" + entities.size());
            if (entity != null) {
                SimulationEditorRepository.EntityRef ref = currentInGameEditorRef();
                entityLines.add("Name: " + entity.name());
                entityLines.add("ID: " + entity.id());
                entityLines.add("Included in mod: " + (inGameEditorRepository.selected(ref) ? "YES" : "NO"));
            }
        }
        int summaryH = Math.max(130, body.height / 3);
        drawDetailBox(g, new Rectangle(body.x, body.y, body.width, summaryH), "Current Record", entityLines, null);
        java.util.ArrayList<String> propertyLines = new java.util.ArrayList<>();
        String property = currentInGameEditorProperty();
        Object value = entity == null || property == null ? null : entity.properties().get(property);
        if (entity == null) propertyLines.add("No record selected.");
        else {
            propertyLines.add("Property: " + (property == null ? "none" : property));
            propertyLines.add("Value: " + (inGameEditorTextEditActive ? inGameEditorTextBuffer + "|" : String.valueOf(value)));
            propertyLines.add("Type: " + (value == null ? "text" : value.getClass().getSimpleName()));
            propertyLines.add("Use Previous/Next Value for booleans, numbers, and registered choices.");
            propertyLines.add("Edit Text accepts typing; Enter commits and Escape cancels.");
        }
        propertyLines.add("");
        propertyLines.add("Status: " + inGameEditorStatus);
        propertyLines.add("History: " + inGameEditorHistory.compactState());
        drawDetailBox(g, new Rectangle(body.x, body.y + summaryH + 12, body.width, body.height - summaryH - 12), "Editable Property", propertyLines, null);

        String[] labels = {"Previous Record", "Next Record", "Previous Property", "Next Property", "Previous Value", "Next Value", "Edit Text Value", "New Record", "Include / Exclude", "Undo", "Redo", "Save Mod Package", "Return to Tools"};
        Runnable[] actions = {() -> cycleInGameEditorEntity(-1), () -> cycleInGameEditorEntity(1), () -> cycleInGameEditorProperty(-1), () -> cycleInGameEditorProperty(1),
                () -> adjustInGameEditorValue(-1), () -> adjustInGameEditorValue(1), this::beginInGameEditorTextEdit, this::createNewInGameEditorEntry,
                this::toggleCurrentInGameEditorScope, this::inGameEditorUndo, this::inGameEditorRedo, this::saveInGameEditorModPackage, () -> setScreen(Screen.MODS)};
        int gap = 6;
        int buttonH = Math.max(27, Math.min(38, (command.height - gap * (labels.length - 1)) / labels.length));
        for (int i = 0; i < labels.length; i++) addOverlayButton(labels[i], command.x + 8, command.y + i * (buttonH + gap), command.width - 16, buttonH, labels[i], actions[i]);
        drawOverlayButtons(g);
    }

    void openInGameEditor(String editorName) {
        inGameEditorName = SimulationToolSuiteRegistry.isKnownEditor(editorName) || SimulationToolSuiteRegistry.MOD_PACKAGING_EDITOR.equals(editorName)
                ? editorName : SimulationToolSuiteRegistry.fallbackEditor();
        inGameEditorEntityIndex = 0;
        inGameEditorPropertyIndex = 0;
        inGameEditorTextEditActive = false;
        inGameEditorStatus = "Opened " + inGameEditorName + " against current project defaults.";
        setScreen(Screen.EDITOR);
    }

    private SimulationEditorRepository.EditableEntity currentInGameEditorEntity() {
        java.util.List<SimulationEditorRepository.EditableEntity> entities = inGameEditorRepository.entities(inGameEditorName);
        if (entities.isEmpty()) return null;
        inGameEditorEntityIndex = Math.floorMod(inGameEditorEntityIndex, entities.size());
        return entities.get(inGameEditorEntityIndex);
    }

    private SimulationEditorRepository.EntityRef currentInGameEditorRef() {
        SimulationEditorRepository.EditableEntity entity = currentInGameEditorEntity();
        return entity == null ? null : new SimulationEditorRepository.EntityRef(inGameEditorName, entity.id());
    }

    private String currentInGameEditorProperty() {
        SimulationEditorRepository.EditableEntity entity = currentInGameEditorEntity();
        if (entity == null || entity.properties().isEmpty()) return null;
        java.util.ArrayList<String> names = new java.util.ArrayList<>(entity.properties().keySet());
        inGameEditorPropertyIndex = Math.floorMod(inGameEditorPropertyIndex, names.size());
        return names.get(inGameEditorPropertyIndex);
    }

    private void cycleInGameEditorEntity(int delta) {
        inGameEditorEntityIndex += delta;
        inGameEditorPropertyIndex = 0;
        inGameEditorTextEditActive = false;
        repaint();
    }

    private void cycleInGameEditorProperty(int delta) {
        inGameEditorPropertyIndex += delta;
        inGameEditorTextEditActive = false;
        repaint();
    }

    private void adjustInGameEditorValue(int delta) {
        SimulationEditorRepository.EntityRef ref = currentInGameEditorRef();
        SimulationEditorRepository.EditableEntity entity = currentInGameEditorEntity();
        String property = currentInGameEditorProperty();
        if (ref == null || entity == null || property == null) return;
        Object oldValue = entity.properties().get(property);
        Object next = oldValue;
        java.util.List<String> choices = SimulationToolSuiteRegistry.linkOptionsFor(inGameEditorName, inGameEditorRepository).get(property);
        if (choices != null && !choices.isEmpty()) {
            int current = Math.max(0, choices.indexOf(String.valueOf(oldValue)));
            next = choices.get(Math.floorMod(current + delta, choices.size()));
        } else if (oldValue instanceof Boolean b) next = !b;
        else if (oldValue instanceof Integer n) next = n + delta;
        else if (oldValue instanceof Long n) next = n + delta;
        else if (oldValue instanceof Double n) next = Math.max(0.0, n + delta * 0.1);
        else {
            inGameEditorStatus = "This text property has no registered choices; use Edit Text Value.";
            repaint();
            return;
        }
        inGameEditorHistory.execute(new EditorCommand.PropertyChange(inGameEditorRepository, ref, property, oldValue, next, inGameEditorEvents));
        inGameEditorRepository.setSelected(ref, true);
        inGameEditorStatus = "Changed " + property + " to " + next + ".";
        repaint();
    }

    private void beginInGameEditorTextEdit() {
        SimulationEditorRepository.EditableEntity entity = currentInGameEditorEntity();
        String property = currentInGameEditorProperty();
        if (entity == null || property == null) return;
        inGameEditorTextBuffer = String.valueOf(entity.properties().getOrDefault(property, ""));
        inGameEditorTextEditActive = true;
        inGameEditorStatus = "Editing " + property + "; Enter commits, Escape cancels.";
        requestFocusInWindow();
        repaint();
    }

    boolean handleInGameEditorTyped(char ch) {
        if (screen != Screen.EDITOR || !inGameEditorTextEditActive) return false;
        if (!Character.isISOControl(ch)) inGameEditorTextBuffer += ch;
        repaint();
        return true;
    }

    boolean handleInGameEditorTextKey(int code) {
        if (screen != Screen.EDITOR || !inGameEditorTextEditActive) return false;
        if (code == KeyEvent.VK_ESCAPE) {
            inGameEditorTextEditActive = false;
            inGameEditorStatus = "Text edit cancelled.";
        } else if (code == KeyEvent.VK_BACK_SPACE) {
            if (!inGameEditorTextBuffer.isEmpty()) inGameEditorTextBuffer = inGameEditorTextBuffer.substring(0, inGameEditorTextBuffer.length() - 1);
        } else if (code == KeyEvent.VK_ENTER) {
            SimulationEditorRepository.EntityRef ref = currentInGameEditorRef();
            SimulationEditorRepository.EditableEntity entity = currentInGameEditorEntity();
            String property = currentInGameEditorProperty();
            if (ref != null && entity != null && property != null) {
                Object oldValue = entity.properties().get(property);
                inGameEditorHistory.execute(new EditorCommand.PropertyChange(inGameEditorRepository, ref, property, oldValue, inGameEditorTextBuffer, inGameEditorEvents));
                inGameEditorRepository.setSelected(ref, true);
                inGameEditorStatus = "Committed text value for " + property + ".";
            }
            inGameEditorTextEditActive = false;
        }
        repaint();
        return true;
    }

    private void toggleCurrentInGameEditorScope() {
        SimulationEditorRepository.EntityRef ref = currentInGameEditorRef();
        if (ref == null) return;
        boolean old = inGameEditorRepository.selected(ref);
        inGameEditorHistory.execute(new EditorCommand.ToggleProjectSelection(inGameEditorRepository, ref, old, !old, inGameEditorEvents));
        inGameEditorStatus = (!old ? "Included " : "Excluded ") + ref.entityId() + " in mod scope.";
        repaint();
    }

    private void saveInGameEditorModPackage() {
        try {
            java.nio.file.Path dir = java.nio.file.Path.of("PACKAGE_client", "mods");
            java.nio.file.Files.createDirectories(dir);
            SimulationEditorRepository.ModMetadata metadata = inGameEditorRepository.metadata();
            java.nio.file.Path output = dir.resolve(SimulationEditorRepository.slug(metadata.name()) + "-" + SimulationEditorRepository.randomId() + ".zip");
            ModDeploymentManager.DeploymentRequest request = new ModDeploymentManager.DeploymentRequest(metadata, inGameEditorRepository.selectedEntities(), output, false,
                    ModDeploymentManager.SteamPublicationMode.CREATE_NEW_ITEM, 0L, 0L, null, "Saved from the in-game Mechanist editor.");
            ModDeploymentManager.DeploymentResult result = inGameModDeployment.deploy(request, ModDeploymentManager.ProgressSink.ignored());
            inGameEditorStatus = result.summary();
        } catch (Exception ex) {
            inGameEditorStatus = "Mod save failed: " + ex.getMessage();
        }
        repaint();
    }

    private void paintGameBridgeSurface(java.awt.Graphics2D g, int w, int h) {
        g.setColor(new java.awt.Color(10, 11, 10));
        g.fillRect(0, 0, w, h);
        if (WorldStartFlowAuthority.isActive(this)) {
            new MainMenuSurfacePainter().paint(g, this);
            return;
        }
        if (world == null) {
            if (screen == Screen.PANEL && panelMode == PanelMode.INFOPEDIA) {
                drawActivePanelOverlay(g, w, h);
                return;
            }
            g.setFont(titleFont.deriveFont(java.awt.Font.BOLD, Math.max(26f, Math.min(46f, h / 12f))));
            g.setColor(new java.awt.Color(225, 205, 140));
            center(g, "NO WORLD LOADED", w / 2, Math.max(86, h / 4));
            g.setFont(uiFont);
            g.setColor(new java.awt.Color(205, 210, 195));
            center(g, "Return to the main menu and start or load a game.", w / 2, Math.max(132, h / 4 + 44));
            return;
        }
        if (isWorldCommandSurface()) buttons.clear();

        Rectangle map = gameWorldViewportRect(w, h);
        int baseTile = Math.max(8, Math.min(28, Math.min(map.width / 28, map.height / 20)));
        int tile = MapViewportOptionsSubsystem.scaledTileSize(baseTile, options, 6, 56);
        lastWorldViewTileSize = tile;
        int cols = Math.max(1, map.width / tile);
        int rows = Math.max(1, map.height / tile);
        lastWorldViewMinX = Math.max(0, Math.min(Math.max(0, world.w - cols), playerX - cols / 2));
        lastWorldViewMinY = Math.max(0, Math.min(Math.max(0, world.h - rows), playerY - rows / 2));
        lastWorldViewOriginX = map.x + Math.max(0, (map.width - cols * tile) / 2);
        lastWorldViewOriginY = map.y + Math.max(0, (map.height - rows * tile) / 2);

        ensureSensoryModelCurrent("world render");
        drawWorldTiles(g, cols, rows, tile);
        visualLighting.render(this, g, lastWorldViewMinX, lastWorldViewMinY, cols, rows, lastWorldViewOriginX, lastWorldViewOriginY, tile, tile);
        drawWorldEntities(g, cols, rows, tile);
        drawWorldHud(g, w, h, map);
        if ((screen != Screen.GAME || panelMode != PanelMode.NONE) && !isWorldInlineInteractionPanel()) drawActivePanelOverlay(g, w, h);
    }

    private Rectangle gameWorldViewportRect(int w, int h) {
        int sideW = worldCommandBarWidth(w);
        int top = 58;
        int bottom = worldDescriptorHeight(h) + 24;
        int width = Math.max(240, w - sideW - 48);
        int height = Math.max(200, h - top - bottom);
        return new Rectangle(16, top, width, height);
    }

    private int worldCommandBarWidth(int w) {
        return Math.max(132, Math.min(190, w / 6));
    }

    private int worldDescriptorHeight(int h) {
        return Math.max(118, Math.min(168, h / 5));
    }

    private Rectangle worldCommandBarRect(int w, int h) {
        int width = worldCommandBarWidth(w);
        return new Rectangle(w - width - 16, 58, width, Math.max(220, h - 74));
    }

    private Rectangle worldDescriptorRect(int w, int h) {
        int commandW = worldCommandBarWidth(w);
        int height = worldDescriptorHeight(h);
        return new Rectangle(16, h - height - 16, Math.max(260, w - commandW - 48), height);
    }

    private boolean isWorldInlineInteractionPanel() {
        return screen == Screen.PANEL && (panelMode == PanelMode.LOOK || panelMode == PanelMode.INTERACT || panelMode == PanelMode.COMBAT);
    }

    private boolean isWorldCommandSurface() {
        return screen == Screen.GAME || isWorldInlineInteractionPanel();
    }

    private void drawWorldTiles(java.awt.Graphics2D g, int cols, int rows, int tile) {
        java.awt.Font glyphFont = asciiFont.deriveFont(java.awt.Font.BOLD, Math.max(10f, tile * 0.62f));
        g.setFont(glyphFont);
        java.awt.FontMetrics fm = g.getFontMetrics();
        for (int vy = 0; vy < rows; vy++) {
            for (int vx = 0; vx < cols; vx++) {
                int wx = lastWorldViewMinX + vx;
                int wy = lastWorldViewMinY + vy;
                int sx = lastWorldViewOriginX + vx * tile;
                int sy = lastWorldViewOriginY + vy * tile;
                if (!world.inBounds(wx, wy)) continue;
                if (!isRemembered(wx, wy)) {
                    drawUnseenWorldTile(g, sx, sy, tile);
                    continue;
                }
                char ch = world.tiles[wx][wy];
                CompiledTileDescriptor descriptor = TileDataCompilationAuthority.resolve(world, wx, wy, ch);
                g.setColor(tileColor(ch, wx, wy));
                g.fillRect(sx, sy, tile, tile);
                boolean drewArt = options != null && options.tileIconRendering && drawCompiledTile(g, descriptor, ch, sx, sy, tile);
                if (!drewArt && tile >= 13) {
                    g.setColor(tileGlyphColor(ch));
                    String s = Character.toString(ch);
                    g.drawString(s, sx + (tile - fm.stringWidth(s)) / 2, sy + (tile + fm.getAscent() - fm.getDescent()) / 2);
                }
                drawWorldLightMask(g, wx, wy, sx, sy, tile);
                g.setColor(new java.awt.Color(0, 0, 0, 70));
                g.drawRect(sx, sy, tile, tile);
            }
        }
    }

    private void drawUnseenWorldTile(java.awt.Graphics2D g, int sx, int sy, int tile) {
        g.setColor(new java.awt.Color(2, 3, 3));
        g.fillRect(sx, sy, tile, tile);
        g.setColor(new java.awt.Color(12, 14, 13, 120));
        g.drawRect(sx, sy, tile, tile);
    }

    private void drawWorldLightMask(java.awt.Graphics2D g, int wx, int wy, int sx, int sy, int tile) {
        if (!isVisible(wx, wy)) {
            g.setColor(new java.awt.Color(0, 0, 0, 176));
            g.fillRect(sx, sy, tile, tile);
            return;
        }
        int light = lightLevelAt(wx, wy);
        int alpha = visibilityMaskAlpha(wx, wy, light);
        if (alpha > 0) {
            g.setColor(new java.awt.Color(0, 0, 0, alpha));
            g.fillRect(sx, sy, tile, tile);
        }
    }

    private int visibilityMaskAlpha(int wx, int wy, int light) {
        double distance = Math.hypot(wx - playerX, wy - playerY);
        double range = Math.max(1.0, visionRange());
        double edge = Math.max(0.0, Math.min(1.0, distance / range));
        double smoothEdge = edge * edge * (3.0 - 2.0 * edge);
        int lightAlpha = Math.max(0, Math.min(188, 176 - light * 2));
        int edgeAlpha = (int)Math.round(26 + smoothEdge * 62);
        return Math.max(0, Math.min(188, lightAlpha + edgeAlpha));
    }

    private boolean drawCompiledTile(java.awt.Graphics2D g, CompiledTileDescriptor descriptor, char fallbackGlyph, int x, int y, int tile) {
        if (descriptor == null) {
            BufferedImage img = images.getTile(fallbackGlyph);
            if (img == null) return false;
            g.drawImage(img, x, y, tile, tile, null);
            return true;
        }
        boolean drew = false;
        if (descriptor.hasOverlay()) {
            BufferedImage under = tileArtImage(descriptor.underlayArtKey, descriptor.underlayAssetId, descriptor.underlayGlyph == null ? '.' : descriptor.underlayGlyph.charValue());
            if (under != null) {
                g.drawImage(under, x, y, tile, tile, null);
                drew = true;
            }
            BufferedImage over = tileArtImage(descriptor.overlayArtKey, descriptor.overlayAssetId, fallbackGlyph);
            if (over != null) {
                g.drawImage(over, x, y, tile, tile, null);
                drew = true;
            }
            return drew;
        }
        BufferedImage img = tileArtImage(descriptor.primaryArtKey, descriptor.primaryAssetId, fallbackGlyph);
        if (img == null) return false;
        g.drawImage(img, x, y, tile, tile, null);
        return true;
    }

    private BufferedImage tileArtImage(String artKey, String assetId, char fallbackGlyph) {
        BufferedImage img = artKey == null ? null : images.getTile(artKey, fallbackGlyph);
        if (img == null && assetId != null) img = images.getSemanticAssetImage(assetId);
        if (img == null) img = images.getTile(fallbackGlyph);
        return img;
    }

    private void drawWorldEntities(java.awt.Graphics2D g, int cols, int rows, int tile) {
        if (world == null) return;
        if (world.mapObjects != null) {
            for (MapObjectState object : world.mapObjects) {
                if (object == null || !worldPointInView(object.x, object.y, cols, rows)) continue;
                if (!isVisible(object.x, object.y)) continue;
                drawWorldSprite(g, images.getMapObjectImage(object), object.x, object.y, tile, new java.awt.Color(230, 190, 88), Character.toString(object.glyph));
            }
        }
        if (baseObjects != null) {
            for (BaseObject object : baseObjects) {
                if (object == null || !worldPointInView(object.x, object.y, cols, rows)) continue;
                if (!isVisible(object.x, object.y)) continue;
                drawWorldSprite(g, images.getBaseObjectImage(object), object.x, object.y, tile, new java.awt.Color(104, 220, 145), Character.toString(object.symbol));
            }
        }
        if (world.npcs != null) {
            for (NpcEntity npc : world.npcs) {
                if (npc == null || !worldPointInView(npc.x, npc.y, cols, rows)) continue;
                if (!isVisible(npc.x, npc.y)) continue;
                java.awt.Color outline = npc.faction == Faction.NONE ? new java.awt.Color(185, 170, 120) : new java.awt.Color(215, 126, 84);
                drawWorldSprite(g, images.getNpcPortraitFor(npc), npc.x, npc.y, tile, outline, Character.toString(npc.symbol == 0 ? '@' : npc.symbol), npc.facingDx, npc.facingDy);
            }
        }
        drawQuestObjectiveWorldGuidance(g, cols, rows, tile);
        drawFacingVisionConeOverlay(g, cols, rows, tile);
        drawMovementPlannerOverlay(g, cols, rows, tile);
        drawTargetCursor(g, lookX, lookY, cols, rows, tile, lookCursorActive || panelMode == PanelMode.LOOK || panelMode == PanelMode.INTERACT, new java.awt.Color(116, 188, 235));
        drawTargetCursor(g, combatX, combatY, cols, rows, tile, panelMode == PanelMode.COMBAT, new java.awt.Color(235, 88, 74));
        drawTargetCursor(g, buildX, buildY, cols, rows, tile, buildPlacementActive, new java.awt.Color(124, 230, 144));
        drawPlayerWorldSprite(g, cols, rows, tile);
    }

    private boolean worldPointInView(int x, int y, int cols, int rows) {
        return x >= lastWorldViewMinX && y >= lastWorldViewMinY && x < lastWorldViewMinX + cols && y < lastWorldViewMinY + rows;
    }

    private void drawWorldSprite(java.awt.Graphics2D g, BufferedImage img, int wx, int wy, int tile, java.awt.Color outline, String fallback) {
        drawWorldSpriteAt(g, img, wx, wy, tile, outline, fallback, 1.0f);
    }

    private void drawWorldSprite(java.awt.Graphics2D g, BufferedImage img, int wx, int wy, int tile, java.awt.Color outline, String fallback, int facingDx, int facingDy) {
        drawWorldSpriteAt(g, img, wx, wy, tile, outline, fallback, 1.0f, facingDx, facingDy);
    }

    private void drawWorldSpriteAt(java.awt.Graphics2D g, BufferedImage img, double wx, double wy, int tile, java.awt.Color outline, String fallback, float alpha) {
        drawWorldSpriteAt(g, img, wx, wy, tile, outline, fallback, alpha, 0, 0);
    }

    private void drawWorldSpriteAt(java.awt.Graphics2D g, BufferedImage img, double wx, double wy, int tile, java.awt.Color outline, String fallback, float alpha, int facingDx, int facingDy) {
        int sx = (int)Math.round(lastWorldViewOriginX + (wx - lastWorldViewMinX) * tile);
        int sy = (int)Math.round(lastWorldViewOriginY + (wy - lastWorldViewMinY) * tile);
        int pad = Math.max(1, tile / 9);
        int size = Math.max(4, tile - pad * 2);
        java.awt.Composite oldComposite = g.getComposite();
        if (alpha < 0.999f) g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, Math.max(0.05f, Math.min(1f, alpha))));
        if (img != null) {
            g.drawImage(img, sx + pad, sy + pad, size, size, null);
        } else {
            g.setFont(asciiFont.deriveFont(java.awt.Font.BOLD, Math.max(9f, tile * 0.66f)));
            java.awt.FontMetrics fm = g.getFontMetrics();
            String s = fallback == null || fallback.isBlank() ? "?" : fallback.substring(0, 1);
            g.setColor(new java.awt.Color(10, 12, 11, 210));
            g.fillOval(sx + pad, sy + pad, size, size);
            g.setColor(outline == null ? new java.awt.Color(230, 190, 88) : outline);
            g.drawString(s, sx + (tile - fm.stringWidth(s)) / 2, sy + (tile + fm.getAscent() - fm.getDescent()) / 2);
        }
        g.setColor(outline == null ? new java.awt.Color(230, 190, 88) : outline);
        g.drawRect(sx + pad, sy + pad, size, size);
        java.awt.Polygon facing = FacingIndicatorAuthority.tileTriangle(sx, sy, tile, pad, facingDx, facingDy);
        if (facing != null) g.fillPolygon(facing);
        if (alpha < 0.999f) g.setComposite(oldComposite);
    }

    private void drawPlayerWorldSprite(java.awt.Graphics2D g, int cols, int rows, int tile) {
        double rx = playerRenderWorldX();
        double ry = playerRenderWorldY();
        int cellX = (int)Math.floor(rx + 0.5);
        int cellY = (int)Math.floor(ry + 0.5);
        if (!worldPointInView(playerX, playerY, cols, rows) && !worldPointInView(cellX, cellY, cols, rows)) return;
        drawWorldSpriteAt(g, images.getPlayerPortrait(active), rx, ry, tile, new java.awt.Color(245, 214, 118), "@", 1.0f, facingDx, facingDy);
    }

    private void drawTargetCursor(java.awt.Graphics2D g, int wx, int wy, int cols, int rows, int tile, boolean active, java.awt.Color color) {
        if (!active || !worldPointInView(wx, wy, cols, rows)) return;
        int sx = lastWorldViewOriginX + (wx - lastWorldViewMinX) * tile;
        int sy = lastWorldViewOriginY + (wy - lastWorldViewMinY) * tile;
        java.awt.Stroke old = g.getStroke();
        g.setStroke(new java.awt.BasicStroke(Math.max(1f, tile / 10f)));
        g.setColor(color == null ? new java.awt.Color(245, 214, 118) : color);
        g.drawRect(sx + 1, sy + 1, Math.max(2, tile - 3), Math.max(2, tile - 3));
        g.setStroke(old);
    }

    private void drawQuestObjectiveWorldGuidance(java.awt.Graphics2D g, int cols, int rows, int tile) {
        for (QuestObjectiveGuidanceAuthority.ObjectiveGuidance objective :
                QuestObjectiveGuidanceAuthority.orderedActive(activeQuestGuidance, playerX, playerY)) {
            QuestObjectiveGuidanceAuthority.GuidanceReadout readout =
                    QuestObjectiveGuidanceAuthority.describe(objective, playerX, playerY, System.currentTimeMillis());
            if (!readout.showTargetMarker() || !worldPointInView(readout.targetX(), readout.targetY(), cols, rows)) continue;
            java.awt.Color color = objective.kind() == QuestObjectiveGuidanceAuthority.GuidanceKind.UNSAFE
                    ? new java.awt.Color(238, 90, 70, readout.pulsing() ? 245 : 145)
                    : new java.awt.Color(244, 205, 84, readout.pulsing() ? 245 : 145);
            drawTargetCursor(g, readout.targetX(), readout.targetY(), cols, rows, tile, true, color);
        }
    }

    private void drawFacingVisionConeOverlay(java.awt.Graphics2D g, int cols, int rows, int tile) {
        if (world == null) return;
        int range = visionRange();
        double originX = playerRenderWorldX();
        double originY = playerRenderWorldY();
        java.awt.Color fill = new java.awt.Color(86, 148, 178, 32);
        java.awt.Color edge = new java.awt.Color(140, 210, 236, 54);
        for (int y = Math.max(0, lastWorldViewMinY); y < Math.min(world.h, lastWorldViewMinY + rows); y++) {
            for (int x = Math.max(0, lastWorldViewMinX); x < Math.min(world.w, lastWorldViewMinX + cols); x++) {
                if (Math.hypot(x - originX, y - originY) <= 0.35 || !isVisible(x, y) || !withinFacingVisionConeVisual(x, y, range, originX, originY)) continue;
                int sx = lastWorldViewOriginX + (x - lastWorldViewMinX) * tile;
                int sy = lastWorldViewOriginY + (y - lastWorldViewMinY) * tile;
                g.setColor(fill);
                g.fillRect(sx + 1, sy + 1, Math.max(1, tile - 2), Math.max(1, tile - 2));
                g.setColor(edge);
                g.drawRect(sx + 2, sy + 2, Math.max(1, tile - 5), Math.max(1, tile - 5));
            }
        }
    }

    private void drawMovementPlannerOverlay(java.awt.Graphics2D g, int cols, int rows, int tile) {
        if (world == null) return;
        boolean planning = manualMovementPlanActive || mouseMovePreviewActive;
        if (planning) drawMovementRangeOverlay(g, cols, rows, tile);
        if (manualMovementPlanActive) {
            drawMovementPathOverlay(g, manualMovementPlanPath, lookX, lookY, true, manualMovementPlanHazardous, cols, rows, tile);
        }
        if (mouseMovePreviewActive && mouseMovePreviewPath != null) {
            drawMovementPathOverlay(g, mouseMovePreviewPath, mouseMovePreviewTargetX, mouseMovePreviewTargetY,
                    mouseMovePreviewValid, mouseMovePreviewHazardous, cols, rows, tile);
        }
    }

    private void drawMovementRangeOverlay(java.awt.Graphics2D g, int cols, int rows, int tile) {
        boolean[][] reachable = reachableMovementTiles(movementModeRange());
        if (reachable == null) return;
        java.awt.Color fill = movementModeColor(28);
        java.awt.Color edge = movementModeColor(82);
        for (int y = Math.max(0, lastWorldViewMinY); y < Math.min(world.h, lastWorldViewMinY + rows); y++) {
            for (int x = Math.max(0, lastWorldViewMinX); x < Math.min(world.w, lastWorldViewMinX + cols); x++) {
                if (!reachable[x][y] || (x == playerX && y == playerY)) continue;
                int sx = lastWorldViewOriginX + (x - lastWorldViewMinX) * tile;
                int sy = lastWorldViewOriginY + (y - lastWorldViewMinY) * tile;
                g.setColor(fill);
                g.fillRect(sx + 2, sy + 2, Math.max(1, tile - 4), Math.max(1, tile - 4));
                g.setColor(edge);
                g.drawRect(sx + 3, sy + 3, Math.max(1, tile - 7), Math.max(1, tile - 7));
            }
        }
    }

    private void drawMovementPathOverlay(java.awt.Graphics2D g, java.util.List<Point> path, int targetX, int targetY,
                                         boolean usable, boolean hazardous, int cols, int rows, int tile) {
        if (path == null || path.isEmpty()) return;
        java.awt.Color pathColor = !usable ? new java.awt.Color(222, 82, 72, 210)
                : hazardous ? new java.awt.Color(245, 184, 76, 220) : movementModeColor(210);
        java.awt.Color endpointColor = movementPathReaches(path, targetX, targetY) ? pathColor : new java.awt.Color(240, 196, 82, 210);
        java.awt.Stroke oldStroke = g.getStroke();
        g.setStroke(new java.awt.BasicStroke(Math.max(2f, tile / 7f), java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        int prevX = worldTileCenterScreenX(playerX, tile);
        int prevY = worldTileCenterScreenY(playerY, tile);
        for (Point p : path) {
            if (p == null) continue;
            int cx = worldTileCenterScreenX(p.x, tile);
            int cy = worldTileCenterScreenY(p.y, tile);
            g.setColor(pathColor);
            g.drawLine(prevX, prevY, cx, cy);
            g.fillOval(cx - Math.max(2, tile / 8), cy - Math.max(2, tile / 8), Math.max(4, tile / 4), Math.max(4, tile / 4));
            prevX = cx;
            prevY = cy;
        }
        g.setStroke(oldStroke);
        Point end = path.get(path.size() - 1);
        if (end != null && worldPointInView(end.x, end.y, cols, rows)) {
            drawWorldSpriteAt(g, images.getPlayerPortrait(active), end.x, end.y, tile, endpointColor, "@", 0.46f);
            drawTargetCursor(g, end.x, end.y, cols, rows, tile, true, endpointColor);
        }
    }

    private int worldTileCenterScreenX(int wx, int tile) {
        return lastWorldViewOriginX + (wx - lastWorldViewMinX) * tile + tile / 2;
    }

    private int worldTileCenterScreenY(int wy, int tile) {
        return lastWorldViewOriginY + (wy - lastWorldViewMinY) * tile + tile / 2;
    }

    private double playerRenderWorldX() {
        double t = playerMotionProgress();
        if (t >= 1.0) return playerX;
        return playerMotionFromX + (playerMotionToX - playerMotionFromX) * smoothStep(t);
    }

    private double playerRenderWorldY() {
        double t = playerMotionProgress();
        if (t >= 1.0) return playerY;
        return playerMotionFromY + (playerMotionToY - playerMotionFromY) * smoothStep(t);
    }

    private double playerMotionProgress() {
        if (playerMotionDurationMillis <= 0 || playerMotionStartedMillis <= 0) return 1.0;
        long elapsed = System.currentTimeMillis() - playerMotionStartedMillis;
        return Math.max(0.0, Math.min(1.0, elapsed / (double)Math.max(1, playerMotionDurationMillis)));
    }

    private double smoothStep(double t) {
        double c = Math.max(0.0, Math.min(1.0, t));
        return c * c * (3.0 - 2.0 * c);
    }

    private java.awt.Color tileColor(char ch, int x, int y) {
        if (ch == '#') return new java.awt.Color(38, 39, 36);
        if (ch == '.' || ch == ',' || ch == ':' || ch == ';' || ch == '=') return new java.awt.Color(47, 48, 43);
        if (ch == '+' || ch == '/' || ch == '\\' || ch == 'D') return new java.awt.Color(91, 76, 43);
        if (ch == '~') return new java.awt.Color(35, 57, 58);
        if (world != null && world.inBounds(x, y) && world.walkable(x, y)) return new java.awt.Color(54, 50, 42);
        return new java.awt.Color(28, 29, 27);
    }

    private java.awt.Color tileGlyphColor(char ch) {
        if (ch == '+' || ch == '/' || ch == '\\' || ch == 'D') return new java.awt.Color(235, 198, 110);
        if (ch == '~') return new java.awt.Color(145, 198, 190);
        if (Character.isUpperCase(ch)) return new java.awt.Color(205, 177, 112);
        return new java.awt.Color(168, 166, 148);
    }

    private void drawWorldHud(java.awt.Graphics2D g, int w, int h, Rectangle map) {
        g.setFont(uiFont.deriveFont(java.awt.Font.BOLD, 16f));
        g.setColor(new java.awt.Color(225, 205, 140));
        drawUiTextLine(g, "THE MECHANIST", 18, 30);
        g.setFont(smallFont);
        g.setColor(new java.awt.Color(205, 210, 195));
        String zone = world.zoneType.label + " | " + world.zoneCoordText() + " | " + timeText();
        drawUiTextLine(g, GuiLayoutApi.fitLabel(zone, g.getFontMetrics(), Math.max(180, w - 36)), 18, 50);

        Rectangle command = worldCommandBarRect(w, h);
        drawBox(g, command, "Commands");
        if (isWorldCommandSurface()) {
            addWorldCommandButtons(command);
        } else {
            g.setFont(smallFont);
            g.setColor(new java.awt.Color(205, 210, 195));
            int y = command.y + 38;
            for (String line : java.util.List.of("Esc returns to the world.", "Use the panel controls below.", "Movement: WASD or arrows.")) {
                drawUiTextLine(g, GuiLayoutApi.fitLabel(line, g.getFontMetrics(), command.width - 22), command.x + 11, y);
                y += 18;
            }
        }

        drawWorldDescriptor(g, worldDescriptorRect(w, h));
        if (isWorldCommandSurface()) drawOverlayButtons(g);
    }

    private void drawWorldDescriptor(java.awt.Graphics2D g, Rectangle r) {
        String title = switch (panelMode) {
            case LOOK -> "Look";
            case INTERACT -> "Interact";
            case COMBAT -> "Combat";
            default -> "World";
        };
        drawBox(g, r, title);
        int tx = panelMode == PanelMode.COMBAT ? combatX : ((lookCursorActive || panelMode == PanelMode.LOOK || panelMode == PanelMode.INTERACT) ? lookX : playerX);
        int ty = panelMode == PanelMode.COMBAT ? combatY : ((lookCursorActive || panelMode == PanelMode.LOOK || panelMode == PanelMode.INTERACT) ? lookY : playerY);
        if (world != null) {
            tx = Math.max(0, Math.min(world.w - 1, tx));
            ty = Math.max(0, Math.min(world.h - 1, ty));
        }

        ArrayList<String> lines = new ArrayList<>();
        lines.add((active == null ? "Unnamed survivor" : active.name + " / " + active.job)
                + " | Pos " + playerX + "," + playerY
                + " | Food " + food + " Water " + water
                + " | Wounds " + wounds + " Fatigue " + fatigue);
        lines.add("Movement " + movementModeLabel(selectedMovementModeIndex)
                + " / range " + movementModeRange()
                + " / facing " + facingLabel()
                + " / " + activeMotionStateLabel() + ".");
        if (world != null && world.inBounds(tx, ty)) {
            lines.add("Target " + tx + "," + ty + " | " + (panelMode == PanelMode.COMBAT ? targetingSolutionAt(tx, ty).summary : "inspectable world cell"));
            ArrayList<String> stack = tileStackAt(tx, ty);
            for (int i = 0; i < Math.min(5, stack.size()); i++) lines.add(stack.get(i));
        } else {
            lines.add("No valid target selected.");
        }
        if (lastTargetingReport != null && !lastTargetingReport.isBlank()) lines.add("Report: " + lastTargetingReport);
        if (!eventLog.isEmpty()) lines.add("Recent: " + eventLog.get(eventLog.size() - 1));

        int actionReserve = isWorldInlineInteractionPanel() ? Math.min(220, Math.max(118, r.width / 4)) : 0;
        int textW = Math.max(120, r.width - 24 - actionReserve);
        g.setFont(smallFont);
        g.setColor(new java.awt.Color(205, 210, 195));
        int y = r.y + 34;
        for (String line : lines) {
            for (String wrapped : GuiLayoutApi.wrapText(line, g.getFontMetrics(), textW)) {
                if (y > r.y + r.height - 14) break;
                drawUiTextLine(g, wrapped, r.x + 12, y);
                y += 18;
            }
            if (y > r.y + r.height - 14) break;
        }

        if (isWorldInlineInteractionPanel()) {
            int bx = r.x + r.width - actionReserve + 10;
            int bw = Math.max(90, actionReserve - 22);
            int by = r.y + r.height - 74;
            String actionLabel = panelMode == PanelMode.INTERACT ? "Confirm" : panelMode == PanelMode.COMBAT ? "Fire" : "Inspect";
            addOverlayButton(actionLabel, bx, by, bw, 30, "Run the selected target action.", () -> {
                if (panelMode == PanelMode.INTERACT) confirmInteraction();
                else if (panelMode == PanelMode.COMBAT) confirmCombatTarget();
                else examineSelectedLookTarget();
            });
            addOverlayButton("Close", bx, by + 38, bw, 30, "Return to direct world control.", this::closePanel);
        }
    }

    private void addWorldCommandButtons(Rectangle command) {
        String[] labels = { "Look", "Use", "Inv", "Char", "Map", "Auspex", "Build", "Craft", "Scav", "Info", "Fight", "Bomb", "Load", "Wait", "Move", "Plan", "Pause" };
        String[] tips = {
                "Open the look and inspect panel.",
                "Open adjacent interact targeting.",
                "Open carried inventory and base storage.",
                "Open the character panel.",
                "Open the world map panel.",
                "Open sensory and local signal readouts.",
                "Open construction recipes.",
                "Open crafting and workbench recipes.",
                "Open the nearby scavenge/search panel.",
                "Open the event log and command catalog.",
                "Open combat targeting.",
                "Open explosive targeting.",
                "Reload the current ranged weapon, if one is available.",
                "Spend one turn waiting.",
                "Cycle movement mode. Current: " + movementModeLabel(selectedMovementModeIndex) + ".",
                "Begin manual path planning.",
                "Open the pause command panel."
        };
        Runnable[] actions = {
                this::beginLookMode,
                this::beginInteractMode,
                () -> openPanel(PanelMode.INVENTORY),
                () -> openPanel(PanelMode.CHARACTER),
                () -> openPanel(PanelMode.MAP),
                this::openAuspexPanel,
                () -> openPanel(PanelMode.BUILD),
                this::openCraftingPanel,
                this::openScavengePanel,
                this::toggleTacticalSlate,
                this::beginCombatTargeting,
                this::beginExplosiveTargeting,
                this::reloadCurrentRangedWeapon,
                this::waitOneTurn,
                this::cycleMovementMode,
                this::beginManualMovementPlan,
                () -> setScreen(Screen.PAUSE)
        };
        int count = labels.length;
        int cols = command.width >= 148 ? 2 : 1;
        int rows = Math.max(1, (count + cols - 1) / cols);
        int gap = rows > 7 ? 6 : 8;
        int availableW = Math.max(42, command.width - 22 - (cols - 1) * gap);
        int availableH = Math.max(42, command.height - 46 - (rows - 1) * gap);
        int size = Math.max(30, Math.min(64, Math.min(availableW / cols, availableH / rows)));
        int startX = command.x + (command.width - (cols * size + (cols - 1) * gap)) / 2;
        int startY = command.y + 34;
        for (int i = 0; i < count; i++) {
            int col = i % cols;
            int row = i / cols;
            addOverlayButton(labels[i], startX + col * (size + gap), startY + row * (size + gap), size, size, tips[i], actions[i], worldCommandIcon(labels[i]));
        }
    }

    private BufferedImage worldCommandIcon(String key) {
        return systemButtonIconForLabel(key);
    }

    void ensureButtonSystemIcon(ButtonBox button) {
        if (button == null || button.icon != null) return;
        button.icon = systemButtonIconForLabel(button.label);
    }

    BufferedImage systemButtonIconForLabel(String label) {
        int[] cell = systemButtonCellForLabel(label);
        return cell == null ? null : images.getCompiledSystemIcon(cell[0], cell[1], cell[2]);
    }

    private int[] systemButtonCellForLabel(String label) {
        String key = normalizeButtonLabel(label);
        if (key.isBlank()) return null;
        if (key.contains("main menu")) return systemCell(3, 5, 5);
        if (key.contains("save") && key.contains("load")) return systemCell(3, 5, 3);
        if (key.contains("load")) return systemCell(3, 5, 3);
        if (key.contains("save")) return systemCell(3, 5, 2);
        if (key.contains("new game") || key.contains("start game") || key.startsWith("start") || key.contains("generate") || key.equals("play") || key.equals("resume") || key.equals("continue")) return systemCell(4, 1, 1);
        if (key.equals("quit")) return systemCell(3, 5, 4);
        if (key.equals("options") || key.equals("graphics") || key.equals("display") || key.equals("text/ui") || key.equals("jvm") || key.equals("qol") || key.equals("audio") || key.equals("controls") || key.equals("game") || key.equals("access") || key.contains("settings")) return systemCell(3, 5, 1);
        if (key.contains("mode") || key.contains("quality") || key.contains("palette") || key.contains("color key") || key.contains("runtime") || key.contains("java2d") || key.contains("gc")) return systemCell(4, 5, 4);
        if (key.contains("resolution") || key.contains("viewport") || key.contains("textures") || key.contains("downscale")) return systemCell(4, 4, 4);
        if (key.endsWith("+") || key.contains(" +")) return systemCell(4, 2, 5);
        if (key.endsWith("-") || key.contains(" -")) return systemCell(4, 3, 1);
        if (key.contains("apply window") || key.contains("accept")) return systemCell(4, 2, 4);
        if (key.contains("screensaver") || key.contains("boot") || key.contains("frame limit") || key.contains("reduced motion") || key.contains("string dedup") || key.contains("trans blit") || key.contains("no aa")) return systemCell(4, 2, 1);
        if (key.contains("sfx") || key.contains("music") || key.contains("voice") || key.contains("audio")) return systemCell(4, 5, 3);
        if (key.contains("keyboard") || key.contains("mouse") || key.contains("pad") || key.contains("xbox") || key.contains("playstation") || key.contains("steam deck")) return systemCell(4, 5, 2);
        if (key.contains("fps") || key.contains("diagnostics") || key.contains("stress")) return systemCell(4, 5, 1);
        if (key.contains("lighting")) return systemCell(4, 5, 3);
        if (key.contains("payload root") || key.contains("clear payload")) return key.contains("clear") ? systemCell(4, 2, 3) : systemCell(4, 4, 1);
        if (key.contains("heap")) return key.contains("+") ? systemCell(4, 2, 5) : systemCell(4, 3, 1);
        if (key.contains("restart")) return systemCell(4, 2, 1);
        if (key.equals("infopedia") || key.equals("info pedia") || key.equals("info")) return systemCell(3, 2, 2);
        if (key.equals("knowledge") || key.equals("knowledges")) return systemCell(3, 1, 4);
        if (key.equals("multiplayer")) return systemCell(4, 5, 2);
        if (key.equals("tools") || key.equals("editor")) return systemCell(4, 5, 4);
        if (key.equals("sector audit") || key.equals("scan") || key.equals("auspex")) return systemCell(3, 2, 1);
        if (key.equals("back") || key.equals("exit")) return systemCell(3, 3, 1);
        if (key.equals("close")) return systemCell(2, 2, 4);
        if (key.equals("confirm") || key.equals("apply") || key.equals("accept")) return systemCell(4, 2, 4);
        if (key.equals("cancel")) return systemCell(4, 2, 3);
        if (key.equals("reroll") || key.equals("refresh") || key.equals("reset")) return systemCell(4, 2, 2);
        if (key.equals("play")) return systemCell(4, 1, 1);
        if (key.equals("pause")) return systemCell(4, 1, 4);
        if (key.equals("step")) return systemCell(4, 1, 2);
        if (key.equals("end") || key.equals("stop")) return systemCell(4, 1, 5);
        if (key.equals("prev") || key.endsWith("<") || key.contains("previous")) return systemCell(4, 3, 4);
        if (key.equals("next") || key.endsWith(">")) return systemCell(4, 3, 5);
        if (key.equals("zone") || key.equals("map") || key.equals("plan")) return systemCell(3, 1, 5);
        if (key.equals("density")) return systemCell(4, 5, 1);
        if (key.equals("overlay")) return systemCell(4, 5, 4);
        if (key.equals("look") || key.equals("inspect") || key.equals("examine")) return systemCell(3, 2, 2);
        if (key.equals("use") || key.equals("interact")) return systemCell(3, 2, 3);
        if (key.equals("inv") || key.equals("inventory")) return systemCell(3, 1, 2);
        if (key.equals("char") || key.equals("character") || key.contains("edit name")) return systemCell(3, 1, 3);
        if (key.equals("build")) return systemCell(2, 1, 1);
        if (key.equals("craft") || key.equals("crafting") || key.equals("workbench")) return systemCell(2, 1, 2);
        if (key.equals("scav") || key.equals("scavenge") || key.equals("search")) return systemCell(2, 4, 2);
        if (key.equals("fight") || key.equals("attack")) return systemCell(3, 1, 1);
        if (key.equals("bomb") || key.equals("hunt")) return systemCell(4, 4, 3);
        if (key.equals("wait") || key.equals("sleep")) return systemCell(2, 4, 3);
        if (key.equals("move") || key.equals("walk")) return systemCell(2, 2, 1);
        if (key.equals("sneak")) return systemCell(2, 1, 3);
        if (key.equals("run")) return systemCell(2, 2, 2);
        if (key.equals("sprint")) return systemCell(3, 4, 3);
        if (key.equals("roster") || key.equals("recruit")) return systemCell(2, 5, 2);
        if (key.contains("store")) return systemCell(4, 4, 1);
        if (key.contains("take")) return systemCell(4, 4, 2);
        if (key.equals("open") || key.equals("enter")) return systemCell(3, 3, 2);
        if (key.equals("lock")) return systemCell(3, 3, 4);
        if (key.equals("unlock")) return systemCell(3, 3, 3);
        return null;
    }

    private int[] systemCell(int sheet, int row, int col) {
        return new int[] { sheet, row, col };
    }

    private String normalizeButtonLabel(String label) {
        String s = label == null ? "" : label.trim();
        while (s.startsWith(">")) s = s.substring(1).trim();
        s = s.replaceAll("^\\d+\\.\\s*", "");
        s = s.replace("->", " store").replace("<-", " take");
        s = s.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        return s;
    }

    private void drawNewGameSetupSurface(java.awt.Graphics2D g, int w, int h) {
        buttons.clear();
        BufferedImage backdrop = images.get("new_world_backdrop_rebase");
        if (backdrop != null) g.drawImage(backdrop, 0, 0, w, h, null);
        g.setColor(new java.awt.Color(5, 7, 8, backdrop == null ? 255 : 205));
        g.fillRect(0, 0, w, h);

        Rectangle panel = new Rectangle(Math.max(18, w / 2 - Math.min(560, w - 60) / 2), Math.max(42, h / 2 - Math.min(600, h - 84) / 2), Math.min(1120, w - 36), Math.min(600, h - 84));
        if (panel.width < Math.min(980, w - 36)) panel.width = Math.min(980, w - 36);
        panel.x = Math.max(18, (w - panel.width) / 2);
        drawOverlayFrame(g, panel, "NEW GAME / CHARACTER AND WORLD SETUP");

        Rectangle body = new Rectangle(panel.x + 18, panel.y + 54, panel.width - 36, panel.height - 112);
        int gap = 14;
        int leftW = Math.max(190, Math.min(240, body.width / 4));
        int rightW = Math.max(250, Math.min(330, body.width / 3));
        int midW = Math.max(260, body.width - leftW - rightW - gap * 2);
        Rectangle roster = new Rectangle(body.x, body.y, leftW, body.height);
        Rectangle character = new Rectangle(roster.x + roster.width + gap, body.y, midW, body.height);
        Rectangle setup = new Rectangle(character.x + character.width + gap, body.y, rightW, body.height);
        drawNewGameRoster(g, roster);
        drawNewGameCharacter(g, character);
        drawNewGameWorldSetup(g, setup);

        int by = panel.y + panel.height - 46;
        addOverlayButton("Start Game", panel.x + 18, by, 132, 30, "Generate the world with this character and setup.", this::startPackagedClientNewGame);
        addOverlayButton("Back", panel.x + 160, by, 86, 30, "Return to the main menu.", () -> { newGameSetupActive = false; characterNameEditActive = false; setScreen(Screen.MENU); });
        drawOverlayButtons(g);
    }

    private void drawNewGameRoster(java.awt.Graphics2D g, Rectangle r) {
        drawBox(g, r, "Candidate Roster");
        if (candidates.isEmpty()) prepareNewGameRoster(System.currentTimeMillis());
        int y = r.y + 38;
        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            int idx = i;
            ButtonBox b = new ButtonBox((i == candidateIndex ? "> " : "") + c.name, r.x + 10, y, r.width - 20, 30, c.job, () -> {
                candidateIndex = idx;
                active = selectedNewGameCandidate();
                characterNameEditActive = false;
                repaint();
            }, images.getPlayerPortrait(c));
            buttons.add(b);
            y += 38;
        }
        y += 12;
        addOverlayButton("Prev", r.x + 10, y, 74, 28, "Previous candidate.", () -> cycleSelectedCandidate(-1));
        addOverlayButton("Next", r.x + 92, y, 74, 28, "Next candidate.", () -> cycleSelectedCandidate(1));
        y += 36;
        addOverlayButton("Reroll", r.x + 10, y, 82, 28, "Reroll the selected candidate.", this::rerollSelectedCandidate);
        addOverlayButton("Roster", r.x + 100, y, 82, 28, "Generate a fresh roster.", this::rerollNewGameRoster);
    }

    private void drawNewGameCharacter(java.awt.Graphics2D g, Rectangle r) {
        Candidate c = selectedNewGameCandidate();
        drawBox(g, r, "Character");
        int x = r.x + 14;
        int y = r.y + 34;
        BufferedImage portrait = c == null ? null : images.getPlayerPortrait(c);
        int portraitSize = Math.min(124, Math.max(72, r.width / 3));
        if (portrait != null) {
            g.drawImage(portrait, x, y, portraitSize, portraitSize, null);
            g.setColor(new java.awt.Color(145, 118, 64, 170));
            g.drawRect(x, y, portraitSize, portraitSize);
        }
        int tx = x + portraitSize + 14;
        characterNameEditRect = new Rectangle(tx, y + 22, Math.max(140, r.x + r.width - tx - 14), 30);
        g.setFont(smallFont);
        g.setColor(new java.awt.Color(177, 180, 162));
        drawUiTextLine(g, "Name", tx, y + 16);
        g.setColor(characterNameEditActive ? new java.awt.Color(54, 47, 31, 235) : new java.awt.Color(16, 18, 17, 230));
        g.fillRect(characterNameEditRect.x, characterNameEditRect.y, characterNameEditRect.width, characterNameEditRect.height);
        g.setColor(characterNameEditActive ? new java.awt.Color(245, 220, 140) : new java.awt.Color(145, 118, 64, 160));
        g.drawRect(characterNameEditRect.x, characterNameEditRect.y, characterNameEditRect.width, characterNameEditRect.height);
        g.setColor(new java.awt.Color(225, 205, 140));
        String name = c == null ? "" : c.name;
        drawUiTextLine(g, GuiLayoutApi.fitLabel(name + (characterNameEditActive ? "|" : ""), g.getFontMetrics(), characterNameEditRect.width - 12), characterNameEditRect.x + 8, characterNameEditRect.y + 20);
        g.setColor(new java.awt.Color(205, 210, 195));
        drawUiTextLine(g, "Job: " + (c == null ? "" : c.job), tx, y + 82);
        drawUiTextLine(g, c == null ? "" : c.ageYears + " years / " + c.ageBand, tx, y + 104);

        int buttonY = y + portraitSize + 14;
        addOverlayButton("Job <", x, buttonY, 74, 28, "Previous job.", () -> cycleSelectedCandidateJob(-1));
        addOverlayButton("Job >", x + 82, buttonY, 74, 28, "Next job.", () -> cycleSelectedCandidateJob(1));
        addOverlayButton("Edit Name", x + 164, buttonY, 106, 28, "Edit candidate name.", () -> { characterNameEditActive = true; requestFocusInWindow(); repaint(); });

        Rectangle stats = new Rectangle(x, buttonY + 42, r.width - 28, r.y + r.height - buttonY - 54);
        drawBox(g, stats, "Stats / Body");
        g.setFont(smallFont);
        int sy = stats.y + 34;
        if (c != null) {
            int colW = Math.max(120, (stats.width - 24) / 2);
            int idx = 0;
            for (Map.Entry<String,Integer> e : c.stats.entrySet()) {
                int cx = stats.x + 12 + (idx / 7) * colW;
                int cy = sy + (idx % 7) * 18;
                g.setColor(new java.awt.Color(205, 210, 195));
                drawUiTextLine(g, e.getKey() + " " + e.getValue(), cx, cy);
                idx++;
            }
            int by = sy + 138;
            g.setColor(new java.awt.Color(177, 180, 162));
            int shown = 0;
            for (BodyPart part : c.body.values()) {
                if (shown >= 5 || by > stats.y + stats.height - 12) break;
                drawUiTextLine(g, part.name + " END " + part.endurance + " / AGI " + part.agility, stats.x + 12, by);
                by += 18;
                shown++;
            }
        }
    }

    private void drawNewGameWorldSetup(java.awt.Graphics2D g, Rectangle r) {
        if (worldSetup == null) worldSetup = WorldSetupSettings.standard();
        drawBox(g, r, "World Management");
        g.setFont(smallFont);
        int y = r.y + 34;
        g.setColor(new java.awt.Color(205, 210, 195));
        for (String line : worldSetup.detailLines()) {
            if (y > r.y + r.height - 236) break;
            for (String wrapped : GuiLayoutApi.wrapText(line, g.getFontMetrics(), r.width - 24)) {
                if (y > r.y + r.height - 236) break;
                drawUiTextLine(g, wrapped, r.x + 12, y);
                y += 17;
            }
        }
        int by = Math.max(y + 12, r.y + r.height - 222);
        String[] labels = {
                "NPC " + WorldSetupSettings.NPC_DENSITY[worldSetup.npcDensity],
                "Size " + WorldSetupSettings.ZONE_SIZE[worldSetup.zoneSize],
                "Density " + WorldSetupSettings.ZONE_DENSITY[worldSetup.zoneDensity],
                "Prices " + WorldSetupSettings.PRICE[worldSetup.priceDifficulty],
                "Craft " + WorldSetupSettings.CRAFT[worldSetup.craftDifficulty],
                "Hoarder " + (worldSetup.hoarderMode ? "ON" : "OFF"),
                "Age " + WorldSetupSettings.AGE[worldSetup.simulationAge]
        };
        for (int i = 0; i < labels.length; i++) {
            int option = i;
            int row = i / 2;
            int col = i % 2;
            int bw = (r.width - 32) / 2;
            int bx = r.x + 12 + col * (bw + 8);
            int bh = 28;
            addOverlayButton(labels[i], bx, by + row * 34, bw, bh, "Cycle " + labels[i] + ".", () -> cycleWorldSetupOption(option));
        }
    }

    private void drawActivePanelOverlay(java.awt.Graphics2D g, int w, int h) {
        buttons.clear();
        Rectangle panel = activePanelRect(w, h);
        drawOverlayFrame(g, panel, panelTitle());
        Rectangle body = new Rectangle(panel.x + 18, panel.y + 54, panel.width - 36, panel.height - 112);
        if (panelMode == PanelMode.INVENTORY || screen == Screen.INVENTORY) {
            drawInventoryOverlay(g, body);
        } else if (panelMode == PanelMode.CHARACTER || screen == Screen.CHARACTER) {
            drawCharacterOverlay(g, body);
        } else if (panelMode == PanelMode.MAP || screen == Screen.MAP) {
            drawMapOverlay(g, body);
        } else if (panelMode == PanelMode.TRADE) {
            drawTradeOverlay(g, body);
        } else if (panelMode == PanelMode.CONTAINER) {
            drawContainerOverlay(g, body);
        } else if (panelMode == PanelMode.DIALOGUE) {
            drawDialogueOverlay(g, body);
        } else if (panelMode == PanelMode.OBJECT) {
            drawObjectInteractionOverlay(g, body);
        } else if (panelMode == PanelMode.LOOK || panelMode == PanelMode.INTERACT || panelMode == PanelMode.COMBAT) {
            drawTargetOverlay(g, body);
        } else if (panelMode == PanelMode.BUILD) {
            drawBuildOverlay(g, body);
        } else if (panelMode == PanelMode.WORKBENCH || panelMode == PanelMode.CRAFTING) {
            drawCraftingOverlay(g, body);
        } else if (panelMode == PanelMode.AUSPEX) {
            drawAuspexOverlay(g, body);
        } else if (panelMode == PanelMode.SCAVENGE) {
            drawScavengeOverlay(g, body);
        } else if (panelMode == PanelMode.INFOPEDIA) {
            drawInfopediaOverlay(g, body);
        } else if (screen == Screen.PAUSE || panelMode == PanelMode.INFO || screen == Screen.INFO || panelMode == PanelMode.CONSOLE) {
            drawInfoPauseOverlay(g, body);
        } else {
            drawGenericOverlay(g, body);
        }
        addOverlayButton("Close", panel.x + panel.width - 126, panel.y + panel.height - 46, 96, 30, "Return to the world.", this::closePanel);
        drawOverlayButtons(g);
    }

    private Rectangle activePanelRect(int w, int h) {
    boolean characterSurface = panelMode == PanelMode.CHARACTER || screen == Screen.CHARACTER;
    double widthShare = characterSurface ? 0.90 : 0.78;
    double heightShare = characterSurface ? 0.84 : 0.68;
    int pw = Math.max(characterSurface ? 820 : 520,
            Math.min(w - 48, (int)Math.round(w * widthShare)));
    int ph = Math.max(characterSurface ? 560 : 360,
            Math.min(h - 92, (int)Math.round(h * heightShare)));
    return new Rectangle(Math.max(18, (w - pw) / 2), Math.max(46, (h - ph) / 2), pw, ph);
}

    private String panelTitle() {
        if (screen == Screen.PAUSE) return "PAUSE / COMMAND";
        if (panelMode == PanelMode.NONE) return screen.name();
        return switch (panelMode) {
            case INVENTORY -> "INVENTORY / STORAGE";
            case CHARACTER -> "CHARACTER";
            case TRADE -> "TRADE / STORE";
            case CONTAINER -> "CONTAINER / TRANSFER";
            case DIALOGUE -> "CONVERSATION";
            case OBJECT -> "OBJECT INTERACTION";
            case LOOK -> "LOOK / INSPECT";
            case INTERACT -> "INTERACT";
            case COMBAT -> "COMBAT TARGETING";
            case BUILD -> "BUILD / CONSTRUCTION";
            case WORKBENCH, CRAFTING -> "WORKBENCH / CRAFTING";
            case AUSPEX -> "AUSPEX / LOCAL SIGNALS";
            case SCAVENGE -> "SCAVENGE / SEARCH";
            case MAP -> "WORLD MAP";
            case INFOPEDIA -> "INFOPEDIA / SEMANTIC ASSET INDEX";
            case INFO -> "INFO / EVENT LOG";
            case CONSOLE -> "CONSOLE";
            default -> panelMode.name();
        };
    }

    private void drawOverlayFrame(java.awt.Graphics2D g, Rectangle panel, String title) {
        g.setColor(new java.awt.Color(5, 7, 7, 236));
        g.fillRoundRect(panel.x, panel.y, panel.width, panel.height, 10, 10);
        g.setColor(new java.awt.Color(145, 118, 64, 175));
        g.drawRoundRect(panel.x, panel.y, panel.width, panel.height, 10, 10);
        g.setColor(new java.awt.Color(24, 27, 25, 230));
        g.fillRect(panel.x + 2, panel.y + 2, panel.width - 4, 44);
        g.setFont(uiFont.deriveFont(java.awt.Font.BOLD, 18f));
        g.setColor(new java.awt.Color(225, 205, 140));
        drawUiTextLine(g, title == null ? "PANEL" : title, panel.x + 18, panel.y + 29);
        g.setFont(smallFont);
        g.setColor(new java.awt.Color(177, 180, 162));
        String prompt = ControlReferenceTextSubsystem.livePanelPrompt(activePanelPromptContext(), controlsTab);
        drawUiTextLine(g, GuiLayoutApi.fitLabel(prompt, g.getFontMetrics(), Math.max(220, panel.width - 250)),
                panel.x + 230, panel.y + 29);
    }

    private String activePanelPromptContext() {
        if (manualMovementPlanActive) return "Movement planning";
        if (screen == Screen.PAUSE) return "Pause";
        return switch (panelMode) {
            case INVENTORY -> "Inventory";
            case CHARACTER -> "Character";
            case TRADE -> "Trade";
            case CONTAINER -> "Container";
            case DIALOGUE -> "Conversation";
            case OBJECT -> "Object";
            case LOOK -> "Look";
            case INTERACT -> "Interact";
            case COMBAT -> "Combat";
            case BUILD -> "Build";
            case WORKBENCH, CRAFTING -> "Crafting";
            case AUSPEX -> "Auspex";
            case SCAVENGE -> "Scavenge";
            case MAP -> "Map";
            case INFOPEDIA -> "Infopedia";
            default -> "Panel";
        };
    }

    private void drawInventoryOverlay(java.awt.Graphics2D g, Rectangle body) {
        int gap = 14;
        int col = Math.max(140, (body.width - gap * 2) / 3);
        Rectangle carried = new Rectangle(body.x, body.y, col, body.height);
        Rectangle storage = new Rectangle(body.x + col + gap, body.y, col, body.height);
        Rectangle detail = new Rectangle(body.x + (col + gap) * 2, body.y, Math.max(160, body.width - (col + gap) * 2), body.height);
        drawListBox(g, carried, "Carried", inventory, selectedInventoryIndex, !inventoryTargetColumnActive);
        drawListBox(g, storage, "Base Storage", baseStorage, selectedTargetInventoryIndex, inventoryTargetColumnActive);
        String selected = inventoryTargetColumnActive ? selectedBaseStorageItem() : selectedInventoryItem();
        ArrayList<String> lines = new ArrayList<>();
        lines.addAll(InventoryReadabilityAuthority.detailLines(selected, inventoryTargetColumnActive,
                selected != null && ItemQuality.namesMatch(selected, equippedLeftHandItem),
                selected != null && ItemQuality.namesMatch(selected, equippedRightHandItem),
                inventoryWeight(), carryCapacity(), peekProvenanceForItem(selected)));
        lines.addAll(InventoryReadabilityAuthority.stackLines(selected,
                inventoryTargetColumnActive ? baseStorage : inventory));
        lines.add("Weight " + inventoryWeight() + "/" + carryCapacity() + " / script " + carriedScript + " / banked " + baseStashedScript);
        lines.add("Equipped L/R: " + safeLabel(equippedLeftHandItem, "LEFT EMPTY") + " / " + safeLabel(equippedRightHandItem, "RIGHT EMPTY"));
        drawDetailBox(g, detail, "Selection", lines, selected == null ? null : images.getItemIcon(selected));
        int by = detail.y + detail.height - 70;
        addOverlayButton("Use", detail.x + 12, by, 72, 28, "Use or inspect the selected carried item.", this::useSelectedInventoryItemBody);
        addOverlayButton("Equip L", detail.x + 92, by, 82, 28, "Equip selected carried item in the left hand.", () -> equipSelectedInventoryItemToHand(true));
        addOverlayButton("Equip R", detail.x + 182, by, 82, 28, "Equip selected carried item in the right hand.", () -> equipSelectedInventoryItemToHand(false));
        by += 34;
        addOverlayButton("Store ->", detail.x + 12, by, 92, 28, "Move the carried item to base storage.", this::storeSelectedInventoryItem);
        addOverlayButton("<- Take", detail.x + 112, by, 92, 28, "Move the selected base item to carried inventory.", this::takeSelectedBaseStorageItem);
        addOverlayButton("Item Info", detail.x + 212, by, 88, 28, "Open the Inventory and Equipment mechanic reference.",
                () -> InfopediaHotLinkAuthority.openMechanic(this, "inventory-equipment", "inventory equipment reference"));
    }

    private void drawCharacterOverlay(java.awt.Graphics2D g, Rectangle body) {
    Candidate c = active;
    int gap = 12;
    int portraitWidth = Math.max(150, Math.min(190, body.width / 6));
    int statsWidth = Math.max(220, Math.min(310, body.width / 4));

    Rectangle portrait = new Rectangle(body.x, body.y, portraitWidth, body.height);
    drawDetailBox(g, portrait, c == null ? "Character" : c.name,
            java.util.List.of(c == null ? "No active character." : c.job,
                    c == null ? "" : c.ageYears + " years / " + c.ageBand),
            c == null ? null : images.getPlayerPortrait(c));

    Rectangle stats = new Rectangle(portrait.x + portrait.width + gap, body.y, statsWidth, body.height);
    ArrayList<String> statLines = new ArrayList<>();
    if (c != null) for (Map.Entry<String,Integer> e : c.stats.entrySet()) statLines.add(e.getKey() + ": " + e.getValue());
    statLines.add("XP " + xp + " / knowledge credits " + knowledgeCredits);
    statLines.add("Food " + food + " / water " + water + " / fatigue " + fatigue);
    statLines.add("Wounds " + wounds + " / bleeding " + bleeding + " / pain " + pain);
    drawDetailBox(g, stats, "Stats", statLines, null);

    Rectangle rightColumn = new Rectangle(stats.x + stats.width + gap, body.y,
            Math.max(360, body.x + body.width - stats.x - stats.width - gap), body.height);
    int topHeight = Math.max(300, Math.min(rightColumn.height - 150, (int)Math.round(rightColumn.height * 0.62)));
    int dollWidth = Math.max(210, Math.min(300, rightColumn.width / 2));
    Rectangle dollBox = new Rectangle(rightColumn.x, rightColumn.y, dollWidth, topHeight);
    Rectangle equipmentBox = new Rectangle(dollBox.x + dollBox.width + gap, rightColumn.y,
            Math.max(190, rightColumn.width - dollBox.width - gap), topHeight);

    CharacterPaperDollAuthority.paint(g, dollBox, c, smallFont.deriveFont(10f));

    CharacterPaperDollAuthority.EquipmentView selectedEquipment =
            CharacterPaperDollAuthority.selectedEquipment(selectedCharacterEquipmentSlot,
                    equippedLeftHandItem, equippedRightHandItem, equippedClothing);
    ArrayList<String> bodySummary = new ArrayList<>(BodyConditionReadabilityAuthority.summary(c, wounds, bleeding,
            infectionRisk, pain, fatigue, sleepNeed, food, water, equippedClothing,
            equippedLeftHandItem, equippedRightHandItem));
    ArrayList<String> equipmentLines = new ArrayList<>();
    equipmentLines.add("Selected slot: " + selectedEquipment.slot().label());
    equipmentLines.add("Equipped: " + selectedEquipment.itemName());
    equipmentLines.add(selectedEquipment.empty()
            ? "This slot is currently empty."
            : "Unequip returns this item to carried inventory.");
    equipmentLines.add("Active weapon hand: " + (activeWeaponHandIndex == 0 ? "left" : "right") + ".");
    equipmentLines.addAll(bodySummary.subList(0, Math.min(4, bodySummary.size())));
    BufferedImage selectedIcon = selectedEquipment.empty() ? null : images.getItemIcon(selectedEquipment.itemName());
    drawDetailBox(g, equipmentBox, "Equipped Items", equipmentLines, selectedIcon);

    int slotY = equipmentBox.y + Math.max(116, equipmentBox.height - 154);
    java.util.List<CharacterPaperDollAuthority.EquipmentView> slots =
            CharacterPaperDollAuthority.equipment(equippedLeftHandItem, equippedRightHandItem, equippedClothing);
    for (int i = 0; i < slots.size(); i++) {
        final int slotIndex = i;
        CharacterPaperDollAuthority.EquipmentView slot = slots.get(i);
        addOverlayButton(slot.rowLabel(i == selectedCharacterEquipmentSlot),
                equipmentBox.x + 10, slotY + i * 32, equipmentBox.width - 20, 27,
                "Select " + slot.slot().label() + " equipment slot.",
                () -> selectCharacterEquipmentSlot(slotIndex));
    }
    int actionY = slotY + slots.size() * 32 + 2;
    addOverlayButton("Unequip Selected", equipmentBox.x + 10, actionY,
            Math.max(130, equipmentBox.width - 116), 28,
            "Unequip the selected character equipment slot.", this::unequipSelectedCharacterEquipment);
    addOverlayButton("Health Info", equipmentBox.x + equipmentBox.width - 100, actionY,
            90, 28, "Open the Body Condition mechanic reference.",
            () -> InfopediaHotLinkAuthority.openMechanic(this, "body-condition", "character health reference"));

    int lowerY = rightColumn.y + topHeight + gap;
    int lowerHeight = Math.max(100, rightColumn.y + rightColumn.height - lowerY);
    int statusWidth = Math.max(240, rightColumn.width / 2);
    Rectangle statusBox = new Rectangle(rightColumn.x, lowerY, statusWidth, lowerHeight);
    Rectangle rosterBox = new Rectangle(statusBox.x + statusBox.width + gap, lowerY,
            Math.max(180, rightColumn.width - statusBox.width - gap), lowerHeight);

    ArrayList<String> limbLines = new ArrayList<>(CharacterPaperDollAuthority.regionReadouts(
            c, new Rectangle(0, 0, 280, 440)));
    if (limbLines.isEmpty()) limbLines.add("No tracked body regions are available.");
    drawDetailBox(g, statusBox, "Limb Hit Points / Status", limbLines, null);

    drawDetailBox(g, rosterBox, "Faction Members",
            FactionRosterReadabilityAuthority.summary(this, Math.max(1, (rosterBox.height - 82) / 18)), null);
    addOverlayButton("Personnel Info", rosterBox.x + 12, rosterBox.y + rosterBox.height - 36, 112, 28,
            "Open the faction personnel and staffing reference.",
            () -> InfopediaHotLinkAuthority.openMechanic(this, "faction-personnel", "character personnel reference"));
}

private void selectCharacterEquipmentSlot(int slotIndex) {
    selectedCharacterEquipmentSlot = CharacterPaperDollAuthority.EquipmentSlot.at(slotIndex).ordinal();
    repaint();
}

private void unequipSelectedCharacterEquipment() {
    CharacterPaperDollAuthority.EquipmentSlot slot =
            CharacterPaperDollAuthority.EquipmentSlot.at(selectedCharacterEquipmentSlot);
    if (slot == CharacterPaperDollAuthority.EquipmentSlot.LEFT_HAND) {
        unequipEquipmentSlot(0);
        return;
    }
    if (slot == CharacterPaperDollAuthority.EquipmentSlot.RIGHT_HAND) {
        unequipEquipmentSlot(1);
        return;
    }
    if (equippedClothing == null) {
        logEvent("Body protection slot is already empty.");
        repaint();
        return;
    }
    String itemName = equippedClothing.name;
    if (itemName != null && !itemName.isBlank()) inventory.add(itemName);
    equippedClothing = null;
    logEvent("Unequipped body protection: " + safeLabel(itemName, "clothing") + ".");
    repaint();
}

    private void drawMapOverlay(java.awt.Graphics2D g, Rectangle body) {
        Rectangle mapBox = new Rectangle(body.x, body.y, Math.max(260, body.width * 2 / 3), body.height);
        drawBox(g, mapBox, "Current Slice");
        if (world != null && world.w > 0 && world.h > 0) {
            double sx = mapBox.width / (double)world.w;
            double sy = mapBox.height / (double)world.h;
            for (int x = 0; x < world.w; x++) for (int y = 0; y < world.h; y++) {
                g.setColor(tileColor(world.tiles[x][y], x, y));
                int rx = mapBox.x + (int)Math.floor(x * sx);
                int ry = mapBox.y + (int)Math.floor(y * sy);
                int rw = Math.max(1, (int)Math.ceil(sx));
                int rh = Math.max(1, (int)Math.ceil(sy));
                g.fillRect(rx, ry, rw, rh);
            }
            g.setColor(new java.awt.Color(245, 214, 118));
            g.fillOval(mapBox.x + (int)Math.round(playerX * sx) - 3, mapBox.y + (int)Math.round(playerY * sy) - 3, 7, 7);
            for (QuestObjectiveGuidanceAuthority.ObjectiveGuidance objective :
                    QuestObjectiveGuidanceAuthority.orderedActive(activeQuestGuidance, playerX, playerY)) {
                QuestObjectiveGuidanceAuthority.GuidanceReadout readout =
                        QuestObjectiveGuidanceAuthority.describe(objective, playerX, playerY, System.currentTimeMillis());
                if (!readout.showTargetMarker()) continue;
                int mx = mapBox.x + (int)Math.round(readout.targetX() * sx);
                int my = mapBox.y + (int)Math.round(readout.targetY() * sy);
                g.setColor(objective.kind() == QuestObjectiveGuidanceAuthority.GuidanceKind.UNSAFE
                        ? new java.awt.Color(238, 90, 70) : new java.awt.Color(244, 205, 84));
                int radius = readout.pulsing() ? 6 : 4;
                g.drawOval(mx - radius, my - radius, radius * 2, radius * 2);
            }
        }
        Rectangle info = new Rectangle(mapBox.x + mapBox.width + 14, body.y, Math.max(180, body.x + body.width - mapBox.x - mapBox.width - 14), body.height);
        ArrayList<String> mapInfo = new ArrayList<>();
        mapInfo.add(world == null ? "No world loaded." : world.zoneType.label);
        mapInfo.add(world == null ? "" : world.zoneCoordText());
        mapInfo.add("Visited slices " + visitedZoneInstances.size());
        mapInfo.add("Visited zone types " + visitedZoneTypes.size());
        mapInfo.add(atlas == null ? "Atlas unavailable." : atlas.summary());
        List<QuestObjectiveGuidanceAuthority.ObjectiveGuidance> guidance =
                QuestObjectiveGuidanceAuthority.orderedActive(activeQuestGuidance, playerX, playerY);
        if (guidance.isEmpty()) mapInfo.add("No active objective guidance.");
        else for (QuestObjectiveGuidanceAuthority.ObjectiveGuidance objective : guidance) {
            mapInfo.add(QuestObjectiveGuidanceAuthority.describe(objective, playerX, playerY, System.currentTimeMillis()).summary());
            if (mapInfo.size() >= 11) break;
        }
        mapInfo.addAll(ContractObjectiveReadabilityAuthority.summary(factionContracts, inventory, baseStorage, 2));
        drawDetailBox(g, info, "World / Objectives", mapInfo, null);
        addOverlayButton("Contract Info", info.x + 12, info.y + info.height - 36, 112, 28,
                "Open the Contract Objectives and Evidence mechanic reference.",
                () -> InfopediaHotLinkAuthority.openMechanic(this, "contract-evidence", "map contract reference"));
    }

    private void drawTargetOverlay(java.awt.Graphics2D g, Rectangle body) {
        int tx = panelMode == PanelMode.COMBAT ? combatX : lookX;
        int ty = panelMode == PanelMode.COMBAT ? combatY : lookY;
        tx = world == null ? tx : Math.max(0, Math.min(world.w - 1, tx));
        ty = world == null ? ty : Math.max(0, Math.min(world.h - 1, ty));
        Rectangle preview = new Rectangle(body.x, body.y, Math.min(170, body.width / 4), Math.min(190, body.height));
        BufferedImage img = null;
        String previewTitle = "Target " + tx + "," + ty;
        NpcEntity npc = world == null ? null : world.npcAt(tx, ty);
        MapObjectState obj = world == null ? null : world.mapObjectAt(tx, ty);
        if (npc != null) { img = images.getNpcPortraitFor(npc); previewTitle = npc.name; }
        else if (obj != null) { img = images.getMapObjectImage(obj); previewTitle = obj.label; }
        else if (world != null && world.inBounds(tx, ty)) {
            CompiledTileDescriptor d = TileDataCompilationAuthority.resolve(world, tx, ty, world.tiles[tx][ty]);
            img = tileArtImage(d == null ? null : d.primaryArtKey, d == null ? null : d.primaryAssetId, world.tiles[tx][ty]);
        }
        drawDetailBox(g, preview, previewTitle, java.util.List.of(panelMode.name(), "Cursor " + tx + "," + ty), img);
        Rectangle linesBox = new Rectangle(preview.x + preview.width + 14, body.y, Math.max(260, body.x + body.width - preview.x - preview.width - 14), body.height);
        ArrayList<String> lines = tileStackAt(tx, ty);
        if (panelMode == PanelMode.INTERACT) lines.add("Action: confirm targets adjacent fixtures, NPCs, base objects, and doors.");
        if (panelMode == PanelMode.COMBAT) lines.add(lastTargetingReport == null || lastTargetingReport.isBlank() ? "No combat target report." : lastTargetingReport);
        if (lines.isEmpty()) lines.add("No target data available.");
        drawDetailBox(g, linesBox, "Inspection", lines, null);
        int by = linesBox.y + linesBox.height - 36;
        addOverlayButton(panelMode == PanelMode.COMBAT ? "Attack" : panelMode == PanelMode.INTERACT ? "Confirm" : "Inspect", linesBox.x + 12, by, 92, 28, "Run the current target action.", () -> {
            if (panelMode == PanelMode.INTERACT) confirmInteraction();
            else if (panelMode == PanelMode.COMBAT) confirmCombatTarget();
            else examineSelectedLookTarget();
        });
        if (panelMode == PanelMode.COMBAT) {
            addOverlayButton("Cycle Weapon", linesBox.x + 110, by, 126, 28, "Alternate the active attack hand.", this::cycleEquippedWeaponHand);
            addOverlayButton("Fire Mode", linesBox.x + 244, by, 96, 28, "Cycle snap/aimed/burst fire mode.", this::cycleFireMode);
        }
    }

    private void drawBuildOverlay(java.awt.Graphics2D g, Rectangle body) {
        Rectangle recipeList = new Rectangle(body.x, body.y, Math.max(260, body.width / 2), body.height);
        String category = ConstructionCategoryAuthority.categoryName(buildRecipeCategoryIndex);
        drawBox(g, recipeList, "Build Recipes / " + category);
        ArrayList<BuildRecipe> allRecipes = BuildRecipe.allBuildRecipes();
        ArrayList<BuildRecipe> recipes = ConstructionCategoryAuthority.filtered(allRecipes, buildRecipeCategoryIndex);
        int pageSize = 10;
        int pageCount = Math.max(1, (recipes.size() + pageSize - 1) / pageSize);
        buildRecipePage = Math.max(0, Math.min(pageCount - 1, buildRecipePage));
        int first = buildRecipePage * pageSize;
        int y = recipeList.y + 34;
        int shown = Math.min(pageSize, Math.max(0, recipes.size() - first));
        for (int i = 0; i < shown; i++) {
            BuildRecipe recipe = recipes.get(first + i);
            int rowY = y + i * 30;
            BufferedImage icon = images.getSemanticAssetImage(ObjectSemanticAssetAuthority.assetIdForBuildRecipe(recipe));
            ButtonBox b = new ButtonBox(recipe.name, recipeList.x + 10, rowY - 20, recipeList.width - 20, 26, recipe.shortTip(), () -> {
                pendingBuildRecipe = recipe;
                buildPlacementActive = true;
                buildX = playerX;
                buildY = playerY;
                panelMode = PanelMode.BUILD;
                logEvent("Selected build recipe: " + recipe.name + ".");
            }, icon);
            buttons.add(b);
        }
        Rectangle detail = new Rectangle(recipeList.x + recipeList.width + 14, body.y, Math.max(220, body.x + body.width - recipeList.x - recipeList.width - 14), body.height);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Category: " + category + " / " + recipes.size() + " blueprint(s); catalog total " + allRecipes.size() + ".");
        lines.add("Blueprint page " + (buildRecipePage + 1) + "/" + pageCount + "; showing "
                + (shown == 0 ? 0 : first + 1) + "-" + (first + shown) + ".");
        lines.addAll(ConstructionReadabilityAuthority.detailLines(this, pendingBuildRecipe, buildX, buildY));
        drawDetailBox(g, detail, "Construction", lines, pendingBuildRecipe == null ? null : images.getSemanticAssetImage(ObjectSemanticAssetAuthority.assetIdForBuildRecipe(pendingBuildRecipe)));
        int by = detail.y + detail.height - 36;
        addOverlayButton("Category", detail.x + 12, by, 96, 28, "Cycle to the next blueprint category.", () -> {
            buildRecipeCategoryIndex = ConstructionCategoryAuthority.nextCategory(buildRecipeCategoryIndex, 1);
            buildRecipePage = 0;
            repaint();
        });
        addOverlayButton("Previous", detail.x + 116, by, 96, 28, "Show the previous blueprint page.", () -> {
            buildRecipePage = Math.max(0, buildRecipePage - 1);
            repaint();
        });
        addOverlayButton("Next", detail.x + 220, by, 82, 28, "Show the next blueprint page.", () -> {
            buildRecipePage = Math.min(pageCount - 1, buildRecipePage + 1);
            repaint();
        });
    }

    private void drawCraftingOverlay(java.awt.Graphics2D g, Rectangle body) {
        Rectangle recipeList = new Rectangle(body.x, body.y, Math.max(280, body.width / 2), body.height);
        drawBox(g, recipeList, "Craft Recipes");
        ArrayList<CraftingRecipe> all = CraftingRecipe.all();
        ArrayList<CraftingRecipe> visible = new ArrayList<>();
        for (CraftingRecipe recipe : all) if (recipe.visibleTo(this)) visible.add(recipe);
        if (visible.isEmpty()) visible.add(CraftingRecipe.noKnownRecipes());
        selectedCraftingRecipe = visibleCraftingRecipeMatching(visible, selectedCraftingRecipe);
        int shown = Math.min(10, visible.size());
        int y = recipeList.y + 34;
        for (int i = 0; i < shown; i++) {
            CraftingRecipe recipe = visible.get(i);
            int rowY = y + i * 30;
            BufferedImage icon = images.getItemIcon(recipe.outputBaseItem);
            ButtonBox b = new ButtonBox(recipe.name, recipeList.x + 10, rowY - 20, recipeList.width - 20, 26,
                    recipe.shortStatus(this), () -> {
                        selectedCraftingRecipe = recipe;
                        panelMode = PanelMode.CRAFTING;
                        logEvent("Selected craft recipe: " + recipe.name + ".");
                    }, icon);
            buttons.add(b);
        }

        Rectangle detail = new Rectangle(recipeList.x + recipeList.width + 14, body.y, Math.max(240, body.x + body.width - recipeList.x - recipeList.width - 14), body.height);
        CraftingRecipe recipe = selectedCraftingRecipe;
        ArrayList<String> lines = new ArrayList<>();
        if (recipe == null) {
            lines.add("No crafting recipe selected.");
        } else {
            lines.addAll(ProductionReadabilityAuthority.detailLines(this, recipe));
        }
        drawDetailBox(g, detail, "Crafting", lines, recipe == null ? null : images.getItemIcon(recipe.outputBaseItem));
        int by = detail.y + detail.height - 36;
        addOverlayButton("Craft", detail.x + 12, by, 92, 28, "Craft the selected recipe if inputs and machine are valid.", this::craftSelectedRecipe);
        addOverlayButton("Build", detail.x + 112, by, 86, 28, "Open construction recipes.", () -> openPanel(PanelMode.BUILD));
        addOverlayButton("Production Info", detail.x + 206, by, 112, 28, "Open the Production Forecast mechanic reference.",
                () -> InfopediaHotLinkAuthority.openMechanic(this, "production-forecast", "crafting production reference"));
        addOverlayButton("Teach Machine", detail.x + 326, by, 112, 28, "Install the selected recipe doctrine in its required machine.",
                this::teachSelectedRecipeToMachine);
    }

    private void drawAuspexOverlay(java.awt.Graphics2D g, Rectangle body) {
        Rectangle left = new Rectangle(body.x, body.y, Math.max(280, body.width / 2), body.height);
        ArrayList<String> scan = new ArrayList<>();
        scan.add(stateSummary());
        scan.add("World: " + (world == null ? "none" : world.zoneType.label + " / " + world.zoneCoordText()));
        scan.add(world == null ? "No compiled tile descriptors available." : world.compiledTileDescriptorSummary);
        scan.add(lastSensoryModelReport);
        scan.add(world == null ? "No sensory model available." : world.lightNoiseSummary);
        scan.add(world == null ? "No hearing field available." : world.hearingFieldSummary);
        scan.add(world == null ? "No hazard overlay available." : world.hazardVisibilitySummary);
        scan.add(world == null ? "No trap interaction metadata available." : world.trapInteractionSummary);
        drawDetailBox(g, left, "Local Readout", scan, null);

        Rectangle right = new Rectangle(left.x + left.width + 14, body.y, Math.max(240, body.x + body.width - left.x - left.width - 14), body.height);
        ArrayList<String> signals = new ArrayList<>();
        signals.add("NPCs in slice: " + (world == null || world.npcs == null ? 0 : world.npcs.size()));
        signals.add("Map objects in slice: " + (world == null || world.mapObjects == null ? 0 : world.mapObjects.size()));
        signals.add("Light sources: " + (world == null || world.lightSources == null ? 0 : world.lightSources.size()));
        signals.add("Noise sources: " + (world == null || world.noiseSources == null ? 0 : world.noiseSources.size()));
        signals.add("Hazard warnings: " + (world == null || world.hazardWarnings == null ? 0 : world.hazardWarnings.size()));
        signals.add("Current target: " + lastTargetingReport);
        signals.addAll(ExpansionHeatReadabilityAuthority.summary(suspicion, gangHeat, baseObjects));
        for (MapObjectState target : nearbyScavengeTargets(5)) {
            signals.add("Nearby searchable: " + WasteNewsprintScavengeAuthority.shortLabel(target) + " at " + target.x + "," + target.y);
            if (signals.size() >= 11) break;
        }
        drawDetailBox(g, right, "Signals", signals, null);
        int by = right.y + right.height - 36;
        addOverlayButton("Refresh", right.x + 12, by, 92, 28, "Refresh sensory metadata.", () -> { updateSensoryModel("auspex panel"); logEvent("Auspex sensory model refresh requested."); repaint(); });
        addOverlayButton("Map", right.x + 110, by, 72, 28, "Open world map.", () -> openPanel(PanelMode.MAP));
        addOverlayButton("Scav", right.x + 190, by, 80, 28, "Open nearby scavenge search.", this::openScavengePanel);
        addOverlayButton("Heat Info", left.x + 12, left.y + left.height - 36, 92, 28,
                "Open the Expansion Heat mechanic reference.",
                () -> InfopediaHotLinkAuthority.openMechanic(this, "expansion-heat", "auspex heat reference"));
    }

    private void drawScavengeOverlay(java.awt.Graphics2D g, Rectangle body) {
        Rectangle left = new Rectangle(body.x, body.y, Math.max(280, body.width / 2), body.height);
        ArrayList<MapObjectState> targets = nearbyScavengeTargets(6);
        ArrayList<String> rows = new ArrayList<>();
        if (targets.isEmpty()) {
            rows.add("No searchable public refuse/newsprint fixtures within local range.");
            rows.add("Use Look or Interact to inspect adjacent fixtures directly.");
        } else {
            for (MapObjectState target : targets) {
                int d = Math.abs(target.x - playerX) + Math.abs(target.y - playerY);
                rows.add(WasteNewsprintScavengeAuthority.shortLabel(target) + " | " + target.x + "," + target.y + " | d" + d + " | chance " + WasteNewsprintScavengeAuthority.searchChance(this, target) + "%");
                if (rows.size() >= 12) break;
            }
        }
        drawDetailBox(g, left, "Nearby Searchables", rows, targets.isEmpty() ? null : images.getMapObjectImage(targets.get(0)));

        Rectangle right = new Rectangle(left.x + left.width + 14, body.y, Math.max(240, body.x + body.width - left.x - left.width - 14), body.height);
        MapObjectState nearest = nearestScavengeTarget(1);
        ArrayList<String> detail = new ArrayList<>();
        detail.add("Inventory load " + inventoryWeight() + "/" + carryCapacity() + ".");
        detail.add("Adjacent target: " + (nearest == null ? "none" : WasteNewsprintScavengeAuthority.shortLabel(nearest) + " at " + nearest.x + "," + nearest.y));
        detail.add("Search action reuses the live frontage fixture scavenge system.");
        detail.add("Cooldown and provenance are preserved by the underlying interaction authority.");
        if (!targets.isEmpty() && nearest == null) detail.add("Move closer to search: nearest is range " + (Math.abs(targets.get(0).x - playerX) + Math.abs(targets.get(0).y - playerY)) + ".");
        drawDetailBox(g, right, "Search", detail, nearest == null ? null : images.getMapObjectImage(nearest));
        int by = right.y + right.height - 36;
        addOverlayButton("Search", right.x + 12, by, 92, 28, "Search the nearest adjacent scavenge fixture.", this::searchNearestScavengeTarget);
        addOverlayButton("Look", right.x + 110, by, 72, 28, "Open look targeting.", this::beginLookMode);
        addOverlayButton("Use", right.x + 190, by, 72, 28, "Open interact targeting.", this::beginInteractMode);
    }

    private void drawInfoPauseOverlay(java.awt.Graphics2D g, Rectangle body) {
        Rectangle left = new Rectangle(body.x, body.y, Math.max(240, body.width / 2), body.height);
        ArrayList<String> lines = new ArrayList<>();
        lines.add(stateSummary());
        lines.add("World: " + (world == null ? "none" : world.zoneCoordText()));
        lines.add("Setup: " + (worldSetup == null ? "standard" : worldSetup.shortSummary()));
        lines.add("Window authority: " + universalWindowAuthority.auditSummary());
        lines.add(MenuDefinitionAuditAuthority.summary(universalWindowAuthority));
        lines.add(InputRebindingAuditAuthority.playerFacingSummary());
        lines.add("Management windows: " + UniversalManagementWindowAuthority.summary());
        lines.addAll(MovementDebugOverlayAuthority.overlayLines(this));
        int start = Math.max(0, eventLog.size() - 14);
        for (int i = start; i < eventLog.size(); i++) lines.add(eventLog.get(i));
        drawDetailBox(g, left, screen == Screen.PAUSE ? "Session" : "Event Log", lines, null);
        Rectangle right = new Rectangle(left.x + left.width + 14, body.y, Math.max(220, body.x + body.width - left.x - left.width - 14), body.height);
        drawDetailBox(g, right, "Sorted Commands", java.util.List.of(
                "World: WASD/arrows move, . waits, R cycles movement, P starts manual path planning.",
                "Targeting: L looks, E interacts, F targets combat, G targets explosives, X reloads.",
                "Panels: I inventory, C character, M map, B build, F5 crafting, F6 scavenge, Esc pause/resume.",
                "Utility: F1 opens info/tactical slate, F2/Y opens chat/console, F4 opens Auspex, options live under pause.",
                "Mouse: command bar buttons now route to the same live actions as the keys."
        ), null);
        int y = Math.max(right.y + 126, right.y + right.height - 190);
        addOverlayButton("Resume", right.x + 12, y, 120, 30, "Return to game.", () -> setScreen(Screen.GAME)); y += 38;
        addOverlayButton("Menus", right.x + right.width - 88, y - 38, 76, 30, "Open the structured menu definition audit.",
                () -> InfopediaHotLinkAuthority.openMechanic(this, "menu-uniformity", "tactical slate menu audit"));
        addOverlayButton(PauseMovementRecoveryAuthority.BUTTON_LABEL, right.x + 12, y, 120, 30,
                PauseMovementRecoveryAuthority.BUTTON_TIP, () -> PauseMovementRecoveryAuthority.recoverFromPause(this)); y += 38;
        addOverlayButton("Move", right.x + right.width - 88, y - 38, 76, 30, "Open the movement planning definition audit.",
                () -> InfopediaHotLinkAuthority.openMechanic(this, "movement-planning", "tactical slate movement audit"));
        addOverlayButton("Save / Load", right.x + 12, y, 140, 30, "Open save/load panel.", () -> openSaveLoadPanel("Pause menu opened save/load.")); y += 38;
        addOverlayButton("Input", right.x + right.width - 68, y - 38, 56, 30, "Open the live input rebinding audit.",
                () -> InfopediaHotLinkAuthority.openMechanic(this, "input-rebinding-audit", "tactical slate input audit"));
        addOverlayButton("Options", right.x + 12, y, 120, 30, "Open options.", () -> openOptionsScreen("pause menu")); y += 38;
        addOverlayButton("Main Menu", right.x + 12, y, 140, 30, "Return to main menu.", () -> setScreen(Screen.MENU));
    }

    private void drawInfopediaOverlay(java.awt.Graphics2D g, Rectangle body) {
        mechanist.assets.AssetType selectedType = selectedInfopediaAssetType();
        java.util.List<String> entries = currentInfopediaEntries(selectedType);

        g.setFont(smallFont);
        Rectangle controls = new Rectangle(body.x, body.y, body.width, infopediaControlsHeight(g.getFontMetrics(), body.width));
        drawBox(g, controls, "InfoPedia Categories");
        int tabsBottom = addInfopediaTabButtons(g, controls);
        g.setFont(smallFont);
        g.setColor(new java.awt.Color(205, 210, 195));
        String filter = infopediaAssetFilter == null || infopediaAssetFilter.isBlank() ? "<none>" : infopediaAssetFilter;
        String focus = "infopedia-asset-filter".equals(activeScrollTag) ? " / typing filter" : "";
        int filterY = Math.min(controls.y + controls.height - 14, tabsBottom + 26);
        int bx = Math.max(controls.x + controls.width - 136, controls.x + 12);
        drawInfopediaTextLine(g, GuiLayoutApi.fitLabel("Selected: " + SemanticAssetInfopediaAuthority.typeLabel(selectedType) + " / Filter: " + filter + focus, g.getFontMetrics(), Math.max(80, bx - controls.x - 24)), controls.x + 12, filterY, Math.max(80, bx - controls.x - 24), new java.awt.Color(205, 210, 195), false);
        addOverlayButton("Filter", bx, filterY - 22, 64, 28, "Focus InfoPedia search filter.", () -> { activeScrollTag = "infopedia-asset-filter"; repaint(); });
        addOverlayButton("Clear", bx + 72, filterY - 22, 58, 28, "Clear InfoPedia search filter.", () -> { infopediaAssetFilter = ""; infopediaSelectionIndex = 0; infopediaListScroll = 0; infopediaDetailScroll = 0; activeScrollTag = "infopedia-list"; repaint(); });

        int gap = 14;
        int contentTop = controls.y + controls.height + 12;
        int contentH = Math.max(120, body.y + body.height - contentTop);
        int rightW = Math.max(330, Math.min(520, body.width / 3));
        Rectangle rightColumn = new Rectangle(body.x + body.width - rightW, contentTop, rightW, contentH);
        int previewH = Math.max(156, Math.min(250, rightColumn.height / 3));
        Rectangle preview = new Rectangle(rightColumn.x, rightColumn.y + rightColumn.height - previewH, rightColumn.width, previewH);
        Rectangle list = new Rectangle(rightColumn.x, rightColumn.y, rightColumn.width, Math.max(120, preview.y - rightColumn.y - 12));
        Rectangle detail = new Rectangle(body.x, contentTop, Math.max(180, rightColumn.x - body.x - gap), contentH);
        syncInfopediaViewport(entries, list.height);
        int maxRows = infopediaMaxRows(list.height);
        int start = Math.max(0, Math.min(infopediaListScroll, Math.max(0, entries.size() - 1)));
        int end = Math.min(entries.size(), start + maxRows);
        java.util.List<String> visible = entries.isEmpty() ? java.util.List.of("No InfoPedia entries are available.") : new ArrayList<>(entries.subList(start, end));

        String selected = entries.isEmpty() ? "" : entries.get(Math.max(0, Math.min(infopediaSelectionIndex, entries.size() - 1)));
        java.util.List<String> detailLines = SemanticAssetInfopediaAuthority.detailLines(mechanist.assets.AssetManager.registry(), selected, selectedType, infopediaAssetFilter);
        java.util.List<String> scrolled = infopediaDetailScroll <= 0 ? detailLines : detailLines.subList(Math.min(infopediaDetailScroll, detailLines.size()), detailLines.size());
        String assetId = SemanticAssetInfopediaAuthority.assetIdFromEntry(selected).orElse("");
        BufferedImage icon = assetId.isBlank() ? null : images.getSemanticAssetImage(assetId);
        drawInfopediaDetailBox(g, detail, "Entry Detail", scrolled);
        drawInfopediaListBox(g, list, "Assets In Current Tab", visible, Math.max(0, infopediaSelectionIndex - start), true);
        drawInfopediaIconPreview(g, preview, detailLines, icon);
        if (SemanticAssetInfopediaAuthority.firstRelatedRowForEntry(mechanist.assets.AssetManager.registry(), selected, selectedType).isPresent()) {
            addOverlayButton("Related", preview.x + 10, preview.y + preview.height - 36, 86, 28, "Open the first related InfoPedia entry.", () -> openFirstRelatedInfopediaEntry(selected, selectedType));
        }
    }

    private void openFirstRelatedInfopediaEntry(String selected, mechanist.assets.AssetType selectedType) {
        java.util.Optional<String> related = SemanticAssetInfopediaAuthority.firstRelatedRowForEntry(mechanist.assets.AssetManager.registry(), selected, selectedType);
        if (related.isEmpty()) return;
        String target = related.get();
        infopediaAssetFilter = "";
        if (target.startsWith("MECHANIC - ")) infopediaTab = 0;
        java.util.List<String> entries = currentInfopediaEntries(selectedInfopediaAssetType());
        for (int i = 0; i < entries.size(); i++) {
            if (target.equals(entries.get(i))) {
                infopediaSelectionIndex = i;
                infopediaDetailScroll = 0;
                infopediaListScroll = Math.max(0, i - 2);
                activeScrollTag = "infopedia-list";
                repaint();
                return;
            }
        }
        logEvent("Related InfoPedia entry is not visible in the current category.");
        repaint();
    }

    private int infopediaControlsHeight(java.awt.FontMetrics fm, int width) {
        int rows = infopediaTabRows(fm, width);
        return 34 + rows * INFOPEDIA_TAB_ROW_HEIGHT + Math.max(0, rows - 1) * INFOPEDIA_TAB_GAP + 42;
    }

    private int infopediaTabRows(java.awt.FontMetrics fm, int width) {
        int usable = Math.max(120, width - 24);
        int x = 0;
        int rows = 1;
        for (String label : infopediaTabLabels()) {
            int tabW = infopediaTabWidth(fm, label);
            if (x > 0 && x + tabW > usable) {
                rows++;
                x = 0;
            }
            x += tabW + INFOPEDIA_TAB_GAP;
        }
        return rows;
    }

    private java.util.List<String> infopediaTabLabels() {
        java.util.ArrayList<String> labels = new java.util.ArrayList<>();
        labels.add("All");
        for (mechanist.assets.AssetType type : SemanticAssetInfopediaAuthority.browseTypes()) labels.add(SemanticAssetInfopediaAuthority.typeLabel(type));
        return labels;
    }

    private int infopediaTabWidth(java.awt.FontMetrics fm, String label) {
        return Math.max(64, Math.min(140, fm.stringWidth(label == null ? "" : label) + 24));
    }

    private int addInfopediaTabButtons(java.awt.Graphics2D g, Rectangle controls) {
        g.setFont(smallFont);
        java.awt.FontMetrics fm = g.getFontMetrics();
        java.util.List<String> labels = infopediaTabLabels();
        int x = controls.x + 12;
        int y = controls.y + 34;
        int startX = x;
        int maxX = controls.x + controls.width - 12;
        for (int i = 0; i < labels.size(); i++) {
            String label = labels.get(i);
            int tabW = infopediaTabWidth(fm, label);
            if (x > startX && x + tabW > maxX) {
                x = startX;
                y += INFOPEDIA_TAB_ROW_HEIGHT + INFOPEDIA_TAB_GAP;
            }
            final int tab = i;
            boolean selected = tab == Math.max(0, Math.min(infopediaTab, labels.size() - 1));
            String drawLabel = (selected ? "> " : "") + GuiLayoutApi.fitLabel(label, fm, Math.max(24, tabW - 18));
            addOverlayButton(drawLabel, x, y, tabW, INFOPEDIA_TAB_ROW_HEIGHT, "Show InfoPedia category: " + label + ".", () -> setInfopediaTab(tab));
            x += tabW + INFOPEDIA_TAB_GAP;
        }
        return y + INFOPEDIA_TAB_ROW_HEIGHT;
    }

    private int infopediaMaxRows(int listHeight) {
        return Math.max(1, (Math.max(80, listHeight) - 42) / INFOPEDIA_ROW_HEIGHT);
    }

    private void drawInfopediaListBox(java.awt.Graphics2D g, Rectangle r, String title, java.util.List<String> rows, int selected, boolean activeColumn) {
        drawBox(g, r, title);
        g.setFont(smallFont);
        java.awt.FontMetrics fm = g.getFontMetrics();
        int y = r.y + 34;
        if (rows == null || rows.isEmpty()) {
            drawInfopediaTextLine(g, "Empty", r.x + 12, y, r.width - 24, new java.awt.Color(145, 148, 132), false);
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (y > r.y + r.height - Math.max(8, fm.getDescent())) break;
            boolean rowSelected = activeColumn && i == selected;
            if (rowSelected) {
                g.setColor(new java.awt.Color(55, 48, 32, 210));
                g.fillRoundRect(r.x + 8, y - fm.getAscent() - 4, r.width - 16, Math.max(14, fm.getHeight() + 6), 6, 6);
            }
            String text = GuiLayoutApi.fitLabel((rowSelected ? "> " : "  ") + rows.get(i), fm, r.width - 24);
            drawInfopediaTextLine(g, text, r.x + 12, y, r.width - 24, rowSelected ? new java.awt.Color(245, 220, 140) : new java.awt.Color(205, 210, 195), rowSelected);
            y += INFOPEDIA_ROW_HEIGHT;
        }
    }

    private void drawInfopediaDetailBox(java.awt.Graphics2D g, Rectangle r, String title, java.util.List<String> lines) {
        drawBox(g, r, title);
        int x = r.x + 12;
        int y = r.y + 34;
        int textW = Math.max(120, r.width - 24);
        g.setFont(smallFont);
        java.awt.FontMetrics fm = g.getFontMetrics();
        java.awt.Shape oldClip = g.getClip();
        g.setClip(r.x + 8, r.y + 28, Math.max(1, r.width - 16), Math.max(1, r.height - 36));
        if (lines != null) {
            for (String line : lines) {
                if (y > r.y + r.height - Math.max(8, fm.getDescent())) break;
                if (line == null || line.isBlank()) {
                    y += Math.max(8, INFOPEDIA_ROW_HEIGHT / 2);
                    continue;
                }
                for (String wrapped : GuiLayoutApi.wrapText(line, fm, textW)) {
                    if (y > r.y + r.height - Math.max(8, fm.getDescent())) break;
                    drawInfopediaTextLine(g, wrapped, x, y, textW, new java.awt.Color(205, 210, 195), false);
                    y += INFOPEDIA_ROW_HEIGHT;
                }
            }
        }
        g.setClip(oldClip);
    }

    private void drawInfopediaIconPreview(java.awt.Graphics2D g, Rectangle r, java.util.List<String> lines, BufferedImage icon) {
        g.setColor(new java.awt.Color(8, 10, 9, 226));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(new java.awt.Color(145, 118, 64, 150));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setFont(smallFont.deriveFont(java.awt.Font.BOLD));
        drawInfopediaTextLine(g, "Visual Preview", r.x + 10, r.y + 21, r.width - 20, new java.awt.Color(245, 220, 140), true);
        int imageTop = r.y + 34;
        int imageH = Math.max(48, Math.min(r.width - 20, r.height - 96));
        int imageW = Math.max(48, r.width - 20);
        Rectangle box = new Rectangle(r.x + 10, imageTop, imageW, imageH);
        g.setColor(new java.awt.Color(0, 0, 0, 150));
        g.fillRect(box.x, box.y, box.width, box.height);
        g.setColor(new java.awt.Color(145, 118, 64, 120));
        g.drawRect(box.x, box.y, box.width, box.height);
        if (icon != null) {
            int drawW = Math.max(1, box.width - 12);
            int drawH = Math.max(1, icon.getHeight() * drawW / Math.max(1, icon.getWidth()));
            if (drawH > box.height - 12) {
                drawH = Math.max(1, box.height - 12);
                drawW = Math.max(1, icon.getWidth() * drawH / Math.max(1, icon.getHeight()));
            }
            int ix = box.x + (box.width - drawW) / 2;
            int iy = box.y + (box.height - drawH) / 2;
            Object oldHint = g.getRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION);
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(icon, ix, iy, drawW, drawH, null);
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, oldHint == null ? java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR : oldHint);
        } else {
            drawInfopediaTextLine(g, "No icon mapped", box.x + 8, box.y + box.height / 2 + 5, box.width - 16, new java.awt.Color(145, 148, 132), false);
        }
        int y = box.y + box.height + 20;
        if (lines != null) {
            int drawn = 0;
            for (String line : lines) {
                if (line == null || line.isBlank()) continue;
                drawInfopediaTextLine(g, line, r.x + 10, y, r.width - 20, drawn == 0 ? new java.awt.Color(245, 220, 140) : new java.awt.Color(205, 210, 195), drawn == 0);
                y += INFOPEDIA_ROW_HEIGHT;
                drawn++;
                if (drawn >= 3 || y > r.y + r.height - 8) break;
            }
        }
    }

    private void drawInfopediaTextLine(java.awt.Graphics2D g, String text, int x, int baseline, int maxWidth, java.awt.Color color, boolean strong) {
        if (g == null || text == null) return;
        java.awt.Font oldFont = g.getFont();
        if (strong) g.setFont(oldFont.deriveFont(java.awt.Font.BOLD));
        java.awt.FontMetrics fm = g.getFontMetrics();
        String fitted = GuiLayoutApi.fitLabel(text, fm, Math.max(8, maxWidth));
        java.awt.Color old = g.getColor();
        g.setColor(color == null ? new java.awt.Color(205, 210, 195) : color);
        g.drawString(fitted, x, baseline);
        g.setColor(old);
        g.setFont(oldFont);
    }

    private mechanist.assets.AssetType selectedInfopediaAssetType() {
        mechanist.assets.AssetType[] types = SemanticAssetInfopediaAuthority.browseTypes();
        if (infopediaTab <= 0 || types.length == 0) return null;
        return types[Math.floorMod(infopediaTab - 1, types.length)];
    }

    private java.util.List<String> currentInfopediaEntries(mechanist.assets.AssetType selectedType) {
        return SemanticAssetInfopediaAuthority.entries(mechanist.assets.AssetManager.registry(), selectedType, infopediaAssetFilter);
    }

    private void syncInfopediaViewport(java.util.List<String> entries, int listHeight) {
        int count = entries == null ? 0 : entries.size();
        if (count <= 0) {
            infopediaSelectionIndex = 0;
            infopediaListScroll = 0;
            infopediaDetailScroll = 0;
            return;
        }
        infopediaSelectionIndex = Math.max(0, Math.min(infopediaSelectionIndex, count - 1));
        int maxRows = infopediaMaxRows(listHeight);
        if (infopediaSelectionIndex < infopediaListScroll) infopediaListScroll = infopediaSelectionIndex;
        if (infopediaSelectionIndex >= infopediaListScroll + maxRows) infopediaListScroll = infopediaSelectionIndex - maxRows + 1;
        infopediaListScroll = Math.max(0, Math.min(infopediaListScroll, Math.max(0, count - maxRows)));
    }

    private void drawGenericOverlay(java.awt.Graphics2D g, Rectangle body) {
        drawDetailBox(g, body, panelTitle(), java.util.List.of("This panel is routed into the live game context.", "Position " + playerX + "," + playerY, "World " + (world == null ? "none" : world.zoneCoordText())), null);
    }

    private void drawDialogueOverlay(java.awt.Graphics2D g, Rectangle body) {
        NpcEntity npc = activeInteractionNpc;
        int gap = 14;
        Rectangle portrait = new Rectangle(body.x, body.y, Math.max(190, Math.min(250, body.width / 3)), body.height);
        drawDetailBox(g, portrait, npc == null ? "No Entity" : safeLabel(npc.name, "Entity"), npcInteractionLines(npc), npc == null ? null : images.getNpcPortraitFor(npc));
        Rectangle actions = new Rectangle(portrait.x + portrait.width + gap, body.y, Math.max(260, body.x + body.width - portrait.x - portrait.width - gap), body.height);
        ArrayList<String> lines = new ArrayList<>();
        if (npc == null) {
            lines.add("No active entity is attached to this conversation surface.");
        } else if (npc.isAnimalActor()) {
            lines.add(npc.isPetActor() ? "Pet interaction surface." : "Animal interaction surface.");
            lines.add(npc.animalLine());
            lines.add("Use the options below to handle animal-specific interaction instead of routing it through humanoid dialogue.");
        } else {
            lines.add("Conversation surface for " + safeLabel(npc.role, "local actor") + ".");
            lines.add("Faction: " + (npc.faction == null ? "No faction" : npc.faction.label) + ".");
            lines.add(npc.rankLine());
            Faction conversationFaction = npc.faction == null ? Faction.NONE : npc.faction;
            lines.addAll(ConversationReadabilityAuthority.describe(npc,
                    factionStanding.getOrDefault(conversationFaction, 0),
                    temporaryHostileTurns.getOrDefault(conversationFaction, 0)).lines());
        }
        drawDetailBox(g, actions, npc != null && npc.isAnimalActor() ? "Animal Options" : "Conversation Options", lines, null);
        int by = actions.y + actions.height - 36;
        if (npc != null && npc.isAnimalActor()) {
            PetInteractionFeedbackAuthority.PetInteractionReadout petReadout = PetInteractionFeedbackAuthority.describe(npc);
            addOverlayButton(petReadout.actionLabel(), actions.x + 12, by, 96, 28, petReadout.feedback(), this::petActiveAnimal);
            addOverlayButton("Look", actions.x + 116, by, 72, 28, "Inspect this entity in look mode.", this::lookAtActiveInteractionTarget);
            addOverlayButton("Approach", actions.x + 196, by, 92, 28, "Plan movement to a reachable adjacent tile.", () -> approachActiveInteractionTarget(npc.x, npc.y, npc.name));
        } else {
            addOverlayButton("Talk", actions.x + 12, by, 82, 28, "Talk to this entity.", this::talkToActiveNpc);
            if (npc != null && npc.isTrader()) addOverlayButton("Trade", actions.x + 102, by, 92, 28, "Open this trader's store panel.", () -> openTradeForNpc(npc));
            addOverlayButton("Look", actions.x + (npc != null && npc.isTrader() ? 202 : 102), by, 72, 28, "Inspect this entity in look mode.", this::lookAtActiveInteractionTarget);
            if (npc != null) addOverlayButton("Approach", actions.x + (npc.isTrader() ? 282 : 182), by, 92, 28,
                    "Plan movement to a reachable adjacent tile.", () -> approachActiveInteractionTarget(npc.x, npc.y, npc.name));
        }
    }

    private void drawTradeOverlay(java.awt.Graphics2D g, Rectangle body) {
        TraderSession t = activeTraderSession;
        int gap = 14;
        int offerW = Math.max(300, Math.min(440, body.width / 2));
        Rectangle offers = new Rectangle(body.x, body.y, offerW, body.height);
        Rectangle right = new Rectangle(offers.x + offers.width + gap, body.y, Math.max(260, body.x + body.width - offers.x - offers.width - gap), body.height);
        drawBox(g, offers, t == null ? "Store" : safeLabel(t.name, "Store"));
        g.setFont(smallFont);
        java.awt.FontMetrics fm = g.getFontMetrics();
        int rowH = 30;
        int shown = Math.max(1, (offers.height - 48) / rowH);
        int count = t == null || t.offers == null ? 0 : t.offers.size();
        selectedTradeOfferIndex = count <= 0 ? 0 : Math.max(0, Math.min(selectedTradeOfferIndex, count - 1));
        int start = count <= shown ? 0 : Math.max(0, Math.min(selectedTradeOfferIndex - shown + 1, count - shown));
        int y = offers.y + 34;
        if (count <= 0) {
            g.setColor(new java.awt.Color(145, 148, 132));
            drawUiTextLine(g, "No accessible stock.", offers.x + 12, y);
        } else {
            for (int i = start; i < Math.min(count, start + shown); i++) {
                TradeOffer offer = t.offers.get(i);
                int rowY = y + (i - start) * rowH;
                int price = t.buyPrice(offer);
                int idx = i;
                String label = (idx == selectedTradeOfferIndex ? "> " : "") + offer.name + " / " + price + " script";
                buttons.add(new ButtonBox(GuiLayoutApi.fitLabel(label, fm, offers.width - 28), offers.x + 10, rowY - 20, offers.width - 20, 26,
                        "Select " + offer.name + ".", () -> { selectedTradeOfferIndex = idx; repaint(); }, images.getItemIcon(offer.name)));
            }
        }

        TradeOffer selected = selectedTradeOffer();
        ArrayList<String> detail = new ArrayList<>();
        detail.add("Carried script " + carriedScript + " / inventory " + inventoryWeight() + "/" + carryCapacity() + ".");
        if (t != null) {
            detail.add("Archetype: " + safeLabel(t.archetype, "store") + " / " + safeLabel(t.zoneLabel, world == null ? "unknown zone" : world.zoneType.label) + ".");
            Faction marketFaction = activeInteractionNpc == null ? FactionInventoryStockAuthority.factionForZone(world == null ? null : world.zoneType) : activeInteractionNpc.faction;
            detail.addAll(TradeReadabilityAuthority.marketContext(t,
                    marketFaction == null ? "local independent trade" : marketFaction.label,
                    marketFaction == null ? 0 : factionStanding.getOrDefault(marketFaction, 0)));
        }
        detail.addAll(TradeReadabilityAuthority.offerPreview(t, selected, carriedScript, inventoryWeight(), carryCapacity()));
        if (selected != null) detail.add(selected.displayLine(t.buyPrice(selected)));
        String carried = selectedInventoryItem();
        ItemProvenanceRecord carriedProvenance = peekProvenanceForItem(carried);
        detail.addAll(TradeReadabilityAuthority.salePreview(carried,
                t == null || carried == null ? 0 : t.sellPrice(carried), carriedProvenance));
        drawDetailBox(g, right, "Offer Detail", detail, selected == null ? null : images.getItemIcon(selected.name));
        int by = right.y + right.height - 36;
        addOverlayButton("Buy", right.x + 12, by, 72, 28, "Buy the selected offer.", this::buySelectedTradeOffer);
        addOverlayButton("Sell", right.x + 92, by, 72, 28, "Sell the selected carried item.", this::sellSelectedInventoryItemToTrader);
        addOverlayButton("Inventory", right.x + 172, by, 112, 28, "Open carried inventory.", () -> openPanel(PanelMode.INVENTORY));
    }

    private void drawContainerOverlay(java.awt.Graphics2D g, Rectangle body) {
        ensureActiveContainerReady();
        int gap = 14;
        int leftW = Math.max(280, Math.min(420, body.width / 2));
        Rectangle container = new Rectangle(body.x, body.y, leftW, body.height);
        Rectangle detail = new Rectangle(container.x + container.width + gap, body.y, Math.max(260, body.x + body.width - container.x - container.width - gap), body.height);
        ContainerRecord c = itemContainers.get(activeInteractionContainerId);
        drawBox(g, container, c == null ? "Container" : safeLabel(c.label, "Container"));
        java.util.List<ItemInstance> items = activeContainerItems();
        g.setFont(smallFont);
        java.awt.FontMetrics fm = g.getFontMetrics();
        int rowH = 30;
        int shown = Math.max(1, (container.height - 48) / rowH);
        selectedContainerItemIndex = items.isEmpty() ? 0 : Math.max(0, Math.min(selectedContainerItemIndex, items.size() - 1));
        int start = items.size() <= shown ? 0 : Math.max(0, Math.min(selectedContainerItemIndex - shown + 1, items.size() - shown));
        int y = container.y + 34;
        if (items.isEmpty()) {
            g.setColor(new java.awt.Color(145, 148, 132));
            drawUiTextLine(g, "Empty", container.x + 12, y);
        } else {
            for (int i = start; i < Math.min(items.size(), start + shown); i++) {
                ItemInstance inst = items.get(i);
                int rowY = y + (i - start) * rowH;
                int idx = i;
                String label = (idx == selectedContainerItemIndex ? "> " : "") + inst.displayName;
                buttons.add(new ButtonBox(GuiLayoutApi.fitLabel(label, fm, container.width - 28), container.x + 10, rowY - 20, container.width - 20, 26,
                        "Select " + inst.displayName + ".", () -> { selectedContainerItemIndex = idx; repaint(); }, images.getItemIcon(inst.displayName)));
            }
        }
        ItemInstance selected = selectedContainerItem();
        ArrayList<String> lines = new ArrayList<>();
        String carried = selectedInventoryItem();
        lines.addAll(ContainerReadabilityAuthority.transferPreview(
                safeLabel(activeInteractionTitle, c == null ? "container" : c.label),
                items.size(), selected, carried, inventoryWeight(), carryCapacity()));
        drawDetailBox(g, detail, "Transfer", lines, selected == null ? interactionObjectImage() : images.getItemIcon(selected.displayName));
        int by = detail.y + detail.height - 36;
        addOverlayButton("Take", detail.x + 12, by, 74, 28, "Take the selected container item.", this::takeSelectedContainerItem);
        addOverlayButton("Put", detail.x + 94, by, 64, 28, "Put the selected carried item into this container.", this::putSelectedInventoryItemIntoContainer);
        addOverlayButton("Inventory", detail.x + 166, by, 112, 28, "Open carried inventory.", () -> openPanel(PanelMode.INVENTORY));
    }

    private void drawObjectInteractionOverlay(java.awt.Graphics2D g, Rectangle body) {
        MapObjectState obj = activeInteractionObject;
        BaseObject base = activeInteractionBaseObject;
        int gap = 14;
        Rectangle preview = new Rectangle(body.x, body.y, Math.max(220, Math.min(300, body.width / 3)), body.height);
        drawDetailBox(g, preview, activeInteractionTitleOrDefault(), objectInteractionLines(), interactionObjectImage());
        Rectangle actions = new Rectangle(preview.x + preview.width + gap, body.y, Math.max(260, body.x + body.width - preview.x - preview.width - gap), body.height);
        ArrayList<String> lines = new ArrayList<>();
        lines.add("Interaction type: " + safeLabel(activeInteractionKind, "object") + ".");
        if (obj != null) {
            lines.add("Map object at " + obj.x + "," + obj.y + " / " + safeLabel(obj.type, "object") + ".");
            lines.add("Stock/state: " + safeLabel(obj.stockState, "none") + ".");
        }
        if (base != null) {
            lines.add("Base object at " + base.x + "," + base.y + " / " + safeLabel(base.qualityName, "Common") + ".");
            lines.add(base.businessReturnLine(this));
            lines.addAll(MachineRepairAuthority.detailLines(base, machineParts));
        }
        lines.add("Use the action buttons below; this target is no longer routed to the dead generic placeholder.");
        drawDetailBox(g, actions, "Actions", lines, null);
        int by = actions.y + actions.height - 36;
        int x = actions.x + 12;
        if (obj != null && isTradeObject(obj)) {
            addOverlayButton("Trade", x, by, 84, 28, "Open this store or vending stock.", () -> openTradeForObject(obj)); x += 92;
        }
        if (obj != null && isContainerObject(obj)) {
            addOverlayButton("Open", x, by, 74, 28, "Open this object's container.", () -> openContainerForObject(obj)); x += 82;
        }
        if (base != null || (obj != null && isMachineObject(obj))) {
            addOverlayButton("Operate", x, by, 94, 28, "Operate or inspect this machine.", this::operateActiveInteractionObject); x += 102;
            addOverlayButton("Craft", x, by, 78, 28, "Open crafting/workbench recipes.", () -> openPanel(PanelMode.CRAFTING)); x += 86;
            if (base != null) {
                addOverlayButton("Repair", x, by, 82, 28, "Spend one machine part to restore this owned machine toward serviceable condition.",
                        this::repairActiveBaseMachine); x += 90;
            }
        } else {
            addOverlayButton("Use", x, by, 70, 28, "Use or inspect this object.", this::operateActiveInteractionObject); x += 78;
        }
        addOverlayButton("Look", x, by, 72, 28, "Inspect this target in look mode.", this::lookAtActiveInteractionTarget);
        int targetX = obj != null ? obj.x : base == null ? playerX : base.x;
        int targetY = obj != null ? obj.y : base == null ? playerY : base.y;
        addOverlayButton("Approach", x + 80, by, 92, 28, "Plan movement to a reachable adjacent tile.",
                () -> approachActiveInteractionTarget(targetX, targetY, activeInteractionTitleOrDefault()));
    }

    private ArrayList<String> npcInteractionLines(NpcEntity npc) {
        return new ArrayList<>(EntityIdentityReadabilityAuthority.summary(npc));
    }

    private ArrayList<String> objectInteractionLines() {
        ArrayList<String> lines = new ArrayList<>();
        MapObjectState obj = activeInteractionObject;
        BaseObject base = activeInteractionBaseObject;
        if (obj != null) {
            lines.add(safeLabel(obj.label, "Map object") + ".");
            lines.add("Type: " + safeLabel(obj.type, "object") + " / glyph " + obj.glyph + ".");
            lines.add("Uses: " + obj.vendCount + " / cooldown until turn " + obj.cooldownUntilTurn + ".");
            lines.add("State: " + safeLabel(obj.stockState, "none") + ".");
        }
        if (base != null) {
            lines.add(safeLabel(base.name, "Base object") + " / " + safeLabel(base.description, "built base object") + ".");
            lines.add("Quality: " + safeLabel(base.qualityName, "Common") + " / integrity " + base.integrity + " / capacity " + base.capacity + ".");
            lines.add("Business: " + (base.isBusinessAsset() ? base.businessName() : "not a public business surface") + ".");
        }
        if (lines.isEmpty()) lines.add("No active object target.");
        return lines;
    }

    private BufferedImage interactionObjectImage() {
        if (activeInteractionObject != null) return images.getMapObjectImage(activeInteractionObject);
        if (activeInteractionBaseObject != null) return images.getBaseObjectImage(activeInteractionBaseObject);
        return null;
    }

    private String activeInteractionTitleOrDefault() {
        if (activeInteractionTitle != null && !activeInteractionTitle.isBlank()) return activeInteractionTitle;
        if (activeInteractionObject != null) return safeLabel(activeInteractionObject.label, "Object");
        if (activeInteractionBaseObject != null) return safeLabel(activeInteractionBaseObject.name, "Base object");
        return "Interaction";
    }

    private void clearActiveInteractionState() {
        activeInteractionTitle = "";
        activeInteractionKind = "";
        activeInteractionContainerId = "";
        activeInteractionNpc = null;
        activeInteractionObject = null;
        activeInteractionBaseObject = null;
        activeTraderSession = null;
        selectedTradeOfferIndex = 0;
        selectedContainerItemIndex = 0;
    }

    private void repairActiveBaseMachine() {
        BaseObject machine = activeInteractionBaseObject;
        MachineRepairAuthority.RepairPreview preview = MachineRepairAuthority.preview(machine, machineParts);
        if (!preview.available()) {
            logEvent(preview.summary());
            repaint();
            return;
        }
        machineParts = Math.max(0, machineParts - preview.partCost());
        machine.integrity = preview.projectedIntegrity();
        logEvent("Repaired " + safeLabel(machine.name, "machine") + ". " + preview.summary()
                + " Machine parts remaining " + machineParts + ".");
        advanceTurn("repairs " + safeLabel(machine.name, "a machine") + ".");
    }

    private void teachSelectedRecipeToMachine() {
        CraftingRecipe recipe = selectedCraftingRecipe;
        if (recipe == null) {
            logEvent("Machine teaching failed: no crafting recipe selected.");
            repaint();
            return;
        }
        BaseObject machine = recipe.requiredMachine(this);
        String result = ProductionKnowledgeSourceAuthority.install(this, machine, recipe.requiredKnowledge);
        logEvent(result);
        repaint();
    }

    private void openDialogueForNpc(NpcEntity npc) {
        clearActiveInteractionState();
        activeInteractionNpc = npc;
        activeInteractionTitle = npc == null ? "Conversation" : safeLabel(npc.name, "Entity");
        activeInteractionKind = npc != null && npc.isAnimalActor() ? (npc.isPetActor() ? "pet" : "animal") : "conversation";
        panelMode = PanelMode.DIALOGUE;
        screen = Screen.PANEL;
        noteUniversalWindowOpened(panelMode, "NPC interaction");
        selectedButton = 0;
        logEvent((npc == null ? "Conversation" : "Opened interaction with " + npc.name) + ".");
        DebugLog.audit("INTERACTION_ROUTE", "npc=" + (npc == null ? "none" : npc.id) + " kind=" + activeInteractionKind);
        repaint();
    }

    private void openObjectPanelFor(MapObjectState obj, String kind) {
        clearActiveInteractionState();
        activeInteractionObject = obj;
        activeInteractionTitle = obj == null ? "Object" : safeLabel(obj.label, "Object");
        activeInteractionKind = kind == null || kind.isBlank() ? "object" : kind;
        panelMode = PanelMode.OBJECT;
        screen = Screen.PANEL;
        noteUniversalWindowOpened(panelMode, "object interaction");
        selectedButton = 0;
        DebugLog.audit("INTERACTION_ROUTE", "object=" + (obj == null ? "none" : obj.summary()) + " kind=" + activeInteractionKind);
        repaint();
    }

    private void openObjectPanelFor(BaseObject base) {
        clearActiveInteractionState();
        activeInteractionBaseObject = base;
        activeInteractionTitle = base == null ? "Base object" : safeLabel(base.name, "Base object");
        activeInteractionKind = "base-machine";
        panelMode = PanelMode.OBJECT;
        screen = Screen.PANEL;
        noteUniversalWindowOpened(panelMode, "base object interaction");
        selectedButton = 0;
        repaint();
    }

    private void openTradeForNpc(NpcEntity npc) {
        clearActiveInteractionState();
        activeInteractionNpc = npc;
        activeInteractionTitle = npc == null ? "Trader" : safeLabel(npc.name, "Trader");
        activeInteractionKind = "npc-trade";
        activeTraderSession = buildTraderSession(npc, null);
        panelMode = PanelMode.TRADE;
        screen = Screen.PANEL;
        noteUniversalWindowOpened(panelMode, "NPC trade");
        selectedButton = 0;
        logEvent("Opened trade with " + activeInteractionTitle + ".");
        DebugLog.audit("INTERACTION_ROUTE", "tradeNpc=" + (npc == null ? "none" : npc.id) + " offers=" + (activeTraderSession == null ? 0 : activeTraderSession.offers.size()));
        repaint();
    }

    private void openTradeForObject(MapObjectState obj) {
        clearActiveInteractionState();
        activeInteractionObject = obj;
        activeInteractionTitle = obj == null ? "Store" : safeLabel(obj.label, "Store");
        activeInteractionKind = "object-trade";
        activeTraderSession = buildObjectTradeSession(obj);
        panelMode = PanelMode.TRADE;
        screen = Screen.PANEL;
        noteUniversalWindowOpened(panelMode, "object trade");
        selectedButton = 0;
        logEvent("Opened store panel: " + activeInteractionTitle + ".");
        DebugLog.audit("INTERACTION_ROUTE", "tradeObject=" + (obj == null ? "none" : obj.summary()) + " offers=" + (activeTraderSession == null ? 0 : activeTraderSession.offers.size()));
        repaint();
    }

    private void openContainerForObject(MapObjectState obj) {
        clearActiveInteractionState();
        activeInteractionObject = obj;
        activeInteractionTitle = obj == null ? "Container" : safeLabel(obj.label, "Container");
        activeInteractionKind = "object-container";
        activeInteractionContainerId = containerIdForInteractionObject(obj);
        ensureObjectContainerSeeded(obj, activeInteractionContainerId);
        panelMode = PanelMode.CONTAINER;
        screen = Screen.PANEL;
        noteUniversalWindowOpened(panelMode, "container transfer");
        selectedButton = 0;
        logEvent("Opened container: " + activeInteractionTitle + ".");
        DebugLog.audit("INTERACTION_ROUTE", "containerObject=" + (obj == null ? "none" : obj.summary()) + " cid=" + activeInteractionContainerId + " items=" + containerItemCount(activeInteractionContainerId));
        repaint();
    }

    private TraderSession buildTraderSession(NpcEntity npc, MapObjectState source) {
        ZoneType zone = world == null ? ZoneType.NEUTRAL_CIVILIAN_FLOOR : world.zoneType;
        Random r = interactionRandom("trade", npc == null ? null : npc.id, source == null ? null : source.id);
        TraderSession t = TraderSession.forNpc(npc, zone, r);
        TraderTradeActionAuthority.populateAccessibleFactionStock(t, npc, zone, r);
        Faction f = npc == null ? FactionInventoryStockAuthority.factionForZone(zone) : npc.faction;
        NpcFactionSite site = siteForFaction(f, zone);
        TraderTradeActionAuthority.attachNpcSiteStock(t, site, r, world, turn);
        t.applySupplyChainStock(world, turn, r);
        if (source != null) {
            t.name = safeLabel(source.label, "Store");
            t.archetype = safeLabel(source.type, "Store");
        }
        return t;
    }

    private TraderSession buildObjectTradeSession(MapObjectState obj) {
        if (obj != null && isVendingObject(obj)) {
            if (!LimitedVendingStockAuthority.hasStock(obj)) LimitedVendingStockAuthority.seedMachineStock(obj, world == null ? null : world.zoneType, interactionRandom("vending-seed", obj.id));
            TraderSession t = new TraderSession();
            t.name = safeLabel(obj.label, "Vending machine");
            t.archetype = "Vending machine";
            t.zoneLabel = world == null ? "Unknown Zone" : world.zoneType.label;
            t.markupPct = 12;
            t.supplyChainSummary = "Limited vending stock: " + LimitedVendingStockAuthority.remaining(obj) + " vend(s) remaining.";
            for (String item : LimitedVendingStockAuthority.items(obj)) {
                t.offers.add(new TradeOffer(item, categoryForTradeItem(item), Math.max(1, ItemCatalog.priceFor(item)), "limited-stock vending machine item."));
            }
            return t;
        }
        return buildTraderSession(null, obj);
    }

    private String categoryForTradeItem(String item) {
        ItemDef d = ItemCatalog.get(item);
        return d == null || d.category == null || d.category.isBlank() ? "stock" : d.category;
    }

    private Random interactionRandom(String salt, String a) {
        return interactionRandom(salt, a, null);
    }

    private Random interactionRandom(String salt, String a, String b) {
        long base = seed == 0L ? 0x5EEDL : seed;
        return new Random(base ^ (long)turn * 65537L ^ Objects.hash(salt, a, b, playerX, playerY));
    }

    private TradeOffer selectedTradeOffer() {
        if (activeTraderSession == null || activeTraderSession.offers == null || activeTraderSession.offers.isEmpty()) return null;
        selectedTradeOfferIndex = Math.max(0, Math.min(selectedTradeOfferIndex, activeTraderSession.offers.size() - 1));
        return activeTraderSession.offers.get(selectedTradeOfferIndex);
    }

    private void buySelectedTradeOffer() {
        TradeOffer offer = selectedTradeOffer();
        if (offer == null || activeTraderSession == null) {
            logEvent("No trade offer selected.");
            return;
        }
        if (inventoryWeight() + 1 > carryCapacity()) {
            logEvent("Cannot buy " + offer.name + ": carrying load is full.");
            return;
        }
        if (activeInteractionObject != null && isVendingObject(activeInteractionObject) && LimitedVendingStockAuthority.remaining(activeInteractionObject) <= 0) {
            logEvent("Cannot buy " + offer.name + ": this vending machine is out of stock.");
            return;
        }
        int price = activeTraderSession.buyPrice(offer);
        if (!spendImperialScript(price)) {
            logEvent("Cannot buy " + offer.name + ": need " + price + " script, have " + carriedScript + ".");
            return;
        }
        if (activeInteractionObject != null && isVendingObject(activeInteractionObject)) {
            int rem = Math.max(0, LimitedVendingStockAuthority.remaining(activeInteractionObject) - 1);
            activeInteractionObject.stockState = MapObjectState.setStockFlag(activeInteractionObject.stockState, "remaining", String.valueOf(rem));
            activeInteractionObject.stockState = MapObjectState.setStockFlag(activeInteractionObject.stockState, "lastVend", offer.name.replace(' ', '_'));
            activeInteractionObject.vendCount++;
            activeTraderSession.supplyChainSummary = "Limited vending stock: " + rem + " vend(s) remaining.";
        }
        ItemProvenanceRecord pr = offer.provenance == null
                ? ItemProvenanceRecord.trade(offer.name, activeInteractionNpc == null ? FactionInventoryStockAuthority.factionForZone(world == null ? null : world.zoneType) : activeInteractionNpc.faction, activeTraderSession.name, world, turn, "bought by player through interaction trade panel")
                : ItemProvenanceRecord.transferred(offer.provenance, offer.name, world, turn, "sold to player by " + activeTraderSession.name);
        addInventoryItem(offer.name, pr);
        rememberItemProvenance(offer.name, pr);
        rebuildItemContainersFromLegacyLists();
        gainXp("Commerce", 1, "bought " + offer.name);
        logEvent("Bought " + offer.name + " for " + price + " script from " + safeLabel(activeTraderSession.name, "store") + ".");
        advanceTurn("buys " + offer.name + ".");
        repaint();
    }

    private void sellSelectedInventoryItemToTrader() {
        if (activeTraderSession == null) {
            logEvent("No active trader is available.");
            return;
        }
        String item = selectedInventoryItem();
        if (item == null) {
            logEvent("No carried item selected to sell.");
            return;
        }
        if (!TradeReadabilityAuthority.saleAllowed(item)) {
            logEvent("Cannot sell " + item + ": mission, evidence, or intelligence items require a dedicated hand-in or explicit release flow.");
            repaint();
            return;
        }
        ItemProvenanceRecord pr = peekProvenanceForItem(item);
        int ordinaryPrice = activeTraderSession.sellPrice(item);
        int price = ProductionDefectAppraisalAuthority.appraise(ordinaryPrice, pr).adjustedPrice();
        inventory.remove(selectedInventoryIndex);
        selectedInventoryIndex = Math.max(0, Math.min(selectedInventoryIndex, Math.max(0, inventory.size() - 1)));
        addImperialScript(price);
        pr = takeProvenanceForItem(item);
        if (pr != null) rememberItemProvenance(item, ItemProvenanceRecord.transferred(pr, item, world, turn, "sold by player to " + safeLabel(activeTraderSession.name, "trader")));
        rebuildItemContainersFromLegacyLists();
        gainXp("Commerce", 1, "sold " + item);
        logEvent("Sold " + item + " for " + price + " script to " + safeLabel(activeTraderSession.name, "trader")
                + (ordinaryPrice == price ? "." : " after its recorded defect appraisal."));
        advanceTurn("sells " + item + ".");
        repaint();
    }

    private void ensureActiveContainerReady() {
        if (activeInteractionContainerId == null || activeInteractionContainerId.isBlank()) {
            activeInteractionContainerId = activeInteractionObject == null ? CONTAINER_BASE_STORAGE : containerIdForInteractionObject(activeInteractionObject);
        }
        ensureObjectContainerSeeded(activeInteractionObject, activeInteractionContainerId);
    }

    private java.util.List<ItemInstance> activeContainerItems() {
        ensureActiveContainerReady();
        ArrayList<ItemInstance> out = new ArrayList<>();
        ContainerRecord c = itemContainers.get(activeInteractionContainerId);
        if (c == null) return out;
        for (String id : c.itemInstanceIds) {
            ItemInstance inst = itemInstances.get(id);
            if (inst != null) out.add(inst);
        }
        return out;
    }

    private ItemInstance selectedContainerItem() {
        java.util.List<ItemInstance> items = activeContainerItems();
        if (items.isEmpty()) return null;
        selectedContainerItemIndex = Math.max(0, Math.min(selectedContainerItemIndex, items.size() - 1));
        return items.get(selectedContainerItemIndex);
    }

    private void takeSelectedContainerItem() {
        ItemInstance inst = selectedContainerItem();
        if (inst == null) {
            logEvent("No container item selected.");
            return;
        }
        if (inventoryWeight() + 1 > carryCapacity()) {
            logEvent("Cannot take " + inst.displayName + ": carrying load is full.");
            return;
        }
        ItemInstance moved = transferContainerItemByName(activeInteractionContainerId, null, inst.displayName, CONTAINER_PLAYER_INVENTORY, "Player inventory", inventory, "player took from interaction container");
        if (moved == null) {
            logEvent("Could not take " + inst.displayName + " from this container.");
            return;
        }
        selectedContainerItemIndex = Math.max(0, Math.min(selectedContainerItemIndex, Math.max(0, activeContainerItems().size() - 1)));
        rememberItemProvenance(moved.displayName, moved.provenance);
        logEvent("Took " + moved.displayName + " from " + activeInteractionTitleOrDefault() + ".");
        advanceTurn("takes " + moved.displayName + ".");
        repaint();
    }

    private void putSelectedInventoryItemIntoContainer() {
        String item = selectedInventoryItem();
        if (item == null) {
            logEvent("No carried item selected to put away.");
            return;
        }
        ensureActiveContainerReady();
        ItemInstance moved = transferContainerItemByName(CONTAINER_PLAYER_INVENTORY, inventory, item, activeInteractionContainerId, activeInteractionTitleOrDefault(), null, "player put into interaction container");
        if (moved == null) {
            logEvent("Could not put " + item + " into " + activeInteractionTitleOrDefault() + ".");
            return;
        }
        selectedInventoryIndex = Math.max(0, Math.min(selectedInventoryIndex, Math.max(0, inventory.size() - 1)));
        logEvent("Put " + moved.displayName + " into " + activeInteractionTitleOrDefault() + ".");
        advanceTurn("puts " + moved.displayName + " away.");
        repaint();
    }

    private String containerIdForInteractionObject(MapObjectState obj) {
        if (obj == null) return CONTAINER_BASE_STORAGE;
        String type = canonicalObjectType(obj);
        if (type.contains("corpse")) {
            String cid = obj.stockState == null || obj.stockState.isBlank() ? "" : obj.stockState.trim();
            return cid.isBlank() ? CONTAINER_CORPSE_LOOT_PREFIX + safeObjectId(obj) : cid;
        }
        if (type.contains("contract")) return CONTAINER_CONTRACT_OBJECT_PREFIX + safeObjectId(obj);
        if (type.contains("vault")) return "bank-vault-" + safeObjectId(obj);
        return persistentContainerIdForObject(obj);
    }

    private void ensureObjectContainerSeeded(MapObjectState obj, String containerId) {
        if (containerId == null || containerId.isBlank()) return;
        ensureContainer(containerId, obj == null ? containerId : safeLabel(obj.label, containerId));
        if (containerItemCount(containerId) > 0 || obj == null) return;
        String type = canonicalObjectType(obj);
        if (type.contains("contract")) {
            String item = MapObjectState.itemNameFromStock(obj.stockState);
            if (item != null && !item.isBlank()) addItemToContainerResult(containerId, safeLabel(obj.label, containerId), item, ItemProvenanceRecord.found(item, world, turn, safeLabel(obj.label, "contract object")), obj, "seed contract object container");
        } else if (type.contains("vault")) {
            addItemToContainerResult(containerId, safeLabel(obj.label, containerId), "Sealed bank lockbox", ItemProvenanceRecord.found("Sealed bank lockbox", world, turn, safeLabel(obj.label, "bank vault")), obj, "seed bank vault container");
        }
    }

    private void talkToActiveNpc() {
        NpcEntity npc = activeInteractionNpc;
        if (npc == null) {
            logEvent("No entity is available to talk to.");
            return;
        }
        runNpcTalkedTo++;
        logEvent("Talked to " + npc.name + " (" + safeLabel(npc.role, "local actor") + ", " + (npc.faction == null ? "No faction" : npc.faction.label) + ").");
        if (npc.isFactionRepresentative()) logEvent("Faction representative interface is active; contract/reputation submenu remains attached to this actor.");
        gainXp("Social", 1, "spoke with " + npc.name);
        advanceTurn("talks with " + npc.name + ".");
        repaint();
    }

    private void petActiveAnimal() {
        NpcEntity npc = activeInteractionNpc;
        if (npc == null || !npc.isAnimalActor()) {
            logEvent("No pet or animal is selected.");
            return;
        }
        PetInteractionFeedbackAuthority.PetInteractionReadout readout = PetInteractionFeedbackAuthority.apply(npc);
        logEvent(readout.feedback());
        if (!readout.allowed()) {
            lastTargetingReport = readout.feedback();
            repaint();
            return;
        }
        gainXp("Animal Handling", 1, "interacted with " + npc.name);
        advanceTurn(readout.actionLabel().toLowerCase(Locale.ROOT) + " for " + npc.name + ".");
        repaint();
    }

    private void operateActiveInteractionObject() {
        MapObjectState obj = activeInteractionObject;
        BaseObject base = activeInteractionBaseObject;
        if (base != null) {
            logEvent("Opened " + safeLabel(base.name, "base object") + " operation surface.");
            panelMode = PanelMode.WORKBENCH;
            screen = Screen.PANEL;
            repaint();
            return;
        }
        if (obj == null) {
            logEvent("No object is selected.");
            return;
        }
        if (NeedSupplyInteractionAuthority.tryUse(this, obj)) {
            repaint();
            return;
        }
        String type = canonicalObjectType(obj);
        if (type.equals("light-switch")) {
            toggleLightSwitch(obj);
            return;
        }
        if (type.equals("broadcast-device") || type.contains("newspaper") || type.contains("inn")) {
            String report = ImperialNewsNetworkApi.broadcastBulletin(this, "interaction-object/" + safeLabel(obj.type, "broadcast"), interactionRandom("broadcast", obj.id));
            lastBroadcastReport = report;
            lastInnNewsIssue = report;
            logEvent("BROADCAST OBJECT: " + report);
            gainXp("Investigation", 1, "used broadcast object");
        } else if (type.equals("bank-terminal")) {
            openBankAccounts.add(MapObjectState.itemNameFromStock(obj.stockState));
            lastBankReport = "Bank terminal used at " + obj.x + "," + obj.y + " / " + safeLabel(obj.label, "bank terminal") + ".";
            logEvent(lastBankReport);
            gainXp("Commerce", 1, "used bank terminal");
        } else if (type.equals("bank-alarm-panel")) {
            obj.stockState = MapObjectState.setStockFlag(obj.stockState, "disabled", "true");
            obj.stockState = MapObjectState.setStockFlag(obj.stockState, "armed", "false");
            lastBankAlarmReport = "Bank alarm panel disabled at " + obj.x + "," + obj.y + ".";
            logEvent(lastBankAlarmReport);
            gainXp("Security", 1, "disabled bank alarm panel");
        } else if (type.equals("bank-vault")) {
            if (hasInventoryLike("Bank manager keycard", "Secure vault key", "Data spike")) {
                obj.stockState = MapObjectState.setStockFlag(obj.stockState, "locked", "false");
                obj.stockState = MapObjectState.setStockFlag(obj.stockState, "open", "true");
                openContainerForObject(obj);
                return;
            }
            logEvent("Bank vault remains locked. It wants a keycard, vault key, or data spike.");
        } else if (type.contains("explosive")) {
            obj.stockState = MapObjectState.setStockFlag(obj.stockState, "armed", "false");
            logEvent("Disarmed " + safeLabel(obj.label, "explosive object") + ".");
            gainXp("Security", 1, "disarmed explosive object");
        } else if (isMachineObject(obj)) {
            obj.vendCount++;
            obj.cooldownUntilTurn = Math.max(obj.cooldownUntilTurn, turn + 8);
            logEvent("Operated " + safeLabel(obj.label, "machine") + ". This machine is attached to the interaction/options surface; use Craft for recipes or Inventory for inserted items.");
            gainXp("Mechanics", 1, "operated " + safeLabel(obj.type, "machine"));
        } else {
            obj.vendCount++;
            logEvent("Inspected " + safeLabel(obj.label, "object") + " (" + safeLabel(obj.type, "object") + ").");
        }
        advanceTurn("uses " + safeLabel(obj.label, "an object") + ".");
        repaint();
    }

    private void toggleLightSwitch(MapObjectState obj) {
        String group = MapObjectState.stockValue(obj.stockState, "group");
        boolean oldOn = !"false".equalsIgnoreCase(MapObjectState.stockValue(obj.stockState, "on"));
        boolean next = !oldOn;
        obj.stockState = MapObjectState.setStockFlag(obj.stockState, "on", String.valueOf(next));
        int changed = 0;
        if (world != null && world.lightSources != null) {
            for (ZoneLightSourceRecord light : world.lightSources) {
                if (light == null) continue;
                if (group == null || group.isBlank() || group.equals(light.groupId)) {
                    light.on = next;
                    changed++;
                }
            }
        }
        markLocalDirtyRegion("light switch toggled", obj.x, obj.y, 10, true, false, true, false);
        updateSensoryModel("light switch toggled");
        logEvent("Light switch toggled " + (next ? "on" : "off") + " for group " + safeLabel(group, "local") + " (" + changed + " light source(s)).");
        advanceTurn("toggles a light switch.");
        repaint();
    }

    private void lookAtActiveInteractionTarget() {
        if (activeInteractionNpc != null) {
            lookX = activeInteractionNpc.x;
            lookY = activeInteractionNpc.y;
        } else if (activeInteractionObject != null) {
            lookX = activeInteractionObject.x;
            lookY = activeInteractionObject.y;
        } else if (activeInteractionBaseObject != null) {
            lookX = activeInteractionBaseObject.x;
            lookY = activeInteractionBaseObject.y;
        }
        beginLookMode();
    }

    private boolean isTradeObject(MapObjectState obj) {
        String type = canonicalObjectType(obj);
        return type.equals("shop") || type.equals("vending") || type.contains("store") || type.contains("trade") || type.contains("counter");
    }

    private boolean isVendingObject(MapObjectState obj) {
        String type = canonicalObjectType(obj);
        return type.equals("vending") || type.contains("vending");
    }

    private boolean isContainerObject(MapObjectState obj) {
        String type = canonicalObjectType(obj);
        return type.contains("corpse") || type.contains("contract") || type.contains("container") || type.contains("vault");
    }

    private boolean isMachineObject(MapObjectState obj) {
        String type = canonicalObjectType(obj);
        return type.contains("machine") || type.contains("fixture") || type.contains("forge") || type.contains("lab")
                || type.contains("terminal") || type.contains("switch") || type.contains("broadcast") || type.contains("bank")
                || type.contains("light") || type.contains("alarm");
    }

    private String canonicalObjectType(MapObjectState obj) {
        if (obj == null || obj.type == null) return "";
        return AssetIntegrationDisciplineAuthority.canonicalType(obj.type).toLowerCase(Locale.ROOT);
    }

    private String safeObjectId(MapObjectState obj) {
        String id = obj == null ? "" : obj.id;
        if (id == null || id.isBlank()) id = String.valueOf(Objects.hash(obj == null ? 0 : obj.x, obj == null ? 0 : obj.y, obj == null ? "" : obj.type));
        return id.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    private boolean hasInventoryLike(String... names) {
        if (names == null) return false;
        for (String item : inventory) for (String name : names) if (ItemQuality.namesMatch(item, name)) return true;
        for (String item : baseStorage) for (String name : names) if (ItemQuality.namesMatch(item, name)) return true;
        return false;
    }

    private String selectedInventoryItem() {
        if (inventory.isEmpty()) return null;
        selectedInventoryIndex = Math.max(0, Math.min(selectedInventoryIndex, inventory.size() - 1));
        return inventory.get(selectedInventoryIndex);
    }

    private String selectedBaseStorageItem() {
        if (baseStorage.isEmpty()) return null;
        selectedTargetInventoryIndex = Math.max(0, Math.min(selectedTargetInventoryIndex, baseStorage.size() - 1));
        return baseStorage.get(selectedTargetInventoryIndex);
    }

    private void storeSelectedInventoryItem() {
        String item = selectedInventoryItem();
        if (item == null) return;
        inventory.remove(selectedInventoryIndex);
        baseStorage.add(item);
        selectedInventoryIndex = Math.max(0, Math.min(selectedInventoryIndex, Math.max(0, inventory.size() - 1)));
        logEvent("Stored " + item + ".");
        rebuildItemContainersFromLegacyLists();
        repaint();
    }

    private void takeSelectedBaseStorageItem() {
        String item = selectedBaseStorageItem();
        if (item == null) return;
        baseStorage.remove(selectedTargetInventoryIndex);
        inventory.add(item);
        selectedTargetInventoryIndex = Math.max(0, Math.min(selectedTargetInventoryIndex, Math.max(0, baseStorage.size() - 1)));
        logEvent("Took " + item + ".");
        rebuildItemContainersFromLegacyLists();
        repaint();
    }

    private void drawListBox(java.awt.Graphics2D g, Rectangle r, String title, java.util.List<String> rows, int selected, boolean activeColumn) {
        drawBox(g, r, title);
        g.setFont(smallFont);
        int y = r.y + 34;
        if (rows == null || rows.isEmpty()) {
            g.setColor(new java.awt.Color(145, 148, 132));
            drawUiTextLine(g, "Empty", r.x + 12, y);
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (y > r.y + r.height - 12) break;
            boolean rowSelected = activeColumn && i == selected;
            g.setColor(rowSelected ? new java.awt.Color(55, 48, 32, 230) : new java.awt.Color(0, 0, 0, 0));
            if (rowSelected) g.fillRect(r.x + 8, y - 15, r.width - 16, 20);
            g.setColor(rowSelected ? new java.awt.Color(245, 220, 140) : new java.awt.Color(205, 210, 195));
            drawUiTextLine(g, GuiLayoutApi.fitLabel((rowSelected ? "> " : "  ") + rows.get(i), g.getFontMetrics(), r.width - 24), r.x + 12, y);
            y += 20;
        }
    }

    private void drawDetailBox(java.awt.Graphics2D g, Rectangle r, String title, java.util.List<String> lines, BufferedImage icon) {
        drawBox(g, r, title);
        int x = r.x + 12;
        int y = r.y + 34;
        if (icon != null) {
            int size = Math.min(96, Math.max(42, Math.min(r.width - 24, r.height / 3)));
            g.drawImage(icon, x, y, size, size, null);
            g.setColor(new java.awt.Color(145, 118, 64, 160));
            g.drawRect(x, y, size, size);
            y += size + 14;
        }
        g.setFont(smallFont);
        g.setColor(new java.awt.Color(205, 210, 195));
        if (lines != null) {
            for (String line : lines) {
                if (y > r.y + r.height - 12) break;
                for (String wrapped : GuiLayoutApi.wrapText(line, g.getFontMetrics(), r.width - 24)) {
                    if (y > r.y + r.height - 12) break;
                    drawUiTextLine(g, wrapped, x, y);
                    y += 18;
                }
            }
        }
    }

    private void drawBox(java.awt.Graphics2D g, Rectangle r, String title) {
        g.setColor(new java.awt.Color(12, 14, 13, 226));
        g.fillRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        g.setColor(new java.awt.Color(100, 82, 46, 150));
        g.drawRoundRect(r.x, r.y, r.width, r.height, 8, 8);
        if (title != null && !title.isBlank()) {
            g.setFont(uiFont.deriveFont(java.awt.Font.BOLD, 14f));
            g.setColor(new java.awt.Color(225, 205, 140));
            drawUiTextLine(g, GuiLayoutApi.fitLabel(title, g.getFontMetrics(), r.width - 24), r.x + 12, r.y + 22);
        }
    }

    private void addOverlayButton(String label, int x, int y, int w, int h, String tip, Runnable action) {
        buttons.add(new ButtonBox(label, x, y, w, h, tip, action, systemButtonIconForLabel(label)));
    }

    private void addOverlayButton(String label, int x, int y, int w, int h, String tip, Runnable action, BufferedImage icon) {
        buttons.add(new ButtonBox(label, x, y, w, h, tip, action, icon));
    }

    private void drawOverlayButtons(java.awt.Graphics2D g) {
        if (buttons.isEmpty()) return;
        selectedButton = Math.max(0, Math.min(selectedButton, buttons.size() - 1));
        for (int i = 0; i < buttons.size(); i++) {
            ButtonBox button = buttons.get(i);
            ensureButtonSystemIcon(button);
            button.draw(g, smallFont, i == selectedButton, null, options);
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
    void executePacedMovementBody(int dx, int dy, String source) {
        MovementExecutionAuthority.executeStep(this, dx, dy, source, true);
    }
    void clearPendingMovementInput(String reason) {}
    void advanceTurnBody(String line) {
        turn++;
        worldTurn++;
        if (line != null && !line.isBlank()) logEvent(line);
        tickFactionRuntimeSystems(false);
        tickPopulationEconomyRuntimeSystems(false);
    }
    void advanceTurn(String line) { advanceTurnBody(line); }
    void settlePlayerMotionAfterNoMoveTurn(String reason) {}
    void confirmInteractionBody() {
        if (world == null) {
            logEvent("Interact unavailable: no world loaded.");
            return;
        }
        clampInteractCursorToAdjacent();
        if (!world.inBounds(lookX, lookY)) {
            logEvent("Interact target is outside the current world slice.");
            return;
        }
        setFacingToward(lookX, lookY, "interaction facing");
        NpcEntity npc = world.npcAt(lookX, lookY);
        if (npc != null) {
            if (npc.isTrader()) openTradeForNpc(npc);
            else openDialogueForNpc(npc);
            updatePendingInteractionSummary();
            return;
        }
        if (RoomFixtureInteractionAuthority.tryInteract(this, lookX, lookY)
                || FrontageFixtureInteractionAuthority.tryInteract(this, lookX, lookY)
                || RoadTransitFixtureInteractionAuthority.tryInteract(this, lookX, lookY)) {
            updatePendingInteractionSummary();
            return;
        }
        MapObjectState obj = world.mapObjectAt(lookX, lookY);
        if (obj != null) {
            if (isTradeObject(obj)) openTradeForObject(obj);
            else if (isContainerObject(obj)) openContainerForObject(obj);
            else openObjectPanelFor(obj, isMachineObject(obj) ? "machine/object" : "object");
            updatePendingInteractionSummary();
            return;
        }
        BaseObject base = baseObjectAt(lookX, lookY);
        if (base != null) {
            openObjectPanelFor(base);
            updatePendingInteractionSummary();
            return;
        }
        char tile = world.tiles[lookX][lookY];
        if (isDoorTile(tile)) {
            interactDoorAt(lookX, lookY, tile);
            updatePendingInteractionSummary();
            return;
        }
        ArrayList<String> stack = tileStackAt(lookX, lookY);
        logEvent("Nothing usable at " + lookX + "," + lookY + (stack.isEmpty() ? "." : ": " + stack.get(0)));
        updatePendingInteractionSummary();
    }
    void confirmCombatTargetBody() {
        if (world == null) {
            lastTargetingReport = "Combat unavailable: no world loaded.";
            logEvent(lastTargetingReport);
            return;
        }
        clampCombatCursorToWorld();
        setFacingToward(combatX, combatY, "combat target");
        LegacyTargetingSolution solution = targetingSolutionAt(combatX, combatY);
        lastTargetingReport = solution.summary;
        NpcEntity npc = world.npcAt(combatX, combatY);
        if (npc == null) {
            logEvent("No combat target at " + combatX + "," + combatY + ".");
            return;
        }
        logEvent("Combat target confirmed: " + npc.name + " at " + combatX + "," + combatY + ".");
        advanceTurn("takes aim at " + npc.name + ".");
    }
    void useSelectedInventoryItemBody() {
        if (inventoryTargetColumnActive) {
            String stored = selectedBaseStorageItem();
            logEvent(stored == null ? "No base-storage item selected." : stored + " is in base storage; take it before using it.");
            return;
        }
        String item = selectedInventoryItem();
        if (item == null) {
            logEvent("No carried item selected.");
            return;
        }
        String low = item.toLowerCase(Locale.ROOT);
        if (low.contains("ration") || low.contains("food") || low.contains("loaf") || low.contains("broth") || low.contains("meal")) {
            food = Math.min(MAX_FOOD_WATER, food + 28);
            inventory.remove(selectedInventoryIndex);
            moveInventorySelection(0);
            logEvent("Consumed " + item + ". Food " + food + "/" + MAX_FOOD_WATER + ".");
            advanceTurn("eats " + item + ".");
            return;
        }
        if (low.contains("water") || low.contains("canteen") || low.contains("flask") || low.contains("bottle")) {
            water = Math.min(MAX_FOOD_WATER, water + 28);
            inventory.remove(selectedInventoryIndex);
            moveInventorySelection(0);
            logEvent("Drank " + item + ". Water " + water + "/" + MAX_FOOD_WATER + ".");
            advanceTurn("drinks " + item + ".");
            return;
        }
        if (low.contains("medkit") || low.contains("bandage") || low.contains("splint") || low.contains("antiseptic")) {
            wounds = Math.max(0, wounds - 1);
            bleeding = Math.max(0, bleeding - 2);
            infectionRisk = Math.max(0, infectionRisk - 2);
            pain = Math.max(0, pain - 1);
            inventory.remove(selectedInventoryIndex);
            moveInventorySelection(0);
            logEvent("Used " + item + ". Wounds " + wounds + ", bleeding " + bleeding + ", infection risk " + infectionRisk + ".");
            advanceTurn("uses " + item + ".");
            return;
        }
        PortableLightProfile portableProfile = PortableLightProfile.profile(item);
        if (portableProfile != null || low.contains("lamp") || low.contains("lantern") || low.contains("torch") || low.contains("glow") || low.contains("light")) {
            activePortableLightItem = item;
            activePortableLightWorn = portableProfile != null && portableProfile.wearable;
            activePortableLightExpiresTurn = turn + (portableProfile == null ? Math.max(60, TURNS_PER_HOUR) : portableProfile.remainingDuration(turn, item, 0));
            lastPortableLightReport = "Portable light active: " + item + " radius " + activePortableLightRadius() + " until turn " + activePortableLightExpiresTurn + ".";
            logEvent(lastPortableLightReport);
            markLocalDirtyRegion("portable light activated", playerX, playerY, activePortableLightRadius() + 2, true, false, false, false);
            updateSensoryModel("portable light activated");
            repaint();
            return;
        }
        ItemDef def = ItemCatalog.get(item);
        if (def != null && def.weapon) {
            equipInventoryItemToHand(item, false);
            return;
        }
        if (def != null) {
            logEvent(item + ": " + def.use);
            return;
        }
        logEvent("Inspected " + item + ".");
    }
    void unequipSelectedEquipmentSlotBody() {
        unequipEquipmentSlot(1);
    }

    void equipSelectedInventoryItemToHand(boolean leftHand) {
        if (inventoryTargetColumnActive) {
            logEvent("Take the base-storage item before equipping it.");
            return;
        }
        String item = selectedInventoryItem();
        if (item == null) {
            logEvent("No carried item selected to equip.");
            return;
        }
        equipInventoryItemToHand(item, leftHand);
    }

    private void equipInventoryItemToHand(String item, boolean leftHand) {
        if (item == null || item.isBlank()) {
            logEvent("No item selected to equip.");
            return;
        }
        ItemDef def = ItemCatalog.get(item);
        if (def == null || !def.weapon) {
            logEvent(item + " is not a hand-equipped weapon.");
            return;
        }
        if (leftHand) equippedLeftHandItem = item;
        else equippedRightHandItem = item;
        activeWeaponHandIndex = leftHand ? 0 : 1;
        if (ItemCatalog.isFirearmLike(item)) loadedWeaponShots.putIfAbsent(item, 0);
        logEvent("Equipped " + item + " in the " + (leftHand ? "left" : "right") + " hand.");
        repaint();
    }

    void unequipEquipmentSlot(int slotIndex) {
        if (slotIndex == 0) {
            if (equippedLeftHandItem == null || equippedLeftHandItem.isBlank() || "LEFT EMPTY".equals(equippedLeftHandItem)) {
                logEvent("Left hand is already empty.");
                return;
            }
            logEvent("Unequipped " + equippedLeftHandItem + " from the left hand.");
            equippedLeftHandItem = "LEFT EMPTY";
        } else {
            if (equippedRightHandItem == null || equippedRightHandItem.isBlank() || "RIGHT EMPTY".equals(equippedRightHandItem)) {
                logEvent("Right hand is already empty.");
                return;
            }
            logEvent("Unequipped " + equippedRightHandItem + " from the right hand.");
            equippedRightHandItem = "RIGHT EMPTY";
        }
        repaint();
    }
    void addImperialScript(int amount) { carriedScript += Math.max(0, amount); }
    boolean spendImperialScript(int amount) { if (amount <= 0) return true; if (carriedScript < amount) return false; carriedScript -= amount; return true; }
    void markLocalDirtyRegion(String reason, int x, int y, int radius, boolean tiles, boolean npcs, boolean objects, boolean full) {
        if (world == null) return;
        int r = Math.max(1, radius);
        if (tiles || objects || full) world.dirtyLightRevision++;
        if (npcs || objects || tiles || full) world.dirtyVisionRevision++;
        if (objects || full) world.dirtyNoiseRevision++;
        if (full) world.dirtyHazardRevision++;
        lastSensoryModelReport = "Dirty sensory region " + x + "," + y + " r" + r + " reason=" + (reason == null ? "unspecified" : reason) + ".";
    }
    void ensureSensoryModelCurrent(String reason) {
        if (world == null) return;
        int portableSignature = portableLightSignature();
        boolean badArrays = lightLevels == null || lightLevels.length != world.w || lightLevels[0].length != world.h
                || visibleTiles == null || visibleTiles.length != world.w || visibleTiles[0].length != world.h
                || rememberedTiles == null || rememberedTiles.length != world.w || rememberedTiles[0].length != world.h;
        if (badArrays || sensoryWorldRef != world || sensoryTurn != turn || sensoryPlayerX != playerX || sensoryPlayerY != playerY
                || sensoryMovementMode != selectedMovementModeIndex
                || sensoryFacingDx != facingDx || sensoryFacingDy != facingDy
                || sensoryLightRevision != world.dirtyLightRevision || sensoryVisionRevision != world.dirtyVisionRevision
                || sensoryNoiseRevision != world.dirtyNoiseRevision
                || sensoryPortableSignature != portableSignature) {
            updateSensoryModel(reason);
        }
    }
    void updateSensoryModel(String reason) {
        if (world == null) return;
        ensureSensoryArrays();
        purgeExpiredPortableLights();
        if (world.noiseFieldTurn != turn || world.dirtyNoiseRevision != sensoryNoiseRevision) {
            NoiseHearingFieldApi.rebuild(world, turn);
        }
        applyPlayerMovementNoiseToHearingField();
        rebuildLightLevels();
        rebuildVisionFields();
        sensoryWorldRef = world;
        sensoryTurn = turn;
        sensoryPlayerX = playerX;
        sensoryPlayerY = playerY;
        sensoryMovementMode = selectedMovementModeIndex;
        sensoryFacingDx = facingDx;
        sensoryFacingDy = facingDy;
        sensoryLightRevision = world.dirtyLightRevision;
        sensoryNoiseRevision = world.dirtyNoiseRevision;
        sensoryVisionRevision = world.dirtyVisionRevision;
        sensoryPortableSignature = portableLightSignature();
        lastSensoryModelReport = "visualSensory reason=" + (reason == null ? "unspecified" : reason)
                + " visible=" + visibleTileCount()
                + " remembered=" + rememberedTileCount()
                + " peakLight=" + peakLightLevel()
                + " ambient=" + ambientLightLevelForWorld()
                + " visionRange=" + visionRange()
                + " movement=" + movementModeLabel(selectedMovementModeIndex)
                + " movementVision=" + movementVisionRangeModifier()
                + " movementNoise=" + playerMovementNoiseSummary()
                + " projection=" + DirectionalVisionAuthority.summary(this) + ".";
    }
    void refreshNameLockedCandidateState(Candidate candidate) {}
    int visionRange() {
        return Math.max(3, DirectionalVisionAuthority.range(this) + movementVisionRangeModifier());
    }
    boolean isVisible(int x, int y) { return world != null && x >= 0 && y >= 0 && x < visibleTiles.length && y < visibleTiles[0].length && visibleTiles[x][y]; }
    boolean isRemembered(int x, int y) { return world != null && x >= 0 && y >= 0 && x < rememberedTiles.length && y < rememberedTiles[0].length && rememberedTiles[x][y]; }
    int lightLevelAt(int x, int y) { return world == null || x < 0 || y < 0 || x >= lightLevels.length || y >= lightLevels[0].length ? 0 : Math.max(0, Math.min(100, lightLevels[x][y])); }
    private void ensureSensoryArrays() {
        if (world == null) return;
        if (lightLevels == null || lightLevels.length != world.w || lightLevels[0].length != world.h) lightLevels = new int[world.w][world.h];
        if (visibleTiles == null || visibleTiles.length != world.w || visibleTiles[0].length != world.h) visibleTiles = new boolean[world.w][world.h];
        if (rememberedTiles == null || rememberedTiles.length != world.w || rememberedTiles[0].length != world.h) rememberedTiles = new boolean[world.w][world.h];
    }
    private void rebuildLightLevels() {
        int ambient = ambientLightLevelForWorld();
        for (int x = 0; x < world.w; x++) Arrays.fill(lightLevels[x], ambient);
        if (world.lightSources != null) {
            for (ZoneLightSourceRecord light : world.lightSources) {
                if (light == null || !light.on || !light.powered || !world.inBounds(light.x, light.y)) continue;
                int intensity = Math.max(1, light.intensity);
                if (light.flicker && Math.floorMod(turn + light.phase, Math.max(2, light.flickerPeriod)) == 0) intensity = Math.max(1, intensity / 3);
                applyBandedLight(light.x, light.y, Math.max(1, light.radius), intensity, light.profile);
            }
        }
        if (activePortableLightRadius() > 0) applyBandedLight(playerX, playerY, activePortableLightRadius(), portableLightIntensity(activePortableLightItem), activePortableLightItem);
        if (portableLights != null) {
            for (PortableLightInstance light : portableLights) {
                if (light == null || light.expiresTurn <= turn || !sameWorldLocation(light.worldKey)) continue;
                applyBandedLight(light.x, light.y, Math.max(1, light.radius), portableLightIntensity(light.itemName), light.itemName);
            }
        }
        for (WorldLightEmitterAuthority.Emitter emitter : WorldLightEmitterAuthority.gameplayEmitters(this)) {
            applyBandedLight(emitter.x, emitter.y, emitter.radius, emitter.intensity, emitter.id);
        }
    }
    private void applyBandedLight(int sx, int sy, int radius, int intensity) {
        applyBandedLight(sx, sy, radius, intensity, "");
    }
    private void applyBandedLight(int sx, int sy, int radius, int intensity, String profile) {
        if (world == null || !world.inBounds(sx, sy)) return;
        int r = Math.max(1, radius);
        int base = Math.max(1, Math.min(100, intensity));
        LightFalloffProfile falloff = LightFalloffProfile.forProfile(profile);
        for (int x = Math.max(0, sx - r); x <= Math.min(world.w - 1, sx + r); x++) {
            for (int y = Math.max(0, sy - r); y <= Math.min(world.h - 1, sy + r); y++) {
                double distance = Math.hypot(x - sx, y - sy);
                if (distance > r + 0.35) continue;
                if (!lineOfLightCanReach(sx, sy, x, y)) continue;
                double norm = Math.max(0.0, Math.min(1.0, distance / Math.max(1.0, r)));
                double bandFactor = falloff.factor(norm);
                if (!(x == sx && y == sy) && blocksLightTile(x, y)) bandFactor *= 0.78;
                int add = Math.max(1, (int)Math.round(base * bandFactor));
                lightLevels[x][y] = Math.max(lightLevels[x][y], Math.min(100, lightLevels[x][y] + add));
            }
        }
    }
    private void rebuildVisionFields() {
        for (int x = 0; x < world.w; x++) Arrays.fill(visibleTiles[x], false);
        int range = visionRange();
        int threshold = litVisionThreshold();
        int minX = Math.max(0, playerX - range);
        int maxX = Math.min(world.w - 1, playerX + range);
        int minY = Math.max(0, playerY - range);
        int maxY = Math.min(world.h - 1, playerY + range);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                double distance = Math.hypot(x - playerX, y - playerY);
                if (!withinFacingVisionCone(x, y, range)) continue;
                if (!hasLineOfSight(playerX, playerY, x, y)) continue;
                boolean lit = lightLevelAt(x, y) >= threshold || distance <= 1.05 || (x == playerX && y == playerY);
                if (!lit) continue;
                visibleTiles[x][y] = true;
                rememberedTiles[x][y] = true;
            }
        }
        if (world.inBounds(playerX, playerY)) {
            visibleTiles[playerX][playerY] = true;
            rememberedTiles[playerX][playerY] = true;
            lightLevels[playerX][playerY] = Math.max(lightLevels[playerX][playerY], 18);
        }
    }
    private int litVisionThreshold() { return activePortableLightRadius() > 0 ? 8 : 11; }
    private int movementVisionRangeModifier() {
        return switch (selectedMovementModeIndex) {
            case MOTION_SNEAK -> 1;
            case MOTION_RUN -> -1;
            case MOTION_SPRINT -> -2;
            default -> 0;
        };
    }

    private int playerMovementNoiseRadius() {
        if (!playerMotionAnimating()) return 0;
        return switch (selectedMovementModeIndex) {
            case MOTION_SNEAK -> 2;
            case MOTION_RUN -> 7;
            case MOTION_SPRINT -> 10;
            default -> 4;
        };
    }

    private int playerMovementNoiseIntensity() {
        if (!playerMotionAnimating()) return 0;
        return switch (selectedMovementModeIndex) {
            case MOTION_SNEAK -> 6;
            case MOTION_RUN -> 28;
            case MOTION_SPRINT -> 42;
            default -> 14;
        };
    }

    private String playerMovementNoiseSummary() {
        int radius = playerMovementNoiseRadius();
        int intensity = playerMovementNoiseIntensity();
        if (radius <= 0 || intensity <= 0) return "stationary";
        return "r" + radius + "/i" + intensity;
    }

    private void applyPlayerMovementNoiseToHearingField() {
        if (world == null || world.noiseField == null || !world.inBounds(playerX, playerY)) return;
        int radius = playerMovementNoiseRadius();
        int intensity = playerMovementNoiseIntensity();
        if (radius <= 0 || intensity <= 0) return;
        NoiseHearingFieldApi.Result movementNoise = new NoiseHearingFieldApi.Result();
        NoiseHearingFieldApi.applySource(world, playerX, playerY, radius, intensity, movementNoise);
        world.hearingFieldSummary = (world.hearingFieldSummary == null ? "" : world.hearingFieldSummary)
                + " playerMovement=" + movementModeLabel(selectedMovementModeIndex).toLowerCase(Locale.ROOT)
                + " radius=" + radius + " intensity=" + intensity;
    }

    private boolean lineOfLightCanReach(int sx, int sy, int tx, int ty) { return traceLightLine(sx, sy, tx, ty, true); }
    private boolean traceLightLine(int x0, int y0, int x1, int y1, boolean allowBlockedTarget) {
        if (world == null || !world.inBounds(x0, y0) || !world.inBounds(x1, y1)) return false;
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0;
        int y = y0;
        while (true) {
            if (!(x == x0 && y == y0) && blocksLightTile(x, y)) return allowBlockedTarget && x == x1 && y == y1;
            if (x == x1 && y == y1) return true;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 < dx) { err += dx; y += sy; }
            if (!world.inBounds(x, y)) return false;
        }
    }
    private boolean blocksLightTile(int x, int y) {
        if (world == null || !world.inBounds(x, y)) return true;
        char ch = world.tiles[x][y];
        if (isDoorTile(ch)) return true;
        if (ch == '#' || ch == 'D' || ch == 'H' || ch == 'W' || ch == 'X' || ch == '|' || ch == 'L' || ch == 'V') return true;
        if (InterstitialInfrastructureApi.isInterstitialSolid(ch)) return true;
        return !world.walkable(x, y) && ch != '~';
    }
    private int visibleTileCount() {
        int n = 0;
        if (visibleTiles != null) for (int x = 0; x < visibleTiles.length; x++) for (int y = 0; y < visibleTiles[x].length; y++) if (visibleTiles[x][y]) n++;
        return n;
    }
    private int rememberedTileCount() {
        int n = 0;
        if (rememberedTiles != null) for (int x = 0; x < rememberedTiles.length; x++) for (int y = 0; y < rememberedTiles[x].length; y++) if (rememberedTiles[x][y]) n++;
        return n;
    }
    private int peakLightLevel() {
        int peak = 0;
        if (lightLevels != null) for (int x = 0; x < lightLevels.length; x++) for (int y = 0; y < lightLevels[x].length; y++) peak = Math.max(peak, lightLevels[x][y]);
        return peak;
    }
    private int portableLightSignature() {
        int h = Objects.hash(activePortableLightItem, activePortableLightExpiresTurn, activePortableLightWorn);
        if (portableLights != null) {
            for (PortableLightInstance light : portableLights) {
                if (light == null) continue;
                h = 31 * h + Objects.hash(light.itemName, light.x, light.y, light.radius, light.expiresTurn, light.worldKey);
            }
        }
        return h;
    }
    private void purgeExpiredPortableLights() {
        if (portableLights == null || portableLights.isEmpty()) return;
        portableLights.removeIf(light -> light == null || light.expiresTurn <= turn);
    }
    int countMoney() { return Math.max(0, carriedScript); }
    int totalBankedCash() { return Math.max(0, baseStashedScript); }
    int inventoryWeight() { return inventory == null ? 0 : inventory.size(); }
    int carryCapacity() {
        return WorldGenerationSettingsAuthority.playerCarryCapacity(supplies, WorldGenerationSettingsAuthority.forGame(this));
    }
    void addInventoryItem(String item, ItemProvenanceRecord provenance) { if (item != null && !item.isBlank()) inventory.add(item); }
    void gainXp(String skill, int amount, String reason) { xp += Math.max(0, amount); }
    String facingLabel() {
        if (facingDy < 0) return "N";
        if (facingDy > 0) return "S";
        if (facingDx < 0) return "W";
        return "E";
    }
    String activeMotionStateLabel() {
        if (!playerMotionAnimating()) return "stationary";
        return "moving " + movementModeLabel(selectedMovementModeIndex).toLowerCase(Locale.ROOT);
    }
    String timeText() { return "day " + Math.max(0, turn / Math.max(1, TURNS_PER_HOUR * HOURS_PER_DAY)) + " hour " + Math.max(0, (turn / Math.max(1, TURNS_PER_HOUR)) % HOURS_PER_DAY); }
    String stateSummary() { return "turn=" + turn + " pos=" + playerX + "," + playerY + " screen=" + screen + " panel=" + panelMode; }
    void logEvent(String line) { if (line != null && !line.isBlank()) eventLog.add(line); }

    void setScreen(Screen next) {
        Screen previous = screen;
        screen = next == null ? Screen.MENU : next;
        if (universalWindowAuthority != null) {
            if (screen == Screen.PAUSE) universalWindowAuthority.open("pause", turn, "pause screen");
            else if (screen == Screen.OPTIONS) universalWindowAuthority.open("options", turn, "options screen");
            else if (screen == Screen.SAVE_LOAD) universalWindowAuthority.open("save_load", turn, "save/load screen");
            else if (screen == Screen.GAME && (previous == Screen.PAUSE || previous == Screen.OPTIONS || previous == Screen.SAVE_LOAD)
                    && !universalWindowAuthority.focusedWindowId().isBlank()) {
                universalWindowAuthority.close(universalWindowAuthority.focusedWindowId(), turn, "returned to game");
            }
        }
        if (screen == Screen.MENU || screen == Screen.MAIN) {
            newGameSetupActive = false;
            characterNameEditActive = false;
            graphicsDropdown = -1;
            panelMode = PanelMode.NONE;
            selectedButton = Math.max(0, Math.min(selectedButton, mainMenuRouteLabels().size() - 1));
            sounds.requestMusic("MAIN_MENU", options);
        } else if (screen == Screen.OPTIONS) {
            graphicsDropdown = -1;
            selectedButton = Math.max(0, Math.min(optionsTab, 7));
            sounds.requestMusic("MAIN_MENU", options);
        } else if (screen == Screen.INTRO_CRAWL) {
            sounds.playIntroCrawlNarration(options);
        } else if (screen == Screen.CHARACTER && newGameSetupActive) {
            graphicsDropdown = -1;
            sounds.stopIntroCrawlNarration("entered new game setup");
            sounds.requestMusic("MAIN_MENU", options);
        } else if (screen == Screen.GAME || screen == Screen.PANEL || screen == Screen.INVENTORY || screen == Screen.CHARACTER || screen == Screen.INFO || screen == Screen.MAP || screen == Screen.KNOWLEDGE || screen == Screen.PAUSE) {
            graphicsDropdown = -1;
            sounds.stopIntroCrawlNarration("left intro crawl");
            sounds.requestMusic("LOW_HABITATION", options);
        }
    }
    Point screenPointToWorldTile(int mx, int my) {
        if (world == null) return new Point(Math.max(0, mx / 16), Math.max(0, my / 16));
        int tile = Math.max(1, lastWorldViewTileSize);
        int tx = lastWorldViewMinX + (mx - lastWorldViewOriginX) / tile;
        int ty = lastWorldViewMinY + (my - lastWorldViewOriginY) / tile;
        return new Point(Math.max(0, Math.min(world.w - 1, tx)), Math.max(0, Math.min(world.h - 1, ty)));
    }
    Point screenPointToAuditTile(int mx, int my) {
        if (auditWorld == null) return null;
        int tile = Math.max(1, lastAuditViewTileSize);
        int gx = mx - lastAuditViewOriginX;
        int gy = my - lastAuditViewOriginY;
        if (gx < 0 || gy < 0 || gx >= lastAuditViewCols * tile || gy >= lastAuditViewRows * tile) return null;
        int tx = lastAuditViewMinX + gx / tile;
        int ty = lastAuditViewMinY + gy / tile;
        return new Point(Math.max(0, Math.min(auditWorld.w - 1, tx)), Math.max(0, Math.min(auditWorld.h - 1, ty)));
    }
    void closePanel() {
        if (universalWindowAuthority != null && !universalWindowAuthority.focusedWindowId().isBlank()) {
            universalWindowAuthority.close(universalWindowAuthority.focusedWindowId(), turn, "panel closed");
        }
        panelMode = PanelMode.NONE;
        screen = world == null ? Screen.MENU : Screen.GAME;
    }
    void closeKnowledgeScreen() { screen = Screen.GAME; }
    void handleKnowledgeKeyPressed(int code) {}
    void cancelManualMovementPlan(String reason) {
        manualMovementPlanActive = false;
        manualMovementPlanHazardous = false;
        manualMovementPlanPath.clear();
        lookCursorActive = false;
        lastTargetingReport = "Manual movement plan canceled" + (reason == null || reason.isBlank() ? "." : ": " + reason + ".");
        repaint();
    }
    void openSectorAuditPanel() {
        if (auditWorld == null) rerollSectorAudit();
        selectedButton = 0;
        setScreen(Screen.SECTOR_AUDIT);
    }
    void rerollSectorAudit() {
        WorldSetupSettings setup = worldSetup == null ? WorldSetupSettings.standard() : worldSetup.copy();
        setup.zoneDensity = Math.max(0, Math.min(auditZoneDensityIndex, WorldSetupSettings.ZONE_DENSITY.length - 1));
        long auditSeed = (seed == 0L ? System.currentTimeMillis() : seed) ^ 0xA9D17L ^ (long)auditZoneTypeIndex * 65537L ^ (long)auditZoneDensityIndex * 8191L ^ System.nanoTime();
        java.awt.Dimension size = WorldGenerationApi.zoneSliceSize(auditSeed);
        World next = new World(auditSeed, size.width, size.height);
        ZoneType[] zones = ZoneType.values();
        next.zoneType = zones[Math.floorMod(auditZoneTypeIndex, zones.length)];
        ArrayList<SectorGenerationTraceAuthority.Step> captured = new ArrayList<>();
        try {
            SectorGenerationTraceAuthority.begin();
            try {
                next.generate();
            } finally {
                captured = SectorGenerationTraceAuthority.end();
            }
            auditWorld = next;
            auditTraceSteps.clear();
            auditTraceSteps.addAll(captured);
            auditSnapshot = SectorAuditRuntimeAuthority.analyze(auditWorld, setup, seed, auditSeed);
            auditFindingIndex = 0;
            resetAuditReplay(!auditTraceSteps.isEmpty());
            logEvent("Sector audit generated: " + auditWorld.zoneType.label + " " + auditWorld.w + "x" + auditWorld.h + " | replay steps " + auditTraceSteps.size() + ".");
        } catch (RuntimeException ex) {
            if (SectorGenerationTraceAuthority.active()) captured = SectorGenerationTraceAuthority.end();
            auditWorld = null;
            auditSnapshot = null;
            auditTraceSteps.clear();
            auditTraceSteps.addAll(captured);
            auditTracePlaying = false;
            logEvent("Sector audit generation failed: " + ex.getMessage());
        }
        repaint();
    }

    void resetAuditReplay(boolean playing) {
        auditTraceIndex = 0;
        auditTracePlaying = playing && !auditTraceSteps.isEmpty();
        auditTraceStartedMillis = System.currentTimeMillis();
        SectorGenerationTraceAuthority.Step step = auditTraceSteps.isEmpty() ? null : auditTraceSteps.get(0);
        if (step != null) {
            auditCursorX = step.x;
            auditCursorY = step.y;
        } else if (auditWorld != null) {
            auditCursorX = auditWorld.w / 2;
            auditCursorY = auditWorld.h / 2;
        }
    }

    void toggleAuditReplay() {
        if (auditTraceSteps.isEmpty()) return;
        auditTracePlaying = !auditTracePlaying;
        auditTraceStartedMillis = System.currentTimeMillis() - (long)Math.max(0, auditTraceIndex) * Math.max(120, auditTraceStepMillis);
        repaint();
    }

    void stepAuditReplay(int delta) {
        if (auditTraceSteps.isEmpty()) return;
        auditTracePlaying = false;
        auditTraceIndex = Math.max(0, Math.min(auditTraceSteps.size() - 1, auditTraceIndex + delta));
        SectorGenerationTraceAuthority.Step step = auditTraceSteps.get(auditTraceIndex);
        if (step != null) {
            auditCursorX = step.x;
            auditCursorY = step.y;
        }
        repaint();
    }

    void rewindAuditReplay() {
        if (auditTraceSteps.isEmpty()) return;
        auditTracePlaying = false;
        auditTraceIndex = 0;
        stepAuditReplay(0);
    }

    void finishAuditReplay() {
        if (auditTraceSteps.isEmpty()) return;
        auditTracePlaying = false;
        auditTraceIndex = auditTraceSteps.size() - 1;
        stepAuditReplay(0);
    }
    void cycleAuditZoneType(int delta) {
        auditZoneTypeIndex = Math.floorMod(auditZoneTypeIndex + delta, ZoneType.values().length);
        rerollSectorAudit();
    }
    void cycleAuditZoneDensity() {
        auditZoneDensityIndex = Math.floorMod(auditZoneDensityIndex + 1, WorldSetupSettings.ZONE_DENSITY.length);
        rerollSectorAudit();
    }
    void cycleAuditOverlay() {
        auditOverlayIndex = Math.floorMod(auditOverlayIndex + 1, SectorAuditRuntimeAuthority.OVERLAY_LABELS.length);
        logEvent("Audit overlay: " + auditOverlayLabel() + ".");
        repaint();
    }
    void jumpAuditFinding(int delta) {
        auditTracePlaying = false;
        int count = auditSnapshot == null ? 0 : auditSnapshot.findings.size();
        if (count <= 0) {
            auditFindingIndex = 0;
            repaint();
            return;
        }
        auditFindingIndex = Math.floorMod(auditFindingIndex + delta, count);
        SectorAuditRuntimeAuthority.AuditFinding finding = auditSnapshot.selected(auditFindingIndex);
        if (finding != null) {
            auditCursorX = finding.x;
            auditCursorY = finding.y;
        }
        repaint();
    }
    void moveAuditCursor(int dx, int dy) {
        if (auditWorld == null) return;
        auditTracePlaying = false;
        auditCursorX = Math.max(0, Math.min(auditWorld.w - 1, auditCursorX + dx));
        auditCursorY = Math.max(0, Math.min(auditWorld.h - 1, auditCursorY + dy));
        repaint();
    }
    String auditOverlayLabel() {
        return SectorAuditRuntimeAuthority.overlayLabel(auditOverlayIndex);
    }
    boolean isAssetInfopediaTab(int tab) { return screen == Screen.PANEL && panelMode == PanelMode.INFOPEDIA; }
    void backspaceInfopediaAssetFilter() {
        if (infopediaAssetFilter != null && !infopediaAssetFilter.isEmpty()) {
            infopediaAssetFilter = infopediaAssetFilter.substring(0, infopediaAssetFilter.length() - 1);
            infopediaSelectionIndex = 0;
            infopediaListScroll = 0;
            infopediaDetailScroll = 0;
            repaint();
        }
    }
    void cycleInfopediaTab(int delta) {
        int tabCount = SemanticAssetInfopediaAuthority.browseTypes().length + 1;
        setInfopediaTab(Math.floorMod(infopediaTab + delta, Math.max(1, tabCount)));
    }
    void setInfopediaTab(int tab) {
        int tabCount = SemanticAssetInfopediaAuthority.browseTypes().length + 1;
        infopediaTab = Math.max(0, Math.min(Math.max(0, tabCount - 1), tab));
        infopediaSelectionIndex = 0;
        infopediaListScroll = 0;
        infopediaDetailScroll = 0;
        activeScrollTag = "infopedia-list";
        repaint();
    }
    void moveInfopediaSelection(int delta) {
        java.util.List<String> entries = currentInfopediaEntries(selectedInfopediaAssetType());
        int count = entries == null ? 0 : entries.size();
        if (count <= 0) {
            infopediaSelectionIndex = 0;
        } else {
            infopediaSelectionIndex = Math.max(0, Math.min(count - 1, infopediaSelectionIndex + delta));
        }
        infopediaDetailScroll = 0;
        activeScrollTag = "infopedia-list";
        repaint();
    }
    boolean scrollActivePanel(int delta, boolean page) {
        if (screen == Screen.PANEL && panelMode == PanelMode.INFOPEDIA) {
            int step = Math.max(1, page ? 8 : 1);
            if ("infopedia-detail".equals(activeScrollTag)) {
                infopediaDetailScroll = Math.max(0, infopediaDetailScroll + delta * step);
            } else {
                moveInfopediaSelection(delta * step);
            }
            repaint();
            return true;
        }
        return false;
    }
    void cycleFireMode() {
        selectedFireModeIndex = Math.floorMod(selectedFireModeIndex + 1, 3);
        lastTargetingReport = "Fire mode: " + fireModeLabel() + ". " + targetingSolutionAt(combatX, combatY).summary;
        logEvent(lastTargetingReport);
    }

    void cycleEquippedWeaponHand() {
        boolean leftUsable = handItemUsableWeapon(equippedLeftHandItem);
        boolean rightUsable = handItemUsableWeapon(equippedRightHandItem);
        if (leftUsable && rightUsable) activeWeaponHandIndex = activeWeaponHandIndex == 0 ? 1 : 0;
        else if (leftUsable) activeWeaponHandIndex = 0;
        else activeWeaponHandIndex = 1;
        lastTargetingReport = "Active weapon: " + activeWeaponHandLabel() + " / " + activeWeaponName() + ". " + targetingSolutionAt(combatX, combatY).summary;
        logEvent(lastTargetingReport);
        repaint();
    }
    void throwSelectedExplosiveAtCursor() {
        if (world == null) {
            logEvent("Cannot throw an explosive without a loaded world.");
            return;
        }
        int targetX = panelMode == PanelMode.COMBAT ? combatX : lookX;
        int targetY = panelMode == PanelMode.COMBAT ? combatY : lookY;
        if (!world.inBounds(targetX, targetY)) {
            logEvent("Explosive target is outside the current world slice.");
            return;
        }
        setFacingToward(targetX, targetY, "explosive target");
        String explosive = firstInventoryMatch(true, false);
        if (explosive == null) {
            logEvent("No carried explosive is available.");
            return;
        }
        inventory.remove(explosive);
        ExplosiveProfile profile = ExplosiveProfile.forItem(explosive);
        world.mapObjects.add(MapObjectState.thrownExplosive(targetX, targetY, explosive, world.zoneType, turn + Math.max(1, profile.fuseTurns), world.tiles[targetX][targetY]));
        lastTargetingReport = "Thrown " + explosive + " to " + targetX + "," + targetY + " / fuse " + profile.fuseTurns + " turn(s).";
        logEvent(lastTargetingReport);
        advanceTurn("throws " + explosive + ".");
    }
    void throwSelectedPortableLight() {
        if (world == null) {
            logEvent("Cannot throw a portable light without a loaded world.");
            return;
        }
        String item = selectedInventoryItem();
        PortableLightProfile profile = PortableLightProfile.profile(item);
        if (item == null || profile == null) {
            logEvent("Select a carried portable light before throwing one.");
            return;
        }
        int targetX = lookCursorActive || panelMode == PanelMode.LOOK || panelMode == PanelMode.INTERACT || panelMode == PanelMode.COMBAT ? lookX : playerX;
        int targetY = lookCursorActive || panelMode == PanelMode.LOOK || panelMode == PanelMode.INTERACT || panelMode == PanelMode.COMBAT ? lookY : playerY;
        if (panelMode == PanelMode.COMBAT) { targetX = combatX; targetY = combatY; }
        targetX = Math.max(0, Math.min(world.w - 1, targetX));
        targetY = Math.max(0, Math.min(world.h - 1, targetY));
        int distance = Math.abs(targetX - playerX) + Math.abs(targetY - playerY);
        if (distance > Math.max(1, profile.throwRange) || !hasLineOfSight(playerX, playerY, targetX, targetY)) {
            targetX = playerX;
            targetY = playerY;
        }
        setFacingToward(targetX, targetY, "portable light throw");
        inventory.remove(selectedInventoryIndex);
        selectedInventoryIndex = Math.max(0, Math.min(selectedInventoryIndex, Math.max(0, inventory.size() - 1)));
        portableLights.add(new PortableLightInstance(item, targetX, targetY, profile.radius, turn + profile.remainingDuration(turn, item, 0), currentWorldKey(), "thrown"));
        activePortableLightItem = "";
        activePortableLightExpiresTurn = 0;
        activePortableLightWorn = false;
        lastPortableLightReport = "Thrown portable light: " + item + " at " + targetX + "," + targetY + " radius " + profile.radius + ".";
        logEvent(lastPortableLightReport);
        markLocalDirtyRegion("portable light thrown", targetX, targetY, profile.radius + 2, true, false, true, false);
        updateSensoryModel("portable light thrown");
        advanceTurn("throws " + item + ".");
        repaint();
    }
    void reloadCurrentRangedWeapon() {
        String weapon = equippedFirearmName();
        if (weapon == null) weapon = firstInventoryMatch(false, true);
        if (weapon == null) {
            logEvent("No ranged weapon is available to reload.");
            return;
        }
        int loaded = Math.max(0, loadedWeaponShots.getOrDefault(weapon, 0));
        int next = Math.max(loaded, 6);
        loadedWeaponShots.put(weapon, next);
        logEvent("Reloaded " + weapon + " to " + next + " shot(s) in the compatibility bridge.");
        advanceTurn("reloads " + weapon + ".");
    }
    void confirmCombatTarget() { confirmCombatTargetBody(); }
    void moveCombatCursor(int dx, int dy) { combatX += dx; combatY += dy; clampCombatCursorToWorld(); lookX = combatX; lookY = combatY; setFacingToward(combatX, combatY, "combat cursor"); lastTargetingReport = targetingSolutionAt(combatX, combatY).summary; }
    LegacyTargetingSolution targetingSolutionAt(int x, int y) {
        if (world == null || !world.inBounds(x, y)) return new LegacyTargetingSolution("No target selected.");
        int distance = Math.max(Math.abs(playerX - x), Math.abs(playerY - y));
        NpcEntity npc = world.npcAt(x, y);
        MapObjectState obj = world.mapObjectAt(x, y);
        String target = npc != null ? ("NPC " + npc.name + " / HP " + npc.hp)
                : obj != null ? ("object " + safeLabel(obj.label, obj.type))
                : ("tile " + world.tiles[x][y]);
        return new LegacyTargetingSolution("Target " + x + "," + y + " distance " + distance
                + " / " + target
                + " / weapon " + activeWeaponHandLabel() + ": " + activeWeaponName()
                + " / fire " + fireModeLabel() + ".");
    }
    void confirmInteraction() { confirmInteractionBody(); }
    void examineSelectedLookTarget() {
        setFacingToward(lookX, lookY, "look target");
        int depth = ProgressiveLookAuthority.advance(this, lookX, lookY);
        ArrayList<String> stack = ProgressiveLookAuthority.tileStackAt(this, lookX, lookY, depth);
        lastTargetingReport = stack.isEmpty() ? "No target data available." : stack.get(Math.max(0, Math.min(lookStackIndex, stack.size() - 1)));
        logEvent("Look " + lookX + "," + lookY + ": " + lastTargetingReport);
    }
    void moveInteractCursor(int dx, int dy) { lookX += dx; lookY += dy; clampInteractCursorToAdjacent(); setFacingToward(lookX, lookY, "interact cursor"); lookStackIndex = 0; lookStackScroll = 0; ProgressiveLookAuthority.reset(this, "interact cursor moved"); updatePendingInteractionSummary(); }
    ArrayList<String> tileStackAt(int x, int y) {
        return ProgressiveLookAuthority.tileStackAt(this, x, y);
    }
    boolean isDoorTile(char tile) { return TileDataCompilationAuthority.isDoorGlyph(tile); }
    void interactDoorAt(int x, int y, char tile) {
        if (world == null || !world.inBounds(x, y)) return;
        String label = switch (tile) {
            case '/' -> "open archway";
            case '|' -> "hinged scrap door";
            case 'L' -> "locked door";
            case 'X' -> "security door";
            case 'V' -> "vent panel";
            case 'D' -> "maintenance bulkhead";
            default -> "door";
        };
        if (tile == '/') {
            logEvent("Passed the " + label + " at " + x + "," + y + ".");
            advanceTurn("checks an open doorway.");
            return;
        }
        boolean opens = tile == '|'
                || (tile == 'L' && (hasInventoryLike("Lockpicks", "Tool bundle", "Mechanist Collegia tool roll", "Secure vault key") || stat("Agility", 5) + stat("Mechanics", 5) >= 14))
                || (tile == 'X' && (hasInventoryLike("Data spike", "Bank manager keycard", "Secure vault key") || stat("Intellect", 5) + stat("Mechanics", 5) >= 16))
                || (tile == 'V' && (hasInventoryLike("Tool bundle", "Mechanist Collegia tool roll") || stat("Mechanics", 5) >= 8))
                || (tile == 'D' && (hasInventoryLike("Tool bundle", "Mechanist Collegia tool roll", "Data spike") || stat("Strength", 5) + stat("Mechanics", 5) >= 15));
        if (opens) {
            world.tiles[x][y] = '/';
            markLocalDirtyRegion("door opened", x, y, 6, true, false, false, false);
            updateSensoryModel("door opened");
            logEvent("Opened " + label + " at " + x + "," + y + ".");
            advanceTurn("opens a door.");
        } else {
            logEvent("Could not open " + label + " at " + x + "," + y + ". It needs tools, credentials, or a better check.");
            advanceTurn("works at a sealed door.");
        }
        repaint();
    }
    void enforceEntityOccupancy(String reason) {}
    void moveBuildCursor(int dx, int dy) { buildX += dx; buildY += dy; clampBuildCursorToWorld(); }
    void confirmBuildPlacement() {
        String raw = rawCanPlacePendingBuildAt(buildX, buildY);
        if (!"ok".equalsIgnoreCase(raw)) {
            logEvent(constructionPlacementResult(pendingBuildRecipe, buildX, buildY, raw));
            return;
        }
        BuildRecipe recipe = pendingBuildRecipe;
        supplies -= Math.max(0, recipe.supplyCost);
        machineParts -= Math.max(0, recipe.partCost);
        consumeBuildComponents(recipe);
        BaseObject object = new BaseObject(recipe.name, recipe.symbol, buildX, buildY, recipe.supplyCost, recipe.attention);
        object.qualityName = recipe.qualityName;
        object.description = recipe.description;
        object.capacity = Math.max(1, recipe.supplyCost + recipe.partCost);
        object.integrity = Math.max(1, 4 + recipe.partCost + recipe.supplyCost / 2);
        object.faction = recipe.requiredFaction == null ? Faction.NONE : recipe.requiredFaction;
        configureBaseObject(object);
        baseObjects.add(object);
        buildPlacementActive = false;
        pendingBuildRecipe = null;
        rebuildItemContainersFromLegacyLists();
        logEvent("Built " + object.name + " at " + object.x + "," + object.y + ".");
        advanceTurn("builds " + object.name + ".");
        repaint();
    }
    void moveSelectedButton(int delta) { if (!buttons.isEmpty()) selectedButton = Math.floorMod(selectedButton + delta, buttons.size()); }
    void activateSelectedButtonUniversal() { UiModalButtonController.activateSelectedButton(this); }
    void cycleMovementMode() {
        int[] modes = { MOTION_SNEAK, MOTION_WALK, MOTION_RUN, MOTION_SPRINT };
        int idx = 1;
        for (int i = 0; i < modes.length; i++) if (modes[i] == selectedMovementModeIndex) idx = (i + 1) % modes.length;
        selectedMovementModeIndex = modes[idx];
        if (manualMovementPlanActive) refreshManualMovementPlanPath("movement mode changed");
        if (mouseMovePreviewActive) {
            updateMouseMovementPreviewTo(mouseMovePreviewTargetX, mouseMovePreviewTargetY);
        }
        if (world != null) {
            world.dirtyVisionRevision++;
            world.dirtyNoiseRevision++;
        }
        logEvent("Movement mode: " + movementModeLabel(selectedMovementModeIndex) + " / range " + movementModeRange()
                + " / vision " + signedValue(movementVisionRangeModifier()) + " / noise " + playerMovementNoiseSummary() + ".");
        repaint();
    }
    void beginManualMovementPlan() {
        manualMovementPlanActive = true;
        manualMovementPlanHazardous = false;
        lookCursorActive = true;
        lookX = playerX;
        lookY = playerY;
        manualMovementPlanPath.clear();
        lastTargetingReport = "Manual path planning started at " + lookX + "," + lookY + " with " + movementModeLabel(selectedMovementModeIndex) + " range " + movementModeRange() + ".";
        logEvent(lastTargetingReport);
        repaint();
    }
    void approachActiveInteractionTarget(int targetX, int targetY, String label) {
        InteractionApproachAuthority.ApproachPlan approach = InteractionApproachAuthority.plan(this, targetX, targetY, label);
        lastTargetingReport = approach.message();
        logEvent(lastTargetingReport);
        if (!approach.available() || approach.path().isEmpty()) {
            repaint();
            return;
        }
        manualMovementPlanActive = true;
        MovementPlanningAuthority.HazardRouteReadout approachHazards = MovementPlanningAuthority.inspectRouteHazards(world, approach.path());
        manualMovementPlanHazardous = approachHazards.hazardous();
        if (approachHazards.hazardous()) {
            lastTargetingReport = approach.message() + " " + approachHazards.summary();
            logEvent(approachHazards.summary());
        }
        lookCursorActive = true;
        interactCursorActive = false;
        lookX = approach.x();
        lookY = approach.y();
        manualMovementPlanPath.clear();
        manualMovementPlanPath.addAll(approach.path());
        panelMode = PanelMode.NONE;
        screen = Screen.GAME;
        repaint();
    }
    void waitOneTurn() { advanceTurnBody("waits."); }
    void beginLookMode() {
        interactCursorActive = false;
        lookCursorActive = true;
        lookX = playerX;
        lookY = playerY;
        lookStackIndex = 0;
        lookStackScroll = 0;
        ProgressiveLookAuthority.reset(this, "look mode opened");
        ArrayList<String> stack = tileStackAt(lookX, lookY);
        lastTargetingReport = stack.isEmpty() ? "No target data available." : stack.get(0);
        openPanel(PanelMode.LOOK);
    }
    void beginInteractMode() {
        panelMode = PanelMode.INTERACT;
        screen = Screen.PANEL;
        interactCursorActive = true;
        lookCursorActive = false;
        lookX = playerX + 1;
        lookY = playerY;
        clampInteractCursorToAdjacent();
        lookStackIndex = 0;
        lookStackScroll = 0;
        ProgressiveLookAuthority.reset(this, "interact mode opened");
        updatePendingInteractionSummary();
        noteUniversalWindowOpened(panelMode, "interact targeting");
    }
    void openPanel(PanelMode mode) {
        panelMode = mode == null ? PanelMode.NONE : mode;
        screen = mode == PanelMode.CHARACTER ? Screen.CHARACTER : Screen.PANEL;
        if (mode == PanelMode.LOOK) lookCursorActive = true;
        noteUniversalWindowOpened(mode, "open panel");
    }

    private void noteUniversalWindowOpened(PanelMode mode, String context) {
        if (universalWindowAuthority == null || mode == null) return;
        String id = switch (mode) {
            case INVENTORY -> "inventory";
            case CHARACTER -> "character";
            case CONTAINER -> "container";
            case TRADE -> "trade";
            case DIALOGUE -> "dialogue";
            case OBJECT -> "object";
            case LOOK, INTERACT, COMBAT -> "targeting";
            case BUILD -> "construction";
            case WORKBENCH -> "machine_operations";
            case CRAFTING -> "crafting";
            case MAP -> "map";
            case AUSPEX -> "auspex";
            case SCAVENGE -> "scavenge";
            case INFOPEDIA -> "infopedia";
            case CONSOLE -> "console";
            default -> "";
        };
        if (!id.isBlank()) universalWindowAuthority.open(id, turn, context);
    }
    void beginCombatTargeting() {
        panelMode = PanelMode.COMBAT;
        screen = Screen.PANEL;
        combatCursorActive = true;
        combatX = lookCursorActive ? lookX : playerX;
        combatY = lookCursorActive ? lookY : playerY;
        clampCombatCursorToWorld();
        lookX = combatX;
        lookY = combatY;
        ProgressiveLookAuthority.reset(this, "combat targeting opened");
        lastTargetingReport = targetingSolutionAt(combatX, combatY).summary;
        noteUniversalWindowOpened(panelMode, "combat targeting");
    }
    void beginExplosiveTargeting() {
        beginCombatTargeting();
        lastTargetingReport = "Explosive targeting armed. " + targetingSolutionAt(combatX, combatY).summary;
    }
    void queueOrExecuteMovementInput(int dx, int dy) {
        if (dx == 0 && dy == 0) return;
        setFacingFromDelta(dx, dy, "movement input");
        if (movementModeRange() <= 1) {
            int targetX = playerX + dx;
            int targetY = playerY + dy;
            if (movementCanEnter(targetX, targetY) && MovementPlanningAuthority.requiresHazardConfirmation(world, targetX, targetY)) {
                manualMovementPlanActive = true;
                lookCursorActive = true;
                lookX = targetX;
                lookY = targetY;
                refreshManualMovementPlanPath("hazardous direct movement");
                logEvent("Hazardous step held for confirmation. " + lastTargetingReport);
                repaint();
                return;
            }
            MovementExecutionAuthority.executeStep(this, dx, dy,
                    "direct-" + movementModeLabel(selectedMovementModeIndex).toLowerCase(Locale.ROOT), true);
            return;
        }
        beginDirectionalMovementPlan(dx, dy);
    }
    void sanityCheck(String phase) {}
    void nudgeManualMovementPlan(int dx, int dy) {
        lookX += dx;
        lookY += dy;
        clampLookCursorToWorld();
        refreshManualMovementPlanPath("manual nudge");
        repaint();
    }
    void confirmManualMovementPlan() {
        refreshManualMovementPlanPath("manual confirm");
        if (manualMovementPlanPath.isEmpty()) {
            manualMovementPlanActive = false;
            manualMovementPlanHazardous = false;
            lookCursorActive = false;
            logEvent(lastTargetingReport);
            repaint();
            return;
        }
        executeMovementPath(manualMovementPlanPath, "manual plan");
        manualMovementPlanActive = false;
        manualMovementPlanHazardous = false;
        manualMovementPlanPath.clear();
        repaint();
    }

    private void beginDirectionalMovementPlan(int dx, int dy) {
        manualMovementPlanActive = true;
        manualMovementPlanHazardous = false;
        lookCursorActive = true;
        int range = movementModeRange();
        lookX = playerX + Integer.signum(dx) * range;
        lookY = playerY + Integer.signum(dy) * range;
        clampLookCursorToWorld();
        refreshManualMovementPlanPath("directional movement input");
        if (manualMovementPlanPath.isEmpty()) {
            logEvent("No " + movementModeLabel(selectedMovementModeIndex) + " route is available in that direction.");
        } else {
            logEvent(lastTargetingReport);
        }
        repaint();
    }

    private void refreshManualMovementPlanPath(String source) {
        manualMovementPlanPath.clear();
        if (!manualMovementPlanActive || world == null) return;
        manualMovementPlanPath.addAll(buildMovementPathTo(lookX, lookY, movementModeRange()));
        MovementPlanningAuthority.HazardRouteReadout hazards = MovementPlanningAuthority.inspectRouteHazards(world, manualMovementPlanPath);
        manualMovementPlanHazardous = hazards.hazardous();
        MovementPlanningAuthority.MovementPlanReadout readout = MovementPlanningAuthority.describePlan(
                this, lookX, lookY, movementModeRange(), movementModeLabel(selectedMovementModeIndex));
        lastTargetingReport = readout.summary();
    }

    void clearMouseMovementPreview(String reason) {
        mouseMovePreviewActive = false;
        mouseMovePreviewValid = false;
        mouseMovePreviewHazardous = false;
        mouseMovePreviewPath.clear();
        mouseMovePreviewTargetX = playerX;
        mouseMovePreviewTargetY = playerY;
        repaint();
    }

    void updateMouseMovementPreviewTo(int x, int y) {
        mouseMovePreviewActive = true;
        mouseMovePreviewHazardous = false;
        mouseMovePreviewPath.clear();
        if (world == null) {
            mouseMovePreviewValid = false;
            return;
        }
        int tx = Math.max(0, Math.min(world.w - 1, x));
        int ty = Math.max(0, Math.min(world.h - 1, y));
        mouseMovePreviewTargetX = tx;
        mouseMovePreviewTargetY = ty;
        mouseMovePreviewPath.addAll(buildMovementPathTo(tx, ty, movementModeRange()));
        mouseMovePreviewValid = !mouseMovePreviewPath.isEmpty();
        MovementPlanningAuthority.HazardRouteReadout hazards = MovementPlanningAuthority.inspectRouteHazards(world, mouseMovePreviewPath);
        mouseMovePreviewHazardous = hazards.hazardous();
        if (mouseMovePreviewPath.isEmpty()) {
            lastTargetingReport = "No reachable mouse movement preview to " + tx + "," + ty + ".";
        } else {
            Point end = mouseMovePreviewPath.get(mouseMovePreviewPath.size() - 1);
            boolean exact = end.x == tx && end.y == ty;
            lastTargetingReport = (exact ? "Mouse movement route" : "Mouse movement partial route")
                    + " / " + movementModeLabel(selectedMovementModeIndex)
                    + " / " + mouseMovePreviewPath.size() + " tile(s)"
                    + " / endpoint " + end.x + "," + end.y + "."
                    + (hazards.hazardous() ? " " + hazards.summary() : "");
        }
    }

    void executeMouseMovementPreview() {
        if (!mouseMovePreviewActive) return;
        ArrayList<Point> route = new ArrayList<>(mouseMovePreviewPath);
        if (route.isEmpty()) {
            logEvent("Mouse movement preview has no reachable route.");
            clearMouseMovementPreview("empty mouse route");
            return;
        }
        executeMovementPath(route, "mouse movement preview");
        clearMouseMovementPreview("executed mouse movement preview");
    }

    private boolean executeMovementPath(java.util.List<Point> route, String source) {
        return MovementExecutionAuthority.executePlannedPath(this, route, source).success();
    }

    private ArrayList<Point> buildMovementPathTo(int targetX, int targetY, int maxSteps) {
        return MovementPlanningAuthority.buildPathTo(this, targetX, targetY, maxSteps);
    }

    private boolean[][] reachableMovementTiles(int maxSteps) {
        return MovementPlanningAuthority.reachableTiles(this, maxSteps);
    }

    private boolean movementCanEnter(int x, int y) {
        return MovementPlanningAuthority.canEnter(this, x, y);
    }

    private boolean movementPathReaches(java.util.List<Point> path, int targetX, int targetY) {
        return MovementPlanningAuthority.pathReaches(path, targetX, targetY);
    }

    private int movementModeRange() {
        return MovementPlanningAuthority.rangeForMode(selectedMovementModeIndex);
    }

    private int movementFatigueCost(int steps) {
        return MovementPlanningAuthority.fatigueCost(selectedMovementModeIndex, steps);
    }

    private java.awt.Color movementModeColor(int alpha) {
        return MovementPlanningAuthority.modeColor(selectedMovementModeIndex, alpha);
    }

    private void setFacingFromDelta(int dx, int dy, String source) {
        if (dx == 0 && dy == 0) return;
        int ndx = Math.abs(dx) >= Math.abs(dy) ? Integer.signum(dx) : 0;
        int ndy = Math.abs(dy) > Math.abs(dx) ? Integer.signum(dy) : 0;
        if (ndx == 0 && ndy == 0) return;
        if (facingDx == ndx && facingDy == ndy) return;
        facingDx = ndx;
        facingDy = ndy;
        refreshSensoryFacingState(source == null ? "facing changed" : source);
    }

    void setFacingToward(int tx, int ty, String source) {
        setFacingFromDelta(tx - playerX, ty - playerY, source);
    }

    private void refreshSensoryFacingState(String reason) {
        sensoryFacingDx = Integer.MIN_VALUE;
        sensoryFacingDy = Integer.MIN_VALUE;
        if (world != null) world.dirtyVisionRevision++;
    }

    private void startPlayerMotionTween(int fromX, int fromY, int toX, int toY, int steps, String source) {
        playerMotionFromX = fromX;
        playerMotionFromY = fromY;
        playerMotionToX = toX;
        playerMotionToY = toY;
        playerMotionStartedMillis = System.currentTimeMillis();
        int perStep = options != null && options.reducedMotion ? 120 : 420;
        int cap = options != null && options.reducedMotion ? 420 : 1800;
        playerMotionDurationMillis = Math.max(120, Math.min(cap, perStep * Math.max(1, steps)));
    }

    private boolean playerMotionAnimating() {
        return playerMotionDurationMillis > 0 && playerMotionStartedMillis > 0
                && System.currentTimeMillis() - playerMotionStartedMillis < playerMotionDurationMillis;
    }

    private boolean withinFacingVisionCone(int x, int y, int range) {
        int dx = x - playerX;
        int dy = y - playerY;
        if (dx == 0 && dy == 0) return true;
        return DirectionalVisionAuthority.contains(this, x, y, range);
    }

    private boolean withinFacingVisionConeVisual(int x, int y, int range, double originX, double originY) {
        return DirectionalVisionAuthority.containsAt(this, x, y, range, originX, originY);
    }

    private void invalidateMovementPlans(String reason) {
        if (manualMovementPlanActive) refreshManualMovementPlanPath(reason);
        if (mouseMovePreviewActive) {
            mouseMovePreviewActive = false;
            mouseMovePreviewValid = false;
            mouseMovePreviewPath.clear();
        }
    }
    void createNewInGameEditorEntry() {
        if (SimulationToolSuiteRegistry.MOD_PACKAGING_EDITOR.equals(inGameEditorName)) {
            inGameEditorStatus = "Choose an editor before creating a record.";
            repaint();
            return;
        }
        SimulationEditorRepository.EditableEntity entity = inGameEditorRepository.createBlankEntity(inGameEditorName);
        inGameEditorRepository.removeEntity(new SimulationEditorRepository.EntityRef(inGameEditorName, entity.id()));
        inGameEditorHistory.execute(new EditorCommand.CreateEntity(inGameEditorRepository, inGameEditorName, entity, inGameEditorEvents));
        java.util.List<SimulationEditorRepository.EditableEntity> entities = inGameEditorRepository.entities(inGameEditorName);
        inGameEditorEntityIndex = Math.max(0, entities.size() - 1);
        inGameEditorPropertyIndex = 0;
        SimulationEditorRepository.EntityRef ref = new SimulationEditorRepository.EntityRef(inGameEditorName, entity.id());
        inGameEditorRepository.setSelected(ref, true);
        inGameEditorStatus = "Created mod-scoped record " + entity.id() + ".";
        repaint();
    }
    void inGameEditorUndo() { inGameEditorHistory.undo(); inGameEditorStatus = "Undo applied. " + inGameEditorHistory.compactState(); repaint(); }
    void inGameEditorRedo() { inGameEditorHistory.redo(); inGameEditorStatus = "Redo applied. " + inGameEditorHistory.compactState(); repaint(); }

    void moveInventorySelection(int delta) { if (!inventory.isEmpty()) selectedInventoryIndex = Math.floorMod(selectedInventoryIndex + delta, inventory.size()); inventoryItemDescriptionScroll = 0; }
    void moveTargetInventorySelection(int delta) { if (!baseStorage.isEmpty()) selectedTargetInventoryIndex = Math.floorMod(selectedTargetInventoryIndex + delta, baseStorage.size()); inventoryItemDescriptionScroll = 0; }

    void beginWindowModeReconfigure() {}
    void endWindowModeReconfigure() {}
    void requestApplicationExit(String reason) {
        String why = reason == null ? "unspecified" : reason;
        logEvent("Exit requested: " + why);
        DebugLog.audit("CLIENT_EXIT", why);
        shutdownRuntime();
        java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (window != null) window.dispose();
        else System.exit(0);
    }
    String togglePerformanceDiagnostics() { return performanceDiagnostics.toggle(); }
    boolean toggleConsoleFlag(String key) { String safe = key == null ? "" : key; boolean next = !Boolean.TRUE.equals(consoleFlags.get(safe)); consoleFlags.put(safe, next); return next; }
    void setConsoleNumericFlag(String key, float value) { consoleNumericFlags.put(key == null ? "" : key, value); }
    void setConsoleStringFlag(String key, String value) { consoleStringFlags.put(key == null ? "" : key, value == null ? "" : value); }
    void healWorstBodyPart(int amount) { wounds = Math.max(0, wounds - Math.max(0, amount)); }
    void triggerPlayerDeath(String cause, String attacker, String weapon, String location) { lastDefeatCause = cause; lastDefeatAttacker = attacker; lastDefeatWeapon = weapon; lastDefeatLocation = location; runUnconsciousEvents++; }
    void writeSaveFile(int slot, boolean quick) {
        if (world == null) throw new IllegalStateException("No active world is loaded.");
        int safeSlot = slot == AUTOSAVE_HOURLY_SLOT || slot == AUTOSAVE_ZONE_SLOT ? slot : Math.max(1, Math.min(SAVE_SLOT_COUNT, slot));
        java.nio.file.Path path = SaveSlotSurfaceApi.savePathForSlot(safeSlot);
        try {
            java.nio.file.Files.createDirectories(path.getParent());
            Properties p = new Properties();
            Persistence.writeCore(this, p);
            try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(path)) {
                p.store(out, "The Mechanist " + SaveSlotSurfaceApi.slotLabel(safeSlot));
            }
            logEvent((quick ? "Quick-saved " : "Saved ") + SaveSlotSurfaceApi.slotLabel(safeSlot) + ".");
            DebugLog.audit("SAVE_LOAD_MENU", "wrote slot=" + safeSlot + " path=" + path);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Could not write " + SaveSlotSurfaceApi.slotLabel(safeSlot) + ": " + ex.getMessage(), ex);
        }
    }
    void shutdownRuntime() {
        if (timer != null) timer.stop();
        if (multiplayerMenu != null) multiplayerMenu.close();
    }

    void toggleTacticalSlate() { openPanel(PanelMode.INFO); }
    void openChatWindow() { openPanel(PanelMode.CONSOLE); }
    void openAuspexPanel() {
        openPanel(PanelMode.AUSPEX);
        updateSensoryModel("auspex opened");
        logEvent("Auspex panel opened.");
        repaint();
    }
    void openCraftingPanel() {
        selectedCraftingRecipe = selectedCraftingRecipe == null ? firstVisibleCraftingRecipe() : selectedCraftingRecipe;
        openPanel(PanelMode.CRAFTING);
        logEvent("Crafting panel opened.");
        repaint();
    }
    void openScavengePanel() {
        openPanel(PanelMode.SCAVENGE);
        logEvent("Scavenge panel opened.");
        repaint();
    }
    private CraftingRecipe firstVisibleCraftingRecipe() {
        for (CraftingRecipe recipe : CraftingRecipe.all()) if (recipe.visibleTo(this)) return recipe;
        return CraftingRecipe.noKnownRecipes();
    }
    private CraftingRecipe visibleCraftingRecipeMatching(ArrayList<CraftingRecipe> visible, CraftingRecipe previous) {
        if (visible == null || visible.isEmpty()) return CraftingRecipe.noKnownRecipes();
        if (previous != null && previous.name != null) {
            for (CraftingRecipe recipe : visible) if (previous.name.equals(recipe.name)) return recipe;
        }
        return visible.get(0);
    }
    private void craftSelectedRecipe() {
        CraftingRecipe recipe = selectedCraftingRecipe == null ? firstVisibleCraftingRecipe() : selectedCraftingRecipe;
        if (recipe == null || recipe.disabled) {
            logEvent("Crafting unavailable: no real recipe is selected.");
            repaint();
            return;
        }
        BaseObject machine = recipe.requiredMachine(this);
        String problem = recipe.blockingProblemForMachine(this, machine);
        if (problem != null) {
            logEvent("Cannot craft " + recipe.name + ": " + problem);
            repaint();
            return;
        }
        ProductionMaterialQualityAuthority.MaterialQuality materialQuality = ProductionMaterialQualityAuthority.evaluate(this, recipe);
        int materialTier = materialQuality.active() && materialQuality.complete() ? materialQuality.limitingTier() : -1;
        ProductionFacilityQualityAuthority.FacilityQuality facilityQuality = ProductionFacilityQualityAuthority.evaluate(this, machine);
        int facilityTier = facilityQuality.active() ? facilityQuality.tier() : -1;
        ProductionToolQualityAuthority.ToolQuality toolQuality = ProductionToolQualityAuthority.evaluate(this);
        int toolTier = toolQuality.active() ? toolQuality.tier() : -1;
        ProductionKnowledgeSourceAuthority.KnowledgeSource knowledgeSource = ProductionKnowledgeSourceAuthority.evaluate(
                this, machine, recipe.requiredKnowledge);
        ProductionQualityTraceAuthority.QualityTrace qualityTrace = ProductionQualityTraceAuthority.evaluate(
                knowledgeSource.effectiveKnowledge(), recipe.requiredKnowledge, machine == null ? "Common" : machine.qualityName, materialTier, facilityTier, toolTier);
        ProductionOperatorSkillAuthority.OperatorSkill operatorSkill = ProductionOperatorSkillAuthority.evaluate(this, recipe.xpSkill);
        recipe.consumeInputs(this);
        String quality = qualityTrace.outputQuality();
        ProductionRecipe production = ProductionRecipe.create(recipe.outputBaseItem, recipe.faction, quality, recipe.requiredKnowledge, recipe.machineName());
        String output = production.outputItemName();
        int count = Math.max(1, recipe.outputCount);
        String worker = active == null ? "player" : active.name;
        ProductionBatchAuthority.BatchDisposition batch = ProductionBatchAuthority.assess(
                production, machine, operatorSkill, rng, turn);
        for (int i = 0; i < count; i++) {
            addInventoryItem(output, ItemProvenanceRecord.produced(
                    production, machine, world, turn, worker, qualityTrace, operatorSkill, knowledgeSource, batch));
        }
        if (machine != null && recipe.machineWear > 0) machine.integrity = Math.max(0, machine.integrity - recipe.machineWear);
        fatigue = Math.min(MAX_FOOD_WATER, fatigue + ControlledProductionJobAuthority.manualFatigueCost(this, machine, recipe));
        int turns = ControlledProductionJobAuthority.manualTurnCost(this, machine, recipe);
        turn += Math.max(0, turns - 1);
        worldTurn += Math.max(0, turns - 1);
        gainXp(recipe.xpSkill, recipe.xpGain, "crafted " + recipe.name);
        rebuildItemContainersFromLegacyLists();
        logEvent("Crafted " + count + "x " + output + " from " + recipe.name + ".");
        logEvent(batch.lines().get(0) + " " + batch.lines().get(1));
        advanceTurn("crafts " + recipe.name + ".");
        repaint();
    }
    private ArrayList<MapObjectState> nearbyScavengeTargets(int radius) {
        ArrayList<MapObjectState> out = new ArrayList<>();
        if (world == null || world.mapObjects == null) return out;
        int max = Math.max(0, radius);
        for (MapObjectState object : world.mapObjects) {
            if (object == null || !WasteNewsprintScavengeAuthority.isSearchableContainer(object.type)) continue;
            int distance = Math.abs(object.x - playerX) + Math.abs(object.y - playerY);
            if (distance <= max) out.add(object);
        }
        out.sort(Comparator.comparingInt((MapObjectState object) -> Math.abs(object.x - playerX) + Math.abs(object.y - playerY))
                .thenComparing(object -> safeLabel(object.label, object.type)));
        return out;
    }
    private MapObjectState nearestScavengeTarget(int radius) {
        ArrayList<MapObjectState> targets = nearbyScavengeTargets(radius);
        return targets.isEmpty() ? null : targets.get(0);
    }
    private void searchNearestScavengeTarget() {
        MapObjectState target = nearestScavengeTarget(1);
        if (target == null) {
            logEvent("No adjacent searchable refuse/newsprint fixture is in reach.");
            repaint();
            return;
        }
        lookX = target.x;
        lookY = target.y;
        if (!FrontageFixtureInteractionAuthority.tryInteract(this, target.x, target.y)) {
            logEvent("Scavenge target did not accept the live frontage interaction route: " + WasteNewsprintScavengeAuthority.shortLabel(target) + ".");
        }
        updatePendingInteractionSummary();
        repaint();
    }
    void openInfopediaPanel(String reason) {
        panelMode = PanelMode.INFOPEDIA;
        screen = Screen.PANEL;
        activeScrollTag = "infopedia-list";
        selectedButton = 0;
        sounds.stopIntroCrawlNarration("opened infopedia");
        sounds.requestMusic(world == null ? "MAIN_MENU" : "LOW_HABITATION", options);
        logEvent("InfoPedia opened" + (reason == null || reason.isBlank() ? "." : ": " + reason + "."));
        DebugLog.audit("CLIENT_MENU_ROUTE", "infopedia panel opened reason=" + (reason == null ? "unspecified" : reason) + " tab=" + infopediaTab);
        repaint();
    }
    boolean handleInfopediaTyped(char ch) {
        if (screen != Screen.PANEL || panelMode != PanelMode.INFOPEDIA || !"infopedia-asset-filter".equals(activeScrollTag)) return false;
        if (Character.isISOControl(ch)) return false;
        if (infopediaAssetFilter == null) infopediaAssetFilter = "";
        if (infopediaAssetFilter.length() >= 80) return true;
        infopediaAssetFilter += ch;
        infopediaSelectionIndex = 0;
        infopediaListScroll = 0;
        infopediaDetailScroll = 0;
        return true;
    }
    boolean worldZoomControlActive() { return MapViewportOptionsSubsystem.worldZoomControlActive(this); }
    void changeWorldZoom(int delta, String source) { MapViewportOptionsSubsystem.changeWorldZoom(this, delta, source); }
    void continueFromIntroCrawl() { setScreen(Screen.MENU); logEvent("Intro crawl completed."); }
    void continueFromZoneSplash() { setScreen(Screen.GAME); }
    void finishBootSequence(String source) { setScreen(Screen.INTRO_CRAWL); logEvent("Boot sequence finished by " + source + "."); }
    void acceptEulaGate() { eulaGateActive = false; logEvent("EULA accepted."); }
    void scrollEulaGate(int delta, boolean page) { eulaScroll = Math.max(0, Math.min(Math.max(0, eulaMaxScroll), eulaScroll + delta * (page ? 10 : 1))); }
    void openKnowledgeMenu() {
        if (java.awt.GraphicsEnvironment.isHeadless()) {
            setScreen(Screen.KNOWLEDGE);
            logEvent("Knowledge menu unavailable in headless mode.");
            return;
        }
        if (knowledgeMenu != null && knowledgeMenu.isDisplayable()) {
            knowledgeMenu.refreshFromGameState();
            knowledgeMenu.toFront();
            knowledgeMenu.requestFocus();
            return;
        }
        java.awt.Window owner = javax.swing.SwingUtilities.getWindowAncestor(this);
        knowledgeMenu = new KnowledgeMenu(owner, new KnowledgeMenu.KnowledgeStateBridge() {
            @Override public int availableKnowledgePoints() { return knowledgeCredits; }
            @Override public Set<String> unlockedKnowledgeIds() { return new LinkedHashSet<>(unlockedKnowledges); }
            @Override public boolean unlockKnowledgeNode(KnowledgeTree tree, KnowledgeNode node) {
                if (tree == null || node == null || !tree.canUnlock(node.id())) return false;
                KnowledgeTree.UnlockResult result = tree.unlockNode(node.id());
                if (!result.success()) return false;
                knowledgeCredits = Math.max(0, result.remainingPoints());
                unlockedKnowledges.add(node.id());
                logEvent("Knowledge unlocked: " + node.name() + ".");
                DebugLog.audit("KNOWLEDGE_MENU", "unlocked=" + node.id() + " remaining=" + knowledgeCredits);
                repaint();
                return true;
            }
            @Override public void menuClosed() {
                knowledgeMenu = null;
                repaint();
            }
        });
        knowledgeMenu.setLocationRelativeTo(owner == null ? this : owner);
        knowledgeMenu.setVisible(true);
        logEvent("Knowledge menu opened.");
        DebugLog.audit("CLIENT_MENU_ROUTE", "knowledge dialog opened credits=" + knowledgeCredits + " unlocked=" + unlockedKnowledges.size());
    }
    Rectangle graphicsDropdownInnerRect() {
        Rectangle outer = graphicsDropdownOuterRect();
        int pad = 14;
        return new Rectangle(outer.x + pad, outer.y + pad, Math.max(1, outer.width - pad * 2), Math.max(1, outer.height - pad * 2));
    }
    int scaled(int value) { return value; }
    void clampInteractCursorToAdjacent() {
        lookX = Math.max(playerX - 1, Math.min(playerX + 1, lookX));
        lookY = Math.max(playerY - 1, Math.min(playerY + 1, lookY));
        clampLookCursorToWorld();
    }
    void clampLookCursorToWorld() {
        if (world == null) return;
        lookX = Math.max(0, Math.min(world.w - 1, lookX));
        lookY = Math.max(0, Math.min(world.h - 1, lookY));
    }
    void clampCombatCursorToWorld() {
        if (world == null) return;
        combatX = Math.max(0, Math.min(world.w - 1, combatX));
        combatY = Math.max(0, Math.min(world.h - 1, combatY));
    }
    String movementModeLabel(int mode) {
        return switch (mode) {
            case MOTION_SNEAK -> "SNEAK";
            case MOTION_RUN -> "RUN";
            case MOTION_SPRINT -> "SPRINT";
            case MOTION_WALK -> "WALK";
            default -> "WALK";
        };
    }
    private String signedValue(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }
    String fireModeLabel() {
        return switch (selectedFireModeIndex) {
            case 1 -> "AIMED";
            case 2 -> "BURST";
            default -> "SNAP";
        };
    }
    String safeLabel(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    String equippedFirearmName() {
        String active = activeWeaponName();
        if (ItemCatalog.isFirearmLike(active)) return active;
        if (ItemCatalog.isFirearmLike(equippedRightHandItem)) return equippedRightHandItem;
        if (ItemCatalog.isFirearmLike(equippedLeftHandItem)) return equippedLeftHandItem;
        return null;
    }

    String activeWeaponName() {
        String preferred = activeWeaponHandIndex == 0 ? equippedLeftHandItem : equippedRightHandItem;
        if (handItemUsableWeapon(preferred)) return preferred;
        String fallback = activeWeaponHandIndex == 0 ? equippedRightHandItem : equippedLeftHandItem;
        return handItemUsableWeapon(fallback) ? fallback : "Bare hands";
    }

    String activeWeaponHandLabel() {
        String preferred = activeWeaponHandIndex == 0 ? equippedLeftHandItem : equippedRightHandItem;
        if (handItemUsableWeapon(preferred)) return activeWeaponHandIndex == 0 ? "LEFT" : "RIGHT";
        String fallback = activeWeaponHandIndex == 0 ? equippedRightHandItem : equippedLeftHandItem;
        if (handItemUsableWeapon(fallback)) return activeWeaponHandIndex == 0 ? "RIGHT" : "LEFT";
        return "UNARMED";
    }

    boolean handItemUsableWeapon(String item) {
        ItemDef def = ItemCatalog.get(item);
        return def != null && def.weapon;
    }
    String firstInventoryMatch(boolean explosive, boolean firearm) {
        if (inventory == null) return null;
        for (String item : inventory) {
            if (explosive && ItemCatalog.isExplosiveLike(item)) return item;
            if (firearm && ItemCatalog.isFirearmLike(item)) return item;
        }
        return null;
    }
    void updatePendingInteractionSummary() {
        ArrayList<String> stack = tileStackAt(lookX, lookY);
        String top = stack.isEmpty() ? "empty target" : stack.get(0);
        lastTargetingReport = "Interaction target " + lookX + "," + lookY + " / " + top;
    }
    void auditItemLedgers(String reason) { lastItemLedgerAuditReport = "Item ledger audit: " + (reason == null ? "unspecified" : reason); }
    void migrateLegacyPhysicalScript(String reason) {}
    void rebuildItemContainersFromLegacyLists() { ensureContainer(CONTAINER_PLAYER_INVENTORY, "Player inventory"); ensureContainer(CONTAINER_BASE_STORAGE, "Base storage"); }
    void repairLegacyListsFromContainersIfNeeded() {}
    void initFactionState() {
        Faction[] core = {
            Faction.HIVER, Faction.SCAVENGER, Faction.BANDIT, Faction.CIVIC_WARDENS,
            Faction.IMPERIAL_GUARD, Faction.MECHANIST_COLLEGIA, Faction.NOBLE,
            Faction.CULTIST, Faction.MUTANT, Faction.CIVIC_LEDGER_OFFICE, Faction.INN
        };
        for (Faction f : core) {
            factionStanding.putIfAbsent(f, 0);
            factionMarketPressure.putIfAbsent(f, 0);
        }
        FactionStrategySimulationApi.ensurePlans(this);
    }

    void seedNpcFactionProductionSites() {
        ZoneType zone = world == null ? ZoneType.NEUTRAL_CIVILIAN_FLOOR : world.zoneType;
        LinkedHashSet<Faction> factions = new LinkedHashSet<>();
        factions.add(FactionInventoryStockAuthority.factionForZone(zone));
        factions.add(Faction.HIVER);
        factions.add(Faction.SCAVENGER);
        factions.add(Faction.BANDIT);
        factions.add(Faction.CIVIC_WARDENS);
        factions.add(Faction.IMPERIAL_GUARD);
        factions.add(Faction.MECHANIST_COLLEGIA);
        factions.add(Faction.NOBLE);
        factions.add(Faction.CULTIST);
        factions.add(Faction.MUTANT);
        for (Faction f : factions) siteForFaction(f, zone);
        DebugLog.audit("NPC_FACTION_SITE_SEED", "sites=" + npcFactionSites.size() + " zone=" + zone.label);
    }
    void configureBaseObject(BaseObject b) {
        if (b == null) return;
        if (b.description == null || b.description.isBlank()) b.description = "Built base object.";
        MachineTierAuthority.applyToConfiguredObject(b);
    }
    int activePortableLightRadius() {
        if (activePortableLightItem == null || activePortableLightItem.isBlank() || activePortableLightExpiresTurn <= turn) return 0;
        PortableLightProfile profile = PortableLightProfile.profile(activePortableLightItem);
        return profile == null ? 4 : Math.max(1, profile.radius);
    }
    boolean sameWorldLocation(String worldKey) {
        return worldKey == null || worldKey.isBlank() || worldKey.equals(currentWorldKey());
    }
    String currentWorldKey() {
        return world == null ? "" : world.zoneCoordText();
    }
    int portableLightIntensity(String itemName) {
        PortableLightProfile profile = PortableLightProfile.profile(itemName);
        if (profile == null) return 62;
        String key = profile.name.toLowerCase(Locale.ROOT);
        if (key.contains("flashlight") || key.contains("helmet")) return 86;
        if (key.contains("lantern") || key.contains("swamp")) return 72;
        if (key.contains("glow") || key.contains("phosphor")) return 58;
        if (key.contains("stub")) return 44;
        return 66;
    }
    int ambientLightLevelForWorld() {
        if (world == null || world.zoneType == null) return 4;
        String z = world.zoneType.name();
        if (world.sewerLayer || z.contains("SEWER") || z.contains("SUMP")) return 2;
        if (z.contains("NOBLE") || z.contains("GOVERNOR")) return 13;
        if (z.contains("CIVILIAN") || z.contains("ADMINISTRATUM") || z.contains("NEWS") || z.contains("ARBITES")) return 10;
        if (z.contains("MECHANICUS") || z.contains("FORGE") || z.contains("GUARD")) return 8;
        if (z.contains("GANGER") || z.contains("TRASH") || z.contains("MUTANT")) return 4;
        return 6;
    }

    void tickPassiveAmbientSounds() {
        if (world == null || options == null || !options.soundEnabled || options.sfxVolume <= 0) return;
        if (!(screen == Screen.GAME || screen == Screen.PANEL)) return;
        long now = System.currentTimeMillis();
        if (now - lastPassiveAmbientMillis < 8200L) return;
        lastPassiveAmbientMillis = now;
        PassiveAmbientCue cue = passiveAmbientCueForCurrentView();
        if (cue == null) return;
        sounds.playDistantCue(cue.key, cue.distance, options);
    }

    void tickSinglePlayerPassiveWorldTime() {
        if (world == null || options == null || !options.passiveSinglePlayerTicking()) return;
        if (!(screen == Screen.GAME || screen == Screen.PANEL)) return;
        if (multiplayerMenu != null && multiplayerMenu.hasActiveHost()) return;
        long now = System.currentTimeMillis();
        if (lastPassiveWorldTickMillis <= 0L) {
            lastPassiveWorldTickMillis = now;
            return;
        }
        if (now - lastPassiveWorldTickMillis < PASSIVE_TURN_INTERVAL_MILLIS) return;
        lastPassiveWorldTickMillis = now;
        advanceTurnBody(null);
        if (world != null) world.dirtyVisionRevision++;
        if (turn % 10 == 0) DebugLog.audit("PASSIVE_SINGLE_PLAYER_TIME", "turn=" + turn + " worldTurn=" + worldTurn + " mode=" + options.singlePlayerTickModeLabel());
    }

    long passiveTurnCountdownMillis(long nowMillis) {
        return passiveTurnCountdownMillis(options != null && options.passiveSinglePlayerTicking(), world != null
                && (screen == Screen.GAME || screen == Screen.PANEL), lastPassiveWorldTickMillis, nowMillis);
    }

    String passiveTurnCountdownLabel(long nowMillis) {
        long remaining = passiveTurnCountdownMillis(nowMillis);
        if (remaining < 0L) return "AUTO TURN: PAUSED";
        long tenths = (remaining + 99L) / 100L;
        return "AUTO TURN: " + (tenths / 10L) + "." + (tenths % 10L) + "s";
    }

    static long passiveTurnCountdownMillis(boolean passiveEnabled, boolean activeWorldScreen, long lastTickMillis, long nowMillis) {
        if (!passiveEnabled || !activeWorldScreen) return -1L;
        long anchor = lastTickMillis <= 0L ? nowMillis : lastTickMillis;
        return Math.max(0L, PASSIVE_TURN_INTERVAL_MILLIS - Math.max(0L, nowMillis - anchor));
    }

    PassiveAmbientCue passiveAmbientCueForCurrentView() {
        String nearby = nearbyAmbientCue();
        if (nearby != null) return new PassiveAmbientCue(nearby, 2);
        String z = world.zoneType == null ? "" : world.zoneType.name();
        if (world.sewerLayer || z.contains("SEWER") || z.contains("SUMP")) return new PassiveAmbientCue("ambient_sludge", 6);
        if (z.contains("MECHANICUS") || z.contains("FORGE") || z.contains("INDUSTR")) return new PassiveAmbientCue("ambient_press", 5);
        if (z.contains("NEWS") || z.contains("MARKET") || z.contains("INN")) return new PassiveAmbientCue("ambient_radio", 5);
        if (z.contains("NOBLE") || z.contains("GOVERNOR") || z.contains("CIVIC")) return new PassiveAmbientCue("ambient_chime", 6);
        if (z.contains("TRASH") || z.contains("MUTANT") || z.contains("WASTE")) return new PassiveAmbientCue("ambient_drip", 7);
        return new PassiveAmbientCue("ambient_vent", 7);
    }

    String nearbyAmbientCue() {
        if (world.mapObjects != null) {
            int bestDistance = Integer.MAX_VALUE;
            String best = null;
            for (MapObjectState object : world.mapObjects) {
                if (object == null) continue;
                int d = Math.abs(object.x - playerX) + Math.abs(object.y - playerY);
                if (d > 7 || d >= bestDistance) continue;
                String type = object.type == null ? "" : object.type.toLowerCase(Locale.ROOT);
                if (type.contains("broadcast") || type.contains("radio") || type.contains("news")) best = "ambient_radio";
                else if (type.contains("light")) best = "ambient_spark";
                else if (type.contains("terminal") || type.contains("bank")) best = "ambient_servo";
                else if (type.contains("water") || type.contains("recycler")) best = "ambient_pipe";
                else if (type.contains("machine") || type.contains("utility")) best = "ambient_machine";
                else if (type.contains("door") || type.contains("security")) best = "ambient_door_servo";
                if (best != null) bestDistance = d;
            }
            if (best != null) return best;
        }
        if (world.lightSources != null) {
            for (ZoneLightSourceRecord light : world.lightSources) {
                if (light != null && Math.abs(light.x - playerX) + Math.abs(light.y - playerY) <= 5) return "ambient_spark";
            }
        }
        return null;
    }

    record PassiveAmbientCue(String key, int distance) { }

    boolean hasLineOfSight(int x0, int y0, int x1, int y1) { return traceLightLine(x0, y0, x1, y1, true); }
    boolean hasKnowledge(String knowledge) { return knowledge == null || knowledge.isBlank() || unlockedKnowledges.contains(knowledge); }
    boolean hasProductionKnowledge(CraftingRecipe recipe, BaseObject machine) {
        return recipe == null || ProductionKnowledgeSourceAuthority.evaluate(this, machine, recipe.requiredKnowledge).available();
    }
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
    String cappedProductionQuality(BaseObject machine, String requiredKnowledge) {
        return ProductionQualityTraceAuthority.evaluate(unlockedKnowledges, requiredKnowledge,
                machine == null ? "Common" : machine.qualityName).outputQuality();
    }
    String cappedProductionQuality(BaseObject machine, CraftingRecipe recipe) {
        ProductionMaterialQualityAuthority.MaterialQuality materials = ProductionMaterialQualityAuthority.evaluate(this, recipe);
        int materialTier = materials.active() && materials.complete() ? materials.limitingTier() : -1;
        ProductionFacilityQualityAuthority.FacilityQuality facility = ProductionFacilityQualityAuthority.evaluate(this, machine);
        int facilityTier = facility.active() ? facility.tier() : -1;
        ProductionToolQualityAuthority.ToolQuality tool = ProductionToolQualityAuthority.evaluate(this);
        int toolTier = tool.active() ? tool.tier() : -1;
        ProductionKnowledgeSourceAuthority.KnowledgeSource knowledge = ProductionKnowledgeSourceAuthority.evaluate(
                this, machine, recipe == null ? null : recipe.requiredKnowledge);
        return ProductionQualityTraceAuthority.evaluate(knowledge.effectiveKnowledge(),
                recipe == null ? null : recipe.requiredKnowledge,
                machine == null ? "Common" : machine.qualityName, materialTier, facilityTier, toolTier).outputQuality();
    }
    int availableRecruitLabor() { return Math.max(0, factionRecruits.size()); }
    int stat(String statName, int fallback) {
        if (active != null && active.stats != null && statName != null) return Math.max(0, active.stats.getOrDefault(statName, fallback));
        return Math.max(0, fallback);
    }
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
    ItemProvenanceRecord peekProvenanceForItem(String item) {
        if (item == null || item.isBlank()) return null;
        ArrayDeque<ItemProvenanceRecord> q = itemProvenance.get(item);
        return q == null || q.isEmpty() ? null : q.peekFirst();
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

    void clampBuildCursorToWorld() {
        if (world == null) return;
        buildX = Math.max(0, Math.min(world.w - 1, buildX));
        buildY = Math.max(0, Math.min(world.h - 1, buildY));
    }
    String rawCanPlacePendingBuildAtUncached(int x, int y) { return rawCanPlacePendingBuildAt(x, y); }
    String rawCanPlacePendingBuildAt(int x, int y) {
        if (pendingBuildRecipe == null) return "no selected build";
        if (world == null) return "no loaded world";
        if (!world.inBounds(x, y)) return "outside world bounds";
        if (x == playerX && y == playerY) return "move off the target tile before building";
        if (!world.walkable(x, y)) return "tile is not walkable";
        if (world.npcAt(x, y) != null) return "tile is occupied by an NPC";
        if (world.mapObjectAt(x, y) != null) return "tile already contains a map object";
        if (baseObjectAt(x, y) != null) return "tile already contains a base object";
        if (supplies < pendingBuildRecipe.supplyCost) return "need " + pendingBuildRecipe.supplyCost + " supplies, have " + supplies;
        if (machineParts < pendingBuildRecipe.partCost) return "need " + pendingBuildRecipe.partCost + " machine parts, have " + machineParts;
        String componentProblem = buildComponentRequirementProblem(pendingBuildRecipe);
        if (componentProblem != null && !"ok".equalsIgnoreCase(componentProblem)) return componentProblem;
        String requirementProblem = buildRequirementProblem(pendingBuildRecipe);
        if (requirementProblem != null && !"ok".equalsIgnoreCase(requirementProblem)) return requirementProblem;
        return "ok";
    }
    String constructionPlacementResult(BuildRecipe recipe, int x, int y, String raw) {
        if (raw == null || raw.isBlank() || "ok".equalsIgnoreCase(raw)) return "ok";
        String blueprint = constructionBlueprintFor(recipe);
        new ConstructionGovernanceAuthority().explainPlacementResult(blueprint, false, raw, "build cursor " + x + "," + y);
        return ActionDenialGuidanceAuthority.explain(ActionDenialGuidanceAuthority.DenialKind.CONSTRUCTION, raw);
    }
    String constructionBlueprintFor(BuildRecipe recipe) { return recipe == null ? "unselected blueprint" : safeLabel(recipe.name, "construction") + " blueprint"; }
    String buildComponentRequirementProblem(BuildRecipe recipe) {
        if (recipe == null || recipe.componentCosts.isEmpty()) return "ok";
        ArrayList<String> missing = new ArrayList<>();
        for (Map.Entry<String,Integer> e : recipe.componentCosts.entrySet()) {
            if (countProductionInput(e.getKey()) < e.getValue()) missing.add(e.getValue() + "x " + e.getKey());
        }
        return missing.isEmpty() ? "ok" : "missing components: " + String.join(", ", missing);
    }
    String buildRequirementProblem(BuildRecipe recipe) {
        if (recipe == null) return "no selected build";
        if (recipe.requiredKnowledge != null && !recipe.requiredKnowledge.isBlank() && !hasKnowledge(recipe.requiredKnowledge)) return "missing knowledge: " + recipe.requiredKnowledge;
        if (recipe.requiresWorkbench && firstBaseObject('w') == null) return "requires a Scrap Workbench";
        return "ok";
    }
    void consumeBuildComponents(BuildRecipe recipe) {
        if (recipe == null) return;
        for (Map.Entry<String,Integer> e : recipe.componentCosts.entrySet()) {
            for (int i = 0; i < e.getValue(); i++) consumeInventoryNamed(e.getKey());
        }
    }

    int currentInnDay() { return Math.max(0, turn / Math.max(1, TURNS_PER_HOUR * HOURS_PER_DAY)); }
    NpcFactionSite siteForFaction(Faction faction, ZoneType zoneType) {
        ZoneType zone = zoneType == null ? (world == null ? ZoneType.NEUTRAL_CIVILIAN_FLOOR : world.zoneType) : zoneType;
        Faction requested = faction == null || faction == Faction.NONE ? FactionInventoryStockAuthority.factionForZone(zone) : faction;
        Faction normalized = FactionInventoryStockAuthority.normalizeFaction(requested);
        NpcFactionSite anyMatch = null;
        for (NpcFactionSite site : npcFactionSites) {
            if (site == null) continue;
            Faction sf = FactionInventoryStockAuthority.normalizeFaction(site.faction);
            if (sf != normalized) continue;
            if (world != null && site.sectorX == world.sectorX && site.sectorY == world.sectorY
                    && site.zoneX == world.zoneX && site.zoneY == world.zoneY && site.floor == world.floor) return site;
            if (anyMatch == null) anyMatch = site;
        }
        if (anyMatch != null) return anyMatch;

        Random r = new Random((seed == 0L ? System.currentTimeMillis() : seed)
                ^ normalized.name().hashCode() * 1103515245L
                ^ zone.name().hashCode() * 2654435761L);
        ArrayList<String> stock = FactionInventoryStockAuthority.accessibleStock(normalized, zone, r);
        String primary = stock.isEmpty() ? "Emergency rations" : stock.get(0);
        String secondary = stock.size() < 2 ? "Water bottle" : stock.get(1);
        int sx = world == null ? (atlas == null ? 1 : atlas.sectorX) : world.sectorX;
        int sy = world == null ? (atlas == null ? 1 : atlas.sectorY) : world.sectorY;
        int zx = world == null ? (atlas == null ? 2 : atlas.zoneX) : world.zoneX;
        int zy = world == null ? (atlas == null ? 2 : atlas.zoneY) : world.zoneY;
        int fl = world == null ? (atlas == null ? 4 : atlas.floor) : world.floor;
        String facility = factionFacilityType(normalized, zone);
        String siteName = normalized.label + " " + facility + " " + sx + "." + sy + "." + zx + "." + zy + "." + fl;
        NpcFactionSite site = NpcFactionSite.create(siteName, normalized, facility, sx, sy, zx, zy, fl, primary, secondary, "runtime faction inventory seed");
        site.stock = Math.max(site.stock, 6 + stock.size());
        site.lastProductionTurn = Math.max(0, turn);
        npcFactionSites.add(site);
        ensureContainer(factionStockContainerId(site), site.name + " strategic stock");
        DebugLog.audit("NPC_FACTION_SITE_CREATE", site.summaryLine());
        return site;
    }

    void addFactionMarketPressure(Faction faction, int pressure, String reason) {
        if (faction == null || faction == Faction.NONE || pressure == 0) return;
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        int next = Math.max(0, Math.min(999, factionMarketPressure.getOrDefault(f, 0) + pressure));
        factionMarketPressure.put(f, next);
        lastFactionSimulationReport = "market pressure " + f.label + "=" + next + " from " + (reason == null ? "unspecified faction event" : reason);
        DebugLog.audit("FACTION_MARKET_PRESSURE", "faction=" + f.label + " pressure=" + next + " delta=" + pressure + " reason=" + reason);
    }

    String factionFacilityType(Faction faction, ZoneType zone) {
        Faction f = FactionInventoryStockAuthority.normalizeFaction(faction);
        if (f == Faction.MECHANIST_COLLEGIA) return "machine shop";
        if (f == Faction.IMPERIAL_GUARD) return "munition depot";
        if (f == Faction.CIVIC_WARDENS) return "authorized stores cage";
        if (f == Faction.NOBLE) return "house commissary";
        if (f == Faction.CULTIST || f == Faction.HERETIC) return "hidden reliquary";
        if (f == Faction.MUTANT) return "sump cache";
        if (f == Faction.INN) return "newsroom stores";
        if (zone == ZoneType.NEUTRAL_RAIL_DEPOT || zone == ZoneType.TRAIN_SERVICE_YARD) return "rail freight desk";
        if (zone == ZoneType.SUMP_MARKET) return "market lockup";
        return "stockroom";
    }

    void tickFactionRuntimeSystems(boolean sleeping) {
        if (world == null) return;
        FactionStrategySimulationApi.tick(this, sleeping);
        tickNpcFactionSiteProduction();
    }

    void tickPopulationEconomyRuntimeSystems(boolean sleeping) {
        if (world == null) return;
        EconomyRuntimeState economy = ZoneEconomyInitializationManager.stateFor(world);
        int locationKey = WorldEconomyInitializationAuthority.locationKey(world);
        if (!economy.isInitialized(locationKey)) {
            WorldEconomyInitializationAuthority.Result init = WorldEconomyInitializationAuthority.apply(world, rng, economy);
            lastEconomyRuntimeReport = init.summary();
        }
        EconomyRuntimeState.ExpansionTickResult expansion = ZoneEconomyInitializationManager.slowGameplayExpansionTick(world, worldTurn, rng);
        if (expansion.applied()) {
            lastEconomyRuntimeReport = expansion.summary();
            DebugLog.audit("ECONOMY_RUNTIME_TICK", expansion.summary());
        } else if (lastEconomyRuntimeReport == null || lastEconomyRuntimeReport.startsWith("No persistent economy")) {
            lastEconomyRuntimeReport = ZoneEconomyInitializationManager.summary(world);
        }

        NpcNeedsDutyRuntimeAuthority.TickResult npcTick = NpcNeedsDutyRuntimeAuthority.tick(this, sleeping);
        lastNpcRuntimeReport = npcTick.summary();
        if (npcTurnBudgetScheduler.shouldAudit(turn)) {
            DebugLog.audit("NPC_NEEDS_DUTY_TICK", lastNpcRuntimeReport);
        }

        int abstractBudget = Math.max(1, npcTick.deferredBudget);
        if (sleeping) abstractBudget += 4;
        AbstractDistantZoneSimulation.TickResult abstractTick = abstractDistantZoneSimulation.spend(world, playerX, playerY, turn, abstractBudget);
        lastAbstractDistantZoneReport = abstractTick.summary;
        if (abstractDistantZoneSimulation.shouldAudit(turn)) {
            DebugLog.audit("ABSTRACT_DISTANT_ZONE_TICK", "budget=" + abstractBudget + " " + abstractTick.summary);
        }
    }

    void tickNpcFactionSiteProduction() {
        if (turn <= 0 || turn % TURNS_PER_HOUR != 0 || npcFactionSites.isEmpty()) return;
        int produced = 0;
        for (NpcFactionSite site : npcFactionSites) {
            if (site != null && site.produceHour(turn, rng)) produced++;
        }
        if (produced > 0) {
            DebugLog.audit("NPC_FACTION_SITE_PRODUCTION", "sitesProduced=" + produced + " turn=" + turn + " sites=" + npcFactionSites.size());
        }
    }
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
    private final SoundManager manager = new SoundManager();
    private boolean loaded;

    private synchronized void ensureLoaded() {
        if (loaded) return;
        try {
            manager.load();
        } catch (Throwable t) {
            DebugLog.error("AUDIO", "Audio manager failed to load; continuing silently where assets/devices are unavailable.", t);
        }
        loaded = true;
    }

    void play(String key, GameOptions options) {
        ensureLoaded();
        manager.play(key, options);
    }

    void playDistantCue(String key, int distance, GameOptions options) {
        ensureLoaded();
        manager.playDistantCue(key, distance, options);
    }

    void playIntroCrawlNarration(GameOptions options) {
        ensureLoaded();
        manager.playIntroCrawlNarration(options);
    }

    void stopIntroCrawlNarration(String reason) {
        ensureLoaded();
        manager.stopIntroCrawlNarration(reason == null ? "unspecified" : reason);
    }

    void requestMusic(String key, GameOptions options) {
        ensureLoaded();
        manager.requestMusic(key, options);
    }

    void setMusicVolume(GameOptions options) {
        ensureLoaded();
        manager.setMusicVolume(options);
    }

    void stopMusic(String reason) {
        ensureLoaded();
        manager.stopMusic();
        DebugLog.audit("AUDIO", "Music stop requested by bridge: " + (reason == null ? "unspecified" : reason));
    }

    String auditSummary() {
        ensureLoaded();
        return "soundHandles=" + manager.sounds.size() + " generatedCueFallbacks=" + manager.generatedCueKeys.size() + " playlists=" + manager.music.playlistSummary();
    }
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
    private final ImageCache media = new ImageCache();
    private final HashMap<String, BufferedImage> compiledSystemIconCache = new HashMap<>();
    private boolean loaded;
    private GameOptions lastOptions;

    void reloadArtQuality(GameOptions options) {
        loaded = false;
        lastOptions = options;
        clearMediaCaches();
        ensureLoaded(options);
    }

    BufferedImage get(String key) {
        if (key == null || key.isBlank()) return null;
        ensureLoaded(lastOptions);
        return media.cache.get(key);
    }

    BufferedImage getTile(char tile) {
        ensureLoaded(lastOptions);
        return media.tileArt.getRegistry().getTile(tile);
    }

    BufferedImage getTile(String semanticOrAlias, char fallbackGlyph) {
        ensureLoaded(lastOptions);
        return media.getTile(semanticOrAlias, fallbackGlyph);
    }

    BufferedImage getSemanticAssetImage(String assetId) {
        ensureLoaded(lastOptions);
        return media.getSemanticAssetImage(assetId);
    }

    BufferedImage getCompiledSystemIcon(int sheet, int row, int col) {
        ensureLoaded(lastOptions);
        String cacheKey = sheet + ":" + row + ":" + col;
        if (compiledSystemIconCache.containsKey(cacheKey)) return compiledSystemIconCache.get(cacheKey);
        LinkedHashSet<Integer> resolutions = new LinkedHashSet<>();
        int propertyResolution = media.assetResolutionProperty();
        if (propertyResolution > 0) resolutions.add(propertyResolution);
        if (lastOptions != null) resolutions.add(lastOptions.artQualityResolution());
        resolutions.add(32);
        resolutions.add(64);
        resolutions.add(128);
        resolutions.add(256);
        for (int resolution : resolutions) {
            if (resolution <= 0) continue;
            String filename = String.format(Locale.US, "FRAMEDSystem_%d_r%02dc%02d_%dpx.png", sheet, row, col, resolution);
            BufferedImage img = readCompiledSystemIcon(resolution, filename);
            if (img != null) {
                compiledSystemIconCache.put(cacheKey, img);
                return img;
            }
        }
        compiledSystemIconCache.put(cacheKey, null);
        return null;
    }

    BufferedImage getMapObjectImage(MapObjectState object) {
        ensureLoaded(lastOptions);
        String id = ObjectSemanticAssetAuthority.assetIdForMapObject(object);
        BufferedImage img = media.getSemanticAssetImage(id);
        return img != null ? img : ObjectSemanticAssetAuthority.imageForAssetId(id);
    }

    BufferedImage getBaseObjectImage(BaseObject object) {
        ensureLoaded(lastOptions);
        String id = ObjectSemanticAssetAuthority.assetIdForBaseObject(object);
        BufferedImage img = media.getSemanticAssetImage(id);
        return img != null ? img : ObjectSemanticAssetAuthority.imageForAssetId(id);
    }

    BufferedImage getNpcPortraitFor(Object npc) {
        ensureLoaded(lastOptions);
        if (npc instanceof NpcEntity entity) return media.getNpcPortraitFor(entity);
        return media.getNpcPortrait(npc == null ? 0 : Math.abs(npc.hashCode()));
    }

    BufferedImage getPlayerPortrait(Candidate candidate) {
        ensureLoaded(lastOptions);
        if (candidate == null) return media.getPortrait(ImageCache.PLAYER_BASELINE_HUMAN_POOL, 0);
        return media.getPortrait(candidate.portraitSheet, candidate.portraitIndex);
    }

    BufferedImage getItemIcon(String itemName) {
        ensureLoaded(lastOptions);
        return media.getItemIcon(itemName);
    }

    java.util.ArrayList<String> loadIntroCrawlLines() {
        ensureLoaded(lastOptions);
        return media.loadIntroCrawlLines();
    }

    private void ensureLoaded(GameOptions options) {
        if (loaded) return;
        clearMediaCaches();
        media.load(options == null ? new GameOptions() : options);
        loaded = true;
    }

    private void clearMediaCaches() {
        media.cache.clear();
        media.bootFrames.clear();
        media.portraitSheets.clear();
        media.playerHumanPortraitCells.clear();
        media.npcPortraitCells.clear();
        media.nameLockedProfilePortraits.clear();
        media.npcPortraitRanges.clear();
        media.portraitProfiles.clear();
        media.semanticAssetImageCache.clear();
        compiledSystemIconCache.clear();
    }

    private BufferedImage readCompiledSystemIcon(int resolution, String filename) {
        java.util.List<java.nio.file.Path> candidates = java.util.List.of(
                java.nio.file.Paths.get("assets", "compiled_assets", resolution + "px", "SYSTEM", filename),
                java.nio.file.Paths.get("PACKAGE_client", "assets", "compiled_assets", resolution + "px", "SYSTEM", filename),
                java.nio.file.Paths.get("client", "assets", "compiled_assets", resolution + "px", "SYSTEM", filename),
                java.nio.file.Paths.get("ROOT_tools", "atlas_asset_pipeline", "compiled_assets", resolution + "px", "SYSTEM", filename),
                java.nio.file.Paths.get("assets", "graphics", "packages", "default_" + resolution, "SYSTEM", filename),
                java.nio.file.Paths.get("PACKAGE_client", "assets", "graphics", "packages", "default_" + resolution, "SYSTEM", filename)
        );
        for (java.nio.file.Path path : candidates) {
            try {
                if (java.nio.file.Files.exists(path)) return javax.imageio.ImageIO.read(path.toFile());
            } catch (Throwable t) {
                DebugLog.error("SYSTEM_BUTTON_ICON", "Failed to load compiled system button icon " + path, t);
            }
        }
        return null;
    }
}

final class LegacyRenderStressTest {
    private boolean active;
    String toggle(LegacyFrameLimiter limiter) { active = !active; return active ? "Render stress test active." : "Render stress test inactive."; }
    boolean active() { return active; }
}

final class LightFalloffProfile {
    private final double near;
    private final double mid;
    private final double far;
    private final double tail;

    private LightFalloffProfile(double near, double mid, double far, double tail) {
        this.near = near;
        this.mid = mid;
        this.far = far;
        this.tail = tail;
    }

    double factor(double norm) {
        double n = Math.max(0.0, Math.min(1.0, norm));
        if (n < 0.22) return lerp(near, mid, n / 0.22);
        if (n < 0.58) return lerp(mid, far, (n - 0.22) / 0.36);
        double t = (n - 0.58) / 0.42;
        double smooth = t * t * (3.0 - 2.0 * t);
        return lerp(far, tail, smooth);
    }

    static LightFalloffProfile forProfile(String profile) {
        String s = profile == null ? "" : profile.toLowerCase(Locale.ROOT);
        if (s.contains("street")) return new LightFalloffProfile(1.00, 0.78, 0.46, 0.18);
        if (s.contains("flashlight") || s.contains("helmet") || s.contains("electrician") || s.contains("intrinsic")) return new LightFalloffProfile(1.00, 0.64, 0.30, 0.08);
        if (s.contains("lantern") || s.contains("swamp") || s.contains("sump") || s.contains("sewer") || s.contains("lower")) return new LightFalloffProfile(0.92, 0.76, 0.48, 0.20);
        if (s.contains("noble") || s.contains("civilian") || s.contains("civic")) return new LightFalloffProfile(1.00, 0.82, 0.54, 0.16);
        return new LightFalloffProfile(1.00, 0.74, 0.44, 0.12);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * Math.max(0.0, Math.min(1.0, t));
    }
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

























