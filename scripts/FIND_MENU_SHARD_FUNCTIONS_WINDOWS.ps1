$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$diagRoot = Join-Path $root 'diagnostics'
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$runRoot = Join-Path $diagRoot "find_menu_shard_functions_$stamp"
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null

$summary = Join-Path $runRoot 'SUMMARY.txt'
$menuHits = Join-Path $runRoot 'menu_function_hits.tsv'
$buttonHits = Join-Path $runRoot 'button_setup_hits.tsv'
$painterHits = Join-Path $runRoot 'painter_hits.tsv'
$tempHits = Join-Path $runRoot 'temporary_bridge_contamination_hits.tsv'
$historyHints = Join-Path $runRoot 'git_history_menu_hints.txt'

function Publish-Latest() {
    Copy-Item -LiteralPath $summary -Destination (Join-Path $diagRoot 'LATEST_FIND_MENU_SHARD_FUNCTIONS_SUMMARY.txt') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $menuHits -Destination (Join-Path $diagRoot 'LATEST_MENU_FUNCTION_HITS.tsv') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $buttonHits -Destination (Join-Path $diagRoot 'LATEST_BUTTON_SETUP_HITS.tsv') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $painterHits -Destination (Join-Path $diagRoot 'LATEST_MENU_PAINTER_HITS.tsv') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $tempHits -Destination (Join-Path $diagRoot 'LATEST_TEMPORARY_BRIDGE_CONTAMINATION_HITS.tsv') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $historyHints -Destination (Join-Path $diagRoot 'LATEST_GIT_HISTORY_MENU_HINTS.txt') -Force -ErrorAction Continue
}

"Menu shard function locator: $stamp" | Set-Content -LiteralPath $summary
"Repo root: $root" | Add-Content -LiteralPath $summary
"file`tline`ttext" | Set-Content -LiteralPath $menuHits
"file`tline`ttext" | Set-Content -LiteralPath $buttonHits
"file`tline`ttext" | Set-Content -LiteralPath $painterHits
"file`tline`ttext" | Set-Content -LiteralPath $tempHits

$javaFiles = @(Get-ChildItem -LiteralPath (Join-Path $root 'src') -Recurse -File -Filter '*.java' -ErrorAction SilentlyContinue)
"JavaFiles: $($javaFiles.Count)" | Add-Content -LiteralPath $summary

$menuPattern = '(?i)(main.?menu|launcher.?menu|setup.*menu|build.*menu|menu.*button|new game|continue|load game|options|multiplayer|quit|mods|editor|knowledge)'
$buttonPattern = '(?i)(buttons\.add|new ButtonBox|ButtonBox\(|selectedButton|mainMenuButton|drawButtons|button\.action|runGuarded\(".*MENU|activate.*menu|handle.*menu)'
$painterPattern = '(?i)(implements ScreenPainter|paint\(Graphics2D g, GamePanel panel\)|BootSurfacePainter|IntroCrawlSurfacePainter|MainMenuSurfacePainter|LoadingSurfacePainter|MultiplayerSurfacePainter|OptionsScreenPainter)'
$tempPattern = '(?i)(temporary|compatibility bridge|PACKAGE ASSET FALLBACK|UNFOLDED CLIENT|GamePanel compatibility bridge initialized|paintGameBridgeSurface|installMainMenuInputBridge|mainMenuRouteLabels|startPackagedClientNewGame|INDEXED 32PX ASSET PACKAGE ACTIVE)'

$menuCount = 0
$buttonCount = 0
$painterCount = 0
$tempCount = 0
foreach ($file in $javaFiles) {
    $lineNo = 0
    try {
        Get-Content -LiteralPath $file.FullName -ErrorAction Stop | ForEach-Object {
            $lineNo++
            $line = $_
            if ($line -match $menuPattern) { "$($file.FullName)`t$lineNo`t$($line.Trim())" | Add-Content -LiteralPath $menuHits; $script:menuCount++ }
            if ($line -match $buttonPattern) { "$($file.FullName)`t$lineNo`t$($line.Trim())" | Add-Content -LiteralPath $buttonHits; $script:buttonCount++ }
            if ($line -match $painterPattern) { "$($file.FullName)`t$lineNo`t$($line.Trim())" | Add-Content -LiteralPath $painterHits; $script:painterCount++ }
            if ($line -match $tempPattern) { "$($file.FullName)`t$lineNo`t$($line.Trim())" | Add-Content -LiteralPath $tempHits; $script:tempCount++ }
        }
    } catch {}
}

Push-Location $root
try {
    "git log --oneline -- src/mechanist/MainMenuSurfacePainter.java src/mechanist/LegacyPanelContext.java" | Set-Content -LiteralPath $historyHints
    git log --oneline -- src/mechanist/MainMenuSurfacePainter.java src/mechanist/LegacyPanelContext.java 2>&1 | Add-Content -LiteralPath $historyHints
    "`r`nRecent diffs touching menu bridge files:`r`n" | Add-Content -LiteralPath $historyHints
    git log --stat -n 20 -- src/mechanist/MainMenuSurfacePainter.java src/mechanist/LegacyPanelContext.java 2>&1 | Add-Content -LiteralPath $historyHints
} catch {
    "git history lookup failed: $($_.Exception.Message)" | Add-Content -LiteralPath $historyHints
} finally {
    Pop-Location
}

"MenuHitRows: $script:menuCount" | Add-Content -LiteralPath $summary
"ButtonSetupHitRows: $script:buttonCount" | Add-Content -LiteralPath $summary
"PainterHitRows: $script:painterCount" | Add-Content -LiteralPath $summary
"TemporaryBridgeContaminationRows: $script:tempCount" | Add-Content -LiteralPath $summary
Publish-Latest
Write-Host "Menu shard locator complete. Menu hits: $script:menuCount Button hits: $script:buttonCount Painter hits: $script:painterCount Temp contamination hits: $script:tempCount"
exit 0
