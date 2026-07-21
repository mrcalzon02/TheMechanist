[CmdletBinding()]
param(
    [string]$Distribution,
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$AdditionalArguments
)

$ErrorActionPreference = "Stop"
$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
Set-Location $ProjectRoot

$Arguments = @("ROOT_build/ci/run_local_native_gate.py")
if (-not [string]::IsNullOrWhiteSpace($Distribution)) {
    $Arguments += @("--distribution", $Distribution)
}
if ($AdditionalArguments) {
    $Arguments += $AdditionalArguments
}

& python @Arguments
exit $LASTEXITCODE
