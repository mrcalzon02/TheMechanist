param(
    [string]$PackageDir = "PACKAGE_client",
    [string[]]$ExtraSearchRoots = @(),
    [switch]$IncludeTextManifests
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$packageRoot = if ([System.IO.Path]::IsPathRooted($PackageDir)) { $PackageDir } else { Join-Path $root $PackageDir }
$packageRoot = [System.IO.Path]::GetFullPath([string]$packageRoot)
$assetDestRoot = Join-Path $packageRoot 'assets'
$diagRoot = Join-Path $root 'diagnostics'
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$runRoot = Join-Path $diagRoot "copy_real_assets_$stamp"
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null
New-Item -ItemType Directory -Force -Path $assetDestRoot | Out-Null

$summary = Join-Path $runRoot 'COPY_REAL_ASSETS_SUMMARY.txt'
$copiedList = Join-Path $runRoot 'copied_assets.tsv'
$candidateList = Join-Path $runRoot 'candidate_assets.tsv'
$assetInventory = Join-Path $runRoot 'package_asset_inventory.txt'

function Publish-LatestAliases() {
    Copy-Item -LiteralPath $summary -Destination (Join-Path $diagRoot 'LATEST_COPY_REAL_ASSETS_SUMMARY.txt') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $copiedList -Destination (Join-Path $diagRoot 'LATEST_COPY_REAL_ASSETS_COPIED.tsv') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $candidateList -Destination (Join-Path $diagRoot 'LATEST_COPY_REAL_ASSETS_CANDIDATES.tsv') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $assetInventory -Destination (Join-Path $diagRoot 'LATEST_PACKAGE_ASSET_INVENTORY.txt') -Force -ErrorAction Continue
}

function Is-ExcludedPath([string]$path) {
    $p = $path.ToLowerInvariant()
    return $p.Contains('\.git\') -or
           $p.Contains('\diagnostics\') -or
           $p.Contains('\build\') -or
           $p.Contains('\classes\') -or
           $p.Contains('\target\') -or
           $p.Contains('\out\') -or
           $p.Contains('\node_modules\') -or
           $p.Contains('\.gradle\') -or
           $p.Contains('\.idea\') -or
           $p.Contains('\.vscode\')
}

function Is-RealAssetFile([System.IO.FileInfo]$file) {
    if ($null -eq $file) { return $false }
    if (Is-ExcludedPath $file.FullName) { return $false }
    $ext = $file.Extension.ToLowerInvariant()
    $media = @('.png','.jpg','.jpeg','.gif','.bmp','.webp','.svg','.wav','.ogg','.mp3','.flac','.midi','.mid','.ttf','.otf','.fnt')
    $data = @('.json','.jsonl','.tsv','.csv','.properties','.xml','.atlas','.aseprite','.tsx','.tmx','.txt')
    if ($media -contains $ext) { return $true }
    if (($data -contains $ext) -and $IncludeTextManifests) { return $true }
    if ($file.Name -ieq 'semantic_asset_registry.tsv') { return $true }
    if ($file.Name -ieq 'generated_asset_runtime.tsv') { return $true }
    return $false
}

function Relative-AfterUsefulRoot([string]$filePath, [string]$searchRoot) {
    $full = [System.IO.Path]::GetFullPath($filePath)
    $norm = $full -replace '/', '\'
    $markers = @('\assets\', '\asset\', '\art\', '\images\', '\image\', '\sprites\', '\sprite\', '\audio\', '\sounds\', '\sound\', '\music\', '\fonts\', '\tiles\', '\textures\')
    foreach ($marker in $markers) {
        $idx = $norm.ToLowerInvariant().IndexOf($marker)
        if ($idx -ge 0) {
            return $norm.Substring($idx + $marker.Length)
        }
    }
    $rootFull = [System.IO.Path]::GetFullPath($searchRoot).TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)) + [System.IO.Path]::DirectorySeparatorChar
    if ($full.StartsWith($rootFull, [System.StringComparison]::OrdinalIgnoreCase)) { return $full.Substring($rootFull.Length) }
    return [System.IO.Path]::GetFileName($full)
}

function Copy-Asset([System.IO.FileInfo]$file, [string]$searchRoot) {
    $relative = Relative-AfterUsefulRoot $file.FullName $searchRoot
    if ([string]::IsNullOrWhiteSpace($relative)) { $relative = $file.Name }
    if ([System.IO.Path]::IsPathRooted($relative)) { $relative = $file.Name }
    $dest = Join-Path $assetDestRoot $relative
    $parent = Split-Path -Parent $dest
    if (-not [string]::IsNullOrWhiteSpace($parent)) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }
    Copy-Item -LiteralPath $file.FullName -Destination $dest -Force -ErrorAction Stop
    return $dest
}

"Copy Real Assets Run: $stamp" | Set-Content -LiteralPath $summary
"Repo root: $root" | Add-Content -LiteralPath $summary
"Package root: $packageRoot" | Add-Content -LiteralPath $summary
"Asset destination: $assetDestRoot" | Add-Content -LiteralPath $summary
"IncludeTextManifests: $IncludeTextManifests" | Add-Content -LiteralPath $summary

$searchRoots = New-Object System.Collections.Generic.List[string]
$searchRoots.Add($root)
$parent = Split-Path -Parent $root
if ($parent -and (Test-Path -LiteralPath $parent -PathType Container)) { $searchRoots.Add($parent) }
foreach ($extra in $ExtraSearchRoots) {
    if (-not [string]::IsNullOrWhiteSpace($extra)) {
        $candidate = if ([System.IO.Path]::IsPathRooted($extra)) { $extra } else { Join-Path $root $extra }
        if (Test-Path -LiteralPath $candidate -PathType Container) { $searchRoots.Add([System.IO.Path]::GetFullPath($candidate)) }
    }
}
$uniqueRoots = $searchRoots | Select-Object -Unique
"Search roots:" | Add-Content -LiteralPath $summary
$uniqueRoots | ForEach-Object { "  $_" | Add-Content -LiteralPath $summary }

"source`troot`textManifestAllowed" | Set-Content -LiteralPath $candidateList
"source`tdestination`tbytes" | Set-Content -LiteralPath $copiedList
$seen = New-Object 'System.Collections.Generic.HashSet[string]' ([System.StringComparer]::OrdinalIgnoreCase)
$candidateCount = 0
$copiedCount = 0
$copiedBytes = [int64]0

foreach ($sr in $uniqueRoots) {
    if (-not (Test-Path -LiteralPath $sr -PathType Container)) { continue }
    Get-ChildItem -LiteralPath $sr -Recurse -File -Force -ErrorAction SilentlyContinue | ForEach-Object {
        $file = $_
        if (-not (Is-RealAssetFile $file)) { return }
        if (-not $seen.Add($file.FullName)) { return }
        $candidateCount++
        "$($file.FullName)`t$sr`t$IncludeTextManifests" | Add-Content -LiteralPath $candidateList
        try {
            $dest = Copy-Asset $file $sr
            $copiedCount++
            $copiedBytes += [int64]$file.Length
            "$($file.FullName)`t$dest`t$($file.Length)" | Add-Content -LiteralPath $copiedList
        } catch {
            "FAILED`t$($file.FullName)`t$($_.Exception.Message)" | Add-Content -LiteralPath $copiedList
        }
    }
}

if (Test-Path -LiteralPath $assetDestRoot -PathType Container) {
    Get-ChildItem -LiteralPath $assetDestRoot -Recurse -File -Force -ErrorAction SilentlyContinue |
        ForEach-Object { $_.FullName.Substring($assetDestRoot.Length).TrimStart([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)) } |
        Sort-Object | Set-Content -LiteralPath $assetInventory
} else {
    'NO PACKAGE assets DIRECTORY PRESENT' | Set-Content -LiteralPath $assetInventory
}
$finalAssetCount = @(Get-ChildItem -LiteralPath $assetDestRoot -Recurse -File -Force -ErrorAction SilentlyContinue).Count

"CandidateAssets: $candidateCount" | Add-Content -LiteralPath $summary
"CopiedAssets: $copiedCount" | Add-Content -LiteralPath $summary
"CopiedBytes: $copiedBytes" | Add-Content -LiteralPath $summary
"FinalPackageAssetFiles: $finalAssetCount" | Add-Content -LiteralPath $summary
if ($copiedCount -eq 0) {
    "WARNING: No physical asset payload files were copied. Either the assets are outside the searched roots, not present locally, or have unsupported extensions." | Add-Content -LiteralPath $summary
}
Publish-LatestAliases
Write-Host "Copied $copiedCount real asset file(s) into $assetDestRoot. Final package asset file count: $finalAssetCount"
exit 0
