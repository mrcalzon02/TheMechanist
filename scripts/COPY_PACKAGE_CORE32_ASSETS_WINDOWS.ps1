param(
    [string]$PackageDir = "PACKAGE_client",
    [int]$Resolution = 32,
    [switch]$CleanAssetRoot
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$pkg = if ([System.IO.Path]::IsPathRooted($PackageDir)) { $PackageDir } else { Join-Path $root $PackageDir }
$pkg = [System.IO.Path]::GetFullPath([string]$pkg)
$compiledRoot = Join-Path $root 'ROOT_tools\atlas_asset_pipeline\compiled_assets'
$sourceFolder = Join-Path $compiledRoot ("${Resolution}px")
$assetRoot = Join-Path $pkg 'assets'
$destCompiledRoot = Join-Path (Join-Path $assetRoot 'compiled_assets') ("${Resolution}px")
$destIndexes = Join-Path $assetRoot 'indexes'
$sourceIndex = Join-Path $compiledRoot 'asset_content_index_256px.tsv'
$sourceManifest = Join-Path $compiledRoot 'asset_compile_manifest.json'
$packageIndex = Join-Path $destIndexes ("asset_content_index_${Resolution}px.tsv")
$diag = Join-Path $root 'diagnostics'
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$run = Join-Path $diag "package_compiled_${Resolution}px_assets_$stamp"
New-Item -ItemType Directory -Force -Path $run | Out-Null
$summary = Join-Path $run 'SUMMARY.txt'
$copied = Join-Path $run 'copied.tsv'
$missing = Join-Path $run 'missing.tsv'
$inventory = Join-Path $run 'package_asset_inventory.txt'

function Publish-Latest() {
    Copy-Item -LiteralPath $summary -Destination (Join-Path $diag 'LATEST_CORE32_ASSET_COPY_SUMMARY.txt') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $copied -Destination (Join-Path $diag 'LATEST_CORE32_ASSET_COPIED.tsv') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $missing -Destination (Join-Path $diag 'LATEST_CORE32_ASSET_MISSING.tsv') -Force -ErrorAction Continue
    Copy-Item -LiteralPath $inventory -Destination (Join-Path $diag 'LATEST_PACKAGE_ASSET_INVENTORY.txt') -Force -ErrorAction Continue
}

function Rewrite-ResolutionPath([string]$path, [int]$resolution) {
    if ([string]::IsNullOrWhiteSpace($path)) { return $null }
    $p = $path.Trim().Trim('"') -replace '\\','/'
    $p = $p -replace '^256px/', ("${resolution}px/")
    $p = $p -replace '_256px\.', ("_${resolution}px.")
    return $p
}

function Copy-IndexedAsset([string]$indexedPath) {
    $rel = Rewrite-ResolutionPath $indexedPath $Resolution
    if ([string]::IsNullOrWhiteSpace($rel)) { return }
    $source = Join-Path $compiledRoot ($rel -replace '/', [System.IO.Path]::DirectorySeparatorChar)
    if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
        "$indexedPath`t$rel`tmissing source file" | Add-Content -LiteralPath $missing
        $script:missingCount++
        return
    }
    $tail = $rel -replace "^${Resolution}px/", ''
    $dest = Join-Path $destCompiledRoot ($tail -replace '/', [System.IO.Path]::DirectorySeparatorChar)
    New-Item -ItemType Directory -Force -Path (Split-Path -Parent $dest) | Out-Null
    Copy-Item -LiteralPath $source -Destination $dest -Force -ErrorAction Stop
    $len = (Get-Item -LiteralPath $source).Length
    "$source`t$dest`t$len" | Add-Content -LiteralPath $copied
    $script:copiedCount++
    $script:copiedBytes += [int64]$len
}

"Compiled asset package copy run: $stamp" | Set-Content -LiteralPath $summary
"Repo root: $root" | Add-Content -LiteralPath $summary
"Package root: $pkg" | Add-Content -LiteralPath $summary
"Compiled root: $compiledRoot" | Add-Content -LiteralPath $summary
"Source folder: $sourceFolder" | Add-Content -LiteralPath $summary
"Source index: $sourceIndex" | Add-Content -LiteralPath $summary
"Destination compiled root: $destCompiledRoot" | Add-Content -LiteralPath $summary
"Resolution: $Resolution" | Add-Content -LiteralPath $summary
"source`tdestination`tbytes" | Set-Content -LiteralPath $copied
"index_path`trewritten_path`treason" | Set-Content -LiteralPath $missing

