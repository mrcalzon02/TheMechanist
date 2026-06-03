$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$basePath = Join-Path $root 'src\mechanist\LegacyPanelBridgeBase.java'
$panelPath = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
if (-not (Test-Path -LiteralPath $basePath -PathType Leaf)) { throw "Missing $basePath" }
if (-not (Test-Path -LiteralPath $panelPath -PathType Leaf)) { throw "Missing $panelPath" }

function Insert-BeforeLastBrace([string]$text, [string]$insert) {
    $idx = $text.LastIndexOf('}')
    if ($idx -lt 0) { throw 'Could not find final brace.' }
    return $text.Substring(0, $idx) + $insert + $text.Substring($idx)
}

function Insert-BeforePattern([string]$text, [string]$pattern, [string]$insert) {
    $idx = $text.IndexOf($pattern, [System.StringComparison]::Ordinal)
    if ($idx -lt 0) { throw "Could not find insertion pattern: $pattern" }
    return $text.Substring(0, $idx) + $insert + $text.Substring($idx)
}

$base = Get-Content -LiteralPath $basePath -Raw
$base = $base.Replace('profile.name()', 'profile.compactLabel()')
if ($base -notmatch 'void drawPanelBox\(Graphics2D g, int x, int y, int w, int h, String title\)') {
    $insert = @'

    void drawPanelBox(Graphics2D g, int x, int y, int w, int h, String title) {
        if (g == null) return;
        g.drawRect(x, y, Math.max(1, w), Math.max(1, h));
        if (title != null && !title.isBlank()) g.drawString(title, x + 12, y + 22);
    }
    void drawTextPanel(Graphics2D g, int x, int y, int w, int h, java.util.List<String> lines, boolean highlighted) {
        drawPanelBox(g, x, y, w, h, null);
        if (g == null || lines == null) return;
        int yy = y + 22;
        for (String line : lines) {
            if (yy > y + h - 8) break;
            drawUiTextLine(g, line, x + 12, yy);
            yy += Math.max(12, g.getFontMetrics().getHeight());
        }
    }
    void drawUiTextLine(Graphics2D g, String line, int x, int y) {
        if (g != null && line != null) g.drawString(line, x, y);
    }
    Rectangle graphicsDropdownOuterRect() { return new Rectangle(Math.max(20, getWidth()/2 - 220), 96, 440, 260); }
'@
    $base = Insert-BeforePattern $base "`r`n}`r`n`r`nfinal class LegacyLauncherRuntime" $insert
}
Set-Content -LiteralPath $basePath -Value $base

$panel = Get-Content -LiteralPath $panelPath -Raw
$panel = $panel.Replace('    static final int HOURS_PER_DAY = 24;`r`n', '    static final int HOURS_PER_DAY = 24;`r`n    static final int MAX_FOOD_WATER = 100;`r`n')
$panel = $panel.Replace('    static final int HOURS_PER_DAY = 24;`n', '    static final int HOURS_PER_DAY = 24;`n    static final int MAX_FOOD_WATER = 100;`n')
$panel = $panel.Replace('    World world;`r`n    LegacyPanelAtlas atlas;', '    World world;`r`n    WorldAtlas atlas;')
$panel = $panel.Replace('    World world;`n    LegacyPanelAtlas atlas;', '    World world;`n    WorldAtlas atlas;')
$panel = $panel.Replace('    final HashSet<String> visitedZoneTypes = new HashSet<>();', '    final HashSet<ZoneType> visitedZoneTypes = new HashSet<>();')
$panel = $panel.Replace('    int worldTurn;', '    long worldTurn;')

if ($panel -notmatch 'WorldSetupSettings worldSetup') {
    $fields = @'
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
    final LinkedHashMap<String, ItemProvenanceRecord> itemProvenance = new LinkedHashMap<>();
    final LinkedHashMap<Faction, Integer> factionStanding = new LinkedHashMap<>();
    final LinkedHashMap<Faction, Integer> temporaryHostileTurns = new LinkedHashMap<>();
    final LinkedHashMap<Faction, Integer> factionMarketPressure = new LinkedHashMap<>();
    final LinkedHashMap<String, Integer> bankBalances = new LinkedHashMap<>();
    final LinkedHashMap<Integer, Integer> scavengeCooldownUntilTurn = new LinkedHashMap<>();
    final HashSet<String> openBankAccounts = new HashSet<>();
    final HashSet<String> lootedBankVaultIds = new HashSet<>();
    final HashSet<String> disabledBankAlarmPanelIds = new HashSet<>();
'@
    $panel = $panel.Replace('    final HashMap<String, String> consoleStringFlags = new HashMap<>();', '    final HashMap<String, String> consoleStringFlags = new HashMap<>();' + "`r`n" + $fields)
}

if ($panel -notmatch 'void clampInteractCursorToAdjacent\(\)') {
    $methods = @'
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
    float portableLightIntensity(String itemName) { return 1.0f; }
    int ambientLightLevelForWorld() { return 50; }
'@
    $panel = $panel.Replace('    int scaled(int value) { return value; }', '    int scaled(int value) { return value; }' + "`r`n" + $methods)
}

