[CmdletBinding()]
param(
    [string]$Output,
    [string]$Report,
    [switch]$NoReleaseHardening
)

$ErrorActionPreference = 'Stop'
$Root = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $Root

if ([string]::IsNullOrWhiteSpace($Output)) {
    $Output = if ($env:MECHANIST_LOCAL_GATE_OUTPUT) {
        $env:MECHANIST_LOCAL_GATE_OUTPUT
    } else {
        Join-Path $Root 'dist\local-java-gate'
    }
}
if ([string]::IsNullOrWhiteSpace($Report)) {
    $Report = if ($env:MECHANIST_LOCAL_GATE_REPORT) {
        $env:MECHANIST_LOCAL_GATE_REPORT
    } else {
        Join-Path $Root 'dist\local-java-gate-report.json'
    }
}

$Python = Get-Command python -ErrorAction Stop
$Arguments = @(
    'ROOT_build/ci/run_local_java_release_gate.py',
    '--output', $Output,
    '--report', $Report
)
if (-not $NoReleaseHardening) {
    $Arguments += '--release-hardened'
}

& $Python.Source @Arguments
if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}
