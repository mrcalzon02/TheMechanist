$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$diagRoot = Join-Path $root 'diagnostics'
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$runRoot = Join-Path $diagRoot "find_boot_menu_surfaces_$stamp"
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null

$summary = Join-Path $runRoot 'SUMMARY.txt'
$classHits = Join-Path $runRoot 'boot_menu_class_candidates.tsv'
$contentHits = Join-Path $runRoot 'boot_menu_content_hits.tsv'
$screenHits = Join-Path $runRoot 'screen_enum_hits.tsv'

function Publish-Latest() {
    Copy-Item -LiteralPath $summary -Destination (Join-Path $diagRoot 'LATEST_FIND_BOOT_MENU_SURFACES_SUMMARY.txt') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $classHits -Destination (Join-Path $diagRoot 'LATEST_BOOT_MENU_CLASS_CANDIDATES.tsv') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $contentHits -Destination (Join-Path $diagRoot 'LATEST_BOOT_MENU_CONTENT_HITS.tsv') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $screenHits -Destination (Join-Path $diagRoot 'LATEST_SCREEN_ENUM_HITS.tsv') -Force -ErrorAction Continue
}

"Boot/menu surface locator: $stamp" | Set-Content -LiteralPath $summary
"Repo root: $root" | Add-Content -LiteralPath $summary
"file`tclass_or_filename" | Set-Content -LiteralPath $classHits
"file`tline`ttext" | Set-Content -LiteralPath $contentHits
"file`tline`ttext" | Set-Content -LiteralPath $screenHits

$javaFiles = @(Get-ChildItem -LiteralPath (Join-Path $root 'src') -Recurse -File -Filter '*.java' -ErrorAction SilentlyContinue)
"JavaFiles: $($javaFiles.Count)" | Add-Content -LiteralPath $summary

$classTerms = '(?i)(Boot|Intro|Crawl|Splash|Menu|Launcher|Title|Eula|Route|Surface|Painter|Sequence)'
$contentTerms = '(?i)(INTRO_CRAWL|ZONE_SPLASH|Screen\.BOOT|Screen\.MENU|Screen\.MAIN|boot sequence|intro crawl|zone splash|main menu|title_mechanist|subtitle_rebase|continueFromIntroCrawl|continueFromZoneSplash|finishBootSequence|paintComponent|implements ScreenPainter)'
$screenTerms = '(?i)enum Screen|Screen\.|setScreen\(|screen ==|screen ='

$classCount = 0
$contentCount = 0
$screenCount = 0
foreach ($file in $javaFiles) {
    if ($file.Name -match $classTerms) {
        "$($file.FullName)`t$($file.Name)" | Add-Content -LiteralPath $classHits
        $classCount++
    }
    $lineNo = 0
    try {
        Get-Content -LiteralPath $file.FullName -ErrorAction Stop | ForEach-Object {
            $lineNo++
            $line = $_
            if ($line -match $contentTerms) {
                "$($file.FullName)`t$lineNo`t$($line.Trim())" | Add-Content -LiteralPath $contentHits
                $script:contentCount++
            }
            if ($line -match $screenTerms) {
                "$($file.FullName)`t$lineNo`t$($line.Trim())" | Add-Content -LiteralPath $screenHits
                $script:screenCount++
            }
        }
    } catch {}
}

"ClassCandidateRows: $classCount" | Add-Content -LiteralPath $summary
"ContentHitRows: $script:contentCount" | Add-Content -LiteralPath $summary
"ScreenHitRows: $script:screenCount" | Add-Content -LiteralPath $summary
Publish-Latest
Write-Host "Boot/menu surface locator complete. Class candidates: $classCount Content hits: $script:contentCount Screen hits: $script:screenCount"
exit 0
