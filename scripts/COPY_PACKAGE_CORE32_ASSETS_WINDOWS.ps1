param(
    [string]$PackageDir = "PACKAGE_client",
    [string]$Tier = "low_32",
    [switch]$CleanAssetRoot
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$pkg = if ([System.IO.Path]::IsPathRooted($PackageDir)) { $PackageDir } else { Join-Path $root $PackageDir }
$pkg = [System.IO.Path]::GetFullPath($pkg)
$assetRoot = Join-Path $pkg 'assets'
$diag = Join-Path $root 'diagnostics'
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'
$run = Join-Path $diag "package_core32_assets_$stamp"
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

function Copy-Tree($source, $dest) {
    if (-not (Test-Path -LiteralPath $source -PathType Container)) { return 0 }
    $count = 0
    $base = [System.IO.Path]::GetFullPath($source).TrimEnd([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)) + [System.IO.Path]::DirectorySeparatorChar
    Get-ChildItem -LiteralPath $source -Recurse -File -Force -ErrorAction SilentlyContinue | ForEach-Object {
        $rel = $_.FullName.Substring($base.Length)
        $target = Join-Path $dest $rel
        New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
        Copy-Item -LiteralPath $_.FullName -Destination $target -Force -ErrorAction Stop
        "$($_.FullName)`t$target`t$($_.Length)" | Add-Content -LiteralPath $copied
        $script:bytes += [int64]$_.Length
        $script:count++
    }
    return $count
}

"Generated core/package asset copy: $stamp" | Set-Content -LiteralPath $summary
"Repo root: $root" | Add-Content -LiteralPath $summary
"Package root: $pkg" | Add-Content -LiteralPath $summary
"Tier: $Tier" | Add-Content -LiteralPath $summary
"source`tdestination`tbytes" | Set-Content -LiteralPath $copied
"path	reason" | Set-Content -LiteralPath $missing

if ($CleanAssetRoot -and (Test-Path -LiteralPath $assetRoot)) { Remove-Item -LiteralPath $assetRoot -Recurse -Force -ErrorAction Continue }
New-Item -ItemType Directory -Force -Path $assetRoot | Out-Null
$script:count = 0
$script:bytes = [int64]0

# The package client must use the sliced/generated runtime tier, not source atlases.
# Copy only canonical generated low_32/core_32 package trees when present.
$destGenerated = Join-Path (Join-Path (Join-Path $assetRoot 'graphics') 'generated') $Tier
Copy-Tree (Join-Path (Join-Path (Join-Path (Join-Path $root 'assets') 'graphics') 'generated') $Tier) $destGenerated | Out-Null
Copy-Tree (Join-Path (Join-Path $root 'exports') $Tier) $destGenerated | Out-Null
Copy-Tree (Join-Path $root 'core_32') $destGenerated | Out-Null
Copy-Tree (Join-Path $root 'package_32') $destGenerated | Out-Null

# Runtime indexes are small metadata and are required for registry/runtime resolution.
$destIndexes = Join-Path $assetRoot 'indexes'
New-Item -ItemType Directory -Force -Path $destIndexes | Out-Null
foreach ($idxRoot in @((Join-Path $root 'assets\indexes'), (Join-Path $pkg 'assets\indexes'))) {
    if (Test-Path -LiteralPath $idxRoot -PathType Container) {
        Get-ChildItem -LiteralPath $idxRoot -File -Force -ErrorAction SilentlyContinue | Where-Object { $_.Extension.ToLowerInvariant() -in @('.json','.tsv','.txt') } | ForEach-Object {
            $target = Join-Path $destIndexes $_.Name
            Copy-Item -LiteralPath $_.FullName -Destination $target -Force -ErrorAction Continue
            "$($_.FullName)`t$target`t$($_.Length)" | Add-Content -LiteralPath $copied
            $script:count++
            $script:bytes += [int64]$_.Length
        }
    }
}

if (Test-Path -LiteralPath $assetRoot -PathType Container) {
    Get-ChildItem -LiteralPath $assetRoot -Recurse -File -Force -ErrorAction SilentlyContinue |
        ForEach-Object { $_.FullName.Substring($assetRoot.Length).TrimStart([char[]]@([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)) } |
        Sort-Object | Set-Content -LiteralPath $inventory
} else { 'NO PACKAGE assets DIRECTORY PRESENT' | Set-Content -LiteralPath $inventory }
$final = @(Get-ChildItem -LiteralPath $assetRoot -Recurse -File -Force -ErrorAction SilentlyContinue).Count
"CopiedFiles: $script:count" | Add-Content -LiteralPath $summary
"CopiedBytes: $script:bytes" | Add-Content -LiteralPath $summary
"FinalPackageAssetFiles: $final" | Add-Content -LiteralPath $summary
if ($script:count -eq 0) { "WARNING: no generated low_32/core_32/package_32 assets were found under canonical package roots." | Add-Content -LiteralPath $summary }
Publish-Latest
Write-Host "Copied $script:count generated package asset/index files. Final PACKAGE_client asset file count: $final"
exit 0
