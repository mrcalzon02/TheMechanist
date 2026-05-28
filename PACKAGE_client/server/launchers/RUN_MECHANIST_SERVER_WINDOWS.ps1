param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $ServerArgs
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ServerRoot = (Resolve-Path (Join-Path $ScriptDir '..')).Path
$JarPath = Join-Path $ServerRoot 'TheMechanistServer.jar'
$LogDir = Join-Path $env:LOCALAPPDATA 'TheMechanist\logs'
if ([string]::IsNullOrWhiteSpace($env:LOCALAPPDATA)) {
    $LogDir = Join-Path $ServerRoot 'logs'
}
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null
$LogFile = Join-Path $LogDir 'launch-server.log'

function Write-LogLine([string]$Message) {
    Add-Content -Path $LogFile -Value $Message -Encoding UTF8
}

Set-Content -Path $LogFile -Value '==================================================' -Encoding UTF8
Write-LogLine "The Mechanist segmented Windows server launcher started at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')"
Write-LogLine "Script dir: $ScriptDir"
Write-LogLine "Server root: $ServerRoot"

if (-not (Test-Path -LiteralPath $JarPath -PathType Leaf)) {
    Write-Host "ERROR: TheMechanistServer.jar was not found at $JarPath"
    Write-LogLine "ERROR: TheMechanistServer.jar was not found at $JarPath"
    exit 2
}

$java = Get-Command java.exe -ErrorAction SilentlyContinue
if ($null -eq $java) {
    Write-Host 'ERROR: Java 17 or newer was not found on PATH.'
    Write-LogLine 'ERROR: Java was not found on PATH.'
    exit 17
}

if ($null -eq $ServerArgs -or $ServerArgs.Count -eq 0) {
    $ServerArgs = @('--status')
}

Write-Host 'Launching The Mechanist headless server...'
Write-LogLine ('Server args: ' + ($ServerArgs -join ' '))
$output = & $java.Source -Xms256m -Xmx2048m -jar $JarPath @ServerArgs 2>&1
$exitCode = $LASTEXITCODE
foreach ($line in $output) { Write-LogLine ([string]$line) }
Write-LogLine "Process exited with code $exitCode at $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss.fff')"
if ($exitCode -ne 0) {
    Write-Host "ERROR: The Mechanist server exited with code $exitCode. See $LogFile"
}
exit $exitCode
