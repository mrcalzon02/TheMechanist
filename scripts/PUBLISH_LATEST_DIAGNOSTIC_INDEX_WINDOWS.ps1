param(
    [switch]$DryRun
)

$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent $PSScriptRoot
$diagRoot = Join-Path $root 'diagnostics'

if (-not (Test-Path -LiteralPath $diagRoot -PathType Container)) {
    Write-Host "No diagnostics directory found: $diagRoot"
    exit 0
}

function Latest-Directory($pattern) {
    @(Get-ChildItem -LiteralPath $diagRoot -Directory -Filter $pattern -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1)
}

function Copy-IfExists($source, $dest) {
    if (Test-Path -LiteralPath $source -PathType Leaf) {
        Write-Host "PUBLISH $source -> $dest"
        if (-not $DryRun) { Copy-Item -LiteralPath $source -Destination $dest -Force }
    } else {
        Write-Host "SKIP missing $source"
    }
}

$latestCompile = Latest-Directory 'function_compile_smoke_*'
$latestOps = Latest-Directory 'function_ops_smoke_*'
$index = Join-Path $diagRoot 'LATEST_DIAGNOSTICS_INDEX.txt'

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("Latest Diagnostics Index") | Out-Null
$lines.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')") | Out-Null
$lines.Add("Diagnostics root: $diagRoot") | Out-Null
$lines.Add("") | Out-Null

if ($latestCompile.Count -gt 0) {
    $compileDir = $latestCompile[0]
    $lines.Add("Latest compile diagnostic: $($compileDir.Name)") | Out-Null
    $lines.Add("Latest compile diagnostic path: $($compileDir.FullName)") | Out-Null
    Copy-IfExists (Join-Path $compileDir.FullName 'SUMMARY.txt') (Join-Path $diagRoot 'LATEST_COMPILE_SUMMARY.txt')
    Copy-IfExists (Join-Path $compileDir.FullName 'compile_errors.tsv') (Join-Path $diagRoot 'LATEST_COMPILE_ERRORS.tsv')
    Copy-IfExists (Join-Path $compileDir.FullName 'compile.log') (Join-Path $diagRoot 'LATEST_COMPILE_LOG.txt')
} else {
    $lines.Add('Latest compile diagnostic: none') | Out-Null
}

$lines.Add("") | Out-Null

if ($latestOps.Count -gt 0) {
    $opsDir = $latestOps[0]
    $lines.Add("Latest operations diagnostic: $($opsDir.Name)") | Out-Null
    $lines.Add("Latest operations diagnostic path: $($opsDir.FullName)") | Out-Null
    Copy-IfExists (Join-Path $opsDir.FullName 'SUMMARY.txt') (Join-Path $diagRoot 'LATEST_OPERATIONS_SUMMARY.txt')
    Copy-IfExists (Join-Path $opsDir.FullName 'operation_gates.tsv') (Join-Path $diagRoot 'LATEST_OPERATION_GATES.tsv')
} else {
    $lines.Add('Latest operations diagnostic: none') | Out-Null
}

if (-not $DryRun) {
    $lines | Set-Content -LiteralPath $index
    Write-Host "Wrote $index"
} else {
    $lines | ForEach-Object { Write-Host $_ }
    Write-Host 'Dry run only; no latest diagnostic files were written.'
}
