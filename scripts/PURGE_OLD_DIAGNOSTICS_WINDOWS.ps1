param(
    [int]$KeepLatest = 1,
    [switch]$DryRun
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$diagRoot = Join-Path $root 'diagnostics'

if (-not (Test-Path -LiteralPath $diagRoot -PathType Container)) {
    Write-Host "No diagnostics directory found: $diagRoot"
    exit 0
}

$patterns = @(
    'function_ops_smoke_*',
    'function_compile_smoke_*',
    'shard8_smoke_*'
)

$targets = New-Object System.Collections.Generic.List[System.IO.DirectoryInfo]
foreach ($pattern in $patterns) {
    Get-ChildItem -LiteralPath $diagRoot -Directory -Filter $pattern -ErrorAction SilentlyContinue | ForEach-Object { $targets.Add($_) | Out-Null }
}

$deduped = $targets | Sort-Object FullName -Unique | Sort-Object Name -Descending
$keepCount = [Math]::Max(0, $KeepLatest)
$toKeep = @($deduped | Select-Object -First $keepCount)
$toDelete = @($deduped | Select-Object -Skip $keepCount)

Write-Host "Diagnostic run folders found: $($deduped.Count)"
Write-Host "Keeping latest: $($toKeep.Count)"
foreach ($dir in $toKeep) { Write-Host "KEEP   $($dir.FullName)" }

Write-Host "Deleting old diagnostic folders: $($toDelete.Count)"
foreach ($dir in $toDelete) {
    Write-Host "DELETE $($dir.FullName)"
    if (-not $DryRun) {
        Remove-Item -LiteralPath $dir.FullName -Recurse -Force -ErrorAction Continue
    }
}

if ($DryRun) {
    Write-Host 'Dry run only; no files were removed.'
} else {
    Write-Host 'Diagnostic purge complete.'
}
