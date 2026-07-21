[CmdletBinding()]
param(
    [switch]$UpdateCommittedManifest,
    [switch]$RequireReleaseClearance
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
Set-Location $repo

$arguments = @('ROOT_build/ci/run_local_inventory_gate.py')
if ($UpdateCommittedManifest) { $arguments += '--update-committed-manifest' }
if ($RequireReleaseClearance) { $arguments += '--require-release-clearance' }

& python @arguments
exit $LASTEXITCODE
