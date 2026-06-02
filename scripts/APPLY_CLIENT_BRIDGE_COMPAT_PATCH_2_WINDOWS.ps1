$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$panelPath = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
$adapterPath = Join-Path $root 'src\mechanist\LegacyPanelContextAdapters.java'
if (-not (Test-Path -LiteralPath $panelPath -PathType Leaf)) { throw "Missing $panelPath" }
if (-not (Test-Path -LiteralPath $adapterPath -PathType Leaf)) { throw "Missing $adapterPath" }

function Insert-Before([string]$text, [string]$needle, [string]$insert) {
    $idx = $text.IndexOf($needle, [System.StringComparison]::Ordinal)
    if ($idx -lt 0) { throw "Needle not found: $needle" }
    return $text.Substring(0, $idx) + $insert + $text.Substring($idx)
}

$panel = Get-Content -LiteralPath $panelPath -Raw

# Core GamePanel constants and type surface.
if ($panel -notmatch 'MAX_FOOD_WATER') {
    $panel = $panel.Replace('    static final int HOURS_PER_DAY = 24;', @'
    static final int HOURS_PER_DAY = 24;
    static final int MAX_FOOD_WATER = 100;
    static final int AUTOSAVE_HOURLY_SLOT = 1001;
    static final int AUTOSAVE_ZONE_SLOT = 1002;
'@)
}
if ($panel -notmatch 'CONTAINER_TRADER_SHELF_PREFIX') {
    $panel = $panel.Replace('    static final String CONTAINER_MACHINE_INPUT_PREFIX = "machine-input-";', @'
    static final String CONTAINER_MACHINE_INPUT_PREFIX = "machine-input-";
    static final String CONTAINER_TRADER_SHELF_PREFIX = "trader-shelf-";
    static final String CONTAINER_FACTION_STOCK_PREFIX = "faction-stock-";
    static final String CONTAINER_CONTRACT_OBJECT_PREFIX = "contract-object-";
    static final String CONTAINER_CORPSE_LOOT_PREFIX = "corpse-loot-";
    static final String CONTAINER_ROOM_CACHE_PREFIX = "room-cache-";
'@)
}
$panel = $panel.Replace('    LegacyPanelAtlas atlas;', '    WorldAtlas atlas;')
$panel = $panel.Replace('    final LinkedHashMap<String, ItemProvenanceRecord> itemProvenance = new LinkedHashMap<>();', '    final LinkedHashMap<String, ArrayDeque<ItemProvenanceRecord>> itemProvenance = new LinkedHashMap<>();')
$panel = $panel.Replace('    final LinkedHashMap<Faction, Integer> factionStanding = new LinkedHashMap<>();', '    final EnumMap<Faction, Integer> factionStanding = new EnumMap<>(Faction.class);')
$panel = $panel.Replace('    final LinkedHashMap<Faction, Integer> temporaryHostileTurns = new LinkedHashMap<>();', '    final EnumMap<Faction, Integer> temporaryHostileTurns = new EnumMap<>(Faction.class);')
$panel = $panel.Replace('    final LinkedHashMap<Faction, Integer> factionMarketPressure = new LinkedHashMap<>();', '    final EnumMap<Faction, Integer> factionMarketPressure = new EnumMap<>(Faction.class);')

if ($panel -notmatch 'generatedJobCategoryFilterIndex') {
    $panel = $panel.Replace('    int graphicsDropdown = -1;', @'
    int graphicsDropdown = -1;
    int generatedJobCategoryFilterIndex;
    int generatedJobReadinessFilterIndex;
    int mouseX = -1;
    int mouseY = -1;
'@)
}

if ($panel -notmatch 'GamePanel\(JvmRuntimeProfileAuthority\.RuntimeConfig runtimeProfile\)') {
    $constructors = @'
    GamePanel() {}
    GamePanel(JvmRuntimeProfileAuthority.RuntimeConfig runtimeProfile) {
        if (runtimeProfile != null) this.jvmRuntimeProfile = runtimeProfile;
    }

'@
    $panel = Insert-Before $panel '    void runGuarded(String tag, String reason, Runnable body)' $constructors
}

$panel = $panel.Replace('    float portableLightIntensity(String itemName) { return 1.0f; }', '    int portableLightIntensity(String itemName) { return 100; }')

if ($panel -notmatch 'boolean hasLineOfSight\(int x0, int y0, int x1, int y1\)') {
    $bridgeMethods = @'
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
        if (atlas != null && atlas.currentZoneType() != null) visitedZoneTypes.add(atlas.currentZoneType());
        if (atlas != null) visitedZoneInstances.add(atlas.sectorX + ":" + atlas.sectorY + ":" + atlas.floor + ":" + atlas.zoneX + ":" + atlas.zoneY + ":" + atlas.sewer);
    }

'@
    $panel = Insert-Before $panel '    boolean verifyItemOperationalParity(String context)' $bridgeMethods
}

# Replace the fragile helper-class footer wholesale.  A previous patch inserted LegacyGamepadInputEngine inside LegacyPanelProfile; this normalizes the entire footer.
$footerPattern = '(?s)final class LegacyMultiplayerMenu \{.*\z'
$footer = @'
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
    void reloadArtQuality(GameOptions options) {}
    BufferedImage get(String key) { return null; }
    BufferedImage getTile(char tile) { return null; }
    BufferedImage getNpcPortraitFor(Object npc) { return null; }
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
'@
$panel = [regex]::Replace($panel, $footerPattern, $footer)
Set-Content -LiteralPath $panelPath -Value $panel

$adapter = Get-Content -LiteralPath $adapterPath -Raw
$adapter = $adapter.Replace('public int runtimeWorldTurn() { return panel == null ? 0 : panel.worldTurn; }', 'public int runtimeWorldTurn() { return panel == null ? 0 : (int)Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, panel.worldTurn)); }')
Set-Content -LiteralPath $adapterPath -Value $adapter

Write-Host 'Applied client bridge compatibility patch 2.'