$panel = $panel.Replace('final class LegacyMultiplayerMenu {`r`n    boolean handleKeyPressed(int code) { return false; }`r`n    void endDirectEdit() {}`r`n}', @'
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
'@)
$panel = $panel.Replace('final class LegacyMultiplayerMenu {`n    boolean handleKeyPressed(int code) { return false; }`n    void endDirectEdit() {}`n}', @'
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
'@)

$panel = $panel.Replace('final class LegacyRenderScaling {`r`n    void applyOptions(GameOptions options) {}`r`n    String auditSummary() { return "legacyRenderScaling active"; }`r`n}', @'
final class LegacyRenderScaling {
    void applyOptions(GameOptions options) {}
    String auditSummary() { return "legacyRenderScaling active"; }
    int internalWidth() { return 1280; }
    int internalHeight() { return 720; }
    String profileLabel() { return "Legacy bridge"; }
    String downscaleLabel() { return "1x"; }
}
'@)
$panel = $panel.Replace('final class LegacyRenderScaling {`n    void applyOptions(GameOptions options) {}`n    String auditSummary() { return "legacyRenderScaling active"; }`n}', @'
final class LegacyRenderScaling {
    void applyOptions(GameOptions options) {}
    String auditSummary() { return "legacyRenderScaling active"; }
    int internalWidth() { return 1280; }
    int internalHeight() { return 720; }
    String profileLabel() { return "Legacy bridge"; }
    String downscaleLabel() { return "1x"; }
}
'@)

$panel = $panel.Replace('final class LegacyFrameLimiter {`r`n    void configure(GameOptions options) {}`r`n}', @'
final class LegacyFrameLimiter {
    void configure(GameOptions options) {}
    LegacyFrameLimiterSnapshot snapshot(boolean stressActive) { return new LegacyFrameLimiterSnapshot(stressActive); }
}

final class LegacyFrameLimiterSnapshot {
    final boolean stressActive;
    LegacyFrameLimiterSnapshot(boolean stressActive) { this.stressActive = stressActive; }
    String compactLine() { return stressActive ? "stress test active" : "nominal"; }
}
'@)
$panel = $panel.Replace('final class LegacyFrameLimiter {`n    void configure(GameOptions options) {}`n}', @'
final class LegacyFrameLimiter {
    void configure(GameOptions options) {}
    LegacyFrameLimiterSnapshot snapshot(boolean stressActive) { return new LegacyFrameLimiterSnapshot(stressActive); }
}

final class LegacyFrameLimiterSnapshot {
    final boolean stressActive;
    LegacyFrameLimiterSnapshot(boolean stressActive) { this.stressActive = stressActive; }
    String compactLine() { return stressActive ? "stress test active" : "nominal"; }
}
'@)

$panel = $panel.Replace('final class LegacyImageSurface {`r`n    void reloadArtQuality(GameOptions options) {}`r`n    BufferedImage getTile(char tile) { return null; }`r`n    BufferedImage getNpcPortraitFor(Object npc) { return null; }`r`n}', @'
final class LegacyImageSurface {
    void reloadArtQuality(GameOptions options) {}
    BufferedImage get(String key) { return null; }
    BufferedImage getTile(char tile) { return null; }
    BufferedImage getNpcPortraitFor(Object npc) { return null; }
}
'@)
$panel = $panel.Replace('final class LegacyImageSurface {`n    void reloadArtQuality(GameOptions options) {}`n    BufferedImage getTile(char tile) { return null; }`n    BufferedImage getNpcPortraitFor(Object npc) { return null; }`n}', @'
final class LegacyImageSurface {
    void reloadArtQuality(GameOptions options) {}
    BufferedImage get(String key) { return null; }
    BufferedImage getTile(char tile) { return null; }
    BufferedImage getNpcPortraitFor(Object npc) { return null; }
}
'@)

$panel = $panel.Replace('final class LegacyFirstPersonRenderViewport {`r`n    boolean handleKeyPressed(GamePanel panel, int code) { return false; }`r`n}', @'
final class LegacyFirstPersonRenderViewport {
    boolean handleKeyPressed(GamePanel panel, int code) { return false; }
    boolean handleMouseClicked(GamePanel panel, java.awt.event.MouseEvent event, int mx, int my) { return false; }
}
'@)
$panel = $panel.Replace('final class LegacyFirstPersonRenderViewport {`n    boolean handleKeyPressed(GamePanel panel, int code) { return false; }`n}', @'
final class LegacyFirstPersonRenderViewport {
    boolean handleKeyPressed(GamePanel panel, int code) { return false; }
    boolean handleMouseClicked(GamePanel panel, java.awt.event.MouseEvent event, int mx, int my) { return false; }
}
'@)

$panel = $panel.Replace('final class LegacyRenderStressTest {`r`n    String toggle(LegacyFrameLimiter limiter) { return "Render stress test toggled."; }`r`n}', @'
final class LegacyRenderStressTest {
    private boolean active;
    String toggle(LegacyFrameLimiter limiter) { active = !active; return active ? "Render stress test active." : "Render stress test inactive."; }
    boolean active() { return active; }
}
'@)
$panel = $panel.Replace('final class LegacyRenderStressTest {`n    String toggle(LegacyFrameLimiter limiter) { return "Render stress test toggled."; }`n}', @'
final class LegacyRenderStressTest {
    private boolean active;
    String toggle(LegacyFrameLimiter limiter) { active = !active; return active ? "Render stress test active." : "Render stress test inactive."; }
    boolean active() { return active; }
}
'@)

if ($panel -notmatch 'final class LegacyGamepadInputEngine') {
    $panel = Insert-BeforeLastBrace $panel @'

final class LegacyGamepadInputEngine {
    String status() { return "not started"; }
}
'@
}

Set-Content -LiteralPath $panelPath -Value $panel
Write-Host 'Applied client bridge runtime compatibility patch.'
