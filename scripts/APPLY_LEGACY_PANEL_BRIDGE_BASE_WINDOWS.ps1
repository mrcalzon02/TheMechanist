$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$path = Join-Path $root 'src\mechanist\LegacyPanelContext.java'
if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing $path" }
$text = Get-Content -LiteralPath $path -Raw
$old = 'class GamePanel extends JPanel {'
$new = 'class GamePanel extends LegacyPanelBridgeBase {'
if ($text.Contains($new)) {
    Write-Host 'GamePanel already extends LegacyPanelBridgeBase.'
    exit 0
}
if (-not $text.Contains($old)) { throw "Could not find expected declaration: $old" }
$text = $text.Replace($old, $new)
Set-Content -LiteralPath $path -Value $text
Write-Host 'Updated GamePanel to extend LegacyPanelBridgeBase.'
