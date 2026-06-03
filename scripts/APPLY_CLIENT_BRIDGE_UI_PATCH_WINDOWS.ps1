$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$panelPath = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
$basePath = Join-Path $root 'src\mechanist\LegacyPanelBridgeBase.java'
if (-not (Test-Path -LiteralPath $panelPath -PathType Leaf)) { throw "Missing $panelPath" }
if (-not (Test-Path -LiteralPath $basePath -PathType Leaf)) { throw "Missing $basePath" }

$panel = Get-Content -LiteralPath $panelPath -Raw
$panel = $panel.Replace('enum PanelMode { NONE, CHARACTER, INVENTORY, CONTAINER, TRADE, LOOK, INTERACT, COMBAT, AUSPEX, CONSOLE, INFO, INFOPEDIA, BUILD, WORKBENCH, MAP, CRAFTING }', 'enum PanelMode { NONE, CHARACTER, INVENTORY, CONTAINER, TRADE, LOOK, INTERACT, COMBAT, AUSPEX, CONSOLE, INFO, INFOPEDIA, BUILD, WORKBENCH, MAP, CRAFTING, SCAVENGE }')
$panel = $panel.Replace('final ArrayList<Object> buttons = new ArrayList<>();', 'final ArrayList<ButtonBox> buttons = new ArrayList<>();')
Set-Content -LiteralPath $panelPath -Value $panel

$base = Get-Content -LiteralPath $basePath -Raw
if ($base -notmatch 'handleMouseClicked\(GamePanel panel, MouseEvent event, int mx, int my\)') {
    $base = $base.Replace('final class LegacyFirstPersonRenderViewport {`r`n    boolean handleKeyPressed(GamePanel panel, int code) { return false; }`r`n}', 'final class LegacyFirstPersonRenderViewport {`r`n    boolean handleKeyPressed(GamePanel panel, int code) { return false; }`r`n    boolean handleMouseClicked(GamePanel panel, MouseEvent event, int mx, int my) { return false; }`r`n}')
    $base = $base.Replace('final class LegacyFirstPersonRenderViewport {`n    boolean handleKeyPressed(GamePanel panel, int code) { return false; }`n}', 'final class LegacyFirstPersonRenderViewport {`n    boolean handleKeyPressed(GamePanel panel, int code) { return false; }`n    boolean handleMouseClicked(GamePanel panel, MouseEvent event, int mx, int my) { return false; }`n}')
}
if ($base -notmatch 'BufferedImage get\(String key\)') {
    $base = $base.Replace('BufferedImage getNpcPortraitFor(Object npc) { return null; }', 'BufferedImage getNpcPortraitFor(Object npc) { return null; }`r`n    BufferedImage get(String key) { return null; }')
    $base = $base.Replace('BufferedImage getNpcPortraitFor(Object npc) { return null; }', 'BufferedImage getNpcPortraitFor(Object npc) { return null; }`n    BufferedImage get(String key) { return null; }')
}
if ($base -notmatch 'boolean inputActive\(\)') {
    $base = $base.Replace('void endDirectEdit() {}', 'void endDirectEdit() {}`r`n    boolean inputActive() { return false; }`r`n    String directInput() { return ""; }`r`n    java.util.List<MultiplayerMenuController.ConnectionHistoryItem> history() { return java.util.Collections.emptyList(); }`r`n    int historyIndex() { return -1; }`r`n    java.util.List<MultiplayerMenuController.FavoriteServer> favorites() { return java.util.Collections.emptyList(); }`r`n    int favoriteIndex() { return -1; }`r`n    String status() { return "Multiplayer menu unavailable in legacy bridge."; }`r`n    boolean hasActiveHost() { return false; }`r`n    String activeHostLine() { return "No active host."; }')
    $base = $base.Replace('void endDirectEdit() {}', 'void endDirectEdit() {}`n    boolean inputActive() { return false; }`n    String directInput() { return ""; }`n    java.util.List<MultiplayerMenuController.ConnectionHistoryItem> history() { return java.util.Collections.emptyList(); }`n    int historyIndex() { return -1; }`n    java.util.List<MultiplayerMenuController.FavoriteServer> favorites() { return java.util.Collections.emptyList(); }`n    int favoriteIndex() { return -1; }`n    String status() { return "Multiplayer menu unavailable in legacy bridge."; }`n    boolean hasActiveHost() { return false; }`n    String activeHostLine() { return "No active host."; }')
}
Set-Content -LiteralPath $basePath -Value $base
Write-Host 'Applied client bridge UI compatibility patch.'