if ($CleanAssetRoot -and (Test-Path -LiteralPath $assetRoot)) { Remove-Item -LiteralPath $assetRoot -Recurse -Force -ErrorAction Continue }
New-Item -ItemType Directory -Force -Path $destCompiledRoot | Out-Null
New-Item -ItemType Directory -Force -Path $destIndexes | Out-Null
$script:copiedCount = 0
$script:missingCount = 0
$script:copiedBytes = [int64]0

if (-not (Test-Path -LiteralPath $compiledRoot -PathType Container)) {
    "ERROR: compiled asset root missing: $compiledRoot" | Add-Content -LiteralPath $summary
    Publish-Latest
    exit 2
}
if (-not (Test-Path -LiteralPath $sourceFolder -PathType Container)) {
    "ERROR: source resolution folder missing: $sourceFolder" | Add-Content -LiteralPath $summary
    Publish-Latest
    exit 3
}
if (-not (Test-Path -LiteralPath $sourceIndex -PathType Leaf)) {
    "ERROR: source index missing: $sourceIndex" | Add-Content -LiteralPath $summary
    Publish-Latest
    exit 4
}

$header = $true
$rows = 0
Get-Content -LiteralPath $sourceIndex -ErrorAction Stop | ForEach-Object {
    $line = $_
    if ($header) { $header = $false; return }
    if ([string]::IsNullOrWhiteSpace($line)) { return }
    $cols = $line -split "`t"
    if ($cols.Count -lt 2) { return }
    $rows++
    Copy-IndexedAsset $cols[1]
}

if (Test-Path -LiteralPath $sourceManifest -PathType Leaf) { Copy-Item -LiteralPath $sourceManifest -Destination (Join-Path $destIndexes 'asset_compile_manifest.json') -Force -ErrorAction Continue }
if (Test-Path -LiteralPath (Join-Path $compiledRoot 'asset_content_index_256px.json') -PathType Leaf) { Copy-Item -LiteralPath (Join-Path $compiledRoot 'asset_content_index_256px.json') -Destination (Join-Path $destIndexes 'asset_content_index_256px.json') -Force -ErrorAction Continue }
if (Test-Path -LiteralPath $sourceIndex -PathType Leaf) { Copy-Item -LiteralPath $sourceIndex -Destination (Join-Path $destIndexes 'asset_content_index_256px.tsv') -Force -ErrorAction Continue }

$first = $true
Get-Content -LiteralPath $sourceIndex -ErrorAction Continue | ForEach-Object {
    if ($first) { $_ | Set-Content -LiteralPath $packageIndex; $first = $false; return }
    if ([string]::IsNullOrWhiteSpace($_)) { return }
    $cols = $_ -split "`t"
    if ($cols.Count -ge 3) {
        $cols[1] = Rewrite-ResolutionPath $cols[1] $Resolution
        $cols[2] = [string]$Resolution
        ($cols -join "`t") | Add-Content -LiteralPath $packageIndex
    }
}

if (Test-Path -LiteralPath $assetRoot -PathType Container) {
    Get-ChildItem -LiteralPath $assetRoot -Recurse -File -Force -ErrorAction SilentlyContinue |
        ForEach-Object { $_.FullName.Substring($assetRoot.Length).TrimStart([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)) } |
        Sort-Object | Set-Content -LiteralPath $inventory
} else { 'NO PACKAGE assets DIRECTORY PRESENT' | Set-Content -LiteralPath $inventory }
$final = @(Get-ChildItem -LiteralPath $assetRoot -Recurse -File -Force -ErrorAction SilentlyContinue).Count
"IndexedRows: $rows" | Add-Content -LiteralPath $summary
"CopiedAssets: $script:copiedCount" | Add-Content -LiteralPath $summary
"MissingAssets: $script:missingCount" | Add-Content -LiteralPath $summary
"CopiedBytes: $script:copiedBytes" | Add-Content -LiteralPath $summary
"FinalPackageAssetFiles: $final" | Add-Content -LiteralPath $summary
"Package index: $packageIndex" | Add-Content -LiteralPath $summary
Publish-Latest
Write-Host "Copied $script:copiedCount indexed ${Resolution}px asset(s) into PACKAGE_client. Missing: $script:missingCount. Final asset files: $final"
exit 0
